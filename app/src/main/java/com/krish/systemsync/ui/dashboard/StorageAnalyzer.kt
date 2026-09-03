package com.krish.systemsync.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krish.systemsync.ui.theme.SystemSYNCTheme

data class StorageCategory(
    val name: String,
    val sizeGb: Float,
    val color: Color
)

@Composable
fun StorageAnalyzer(
    totalStorageGb: Float,
    categories: List<StorageCategory>
) {
    val usedStorageGb = categories.sumOf { it.sizeGb.toDouble() }.toFloat()
    val freeStorageGb = totalStorageGb - usedStorageGb

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Storage Analyzer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${String.format("%.1f", usedStorageGb)}GB / ${totalStorageGb.toInt()}GB",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                DonutChart(categories, totalStorageGb)
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (totalStorageGb > 0) "${((usedStorageGb / totalStorageGb) * 100).toInt()}%" else "0%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Used",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { category ->
                            CategoryItem(category, Modifier.weight(1f))
                        }
                        if (row.size < 2) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(categories: List<StorageCategory>, total: Float) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animateStroke by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }

    Canvas(modifier = Modifier.size(180.dp)) {
        if (total <= 0f) return@Canvas
        var startAngle = -90f
        categories.forEach { category ->
            val sweepAngle = (category.sizeGb / total) * 360f
            drawArc(
                color = category.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle * animateStroke,
                useCenter = false,
                style = Stroke(width = 30f, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )
            startAngle += sweepAngle
        }

        // Draw background remaining
        val remainingSweep = (1f - categories.sumOf { it.sizeGb.toDouble() }.toFloat() / total) * 360f
        drawArc(
            color = Color.Gray.copy(alpha = 0.2f),
            startAngle = startAngle,
            sweepAngle = remainingSweep,
            useCenter = false,
            style = Stroke(width = 30f, cap = StrokeCap.Round),
            size = Size(size.width, size.height)
        )
    }
}

@Composable
fun CategoryItem(category: StorageCategory, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(category.color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(category.name, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text("${category.sizeGb} GB", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StorageAnalyzerPreview() {
    SystemSYNCTheme {
        Box(Modifier.padding(16.dp)) {
            StorageAnalyzer(
                totalStorageGb = 256f,
                categories = listOf(
                    StorageCategory("Images", 45.5f, Color(0xFF4CAF50)),
                    StorageCategory("Videos", 88.2f, Color(0xFF2196F3)),
                    StorageCategory("Documents", 12.8f, Color(0xFFFFC107)),
                    StorageCategory("System", 30.0f, Color(0xFF9C27B0)),
                    StorageCategory("Apps", 25.4f, Color(0xFFFF5722))
                )
            )
        }
    }
}
