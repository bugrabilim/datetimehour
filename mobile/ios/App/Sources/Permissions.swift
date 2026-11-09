import AVFoundation
import CoreLocation
import EventKit
import FamilyControls
import HealthKit
import MediaPlayer
import Speech
import SwiftUI
import UserNotifications
#if canImport(AlarmKit)
import AlarmKit
#endif

/*
 * Ayarlar › İzinler (5 Kasım 2026, sahibin isteği): uygulamanın isteyebileceği her iznin durumu, ne işe yaradığı
 * ("izin verirseniz … vermezseniz …") ve nereden kapatılacağı. Üstte güven kartı (onboarding'dekiyle aynı), altta hiç
 * istemediğimiz izinler. iOS'ta izinler uygulamanın içinden kapatılamaz; satıra dokununca telefonun Ayarlar'ındaki
 * sayfa açılır (bildirimde doğrudan bildirim ayarı). Henüz sorulmamış izin yalnız bilgi olarak görünür: her izin o
 * özelliği ilk kullanırken sorulur. Android'deki Permissions.kt ile aynı metinler (perms.*).
 */
@MainActor
enum AppPermissions {
    /// `state`: nil henüz sorulmadı, true açık, false kapalı
    struct Item: Identifiable {
        let id: String
        let icon: String
        let state: Bool?
    }

    static func items() async -> [Item] {
        var out: [Item] = []
        let loc = CLLocationManager().authorizationStatus
        out.append(Item(id: "location", icon: "pin",
                        state: loc == .notDetermined ? nil : (loc == .authorizedWhenInUse || loc == .authorizedAlways)))
        let n = await PrayerNotifier.authorizationStatus()
        out.append(Item(id: "notify", icon: "bell", state: n == .notDetermined ? nil : PrayerNotifier.isAllowed(n)))
        #if canImport(AlarmKit)
        if #available(iOS 26.0, *) {
            let a = AlarmManager.shared.authorizationState
            out.append(Item(id: "alarm", icon: "alarm", state: a == .notDetermined ? nil : a == .authorized))
        }
        #endif
        let cal = EKEventStore.authorizationStatus(for: .event)
        out.append(Item(id: "calendar", icon: "calendar",
                        state: cal == .notDetermined ? nil : (cal == .fullAccess || cal == .writeOnly)))
        let cam = AVCaptureDevice.authorizationStatus(for: .video)
        out.append(Item(id: "camera", icon: "camera", state: cam == .notDetermined ? nil : cam == .authorized))
        let mic = AVAudioApplication.shared.recordPermission
        out.append(Item(id: "mic", icon: "mic", state: mic == .undetermined ? nil : mic == .granted))
        let sp = SFSpeechRecognizer.authorizationStatus()
        out.append(Item(id: "speech", icon: "waveform", state: sp == .notDetermined ? nil : sp == .authorized))
        if HealthLog.available {
            let h = HKHealthStore().authorizationStatus(for: HKObjectType.workoutType())
            out.append(Item(id: "health", icon: "heart", state: h == .notDetermined ? nil : h == .sharingAuthorized))
        }
        let m = MPMediaLibrary.authorizationStatus()
        out.append(Item(id: "music", icon: "music", state: m == .notDetermined ? nil : m == .authorized))
        if FocusBlock.available {
            let f = AuthorizationCenter.shared.authorizationStatus
            out.append(Item(id: "screenTime", icon: "hourglass", state: f == .notDetermined ? nil : f == .approved))
        }
        return out
    }

    /// Telefonun Ayarlar'ında uygulamanın sayfası (bildirimde doğrudan bildirim ayarı)
    static func open(_ id: String) {
        let s = id == "notify" ? UIApplication.openNotificationSettingsURLString : UIApplication.openSettingsURLString
        if let url = URL(string: s) { UIApplication.shared.open(url) }
    }
}

struct PermissionsView: View {
    @State private var items: [AppPermissions.Item] = []
    @Environment(\.scenePhase) private var phase

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                TrustCard()
                Text(L("perms.note")).font(.footnote).foregroundStyle(Theme.muted)
                    .fixedSize(horizontal: false, vertical: true)
                ForEach(items) { item in
                    Button { AppPermissions.open(item.id) } label: { PermissionRow(item: item) }
                        .buttonStyle(.plain)
                        .disabled(item.state == nil)
                }
                VStack(alignment: .leading, spacing: 8) {
                    Text(L("perms.notUsed.title")).font(.headline).foregroundStyle(Theme.text)
                    Text(L("perms.notUsed.ios")).font(.subheadline).foregroundStyle(Theme.muted)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .card(padding: 16)
                RoundButton(title: L("perms.open"), prominent: true) { AppPermissions.open("app") }
            }
            .padding(16)
        }
        .themedBackground()
        .navigationTitle(L("perms.title"))
        // Ayarlar'dan dönünce durum yenilenir
        .task(id: phase) { if phase == .active { items = await AppPermissions.items() } }
    }
}

private struct PermissionRow: View {
    let item: AppPermissions.Item

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            // İki tonlu simge (8 Kasım 2026, DUOTONE.md): İzinler sayfasında hepsi adaçayı
            DuoChip(name: item.icon, color: Theme.sage)
            VStack(alignment: .leading, spacing: 4) {
                HStack(alignment: .firstTextBaseline) {
                    Text(L("perms.\(item.id).title")).font(.headline).foregroundStyle(Theme.text)
                    Spacer(minLength: 8)
                    chip
                }
                Text(Onboarding.permWhy(item.id)).font(.footnote).foregroundStyle(Theme.muted)
                    .multilineTextAlignment(.leading)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .card(padding: 14)
        .accessibilityElement(children: .combine)
    }

    /// Durum hapı: Açık (vurgu rengi), Kapalı ve Sorulmadı (soluk)
    private var chip: some View {
        let key = item.state == nil ? "perms.status.ask" : item.state == true ? "perms.status.on" : "perms.status.off"
        let on = item.state == true
        return Text(L(key))
            .font(.caption.weight(.semibold))
            .foregroundStyle(on ? Theme.onAccent : Theme.muted)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(on ? Theme.accent : Theme.surface2, in: Capsule())
    }
}
