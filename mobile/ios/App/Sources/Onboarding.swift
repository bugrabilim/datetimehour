import SwiftUI
import UserNotifications

/*
 * İlk kurulum sihirbazı (3 Kasım 2026, 2. analiz #4; sahibin ekran görüntüleri): yalnız yeni kurulumda bir kez açılır
 * (önceki sürümü kullananlar ve CI görmez; Features.migrate işaretler). 4 Kasım 2026'dan beri altı adımlık tanıtım
 * (sahibin isteği): hoş geldiniz (canlı saat), saati kaydırma, tam ekran ve gece modu, özellik seçimi (alt çubukta
 * görünecekler; Saat hep açık), Tümü ve Ayarlar, konum ve bildirimler. Çizimler kodla (telefon çerçevesi içinde kısa
 * döngüler; hareketi azalt açıksa durur). Ayarlar › Destek'ten yeniden gösterilir. Bekletme yok; "Geç" her sayfada. Markayı ve
 * tanıtımı anlatma yeri burasıdır (açılış ekranı yalnız simge, #83). Android'deki Onboarding.kt ile aynı akış.
 */
enum Onboarding {
    static let key = "onboard.pending"
    static var pending: Bool { UserDefaults.standard.bool(forKey: key) }
    /// Ayarlar › Destek › Tanıtımı yeniden göster: kök görünüm bu bildirimle sihirbazı açar
    static let replayNote = Notification.Name("onboard.replay")
    static func replay() {
        UserDefaults.standard.set(true, forKey: key)
        NotificationCenter.default.post(name: replayNote, object: nil)
    }

    /// Bitince: bildirim izni burada sorulduğu için açılıştaki kısa açıklama bir daha çıkmaz
    static func finish() {
        UserDefaults.standard.set(false, forKey: key)
        UserDefaults.standard.set(true, forKey: "notify.asked")
    }
}

struct OnboardingView: View {
    let done: () -> Void
    /// CI ekran görüntüleri: -onboardPage N ile doğrudan bir adım
    @State private var page = min(5, max(0, UserDefaults.standard.integer(forKey: "onboardPage")))
    @AppStorage(Features.key) private var raw = ""
    @State private var notifyOn = false
    private let last = 5

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
                OnboardTip(key: "onboard.swipe") { SwipeArt() }.tag(1)
                OnboardTip(key: "onboard.full") { FullscreenArt() }.tag(2)
                features.tag(3)
                OnboardTip(key: "onboard.all") { AllArt() }.tag(4)
                setup.tag(5)
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
                // Seçtikçe değişen alt çubuk (Seçenek B; ilk 3 özellik çubuğa girer, gerisi Tümü'nde)
                BarPreview()
                    .background(Theme.surface, in: RoundedRectangle(cornerRadius: 14))
                ForEach(Features.optional, id: \.self) { tab in
                    let on = Features.parse(raw).contains(tab)
                    Button {
                        Features.set(tab, on: !on)
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

/// Tanıtım adımı: üstte çizim, altında başlık ve kısa açıklama (anahtarlar `<key>.title`, `<key>.text`)
private struct OnboardTip<Art: View>: View {
    let key: String
    @ViewBuilder var art: () -> Art

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                art()
                    .frame(width: 290, height: 290)
                    .environment(\.layoutDirection, .leftToRight)
                    .accessibilityHidden(true)
                Text(L("\(key).title")).font(.title2.weight(.bold)).foregroundStyle(Theme.text)
                    .multilineTextAlignment(.center)
                Text(L("\(key).text")).font(.body).foregroundStyle(Theme.muted)
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.horizontal, 28)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
        }
    }
}

/// Telefon çerçevesi: yuvarlak köşeli ekran, üstte çentik
private struct PhoneFrame<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        ZStack(alignment: .top) {
            RoundedRectangle(cornerRadius: 23).fill(Theme.surface)
            content()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .clipShape(RoundedRectangle(cornerRadius: 23))
            Capsule().fill(Theme.text.opacity(0.82)).frame(width: 38, height: 9).padding(.top, 7)
        }
        .padding(5)
        .frame(width: 150, height: 280)
        .overlay(RoundedRectangle(cornerRadius: 28).strokeBorder(Theme.text.opacity(0.82), lineWidth: 5))
    }
}

/// 0 → 1 → 0 döngüsü (Android'deki loopPhase): bekle, yumuşak geç, bekle; `half` bir yönün süresi
private func loopPhase(_ date: Date, half: Double) -> CGFloat {
    let t = date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: half * 2) / half
    let p = t <= 1 ? t : 2 - t
    let x = min(1, max(0, (p - 0.3) / 0.4))
    return CGFloat(x * x * (3 - 2 * x))
}

/// Saat ve dakika, her dilde Latin rakamıyla
private func hhmm(_ date: Date) -> String {
    let c = Calendar.current.dateComponents([.hour, .minute], from: date)
    return String(format: "%02d:%02d", c.hour ?? 0, c.minute ?? 0)
}

