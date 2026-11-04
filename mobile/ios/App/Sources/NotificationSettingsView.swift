import SwiftUI
import UIKit
import SaatCore

/// Vakit bildirimleri ayarı: aç/kapat, hangi vakitler, kaç dakika önce.
struct NotificationSettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @AppStorage(NotificationPrefs.enabledKey) private var enabled = true
    @AppStorage(NotificationPrefs.prayersKey) private var prayersRaw = NotificationPrefs.defaultPrayers
    @AppStorage(NotificationPrefs.leadKey) private var lead = 0
    @AppStorage(SoundMode.key) private var sound = SoundMode.voice.rawValue
    @AppStorage(AdhanVoice.key) private var adhanVoice = AdhanVoice.azeez.rawValue
    @State private var denied = false
    /// Kilit ekranında / Dynamic Island'da sıradaki vakit (Live Activity; 2. analiz #199 ile Vakitler'den buraya)
    @AppStorage(LiveActivities.prayerKey) private var liveOn = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle(L("notify.enable"), isOn: Binding(get: { enabled }, set: setEnabled))
                } footer: {
                    Text(L("notify.note"))
                }
                Section {
                    Toggle(isOn: $liveOn) { Label(L("live.prayer.toggle"), systemImage: "lock.iphone") }
                        .onChange(of: liveOn) { LiveActivities.sync() }
                } footer: {
                    Text(L("live.prayer.note"))
                }
                if denied {
                    Section {
                        Text(L("notify.denied"))
                        Button(L("notify.openSettings")) {
                            if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                        }
                    }
                }
                PrayerAlarmSection()
                if enabled {
                    Section(L("notify.prayers")) {
                        ForEach(Prayer.allCases, id: \.self) { prayer in
                            Toggle(prayer.localizedName, isOn: selected(prayer))
                        }
                    }
                    Section {
                        ForEach(Prayer.allCases.filter { NotificationPrefs.decode(prayersRaw).contains($0) }, id: \.self) { prayer in
                            NavigationLink { PerPrayerSettings(prayer: prayer) } label: { Text(prayer.localizedName) }
                        }
                    } header: {
                        Text(L("notify.perPrayer"))
                    } footer: {
                        Text(L("notify.perPrayer.note"))
                    }
                    Section {
                        Picker(L("notify.lead"), selection: $lead) {
                            ForEach(PrayerAlerts.Settings.leadChoices, id: \.self) { m in
                                Text(m == 0 ? L("notify.lead.zero") : String(format: L("notify.lead.minutes"), String(m))).tag(m)
                            }
                        }
                    }
                    Section {
                        Picker(L("notify.sound"), selection: $sound) {
                            ForEach(SoundMode.allCases, id: \.rawValue) { Text($0.label).tag($0.rawValue) }
                        }
                        if sound == SoundMode.adhan.rawValue {
                            Picker(L("notify.adhanVoice"), selection: $adhanVoice) {
                                ForEach(AdhanVoice.allCases, id: \.rawValue) { Text($0.label).tag($0.rawValue) }
                            }
                        }
                        Button {
                            Task { await NotificationSounds.preview(mode: SoundMode(rawValue: sound) ?? .voice, lead: lead,
                                                                     voice: AdhanVoice(rawValue: adhanVoice) ?? .azeez) }
                        } label: {
                            Label(L("notify.sound.preview"), systemImage: "play.circle")
                        }
                    } footer: {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(L("notify.sound.note"))
                            if sound == SoundMode.adhan.rawValue {
                                Text(L("notify.sound.adhanNote"))
                                Text((AdhanVoice(rawValue: adhanVoice) ?? .azeez).credit).font(.caption2)
                            }
                            if sound == SoundMode.voice.rawValue && NotificationSounds.hasOnlyBasicVoice {
                                Text(L("notify.sound.better"))
                            }
                        }
                    }
                }
            }
            .themedBackground()
            .tint(Theme.accent)
            .navigationTitle(L("notify.heading"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L("common.done")) { dismiss() }
                }
            }
        }
        .task { await refreshDenied() }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
            Task { await refreshDenied() }
        }
        .onChange(of: prayersRaw) { Task { await PrayerNotifier.reschedule() } }
        .onChange(of: lead) { Task { await PrayerNotifier.reschedule() } }
        .onChange(of: sound) { Task { await PrayerNotifier.reschedule() } }
        .onChange(of: adhanVoice) { Task { await PrayerNotifier.reschedule() } }
    }

    private func selected(_ prayer: Prayer) -> Binding<Bool> {
        Binding {
            NotificationPrefs.decode(prayersRaw).contains(prayer)
        } set: { on in
            var set = NotificationPrefs.decode(prayersRaw)
            if on { set.insert(prayer) } else { set.remove(prayer) }
            prayersRaw = NotificationPrefs.encode(set)
        }
    }

    /// Açarken izin istenir (ilk seferde iOS sorar); izin yoksa ayar kapalı kalır ve Ayarlar'a yönlendirilir.
    private func setEnabled(_ on: Bool) {
        guard on else {
            enabled = false
            Task { await PrayerNotifier.reschedule() }
            return
        }
        Task {
            let status = await PrayerNotifier.authorizationStatus()
            var ok = PrayerNotifier.isAllowed(status)
            if status == .notDetermined { ok = await PrayerNotifier.requestAuthorization() }
            enabled = ok
            denied = !ok
            await PrayerNotifier.reschedule()
        }
    }

    private func refreshDenied() async {
        let status = await PrayerNotifier.authorizationStatus()
        denied = status == .denied && enabled
    }
}

