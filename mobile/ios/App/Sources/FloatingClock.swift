import AVFoundation
import AVKit
import SwiftUI
import UIKit

/// Yüzen saat (rakip analizi listesi #80; Android'de diğer uygulamaların üstünde pencere): resim içinde resim
/// penceresinde saat. Saat her saniye bir görüntü olarak çizilir ve AVSampleBufferDisplayLayer'a verilir; pencere
/// sistemin PiP penceresidir (sürüklenir, küçülür, kenara itilir). Arka plan ses modu (Info.plist) PiP'i de kapsar;
/// ses çalınmaz.
@MainActor
final class FloatingClock: NSObject, ObservableObject {
    static let shared = FloatingClock()

    let layer = AVSampleBufferDisplayLayer()
    @Published private(set) var active = false
    private var pip: AVPictureInPictureController?
    private var timer: Timer?
    private var observation: NSKeyValueObservation?

    static var supported: Bool { AVPictureInPictureController.isPictureInPictureSupported() }

    private static let size = CGSize(width: 480, height: 180)

    override init() {
        super.init()
        layer.videoGravity = .resizeAspect
        layer.backgroundColor = UIColor.black.cgColor
        render()
    }

    /// Önizleme görünür olunca hazırlanır; dokunuşla başlar
    func prepare() {
        guard Self.supported, pip == nil else { return }
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [.mixWithOthers])
        let source = AVPictureInPictureController.ContentSource(sampleBufferDisplayLayer: layer, playbackDelegate: self)
        let c = AVPictureInPictureController(contentSource: source)
        c.delegate = self
        c.canStartPictureInPictureAutomaticallyFromInline = false
        pip = c
        startTicking()
    }

    func toggle() {
        guard let pip else { return }
        if pip.isPictureInPictureActive {
            pip.stopPictureInPicture()
        } else {
            try? AVAudioSession.sharedInstance().setActive(true)
            pip.startPictureInPicture()
        }
    }

    private func startTicking() {
        guard timer == nil else { return }
        render()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            MainActor.assumeIsolated { FloatingClock.shared.render() }
        }
    }

    /// Saati bir kare olarak çizer (koyu zemin, açık rakamlar; saniyeli, 24/12 ayara göre)
    func render() {
        let h12 = UserDefaults.standard.bool(forKey: "h12")
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = h12 ? "h:mm:ss a" : "HH:mm:ss"
        let text = f.string(from: Date())
        let size = Self.size
        let image = UIGraphicsImageRenderer(size: size, format: { let r = UIGraphicsImageRendererFormat(); r.scale = 1; return r }()).image { ctx in
            UIColor(red: 0.06, green: 0.07, blue: 0.10, alpha: 1).setFill()
            ctx.fill(CGRect(origin: .zero, size: size))
            let font = UIFont.monospacedDigitSystemFont(ofSize: h12 ? 88 : 104, weight: .semibold)
            let attrs: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: UIColor.white]
            let s = (text as NSString).size(withAttributes: attrs)
            (text as NSString).draw(at: CGPoint(x: (size.width - s.width) / 2, y: (size.height - s.height) / 2), withAttributes: attrs)
        }
        guard let cg = image.cgImage, let buffer = Self.sampleBuffer(cg) else { return }
        if layer.status == .failed { layer.flush() }
        layer.enqueue(buffer)
    }

    private static func sampleBuffer(_ image: CGImage) -> CMSampleBuffer? {
        let w = image.width, h = image.height
        var pb: CVPixelBuffer?
        let attrs: [String: Any] = [kCVPixelBufferIOSurfacePropertiesKey as String: [String: Any](),
                                    kCVPixelBufferCGImageCompatibilityKey as String: true,
                                    kCVPixelBufferCGBitmapContextCompatibilityKey as String: true]
        guard CVPixelBufferCreate(kCFAllocatorDefault, w, h, kCVPixelFormatType_32BGRA, attrs as CFDictionary, &pb) == kCVReturnSuccess, let pb else { return nil }
        CVPixelBufferLockBaseAddress(pb, [])
        if let ctx = CGContext(data: CVPixelBufferGetBaseAddress(pb), width: w, height: h, bitsPerComponent: 8,
                               bytesPerRow: CVPixelBufferGetBytesPerRow(pb), space: CGColorSpaceCreateDeviceRGB(),
                               bitmapInfo: CGImageAlphaInfo.premultipliedFirst.rawValue | CGBitmapInfo.byteOrder32Little.rawValue) {
            ctx.draw(image, in: CGRect(x: 0, y: 0, width: w, height: h))
        }
        CVPixelBufferUnlockBaseAddress(pb, [])
        var format: CMVideoFormatDescription?
        guard CMVideoFormatDescriptionCreateForImageBuffer(allocator: kCFAllocatorDefault, imageBuffer: pb, formatDescriptionOut: &format) == noErr,
              let format else { return nil }
        var timing = CMSampleTimingInfo(duration: .invalid, presentationTimeStamp: CMClockGetTime(CMClockGetHostTimeClock()), decodeTimeStamp: .invalid)
        var sample: CMSampleBuffer?
        guard CMSampleBufferCreateReadyWithImageBuffer(allocator: kCFAllocatorDefault, imageBuffer: pb, formatDescription: format,
                                                       sampleTiming: &timing, sampleBufferOut: &sample) == noErr, let sample else { return nil }
        if let atts = CMSampleBufferGetSampleAttachmentsArray(sample, createIfNecessary: true), CFArrayGetCount(atts) > 0 {
            let dict = unsafeBitCast(CFArrayGetValueAtIndex(atts, 0), to: CFMutableDictionary.self)
            CFDictionarySetValue(dict, Unmanaged.passUnretained(kCMSampleAttachmentKey_DisplayImmediately).toOpaque(),
                                 Unmanaged.passUnretained(kCFBooleanTrue).toOpaque())
        }
        return sample
    }
}

