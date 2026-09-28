import SwiftUI
import Shared

/// Soft-card onboarding. Step copy comes from the shared controller so a later
/// pixel pass can restyle these layouts without forking the product words.
struct OnboardingView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        let step = model.controller.onboarding()
        let _ = model.revision
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 6) {
                ForEach(0..<Int(step.stepCount), id: \.self) { index in
                    Capsule()
                        .fill(index == Int(step.step) ? Theme.accent : Theme.accentSoft)
                        .frame(width: index == Int(step.step) ? 22 : 7, height: 7)
                }
            }
            .padding(.top, 18)
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text(step.kicker)
                        .font(Theme.semi(13))
                        .foregroundStyle(Theme.accent)
                        .padding(.top, 28)
                    Text(step.title)
                        .font(Theme.title(32))
                        .foregroundStyle(Theme.text)
                    Text(step.body)
                        .font(Theme.body(16))
                        .foregroundStyle(Theme.muted)
                        .fixedSize(horizontal: false, vertical: true)
                    stepBody(step)
                        .padding(.top, 8)
                }
            }
            PrimaryButton(label: step.primaryCta, enabled: step.canAdvance) {
                model.controller.advanceOnboarding()
            }
            if step.canGoBack {
                SecondaryButton(label: "Back") { model.controller.backOnboarding() }
                    .padding(.top, 8)
            }
        }
        .padding(.horizontal, 24)
        .padding(.bottom, 18)
        .frame(maxWidth: 480)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }

    @ViewBuilder
    private func stepBody(_ step: OnboardingModel) -> some View {
        switch Int(step.step) {
        case 0:
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    iconBubble("clock")
                    Spacer()
                    ImpactPill(text: step.previewImpact)
                }
                Text(step.previewTitle).font(Theme.semi(18)).foregroundStyle(Theme.text)
                Text(step.previewBody).font(Theme.body(14)).foregroundStyle(Theme.muted)
                Text("Cancel · save $47/mo  →").font(Theme.semi(15)).foregroundStyle(Theme.accent)
            }
            .padding(18)
            .softCard()
        case 1:
            VStack(spacing: 12) {
                ForEach(0..<Int(step.bulletCount()), id: \.self) { index in
                    Text(step.bulletAt(index: Int32(index)))
                        .font(Theme.semi(15))
                        .foregroundStyle(Theme.text)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(16)
                        .softCard(radius: 20)
                }
            }
        case 2:
            VStack(alignment: .leading, spacing: 12) {
                VStack(alignment: .leading, spacing: 10) {
                    ForEach(0..<Int(step.bulletCount()), id: \.self) { index in
                        Text("•  \(step.bulletAt(index: Int32(index)))")
                            .font(Theme.body(15))
                            .foregroundStyle(Theme.text)
                    }
                }
                .padding(18)
                .softCard()
                if step.banksLinked {
                    Text(step.linkedSummary)
                        .font(Theme.semi(14))
                        .foregroundStyle(Theme.success)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(16)
                        .softCard(radius: 18)
                } else {
                    SecondaryButton(label: step.connectCta) {
                        _ = model.controller.connectReadOnlyStub()
                    }
                }
            }
        default:
            HStack(spacing: 12) {
                priceCard(step.priceLeft, "/ month")
                priceCard(step.priceRight, "/ year")
            }
        }
    }

    private func priceCard(_ price: String, _ period: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(price).font(Theme.title(26)).foregroundStyle(Theme.text)
            Text(period).font(Theme.body(13)).foregroundStyle(Theme.muted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .softCard(radius: 20)
    }

    private func iconBubble(_ symbol: String) -> some View {
        Image(systemName: symbol)
            .foregroundStyle(Theme.danger)
            .frame(width: 40, height: 40)
            .background(Theme.dangerSoft, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}
