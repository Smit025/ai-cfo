import SwiftUI
import Shared

struct PaywallView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        let paywall = model.controller.paywall()
        let _ = model.revision
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text("Finwise Pro")
                    .font(Theme.semi(13))
                    .foregroundStyle(Theme.accent)
                    .padding(.top, 28)
                Text(paywall.title).font(Theme.title(32)).foregroundStyle(Theme.text)
                Text(paywall.lede).font(Theme.body(16)).foregroundStyle(Theme.muted)
                VStack(alignment: .leading, spacing: 8) {
                    Text(paywall.yearlyNote).font(Theme.semi(13)).foregroundStyle(Theme.accent)
                    Text(paywall.yearlyPrice).font(Theme.title(36)).foregroundStyle(Theme.text)
                    Text(paywall.yearlyPeriod).font(Theme.body(14)).foregroundStyle(Theme.muted)
                    PrimaryButton(label: paywall.yearlyCta) { _ = model.controller.purchaseYearly() }
                        .padding(.top, 8)
                }
                .padding(18)
                .softCard()
                VStack(alignment: .leading, spacing: 8) {
                    Text(paywall.monthlyPrice).font(Theme.title(28)).foregroundStyle(Theme.text)
                    Text(paywall.monthlyPeriod).font(Theme.body(14)).foregroundStyle(Theme.muted)
                    SecondaryButton(label: paywall.monthlyCta) { _ = model.controller.purchaseMonthly() }
                }
                .padding(18)
                .softCard()
                Text(paywall.finePrint).font(Theme.body(13)).foregroundStyle(Theme.muted)
                Text("QA").font(Theme.semi(13)).foregroundStyle(Theme.text)
                SecondaryButton(label: "QA: return to trial") { model.controller.debugForceTrial() }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
            .frame(maxWidth: 480)
            .frame(maxWidth: .infinity)
        }
    }
}