/// Saati kaydırma: analog saat sola kayar, dijital saat gelir; altta kayan parmak
private struct SwipeArt: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30, paused: reduceMotion)) { ctx in
            let k = reduceMotion ? 1 : loopPhase(ctx.date, half: 3.2)
            PhoneFrame {
                GeometryReader { g in
                    ZStack {
                        AnalogClock(date: ctx.date)
                            .frame(width: 112, height: 112)
                            .frame(width: g.size.width, height: g.size.height)
                            .offset(x: -g.size.width * k)
                        Text(hhmm(ctx.date))
                            .font(.system(size: 38, weight: .light).monospacedDigit())
                            .foregroundStyle(Theme.ink)
                            .frame(width: g.size.width, height: g.size.height)
                            .offset(x: g.size.width * (1 - k))
                    }
                }
                .overlay(alignment: .bottom) {
                    VStack(spacing: 14) {
                        Circle().fill(Theme.accent.opacity(0.25))
                            .overlay(Circle().strokeBorder(Theme.accent, lineWidth: 2))
                            .frame(width: 30, height: 30)
                            .offset(x: 28 - 56 * k)
                        HStack(spacing: 5) {
                            Capsule().fill(k < 0.5 ? Theme.accent : Theme.border).frame(width: k < 0.5 ? 14 : 5, height: 5)
                            Capsule().fill(k >= 0.5 ? Theme.accent : Theme.border).frame(width: k >= 0.5 ? 14 : 5, height: 5)
                        }
                    }
                    .padding(.bottom, 14)
                }
            }
        }
    }
}

/// Tam ekran ve gece: telefon yan döner, ekran kararır, saat kısık kırmızı olur
private struct FullscreenArt: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    private let night = Color(red: 0.88, green: 0.25, blue: 0.16)

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30, paused: reduceMotion)) { ctx in
            let k = reduceMotion ? 1 : loopPhase(ctx.date, half: 3.6)
            PhoneFrame {
                ZStack {
                    Color.black.opacity(k)
                    VStack(spacing: 6) {
                        ZStack {
                            Text(hhmm(ctx.date)).foregroundStyle(Theme.ink).opacity(1 - k)
                            Text(hhmm(ctx.date)).foregroundStyle(night).opacity(k)
                        }
                        .font(.system(size: 34 + 14 * k, weight: .light).monospacedDigit())
                        .lineLimit(1)
                        .fixedSize()
                        Image(systemName: "moon.fill").font(.system(size: 16)).foregroundStyle(night).opacity(k)
                    }
                    .rotationEffect(.degrees(90 * k))
                }
            }
            .rotationEffect(.degrees(-90 * k))
        }
    }
}

/// Tümü: arama, renkli araç kutuları, alt çubukta vurgulanan "Tümü" ve uzun basılan kutunun halkası
private struct AllArt: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.colorScheme) private var scheme
    // 3D simgeler (4 Kasım 2026, sahibin seçimi)
    private let tiles: [(String, Color)] = [
        ("alarm", Theme.terra), ("hourglass", Theme.terra), ("calendar", Theme.sage),
        ("sun_cloud", Theme.sky), ("compass", Theme.gold), ("book", Theme.gold),
        ("globe", Theme.sky), ("moon", Theme.dusk), ("gear", Theme.dusk),
    ]

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30, paused: reduceMotion)) { ctx in
            let k = reduceMotion ? 1 : loopPhase(ctx.date, half: 2.8)
            PhoneFrame {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Image(systemName: "magnifyingglass").font(.system(size: 10)).foregroundStyle(Theme.muted)
                        Spacer()
                    }
                    .padding(.horizontal, 7)
                    .frame(height: 20)
                    .background(Theme.surface2, in: Capsule())
                    ForEach(0..<3, id: \.self) { r in
                        HStack(spacing: 8) {
                            ForEach(0..<3, id: \.self) { i in
                                tile(tiles[r * 3 + i], pressed: r == 0 && i == 0, k: k)
                            }
                        }
                    }
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, 10)
                .padding(.top, 24)
                .overlay(alignment: .bottom) {
                    HStack {
                        Image(systemName: "clock")
                        Spacer()
                        Image(systemName: "moon")
                        Spacer()
                        Image(systemName: "sun.max")
                        Spacer()
                        Image(systemName: "square.grid.3x3.fill")
                            .foregroundStyle(Theme.accent)
                            .padding(.horizontal, 7)
                            .padding(.vertical, 3)
                            .background(Theme.accent.opacity(0.12 + 0.2 * k), in: Capsule())
                    }
                    .font(.system(size: 13))
                    .foregroundStyle(Theme.muted)
                    .padding(.horizontal, 12)
                    .frame(height: 34)
                    .background(Theme.surface2)
                }
            }
        }
    }

    private func tile(_ t: (String, Color), pressed: Bool, k: CGFloat) -> some View {
        RoundedRectangle(cornerRadius: 11)
            .fill(t.1.opacity(scheme == .dark ? 0.22 : 0.14))
            .frame(width: 34, height: 34)
            .overlay(Icon3D(name: t.0, size: 22))
            .overlay(RoundedRectangle(cornerRadius: 11).strokeBorder(Theme.accent.opacity(pressed ? k : 0), lineWidth: 1 + 2 * k))
    }
}
