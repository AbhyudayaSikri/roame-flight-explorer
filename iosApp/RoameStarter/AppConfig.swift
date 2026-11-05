import Foundation
import SwiftUI
@preconcurrency import Shared

enum AppConfig {
    /// `-apiBaseUrl <url>` wins, then the Info.plist key `API_BASE_URL`.
    static var baseURL: String {
        let arguments = ProcessInfo.processInfo.arguments
        if let index = arguments.firstIndex(of: "-apiBaseUrl"), arguments.indices.contains(index + 1) {
            return arguments[index + 1]
        }
        return Bundle.main.object(forInfoDictionaryKey: "API_BASE_URL") as? String ?? "http://localhost:8080"
    }

    /// Launch arguments for UI tests and QA: `-resetData`, `-failSaves`, `-failRemoves` (Debug only).
    static var debugOptions: DebugOptions {
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        return DebugOptions(
            resetData: arguments.contains("-resetData"),
            failSaves: arguments.contains("-failSaves"),
            failRemoves: arguments.contains("-failRemoves")
        )
        #else
        return DebugOptions(resetData: false, failSaves: false, failRemoves: false)
        #endif
    }

    /// `-forceDarkMode` (Debug only): dark appearance for UI-test screenshots, whatever the simulator uses.
    static var forcedColorScheme: ColorScheme? {
        #if DEBUG
        return ProcessInfo.processInfo.arguments.contains("-forceDarkMode") ? .dark : nil
        #else
        return nil
        #endif
    }
}
