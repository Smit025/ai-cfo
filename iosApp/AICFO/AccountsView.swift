import Combine
import SwiftUI
import Shared

struct AccountsView: View {
    @EnvironmentObject private var model: AppModel
    @State private var freshnessTick = Date()

    var body: some View {
        let accounts = model.controller.accounts()
        let _ = model.revision
        let _ = freshnessTick
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(accounts.title).font(Theme.title(32)).foregroundStyle(Theme.text)
                Text(accounts.subtitle).font(Theme.body(14)).foregroundStyle(Theme.muted)
                if !accounts.freshnessLabel.isEmpty {
                    FreshnessLine(
                        label: accounts.freshnessLabel,
                        action: accounts.syncActionLabel,
                        code: accounts.syncCode,
                        onAction: { model.performSyncAction(code: accounts.syncCode) }
                    )
                }
                HStack(alignment: .top, spacing: 10) {
                    Image(systemName: "lock")
                        .foregroundStyle(Theme.muted)
                    Text(accounts.trust)
                        .font(Theme.body(13))
                        .foregroundStyle(Theme.muted)
                }
                .padding(14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .softCard(radius: 18)
                if !accounts.linked {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(accounts.emptyTitle).font(Theme.semi(17)).foregroundStyle(Theme.text)
                        Text(accounts.emptyBody).font(Theme.body(14)).foregroundStyle(Theme.muted)
                        if !accounts.linkNote.isEmpty {
                            Text(accounts.linkNote)
                                .font(Theme.body(14))
                                .foregroundStyle(Theme.muted)
                        }
                        if !accounts.linkError.isEmpty {
                            Text(accounts.linkError)
                                .font(Theme.body(14))
                                .foregroundStyle(Theme.danger)
                        }
                        PrimaryButton(label: accounts.emptyCta) {
                            let note = accounts.linkNote.isEmpty
                                ? "Plaid Link is not in the iOS app yet. Adding it needs a Mac."
                                : accounts.linkNote
                            model.controller.reportLinkError(message: note)
                        }
                        .padding(.top, 6)
                    }
                    .padding(18)
                    .softCard()
                } else {
                    ForEach(0..<Int(accounts.groupCount()), id: \.self) { groupIndex in
                        let group = accounts.groupAt(index: Int32(groupIndex))
                        Text(group.title)
                            .font(Theme.semi(12))
                            .foregroundStyle(Theme.muted)
                            .tracking(1.1)
                        ForEach(0..<Int(group.accountCount()), id: \.self) { index in
                            let account = group.accountAt(index: Int32(index))
                            HStack(spacing: 12) {
                                Text(account.initials)
                                    .font(Theme.semi(13))
                                    .foregroundStyle(.white)
                                    .frame(width: 44, height: 44)
                                    .background(parseHex(account.colorHex), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(account.name).font(Theme.semi(15)).foregroundStyle(Theme.text)
                                    Text(account.detail).font(Theme.body(12)).foregroundStyle(Theme.muted)
                                }
                                Spacer()
                                VStack(alignment: .trailing, spacing: 4) {
                                    Text(account.balance).font(Theme.semi(15)).foregroundStyle(Theme.text)
                                    ReadOnlyChip()
                                }
                            }
                            .padding(14)
                            .softCard(radius: 22)
                        }
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 120)
        }
        .refreshable { await model.pullToRefresh() }
        .onReceive(Timer.publish(every: 30, on: .main, in: .common).autoconnect()) { freshnessTick = $0 }
    }
}
