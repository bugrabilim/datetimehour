import SwiftUI
import SaatCore

/* Geri sayım ekleri (2. analiz #73, #102, #138, #139; 3 Kasım 2026): gösterim birimi, tarih geçince ileri sayım,
   "38. yıl", kart rengi, arşiv, etiket, yalnız iş günü; tam ekran ayrıntı (gün/saat/dakika/saniye sütunları, sıfırda
   konfeti), bağlantıyla paylaşma (sitedeki ?t=&n= biçimi; uygulamada saattarih://countdown?t=&n= ile eklenir) ve
   hedef anına alarm. Android'deki CountdownExtras.kt ile aynı alanlar. */

extension CountdownItem {
    static let units = ["auto", "days", "weeks", "hours", "minutes", "parts"]
    /// Kart renkleri (0 = renksiz)
    static let palette: [Color] = [.clear, Color(red: 0.86, green: 0.25, blue: 0.25), Color(red: 0.93, green: 0.52, blue: 0.13),
                                   Color(red: 0.85, green: 0.68, blue: 0.08), Color(red: 0.18, green: 0.62, blue: 0.36),
                                   Color(red: 0.16, green: 0.47, blue: 0.85), Color(red: 0.52, green: 0.33, blue: 0.82),
                                   Color(red: 0.86, green: 0.30, blue: 0.58)]
    static let importedNote = Notification.Name("countdown.imported")

    var tint: Color? {
        guard let c = color, c > 0, c < Self.palette.count else { return nil }
        return Self.palette[c]
    }

    /// Her yıl tekrarlanan geri sayımda kaçıncı yıl ("38. yıl"); ilk yıl ya da tekrarsızda boş
    func ordinalText(at: Date) -> String {
        guard yearly else { return "" }
        let cal = Calendar.current
        let n = cal.component(.year, from: at) - cal.component(.year, from: date)
        return n > 0 ? " · " + T("countdowns.ordinal", n) : ""
    }

    /// Sitedeki geri sayım aracının paylaşım bağlantısı (?t=YYYY-MM-DDTHH:MM&n=Başlık)
    var shareURL: URL? {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd'T'HH:mm"
        var c = URLComponents(string: "https://time.bumba.tr" + (AppLocale.code == "tr" ? "/geri-sayim/" : "/\(AppLocale.code)/countdown/"))
        c?.queryItems = [URLQueryItem(name: "t", value: f.string(from: effective())), URLQueryItem(name: "n", value: title)]
        return c?.url
    }

    /// saattarih://countdown?t=…&n=… (sitedeki "Uygulamaya ekle" bağlantısı) → listeye eklenir
    @discardableResult
    static func importURL(_ url: URL) -> Bool {
        let q = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems ?? []
        guard let t = q.first(where: { $0.name == "t" })?.value else { return false }
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = t.count > 10 ? "yyyy-MM-dd'T'HH:mm" : "yyyy-MM-dd"
        guard let date = f.date(from: t) else { return false }
        let title = String((q.first(where: { $0.name == "n" })?.value ?? "").prefix(40))
        var list = load()
        let ms = Int64(date.timeIntervalSince1970 * 1000)
        if !list.contains(where: { $0.target == ms && $0.title == title }) {
            list.append(CountdownItem(title: title, emoji: "🎉", target: ms))
            save(list)
            NotificationCenter.default.post(name: importedNote, object: nil)
        }
        return true
    }
}

/// Listede kalan süre: seçilen birimle; tarih geçtiyse (yıllık değilse) ileri sayım ("12 gün geçti")
func countdownDisplay(_ c: CountdownItem, at: Date, now: Date) -> String {
    let cal = Calendar.current
    let left = at.timeIntervalSince(now)
    let days = cal.dateComponents([.day], from: cal.startOfDay(for: now), to: cal.startOfDay(for: at)).day ?? 0
    if left <= 0 {
        if c.yearly || days == 0 { return countdownText(at, now: now) }
        return T("countdowns.since", -days)
    }
    if c.workdays == true {
        let w = DateCalc.workdays(from: CalendarDay(now), to: CalendarDay(at), offDays: CalendarData.shared.offDays)
        return T("countdowns.workLeft", w == w.rounded() ? "\(Int(w))" : String(format: "%.1f", w).replacingOccurrences(of: ".", with: AppLocale.numberLocale.decimalSeparator ?? "."))
    }
    switch c.unit ?? "auto" {
    case "days": return T("client.daysLeft", max(days, 0))
    case "weeks": return T("countdowns.weeksLeft", days / 7, days % 7)
    case "hours": return T("countdowns.hoursLeft", Int(left / 3600))
    case "minutes": return T("countdowns.minutesLeft", Int(left / 60))
    case "parts":
        let s = Int(left)
        return T("countdowns.parts", s / 86400) + " " + Fmt.countdown(s % 86400)
    default: return countdownText(at, now: now)
    }
}

struct CountdownChip: View {
    let title: String
    let on: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title).font(.subheadline.weight(on ? .semibold : .regular))
                .padding(.horizontal, 12).padding(.vertical, 6)
                .background(on ? Theme.accent : Theme.surface2, in: Capsule())
                .foregroundStyle(on ? Color.white : Theme.text)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(on ? .isSelected : [])
    }
}

