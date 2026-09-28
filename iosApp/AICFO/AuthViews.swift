import Combine
import SwiftUI
import Shared

struct AuthFlowView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        let _ = model.revision
        let step = model.controller.authStep()
        VStack(alignment: .leading, spacing: 0) {
            BrandHeader()
                .padding(.top, 8)
                .padding(.bottom, 28)
            switch step {
            case "OTP":
                OtpStep()
            case "EMAIL":
                EmailStep()
            case "UNLOCK":
                FaceIdSetupStep()
            default:
                PhoneStep()
            }
        }
        .padding(.horizontal, 22)
        .padding(.bottom, 12)
        .frame(maxWidth: 440)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }
}

private struct PhoneStep: View {
    @EnvironmentObject private var model: AppModel
    @State private var digits = ""

    var body: some View {
        let error = model.controller.phoneError()
        let _ = model.revision
        VStack(alignment: .leading, spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text("What's your number?")
                        .font(Theme.title(32))
                        .foregroundStyle(Theme.text)
                    Text("We'll text a one-time code. No password to remember.")
                        .font(Theme.body(16))
                        .foregroundStyle(Theme.muted)
                        .padding(.top, 8)
                    Text("Mobile number")
                        .font(Theme.body(13))
                        .foregroundStyle(Theme.muted)
                        .padding(.top, 22)
                    HStack(spacing: 10) {
                        HStack(spacing: 8) {
                            Text("🇺🇸")
                            Text("+1").font(Theme.semi(16)).foregroundStyle(Theme.text)
                            Text("▾").font(Theme.body(12)).foregroundStyle(Theme.muted)
                        }
                        .padding(.horizontal, 14)
                        .frame(height: 56)
                        .background(.white, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                        TextField("(555) 000-0000", text: Binding(
                            get: { PhoneNumbers.shared.formatNational(nationalDigits: digits) },
                            set: { digits = PhoneNumbers.shared.usDigits(raw: $0) }
                        ))
                        .keyboardType(.phonePad)
                        .font(Theme.body(16))
                        .padding(.horizontal, 16)
                        .frame(height: 56)
                        .background(.white, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                    }
                    .padding(.top, 8)
                    if !error.isEmpty {
                        Text(error)
                            .font(Theme.body(13))
                            .foregroundStyle(Theme.danger)
                            .padding(.top, 8)
                    }
                    HStack(alignment: .top, spacing: 10) {
                        Image(systemName: "checkmark.shield")
                            .foregroundStyle(Theme.accent)
                        Text("We'll text a code — no password. US numbers first.")
                            .font(Theme.body(14))
                            .foregroundStyle(Theme.accent)
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(red: 231 / 255, green: 228 / 255, blue: 1), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .padding(.top, 14)
                }
            }
            PrimaryButton(label: "Continue", enabled: digits.count == 10) {
                _ = model.controller.submitPhone(raw: digits)
            }
            .padding(.top, 12)
            if model.controller.debugAuthTools() {
                Button("Debug skip") { model.controller.debugSkipPhone() }
                    .font(Theme.semi(13))
                    .foregroundStyle(Theme.muted)
                    .frame(maxWidth: .infinity)
                    .padding(.top, 8)
            }
        }
    }
}

private struct OtpStep: View {
    @EnvironmentObject private var model: AppModel
    @State private var code = ""
    @State private var pulse = 0

