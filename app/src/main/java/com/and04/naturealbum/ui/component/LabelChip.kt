package com.and04.naturealbum.ui.component

import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.and04.naturealbum.utils.color.toColor

@Composable
fun LabelChip(
    modifier: Modifier = Modifier,
    backgroundColor: String,
    onClick: () -> Unit = {},
    label: @Composable () -> Unit,
) {
    val color = backgroundColor.toColor()

    SuggestionChip(
        modifier = modifier,
        onClick = { onClick() },
        label = label,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color,
            labelColor = if (color.luminance() > 0.5f) Color.Black else Color.White
        )
    )
}
