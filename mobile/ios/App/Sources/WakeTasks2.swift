import AVFoundation
import Speech
import SwiftUI
import Vision
import SaatCore

/// Nesne görevi (2. analiz #36): kamerayla çekilen fotoğraf Vision'ın görüntü sınıflandırmasıyla (VNClassifyImageRequest)
/// telefonda tanınır; hedef `code` alanında "obj:<kimlik>", yoksa her seferinde rastgele. Fotoğraf saklanmaz.
enum ObjectTarget {
    static let prefix = "obj:"

    static func of(_ code: String) -> String {
        let id = String(code.dropFirst(prefix.count))
        if code.hasPrefix(prefix), WakeTask.objectWords[id] != nil { return id }
        return WakeTask.objectIds.randomElement() ?? "cup"
    }

    /// Görüntünün etiketleri (İngilizce tanımlayıcı, güven)
    static func labels(_ image: UIImage) async -> [(String, Float)] {
        guard let cg = image.cgImage else { return [] }
        return await Task.detached(priority: .userInitiated) { () -> [(String, Float)] in
            let request = VNClassifyImageRequest()
            let handler = VNImageRequestHandler(cgImage: cg, options: [:])
            guard (try? handler.perform([request])) != nil else { return [] }
            let results = request.results ?? []
            return results.filter { $0.confidence >= 0.05 }.map { ($0.identifier, $0.confidence) }
        }.value
    }
}

struct ObjectTaskView: View {
    let code: String
    let onSolved: () -> Void
    @State private var target = ""
    @State private var shooting = false
    @State private var busy = false
    @State private var wrong = false

    var body: some View {
        VStack(spacing: 14) {
            Text(T("wake.object", L("wake.object." + target))).font(.headline).foregroundStyle(Theme.text)
                .multilineTextAlignment(.center)
            if CameraShot.available {
                RoundButton(title: L("wake.photo.take"), prominent: true) { wrong = false; shooting = true }
                    .disabled(busy)
            } else {
                Text(L("wake.noCamera")).font(.subheadline).foregroundStyle(Theme.muted).multilineTextAlignment(.center)
            }
            if busy { ProgressView() }
            if wrong {
                Text(L("wake.object.wrong")).font(.subheadline).foregroundStyle(Theme.err).multilineTextAlignment(.center)
            }
            Text(L("wake.object.note")).font(.footnote).foregroundStyle(Theme.muted).multilineTextAlignment(.center)
        }
        .onAppear { if target.isEmpty { target = ObjectTarget.of(code) } }
        .fullScreenCover(isPresented: $shooting) {
            CameraShot { img in
                shooting = false
                guard let img else { return }
                busy = true
                Task {
                    let labels = await ObjectTarget.labels(img)
                    busy = false
                    if WakeTask.objectMatch(target, labels) { onSolved() } else { wrong = true }
                }
            }
            .ignoresSafeArea()
        }
    }
}

/// Sesli okuma (#37): günün sözlerinden biri; yalnız cihaz içi tanıma (requiresOnDeviceRecognition). Cihaz içi tanıma
/// yoksa ya da izin verilmezse [onUnavailable] (yazma görevi). Ses kaydedilmez, hiçbir yere gönderilmez.
@MainActor
final class OnDeviceListener: ObservableObject {
    @Published var heard = ""
    @Published var listening = false
    private let engine = AVAudioEngine()
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var task: SFSpeechRecognitionTask?
    private static let regions = ["tr": "tr-TR", "en": "en-US", "de": "de-DE", "az": "az-AZ", "ar": "ar-SA", "id": "id-ID",
                                  "ur": "ur-PK", "fa": "fa-IR", "fr": "fr-FR", "ru": "ru-RU", "ja": "ja-JP", "ko": "ko-KR",
                                  "zh": "zh-CN", "fil": "fil-PH", "th": "th-TH",
                                  "es": "es-ES", "pt": "pt-BR", "hi": "hi-IN", "bn": "bn-BD"]
    private var recognizer: SFSpeechRecognizer? {
        SFSpeechRecognizer(locale: Locale(identifier: Self.regions[AppLocale.code] ?? AppLocale.code))
    }

    var supported: Bool { recognizer?.supportsOnDeviceRecognition == true }

    func start(phrase: String, onMatch: @escaping () -> Void, onFail: @escaping () -> Void) {
        SFSpeechRecognizer.requestAuthorization { status in
            Task { @MainActor in
                guard status == .authorized else { onFail(); return }
                let ok = await AVAudioApplication.requestRecordPermission()
                guard ok else { onFail(); return }
                self.begin(phrase: phrase, onMatch: onMatch, onFail: onFail)
            }
        }
    }

    private func begin(phrase: String, onMatch: @escaping () -> Void, onFail: @escaping () -> Void) {
        guard let rec = recognizer, rec.supportsOnDeviceRecognition, rec.isAvailable else { onFail(); return }
        stop()
        let req = SFSpeechAudioBufferRecognitionRequest()
        req.requiresOnDeviceRecognition = true
        req.shouldReportPartialResults = true
        request = req
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playAndRecord, mode: .measurement, options: [.duckOthers, .defaultToSpeaker])
        try? session.setActive(true, options: .notifyOthersOnDeactivation)
        let input = engine.inputNode
        let format = input.outputFormat(forBus: 0)
        input.removeTap(onBus: 0)
        input.installTap(onBus: 0, bufferSize: 1024, format: format) { buffer, _ in req.append(buffer) }
        engine.prepare()
        guard (try? engine.start()) != nil else { onFail(); return }
        listening = true
        task = rec.recognitionTask(with: req) { [weak self] result, error in
            Task { @MainActor in
                guard let self else { return }
                if let result {
                    self.heard = result.bestTranscription.formattedString
                    let all = [result.bestTranscription] + result.transcriptions
                    if all.contains(where: { WakeTask.spoken(phrase, $0.formattedString) }) {
                        self.stop()
                        onMatch()
                        return
                    }
                    if result.isFinal { self.stop() }
                }
                if error != nil { self.stop() }
            }
        }
    }

    func stop() {
        if engine.isRunning { engine.stop() }
        engine.inputNode.removeTap(onBus: 0)
        request?.endAudio()
        task?.cancel()
        task = nil
        request = nil
        listening = false
    }
}

struct SpeakTaskView: View {
    let onSolved: () -> Void
    let onUnavailable: () -> Void
    @StateObject private var listener = OnDeviceListener()
    @State private var phrase = Sayings.random() ?? "Saat Tarih"

    var body: some View {
        VStack(spacing: 14) {
            Text(L("wake.speak")).font(.headline).foregroundStyle(Theme.text)
            Text(phrase).font(.title3.weight(.bold)).foregroundStyle(Theme.ink).multilineTextAlignment(.center)
            RoundButton(title: listener.listening ? L("wake.speak.listening") : L("wake.speak.start"), prominent: true) {
                listener.start(phrase: phrase, onMatch: onSolved, onFail: onUnavailable)
            }
            .disabled(listener.listening)
            if !listener.heard.isEmpty {
                Text(T("wake.speak.heard", listener.heard)).font(.subheadline).foregroundStyle(Theme.muted)
                    .multilineTextAlignment(.center)
            }
            Text(L("wake.speak.note")).font(.footnote).foregroundStyle(Theme.muted).multilineTextAlignment(.center)
        }
        .onAppear { if !listener.supported { onUnavailable() } }
        .onDisappear { listener.stop() }
    }
}
