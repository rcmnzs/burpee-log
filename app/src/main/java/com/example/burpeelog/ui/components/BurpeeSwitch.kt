package com.example.burpeelog.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Componente centralizado de Switch para o Burpee Log.
 *
 * Garante alto contraste em estados desligado e desabilitado tanto no tema escuro quanto claro:
 * - Ligado: trilho em primary, botão em onPrimary
 * - Desligado: trilho em surfaceVariant, borda em onSurfaceVariant e botão em onSurfaceVariant
 * - Desabilitado: mesmas cores do estado desligado com alpha 0.5 (visível, porém atenuado)
 */
@Composable
fun BurpeeSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme

    val switchColors = SwitchDefaults.colors(
        // Ligado: trilho em primary, botão em onPrimary
        checkedThumbColor = colorScheme.onPrimary,
        checkedTrackColor = colorScheme.primary,
        checkedBorderColor = colorScheme.primary,

        // Desligado: trilho em surfaceVariant, borda de 2.dp em onSurfaceVariant e botão em onSurfaceVariant
        uncheckedThumbColor = colorScheme.onSurfaceVariant,
        uncheckedTrackColor = colorScheme.surfaceVariant,
        uncheckedBorderColor = colorScheme.onSurfaceVariant,

        // Desabilitado (enabled = false): mesmas cores do estado desligado com alpha 0.5
        disabledUncheckedThumbColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        disabledUncheckedTrackColor = colorScheme.surfaceVariant.copy(alpha = 0.5f),
        disabledUncheckedBorderColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),

        // Caso esteja desabilitado mas ligado
        disabledCheckedThumbColor = colorScheme.onPrimary.copy(alpha = 0.5f),
        disabledCheckedTrackColor = colorScheme.primary.copy(alpha = 0.5f),
        disabledCheckedBorderColor = colorScheme.primary.copy(alpha = 0.5f)
    )

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = switchColors
    )
}
