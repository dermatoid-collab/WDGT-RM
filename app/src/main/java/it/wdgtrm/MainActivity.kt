package it.wdgtrm

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Elenco generale delle app, base per i nuovi widget, scorciatoie e backup su file. */
class MainActivity : Activity() {
    private lateinit var editor: AppListEditor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.padForSystemBars(root.dp(20))
        root.addTitle("WDGT-RM · Launcher")
        root.addView(TextView(this).apply {
            text = "Aggiungi un widget «Launcher» alla Home: toccandolo si apre un menu e scegli " +
                "ogni volta quale app lanciare. Ogni widget ha il suo elenco di app, il suo nome, " +
                "la sua icona e il suo sfondo: tenendolo premuto → Impostazioni li modifichi.\n\n" +
                "Qui sotto c'è l'elenco generale: i nuovi widget partono con una copia di questo " +
                "elenco (poi ognuno è indipendente), e le prime app compaiono come scorciatoie " +
                "tenendo premuta l'icona di WDGT-RM (fino al limite di Android, di solito 4-5)."
        })
        root.addLabel("Elenco generale")
        editor = AppListEditor(this, { Store.apps(this) }, { Store.setApps(this, it) })
        root.addView(editor.view)

        root.addLabel("Backup")
        root.addView(TextView(this).apply {
            text = "Salva in un file l'elenco generale e tutte le istanze del widget in Home " +
                "(app, nome, icona, colori, sfondo). Dopo una reinstallazione importa il file e, " +
                "nelle impostazioni di ogni nuovo widget, usa «Carica da backup»."
        })
        root.addButton("Esporta backup…") {
            startActivityForResult(
                Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/json")
                    .putExtra(Intent.EXTRA_TITLE, "wdgt-rm-backup.json"),
                REQ_EXPORT,
            )
        }
        root.addButton("Importa backup…") {
            startActivityForResult(
                Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),
                REQ_IMPORT,
            )
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        Shortcuts.publish(this)
        editor.render()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data ?: return
        if (resultCode != RESULT_OK) return
        try {
            when (requestCode) {
                REQ_EXPORT -> {
                    val (json, count) = Backup.export(this)
                    contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                    toast("Backup salvato: $count ${if (count == 1) "istanza" else "istanze"}")
                }
                REQ_IMPORT -> {
                    val json = contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: return
                    val count = Backup.importJson(this, json)
                    editor.render()
                    toast("Importate $count istanze: ora aggiungi i widget e usa «Carica da backup»")
                }
            }
        } catch (e: Exception) {
            toast("Operazione non riuscita: file non valido o non accessibile")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    companion object {
        private const val REQ_EXPORT = 10
        private const val REQ_IMPORT = 11
    }
}
