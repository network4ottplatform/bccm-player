package media.bcc.bccm_player.players.chromecast

import android.view.Menu
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity
import com.google.android.gms.cast.framework.media.uicontroller.UIMediaController;

import media.bcc.bccm_player.R

class CastExpandedControlsActivity : ExpandedControllerActivity() {
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.cast_expanded_controller, menu)
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item)
        
        UIMediaController uiMediaController = getUIMediaController()
        uiMediaController.bindViewToRewind(button_2, 10000)
        uiMediaController.bindViewToForward(button_4, 10000)

        return true
    }
}
