import EventKit
import PhotosUI
import SwiftUI
import SaatCore

/* Kişisel geri sayım listesi (30 Ekim 2026, rakip analizi #15); Android'deki Countdowns.kt ile aynı kayıt: "countdowns"
   (JSON; hedef Unix ms). Simge (emoji), her yıl tekrar, hatırlatma (bir gün önce 09.00 ve zamanı gelince; özellik
   bildirimleriyle birlikte kurulur), telefon takviminden seçme, isteğe bağlı fotoğraf (yalnız telefonda,
   "cd-<id>.jpg"; `photo` sürüm numarası). Eski tek hedef (cd.target) bir kez taşınır. */

struct CountdownItem: Codable, Identifiable, Equatable {
    var id = UUID().uuidString
    var title = ""
    var emoji = "⏳"
    /// Hedef (Unix ms)
    var target: Int64
    var yearly = false
    var remind = true
    /// Fotoğrafın sürümü (yoksa nil); dosya "cd-<id>.jpg"
    var photo: Double?

    static let emojis = ["⏳", "🎂", "🎉", "✈️", "🏖️", "🎓", "💍", "❤️", "🎄", "🌙", "⚽", "📚", "💼", "🏥", "👶", "🎁"]

    var date: Date {
        get { Date(timeIntervalSince1970: Double(target) / 1000) }
        set { target = Int64(newValue.timeIntervalSince1970 * 1000) }
    }
    var label: String { "\(emoji) \(title)".trimmingCharacters(in: .whitespaces) }
    var photoName: String { "cd-\(id).jpg" }
    var image: UIImage? { photo.flatMap { PhotoFile.image(photoName, version: $0) } }

    /// Yıllık olanda hedef günü geçtiyse sıradaki yıl (hedef günü boyunca "Bugün!")
    func effective(now: Date = Date()) -> Date {
        var t = date
        guard yearly else { return t }
        let cal = Calendar.current
        let today = cal.startOfDay(for: now)
        while cal.startOfDay(for: t) < today, let next = cal.date(byAdding: .year, value: 1, to: t) { t = next }
        return t
    }

    static func load() -> [CountdownItem] {
        let d = UserDefaults.standard
        var list = d.data(forKey: "countdowns").flatMap { try? JSONDecoder().decode([CountdownItem].self, from: $0) } ?? []
        let old = d.double(forKey: "cd.target") // referans tarihinden saniye
        if old > 0 {
            list.append(CountdownItem(title: d.string(forKey: "cd.title") ?? "",
                                      target: Int64(Date(timeIntervalSinceReferenceDate: old).timeIntervalSince1970 * 1000)))
            d.removeObject(forKey: "cd.target"); d.removeObject(forKey: "cd.title")
            save(list)
        }
        return list
    }

    static func save(_ items: [CountdownItem]) {
        UserDefaults.standard.set(try? JSONEncoder().encode(items), forKey: "countdowns")
    }

    /// Hatırlatmalar: bir gün önce 09.00 ve hedef anında (60 gün içinde)
    @MainActor static func reminders(now: Date) -> [PlannedNotification] {
        load().filter(\.remind).flatMap { c -> [PlannedNotification] in
            let at = c.effective(now: now)
            guard at.timeIntervalSince(now) < 60 * 86400 else { return [] }
            let cal = Calendar.current
            let ms = Int64(at.timeIntervalSince1970 * 1000)
            var out = [PlannedNotification(id: "\(FeatureNotifier.featurePrefix)cd-\(c.id)-now-\(ms)", fireAt: at, title: T("countdowns.remindNow", c.label), body: "")]
            if let dayBefore = cal.date(byAdding: .day, value: -1, to: cal.startOfDay(for: at)),
               let pre = cal.date(bySettingHour: 9, minute: 0, second: 0, of: dayBefore) {
                out.append(PlannedNotification(id: "\(FeatureNotifier.featurePrefix)cd-\(c.id)-pre-\(ms)", fireAt: pre, title: T("countdowns.remindTomorrow", c.label),
                                               body: Fmt.longDate(at)))
            }
            return out.filter { $0.fireAt > now }
        }
    }
}

/// Kalan: "Bugün!", son 24 saatte SS:DD:ss, değilse "N gün kaldı"
func countdownText(_ at: Date, now: Date) -> String {
    let cal = Calendar.current
    let days = cal.dateComponents([.day], from: cal.startOfDay(for: now), to: cal.startOfDay(for: at)).day ?? 0
    let left = Int(at.timeIntervalSince(now).rounded(.up))
    if left <= 0 { return days == 0 ? L("countdowns.today") : L("tools.countdown.finishedDate") }
    if left < 86400 { return Fmt.countdown(left) }
    return T("client.daysLeft", days)
}

