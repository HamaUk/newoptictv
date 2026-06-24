package com.streamvault.player.vlc

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.streamvault.domain.model.StreamInfo
import com.streamvault.domain.model.DecoderMode
import com.streamvault.player.PlaybackState
import com.streamvault.player.PlayerEngine
import com.streamvault.player.PlayerError
import com.streamvault.player.PlayerRetryStatus
import com.streamvault.domain.model.VideoFormat
import com.streamvault.player.PlayerStats
import com.streamvault.player.PlayerTrack
import com.streamvault.player.PlayerSubtitleStyle
import com.streamvault.player.PlayerRenderSurfaceType
import com.streamvault.player.PlayerSurfaceResizeMode
import com.streamvault.player.timeshift.LiveTimeshiftState
import com.streamvault.player.timeshift.TimeshiftConfig

class VlcPlayerEngine(
    private val context: Context
) : PlayerEngine, MediaPlayer.EventListener {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    
    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    override val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _videoFormat = MutableStateFlow(VideoFormat(0, 0))
    override val videoFormat: StateFlow<VideoFormat> = _videoFormat.asStateFlow()

    private val _error = MutableSharedFlow<PlayerError?>(replay = 1)
    override val error: Flow<PlayerError?> = _error

    private val _retryStatus = MutableStateFlow<PlayerRetryStatus?>(null)
    override val retryStatus: StateFlow<PlayerRetryStatus?> = _retryStatus.asStateFlow()

    private val _playerStats = MutableStateFlow(PlayerStats())
    override val playerStats: StateFlow<PlayerStats> = _playerStats.asStateFlow()

    private val _availableAudioTracks = MutableStateFlow<List<PlayerTrack>>(emptyList())
    override val availableAudioTracks: StateFlow<List<PlayerTrack>> = _availableAudioTracks.asStateFlow()

    private val _availableSubtitleTracks = MutableStateFlow<List<PlayerTrack>>(emptyList())
    override val availableSubtitleTracks: StateFlow<List<PlayerTrack>> = _availableSubtitleTracks.asStateFlow()

    private val _availableVideoTracks = MutableStateFlow<List<PlayerTrack>>(emptyList())
    override val availableVideoTracks: StateFlow<List<PlayerTrack>> = _availableVideoTracks.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    override val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _timeshiftState = MutableStateFlow<LiveTimeshiftState>(LiveTimeshiftState())
    override val timeshiftState: StateFlow<LiveTimeshiftState> = _timeshiftState.asStateFlow()

    private val _mediaTitle = MutableStateFlow<String?>(null)
    override val mediaTitle: StateFlow<String?> = _mediaTitle.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    override val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _audioFocusDenied = MutableSharedFlow<Unit>()
    override val audioFocusDenied: Flow<Unit> = _audioFocusDenied

    private var activeDecoderMode: DecoderMode = DecoderMode.HARDWARE
    private var renderView: View? = null

    init {
        val options = arrayListOf("-vvv")
        libVLC = LibVLC(context, options)
        mediaPlayer = MediaPlayer(libVLC)
        mediaPlayer?.setEventListener(this)
    }

    override fun prepare(streamInfo: StreamInfo) {
        val media = Media(libVLC, Uri.parse(streamInfo.url))
        
        if (activeDecoderMode == DecoderMode.SOFTWARE) {
            media.setHWDecoderEnabled(false, false)
        } else {
            media.setHWDecoderEnabled(true, false)
        }

        mediaPlayer?.media = media
        media.release()
        _playbackState.value = PlaybackState.BUFFERING
        mediaPlayer?.play()
    }

    override fun renewStreamUrl(streamInfo: StreamInfo) {
        val pos = mediaPlayer?.time ?: 0L
        prepare(streamInfo)
        mediaPlayer?.time = pos
    }

    override fun play() { mediaPlayer?.play() }
    override fun pause() { mediaPlayer?.pause() }
    override fun stop() { mediaPlayer?.stop() }
    override fun seekTo(positionMs: Long) { mediaPlayer?.time = positionMs }
    override fun seekForward(ms: Long) { mediaPlayer?.let { it.time = it.time + ms } }
    override fun seekBackward(ms: Long) { mediaPlayer?.let { it.time = it.time - ms } }
    
    override fun setDecoderMode(mode: DecoderMode) {
        activeDecoderMode = mode
    }

    override fun setMediaSessionEnabled(enabled: Boolean) {}

    override fun setVolume(volume: Float) {
        mediaPlayer?.volume = (volume * 100).toInt()
        _isMuted.value = volume == 0f
    }

    override fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        if (muted) mediaPlayer?.volume = 0 else mediaPlayer?.volume = 100
    }

    override fun setPlaybackSpeed(speed: Float) {
        mediaPlayer?.rate = speed
        _playbackSpeed.value = speed
    }

    override fun startLiveTimeshift(streamInfo: StreamInfo, channelKey: String, config: TimeshiftConfig) {}
    override fun stopLiveTimeshift() {}
    override fun seekToLiveEdge() {}
    override fun pauseTimeshift() {}
    override fun resumeTimeshift() {}
    override fun setPreferredAudioLanguage(languageTag: String?) {}
    override fun setSubtitleStyle(style: PlayerSubtitleStyle) {}
    override fun setNetworkQualityPreferences(wifiMaxHeight: Int?, ethernetMaxHeight: Int?) {}

    override fun selectAudioTrack(trackId: String) {
        mediaPlayer?.setAudioTrack(trackId.toIntOrNull() ?: -1)
    }

    override fun selectVideoTrack(trackId: String) {}

    override fun selectSubtitleTrack(trackId: String?) {
        mediaPlayer?.setSpuTrack(trackId?.toIntOrNull() ?: -1)
    }

    override fun release() {
        mediaPlayer?.release()
        libVLC?.release()
    }

    override fun toggleMute() {
        setMuted(!_isMuted.value)
    }

    override fun setScrubbingMode(enabled: Boolean) {}
    override fun preload(streamInfo: StreamInfo?) {}

    override fun createRenderView(context: Context, resizeMode: PlayerSurfaceResizeMode, surfaceType: PlayerRenderSurfaceType): View {
        return SurfaceView(context).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    override fun bindRenderView(renderView: View, resizeMode: PlayerSurfaceResizeMode) {
        this.renderView = renderView
        if (renderView is SurfaceView) {
            mediaPlayer?.vlcVout?.setVideoView(renderView)
            mediaPlayer?.vlcVout?.attachViews()
        } else if (renderView is TextureView) {
            mediaPlayer?.vlcVout?.setVideoView(renderView)
            mediaPlayer?.vlcVout?.attachViews()
        }
    }

    override fun clearRenderBinding() {
        mediaPlayer?.vlcVout?.detachViews()
        this.renderView = null
    }

    override fun releaseRenderView(renderView: View) {
        if (this.renderView == renderView) {
            clearRenderBinding()
        }
    }

    override fun onEvent(event: MediaPlayer.Event?) {
        when (event?.type) {
            MediaPlayer.Event.Playing -> {
                _playbackState.value = PlaybackState.READY
                _isPlaying.value = true
                val w = mediaPlayer?.vlcVout?.let { if (it.areViewsAttached()) 1920 else 0 } ?: 0
                _videoFormat.value = VideoFormat(0, 0) // We would read real format here
            }
            MediaPlayer.Event.Paused -> _isPlaying.value = false
            MediaPlayer.Event.Stopped -> _playbackState.value = PlaybackState.ENDED
            MediaPlayer.Event.EncounteredError -> _playbackState.value = PlaybackState.ERROR
            MediaPlayer.Event.TimeChanged -> _currentPosition.value = event.timeChanged
            MediaPlayer.Event.LengthChanged -> _duration.value = event.lengthChanged
        }
    }
}
