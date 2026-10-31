import AVFoundation
import MediaPlayer
import SwiftUI
import UniformTypeIdentifiers

/// Müzikle alarm (rakip analizi listesi #94). AlarmKit ve bildirimler yalnız uygulamanın kendi ses dosyasını çalabildiği
/// için seçilen şarkıdan 29,5 sn'lik bir kesit telefonda dosyaya yazılır (Library/Sounds, 22.050 Hz tek kanal PCM; iOS
/// bildirim sesinin 30 sn sınırı) ve alarm sesi olur. Kaynak: Müzik arşivi (yalnız telefonda duran, korumasız şarkılar;
/// Apple Music'ten indirilen şarkılar DRM'li olduğu için okunamaz) ya da Dosyalar. Kademeli seçiliyse ikinci bir kesit
/// ilk 20 sn'de kısıktan tam sese çıkar. Şarkı ve kesit telefonda kalır, hiçbir yere gönderilmez.
enum AlarmMusic {
    static let prefix = "alarm-music-"
    static let length = 29.5
    private static let rate = 22_050.0

    enum Failure: Error {
        case protected, noAudio, unreadable
        var text: String {
            switch self {
            case .protected: return L("alarm.music.protected")
            case .noAudio, .unreadable: return L("alarm.music.unreadable")
            }
        }
    }

    static var directory: URL {
        FileManager.default.urls(for: .libraryDirectory, in: .userDomainMask)[0].appendingPathComponent("Sounds", isDirectory: true)
    }

    static func fileName(_ key: String, rise: Bool) -> String { "\(prefix)\(key)\(rise ? "-rise" : "").caf" }

    /// Alarmın çalacağı kesit (kademeliyse yükselen hâli); dosya silinmişse nil
    static func sound(for item: AlarmItem) -> String? {
        guard !item.music.isEmpty else { return nil }
        let name = fileName(item.music, rise: item.rise)
        return FileManager.default.fileExists(atPath: directory.appendingPathComponent(name).path) ? name : nil
    }

    /// Kaynağın süresi (sn); okunamıyorsa ya da korumalıysa hata
    static func duration(of url: URL) async throws -> Double {
        let asset = AVURLAsset(url: url)
        if (try? await asset.load(.hasProtectedContent)) == true { throw Failure.protected }
        guard let track = try await asset.loadTracks(withMediaType: .audio).first else { throw Failure.noAudio }
        let range = try await track.load(.timeRange)
        return max(0, range.duration.seconds)
    }

    /// Kesiti üretir; anahtar dosya adının parçasıdır (alarm başına yeni anahtar, eskisi `remove` ile silinir)
    static func make(from url: URL, start: Double, key: String) async throws {
        let asset = AVURLAsset(url: url)
        if (try? await asset.load(.hasProtectedContent)) == true { throw Failure.protected }
        let tracks = try await asset.loadTracks(withMediaType: .audio)
        guard !tracks.isEmpty else { throw Failure.noAudio }
        let reader = try AVAssetReader(asset: asset)
        let settings: [String: Any] = [
            AVFormatIDKey: kAudioFormatLinearPCM, AVSampleRateKey: rate, AVNumberOfChannelsKey: 1,
            AVLinearPCMBitDepthKey: 16, AVLinearPCMIsFloatKey: false, AVLinearPCMIsBigEndianKey: false,
            AVLinearPCMIsNonInterleaved: false,
        ]
        let output = AVAssetReaderAudioMixOutput(audioTracks: tracks, audioSettings: settings)
        reader.timeRange = CMTimeRange(start: CMTime(seconds: max(0, start), preferredTimescale: 600),
                                       duration: CMTime(seconds: length, preferredTimescale: 600))
        guard reader.canAdd(output) else { throw Failure.unreadable }
        reader.add(output)
        guard reader.startReading() else { throw Failure.unreadable }
        var samples: [Int16] = []
        samples.reserveCapacity(Int(rate * length))
        while let buffer = output.copyNextSampleBuffer() {
            guard let block = CMSampleBufferGetDataBuffer(buffer) else { continue }
            let bytes = CMBlockBufferGetDataLength(block)
            var chunk = [Int16](repeating: 0, count: bytes / 2)
            let status = chunk.withUnsafeMutableBytes { CMBlockBufferCopyDataBytes(block, atOffset: 0, dataLength: bytes, destination: $0.baseAddress!) }
            if status == noErr { samples += chunk }
        }
        guard reader.status == .completed, samples.count > Int(rate) else { throw Failure.unreadable }
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        try write(shape(samples, rise: false), to: directory.appendingPathComponent(fileName(key, rise: false)))
        try write(shape(samples, rise: true), to: directory.appendingPathComponent(fileName(key, rise: true)))
    }

