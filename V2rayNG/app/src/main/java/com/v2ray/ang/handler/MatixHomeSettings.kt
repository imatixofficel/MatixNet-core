package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.RulesetItem
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** MatixNET: what the settings page of the home screen shows. */
data class MatixHomeSettingsState(
    val bypassIran: Boolean = false,
    val autoFastest: Boolean = false,
    val autoStart: Boolean = false
)

/**
 * MatixNET: the few switches of the home screen's settings page. They write the same storage the full settings
 * write, so both always agree. The Iranian bypass is one routing rule, found by its id, that goes direct.
 */
object MatixHomeSettings {
    const val IRAN_RULE_ID = "matix_iran_bypass"
    const val PREF_AUTO_FASTEST = "pref_matix_auto_fastest"

    private val _state = MutableStateFlow(MatixHomeSettingsState())
    val state: StateFlow<MatixHomeSettingsState> = _state.asStateFlow()

    /** Reads the switches from storage; call from a background thread. */
    fun refresh() {
        _state.value = MatixHomeSettingsState(
            bypassIran = isIranBypassOn(),
            autoFastest = MmkvManager.decodeSettingsBool(PREF_AUTO_FASTEST, false),
            autoStart = MmkvManager.decodeStartOnBoot()
        )
    }

    private fun isIranBypassOn(): Boolean =
        MmkvManager.decodeRoutingRulesets()?.any { it.id == IRAN_RULE_ID && it.enabled } == true

    /** Adds the direct rule for Iranian sites at the top of the routing rules, or removes it; true when stored. */
    fun setBypassIran(enabled: Boolean): Boolean {
        return try {
            val stored = SettingsManager.changeRoutingRulesets {
                val list = MmkvManager.decodeRoutingRulesets() ?: mutableListOf()
                list.removeAll { it.id == IRAN_RULE_ID }
                if (enabled) {
                    list.add(
                        0,
                        RulesetItem(
                            id = IRAN_RULE_ID,
                            remarks = "MatixNET Iran",
                            ip = listOf("geoip:ir"),
                            domain = listOf("regexp:.*\\.ir$", "geosite:category-ir"),
                            outboundTag = AppConfig.TAG_DIRECT,
                            enabled = true
                        )
                    )
                }
                MmkvManager.encodeRoutingRulesets(list)
            }
            if (stored) _state.update { it.copy(bypassIran = enabled) }
            stored
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "MatixNET: the Iranian bypass rule could not be saved", e)
            false
        }
    }

    fun setAutoFastest(enabled: Boolean) {
        MmkvManager.encodeSettings(PREF_AUTO_FASTEST, enabled)
        _state.update { it.copy(autoFastest = enabled) }
    }

    fun setAutoStart(enabled: Boolean) {
        MmkvManager.encodeStartOnBoot(enabled)
        _state.update { it.copy(autoStart = enabled) }
    }

    /** After a ping test of [subscriptionId]: with the switch on, the quickest working server becomes the selected one. */
    fun selectFastest(subscriptionId: String) {
        if (!MmkvManager.decodeSettingsBool(PREF_AUTO_FASTEST, false)) return
        try {
            val best = MmkvManager.decodeServerList(subscriptionId)
                .mapNotNull { guid ->
                    val delay = MmkvManager.decodeServerAffiliationInfo(guid)?.testDelayMillis ?: 0L
                    if (delay > 0L) guid to delay else null
                }
                .minByOrNull { it.second }
                ?.first ?: return
            MmkvManager.setSelectServer(best)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "MatixNET: the fastest server could not be selected", e)
        }
    }
}
