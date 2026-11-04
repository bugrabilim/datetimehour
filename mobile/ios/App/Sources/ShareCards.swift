import SwiftUI
import UIKit
import SaatCore
import WeatherKit

/* Paylaşım kartları (30 Ekim 2026, rakip analizi #16 ve #29): bugünün vakitleri (ve varsa dini/özel gün) ile kişisel
   geri sayım, görsele çevrilip sistemin paylaşım penceresiyle gönderilir. Kart telefonda çizilir; bir yere yüklenmez. */

/// Kartın ortak çerçevesi: koyu degrade zemin, açık yazı, altta Saat Tarih imzası (her temada okunaklı)
struct ShareCardFrame<Content: View>: View {
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            content()
            HStack {
                Spacer()
                Text(verbatim: "Saat Tarih · time.bumba.tr").font(.caption.weight(.semibold)).opacity(0.8)
            }
        }
        .padding(22)
        .foregroundStyle(.white)
        .background(LinearGradient(colors: [Color(red: 0.10, green: 0.13, blue: 0.25), Color(red: 0.25, green: 0.13, blue: 0.33)],
                                   startPoint: .topLeading, endPoint: .bottomTrailing))
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .environment(\.colorScheme, .dark)
    }
}

/// Kartı görsele çevirip önizleme ve "Paylaş" düğmesiyle gösteren pencere
struct ShareCardSheet<Card: View>: View {
    let title: String
    @ViewBuilder let card: () -> Card
    @Environment(\.displayScale) private var displayScale
    @Environment(\.dismiss) private var dismiss
    @State private var image: UIImage?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 18) {
                    card().frame(width: 340)
                    if let image {
                        ShareLink(item: Image(uiImage: image), preview: SharePreview(title, image: Image(uiImage: image))) {
                            Label(L("share.card"), systemImage: "square.and.arrow.up").font(.headline)
                        }
                        .tint(Theme.accent)
                    }
                }
                .padding(20)
                .frame(maxWidth: .infinity)
            }
            .background(AppBackdrop().ignoresSafeArea())
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.large)
            .toolbar { Button(L("common.done")) { dismiss() } }
        }
        .task {
            let r = ImageRenderer(content: card().frame(width: 340))
            r.scale = max(displayScale, 3)
            image = r.uiImage
        }
    }
}

/// Bugünün vakitleri: yer, miladi ve hicri tarih, varsa o günün dini/özel günleri, altı vakit
struct PrayerShareCard: View {
    let place: AppPlace
    let now: Date

    var body: some View {
        let entries = place.schedule.entries(on: now)
        let names = eventNames()
        ShareCardFrame {
            VStack(alignment: .leading, spacing: 4) {
                Text(place.name).font(.title2.weight(.bold))
                Text(Fmt.longDate(now, tz: place.zone)).font(.subheadline).opacity(0.85)
                if let h = Hijri.date(now, timeZone: place.zone) { Text(h.text).font(.subheadline).opacity(0.85) }
            }
            if !names.isEmpty {
                Text("✦ " + names.joined(separator: " · "))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Color(red: 1, green: 0.82, blue: 0.45))
            }
            VStack(spacing: 6) {
                ForEach(entries) { e in
                    HStack {
                        Text(e.prayer.localizedName)
                        Spacer()
                        Text(place.hm(e.at)).monospacedDigit().fontWeight(.semibold)
                            .environment(\.layoutDirection, .leftToRight)
                    }
                    .font(.body)
                }
            }
            .padding(.vertical, 6)
        }
    }
}

extension PrayerShareCard {
    /// O günün dini ve özel günleri (tekrarsız)
    func eventNames() -> [String] {
        var seen = Set<String>()
        return CalendarData.shared.events(on: CalendarDay(now, timeZone: place.zone)).map(\.name).filter { seen.insert($0).inserted }
    }
}

/// Kişisel geri sayım: simge, başlık, kalan gün, hedef tarih
struct CountdownShareCard: View {
    let item: CountdownItem
    let now: Date

