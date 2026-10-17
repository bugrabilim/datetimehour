import SwiftUI
import EventKit
import EventKitUI
import SaatCore

/// Telefondaki takvimler (iCloud, Google, Outlook… telefona ekli hepsi). Okuma için tam erişim izni gerekir;
/// etkinlik eklemek (sistem düzenleyicisi) iOS 17'de izinsiz de çalışır. Veriler telefonda kalır.
@MainActor @Observable
final class DeviceCalendar {
    static let shared = DeviceCalendar()

    let store = EKEventStore()
    private(set) var authorized = EKEventStore.authorizationStatus(for: .event) == .fullAccess
    /// Takvim değişince artar (görünümler yeniden okur)
    private(set) var version = 0
    private var observer: NSObjectProtocol?

    var canAsk: Bool { EKEventStore.authorizationStatus(for: .event) == .notDetermined }

    private init() {
        observer = NotificationCenter.default.addObserver(forName: .EKEventStoreChanged, object: store, queue: .main) { [weak self] _ in
            MainActor.assumeIsolated { self?.reload() }
        }
    }

    func requestAccess() async {
        authorized = (try? await store.requestFullAccessToEvents()) ?? false
        reload()
    }

    func reload() {
        authorized = EKEventStore.authorizationStatus(for: .event) == .fullAccess
        if authorized { store.refreshSourcesIfNecessary() }
        version += 1
    }

    /// Ayın etkinlikleri, gün gün (birden çok güne yayılanlar her günde)
    func events(month: CalendarDay) -> [Int: [EKEvent]] {
        _ = version
        guard authorized else { return [:] }
        let first = month.firstOfMonth
        let start = first.noon(in: .current).addingTimeInterval(-12 * 3600)
        let end = first.adding(months: 1).noon(in: .current).addingTimeInterval(-12 * 3600)
        let found = store.events(matching: store.predicateForEvents(withStart: start, end: end, calendars: nil))
        var out: [Int: [EKEvent]] = [:]
        for e in found {
            var d = CalendarDay(max(e.startDate, start))
            // Bitişi tam gece yarısı olan etkinlik o günü kapsamaz
            let last = CalendarDay(min(e.endDate.addingTimeInterval(-1), end.addingTimeInterval(-1)))
            while d <= last {
                out[d.dayNumber, default: []].append(e)
                d = d.adding(days: 1)
            }
        }
        for k in out.keys { out[k]?.sort { ($0.isAllDay ? 0 : 1, $0.startDate) < ($1.isAllDay ? 0 : 1, $1.startDate) } }
        return out
    }

    func remove(_ event: EKEvent) {
        try? store.remove(event, span: .thisEvent, commit: true)
        reload()
    }
}

/// Sistemin etkinlik düzenleyicisi (yeni etkinlik; takvim, saat, uyarı kullanıcıda)
struct EventEditor: UIViewControllerRepresentable {
    let configure: (EKEvent) -> Void
    let onDone: () -> Void

    func makeUIViewController(context: Context) -> EKEventEditViewController {
        let store = DeviceCalendar.shared.store
        let vc = EKEventEditViewController()
        vc.eventStore = store
        let event = EKEvent(eventStore: store)
        configure(event)
        vc.event = event
        vc.editViewDelegate = context.coordinator
        return vc
    }

    func updateUIViewController(_ vc: EKEventEditViewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onDone: onDone) }

    final class Coordinator: NSObject, EKEventEditViewDelegate {
        let onDone: () -> Void
        init(onDone: @escaping () -> Void) { self.onDone = onDone }

        func eventEditViewController(_ controller: EKEventEditViewController, didCompleteWith action: EKEventEditViewAction) {
            onDone()
        }
    }
}

/// Sistemin etkinlik ayrıntısı (düzenleme ve silme dahil)
struct EventDetail: UIViewControllerRepresentable {
    let event: EKEvent
    let onDone: () -> Void

    func makeUIViewController(context: Context) -> UINavigationController {
        let vc = EKEventViewController()
        vc.event = event
        vc.allowsEditing = true
        vc.delegate = context.coordinator
        return UINavigationController(rootViewController: vc)
    }

    func updateUIViewController(_ vc: UINavigationController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onDone: onDone) }

    final class Coordinator: NSObject, EKEventViewDelegate {
        let onDone: () -> Void
        init(onDone: @escaping () -> Void) { self.onDone = onDone }

        func eventViewController(_ controller: EKEventViewController, didCompleteWith action: EKEventViewAction) {
            onDone()
        }
    }
}

extension EKEvent: @retroactive Identifiable {
    public var id: String { (eventIdentifier ?? "") + "|" + String(startDate.timeIntervalSince1970) }
}
