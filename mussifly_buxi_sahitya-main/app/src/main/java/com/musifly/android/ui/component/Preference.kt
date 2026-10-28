package com.musifly.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musifly.android.R
import kotlin.math.roundToInt

@Composable
fun PreferenceEntry(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: String? = null,
    content: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    iconBackgroundColor: androidx.compose.ui.graphics.Color? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    isEnabled: Boolean = true,
) {
    val isLiquidGlass = com.musifly.android.ui.component.liquidglass.isLiquidGlassActive()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                enabled = isEnabled && onClick != null,
                onClick = onClick ?: {},
            )
            .alpha(if (isEnabled) 1f else 0.5f)
            .padding(
                horizontal = if (isLiquidGlass) 12.dp else 8.dp, 
                vertical = 12.dp // Consistent vertical padding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with background
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        iconBackgroundColor ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                 // Wrapper to tint icon if needed, or caller handles it
                 // For now, let caller handle tint, but LiquidGlass usually implies white icons.
                 // We can provide local content color.
                 androidx.compose.runtime.CompositionLocalProvider(
                     androidx.compose.material3.LocalContentColor provides 
                         if (iconBackgroundColor != null) androidx.compose.ui.graphics.Color.White 
                         else MaterialTheme.colorScheme.primary
                 ) {
                     icon()
                 }
            }

            Spacer(Modifier.width(if (isLiquidGlass) 12.dp else 12.dp))
        }

        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f),
        ) {
            ProvideTextStyle(
                MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold 
                )
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    title()
                }
            }

            if (description != null) {
                Spacer(modifier = Modifier.height(4.dp)) // Increased spacing
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            content?.invoke()
        }

        // Trailing content
        if (trailingContent != null) {
            Spacer(Modifier.width(12.dp)) // Increased spacer
            Box(contentAlignment = Alignment.CenterEnd) { // Box to stabilize layout
                trailingContent()
            }
        }
    }
}

@Composable
fun <T> ListPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    iconBackgroundColor: androidx.compose.ui.graphics.Color? = null,
    selectedValue: T,
    values: List<T>,
    valueText: @Composable (T) -> String,
    onValueSelected: (T) -> Unit,
    isEnabled: Boolean = true,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }

    if (showDialog) {
        ListDialog(
            onDismiss = { showDialog = false },
        ) {
            items(values) { value ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDialog = false
                            onValueSelected(value)
                        }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = valueText(value),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (value == selectedValue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )

                    androidx.compose.material3.RadioButton(
                        selected = value == selectedValue,
                        onClick = null, // Handled by Row clickable
                        colors = androidx.compose.material3.RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }

    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = valueText(selectedValue),
        icon = icon,
        iconBackgroundColor = iconBackgroundColor,
        onClick = { showDialog = true },
        isEnabled = isEnabled,
        trailingContent = {
             Icon(
                 painter = painterResource(R.drawable.navigate_next),
                 contentDescription = null,
                 modifier = Modifier.size(24.dp),
                 tint = MaterialTheme.colorScheme.onSurfaceVariant
             )
        }
    )
}

@Composable
inline fun <reified T : Enum<T>> EnumListPreference(
    modifier: Modifier = Modifier,
    noinline title: @Composable () -> Unit,
    noinline icon: (@Composable () -> Unit)?,
    iconBackgroundColor: androidx.compose.ui.graphics.Color? = null,
    selectedValue: T,
    noinline valueText: @Composable (T) -> String,
    noinline onValueSelected: (T) -> Unit,
    isEnabled: Boolean = true,
    values: List<T> = enumValues<T>().toList(),
) {
    ListPreference(
        modifier = modifier,
        title = title,
        icon = icon,
        iconBackgroundColor = iconBackgroundColor,
        selectedValue = selectedValue,
        values = values,
        valueText = valueText,
        onValueSelected = onValueSelected,
        isEnabled = isEnabled,
    )
}

@Composable
fun SwitchPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
    iconBackgroundColor: androidx.compose.ui.graphics.Color? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isEnabled: Boolean = true,
) {
    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = description,
        icon = icon,
        iconBackgroundColor = iconBackgroundColor,
        trailingContent = {
            com.musifly.android.ui.component.liquidglass.AdaptiveSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = isEnabled
            )
        },
        onClick = { onCheckedChange(!checked) },
        isEnabled = isEnabled
    )
}

@Composable
fun EditTextPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    iconBackgroundColor: androidx.compose.ui.graphics.Color? = null,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    isInputValid: (String) -> Boolean = { it.isNotEmpty() },
    isEnabled: Boolean = true,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }

    if (showDialog) {
        TextFieldDialog(
            initialTextFieldValue =
                TextFieldValue(
                    text = value,
                    selection = TextRange(value.length),
                ),
            singleLine = singleLine,
            isInputValid = isInputValid,
            onDone = onValueChange,
            onDismiss = { showDialog = false },
        )
    }

    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = value,
        icon = icon,
        iconBackgroundColor = iconBackgroundColor,
        onClick = { showDialog = true },
        isEnabled = isEnabled,
        trailingContent = {
             Icon(
                 painter = painterResource(R.drawable.navigate_next),
                 contentDescription = null,
                 modifier = Modifier.size(24.dp),
                 tint = MaterialTheme.colorScheme.onSurfaceVariant
             )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationSliderPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    iconBackgroundColor: androidx.compose.ui.graphics.Color? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    isEnabled: Boolean = true,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }

    var sliderValue by remember {
        mutableFloatStateOf(value)
    }

    if (showDialog) {
        ActionPromptDialog(
            titleBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.history_duration),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            },
            onDismiss = { showDialog = false },
            onConfirm = {
                showDialog = false
                onValueChange.invoke(sliderValue)
            },
            onCancel = {
                sliderValue = value
                showDialog = false
            },
            onReset = {
                sliderValue = 30f
            },
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.seconds,
                            sliderValue.roundToInt(),
                            sliderValue.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Spacer(Modifier.height(16.dp))

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 15f..60f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = value.roundToInt().toString(),
        icon = icon,
        iconBackgroundColor = iconBackgroundColor,
        onClick = { showDialog = true },
        isEnabled = isEnabled,
        trailingContent = {
             Icon(
                 painter = painterResource(R.drawable.navigate_next),
                 contentDescription = null,
                 modifier = Modifier.size(24.dp),
                 tint = MaterialTheme.colorScheme.onSurfaceVariant
             )
        }
    )
}

@Composable
fun PreferenceGroupTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, bottom = 4.dp, top = 8.dp),
    )
}
