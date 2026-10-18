package it.wdgtrm.widgets

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import it.wdgtrm.BgShapes
import it.wdgtrm.MenuActivity
import it.wdgtrm.Store
import it.wdgtrm.Symbols

/**
 * Launcher 1x1: tessera a forma di icona (opzionale) con simbolo, nome sotto.
 * Il tocco apre il menu delle app (o l'app fissa); pressione prolungata → impostazioni.
 */
class LauncherWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val cfg = Store.config(context, widgetId)
        val pm = context.packageManager

        // pkg vuoto = modalità menu: il tocco apre l'elenco e si sceglie ogni volta
        val launch = cfg?.let {
            if (it.pkg.isEmpty()) {
                // data distingue i PendingIntent delle varie istanze (gli extra non bastano)
                Intent(context, MenuActivity::class.java)
                    .setData(Uri.parse("wdgt://menu/$widgetId"))
                    .putExtra(MenuActivity.EXTRA_WIDGET_ID, widgetId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                pm.getLaunchIntentForPackage(it.pkg)
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${it.pkg}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        // Simbolo monocromatico (set di icone, o predefinito quando manca un'app da mostrare)
        val symbolRes: Int? = cfg?.let {
            val useSymbol = it.iconMode == Store.ICON_SET || (it.iconMode == Store.ICON_APP && it.pkg.isEmpty())
            if (useSymbol) Symbols.res(it.symbol) else null
        }
        val iconBitmap: Bitmap? = when (cfg?.iconMode) {
            Store.ICON_IMAGE -> BitmapFactory.decodeFile(Store.imageFile(context, widgetId).path)
            Store.ICON_APP -> try { pm.getApplicationIcon(cfg?.pkg.orEmpty()).toBitmap(192, 192) } catch (e: Exception) { null }
            else -> null
        }
        val emoji = cfg?.emoji?.takeIf { cfg.iconMode == Store.ICON_EMOJI && it.isNotEmpty() } ?: "▦"

        val iconColor = ColorProvider(Color(cfg?.iconColor ?: 0xFFFFFFFF.toInt()))
        val shapeRes = cfg?.let { BgShapes.res(it.bgShape) }
        // Colore dello sfondo con l'opacità scelta (il tint rispetta il canale alpha)
        val bgTint = cfg?.let { ColorProvider(Color(it.bgColor).copy(alpha = it.bgAlpha / 100f)) }
        val tile = (cfg?.iconSize ?: 60).dp
        // Con la tessera il simbolo sta dentro, come nelle icone One UI
        val glyphDp = (cfg?.iconSize ?: 60) * if (shapeRes != null) 0.6f else 1f
        val glyph = glyphDp.dp

        provideContent {
            val base = GlanceModifier.fillMaxSize().padding(4.dp)
            Column(
                modifier = if (launch != null) base.clickable(actionStartActivity(launch)) else base,
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (cfg == null) {
                    Text("Tieni premuto → Impostazioni", style = TextStyle(color = iconColor, fontSize = 10.sp))
                } else {
                    Box(modifier = GlanceModifier.size(tile), contentAlignment = Alignment.Center) {
                        if (shapeRes != null && bgTint != null) {
                            Image(
                                provider = ImageProvider(shapeRes),
                                contentDescription = null,
                                modifier = GlanceModifier.fillMaxSize(),
                                colorFilter = ColorFilter.tint(bgTint),
                            )
                        }
                        when {
                            symbolRes != null -> Image(
                                provider = ImageProvider(symbolRes),
                                contentDescription = cfg.name,
                                modifier = GlanceModifier.size(glyph),
                                colorFilter = ColorFilter.tint(iconColor),
                            )
                            iconBitmap != null -> Image(
                                provider = ImageProvider(iconBitmap),
                                contentDescription = cfg.name,
                                modifier = GlanceModifier.size(glyph),
                            )
                            else -> Text(emoji, style = TextStyle(fontSize = (glyphDp * 0.8f).sp, color = iconColor))
                        }
                    }
                    if (cfg.name.isNotEmpty()) {
                        Text(
                            cfg.name,
                            maxLines = 1,
                            style = TextStyle(color = iconColor, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        )
                    }
                }
            }
        }
    }
}

class LauncherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = LauncherWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { Store.delete(context, it) }
    }
}