    var body: some View {
        let error = model.controller.otpError()
        let seconds = model.controller.resendSeconds()
        let _ = model.revision
        let _ = pulse
        VStack(alignment: .leading, spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text("Enter the code")
                        .font(Theme.title(32))
                        .foregroundStyle(Theme.text)
                    Text("Sent to \(model.controller.maskedPhone())")
                        .font(Theme.body(16))
                        .foregroundStyle(Theme.muted)
                        .padding(.top, 8)
                    OtpBoxes(code: code)
                        .padding(.top, 22)
                        .overlay {
                            TextField("", text: Binding(
                                get: { code },
                                set: { code = String($0.filter(\.isNumber).prefix(6)) }
                            ))
                            .keyboardType(.numberPad)
                            .foregroundStyle(.clear)
                            .tint(.clear)
                            .frame(maxWidth: .infinity, minHeight: 58)
                        }
                    if !error.isEmpty {
                        Text(error).font(Theme.body(13)).foregroundStyle(Theme.danger).padding(.top, 10)
                    }
                    HStack {
                        if seconds > 0 {
                            Text(resendLabel(seconds)).font(Theme.body(14)).foregroundStyle(Theme.muted)
                        } else {
                            Button("Resend code") { _ = model.controller.resendOtp() }
                                .font(Theme.semi(14))
                                .foregroundStyle(Theme.accent)
                        }
                        Spacer()
                        Button("Change number") { model.controller.changePhoneNumber() }
                            .font(Theme.semi(14))
                            .foregroundStyle(Theme.accent)
                    }
                    .padding(.top, 14)
                    if !model.controller.debugOtpCode().isEmpty {
                        Button("Fill debug code") { code = model.controller.debugOtpCode() }
                            .font(Theme.semi(13))
                            .foregroundStyle(Theme.muted)
                            .padding(.top, 10)
                    }
                }
            }
            PrimaryButton(label: "Verify", enabled: code.count == 6) {
                _ = model.controller.verifyOtp(code: code)
            }
            .padding(.top, 12)
        }
        .onReceive(Timer.publish(every: 1, on: .main, in: .common).autoconnect()) { _ in
            pulse += 1
        }
    }
}

private struct EmailStep: View {
    @EnvironmentObject private var model: AppModel
    @State private var email = ""

    var body: some View {
        let error = model.controller.emailError()
        let _ = model.revision
        VStack(alignment: .leading, spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text("Where should we send\nyour wins?")
                        .font(Theme.title(32))
                        .foregroundStyle(Theme.text)
                    Text("Monthly email: how much you saved this month — calm summary, not spam.")
                        .font(Theme.body(16))
                        .foregroundStyle(Theme.muted)
                        .padding(.top, 8)
                    Text("Email for reports")
                        .font(Theme.body(13))
                        .foregroundStyle(Theme.muted)
                        .padding(.top, 22)
                    HStack(spacing: 10) {
                        Image(systemName: "envelope")
                            .foregroundStyle(Theme.muted)
                        TextField("maya@studio.example", text: $email)
                            .textInputAutocapitalization(.never)
                            .keyboardType(.emailAddress)
                            .font(Theme.body(16))
                    }
                    .padding(.horizontal, 16)
                    .frame(height: 56)
                    .background(.white, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .padding(.top, 8)
                    if !error.isEmpty {
                        Text(error).font(Theme.body(13)).foregroundStyle(Theme.danger).padding(.top, 8)
                    }
                    VStack(alignment: .leading, spacing: 6) {
                        HStack(spacing: 8) {
                            Circle().fill(Theme.success).frame(width: 8, height: 8)
                            Text("Monthly savings report").font(Theme.semi(16)).foregroundStyle(Theme.text)
                        }
                        Text("One note each month on moves completed and dollars kept — not a login method.")
                            .font(Theme.body(14))
                            .foregroundStyle(Theme.muted)
                    }
                    .padding(16)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .softCard(radius: 22)
                    .padding(.top, 14)
                    Text("Account login stays phone + OTP. Email is only for reports.")
                        .font(Theme.body(13))
                        .foregroundStyle(Theme.muted)
                        .padding(.top, 14)
                }
            }
            PrimaryButton(label: "Continue", enabled: !email.trimmingCharacters(in: .whitespaces).isEmpty) {
                _ = model.controller.saveReportEmail(raw: email)
            }
            .padding(.top, 12)
            SecondaryButton(label: "Skip for now") { model.controller.skipReportEmail() }
                .padding(.top, 10)
        }
    }
}

