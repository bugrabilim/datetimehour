import SwiftUI

/*
 * Alarm duvar kâğıdı (3 Kasım 2026, 2. analiz #2; sahibin ekran görüntüleri). Görsel indirilmez, telif yoktur: sade
 * olanlar siyah ve atmosfer renk geçişlerinin koyu hâli, hareketliler tam ekran saatteki hareketli arka planlar
 * (yıldızlar, renk akışı, gökyüzü, hava). Alarm alanı `wall` (boş = tema rengi); Android'deki AlarmWalls.kt ile aynı
 * adlar. iPhone'da alarm Apple'ın kendi ekranında çaldığı için zemin kapatma görevi ekranında (WakeTaskView) ve
 * alarmdan sonra açılan günaydın kartında görünür. Zemin seçiliyken yazılar koyu temanın açık renkleriyle çizilir.
 */
enum AlarmWalls {
    static let simple: [String] = ["black"] + Atmosphere.all.map(\.id)
    static let animated = ["stars", "flow", "sky", "weather"]
    static let categories = ["simple", "animated"]

    static func title(_ id: String) -> String {
        if id.isEmpty { return L("wall.none") }
        if id == "black" { return L("wall.black") }
        if Atmosphere.find(id) != nil { return L("atmo.\(id)") }
        return L("look.bg.\(id)")
    }

    /// Son iki saatte çalan alarmın duvar kâğıdı (günaydın kartı için); yoksa nil
    static func recent(now: Date = Date()) -> String? {
        let rang = AlarmItem.load().filter(\.enabled)
            .compactMap { a in a.occurrence(before: now).map { (a, $0) } }
            .filter { now.timeIntervalSince($0.1) < 2 * 3600 }
            .max { $0.1 < $1.1 }
        guard let w = rang?.0.wall, !w.isEmpty else { return nil }
        return w
    }
}

/// Duvar kâğıdının kendisi (görev ekranında, günaydın kartında, önizlemede ve seçicideki kartlarda)
struct AlarmWall: View {
    let id: String

    var body: some View {
        if id == "black" {
            Color.black
        } else if let a = Atmosphere.find(id) {
            LinearGradient(colors: [Color(hex: a.dark.0), Color(hex: a.dark.1)], startPoint: .top, endPoint: .bottom)
        } else if let kind = FullscreenBackground(rawValue: id), kind.animated {
            AnimatedBackground(kind: kind)
        } else {
            AppBackdrop()
        }
    }
}

extension View {
    /// Duvar kâğıdı seçiliyse zemin o olur ve görünüm koyu temaya geçer; boşsa temanın zemini
    @ViewBuilder func alarmWall(_ id: String?) -> some View {
        if let id, !id.isEmpty {
            self.scrollContentBackground(.hidden)
                .background(AlarmWall(id: id).ignoresSafeArea())
                .environment(\.colorScheme, .dark)
        } else {
            self.themedBackground()
        }
    }
}

/// Seçici: kategori, 9:16 kartlar; karta dokununca tam ekran önizleme ve "Seç"
struct AlarmWallPicker: View {
    @Binding var item: AlarmItem
    @Environment(\.dismiss) private var dismiss
    @State private var category = "simple"
    @State private var preview: String?

    private var time: String { String(format: "%02d:%02d", item.hour, item.minute) }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    Picker(L("wall.title"), selection: $category) {
                        ForEach(AlarmWalls.categories, id: \.self) { Text(L("wall.cat.\($0)")).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 3), spacing: 12) {
                        ForEach(category == "simple" ? AlarmWalls.simple : AlarmWalls.animated, id: \.self) { id in
                            card(id)
                        }
                    }
                    Text(L("wall.iosNote")).font(.footnote).foregroundStyle(Theme.muted)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(16)
            }
            .themedBackground()
            .tint(Theme.accent)
            .navigationTitle(L("wall.title"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L("wall.none")) { item.wall = ""; dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) { Button(L("common.done")) { dismiss() } }
            }
            .fullScreenCover(item: Binding(get: { preview.map(WallID.init) }, set: { preview = $0?.id })) { w in
                previewView(w.id)
            }
        }
        .onAppear { if AlarmWalls.animated.contains(item.wall) { category = "animated" } }
    }

    private struct WallID: Identifiable { let id: String }

    private func card(_ id: String) -> some View {
        let on = id == item.wall
        return Button { preview = id } label: {
            VStack(spacing: 6) {
                ZStack(alignment: .topTrailing) {
                    AlarmWall(id: id)
                        .overlay {
                            Text(time).font(.title3.bold()).monospacedDigit().foregroundStyle(.white)
                                .environment(\.layoutDirection, .leftToRight)
                        }
                    if on {
                        Image(systemName: "checkmark.circle.fill").foregroundStyle(.white).padding(5)
                    }
                }
                .aspectRatio(9.0 / 16.0, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(on ? Theme.accent : Theme.border, lineWidth: on ? 2.5 : 1))
                Text(AlarmWalls.title(id)).font(.caption).foregroundStyle(Theme.text).lineLimit(1)
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(AlarmWalls.title(id))
        .accessibilityAddTraits(on ? .isSelected : [])
    }

    private func previewView(_ id: String) -> some View {
        ZStack {
            AlarmWall(id: id).ignoresSafeArea()
            VStack(spacing: 8) {
                HStack {
                    Spacer()
                    Button { preview = nil } label: {
                        Image(systemName: "xmark.circle.fill").font(.title).foregroundStyle(.white.opacity(0.85))
                    }
                    .accessibilityLabel(L("common.close"))
                }
                Spacer()
                Text(Fmt.longDate(Date())).font(.title3).foregroundStyle(.white.opacity(0.85))
                Text(time).font(.system(size: 88, weight: .bold, design: .rounded)).monospacedDigit().foregroundStyle(.white)
                    .environment(\.layoutDirection, .leftToRight)
                Spacer()
                Spacer()
                Button {
                    item.wall = id
                    preview = nil
                } label: {
                    Text(L("wall.choose")).font(.headline).frame(maxWidth: .infinity).padding(.vertical, 14)
                }
                .buttonStyle(.borderedProminent)
                .tint(Theme.accent)
            }
            .padding(24)
        }
    }
}

/// Alarm düzenleyicisindeki satır
struct AlarmWallSection: View {
    @Binding var item: AlarmItem
    @State private var picking = false

    var body: some View {
        Section {
            Button { picking = true } label: {
                LabeledContent(L("wall.title")) {
                    HStack(spacing: 8) {
                        if !item.wall.isEmpty {
                            AlarmWall(id: item.wall).frame(width: 18, height: 30).clipShape(RoundedRectangle(cornerRadius: 4))
                        }
                        Text(AlarmWalls.title(item.wall)).lineLimit(1)
                    }
                }
            }
            .foregroundStyle(Theme.text)
        } footer: {
            Text(L("wall.iosNote"))
        }
        .sheet(isPresented: $picking) { AlarmWallPicker(item: $item) }
    }
}
