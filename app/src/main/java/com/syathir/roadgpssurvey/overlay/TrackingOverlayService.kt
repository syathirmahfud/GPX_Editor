package com.syathir.roadgpssurvey.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.syathir.roadgpssurvey.R
import com.syathir.roadgpssurvey.RoadGpsApplication
import com.syathir.roadgpssurvey.model.TrackingStatus
import com.syathir.roadgpssurvey.service.TrackingService
import com.syathir.roadgpssurvey.tracking.TrackingRuntime
import com.syathir.roadgpssurvey.tracking.TrackingSnapshot
import com.syathir.roadgpssurvey.util.ChainageFormatter
import com.syathir.roadgpssurvey.ui.theme.DayNightSchedule
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TrackingOverlayService : Service() {
    companion object {
        fun show(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                context.startService(Intent(context, TrackingOverlayService::class.java))
            }
        }

        fun hide(context: Context) {
            context.stopService(Intent(context, TrackingOverlayService::class.java))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private lateinit var root: DragLayout
    private lateinit var staText: TextView
    private lateinit var detailText: TextView
    private lateinit var actionButton: Button
    private lateinit var markButton: Button
    private lateinit var closeButton: ImageButton
    private lateinit var layoutParams: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WindowManager::class.java)
        root = createOverlayView()
        layoutParams = WindowManager.LayoutParams(
            dp(224),
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = dp(96)
        }
        windowManager.addView(root, layoutParams)
        installDragHandler()
        combine(TrackingRuntime.snapshot, DayNightSchedule.observeNight()) { snapshot, night -> snapshot to night }
            .onEach { (snapshot, night) -> render(snapshot, night) }
            .launchIn(scope)
    }

    private fun createOverlayView(): DragLayout {
        val container = DragLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.argb(238, 10, 15, 20))
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.rgb(94, 230, 168))
            }
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        staText = overlayText(sizeSp = 22f, bold = true).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        closeButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = "Sembunyikan panel melayang"
            setOnClickListener { hideAndDisable() }
        }
        header.addView(staText)
        header.addView(closeButton, LinearLayout.LayoutParams(dp(40), dp(40)))
        detailText = overlayText(sizeSp = 15f, bold = false)
        actionButton = Button(this).apply {
            minHeight = dp(48)
            setOnClickListener {
                when (TrackingRuntime.snapshot.value.status) {
                    TrackingStatus.TRACKING -> TrackingService.send(this@TrackingOverlayService, TrackingService.ACTION_PAUSE_MARK)
                    TrackingStatus.PAUSED -> TrackingService.send(this@TrackingOverlayService, TrackingService.ACTION_RESUME)
                    else -> Unit
                }
            }
        }
        container.addView(header)
        container.addView(detailText)
        markButton = Button(this).apply {
            setText(R.string.overlay_mark)
            setOnClickListener { TrackingService.send(this@TrackingOverlayService, TrackingService.ACTION_MARK) }
        }
        container.addView(markButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)))
        container.addView(
            actionButton,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply {
                topMargin = dp(6)
            },
        )
        return container
    }

    private fun overlayText(sizeSp: Float, bold: Boolean) = TextView(this).apply {
        setTextColor(Color.WHITE)
        textSize = sizeSp
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun installDragHandler() {
        var initialX = 0
        var initialY = 0
        var downX = 0f
        var downY = 0f
        root.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    downX = event.rawX
                    downY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = (initialX - (event.rawX - downX)).toInt()
                    layoutParams.y = (initialY + (event.rawY - downY)).toInt()
                    windowManager.updateViewLayout(root, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    root.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun render(snapshot: TrackingSnapshot, night: Boolean) {
        val foreground = if (night) Color.WHITE else Color.BLACK
        (root.background as GradientDrawable).setColor(if (night) Color.BLACK else Color.WHITE)
        staText.setTextColor(foreground)
        detailText.setTextColor(foreground)
        closeButton.setColorFilter(foreground)
        listOf(actionButton, markButton).forEach {
            it.setTextColor(foreground)
            it.backgroundTintList = ColorStateList.valueOf(if (night) Color.DKGRAY else Color.LTGRAY)
        }
        markButton.isEnabled = snapshot.status == TrackingStatus.TRACKING
        staText.text = getString(
            R.string.overlay_sta,
            ChainageFormatter.format(snapshot.startChainageM + snapshot.totalDistanceM),
        )
        val accuracy = snapshot.accuracyM?.let { String.format(Locale.US, "±%.1f m", it) } ?: "Menunggu GPS"
        detailText.text = resources.getQuantityString(R.plurals.overlay_details, snapshot.markerCount,
            String.format(Locale.US, "%.2f", snapshot.totalDistanceM / 1_000.0), accuracy,
            snapshot.markerCount, snapshot.errorMessage?.let { "\n$it" } ?: "")
        when (snapshot.status) {
            TrackingStatus.TRACKING -> {
                actionButton.setText(R.string.overlay_pause_mark)
                actionButton.isEnabled = true
            }
            TrackingStatus.PAUSED -> {
                actionButton.setText(R.string.overlay_resume)
                actionButton.isEnabled = true
            }
            else -> {
                actionButton.setText(R.string.overlay_inactive)
                actionButton.isEnabled = false
            }
        }
    }

    private fun hideAndDisable() {
        val app = application as RoadGpsApplication
        scope.launch {
            val current = app.settingsRepository.settings.first()
            app.settingsRepository.update(current.copy(overlayEnabled = false))
            stopSelf()
        }
    }

    override fun onDestroy() {
        if (::root.isInitialized && root.isAttachedToWindow) windowManager.removeView(root)
        scope.cancel()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private class DragLayout(context: Context) : LinearLayout(context) {
        override fun performClick(): Boolean {
            super.performClick()
            return true
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
