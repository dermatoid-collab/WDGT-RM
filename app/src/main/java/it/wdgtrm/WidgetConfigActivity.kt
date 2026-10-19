package it.wdgtrm

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.widget.doAfterTextChanged
import androidx.glance.appwidget.GlanceAppWidgetManager
import it.wdgtrm.widgets.LauncherWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Impostazioni di un singolo widget: cosa lancia, nome, icona, sfondo, menu.
 * Si apre alla creazione e con pressione prolungata.
 */
class WidgetConfigActivity : Activity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    /** pkgs[0] = "" è la modalità menu; poi i pacchetti dell'elenco. */
    private var pkgs: List<String> = emptyList()
    private var selectedPkg = ""
    private var autoName = ""
    private var symbol = Symbols.DEFAULT
    private var pendingImage: Bitmap? = null
    private var menuApps: MutableList<String> = mutableListOf()
    private lateinit var appEditor: AppListEditor
    private var savedImage: Bitmap? = null

    private var iconColor = 0xFFFFFFFF.toInt()
    private var iconSize = 60
    private var bgShape = BgShapes.NONE
    private var bgColor = 0xFF000000.toInt()
    private var bgAlpha = 70
    private var menuColor = 0
    private var menuAlpha = 100

    private lateinit var spinner: Spinner
    private lateinit var emptyHint: TextView
    private lateinit var nameField: EditText
    private lateinit var emojiField: EditText
    private lateinit var imageStatus: TextView
    private lateinit var iconGroup: RadioGroup
    private lateinit var positionCheck: CheckBox
    private val symbolViews = mutableMapOf<String, ImageView>()

    private lateinit var previewTile: FrameLayout
    private lateinit var previewBg: ImageView
    private lateinit var previewIcon: ImageView
    private lateinit var previewEmoji: TextView
    private lateinit var previewName: TextView

    private val idSet = View.generateViewId()
    private val idApp = View.generateViewId()
    private val idEmoji = View.generateViewId()
    private val idImage = View.generateViewId()

    private val palette = listOf(
        0xFFFFFFFFL, 0xFF000000L, 0xFF303030L, 0xFFBDBDBDL, 0xFFE53935L,
        0xFFFB8C00L, 0xFFFDD835L, 0xFF43A047L, 0xFF1E88E5L, 0xFF8E24AAL,
    ).map { it.toInt() }

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
        // Elenco proprio del widget; i nuovi (o mai personalizzati) partono da una copia di quello generale
        menuApps = (Store.widgetApps(this, widgetId) ?: Store.apps(this)).toMutableList()
        existing?.let {
            symbol = it.symbol
            iconColor = it.iconColor
            iconSize = it.iconSize
            bgShape = it.bgShape
            bgColor = it.bgColor
            bgAlpha = it.bgAlpha
            menuColor = it.menuColor
            menuAlpha = it.menuAlpha
        }
        val imgFile = Store.imageFile(this, widgetId)
        if (imgFile.exists()) savedImage = BitmapFactory.decodeFile(imgFile.path)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.padForSystemBars(root.dp(20))
        root.addTitle("Impostazioni widget")
        root.addView(buildPreview(root))

        root.addLabel("Cosa lancia il widget")
        spinner = Spinner(this)
        root.addView(spinner)
        emptyHint = TextView(this).apply { text = "L'elenco è vuoto: aggiungi delle app qui sotto." }
        root.addView(emptyHint)

        root.addLabel("App del menu di questo widget")
        appEditor = AppListEditor(this, { menuApps }, { menuApps = it.toMutableList() }) { refreshAppChoices() }
        root.addView(appEditor.view)

        root.addLabel("Nome")
        nameField = EditText(this).apply {
            setSingleLine()
            setText(existing?.name.orEmpty())
        }
        root.addView(nameField)

        // --- Icona ---
        root.addLabel("Icona")
        iconGroup = RadioGroup(this).apply {
            addView(RadioButton(context).apply { id = idSet; text = "Set di icone" })
            addView(RadioButton(context).apply { id = idApp; text = "Icona dell'app (solo con app fissa)" })
            addView(RadioButton(context).apply { id = idEmoji; text = "Emoji" })
            addView(RadioButton(context).apply { id = idImage; text = "Immagine dalla galleria" })
        }
        root.addView(iconGroup)
        root.addView(buildSymbolGrid(root))
        emojiField = EditText(this).apply {
            hint = "Emoji, es. 🚴"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(existing?.emoji.orEmpty())
        }
        root.addView(emojiField)
        imageStatus = TextView(this).apply { text = if (savedImage != null) "Immagine impostata" else "Nessuna immagine" }
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

        root.addLabel("Colore di icona del set, emoji e nome")
        root.addView(swatchRow(root, palette, false, { iconColor }) { iconColor = it; refreshPreview() })
        root.addView(seekRow(root, "Dimensione icona", 32, 88, iconSize, " dp") { iconSize = it; refreshPreview() })

        // --- Sfondo del widget ---
        root.addLabel("Sfondo del widget (forma dell'icona)")
        val shapeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@WidgetConfigActivity, android.R.layout.simple_spinner_dropdown_item,
                BgShapes.all.map { it.second },
            )
            setSelection(BgShapes.all.indexOfFirst { it.first == bgShape }.coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    bgShape = BgShapes.all[pos].first
                    refreshPreview()
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }
        root.addView(shapeSpinner)
        root.addView(swatchRow(root, palette, false, { bgColor }) { bgColor = it; refreshPreview() })
        root.addView(seekRow(root, "Opacità sfondo", 0, 100, bgAlpha, "%") { bgAlpha = it; refreshPreview() })

        // --- Menu ---
        root.addLabel("Sfondo del menu (si apre al tocco sul widget)")
        root.addView(swatchRow(root, palette, true, { menuColor }) { menuColor = it })
        root.addView(seekRow(root, "Opacità menu", 10, 100, menuAlpha, "%") { menuAlpha = it })

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
                refreshPreview()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        iconGroup.setOnCheckedChangeListener { _, _ -> refreshPreview() }
        nameField.doAfterTextChanged { refreshPreview() }
        emojiField.doAfterTextChanged { refreshPreview() }
        refreshPreview()
    }

    // ---------- Anteprima ----------

    /** Riquadro grigio (per far vedere la trasparenza) con il widget com'è adesso. */
    private fun buildPreview(root: View): View {
        previewBg = ImageView(this)
        previewIcon = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
        previewEmoji = TextView(this).apply { gravity = Gravity.CENTER }
        previewTile = FrameLayout(this).apply {
            addView(previewBg, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            addView(previewIcon, FrameLayout.LayoutParams(root.dp(40), root.dp(40), Gravity.CENTER))
            addView(previewEmoji, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        }
        previewName = TextView(this).apply {
            textSize = 12f
            gravity = Gravity.CENTER
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(root.dp(12), root.dp(12), root.dp(12), root.dp(12))
            addView(previewTile, LinearLayout.LayoutParams(root.dp(60), root.dp(60)))
            addView(previewName, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        return FrameLayout(this).apply {
            setBackgroundColor(0xFF7A7A7A.toInt())
            addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        }
    }

    private fun refreshPreview() {
        val tilePx = previewTile.dp(iconSize)
        previewTile.layoutParams = LinearLayout.LayoutParams(tilePx, tilePx)
        val shape = BgShapes.res(bgShape)
        if (shape != null) {
            previewBg.visibility = View.VISIBLE
            previewBg.setImageResource(shape)
            previewBg.setColorFilter(ColorUtils.setAlphaComponent(bgColor, bgAlpha * 255 / 100))
        } else {
            previewBg.visibility = View.GONE
        }
        // Con la tessera il simbolo sta dentro, come nelle icone One UI
        val glyphPx = if (shape != null) (tilePx * 0.6f).toInt() else tilePx
        previewIcon.layoutParams = FrameLayout.LayoutParams(glyphPx, glyphPx, Gravity.CENTER)
        previewIcon.clearColorFilter()
        previewIcon.visibility = View.VISIBLE
        previewEmoji.visibility = View.GONE

        fun showSymbol() {
            previewIcon.setImageResource(Symbols.res(symbol))
            previewIcon.setColorFilter(iconColor)
        }
        when (iconGroup.checkedRadioButtonId) {
            idEmoji -> {
                previewIcon.visibility = View.GONE
                previewEmoji.visibility = View.VISIBLE
                previewEmoji.text = emojiField.text.toString().ifBlank { "▦" }
                previewEmoji.setTextColor(iconColor)
                previewEmoji.setTextSize(TypedValue.COMPLEX_UNIT_PX, glyphPx * 0.8f)
            }
            idImage -> {
                val bmp = pendingImage ?: savedImage
                if (bmp != null) previewIcon.setImageBitmap(bmp) else showSymbol()
            }
            idApp -> {
                if (selectedPkg.isEmpty()) {
                    showSymbol()
                } else {
                    try {
                        previewIcon.setImageDrawable(packageManager.getApplicationIcon(selectedPkg))
                    } catch (e: Exception) {
                        showSymbol()
                    }
                }
            }
            else -> showSymbol()
        }
        previewName.text = nameField.text.toString()
        previewName.setTextColor(iconColor)
    }

    // ---------- Componenti ----------

    /** Griglia con le icone del set; quella scelta è evidenziata. */
    private fun buildSymbolGrid(root: View): View {
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
                    refreshSymbolGrid()
                    refreshPreview()
                }
            }
            symbolViews[key] = iv
            grid.addView(iv)
        }
        refreshSymbolGrid()
        return grid
    }

    private fun refreshSymbolGrid() {
        symbolViews.forEach { (key, iv) -> iv.alpha = if (key == symbol) 1f else 0.3f }
    }

    /** Riga scorrevole di campioni di colore; [withAuto] aggiunge «Auto» (valore 0). */
    private fun swatchRow(
        root: View,
        colors: List<Int>,
        withAuto: Boolean,
        current: () -> Int,
        onPick: (Int) -> Unit,
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, root.dp(4), 0, root.dp(4))
        }
        val items = mutableListOf<Pair<Int, View>>()
        fun refresh() {
            items.forEach { (c, v) ->
                val selected = c == current()
                v.alpha = if (selected) 1f else 0.45f
                v.scaleX = if (selected) 1.15f else 1f
                v.scaleY = v.scaleX
            }
        }
        fun add(value: Int, v: View) {
            v.setOnClickListener {
                onPick(value)
                refresh()
            }
            items += value to v
            row.addView(v)
        }
        val size = root.dp(40)
        val margin = root.dp(4)
        if (withAuto) {
            add(
                0,
                TextView(this).apply {
                    text = "Auto"
                    textSize = 12f
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setStroke(root.dp(1), 0x99888888.toInt())
                    }
                    layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(margin, 0, margin, 0) }
                },
            )
        }
        colors.forEach { col ->
            add(
                col,
                View(this).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(col)
                        setStroke(root.dp(1), 0x99888888.toInt())
                    }
                    layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(margin, 0, margin, 0) }
                },
            )
        }
        refresh()
        return HorizontalScrollView(this).apply { addView(row) }
    }

    /** Cursore con etichetta che mostra il valore corrente. */
    private fun seekRow(
        root: View,
        label: String,
        min: Int,
        max: Int,
        value: Int,
        unit: String,
        onChange: (Int) -> Unit,
    ): View {
        val text = TextView(this).apply { this.text = "$label: $value$unit" }
        val bar = SeekBar(this).apply {
            this.max = max - min
            progress = value - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                    val v = progress + min
                    text.text = "$label: $v$unit"
                    onChange(v)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, root.dp(4), 0, root.dp(4))
            addView(text)
            addView(bar)
        }
    }

    // ---------- Ciclo di vita ----------

    override fun onResume() {
        super.onResume()
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        refreshAppChoices()
    }

    /** Aggiorna la scelta «cosa lancia» quando cambia l'elenco di app del widget. */
    private fun refreshAppChoices() {
        pkgs = listOf("") + menuApps
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
        refreshPreview()
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
        Store.setWidgetApps(this, widgetId, menuApps)
        Store.saveConfig(
            this, widgetId,
            Store.WidgetCfg(
                pkg = pkg,
                name = nameField.text.toString().trim(),
                iconMode = finalMode,
                emoji = emojiField.text.toString().trim(),
                symbol = symbol,
                iconColor = iconColor,
                iconSize = iconSize,
                bgShape = bgShape,
                bgColor = bgColor,
                bgAlpha = bgAlpha,
                menuColor = menuColor,
                menuAlpha = menuAlpha,
            ),
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
