/*
 * CallVault: FOSS call recording, self-contained over embedded ADB
 *  Copyright (C) 2026-present The CallVault Authors
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.baba.callvault.services.recording

/**
 * Serialises and throttles recorder relaunches.
 *
 * **Crash-loop backoff.** A forced relaunch (binder death) used to bypass the throttle entirely. On phones
 * whose memory manager reaps the shell-uid recorder repeatedly (Samsung's Freecess / Game Booster under load,
 * OnePlus Athena), that meant: reaped → relaunched at once → reaped again, forever. Every relaunch is an ADB
 * round trip, an `app_process` cold start and, in the worst case, Wireless debugging being switched back on —
 * which is exactly what made phones sluggish. Now any attempt that comes within [stableAfterMs] of the
 * previous one counts as a repeat, and repeats wait an exponentially growing gap (capped at [maxBackoffMs])
 * whether forced or not. The first relaunch after a recorder that stayed up, and the first repeat after it,
 * still happen immediately: a single death is recovered before the next call, a loop is not fed.
 */
class RewarmGate(
    private val stuckAfterMs: Long,
    private val throttleMs: Long,
    private val stableAfterMs: Long = DEFAULT_STABLE_AFTER_MS,
    private val firstBackoffMs: Long = DEFAULT_FIRST_BACKOFF_MS,
    private val maxBackoffMs: Long = DEFAULT_MAX_BACKOFF_MS,
) {

    private var inFlight = false
    private var startedAtMs = 0L

    // An explicit "never attempted" flag rather than a sentinel timestamp: with Long.MIN_VALUE,
    // `now - lastAttemptAtMs` overflows to a negative value and the throttle silently blocks the
    // FIRST attempt — the daemon would then never warm until the throttle window happened to pass.
    private var hasAttempted = false
    private var lastAttemptAtMs = 0L

    /** Attempts in a row that each came soon after the previous one. 0 = the recorder had been stable. */
    private var repeatStreak = 0

    @Synchronized
    fun tryEnter(now: Long, force: Boolean): Boolean {
        if (inFlight && now - startedAtMs < stuckAfterMs) return false
        if (hasAttempted) {
            val since = now - lastAttemptAtMs
            val streak = if (since >= stableAfterMs) 0 else repeatStreak + 1
            val required = maxOf(if (force) 0L else throttleMs, backoffFor(streak))
            if (since < required) return false
            repeatStreak = streak
        }
        inFlight = true
        startedAtMs = now
        hasAttempted = true
        lastAttemptAtMs = now
        return true
    }

    @Synchronized
    fun leave() {
        inFlight = false
    }

    /**
     * The gap a relaunch must wait after [streak] quick repeats. The first repeat is free (one genuine death
     * still recovers at once, before the next call); from the second on: first, doubling, capped.
     */
    internal fun backoffFor(streak: Int): Long {
        if (streak <= 1) return 0L
        val shift = (streak - 2).coerceAtMost(20)
        return (firstBackoffMs shl shift).coerceAtMost(maxBackoffMs)
    }

    companion object {
        /** A recorder that has not needed a relaunch for this long counts as healthy again. */
        const val DEFAULT_STABLE_AFTER_MS = 10 * 60_000L
        const val DEFAULT_FIRST_BACKOFF_MS = 15_000L
        const val DEFAULT_MAX_BACKOFF_MS = 15 * 60_000L
    }
}
