import SwiftUI
import UserNotifications
import WatchKit

/// Saatte zaman araçları (2. analiz #137; 4 Kasım 2026): zamanlayıcı (hazır süreler 5/10/15/20/30 dk), kronometre,
/// pomodoro (25/5), dünya saati (iPhone'daki listenin ilk 8 yeri; gelmediyse 6 büyük şehir) ve zikirmatik.
/// Süre bitince saat kendi yerel bildirimini verir (uygulama kapalıyken de); açıkken dokunsal uyarı. Kayıtlar yalnız
/// saatte. Alarmlar telefondan saate yansıdığı için saatte ayrı alarm listesi yok.
struct WatchToolsList: View {
    var body: some View {
        List {
            NavigationLink { WatchTimer() } label: { Label(L("watch.timer"), systemImage: "timer") }
            NavigationLink { WatchStopwatch() } label: { Label(L("watch.stopwatch"), systemImage: "stopwatch") }
            NavigationLink { WatchPomodoro() } label: { Label(L("watch.pomodoro"), systemImage: "leaf") }
            NavigationLink { WatchWorld() } label: { Label(L("watch.world"), systemImage: "globe") }
            NavigationLink { WatchZikir() } label: { Label(L("watch.zikir"), systemImage: "circle.grid.cross") }
        }
        .navigationTitle(L("watch.tools"))
    }
}

enum WatchNotify {
    static func schedule(id: String, at date: Date, title: String) {
        let c = UNUserNotificationCenter.current()
        c.requestAuthorization(options: [.alert, .sound]) { ok, _ in
            guard ok else { return }
            let content = UNMutableNotificationContent()
            content.title = title
            content.sound = .default
            let t = UNTimeIntervalNotificationTrigger(timeInterval: max(1, date.timeIntervalSinceNow), repeats: false)
            c.add(UNNotificationRequest(identifier: id, content: content, trigger: t))
        }
    }

    static func cancel(_ id: String) {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [id])
    }
}

private func minutesText(_ m: Int) -> String { String(format: L("watch.minutes"), "\(m)") }

struct WatchTimer: View {
    @AppStorage("w.timer.end") private var end = 0.0
    @AppStorage("w.timer.total") private var total = 0.0

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { ctx in
            let now = ctx.date.timeIntervalSince1970
            List {
                if end > now {
                    VStack(spacing: 4) {
                        Text(timerInterval: ctx.date...Date(timeIntervalSince1970: end), countsDown: true)
                            .font(.system(size: 34, weight: .bold, design: .rounded))
                            .monospacedDigit()
                            .foregroundStyle(.orange)
                        ProgressView(value: max(0, min(1, 1 - (end - now) / max(1, total))))
                            .tint(.orange)
                    }
                    .frame(maxWidth: .infinity)
                    Button(L("watch.reset"), role: .destructive) {
                        end = 0
                        WatchNotify.cancel("w-timer")
                    }
                } else {
                    if end > 0 {
                        Text(L("watch.done")).font(.headline).foregroundStyle(.orange)
                            .onAppear { WKInterfaceDevice.current().play(.notification) }
                    }
                    ForEach([5, 10, 15, 20, 30], id: \.self) { m in
                        Button(minutesText(m)) {
                            total = Double(m * 60)
                            end = Date().timeIntervalSince1970 + total
                            WatchNotify.schedule(id: "w-timer", at: Date(timeIntervalSince1970: end), title: L("watch.done"))
                        }
                    }
                }
            }
        }
        .navigationTitle(L("watch.timer"))
    }
}

struct WatchStopwatch: View {
    @AppStorage("w.sw.start") private var start = 0.0
    @AppStorage("w.sw.acc") private var acc = 0.0

    var body: some View {
        TimelineView(.periodic(from: .now, by: 0.1)) { ctx in
            let now = ctx.date.timeIntervalSince1970
            let elapsed = acc + (start > 0 ? now - start : 0)
            VStack(spacing: 10) {
                Text(Self.format(elapsed))
                    .font(.system(size: 30, weight: .bold, design: .rounded))
                    .monospacedDigit()
                HStack {
                    Button(L("watch.reset")) { start = 0; acc = 0 }
                        .disabled(start > 0 || acc == 0)
                    Button(start > 0 ? L("watch.pause") : L("watch.start")) {
                        if start > 0 { acc += now - start; start = 0 } else { start = now }
                        WKInterfaceDevice.current().play(.click)
                    }
                    .tint(start > 0 ? .red : .green)
                }
            }
        }
        .navigationTitle(L("watch.stopwatch"))
    }

