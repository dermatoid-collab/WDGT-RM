package it.wdgtrm

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils

/**
 * Menu delle app del widget: scheda con le sole icone, in 1-4 colonne o in riga, che si apre
 * sopra il widget (sotto se manca spazio). Lo usano sia il menu vero ([MenuActivity], con
 * [interactive] = true) sia l'anteprima di posizionamento ([PositionActivity]).
 */
class MenuPanel(
    private val activity: Activity,
    private val root: FrameLayout,
    widgetId: Int,
    private val interactive: Boolean,
    private val onLaunch: (String) -> Unit = {},
    private val onOpenSettings: () -> Unit = {},
) {
    private val cfg = Store.config(activity, widgetId)
    private val apps = (Store.widgetApps(activity, widgetId) ?: Store.apps(activity))
        .mapNotNull { pkg -> Apps.label(activity, pkg)?.let { pkg to it } }
    private val card = LinearLayout(activity)
    private val items = mutableListOf<View>()

    init {
        card.orientation = LinearLayout.VERTICAL
        card.background = menuBackground()
        card.isClickable = interactive // in anteprima i tocchi passano al fondo
        // L'ombra con uno sfondo semitrasparente sporcherebbe il colore
        card.elevation = if ((cfg?.menuAlpha ?: 100) >= 100) root.dp(8).toFloat() else 0f
        if (apps.isEmpty()) {
            card.setPadding(root.dp(16), root.dp(16), root.dp(16), root.dp(8))
            card.addView(TextView(activity).apply { text = "Nessuna app nell'elenco." })
            if (interactive) card.addButton("Apri impostazioni") { onOpenSettings() }
        } else {
            card.setPadding(root.dp(4), root.dp(4), root.dp(4), root.dp(4))
            apps.forEach { (pkg, label) ->
                items += ImageView(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(root.dp(48), root.dp(48))
                    setPadding(root.dp(6), root.dp(6), root.dp(6), root.dp(6))
                    try { setImageDrawable(activity.packageManager.getApplicationIcon(pkg)) } catch (e: Exception) { }
                    contentDescription = label
                    if (interactive) setOnClickListener { onLaunch(pkg) }
                }
            }
            fillRows(above = false)
        }
        root.addView(card, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
        ))
    }

    /** Dispone la scheda attorno al widget [widget] (null = centro schermo). */
    fun place(widget: RectF?, animate: Boolean) {
        fillRows(above = false)
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

        val x = clamp((widget?.centerX() ?: (root.width / 2f)) - w / 2f, edge, root.width - w - edge)
        var above = false
        val y = if (widget != null) {
            val aboveY = widget.top - h - gap
            if (aboveY >= minY) {
                above = true
                aboveY
            } else {
                clamp(widget.bottom + gap, minY, maxY)
            }
        } else {
            clamp((root.height - h) / 2f, minY, maxY)
        }
        // Sopra il widget la prima riga sta in basso, vicino al pollice
        if (above) fillRows(above = true)
        card.x = x
        card.y = y
        card.pivotX = w / 2f
        card.pivotY = if (above) h.toFloat() else 0f
        if (animate) {
            card.alpha = 0f
            card.scaleX = 0.85f
            card.scaleY = 0.85f
            card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(120).start()
        } else {
            card.alpha = 1f
            card.scaleX = 1f
            card.scaleY = 1f
        }
    }

    private fun fillRows(above: Boolean) {
        if (items.isEmpty()) return
        items.forEach { (it.parent as? ViewGroup)?.removeView(it) }
        card.removeAllViews()
        val perRow = columnsPerRow()
        var rows = items.chunked(perRow)
        // Solo in colonna singola, sopra il widget, la prima app sta in basso (vicino al pollice);
        // con più colonne si legge sempre dall'alto in basso e da sinistra a destra
        if (above && perRow == 1) rows = rows.asReversed()
        rows.forEach { row ->
            card.addView(LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                row.forEach { addView(it) }
            })
        }
    }

    /** 1-4 colonne; 0 = riga orizzontale (quante icone entrano nella larghezza dello schermo). */
    private fun columnsPerRow(): Int {
        val c = cfg?.menuColumns ?: 1
        if (c in 1..4) return c
        val fit = ((root.width - root.dp(16)) / root.dp(48)).coerceAtLeast(1)
        return minOf(items.size, fit)
    }

    /** Sfondo del menu: colore scelto (o del tema) con l'opacità scelta. */
    private fun menuBackground(): GradientDrawable {
        val tv = TypedValue()
        activity.theme.resolveAttribute(android.R.attr.colorBackground, tv, true)
        val base = if (cfg == null || cfg.menuColor == 0) tv.data else cfg.menuColor
        val alpha = (cfg?.menuAlpha ?: 100) * 255 / 100
        return GradientDrawable().apply {
            setColor(ColorUtils.setAlphaComponent(base, alpha))
            cornerRadius = activity.resources.displayMetrics.density * 16
        }
    }

    private fun clamp(v: Float, lo: Float, hi: Float) = if (lo > hi) (lo + hi) / 2 else v.coerceIn(lo, hi)

    companion object {
        /** Dimensioni (dp) del widget secondo Android; 72x72 se non disponibili. */
        fun widgetSizeDp(c: Context, widgetId: Int): Pair<Int, Int> = try {
            val o = AppWidgetManager.getInstance(c).getAppWidgetOptions(widgetId)
            val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).takeIf { it > 0 } ?: 72
            val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0).takeIf { it > 0 } ?: 72
            w to h
        } catch (e: Exception) {
            72 to 72
        }
    }
}
