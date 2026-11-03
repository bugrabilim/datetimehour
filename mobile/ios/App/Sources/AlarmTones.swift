import SwiftUI
import AVFoundation

/*
 * Alarm ses kitaplığı (3 Kasım 2026, 2. analiz #3; sahibin ekran görüntüleri): uygulamayla gelen, telifsiz (sentez) 10
 * ses, üç kategoride (yumuşak, klasik, güçlü). Dosyalar App/Resources/tone-<ad>.caf (üretici
 * mobile/shared/sounds/gen-tones.mjs). Seçilen ses, müzikle alarm (AlarmMusic) yolundan Library/Sounds'a kesit olarak
 * yazılır: AlarmKit `.named`, kademeli ses (yükselen kopya) ve erteleme olduğu gibi çalışır. Android'deki
 * AlarmTones.kt ile aynı adlar ve sıra.
 */
enum AlarmTones {
    struct Tone: Identifiable, Hashable { let id: String; let category: String }

    static let categories = ["soft", "classic", "loud"]
    static let all: [Tone] = [
        .init(id: "chime", category: "soft"), .init(id: "marimba", category: "soft"), .init(id: "harp", category: "soft"),
        .init(id: "birds", category: "soft"), .init(id: "kalimba", category: "soft"),
        .init(id: "bell", category: "classic"), .init(id: "digital", category: "classic"), .init(id: "buzzer", category: "classic"),
        .init(id: "siren", category: "loud"), .init(id: "pulse", category: "loud"),
    ]

    /// AlarmItem.music anahtarı (aynı ses birden çok alarmda paylaşılır)
    static func key(_ id: String) -> String { "tone-\(id)" }
    static func id(fromKey key: String) -> String? { key.hasPrefix("tone-") ? String(key.dropFirst(5)) : nil }
    static func url(_ id: String) -> URL? { Bundle.main.url(forResource: "tone-\(id)", withExtension: "caf") }
}

/// Kitaplık sayfası: kategori seçici, ▶ ile dinleme, satıra dokununca seçim
struct AlarmToneLibrary: View {
    @Binding var item: AlarmItem
    @Environment(\.dismiss) private var dismiss
    @State private var category = "soft"
    @State private var playing: String?
    @State private var player: AVAudioPlayer?
    @State private var working = false

    private var selected: String? { AlarmTones.id(fromKey: item.music) }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Picker(L("tone.library"), selection: $category) {
                        ForEach(AlarmTones.categories, id: \.self) { Text(L("tone.cat.\($0)")).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    .listRowBackground(Color.clear)
                }
                Section {
                    ForEach(AlarmTones.all.filter { $0.category == category }) { tone in
                        HStack(spacing: 12) {
                            Button { toggle(tone.id) } label: {
                                Image(systemName: playing == tone.id ? "stop.circle.fill" : "play.circle.fill")
                                    .font(.title2)
                                    .foregroundStyle(playing == tone.id ? Color.teal : Theme.accent)
                                    .symbolEffect(.variableColor.iterative, isActive: playing == tone.id)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(L(playing == tone.id ? "tone.stop" : "tone.play"))
                            Button { pick(tone.id) } label: {
                                HStack {
                                    Text(L("tone.\(tone.id)")).foregroundStyle(Theme.text)
                                    Spacer()
                                    if selected == tone.id { Image(systemName: "checkmark").foregroundStyle(Theme.accent) }
                                }
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                            .accessibilityAddTraits(selected == tone.id ? .isSelected : [])
                        }
                    }
                } footer: {
                    Text(L("tone.note"))
                }
            }
            .themedBackground()
            .tint(Theme.accent)
            .navigationTitle(L("tone.library"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button(L("common.done")) { dismiss() } }
            }
            .disabled(working)
            .overlay { if working { ProgressView() } }
        }
        .onDisappear { player?.stop() }
    }

    private func toggle(_ id: String) {
        player?.stop()
        if playing == id { playing = nil; return }
        guard let url = AlarmTones.url(id), let p = try? AVAudioPlayer(contentsOf: url) else { return }
        try? AVAudioSession.sharedInstance().setCategory(.playback, options: [.mixWithOthers])
        p.play()
        player = p
        playing = id
    }

    private func pick(_ id: String) {
        guard let url = AlarmTones.url(id) else { return }
        player?.stop()
        playing = nil
        working = true
        Task {
            // Kesit ve kademeli kopyası Library/Sounds'a (AlarmMusic); alarm kaydedilince kullanılmayanlar silinir
            if (try? await AlarmMusic.make(from: url, start: 0, key: AlarmTones.key(id))) != nil {
                item.music = AlarmTones.key(id)
                item.musicName = L("tone.\(id)")
            }
            working = false
        }
    }
}
