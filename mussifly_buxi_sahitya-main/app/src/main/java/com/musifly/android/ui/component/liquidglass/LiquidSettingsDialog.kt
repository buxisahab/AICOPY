package com.musifly.android.ui.component.liquidglass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musifly.android.R
import com.musifly.android.constants.*
import com.musifly.android.utils.rememberPreference

@Composable
fun LiquidSettingsDialog(
    onDismiss: () -> Unit
) {
    LiquidGlassDialog(
        onDismiss = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Advanced settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Close"
                    )
                }
            }
        },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Enable unsafe values
                val (enableUnsafeValues, setEnableUnsafeValues) = rememberPreference(LiquidGlassEnableUnsafeValuesKey, defaultValue = false)
                LiquidGlassSettingItem(
                    label = "Enable unsafe values",
                    control = {
                        LiquidToggle(
                            selected = { enableUnsafeValues },
                            onSelect = { setEnableUnsafeValues(it) }
                        )
                    }
                )

                // Bleed blur radius
                val (bleedBlurRadius, setBleedBlurRadius) = rememberPreference(LiquidGlassBleedBlurRadiusKey, defaultValue = 26f)
                LiquidGlassSettingItem(
                    label = "Bleed blur radius",
                    value = "${bleedBlurRadius.toInt()} dp",
                    control = {
                        LiquidSlider(
                            value = { bleedBlurRadius },
                            onValueChange = { setBleedBlurRadius(it) },
                            valueRange = 0f..100f
                        )
                    }
                )

                // Eccentric factor
                val (eccentricFactor, setEccentricFactor) = rememberPreference(LiquidGlassEccentricFactorKey, defaultValue = 1.0f)
                LiquidGlassSettingItem(
                    label = "Eccentric factor",
                    value = String.format("%.2f", eccentricFactor),
                    control = {
                        LiquidSlider(
                            value = { eccentricFactor },
                            onValueChange = { setEccentricFactor(it) },
                            valueRange = 0f..2f
                        )
                    }
                )

                // Enable dispersion
                val (enableDispersion, setEnableDispersion) = rememberPreference(LiquidGlassEnableDispersionKey, defaultValue = false)
                LiquidGlassSettingItem(
                    label = "Enable dispersion",
                    control = {
                        LiquidToggle(
                            selected = { enableDispersion },
                            onSelect = { setEnableDispersion(it) }
                        )
                    }
                )
            }
        }
    )
}

@Composable
fun LiquidGlassSettingItem(
    label: String,
    value: String? = null,
    control: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Determine icon based on label
                val iconRes = when {
                    label.contains("radius", ignoreCase = true) -> R.drawable.blur
                    label.contains("factor", ignoreCase = true) -> R.drawable.contrast
                    label.contains("dispersion", ignoreCase = true) -> R.drawable.auto_awesome
                    label.contains("unsafe", ignoreCase = true) -> R.drawable.bug_report
                    else -> null
                }
                
                if (iconRes != null) {
                   Icon(
                       painter = painterResource(iconRes),
                       contentDescription = null,
                       modifier = Modifier.size(24.dp).padding(end = 8.dp),
                       tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                   )
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        control()
    }
}
