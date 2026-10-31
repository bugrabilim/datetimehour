import SwiftUI
import UIKit

/// Ayarlar › Bildirimler: her özelliğin bildirimi (varsayılan açık) tek sayfada; vakitlerin ayrıntısı kendi penceresinde.
struct NotificationsView: View {
    @Environment(\.openURL) private var openURL
    @AppStorage(NotificationPrefs.enabledKey) private var prayer = true
    @AppStorage(NotifyFeature.calendar.key) private var calendar = true
    @AppStorage(NotifyFeature.seasons.key) private var seasons = true
    @AppStorage(NotifyFeature.moon.key) private var moon = true
    @AppStorage(NotifyFeature.sun.key) private var sun = true
    @AppStorage(NotifyFeature.weather.key) private var weather = true
    @AppStorage(NotifyFeature.hourly.key) private var hourly = false
    @AppStorage(NotifyFeature.dst.key) private var dst = true
    @AppStorage(NotifyFeature.bedtime.key) private var bedtime = true
    @AppStorage(NotifyFeature.weatherTimeKey) private var weatherTime = 450
    @AppStorage(NotifyFeature.hourlyFromKey) private var hourlyFrom = 9
    @AppStorage(NotifyFeature.hourlyToKey) private var hourlyTo = 21
    @State private var showPrayer = false
    @State private var denied = false

    var body: some View {
        Form {
            Section {
                Text(L("notify.all.note")).font(.footnote)
            }
            if denied {
                Section {
                    Text(L("notify.denied"))
                    Button(L("notify.openSettings")) {
                        if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                    }
                }
            }
            Section {
                row(L("notify.cat.prayer"), L("notify.cat.prayer.note"), "moon.stars", $prayer)
                if prayer {
                    Button(L("notify.heading")) { showPrayer = true }
                }
            }
            Section {
                row(.calendar, $calendar)
                row(.bedtime, $bedtime)
            }
            Section {
                row(.seasons, $seasons)
                row(.moon, $moon)
                row(.sun, $sun)
            }
            Section {
                row(.weather, $weather)
                if weather {
                    DatePicker(L("notify.cat.weather.time"), selection: minutes($weatherTime), displayedComponents: .hourAndMinute)
                }
            }
            Section {
                row(.hourly, $hourly)
                if hourly {
                    hourPicker(L("notify.cat.hourly.from"), $hourlyFrom)
                    hourPicker(L("notify.cat.hourly.to"), $hourlyTo)
                }
                row(.dst, $dst)
            } footer: {
                Text(L("notify.all.limit"))
            }
        }
        .themedBackground()
        .tint(Theme.accent)
        .navigationTitle(L("settings.notifications"))
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showPrayer) { NotificationSettingsView() }
        .task { await refresh() }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
            Task { await refresh() }
        }
        .onChange(of: [prayer, calendar, seasons, moon, sun, weather, hourly, dst, bedtime]) { reschedule() }
        .onChange(of: [weatherTime, hourlyFrom, hourlyTo]) { reschedule() }
    }

    private func row(_ f: NotifyFeature, _ on: Binding<Bool>) -> some View {
        row(f.title, f.note, f.icon, on)
    }

    private func row(_ title: String, _ note: String, _ icon: String, _ on: Binding<Bool>) -> some View {
        Toggle(isOn: on) {
            VStack(alignment: .leading, spacing: 2) {
                Label(title, systemImage: icon)
                Text(note).font(.caption).foregroundStyle(Theme.muted)
            }
        }
    }

    private func hourPicker(_ title: String, _ value: Binding<Int>) -> some View {
        Picker(title, selection: value) {
            ForEach(0..<24, id: \.self) { h in
                let at = Calendar.current.date(bySettingHour: h, minute: 0, second: 0, of: Date()) ?? Date()
                Text([Fmt.time(at), Fmt.ampm(at)].filter { !$0.isEmpty }.joined(separator: " ")).tag(h)
            }
        }
    }

    /// Gece yarısından dakika ↔ saat seçici
    private func minutes(_ value: Binding<Int>) -> Binding<Date> {
        Binding {
            Calendar.current.startOfDay(for: Date()).addingTimeInterval(Double(value.wrappedValue * 60))
        } set: { date in
            let c = Calendar.current.dateComponents([.hour, .minute], from: date)
            value.wrappedValue = (c.hour ?? 7) * 60 + (c.minute ?? 30)
        }
    }

    private func reschedule() {
        Task {
            // İzin hiç sorulmadıysa açarken sorulur
            if await PrayerNotifier.authorizationStatus() == .notDetermined { _ = await PrayerNotifier.requestAuthorization() }
            await PrayerNotifier.reschedule()
            await refresh()
        }
    }

    private func refresh() async {
        denied = await PrayerNotifier.authorizationStatus() == .denied
    }
}
