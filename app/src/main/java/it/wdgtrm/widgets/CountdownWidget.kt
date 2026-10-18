package it.wdgtrm.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import it.wdgtrm.Config
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class CountdownWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val days = ChronoUnit.DAYS.between(LocalDate.now(Config.ZONE), Config.EVENT_DATE)
        val date = Config.EVENT_DATE.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALY))
        provideContent {
            GlanceTheme {
                WidgetScaffold {
                    Title("🚴 ${Config.EVENT_NAME}")
                    when {
                        days > 0 -> {
                            Title("$days", size = 40)
                            Caption("giorni · $date")
                        }
                        days == 0L -> Title("Oggi si pedala! 🔥", size = 18)
                        else -> Caption("Evento concluso")
                    }
                }
            }
        }
    }
}

class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = CountdownWidget()
}
