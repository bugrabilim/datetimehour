import SwiftUI

/// Uygulama içi widget galerisi (2. analiz #166): bütün widget'lar, boyutları ve nasıl eklendiği. iPhone'da uygulama
/// widget'ı ana ekrana kendisi ekleyemez (Apple izin vermiyor); ekleme ana ekranda uzun basıp "+" ile yapılır.
struct WidgetGalleryView: View {
    private struct Item: Identifiable {
        let id: String
        let icon: String
        let name: String
        let desc: String
        let sizes: [String]
    }

    private var items: [Item] {
        let s = "widgets.size.small", m = "widgets.size.medium", l = "widgets.size.large", k = "widgets.size.lock"
        return [
            Item(id: "next", icon: "moon.stars", name: L("widget.next.name"), desc: L("widget.next.desc"), sizes: [s, k]),
            Item(id: "today", icon: "list.bullet.rectangle", name: L("prayer.today"), desc: L("widget.today.desc"), sizes: [m]),
            Item(id: "analog", icon: "clock", name: L("widget.analog.name"), desc: L("widget.analog.desc"), sizes: [s, l, k]),
            Item(id: "digital", icon: "textformat.123", name: L("widget.digital.name"), desc: L("widget.digital.desc"), sizes: [s]),
            Item(id: "clockdate", icon: "calendar.badge.clock", name: L("widget.clockDate.name"), desc: L("widget.clockDate.desc"), sizes: [s, m]),
            Item(id: "seconds", icon: "stopwatch", name: L("widget.seconds.name"), desc: L("widget.seconds.desc"), sizes: [s]),
            Item(id: "dual", icon: "clock.badge.questionmark", name: L("widget.dual.name"), desc: L("widget.dual.desc"), sizes: [s, m]),
            Item(id: "world", icon: "globe", name: L("world.h1"), desc: L("widget.world.descIOS"), sizes: [s, m]),
            Item(id: "words", icon: "textformat", name: L("widget.words.name"), desc: L("widget.words.desc"), sizes: [s, m]),
            Item(id: "year", icon: "circle.grid.3x3", name: L("widget.year.name"), desc: L("widget.year.desc"), sizes: [s, m, k]),
            Item(id: "sun", icon: "sunrise", name: L("widget.sun.name"), desc: L("widget.sun.desc"), sizes: [s, k]),
            Item(id: "moon", icon: "moon", name: L("widget.moon.name"), desc: L("widget.moon.desc"), sizes: [s]),
            Item(id: "hijri", icon: "moon.haze", name: L("widget.hijri.name"), desc: L("widget.hijri.desc"), sizes: [s, k]),
            Item(id: "daily", icon: "quote.bubble", name: L("widget.daily.name"), desc: L("widget.daily.desc"), sizes: [s, m, l]),
            Item(id: "countdown", icon: "hourglass", name: L("tools.hub.cards.countdown.name"), desc: L("widget.countdown.descIOS"), sizes: [s]),
            Item(id: "zikir", icon: "circle.dotted", name: L("widget.zikir.name"), desc: L("widget.zikir.desc"), sizes: [s]),
            Item(id: "stopwatch", icon: "stopwatch.fill", name: L("tools.hub.cards.stopwatch.name"), desc: L("widget.stopwatch.desc"), sizes: [s]),
        ]
    }

    var body: some View {
        List {
            Section {
                Text(L("widgets.howIOS")).font(.footnote).foregroundStyle(Theme.muted)
            }
            // Şeffaf widget (2. analiz #76): duvar kâğıdından kesilmiş zemin
            if UIDevice.current.userInterfaceIdiom == .phone {
                Section {
                    NavigationLink { WallpaperView() } label: {
                        Label(L("wallpaper.title"), systemImage: "square.dashed")
                    }
                }
            }
            ForEach(items) { w in
                HStack(alignment: .top, spacing: 14) {
                    Image(systemName: w.icon)
                        .font(.title2)
                        .foregroundStyle(Theme.accent)
                        .frame(width: 48, height: 48)
                        .background(Theme.accentSoft, in: RoundedRectangle(cornerRadius: 12))
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 3) {
                        Text(w.name).font(.headline).foregroundStyle(Theme.text)
                        Text(w.desc).font(.subheadline).foregroundStyle(Theme.muted)
                        Text(w.sizes.map { L($0) }.joined(separator: " · ")).font(.caption).foregroundStyle(Theme.accent)
                    }
                }
                .padding(.vertical, 4)
                .accessibilityElement(children: .combine)
            }
        }
        .themedBackground()
        .navigationTitle(L("widgets.gallery"))
    }
}
