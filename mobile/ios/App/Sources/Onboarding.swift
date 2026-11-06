import SwiftUI
import UserNotifications

/*
 * İlk kurulum sihirbazı (3 Kasım 2026, 2. analiz #4). 5 Kasım 2026'dan beri (sahibin isteği) sorulu akış:
 * hoş geldiniz → güven kartı, konum ve bildirim → beş kısa soru (Vakitler, Hava, Takvim, Dünya, Mevsimler; her biri Evet/Hayır,
 * yanıt alt çubuğu hemen değiştirir) → özet → "Başka neler var, öğrenmek ister misiniz?" (Evet: özellikler tek tek; Hayır:
 * doğrudan son sayfa) → son sayfa "Diğer her şey Tümü'nde". "Geç" baştan itibaren her sayfada: soruların sonuna kadar geçilirse
 * yeni kurulumda yalnız Saat açık kalır ve alt çubuğun ortasında "Kuruluma devam et" çıkar (Tümü sağda); kişi ayarlardan bir şey
 * değiştirirse bu düğme kalkar. Özetten sonra "Geç" son sayfaya gider. Yeni kurulumda ve tanıtımın bu sürümünü görmemiş eski
 * kullanıcıda bir kez açılır (`onboard.seen` < `version`). Ayarlar › Destek'ten yeniden gösterilir. Çizimler kodla (hareketi
 * azalt açıksa durur). Android'deki Onboarding.kt ile aynı akış ve anahtarlar.
 */
enum Onboarding {
    static let key = "onboard.pending"
    /// Görülen tanıtım sürümü; `version`'dan küçükse (eski kullanıcı) bir kez daha açılır
    static let seenKey = "onboard.seen"
    /// 2: sorulu akış ve güven kartı (1.1, 5 Kasım 2026)
    static let version = 2
    /// Yeni kurulum işareti (Features.migrate yazar, bitince silinir): sorular boş başlar, geçilirse yalnız Saat açık kalır
    static let freshKey = "onboard.fresh"
    /// Geçilen kurulum: alt çubukta "Kuruluma devam et" (yalnız yeni kurulumda)
    static let resumeKey = "onboard.resume"
    private static let sigKey = "onboard.resume.sig"
    /// Bunlardan biri değişirse (kişi ayarlardan bir şey değiştirdi) "Kuruluma devam et" kalkar
    private static let settingKeys = [Features.key, Features.barKey, "province", NotificationPrefs.enabledKey, "accent",
                                      "appearance", Atmosphere.key, "h12", "units", "fontScale", "homeItems", "AppleLanguages"]
    /// Sorulan özellikler, sırayla (alt çubuğa giriş sırası da budur)
    static let questions: [AppTab] = [.prayer, .weather, .calendar, .world, .seasons]
    /// Özellik turu ("başka neler var" sorusuna Evet denince)
    static let tours = ["alarm", "prayer", "weather", "tools", "look", "widgets"]
    /// Sayfa kimlikleri (CI -onboardPage N: 0 hoş geldiniz, 1 kurulum, 2–6 sorular, 7 özet, 8 "başka neler var", 9–10 ipuçları,
    /// 11–16 özellik turu, 17 son sayfa "Diğer her şey Tümü'nde")
    static let pages: [String] = ["welcome", "setup"] + questions.map { "q:" + $0.key } +
        ["summary", "more", "tip:swipe", "tip:full"] + tours.map { "tour:" + $0 } + ["tip:all"]
    static let summary = pages.firstIndex(of: "summary")!

    static var pending: Bool { UserDefaults.standard.bool(forKey: key) }
    static var fresh: Bool { UserDefaults.standard.bool(forKey: freshKey) }
    /// Sihirbazın açılacağı sayfa (kuruluma devam edilince kurulum sayfası)
    static var startPage = 0
    /// Bu tanıtımda verilen yanıtlar
    static var answers: [AppTab: Bool] = [:]
    /// Sihirbaz açılırken özellikler; eski kullanıcı geçerse seçimi geri gelir
    private static var before: (String, String)?

    /// Ayarlar › Destek › Tanıtımı yeniden göster (ve "Kuruluma devam et"): kök görünüm bu bildirimle sihirbazı açar
    static let replayNote = Notification.Name("onboard.replay")
    static func replay() { open(at: 0) }

    /// Alt çubuktaki "Kuruluma devam et": hoş geldiniz sayfası atlanır, kurulum sayfasından başlar
    static func resumeSetup() { open(at: 1) }

