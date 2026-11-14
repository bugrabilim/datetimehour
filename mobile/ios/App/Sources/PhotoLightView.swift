import SwiftUI
import SaatCore

/*
 * Fotoğraf ışığı (9 Kasım 2026, sahibin onayı; Android PhotoLightScreen ve sitedeki fotoğraf ışığı sayfasıyla aynı içerik):
 * seçili yer ve gün için sabah/akşam mavi ve altın saat, gün doğumu/batımı saati ve yönü, Ay (doğuş, batış, evre), bulut
 * tahmininden bugün ve sonraki 6 günün gün doğumu/batımı fotoğrafı tahmini (iyi / orta / kötü) ve 7 günlük ışık tablosu.
 * Saatler telefonda (SaatCore PhotoLight; Mevsimler'deki tanımlar), bulut tahmini Open-Meteo'dan (yalnız yaklaşık konum;
 * WeatherKit katman katman bulut vermediği için iOS'ta da Open-Meteo).
 */

/// Tercihler (Android'deki PhotoPrefs ile aynı anahtarlar)
enum PhotoPrefs {
    /// Ekran bir kez açıldı mı: bildirimler ancak o zaman kurulur (uyku planındaki yatma hatırlatması gibi)
    static let usedKey = "photo.used"
    static let leadKey = "notify.photo.lead"
    static let eventsKey = "notify.photo.events"
    static let eventIDs = ["amBlue", "amGold", "pmGold", "pmBlue"]
    static let leads = [10, 20, 30, 60]

    static var used: Bool {
        get { UserDefaults.standard.bool(forKey: usedKey) }
        set { UserDefaults.standard.set(newValue, forKey: usedKey) }
    }
    static var lead: Int { UserDefaults.standard.object(forKey: leadKey) as? Int ?? 20 }
    static var events: Set<String> {
        get { Set((UserDefaults.standard.string(forKey: eventsKey) ?? "pmGold").split(separator: ",").map(String.init)) }
        set { UserDefaults.standard.set(eventIDs.filter { newValue.contains($0) }.joined(separator: ","), forKey: eventsKey) }
    }

    /// Olayın adı (Mevsimler'deki metinler)
    static func label(_ e: String) -> String {
        switch e {
        case "amBlue": return L("nature.blueAm")
        case "amGold": return L("nature.goldenAm")
        case "pmGold": return L("nature.goldenPm")
        default: return L("nature.bluePm")
        }
    }
}

/// Bulut tahmini (Open-Meteo, anahtarsız): saatlik alçak/orta/yüksek bulut ve yağış, 7 gün. Koordinat 2 ondalık, aynı yer
/// 30 dk önbellekte (OpenMeteoExtras'taki gibi).
enum PhotoSky {
    private static var cache: (String, Date, [PhotoLight.Sample])?

    private static func key(_ lat: Double, _ lon: Double) -> String { String(format: "%.2f,%.2f", lat, lon) }

    /// Önbellekteki son veri (bildirimler ağa çıkmaz, yalnız bunu kullanır)
    static func cached(lat: Double, lon: Double) -> [PhotoLight.Sample]? {
        guard let c = cache, c.0 == key(lat, lon) else { return nil }
        return c.2
    }

    static func get(lat: Double, lon: Double) async -> [PhotoLight.Sample]? {
        let k = key(lat, lon)
        if let c = cache, c.0 == k, Date().timeIntervalSince(c.1) < 1800 { return c.2 }
        let parts = k.split(separator: ",")
        let url = "https://api.open-meteo.com/v1/forecast?latitude=\(parts[0])&longitude=\(parts[1])"
            + "&hourly=cloud_cover_low,cloud_cover_mid,cloud_cover_high,precipitation&forecast_days=7&timezone=auto&timeformat=unixtime"
        guard let u = URL(string: url) else { return cached(lat: lat, lon: lon) }
        var req = URLRequest(url: u, timeoutInterval: 15)
        req.setValue("SaatTarih/1.0", forHTTPHeaderField: "User-Agent")
        guard let (data, resp) = try? await URLSession.shared.data(for: req), (resp as? HTTPURLResponse)?.statusCode == 200,
              let o = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else { return cached(lat: lat, lon: lon) }
        let v = parse(o)
        guard !v.isEmpty else { return cached(lat: lat, lon: lon) }
        cache = (k, Date(), v)
        return v
    }

