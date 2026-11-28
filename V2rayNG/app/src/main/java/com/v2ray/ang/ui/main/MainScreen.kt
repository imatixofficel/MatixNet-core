package com.v2ray.ang.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.AppConfig
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.extension.toTrafficString
import com.v2ray.ang.handler.MatixHomeSettings
import com.v2ray.ang.ui.compose.FlagIcon
import com.v2ray.ang.ui.compose.MatixBlueBrush
import com.v2ray.ang.ui.compose.MatixBuyButton
import com.v2ray.ang.ui.compose.MatixLogo
import com.v2ray.ang.ui.compose.glassCard
import com.v2ray.ang.util.Utils
import java.text.NumberFormat
import java.util.Locale

private val WarnAmber = Color(0xFFE59A00)
private val WarnRed = Color(0xFFE5484D)
private val OkGreen = Color(0xFF12A36B)

/**
 * MatixNET: the home screen. Only the connection button, the chosen server, the add-subscription button, the
 * subscription card with its renew warning and a small settings page. Every operation goes through [onAction].
 */
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val settings by MatixHomeSettings.state.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showAdd by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { onAction(MainAction.LoadHomeSettings) }

    val group = uiState.groups.firstOrNull { it.id == uiState.selectedGroupId } ?: uiState.groups.firstOrNull()
    val rows = if (group != null) {
        remember(group.id) { mainViewModel.serverGroupState(group.id) }.collectAsStateWithLifecycle().value.rows
    } else {
        emptyList()
    }
    val selectedRow = rows.firstOrNull { it.guid == uiState.selectedGuid }
    val usageInfo = group?.usageInfo

    if (showSettings) {
        BackHandler { showSettings = false }
        MatixSettingsPage(
            settings = settings,
            supportUrl = usageInfo?.supportUrl ?: AppConfig.MATIX_BOT_URL,
            onAction = onAction,
            onNavigate = onNavigate,
            onBack = { showSettings = false }
        )
        return
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(stringResource(R.string.matix_choose_server)) },
            text = {
                LazyColumn {
                    items(rows, key = { it.guid }) { row ->
                        val selected = row.guid == uiState.selectedGuid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    showPicker = false
                                    onAction(MainAction.SelectServer(row.guid))
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FlagIcon(row.remarks, Modifier.padding(end = 10.dp), size = 30.dp)
                            Text(row.remarks, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.action_cancel)) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(R.string.matix_add_sub)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AddOption(R.drawable.ic_paste_24dp, stringResource(R.string.matix_paste)) {
                        showAdd = false
                        onAction(MainAction.ImportClipboard)
                    }
                    AddOption(R.drawable.ic_qu_scan_24dp, stringResource(R.string.matix_scan)) {
                        showAdd = false
                        onAction(MainAction.ImportQRcode)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(stringResource(R.string.action_cancel)) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HomeHeader(onSettings = { showSettings = true })

        ConnectCard(
            isRunning = uiState.isRunning,
            connecting = uiState.isRunning && uiState.status is MainStatus.Connecting,
            detail = mainViewModel.formatStatus(uiState.status),
            serverName = selectedRow?.remarks,
            onToggle = { onAction(MainAction.ToggleService) },
            onPickServer = { if (rows.isNotEmpty()) showPicker = true }
        )

        AddServerButton(onClick = { showAdd = true })

        val status = remember(usageInfo) {
            usageInfo?.let { buildMatixSubscriptionStatus(it, System.currentTimeMillis()) }
        }
        if (status != null && status.warning != MatixWarning.NONE) {
            RenewWarning(status, onRenew = { onAction(MainAction.BuySubscription) })
        }
        if (group != null) {
            SubscriptionCard(
                title = usageInfo?.title ?: group.remarks,
                status = status,
                group = group,
                onRefresh = { onAction(MainAction.UpdateGroupSubscription(group.id)) }
            )
        }
    }
}

/** The "add server / subscription" button of the home screen: an outlined sky-blue pill. */
@Composable
private fun AddServerButton(onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(primary.copy(alpha = 0.06f))
            .border(1.5.dp, primary.copy(alpha = 0.55f), shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+  " + stringResource(R.string.matix_add_sub),
            color = primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AddOption(iconRes: Int, label: String, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(primary.copy(alpha = 0.08f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HomeHeader(onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MatixLogo(size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.weight(1f),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .size(46.dp)
                .glassCard(shape = CircleShape)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings_24dp),
                contentDescription = stringResource(R.string.matix_open_settings),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ConnectCard(
    isRunning: Boolean,
    connecting: Boolean,
    detail: String,
    serverName: String?,
    onToggle: () -> Unit,
    onPickServer: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 18.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PowerButton(isRunning = isRunning, connecting = connecting, onClick = onToggle)
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(if (isRunning) R.string.matix_connected else R.string.matix_not_connected),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isRunning) OkGreen else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isRunning) detail else stringResource(R.string.matix_tap_to_connect),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .clickable(onClick = onPickServer)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlagIcon(serverName.orEmpty(), Modifier.padding(end = 12.dp), size = 38.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = serverName ?: stringResource(R.string.matix_no_server),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.matix_choose_server),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(if (isRunning) R.string.matix_status_connected else R.string.matix_status_disconnected),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRunning) OkGreen else WarnRed
            )
        }
    }
}

/**
 * The round connect button. A glowing sky-blue disc with soft halos that breathe while connected, and a ring of light
 * that spins around it while the tunnel is still on its way up.
 */
@Composable
private fun PowerButton(isRunning: Boolean, connecting: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "power")
    val breathe by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )
    val primary = MaterialTheme.colorScheme.primary
    val label = stringResource(if (isRunning) R.string.acc_stop else R.string.acc_start)
    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(220.dp)
                .scale(if (isRunning) breathe else 1f)
                .clip(CircleShape)
                .background(primary.copy(alpha = if (isRunning) 0.09f else 0.05f))
        )
        Box(
            Modifier
                .size(182.dp)
                .scale(if (isRunning) 1f + (breathe - 1f) * 0.6f else 1f)
                .clip(CircleShape)
                .background(primary.copy(alpha = if (isRunning) 0.15f else 0.08f))
        )
        if (connecting) {
            Canvas(
                Modifier
                    .size(166.dp)
                    .graphicsLayer { rotationZ = spin }
            ) {
                val width = 5.dp.toPx()
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color.Transparent, primary)),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(width / 2f, width / 2f),
                    size = Size(size.width - width, size.height - width),
                    style = Stroke(width = width, cap = StrokeCap.Round)
                )
            }
        }
        Box(
            modifier = Modifier
                .size(140.dp)
                .shadow(
                    elevation = if (isRunning) 22.dp else 10.dp,
                    shape = CircleShape,
                    ambientColor = primary,
                    spotColor = primary
                )
                .clip(CircleShape)
                .background(
                    if (isRunning) MatixBlueBrush
                    else Brush.verticalGradient(listOf(Color.White, Color(0xFFE6EEFF)))
                )
                .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            val tint = if (isRunning) Color.White else primary
            Canvas(Modifier.size(56.dp)) {
                val stroke = Stroke(width = size.width * 0.11f, cap = StrokeCap.Round)
                val inset = stroke.width / 2f
                drawArc(
                    color = tint,
                    startAngle = -60f,
                    sweepAngle = 300f,
                    useCenter = false,
                    topLeft = Offset(inset, inset + size.height * 0.06f),
                    size = Size(size.width - stroke.width, size.height - stroke.width - size.height * 0.06f),
                    style = stroke
                )
                drawLine(
                    color = tint,
                    start = Offset(size.width / 2f, inset),
                    end = Offset(size.width / 2f, size.height * 0.48f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun RenewWarning(status: MatixSubscriptionStatus, onRenew: () -> Unit) {
    val message = when (status.warning) {
        MatixWarning.DAYS_LEFT -> stringResource(R.string.matix_warn_days, (status.daysLeft ?: 0L).toInt())
        MatixWarning.LAST_DAY -> stringResource(R.string.matix_warn_last_day)
        MatixWarning.TRAFFIC_LOW -> stringResource(R.string.matix_warn_traffic)
        MatixWarning.TIME_EXPIRED -> stringResource(R.string.matix_warn_expired)
        MatixWarning.TRAFFIC_OVER -> stringResource(R.string.matix_warn_traffic_over)
        MatixWarning.NONE -> return
    }
    val tone = if (status.expired) WarnRed else WarnAmber
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(tone.copy(alpha = 0.14f))
            .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = tone
        )
        MatixBuyButton(text = stringResource(R.string.matix_renew), onClick = onRenew, compact = true)
    }
}

@Composable
private fun SubscriptionCard(
    title: String,
    status: MatixSubscriptionStatus?,
    group: GroupMapItem,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.matix_subscription),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (status != null) {
                val tone = if (status.expired) WarnRed else OkGreen
                Text(
                    text = stringResource(if (status.expired) R.string.matix_sub_expired else R.string.matix_sub_active),
                    color = tone,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(tone.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = onRefresh, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh_24dp),
                    contentDescription = stringResource(R.string.matix_sub_update) + " " + group.remarks,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (status == null) return@Column

        Spacer(Modifier.height(12.dp))
        val totalText = if (status.totalBytes > 0L) status.totalBytes.toTrafficString() else stringResource(R.string.matix_unlimited)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.matix_traffic_used_of, status.usedBytes.toTrafficString(), totalText),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            status.leftBytes?.let {
                Text(
                    text = stringResource(R.string.matix_traffic_left, it.toTrafficString()),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        status.fraction?.let { fraction ->
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = when {
                    fraction >= 1f -> WarnRed
                    fraction >= MATIX_WARN_TRAFFIC_FRACTION -> WarnAmber
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val timeLeftText = when {
                status.daysLeft == null -> stringResource(R.string.matix_unlimited)
                status.daysLeft < 1L -> stringResource(R.string.matix_less_than_a_day)
                else -> stringResource(R.string.matix_days_left, status.daysLeft.toInt())
            }
            InfoTile(
                label = stringResource(R.string.matix_time_left),
                value = timeLeftText,
                valueColor = if (status.expired || (status.daysLeft != null && status.daysLeft <= MATIX_WARN_DAYS)) WarnRed
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            InfoTile(
                label = stringResource(R.string.matix_expiry_date),
                value = status.expireMillis?.let { formatExpiry(it) } ?: stringResource(R.string.matix_unlimited),
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(14.dp)
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The expiry date: Persian calendar with Persian digits in Persian, else a plain yyyy-MM-dd. */
@Composable
private fun formatExpiry(millis: Long): String {
    val language = LocalConfiguration.current.locales[0]?.language
    val months = stringArrayResource(R.array.matix_jalali_months)
    return if (language == "fa") {
        val (y, m, d) = jalaliDate(millis)
        val nf = NumberFormat.getInstance(Locale.forLanguageTag("fa")).apply { isGroupingUsed = false }
        "${nf.format(d)} ${months[(m - 1).coerceIn(0, 11)]} ${nf.format(y)}"
    } else {
        Utils.formatTimestamp(millis, "yyyy-MM-dd")
    }
}

@Composable
private fun MatixSettingsPage(
    settings: com.v2ray.ang.handler.MatixHomeSettingsState,
    supportUrl: String,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back_24dp), contentDescription = stringResource(R.string.matix_back))
            }
            Text(
                text = stringResource(R.string.matix_settings),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        SectionTitle(stringResource(R.string.matix_section_connection))
        Column(Modifier.fillMaxWidth().glassCard().padding(vertical = 6.dp)) {
            SwitchRow(R.drawable.ic_routing_24dp, R.string.matix_bypass_iran, R.string.matix_bypass_iran_desc, settings.bypassIran) {
                onAction(MainAction.SetBypassIran(it))
            }
            ActionRow(R.drawable.ic_per_apps_24dp, stringResource(R.string.matix_per_app), stringResource(R.string.matix_per_app_desc)) {
                onNavigate(MainDestination.PerAppProxy)
            }
            SwitchRow(R.drawable.ic_refresh_24dp, R.string.matix_auto_fastest, R.string.matix_auto_fastest_desc, settings.autoFastest) {
                onAction(MainAction.SetAutoFastest(it))
            }
            SwitchRow(R.drawable.ic_play_24dp, R.string.matix_auto_start, R.string.matix_auto_start_desc, settings.autoStart) {
                onAction(MainAction.SetAutoStart(it))
            }
            ActionRow(R.drawable.ic_settings_24dp, stringResource(R.string.matix_always_on), stringResource(R.string.matix_always_on_desc)) {
                onAction(MainAction.OpenAlwaysOnVpnSettings)
            }
        }

        SectionTitle(stringResource(R.string.matix_section_support))
        Column(Modifier.fillMaxWidth().glassCard().padding(vertical = 6.dp)) {
            ActionRow(
                R.drawable.ic_telegram_24dp,
                stringResource(R.string.matix_buy_renew),
                stringResource(R.string.matix_buy_renew_desc, AppConfig.MATIX_BOT_ID)
            ) { onAction(MainAction.BuySubscription) }
            ActionRow(R.drawable.ic_feedback_24dp, stringResource(R.string.matix_support), supportUrl.removePrefix("https://")) {
                onAction(MainAction.OpenSubscriptionLink(supportUrl))
            }
        }
        Text(
            text = stringResource(R.string.matix_version, BuildConfig.VERSION_NAME),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 6.dp),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RowIcon(iconRes: Int) {
    Box(
        Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SwitchRow(iconRes: Int, titleRes: Int, descRes: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    val title = stringResource(titleRes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowIcon(iconRes)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(descRes), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ActionRow(iconRes: Int, title: String, description: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowIcon(iconRes)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
