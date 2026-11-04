import ActivityKit
import AppIntents
import SwiftUI
import WidgetKit

/// Live Activity görünümü (kilit ekranı ve Dynamic Island): zamanlayıcı, pomodoro, kronometre, sıradaki vakit,
/// gün doğumu/batımı ve hava durumu. Süre sistemin sayacıyla akar (Text(timerInterval:)), uygulamanın her saniye
/// güncellemesi gerekmez. Hava durumunda süre yerine sıcaklık durur; × düğmesi etkinliği kapatır (LiveWeatherOffIntent).
struct SaatLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: SaatActivityAttributes.self) { context in
            Group {
                if context.attributes.isWeather {
                    LiveWeatherLockScreen(state: context.state, stale: context.isStale)
                        .activityBackgroundTint(LiveWeatherStyle.sky)
                        .activitySystemActionForegroundColor(.white)
                } else {
                    LiveLockScreen(attributes: context.attributes, state: context.state, stale: context.isStale)
                        .activityBackgroundTint(Theme.bg)
                        .activitySystemActionForegroundColor(Theme.accent)
                }
            }
            // Hava kartına dokununca Hava sekmesi; diğerlerinde uygulama kaldığı yerde açılır
            .widgetURL(context.attributes.isWeather ? URL(string: "saattarih://weather") : nil)
        } dynamicIsland: { context in
            let a = context.attributes, s = context.state
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Label {
                        Text(s.title).lineLimit(1)
                    } icon: {
                        if a.isWeather {
                            LiveWeatherStyle.glyph(s.symbol)
                        } else {
                            Image(systemName: a.symbol).foregroundStyle(Theme.accent)
                        }
                    }
                    .font(.subheadline.weight(.semibold))
                }
                DynamicIslandExpandedRegion(.trailing) {
                    if a.isWeather {
                        Text(s.value ?? "")
                            .font(.title2.weight(.medium))
                            .foregroundStyle(.white)
                            .environment(\.layoutDirection, .leftToRight)
                    } else {
                        LiveTime(attributes: a, state: s)
                            .font(.title2.weight(.semibold).monospacedDigit())
                            .foregroundStyle(Theme.accent)
                    }
                }
                DynamicIslandExpandedRegion(.bottom) {
                    if a.isWeather {
                        HStack(spacing: 10) {
                            VStack(alignment: .leading, spacing: 4) {
                                if !s.subtitle.isEmpty {
                                    Text(s.subtitle).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                                }
                                LiveWeatherStyle.mark
                            }
                            Spacer(minLength: 8)
                            LiveWeatherStyle.closeButton
                        }
                    } else {
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
                }
            } compactLeading: {
                if a.isWeather {
                    LiveWeatherStyle.glyph(s.symbol)
                } else {
                    Image(systemName: a.symbol).foregroundStyle(Theme.accent)
                }
            } compactTrailing: {
                if a.isWeather {
                    Text(s.value ?? "")
                        .foregroundStyle(.white)
                        .environment(\.layoutDirection, .leftToRight)
                } else {
                    LiveTime(attributes: a, state: s)
                        .monospacedDigit()
                        .frame(maxWidth: 64)
                        .foregroundStyle(Theme.accent)
                }
            } minimal: {
                if a.isWeather {
                    Text(s.value ?? "")
                        .font(.caption.weight(.semibold))
                        .minimumScaleFactor(0.6)
                        .foregroundStyle(.white)
                        .environment(\.layoutDirection, .leftToRight)
                } else {
                    Image(systemName: a.symbol).foregroundStyle(Theme.accent)
                }
            }
            .widgetURL(a.isWeather ? URL(string: "saattarih://weather") : nil)
            .keylineTint(a.isWeather ? LiveWeatherStyle.sky : Theme.accent)
        }
    }
}

/// Hava etkinliğinin ortak parçaları
enum LiveWeatherStyle {
    /// Kilit ekranındaki kartın zemini: gündüz göğü mavisi (beyaz bulut ve yazı okunur)
    static let sky = Color(red: 0.20, green: 0.42, blue: 0.68)

    /// Çok renkli hava simgesi (WeatherKit'in simgesi; güneş sarı, bulut beyaz, yağmur mavi)
    static func glyph(_ name: String?) -> some View {
        Image(systemName: name ?? "cloud.sun.fill").symbolRenderingMode(.multicolor)
    }

    /// Apple Hava Durumu işareti ve yasal bilgiler (WeatherKit şartı; bağlantı uygulamayı açar, uygulama Safari'ye verir)
    static var mark: some View {
        HStack(spacing: 8) {
            Text("\u{F8FF} Weather").font(.caption2.weight(.semibold))
            Link(L("weather.legal"), destination: URL(string: "https://weatherkit.apple.com/legal-attribution.html")!)
                .font(.caption2)
        }
        .foregroundStyle(.white.opacity(0.85))
    }

    /// × : hava etkinliğini kapatır (Ayarlardaki anahtar da kapanır)
    static var closeButton: some View {
        Button(intent: LiveWeatherOffIntent()) {
            Image(systemName: "xmark")
                .font(.footnote.weight(.bold))
                .foregroundStyle(.white)
                .frame(width: 30, height: 30)
                .background(.white.opacity(0.22), in: Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(L("live.weather.hide"))
    }
}

/// Kilit ekranında hava: simge, durum, yer ve en yüksek/en düşük, büyük sıcaklık; altta işaret ve ×
private struct LiveWeatherLockScreen: View {
    let state: SaatActivityAttributes.ContentState
    let stale: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 12) {
                LiveWeatherStyle.glyph(state.symbol)
                    .font(.system(size: 38))
                VStack(alignment: .leading, spacing: 2) {
                    Text(state.title).font(.headline).foregroundStyle(.white).lineLimit(1)
                    if !state.subtitle.isEmpty {
                        Text(state.subtitle).font(.subheadline).foregroundStyle(.white.opacity(0.8)).lineLimit(1)
                    }
                }
                Spacer(minLength: 8)
                Text(state.value ?? "")
                    .font(.system(size: 40, weight: .medium, design: .rounded))
                    .foregroundStyle(.white)
                    .environment(\.layoutDirection, .leftToRight)
            }
            HStack {
                LiveWeatherStyle.mark
                Spacer(minLength: 8)
                LiveWeatherStyle.closeButton
            }
        }
        .padding(16)
        .opacity(stale ? 0.6 : 1)
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
                        Text(state.subtitle).font(.subheadline).foregroundStyle(Theme.muted).lineLimit(2)
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
