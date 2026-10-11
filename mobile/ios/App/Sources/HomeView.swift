import SwiftUI
import SaatCore

/// İlk ekran: büyük saat, bugünün tarihi, il seçimi, bugünün namaz vakitleri ve sıradaki vakte kalan süre.
struct HomeView: View {
    @AppStorage("province") private var provinceSlug = "istanbul"

    private var province: Province { Province.find(provinceSlug) }

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let now = context.date
            ScrollView {
                VStack(spacing: 28) {
                    ClockHeader(now: now)
                    PrayerPanel(now: now, schedule: PrayerSchedule(province: province), provinceSlug: $provinceSlug)
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
    }
}

private struct ClockHeader: View {
    let now: Date

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX") // saat rakamları her dilde aynı (sitedeki gibi)
        f.dateFormat = "HH:mm:ss"
        return f
    }()

    var body: some View {
        VStack(spacing: 8) {
            Text(Self.timeFormatter.string(from: now))
                .font(.system(size: 76, weight: .bold, design: .rounded))
                .monospacedDigit()
                .minimumScaleFactor(0.5)
                .lineLimit(1)
                .foregroundStyle(Theme.text)
                .environment(\.layoutDirection, .leftToRight)
            Text(now.formatted(.dateTime.day().month(.wide).year().weekday(.wide)))
                .font(.title3.weight(.semibold))
                .foregroundStyle(Theme.text)
        }
        .accessibilityElement(children: .combine)
    }
}

private struct PrayerPanel: View {
    let now: Date
    let schedule: PrayerSchedule
    @Binding var provinceSlug: String

    private static let hm: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "HH:mm"
        f.timeZone = PrayerSchedule.zone
        return f
    }()

    var body: some View {
        let next = schedule.next(after: now)
        VStack(spacing: 16) {
            Menu {
                Picker(String(localized: "prayer.province"), selection: $provinceSlug) {
                    ForEach(Province.all) { Text($0.displayName).tag($0.slug) }
                }
            } label: {
                Label(schedule.province.displayName, systemImage: "mappin.and.ellipse")
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
                        Text(Self.hm.string(from: entry.at))
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
