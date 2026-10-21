package it.wdgtrm

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.doOnLayout

/**
 * Posizionamento del menu sopra il widget, con anteprima: un riquadro grande come il widget
 * si trascina (o si sposta con le frecce) sul widget visibile sotto; il menu vero in anteprima
 * lo segue. One UI non comunica la posizione dei widget alle app, quindi la si indica qui.
 */
class PositionActivity : Activity() {
    private var cx = 0f
    private var cy = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (widgetId == -1) {
            finish()
            return
        }
        val root = FrameLayout(this)
        root.setBackgroundColor(0x55000000)

        val (wDp, hDp) = MenuPanel.widgetSizeDp(this, widgetId)
        val markerW = root.dp(wDp)
        val markerH = root.dp(hDp)
        val marker = View(this).apply {
            background = GradientDrawable().apply {
                setColor(0x22FFFFFF)
                setStroke(root.dp(2), 0xFFFFFFFF.toInt())
                cornerRadius = root.dp(14).toFloat()
            }
        }
        root.addView(marker, FrameLayout.LayoutParams(markerW, markerH))
        val panel = MenuPanel(this, root, widgetId, interactive = false)

        fun update() {
            marker.x = cx - markerW / 2f
            marker.y = cy - markerH / 2f
            panel.place(RectF(cx - markerW / 2f, cy - markerH / 2f, cx + markerW / 2f, cy + markerH / 2f), animate = false)
        }

        root.setOnTouchListener { _, e ->
            if (e.actionMasked == MotionEvent.ACTION_DOWN || e.actionMasked == MotionEvent.ACTION_MOVE) {
                cx = e.rawX
                cy = e.rawY
                update()
            }
            true
        }

        val hint = TextView(this).apply {
            text = "Trascina il riquadro sul widget: il menu mostra dove si aprirà"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(root.dp(24), root.dp(12), root.dp(24), root.dp(12))
        }
        root.addView(hint, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP,
        ))

        val step = root.dp(2).toFloat()
        fun nudge(dx: Float, dy: Float) {
            cx += dx
            cy += dy
            update()
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xCC000000.toInt())
            setPadding(root.dp(12), root.dp(8), root.dp(12), root.dp(8))
            addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER
                addView(arrowButton("◀") { nudge(-step, 0f) })
                addView(arrowButton("▲") { nudge(0f, -step) })
                addView(arrowButton("▼") { nudge(0f, step) })
                addView(arrowButton("▶") { nudge(step, 0f) })
            })
            addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER
                addView(Button(context).apply {
                    text = "Annulla"
                    setOnClickListener { finish() }
                })
                addView(Button(context).apply {
                    text = "Conferma"
                    setOnClickListener {
                        Store.savePosition(this@PositionActivity, widgetId, cx, cy)
                        finish()
                    }
                })
            })
        }
        root.addView(bar, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM,
        ))
        setContentView(root)

        root.doOnLayout {
            // Lascia spazio alle barre di sistema (schermata a tutto schermo)
            val bars = root.rootWindowInsets?.getInsets(WindowInsets.Type.systemBars())
            hint.setPadding(root.dp(24), (bars?.top ?: 0) + root.dp(12), root.dp(24), root.dp(12))
            bar.setPadding(root.dp(12), root.dp(8), root.dp(12), (bars?.bottom ?: 0) + root.dp(8))
            // Parte dall'ultimo punto indicato, altrimenti dal centro dello schermo
            val saved = Store.position(this, widgetId)
            cx = saved?.first ?: (root.width / 2f)
            cy = saved?.second ?: (root.height / 2f)
            update()
        }
    }

    private fun arrowButton(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        minWidth = 0
        minimumWidth = dp(56)
        setOnClickListener { onClick() }
    }
}
