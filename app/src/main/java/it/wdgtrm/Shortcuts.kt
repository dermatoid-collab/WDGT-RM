package it.wdgtrm

import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import androidx.core.graphics.drawable.toBitmap

/** Pubblica le prime app dell'elenco come scorciatoie dell'icona WDGT-RM (tocco prolungato). */
object Shortcuts {
    fun publish(c: Context) {
        try {
            val sm = c.getSystemService(ShortcutManager::class.java) ?: return
            val pm = c.packageManager
            val shortcuts = Store.apps(c).mapNotNull { pkg ->
                val intent = pm.getLaunchIntentForPackage(pkg) ?: return@mapNotNull null
                val label = Apps.label(c, pkg) ?: return@mapNotNull null
                Triple(pkg, label, intent)
            }.take(sm.maxShortcutCountPerActivity).mapIndexed { i, (pkg, label, intent) ->
                ShortcutInfo.Builder(c, pkg)
                    .setShortLabel(label)
                    .setLongLabel(label)
                    .setIcon(Icon.createWithBitmap(pm.getApplicationIcon(pkg).toBitmap(128, 128)))
                    .setIntent(intent)
                    .setRank(i)
                    .build()
            }
            sm.setDynamicShortcuts(shortcuts)
        } catch (e: Exception) {
            // le scorciatoie sono un extra: un errore non deve bloccare l'app
        }
    }
}