    var body: some View {
        let at = item.effective(now: now)
        ShareCardFrame {
            Text(item.emoji).font(.system(size: 54))
            Text(item.title.isEmpty ? L("tools.hub.cards.countdown.name") : item.title).font(.title2.weight(.bold))
            Text(countdownText(at, now: now))
                .font(.system(size: 40, weight: .heavy, design: .rounded))
                .foregroundStyle(Color(red: 1, green: 0.82, blue: 0.45))
                .environment(\.layoutDirection, .leftToRight)
            Text(Fmt.longDate(at)).font(.subheadline).opacity(0.85)
        }
    }
}

/// Hava (2. analiz #105): yer, simge ve sıcaklık, durum, en yüksek/en düşük, sözlü özet; Apple Hava Durumu adı (WeatherKit şartı)
struct WeatherShareCard: View {
    let place: AppPlace
    let weather: Weather

    var body: some View {
        let c = weather.currentWeather
        ShareCardFrame {
            Text(place.name).font(.title2.weight(.bold))
            Text(Fmt.longDate(Date(), tz: place.zone)).font(.subheadline).opacity(0.85)
            HStack(spacing: 10) {
                WeatherIcon3D(symbol: c.symbolName, size: 58)
                Text(Units.temp(c.temperature)).font(.system(size: 54, weight: .heavy, design: .rounded))
                    .foregroundStyle(Color(red: 1, green: 0.82, blue: 0.45))
            }
            .environment(\.layoutDirection, .leftToRight)
            Text(c.condition.description).font(.title3.weight(.semibold))
            if let t = weather.dailyForecast.first {
                Text("↑ \(Units.temp(t.highTemperature))   ↓ \(Units.temp(t.lowTemperature))").environment(\.layoutDirection, .leftToRight)
            }
            let sum = WeatherSpeech.summary(weather, zone: place.zone)
            if !sum.isEmpty { Text(sum).font(.subheadline) }
            Text(verbatim: "\u{F8FF} Weather").font(.caption2).opacity(0.75)
        }
    }
}

/// Ay (2. analiz #105): evre simgesi ve adı, aydınlık yüzde, tarih, doğuş/batış
struct MoonShareCard: View {
    let place: AppPlace
    let day: CalendarDay

    var body: some View {
        let m = Nature.moon(at: place.startOfDay(day).addingTimeInterval(21 * 3600))
        let rs = place.moonRiseSet(day)
        ShareCardFrame {
            Text(moonPhaseEmoji[m.phaseIndex]).font(.system(size: 72))
            Text(L("nature.phases.\(moonPhaseKeys[m.phaseIndex])")).font(.title2.weight(.bold))
                .foregroundStyle(Color(red: 1, green: 0.82, blue: 0.45))
            Text(T("nature.illumination", Int((m.illumination * 100).rounded())))
            Text(Fmt.day(day) + " · " + place.name).font(.subheadline).opacity(0.85)
            Text("↑ " + place.moonTime(rs.rise, none: "nature.moonNoRise") + "   ↓ " + place.moonTime(rs.set, none: "nature.moonNoSet"))
                .font(.subheadline).monospacedDigit()
        }
    }
}

/// Odak (2. analiz #105): "Bugün 3 sa 58 dk odaklandım", tur sayısı, seri, en çok çalışılan görev
struct FocusShareCard: View {
    let minutes: Int
    let sessions: Int
    let streak: Int
    let task: String?

    var body: some View {
        ShareCardFrame {
            Text("🍅").font(.system(size: 56))
            Text(T("share.focus", Fmt.hm(seconds: minutes * 60))).font(.title.weight(.heavy))
                .foregroundStyle(Color(red: 1, green: 0.82, blue: 0.45))
            Text(T("tools.pomodoro.statLine", sessions, minutes))
            if streak >= 2 { Text("🔥 " + T("tools.pomodoro.streak", streak)) }
            if let task, !task.isEmpty { Text("✦ " + task).font(.subheadline).opacity(0.85) }
        }
    }
}

