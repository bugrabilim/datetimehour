import SwiftUI
import SaatCore

/// Kazanım rozetleri (Bumba standardı madde 13; Saat Tarih'te yalnız uygulamalarda, 30 Ekim 2026). Her rozet
/// uygulamada gerçekten olan bir eyleme dayanır; inanç ele vermemek için namaz/imsakiye rozeti yoktur.
/// Kimlikler Android'deki `Badge` ile aynıdır; adlar `badges.items.<kimlik>.*` metinleri (app-strings.json).
enum Badge: String, CaseIterable, Identifiable {
    case renk, saniye
    case ilkModel = "ilk-model", tamEkran = "tam-ekran"
    case duzenle, takvim, kronometre, zamanlayici, alarm, uyanik, cevirici
    case tarihHesap = "tarih-hesap"
    case dunya, kure, pomodoro
    case seri3 = "seri-3", model10 = "model-10", seri7 = "seri-7", modelHepsi = "model-hepsi"

    var id: String { rawValue }
    var name: String { L("badges.items.\(rawValue).name") }
    var how: String { L("badges.items.\(rawValue).how") }

    var icon: String {
        switch self {
        case .renk: "paintpalette"
        case .saniye: "hand.tap"
        case .ilkModel: "clock.arrow.2.circlepath"
        case .tamEkran: "arrow.up.left.and.arrow.down.right"
        case .duzenle: "slider.horizontal.3"
        case .takvim: "calendar"
        case .kronometre: "stopwatch"
        case .zamanlayici: "timer"
        case .alarm: "alarm"
        case .uyanik: "sun.max"
        case .cevirici: "arrow.left.arrow.right"
        case .tarihHesap: "plus.forwardslash.minus"
        case .dunya: "globe.europe.africa"
        case .kure: "binoculars"
        case .pomodoro: "leaf"
        case .seri3: "flame"
        case .model10: "square.grid.3x3"
        case .seri7: "trophy"
        case .modelHepsi: "crown"
        }
    }
}

/// Rozetler yalnız bu cihazda, tek anahtarda ("badges", JSON) tutulur: kazanılan rozetin kimliği ve kazanma günü
/// (yerel tarih, saat yok) ile koşulu hesaplamaya yeten sayaçlar. Eylem günlüğü yok; hiçbir yere gönderilmez
/// (uygulamada istatistik yok). "Rozetleri sil" hepsini siler.
@MainActor @Observable
final class BadgeStore {
    static let shared = BadgeStore()
    static let modelGoal = 10

    struct Saved: Codable, Equatable {
        /// rozet kimliği → kazanma günü ("2026-10-30")
        var r: [String: String] = [:]
        /// görülen saat modelleri (sıra numarası bit maskesi)
        var m: Int = 0
        /// son açılış günü ve üst üste gün sayısı (seri rozetleri için)
        var g: String = ""
        var n: Int = 0
    }

    private(set) var saved: Saved
    /// Ekrandaki bildirim (akışı bölmez, birkaç saniyede kaybolur); ilk rozette not bir kez gösterilir
    private(set) var toast: Badge?
    private(set) var toastShowsNote = false
    private var queue: [(Badge, Bool)] = []
    private var showing = false
    private static let key = "badges"

    private init() {
        saved = UserDefaults.standard.data(forKey: Self.key)
            .flatMap { try? JSONDecoder().decode(Saved.self, from: $0) } ?? Saved()
    }

    private func persist() {
        if let d = try? JSONEncoder().encode(saved) { UserDefaults.standard.set(d, forKey: Self.key) }
    }

    nonisolated static func day(_ d: Date) -> String {
        let c = Calendar(identifier: .gregorian).dateComponents([.year, .month, .day], from: d)
        return String(format: "%04d-%02d-%02d", c.year ?? 0, c.month ?? 0, c.day ?? 0)
    }

    func earned(_ b: Badge) -> String? { saved.r[b.rawValue] }
    var earnedCount: Int { Badge.allCases.filter { saved.r[$0.rawValue] != nil }.count }
    var modelsSeen: Int { saved.m.nonzeroBitCount }
    var streak: Int { saved.n }

    func hit(_ b: Badge) {
        guard saved.r[b.rawValue] == nil else { return }
        let first = saved.r.isEmpty
        saved.r[b.rawValue] = Self.day(Date())
        persist()
        queue.append((b, first))
        if !showing { showNext() }
    }

    private func showNext() {
        guard !queue.isEmpty else { showing = false; return }
        showing = true
        let (b, first) = queue.removeFirst()
        toastShowsNote = first
        withAnimation { toast = b }
        UIAccessibility.post(notification: .announcement, argument: T("badges.toast", b.name))
        Task { @MainActor in
            try? await Task.sleep(for: .seconds(first ? 7 : 4))
            withAnimation { self.toast = nil }
            try? await Task.sleep(for: .milliseconds(400))
            self.showNext()
        }
    }

