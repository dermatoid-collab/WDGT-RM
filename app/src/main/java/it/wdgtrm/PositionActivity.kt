package it.wdgtrm

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Schermata trasparente sopra la Home: l'utente tocca il centro del widget e il punto
 * viene memorizzato, perché One UI non comunica la posizione dei widget alle app.
 */
class PositionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (widgetId == -1) {
            finish()
            return
        }
        val root = FrameLayout(this)
        root.setBackgroundColor(0x80000000.toInt())
        root.addView(
            TextView(this).apply {
                text = "Tocca il centro del widget\n(indietro per annullare)"
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(dp(24), dp(24), dp(24), dp(24))
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP,
            ).apply { topMargin = root.dp(96) },
        )
        root.setOnTouchListener { _, e ->
            if (e.actionMasked == MotionEvent.ACTION_UP) {
                Store.savePosition(this, widgetId, e.rawX, e.rawY)
                finish()
            }
            true
        }
        setContentView(root)
    }
}
