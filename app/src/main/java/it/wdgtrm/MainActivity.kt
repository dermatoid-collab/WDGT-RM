package it.wdgtrm

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Impostazioni globali: elenco delle app disponibili ai widget, con riordino. */
class MainActivity : Activity() {
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.padForSystemBars(root.dp(20))
        root.addTitle("WDGT-RM · Launcher")
        root.addView(TextView(this).apply {
            text = "Definisci qui l'elenco delle app e il loro ordine. " +
                "Poi aggiungi un widget «Launcher» alla Home: toccandolo si apre un menu con " +
                "queste app e scegli ogni volta quale lanciare. Tenendolo premuto → Impostazioni " +
                "scegli nome e icona, oppure una singola app fissa."
        })
        root.addLabel("App nell'elenco")
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        root.addButton("+ Aggiungi app") { showAddDialog() }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        list.removeAllViews()
        val pkgs = Store.apps(this)
        if (pkgs.isEmpty()) {
            list.addView(TextView(this).apply { text = "Nessuna app. Premi «Aggiungi app»." })
            return
        }
        val pm = packageManager
        pkgs.forEachIndexed { i, pkg ->
            val label = Apps.label(this, pkg)
            val row = list.horizontalRow()
            row.addView(ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
                try { setImageDrawable(pm.getApplicationIcon(pkg)) } catch (e: Exception) { }
            })
            row.addView(TextView(this).apply {
                text = label ?: "$pkg (non installata)"
                setPadding(dp(12), 0, dp(8), 0)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(smallButton("▲", i > 0) { move(pkgs, i, i - 1) })
            row.addView(smallButton("▼", i < pkgs.lastIndex) { move(pkgs, i, i + 1) })
            row.addView(smallButton("✕", true) {
                Store.setApps(this, pkgs.filterIndexed { j, _ -> j != i })
                render()
            })
            list.addView(row)
        }
    }

    private fun smallButton(text: String, enabled: Boolean, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isEnabled = enabled
        minWidth = 0
        minimumWidth = dp(48)
        setOnClickListener { onClick() }
    }

    private fun move(pkgs: List<String>, from: Int, to: Int) {
        val m = pkgs.toMutableList()
        m.add(to, m.removeAt(from))
        Store.setApps(this, m)
        render()
    }

    private fun showAddDialog() {
        val current = Store.apps(this)
        val all = Apps.launchable(this).filter { it.pkg !in current && it.pkg != packageName }
        if (all.isEmpty()) return
        val checked = BooleanArray(all.size)
        AlertDialog.Builder(this)
            .setTitle("Aggiungi app")
            .setMultiChoiceItems(all.map { it.label }.toTypedArray(), checked) { _, i, c -> checked[i] = c }
            .setPositiveButton("Aggiungi") { _, _ ->
                val added = all.filterIndexed { i, _ -> checked[i] }.map { it.pkg }
                Store.setApps(this, current + added)
                render()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
}
