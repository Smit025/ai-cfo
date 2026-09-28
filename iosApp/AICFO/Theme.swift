import SwiftUI

enum Theme {
    static let accent = Color(red: 99 / 255, green: 91 / 255, blue: 255 / 255)
    static let accentSoft = Color(red: 238 / 255, green: 240 / 255, blue: 255 / 255)
    static let text = Color(red: 15 / 255, green: 23 / 255, blue: 42 / 255)
    static let muted = Color(red: 139 / 255, green: 147 / 255, blue: 167 / 255)
    static let bg = Color(red: 244 / 255, green: 245 / 255, blue: 247 / 255)
    static let nav = Color(red: 26 / 255, green: 29 / 255, blue: 38 / 255)
    static let success = Color(red: 5 / 255, green: 150 / 255, blue: 105 / 255)
    static let successSoft = Color(red: 231 / 255, green: 248 / 255, blue: 241 / 255)
    static let warning = Color(red: 217 / 255, green: 119 / 255, blue: 6 / 255)
    static let warningSoft = Color(red: 255 / 255, green: 244 / 255, blue: 229 / 255)
    static let danger = Color(red: 220 / 255, green: 38 / 255, blue: 38 / 255)
    static let dangerSoft = Color(red: 253 / 255, green: 236 / 255, blue: 236 / 255)
    static let chip = Color(red: 241 / 255, green: 243 / 255, blue: 246 / 255)
    static let doneCard = Color(red: 244 / 255, green: 251 / 255, blue: 247 / 255)

    static func title(_ size: CGFloat) -> Font { .system(size: size, weight: .bold) }
    static func semi(_ size: CGFloat) -> Font { .system(size: size, weight: .semibold) }
    static func body(_ size: CGFloat) -> Font { .system(size: size, weight: .regular) }
}

struct SoftCard: ViewModifier {
    var radius: CGFloat = 24
    var fill: Color = .white
    func body(content: Content) -> some View {
        content
            .background(
                RoundedRectangle(cornerRadius: radius, style: .continuous)
                    .fill(fill)
                    .shadow(color: Color(red: 0.07, green: 0.09, blue: 0.16).opacity(0.08), radius: 18, x: 0, y: 8)
                    .shadow(color: Color(red: 0.07, green: 0.09, blue: 0.16).opacity(0.04), radius: 2, x: 0, y: 1)
            )
    }
}

extension View {
    func softCard(radius: CGFloat = 24, fill: Color = .white) -> some View {
        modifier(SoftCard(radius: radius, fill: fill))
    }
}

struct PrimaryButton: View {
    let label: String
    var enabled: Bool = true
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(label)
                .font(Theme.semi(16))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Theme.accent.opacity(enabled ? 1 : 0.4))
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                .shadow(color: Theme.accent.opacity(0.28), radius: 12, x: 0, y: 6)
        }
        .disabled(!enabled)
        .buttonStyle(.plain)
    }
}

struct SecondaryButton: View {
    let label: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(label)
                .font(Theme.semi(16))
                .foregroundStyle(Theme.text)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .softCard(radius: 18)
        }
        .buttonStyle(.plain)
    }
}

struct ImpactPill: View {
    let text: String
    var tone: String = "POSITIVE"
    var body: some View {
        let fg: Color = tone == "WARNING" ? Theme.warning : (tone == "DANGER" ? Theme.danger : Theme.success)
        let bg: Color = tone == "WARNING" ? Theme.warningSoft : (tone == "DANGER" ? Theme.dangerSoft : Theme.successSoft)
        Text(text)
            .font(Theme.semi(13))
            .foregroundStyle(fg)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(bg, in: Capsule())
    }
}

struct MetaPill: View {
    let text: String
    let tone: String
    var body: some View {
        let pair: (Color, Color) = {
            switch tone {
            case "P1", "DANGER": return (Theme.dangerSoft, Theme.danger)
            case "P2", "WARNING": return (Theme.warningSoft, Theme.warning)
            case "P3": return (Theme.accentSoft, Theme.accent)
            case "DONE": return (Theme.successSoft, Theme.success)
            default: return (Theme.chip, Color(red: 0.39, green: 0.44, blue: 0.54))
            }
        }()
        Text(text)
            .font(Theme.semi(12))
            .foregroundStyle(pair.1)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(pair.0, in: Capsule())
    }
}

struct PillNav: View {
    let selected: String
    let onSelect: (String) -> Void
    private let tabs: [(String, String, String)] = [
        ("HOME", "Home", "house"),
        ("MOVES", "Moves", "list.bullet"),
        ("ACCOUNTS", "Accounts", "wallet.pass"),
        ("SETTINGS", "Settings", "person"),
    ]
    var body: some View {
        HStack(spacing: 2) {
            ForEach(tabs, id: \.0) { tab in
                if tab.0 == selected {
                    Button { onSelect(tab.0) } label: {
                        HStack(spacing: 8) {
                            Image(systemName: tab.2)
                            Text(tab.1).font(Theme.semi(14))
                        }
                        .foregroundStyle(.white)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Theme.nav, in: Capsule())
                    }
                    .buttonStyle(.plain)
                } else {
                    Button { onSelect(tab.0) } label: {
                        Image(systemName: tab.2)
                            .font(.system(size: 18, weight: .regular))
                            .foregroundStyle(Theme.muted)
                            .frame(width: 44, height: 44)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(6)
        .softCard(radius: 32)
    }
}

struct FreshnessLine: View {
    let label: String
    let action: String
    let code: String
    let onAction: () -> Void

    var body: some View {
        if !label.isEmpty {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(label)
                    .font(Theme.body(12))
                    .foregroundStyle(color)
                    .fixedSize(horizontal: false, vertical: true)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if !action.isEmpty {
                    Button(action, action: onAction)
                        .font(Theme.semi(13))
                        .foregroundStyle(Theme.accent)
                        .buttonStyle(.plain)
                }
            }
        }
    }

    private var color: Color {
        switch code {
        case "NEEDS_REAUTH": return Theme.danger
        case "FAILED": return Theme.warning
        default: return Theme.muted
        }
    }
}

struct ReadOnlyChip: View {
    var body: some View {
        HStack(spacing: 6) {
            Circle().fill(Theme.success).frame(width: 6, height: 6)
            Text("Read-only").font(Theme.semi(11)).foregroundStyle(Theme.success)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 3)
        .background(Theme.successSoft, in: Capsule())
    }
}

func parseHex(_ hex: String) -> Color {
    var raw = hex
    if raw.hasPrefix("#") { raw.removeFirst() }
    var value: UInt64 = 0
    Scanner(string: raw).scanHexInt64(&value)
    return Color(
        red: Double((value >> 16) & 0xFF) / 255,
        green: Double((value >> 8) & 0xFF) / 255,
        blue: Double(value & 0xFF) / 255
    )
}