/// Vakte göre ayar (2. analiz #54): bu vakit için önceden bildirim süresi, ses ve günler; boşsa genel ayar
private struct PerPrayerSettings: View {
    let prayer: Prayer
    @State private var lead = -1
    @State private var sound = ""
    @State private var days: Set<Int> = []

    var body: some View {
        Form {
            Picker(L("notify.lead"), selection: $lead) {
                Text(L("notify.perPrayer.general")).tag(-1)
                ForEach(PrayerAlerts.Settings.leadChoices, id: \.self) { m in
                    Text(m == 0 ? L("notify.lead.zero") : String(format: L("notify.lead.minutes"), String(m))).tag(m)
                }
            }
            Picker(L("notify.sound"), selection: $sound) {
                Text(L("notify.perPrayer.general")).tag("")
                ForEach(SoundMode.allCases, id: \.rawValue) { Text($0.label).tag($0.rawValue) }
            }
            Section(L("notify.perPrayer.days")) {
                ForEach(1...7, id: \.self) { d in
                    Toggle(L("tools.alarm.dayNames.\(d - 1)"), isOn: dayBinding(d))
                }
            }
        }
        .navigationTitle(prayer.localizedName)
        .onAppear(perform: load)
        .onChange(of: lead) { save() }
        .onChange(of: sound) { save() }
        .onChange(of: days) { save() }
    }

    private func dayBinding(_ d: Int) -> Binding<Bool> {
        Binding(get: { days.contains(d) }, set: { on in
            if on { days.insert(d) } else { days.remove(d) }
        })
    }

    private func load() {
        let ud = UserDefaults.standard
        lead = (ud.object(forKey: NotificationPrefs.perLeadKey(prayer)) as? Int) ?? -1
        sound = ud.string(forKey: NotificationPrefs.perSoundKey(prayer)) ?? ""
        days = NotificationPrefs.daysOf(prayer)
    }

    private func save() {
        let ud = UserDefaults.standard
        ud.set(lead, forKey: NotificationPrefs.perLeadKey(prayer))
        ud.set(sound, forKey: NotificationPrefs.perSoundKey(prayer))
        ud.set(days.sorted().map(String.init).joined(separator: ","), forKey: NotificationPrefs.perDaysKey(prayer))
        Task { await PrayerNotifier.reschedule() }
    }
}
