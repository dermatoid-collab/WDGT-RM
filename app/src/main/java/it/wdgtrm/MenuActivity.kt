package it.wdgtrm

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.graphics.RectF
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.core.view.doOnLayout

/**
 * Menu tradizionale aperto dal tocco sul widget: colonna di sole icone, sfondo opaco,
 * larghezza minima. Compare sopra il widget (sotto, se sopra non c'è spazio).
 * Un tocco su un'icona lancia l'app; un tocco fuori chiude il menu.
 */
class MenuActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apps = Store.apps(this).mapNotNull { pkg -> Apps.label(this, pkg)?.let { pkg to it } }
        val widgetId = intent?.getIntExtra(EXTRA_WIDGET_ID, -1) ?: -1
        val cfg = Store.config(this, widgetId)
        val root = FrameLayout(this)
        root.setOnClickListener { finish() }
        setContentView(root)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = menuBackground(cfg)
            isClickable = true // un tocco sul bordo della scheda non chiude il menu
            // L'ombra con uno sfondo semitrasparente sporcherebbe il colore
            elevation = if ((cfg?.menuAlpha ?: 100) >= 100) root.dp(8).toFloat() else 0f
        }
        val items = mutableListOf<View>()
        if (apps.isEmpty()) {
            card.setPadding(root.dp(16), root.dp(16), root.dp(16), root.dp(8))
            card.addView(TextView(this).apply { text = "Nessuna app nell'elenco." })
            card.addButton("Apri impostazioni") {
                startActivity(Intent(this@MenuActivity, MainActivity::class.java))
                finish()
            }
        } else {
            card.setPadding(root.dp(4), root.dp(4), root.dp(4), root.dp(4))
            apps.forEach { (pkg, label) ->
                items += ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(root.dp(48), root.dp(48))
                    setPadding(root.dp(6), root.dp(6), root.dp(6), root.dp(6))
                    try { setImageDrawable(packageManager.getApplicationIcon(pkg)) } catch (e: Exception) { }
                    contentDescription = label
                    setOnClickListener { launch(pkg) }
                }
            }
            items.forEach { card.addView(it) }
        }
        root.addView(card, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
        ))
        root.doOnLayout { place(root, card, items) }
    }

    private fun place(root: FrameLayout, card: LinearLayout, items: List<View>) {
        card.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val w = card.measuredWidth
        val h = card.measuredHeight
        val gap = root.dp(8).toFloat()
        val edge = root.dp(8).toFloat()
        val bars = root.rootWindowInsets?.getInsets(WindowInsets.Type.systemBars())
        val minY = (bars?.top ?: 0) + edge
        val maxY = root.height - (bars?.bottom ?: 0) - h - edge

        val b = widgetBounds(root)
        val x = clamp((b?.centerX() ?: (root.width / 2f)) - w / 2f, edge, root.width - w - edge)
        var above = false
        val y = if (b != null) {
            val aboveY = b.top - h - gap
            if (aboveY >= minY) {
                above = true
                aboveY
            } else {
                clamp(b.bottom + gap, minY, maxY)
            }
        } else {
            clamp((root.height - h) / 2f, minY, maxY)
        }

        // Sopra il widget la prima app sta in basso, vicino al pollice
        if (above && items.isNotEmpty()) {
            card.removeAllViews()
            items.asReversed().forEach { card.addView(it) }
        }
        card.x = x
        card.y = y
        card.pivotX = w / 2f
        card.pivotY = if (above) h.toFloat() else 0f
        card.alpha = 0f
        card.scaleX = 0.85f
        card.scaleY = 0.85f
        card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(120).start()
    }

    /**
     * Dove si trova il widget: prima il dato del launcher (se lo fornisce), poi il punto
     * indicato dall'utente nelle impostazioni. Senza nessuno dei due: centro schermo.
     */
    private fun widgetBounds(root: View): RectF? {
        intent?.sourceBounds?.let { return RectF(it) }
        val widgetId = intent?.getIntExtra(EXTRA_WIDGET_ID, -1) ?: -1
        val (px, py) = Store.position(this, widgetId) ?: return null
        val heightDp = try {
            AppWidgetManager.getInstance(this).getAppWidgetOptions(widgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        } catch (e: Exception) {
            0
        }.takeIf { it > 0 } ?: 72
        val half = root.dp(heightDp) / 2f
        return RectF(px - half, py - half, px + half, py + half)
    }

    /** Sfondo del menu: colore scelto (o del tema) con l'opacità scelta. */
    private fun menuBackground(cfg: Store.WidgetCfg?): GradientDrawable {
        val tv = TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, tv, true)
        val base = if (cfg == null || cfg.menuColor == 0) tv.data else cfg.menuColor
        val alpha = (cfg?.menuAlpha ?: 100) * 255 / 100
        return GradientDrawable().apply {
            setColor(ColorUtils.setAlphaComponent(base, alpha))
            cornerRadius = resources.displayMetrics.density * 16
        }
    }

    private fun clamp(v: Float, lo: Float, hi: Float) = if (lo > hi) (lo + hi) / 2 else v.coerceIn(lo, hi)

    private fun launch(pkg: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            Toast.makeText(this, "App non disponibile", Toast.LENGTH_SHORT).show()
        } else {
            startActivity(intent)
        }
        finish()
    }

    companion object {
        const val EXTRA_WIDGET_ID = "widgetId"
    }
}
