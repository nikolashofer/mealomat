package com.example.mealomat.testing

import kotlin.time.Clock
import kotlin.time.Instant

// for deterministic `updated_at`. advance with `clock.now += 1000`
class FixedClock(var now: Long = 1_700_000_000_000L) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(now)
}
