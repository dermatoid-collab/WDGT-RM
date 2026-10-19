package it.wdgtrm

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Elenco generale delle app: base per i nuovi widget e scorciatoie dell'icona WDGT-RM. */
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
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        Shortcuts.publish(this)
        editor.render()
    }
}
