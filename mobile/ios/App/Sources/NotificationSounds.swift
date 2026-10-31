import AVFoundation
import AudioToolbox
import Foundation
import UserNotifications
import SaatCore

/// Bildirim sesleri telefonda üretilir: kısa bir zil ve isteğe bağlı sesli duyuru ("Öğle vakti geldi.").
/// Duyuru iPhone'un kendi konuşma sesiyle (AVSpeechSynthesizer) yazılır; internet gerekmez.
/// Dosyalar Library/Sounds içine yazılır; iOS bildirim sesini orada arar (en çok 30 sn).
/// "Ezan" seçeneği (30 Ekim 2026, sahibin kararı): vaktinde uygulamayla gelen 5 sn'lik ezan kesiti (ezan.caf;
/// Wikimedia Commons, CC BY-SA 4.0, atıf ayarda; kaynak mobile/shared/ezan/README.md). Vakitten önceki
/// bildirimlerde ve Güneş'te sesli duyuru çalar.
enum SoundMode: String, CaseIterable {
    case voice, chime, adhan, system

    static let key = "notify.sound"
    static var current: SoundMode { SoundMode(rawValue: UserDefaults.standard.string(forKey: key) ?? "") ?? .voice }

    var label: String { L("notify.sound." + rawValue) }
}

/// Ezan sesi (rakip analizi listesi #92): uygulamayla gelen açık lisanslı kayıtların ilk cümlesi; kaynaklar ve kesim
/// mobile/shared/ezan/README.md. Seçim `notify.adhanVoice` (Android'le aynı kimlikler); atıf ayarda seçili kaydın altında.
enum AdhanVoice: String, CaseIterable {
    case azeez, medina

    static let key = "notify.adhanVoice"
    static var current: AdhanVoice { AdhanVoice(rawValue: UserDefaults.standard.string(forKey: key) ?? "") ?? .azeez }

    /// Uygulama paketindeki dosya (iOS bildirim sesi en çok 30 sn)
    var resource: String { self == .azeez ? "ezan" : "ezan-\(rawValue)" }
    var file: String { resource + ".caf" }
    var label: String { L("notify.adhan." + rawValue) }
    var credit: String { L(self == .azeez ? "notify.sound.adhanCredit" : "notify.adhan.credit." + rawValue) }
}

@MainActor
enum NotificationSounds {
    /// Dosya adı sürümü: ses üretimi değişirse artırılır, eski dosyalar silinir
    private static let version = "s2"
    private static var player: AVAudioPlayer?

    static var directory: URL {
        FileManager.default.urls(for: .libraryDirectory, in: .userDomainMask)[0].appendingPathComponent("Sounds", isDirectory: true)
    }

    /// Uygulamanın görünen dili ve ona uygun konuşma sesi (Azericede Türkçe ses)
    private static var language: String { Bundle.main.preferredLocalizations.first ?? "tr" }
    private static var voiceCode: String {
        ["tr": "tr-TR", "en": "en-US", "de": "de-DE", "az": "tr-TR", "ar": "ar-SA"][language] ?? "tr-TR"
    }

    /// Kurulu en iyi ses: önce "Premium", sonra "Gelişmiş", yoksa standart ses.
    /// Kullanıcı daha iyi bir ses indirirse dosya adı değişir, sesler yeniden üretilir.
    static var bestVoice: AVSpeechSynthesisVoice? {
        let voices = AVSpeechSynthesisVoice.speechVoices().filter {
            $0.language == voiceCode && !$0.voiceTraits.contains(.isNoveltyVoice) && !$0.voiceTraits.contains(.isPersonalVoice)
        }
        return voices.max { $0.quality.rawValue < $1.quality.rawValue } ?? AVSpeechSynthesisVoice(language: voiceCode)
    }

    /// Standart (robotik) ses mi kullanılıyor; ayar penceresinde daha iyi ses indirme ipucu gösterilir
    static var hasOnlyBasicVoice: Bool { (bestVoice?.quality ?? .default) == .default }

    static func fileName(_ prayer: Prayer, lead: Int) -> String {
        let voiceKey = String((bestVoice?.identifier ?? "none").filter { $0.isLetter || $0.isNumber }.suffix(40))
        return "\(version)-\(language)-\(voiceKey)-\(prayer.rawValue)-\(lead).caf"
    }

    static func spokenText(_ prayer: Prayer, lead: Int) -> String {
        lead > 0
            ? String(format: L("notify.spoken.before"), prayer.localizedName, String(lead))
            : String(format: L("notify.spoken.now"), prayer.localizedName)
    }

    /// Ayara göre bildirim sesi; üretilemezse telefonun sesi
    /// Ezan yalnız vaktinde ve namaz vakitlerinde (Güneş bir namaz vakti değil); diğerlerinde sesli duyuru
    static func effective(_ mode: SoundMode, prayer: Prayer, lead: Int) -> SoundMode {
        mode == .adhan && (lead > 0 || prayer == .sunrise) ? .voice : mode
    }

    static func sound(for prayer: Prayer, lead: Int, mode: SoundMode = .current) async -> UNNotificationSound {
        switch effective(mode, prayer: prayer, lead: lead) {
        case .adhan:
            return UNNotificationSound(named: UNNotificationSoundName(AdhanVoice.current.file))
        case .system:
            return .default
        case .chime:
            let name = "\(version)-chime.caf"
            return await make(name: name, text: nil) ? UNNotificationSound(named: UNNotificationSoundName(name)) : .default
        case .voice:
            let name = fileName(prayer, lead: lead)
            return await make(name: name, text: spokenText(prayer, lead: lead))
                ? UNNotificationSound(named: UNNotificationSoundName(name)) : .default
        }
    }