    /// Open-Meteo yanıtı (timeformat=unixtime): eksik değerli saatler atlanır
    static func parse(_ o: [String: Any]) -> [PhotoLight.Sample] {
        guard let h = o["hourly"] as? [String: Any], let t = h["time"] as? [Any] else { return [] }
        let low = h["cloud_cover_low"] as? [Any] ?? [], mid = h["cloud_cover_mid"] as? [Any] ?? []
        let high = h["cloud_cover_high"] as? [Any] ?? [], pr = h["precipitation"] as? [Any] ?? []
        func num(_ a: [Any], _ i: Int) -> Double? { i < a.count ? (a[i] as? NSNumber)?.doubleValue : nil }
        return t.indices.compactMap { i -> PhotoLight.Sample? in
            guard let ts = (t[i] as? NSNumber)?.int64Value, let l = num(low, i), let m = num(mid, i), let hh = num(high, i) else { return nil }
            return PhotoLight.Sample(t: ts * 1000, low: l, mid: m, high: hh, precip: num(pr, i) ?? 0)
        }
    }
}

/// Bir yer ve günün ışığı; ondalık saatler o günün yerel öğlesinden sayılır (yaz saati geçişinde de doğru an)
struct PhotoDay {
    let date: CalendarDay
    let place: AppPlace
    let day: PhotoLight.Day
    private let noon: Date

    init(_ date: CalendarDay, _ place: AppPlace) {
        self.date = date
        self.place = place
        noon = date.noon(in: place.zone)
        let off = Double(place.zone.secondsFromGMT(for: noon)) / 3600
        day = PhotoLight.day(year: date.year, month: date.month, day: date.day, latitude: place.lat, longitude: place.lon, utcOffset: off)
    }

    /// Ondalık saat → an (dakikaya yuvarlanır; Mevsimler'deki saatlerle aynı)
    func at(_ h: Double?) -> Date? { h.map { noon.addingTimeInterval((($0 * 60).rounded() - 720) * 60) } }

    var sunrise: Date? { at(day.sunrise) }
    var sunset: Date? { at(day.sunset) }

    /// Herhangi bir eşik o gün oluşmuyor mu ("—" notu için)
    var partial: Bool {
        let l = day.light
        return day.polar == 0 && [l.blueAm, l.goldenAm, l.goldenPm, l.bluePm].contains { $0.0 == nil || $0.1 == nil }
    }

    /// Olayın başladığı an: amBlue, amGold, pmGold, pmBlue
    func start(_ e: String) -> Date? {
        switch e {
        case "amBlue": return at(day.light.blueAm.0)
        case "amGold": return at(day.light.goldenAm.0)
        case "pmGold": return at(day.light.goldenPm.0)
        default: return at(day.light.bluePm.0)
        }
    }

    /// Saat (kullanıcının 24/12 seçimiyle)
    func clock(_ d: Date?) -> String {
        guard let d else { return "—" }
        return [Fmt.time(d, tz: place.zone), Fmt.ampm(d, tz: place.zone)].filter { !$0.isEmpty }.joined(separator: " ")
    }

    /// "06:41–07:28" (12 saatte "6:41–7:28 AM"; öğleden önce ve sonra ayrıysa ikisi de)
    func span(_ r: (Double?, Double?), ampm: Bool = true) -> String {
        let a = at(r.0), b = at(r.1)
        if a == nil && b == nil { return "—" }
        guard let a, let b, ampm else {
            let sep = (a == nil || b == nil) ? " – " : "–"
            return [a, b].map { d in d.map { Fmt.time($0, tz: place.zone) } ?? "—" }.joined(separator: sep)
        }
        let pa = Fmt.ampm(a, tz: place.zone), pb = Fmt.ampm(b, tz: place.zone)
        return Fmt.time(a, tz: place.zone) + (!pa.isEmpty && pa != pb ? " \(pa)" : "") + "–" + clock(b)
    }

    /// "108° Doğu" (kuzeyden saat yönünde; 8 yönün adı)
    func direction(_ az: Double?) -> String {
        guard let az else { return "" }
        return "\(Int(az.rounded()) % 360)° " + L("weather.compass.\(WeatherWords.compass8(az))")
    }

    /// "Pzt 9"
    var label: String { L("calendar.weekdaysShort.\(date.weekday)") + " \(date.day)" }
}

/// Ekran: yer seçici, gün gezintisi, sabah ve akşam ışığı, Ay, bulut tahmini, 7 günlük tablo
struct PhotoLightView: View {
    @State private var model = PlaceModel.shared
    @State private var selected: CalendarDay?
    @State private var samples: [PhotoLight.Sample]?
    @State private var failed = false
    @State private var attempt = 0

