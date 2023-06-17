package com.infinitepower.newquiz.settings_presentation.components.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.infinitepower.newquiz.core.compose.preferences.LocalPreferenceEnabledStatus
import com.infinitepower.newquiz.settings_presentation.model.Preference
import kotlin.math.roundToInt

@Composable
@ExperimentalMaterial3Api
internal fun SeekBarPreferenceWidget(
    preference: Preference.PreferenceItem.SeekBarPreference,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    val (currentValue, setCurrentValue) = remember(value) { mutableIntStateOf(value) }

    TextPreferenceWidget(
        preference = preference,
        summary = {
            PreferenceSummary(
                preference = preference,
                sliderValue = currentValue,
                onValueChange = setCurrentValue,
                onValueChangeEnd = { onValueChange(currentValue) }
            )
        }
    )
}

@Composable
private fun PreferenceSummary(
    preference: Preference.PreferenceItem.SeekBarPreference,
    sliderValue: Int,
    onValueChange: (Int) -> Unit,
    onValueChangeEnd: () -> Unit,
) {
    val isEnabled = LocalPreferenceEnabledStatus.current && preference.enabled

    Column {
        preference.summary?.let { Text(text = it) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = preference.valueRepresentation(sliderValue))
            Spacer(modifier = Modifier.width(16.dp))
            Slider(
                value = sliderValue.toFloat(),
                onValueChange = { if (preference.enabled) onValueChange(it.roundToInt()) },
                valueRange = preference.valueRange.toClosedFloatingPointRange(),
                steps = preference.steps,
                onValueChangeFinished = onValueChangeEnd,
                enabled = isEnabled
            )
        }
    }
}

private fun ClosedRange<Int>.toClosedFloatingPointRange(): ClosedFloatingPointRange<Float> = (start.toFloat()..endInclusive.toFloat())