import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() { MainViewControllerKt.startDependencyInjection() }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}