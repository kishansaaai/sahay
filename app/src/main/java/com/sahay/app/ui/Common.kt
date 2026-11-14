package com.sahay.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
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
    // Hindi and English are complete. The other four locales translate only the main screens (B19), so their chips carry
    // a "beta" tag and a note below says so; nothing pretends they are finished.
    val beta = sr(R.string.beta_tag)
    val options = listOf(
        Triple("en", sr(R.string.english), false),
        Triple("hi", sr(R.string.hindi), false),
        Triple("ta", sr(R.string.tamil), true),
        Triple("te", sr(R.string.telugu), true),
        Triple("bn", sr(R.string.bengali), true),
        Triple("mr", sr(R.string.marathi), true),
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (code, label, isBeta) ->
                FilterChip(
                    selected = state.language == code,
                    onClick = { state.selectLanguage(code) },
                    label = { Text(if (isBeta) "$label · $beta" else label) },
                )
            }
        }
        if (options.any { it.third && it.first == state.language }) {
            Text(text = sr(R.string.locale_beta_note), style = MaterialTheme.typography.bodySmall)
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

/**
 * One answer choice (B20). TalkBack announces it as a radio button with its selected state, and the selection is shown by a
 * filled or empty marker as well as by colour, so it does not depend on telling two similar colours apart.
 */
@Composable
fun OptionCard(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            // Decorative: the selected state is already exposed by selectable().
            Text(text = if (selected) "●" else "○", modifier = Modifier.clearAndSetSemantics {})
            Text(text = text, modifier = Modifier.weight(1f))
        }
    }
}
