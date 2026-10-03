import Combine
import SwiftUI
import Shared

struct HomeView: View {
    @EnvironmentObject private var model: AppModel
    var wide: Bool
    @State private var freshnessTick = Date()

    var body: some View {
        let home = model.controller.home()
        let _ = model.revision
        let _ = freshnessTick
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                header(home)
                wealth(home)
                if !home.hope.isEmpty {
                    hope(home.hope)
                }
                snapshot(home)
                HStack {
                    Text(home.sectionTitle).font(Theme.title(20)).foregroundStyle(Theme.text)
                    Spacer()
                    Button(home.seeAllLabel) { model.controller.selectTab(value: "MOVES") }
                        .font(Theme.semi(15))
                        .foregroundStyle(Theme.accent)
                }
                .padding(.top, 10)
                if home.moveCount() == 0 {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(home.emptyTitle).font(Theme.semi(17)).foregroundStyle(Theme.text)
                        Text(home.emptyBody).font(Theme.body(14)).foregroundStyle(Theme.muted)
                    }
                    .padding(20)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .softCard()
                } else {
                    ForEach(0..<Int(home.moveCount()), id: \.self) { index in
                        let card = home.moveAt(index: Int32(index))
                        Button {
                            model.openMove(card.id, wide: wide)
                        } label: {
                            homeCard(card)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 120)
            .frame(maxWidth: wide ? 480 : .infinity)
            .frame(maxWidth: .infinity)
        }
        .refreshable { await model.pullToRefresh() }
        .onReceive(Timer.publish(every: 30, on: .main, in: .common).autoconnect()) { freshnessTick = $0 }
    }

    private func header(_ home: HomeModel) -> some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 2) {
                Text(home.greeting).font(Theme.title(32)).foregroundStyle(Theme.text)
                Text(home.subtitle).font(Theme.body(15)).foregroundStyle(Theme.muted)
            }
            Spacer()
            ZStack(alignment: .topTrailing) {
                Image(systemName: "bell")
                    .foregroundStyle(Theme.text)
                    .frame(width: 44, height: 44)
                    .softCard(radius: 16)
                if home.showNotificationDot {
                    Circle().fill(Theme.danger).frame(width: 8, height: 8).offset(x: -8, y: 8)
                }
            }
            Text(home.initials)
                .font(Theme.semi(16))
                .foregroundStyle(Theme.accent)
                .frame(width: 48, height: 48)
                .background(Theme.accentSoft, in: Circle())
        }
    }

    private func wealth(_ home: HomeModel) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 12) {
                wealthColumn(home.savingsLabel, home.savingsAmount, home.savingsDelta, home.savingsUp)
                wealthColumn(home.netWorthLabel, home.netWorthAmount, home.netWorthDelta, home.netWorthUp)
            }
            Rectangle().fill(Color(red: 230 / 255, green: 232 / 255, blue: 238 / 255)).frame(height: 1)
            HStack(spacing: 8) {
                Circle().fill(Theme.accent).frame(width: 6, height: 6)
                Text(home.runway).font(Theme.semi(13)).foregroundStyle(Theme.muted)
            }
            if !home.freshnessLabel.isEmpty {
                FreshnessLine(
                    label: home.freshnessLabel,
                    action: home.syncActionLabel,
                    code: home.syncCode,
                    onAction: { model.performSyncAction(code: home.syncCode) }
                )
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .softCard(radius: 22)
    }

    private func wealthColumn(_ label: String, _ amount: String, _ delta: String, _ up: Bool) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(Theme.semi(13)).foregroundStyle(Theme.muted)
            Text(amount).font(Theme.title(28)).foregroundStyle(Theme.text)
            if !delta.isEmpty {
                Text(delta)
                    .font(Theme.semi(12))
                    .foregroundStyle(up ? Theme.success : Theme.muted)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(up ? Theme.successSoft : Theme.chip, in: Capsule())
                    .padding(.top, 4)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func hope(_ text: String) -> some View {
        HStack(alignment: .center, spacing: 10) {
            Image(systemName: "banknote")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(Theme.success)
                .frame(width: 28, height: 28)
                .background(Color(red: 215 / 255, green: 243 / 255, blue: 230 / 255), in: RoundedRectangle(cornerRadius: 9, style: .continuous))
            Text(text)
                .font(Theme.semi(14))
                .foregroundStyle(Theme.success)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.successSoft, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private func snapshot(_ home: HomeModel) -> some View {
        HStack(spacing: 0) {
            let count = Int(home.snapshotCount())
            ForEach(0..<count, id: \.self) { index in
                if index > 0 {
                    Rectangle()
                        .fill(Color(red: 230 / 255, green: 232 / 255, blue: 238 / 255))
                        .frame(width: 1, height: 52)
                }
                let amount = home.snapshotAt(index: Int32(index))
                VStack(spacing: 2) {
                    Text(amount.label).font(Theme.semi(13)).foregroundStyle(Theme.muted)
                    Text(amount.amount)
                        .font(Theme.title(20))
                        .foregroundStyle(amount.tone == "POSITIVE" ? Theme.success : Theme.text)
                    Text(amount.caption)
                        .font(Theme.body(12))
                        .foregroundStyle(Theme.muted)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 6)
            }
        }
        .padding(.vertical, 16)
        .softCard(radius: 22)
    }

    private func homeCard(_ card: HomeMoveModel) -> some View {
        HStack(alignment: .top, spacing: 12) {
            icon(card.icon)
            VStack(alignment: .leading, spacing: 4) {
                Text(card.title).font(Theme.title(16)).foregroundStyle(Theme.text)
                Text(card.body)
                    .font(Theme.body(14))
                    .foregroundStyle(Theme.muted)
                    .fixedSize(horizontal: false, vertical: true)
                Text("\(card.cta)  →")
                    .font(Theme.semi(15))
                    .foregroundStyle(Theme.accent)
                    .padding(.top, 6)
            }
            Spacer(minLength: 8)
            ImpactPill(text: card.impact)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .softCard()
    }

    private func icon(_ key: String) -> some View {
        let symbol: String
        let fg: Color
        let bg: Color
        switch key {
        case "CLOCK":
            symbol = "clock"; fg = Theme.danger; bg = Theme.dangerSoft
        case "CARD":
            symbol = "creditcard"; fg = Theme.warning; bg = Theme.warningSoft
        default:
            symbol = "banknote"; fg = Theme.success; bg = Theme.successSoft
        }
        return Image(systemName: symbol)
            .foregroundStyle(fg)
            .frame(width: 40, height: 40)
            .background(bg, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}
