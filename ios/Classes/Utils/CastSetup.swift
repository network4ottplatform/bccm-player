//
//  CastSetup.swift
//  bccm_player
//
//  Created by Andreas Gangsø on 27/04/2023.
//

import Foundation
import GoogleCast

func setupCast() {
    guard let appId = Bundle.main.object(forInfoDictionaryKey: "cast_app_id") as? String else {
        return 
    }
    let criteria = GCKDiscoveryCriteria(applicationID: appId)
    let options = GCKCastOptions(discoveryCriteria: criteria)
    options.physicalVolumeButtonsWillControlDeviceVolume = true

    GCKCastContext.setSharedInstanceWith(options)
    let styler = GCKUIStyle.sharedInstance()
    styler.castViews.mediaControl.expandedController.backgroundImageContentMode = UIImageView.ContentMode.scaleAspectFit.rawValue as NSNumber
    styler.castViews.mediaControl.expandedController.backgroundColor = UIColor(red: CGFloat(13/255.0), green: CGFloat(22/255.0), blue: CGFloat(55/255.0), alpha: CGFloat(1.0))
    GCKCastContext.sharedInstance().useDefaultExpandedMediaControls = false
}
