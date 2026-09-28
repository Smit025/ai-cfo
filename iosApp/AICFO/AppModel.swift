import Foundation
import LocalAuthentication
import Shared

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

    init() {
        let vault = KeychainTokenVault()
        let store = DefaultsStore()
        controller = AiCfoController(vault: vault, store: store, clock: SystemAppClock())
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
            context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: "Unlock AI CFO") { ok, _ in
                Task { @MainActor in
                    self.controller.unlockFromBiometric(success: ok)
                }
            }
        } else {
            controller.setBiometricHardware(available: false)
            controller.unlockWithoutHardware()
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
