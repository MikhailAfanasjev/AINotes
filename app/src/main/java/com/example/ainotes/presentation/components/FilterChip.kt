package com.example.ainotes.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ainotes.presentation.ui.theme.Black
import com.example.ainotes.presentation.ui.theme.Blue
import com.example.ainotes.presentation.ui.theme.LightGray
import com.example.ainotes.presentation.ui.theme.White
import com.example.linguareader.R

@Composable
fun FilterChip(
    text: String,
    onClick: () -> Unit,
    selected: Boolean = false
) {
    val colors = MaterialTheme.colorScheme

    // фон чипа: выделенный — primary, невыделенный — secondary
    val background = if (selected) colors.primary else colors.background
    val contentColor = if (selected) colors.onSecondary else colors.onSecondary
    // цвет рамки всегда onBackground (цвет BorderStroke)
    val borderColor = colors.onBackground

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = background,
        contentColor = contentColor,
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}