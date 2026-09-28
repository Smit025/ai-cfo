import SwiftUI
import Shared

struct MovesView: View {
    @EnvironmentObject private var model: AppModel
    var selectedId: String?
    var onOpen: (String) -> Void

    var body: some View {
        let board = model.controller.moves()
        let _ = model.revision
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(board.title).font(Theme.title(30)).foregroundStyle(Theme.text)
                Text(board.subtitle).font(Theme.body(14)).foregroundStyle(Theme.muted)
                Text(board.summaryPill)
                    .font(Theme.semi(13))
                    .foregroundStyle(Theme.accent)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Theme.accentSoft, in: Capsule())
                HStack(spacing: 10) {
                    stat("\(board.todoCount)", "To do")
                    stat("\(board.doneCount)", "Done")
                    stat("\(board.skippedCount)", "Skipped")
                }
                ForEach(0..<Int(board.moveCount()), id: \.self) { index in
                    let row = board.moveAt(index: Int32(index))
                    Button { onOpen(row.id) } label: {
                        rowView(row, selected: row.id == selectedId)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 120)
        }
    }

    private func stat(_ value: String, _ label: String) -> some View {
        VStack(spacing: 2) {
            Text(value).font(Theme.title(22)).foregroundStyle(Theme.text)
            Text(label).font(Theme.body(13)).foregroundStyle(Theme.muted)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .softCard(radius: 20)
    }

    private func rowView(_ row: MoveRowModel, selected: Bool) -> some View {
        let fill = selected ? Theme.accentSoft : (row.showCheck ? Theme.doneCard : .white)
        return HStack(alignment: .center, spacing: 12) {
            if row.showCheck {
                Image(systemName: "checkmark")
                    .foregroundStyle(Theme.success)
                    .frame(width: 36, height: 36)
                    .background(Theme.successSoft, in: Circle())
            } else {
                Text("\(row.rank)")
                    .font(Theme.semi(14))
                    .foregroundStyle(Theme.accent)
                    .frame(width: 36, height: 36)
                    .background(Theme.accentSoft, in: Circle())
            }
            VStack(alignment: .leading, spacing: 6) {
                Text(row.title).font(Theme.semi(15)).foregroundStyle(Theme.text).multilineTextAlignment(.leading)
                HStack(spacing: 6) {
                    MetaPill(text: row.priority, tone: row.priority)
                    MetaPill(text: row.statusLabel, tone: row.status)
                }
            }
            Spacer(minLength: 8)
            ImpactPill(text: row.impact, tone: row.impactTone)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .softCard(fill: fill)
    }
}