    /// "Nasıl çalışır?" bölümleri (Android'dekiyle aynı)
    static var howSections: [(String?, String)] {
        [
            (L("photoApp.subtitle"), L("nature.lightNote")),
            (L("photoApp.how.forecastTitle"), T("photoApp.how.forecast", Fmt.percent(75), Fmt.percent(40), Fmt.percent(20),
                                                 Fmt.percent(80), Fmt.percent(60))),
            (L("photoApp.how.dirTitle"), L("photoApp.how.dir")),
            (L("settings.privacy"), L("photoApp.how.privacy")),
        ]
    }

    var body: some View {
        let place = model.place
        let today = CalendarDay(Date(), timeZone: place.zone)
        let date = selected ?? today
        let p = PhotoDay(date, place)
        let week = (0..<7).map { PhotoDay(today.adding(days: $0), place) }
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Text(L("photoApp.subtitle")).font(.subheadline).foregroundStyle(Theme.muted)
                // Uzun açıklamalar ekranda değil "Nasıl çalışır?" alt sayfasında (doğal tasarım madde 13)
                HowItWorks(sections: Self.howSections)
                PlacePicker()
                dayNav(date, today: today, place: place)
                if p.day.polar != 0 {
                    Text(L(p.day.polar == 1 ? "photoApp.polarDay" : "photoApp.polarNight")).font(.footnote).foregroundStyle(Theme.muted)
                }
                morning(p)
                evening(p)
                if p.partial { Text(L("photoApp.partial")).font(.footnote).foregroundStyle(Theme.muted) }
                PhotoMoonCard(p: p, isToday: date == today)
                forecast(week, date: date)
                weekTable(week, date: date)
            }
            .padding(16)
        }
        .themedBackground()
        .navigationTitle(L("photoApp.title"))
        .navigationBarTitleDisplayMode(.large)
        .task(id: "\(place.key)-\(attempt)") { await load(place) }
        .onAppear {
            // İlk açılış: bildirimler artık kurulabilir
            if !PhotoPrefs.used {
                PhotoPrefs.used = true
                Task { await PrayerNotifier.reschedule() }
            }
        }
    }

    private func load(_ place: AppPlace) async {
        samples = PhotoSky.cached(lat: place.lat, lon: place.lon)
        failed = false
        if let v = await PhotoSky.get(lat: place.lat, lon: place.lon) { samples = v } else { failed = samples == nil }
    }

    private func quality(_ d: Date?) -> PhotoLight.Quality? {
        guard let d, let samples else { return nil }
        return PhotoLight.quality(at: Int64(d.timeIntervalSince1970 * 1000), in: samples)
    }

    /// Önceki / sonraki gün (yazı yönüne göre çevrilen oklar), bugüne dön
    private func dayNav(_ date: CalendarDay, today: CalendarDay, place: AppPlace) -> some View {
        HStack {
            Button {
                if date > today.adding(days: -366) { selected = date.adding(days: -1) }
            } label: {
                Image(systemName: "chevron.backward").font(.headline).frame(width: 44, height: 44)
            }
            .accessibilityLabel(L("photoApp.prevDay"))
            Spacer()
            VStack(spacing: 2) {
                Text(Fmt.longDate(date.noon(in: place.zone), tz: place.zone)).font(.headline).multilineTextAlignment(.center)
                if date != today {
                    Button(L("client.today")) { selected = today }.font(.subheadline.weight(.medium))
                }
            }
            Spacer()
            Button {
                if date < today.adding(days: 366) { selected = date.adding(days: 1) }
            } label: {
                Image(systemName: "chevron.forward").font(.headline).frame(width: 44, height: 44)
            }
            .accessibilityLabel(L("photoApp.nextDay"))
        }
        .buttonStyle(.borderless)
        .tint(Theme.accent)
    }

    private func morning(_ p: PhotoDay) -> some View {
        let l = p.day.light
        let rise = p.sunrise.map { p.clock($0) + " · " + p.direction(p.day.riseAzimuth) } ?? "—"
        return lightCard(L("photoApp.morning"), quality(p.sunrise)) {
            PhotoLightRow(icon: "sunrise", tint: Theme.sky, label: L("photoApp.blue"), value: p.span(l.blueAm))
            PhotoLightRow(icon: "sunrise", tint: Theme.gold, label: L("photoApp.golden"), value: p.span(l.goldenAm))
            PhotoLightRow(icon: "sun", tint: Theme.terra, label: L("nature.sunrise"), value: rise)
        }
    }

    private func evening(_ p: PhotoDay) -> some View {
        let l = p.day.light
        let set = p.sunset.map { p.clock($0) + " · " + p.direction(p.day.setAzimuth) } ?? "—"
        return lightCard(L("photoApp.evening"), quality(p.sunset)) {
            PhotoLightRow(icon: "sunset", tint: Theme.gold, label: L("photoApp.golden"), value: p.span(l.goldenPm))
            PhotoLightRow(icon: "sun", tint: Theme.terra, label: L("nature.sunset"), value: set)
            PhotoLightRow(icon: "sunset", tint: Theme.sky, label: L("photoApp.blue"), value: p.span(l.bluePm))
        }
    }

    private func lightCard<C: View>(_ title: String, _ q: PhotoLight.Quality?, @ViewBuilder content: () -> C) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                SectionTitle(text: title)
                if let q { PhotoQualityPill(q: q) }
            }
            content()
        }
        .card()
    }

    /// Bugün ve sonraki 6 günün gün doğumu ve batımı tahmini; satıra dokununca o gün seçilir
    private func forecast(_ week: [PhotoDay], date: CalendarDay) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            SectionTitle(text: L("photoApp.forecast"))
            Text(L("photoApp.forecastNote")).font(.caption).foregroundStyle(Theme.muted)
            if let samples {
                HStack(spacing: 8) {
                    Spacer()
                    columnHead("sunrise", L("nature.sunrise"))
                    columnHead("sunset", L("nature.sunset"))
                }
                ForEach(week, id: \.date) { d in
                    Button { selected = d.date } label: {
                        HStack(spacing: 8) {
                            Text(d.label).font(.subheadline.weight(d.date == date ? .semibold : .regular)).foregroundStyle(Theme.text)
                            Spacer()
                            PhotoQualityPill(q: d.sunrise.flatMap { PhotoLight.quality(at: Int64($0.timeIntervalSince1970 * 1000), in: samples) })
                                .frame(width: 96)
                            PhotoQualityPill(q: d.sunset.flatMap { PhotoLight.quality(at: Int64($0.timeIntervalSince1970 * 1000), in: samples) })
                                .frame(width: 96)
                        }
                        .padding(.horizontal, 6)
                        .padding(.vertical, 6)
                        .background(d.date == date ? Theme.accentSoft : Color.clear, in: RoundedRectangle(cornerRadius: 12))
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
                if !week.contains(where: { $0.date == date }) {
                    Text(L("photoApp.noForecast")).font(.caption).foregroundStyle(Theme.muted)
                }
            } else if failed {
                Text(L("weather.error")).font(.subheadline).foregroundStyle(Theme.muted)
                Button(L("weather.retry")) { attempt += 1 }.tint(Theme.accent)
            } else {
                Text(L("weather.loading")).font(.subheadline).foregroundStyle(Theme.muted)
            }
            Text(L("weather.attribution")).font(.caption2).foregroundStyle(Theme.muted)
        }
        .card()
    }

    private func columnHead(_ icon: String, _ title: String) -> some View {
        HStack(spacing: 4) {
            DuoIcon(name: icon, size: 14, color: Theme.gold)
            Text(title).font(.caption2).foregroundStyle(Theme.muted).lineLimit(1).minimumScaleFactor(0.8)
        }
        .frame(width: 96)
    }

    /// 7 günlük tablo: sabah altın, akşam altın, akşam mavi saat (sütun başlıkları sabah/akşamı söylediği için ÖÖ/ÖS yazılmaz)
    private func weekTable(_ week: [PhotoDay], date: CalendarDay) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            SectionTitle(text: L("photoApp.week")).padding(.bottom, 4)
            HStack(alignment: .bottom, spacing: 4) {
                Spacer().frame(maxWidth: .infinity)
                ForEach(["photoApp.colAmGold", "photoApp.colPmGold", "photoApp.colPmBlue"], id: \.self) { k in
                    Text(L(k)).font(.caption2).foregroundStyle(Theme.muted).multilineTextAlignment(.center).frame(maxWidth: .infinity)
                }
            }
            ForEach(week, id: \.date) { d in
                Button { selected = d.date } label: {
                    HStack(spacing: 4) {
                        Text(d.label).font(.caption.weight(d.date == date ? .semibold : .regular)).frame(maxWidth: .infinity, alignment: .leading)
                        ForEach(Array([d.day.light.goldenAm, d.day.light.goldenPm, d.day.light.bluePm].enumerated()), id: \.offset) { _, r in
                            Text(d.span(r, ampm: false)).font(.caption).monospacedDigit().lineLimit(1).minimumScaleFactor(0.7)
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .foregroundStyle(Theme.text)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 7)
                    .background(d.date == date ? Theme.accentSoft : Color.clear, in: RoundedRectangle(cornerRadius: 12))
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .card()
    }
}

/// Satır: iki tonlu simge, ad ve değer
private struct PhotoLightRow: View {
    let icon: String
    let tint: Color
    let label: String
    let value: String

    var body: some View {
        HStack(spacing: 8) {
            DuoIcon(name: icon, size: 18, color: tint)
            Text(label).foregroundStyle(Theme.text)
            Spacer(minLength: 8)
            Text(value).monospacedDigit().foregroundStyle(Theme.text).multilineTextAlignment(.trailing)
        }
        .font(.subheadline)
        .frame(minHeight: 30)
    }
}

/// Tahmin hapı: renkli nokta ve ad (renk tek başına anlam taşımaz)
struct PhotoQualityPill: View {
    let q: PhotoLight.Quality?
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        if let q {
            let col = Self.color(q)
            HStack(spacing: 6) {
                Circle().fill(col).frame(width: 8, height: 8)
                Text(L("photoApp.q.\(q.rawValue)")).font(.footnote.weight(.medium)).foregroundStyle(Theme.text)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(col.opacity(scheme == .dark ? 0.22 : 0.14), in: Capsule())
        } else {
            Text("—").font(.subheadline).foregroundStyle(Theme.muted)
        }
    }

    static func color(_ q: PhotoLight.Quality) -> Color {
        switch q {
        case .good: return Theme.sage
        case .fair: return Theme.gold
        case .poor: return Theme.muted
        }
    }
}

/// Ay: evre ve aydınlık yüzdesi (bugünse şimdiki an, değilse o günün öğlesi), o günün doğuş ve batışı
private struct PhotoMoonCard: View {
    let p: PhotoDay
    let isToday: Bool

    var body: some View {
        let at = isToday ? Date() : p.date.noon(in: p.place.zone)
        let m = Nature.moon(at: at)
        let rs = p.place.moonRiseSet(p.date)
        VStack(alignment: .leading, spacing: 8) {
            SectionTitle(text: L("nature.moonHeading"))
            HStack(spacing: 10) {
                Text(moonPhaseEmoji[m.phaseIndex]).font(.system(size: 30))
                VStack(alignment: .leading, spacing: 2) {
                    Text(L("nature.phases.\(moonPhaseKeys[m.phaseIndex])")).font(.headline).foregroundStyle(Theme.text)
                    Text(T("photoApp.lit", Fmt.percent(Int((m.illumination * 100).rounded())))).font(.caption).foregroundStyle(Theme.muted)
                }
            }
            PhotoLightRow(icon: "moon", tint: Theme.dusk, label: L("nature.moonrise"), value: time(rs.rise))
            PhotoLightRow(icon: "moon", tint: Theme.dusk, label: L("nature.moonset"), value: time(rs.set))
        }
        .card()
    }

    private func time(_ ms: Int64?) -> String {
        guard let ms else { return "—" }
        return p.clock(Date(unixMilliseconds: (ms + 30_000) / 60_000 * 60_000))
    }
}

/// Bildirim ayarı (Ayarlar › Bildirimler; Android'deki PhotoOptions ile aynı): hangi ışık, kaç dakika önce
struct PhotoOptionsView: View {
    let changed: () -> Void
    @State private var events: Set<String> = []
    @AppStorage(PhotoPrefs.leadKey) private var lead = 20

    var body: some View {
        Group {
            ForEach(PhotoPrefs.eventIDs, id: \.self) { e in
                Toggle(PhotoPrefs.label(e), isOn: Binding(get: { events.contains(e) }, set: { on in
                    if on { events.insert(e) } else { events.remove(e) }
                    PhotoPrefs.events = events
                    changed()
                }))
                .padding(.leading, 12)
            }
            Picker(L("notify.lead"), selection: $lead) {
                ForEach(PhotoPrefs.leads, id: \.self) { Text(SunMoonPrefs.leadLabel($0)).tag($0) }
            }
            .padding(.leading, 12)
            .onChange(of: lead) { changed() }
        }
        .onAppear { events = PhotoPrefs.events }
    }
}
