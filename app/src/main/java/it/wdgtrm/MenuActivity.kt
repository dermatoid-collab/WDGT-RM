package it.wdgtrm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

/** Menu a comparsa aperto dal tocco sul widget: sceglie ogni volta quale app lanciare. */
class MenuActivity : Activity() {
    private data class Row(val pkg: String, val label: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rows = Store.apps(this).mapNotNull { pkg ->
            Apps.label(this, pkg)?.let { Row(pkg, it) }
        }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.setPadding(root.dp(8), root.dp(16), root.dp(8), root.dp(8))
        root.addView(TextView(this).apply {
            text = "Apri con…"
            textSize = 18f
            setPadding(dp(16), 0, dp(16), dp(8))
        })
        if (rows.isEmpty()) {
            root.addView(TextView(this).apply {
                text = "Nessuna app nell'elenco."
                setPadding(dp(16), dp(8), dp(16), dp(8))
            })
            root.addButton("Apri impostazioni") {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        } else {
            root.addView(ListView(this).apply {
                divider = null
                adapter = RowAdapter(rows)
                setOnItemClickListener { _, _, pos, _ -> launch(rows[pos].pkg) }
            })
        }
        setContentView(root)
    }

    private fun launch(pkg: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            Toast.makeText(this, "App non disponibile", Toast.LENGTH_SHORT).show()
        } else {
            startActivity(intent)
        }
        finish()
    }

    private inner class RowAdapter(private val rows: List<Row>) : BaseAdapter() {
        override fun getCount() = rows.size
        override fun getItem(position: Int) = rows[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val row = rows[position]
            return LinearLayout(this@MenuActivity).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(10), dp(16), dp(10))
                addView(ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
                    try { setImageDrawable(packageManager.getApplicationIcon(row.pkg)) } catch (e: Exception) { }
                })
                addView(TextView(context).apply {
                    text = row.label
                    textSize = 16f
                    setPadding(dp(16), 0, 0, 0)
                })
            }
        }
    }
}
