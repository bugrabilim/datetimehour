import AppIntents
import SwiftUI
import WidgetKit

/* Dokunulabilen widget'lar ve Denetim Merkezi düğmeleri (30 Ekim 2026, rakip analizi listesi #77).
   App Group olmadığı için widget'lar kendi sayaçlarını widget uzantısının kayıtlarında tutar (uygulamadaki zikirmatik ve
   kronometreden ayrıdır). Denetim Merkezi düğmeleri (iOS 18+) uygulamayı saattarih:// bağlantısıyla ilgili ekranda açar. */

private let store = UserDefaults.standard

// MARK: Zikirmatik

enum WidgetZikir {
    static let key = "w.zikir"
    static var count: Int { store.integer(forKey: key) }
}

struct ZikirTapIntent: AppIntent {
    static var title: LocalizedStringResource = "widget.zikir.name"
    static var isDiscoverable = false
    func perform() async throws -> some IntentResult {
        store.set(WidgetZikir.count + 1, forKey: WidgetZikir.key)
        return .result()
    }
}

struct ZikirResetIntent: AppIntent {
    static var title: LocalizedStringResource = "widget.reset"
    static var isDiscoverable = false
    func perform() async throws -> some IntentResult {
        store.set(0, forKey: WidgetZikir.key)
        return .result()
    }
}

struct ToolEntry: TimelineEntry {
    let date: Date
}

/// Durum dokunuşla değişir (WidgetKit niyetten sonra yeniden çizer); zaman çizelgesi tek kayıt
struct ToolProvider: TimelineProvider {
    func placeholder(in context: Context) -> ToolEntry { ToolEntry(date: Date()) }
    func getSnapshot(in context: Context, completion: @escaping (ToolEntry) -> Void) { completion(ToolEntry(date: Date())) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<ToolEntry>) -> Void) {
        completion(Timeline(entries: [ToolEntry(date: Date())], policy: .never))
    }
}

