import SwiftUI
import SaatCore

/// Ay verileri (2. analiz #147; 3 Kasım 2026): uzaklık, sıradaki yerberi/yeröte ve süper Ay (sitedeki gece gökyüzü
/// sayfasıyla aynı kural), dördünler (Ay–Güneş açısının 90° olduğu an, ikiye bölme), üst geçiş. Android'deki MoonExtras.kt
/// ile aynı.
enum MoonDetails {
    private static let hour: Double = 3_600_000

    static func distanceKm(_ ms: Double) -> Double {
        6378.14 / sin(Nature.moonPosition(atMilliseconds: ms).parallax * .pi / 180)
    }

    struct Extreme { let at: Double; let km: Double }

    /// Sıradaki yerberi ve yeröte (32 gün içinde): saatlik tarama + parabol
    static func perigeeApogee(_ now: Double) -> (Extreme?, Extreme?) {
        var per: Extreme?, apo: Extreme?
        var prev = distanceKm(now - hour), cur = distanceKm(now)
        var i = 1
        while i <= 32 * 24 && (per == nil || apo == nil) {
            let t = now + Double(i) * hour
            let nx = distanceKm(t)
            let den = prev - 2 * cur + nx
            let off = den != 0 ? 0.5 * (prev - nx) / den : 0
            let tt = t - hour + off * hour
            if per == nil && cur < prev && cur <= nx { per = Extreme(at: tt, km: distanceKm(tt)) }
            if apo == nil && cur > prev && cur >= nx { apo = Extreme(at: tt, km: distanceKm(tt)) }
            prev = cur; cur = nx; i += 1
        }
        return (per, apo)
    }

    /// Sıradaki dolunay süper Ay ise anı (ms)
    static func superMoon(_ now: Double, perigee: Extreme?) -> Double? {
        let full = Double(Nature.moon(atMilliseconds: Int64(now)).nextFullMoon)
        guard let p = perigee, abs(full - p.at) < 1.5 * 86_400_000, distanceKm(full) < 362_000 else { return nil }
        return full
    }

    /// Ay ile Güneş arasındaki açı (derece)
    static func elongation(_ ms: Double) -> Double {
        let m = Nature.moonPosition(atMilliseconds: ms), s = Nature.sunPosition(atMilliseconds: ms)
        let r = Double.pi / 180
        let c = sin(m.declination * r) * sin(s.declination * r) + cos(m.declination * r) * cos(s.declination * r) * cos((m.rightAscension - s.rightAscension) * r)
        return acos(max(-1, min(1, c))) / r
    }

    private static func bisect(_ a0: Double, _ b0: Double, rising: Bool) -> Double {
        var a = a0, b = b0
        for _ in 0..<40 {
            let mid = (a + b) / 2
            if (elongation(mid) > 90) == rising { b = mid } else { a = mid }
        }
        return (a + b) / 2
    }

    /// Sıradaki ilk dördün ve son dördün anları (ms)
    static func quarters(_ now: Double) -> (Double, Double) {
        let k0 = Int(floor((now / 86_400_000 + 2440587.5 - 2451550.09766) / 29.530588861)) - 1
        var first = 0.0, last = 0.0
        for k in k0...(k0 + 3) {
            let new = Double(Nature.phaseMilliseconds(k: k, full: false))
            let full = Double(Nature.phaseMilliseconds(k: k, full: true))
            let next = Double(Nature.phaseMilliseconds(k: k + 1, full: false))
            let fq = bisect(new, full, rising: true), lq = bisect(full, next, rising: false)
            if first == 0 && fq > now { first = fq }
            if last == 0 && lq > now { last = lq }
        }
        return (first, last)
    }

    /// O gün Ay'ın en yüksek olduğu an ve yüksekliği
    static func transit(_ place: AppPlace, _ day: CalendarDay) -> (Date, Double) {
        let start = place.startOfDay(day).timeIntervalSince1970 * 1000
        var best = start, bestAlt = -90.0
        var t = start
        while t < start + 86_400_000 {
            let a = Nature.moonAltitude(t, lat: place.lat, lon: place.lon)
            if a > bestAlt { bestAlt = a; best = t }
            t += 600_000
        }
        var lo = best - 600_000, hi = best + 600_000
        for _ in 0..<30 {
            let m1 = lo + (hi - lo) / 3, m2 = hi - (hi - lo) / 3
            if Nature.moonAltitude(m1, lat: place.lat, lon: place.lon) < Nature.moonAltitude(m2, lat: place.lat, lon: place.lon) { lo = m1 } else { hi = m2 }
        }
        let at = (lo + hi) / 2
        return (Date(timeIntervalSince1970: at / 1000), Nature.moonAltitude(at, lat: place.lat, lon: place.lon))
    }
}