/// Tam ekran ayrıntı (2. analiz #102): fotoğraf ya da renk zemini, cam kutuda gün/saat/dakika/saniye, sıfırda konfeti;
/// bağlantıyla paylaş (#138) ve hedef anına alarm (#139)
struct CountdownDetailView: View {
    @Environment(\.dismiss) private var dismiss
    let item: CountdownItem
    let onEdit: () -> Void
    @State private var alarmMsg = ""

    var body: some View {
        NavigationStack {
            TimelineView(.periodic(from: .now, by: 1)) { ctx in
                let at = item.effective(now: ctx.date)
                let left = max(0, Int(at.timeIntervalSince(ctx.date)))
                let finished = at <= ctx.date && ctx.date.timeIntervalSince(at) < 86_400
                ZStack {
                    background
                    VStack(spacing: 18) {
                        Spacer()
                        Text(item.emoji).font(.system(size: 64))
                        Text(item.title.isEmpty ? L("tools.hub.cards.countdown.name") : item.title)
                            .font(.title.bold()).multilineTextAlignment(.center)
                        Text(Fmt.longDate(at) + " " + Fmt.time(at) + item.ordinalText(at: at)).font(.subheadline).opacity(0.8)
                        if at > ctx.date {
                            HStack(spacing: 10) {
                                column(left / 86400, L("countdowns.col.days"))
                                column(left % 86400 / 3600, L("countdowns.col.hours"))
                                column(left % 3600 / 60, L("countdowns.col.minutes"))
                                column(left % 60, L("countdowns.col.seconds"))
                            }
                            .environment(\.layoutDirection, .leftToRight)
                        } else {
                            Text(countdownDisplay(item, at: at, now: ctx.date)).font(.system(size: 40, weight: .bold, design: .rounded))
                        }
                        Spacer()
                        HStack(spacing: 12) {
                            if let url = item.shareURL {
                                ShareLink(item: url, subject: Text(item.label), message: Text(item.label)) {
                                    Label(L("countdowns.shareLink"), systemImage: "link")
                                }
                                .buttonStyle(.bordered)
                            }
                            Button { Task { await addAlarm(at) } } label: { Label(L("countdowns.alarm"), systemImage: "alarm") }
                                .buttonStyle(.bordered)
                                .disabled(at <= ctx.date)
                        }
                        if !alarmMsg.isEmpty { Text(alarmMsg).font(.footnote).opacity(0.85) }
                    }
                    .padding(24)
                    .foregroundStyle(.white)
                    if finished { Confetti(start: at).allowsHitTesting(false).accessibilityHidden(true) }
                }
            }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(L("common.done")) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button(L("countdowns.edit")) { onEdit() } }
            }
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }

    @ViewBuilder private var background: some View {
        if let img = item.image {
            Image(uiImage: img).resizable().scaledToFill().overlay(Color.black.opacity(0.5)).ignoresSafeArea()
        } else {
            LinearGradient(colors: [item.tint ?? Theme.accent, Color.black.opacity(0.85)], startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()
        }
    }

    private func column(_ v: Int, _ label: String) -> some View {
        VStack(spacing: 4) {
            Text(String(format: "%02d", v)).font(.system(size: 40, weight: .bold, design: .rounded)).monospacedDigit()
            Text(label).font(.caption).opacity(0.8)
        }
        .frame(minWidth: 68).padding(.vertical, 12)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 14))
        .accessibilityElement(children: .combine)
    }

    /// Hedef anına bir kezlik alarm (geri sayımın adıyla)
    private func addAlarm(_ at: Date) async {
        let c = Calendar.current.dateComponents([.hour, .minute], from: at)
        var a = AlarmItem(hour: c.hour ?? 9, minute: c.minute ?? 0)
        a.label = String(item.label.prefix(40))
        a.date = CalendarDay(at).iso
        var items = AlarmItem.load()
        items.append(a)
        AlarmItem.save(items)
        await AlarmCenter.sync(items)
        alarmMsg = T("alarm.quickSet", Fmt.longDate(at) + " " + Fmt.time(at))
    }
}

/// Konfeti: renkli parçalar yukarıdan döne döne düşer (hareketi azalt açıksa çizilmez)
struct Confetti: View {
    let start: Date
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        if !reduceMotion {
            TimelineView(.animation) { ctx in
                Canvas { g, size in
                    let t = ctx.date.timeIntervalSince(start)
                    let colors: [Color] = [.red, .orange, .yellow, .green, .blue, .purple, .pink]
                    for i in 0..<90 {
                        let seed = Double(i) * 12.9898
                        let x0 = (sin(seed) * 0.5 + 0.5) * size.width
                        let speed = 60 + (cos(seed * 1.7) * 0.5 + 0.5) * 120
                        let y = (Double(i % 15) * -40 + t.truncatingRemainder(dividingBy: 12) * speed).truncatingRemainder(dividingBy: size.height + 40) - 20
                        let x = x0 + sin(t * 2 + seed) * 18
                        var p = g
                        p.translateBy(x: x, y: y)
                        p.rotate(by: .radians(t * 3 + seed))
                        p.fill(Path(CGRect(x: -4, y: -2, width: 8, height: 4)), with: .color(colors[i % colors.count]))
                    }
                }
            }
        }
    }
}
