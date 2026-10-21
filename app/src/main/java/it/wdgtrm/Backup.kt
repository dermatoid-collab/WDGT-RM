package it.wdgtrm

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Base64
import it.wdgtrm.widgets.LauncherWidgetReceiver
import org.json.JSONArray
import org.json.JSONObject

/**
 * Backup su file delle istanze del widget e dell'elenco generale. Le istanze sul desktop
 * si perdono con la disinstallazione: dopo averle ricreate si applicano i dati salvati.
 */
object Backup {
    private const val VERSION = 1

    class Profile(val cfg: Store.WidgetCfg, val apps: List<String>, val image: ByteArray?)

    /** Contenuto JSON del backup e numero di istanze incluse. */
    fun export(c: Context): Pair<String, Int> {
        val ids = AppWidgetManager.getInstance(c)
            .getAppWidgetIds(ComponentName(c, LauncherWidgetReceiver::class.java))
        val widgets = JSONArray()
        ids.forEach { id ->
            val cfg = Store.config(c, id) ?: return@forEach
            widgets.put(toJson(c, id, cfg))
        }
        val root = JSONObject()
            .put("version", VERSION)
            .put("generalApps", JSONArray(Store.apps(c)))
            .put("widgets", widgets)
        return root.toString(2) to widgets.length()
    }

    /** Legge un file di backup: ripristina l'elenco generale e conserva le istanze da applicare. */
    fun importJson(c: Context, json: String): Int {
        val root = JSONObject(json)
        root.optJSONArray("generalApps")?.let { a ->
            Store.setApps(c, (0 until a.length()).map { a.getString(it) })
        }
        val widgets = root.optJSONArray("widgets") ?: JSONArray()
        Store.setProfilesJson(c, widgets.toString())
        return widgets.length()
    }

    /** Istanze salvate nell'ultimo backup importato. */
    fun profiles(c: Context): List<Profile> = try {
        val a = JSONArray(Store.profilesJson(c))
        (0 until a.length()).map { fromJson(a.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }

    /** Copia le impostazioni di un'istanza salvata su un widget esistente. */
    fun applyProfile(c: Context, widgetId: Int, p: Profile) {
        Store.setWidgetApps(c, widgetId, p.apps)
        Store.saveConfig(c, widgetId, p.cfg)
        val file = Store.imageFile(c, widgetId)
        if (p.image != null) file.writeBytes(p.image) else file.delete()
    }

    private fun toJson(c: Context, id: Int, cfg: Store.WidgetCfg): JSONObject {
        val o = JSONObject()
            .put("pkg", cfg.pkg)
            .put("name", cfg.name)
            .put("iconMode", cfg.iconMode)
            .put("emoji", cfg.emoji)
            .put("symbol", cfg.symbol)
            .put("iconColor", cfg.iconColor)
            .put("iconSize", cfg.iconSize)
            .put("bgShape", cfg.bgShape)
            .put("bgColor", cfg.bgColor)
            .put("bgAlpha", cfg.bgAlpha)
            .put("menuColor", cfg.menuColor)
            .put("menuAlpha", cfg.menuAlpha)
            .put("menuColumns", cfg.menuColumns)
            .put("apps", JSONArray(Store.widgetApps(c, id) ?: Store.apps(c)))
        val img = Store.imageFile(c, id)
        if (img.exists()) o.put("image", Base64.encodeToString(img.readBytes(), Base64.NO_WRAP))
        return o
    }

    private fun fromJson(o: JSONObject): Profile {
        val d = Store.WidgetCfg("", "", Store.ICON_SET, "")
        val cfg = Store.WidgetCfg(
            pkg = o.optString("pkg", ""),
            name = o.optString("name", ""),
            iconMode = o.optInt("iconMode", Store.ICON_SET),
            emoji = o.optString("emoji", ""),
            symbol = o.optString("symbol", Symbols.DEFAULT),
            iconColor = o.optInt("iconColor", d.iconColor),
            iconSize = o.optInt("iconSize", d.iconSize),
            bgShape = o.optString("bgShape", d.bgShape),
            bgColor = o.optInt("bgColor", d.bgColor),
            bgAlpha = o.optInt("bgAlpha", d.bgAlpha),
            menuColor = o.optInt("menuColor", d.menuColor),
            menuAlpha = o.optInt("menuAlpha", d.menuAlpha),
            menuColumns = o.optInt("menuColumns", d.menuColumns),
        )
        val apps = o.optJSONArray("apps")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
        val image = o.optString("image", "").takeIf { it.isNotEmpty() }?.let { Base64.decode(it, Base64.NO_WRAP) }
        return Profile(cfg, apps, image)
    }
}
