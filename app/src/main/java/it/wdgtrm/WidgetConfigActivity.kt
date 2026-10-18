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
import android.widget.CheckBox
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
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

/** Impostazioni di un singolo widget: cosa lancia, nome, icona, colore. Si apre alla creazione e con pressione prolungata. */
class WidgetConfigActivity : Activity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    /** pkgs[0] = "" è la modalità menu; poi i pacchetti dell'elenco. */
    private var pkgs: List<String> = emptyList()
    private var selectedPkg = ""
    private var autoName = ""
    private var symbol = Symbols.DEFAULT
    private var pendingImage: Bitmap? = null

    private lateinit var spinner: Spinner
    private lateinit var emptyHint: TextView
    private lateinit var nameField: EditText
    private lateinit var emojiField: EditText
    private lateinit var imageStatus: TextView
    private lateinit var iconGroup: RadioGroup
    private lateinit var toneGroup: RadioGroup
    private lateinit var positionCheck: CheckBox
    private val symbolViews = mutableMapOf<String, ImageView>()

    private val idSet = View.generateViewId()
    private val idApp = View.generateViewId()
    private val idEmoji = View.generateViewId()
    private val idImage = View.generateViewId()
    private val idWhite = View.generateViewId()
    private val idBlack = View.generateViewId()

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
        symbol = existing?.symbol ?: Symbols.DEFAULT

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
            addView(RadioButton(context).apply { id = idSet; text = "Set di icone" })
            addView(RadioButton(context).apply { id = idApp; text = "Icona dell'app (solo con app fissa)" })
            addView(RadioButton(context).apply { id = idEmoji; text = "Emoji" })
            addView(RadioButton(context).apply { id = idImage; text = "Immagine dalla galleria" })
        }
        root.addView(iconGroup)
        root.addView(buildSymbolStrip(root))
        emojiField = EditText(this).apply {
            hint = "Emoji, es. 🚴"
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
                Store.ICON_APP -> idApp
                Store.ICON_EMOJI -> idEmoji
                Store.ICON_IMAGE -> idImage
                else -> idSet
            },
        )

        root.addLabel("Colore di icone del set e nome (sfondo trasparente)")
        toneGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            addView(RadioButton(context).apply { id = idWhite; text = "Bianco" })
            addView(RadioButton(context).apply { id = idBlack; text = "Nero" })
        }
        root.addView(toneGroup)
        toneGroup.check(if (existing?.tone == Store.TONE_BLACK) idBlack else idWhite)

        root.addLabel("Posizione del menu")
        positionCheck = CheckBox(this).apply {
            text = "Dopo il salvataggio, tocca il widget in Home per posizionare il menu sopra di esso"
            // Utile al primo uso e se il widget è stato spostato
            isChecked = Store.position(this@WidgetConfigActivity, widgetId) == null
        }
        root.addView(positionCheck)

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

    /** Griglia con le icone del set; quella scelta è evidenziata. */
    private fun buildSymbolStrip(root: View): View {
        val ta = obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
        val tint = ta.getColor(0, 0xFF000000.toInt())
        ta.recycle()
        val columns = 6
        val cell = (resources.displayMetrics.widthPixels - 2 * root.dp(20)) / columns
        val pad = cell / 5
        val grid = GridLayout(this).apply { columnCount = columns }
        Symbols.all.forEach { (key, res) ->
            val iv = ImageView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = cell
                    height = cell
                }
                setPadding(pad, pad, pad, pad)
                setImageResource(res)
                setColorFilter(tint)
                contentDescription = key
                setOnClickListener {
                    symbol = key
                    iconGroup.check(idSet)
                    refreshSymbolStrip()
                }
            }
            symbolViews[key] = iv
            grid.addView(iv)
        }
        refreshSymbolStrip()
        return grid
    }

    private fun refreshSymbolStrip() {
        symbolViews.forEach { (key, iv) -> iv.alpha = if (key == symbol) 1f else 0.3f }
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
            idApp -> Store.ICON_APP
            idEmoji -> Store.ICON_EMOJI
            idImage -> Store.ICON_IMAGE
            else -> Store.ICON_SET
        }
        pendingImage?.let { bmp ->
            Store.imageFile(this, widgetId).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        // Senza immagine disponibile si torna al set di icone
        val finalMode = if (mode == Store.ICON_IMAGE && !Store.imageFile(this, widgetId).exists()) Store.ICON_SET else mode
        val tone = if (toneGroup.checkedRadioButtonId == idBlack) Store.TONE_BLACK else Store.TONE_WHITE
        Store.saveConfig(
            this, widgetId,
            Store.WidgetCfg(pkg, nameField.text.toString().trim(), finalMode, emojiField.text.toString().trim(), symbol, tone),
        )
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(widgetId)
                LauncherWidget().update(this@WidgetConfigActivity, glanceId)
            } catch (e: Exception) {
                // il widget viene comunque aggiornato dal sistema dopo l'aggiunta
            }
            if (positionCheck.isChecked && pkg.isEmpty()) {
                // Schermata trasparente sopra la Home: si tocca il centro del widget
                startActivity(
                    Intent(this@WidgetConfigActivity, PositionActivity::class.java)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
                )
            }
            finish()
        }
    }

    companion object {
        private const val REQ_IMAGE = 1
    }
}