/// Yeni yıl, Ramazan, bayramlar, 23 Nisan, 19 Mayıs, 30 Ağustos, 29 Ekim: her birinin sıradaki tarihi
func quickTargets(_ today: CalendarDay) -> [(String, CalendarDay)] {
    let keys = ["newYear", "ramadanStart", "rb1", "kb1", "apr23", "may19", "aug30", "oct29"]
    let events = CalendarData.shared.years.keys.sorted().flatMap { CalendarData.shared.years[$0]!.events }
    var out: [(String, CalendarDay)] = []
    for key in keys {
        if let e = events.first(where: { $0.key == key && ($0.day ?? today) > today }), let d = e.day {
            out.append((key == "newYear" ? L("tools.countdown.quickNewYear") : e.name, d))
        }
    }
    return out.sorted { $0.1 < $1.1 }
}

/// Geri sayımlarım: liste, + ile yeni, dokununca düzenle; altta hazır özel günler
struct CountdownsView: View {
    @State private var items = CountdownItem.load()
    @State private var editing: CountdownItem?
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let today = CalendarDay(Date())
        TimelineView(.periodic(from: .now, by: 1)) { context in
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        SectionTitle(text: L("countdowns.heading"))
                        Spacer()
                        Button {
                            let d = Calendar.current.date(bySettingHour: 9, minute: 0, second: 0, of: Date().addingTimeInterval(7 * 86400)) ?? Date()
                            editing = CountdownItem(target: Int64(d.timeIntervalSince1970 * 1000))
                        } label: { Image(systemName: "plus") }
                            .accessibilityLabel(L("countdowns.add"))
                            .foregroundStyle(Theme.accent)
                    }
                    if items.isEmpty { Text(L("countdowns.empty")).font(.subheadline).foregroundStyle(Theme.muted) }
                    ForEach(items.sorted { $0.effective(now: context.date) < $1.effective(now: context.date) }) { c in
                        let at = c.effective(now: context.date)
                        let photo = c.image
                        HStack(spacing: 12) {
                            Text(c.emoji).font(.system(size: 34))
                            VStack(alignment: .leading, spacing: 2) {
                                Text(c.title.isEmpty ? L("tools.hub.cards.countdown.name") : c.title).font(.headline).lineLimit(1)
                                Text(countdownText(at, now: context.date))
                                    .font(.title3.weight(.bold)).monospacedDigit()
                                    .foregroundStyle(Theme.accent)
                                    .environment(\.layoutDirection, .leftToRight)
                                Text(Fmt.longDate(at) + (Calendar.current.component(.hour, from: at) + Calendar.current.component(.minute, from: at) > 0 ? " " + Fmt.time(at) : "") + (c.yearly ? " · ↻" : ""))
                                    .font(.caption).foregroundStyle(Theme.muted)
                            }
                            Spacer()
                            Button { update(items.filter { $0.id != c.id }) } label: { Image(systemName: "xmark") }
                                .buttonStyle(.borderless)
                                .foregroundStyle(Theme.muted)
                                .accessibilityLabel(L("timers.remove"))
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, minHeight: photo == nil ? nil : 120, alignment: .leading)
                        .background {
                            // Fotoğraflı kart: fotoğraf koyulaştırılır, yazılar koyu temanın açık renklerine geçer
                            if let photo {
                                Image(uiImage: photo).resizable().scaledToFill()
                                    .overlay(Color.black.opacity(0.45))
                            } else {
                                Theme.surface
                            }
                        }
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .environment(\.colorScheme, photo != nil ? .dark : colorScheme)
                        .contentShape(Rectangle())
                        .onTapGesture { editing = c }
                    }
                    SectionTitle(text: L("tools.countdown.quickHeading"))
                    ForEach(Array(quickTargets(today).enumerated()), id: \.offset) { _, pair in
                        Button {
                            let at = pair.1.noon(in: .current).addingTimeInterval(-12 * 3600)
                            let ms = Int64(at.timeIntervalSince1970 * 1000)
                            if !items.contains(where: { $0.title == pair.0 && $0.target == ms }) {
                                update(items + [CountdownItem(title: pair.0, emoji: "🎉", target: ms)])
                            }
                        } label: {
                            HStack {
                                Text(pair.0).foregroundStyle(Theme.text)
                                Spacer()
                                Text(relative(pair.1, today)).font(.caption).foregroundStyle(Theme.muted)
                                Image(systemName: "plus").foregroundStyle(Theme.accent)
                            }
                            .card()
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 16)
            }
        }
        .sheet(item: $editing) { item in
            CountdownEditor(item: item, isNew: !items.contains { $0.id == item.id }) { saved in
                update(items.filter { $0.id != saved.id } + [saved])
            }
        }
    }

    private func update(_ list: [CountdownItem]) {
        // Silinen ya da fotoğrafı kaldırılan geri sayımların dosyaları silinir
        for old in items where !list.contains(where: { $0.id == old.id && $0.photo != nil }) { PhotoFile.remove(old.photoName) }
        items = list
        CountdownItem.save(list)
        Task { await PrayerNotifier.reschedule() } // hatırlatmalar özellik bildirimleriyle birlikte kurulur
    }
}

