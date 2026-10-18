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
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import it.wdgtrm.MenuActivity
import it.wdgtrm.Store
import it.wdgtrm.Symbols

/** Launcher 1x1 minimal, sfondo trasparente: il tocco apre il menu circolare (o l'app fissa). */
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
        val tone = ColorProvider(if (cfg?.tone == Store.TONE_BLACK) Color.Black else Color.White)

        provideContent {
            val base = GlanceModifier.fillMaxSize().padding(4.dp)
            Column(
                modifier = if (launch != null) base.clickable(actionStartActivity(launch)) else base,
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (cfg == null) {
                    Text("Tieni premuto → Impostazioni", style = TextStyle(color = tone, fontSize = 10.sp))
                } else {
                    when {
                        symbolRes != null -> Image(
                            provider = ImageProvider(symbolRes),
                            contentDescription = cfg.name,
                            modifier = GlanceModifier.size(60.dp),
                            colorFilter = ColorFilter.tint(tone),
                        )
                        iconBitmap != null -> Image(
                            provider = ImageProvider(iconBitmap),
                            contentDescription = cfg.name,
                            modifier = GlanceModifier.size(56.dp),
                        )
                        else -> Text(emoji, style = TextStyle(fontSize = 46.sp, color = tone))
                    }
                    if (cfg.name.isNotEmpty()) {
                        Text(
                            cfg.name,
                            maxLines = 1,
                            style = TextStyle(color = tone, fontSize = 12.sp, fontWeight = FontWeight.Medium),
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
