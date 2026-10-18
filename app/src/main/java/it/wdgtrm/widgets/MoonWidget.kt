package it.wdgtrm.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import it.wdgtrm.Astro
import java.time.Instant
import kotlin.math.roundToInt

class MoonWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val moon = Astro.moon(Instant.now())
        provideContent {
            GlanceTheme {
                WidgetScaffold {
                    Title(moon.emoji, size = 40)
                    Title(moon.name)
                    Caption("Illuminata ${(moon.illumination * 100).roundToInt()}%")
                    Caption("Piena tra ${moon.daysToFull.roundToInt()} g")
                }
            }
        }
    }
}

class MoonWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MoonWidget()
}