private struct CountdownEditor: View {
    @Environment(\.dismiss) private var dismiss
    @State var item: CountdownItem
    let isNew: Bool
    let onSave: (CountdownItem) -> Void
    @State private var showCalendar = false
    @State private var pick: PhotosPickerItem?

    var body: some View {
        NavigationStack {
            Form {
                TextField(L("countdowns.titlePlaceholder"), text: $item.title)
                Section(L("countdowns.emoji")) {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) {
                            ForEach(CountdownItem.emojis, id: \.self) { e in
                                Button { item.emoji = e } label: {
                                    Text(e).font(.title2)
                                        .frame(width: 42, height: 42)
                                        .background(e == item.emoji ? Theme.accentSoft : Theme.surface2, in: Circle())
                                        .overlay(Circle().stroke(e == item.emoji ? Theme.accent : .clear, lineWidth: 2))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
                DatePicker(L("tools.countdown.targetLabel"), selection: $item.date)
                Button("📅 " + L("countdowns.fromCalendar")) { showCalendar = true }
                Toggle(L("countdowns.yearly"), isOn: $item.yearly)
                Toggle(L("countdowns.remind"), isOn: $item.remind)
                Section {
                    if let img = item.image {
                        Image(uiImage: img).resizable().scaledToFill()
                            .frame(height: 120).frame(maxWidth: .infinity)
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    PhotosPicker(selection: $pick, matching: .images) { Label(L("look.photo.pick"), systemImage: "photo") }
                    if item.photo != nil {
                        Button(role: .destructive) { item.photo = nil } label: { Label(L("look.photo.remove"), systemImage: "trash") }
                    }
                } header: {
                    Text(L("countdowns.photo"))
                } footer: {
                    Text(L("countdowns.photoNote"))
                }
            }
            .onChange(of: pick) { _, new in
                guard let new else { return }
                Task {
                    if let data = try? await new.loadTransferable(type: Data.self),
                       PhotoFile.save(data, name: item.photoName, maxSide: 1000) {
                        item.photo = Date().timeIntervalSince1970
                    }
                    pick = nil
                }
            }
            .tint(Theme.accent)
            .themedBackground()
            .navigationTitle(L(isNew ? "countdowns.add" : "countdowns.edit"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(L("alarm.cancel")) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L("alarm.save")) {
                        item.title = item.title.trimmingCharacters(in: .whitespaces)
                        onSave(item)
                        dismiss()
                    }
                }
            }
            .sheet(isPresented: $showCalendar) {
                CalendarEventPicker { title, date in
                    item.title = String(title.prefix(40))
                    item.date = date
                }
            }
        }
    }
}

/// Telefondaki takvimlerden önümüzdeki 60 günün etkinlikleri
private struct CalendarEventPicker: View {
    @Environment(\.dismiss) private var dismiss
    let onPick: (String, Date) -> Void

    var body: some View {
        let device = DeviceCalendar.shared
        let now = Date()
        let events: [EKEvent] = device.authorized
            ? device.store.events(matching: device.store.predicateForEvents(withStart: now, end: now.addingTimeInterval(60 * 86400), calendars: nil))
                .sorted { $0.startDate < $1.startDate }
            : []
        NavigationStack {
            List {
                if !device.authorized {
                    Text(L("countdowns.calendarDenied")).foregroundStyle(Theme.muted)
                } else if events.isEmpty {
                    Text(L("countdowns.calendarNone")).foregroundStyle(Theme.muted)
                }
                ForEach(events, id: \.calendarItemIdentifier) { e in
                    Button {
                        onPick(e.title ?? "", e.isAllDay ? Calendar.current.startOfDay(for: e.startDate) : e.startDate)
                        dismiss()
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(e.title ?? "—").foregroundStyle(Theme.text)
                            Text(Fmt.longDate(e.startDate) + (e.isAllDay ? "" : " " + Fmt.time(e.startDate)))
                                .font(.caption).foregroundStyle(Theme.muted)
                        }
                    }
                }
            }
            .themedBackground()
            .navigationTitle(L("countdowns.fromCalendar"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button(L("alarm.cancel")) { dismiss() } } }
        }
    }
}
