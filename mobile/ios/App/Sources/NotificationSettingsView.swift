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

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle(L("notify.enable"), isOn: Binding(get: { enabled }, set: setEnabled))
                } footer: {
                    Text(L("notify.note"))
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
            .navigationBarTitleDisplayMode(.inline)
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
