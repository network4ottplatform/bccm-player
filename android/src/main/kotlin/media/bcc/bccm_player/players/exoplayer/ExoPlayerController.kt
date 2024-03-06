package media.bcc.bccm_player.players.exoplayer

import android.content.Context
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.npaw.youbora.lib6.media3.Media3Adapter
import com.npaw.youbora.lib6.plugin.Options
import com.npaw.youbora.lib6.plugin.Plugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import media.bcc.bccm_player.BccmPlayerPluginSingleton
import media.bcc.bccm_player.Downloader
import media.bcc.bccm_player.pigeon.PlaybackPlatformApi
import media.bcc.bccm_player.pigeon.PlaybackPlatformApi.BufferMode
import media.bcc.bccm_player.pigeon.PlaybackPlatformApi.NpawConfig
import media.bcc.bccm_player.players.PlayerController
import media.bcc.bccm_player.players.chromecast.CastMediaItemConverter.Companion.PLAYER_DATA_IS_LIVE
import media.bcc.bccm_player.players.chromecast.CastMediaItemConverter.Companion.PLAYER_DATA_IS_OFFLINE
import java.util.UUID
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaMetadata
import media.bcc.bccm_player.players.chromecast.CastMediaItemConverter.Companion.PLAYER_DATA_MIME_TYPE
import media.bcc.bccm_player.players.chromecast.CastMediaItemConverter.Companion.BCCM_META_EXTRAS
import androidx.media3.common.util.Util
import androidx.core.net.toUri
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.DefaultDashChunkSource
import androidx.media3.datasource.DefaultDataSource

