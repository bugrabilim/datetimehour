import SwiftUI
import SaatCore

/// Kapsayıcı takvimler (9 Kasım 2026, sahibin onayı; sözleşme v1; Android ExtraCalendars.kt ile aynı): Hristiyan, Yahudi,
/// Çin, İran, Hindu… takvimlerinin özel günleri ve ikinci tarihleri. Hesap yapım anında sitedeki JS modüllerinde yapılır
/// (lib/calendars/<id>.mjs); gen-strings.mjs `calendars.json`'u ve `cals.<id>.*` metinlerini yazar, burada yalnız okunur.
/// Dosya yoksa özellik görünmez. Kod takvimden bağımsızdır (yeni takvim yalnız veriyle gelir); yalnız konuma bağlı küçük
/// eklemeler `hook`'ta.
///
/// Seçim `cal.extra` (virgülle açık takvim kimlikleri; Android ile aynı anahtar, iCloud ile eşitlenir; yalnız kullanıcı
/// değiştirince yazılır). Kayıt yoksa: dinî olmayan takvimler telefonun bölgesi `regions` içindeyse açık, dinî takvimler
/// kapalı: kimsenin inancı tahmin edilmez. Açık takvimlerin olayları `calEvents`'e tür `cal:<id>` ile eklenir.
///
/// Metinler: ad `cals.<id>.name`, açıklama `.desc`, olay `.ev.<anahtar>`, ay adları `.month.<n>` (1'den; dizi 0'dan
/// yazılmışsa `.month.0` vardır ve ona göre okunur), artık ay `.leapFmt` ({m}), ikinci tarih `.fmt` ({d} {m} {y}; Türkçe
/// metinde bu sırayla), yıl etiketi `.yearFmt` (tek yer tutucu), günlük kod `.daily.<kod>` (yoksa bileşik kodun
/// parçaları), kaynak notu `.note`.
enum ExtraCals {
    static let key = "cal.extra"
    private static let defaultColor = Color(hex: 0x6B7C93)

    /// Olay: başlangıç, ad anahtarı, isteğe bağlı bitiş (çok günlü; her günde görünür)
    struct Ev: Hashable {
        let start: CalendarDay
        let key: String
        let end: CalendarDay?
    }

    /// Ay tablosu satırı: miladi ay başı, yıl, ay (1'den), artık/tekrar ay
    struct MonthRow {
        let start: CalendarDay
        let year: Int
        let month: Int
        let leap: Bool
    }

    /// İkinci tarih: gün, ay, yıl ve artık (tekrar) ay işareti
    struct SecondDate: Equatable {
        let day: Int
        let month: Int
        let year: Int
        let leap: Bool
    }

    struct Cal {
        let id: String
        let group: String
        let regions: Set<String>
        let color: Color
        /// gün numarası → olaylar
        let byDay: [Int: [Ev]]
        let months: [MonthRow]?
        let dailyByDate: [Int: String]
        let dailyByYear: [Int: [String]]
        let years: [Int: String]

        var religious: Bool { group == "religious" }

        /// Günlük kod: tarih → kod sözlüğünden ya da yıl başına diziden (yılın gününe göre, 0'dan)
        func daily(_ d: CalendarDay) -> String? {
            if let c = dailyByDate[d.dayNumber], !c.isEmpty { return c }
            if let arr = dailyByYear[d.year], d.dayOfYear - 1 < arr.count, !arr[d.dayOfYear - 1].isEmpty { return arr[d.dayOfYear - 1] }
            return nil
        }
    }

    /// Gün ayrıntısındaki satır
    struct DayLine: Identifiable {
        let id: String
        let name: String
        let color: Color
        let text: String
    }

    /// Bütün takvimler (kimliğe göre sıralı); dosya yoksa boş
    static let all: [Cal] = Bundle.main.url(forResource: "calendars", withExtension: "json")
        .flatMap { try? Data(contentsOf: $0) }
        .map(parse) ?? []

    static func parse(_ data: Data) -> [Cal] {
        guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let cals = root["cals"] as? [String: Any] else { return [] }
        return cals.compactMap { id, v in (v as? [String: Any]).map { parseCal(id, $0) } }.sorted { $0.id < $1.id }
    }

    private static func int(_ v: Any) -> Int { (v as? NSNumber)?.intValue ?? Int("\(v)") ?? 0 }

    private static func hexColor(_ s: String) -> Color? {
        var h = s.trimmingCharacters(in: .whitespaces)
        if h.hasPrefix("#") { h.removeFirst() }
        guard h.count == 6, let v = UInt32(h, radix: 16) else { return nil }
        return Color(hex: v)
    }

