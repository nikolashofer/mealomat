import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        #if DEBUG
        PlatformModule_iosKt.startKoinIos(debug: true)
        #else
        PlatformModule_iosKt.startKoinIos(debug: false)
        #endif
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}