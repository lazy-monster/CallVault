/*
 * CallVault: FOSS call recording, self-contained over embedded ADB
 *  Copyright (C) 2026-present The CallVault Authors
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.baba.callvault.services.recording

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoipGameGuardTest {

    @Test
    fun `known games with voice chat are recognised`() {
        assertTrue(VoipGameGuard.isKnownGamePackage("com.activision.callofduty.shooter"))
        assertTrue(VoipGameGuard.isKnownGamePackage("com.garena.game.codm"))
        assertTrue(VoipGameGuard.isKnownGamePackage("com.tencent.ig"))
        assertTrue(VoipGameGuard.isKnownGamePackage("com.pubg.imobile"))
        assertTrue(VoipGameGuard.isKnownGamePackage("com.dts.freefiremax"))
    }

    @Test
    fun `calling apps are not games`() {
        assertFalse(VoipGameGuard.isKnownGamePackage("com.whatsapp"))
        assertFalse(VoipGameGuard.isKnownGamePackage("org.thoughtcrime.securesms"))
        assertFalse(VoipGameGuard.isKnownGamePackage("org.telegram.messenger"))
        assertFalse(VoipGameGuard.isKnownGamePackage("com.discord"))
        assertFalse(VoipGameGuard.isKnownGamePackage(null))
    }
}
