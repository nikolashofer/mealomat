import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() { PlatformModule_iosKt.startKoinIos() }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}