import ActivityKit
import SwiftUI
import WidgetKit

/// Live Activity görünümü (kilit ekranı ve Dynamic Island): zamanlayıcı, pomodoro, kronometre, sıradaki vakit.
/// Süre sistemin sayacıyla akar (Text(timerInterval:)), uygulamanın her saniye güncellemesi gerekmez.
struct SaatLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: SaatActivityAttributes.self) { context in
            LiveLockScreen(attributes: context.attributes, state: context.state, stale: context.isStale)
                .activityBackgroundTint(Theme.bg)
                .activitySystemActionForegroundColor(Theme.accent)
        } dynamicIsland: { context in
            let a = context.attributes, s = context.state
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Label {
                        Text(s.title).lineLimit(1)
                    } icon: {
                        Image(systemName: a.symbol).foregroundStyle(Theme.accent)
                    }
                    .font(.subheadline.weight(.semibold))
                }
                DynamicIslandExpandedRegion(.trailing) {
                    LiveTime(attributes: a, state: s)
                        .font(.title2.weight(.semibold).monospacedDigit())
                        .foregroundStyle(Theme.accent)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    VStack(alignment: .leading, spacing: 6) {
                        if !s.subtitle.isEmpty {
                            Text(s.subtitle).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                        }
                        if !a.countsUp, s.start < s.date {
                            ProgressView(timerInterval: s.start...s.date, countsDown: true) { EmptyView() } currentValueLabel: { EmptyView() }
                                .tint(Theme.accent)
                        }
                    }
                }
            } compactLeading: {
                Image(systemName: a.symbol).foregroundStyle(Theme.accent)
            } compactTrailing: {
                LiveTime(attributes: a, state: s)
                    .monospacedDigit()
                    .frame(maxWidth: 64)
                    .foregroundStyle(Theme.accent)
            } minimal: {
                Image(systemName: a.symbol).foregroundStyle(Theme.accent)
            }
            .keylineTint(Theme.accent)
        }
    }
}

/// Akan süre: geri sayımda bitişe kalan, kronometrede başlangıçtan geçen
private struct LiveTime: View {
    let attributes: SaatActivityAttributes
    let state: SaatActivityAttributes.ContentState

    var body: some View {
        Group {
            if attributes.countsUp {
                Text(state.date, style: .timer)
            } else {
                Text(timerInterval: min(state.start, state.date)...state.date, countsDown: true)
            }
        }
        .multilineTextAlignment(.trailing)
        .environment(\.layoutDirection, .leftToRight)
    }
}

private struct LiveLockScreen: View {
    let attributes: SaatActivityAttributes
    let state: SaatActivityAttributes.ContentState
    let stale: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 12) {
                Image(systemName: attributes.symbol)
                    .font(.title2)
                    .foregroundStyle(Theme.accent)
                VStack(alignment: .leading, spacing: 2) {
                    Text(state.title).font(.headline).foregroundStyle(Theme.text).lineLimit(1)
                    if !state.subtitle.isEmpty {
                        Text(state.subtitle).font(.subheadline).foregroundStyle(Theme.muted).lineLimit(1)
                    }
                }
                Spacer(minLength: 8)
                LiveTime(attributes: attributes, state: state)
                    .font(.system(size: 34, weight: .semibold, design: .rounded).monospacedDigit())
                    .foregroundStyle(Theme.accent)
                    .opacity(stale ? 0.5 : 1)
            }
            if !attributes.countsUp, state.start < state.date {
                ProgressView(timerInterval: state.start...state.date, countsDown: true) { EmptyView() } currentValueLabel: { EmptyView() }
                    .tint(Theme.accent)
            }
        }
        .padding(16)
    }
}
