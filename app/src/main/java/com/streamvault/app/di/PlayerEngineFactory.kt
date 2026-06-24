package com.streamvault.app.di

import android.content.Context
import com.streamvault.player.Media3PlayerEngine
import com.streamvault.player.PlayerEngine
import com.streamvault.player.vlc.VlcPlayerEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton
import com.streamvault.domain.model.PlayerEngineType

@Singleton
class PlayerEngineFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    fun create(type: PlayerEngineType, forAuxiliary: Boolean = false): PlayerEngine {
        val engine = when (type) {
            PlayerEngineType.EXO_PLAYER -> Media3PlayerEngine(context, okHttpClient)
            PlayerEngineType.VLC -> VlcPlayerEngine(context)
        }
        if (forAuxiliary && engine is Media3PlayerEngine) {
            engine.enableMediaSession = false
            engine.bypassAudioFocus = true
        }
        return engine
    }
}
