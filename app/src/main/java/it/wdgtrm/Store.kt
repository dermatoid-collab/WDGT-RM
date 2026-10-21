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


    data class WidgetCfg(
        val pkg: String,
        val name: String,
        val iconMode: Int,
        val emoji: String,
        val symbol: String = Symbols.DEFAULT,
        val iconColor: Int = 0xFFFFFFFF.toInt(),
        val iconSize: Int = 60,
        val bgShape: String = BgShapes.NONE,
        val bgColor: Int = 0xFF000000.toInt(),
        val bgAlpha: Int = 70,
        /** 0 = colore del tema (chiaro/scuro). */
        val menuColor: Int = 0,
        val menuAlpha: Int = 100,
    )

    private fun prefs(c: Context) = c.getSharedPreferences("wdgt", Context.MODE_PRIVATE)

    /** Pacchetti dell'elenco, nell'ordine scelto dall'utente. */
    fun apps(c: Context): List<String> =
        prefs(c).getString("apps", "")!!.split(",").filter { it.isNotBlank() }

    fun setApps(c: Context, list: List<String>) {
        prefs(c).edit().putString("apps", list.distinct().joinToString(",")).apply()
        Shortcuts.publish(c)
    }

    /** Istanze dell'ultimo backup importato (JSON), da applicare ai nuovi widget. */
    fun profilesJson(c: Context): String = prefs(c).getString("profiles", "[]") ?: "[]"

    fun setProfilesJson(c: Context, json: String) {
        prefs(c).edit().putString("profiles", json).apply()
    }

    /** Elenco di app del menu di un singolo widget; null = mai personalizzato (vale quello generale). */
    fun widgetApps(c: Context, widgetId: Int): List<String>? {
        val s = prefs(c).getString("w${widgetId}_apps", null) ?: return null
        return s.split(",").filter { it.isNotBlank() }
    }

    fun setWidgetApps(c: Context, widgetId: Int, list: List<String>) {
        prefs(c).edit().putString("w${widgetId}_apps", list.distinct().joinToString(",")).apply()
    }

    fun config(c: Context, widgetId: Int): WidgetCfg? {
        val p = prefs(c)
        val pkg = p.getString("w${widgetId}_pkg", null) ?: return null
        val id = widgetId
        // Compatibilità: prima il colore era solo bianco (0) o nero (1)
        val legacyBlack = p.getInt("w${id}_tone", 0) == 1
        return WidgetCfg(
            pkg = pkg,
            name = p.getString("w${id}_name", "") ?: "",
            iconMode = p.getInt("w${id}_mode", ICON_APP),
            emoji = p.getString("w${id}_emoji", "") ?: "",
            symbol = p.getString("w${id}_symbol", Symbols.DEFAULT) ?: Symbols.DEFAULT,
            iconColor = p.getInt("w${id}_icolor", if (legacyBlack) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()),
            iconSize = p.getInt("w${id}_isize", 60),
            bgShape = p.getString("w${id}_bgshape", BgShapes.NONE) ?: BgShapes.NONE,
            bgColor = p.getInt("w${id}_bgcolor", 0xFF000000.toInt()),
            bgAlpha = p.getInt("w${id}_bgalpha", 70),
            menuColor = p.getInt("w${id}_mcolor", 0),
            menuAlpha = p.getInt("w${id}_malpha", 100),
        )
    }

    fun saveConfig(c: Context, widgetId: Int, cfg: WidgetCfg) {
        val id = widgetId
        prefs(c).edit()
            .putString("w${id}_pkg", cfg.pkg)
            .putString("w${id}_name", cfg.name)
            .putInt("w${id}_mode", cfg.iconMode)
            .putString("w${id}_emoji", cfg.emoji)
            .putString("w${id}_symbol", cfg.symbol)
            .putInt("w${id}_icolor", cfg.iconColor)
            .putInt("w${id}_isize", cfg.iconSize)
            .putString("w${id}_bgshape", cfg.bgShape)
            .putInt("w${id}_bgcolor", cfg.bgColor)
            .putInt("w${id}_bgalpha", cfg.bgAlpha)
            .putInt("w${id}_mcolor", cfg.menuColor)
            .putInt("w${id}_malpha", cfg.menuAlpha)
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
        val keys = listOf(
            "pkg", "name", "mode", "emoji", "symbol", "tone", "icolor", "isize",
            "bgshape", "bgcolor", "bgalpha", "mcolor", "malpha", "px", "py", "apps",
        )
        prefs(c).edit().apply { keys.forEach { remove("w${widgetId}_$it") } }.apply()
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
