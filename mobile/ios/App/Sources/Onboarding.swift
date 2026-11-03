import SwiftUI
import UserNotifications

/*
 * İlk kurulum sihirbazı (3 Kasım 2026, 2. analiz #4; sahibin ekran görüntüleri): yalnız yeni kurulumda bir kez açılır
 * (önceki sürümü kullananlar ve CI görmez; Features.migrate işaretler). Üç sayfa: hoş geldiniz (canlı saat), özellik
 * seçimi (alt çubukta görünecekler; Saat hep açık), konum ve bildirimler. Bekletme yok; "Geç" her sayfada. Markayı ve
 * tanıtımı anlatma yeri burasıdır (açılış ekranı yalnız simge, #83). Android'deki Onboarding.kt ile aynı akış.
 */
enum Onboarding {
    static let key = "onboard.pending"
    static var pending: Bool { UserDefaults.standard.bool(forKey: key) }

    /// Bitince: bildirim izni burada sorulduğu için açılıştaki kısa açıklama bir daha çıkmaz
    static func finish() {
        UserDefaults.standard.set(false, forKey: key)
        UserDefaults.standard.set(true, forKey: "notify.asked")
    }
}

struct OnboardingView: View {
    let done: () -> Void
    @State private var page = 0
    @AppStorage(Features.key) private var raw = ""
    @State private var notifyOn = false
    private let last = 2

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Spacer()
                if page < last {
                    Button(L("onboard.skip")) { done() }
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Theme.muted)
                }
            }
            .padding(.horizontal, 20)
            .frame(height: 44)
            TabView(selection: $page) {
                welcome.tag(0)
                features.tag(1)
                setup.tag(2)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            // Sayfa noktaları: seçili olan uzun çizgi
            HStack(spacing: 8) {
                ForEach(0...last, id: \.self) { i in
                    Capsule().fill(i == page ? Theme.accent : Theme.border)
                        .frame(width: i == page ? 22 : 8, height: 8)
                }
            }
            .padding(.bottom, 16)
            .accessibilityHidden(true)
            Button {
                if page < last { withAnimation { page += 1 } } else { done() }
            } label: {
                Text(L(page < last ? "onboard.next" : "onboard.start"))
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.accent)
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
        .background(AppBackdrop().ignoresSafeArea())
        .task { notifyOn = PrayerNotifier.isAllowed(await PrayerNotifier.authorizationStatus()) }
    }

    private var welcome: some View {
        VStack(spacing: 18) {
            Spacer()
            TimelineView(.periodic(from: .now, by: 1)) { ctx in
                AnalogClock(date: ctx.date, showSeconds: true).frame(width: 200, height: 200)
            }
            .accessibilityHidden(true)
            Text(L("app.displayName")).font(.largeTitle.weight(.bold)).foregroundStyle(Theme.text)
            Text(L("onboard.welcome.title")).font(.title3.weight(.semibold)).foregroundStyle(Theme.accent)
            Text(L("onboard.welcome.text"))
                .multilineTextAlignment(.center)
                .foregroundStyle(Theme.muted)
                .fixedSize(horizontal: false, vertical: true)
            Spacer()
        }
        .padding(.horizontal, 28)
    }

    private var features: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(L("onboard.features.title")).font(.title2.weight(.bold)).foregroundStyle(Theme.text)
                Text(L("onboard.features.text")).font(.subheadline).foregroundStyle(Theme.muted)
                    .padding(.bottom, 4)
                ForEach(Features.optional, id: \.self) { tab in
                    let on = Features.parse(raw).contains(tab)
                    Button {
                        var set = Features.parse(raw)
                        if on { set.remove(tab) } else { set.insert(tab) }
                        raw = Features.raw(set)
                    } label: {
                        HStack(spacing: 14) {
                            Image(systemName: tab.icon).font(.title3).foregroundStyle(Theme.accent).frame(width: 32)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(tab.title).font(.headline).foregroundStyle(Theme.text)
                                Text(L("features.\(tab.key).note")).font(.caption).foregroundStyle(Theme.muted)
                                    .multilineTextAlignment(.leading)
                            }
                            Spacer(minLength: 8)
                            Image(systemName: on ? "checkmark.circle.fill" : "circle")
                                .font(.title2)
                                .foregroundStyle(on ? Theme.accent : Theme.border)
                        }
                        .padding(14)
                        .background(Theme.surface, in: RoundedRectangle(cornerRadius: 14))
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(on ? Theme.accent : .clear, lineWidth: 1.5))
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(on ? .isSelected : [])
                }
            }
            .padding(24)
        }
    }

    private var setup: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(L("onboard.setup.title")).font(.title2.weight(.bold)).foregroundStyle(Theme.text)
            Text(L("onboard.setup.text")).font(.subheadline).foregroundStyle(Theme.muted)
            HStack {
                Label(L("settings.location"), systemImage: "location").foregroundStyle(Theme.text)
                Spacer()
                PlacePicker(font: .body)
            }
            .padding(14)
            .background(Theme.surface, in: RoundedRectangle(cornerRadius: 14))
            Button {
                Task {
                    if await PrayerNotifier.authorizationStatus() == .notDetermined { _ = await PrayerNotifier.requestAuthorization() }
                    notifyOn = PrayerNotifier.isAllowed(await PrayerNotifier.authorizationStatus())
                }
            } label: {
                HStack {
                    Label(L(notifyOn ? "onboard.notifyOn" : "onboard.notify"), systemImage: notifyOn ? "bell.badge.fill" : "bell")
                        .font(.headline)
                    Spacer()
                    if notifyOn { Image(systemName: "checkmark").font(.headline) }
                }
                .foregroundStyle(notifyOn ? Theme.accent : Theme.text)
                .padding(14)
                .background(Theme.surface, in: RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
            .disabled(notifyOn)
            Spacer()
        }
        .padding(24)
    }
}
