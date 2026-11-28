package com.v2ray.ang.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.extension.toast
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MatixAppSettings
import com.v2ray.ang.ui.base.HelperBaseComponentActivity
import com.v2ray.ang.ui.compose.MatixBlueBrush
import com.v2ray.ang.ui.compose.MatixBuyButton
import com.v2ray.ang.ui.compose.MatixIntro
import com.v2ray.ang.ui.compose.MatixIntroState
import com.v2ray.ang.ui.compose.MatixLogo
import com.v2ray.ang.ui.compose.glassCard
import com.v2ray.ang.ui.compose.matixBackdrop
import com.v2ray.ang.ui.main.MainActivity
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MatixNET: the first screen. The person pastes or scans a subscription link, or buys one from the Telegram bot.
 * It is shown whenever there is no subscription or server; adding one opens the main screen, which cannot be
 * entered without one.
 */
class WelcomeActivity : HelperBaseComponentActivity() {

    // Owned by the activity: an import in flight, so a second tap on the button does nothing.
    private var busy by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        BackHandler { moveTaskToBack(false) }
        var introDone by remember { mutableStateOf(MatixIntroState.shown) }
        Box(Modifier.fillMaxSize().matixBackdrop()) {
            WelcomeScreen(
                busy = busy,
                onPaste = { runCatching { Utils.getClipboard(this@WelcomeActivity) }.getOrDefault("") },
                onScan = { onResult ->
                    launchQRCodeScanner { result -> if (!result.isNullOrBlank()) onResult(result) }
                },
                onAdd = ::addSubscription,
                onBuy = { Utils.openUri(this@WelcomeActivity, AppConfig.MATIX_BOT_URL) }
            )
            if (!introDone) {
                MatixIntro(onFinished = { MatixIntroState.shown = true; introDone = true })
            }
        }
    }

    private fun addSubscription(raw: String) {
        val text = raw.trim()
        if (text.isEmpty() || busy) return
        busy = true
        lifecycleScope.launch {
            val imported = withContext(Dispatchers.IO) {
                try {
                    val (profiles, subscriptions) = AngConfigManager.importBatchConfig(text, "", false)
                    profiles > 0 || subscriptions > 0
                } catch (e: Exception) {
                    LogUtil.e(AppConfig.TAG, "Welcome screen: the subscription could not be imported", e)
                    false
                }
            }
            busy = false
            if (imported) {
                toast(R.string.import_subscription_success)
                openMain(imported = true)
            } else {
                toast(R.string.matix_sub_failed)
            }
        }
    }

    private fun openMain(imported: Boolean) {
        MatixAppSettings.markWelcomeDone()
        startActivity(Intent(this, MainActivity::class.java).apply {
            if (imported) putExtra(AppConfig.EXTRA_REFRESH_AFTER_IMPORT, true)
        })
        finish()
    }
}

@Composable
private fun WelcomeScreen(
    busy: Boolean,
    onPaste: () -> String,
    onScan: ((String) -> Unit) -> Unit,
    onAdd: (String) -> Unit,
    onBuy: () -> Unit
) {
    var link by rememberSaveable { mutableStateOf("") }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))
        // «به نام پروردگار» comes first, at the very top of the screen, above the icon.
        Text(
            text = stringResource(R.string.matix_bismillah),
            color = primary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        MatixLogo(
            size = 108.dp,
            interactive = true,
            modifier = Modifier.shadow(
                18.dp,
                RoundedCornerShape(24.dp),
                ambientColor = primary,
                spotColor = primary
            )
        )
        Spacer(Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.matix_welcome),
            color = onSurface,
            fontSize = 27.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.matix_welcome_sub),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(26.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard()
                .padding(18.dp)
        ) {
            Text(
                text = stringResource(R.string.matix_sub_link),
                color = onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = TextStyle(textDirection = TextDirection.Ltr, fontSize = 14.sp),
                placeholder = {
                    Text(
                        "https://…",
                        style = TextStyle(textDirection = TextDirection.Ltr),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                leadingIcon = {
                    IconButton(onClick = { val pasted = onPaste(); if (pasted.isNotBlank()) link = pasted.trim() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_paste_24dp),
                            contentDescription = stringResource(R.string.matix_paste),
                            tint = primary
                        )
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { onScan { scanned -> link = scanned.trim() } }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_qu_scan_24dp),
                            contentDescription = stringResource(R.string.matix_scan),
                            tint = primary
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primary,
                    unfocusedBorderColor = primary.copy(alpha = 0.30f),
                    focusedContainerColor = primary.copy(alpha = 0.06f),
                    unfocusedContainerColor = primary.copy(alpha = 0.04f)
                )
            )
            Spacer(Modifier.height(14.dp))
            val enabled = link.isNotBlank() && !busy
            BlueButton(
                text = stringResource(if (busy) R.string.matix_adding else R.string.matix_add_sub),
                enabled = enabled,
                loading = busy,
                onClick = { onAdd(link) }
            )
        }

        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = stringResource(R.string.matix_or),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        }
        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(accent = true)
                .padding(18.dp)
        ) {
            Text(
                text = stringResource(R.string.matix_no_sub_title),
                color = onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.matix_no_sub_body, AppConfig.MATIX_BOT_ID),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(14.dp))
            MatixBuyButton(
                text = stringResource(R.string.matix_buy_telegram),
                onClick = onBuy,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = AppConfig.MATIX_BOT_ID,
                color = primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onBuy)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                style = TextStyle(textDirection = TextDirection.Ltr)
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "MatixNET VPN",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

/** The "buy subscription" pill of the top right corner. */
@Composable
internal fun BuyChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MatixBlueBrush)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_telegram_24dp),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = stringResource(R.string.matix_buy_sub),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun BlueButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    iconRes: Int? = null
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(MatixBlueBrush, alpha = if (enabled) 1f else 0.38f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(Modifier.width(10.dp))
            } else if (iconRes != null) {
                Icon(painterResource(iconRes), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text = text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