struct ZikirWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "zikir-counter", provider: ToolProvider()) { _ in
            let n = WidgetZikir.count
            VStack(spacing: 4) {
                HStack {
                    Text(L("widget.zikir.name")).font(.caption.weight(.semibold)).foregroundStyle(Theme.muted)
                    Spacer()
                    Button(intent: ZikirResetIntent()) {
                        Image(systemName: "arrow.counterclockwise").font(.caption.weight(.semibold))
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(Theme.muted)
                    .accessibilityLabel(L("widget.reset"))
                }
                Button(intent: ZikirTapIntent()) {
                    VStack(spacing: 2) {
                        Text("\(n)")
                            .font(.system(size: 40, weight: .bold, design: .rounded))
                            .monospacedDigit()
                            .minimumScaleFactor(0.5)
                            .lineLimit(1)
                            .contentTransition(.numericText())
                        // 33'lük tur göstergesi
                        ProgressView(value: Double(n % 33), total: 33).tint(Theme.accent)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
            .foregroundStyle(Theme.text)
            .environment(\.layoutDirection, .leftToRight)
            .containerBackground(for: .widget) { Theme.bg }
        }
        .configurationDisplayName(L("widget.zikir.name"))
        .description(L("widget.zikir.desc"))
        .supportedFamilies([.systemSmall])
    }
}

// MARK: Kronometre

enum WidgetStopwatch {
    static let startKey = "w.sw.start", accKey = "w.sw.acc"
    /// Çalışırken başlangıç (Unix sn; 0 = durdu) ve önceki toplam (sn)
    static var start: Double { store.double(forKey: startKey) }
    static var acc: Double { store.double(forKey: accKey) }
    static func elapsed(_ now: Date) -> Double { acc + (start > 0 ? now.timeIntervalSince1970 - start : 0) }
}

struct StopwatchToggleIntent: AppIntent {
    static var title: LocalizedStringResource = "tools.hub.cards.stopwatch.name"
    static var isDiscoverable = false
    func perform() async throws -> some IntentResult {
        let now = Date()
        if WidgetStopwatch.start > 0 {
            store.set(WidgetStopwatch.elapsed(now), forKey: WidgetStopwatch.accKey)
            store.set(0.0, forKey: WidgetStopwatch.startKey)
        } else {
            store.set(now.timeIntervalSince1970, forKey: WidgetStopwatch.startKey)
        }
        return .result()
    }
}

struct StopwatchResetIntent: AppIntent {
    static var title: LocalizedStringResource = "widget.reset"
    static var isDiscoverable = false
    func perform() async throws -> some IntentResult {
        store.set(0.0, forKey: WidgetStopwatch.startKey)
        store.set(0.0, forKey: WidgetStopwatch.accKey)
        return .result()
    }
}

struct StopwatchWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "stopwatch-tool", provider: ToolProvider()) { entry in
            let running = WidgetStopwatch.start > 0
            let elapsed = WidgetStopwatch.elapsed(entry.date)
            VStack(spacing: 8) {
                Text(L("tools.hub.cards.stopwatch.name")).font(.caption.weight(.semibold)).foregroundStyle(Theme.muted)
                Group {
                    if running {
                        // Sistem sayar: başlangıç geriye kaydırılmış sayaç
                        Text(timerInterval: Date(timeIntervalSince1970: WidgetStopwatch.start - WidgetStopwatch.acc)...Date.distantFuture,
                             countsDown: false)
                    } else {
                        Text(Self.format(elapsed))
                    }
                }
                .font(.system(size: 30, weight: .semibold, design: .rounded))
                .monospacedDigit()
                .multilineTextAlignment(.center)
                .minimumScaleFactor(0.5)
                .lineLimit(1)
                HStack(spacing: 10) {
                    Button(intent: StopwatchResetIntent()) {
                        Image(systemName: "arrow.counterclockwise").frame(width: 40, height: 32)
                    }
                    .tint(Theme.muted)
                    .disabled(running || elapsed == 0)
                    .accessibilityLabel(L("widget.reset"))
                    Button(intent: StopwatchToggleIntent()) {
                        Image(systemName: running ? "pause.fill" : "play.fill").frame(width: 40, height: 32)
                    }
                    .tint(Theme.accent)
                    .accessibilityLabel(L(running ? "tools.stopwatch.stop" : "tools.stopwatch.start"))
                }
                .buttonStyle(.bordered)
            }
            .foregroundStyle(Theme.text)
            .environment(\.layoutDirection, .leftToRight)
            .containerBackground(for: .widget) { Theme.bg }
        }
        .configurationDisplayName(L("tools.hub.cards.stopwatch.name"))
        .description(L("widget.stopwatch.desc"))
        .supportedFamilies([.systemSmall])
    }

    static func format(_ t: Double) -> String {
        let s = Int(t)
        return s >= 3600 ? String(format: "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60) : String(format: "%02d:%02d", s / 60, s % 60)
    }
}

// MARK: Denetim Merkezi (iOS 18+)

/// Uygulamayı saattarih://<yol> bağlantısıyla açan düğme
@available(iOSApplicationExtension 18.0, *)
private func appLink(kind: String, titleKey: String, symbol: String, path: String) -> some ControlWidgetConfiguration {
    StaticControlConfiguration(kind: kind) {
        ControlWidgetButton(action: OpenURLIntent(URL(string: "saattarih://\(path)")!)) {
            Label(L(titleKey), systemImage: symbol)
        }
    }
    .displayName(LocalizedStringResource(stringLiteral: titleKey))
}

@available(iOSApplicationExtension 18.0, *)
struct ClockControl: ControlWidget {
    var body: some ControlWidgetConfiguration {
        appLink(kind: "tr.bumba.time.control.clock", titleKey: "control.clock", symbol: "clock", path: "fullscreen")
    }
}

@available(iOSApplicationExtension 18.0, *)
struct TimerControl: ControlWidget {
    var body: some ControlWidgetConfiguration {
        appLink(kind: "tr.bumba.time.control.timer", titleKey: "control.timer", symbol: "timer", path: "timer")
    }
}

@available(iOSApplicationExtension 18.0, *)
struct PrayerControl: ControlWidget {
    var body: some ControlWidgetConfiguration {
        appLink(kind: "tr.bumba.time.control.prayer", titleKey: "control.prayer", symbol: "moon.stars", path: "prayer")
    }
}
