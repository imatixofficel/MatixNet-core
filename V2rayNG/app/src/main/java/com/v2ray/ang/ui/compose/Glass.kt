package com.v2ray.ang.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** MatixNET: the corner radius shared by every card, so the whole app has one rounded look. */
val MatixCardShape = RoundedCornerShape(22.dp)

/**
 * MatixNET: the card of every screen. A clean, solid surface with a thin border and (in the day theme) a very soft
 * shadow, the way modern VPN apps draw their cards. [accent] tints the card sky blue, for the cards that should stand
 * out. The name is kept from the older glass look so every screen picks the new style up at once.
 */
@Composable
fun Modifier.glassCard(
    shape: Shape = MatixCardShape,
    accent: Boolean = false
): Modifier {
    val dark = LocalDarkTheme.current
    val fill = when {
        dark && accent -> Color(0xFF0E2236)
        dark -> Color(0xFF111826)
        accent -> Color(0xFFEAF4FD)
        else -> Color.White
    }
    val edge = when {
        dark && accent -> Color(0xFF1F4C75)
        dark -> Color(0xFF1E283B)
        accent -> Color(0xFFBCDDF4)
        else -> Color(0xFFE2E8F0)
    }
    val shadowTone = Color(0x1F1B3A66)
    return this
        .then(
            if (dark) Modifier
            else Modifier.shadow(3.dp, shape, clip = false, ambientColor = shadowTone, spotColor = shadowTone)
        )
        .clip(shape)
        .background(fill, shape)
        .border(1.dp, edge, shape)
}

/** MatixNET: the app background, a flat soft grey-white page in the day theme and a deep navy one at night. */
@Composable
fun Modifier.matixBackdrop(): Modifier {
    val dark = LocalDarkTheme.current
    val colors = if (dark) {
        listOf(Color(0xFF0B101B), Color(0xFF070A12), Color(0xFF05070D))
    } else {
        listOf(Color(0xFFF8FAFC), Color(0xFFF4F7FA), Color(0xFFF1F5F9))
    }
    return this.background(Brush.verticalGradient(colors))
}

/** MatixNET: the sky-blue gradient of the main buttons. */
val MatixBlueBrush: Brush
    get() = Brush.linearGradient(listOf(Color(0xFF3DB0F7), Color(0xFF1B7FE0)))
