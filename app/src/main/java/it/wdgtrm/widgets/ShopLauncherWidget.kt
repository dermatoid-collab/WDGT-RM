package it.wdgtrm.widgets

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import it.wdgtrm.Config

class ShopLauncherWidget : GlanceAppWidget() {

    private class Entry(val name: String, val icon: Bitmap?, val intent: Intent)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entries = Config.SHOP_APPS.map { resolve(context, it) }
        provideContent {
            GlanceTheme {
                WidgetScaffold(openApp = false) {
                    entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            pair.forEach { Cell(it) }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RowScope.Cell(e: Entry) {
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxSize()
                .padding(4.dp)
                .clickable(actionStartActivity(e.intent)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (e.icon != null) {
                Image(
                    provider = ImageProvider(e.icon),
                    contentDescription = e.name,
                    modifier = GlanceModifier.size(44.dp),
                )
            } else {
                Title("🛒", size = 32)
            }
            Caption(e.name)
        }
    }

    /** Trova l'app installata; se manca, il tocco apre la ricerca sul Play Store. */
    private fun resolve(context: Context, shop: Config.ShopApp): Entry {
        val pm = context.packageManager
        val launchers = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0,
        )
        val info = launchers.firstOrNull { it.activityInfo.packageName == shop.packageHint }
            ?: shop.labelMatch?.let { m ->
                launchers.firstOrNull { it.loadLabel(pm).toString().contains(m, ignoreCase = true) }
            }
        val launch = info?.let { pm.getLaunchIntentForPackage(it.activityInfo.packageName) }
        if (info == null || launch == null) {
            val store = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(shop.storeQuery)}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return Entry(shop.name, null, store)
        }
        return Entry(shop.name, info.loadIcon(pm).toBitmap(128, 128), launch)
    }
}

class ShopLauncherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ShopLauncherWidget()
}
