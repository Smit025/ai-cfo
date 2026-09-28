import SwiftUI
import Shared

struct HomeView: View {
    @EnvironmentObject private var model: AppModel
    var wide: Bool

    var body: some View {
        let home = model.controller.home()
        let _ = model.revision
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
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
                        .foregroundStyle(.white)
                        .frame(width: 48, height: 48)
                        .background(Theme.accent, in: Circle())
                }
                HStack(spacing: 10) {
                    Circle().fill(Theme.accent).frame(width: 8, height: 8)
                    Text(home.pulse).font(Theme.semi(14)).foregroundStyle(Theme.text)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .softCard(radius: 18)
                HStack {
                    Text(home.sectionTitle).font(Theme.title(20)).foregroundStyle(Theme.text)
                    Spacer()
                    Button(home.seeAllLabel) { model.controller.selectTab(value: "MOVES") }
                        .font(Theme.semi(15))
                        .foregroundStyle(Theme.accent)
                }
                if home.moveCount() == 0 {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(home.emptyTitle).font(Theme.semi(17)).foregroundStyle(Theme.text)
                        Text(home.emptyBody).font(Theme.body(14)).foregroundStyle(Theme.muted)
                    }
                    .padding(20)
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
    }

    private func homeCard(_ card: HomeMoveModel) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                icon(card.icon)
                Spacer()
                ImpactPill(text: card.impact)
            }
            Text(card.title).font(Theme.title(18)).foregroundStyle(Theme.text)
            Text(card.body).font(Theme.body(14)).foregroundStyle(Theme.muted).fixedSize(horizontal: false, vertical: true)
            Text("\(card.cta)  →").font(Theme.semi(15)).foregroundStyle(Theme.accent)
        }
        .padding(18)
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
            symbol = "dollarsign"; fg = Theme.success; bg = Theme.successSoft
        }
        return Image(systemName: symbol)
            .foregroundStyle(fg)
            .frame(width: 40, height: 40)
            .background(bg, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}
