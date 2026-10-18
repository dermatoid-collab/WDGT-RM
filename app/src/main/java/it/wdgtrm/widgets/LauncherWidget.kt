package it.wdgtrm.widgets

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import it.wdgtrm.Store

/** Launcher 1x1: un tocco apre l'app scelta; pressione prolungata → impostazioni. */
class LauncherWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val cfg = Store.config(context, widgetId)
        val pm = context.packageManager

        val launch = cfg?.let {
            pm.getLaunchIntentForPackage(it.pkg)
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${it.pkg}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val iconBitmap: Bitmap? = when (cfg?.iconMode) {
            Store.ICON_IMAGE -> BitmapFactory.decodeFile(Store.imageFile(context, widgetId).path)
            Store.ICON_EMOJI -> null
            else -> try { pm.getApplicationIcon(cfg?.pkg ?: "").toBitmap(128, 128) } catch (e: Exception) { null }
        }
        val emoji = cfg?.emoji?.takeIf { cfg.iconMode == Store.ICON_EMOJI && it.isNotEmpty() } ?: "🛒"

        provideContent {
            GlanceTheme {
                val base = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.widgetBackground)
                    .cornerRadius(20.dp)
                    .padding(4.dp)
                Column(
                    modifier = if (launch != null) base.clickable(actionStartActivity(launch)) else base,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (cfg == null) {
                        Text("Tieni premuto → Impostazioni", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp))
                    } else {
                        if (iconBitmap != null) {
                            Image(ImageProvider(iconBitmap), cfg.name, GlanceModifier.size(40.dp))
                        } else {
                            Text(emoji, style = TextStyle(fontSize = 30.sp))
                        }
                        if (cfg.name.isNotEmpty()) {
                            Text(
                                cfg.name,
                                maxLines = 1,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                        }
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
