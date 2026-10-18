package it.wdgtrm

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** Impostazioni persistenti: elenco globale delle app e configurazione di ogni widget. */
object Store {
    const val ICON_APP = 0
    const val ICON_EMOJI = 1
    const val ICON_IMAGE = 2
    const val ICON_SET = 3

    const val TONE_WHITE = 0
    const val TONE_BLACK = 1

    data class WidgetCfg(
        val pkg: String,
        val name: String,
        val iconMode: Int,
        val emoji: String,
        val symbol: String = Symbols.DEFAULT,
        val tone: Int = TONE_WHITE,
    )

    private fun prefs(c: Context) = c.getSharedPreferences("wdgt", Context.MODE_PRIVATE)

    /** Pacchetti dell'elenco, nell'ordine scelto dall'utente. */
    fun apps(c: Context): List<String> =
        prefs(c).getString("apps", "")!!.split(",").filter { it.isNotBlank() }

    fun setApps(c: Context, list: List<String>) {
        prefs(c).edit().putString("apps", list.distinct().joinToString(",")).apply()
        Shortcuts.publish(c)
    }

    fun config(c: Context, widgetId: Int): WidgetCfg? {
        val p = prefs(c)
        val pkg = p.getString("w${widgetId}_pkg", null) ?: return null
        return WidgetCfg(
            pkg = pkg,
            name = p.getString("w${widgetId}_name", "") ?: "",
            iconMode = p.getInt("w${widgetId}_mode", ICON_APP),
            emoji = p.getString("w${widgetId}_emoji", "") ?: "",
            symbol = p.getString("w${widgetId}_symbol", Symbols.DEFAULT) ?: Symbols.DEFAULT,
            tone = p.getInt("w${widgetId}_tone", TONE_WHITE),
        )
    }

    fun saveConfig(c: Context, widgetId: Int, cfg: WidgetCfg) {
        prefs(c).edit()
            .putString("w${widgetId}_pkg", cfg.pkg)
            .putString("w${widgetId}_name", cfg.name)
            .putInt("w${widgetId}_mode", cfg.iconMode)
            .putString("w${widgetId}_emoji", cfg.emoji)
            .putString("w${widgetId}_symbol", cfg.symbol)
            .putInt("w${widgetId}_tone", cfg.tone)
            .apply()
    }

    /** Centro del widget sullo schermo, indicato dall'utente (One UI non lo comunica). */
    fun position(c: Context, widgetId: Int): Pair<Float, Float>? {
        val p = prefs(c)
        if (!p.contains("w${widgetId}_px")) return null
        return p.getFloat("w${widgetId}_px", 0f) to p.getFloat("w${widgetId}_py", 0f)
    }

    fun savePosition(c: Context, widgetId: Int, x: Float, y: Float) {
        prefs(c).edit().putFloat("w${widgetId}_px", x).putFloat("w${widgetId}_py", y).apply()
    }

    fun delete(c: Context, widgetId: Int) {
        prefs(c).edit()
            .remove("w${widgetId}_pkg").remove("w${widgetId}_name")
            .remove("w${widgetId}_mode").remove("w${widgetId}_emoji")
            .remove("w${widgetId}_symbol").remove("w${widgetId}_tone")
            .remove("w${widgetId}_px").remove("w${widgetId}_py")
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