/// Seçili günün Ay'ı (2. analiz #146): büyük evre, aydınlık yüzde, doğuş/batış; çentikli kaydırıcı ve ‹ › ile gün gün
struct MoonScrubber: View {
    let place: AppPlace
    let today: CalendarDay
    @Binding var offset: Int

    var body: some View {
        let day = today.adding(days: offset)
        let at = place.startOfDay(day).addingTimeInterval(offset == 0 ? Date().timeIntervalSince(place.startOfDay(today)) : 21 * 3600)
        let m = Nature.moon(at: at)
        let rs = place.moonRiseSet(day)
        VStack(spacing: 6) {
            HStack {
                Button { offset = max(-60, offset - 1) } label: { Image(systemName: "chevron.left") }.accessibilityLabel(L("prayer.prevMonth"))
                Spacer()
                VStack(spacing: 4) {
                    Text(moonPhaseEmoji[m.phaseIndex]).font(.system(size: 64))
                    Text(L("nature.phases.\(moonPhaseKeys[m.phaseIndex])")).font(.headline)
                    Text(T("nature.illumination", Int((m.illumination * 100).rounded()))).font(.caption).foregroundStyle(Theme.muted)
                    Text(Fmt.day(day) + "  ·  " + (offset == 0 ? L("client.today") : offset > 0 ? T("moon.daysAfter", offset) : T("moon.daysBefore", -offset)))
                        .font(.footnote).multilineTextAlignment(.center)
                    Text("↑ " + place.moonTime(rs.rise, none: "nature.moonNoRise") + "   ↓ " + place.moonTime(rs.set, none: "nature.moonNoSet"))
                        .font(.footnote).monospacedDigit()
                }
                Spacer()
                Button { offset = min(60, offset + 1) } label: { Image(systemName: "chevron.right") }.accessibilityLabel(L("prayer.nextMonth"))
            }
            .buttonStyle(.borderless)
            .environment(\.layoutDirection, .leftToRight)
            Slider(value: Binding(get: { Double(offset) }, set: { offset = Int($0.rounded()) }), in: -60...60, step: 1).tint(Theme.accent)
            Text(L("moon.scrubHint")).font(.caption).foregroundStyle(Theme.muted)
        }
    }
}

/// Ay verileri kartı (2. analiz #147)
struct MoonDataSection: View {
    let place: AppPlace
    let now: Date

    var body: some View {
        let ms = now.timeIntervalSince1970 * 1000
        let pa = MoonDetails.perigeeApogee(ms)
        let per = pa.0, apo = pa.1
        let sup = MoonDetails.superMoon(ms, perigee: per)
        let q = MoonDetails.quarters(ms)
        let today = CalendarDay(now, timeZone: place.zone)
        let tr = MoonDetails.transit(place, today)
        let next = [today, today.adding(days: 1)].flatMap { d -> [(Int64, Bool)] in
            let r = place.moonRiseSet(d)
            return [r.rise.map { ($0, true) }, r.set.map { ($0, false) }].compactMap { $0 }
        }.filter { Double($0.0) > ms }.sorted { $0.0 < $1.0 }.first
        Section(L("moon.data")) {
            if let n = next {
                Text(T(n.1 ? "moon.riseIn" : "moon.setIn", Fmt.hm(seconds: Int((Double(n.0) - ms) / 1000))))
                    .font(.subheadline.weight(.semibold)).foregroundStyle(Theme.accent)
            }
            LabeledContent(L("moon.transit"), value: place.hm(tr.0) + " · \(Int(tr.1.rounded()))°")
            LabeledContent(L("sky.moonNow"), value: km(MoonDetails.distanceKm(ms)))
            if let per { LabeledContent(L("sky.perigee"), value: dt(per.at) + " · " + km(per.km)) }
            if let apo { LabeledContent(L("sky.apogee"), value: dt(apo.at) + " · " + km(apo.km)) }
            LabeledContent(L("moon.firstQuarter"), value: dt(q.0))
            LabeledContent(L("moon.lastQuarter"), value: dt(q.1))
            if let sup {
                Text("🌕 " + T("sky.superMoon", Fmt.longDate(Date(timeIntervalSince1970: sup / 1000), tz: place.zone)))
                    .font(.footnote).foregroundStyle(Theme.accent)
            }
        }
    }

    private func dt(_ ms: Double) -> String {
        let d = Date(timeIntervalSince1970: ms / 1000)
        return Fmt.dayMonth(d, tz: place.zone) + " " + place.hm(d)
    }

    private func km(_ v: Double) -> String {
        let f = NumberFormatter()
        f.numberStyle = .decimal
        f.maximumFractionDigits = 0
        f.locale = AppLocale.numberLocale
        return T("sky.km", f.string(from: NSNumber(value: v.rounded())) ?? "\(Int(v))")
    }
}
