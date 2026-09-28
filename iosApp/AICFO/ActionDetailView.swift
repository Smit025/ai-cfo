import SwiftUI
import Shared

struct ActionDetailView: View {
    @EnvironmentObject private var model: AppModel
    var moveId: String
    var showBack: Bool

    var body: some View {
        let _ = model.revision
        if let detail = model.controller.detail(id: moveId) {
            VStack(spacing: 0) {
                ScrollView {
                    VStack(alignment: .leading, spacing: 14) {
                        if showBack {
                            Button { model.showingDetail = false } label: {
                                HStack(spacing: 4) {
                                    Image(systemName: "chevron.left")
                                    Text(detail.backLabel)
                                }
                                .font(Theme.semi(16))
                                .foregroundStyle(Theme.accent)
                            }
                            .buttonStyle(.plain)
                        }
                        HStack(spacing: 8) {
                            MetaPill(text: detail.priority, tone: detail.priority)
                            Text(detail.category).font(Theme.semi(13)).foregroundStyle(Theme.danger)
                            Spacer()
                            MetaPill(text: detail.statusLabel, tone: detail.status)
                        }
                        Text(detail.title).font(Theme.title(30)).foregroundStyle(Theme.text)
                        Text(detail.lede).font(Theme.body(16)).foregroundStyle(Theme.muted)
                        layout(detail)
                        Text(detail.body).font(Theme.body(16)).foregroundStyle(Theme.text)
                        if !detail.freeUpValue.isEmpty {
                            HStack {
                                Text(detail.freeUpLabel).font(Theme.semi(15)).foregroundStyle(Theme.text)
                                Spacer()
                                Text(detail.freeUpValue)
                                    .font(Theme.semi(16))
                                    .foregroundStyle(Theme.success)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(Theme.successSoft, in: Capsule())
                            }
                            .padding(16)
                            .softCard(radius: 20)
                        }
                        if !detail.accountLine.isEmpty {
                            HStack(spacing: 8) {
                                Circle().fill(Theme.success).frame(width: 8, height: 8)
                                Text(detail.accountLine).font(Theme.semi(13)).foregroundStyle(Theme.text)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(Theme.chip, in: Capsule())
                        }
                        if !detail.banner.isEmpty {
                            Text(detail.banner)
                                .font(Theme.semi(14))
                                .foregroundStyle(Theme.accent)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(14)
                                .background(Theme.accentSoft, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 8)
                    .padding(.bottom, 12)
                }
                VStack(spacing: 10) {
                    PrimaryButton(label: detail.primaryCta) { _ = model.controller.performPrimary(id: detail.id) }
                    SecondaryButton(label: detail.secondaryCta) { _ = model.controller.performSecondary(id: detail.id) }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, showBack ? 16 : 100)
            }
        }
    }

    @ViewBuilder
    private func layout(_ detail: DetailModel) -> some View {
        switch detail.layout {
        case "SUBSCRIPTION":
            subscription(detail)
        case "DEBT", "CASH":
            math(detail, danger: detail.layout == "DEBT")
        default:
            if !detail.heroValue.isEmpty {
                VStack(alignment: .leading, spacing: 2) {
                    Text(detail.heroValue).font(Theme.title(28)).foregroundStyle(Theme.text)
                    Text(detail.heroCaption).font(Theme.semi(14)).foregroundStyle(Theme.text)
                    Text(detail.heroSub).font(Theme.body(13)).foregroundStyle(Theme.muted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
                .softCard()
            }
        }
    }

    private func math(_ detail: DetailModel, danger: Bool) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                Text(detail.heroValue)
                    .font(Theme.title(22))
                    .foregroundStyle(danger ? Theme.danger : Theme.accent)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(danger ? Theme.dangerSoft : Theme.accentSoft, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                VStack(alignment: .leading, spacing: 2) {
                    Text(detail.heroCaption).font(Theme.semi(15)).foregroundStyle(Theme.text)
                    Text(detail.heroSub).font(Theme.body(13)).foregroundStyle(Theme.muted)
                }
            }
            .padding(16)
            .softCard()
            if !detail.sectionLabel.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text(detail.sectionLabel).font(Theme.semi(15)).foregroundStyle(Theme.text)
                    HStack(spacing: 10) {
                        statTile(detail.statLeftValue, detail.statLeftLabel)
                        statTile(detail.statRightValue, detail.statRightLabel)
                    }
                }
                .padding(16)
                .softCard()
            }
        }
    }

    private func statTile(_ value: String, _ label: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value).font(Theme.title(22)).foregroundStyle(Theme.success)
            Text(label).font(Theme.body(13)).foregroundStyle(Theme.muted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(Theme.bg, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func subscription(_ detail: DetailModel) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                Text(detail.merchantInitial)
                    .font(Theme.title(20))
                    .foregroundStyle(.white)
                    .frame(width: 48, height: 48)
                    .background(parseHex(detail.merchantColorHex.isEmpty ? "#16A34A" : detail.merchantColorHex), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                VStack(alignment: .leading, spacing: 2) {
                    Text(detail.merchantName).font(Theme.title(16)).foregroundStyle(Theme.text)
                    Text(detail.merchantMeta).font(Theme.body(13)).foregroundStyle(Theme.muted)
                }
            }
            let count = Int(detail.factCount())
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                ForEach(0..<count, id: \.self) { index in
                    let fact = detail.factAt(index: Int32(index))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(fact.value)
                            .font(Theme.title(20))
                            .foregroundStyle(fact.tone == "WARNING" ? Theme.warning : Theme.text)
                        Text(fact.label).font(Theme.body(12)).foregroundStyle(Theme.muted)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(14)
                    .background(Theme.bg, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
            }
        }
        .padding(16)
        .softCard()
    }
}
