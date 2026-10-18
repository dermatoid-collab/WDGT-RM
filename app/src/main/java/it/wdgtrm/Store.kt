package it.wdgtrm

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** Impostazioni persistenti: elenco globale delle app e configurazione di ogni widget. */
object Store {
    const val ICON_APP = 0
    const val ICON_EMOJI = 1
    const val ICON_IMAGE = 2

    data class WidgetCfg(val pkg: String, val name: String, val iconMode: Int, val emoji: String)

    private fun prefs(c: Context) = c.getSharedPreferences("wdgt", Context.MODE_PRIVATE)

    /** Pacchetti dell'elenco, nell'ordine scelto dall'utente. */
    fun apps(c: Context): List<String> =
        prefs(c).getString("apps", "")!!.split(",").filter { it.isNotBlank() }

    fun setApps(c: Context, list: List<String>) {
        prefs(c).edit().putString("apps", list.distinct().joinToString(",")).apply()
    }

    fun config(c: Context, widgetId: Int): WidgetCfg? {
        val p = prefs(c)
        val pkg = p.getString("w${widgetId}_pkg", null) ?: return null
        return WidgetCfg(
            pkg = pkg,
            name = p.getString("w${widgetId}_name", "") ?: "",
            iconMode = p.getInt("w${widgetId}_mode", ICON_APP),
            emoji = p.getString("w${widgetId}_emoji", "") ?: "",
        )
    }

    fun saveConfig(c: Context, widgetId: Int, cfg: WidgetCfg) {
        prefs(c).edit()
            .putString("w${widgetId}_pkg", cfg.pkg)
            .putString("w${widgetId}_name", cfg.name)
            .putInt("w${widgetId}_mode", cfg.iconMode)
            .putString("w${widgetId}_emoji", cfg.emoji)
            .apply()
    }

    fun delete(c: Context, widgetId: Int) {
        prefs(c).edit()
            .remove("w${widgetId}_pkg").remove("w${widgetId}_name")
            .remove("w${widgetId}_mode").remove("w${widgetId}_emoji")
            .apply()
        imageFile(c, widgetId).delete()
    }

    fun imageFile(c: Context, widgetId: Int) = java.io.File(c.filesDir, "icon_$widgetId.png")
}

object Apps {
    data class Item(val pkg: String, val label: String)

    fun launchable(c: Context): List<Item> {
        val pm = c.packageManager
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(q, 0)
            .map { Item(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

    /** Nome dell'app, o null se non più installata. */
    fun label(c: Context, pkg: String): String? = try {
        val pm = c.packageManager
        pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
