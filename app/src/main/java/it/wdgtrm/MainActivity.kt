package it.wdgtrm

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()
        setContentView(TextView(this).apply {
            setPadding(pad, pad * 2, pad, pad)
            textSize = 16f
            text = "WDGT-RM\n\nTieni premuto sulla schermata Home → Widget → WDGT-RM, " +
                "poi trascina Luna, Sole o Countdown dove preferisci."
        })
    }
}
