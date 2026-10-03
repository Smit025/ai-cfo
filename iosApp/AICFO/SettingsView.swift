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
                Text("SECURITY")
                    .font(Theme.semi(12))
                    .foregroundStyle(Theme.muted)
                    .tracking(1.1)
                HStack(spacing: 12) {
                    Image(systemName: "faceid")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(Theme.accent)
                        .frame(width: 44, height: 44)
                        .background(Theme.accentSoft, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Unlock with Face ID").font(Theme.semi(16)).foregroundStyle(Theme.text)
                        Text("Required on each cold start while signed in")
                            .font(Theme.body(13)).foregroundStyle(Theme.muted)
                    }
                    Spacer()
                    Toggle("", isOn: Binding(
                        get: { settings.biometricEnabled },
                        set: { model.controller.setBiometricEnabled(enabled: $0) }
                    ))
                    .labelsHidden()
                    .tint(Theme.accent)
                }
                .padding(16)
                .softCard(radius: 22)
                Text("Session stays signed in. Phone + OTP only after Log out, reinstall, or cleared session.")
                    .font(Theme.body(13))
                    .foregroundStyle(Theme.muted)
                if settings.deviceLockReady {
                    SecondaryButton(label: "Lock now") { model.controller.lockNow() }
                }
                Text("ACCOUNT")
                    .font(Theme.semi(12))
                    .foregroundStyle(Theme.muted)
                    .tracking(1.1)
                Button {
                    model.controller.logOut()
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                            .foregroundStyle(Theme.danger)
                            .frame(width: 44, height: 44)
                            .background(Theme.dangerSoft, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Log out").font(Theme.semi(16)).foregroundStyle(Theme.danger)
                            Text("Clears session — next open asks for phone + OTP")
                                .font(Theme.body(13)).foregroundStyle(Theme.muted)
                                .multilineTextAlignment(.leading)
                        }
                        Spacer()
                    }
                    .padding(16)
                    .softCard(radius: 22)
                }
                .buttonStyle(.plain)
                toggleRow(
                    "Notifications",
                    "A dot on Home when a move is waiting.",
                    settings.notificationsEnabled
                ) { model.controller.setNotifications(enabled: $0) }
                VStack(alignment: .leading, spacing: 8) {
                    Text("About").font(Theme.semi(16)).foregroundStyle(Theme.text)
                    Text(settings.brandTagline).font(Theme.body(15)).foregroundStyle(Theme.text)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .softCard()
                VStack(alignment: .leading, spacing: 8) {
                    Text("Privacy").font(Theme.semi(16)).foregroundStyle(Theme.text)
                    Text("Read-only linking. Bank passwords are never stored.").font(Theme.body(14)).foregroundStyle(Theme.muted)
                    Text("Link tokens stay in the Keychain on this iPhone.").font(Theme.body(14)).foregroundStyle(Theme.muted)
                    Text("Logs redact tokens, passwords, and card numbers.").font(Theme.body(14)).foregroundStyle(Theme.muted)
                    Text(settings.sampleLink ? "Maya sample is linked. It is not your bank." : (settings.banksLinked ? "A read-only bank is linked." : "No institutions linked."))
                        .font(Theme.body(14)).foregroundStyle(Theme.muted)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .softCard()
                if settings.banksLinked {
                    SecondaryButton(label: "Disconnect institutions") { model.controller.disconnectAll() }
                }
                #if DEBUG
                if settings.qaEnabled {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("QA · trial / paywall").font(Theme.semi(16)).foregroundStyle(Theme.text)
                        Text("Debug tools for this build. Force the hard paywall, restore the 30-day trial, or simulate a bank that needs reconnect.")
                            .font(Theme.body(13)).foregroundStyle(Theme.muted)
                        PrimaryButton(label: "Show paywall") { model.controller.debugForcePaywall() }
                        SecondaryButton(label: "Restore trial") { model.controller.debugForceTrial() }
                        SecondaryButton(label: "Simulate Pro") { model.controller.debugForcePro() }
                        SecondaryButton(label: "Clear QA override") { model.controller.debugClearOverride() }
                        SecondaryButton(label: "Replay onboarding") { model.controller.debugReplayOnboarding() }
                        SecondaryButton(label: "Simulate bank reconnect") { model.controller.debugSimulateNeedsReauth() }
                        SecondaryButton(label: "Simulate sync failure") { model.controller.debugSimulateSyncFailure() }
                    }
                    .padding(16)
                    .softCard()
                }
                #endif
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
