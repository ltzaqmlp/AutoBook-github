package com.autobook.lingxi.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TrendChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxAmount = data.maxOfOrNull { it.second } ?: 1.0
    val scaleMax = if (maxAmount == 0.0) 1.0 else maxAmount * 1.2

    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "近7日支出趋势",
            style = MaterialTheme.typography.labelLarge,
            color = labelColor
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { (date, amount) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Canvas(modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .fillMaxHeight()
                    ) {
                        val barHeight = (amount / scaleMax * size.height).toFloat()
                        if (barHeight > 0) {
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(0f, size.height - barHeight),
                                size = Size(size.width, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx())
                            )
                        }
                    }
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        maxLines = 1,
                        color = labelColor,
                        modifier = Modifier.padding(top = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}