package it.wdgtrm.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import it.wdgtrm.MainActivity

/** Contenitore comune: sfondo Material You, angoli arrotondati, tocco → apre l'app. */
@Composable
fun WidgetScaffold(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
fun Title(text: String, size: Int = 14) = Text(
    text = text,
    style = TextStyle(
        color = GlanceTheme.colors.onSurface,
        fontSize = size.sp,
        fontWeight = FontWeight.Medium,
    ),
)

@Composable
fun Caption(text: String) = Text(
    text = text,
    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
)
