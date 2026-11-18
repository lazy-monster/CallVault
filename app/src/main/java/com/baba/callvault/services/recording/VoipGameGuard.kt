/*
 * CallVault: FOSS call recording, self-contained over embedded ADB
 *  Copyright (C) 2026-present The CallVault Authors
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.baba.callvault.services.recording

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build

/**
 * Keeps game voice chat out of app-call recording.
 *
 * App calls are detected from the audio mode, and in-game voice chat (Call of Duty Mobile, PUBG, Free Fire,
 * …) puts the phone in exactly the same MODE_IN_COMMUNICATION a WhatsApp call does. The per-app list is
 * stored as exclusions, so every game a user never thought to switch off was recorded as an "app call" for
 * the whole match — the recorder capturing and encoding while the game ran, then a transcription of the
 * match on the CPU afterwards. That is a heavy load on a phone that is already busy with a game.
 *
 * A game is never a call app here, whatever the per-app list says.
 */
object VoipGameGuard {

    /**
     * Games that are known to use voice chat, for builds that do not declare the game category. Prefixes,
     * so regional builds (`com.tencent.ig`, `com.pubg.imobile`, …) match too.
     */
    private val KNOWN_GAME_PREFIXES = listOf(
        "com.activision.callofduty",   // Call of Duty: Mobile, Warzone Mobile
        "com.garena.game.codm",        // CODM (Garena)
        "com.tencent.ig",              // PUBG Mobile
        "com.pubg.",                   // PUBG / BGMI
        "com.vng.pubgmobile",
        "com.rekoo.pubgm",
        "com.tencent.tmgp.",           // Tencent games (CN)
        "com.dts.freefire",            // Free Fire / Free Fire MAX
        "com.miHoYo.", "com.HoYoverse.", // Genshin, Honkai, ZZZ
        "com.mobile.legends",          // Mobile Legends
        "com.riotgames.",              // Wild Rift, VALORANT Mobile
        "com.supercell.",
        "com.ea.gp.apexlegendsmobile",
        "com.epicgames.",
        "com.roblox.",
        "com.mojang.",
    )

    /** Pure part, for tests: the prefix list alone. */
    fun isKnownGamePackage(packageName: String?): Boolean =
        packageName != null && KNOWN_GAME_PREFIXES.any { packageName.startsWith(it) }

    /** Whether [packageName] is a game: declared category, the legacy isGame flag, or the known list. */
    @Suppress("DEPRECATION")
    fun isGame(context: Context, packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (isKnownGamePackage(packageName)) return true
        val info = runCatching { context.packageManager.getApplicationInfo(packageName, 0) }.getOrNull() ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && info.category == ApplicationInfo.CATEGORY_GAME) return true
        return info.flags and ApplicationInfo.FLAG_IS_GAME != 0
    }
}
