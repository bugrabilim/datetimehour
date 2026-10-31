import SwiftUI
import UIKit
import SaatCore

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
            .background(Theme.bg.ignoresSafeArea())
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
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
