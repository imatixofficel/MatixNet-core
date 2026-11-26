package com.v2ray.ang.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Matix net: the corner radius shared by every glass card, so the whole app has one rounded look. */
val MatixCardShape = RoundedCornerShape(26.dp)

/**
 * Matix net: a "liquid glass" card. It is a translucent gradient with a bright top edge and a soft
 * lower edge, drawn with plain gradients (no blur), so it costs nothing on weak phones.
 * [accent] tints the card blue, for the cards that should stand out.
 */
@Composable
fun Modifier.glassCard(
    shape: Shape = MatixCardShape,
    accent: Boolean = false
): Modifier {
    val dark = LocalDarkTheme.current
    val blue = Color(0xFF3D8BFF)
    val fillTop = when {
        dark && accent -> blue.copy(alpha = 0.22f)
        dark -> Color.White.copy(alpha = 0.10f)
        accent -> blue.copy(alpha = 0.16f)
        else -> Color.White.copy(alpha = 0.78f)
    }
    val fillBottom = when {
        dark && accent -> blue.copy(alpha = 0.06f)
        dark -> Color.White.copy(alpha = 0.03f)
        accent -> blue.copy(alpha = 0.05f)
        else -> Color.White.copy(alpha = 0.48f)
    }
    val edgeTop = if (dark) Color.White.copy(alpha = 0.38f) else Color.White
    val edgeBottom = if (dark) Color.White.copy(alpha = 0.04f) else Color(0xFF9DBBFF).copy(alpha = 0.40f)
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(fillTop, fillBottom)))
        .border(1.dp, Brush.verticalGradient(listOf(edgeTop, edgeBottom)), shape)
}

/** Matix net: the app background, a black page with a faint blue glow at the top (a pale blue one in the day theme). */
@Composable
fun Modifier.matixBackdrop(): Modifier {
    val dark = LocalDarkTheme.current
    val colors = if (dark) {
        listOf(Color(0xFF071A3F), Color(0xFF000000), Color(0xFF000000))
    } else {
        listOf(Color(0xFFDCE8FF), Color(0xFFF2F6FF), Color(0xFFF7F9FF))
    }
    return this.background(Brush.verticalGradient(colors))
}

/** Matix net: the blue gradient of the main buttons and the logo. */
val MatixBlueBrush: Brush
    get() = Brush.linearGradient(listOf(Color(0xFF4C9BFF), Color(0xFF1456D9)))
