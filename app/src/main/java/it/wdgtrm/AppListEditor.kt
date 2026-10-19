package it.wdgtrm

import android.app.Activity
import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Editor dell'elenco di app (aggiungi, rimuovi, riordina), riusato dall'elenco generale
 * e dall'elenco di ogni singolo widget. [load] e [save] decidono dove vive l'elenco.
 */
class AppListEditor(
    private val activity: Activity,
    private val load: () -> List<String>,
    private val save: (List<String>) -> Unit,
    private val onChanged: () -> Unit = {},
) {
    private val list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }

    val view: View = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        addView(list)
        addButton("+ Aggiungi app") { showAddDialog() }
    }

    init {
        render()
    }

    fun render() {
        list.removeAllViews()
        val pkgs = load()
        if (pkgs.isEmpty()) {
            list.addView(TextView(activity).apply { text = "Nessuna app. Premi «Aggiungi app»." })
            return
        }
        val pm = activity.packageManager
        pkgs.forEachIndexed { i, pkg ->
            val label = Apps.label(activity, pkg)
            val row = list.horizontalRow()
            row.addView(ImageView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(list.dp(40), list.dp(40))
                try { setImageDrawable(pm.getApplicationIcon(pkg)) } catch (e: Exception) { }
            })
            row.addView(TextView(activity).apply {
                text = label ?: "$pkg (non installata)"
                setPadding(list.dp(12), 0, list.dp(8), 0)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(smallButton("▲", i > 0) { move(pkgs, i, i - 1) })
            row.addView(smallButton("▼", i < pkgs.lastIndex) { move(pkgs, i, i + 1) })
            row.addView(smallButton("✕", true) { update(pkgs.filterIndexed { j, _ -> j != i }) })
            list.addView(row)
        }
    }

    private fun update(newList: List<String>) {
        save(newList)
        render()
        onChanged()
    }

    private fun smallButton(text: String, enabled: Boolean, onClick: () -> Unit) = Button(activity).apply {
        this.text = text
        isEnabled = enabled
        minWidth = 0
        minimumWidth = list.dp(48)
        setOnClickListener { onClick() }
    }

    private fun move(pkgs: List<String>, from: Int, to: Int) {
        val m = pkgs.toMutableList()
        m.add(to, m.removeAt(from))
        update(m)
    }

    private fun showAddDialog() {
        val current = load()
        val all = Apps.launchable(activity).filter { it.pkg !in current && it.pkg != activity.packageName }
        if (all.isEmpty()) return
        val checked = BooleanArray(all.size)
        AlertDialog.Builder(activity)
            .setTitle("Aggiungi app")
            .setMultiChoiceItems(all.map { it.label }.toTypedArray(), checked) { _, i, c -> checked[i] = c }
            .setPositiveButton("Aggiungi") { _, _ ->
                update(current + all.filterIndexed { i, _ -> checked[i] }.map { it.pkg })
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
}