    /// Ayar penceresindeki "Dinle": seçili sesin örneği (öğle vakti)
    static func preview(mode: SoundMode, lead: Int, voice: AdhanVoice = .current) async {
        let name: String
        switch effective(mode, prayer: .dhuhr, lead: lead) {
        case .adhan:
            guard let url = Bundle.main.url(forResource: voice.resource, withExtension: "caf") else { return }
            try? AVAudioSession.sharedInstance().setCategory(.playback, options: [.duckOthers])
            player = try? AVAudioPlayer(contentsOf: url)
            player?.play()
            return
        case .system:
            AudioServicesPlaySystemSound(1007) // iOS'un bildirim sesine en yakın sistem sesi
            return
        case .chime:
            name = "\(version)-chime.caf"
            guard await make(name: name, text: nil) else { return }
        case .voice:
            name = fileName(.dhuhr, lead: lead)
            guard await make(name: name, text: spokenText(.dhuhr, lead: lead)) else { return }
        }
        try? AVAudioSession.sharedInstance().setCategory(.playback, options: [.duckOthers])
        player = try? AVAudioPlayer(contentsOf: directory.appendingPathComponent(name))
        player?.play()
    }

    /// Eski sürüm ya da başka dildeki dosyaları siler
    static func removeStale(keeping names: Set<String>) {
        let fm = FileManager.default
        guard let files = try? fm.contentsOfDirectory(atPath: directory.path) else { return }
        for f in files where !names.contains(f) && !f.hasPrefix("\(version)-chime") && !f.hasPrefix(AlarmSpeech.prefix) {
            try? fm.removeItem(at: directory.appendingPathComponent(f))
        }
    }

    // MARK: Üretim

    /// Zil (+ varsa konuşma) dosyasını yazar; zaten varsa dokunmaz.
    static func make(name: String, text: String?) async -> Bool {
        let url = directory.appendingPathComponent(name)
        let fm = FileManager.default
        if fm.fileExists(atPath: url.path) { return true }
        try? fm.createDirectory(at: directory, withIntermediateDirectories: true)

        var speech: [AVAudioPCMBuffer] = []
        if let text {
            guard let voice = bestVoice else { return false }
            speech = await SpeechCollector().buffers(for: text, voice: voice)
            if speech.isEmpty { return false }
        }
        let format = speech.first?.format
            ?? AVAudioFormat(commonFormat: .pcmFormatFloat32, sampleRate: 22050, channels: 1, interleaved: false)!
        guard let chime = chime(format) else { return false }
        let settings: [String: Any] = [
            AVFormatIDKey: kAudioFormatLinearPCM, AVSampleRateKey: format.sampleRate, AVNumberOfChannelsKey: 1,
            AVLinearPCMBitDepthKey: 16, AVLinearPCMIsFloatKey: false, AVLinearPCMIsBigEndianKey: false,
        ]
        do {
            let file = try AVAudioFile(forWriting: url, settings: settings, commonFormat: format.commonFormat,
                                       interleaved: format.isInterleaved)
            try file.write(from: chime)
            for b in speech { try file.write(from: b) }
        } catch {
            try? fm.removeItem(at: url)
            return false
        }
        return true
    }

    /// İki notalı yumuşak zil (E5 → A5), sönümlü; sonunda kısa sessizlik
    private static func chime(_ format: AVAudioFormat) -> AVAudioPCMBuffer? {
        let sr = format.sampleRate
        let total = Int(sr * 1.5)
        guard format.channelCount == 1,
              let buf = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(total)) else { return nil }
        buf.frameLength = AVAudioFrameCount(total)
        let notes: [(start: Double, freq: Double)] = [(0, 659.25), (0.28, 880)]
        func sample(_ i: Int) -> Float {
            let t = Double(i) / sr
            var v = 0.0
            for n in notes where t >= n.start {
                let dt = t - n.start
                let env = min(1, dt / 0.01) * exp(-dt * 4)
                v += sin(2 * .pi * n.freq * dt) * env + 0.3 * sin(4 * .pi * n.freq * dt) * env * exp(-dt * 6)
            }
            return Float(v * 0.32)
        }
        if let f = buf.floatChannelData?[0] {
            for i in 0..<total { f[i] = sample(i) }
        } else if let s = buf.int16ChannelData?[0] {
            for i in 0..<total { s[i] = Int16(max(-1, min(1, sample(i))) * 32767) }
        } else {
            return nil
        }
        return buf
    }
}

/// AVSpeechSynthesizer.write çıktısını toplar (bitiş: boş tampon; 15 sn sınır).
private final class SpeechCollector {
    private let synth = AVSpeechSynthesizer()
    private let lock = NSLock()
    private var out: [AVAudioPCMBuffer] = []
    private var done = false

    func buffers(for text: String, voice: AVSpeechSynthesisVoice) async -> [AVAudioPCMBuffer] {
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = voice
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.9
        return await withCheckedContinuation { (cont: CheckedContinuation<[AVAudioPCMBuffer], Never>) in
            func finish() {
                lock.lock()
                defer { lock.unlock() }
                guard !done else { return }
                done = true
                cont.resume(returning: out)
            }
            synth.write(utterance) { [self] buffer in
                guard let pcm = buffer as? AVAudioPCMBuffer else { return }
                if pcm.frameLength == 0 { finish(); return }
                lock.lock()
                if !done { out.append(pcm) }
                lock.unlock()
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 15) { finish() }
        }
    }
}
