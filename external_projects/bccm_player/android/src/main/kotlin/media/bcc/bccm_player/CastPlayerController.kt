package media.bcc.bccm_player

import android.net.Uri
import android.util.Log
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.MediaItemConverter
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.*
import androidx.media3.common.util.Assertions
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.Session
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.images.WebImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import media.bcc.player.ChromecastControllerPigeon
import media.bcc.player.PlaybackPlatformApi
import org.json.JSONException
import org.json.JSONObject


class CastPlayerController(
        private val castContext: CastContext,
        private val chromecastListenerPigeon: ChromecastControllerPigeon.ChromecastPigeon,
        private val plugin: BccmPlayerPlugin)
    : PlayerController(), SessionManagerListener<Session>, SessionAvailabilityListener {
    override val player = CastPlayer(castContext, CastMediaItemConverter())
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun release() {
        player.release()
    }

    override val id: String = "chromecast"

    init {
        player.playWhenReady = true

        player.setSessionAvailabilityListener(this)
        player.addListener(PlayerListener(this, plugin))
        mainScope.launch {
            BccmPlayerPluginSingleton.appConfigState.collectLatest { handleUpdatedAppConfig(it) }
        }
    }

    override fun stop(reset: Boolean) {
        if (reset) {
            player.clearMediaItems()
        } else {
            player.pause()
        }
    }

    fun getState(): PlaybackPlatformApi.ChromecastState {
        return PlaybackPlatformApi.ChromecastState.Builder().setConnectionState(PlaybackPlatformApi.CastConnectionState.values()[castContext.castState]).build()
    }

    private fun handleUpdatedAppConfig(appConfigState: PlaybackPlatformApi.AppConfig?) {
        Log.d("bccm", "setting preferred audio and sub lang to: ${appConfigState?.audioLanguage}, ${appConfigState?.subtitleLanguage}")

       /* player.trackSelectionParameters = trackSelector.parameters.buildUpon()
                .setPreferredAudioLanguage(appConfigState?.audioLanguage)
                .setPreferredTextLanguage(appConfigState?.subtitleLanguage).build()
*/

/*
        castContext.sessionManager.currentCastSession.remoteMediaClient.s*/
    }

    // SessionManagerListener

    override fun onSessionEnded(p0: Session, p1: Int) {
        chromecastListenerPigeon.onSessionEnded {}
    }

    override fun onSessionEnding(p0: Session) {
        chromecastListenerPigeon.onSessionEnding {}
    }

    override fun onSessionResumeFailed(p0: Session, p1: Int) {
        chromecastListenerPigeon.onSessionResumeFailed {}
    }

    override fun onSessionResumed(p0: Session, p1: Boolean) {
        chromecastListenerPigeon.onSessionResumed {}
    }

    override fun onSessionResuming(p0: Session, p1: String) {
        chromecastListenerPigeon.onSessionResuming {}
    }

    override fun onSessionStartFailed(p0: Session, p1: Int) {
        chromecastListenerPigeon.onSessionStartFailed {}
    }

    override fun onSessionStarted(p0: Session, p1: String) {
        chromecastListenerPigeon.onSessionStarted {}
    }

    override fun onSessionStarting(p0: Session) {
        chromecastListenerPigeon.onSessionStarting {}
    }

    override fun onSessionSuspended(p0: Session, p1: Int) {
        chromecastListenerPigeon.onSessionSuspended {}
    }

    // SessionAvailabilityListener

    override fun onCastSessionAvailable() {
        chromecastListenerPigeon.onCastSessionAvailable {}
        Log.d("Bccm", "Session available. Transferring state from primaryPlayer to castPlayer");
        val primaryPlayer =
                plugin.getPlaybackService()?.getPrimaryController()?.player ?: return
        transferState(primaryPlayer, player);
    }

    override fun onCastSessionUnavailable() {
        val event = ChromecastControllerPigeon.CastSessionUnavailableEvent.Builder();
        val currentPosition = player.currentPosition;
        if (currentPosition > 0) {
            event.setPlaybackPositionMs(currentPosition);
        }
        chromecastListenerPigeon.onCastSessionUnavailable(event.build()) {};
/*
        Log.d("Bccm", "Session unavailable. Transferring state from castPlayer to primaryPlayer");
         val primaryPlayer = plugin.getPlaybackService()?.getPrimaryController()?.player ?: throw Error("PlaybackService not active");
         transferState(primaryPlayer, castPlayer);*/
    }

    // Extra

    private fun transferState(previous: Player, next: Player) {
        /*var isLive = false
        if (previous.isCurrentMediaItemDynamic){
            val mediaItem = previous.currentMediaItem ?: return;
            mediaItem.mediaMetadata.extras?.putString("is_live");
            isLive = true
            previous.stop()
            previous.clearMediaItems()

            next.setMediaItem(mediaItem, true)
            next.playWhenReady = true
            next.prepare()
            next.play()
        }*/

        // Copy state from primary player
        var playbackPositionMs = C.TIME_UNSET
        var currentItemIndex = C.INDEX_UNSET
        var playWhenReady = false

        val queue = mutableListOf<MediaItem>()
        for (x in 0 until previous.mediaItemCount) {
            queue.add(previous.getMediaItemAt(x));
        }

        if (previous.playbackState != Player.STATE_ENDED) {
            if (!previous.isCurrentMediaItemDynamic)
                playbackPositionMs = previous.currentPosition
            playWhenReady = previous.playWhenReady
            currentItemIndex = previous.currentMediaItemIndex
            /*if (currentItemIndex != this.currentItemIndex) {
                playbackPositionMs = C.TIME_UNSET
                currentItemIndex = this.currentItemIndex
            }*/
        }
        previous.stop()
        previous.clearMediaItems()

        next.setMediaItems(queue, currentItemIndex, playbackPositionMs)
        next.playWhenReady = true
        next.prepare()
        next.play()
    }

}