import SwiftUI
import Shared

struct LockView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        let lock = model.controller.lock()
        let _ = model.revision
        VStack(spacing: 0) {
            ZStack {
                Circle()
                    .fill(Theme.accent.opacity(0.18))
                    .frame(width: 132, height: 132)
                    .offset(x: 16, y: -10)
                Circle()
                    .fill(Theme.accent.opacity(0.28))
                    .frame(width: 120, height: 120)
                Text("F")
                    .font(Theme.title(34))
                    .foregroundStyle(Theme.accent)
                    .frame(width: 78, height: 78)
                    .background(.white, in: RoundedRectangle(cornerRadius: 26, style: .continuous))
            }
            .padding(.top, 36)
            Text("WELCOME BACK")
                .font(Theme.semi(13))
                .foregroundStyle(Theme.accent)
                .tracking(1.1)
                .padding(.top, 22)
            Text("Unlock Finwise")
                .font(Theme.title(32))
                .foregroundStyle(Theme.text)
                .padding(.top, 8)
            Text("You're still signed in. Face ID unlocks this device — not a new login.")
                .font(Theme.body(16))
                .foregroundStyle(Theme.muted)
                .multilineTextAlignment(.center)
                .padding(.top, 8)
            VStack(spacing: 6) {
                Image(systemName: "faceid")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(Theme.accent)
                    .frame(width: 52, height: 52)
                    .background(Theme.accentSoft, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                Text("Face ID")
                    .font(Theme.title(18))
                    .foregroundStyle(Theme.text)
                    .padding(.top, 6)
                Text("Device unlock only · account stays via phone+OTP")
                    .font(Theme.body(13))
                    .foregroundStyle(Theme.muted)
                    .multilineTextAlignment(.center)
            }
            .padding(.vertical, 22)
            .padding(.horizontal, 16)
            .frame(maxWidth: .infinity)
            .softCard(radius: 22)
            .padding(.top, 22)
            PrimaryButton(label: lock.hardwareAvailable ? "Unlock with Face ID" : "Use device passcode") {
                if lock.hardwareAvailable {
                    model.unlockWithBiometrics()
                } else {
                    model.unlockWithPasscode()
                }
            }
            .padding(.top, 22)
            Button("Use device passcode") { model.unlockWithPasscode() }
                .font(Theme.semi(15))
                .foregroundStyle(Theme.muted)
                .padding(.top, 14)
        }
        .padding(.horizontal, 24)
        .frame(maxWidth: 440)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .onAppear {
            if lock.hardwareAvailable {
                model.unlockWithBiometrics()
            }
        }
    }
}
