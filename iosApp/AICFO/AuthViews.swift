import Combine
import SwiftUI
import Shared

/// Email + code sign-in. iOS shares the session model. This file does not send mail,
/// and a Mac build is required before this screen can be run.
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
            case "CODE":
                CodeStep()
            case "UNLOCK":
                FaceIdSetupStep()
            default:
                EmailSignInStep()
            }
        }
        .padding(.horizontal, 22)
        .padding(.bottom, 12)
        .frame(maxWidth: 440)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }
}

private struct EmailSignInStep: View {
    @EnvironmentObject private var model: AppModel
    @State private var email = ""

    var body: some View {
        let error = model.controller.emailError()
        let blocker = model.controller.emailSignInBlocker()
        let debugCode = model.controller.debugSignInCode()
        let configured = blocker.isEmpty
        let _ = model.revision
        VStack(alignment: .leading, spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text("What's your email?")
                        .font(Theme.title(32))
                        .foregroundStyle(Theme.text)
                    Text("We'll send a one-time code. Gmail, Outlook, Apple Mail, or any other inbox.")
                        .font(Theme.body(16))
                        .foregroundStyle(Theme.muted)
                        .padding(.top, 8)
                    Text("Email")
                        .font(Theme.body(13))
                        .foregroundStyle(Theme.muted)
                        .padding(.top, 22)
                    HStack(spacing: 10) {
                        Image(systemName: "envelope")
                            .foregroundStyle(Theme.muted)
                        TextField("maya@studio.example", text: $email)
                            .textInputAutocapitalization(.never)
                            .keyboardType(.emailAddress)
                            .textContentType(.emailAddress)
                            .autocorrectionDisabled()
                            .font(Theme.body(16))
                    }
                    .padding(.horizontal, 16)
                    .frame(height: 56)
                    .background(.white, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .padding(.top, 8)
                    if !error.isEmpty {
                        Text(error)
                            .font(Theme.body(13))
                            .foregroundStyle(Theme.danger)
                            .padding(.top, 8)
                    }
                    HStack(alignment: .top, spacing: 10) {
                        Image(systemName: "checkmark.shield")
                            .foregroundStyle(configured ? Theme.accent : Theme.danger)
                        Text(configured
                             ? "No password. The same address can be used later for a monthly savings report. We don't send that email."
                             : blocker)
                            .font(Theme.body(14))
                            .foregroundStyle(configured ? Theme.accent : Theme.danger)
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        configured
                            ? Color(red: 231 / 255, green: 228 / 255, blue: 1)
                            : Color(red: 254 / 255, green: 242 / 255, blue: 242),
                        in: RoundedRectangle(cornerRadius: 18, style: .continuous)
                    )
                    .padding(.top, 14)
                    if !debugCode.isEmpty {
                        Text("This debug build does not send mail. After you continue, the code is \(debugCode).")
                            .font(Theme.body(13))
                            .foregroundStyle(Theme.muted)
                            .padding(.top, 10)
                    }
                }
            }
            PrimaryButton(
                label: "Email me a code",
                enabled: configured && !email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            ) {
                _ = model.controller.submitEmail(raw: email)
            }
            .padding(.top, 12)
        }
    }
}

private struct CodeStep: View {
    @EnvironmentObject private var model: AppModel
    @State private var code = ""
    @State private var pulse = 0

    var body: some View {
        let error = model.controller.codeError()
        let seconds = model.controller.resendSeconds()
        let debugCode = model.controller.debugSignInCode()
        let sentTo = model.controller.maskedEmail()
        let _ = model.revision
        let _ = pulse
        VStack(alignment: .leading, spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text("Enter the code")
                        .font(Theme.title(32))
                        .foregroundStyle(Theme.text)
                    Text(debugCode.isEmpty ? "Sent to \(sentTo)" : "No email was sent. Debug code for \(sentTo).")
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
                            Button("Resend code") { _ = model.controller.resendSignInCode() }
                                .font(Theme.semi(14))
                                .foregroundStyle(Theme.accent)
                        }
                        Spacer()
                        Button("Change email") { model.controller.changeEmail() }
                            .font(Theme.semi(14))
                            .foregroundStyle(Theme.accent)
                    }
                    .padding(.top, 14)
                    if !debugCode.isEmpty {
                        Button("Fill debug code") { code = debugCode }
                            .font(Theme.semi(13))
                            .foregroundStyle(Theme.muted)
                            .padding(.top, 10)
                    }
                }
            }
            PrimaryButton(label: "Verify", enabled: code.count == 6) {
                _ = model.controller.verifySignInCode(code: code)
            }
            .padding(.top, 12)
        }
        .onReceive(Timer.publish(every: 1, on: .main, in: .common).autoconnect()) { _ in
            pulse += 1
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
                Text("Protects this device only. Your account login is the email code — Face ID never signs you in.")
                    .font(Theme.body(16))
                    .foregroundStyle(Theme.muted)
                    .multilineTextAlignment(.center)
                    .padding(.top, 8)
                VStack(alignment: .leading, spacing: 0) {
                    benefit("Cold starts stay fast", "Unlock → Home. No email prompt while signed in.")
                    Divider().padding(.leading, 28)
                    benefit("Device unlock, not account login", "Session is already stored after the code or link.")
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