    private static func parseCal(_ id: String, _ c: [String: Any]) -> Cal {
        let regions = Set(((c["regions"] as? [Any]) ?? []).compactMap { ($0 as? String)?.uppercased() })
        let color = (c["color"] as? String).flatMap(hexColor) ?? defaultColor
        var byDay: [Int: [Ev]] = [:]
        for case let r as [Any] in (c["events"] as? [Any]) ?? [] {
            guard r.count >= 2, let s = r[0] as? String, let start = CalendarDay(iso: s), let k = r[1] as? String else { continue }
            var end: CalendarDay?
            if r.count > 2, let e = r[2] as? String, let ed = CalendarDay(iso: e), ed > start { end = ed }
            let ev = Ev(start: start, key: k, end: end)
            // Çok günlü olay her gününde (en çok 60 gün; bozuk veride sonsuz döngü olmasın)
            for n in start.dayNumber...min((end ?? start).dayNumber, start.dayNumber + 59) { byDay[n, default: []].append(ev) }
        }
        var months: [MonthRow]?
        if let arr = c["months"] as? [Any] {
            let rows = arr.compactMap { item -> MonthRow? in
                guard let r = item as? [Any], r.count >= 3, let s = r[0] as? String, let start = CalendarDay(iso: s) else { return nil }
                return MonthRow(start: start, year: int(r[1]), month: int(r[2]), leap: r.count > 3 && int(r[3]) != 0)
            }.sorted { $0.start < $1.start }
            if rows.count >= 2 { months = rows }
        }
        var byDate: [Int: String] = [:]
        var byYear: [Int: [String]] = [:]
        if let d = c["daily"] as? [String: Any] {
            for (k, v) in d {
                if let arr = v as? [Any], let y = Int(k) { byYear[y] = arr.map { ($0 as? String) ?? "" } }
                else if let s = v as? String, let day = CalendarDay(iso: k) { byDate[day.dayNumber] = s }
            }
        }
        var years: [Int: String] = [:]
        if let y = c["years"] as? [String: Any] {
            for (k, v) in y {
                guard let yr = Int(k) else { continue }
                if let n = v as? NSNumber { let x = n.doubleValue; years[yr] = x == x.rounded() ? String(Int(x)) : "\(x)" }
                else { years[yr] = "\(v)" }
            }
        }
        return Cal(id: id, group: (c["group"] as? String) ?? "cultural", regions: regions, color: color, byDay: byDay,
                   months: months, dailyByDate: byDate, dailyByYear: byYear, years: years)
    }

    static func find(_ id: String) -> Cal? { all.first { $0.id == id } }

    /// Ayarlarda gösterim sırası: resmî, kültürel, dinî; her grupta ada göre
    static func sorted() -> [Cal] {
        let order = ["civil", "cultural", "religious"]
        func rank(_ c: Cal) -> Int { order.firstIndex(of: c.group) ?? order.count }
        return all.sorted {
            rank($0) != rank($1) ? rank($0) < rank($1)
                : name($0.id).compare(name($1.id), locale: AppLocale.locale) == .orderedAscending
        }
    }

    // MARK: Seçim

    /// Telefonun bölgesi (Ayarlar › Dil ve Bölge); uygulama içi dil etkilemez
    static var systemRegion: String { (Locale.current.region?.identifier ?? "").uppercased() }

    /// Kayıt yokken açık takvimler: dinî olmayanlar bölgesinde; dinîler hiçbir yerde
    static func defaults(region: String = systemRegion) -> Set<String> {
        guard !region.isEmpty else { return [] }
        return Set(all.filter { !$0.religious && $0.regions.contains(region) }.map(\.id))
    }

    private static var chosen: Set<String>? {
        UserDefaults.standard.string(forKey: key).map { Set($0.split(separator: ",").map(String.init)) }
    }

    /// Açık takvimler (yalnız verisi olanlar)
    static var enabled: Set<String> {
        guard !all.isEmpty else { return [] }
        guard let ids = chosen else { return defaults() }
        return Set(all.map(\.id).filter { ids.contains($0) })
    }

    static func isOn(_ id: String) -> Bool { enabled.contains(id) }

    /// Aç/kapa: seçimin tamamı yazılır (bilinmeyen kimlikler korunur; ileride gelen takvim kaybolmasın)
    static func set(_ id: String, on: Bool) {
        var s = chosen ?? defaults()
        if on { s.insert(id) } else { s.remove(id) }
        UserDefaults.standard.set(s.sorted().joined(separator: ","), forKey: key)
    }

