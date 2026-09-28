import SwiftUI
import Shared

/// Four-step onboarding matched to Sofia's v1.1 boards.
struct OnboardingView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        let step = model.controller.onboarding()
        let _ = model.revision
        VStack(spacing: 0) {
            if Int(step.step) == 0 {
                Spacer(minLength: 0)
                welcome(step)
                Spacer(minLength: 0)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        Group {
                            switch Int(step.step) {
                            case 1: value(step)
                            case 2: connect(step)
                            default: trial(step)
                            }
                        }
                        .padding(.top, 18)
                        .padding(.bottom, 20)
                    }
                }
            }
            dots(step)
                .padding(.bottom, 18)
            if !step.linkError.isEmpty {
                Text(step.linkError)
                    .font(Theme.body(14))
                    .foregroundStyle(Theme.danger)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .padding(.bottom, 12)
            }
            PrimaryButton(label: step.primaryCta) {
                model.controller.primaryOnboarding()
            }
            secondary(step)
        }
        .padding(.horizontal, 22)
        .padding(.bottom, Int(step.step) == 2 ? 4 : 16)
        .frame(maxWidth: 480)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }

    private func welcome(_ step: OnboardingModel) -> some View {
        VStack(spacing: 0) {
            brandMark()
            Text(step.kicker)
                .font(Theme.semi(15))
                .foregroundStyle(Theme.accent)
                .padding(.top, 28)
            Text(step.title)
                .font(Theme.title(32))
                .foregroundStyle(Theme.text)
                .multilineTextAlignment(.center)
                .padding(.top, 14)
            Text(step.body)
                .font(Theme.body(16))
                .foregroundStyle(Theme.muted)
                .multilineTextAlignment(.center)
                .padding(.top, 14)
                .padding(.horizontal, 12)
        }
    }

    private func brandMark() -> some View {
        ZStack {
            Circle()
                .fill(Theme.accent.opacity(0.55))
                .frame(width: 148, height: 148)
                .offset(x: 26, y: -6)
            Circle()
                .fill(Color(red: 183 / 255, green: 180 / 255, blue: 1).opacity(0.72))
                .frame(width: 118, height: 118)
                .offset(x: -34, y: 22)
            Circle()
                .fill(Theme.accent.opacity(0.28))
                .frame(width: 96, height: 96)
                .offset(x: 46, y: 34)
            Image(systemName: "house")
                .font(.system(size: 28, weight: .regular))
                .foregroundStyle(Theme.accent)
                .frame(width: 84, height: 84)
                .background(Color.white, in: RoundedRectangle(cornerRadius: 26, style: .continuous))
                .shadow(color: Theme.accent.opacity(0.22), radius: 16, y: 8)
        }
        .frame(width: 210, height: 210)
    }

    private func value(_ step: OnboardingModel) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(step.kicker)
                .font(Theme.semi(13))
                .foregroundStyle(Theme.accent)
                .tracking(0.6)
            Text(step.title)
                .font(Theme.title(32))
                .foregroundStyle(Theme.text)
            Text(step.body)
                .font(Theme.body(16))
                .foregroundStyle(Theme.muted)
                .fixedSize(horizontal: false, vertical: true)
            VStack(spacing: 12) {
                ForEach(0..<Int(step.cardCount()), id: \.self) { index in
                    moveCard(step.cardAt(index: Int32(index)))
                }
            }
            .padding(.top, 8)
            if !step.footnote.isEmpty {
                HStack(alignment: .top, spacing: 10) {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(Theme.muted)
                        .padding(.top, 3)
                    Text(step.footnote)
                        .font(Theme.body(14))
                        .foregroundStyle(Theme.muted)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(red: 231 / 255, green: 233 / 255, blue: 238 / 255), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
        }
    }

    private func moveCard(_ card: OnboardingCard) -> some View {
        HStack(spacing: 12) {
            iconBubble(card.icon)
            VStack(alignment: .leading, spacing: 2) {
                Text(card.title).font(Theme.semi(15)).foregroundStyle(Theme.text)
                Text(card.subtitle).font(Theme.body(13)).foregroundStyle(Theme.muted)
            }
            Spacer(minLength: 8)
            ImpactPill(text: card.impact)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 14)
        .softCard(radius: 22)
    }

    private func connect(_ step: OnboardingModel) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(step.kicker)
                .font(Theme.semi(13))
                .foregroundStyle(Theme.accent)
                .tracking(0.6)
            Text(step.title)
                .font(Theme.title(32))
                .foregroundStyle(Theme.text)
            Text(step.body)
                .font(Theme.body(16))
                .foregroundStyle(Theme.muted)
                .fixedSize(horizontal: false, vertical: true)
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 6) {
                    Image(systemName: "lock.fill")
                        .font(.system(size: 11, weight: .semibold))
                    Text(step.badge).font(Theme.semi(13))
                }
                .foregroundStyle(Theme.success)
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(Theme.successSoft, in: Capsule())
                Text(step.trustTitle)
                    .font(Theme.semi(17))
                    .foregroundStyle(Theme.text)
                    .padding(.top, 6)
                Text(step.trustBody)
                    .font(Theme.body(14))
                    .foregroundStyle(Theme.muted)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .softCard(radius: 22)
            Text(step.sectionLabel)
                .font(Theme.semi(12))
                .foregroundStyle(Theme.muted)
                .tracking(1.1)
                .padding(.top, 10)
            let count = Int(step.typeCount())
            VStack(spacing: 12) {
                ForEach(Array(stride(from: 0, to: count, by: 2)), id: \.self) { index in
                    HStack(spacing: 12) {
                        typeCard(step.typeAt(index: Int32(index)))
                        if index + 1 < count {
                            typeCard(step.typeAt(index: Int32(index + 1)))
                        }
                    }
                }
            }
        }
    }

    private func typeCard(_ card: OnboardingCard) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Image(systemName: connectSymbol(card.icon))
                .font(.system(size: 18, weight: .medium))
                .foregroundStyle(Theme.accent)
                .frame(width: 44, height: 44)
                .background(Theme.accentSoft, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            Text(card.title)
                .font(Theme.semi(16))
                .foregroundStyle(Theme.text)
                .padding(.top, 18)
            Text(card.subtitle)
                .font(Theme.body(13))
                .foregroundStyle(Theme.muted)
                .padding(.top, 2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .softCard(radius: 22)
    }

    private func trial(_ step: OnboardingModel) -> some View {
        VStack(spacing: 0) {
            Text(step.badge)
                .font(Theme.semi(13))
                .foregroundStyle(Theme.accent)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Theme.accentSoft, in: Capsule())
            Text(step.title)
                .font(Theme.title(32))
                .foregroundStyle(Theme.text)
                .multilineTextAlignment(.center)
                .padding(.top, 16)
            Text(step.body)
                .font(Theme.body(16))
                .foregroundStyle(Theme.muted)
                .multilineTextAlignment(.center)
                .padding(.top, 12)
                .padding(.horizontal, 8)
            VStack(alignment: .leading, spacing: 0) {
                Text(step.sectionLabel)
                    .font(Theme.semi(12))
                    .foregroundStyle(Theme.muted)
                    .tracking(1.1)
                    .padding(.bottom, 4)
                let features = Int(step.featureCount())
                ForEach(0..<features, id: \.self) { index in
                    featureRow(step.featureAt(index: Int32(index)))
                    if index < features - 1 {
                        Rectangle()
                            .fill(Color(red: 230 / 255, green: 232 / 255, blue: 238 / 255))
                            .frame(height: 1)
                    }
                }
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .softCard(radius: 22)
            .padding(.top, 20)
            HStack(spacing: 8) {
                ForEach(0..<Int(step.chipCount()), id: \.self) { index in
                    Text(step.chipAt(index: Int32(index)))
                        .font(Theme.semi(12))
                        .foregroundStyle(Theme.muted)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 7)
                        .background(Theme.chip, in: Capsule())
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 16)
        }
    }

    private func featureRow(_ card: OnboardingCard) -> some View {
        HStack(alignment: .center, spacing: 12) {
            Image(systemName: "checkmark")
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 22, height: 22)
                .background(Theme.success, in: Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(card.title).font(Theme.semi(15)).foregroundStyle(Theme.text)
                Text(card.subtitle).font(Theme.body(13)).foregroundStyle(Theme.muted)
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, 12)
    }

    @ViewBuilder
    private func secondary(_ step: OnboardingModel) -> some View {
        if !step.secondaryCta.isEmpty {
            if Int(step.step) == 2 {
                Button(step.secondaryCta) {
                    model.controller.secondaryOnboarding()
                }
                .font(Theme.semi(15))
                .foregroundStyle(Theme.muted)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .buttonStyle(.plain)
            } else {
                SecondaryButton(label: step.secondaryCta) {
                    model.controller.secondaryOnboarding()
                }
                .padding(.top, 10)
                if !step.footnote.isEmpty {
                    Text(step.footnote)
                        .font(Theme.body(12))
                        .foregroundStyle(Theme.muted)
                        .multilineTextAlignment(.center)
                        .padding(.top, 12)
                        .padding(.horizontal, 8)
                }
            }
        }
    }

    private func dots(_ step: OnboardingModel) -> some View {
        HStack(spacing: 8) {
            ForEach(0..<Int(step.stepCount), id: \.self) { index in
                if index == Int(step.step) {
                    Capsule()
                        .fill(Theme.accent)
                        .frame(width: 22, height: 6)
                } else {
                    Circle()
                        .fill(Color(red: 213 / 255, green: 216 / 255, blue: 224 / 255))
                        .frame(width: 6, height: 6)
                }
            }
        }
        .frame(maxWidth: .infinity)
    }

    private func iconBubble(_ icon: String) -> some View {
        let spec: (String, Color, Color) = {
            switch icon {
            case "CLOCK": return ("clock", Theme.danger, Theme.dangerSoft)
            case "CARD": return ("creditcard", Theme.warning, Theme.warningSoft)
            default: return ("banknote", Theme.success, Theme.successSoft)
            }
        }()
        return Image(systemName: spec.0)
            .font(.system(size: 16, weight: .medium))
            .foregroundStyle(spec.1)
            .frame(width: 40, height: 40)
            .background(spec.2, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func connectSymbol(_ icon: String) -> String {
        switch icon {
        case "BANK": return "building.columns"
        case "CARD": return "creditcard"
        case "LOAN": return "plus"
        case "INVEST": return "chart.line.uptrend.xyaxis"
        case "CLOCK": return "clock"
        default: return "banknote"
        }
    }
}
