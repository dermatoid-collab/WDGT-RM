package it.wdgtrm

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.glance.appwidget.GlanceAppWidgetManager
import it.wdgtrm.widgets.LauncherWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Impostazioni di un singolo widget: app, nome, icona. Si apre alla creazione e con pressione prolungata. */
class WidgetConfigActivity : Activity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    /** pkgs[0] = "" è la modalità menu; poi i pacchetti dell'elenco. */
    private var pkgs: List<String> = emptyList()
    private var selectedPkg = ""
    private var autoName = ""
    private var pendingImage: Bitmap? = null

    private lateinit var spinner: Spinner
    private lateinit var emptyHint: TextView
    private lateinit var nameField: EditText
    private lateinit var emojiField: EditText
    private lateinit var imageStatus: TextView
    private lateinit var iconGroup: RadioGroup

    private val idApp = View.generateViewId()
    private val idEmoji = View.generateViewId()
    private val idImage = View.generateViewId()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId) ?: widgetId
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val existing = Store.config(this, widgetId)
        selectedPkg = existing?.pkg ?: ""

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.padForSystemBars(root.dp(20))
        root.addTitle("Impostazioni widget")

        root.addLabel("Cosa lancia il widget")
        spinner = Spinner(this)
        root.addView(spinner)
        emptyHint = TextView(this).apply { text = "L'elenco app è vuoto: il menu resterà vuoto finché non ne aggiungi." }
        root.addView(emptyHint)
        root.addButton("Gestisci elenco app") { startActivity(Intent(this, MainActivity::class.java)) }

        root.addLabel("Nome")
        nameField = EditText(this).apply {
            setSingleLine()
            setText(existing?.name.orEmpty())
        }
        root.addView(nameField)

        root.addLabel("Icona")
        iconGroup = RadioGroup(this).apply {
            addView(RadioButton(context).apply { id = idApp; text = "Predefinita (icona dell'app, 🚀 per il menu)" })
            addView(RadioButton(context).apply { id = idEmoji; text = "Emoji" })
            addView(RadioButton(context).apply { id = idImage; text = "Immagine dalla galleria" })
        }
        root.addView(iconGroup)
        emojiField = EditText(this).apply {
            hint = "Emoji, es. 🛒"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(existing?.emoji.orEmpty())
        }
        root.addView(emojiField)
        imageStatus = TextView(this).apply {
            text = if (Store.imageFile(this@WidgetConfigActivity, widgetId).exists()) "Immagine impostata" else "Nessuna immagine"
        }
        root.addView(imageStatus)
        root.addButton("Scegli immagine…") {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), REQ_IMAGE)
        }
        iconGroup.check(
            when (existing?.iconMode) {
                Store.ICON_EMOJI -> idEmoji
                Store.ICON_IMAGE -> idImage
                else -> idApp
            },
        )

        root.addButton("Salva") { save() }
        root.addButton("Annulla") { finish() }
        setContentView(ScrollView(this).apply { addView(root) })

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val pkg = pkgs.getOrNull(pos) ?: return
                selectedPkg = pkg
                val label = if (pkg.isEmpty()) "Launcher" else Apps.label(this@WidgetConfigActivity, pkg) ?: return
                // Propone il nome dell'app finché l'utente non ne ha scritto uno proprio
                val cur = nameField.text.toString()
                if (cur.isBlank() || cur == autoName) nameField.setText(label)
                autoName = label
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    override fun onResume() {
        super.onResume()
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        pkgs = listOf("") + Store.apps(this)
        val labels = pkgs.map {
            if (it.isEmpty()) "Menu: scegli ogni volta" else Apps.label(this, it) ?: "$it (non installata)"
        }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        pkgs.indexOf(selectedPkg).takeIf { it >= 0 }?.let { spinner.setSelection(it) }
        emptyHint.visibility = if (pkgs.size == 1) View.VISIBLE else View.GONE
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_IMAGE || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val bmp = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return
        // Ritaglio quadrato centrale, ridotto per rispettare i limiti dei RemoteViews
        val side = minOf(bmp.width, bmp.height)
        val square = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
        pendingImage = Bitmap.createScaledBitmap(square, 192, 192, true)
        imageStatus.text = "Nuova immagine selezionata"
        iconGroup.check(idImage)
    }

    private fun save() {
        val pkg = pkgs.getOrNull(spinner.selectedItemPosition)
        if (pkg == null) {
            emptyHint.visibility = View.VISIBLE
            return
        }
        val mode = when (iconGroup.checkedRadioButtonId) {
            idEmoji -> Store.ICON_EMOJI
            idImage -> Store.ICON_IMAGE
            else -> Store.ICON_APP
        }
        pendingImage?.let { bmp ->
            Store.imageFile(this, widgetId).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        // Senza immagine disponibile si torna all'icona dell'app
        val finalMode = if (mode == Store.ICON_IMAGE && !Store.imageFile(this, widgetId).exists()) Store.ICON_APP else mode
        Store.saveConfig(
            this, widgetId,
            Store.WidgetCfg(pkg, nameField.text.toString().trim(), finalMode, emojiField.text.toString().trim()),
        )
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(widgetId)
                LauncherWidget().update(this@WidgetConfigActivity, glanceId)
            } catch (e: Exception) {
                // il widget viene comunque aggiornato dal sistema dopo l'aggiunta
            }
            finish()
        }
    }

    companion object {
        private const val REQ_IMAGE = 1
    }
}