    static func format(_ s: Double) -> String {
        let cs = Int(s * 10) % 10, sec = Int(s) % 60, min = Int(s) / 60 % 60, h = Int(s) / 3600
        return h > 0 ? String(format: "%d:%02d:%02d", h, min, sec) : String(format: "%02d:%02d.%d", min, sec, cs)
    }
}

struct WatchPomodoro: View {
    @AppStorage("w.pomo.end") private var end = 0.0
    @AppStorage("w.pomo.phase") private var phase = "work"
    @AppStorage("w.pomo.done") private var done = 0

    private var minutes: Int { phase == "work" ? 25 : 5 }

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { ctx in
            let now = ctx.date.timeIntervalSince1970
            VStack(spacing: 8) {
                Text(phase == "work" ? L("watch.work") : L("watch.break"))
                    .font(.headline)
                    .foregroundStyle(phase == "work" ? .orange : .green)
                if end > now {
                    Text(timerInterval: ctx.date...Date(timeIntervalSince1970: end), countsDown: true)
                        .font(.system(size: 32, weight: .bold, design: .rounded))
                        .monospacedDigit()
                    Button(L("watch.reset"), role: .destructive) {
                        end = 0
                        WatchNotify.cancel("w-pomo")
                    }
                } else {
                    Text(minutesText(minutes)).font(.system(size: 28, weight: .bold, design: .rounded))
                    Button(L("watch.start")) {
                        // Biten aşamadan sonraki aşamaya geç
                        if end > 0 {
                            if phase == "work" { done += 1 }
                            phase = phase == "work" ? "break" : "work"
                        }
                        end = Date().timeIntervalSince1970 + Double(minutes * 60)
                        WatchNotify.schedule(id: "w-pomo", at: Date(timeIntervalSince1970: end),
                                             title: phase == "work" ? L("desk.pomoDone") : L("desk.breakDone"))
                    }
                    .tint(.orange)
                }
                Text("🍅 \(done)").font(.footnote).foregroundStyle(.secondary)
            }
        }
        .navigationTitle(L("watch.pomodoro"))
    }
}

struct WatchWorld: View {
    @ObservedObject private var phone = PhonePlace.shared

    private var list: [(name: String, zone: TimeZone)] {
        if !phone.world.isEmpty { return phone.world }
        return ["Europe/London", "America/New_York", "Asia/Tokyo", "Asia/Dubai", "Europe/Berlin", "Australia/Sydney"].compactMap { id in
            guard let z = TimeZone(identifier: id) else { return nil }
            let city = id.split(separator: "/").last.map { $0.replacingOccurrences(of: "_", with: " ") } ?? id
            return (name: city, zone: z)
        }
    }

    var body: some View {
        TimelineView(.everyMinute) { ctx in
            List(Array(list.enumerated()), id: \.offset) { _, c in
                HStack {
                    Text(c.name).lineLimit(1)
                    Spacer()
                    Text(Self.hm(ctx.date, c.zone)).monospacedDigit().font(.headline)
                }
            }
        }
        .navigationTitle(L("watch.world"))
    }

    static func hm(_ d: Date, _ z: TimeZone) -> String {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = z
        let c = cal.dateComponents([.hour, .minute], from: d)
        return String(format: "%02d:%02d", c.hour ?? 0, c.minute ?? 0)
    }
}

struct WatchZikir: View {
    @AppStorage("w.zikir.count") private var count = 0
    @AppStorage("w.zikir.target") private var target = 33

    var body: some View {
        VStack(spacing: 6) {
            Button {
                count += 1
                let strong = count % 33 == 0 || count == target
                WKInterfaceDevice.current().play(strong ? .success : .click)
            } label: {
                VStack(spacing: 2) {
                    Text("\(count)").font(.system(size: 44, weight: .bold, design: .rounded)).monospacedDigit()
                    Text("/ \(target)").font(.caption).foregroundStyle(.secondary)
                    Text(L("watch.tap")).font(.caption2).foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .buttonStyle(.plain)
            HStack {
                Picker("", selection: $target) {
                    ForEach([33, 99, 100, 500, 1000], id: \.self) { Text("\($0)").tag($0) }
                }
                .labelsHidden()
                .frame(height: 40)
                Button { count = 0 } label: { Image(systemName: "arrow.counterclockwise") }
                    .accessibilityLabel(L("watch.reset"))
            }
        }
        .navigationTitle(L("watch.zikir"))
    }
}
