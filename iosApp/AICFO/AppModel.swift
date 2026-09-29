import Foundation
import LocalAuthentication
import Shared

/// Reads `Localizable.strings`. A missing key returns nil so the shared catalog is used.
final class BundleLocalStrings: NSObject, LocalStrings {
    func text(key: String) -> String? {
        let value = Bundle.main.localizedString(forKey: key, value: "", table: nil)
        if value.isEmpty || value == key { return nil }
        return value
    }
}

final class BridgeObserver: NSObject, AppObserver {
    var handler: () -> Void = {}
    func onChanged() { handler() }
}

@MainActor
final class AppModel: ObservableObject {
    let controller: AiCfoController
    private let observer: BridgeObserver
    @Published var revision: Int = 0
    @Published var showingDetail = false
    private var didColdStartSync = false

    init() {
        let vault = KeychainTokenVault()
        let store = DefaultsStore()
        #if DEBUG
        let debugBuild = true
        #else
        let debugBuild = false
        #endif
        controller = AiCfoController(
            vault: vault,
            store: store,
            clock: SystemAppClock(),
            market: Markets.shared.unitedStates(),
            localStrings: BundleLocalStrings(),
            debugBuild: debugBuild
        )
        let bridge = BridgeObserver()
        observer = bridge
        bridge.handler = { [weak self] in
            Task { @MainActor in self?.revision += 1 }
        }
        controller.addObserver(observer: bridge)
        refreshHardware()
    }

    func refreshHardware() {
        let context = LAContext()
        var error: NSError?
        let can = context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
        controller.setBiometricHardware(available: can)
    }

    func unlock() {
        let context = LAContext()
        var error: NSError?
        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            controller.setBiometricHardware(available: true)
            context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: "Unlock Finwise") { ok, _ in
                Task { @MainActor in
                    self.controller.unlockFromBiometric(success: ok)
                }
            }
        } else {
            controller.setBiometricHardware(available: false)
            controller.unlockWithoutHardware()
        }
    }

    /// First activation refreshes as a cold start. Later activations are foreground resumes.
    func syncForColdStart() {
        guard !didColdStartSync else { return }
        didColdStartSync = true
        controller.refreshAccounts(reason: SyncTrigger.coldStart)
    }

    func syncForForeground() {
        guard didColdStartSync else { return }
        controller.refreshAccounts(reason: SyncTrigger.foreground)
    }

    func pullToRefresh() async {
        controller.refreshAccounts(reason: SyncTrigger.pullToRefresh)
    }

    func performSyncAction(code: String) {
        switch code {
        case "NEEDS_REAUTH":
            controller.reconnectBank()
        case "FAILED":
            controller.refreshAccounts(reason: SyncTrigger.manual)
        default:
            break
        }
    }

    func openMove(_ id: String, wide: Bool) {
        controller.selectMove(id: id)
        if wide {
            controller.selectTab(value: "MOVES")
            showingDetail = false
        } else {
            showingDetail = true
        }
    }
}