    private static func open(at page: Int) {
        answers = [:]
        before = nil
        startPage = page
        UserDefaults.standard.set(true, forKey: key)
        NotificationCenter.default.post(name: replayNote, object: nil)
    }

    /// Features.migrate'ten: tanıtımın bu sürümü görülmediyse (yeni kurulum ya da güncelleme) bir kez işaretler
    static func checkVersion() {
        let d = UserDefaults.standard
        if d.integer(forKey: seenKey) < version { d.set(true, forKey: key) }
    }

    /// Sihirbaz açılınca: o anki özellikler (eski kullanıcı geçerse geri yüklenir)
    static func begin() {
        let d = UserDefaults.standard
        if before == nil { before = (d.string(forKey: Features.key) ?? "", d.string(forKey: Features.barKey) ?? "") }
    }

    /// Sorunun gösterilen yanıtı: verildiyse o, eski kullanıcıda açık/kapalı durumu, yeni kurulumda boş
    static func shown(_ t: AppTab) -> Bool? { answers[t] ?? (fresh ? nil : Features.isOn(t)) }

    /// Yanıt hemen uygulanır (özet sayfasındaki alt çubuk canlı değişir)
    static func answer(_ t: AppTab, _ yes: Bool) {
        answers[t] = yes
        Features.set(t, on: yes)
    }

    /// "Hayır" denen bölümün bildirimleri (Android'dekiyle aynı anahtarlar)
    static func notifyKeys(_ t: AppTab) -> [String] {
        switch t {
        case .prayer: [NotificationPrefs.enabledKey, NotifyFeature.friday.key]
        case .weather: [NotifyFeature.weather.key]
        case .calendar: [NotifyFeature.calendar.key, NotifyFeature.daily.key]
        case .seasons: [NotifyFeature.seasons.key, NotifyFeature.moon.key, NotifyFeature.sun.key, NotifyFeature.sky.key]
        default: []
        }
    }

    /// Sorular bitmeden "Geç": yeni kurulumda yalnız Saat (ve Tümü) açık kalır, soruların bölümleri bildirim göndermez ve alt
    /// çubuğun ortasında "Kuruluma devam et" çıkar; eski kullanıcının seçimi geri gelir (onda devam düğmesi yok).
    static func skip() {
        let d = UserDefaults.standard
        let isFresh = fresh
        if isFresh {
            Features.save([])
            for t in questions { for k in notifyKeys(t) { d.set(false, forKey: k) } }
        } else if let b = before {
            d.set(b.0, forKey: Features.key)
            d.set(b.1, forKey: Features.barKey)
        }
        d.set(false, forKey: key)
        d.set(version, forKey: seenKey)
        d.set(true, forKey: "notify.asked")
        // Ayarların izi yukarıdaki değişikliklerden sonra alınır (yoksa kendi yazdığımız kapatır)
        if isFresh {
            d.set(signature(), forKey: sigKey)
            d.set(true, forKey: resumeKey)
        } else {
            d.removeObject(forKey: resumeKey)
            d.removeObject(forKey: sigKey)
        }
        answers = [:]
        before = nil
    }

    /// Bitince: yanıtlar kalıcı olur. "Hayır" denen bölümün bildirimi kapanır; yeni kurulumda Vakitler'e "Evet" denirse
    /// saatin altında sıradaki vakit görünür. Araçlar (Tümü'ndeki araçlar) her zaman açık kalır. Bildirim izni burada
    /// sorulduğu için açılıştaki kısa açıklama bir daha çıkmaz.
    static func finish() {
        let d = UserDefaults.standard
        for (t, yes) in answers where !yes {
            for k in notifyKeys(t) { d.set(false, forKey: k) }
        }
        if fresh, answers[.prayer] == true, d.object(forKey: "homeItems") == nil {
            var layout = HomeLayout(raw: HomeItem.defaultRaw)
            layout.enabled.insert(.next)
            d.set(layout.raw, forKey: "homeItems")
        }
        if fresh, !Features.isOn(.tools) { Features.set(.tools, on: true) }
        d.set(false, forKey: key)
        d.set(version, forKey: seenKey)
        d.set(true, forKey: "notify.asked")
        for k in [freshKey, resumeKey, sigKey] { d.removeObject(forKey: k) }
        answers = [:]
        before = nil
    }

    /// Ayarların izi: geçildiği andaki ayarlarla karşılaştırılır
    private static func signature() -> String {
        let d = UserDefaults.standard
        return settingKeys.map { "\(d.object(forKey: $0) ?? "")" }.joined(separator: "|")
    }