class ExoPlayerController(
    private val context: Context,
    bufferMode: BufferMode,
    private val disableNpaw: Boolean = false
) :
    PlayerController() {
    override val id: String = UUID.randomUUID().toString()
    private val trackSelector: DefaultTrackSelector = DefaultTrackSelector(context)
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setTrackSelector(trackSelector)
        .setAudioAttributes(AudioAttributes.DEFAULT, true)
        .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context).setDataSourceFactory(
                CacheDataSource.Factory()
                    .setCache(Downloader.getCache(context))
                    .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
                    .setCacheWriteDataSinkFactory(null)
            )
        )
        .setLoadControl(getLoadControlForBufferMode(bufferMode))
        .build()

    override val player: ForwardingPlayer
    override var currentPlayerViewController: BccmPlayerViewController? = null
    private var textLanguagesThatShouldBeSelected: Array<String>? = null

    private var _currentPlayerView: PlayerView? = null
    private var currentPlayerView: PlayerView?
        get() = _currentPlayerView
        set(value) {
            _currentPlayerView = value
            if (value == null) {
                mainScope.launch {
                    delay(6000)
                    if (_currentPlayerView == null) {
                        setForceLowestVideoBitrate(true)
                        val activity = BccmPlayerPluginSingleton.activityState.value
                        Log.d("bccm", "FLAG_KEEP_SCREEN_ON cleared")
                        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            } else {
                Log.d("bccm", "Enabling video, playerView attached")
                setForceLowestVideoBitrate(false)
                val activity = BccmPlayerPluginSingleton.activityState.value
                Log.d("bccm", "FLAG_KEEP_SCREEN_ON added")
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var youboraPlugin: Plugin? = null

    init {
        player = BccmForwardingPlayer(this)
        player.addListener(this)

        handleUpdatedAppConfig(BccmPlayerPluginSingleton.appConfigState.value)
        BccmPlayerPluginSingleton.npawConfigState.value?.let {
            handleUpdatedNpawConfig(it)
        }
        mainScope.launch {
            BccmPlayerPluginSingleton.npawConfigState.collect {
                handleUpdatedNpawConfig(it)
            }
        }
        mainScope.launch {
            BccmPlayerPluginSingleton.appConfigState.collect { handleUpdatedAppConfig(it) }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            // Re-initialize player at the current live window default position.
            // https://exoplayer.dev/live-streaming.html#behindlivewindowexception-and-error_code_behind_live_window
            player.seekToDefaultPosition();
            player.prepare();
        } else {
            // Handle other errors.
        }
    }

    override fun stop(reset: Boolean) {
        player.stop()
        if (reset) {
            player.clearMediaItems()
        }
    }

    private fun handleUpdatedAppConfig(appConfigState: PlaybackPlatformApi.AppConfig?) {
        Log.d(
            "bccm",
            "setting preferred audio and sub lang to: ${appConfigState?.audioLanguages}, ${appConfigState?.subtitleLanguages}"
        )
        val audioLanguages = appConfigState?.audioLanguages?.toTypedArray() ?: arrayOf();
        player.trackSelectionParameters = trackSelector.parameters.buildUpon()
            .setPreferredAudioLanguages(*(audioLanguages))
            .build()

        textLanguagesThatShouldBeSelected =
            appConfigState?.subtitleLanguages?.toTypedArray()
        updateYouboraOptions()
    }

    private fun handleUpdatedNpawConfig(npawConfig: NpawConfig?) {
        if (npawConfig == null) {
            youboraPlugin?.disable()
            return
        }
        if (youboraPlugin != null) {
            youboraPlugin?.enable()
            return
        }
        initYoubora(npawConfig)
    }

    private fun initYoubora(config: NpawConfig) {
        if (disableNpaw) {
            Log.d("bccm", "ExoPlayerController: Youbora is disabled")
            return
        }
        Log.d("bccm", "ExoPlayerController: Initializing youbora")
        val options = Options()
        options.isAutoDetectBackground = false
        options.userObfuscateIp = true
        options.isParseManifest = true
        options.isEnabled = true
        options.accountCode = config.accountCode
        options.appReleaseVersion = config.appReleaseVersion
        options.appName = config.appName
        options.deviceIsAnonymous = config.deviceIsAnonymous ?: false
        youboraPlugin = Plugin(options, context).also {
            it.adapter = Media3Adapter(exoPlayer)
        }
        updateYouboraOptions()
    }

    fun updateYouboraOptions() {
        val youboraPlugin = youboraPlugin ?: return
        Log.d(
            "bccm",
            "ExoPlayerController: Updating youbora options: ${player.mediaMetadata.title}"
        )

        // App config based options
        val appConfig = BccmPlayerPluginSingleton.appConfigState.value
        youboraPlugin.options.username = appConfig?.analyticsId
        youboraPlugin.options.contentCustomDimension1 =
            if (appConfig?.sessionId != null) appConfig.sessionId.toString() else null

        // Metadata options
        val mediaMetadata = player.mediaMetadata
        val extras = mediaMetadata.extras?.let { extractExtrasFromAndroid(it) }
        youboraPlugin.options.contentIsLive =
            extras?.get("npaw.content.isLive")?.toBooleanStrictOrNull()
                ?: player.mediaMetadata.extras?.getString(PLAYER_DATA_IS_LIVE)
                    ?.toBooleanStrictOrNull()
                        ?: player.isCurrentMediaItemLive
        youboraPlugin.options.contentId = extras?.get("npaw.content.id")
            ?: mediaMetadata.extras?.getString("id")
        youboraPlugin.options.contentTitle = extras?.get("npaw.content.title")
            ?: mediaMetadata.title?.toString() ?: mediaMetadata.displayTitle?.toString()
        youboraPlugin.options.contentTvShow = extras?.get("npaw.content.tvShow")
        youboraPlugin.options.contentSeason = extras?.get("npaw.content.season")
        youboraPlugin.options.contentEpisodeTitle = extras?.get("npaw.content.episodeTitle")
        youboraPlugin.options.isOffline =
            extras?.get("npaw.isOffline")?.toBooleanStrictOrNull()
                ?: player.mediaMetadata.extras?.getString(PLAYER_DATA_IS_OFFLINE)
                    ?.toBooleanStrictOrNull() ?: false
        youboraPlugin.options.contentType = extras?.get("npaw.content.type")

        for (t in player.currentTracks.groups) {
            if (!t.isSelected) continue

            if (t.type == C.TRACK_TYPE_TEXT) {
                youboraPlugin.options?.contentSubtitles = t.mediaTrackGroup.getFormat(0).language
            } else if (t.type == C.TRACK_TYPE_AUDIO) {
                youboraPlugin.options?.contentLanguage = t.mediaTrackGroup.getFormat(0).language
            }
        }
    }

    fun getExoPlayer(): ExoPlayer {
        return exoPlayer
    }

    /**
     * This function forces the lowest video bitrate to be used.
     *
     * This was originally a "setRendererDisabled" function.
     * Expected it to stop loading video segments if you disable the renderer,
     * but it doesn't. This is still an open issue: https://github.com/google/ExoPlayer/issues/9282
     */
    private fun setForceLowestVideoBitrate(force: Boolean) {
        val parametersBuilder = trackSelector.buildUponParameters()
        parametersBuilder.setForceLowestBitrate(force)

        trackSelector.setParameters(parametersBuilder)

        Log.d(
            "bccm",
            if (force) "Forcing lowest bitrate" else "No longer forcing lowest bitrate"
        )
    }

    private fun getLowestBitrateTrackIndex(trackGroup: TrackGroup): Int {
        var lowestQuality = 0
        var lowestQualityBitrate = Int.MAX_VALUE
        for (trackIndex in 0 until trackGroup.length) {
            val format = trackGroup.getFormat(trackIndex)
            if (format.bitrate < lowestQualityBitrate) {
                lowestQuality = trackIndex
                lowestQualityBitrate = format.bitrate
            }
        }
        return lowestQuality
    }

    fun takeOwnership(playerView: PlayerView, viewController: BccmPlayerViewController) {
        if (surface != null) {
            Log.w(
                "bccm",
                "takeOwnership called but the player is rendering to a custom surface. Remove the texture first with removeTexture(). Aborting."
            )
            return
        }
        if (currentPlayerView != null && currentPlayerView != playerView) {
            PlayerView.switchTargetView(player, currentPlayerView, playerView)
            currentPlayerViewController?.onOwnershipLost();
        } else {
            playerView.player = player
        }
        currentPlayerView = playerView
        currentPlayerViewController = viewController
        pluginPlayerListener?.onManualPlayerStateUpdate()
    }

    fun releasePlayerView(playerView: PlayerView) {
        if (currentPlayerView == playerView) {
            currentPlayerView = null
            currentPlayerViewController = null
        }
        pluginPlayerListener?.onManualPlayerStateUpdate()
    }

    override fun release() {
        super.release()
        mainScope.cancel()
        pluginPlayerListener?.stop()
        exoPlayer.stop()
        exoPlayer.release()
    }

    override fun onTracksChanged(tracks: Tracks) {
        // We use this callback to set the default language for the subtitles.
        // we only set the default text language once when there is no track selected already and
        // the language is available for the current track.
        val textLanguages = textLanguagesThatShouldBeSelected
        if (!textLanguages.isNullOrEmpty() && tracks.groups.any { it.type == C.TRACK_TYPE_TEXT }) {
            if (setSelectedTrackByLanguages(C.TRACK_TYPE_TEXT, textLanguages, tracks)) {
                textLanguagesThatShouldBeSelected = null
            }
        }
        if (textLanguages?.isEmpty() == true) {
            setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true);
            textLanguagesThatShouldBeSelected = null
        }
        updateYouboraOptions()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        mediaItem?.mediaMetadata?.let { onMediaMetadataChanged(it) }
    }

    override fun setMixWithOthers(mixWithOthers: Boolean) {
        exoPlayer.setAudioAttributes(AudioAttributes.DEFAULT, !mixWithOthers)
    }

    private fun getLoadControlForBufferMode(bufferMode: BufferMode): LoadControl {
        return when (bufferMode) {
            BufferMode.STANDARD -> DefaultLoadControl()
            BufferMode.FAST_START_SHORT_FORM -> DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    5000,
                    40000,
                    500,
                    2000
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        }
    }

    private val dataSourceFactory by lazy {
        DefaultDataSource.Factory(context)
    }

    override fun mapMediaItem(mediaItem: PlaybackPlatformApi.MediaItem): MediaItem {
        Log.d("kxc-bccm", "mapMediaItem called on ExoPlayerController")
        val metaBuilder = MediaMetadata.Builder()
        val exoExtras = Bundle()

        if (mediaItem.metadata?.artworkUri != null) {
            metaBuilder.setArtworkUri(Uri.parse(mediaItem.metadata?.artworkUri))
        }

        val mimeType = mediaItem.mimeType ?: "application/x-mpegURL"
        exoExtras.putString(PLAYER_DATA_MIME_TYPE, mimeType)

        if (mediaItem.isLive == true) {
            exoExtras.putString(PLAYER_DATA_IS_LIVE, "true")
        }
        if (mediaItem.isOffline == true) {
            exoExtras.putString(PLAYER_DATA_IS_OFFLINE, "true")
        }

        val sourceExtra = mediaItem.metadata?.extras
        if (sourceExtra != null) {
            for (extra in sourceExtra) {
                (extra.value as? String?).let {
                    exoExtras.putString(BCCM_META_EXTRAS + "." + extra.key, it)
                }
            }
        }

        metaBuilder
            .setTitle(mediaItem.metadata?.title)
            .setArtist(mediaItem.metadata?.artist)
            .setExtras(exoExtras).build()

        return when (val type = Util.inferContentType(mediaItem.url!!)) {
            C.CONTENT_TYPE_DASH -> {
                val drmConfigurationBuilder = MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                val requestHeaders = mutableMapOf<String, String>()
                mediaItem.drm?.let {
                    if (it.headers != null) {
                        for (item in it.headers!!) { requestHeaders[item.key] = item.value }
                    }
                    drmConfigurationBuilder
                        .setLicenseUri(it.licenseUrl!!)
                        .setMultiSession(false)
                        .setLicenseRequestHeaders(requestHeaders)
                }

                val mediaItemBuilder = MediaItem.Builder()
                    .setUri(mediaItem.url)
                    .setMimeType(mimeType)
                    .setMediaMetadata(metaBuilder.build())
                    .setDrmConfiguration(drmConfigurationBuilder.build())

                DashMediaSource.Factory(
                    DefaultDashChunkSource.Factory(dataSourceFactory),
                    dataSourceFactory
                ).createMediaSource(mediaItemBuilder.build()).mediaItem
            }

            else -> {
                MediaItem.Builder()
                    .setUri(mediaItem.url)
                    .setMimeType(mimeType)
                    .setMediaMetadata(metaBuilder.build())
                    .build()
            }
        }
    }
}