extension FloatingClock: AVPictureInPictureControllerDelegate {
    nonisolated func pictureInPictureControllerDidStartPictureInPicture(_ c: AVPictureInPictureController) {
        Task { @MainActor in FloatingClock.shared.active = true }
    }

    nonisolated func pictureInPictureControllerDidStopPictureInPicture(_ c: AVPictureInPictureController) {
        Task { @MainActor in FloatingClock.shared.active = false }
    }
}

extension FloatingClock: AVPictureInPictureSampleBufferPlaybackDelegate {
    nonisolated func pictureInPictureController(_ c: AVPictureInPictureController, setPlaying playing: Bool) {}

    /// Canlı akış: süresiz
    nonisolated func pictureInPictureControllerTimeRangeForPlayback(_ c: AVPictureInPictureController) -> CMTimeRange {
        CMTimeRange(start: .negativeInfinity, duration: .positiveInfinity)
    }

    nonisolated func pictureInPictureControllerIsPlaybackPaused(_ c: AVPictureInPictureController) -> Bool { false }

    nonisolated func pictureInPictureController(_ c: AVPictureInPictureController, didTransitionToRenderSize newRenderSize: CMVideoDimensions) {}

    nonisolated func pictureInPictureController(_ c: AVPictureInPictureController, skipByInterval skipInterval: CMTime,
                                                completion completionHandler: @escaping () -> Void) {
        completionHandler()
    }
}

/// Önizleme (PiP katmanı görünümde olmalı) ve aç/kapat düğmesi; Saat görünümü sayfasında
struct FloatingClockSection: View {
    @ObservedObject private var clock = FloatingClock.shared

    var body: some View {
        Section {
            if FloatingClock.supported {
                LayerHost(layer: clock.layer)
                    .aspectRatio(480.0 / 180.0, contentMode: .fit)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .accessibilityHidden(true)
                Button(L(clock.active ? "float.stop" : "float.start")) { clock.toggle() }
            } else {
                Text(L("float.unsupported")).foregroundStyle(Theme.muted)
            }
        } header: {
            Text(L("float.title"))
        } footer: {
            Text(L("float.noteIos"))
        }
        .onAppear { clock.prepare() }
    }
}

/// AVSampleBufferDisplayLayer'ı taşıyan görünüm
private struct LayerHost: UIViewRepresentable {
    let layer: CALayer

    final class HostView: UIView {
        var hosted: CALayer?
        override func layoutSubviews() {
            super.layoutSubviews()
            hosted?.frame = bounds
        }
    }

    func makeUIView(context: Context) -> HostView {
        let v = HostView()
        v.hosted = layer
        layer.removeFromSuperlayer()
        v.layer.addSublayer(layer)
        return v
    }

    func updateUIView(_ v: HostView, context: Context) { v.setNeedsLayout() }
}
