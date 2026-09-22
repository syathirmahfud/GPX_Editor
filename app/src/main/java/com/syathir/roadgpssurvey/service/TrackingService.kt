package com.syathir.roadgpssurvey.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.syathir.roadgpssurvey.RoadGpsApplication
import com.syathir.roadgpssurvey.data.GpsSettings
import com.syathir.roadgpssurvey.data.SurveyRepository
import com.syathir.roadgpssurvey.data.SurveySession
import com.syathir.roadgpssurvey.location.LocationFilterSettings
import com.syathir.roadgpssurvey.location.LocationSample
import com.syathir.roadgpssurvey.location.LocationTracker
import com.syathir.roadgpssurvey.model.TrackingStatus
import com.syathir.roadgpssurvey.notification.TrackingNotification
import com.syathir.roadgpssurvey.tracking.TrackingEngine
import com.syathir.roadgpssurvey.tracking.TrackingEvent
import com.syathir.roadgpssurvey.tracking.TrackingRuntime
import com.syathir.roadgpssurvey.tracking.TrackingSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TrackingService : Service() {
    companion object {
        const val ACTION_START = "com.syathir.roadgpssurvey.action.START"
        const val ACTION_PAUSE_MARK = "com.syathir.roadgpssurvey.action.PAUSE_MARK"
        const val ACTION_MARK = "com.syathir.roadgpssurvey.action.MARK"
        const val ACTION_RESUME = "com.syathir.roadgpssurvey.action.RESUME"
        const val ACTION_FINISH = "com.syathir.roadgpssurvey.action.FINISH"

        fun send(context: Context, action: String) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, TrackingService::class.java).setAction(action),
            )
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val stateMutex = Mutex()
    private lateinit var locationTracker: LocationTracker
    private lateinit var trackingNotification: TrackingNotification
    private lateinit var repository: SurveyRepository
    private lateinit var settings: GpsSettings
    private var engine = TrackingEngine()
    private var session: SurveySession? = null
    private var locationJob: Job? = null
    private var checkpointJob: Job? = null
    private var latestLocation: LocationSample? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as RoadGpsApplication
        locationTracker = LocationTracker(this)
        trackingNotification = TrackingNotification(this)
        repository = app.surveyRepository
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasLocationPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }
        ensureForeground()
        serviceScope.launch {
            stateMutex.withLock {
                settings = (application as RoadGpsApplication).settingsRepository.settings.first()
                restoreActiveSessionIfNeeded()
                try {
                when (intent?.action) {
                    ACTION_START -> startTracking()
                    ACTION_PAUSE_MARK -> captureMarker(pause = true)
                    ACTION_MARK -> captureMarker(pause = false)
                    ACTION_RESUME -> resumeTracking()
                    ACTION_FINISH -> finishTracking()
                    null -> if (session != null) startLocationUpdates() else stopTrackingService()
                }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Restore the committed state if a command's database write failed.
                    val stored = repository.getActive()
                    session = stored
                    engine = newEngine(stored?.toSnapshot() ?: TrackingSnapshot())
                    if (stored?.status == TrackingStatus.TRACKING) {
                        engine.restoreDistanceOrigin(repository.getLatestLocation(stored.id))
                    }
                    engine.setError("Tindakan gagal disimpan. Silakan coba lagi.")
                    publish()
                    if (stored == null) stopTrackingService()
                }
            }
        }
        return START_STICKY
    }

    private fun ensureForeground() {
        ServiceCompat.startForeground(
            this,
            TrackingNotification.NOTIFICATION_ID,
            trackingNotification.build(TrackingRuntime.snapshot.value, SystemClock.elapsedRealtime()),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
        )
    }

    private suspend fun restoreActiveSessionIfNeeded() {
        if (session != null) return
        var stored = repository.getActive() ?: return
        val now = SystemClock.elapsedRealtime()
        var restored = stored.toSnapshot()
        if (stored.status == TrackingStatus.TRACKING &&
            (stored.activeSegmentStartedElapsedRealtimeMs == null || stored.activeSegmentStartedElapsedRealtimeMs > now)
        ) {
            restored = restored.copy(
                status = TrackingStatus.PAUSED,
                activeSegmentStartedElapsedMs = null,
                errorMessage = "Perekaman dijeda karena perangkat dimulai ulang",
            )
            engine = newEngine(restored)
            stored = repository.persistTimer(stored, restored)
        } else {
            engine = newEngine(restored)
            if (stored.status == TrackingStatus.TRACKING) {
                engine.restoreDistanceOrigin(repository.getLatestLocation(stored.id))
            }
        }
        session = stored
        publish()
        startLocationUpdates()
    }

    private suspend fun startTracking() {
        if (session != null) {
            startLocationUpdates()
            publish()
            return
        }
        val nowElapsed = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        val name = "RUAS_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(nowWall))}"
        val created = repository.createSession(
            name = name,
            startedAt = nowWall,
            nowElapsedMs = nowElapsed,
            startChainageM = settings.startChainageM,
        )
        session = created
        engine = newEngine()
        engine.start(nowElapsed, created.startChainageM)
        publish()
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (locationJob != null) return
        locationJob = locationTracker.updates(settings.updateIntervalMs)
            .onEach { location ->
                stateMutex.withLock {
                    latestLocation = location
                    val currentSession = session ?: return@withLock
                    val event = engine.onLocation(location)
                    if (event is TrackingEvent.PointAccepted) {
                        session = repository.saveAcceptedPoint(currentSession, location, engine.snapshot)
                    }
                    publish()
                }
            }
            .catch { error ->
                stateMutex.withLock {
                    engine.setError("Pembaruan lokasi terhenti. Periksa GPS dan izin lokasi.")
                    publish()
                    locationJob = null
                }
            }
            .launchIn(serviceScope)
        if (checkpointJob == null) {
            checkpointJob = serviceScope.launch {
                while (isActive) {
                    delay(5_000L)
                    stateMutex.withLock {
                        val currentSession = session ?: return@withLock
                        engine.checkpoint(SystemClock.elapsedRealtime())
                        session = repository.persistTimer(currentSession, engine.snapshot)
                        publish()
                    }
                }
            }
        }
    }

    private suspend fun captureMarker(pause: Boolean) {
        val currentSession = session ?: return
        if (engine.snapshot.status != TrackingStatus.TRACKING) return
        val location = obtainFreshBestFix() ?: run {
            engine.setError("Lokasi GPS belum tersedia; titik belum disimpan dan perekaman tetap berjalan.")
            publish()
            return
        }
        val pointEvent = engine.onLocation(location)
        var updatedSession = currentSession
        if (pointEvent is TrackingEvent.PointAccepted) {
            updatedSession = repository.saveAcceptedPoint(updatedSession, location, engine.snapshot)
        }
        if (pause) engine.pauseAndMark(location, SystemClock.elapsedRealtime())
        else engine.markOnly(location, SystemClock.elapsedRealtime())
        session = repository.saveMarker(updatedSession, location, engine.snapshot)
        publish()
    }

    private suspend fun resumeTracking() {
        val currentSession = session ?: return
        if (engine.snapshot.status != TrackingStatus.PAUSED) return
        val location = obtainFreshBestFix() ?: run {
            engine.setError("Lokasi GPS terbaru belum tersedia; perekaman masih dijeda.")
            publish()
            return
        }
        engine.resume(location, SystemClock.elapsedRealtime())
        session = repository.resumeWithOrigin(currentSession, location, engine.snapshot)
        latestLocation = location
        publish()
    }

    private suspend fun finishTracking() {
        val currentSession = session ?: run {
            stopTrackingService()
            return
        }
        if (engine.snapshot.status == TrackingStatus.TRACKING || engine.snapshot.status == TrackingStatus.PAUSED) {
            engine.finish(SystemClock.elapsedRealtime())
            repository.finish(currentSession, engine.snapshot, System.currentTimeMillis())
            publish()
            trackingNotification.showFinished()
        }
        session = null
        stopTrackingService()
    }

    private suspend fun obtainFreshBestFix(): LocationSample? {
        val requested = locationTracker.currentHighAccuracyFix()
        if (requested != null && usableFix(requested)) return requested
        val latest = latestLocation ?: return null
        val ageNanos = SystemClock.elapsedRealtimeNanos() - latest.elapsedRealtimeNanos
        return latest.takeIf { ageNanos in 0..15_000_000_000L && usableFix(it) }
    }

    private fun usableFix(fix: LocationSample): Boolean =
        fix.latitude.isFinite() && fix.latitude in -90.0..90.0 &&
            fix.longitude.isFinite() && fix.longitude in -180.0..180.0 &&
            fix.horizontalAccuracyM.isFinite() && fix.horizontalAccuracyM > 0f

    private fun newEngine(snapshot: TrackingSnapshot = TrackingSnapshot()): TrackingEngine = TrackingEngine(
        filterSettings = LocationFilterSettings(
            maximumAccuracyM = settings.maximumAccuracyM,
            minimumMovementM = settings.minimumMovementM,
        ),
        initialSnapshot = snapshot,
    )

    private fun SurveySession.toSnapshot() = TrackingSnapshot(
        status = status,
        totalDistanceM = totalDistanceM,
        activeElapsedMs = activeElapsedMs,
        activeSegmentStartedElapsedMs = activeSegmentStartedElapsedRealtimeMs,
        startChainageM = startChainageM,
        markerCount = markerCount,
    )

    private fun publish() {
        TrackingRuntime.publish(engine.snapshot.copy(sessionId = session?.id))
        trackingNotification.update(engine.snapshot, SystemClock.elapsedRealtime())
    }

    private fun stopTrackingService() {
        locationJob?.cancel()
        checkpointJob?.cancel()
        locationJob = null
        checkpointJob = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
