package com.streamvault.player.playback

import android.os.Build
import java.util.Locale

interface PlaybackCompatibilityProfile {
    fun shouldDisableDecoderReuseWorkaround(): Boolean
}

object DefaultPlaybackCompatibilityProfile : PlaybackCompatibilityProfile {
    override fun shouldDisableDecoderReuseWorkaround(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase(Locale.ROOT)
        val hardware = Build.HARDWARE.lowercase(Locale.ROOT)
        val model = Build.MODEL.lowercase(Locale.ROOT)
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        
        return fingerprint.contains("generic") ||
            fingerprint.contains("emulator") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu") ||
            model.contains("android sdk built for") ||
            hardware.contains("amlogic") ||
            hardware.contains("rockchip") ||
            hardware.contains("sun50iw") || // Allwinner
            hardware.contains("mt5") || // Mediatek TV
            manufacturer.contains("amazon") || // Firestick
            model.contains("chromecast") ||
            contextIsTvContext() // Fallback generic TV assumption. Actually we can just return true for most IPTV setups.
    }
    
    // Simplification for the implementation:
    // Just return true to ensure codecs start clean on every channel switch for all devices.
    private fun contextIsTvContext(): Boolean = true

}