    /// Açık takvimlerin bu günkü olayları (çok günlüler her gününde; `date` başlangıç günüdür)
    static func events(on day: CalendarDay) -> [CalendarData.Event] {
        guard !all.isEmpty else { return [] }
        let on = enabled
        guard !on.isEmpty else { return [] }
        return all.filter { on.contains($0.id) }.flatMap { c in
            (c.byDay[day.dayNumber] ?? []).map { e in
                CalendarData.Event(date: e.start.iso, key: "cal:\(c.id):\(e.key)", type: "cal:\(c.id)", label: eventName(c.id, e.key))
            }
        }
    }

    static func color(_ id: String) -> Color { find(id)?.color ?? defaultColor }

    // MARK: Metinler

    /// `cals.*` metni; yoksa nil
    static func text(_ key: String) -> String? {
        let v = L(key)
        return v == key ? nil : v
    }

    /// Yer tutuculu `cals.*` metni (sıra Türkçedeki gibi); yoksa nil
    static func format(_ key: String, _ args: CustomStringConvertible...) -> String? {
        guard let f = text(key) else { return nil }
        return String(format: f, arguments: args.map { "\($0)" as CVarArg })
    }

    static func name(_ id: String) -> String { text("cals.\(id).name") ?? id }
    static func desc(_ id: String) -> String? { text("cals.\(id).desc") }
    static func note(_ id: String) -> String? { text("cals.\(id).note") }
    static func eventName(_ id: String, _ key: String) -> String { text("cals.\(id).ev.\(key)") ?? key }

    /// Ay adı: ay numarası 1'den; metin dizisi 0'dan yazılmışsa (`.month.0` varsa) bir eksiği
    static func monthName(_ id: String, _ month: Int) -> String? {
        let zeroBased = text("cals.\(id).month.0") != nil
        return text("cals.\(id).month.\(zeroBased ? month - 1 : month)")
    }

    /// Ay tablosundan ikinci tarih; son satır yalnız bitiş sınırıdır (son ayın uzunluğu bilinmez)
    static func secondDate(_ c: Cal, _ d: CalendarDay) -> SecondDate? {
        guard let m = c.months else { return nil }
        var lo = 0, hi = m.count - 1, idx = -1
        while lo <= hi {
            let mid = (lo + hi) / 2
            if m[mid].start <= d { idx = mid; lo = mid + 1 } else { hi = mid - 1 }
        }
        guard idx >= 0, idx < m.count - 1 else { return nil }
        let r = m[idx]
        return SecondDate(day: d.dayNumber - r.start.dayNumber + 1, month: r.month, year: r.year, leap: r.leap)
    }

    /// "15 Nisan 5787" gibi (takvimin `fmt` biçimiyle; artık ay `leapFmt` ile)
    static func dateText(_ c: Cal, _ s: SecondDate) -> String {
        let base = monthName(c.id, s.month) ?? String(s.month)
        let m = s.leap ? (format("cals.\(c.id).leapFmt", base) ?? base) : base
        return format("cals.\(c.id).fmt", s.day, m, s.year) ?? "\(s.day) \(m) \(s.year)"
    }

    /// Günlük kodun adı: önce kodun kendisi, yoksa bileşik kodun parçaları ("t5n12" → t5, n12)
    static func dailyText(_ c: Cal, _ d: CalendarDay) -> String? {
        guard let code = c.daily(d) else { return nil }
        if let t = text("cals.\(c.id).daily.\(code)") { return t }
        guard let re = try? NSRegularExpression(pattern: "[A-Za-z]+[0-9]+") else { return nil }
        let ns = code as NSString
        let parts = re.matches(in: code, range: NSRange(location: 0, length: ns.length)).map { ns.substring(with: $0.range) }
        guard parts.count >= 2 else { return nil }
        let names = parts.compactMap { text("cals.\(c.id).daily.\($0)") }
        return names.isEmpty ? nil : names.joined(separator: " · ")
    }

    /// Yıl etiketi ("Reiwa 9" gibi; `yearFmt` yoksa gösterilmez)
    static func yearText(_ c: Cal, _ d: CalendarDay) -> String? {
        c.years[d.year].flatMap { format("cals.\(c.id).yearFmt", $0) }
    }

