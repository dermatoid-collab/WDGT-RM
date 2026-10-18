package it.wdgtrm

import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

fun View.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

/** Con targetSdk 35 le Activity sono edge-to-edge: lascia spazio alle barre di sistema. */
fun View.padForSystemBars(base: Int) {
    setPadding(base, base, base, base)
    setOnApplyWindowInsetsListener { v, ins ->
        val bars = ins.getInsets(WindowInsets.Type.systemBars())
        v.setPadding(base + bars.left, base + bars.top, base + bars.right, base + bars.bottom)
        WindowInsets.CONSUMED
    }
}

fun ViewGroup.addTitle(text: String, size: Float = 22f) = addView(TextView(context).apply {
    this.text = text
    textSize = size
    setPadding(0, dp(8), 0, dp(8))
})

fun ViewGroup.addLabel(text: String) = addView(TextView(context).apply {
    this.text = text
    textSize = 13f
    setPadding(0, dp(16), 0, dp(4))
})

fun ViewGroup.addButton(text: String, onClick: () -> Unit) = addView(Button(context).apply {
    this.text = text
    setOnClickListener { onClick() }
})

fun LinearLayout.horizontalRow(): LinearLayout = LinearLayout(context).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = android.view.Gravity.CENTER_VERTICAL
}
