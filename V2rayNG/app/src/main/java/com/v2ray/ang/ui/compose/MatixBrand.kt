package com.v2ray.ang.ui.compose

import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.v2ray.ang.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Matix net: set once the opening animation has played in this process, so it shows once per launch. */
object MatixIntroState {
    @Volatile
    var shown: Boolean = false
}

/**
 * Matix net: the logo, a blue rounded square with a white "M" and a smile under it, drawn on a canvas so it
 * stays sharp at any size. With [interactive], a tap makes it spin once and bounce.
 */
@Composable
fun MatixLogo(
    size: Dp,
    modifier: Modifier = Modifier,
    interactive: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }
    val pulse = remember { Animatable(1f) }
    val glow = remember { Animatable(0f) }

    val tapModifier = if (interactive) {
        Modifier.pointerInput(Unit) {
            detectTapGestures {
                scope.launch { rotation.snapTo(0f); rotation.animateTo(360f, tween(650, easing = FastOutSlowInEasing)); rotation.snapTo(0f) }
                scope.launch {
                    pulse.animateTo(0.82f, tween(110))
                    pulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                }
                scope.launch { glow.snapTo(1f); glow.animateTo(0f, tween(900)) }
            }
        }
    } else {
        Modifier
    }

    Box(modifier = modifier.size(size).then(tapModifier), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = this.size.width
            // Soft glow ring that flares on a tap.
            if (glow.value > 0f) {
                drawCircle(
                    color = Color(0xFF4C9BFF).copy(alpha = 0.55f * glow.value),
                    radius = w * (0.55f + 0.25f * (1f - glow.value)),
                    style = Stroke(width = w * 0.05f)
                )
            }
        }
        // MatixNET: the brand blue square with the logo, a vector traced from the brand image.
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    rotationY = rotation.value
                    scaleX = pulse.value
                    scaleY = pulse.value
                }
                .clip(RoundedCornerShape(size * 0.22f))
                .background(Color(0xFF0067C6))
        ) {
            Image(
                painter = painterResource(R.drawable.ic_matixnet_fg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.3333f; scaleY = 1.3333f }
            )
        }
    }
}

/** Matix net: the opening animation, a ring that spreads from the logo while the name fades in, then it fades out. */
@Composable
fun MatixIntro(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.5f) }
    val textAlpha = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
            launch { ring.animateTo(1f, tween(1300, easing = FastOutSlowInEasing)) }
            launch { delay(250); textAlpha.animateTo(1f, tween(500)) }
        }
        delay(200)
        fade.animateTo(0f, tween(420))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .background(Color.Black)
            // Swallows touches while the animation plays.
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(300.dp)) {
            drawCircle(
                color = Color(0xFF4C9BFF).copy(alpha = (1f - ring.value) * 0.55f),
                radius = size.minDimension / 2f * (0.35f + 0.65f * ring.value),
                style = Stroke(width = 3.dp.toPx())
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MatixLogo(size = 112.dp, modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value })
            Spacer(Modifier.height(18.dp))
            Text(
                text = "MatixNET",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { alpha = textAlpha.value }
            )
        }
    }
}

/** Matix net: a country flag from assets/flags for the server named [remarks], or a blue globe when none matches. */
@Composable
fun FlagIcon(remarks: String, modifier: Modifier = Modifier, size: Dp = 34.dp) {
    val context = LocalContext.current
    val slug = remember(remarks) { FlagCatalog.slugFor(remarks) }
    val bitmap = produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, slug) {
        value = if (slug == null) null else withContext(Dispatchers.IO) {
            try {
                context.assets.open("flags/$slug.webp").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            } catch (e: Exception) {
                LogUtil.e("MatixNet", "Flag asset could not be read", e)
                null
            }
        }
    }.value
    val shape = CircleShape
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier.size(size).clip(shape)
        )
    } else {
        Canvas(modifier.size(size).clip(shape)) {
            val w = this.size.width
            drawCircle(Brush.linearGradient(listOf(Color(0xFF4C9BFF), Color(0xFF1456D9))))
            val line = Stroke(width = w * 0.05f)
            drawCircle(Color.White.copy(alpha = 0.85f), radius = w * 0.30f, style = line)
            drawOval(Color.White.copy(alpha = 0.85f), topLeft = Offset(w * 0.36f, w * 0.20f), size = androidx.compose.ui.geometry.Size(w * 0.28f, w * 0.60f), style = line)
            drawLine(Color.White.copy(alpha = 0.85f), Offset(w * 0.20f, w * 0.5f), Offset(w * 0.80f, w * 0.5f), strokeWidth = w * 0.05f)
        }
    }
}

/**
 * MatixNET: the "buy / renew subscription" button. A sky-blue gradient pill with a soft glow and a light band that
 * sweeps across it, so it stands apart from every other button of the app.
 */
@Composable
fun MatixBuyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "buy-shine")
    val sweep by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep"
    )
    val shape = RoundedCornerShape(if (compact) 18.dp else 28.dp)
    val glow = Color(0xFF2F9BFF)
    Row(
        modifier = modifier
            .shadow(if (compact) 6.dp else 12.dp, shape, ambientColor = glow, spotColor = glow)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF4FC3FF), Color(0xFF1B8BFF), Color(0xFF1456D9))))
            .border(1.dp, Color.White.copy(alpha = 0.4f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .drawWithContent {
                drawContent()
                val band = size.width * 0.25f
                val start = sweep * size.width
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.38f), Color.Transparent),
                        startX = start,
                        endX = start + band
                    ),
                    topLeft = Offset(start, 0f),
                    size = Size(band, size.height)
                )
            }
            .padding(horizontal = if (compact) 16.dp else 20.dp, vertical = if (compact) 9.dp else 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_telegram_24dp),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(if (compact) 16.dp else 22.dp)
        )
        Spacer(Modifier.width(if (compact) 6.dp else 10.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = if (compact) 13.sp else 16.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}