    /// Saat sahnesinde bir model göründü (açılıştaki model dahil)
    func sawModel(_ index: Int, of total: Int) {
        guard index >= 0, index < 62 else { return }
        let m = saved.m | (1 << index)
        if m != saved.m {
            saved.m = m
            persist()
        }
        if modelsSeen >= Self.modelGoal { hit(.model10) }
        if modelsSeen >= total { hit(.modelHepsi) }
    }

    /// Uygulama öne geldi: üst üste gün serisi (yalnız son gün ve gün sayısı tutulur)
    func appOpened(_ now: Date = Date()) {
        let today = Self.day(now)
        guard saved.g != today else { return }
        let yesterday = Calendar(identifier: .gregorian).date(byAdding: .day, value: -1, to: now).map(Self.day) ?? ""
        saved.n = saved.g == yesterday ? max(saved.n, 1) + 1 : 1
        saved.g = today
        persist()
        if saved.n >= 3 { hit(.seri3) }
        if saved.n >= 7 { hit(.seri7) }
    }

    func deleteAll() {
        saved = Saved()
        UserDefaults.standard.removeObject(forKey: Self.key)
        queue.removeAll()
        toast = nil
    }
}

/// Rozet kazanılınca üstte beliren kısa bildirim; dokununca Rozetler açılır
struct BadgeToast: View {
    @State private var store = BadgeStore.shared

    var body: some View {
        if let b = store.toast {
            VStack(alignment: .leading, spacing: 4) {
                Label(T("badges.toast", b.name), systemImage: b.icon)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Theme.text)
                if store.toastShowsNote {
                    Text(L("badges.note")).font(.caption).foregroundStyle(Theme.muted)
                }
            }
            .padding(12)
            .frame(maxWidth: 440, alignment: .leading)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(Theme.accent.opacity(0.6)))
            .padding(.horizontal, 16)
            .padding(.top, 6)
            .transition(.move(edge: .top).combined(with: .opacity))
            .onTapGesture {
                Router.shared.tab = .tools
                Router.shared.toolsPath = [.badges]
            }
            .accessibilityElement(children: .combine)
        }
    }
}

/// Rozetler ekranı: bütün rozetler (kazanılan ve henüz kazanılmayan, koşulu ve ilerlemesiyle), not, gizlilik
/// bağlantısı ve onaylı "Rozetleri sil"
struct BadgesView: View {
    @State private var store = BadgeStore.shared
    @State private var confirming = false
    @State private var deleted = false

    var body: some View {
        List {
            Section {
                Text(L("badges.note")).font(.footnote)
                Link(L("badges.privacyLink"), destination: SiteLinks.privacy)
                    .font(.footnote)
                    .tint(Theme.accent)
                Text(T("badges.count", num(store.earnedCount), num(Badge.allCases.count)))
                    .font(.subheadline.weight(.semibold))
            }
            Section {
                ForEach(Badge.allCases) { b in row(b) }
            }
            Section {
                Button(L("badges.delete"), role: .destructive) { confirming = true }
                if deleted {
                    Text(L("badges.deleted")).font(.footnote).foregroundStyle(Theme.muted)
                }
            }
        }
        .themedBackground()
        .navigationTitle(L("badges.h1"))
        .navigationBarTitleDisplayMode(.inline)
        .alert(L("badges.deleteTitle"), isPresented: $confirming) {
            Button(L("badges.deleteYes"), role: .destructive) {
                store.deleteAll()
                deleted = true
            }
            Button(L("badges.deleteNo"), role: .cancel) {}
        } message: {
            Text(L("badges.deleteText"))
        }
    }

    private func row(_ b: Badge) -> some View {
        let day = store.earned(b).flatMap { CalendarDay(iso: $0) }
        return HStack(spacing: 14) {
            Image(systemName: b.icon)
                .font(.title2)
                .frame(width: 36)
                .foregroundStyle(day != nil ? Theme.accent : Theme.muted.opacity(0.5))
            VStack(alignment: .leading, spacing: 3) {
                Text(b.name).font(.headline)
                Text(b.how).font(.subheadline)
                // Durum yalnız renkle değil metinle de anlatılır
                Label(status(b, day), systemImage: day != nil ? "checkmark.seal.fill" : "lock")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(day != nil ? Theme.text : Theme.muted)
            }
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .combine)
    }

    private func status(_ b: Badge, _ day: CalendarDay?) -> String {
        if let day { return T("badges.earnedOn", Fmt.day(day)) }
        let progress: (Int, Int)? = switch b {
        case .model10: (min(store.modelsSeen, BadgeStore.modelGoal), BadgeStore.modelGoal)
        case .modelHepsi: (store.modelsSeen, ClockModel.allCases.count)
        case .seri3: (min(store.streak, 3), 3)
        case .seri7: (min(store.streak, 7), 7)
        default: nil
        }
        guard let progress else { return L("badges.locked") }
        return L("badges.locked") + " · " + T("badges.progress", num(progress.0), num(progress.1))
    }
}
