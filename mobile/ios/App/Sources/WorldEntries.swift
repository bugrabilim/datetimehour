import SwiftUI
import SaatCore

/*
 Dünya saatleri kaydı (2. analiz #128, #129; 3 Kasım 2026). `world` listesindeki anahtar üç türden olabilir: şehir anahtarı
 (cities.json, 31 şehir), "p:<enlem>:<boylam>" (places.json'daki 1960 yer; koordinatla bulunur) ya da "tz:<IANA dilimi>"
 (kısaltmayla aramadan, ör. EST). Kişisel ad ve emoji `world.labels` (JSON {anahtar: ad}). Android'deki WorldEntries.kt
 ile aynı biçim.
 */
struct WorldEntry: Identifiable, Hashable {
    let key: String
    let name: String
    let country: String
    let zone: TimeZone
    let lat: Double?
    let lon: Double?
    /// ISO ülke kodu (bayrak için; yalnız 31 şehirde bilinir)
    var cc: String? = nil

    var id: String { key }
    /// Kişisel ad ("Annem 👵"); yoksa nil
    var label: String? { WorldLabels.get(key) }
    var flag: String { cc.map(Self.flagOf) ?? "" }
    /// Kısa alanlarda (saat ekranı) gösterilen ad: kişisel ad varsa o
    var displayName: String { label ?? name }

    /// Saat dilimi kısaltması: platformun kısa adı; "GMT+3" gibi ise "UTC+3"
    func abbr(_ now: Date) -> String {
        if let s = zone.abbreviation(for: now), s == "GMT" || (!s.hasPrefix("GMT") && !s.hasPrefix("UTC") && s.count <= 5 && !s.contains("+") && !s.contains("-")) {
            return s
        }
        return utcOffset(now)
    }

    func utcOffset(_ now: Date) -> String {
        let sec = zone.secondsFromGMT(for: now)
        if sec == 0 { return "UTC" }
        let h = abs(sec) / 3600, m = abs(sec) % 3600 / 60
        return "UTC" + (sec > 0 ? "+" : "−") + "\(h)" + (m > 0 ? String(format: ":%02d", m) : "")
    }

    /// Gündüz (Güneş ufkun üstünde), alacakaranlık (0…−6°), gece; konumu bilinmeyen dilimde saate göre
    func light(_ now: Date) -> String {
        guard let lat, let lon else {
            var cal = Calendar(identifier: .gregorian)
            cal.timeZone = zone
            let h = cal.component(.hour, from: now)
            return (7...17).contains(h) ? "day" : [6, 18, 19].contains(h) ? "dusk" : "night"
        }
        let s = GlobeSky.sun(now)
        let r = Double.pi / 180
        let v = sin(lat * r) * sin(s.lat * r) + cos(lat * r) * cos(s.lat * r) * cos((lon - s.lon) * r)
        let alt = asin(max(-1, min(1, v))) / r
        return alt > 0 ? "day" : alt > -6 ? "dusk" : "night"
    }

    func nextDST(after now: Date, days: Double = 7) -> (at: Date, delta: Int)? {
        guard let t = zone.nextDaylightSavingTimeTransition(after: now), t.timeIntervalSince(now) <= days * 86400 else { return nil }
        let delta = zone.secondsFromGMT(for: t.addingTimeInterval(3600)) - zone.secondsFromGMT(for: t.addingTimeInterval(-3600))
        return delta == 0 ? nil : (t, delta)
    }

    func dstNote(now: Date, days: Double = 7) -> String? {
        guard let d = nextDST(after: now, days: days) else { return nil }
        return T(d.delta > 0 ? "world.dstForward" : "world.dstBack", Fmt.dayMonth(d.at, tz: zone), Fmt.hm(seconds: abs(d.delta)))
    }

    /// 31 şehrin ülke kodu (bayrak)
    static let cityCC: [String: String] = [
        "istanbul": "TR", "ankara": "TR", "izmir": "TR", "london": "GB", "paris": "FR", "berlin": "DE", "moscow": "RU",
        "dubai": "AE", "baku": "AZ", "cairo": "EG", "new-york": "US", "los-angeles": "US", "tokyo": "JP", "beijing": "CN",
        "singapore": "SG", "mumbai": "IN", "sydney": "AU", "riyadh": "SA", "doha": "QA", "amsterdam": "NL", "rome": "IT",
        "madrid": "ES", "athens": "GR", "vienna": "AT", "kyiv": "UA", "tehran": "IR", "hong-kong": "HK", "bangkok": "TH",
        "toronto": "CA", "sao-paulo": "BR", "johannesburg": "ZA",
    ]

