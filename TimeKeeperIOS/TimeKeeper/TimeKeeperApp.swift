//
//  TimeKeeperApp.swift
//  TimeKeeper
//
//  Created by samsung on 25/05/2026.
//

import SwiftUI
import UIKit

@main
struct TimeKeeperApp: App {
    var body: some Scene {
        WindowGroup {
            PlaylistListView()
                .onAppear {
                    UIApplication.shared.isIdleTimerDisabled = true
                }
                .onDisappear {
                    UIApplication.shared.isIdleTimerDisabled = false
                }
                .onOpenURL { url in
                    // Only handle our OAuth redirect URLs
                    if url.scheme?.lowercased() == "betarstimekeeper",
                       url.host?.lowercased() == "oauth" {
                        handleAuthRedirect(url: url)
                    }
                }
        }
    }
}
