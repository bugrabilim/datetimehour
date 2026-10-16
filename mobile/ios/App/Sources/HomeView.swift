import SwiftUI
import WidgetKit
import SaatCore

/// İlk ekran: büyük saat, bugünün tarihi, il seçimi, bugünün namaz vakitleri ve sıradaki vakte kalan süre.
struct HomeView: View {
    @AppStorage("province") private var provinceSlug = "istanbul"
    @AppStorage(NotificationPrefs.enabledKey) private var notifyOn = false
    /// Ekran görüntüsü için: -openNotifications ile ayar penceresi açık başlar
    @State private var showNotify = ProcessInfo.processInfo.arguments.contains("-openNotifications")

    private var province: Province { Province.find(provinceSlug) }

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let now = context.date
            ScrollView {
                VStack(spacing: 28) {
                    HStack {
                        Spacer()
                        Button { showNotify = true } label: {
                            Image(systemName: notifyOn ? "bell.fill" : "bell")
                                .font(.title3)
                                .foregroundStyle(Theme.accent)
                                .frame(width: 44, height: 44)
                        }
                        .accessibilityLabel(L("notify.heading"))
                    }
                    .padding(.bottom, -28)
                    ClockHeader(now: now)
                    PrayerPanel(now: now, province: province, schedule: PrayerSchedule(province: province), provinceSlug: $provinceSlug)
                    Text(String(localized: "brand.product"))
                        .font(.footnote)
                        .foregroundStyle(Theme.muted)
                        .padding(.top, 8)
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 32)
                .frame(maxWidth: 560)
                .frame(maxWidth: .infinity)
            }
            .background(Theme.bg.ignoresSafeArea())
        }
        .sheet(isPresented: $showNotify) { NotificationSettingsView() }
        .task {
            // CI: bildirim seslerinin örneği üretilir, ekran görüntüleriyle birlikte dışarı alınır
            guard ProcessInfo.processInfo.arguments.contains("-makeSoundSample") else { return }
            _ = await NotificationSounds.make(name: NotificationSounds.fileName(.dhuhr, lead: 0),
                                              text: NotificationSounds.spokenText(.dhuhr, lead: 0))
            _ = await NotificationSounds.make(name: NotificationSounds.fileName(.asr, lead: 10),
                                              text: NotificationSounds.spokenText(.asr, lead: 10))
        }
        .onChange(of: provinceSlug) { Task { await PrayerNotifier.reschedule() } }
    }
}

/// Saat: sola/sağa kaydırınca dijital ↔ analog (seçim hatırlanır). Saate dokununca saniye açılır/kapanır;
/// sitedeki gibi varsayılan dijitalde açık, analogda kapalı.
private struct ClockHeader: View {
    let now: Date
    @AppStorage("clockStyle") private var style = 0
    @AppStorage("secD") private var digitalSeconds = true
    @AppStorage("secA") private var analogSeconds = false

    private static let hms: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX") // saat rakamları her dilde aynı (sitedeki gibi)
        f.dateFormat = "HH:mm:ss"
        return f
    }()
    private static let hm: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "HH:mm"
        return f
    }()

    var body: some View {
        VStack(spacing: 8) {
            TabView(selection: $style) {
                Text((digitalSeconds ? Self.hms : Self.hm).string(from: now))
                    .font(.system(size: 76, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .minimumScaleFactor(0.5)
                    .lineLimit(1)
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
            Text(now.formatted(.dateTime.day().month(.wide).year().weekday(.wide)))
                .font(.title3.weight(.semibold))
                .foregroundStyle(Theme.text)
        }
    }
}

private struct PrayerPanel: View {
    let now: Date
    let province: Province
    let schedule: PrayerSchedule
    @Binding var provinceSlug: String

    var body: some View {
        let next = schedule.next(after: now)
        VStack(spacing: 16) {
            Menu {
                Button {
                    Task {
                        guard let loc = await OneShotLocation().fetch(requestPermission: true) else { return }
                        provinceSlug = Province.nearest(latitude: loc.coordinate.latitude, longitude: loc.coordinate.longitude).0.slug
                        WidgetCenter.shared.reloadAllTimelines() // "Otomatik" widget'lar yeni izinle yenilensin
                    }
                } label: {
                    Label(L("place.useLocation"), systemImage: "location")
                }
                Picker(String(localized: "prayer.province"), selection: $provinceSlug) {
                    ForEach(Province.all) { Text($0.displayName).tag($0.slug) }
                }
            } label: {
                Label(province.displayName, systemImage: "mappin.and.ellipse")
                    .font(.headline)
                    .foregroundStyle(Theme.accent)
            }

            if let next {
                VStack(spacing: 4) {
                    Text(String(format: String(localized: "prayer.nextIn"), next.prayer.localizedName))
                        .font(.subheadline)
                        .foregroundStyle(Theme.muted)
                    Text(countdown(to: next.at))
                        .font(.system(size: 40, weight: .semibold, design: .rounded))
                        .monospacedDigit()
                        .foregroundStyle(Theme.accent)
                        .environment(\.layoutDirection, .leftToRight)
                }
            }

            VStack(spacing: 0) {
                Text(String(localized: "prayer.today"))
                    .font(.headline)
                    .foregroundStyle(Theme.text)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.bottom, 8)
                ForEach(schedule.entries(on: now)) { entry in
                    let isNext = entry.at == next?.at
                    HStack {
                        Text(entry.prayer.localizedName)
                        Spacer()
                        Text(PrayerSchedule.hm.string(from: entry.at))
                            .monospacedDigit()
                            .environment(\.layoutDirection, .leftToRight)
                    }
                    .font(.body.weight(isNext ? .bold : .regular))
                    .foregroundStyle(Theme.text)
                    .padding(.vertical, 12)
                    .padding(.horizontal, 14)
                    .background(isNext ? Theme.accentSoft : .clear, in: RoundedRectangle(cornerRadius: 10))
                }
            }
        }
    }

    private func countdown(to date: Date) -> String {
        let s = max(0, Int(date.timeIntervalSince(now).rounded(.up)))
        return String(format: "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }
}

#Preview {
    HomeView()
}