    /// Başta 0,3 sn, sonda 1,5 sn yumuşatma; kademelide ilk 20 sn −25 dB'den tam sese (kare eğri, alarm-rise.caf gibi)
    static func shape(_ input: [Int16], rise: Bool) -> [Int16] {
        let n = input.count
        let fadeIn = Int(rate * 0.3), fadeOut = Int(rate * 1.5), ramp = Int(rate * 20)
        let floor = pow(10.0, -25.0 / 20.0)
        return input.indices.map { i -> Int16 in
            var g = 1.0
            if i < fadeIn { g *= Double(i) / Double(fadeIn) }
            if i > n - fadeOut { g *= Double(n - i) / Double(fadeOut) }
            if rise && i < ramp {
                let t = Double(i) / Double(ramp)
                g *= floor + (1 - floor) * t * t
            }
            return Int16(max(-32767, min(32767, (Double(input[i]) * g).rounded())))
        }
    }

    private static func write(_ samples: [Int16], to url: URL) throws {
        try? FileManager.default.removeItem(at: url)
        guard let format = AVAudioFormat(commonFormat: .pcmFormatInt16, sampleRate: rate, channels: 1, interleaved: true),
              let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(samples.count)),
              let dst = buffer.int16ChannelData?[0] else { throw Failure.unreadable }
        buffer.frameLength = AVAudioFrameCount(samples.count)
        samples.withUnsafeBufferPointer { src in dst.update(from: src.baseAddress!, count: samples.count) }
        let file = try AVAudioFile(forWriting: url, settings: format.settings, commonFormat: .pcmFormatInt16, interleaved: true)
        try file.write(from: buffer)
    }

    static func remove(_ key: String) {
        guard !key.isEmpty else { return }
        for rise in [false, true] { try? FileManager.default.removeItem(at: directory.appendingPathComponent(fileName(key, rise: rise))) }
    }

    /// Hiçbir alarmın kullanmadığı kesitleri siler (alarm silinince ya da şarkı değişince). Son 30 dakikada üretilenlere
    /// dokunmaz: düzenleyici açıkken seçilen şarkı alarm kaydedilmeden silinmesin.
    static func cleanup(keeping items: [AlarmItem]) {
        let used = Set(items.map(\.music).filter { !$0.isEmpty })
        let fm = FileManager.default
        guard let files = try? fm.contentsOfDirectory(atPath: directory.path) else { return }
        for f in files where f.hasPrefix(prefix) {
            let key = String(f.dropFirst(prefix.count).dropLast(4)).replacingOccurrences(of: "-rise", with: "")
            let url = directory.appendingPathComponent(f)
            let made = (try? fm.attributesOfItem(atPath: url.path)[.modificationDate] as? Date) ?? .distantPast
            if !used.contains(key) && Date().timeIntervalSince(made) > 1800 { try? fm.removeItem(at: url) }
        }
    }
}

/// Alarm düzenleyicisindeki "Müzik" bölümü: seçili şarkı, Müzik arşivinden ya da Dosyalar'dan seçme, varsayılana dönme
struct AlarmMusicSection: View {
    @Binding var item: AlarmItem
    @State private var pickingLibrary = false
    @State private var pickingFile = false
    @State private var source: AlarmMusicSource?
    @State private var error: String?

    var body: some View {
        Section {
            LabeledContent(L("alarm.music")) {
                Text(item.music.isEmpty ? L("alarm.music.none") : item.musicName).lineLimit(1)
            }
            Button(L("alarm.music.library")) { pickingLibrary = true }
            Button(L("alarm.music.files")) { pickingFile = true }
            if !item.music.isEmpty {
                Button(L("alarm.music.clear"), role: .destructive) {
                    item.music = ""
                    item.musicName = ""
                }
            }
            if let error { Text(error).font(.footnote).foregroundStyle(.red) }
        } footer: {
            Text(L("alarm.music.note"))
        }
        .sheet(isPresented: $pickingLibrary) {
            MusicLibraryPicker { picked in
                pickingLibrary = false
                guard let picked else { return }
                guard let url = picked.assetURL, !picked.hasProtectedAsset else {
                    error = L("alarm.music.protected")
                    return
                }
                open(url, title: picked.title ?? L("alarm.music"), temporary: false)
            }
            .ignoresSafeArea()
        }
        .fileImporter(isPresented: $pickingFile, allowedContentTypes: [.audio]) { result in
            guard case .success(let url) = result else { return }
            // Dosyalar'dan gelen adres izin kapsamında; okumak için uygulamanın geçici klasörüne kopyalanır
            let ok = url.startAccessingSecurityScopedResource()
            defer { if ok { url.stopAccessingSecurityScopedResource() } }
            let copy = FileManager.default.temporaryDirectory.appendingPathComponent("alarm-src-\(UUID().uuidString).\(url.pathExtension)")
            do {
                try FileManager.default.copyItem(at: url, to: copy)
                open(copy, title: url.deletingPathExtension().lastPathComponent, temporary: true)
            } catch {
                self.error = L("alarm.music.unreadable")
            }
        }
        .sheet(item: $source) { src in
            AlarmMusicTrimSheet(source: src) { key in
                source = nil
                if src.temporary { try? FileManager.default.removeItem(at: src.url) }
                guard let key else { return }
                item.music = key
                item.musicName = src.title
            }
        }
    }

