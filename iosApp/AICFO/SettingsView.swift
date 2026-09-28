import SwiftUI
import Shared

struct SettingsView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        let settings = model.controller.settings()
        let _ = model.revision
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text("Settings").font(Theme.title(32)).foregroundStyle(Theme.text)
                HStack(spacing: 14) {
                    Text(settings.initials)
                        .font(Theme.semi(16))
                        .foregroundStyle(.white)
                        .frame(width: 52, height: 52)
                        .background(Theme.accent, in: Circle())
                    VStack(alignment: .leading, spacing: 2) {
                        Text(settings.name).font(Theme.title(18)).foregroundStyle(Theme.text)
                        Text(settings.meta).font(Theme.body(13)).foregroundStyle(Theme.muted)
                    }
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .softCard()
                VStack(alignment: .leading, spacing: 4) {
                    Text(settings.planLabel).font(Theme.semi(16)).foregroundStyle(Theme.text)
                    Text(settings.planDetail).font(Theme.body(13)).foregroundStyle(Theme.muted)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .softCard()
                toggleRow(
                    "Notifications",
                    "A dot on Home when a move is waiting.",
                    settings.notificationsEnabled
                ) { model.controller.setNotifications(enabled: $0) }
                toggleRow(
                    "Biometric lock",
                    settings.biometricHardware
                        ? "Ask for biometrics each time the app opens."
                        : "No biometric hardware on this device. The gate still shows a continue path.",
                    settings.biometricEnabled
                ) { model.controller.setBiometricEnabled(enabled: $0) }
                if settings.biometricEnabled {
                    SecondaryButton(label: "Lock now") { model.controller.lockNow() }
                }
                VStack(alignment: .leading, spacing: 8) {
                    Text("Privacy").font(Theme.semi(16)).foregroundStyle(Theme.text)
                    Text("Read-only linking. Bank passwords are never stored.").font(Theme.body(14)).foregroundStyle(Theme.muted)
                    Text("Link tokens stay in the Keychain on this iPhone.").font(Theme.body(14)).foregroundStyle(Theme.muted)
                    Text("Logs redact tokens, passwords, and card numbers.").font(Theme.body(14)).foregroundStyle(Theme.muted)
                    Text(settings.banksLinked ? "Sample institutions are linked." : "No institutions linked.")
                        .font(Theme.body(14)).foregroundStyle(Theme.muted)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .softCard()
                if settings.banksLinked {
                    SecondaryButton(label: "Disconnect institutions") { model.controller.disconnectAll() }
                }
                if settings.qaEnabled {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("QA · trial / paywall").font(Theme.semi(16)).foregroundStyle(Theme.text)
                        Text("Debug tools for this build. Force the hard paywall or restore the 30-day trial without waiting.")
                            .font(Theme.body(13)).foregroundStyle(Theme.muted)
                        PrimaryButton(label: "Show paywall") { model.controller.debugForcePaywall() }
                        SecondaryButton(label: "Restore trial") { model.controller.debugForceTrial() }
                        SecondaryButton(label: "Simulate Pro") { model.controller.debugForcePro() }
                        SecondaryButton(label: "Clear QA override") { model.controller.debugClearOverride() }
                        SecondaryButton(label: "Replay onboarding") { model.controller.debugReplayOnboarding() }
                    }
                    .padding(16)
                    .softCard()
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 120)
        }
    }

    private func toggleRow(_ title: String, _ body: String, _ on: Bool, _ change: @escaping (Bool) -> Void) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(Theme.semi(16)).foregroundStyle(Theme.text)
                Text(body).font(Theme.body(13)).foregroundStyle(Theme.muted)
            }
            Spacer()
            Toggle("", isOn: Binding(get: { on }, set: change))
                .labelsHidden()
                .tint(Theme.accent)
        }
        .padding(16)
        .softCard()
    }
}
