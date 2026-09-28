import SwiftUI
import Shared

struct LockView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        let lock = model.controller.lock()
        let _ = model.revision
        VStack(spacing: 16) {
            Image(systemName: "lock")
                .font(.system(size: 28, weight: .semibold))
                .foregroundStyle(Theme.accent)
                .frame(width: 72, height: 72)
                .background(Theme.accentSoft, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
            Text(lock.title).font(Theme.title(28)).foregroundStyle(Theme.text)
            Text(lock.body)
                .font(Theme.body(15))
                .foregroundStyle(Theme.muted)
                .multilineTextAlignment(.center)
            PrimaryButton(label: lock.primaryCta) {
                if lock.hardwareAvailable {
                    model.unlock()
                } else {
                    model.controller.unlockWithoutHardware()
                }
            }
            .padding(.top, 8)
        }
        .padding(24)
        .frame(maxWidth: 420)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
