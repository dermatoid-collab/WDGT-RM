package it.wdgtrm

import android.app.Activity
import android.content.Intent
import android.graphics.RectF
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.doOnLayout

/**
 * Menu aperto dal tocco sul widget. Un tocco su un'icona lancia l'app;
 * un tocco fuori (o indietro) chiude il menu.
 */
class MenuActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.getIntExtra(EXTRA_WIDGET_ID, -1) ?: -1
        val root = FrameLayout(this)
        root.setOnClickListener { finish() }
        setContentView(root)
        val panel = MenuPanel(
            this, root, widgetId, interactive = true,
            onLaunch = { launch(it) },
            onOpenSettings = {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            },
        )
        root.doOnLayout { panel.place(widgetBounds(root, widgetId), animate = true) }
    }

    /**
     * Dove si trova il widget: prima il dato del launcher (se lo fornisce), poi il punto
     * indicato dall'utente nelle impostazioni. Senza nessuno dei due: centro schermo.
     */
    private fun widgetBounds(root: FrameLayout, widgetId: Int): RectF? {
        intent?.sourceBounds?.let { return RectF(it) }
        val (px, py) = Store.position(this, widgetId) ?: return null
        val (wDp, hDp) = MenuPanel.widgetSizeDp(this, widgetId)
        val halfW = root.dp(wDp) / 2f
        val halfH = root.dp(hDp) / 2f
        return RectF(px - halfW, py - halfH, px + halfW, py + halfH)
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

    companion object {
        const val EXTRA_WIDGET_ID = "widgetId"
    }
}
