import SwiftUI
import SaatCore

/// Saat sekmesi: büyük saat (dijital ↔ analog), tarih, hafta, sıradaki vakit ve tam ekran.
struct ClockTab: View {
    @AppStorage("keepAwake") private var keepAwake = true
    @AppStorage("province") private var provinceSlug = "istanbul"
    @State private var fullscreen = false

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let now = context.date
            let today = CalendarDay(now)
            ScrollView {
                VStack(spacing: 22) {
                    ClockHeader(now: now)
                    Text(T("clock.weekDay", today.isoWeek.week, today.dayOfYear))
                        .font(.subheadline)
                        .foregroundStyle(Theme.muted)
                    NextPrayerCard(now: now, province: Province.find(provinceSlug))
                    Button {
                        fullscreen = true
                    } label: {
                        Label(L("clock.fullscreen"), systemImage: "arrow.up.left.and.arrow.down.right")
                            .font(.headline)
                            .foregroundStyle(Theme.accent)
                    }
                    Text(L("brand.product"))
                        .font(.footnote)
                        .foregroundStyle(Theme.muted)
                        .padding(.top, 8)
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 24)
                .frame(maxWidth: 560)
                .frame(maxWidth: .infinity)
            }
            .background(Theme.bg.ignoresSafeArea())
        }
        .onAppear { UIApplication.shared.isIdleTimerDisabled = keepAwake }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
        .onChange(of: keepAwake) { UIApplication.shared.isIdleTimerDisabled = keepAwake }
        .fullScreenCover(isPresented: $fullscreen) { FullscreenClock() }
        .task {
            // CI: bildirim seslerinin örneği üretilir, ekran görüntüleriyle birlikte dışarı alınır
            guard ProcessInfo.processInfo.arguments.contains("-makeSoundSample") else { return }
            _ = await NotificationSounds.make(name: NotificationSounds.fileName(.dhuhr, lead: 0),
                                              text: NotificationSounds.spokenText(.dhuhr, lead: 0))
            _ = await NotificationSounds.make(name: NotificationSounds.fileName(.asr, lead: 10),
                                              text: NotificationSounds.spokenText(.asr, lead: 10))
        }
    }
}

/// Saat: sola/sağa kaydırınca dijital ↔ analog (seçim hatırlanır). Saate dokununca saniye açılır/kapanır;
/// sitedeki gibi varsayılan dijitalde açık, analogda kapalı.
struct ClockHeader: View {
    let now: Date
    var showDate = true
    @AppStorage("clockStyle") private var style = 0
    @AppStorage("secD") private var digitalSeconds = true
    @AppStorage("secA") private var analogSeconds = false
    @AppStorage("h12") private var h12 = false

    var body: some View {
        VStack(spacing: 8) {
            TabView(selection: $style) {
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Text(Fmt.time(now, seconds: digitalSeconds))
                        .font(.system(size: 76, weight: .bold, design: .rounded))
                        .monospacedDigit()
                        .minimumScaleFactor(0.5)
                        .lineLimit(1)
                    if h12 {
                        Text(Fmt.ampm(now)).font(.title2.weight(.semibold))
                    }
                }
                .foregroundStyle(Theme.text)
                .environment(\.layoutDirection, .leftToRight)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .contentShape(Rectangle())
                .onTapGesture { digitalSeconds.toggle() }
                .tag(0)
                AnalogClock(date: now, showSeconds: analogSeconds)
                    .padding(.vertical, 12)
                    .padding(.bottom, 24)
                    .contentShape(Rectangle())
                    .onTapGesture { analogSeconds.toggle() }
                    .tag(1)
            }
            .tabViewStyle(.page(indexDisplayMode: .always))
            .indexViewStyle(.page(backgroundDisplayMode: .always))
            .frame(height: style == 1 ? 320 : 170) // dijitalde boşluk kalmasın
            .animation(.easeInOut(duration: 0.25), value: style)
            if showDate {
                Text(Fmt.longDate(now))
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(Theme.text)
            }
        }
    }
}

/// Saat sekmesinde seçili ilin sıradaki vakti ve kalan süre
private struct NextPrayerCard: View {
    let now: Date
    let province: Province

    var body: some View {
        let schedule = PrayerSchedule(province: province)
        if let next = schedule.next(after: now) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Label(province.displayName, systemImage: "mappin.and.ellipse")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Theme.accent)
                    Text("\(next.prayer.localizedName) \(PrayerSchedule.hm.string(from: next.at))")
                        .font(.headline)
                        .foregroundStyle(Theme.text)
                }
                Spacer()
                Text(Fmt.countdown(Int(next.at.timeIntervalSince(now).rounded(.up))))
                    .font(.title3.weight(.semibold).monospacedDigit())
                    .foregroundStyle(Theme.accent)
                    .environment(\.layoutDirection, .leftToRight)
            }
            .card()
        }
    }
}

/// Tam ekran saat: yalnız saat ve tarih; ekran kapanmaz, dokununca çıkılır.
struct FullscreenClock: View {
    @Environment(\.dismiss) private var dismiss
    @AppStorage("clockStyle") private var style = 0
    @AppStorage("secD") private var digitalSeconds = true
    @AppStorage("secA") private var analogSeconds = false

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let now = context.date
            GeometryReader { g in
                VStack(spacing: 12) {
                    Spacer(minLength: 0)
                    if style == 1 {
                        AnalogClock(date: now, showSeconds: analogSeconds)
                            .frame(maxWidth: min(g.size.width, g.size.height) * 0.85)
                    } else {
                        Text(Fmt.time(now, seconds: digitalSeconds))
                            .font(.system(size: 400, weight: .bold, design: .rounded))
                            .monospacedDigit()
                            .minimumScaleFactor(0.05)
                            .lineLimit(1)
                            .environment(\.layoutDirection, .leftToRight)
                            .padding(.horizontal, 16)
                    }
                    Text(Fmt.longDate(now))
                        .font(.title2.weight(.semibold))
                    Spacer(minLength: 0)
                    Text(L("clock.tapToExit"))
                        .font(.footnote)
                        .foregroundStyle(Theme.muted)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .foregroundStyle(Theme.text)
            .background(Theme.bg.ignoresSafeArea())
        }
        .contentShape(Rectangle())
        .onTapGesture { dismiss() }
        .statusBarHidden()
        .persistentSystemOverlays(.hidden)
        .onAppear { UIApplication.shared.isIdleTimerDisabled = true }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = UserDefaults.standard.object(forKey: "keepAwake") as? Bool ?? true
        }
    }
}