private struct FaceIdSetupStep: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        let _ = model.revision
        ScrollView {
            VStack(spacing: 0) {
                Text("IOS · DEVICE UNLOCK")
                    .font(Theme.semi(11))
                    .foregroundStyle(Theme.muted)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color(red: 232 / 255, green: 234 / 255, blue: 240), in: Capsule())
                Image(systemName: "faceid")
                    .font(.system(size: 28, weight: .semibold))
                    .foregroundStyle(Theme.accent)
                    .frame(width: 72, height: 72)
                    .background(.white, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                    .padding(.top, 22)
                Text("Enable Face ID\nto unlock Finwise")
                    .font(Theme.title(30))
                    .foregroundStyle(Theme.text)
                    .multilineTextAlignment(.center)
                    .padding(.top, 18)
                Text("Protects this device only. Your account login is still phone + OTP — Face ID never signs you in.")
                    .font(Theme.body(16))
                    .foregroundStyle(Theme.muted)
                    .multilineTextAlignment(.center)
                    .padding(.top, 8)
                VStack(alignment: .leading, spacing: 0) {
                    benefit("Cold starts stay fast", "Unlock → Home. No re-OTP while signed in.")
                    Divider().padding(.leading, 28)
                    benefit("Device unlock, not account login", "Session already persisted after OTP.")
                    Divider().padding(.leading, 28)
                    benefit("Passcode fallback always on", "Release requires an unlock path — never empty.")
                }
                .padding(16)
                .softCard(radius: 22)
                .padding(.top, 18)
                PrimaryButton(label: "Enable Face ID") {
                    model.unlockWithBiometrics {
                        _ = model.controller.enableBiometricUnlock()
                    }
                }
                .padding(.top, 18)
                SecondaryButton(label: "Use device passcode") {
                    model.unlockWithPasscode {
                        _ = model.controller.enablePasscodeUnlock()
                    }
                }
                .padding(.top, 10)
                Text("Touch ID on older devices. You can change this anytime in Settings.")
                    .font(Theme.body(13))
                    .foregroundStyle(Theme.muted)
                    .multilineTextAlignment(.center)
                    .padding(.top, 12)
            }
        }
    }

    private func benefit(_ title: String, _ body: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(Theme.success)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(Theme.semi(15)).foregroundStyle(Theme.text)
                Text(body).font(Theme.body(13)).foregroundStyle(Theme.muted)
            }
        }
        .padding(.vertical, 10)
    }
}

private struct BrandHeader: View {
    var body: some View {
        HStack(spacing: 12) {
            Text("F")
                .font(Theme.title(20))
                .foregroundStyle(Theme.accent)
                .frame(width: 44, height: 44)
                .background(.white, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                .shadow(color: Theme.accent.opacity(0.18), radius: 10, y: 4)
            VStack(alignment: .leading, spacing: 0) {
                Text("Finwise").font(Theme.semi(16)).foregroundStyle(Theme.text)
                Text("Your AI CFO").font(Theme.body(13)).foregroundStyle(Theme.muted)
            }
        }
    }
}

private struct OtpBoxes: View {
    let code: String

    var body: some View {
        HStack(spacing: 8) {
            ForEach(0..<6, id: \.self) { index in
                let chars = Array(code)
                let active = code.count < 6 && index == code.count
                ZStack {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(.white)
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(active ? Theme.accent : Color(red: 230 / 255, green: 232 / 255, blue: 238), lineWidth: active ? 2 : 1)
                    if index < chars.count {
                        Text(String(chars[index]))
                            .font(Theme.semi(22))
                            .foregroundStyle(Theme.text)
                    }
                }
                .frame(height: 58)
            }
        }
    }
}

private func resendLabel(_ seconds: Int32) -> String {
    let value = Int(seconds)
    return String(format: "Resend in %d:%02d", value / 60, value % 60)
}
