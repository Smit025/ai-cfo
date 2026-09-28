import SwiftUI
import Shared

struct RootView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        let _ = model.revision
        let gate = model.controller.gate()
        ZStack {
            Theme.bg.ignoresSafeArea()
            switch gate {
            case "ONBOARDING":
                OnboardingView()
            case "LOCK":
                LockView()
            case "PAYWALL":
                PaywallView()
            default:
                MainShell()
            }
        }
        .onAppear { model.refreshHardware() }
    }
}

struct MainShell: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        let _ = model.revision
        GeometryReader { geo in
            let wide = geo.size.width >= 720
            ZStack(alignment: .bottom) {
                Group {
                    if wide && model.controller.tab() == "MOVES" {
                        HStack(spacing: 0) {
                            MovesView(selectedId: model.controller.resolvedMoveId()) { id in
                                model.controller.selectMove(id: id)
                            }
                            .frame(maxWidth: .infinity)
                            Rectangle().fill(Theme.bg).frame(width: 12)
                            ActionDetailView(moveId: model.controller.resolvedMoveId(), showBack: false)
                                .frame(maxWidth: .infinity)
                        }
                    } else if model.showingDetail && !wide {
                        ActionDetailView(
                            moveId: model.controller.selectedMoveId() ?? model.controller.resolvedMoveId(),
                            showBack: true
                        )
                    } else {
                        tabBody(wide: wide)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                if !(model.showingDetail && !wide) {
                    PillNav(selected: model.controller.tab()) { tab in
                        model.showingDetail = false
                        model.controller.selectTab(value: tab)
                    }
                    .padding(.bottom, 10)
                }
            }
        }
    }

    @ViewBuilder
    private func tabBody(wide: Bool) -> some View {
        let tab = model.controller.tab()
        Group {
            switch tab {
            case "MOVES":
                MovesView(selectedId: nil) { id in
                    model.openMove(id, wide: wide)
                }
            case "ACCOUNTS":
                AccountsView()
            case "SETTINGS":
                SettingsView()
            default:
                HomeView(wide: wide)
            }
        }
        .frame(maxWidth: wide && tab != "MOVES" ? 520 : .infinity)
        .frame(maxWidth: .infinity)
    }
}
