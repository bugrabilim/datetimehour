import SwiftUI

/* Kadran galerisi, önizleme ve tanıtım kartı (2. analiz #1); kadranların kendisi Dials.swift'te (widget'ta da) */

/// Kadran galerisi (Saat görünümü › Kadranlar): canlı küçük kadranlar; dokununca önizleme, renk ve "Kullan"
struct DialGalleryView: View {
    @AppStorage(DialFace.key) private var faceRaw = DialFace.classic.rawValue
    @AppStorage(DialFace.colorKey) private var colorRaw = DialColor.auto.rawValue
    @AppStorage(DialFace.seenKey) private var seen = false
    @State private var newOnOpen = !UserDefaults.standard.bool(forKey: DialFace.seenKey)
    @State private var preview: DialFace?

    var body: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 140), spacing: 14)], spacing: 16) {
                ForEach(DialFace.allCases) { f in
                    Button { preview = f } label: {
                        VStack(spacing: 6) {
                            TimelineView(.periodic(from: .now, by: 1)) { ctx in
                                AnalogClock(date: ctx.date, showSeconds: true, face: f, dialColor: f.rawValue == faceRaw ? DialColor(rawValue: colorRaw) ?? .auto : .auto)
                            }
                            .padding(6)
                            .overlay(alignment: .topTrailing) {
                                if newOnOpen && f != .classic {
                                    Text(L("dial.new")).font(.caption2.bold()).padding(.horizontal, 6).padding(.vertical, 2)
                                        .background(Theme.accent, in: Capsule()).foregroundStyle(Theme.bg)
                                }
                            }
                            HStack(spacing: 4) {
                                if f.rawValue == faceRaw { Image(systemName: "checkmark.circle.fill").foregroundStyle(Theme.accent) }
                                Text(f.title).font(.subheadline.weight(.semibold)).foregroundStyle(Theme.text).lineLimit(1)
                            }
                        }
                        .padding(8)
                        .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16))
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(f.rawValue == faceRaw ? .isSelected : [])
                }
            }
            .padding(16)
            Text(L("dial.note")).font(.footnote).foregroundStyle(Theme.muted).padding(.horizontal, 16).padding(.bottom, 16)
        }
        .themedBackground()
        .navigationTitle(L("dial.title"))
        .navigationBarTitleDisplayMode(.large)
        .onAppear { seen = true }
        .sheet(item: $preview) { f in DialPreview(face: f) }
    }
}

/// Önizleme: büyük canlı kadran, 5 renk ve "Kullan"
struct DialPreview: View {
    let face: DialFace
    @AppStorage(DialFace.key) private var faceRaw = DialFace.classic.rawValue
    @AppStorage(DialFace.colorKey) private var colorRaw = DialColor.auto.rawValue
    @Environment(\.dismiss) private var dismiss
    @State private var color: DialColor = .auto

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                TimelineView(.periodic(from: .now, by: 1)) { ctx in
                    AnalogClock(date: ctx.date, showSeconds: true, face: face, dialColor: color)
                }
                .frame(maxWidth: 340)
                .padding(.horizontal, 24)
                HStack(spacing: 16) {
                    ForEach(DialColor.allCases) { c in
                        Button { color = c } label: {
                            Circle()
                                .fill(c.color ?? face.colors(theme: .theme).hand)
                                .frame(width: 34, height: 34)
                                .overlay(Circle().stroke(Theme.text, lineWidth: c == color ? 3 : 0).padding(-4))
                        }
                        .accessibilityLabel(L("dial.color.\(c.rawValue)"))
                        .accessibilityAddTraits(c == color ? .isSelected : [])
                    }
                }
                Button {
                    faceRaw = face.rawValue
                    colorRaw = color.rawValue
                    dismiss()
                } label: {
                    Text(faceRaw == face.rawValue && colorRaw == color.rawValue ? L("dial.inUse") : L("dial.use"))
                        .font(.headline).frame(maxWidth: .infinity).padding(.vertical, 14)
                }
                .buttonStyle(.borderedProminent)
                .tint(Theme.accent)
                .padding(.horizontal, 24)
                Spacer()
            }
            .padding(.top, 20)
            .themedBackground()
            .navigationTitle(face.title)
            .navigationBarTitleDisplayMode(.large)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button(L("common.done")) { dismiss() } } }
        }
        .onAppear { if faceRaw == face.rawValue { color = DialColor(rawValue: colorRaw) ?? .auto } }
    }
}

/// Saat ekranında bir kez "Yeni kadranlar" tanıtımı (galeri açılınca ya da kapatılınca kalkar)
struct DialNewsCard: View {
    @AppStorage(DialFace.seenKey) private var seen = false
    @State private var open = false

    var body: some View {
        if !seen {
            HStack(spacing: 12) {
                TimelineView(.periodic(from: .now, by: 1)) { ctx in
                    AnalogClock(date: ctx.date, face: .chrono)
                }
                .frame(width: 54, height: 54)
                VStack(alignment: .leading, spacing: 2) {
                    Text(L("dial.newTitle")).font(.headline)
                    Text(L("dial.newText")).font(.caption).foregroundStyle(Theme.muted).lineLimit(3)
                }
                Spacer(minLength: 0)
                Button { seen = true } label: { Image(systemName: "xmark").foregroundStyle(Theme.muted) }
                    .accessibilityLabel(L("common.close"))
            }
            .padding(12)
            .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16))
            .contentShape(Rectangle())
            .onTapGesture { open = true }
            .sheet(isPresented: $open) { NavigationStack { DialGalleryView() } }
        }
    }
}
