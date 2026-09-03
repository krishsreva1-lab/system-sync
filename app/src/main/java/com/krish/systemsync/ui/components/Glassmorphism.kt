package com.krish.systemsync.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Rendered as clean, flat Material 3 cards with large border radii (28.dp) matching the design reference.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    opacity: Float = 0.15f,
    borderOpacity: Float = 0.3f,
    blurLevel: Float = 15f,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp // Flat design as requested in the reference image
    ) {
        Box {
            content()
        }
    }
}

/**
 * Standard Material modifier replacement for glassmorphism.
 */
fun Modifier.glassmorphism(
    shape: Shape = RoundedCornerShape(28.dp),
    opacity: Float = 0.1f,
    borderOpacity: Float = 0.25f,
    blurLevel: Float = 0f
): Modifier {
    return this.background(
        color = androidx.compose.ui.graphics.Color.Transparent,
        shape = shape
    )
}
