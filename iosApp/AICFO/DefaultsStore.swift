import Foundation
import Shared

/// Non-secret flags and sync metadata. Tokens go through KeychainTokenVault.
final class DefaultsStore: NSObject, LocalStore {
    private let defaults = UserDefaults.standard

    func read(key: String) -> String? {
        defaults.string(forKey: storageKey(key))
    }

    func write(key: String, value: String) {
        defaults.set(value, forKey: storageKey(key))
    }

    func remove(key: String) {
        defaults.removeObject(forKey: storageKey(key))
    }

    private func storageKey(_ key: String) -> String { "aicfo.\(key)" }
}