    /// Kişi ayarlardan bir şey değiştirdiyse "Kuruluma devam et" kalkar (kök görünüm kayıt değişince çağırır)
    static func checkResume() {
        let d = UserDefaults.standard
        guard !resumeChecking, d.bool(forKey: resumeKey), !pending, d.string(forKey: sigKey) != signature() else { return }
        // Silmek de kayıt değişikliği bildirimi doğurur; aynı anda ikinci kez girilmez
        resumeChecking = true
        defer { resumeChecking = false }
        for k in [resumeKey, sigKey, freshKey] { d.removeObject(forKey: k) }
    }
    private static var resumeChecking = false
}

struct OnboardingView: View {
    let done: () -> Void
    /// CI ekran görüntüleri: -onboardPage N ile doğrudan bir adım; yoksa Onboarding.startPage
    @State private var page = min(Onboarding.pages.count - 1,
                                  max(0, (UserDefaults.standard.object(forKey: "onboardPage") as? Int) ?? Onboarding.startPage))
    /// Gelinen sayfalar: geri düğmesi oraya döner ("başka neler var" sorusuna Hayır denip son sayfaya atlandıysa o soruya)
    @State private var history: [Int] = []
    /// Geçiş yönü (geri düğmesinde sayfa soldan gelir)
    @State private var forward = true
    /// Yanıtlar görünümde de tutulur ki düğmelerdeki işaret yenilensin
    @State private var answers = Onboarding.answers
    @AppStorage(Features.key) private var raw = ""
    @State private var notifyOn = false
    private var last: Int { Onboarding.pages.count - 1 }
    private var id: String { Onboarding.pages[page] }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                if page > 0 {
                    Button { back() } label: {
                        Image(systemName: "chevron.backward").font(.title3.weight(.semibold))
                            .frame(width: 44, height: 44)
                    }
                    .foregroundStyle(Theme.muted)
                    .accessibilityLabel(L("onboard.back"))
                }
                Spacer()
                // "Geç" baştan itibaren: sorular bitmeden geçilirse yalnız Saat açık kalır; özetten sonra son sayfaya gider
                if page < last {
                    Button(L("onboard.skip")) {
                        if page < Onboarding.summary {
                            Onboarding.skip()
                            done()
                        } else {
                            go(last)
                        }
                    }
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.muted)
                    .padding(.horizontal, 8)
                }
            }
            .padding(.horizontal, 12)
            .frame(height: 48)
            // Sayfalar yalnız düğmelerle değişir (sorular kaydırılarak atlanmasın)
            ZStack {
                pageView(page)
                    .id(page)
                    .transition(.asymmetric(
                        insertion: .move(edge: forward ? .trailing : .leading).combined(with: .opacity),
                        removal: .move(edge: forward ? .leading : .trailing).combined(with: .opacity)))
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .clipped()
            // Sayfa noktaları: seçili olan uzun çizgi
            HStack(spacing: 4) {
                ForEach(0...last, id: \.self) { i in
                    Capsule().fill(i == page ? Theme.accent : Theme.border)
                        .frame(width: i == page ? 16 : 5, height: 5)
                }
            }
            .padding(.bottom, 16)
            .accessibilityHidden(true)
            bottomButtons
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
        }
        .background(AppBackdrop().ignoresSafeArea())
        .task {
            Onboarding.begin()
            notifyOn = PrayerNotifier.isAllowed(await PrayerNotifier.authorizationStatus())
        }
    }

    private func go(_ to: Int) {
        history.append(page)
        move(to, forward: true)
    }

    private func back() {
        move(history.popLast() ?? page - 1, forward: false)
    }

    private func move(_ to: Int, forward f: Bool) {
        let target = min(last, max(0, to))
        // Yön önce yazılır, sayfa bir sonraki turda değişir: çıkan sayfa da yeni yönle kayar
        forward = f
        DispatchQueue.main.async { withAnimation(.easeInOut(duration: 0.3)) { page = target } }
    }

    @ViewBuilder private var bottomButtons: some View {
        if id.hasPrefix("q:"), let t = Onboarding.questions.first(where: { "q:" + $0.key == id }) {
            // Soru: Hayır / Evet; yanıt hemen uygulanır, sonraki soruya geçilir
            let shown = answers[t] ?? Onboarding.shown(t)
            HStack(spacing: 12) {
                RoundButton(title: (shown == false ? "✓ " : "") + L("onboard.no")) { reply(t, false) }
                RoundButton(title: (shown == true ? "✓ " : "") + L("onboard.yes"), prominent: true) { reply(t, true) }
            }
        } else if id == "more" {
            // Hayır: doğrudan son sayfa ("Diğer her şey Tümü'nde"); Evet: özellikler tek tek
            HStack(spacing: 12) {
                RoundButton(title: L("onboard.no")) { go(last) }
                RoundButton(title: L("onboard.yes"), prominent: true) { go(page + 1) }
            }
        } else {
            let label = id == "welcome" ? "onboard.go" : id == "tip:all" ? "onboard.start" : "onboard.next"
            RoundButton(title: L(label), prominent: true) {
                if page < last {
                    go(page + 1)
                } else {
                    Onboarding.finish()
                    done()
                }
            }
        }
    }

    private func reply(_ t: AppTab, _ yes: Bool) {
        Onboarding.answer(t, yes)
        answers[t] = yes
        go(page + 1)
    }

    @ViewBuilder private func pageView(_ p: Int) -> some View {
        let pid = Onboarding.pages[p]
        if pid == "welcome" {
            welcome
        } else if pid == "setup" {
            setup
        } else if pid.hasPrefix("q:"), let i = Onboarding.questions.firstIndex(where: { "q:" + $0.key == pid }) {
            OnboardQuestion(tab: Onboarding.questions[i], n: i + 1, total: Onboarding.questions.count)
        } else if pid == "summary" {
            summary
        } else if pid == "more" {
            OnboardFeature(icons: ["sparkles", "books", "star"], title: L("onboard.more.title"), text: L("onboard.more.text"))
        } else if pid == "tip:swipe" {
            OnboardTip(key: "onboard.swipe") { SwipeArt() }
        } else if pid == "tip:full" {
            OnboardTip(key: "onboard.full") { FullscreenArt() }
        } else if pid.hasPrefix("tour:") {
            let name = String(pid.dropFirst(5))
            OnboardFeature(icons: tourIcons(name), title: L("onboard.tour.\(name).title"), text: L("onboard.tour.\(name).text"))
        } else {
            OnboardTip(key: "onboard.all") { AllArt() }
        }
    }

    private func tourIcons(_ name: String) -> [String] {
        switch name {
        case "alarm": ["bed", "alarm", "moon"]
        case "prayer": ["kaaba", "mosque", "beads"]
        case "weather": ["telescope", "sun_cloud", "moon"]
        case "tools": ["metronome", "tomato", "knight"]
        case "look": ["sparkles", "palette", "clock"]
        default: ["sun_cloud", "clock", "star_crescent"]
        }
    }

    private var welcome: some View {
        ScrollView {
            VStack(spacing: 16) {
                TimelineView(.periodic(from: .now, by: 1)) { ctx in
                    AnalogClock(date: ctx.date, showSeconds: true).frame(width: 200, height: 200)
                }
                .accessibilityHidden(true)
                Text(L("app.displayName")).font(.largeTitle.weight(.bold)).foregroundStyle(Theme.text)
                    .multilineTextAlignment(.center)
                Text(L("onboard.welcome.text"))
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
                    .foregroundStyle(Theme.muted)
                    .fixedSize(horizontal: false, vertical: true)
                Text(L("onboard.welcome.hint"))
                    .multilineTextAlignment(.center)
                    .foregroundStyle(Theme.text)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.horizontal, 28)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
        }
    }

    /// Özet: yanıtlara göre oluşan alt çubuk ve açılan bölümler
    private var summary: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text(L("onboard.summary.title")).font(.title2.weight(.bold)).foregroundStyle(Theme.text)
                Text(L("onboard.summary.text")).font(.subheadline).foregroundStyle(Theme.muted)
                    .fixedSize(horizontal: false, vertical: true)
                BarPreview()
                    .padding(.vertical, 6)
                    .card(padding: 6)
                let on = Onboarding.questions.filter { Features.parse(raw).contains($0) }
                if on.isEmpty {
                    Text(L("onboard.summary.none")).foregroundStyle(Theme.muted)
                        .fixedSize(horizontal: false, vertical: true)
                } else {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(L("onboard.summary.on")).font(.footnote.weight(.semibold)).foregroundStyle(Theme.muted)
                        ForEach(on, id: \.self) { t in
                            HStack(spacing: 12) {
                                LineIcon(name: LineIconMap.tab(t), size: 28)
                                Text(t.title).font(.headline).foregroundStyle(Theme.text)
                                Spacer()
                                Image(systemName: "checkmark.circle.fill").foregroundStyle(Theme.accent)
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .card(padding: 16)
                }
                Text(L("onboard.q.note")).font(.footnote).foregroundStyle(Theme.muted)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(24)
        }
    }

    /// Güven kartı, konum ve bildirim (hoş geldiniz sayfasının hemen ardından)
    private var setup: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text(L("onboard.setup.title")).font(.title2.weight(.bold)).foregroundStyle(Theme.text)
                TrustCard()
                VStack(alignment: .leading, spacing: 6) {
                    HStack(spacing: 10) {
                        LineIcon(name: "pin", size: 26)
                        Text(L("settings.location")).font(.headline).foregroundStyle(Theme.text)
                        Spacer()
                        PlacePicker(font: .body)
                    }
                    Text(L("perms.location.why")).font(.footnote).foregroundStyle(Theme.muted)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .card(padding: 14)
                Button {
                    Task {
                        if await PrayerNotifier.authorizationStatus() == .notDetermined { _ = await PrayerNotifier.requestAuthorization() }
                        notifyOn = PrayerNotifier.isAllowed(await PrayerNotifier.authorizationStatus())
                    }
                } label: {
                    VStack(alignment: .leading, spacing: 6) {
                        HStack(spacing: 10) {
                            LineIcon(name: "bell", size: 26)
                            Text(L(notifyOn ? "onboard.notifyOn" : "onboard.notify")).font(.headline)
                                .foregroundStyle(notifyOn ? Theme.accent : Theme.text)
                            Spacer()
                            if notifyOn { Image(systemName: "checkmark.circle.fill").foregroundStyle(Theme.accent) }
                        }
                        Text(L("perms.notify.why")).font(.footnote).foregroundStyle(Theme.muted)
                            .multilineTextAlignment(.leading)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .card(padding: 14)
                }
                .buttonStyle(.plain)
                .disabled(notifyOn)
                Text(L("onboard.trust.later")).font(.footnote).foregroundStyle(Theme.muted)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(24)
        }
    }
}

