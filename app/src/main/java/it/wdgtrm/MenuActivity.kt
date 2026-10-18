package it.wdgtrm

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.doOnLayout
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Menu circolare aperto dal tocco sul widget: solo icone disposte a cerchio attorno
 * al widget. Un tocco su un'icona lancia l'app; un tocco fuori chiude il menu.
 */
class MenuActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val apps = Store.apps(this).mapNotNull { pkg -> Apps.label(this, pkg)?.let { pkg to it } }
        val root = FrameLayout(this)
        root.setBackgroundColor(0x80000000.toInt())
        root.setOnClickListener { finish() }
        setContentView(root)
        if (apps.isEmpty()) {
            showEmpty(root)
        } else {
            root.doOnLayout { layoutRing(root, apps) }
        }
    }

    private fun layoutRing(root: FrameLayout, apps: List<Pair<String, String>>) {
        val pm = packageManager
        val size = root.dp(60)
        val n = apps.size
        // Raggio minimo 88dp; cresce con il numero di app per non sovrapporle
        val radius = maxOf(root.dp(88), (n * root.dp(68) / (2 * PI)).toInt())
        val margin = radius + size / 2 + root.dp(8)

        // Il launcher indica dove si trova il widget toccato; altrimenti centro schermo
        val bounds = intent?.sourceBounds
        val cx = clamp(bounds?.exactCenterX() ?: root.width / 2f, margin.toFloat(), (root.width - margin).toFloat())
        val cy = clamp(bounds?.exactCenterY() ?: root.height / 2f, margin.toFloat(), (root.height - margin).toFloat())

        val tv = TypedValue()
        theme.resolveAttribute(android.R.attr.colorBackground, tv, true)
        val pad = root.dp(10)

        apps.forEachIndexed { i, (pkg, label) ->
            val angle = -PI / 2 + 2 * PI * i / n // parte dall'alto, in senso orario
            val tx = cx + (radius * cos(angle)).toFloat() - size / 2f
            val ty = cy + (radius * sin(angle)).toFloat() - size / 2f
            val icon = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(size, size)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(tv.data)
                }
                setPadding(pad, pad, pad, pad)
                try { setImageDrawable(pm.getApplicationIcon(pkg)) } catch (e: Exception) { }
                contentDescription = label
                elevation = root.dp(6).toFloat()
                setOnClickListener { launch(pkg) }
                // Parte dal centro e si apre a raggiera
                x = cx - size / 2f
                y = cy - size / 2f
                alpha = 0f
                scaleX = 0.3f
                scaleY = 0.3f
            }
            root.addView(icon)
            icon.animate().x(tx).y(ty).alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay(i * 20L).setDuration(180).start()
        }
    }

    private fun clamp(v: Float, lo: Float, hi: Float) = if (lo > hi) (lo + hi) / 2 else v.coerceIn(lo, hi)

    private fun showEmpty(root: FrameLayout) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.menu_bg)
            setPadding(dp(20), dp(20), dp(20), dp(12))
            isClickable = true // un tocco sulla scheda non chiude il menu
            addView(TextView(context).apply { text = "Nessuna app nell'elenco." })
            addButton("Apri impostazioni") {
                startActivity(Intent(this@MenuActivity, MainActivity::class.java))
                finish()
            }
        }
        root.addView(card, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER,
        ))
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
}