    /// Konuma bağlı küçük eklemeler (takvim başına isteğe bağlı; yeni takvim için gerekmez). Metin anahtarı yoksa hiçbir
    /// şey gösterilmez. Yahudi takviminde cuma Şabat mumu (gün batımı − 18 dk), cumartesi Havdala (Güneş −8,5°).
    static func hook(_ id: String, _ d: CalendarDay, _ place: AppPlace) -> [String] {
        switch id {
        case "jewish":
            if d.weekday == 4, let t = sunTime(d, place, altitude: -0.833, plusMinutes: -18) { return [format("cals.jewish.candles", t)].compactMap { $0 } }
            if d.weekday == 5, let t = sunTime(d, place, altitude: -8.5, plusMinutes: 0) { return [format("cals.jewish.havdalah", t)].compactMap { $0 } }
            return []
        default:
            return []
        }
    }

    /// Güneş'in akşam `altitude` dereceye indiği an (+ dakika), yerin saatiyle "HH:mm"; oluşmuyorsa nil
    static func sunTime(_ d: CalendarDay, _ place: AppPlace, altitude: Double, plusMinutes: Int) -> String? {
        let noon = d.noon(in: place.zone)
        let offset = Double(place.zone.secondsFromGMT(for: noon)) / 3600
        guard let h = PrayerTimes.sunAltitudeTimes(year: d.year, month: d.month, day: d.day, latitude: place.lat, longitude: place.lon,
                                                   utcOffset: offset, altitude: altitude).set else { return nil }
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = place.zone
        let at = cal.startOfDay(for: noon).addingTimeInterval((h * 3600).rounded() + Double(plusMinutes * 60))
        return place.hm(at)
    }

    /// Gün ayrıntısındaki tek satır: ikinci tarih · günlük kod · yıl etiketi · konuma bağlı eklemeler
    static func dayLine(_ c: Cal, _ d: CalendarDay, place: AppPlace? = nil) -> String? {
        var parts = [secondDate(c, d).map { dateText(c, $0) }, dailyText(c, d), yearText(c, d)].compactMap { $0 }
        if let place { parts += hook(c.id, d, place) }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    /// Açık takvimlerin gün satırları (ayarlardaki sırayla)
    static func dayLines(_ d: CalendarDay, place: AppPlace? = nil) -> [DayLine] {
        let on = enabled
        guard !on.isEmpty else { return [] }
        return sorted().filter { on.contains($0.id) }.compactMap { c in
            dayLine(c, d, place: place).map { DayLine(id: c.id, name: name(c.id), color: c.color, text: $0) }
        }
    }
}

/// Gün ayrıntısında açık takvimlerin ikinci tarihleri: her takvim tek satır, küçük, takvimin rengiyle nokta
struct ExtraCalDayLines: View {
    let day: CalendarDay
    @AppStorage(ExtraCals.key) private var extraRaw: String?

    var body: some View {
        ForEach(ExtraCals.dayLines(day, place: PlaceModel.shared.place)) { line in
            HStack(spacing: 7) {
                Circle().fill(line.color).frame(width: 7, height: 7)
                Text(line.text).font(.caption).foregroundStyle(Theme.text).lineLimit(1)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(line.name + ": " + line.text)
        }
    }
}

/// "Takvimler" bölümü (Takvimler ve ülke sayfasında): her takvim için ad, bir cümle açıklama ve aç/kapa
struct ExtraCalsSection: View {
    @AppStorage(ExtraCals.key) private var extraRaw: String?

    var body: some View {
        let list = ExtraCals.sorted()
        if !list.isEmpty {
            Section {
                ForEach(list, id: \.id) { c in
                    Toggle(isOn: Binding(get: { ExtraCals.isOn(c.id) }, set: { ExtraCals.set(c.id, on: $0) })) {
                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: 8) {
                                Circle().fill(c.color).frame(width: 10, height: 10)
                                Text(ExtraCals.name(c.id)).foregroundStyle(Theme.text)
                            }
                            if let d = ExtraCals.desc(c.id) {
                                Text(d).font(.caption).foregroundStyle(Theme.muted).lineLimit(2).padding(.leading, 18)
                            }
                        }
                    }
                }
            } header: {
                Text(L("calendars.heading"))
            } footer: {
                VStack(alignment: .leading, spacing: 6) {
                    Text(L("calendars.note"))
                    // Takvimlerin kaynak ve bölge notları "Nasıl çalışır?" alt sayfasında
                    let notes: [(String?, String)] = list.compactMap { c in ExtraCals.note(c.id).map { (ExtraCals.name(c.id), $0) } }
                    if !notes.isEmpty { HowItWorks(sections: notes) }
                }
            }
        }
    }
}
