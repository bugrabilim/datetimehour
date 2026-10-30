import SwiftUI
import HealthKit

/// Apple Sağlık (rakip analizi listesi #84): biten aralık antrenmanı ya da rutin, kullanıcı açarsa (`health.save`, varsayılan kapalı)
/// Sağlık'a antrenman olarak yazılır (tür, başlangıç, süre). Yalnız yazma izni istenir; uygulama Sağlık verisi okumaz.
/// Aynı antrenman bir kez yazılır (`hiit.healthSaved` = antrenmanın başlangıcı).
@MainActor
enum HealthLog {
    static let key = "health.save"
    private static let store = HKHealthStore()
    static var available: Bool { HKHealthStore.isHealthDataAvailable() }
    static var enabled: Bool { UserDefaults.standard.bool(forKey: key) }

    /// İzin ister (yalnız antrenman yazma); sonuç: izin verildi mi
    static func requestAccess() async -> Bool {
        guard available else { return false }
        do {
            try await store.requestAuthorization(toShare: [HKObjectType.workoutType()], read: [])
        } catch {
            return false
        }
        return store.authorizationStatus(for: HKObjectType.workoutType()) == .sharingAuthorized
    }

    static var denied: Bool { available && store.authorizationStatus(for: HKObjectType.workoutType()) == .sharingDenied }

    /// Antrenman bittiyse (ve daha önce yazılmadıysa) Sağlık'a yazar; uygulama öne gelince, aralık ekranından ve HiitRunner'dan
    static func recordHiitIfDone() {
        let d = UserDefaults.standard
        let start = d.double(forKey: "hiit.start")
        guard start > 0, d.double(forKey: "hiit.paused") == 0 else { return }
        let cfg = HiitConfig.load()
        let elapsed = Int((Date().timeIntervalSince1970 * 1000 - start) / 1000)
        guard cfg.phase(elapsed).kind == "done", d.double(forKey: "hiit.healthSaved") != start else { return }
        d.set(start, forKey: "hiit.healthSaved")
        guard enabled, available, store.authorizationStatus(for: HKObjectType.workoutType()) == .sharingAuthorized else { return }
        let begin = Date(timeIntervalSince1970: start / 1000)
        let end = begin.addingTimeInterval(Double(cfg.totalSeconds))
        let title = cfg.isRoutine ? cfg.name : L("hiit.tab")
        Task {
            let config = HKWorkoutConfiguration()
            config.activityType = .highIntensityIntervalTraining
            let builder = HKWorkoutBuilder(healthStore: store, configuration: config, device: .local())
            do {
                try await builder.beginCollection(at: begin)
                try await builder.addMetadata([HKMetadataKeyWorkoutBrandName: title])
                try await builder.endCollection(at: end)
                _ = try await builder.finishWorkout()
            } catch {
                // Yazılamazsa (izin geri alındı vb.) sessizce geçilir
            }
        }
    }
}

/// Aralık ekranındaki anahtar: açınca izin istenir
struct HealthToggle: View {
    @AppStorage(HealthLog.key) private var on = false
    @State private var denied = false

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Toggle(L("health.saveIos"), isOn: Binding(get: { on }, set: { v in
                if !v { on = false; return }
                Task {
                    let ok = await HealthLog.requestAccess()
                    on = ok
                    denied = !ok
                }
            }))
            .disabled(!HealthLog.available)
            Text(L(HealthLog.available ? (denied ? "health.denied" : "health.noteIos") : "health.unavailable"))
                .font(.footnote)
                .foregroundStyle(Theme.muted)
        }
        .onAppear { denied = on && HealthLog.denied }
    }
}