    private func open(_ url: URL, title: String, temporary: Bool) {
        error = nil
        Task {
            do {
                let d = try await AlarmMusic.duration(of: url)
                source = AlarmMusicSource(url: url, title: title, duration: d, temporary: temporary)
            } catch {
                self.error = (error as? AlarmMusic.Failure)?.text ?? L("alarm.music.unreadable")
                if temporary { try? FileManager.default.removeItem(at: url) }
            }
        }
    }
}

struct AlarmMusicSource: Identifiable {
    let id = UUID()
    let url: URL
    let title: String
    let duration: Double
    let temporary: Bool
}

/// Kesitin başlangıcını seçme ve dinleme; "Kaydet" kesiti üretir
private struct AlarmMusicTrimSheet: View {
    let source: AlarmMusicSource
    let done: (String?) -> Void
    @State private var start = 0.0
    @State private var player: AVPlayer?
    @State private var working = false
    @State private var error: String?

    private var maxStart: Double { max(0, source.duration - AlarmMusic.length) }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text(source.title).font(.headline)
                    if maxStart > 0 {
                        VStack(alignment: .leading) {
                            Text(T("alarm.music.start", Self.mmss(start), Self.mmss(min(source.duration, start + AlarmMusic.length))))
                                .monospacedDigit()
                            Slider(value: $start, in: 0...maxStart, step: 1)
                                .environment(\.layoutDirection, .leftToRight)
                        }
                    }
                    Button(L(player == nil ? "alarm.music.listen" : "alarm.music.stop")) { toggle() }
                    if let error { Text(error).font(.footnote).foregroundStyle(.red) }
                } footer: {
                    Text(L("alarm.music.trimNote"))
                }
            }
            .themedBackground()
            .navigationTitle(L("alarm.music"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L("alarm.cancel")) { stop(); done(nil) }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if working { ProgressView() } else { Button(L("alarm.music.save")) { save() } }
                }
            }
            .onChange(of: start) { if player != nil { stop(); toggle() } }
            .onDisappear { stop() }
        }
    }

    private func toggle() {
        if player != nil { stop(); return }
        try? AVAudioSession.sharedInstance().setCategory(.playback, options: [.duckOthers])
        let p = AVPlayer(url: source.url)
        p.seek(to: CMTime(seconds: start, preferredTimescale: 600))
        p.play()
        player = p
    }

    private func stop() {
        player?.pause()
        player = nil
    }

    private func save() {
        stop()
        working = true
        let key = String(UUID().uuidString.prefix(8)).lowercased()
        Task {
            do {
                try await AlarmMusic.make(from: source.url, start: start, key: key)
                working = false
                done(key)
            } catch {
                working = false
                self.error = (error as? AlarmMusic.Failure)?.text ?? L("alarm.music.unreadable")
            }
        }
    }

    static func mmss(_ t: Double) -> String {
        let s = Int(t.rounded())
        return String(format: "%d:%02d", s / 60, s % 60)
    }
}

/// Müzik arşivinden tek şarkı seçimi (MPMediaPickerController; izin sistemi sorar)
private struct MusicLibraryPicker: UIViewControllerRepresentable {
    let done: (MPMediaItem?) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(done: done) }

    func makeUIViewController(context: Context) -> MPMediaPickerController {
        let c = MPMediaPickerController(mediaTypes: .music)
        c.allowsPickingMultipleItems = false
        c.showsCloudItems = false
        c.prompt = L("alarm.music.prompt")
        c.delegate = context.coordinator
        return c
    }

    func updateUIViewController(_ c: MPMediaPickerController, context: Context) {}

    final class Coordinator: NSObject, MPMediaPickerControllerDelegate {
        let done: (MPMediaItem?) -> Void
        init(done: @escaping (MPMediaItem?) -> Void) { self.done = done }

        func mediaPicker(_ picker: MPMediaPickerController, didPickMediaItems collection: MPMediaItemCollection) {
            done(collection.items.first)
        }

        func mediaPickerDidCancel(_ picker: MPMediaPickerController) { done(nil) }
    }
}
