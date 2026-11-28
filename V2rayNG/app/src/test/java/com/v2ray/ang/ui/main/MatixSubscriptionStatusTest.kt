package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneOffset

class MatixSubscriptionStatusTest {
    private val now = 1_800_000_000_000L
    private val day = 86_400L
    private val gb = 1_000_000_000L

    private fun status(used: Long = 0, total: Long = 10 * gb, expireInSeconds: Long? = 30 * day) =
        buildMatixSubscriptionStatus(
            SubscriptionUsageInfo(
                download = used,
                total = total,
                expire = expireInSeconds?.let { now / 1000 + it } ?: 0
            ),
            now
        )

    @Test fun healthy_hasNoWarning() = assertEquals(MatixWarning.NONE, status().warning)
    @Test fun threeDaysLeft_warnsDays() = assertEquals(MatixWarning.DAYS_LEFT, status(expireInSeconds = 3 * day + 100).warning)
    @Test fun underOneDay_warnsLastDay() = assertEquals(MatixWarning.LAST_DAY, status(expireInSeconds = day - 10).warning)
    @Test fun pastExpiry_isExpired() = assertEquals(MatixWarning.TIME_EXPIRED, status(expireInSeconds = -5).warning)
    @Test fun ninetyPercent_warnsTraffic() = assertEquals(MatixWarning.TRAFFIC_LOW, status(used = 9 * gb).warning)
    @Test fun allUsed_isOver() = assertEquals(MatixWarning.TRAFFIC_OVER, status(used = 10 * gb).warning)
    @Test fun unlimitedNoExpiry_hasNoWarning() {
        val s = status(used = 50 * gb, total = 0, expireInSeconds = null)
        assertEquals(MatixWarning.NONE, s.warning)
        assertEquals(null, s.fraction)
        assertEquals(null, s.daysLeft)
    }

    @Test fun jalali_knownDates() {
        assertEquals(Triple(1405, 9, 7), gregorianToJalali(2026, 11, 28))
        assertEquals(Triple(1404, 1, 1), gregorianToJalali(2025, 3, 21))
        assertEquals(Triple(1402, 12, 10), gregorianToJalali(2024, 2, 29))
        assertEquals(Triple(1405, 9, 7), jalaliDate(1_795_900_000_000L, ZoneOffset.UTC))
    }
}