    /// Kısaltmayla arama (sitedeki saat dilimi kısaltmaları; temsilci dilim; Android'deki ABBRS ile aynı sıra)
    static let abbrs: [(String, String)] = [
        ("UTC", "UTC"), ("GMT", "Etc/GMT"), ("BST", "Europe/London"), ("WET", "Europe/Lisbon"), ("CET", "Europe/Paris"),
        ("CEST", "Europe/Paris"), ("EET", "Europe/Athens"), ("EEST", "Europe/Athens"), ("TRT", "Europe/Istanbul"), ("MSK", "Europe/Moscow"),
        ("GST", "Asia/Dubai"), ("PKT", "Asia/Karachi"), ("IST", "Asia/Kolkata"), ("SGT", "Asia/Singapore"), ("JST", "Asia/Tokyo"),
        ("KST", "Asia/Seoul"), ("AEST", "Australia/Sydney"), ("AEDT", "Australia/Sydney"), ("EST", "America/New_York"),
        ("EDT", "America/New_York"), ("CST", "America/Chicago"), ("CDT", "America/Chicago"), ("MST", "America/Denver"),
        ("PST", "America/Los_Angeles"), ("PDT", "America/Los_Angeles"),
    ]

    static func flagOf(_ cc: String) -> String {
        String(String.UnicodeScalarView(cc.uppercased().unicodeScalars.compactMap { Unicode.Scalar(0x1F1E6 + $0.value - 65) }))
    }

    static func placeKey(_ p: GlobePlace) -> String { String(format: "p:%.2f:%.2f", p.lat, p.lon) }

    static func city(_ c: City) -> WorldEntry {
        WorldEntry(key: c.key, name: c.displayName, country: c.displayCountry, zone: c.zone, lat: c.lat, lon: c.lon, cc: cityCC[c.key])
    }

    static func of(_ key: String) -> WorldEntry? {
        if key.hasPrefix("p:") {
            let parts = key.split(separator: ":")
            guard parts.count == 3, let lat = Double(parts[1]), let lon = Double(parts[2]) else { return nil }
            guard let p = GlobePlace.all.min(by: { abs($0.lat - lat) + abs($0.lon - lon) < abs($1.lat - lat) + abs($1.lon - lon) }),
                  abs(p.lat - lat) < 0.05, abs(p.lon - lon) < 0.05 else { return nil }
            return WorldEntry(key: key, name: p.name, country: "", zone: p.zone, lat: p.lat, lon: p.lon)
        }
        if key.hasPrefix("tz:") {
            let id = String(key.dropFirst(3))
            guard let z = TimeZone(identifier: id) else { return nil }
            let abbr = abbrs.first { "tz:" + $0.1 == key }?.0
            let city = (id.split(separator: "/").last.map(String.init) ?? id).replacingOccurrences(of: "_", with: " ")
            return WorldEntry(key: key, name: abbr != nil && id != "UTC" ? "\(abbr!) · \(city)" : city, country: id, zone: z, lat: nil, lon: nil)
        }
        return City.all.first { $0.key == key }.map(city)
    }

    static func list(_ raw: String) -> [WorldEntry] { raw.split(separator: ",").compactMap { of(String($0)) } }

    /// Arama: önce 31 şehir, sonra 1960 yer, sonra kısaltmalar; ad başta eşleşen öne. Boş aramada 31 şehir.
    static func search(_ q: String, exclude: Set<String>, limit: Int = 60) -> [WorldEntry] {
        let f = District.fold(q.trimmingCharacters(in: .whitespaces))
        let cities = City.all.filter { !exclude.contains($0.key) }.map(city)
        if f.isEmpty { return cities }
        func score(_ name: String) -> Int {
            let n = District.fold(name)
            return n.hasPrefix(f) ? 0 : n.contains(f) ? 1 : 9
        }
        var out: [(Int, WorldEntry)] = []
        for c in cities {
            let s = min(score(c.name), score(c.country) + 1)
            if s < 9 { out.append((s, c)) }
        }
        let cityKeys = Set(out.map(\.1.key))
        for p in GlobePlace.all {
            let k = placeKey(p)
            if exclude.contains(k) || cityKeys.contains(k) { continue }
            let s = score(p.name)
            if s < 9 { out.append((s + 2, WorldEntry(key: k, name: p.name, country: "", zone: p.zone, lat: p.lat, lon: p.lon))) }
        }
        let up = q.trimmingCharacters(in: .whitespaces).uppercased()
        if let z = abbrs.first(where: { $0.0 == up })?.1, !exclude.contains("tz:" + z), let e = of("tz:" + z) { out.append((0, e)) }
        if up.count >= 3 {
            for z in TimeZone.knownTimeZoneIdentifiers.filter({ District.fold($0).contains(f) && !exclude.contains("tz:" + $0) }).prefix(10) {
                if let e = of("tz:" + z) { out.append((5, e)) }
            }
        }
        var seen = Set<String>()
        return out.enumerated().sorted { ($0.element.0, $0.offset) < ($1.element.0, $1.offset) }.map(\.element.1)
            .filter { seen.insert($0.key).inserted }.prefix(limit).map { $0 }
    }
}

