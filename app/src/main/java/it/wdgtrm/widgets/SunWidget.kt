package it.wdgtrm.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import it.wdgtrm.Astro
import it.wdgtrm.Config
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SunWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sun = Astro.sun(LocalDate.now(Config.ZONE), Config.LATITUDE, Config.LONGITUDE, Config.ZONE)
        val fmt = DateTimeFormatter.ofPattern("HH:mm")
        val rise = sun.sunrise
        val set = sun.sunset
        provideContent {
            GlanceTheme {
                WidgetScaffold {
                    if (rise == null || set == null) {
                        Title("Sole")
                        Caption("Nessuna alba/tramonto oggi")
                    } else {
                        val light = Duration.between(rise, set)
                        Title("☀️ ${rise.format(fmt)}  →  ${set.format(fmt)}", size = 18)
                        Caption("Luce: ${light.toHours()} h ${light.toMinutesPart()} min")
                        Caption(nextEvent(rise, set))
                    }
                }
            }
        }
    }

    private fun nextEvent(rise: ZonedDateTime, set: ZonedDateTime): String {
        val now = ZonedDateTime.now(Config.ZONE)
        val (label, target) = when {
            now.isBefore(rise) -> "Alba" to rise
            now.isBefore(set) -> "Tramonto" to set
            else -> return "Giornata conclusa"
        }
        val d = Duration.between(now, target)
        return "$label tra ${d.toHours()} h ${d.toMinutesPart()} min"
    }
}

class SunWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = SunWidget()
}
