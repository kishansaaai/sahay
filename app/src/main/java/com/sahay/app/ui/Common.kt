package com.sahay.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sahay.app.R
import com.sahay.app.state.AppState

/** Reads a UI string from the language-selected context provided at the root of [com.sahay.app.SahayApp]. */
@Composable
fun sr(id: Int, vararg args: Any): String = LocalContext.current.resources.getString(id, *args)

@Composable
fun ChipRow(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text) })
}

@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
fun ProgressDots(total: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            val colour = if (index <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            Box(modifier = Modifier.size(if (index == current) 10.dp else 8.dp).background(colour, CircleShape))
        }
    }
}

@Composable
fun LanguageChips(state: AppState, modifier: Modifier = Modifier) {
    // Only languages with a bundled translation are offered; showing a chip that falls back to English would
    // misrepresent the build. B19 adds the remaining locales.
    val options = listOf(
        "en" to sr(R.string.english),
        "hi" to sr(R.string.hindi),
    )
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (code, label) ->
            FilterChip(selected = state.language == code, onClick = { state.selectLanguage(code) }, label = { Text(label) })
        }
    }
}

/** Small progress line used on the dashboard and today screen. [label] is read by TalkBack when the bar is not self-explanatory. */
@Composable
fun MasteryBar(fraction: Double, modifier: Modifier = Modifier, label: String? = null) {
    val described = if (label == null) modifier else modifier.semantics { contentDescription = label }
    androidx.compose.material3.LinearProgressIndicator(
        progress = { fraction.coerceIn(0.0, 1.0).toFloat() },
        modifier = described.fillMaxWidth(),
    )
}
