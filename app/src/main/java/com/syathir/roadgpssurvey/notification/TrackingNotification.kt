package com.syathir.roadgpssurvey.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.syathir.roadgpssurvey.MainActivity
import com.syathir.roadgpssurvey.R
import com.syathir.roadgpssurvey.model.TrackingStatus
import com.syathir.roadgpssurvey.service.TrackingService
import com.syathir.roadgpssurvey.tracking.TrackingSnapshot
import com.syathir.roadgpssurvey.util.ChainageFormatter
import com.syathir.roadgpssurvey.util.TimeFormatter
import java.util.Locale

class TrackingNotification(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "gps_survey_tracking"
        const val NOTIFICATION_ID = 4107
    }

    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.tracking_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.tracking_channel_description)
                    setShowBadge(false)
                },
            )
        }
    }

    fun build(snapshot: TrackingSnapshot, nowElapsedMs: Long): Notification {
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (snapshot.status == TrackingStatus.PAUSED) {
            "SURVEI GPS — DIJEDA"
        } else {
            "SURVEI GPS — MEREKAM"
        }
        val sta = ChainageFormatter.format(snapshot.startChainageM + snapshot.totalDistanceM)
        val accuracy = snapshot.accuracyM?.let { String.format(Locale.US, "±%.1f m", it) } ?: "Menunggu GPS"
        val body = "STA $sta  •  ${String.format(Locale.US, "%.2f km", snapshot.totalDistanceM / 1_000.0)}\n" +
            "$accuracy  •  ${TimeFormatter.formatDuration(snapshot.elapsedAt(nowElapsedMs))}  •  ${snapshot.markerCount} titik" +
            (snapshot.errorMessage?.let { "\n$it" } ?: "")
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_gps)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
        when (snapshot.status) {
            TrackingStatus.TRACKING -> {
                builder.addAction(R.drawable.ic_gps, "TANDAI", serviceAction(4, TrackingService.ACTION_MARK))
                builder.addAction(
                    R.drawable.ic_gps,
                    "JEDA + TITIK",
                    serviceAction(1, TrackingService.ACTION_PAUSE_MARK),
                )
                builder.addAction(
                    R.drawable.ic_gps,
                    "SELESAI",
                    serviceAction(2, TrackingService.ACTION_FINISH),
                )
            }
            TrackingStatus.PAUSED -> {
                builder.addAction(
                    R.drawable.ic_gps,
                    "LANJUT",
                    serviceAction(3, TrackingService.ACTION_RESUME),
                )
                builder.addAction(
                    R.drawable.ic_gps,
                    "SELESAI",
                    serviceAction(2, TrackingService.ACTION_FINISH),
                )
            }
            else -> Unit
        }
        return builder.build()
    }

    private fun serviceAction(requestCode: Int, action: String): PendingIntent {
        val intent = Intent(context, TrackingService::class.java).setAction(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, requestCode, intent, flags)
        } else {
            PendingIntent.getService(context, requestCode, intent, flags)
        }
    }

    fun update(snapshot: TrackingSnapshot, nowElapsedMs: Long) {
        manager.notify(NOTIFICATION_ID, build(snapshot, nowElapsedMs))
    }

    fun showFinished() {
        val openApp = PendingIntent.getActivity(context, 10,
            Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(NOTIFICATION_ID + 1, NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_gps).setContentTitle("Perekaman selesai")
            .setContentText("Ketuk untuk mengisi Nama Ruas dan memilih ekspor.")
            .setContentIntent(openApp).setAutoCancel(true).build())
    }
}