/// Dünya saatlerinde kişisel ad ve emoji (`world.labels`: JSON {anahtar: ad}; yalnız cihazda)
enum WorldLabels {
    static let key = "world.labels"

    private static func all() -> [String: String] {
        guard let s = UserDefaults.standard.string(forKey: key), let d = s.data(using: .utf8),
              let o = try? JSONSerialization.jsonObject(with: d) as? [String: String] else { return [:] }
        return o
    }

    static func get(_ k: String) -> String? { all()[k].flatMap { $0.isEmpty ? nil : $0 } }

    static func set(_ k: String, _ label: String) {
        var o = all()
        let t = label.trimmingCharacters(in: .whitespaces)
        if t.isEmpty { o[k] = nil } else { o[k] = String(t.prefix(30)) }
        if let d = try? JSONSerialization.data(withJSONObject: o) { UserDefaults.standard.set(String(decoding: d, as: UTF8.self), forKey: key) }
    }
}

/// Şehir ayrıntısı (2. analiz #129): yerel saat ve tarih, saat dilimi, UTC farkı, kısaltma, gün doğumu/batımı ve gün
/// uzunluğu, Ay evresi, sıradaki saat değişimi; kişisel ad ve emoji
struct WorldDetailView: View {
    @Environment(\.dismiss) private var dismiss
    let entry: WorldEntry
    @State private var label = ""

    var body: some View {
        NavigationStack {
            TimelineView(.periodic(from: .now, by: 1)) { ctx in
                let now = ctx.date
                Form {
                    Section {
                        if !entry.country.isEmpty { Text(entry.country).foregroundStyle(Theme.muted) }
                        Text(Fmt.time(now, tz: entry.zone) + (Fmt.ampm(now, tz: entry.zone).isEmpty ? "" : " " + Fmt.ampm(now, tz: entry.zone)))
                            .font(.system(size: 44, weight: .light, design: .rounded)).monospacedDigit()
                            .environment(\.layoutDirection, .leftToRight)
                        Text(Fmt.longDate(now, tz: entry.zone))
                        Text(entry.zone.identifier + " · " + entry.abbr(now) + " · " + entry.utcOffset(now)).font(.footnote).foregroundStyle(Theme.muted)
                    }
                    Section {
                        if let lat = entry.lat, let lon = entry.lon {
                            let d = CalendarDay(now, timeZone: entry.zone)
                            let off = Double(entry.zone.secondsFromGMT(for: d.noon(in: entry.zone))) / 3600
                            let sun = PrayerTimes.sunAltitudeTimes(year: d.year, month: d.month, day: d.day, latitude: lat, longitude: lon, utcOffset: off, altitude: -0.833)
                            Text("☀ ↑ " + hm(sun.rise) + "   ↓ " + hm(sun.set)).monospacedDigit()
                            if let r = sun.rise, let s = sun.set {
                                let len = Int(((s - r) * 60).rounded())
                                Text(T("seasons.dayLengthValue", len / 60, len % 60)).font(.footnote).foregroundStyle(Theme.muted)
                            }
                        }
                        let moon = Nature.moon(at: now)
                        Text(moonPhaseEmoji[moon.phaseIndex] + " " + L("nature.phases.\(moonPhaseKeys[moon.phaseIndex])"))
                        if let note = entry.dstNote(now: now, days: 60) {
                            Label(note, systemImage: "clock.arrow.2.circlepath").foregroundStyle(Theme.accent)
                        }
                    }
                    Section(L("world.label")) {
                        TextField(L("world.labelHint"), text: $label)
                    }
                }
                .themedBackground()
            }
            .navigationTitle((entry.flag.isEmpty ? "" : entry.flag + " ") + entry.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L("world.done")) { WorldLabels.set(entry.key, label); dismiss() }
                }
            }
            .onAppear { label = entry.label ?? "" }
            .onDisappear { WorldLabels.set(entry.key, label) }
        }
    }

    private func hm(_ h: Double?) -> String {
        guard let h else { return "–" }
        let m = Int((h * 60).rounded())
        return String(format: "%02d:%02d", ((m / 60) % 24 + 24) % 24, m % 60)
    }
}
