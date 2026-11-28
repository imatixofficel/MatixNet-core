package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import java.time.Instant
import java.time.ZoneId

/** MatixNET: why the renew warning shows, if it does. */
enum class MatixWarning { NONE, DAYS_LEFT, LAST_DAY, TRAFFIC_LOW, TIME_EXPIRED, TRAFFIC_OVER }

/** MatixNET: locale-neutral figures of the subscription card. */
data class MatixSubscriptionStatus(
    val usedBytes: Long,
    /** 0 means unlimited. */
    val totalBytes: Long,
    val leftBytes: Long?,
    val fraction: Float?,
    val expireMillis: Long?,
    /** Whole days left, rounded down; null when there is no expiry. */
    val daysLeft: Long?,
    val warning: MatixWarning
) {
    val expired: Boolean get() = warning == MatixWarning.TIME_EXPIRED || warning == MatixWarning.TRAFFIC_OVER
}

private const val DAY_MILLIS = 86_400_000L
const val MATIX_WARN_DAYS = 3L
const val MATIX_WARN_TRAFFIC_FRACTION = 0.9f

fun buildMatixSubscriptionStatus(info: SubscriptionUsageInfo, nowMillis: Long): MatixSubscriptionStatus {
    val unlimited = info.total <= 0L
    val used = info.used
    val expireMillis = info.expire.takeIf { it > 0L }?.let { it * 1000L }
    val daysLeft = expireMillis?.let { ((it - nowMillis).coerceAtLeast(0L)) / DAY_MILLIS }
    val fraction = if (unlimited) null else (used.toDouble() / info.total).toFloat().coerceIn(0f, 1f)
    val warning = when {
        expireMillis != null && expireMillis <= nowMillis -> MatixWarning.TIME_EXPIRED
        !unlimited && used >= info.total -> MatixWarning.TRAFFIC_OVER
        fraction != null && fraction >= MATIX_WARN_TRAFFIC_FRACTION -> MatixWarning.TRAFFIC_LOW
        daysLeft != null && daysLeft < 1L -> MatixWarning.LAST_DAY
        daysLeft != null && daysLeft <= MATIX_WARN_DAYS -> MatixWarning.DAYS_LEFT
        else -> MatixWarning.NONE
    }
    return MatixSubscriptionStatus(
        usedBytes = used,
        totalBytes = if (unlimited) 0L else info.total,
        leftBytes = if (unlimited) null else (info.total - used).coerceAtLeast(0L),
        fraction = fraction,
        expireMillis = expireMillis,
        daysLeft = daysLeft,
        warning = warning
    )
}

/** Gregorian to Solar Hijri (Persian) date as year, month (1..12), day. */
fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    val gy2 = if (gm > 2) gy + 1 else gy
    var days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 + gd + gdm[gm - 1]
    var jy = -1595 + 33 * (days / 12053)
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }
    val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
    val jd = 1 + if (days < 186) days % 31 else (days - 186) % 30
    return Triple(jy, jm, jd)
}

/** The date in epoch [millis] as year, month, day of the Persian calendar, in [zone]. */
fun jalaliDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Triple<Int, Int, Int> {
    val d = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    return gregorianToJalali(d.year, d.monthValue, d.dayOfMonth)
}