/// "Başka neler var" sorusu ve özellik turu sayfaları: üç 3D simge, başlık ve kısa açıklama
private struct OnboardFeature: View {
    let icons: [String]
    let title: String
    let text: String

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                ZStack {
                    Circle().fill(Theme.surface).frame(width: 190, height: 190)
                    LineIcon(name: icons[0], size: 58)
                        .frame(width: 84, height: 84)
                        .background(Theme.surface2, in: Circle())
                        .offset(x: -78, y: 44)
                    LineIcon(name: icons[2], size: 52)
                        .frame(width: 76, height: 76)
                        .background(Theme.surface2, in: Circle())
                        .offset(x: 80, y: -46)
                    LineIcon(name: icons[1], size: 118)
                }
                .frame(width: 260, height: 220)
                .environment(\.layoutDirection, .leftToRight)
                .accessibilityHidden(true)
                Text(title).font(.title2.weight(.bold)).foregroundStyle(Theme.text)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                Text(text).foregroundStyle(Theme.muted)
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

/// Tek soru: büyük 3D simge, soru, ne değişeceği; altta Hayır / Evet (OnboardingView)
private struct OnboardQuestion: View {
    let tab: AppTab
    let n: Int
    let total: Int

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Text(T("onboard.q.step", n, total)).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.muted)
                LineIcon(name: LineIconMap.tab(tab), size: 104)
                    .frame(width: 150, height: 150)
                    .background(Theme.surface, in: Circle())
                    .accessibilityHidden(true)
                Text(L("onboard.q.\(tab.key).title")).font(.title2.weight(.bold)).foregroundStyle(Theme.text)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                Text(L("onboard.q.\(tab.key).text")).foregroundStyle(Theme.muted)
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
                    .fixedSize(horizontal: false, vertical: true)
                Text(L("onboard.q.note")).font(.footnote).foregroundStyle(Theme.muted)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.horizontal, 28)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
        }
    }
}

/// Güven kartı: hesap/reklam/takip yok, verileriniz bize gelmez (kurulum sayfası ve Ayarlar › İzinler)
struct TrustCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 10) {
                LineIcon(name: "heart", size: 30)
                Text(L("onboard.trust.title")).font(.headline).foregroundStyle(Theme.text)
            }
            Text(L("onboard.trust.text")).font(.subheadline).foregroundStyle(Theme.text)
                .fixedSize(horizontal: false, vertical: true)
            Text(L("onboard.trust.services")).font(.footnote).foregroundStyle(Theme.muted)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .card(padding: 18)
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
