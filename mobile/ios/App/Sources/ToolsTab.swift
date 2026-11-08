import SwiftUI
import Combine
import SaatCore

/// Araçlar sekmesi: alarm, zamanlayıcı, kronometre, pomodoro, tarih hesaplama;
/// altta ayarlar ve hakkında (gizlilik ve e-posta listesi Hakkında sayfasında) (dünya saatleri ve mevsimler alt barda, 18 Ekim 2026)
struct ToolsTab: View {
    @State private var router = Router.shared

    /// Araçlar sekmesi "Tümü" sayfasıdır (Seçenek B, 2. analiz #195): kategorili ızgara, arama, sık kullanılanlar
    var body: some View {
        NavigationStack(path: $router.toolsPath) {
            AllView()
                .navigationDestination(for: ToolRoute.self) { route in route.view }
        }
    }
}

// MARK: Ortak

/// Büyük süre göstergesi
struct BigTime: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.system(size: 64, weight: .light))
            .monospacedDigit()
            .minimumScaleFactor(0.4)
            .lineLimit(1)
            .foregroundStyle(Theme.text)
            .environment(\.layoutDirection, .leftToRight)
            .frame(maxWidth: .infinity)
    }
}

struct RoundButton: View {
    let title: String
    var prominent = false
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.body.weight(.medium))
                .frame(maxWidth: .infinity, minHeight: 50)
        }
        .buttonStyle(.borderedProminent)
        .buttonBorderShape(.capsule)
        .tint(prominent ? Theme.accent : Theme.surface2)
        .foregroundStyle(prominent ? Theme.onAccent : Theme.text)
    }
}

/// Saniyelik süre: "1:02:03" ya da "02:03"
func clock(_ seconds: Int) -> String {
    let s = max(0, seconds)
    return s >= 3600 ? String(format: "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60) : String(format: "%02d:%02d", s / 60, s % 60)
}

// MARK: Kronometre

/// Ek kronometreler (rakip analizi listesi #81): her biri ayrı sayar; kayıt `stopwatches` (JSON, Unix ms; Android'le aynı)
struct StopwatchItem: Codable, Identifiable, Equatable {
    var id = UUID().uuidString
    var name = ""
    /// Çalışırken başlangıç anı (Unix ms; 0 = durdu) ve önceki toplam (ms)
    var start: Int64 = 0
    var acc: Int64 = 0

    func elapsed(_ now: Int64) -> Int64 { acc + (start > 0 ? max(0, now - start) : 0) }

    static let key = "stopwatches"
    static func load() -> [StopwatchItem] {
        UserDefaults.standard.data(forKey: key).flatMap { try? JSONDecoder().decode([StopwatchItem].self, from: $0) } ?? []
    }
    static func save(_ items: [StopwatchItem]) {
        UserDefaults.standard.set(try? JSONEncoder().encode(items), forKey: key)
    }
}

struct StopwatchView: View {
    /// Çalışırken başlangıç anı (0 = durdu); toplam ve turlar kalıcı (uygulama kapansa da süre kaybolmaz)
    @AppStorage("sw.start") private var start = 0.0
    @AppStorage("sw.acc") private var acc = 0.0
    @AppStorage("sw.laps") private var lapsRaw = ""
    @State private var extra = StopwatchItem.load()
    /// Tam ekran ve yatayda dev rakamlar (2. analiz #104)
    @State private var full = false
    @State private var copied = false

    private var laps: [Double] { lapsRaw.split(separator: ",").compactMap { Double($0) } }
    private func elapsed(_ now: Date) -> Double { acc + (start > 0 ? now.timeIntervalSinceReferenceDate - start : 0) }

    var body: some View {
        TimelineView(.animation(minimumInterval: 0.05)) { context in
            let t = elapsed(context.date)
            VStack(spacing: 20) {
                BigTime(text: format(t))
                    .padding(.top, 30)
                lapList(current: t)
            }
            .padding(.horizontal, 20)
            // Araç kalıbı (2. analiz #200): altta tur/sıfırla (soluk) ve başlat/durdur (tek dolu düğme)
            .safeAreaInset(edge: .bottom) { controls(t) }
        }
        .background(AppBackdrop().ignoresSafeArea())
        // Kilit ekranı / Dynamic Island: çalışan kronometre
        .onChange(of: start) { LiveActivities.sync() }
        .navigationTitle(L("tools.hub.cards.stopwatch.name"))
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                MoreMenu(items: menuItems)
            }
        }
        .fullScreenCover(isPresented: $full) { fullscreen }
    }

    /// "…": tam ekran ve turları kopyala (CSV paylaşımı tur listesinin başında)
    private var menuItems: [MoreItem] {
        var items = [MoreItem(title: L("clock.fullscreen"), icon: "arrow.up.left.and.arrow.down.right") { full = true }]
        if !laps.isEmpty {
            items.append(MoreItem(title: L("sw.copyLaps"), icon: "doc.on.doc") { copyLaps() })
        }
        return items
    }

    private func copyLaps() {
        let totals = laps
        let lapTimes = totals.enumerated().map { i, v in v - (i > 0 ? totals[i - 1] : 0) }
        let no = L("tools.stopwatch.lapNo")
        UIPasteboard.general.string = lapTimes.enumerated()
            .map { "\(no) \($0.offset + 1)\t\(format($0.element))\t\(format(totals[$0.offset]))" }
            .joined(separator: "\n")
        copied = true
    }

    private func controls(_ t: Double) -> some View {
        Group {
            if start > 0 {
                ToolPrimaryBar(primary: L("tools.stopwatch.stop"), secondary: L("tools.stopwatch.lap"),
                               onSecondary: { lapsRaw = (laps + [t]).map { String($0) }.joined(separator: ",") }) {
                    acc = t
                    start = 0
                }
            } else {
                ToolPrimaryBar(primary: L(t > 0 ? "tools.stopwatch.resume" : "tools.stopwatch.start"),
                               secondary: t > 0 ? L("tools.stopwatch.reset") : nil,
                               onSecondary: { acc = 0; lapsRaw = "" }) {
                    start = Date().timeIntervalSinceReferenceDate
                    BadgeStore.shared.hit(.kronometre)
                }
            }
        }
    }

    /// Siyah zemin, genişliğe sığan dev rakamlar (yatayda ekranı doldurur), tur ve dur/başlat; ekran açık kalır
    private var fullscreen: some View {
        TimelineView(.animation(minimumInterval: 0.05)) { context in
            let t = elapsed(context.date)
            VStack(spacing: 16) {
                HStack {
                    Spacer()
                    Button { full = false } label: { Image(systemName: "arrow.down.right.and.arrow.up.left").font(.title2) }
                        .accessibilityLabel(L("common.done"))
                }
                Spacer()
                Text(format(t))
                    .font(.system(size: 400, weight: .light, design: .rounded)).monospacedDigit()
                    .minimumScaleFactor(0.05).lineLimit(1)
                    .environment(\.layoutDirection, .leftToRight)
                Spacer()
                HStack(spacing: 16) {
                    if start > 0 {
                        RoundButton(title: L("tools.stopwatch.lap")) { lapsRaw = (laps + [t]).map { String($0) }.joined(separator: ",") }
                        RoundButton(title: L("tools.stopwatch.stop"), prominent: true) { acc = t; start = 0 }
                    } else {
                        RoundButton(title: L("tools.stopwatch.resume"), prominent: true) { start = Date().timeIntervalSinceReferenceDate }
                    }
                }
            }
            .padding(20)
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.black.ignoresSafeArea())
        }
        .onAppear { UIApplication.shared.isIdleTimerDisabled = true }
        .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
    }

    private func format(_ t: Double) -> String {
        let cs = Int((t * 100).rounded(.down))
        let s = cs / 100
        return clock(s) + String(format: ".%02d", cs % 100)
    }

    @ViewBuilder
    private func lapList(current: Double) -> some View {
        let totals = laps
        let lapTimes = totals.enumerated().map { i, v in v - (i > 0 ? totals[i - 1] : 0) }
        let best = lapTimes.count > 1 ? lapTimes.min() : nil
        let worst = lapTimes.count > 1 ? lapTimes.max() : nil
        List {
            if lapTimes.isEmpty {
                Text(L("tools.stopwatch.empty")).foregroundStyle(Theme.muted)
            }
            if !lapTimes.isEmpty {
                ShareLink(item: CSVFile(name: "laps-\(BadgeStore.day(Date())).csv",
                                        rows: [[L("tools.stopwatch.lapNo"), L("tools.stopwatch.lapTime"), L("tools.stopwatch.total")]]
                                            + lapTimes.enumerated().map { [String($0.offset + 1), format($0.element), format(totals[$0.offset])] }),
                          preview: SharePreview("laps.csv")) {
                    Label(L("csv.laps"), systemImage: "square.and.arrow.up")
                }
                .foregroundStyle(Theme.accent)
                // Turları metin olarak kopyalama "…" menüsünde (2. analiz #104, #200); kopyalanınca burada not
                if copied { Label(L("sw.copied"), systemImage: "checkmark").foregroundStyle(Theme.ok) }
            }
            ForEach(Array(lapTimes.enumerated()).reversed(), id: \.offset) { i, lap in
                HStack {
                    Text(verbatim: "\(L("tools.stopwatch.lapNo")) \(i + 1)")
                    if lap == best { Text(L("tools.stopwatch.best")).font(.caption).foregroundStyle(Theme.ok) }
                    if lap == worst { Text(L("tools.stopwatch.worst")).font(.caption).foregroundStyle(Theme.err) }
                    Spacer()
                    Text(format(lap)).monospacedDigit()
                    Text(format(totals[i])).monospacedDigit().foregroundStyle(Theme.muted)
                }
                .environment(\.layoutDirection, .leftToRight)
            }
            extraSection
        }
        .themedBackground()
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
    }

    /// Ek kronometreler: ad, süre, başlat/durdur, sıfırla, kaldır
    @ViewBuilder private var extraSection: some View {
        Section {
            ForEach(Array(extra.enumerated()), id: \.element.id) { i, w in
                let now = Int64(Date().timeIntervalSince1970 * 1000)
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 0) {
                        TextField(T("sw.multi.default", i + 2), text: Binding(get: { w.name }, set: { v in updateExtra(w.id) { $0.name = String(v.prefix(30)) } }))
                            .font(.footnote)
                            .foregroundStyle(Theme.muted)
                        Text(format(Double(w.elapsed(now)) / 1000))
                            .font(.system(size: 28, weight: .light, design: .rounded))
                            .monospacedDigit()
                            .environment(\.layoutDirection, .leftToRight)
                    }
                    Spacer()
                    if w.start == 0 && w.acc > 0 {
                        Button { updateExtra(w.id) { $0.acc = 0 } } label: { Image(systemName: "arrow.counterclockwise") }
                            .accessibilityLabel(L("tools.stopwatch.reset"))
                    }
                    Button {
                        updateExtra(w.id) { x in
                            let t = Int64(Date().timeIntervalSince1970 * 1000)
                            if x.start > 0 { x.acc = x.elapsed(t); x.start = 0 } else { x.start = t }
                        }
                    } label: { Image(systemName: w.start > 0 ? "pause.fill" : "play.fill") }
                        .accessibilityLabel(L(w.start > 0 ? "tools.stopwatch.stop" : "tools.stopwatch.start"))
                    Button {
                        extra.removeAll { $0.id == w.id }
                        StopwatchItem.save(extra)
                    } label: { Image(systemName: "xmark").foregroundStyle(Theme.muted) }
                        .accessibilityLabel(L("timers.remove"))
                }
                .font(.title3)
                .foregroundStyle(Theme.accent)
                .buttonStyle(.borderless)
            }
            if extra.count < 10 {
                Button {
                    extra.append(StopwatchItem(start: Int64(Date().timeIntervalSince1970 * 1000)))
                    StopwatchItem.save(extra)
                } label: { Label(L("sw.multi.add"), systemImage: "plus") }
                    .foregroundStyle(Theme.accent)
            }
            Text(L("sw.multi.note")).font(.footnote).foregroundStyle(Theme.muted)
        } header: {
            Text(L("sw.multi.heading"))
        }
    }

    private func updateExtra(_ id: String, _ change: (inout StopwatchItem) -> Void) {
        guard let i = extra.firstIndex(where: { $0.id == id }) else { return }
        change(&extra[i])
        StopwatchItem.save(extra)
    }
}

// MARK: Zamanlayıcı ve tarihe geri sayım

struct TimerView: View {
    /// Aralık antrenmanı sürüyorsa o sekme açılır
    @State private var mode = UserDefaults.standard.double(forKey: "hiit.start") > 0 ? 1 : 0
    var body: some View {
        VStack(spacing: 0) {
            Picker("", selection: $mode) {
                Text(L("tools.countdown.tabTimer")).tag(0)
                Text(L("hiit.tab")).tag(1)
                Text(L("tools.countdown.tabDate")).tag(2)
            }
            .pickerStyle(.segmented)
            .padding(16)
            switch mode {
            case 0: TimersView()
            case 1: HiitView()
            default: CountdownsView()
            }
            Spacer(minLength: 0)
        }
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(L("tools.timer"))
        .navigationBarTitleDisplayMode(.large)
    }
}

// MARK: Alarm

struct AlarmsView: View {
    @State private var items = AlarmItem.load()
    /// iPad ve geniş pencerede alarmlar iki sütunda (2. analiz #145)
    @Environment(\.horizontalSizeClass) private var hsc
    @State private var editing: AlarmItem?
    @State private var denied = false
    @State private var quickMsg = ""
    @State private var pickingEvent = false
    @AppStorage(CalendarAlarm.leadKey) private var calLead = 15

    var body: some View {
        List {
            // Uzun açıklamalar "Nasıl çalışır?" alt sayfasında (doğal tasarım madde 13)
            Section {
                HowItWorks(sections: [(L("alarm.fromCalendar"), L("alarm.fromCalendarNote"))])
            }
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets(top: 0, leading: 4, bottom: 0, trailing: 4))
            // Sıradaki alarm (2. analiz #51)
            if items.contains(where: \.enabled) {
                Section { AlarmListHeadCard(items: items) }
            }
            // Hızlı alarm (rakip analizi #17): şu andan X dakika sonrasına bir kezlik alarm
            Section(L("alarm.quick")) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach([5, 10, 15, 20, 30, 45, 60], id: \.self) { m in
                            Button(T("alarm.quickIn", m)) { quick(m) }
                                .buttonStyle(.bordered)
                                .tint(Theme.accent)
                        }
                    }
                }
                if !quickMsg.isEmpty { Text(quickMsg).font(.footnote).foregroundStyle(Theme.text) }
            }
            // Takvimdeki etkinliğe alarm ve konuma bağlı hatırlatıcı (rakip analizi listesi #93)
            Section {
                Picker(L("alarm.calLead"), selection: $calLead) {
                    ForEach(CalendarAlarm.leads, id: \.self) { m in
                        Text(m == 0 ? L("alarm.calLead.zero") : T("alarm.preMin", m)).tag(m)
                    }
                }
                Button { pickingEvent = true } label: { Label { Text(L("alarm.fromCalendar")) } icon: { DuoIcon(name: "calendar", size: 20, color: Theme.accent) } }
                NavigationLink {
                    GeoAlarmsView()
                } label: {
                    Label { Text(L("geo.title")) } icon: { DuoIcon(name: "pin", size: 20, color: Theme.accent) }
                }
            }
            if items.isEmpty {
                // Boş durum (2. analiz #96)
                EmptyState(icon: "alarm", title: L("tools.alarm.none").trimmingCharacters(in: CharacterSet(charactersIn: ".。۔")),
                           text: L("empty.alarm.text"), action: L("tools.alarm.add")) {
                    let now = Calendar.current.dateComponents([.hour, .minute], from: Date())
                    editing = AlarmItem(hour: now.hour ?? 7, minute: now.minute ?? 0)
                }
                .listRowBackground(Color.clear)
            }
            // Uyku planının kalkış alarmı ayrı başlıkta (2. analiz #52)
            if items.contains(where: \.sleepPlan) {
                Section(L("alarm.sleepSection")) {
                    ForEach($items) { $item in
                        if item.sleepPlan { alarmRow($item) }
                    }
                    .onDelete { items.remove(atOffsets: $0) }
                }
            }
            Section {
                if hsc == .regular {
                    let idx = items.indices.filter { !items[$0].sleepPlan }
                    ForEach(Array(stride(from: 0, to: idx.count, by: 2)), id: \.self) { k in
                        HStack(alignment: .top, spacing: 16) {
                            alarmRow($items[idx[k]]).frame(maxWidth: .infinity)
                            Divider()
                            if k + 1 < idx.count {
                                alarmRow($items[idx[k + 1]]).frame(maxWidth: .infinity)
                            } else {
                                Color.clear.frame(maxWidth: .infinity)
                            }
                        }
                    }
                } else {
                    ForEach($items) { $item in
                        if !item.sleepPlan { alarmRow($item) }
                    }
                    .onDelete { items.remove(atOffsets: $0) }
                }
            }
            // Tatil modu (2. analiz #44)
            VacationSection {
                Task { await AlarmCenter.sync(items); await PrayerNotifier.reschedule() }
            }
            Section {
                if denied { Text(L("alarm.denied")).foregroundStyle(Theme.err) }
                AlarmNote()
            }
        }
        .themedBackground()
        .navigationTitle(L("tools.hub.cards.alarm.name"))
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    let now = Calendar.current.dateComponents([.hour, .minute], from: Date())
                    editing = AlarmItem(hour: now.hour ?? 7, minute: now.minute ?? 0)
                } label: { Image(systemName: "plus") }
                .accessibilityLabel(L("tools.alarm.add"))
            }
        }
        .modifier(AlarmSheets(items: $items, editing: $editing, pickingEvent: $pickingEvent, quickMsg: $quickMsg, calLead: calLead, denied: $denied))
    }

    @ViewBuilder private func alarmRow(_ binding: Binding<AlarmItem>) -> some View {
        let item = binding.wrappedValue
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.timeText)
                            .font(.system(size: 40, weight: .light, design: .rounded))
                            .monospacedDigit()
                            .environment(\.layoutDirection, .leftToRight)
                        // Görev simgesi (2. analiz #51); emoji yerine iki tonlu simge (8 Kasım 2026, sahibin "çok emoji" notu)
                        HStack(spacing: 4) {
                            if let duo = Self.taskDuo(item) { DuoIcon(name: duo, size: 15, color: Theme.muted) }
                            Text(Self.taskChain(item) + item.title + " · " + item.repeatText)
                            // Sakin ön alarm
                            if item.preAlarm > 0 {
                                Text("·")
                                DuoIcon(name: "rain", size: 15, color: Theme.muted)
                            }
                        }
                        .font(.subheadline)
                        .foregroundStyle(Theme.muted)
                        // Şehir saatine ya da güneşe bağlı alarm (2. analiz #132, #153)
                        if let anchor = item.anchorText {
                            HStack(spacing: 4) {
                                DuoIcon(name: item.sun.isEmpty ? "globe" : "sun", size: 14, color: Theme.accent)
                                Text(anchor)
                            }
                            .font(.footnote)
                            .foregroundStyle(Theme.accent)
                        }
                        if item.sleepPlan { Text(L("alarm.sleepLinked")).font(.caption2).foregroundStyle(Theme.muted) }
                        if let skip = item.skipUntil, skip > Date(), let at = item.unskipped.next() {
                            Text(T("alarm.skipped", Fmt.longDate(at)))
                                .font(.caption)
                                .foregroundStyle(Theme.accent)
                        }
                        if item.enabled, item.holidayShifted(), let at = item.next() {
                            Text(T("alarm.holidayNext", Fmt.longDate(at)))
                                .font(.caption)
                                .foregroundStyle(Theme.muted)
                        }
                    }
                    Spacer()
                    Toggle(L("tools.alarm.enable"), isOn: binding.enabled).labelsHidden()
                }
                .contentShape(Rectangle())
                .onTapGesture { editing = item }
                .swipeActions(edge: .leading) {
                    if item.enabled { skipButton(binding) }
                }
                .contextMenu {
                    if item.enabled { skipButton(binding) }
                }
    }

    /// Alarm kapatma görevinin iki tonlu simgesi (görev yoksa nil; 8 Kasım 2026'ya kadar metin önekinde emoji)
    static func taskDuo(_ item: AlarmItem) -> String? {
        switch AlarmItem.effectiveTask(item.task, code: item.code) {
        case "math": "calc"
        case "shake": "waveform"
        case "steps": "gauge"
        case "code": "qr"
        case "memory": "idea"
        case "typing": "textsize"
        case "photo": "camera"
        case "object": "search"
        case "speak": "mic"
        case "random": "sparkles"
        default: nil
        }
    }

    /// Görev zincirindeki ek görev sayısı ("+2 ")
    static func taskChain(_ item: AlarmItem) -> String {
        item.chain.isEmpty || item.task == "none" ? "" : "+\(item.chain.count) "
    }

}

/// Alarm listesinin sayfaları (etkinlik seçici, düzenleyici) ve kaydetme
private struct AlarmSheets: ViewModifier {
    @Binding var items: [AlarmItem]
    @Binding var editing: AlarmItem?
    @Binding var pickingEvent: Bool
    @Binding var quickMsg: String
    let calLead: Int
    @Binding var denied: Bool

    func body(content: Content) -> some View {
        content
        .sheet(isPresented: $pickingEvent) {
            CalendarEventPicker { title, start, allDay in
                if let item = CalendarAlarm.item(title: title, start: start, allDay: allDay, lead: calLead) {
                    items.append(item)
                    items.sort { ($0.hour, $0.minute) < ($1.hour, $1.minute) }
                    quickMsg = T("alarm.quickSet", Fmt.longDate(start) + " " + Fmt.time(Calendar.current.date(bySettingHour: item.hour, minute: item.minute, second: 0, of: start) ?? start))
                    BadgeStore.shared.hit(.alarm)
                } else {
                    quickMsg = L("alarm.fromCalendarPast")
                }
            }
        }
        .sheet(item: $editing) { item in
            AlarmEditor(item: item) { saved in
                if let i = items.firstIndex(where: { $0.id == saved.id }) { items[i] = saved } else { items.append(saved) }
                BadgeStore.shared.hit(.alarm)
                items.sort { ($0.hour, $0.minute) < ($1.hour, $1.minute) }
            }
        }
        .onChange(of: items) {
            AlarmItem.save(items)
            Task {
                await AlarmCenter.sync(items)
                let allowed = await AlarmCenter.requestAccess()
                denied = !allowed && items.contains { $0.enabled }
                await PrayerNotifier.reschedule() // yaklaşan alarm bildirimleri
            }
        }
    }
}

extension AlarmsView {
    fileprivate func quick(_ minutes: Int) {
        let at = Date().addingTimeInterval(Double(minutes * 60))
        let c = Calendar.current.dateComponents([.hour, .minute], from: at)
        var item = AlarmItem(hour: c.hour ?? 0, minute: c.minute ?? 0)
        item.label = T("alarm.quickIn", minutes)
        item.preNotify = 0
        items.append(item)
        items.sort { ($0.hour, $0.minute) < ($1.hour, $1.minute) }
        quickMsg = T("alarm.quickSet", Fmt.time(at))
        BadgeStore.shared.hit(.alarm)
    }
}

extension AlarmsView {
    /// "Sonraki alarmı atla" ya da geri al
    @ViewBuilder fileprivate func skipButton(_ item: Binding<AlarmItem>) -> some View {
        let now = Date()
        if let skip = item.wrappedValue.skipUntil, skip > now {
            Button { item.wrappedValue.skipUntil = nil } label: { Label(L("alarm.unskip"), systemImage: "arrow.uturn.backward") }
        } else if let next = item.wrappedValue.unskipped.next(after: now) {
            Button { item.wrappedValue.skipUntil = next.addingTimeInterval(60) } label: { Label(L("alarm.skip"), systemImage: "forward.end") }
                .tint(Theme.accent)
        }
    }
}

private struct AlarmEditor: View {
    @Environment(\.dismiss) private var dismiss
    @State var item: AlarmItem
    let onSave: (AlarmItem) -> Void
    @State private var time = Date()
    @State private var scanning = false
    @State private var shootingRef = false
    @State private var tryTask = false
    @State private var tried = false

    /// Saat neye göre: telefon, başka şehir, gün doğumu, gün batımı (2. analiz #132, #153)
    private var base: Binding<String> {
        Binding(get: { !item.sun.isEmpty ? item.sun : (item.tz.isEmpty ? "phone" : "city") }, set: { b in
            item.sun = (b == "rise" || b == "set") ? b : ""
            if b == "city" {
                if item.tz.isEmpty { item.tz = City.all.first { $0.tz != TimeZone.current.identifier }?.tz ?? "Europe/London" }
            } else {
                item.tz = ""
            }
        })
    }

    private var cities: [City] {
        var seen = Set<String>()
        return City.all.filter { seen.insert($0.tz).inserted }.sorted { $0.displayName < $1.displayName }
    }

    /// Son erteleme saati seçenekleri: alarmdan 15 dk – 2 sa sonrası
    private var endChoices: [String] {
        let c = Calendar.current.dateComponents([.hour, .minute], from: time)
        let start = (c.hour ?? 0) * 60 + (c.minute ?? 0)
        var list = [""] + [15, 30, 45, 60, 90, 120].map { m -> String in
            let t = (start + m) % 1440
            return String(format: "%02d:%02d", t / 60, t % 60)
        }
        if !list.contains(item.snoozeEnd) { list.append(item.snoozeEnd) }
        return list
    }

    /// Önizleme için görev (2. analiz #35): alarmın görevi ve zinciri, uyanma denetimi yok
    private var previewTask: PendingWakeTask {
        PendingWakeTask(task: AlarmItem.effectiveTask(item.task, code: item.code), level: item.taskLevel, code: item.code, wakeCheck: false,
                        title: item.title, rise: false, rings: 0, repeatID: nil, wall: item.wall.isEmpty ? nil : item.wall,
                        chain: item.chain.isEmpty ? nil : item.chain)
    }

    /// Gelişmiş bölüm (2. analiz #201): kapalı gelir
    @State private var advanced = false

    var body: some View {
        NavigationStack {
            Form {
                if item.sun.isEmpty {
                    DatePicker(L("tools.alarm.timeLabel"), selection: $time, displayedComponents: .hourAndMinute)
                        .datePickerStyle(.wheel)
                        .labelsHidden()
                        .frame(maxWidth: .infinity)
                }
                TextField(L("tools.alarm.labelPlaceholder"), text: $item.label)
                Section(L("alarm.days")) {
                    // Pazartesiden pazara yedi düğme; hiçbiri seçili değilse bir kez çalar
                    HStack(spacing: 6) {
                        ForEach(1...7, id: \.self) { d in
                            let on = item.days.contains(d)
                            Button {
                                if on { item.days.removeAll { $0 == d } } else { item.days.append(d); item.days.sort() }
                            } label: {
                                Text(L("tools.alarm.dayNames.\(d - 1)"))
                                    .font(.footnote.weight(.semibold))
                                    .frame(maxWidth: .infinity, minHeight: 36)
                                    .background(on ? Theme.accent : Theme.surface2, in: Capsule())
                                    .foregroundStyle(on ? Color.white : Theme.text)
                            }
                            .buttonStyle(.plain)
                            .accessibilityAddTraits(on ? .isSelected : [])
                        }
                    }
                    .environment(\.layoutDirection, .leftToRight)
                    // Esnek tekrar: belirli tarih (bir kezlik), iki haftada bir ve bitiş tarihi (tekrarlayan)
                    if item.days.isEmpty {
                        Toggle(L("alarm.onDate"), isOn: Binding(get: { !item.date.isEmpty }, set: { on in
                            item.date = on ? CalendarDay(Date()).adding(days: 1).iso : ""
                        }))
                        if let d = CalendarDay(iso: item.date) {
                            DatePicker(L("alarm.date"), selection: Binding(get: { d.noon(in: .current) }, set: { item.date = CalendarDay($0).iso }),
                                       in: Calendar.current.startOfDay(for: Date())..., displayedComponents: .date)
                        }
                    } else {
                        Toggle(L("alarm.biweekly"), isOn: Binding(get: { item.every > 1 }, set: { on in
                            item.every = on ? 2 : 1
                            if on && item.anchor.isEmpty { let t = CalendarDay(Date()); item.anchor = t.adding(days: -t.weekday).iso }
                        }))
                        Toggle(L("alarm.untilToggle"), isOn: Binding(get: { !item.until.isEmpty }, set: { on in
                            item.until = on ? CalendarDay(Date()).adding(days: 30).iso : ""
                        }))
                        if let u = CalendarDay(iso: item.until) {
                            DatePicker(L("alarm.until"), selection: Binding(get: { u.noon(in: .current) }, set: { item.until = CalendarDay($0).iso }),
                                       in: Calendar.current.startOfDay(for: Date())..., displayedComponents: .date)
                        }
                    }
                    Text(item.repeatText).font(.footnote).foregroundStyle(Theme.muted)
                }
                // Yalnız tekrarlayan alarmda: Türkiye'deki tam gün resmî tatillerde çalmaz
                if !item.days.isEmpty {
                    Section {
                        Toggle(L("alarm.skipHolidays"), isOn: $item.skipHolidays)
                            .tint(Theme.accent)
                    } footer: {
                        Text(L("alarm.skipHolidaysNote"))
                    }
                }
                Section {
                    Picker(L("alarm.snooze"), selection: $item.snooze) {
                        ForEach(AlarmItem.snoozeChoices, id: \.self) { m in
                            Text(m == 0 ? L("alarm.snoozeOff") : T("alarm.snoozeMin", m)).tag(m)
                        }
                    }
                }
                AlarmMusicSection(item: $item)
                // Gelişmiş (2. analiz #201): kapalı gelir; saat, günler, ses ve erteleme üstte kalır
                Section {
                    Button { withAnimation { advanced.toggle() } } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(L("alarm.advanced")).font(.body.weight(.semibold)).foregroundStyle(Theme.text)
                                if !advanced { Text(L("alarm.advanced.note")).font(.caption).foregroundStyle(Theme.muted) }
                            }
                            Spacer()
                            Image(systemName: advanced ? "chevron.up" : "chevron.down").foregroundStyle(Theme.muted)
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
                if advanced {
                    // Gelişmiş bölümün uzun notları tek "Nasıl çalışır?" alt sayfasında (doğal tasarım madde 13)
                    Section {
                        HowItWorks(sections: [
                            (L("alarm.smart"), L("alarm.smart.note")),
                            (L("alarm.sunrise"), L("alarm.sunriseNote")),
                            (L("alarm.task.heading"), L(AlarmCenter.hasRealAlarms ? "alarm.task.noteIOS" : "alarm.task.oldIOS") + " "
                                + L("alarm.chainNote") + " " + L("alarm.code.hint")),
                            (L("alarm.wakeCheck"), L("alarm.wakeCheck.note")),
                        ])
                    }
                    .listRowBackground(Color.clear)
                    // Saat neye göre (2. analiz #132 şehir saati, #153 gün doğumu/batımı ± dk)
                    Section {
                        Picker(L("alarm.base"), selection: base) {
                            ForEach(["phone", "city", "rise", "set"], id: \.self) { b in Text(L("alarm.base.\(b)")).tag(b) }
                        }
                        if !item.tz.isEmpty && item.sun.isEmpty {
                            Picker(L("alarm.city"), selection: $item.tz) {
                                ForEach(cities) { c in Text(c.displayName).tag(c.tz) }
                            }
                        }
                        if !item.sun.isEmpty {
                            Picker(L("alarm.sunOffset"), selection: $item.sunOffset) {
                                ForEach(AlarmItem.sunOffsets, id: \.self) { m in Text(AlarmItem.sunOffsetText(m)).tag(m) }
                            }
                        }
                    } footer: {
                        if !item.sun.isEmpty {
                            let next = item.next(after: Date())
                            Text(T("alarm.sunNote", AppPlace.stored().name, next.map { Fmt.dayMonth($0) + " " + Fmt.time($0) } ?? "—"))
                        } else if !item.tz.isEmpty {
                            Text(L("alarm.tzNote"))
                        }
                    }
                    if item.snooze > 0 {
                        Section {
                            Picker(L("alarm.snoozeMax"), selection: $item.snoozeMax) {
                                ForEach(AlarmItem.snoozeMaxChoices, id: \.self) { n in
                                    Text(n == 0 ? L("alarm.snoozeMax.none") : T("alarm.snoozeMax.n", n)).tag(n)
                                }
                            }
                            // 2. analiz #40: her ertelemede kısalan süre, belli saatten sonra erteleme yok
                            Toggle(L("alarm.snoozeShrink"), isOn: $item.snoozeShrink).tint(Theme.accent)
                            Picker(L("alarm.snoozeEnd"), selection: $item.snoozeEnd) {
                                ForEach(endChoices, id: \.self) { e in Text(e.isEmpty ? L("alarm.snoozeEnd.none") : e).tag(e) }
                            }
                        } footer: {
                            if item.snoozeShrink { Text(L("alarm.snoozeShrinkNote")) }
                        }
                    }
                    AlarmWallSection(item: $item)
                    Section {
                        Toggle(L("alarm.rise"), isOn: $item.rise)
                            .tint(Theme.accent)
                    } footer: {
                        Text(L("alarm.riseNoteIOS"))
                    }
                    // Saati sesli söyleme (rakip analizi #20) ve yaklaşan alarm bildirimi (#19)
                    Section {
                        Toggle(L("alarm.speak"), isOn: $item.speak)
                            .tint(Theme.accent)
                    } footer: {
                        Text(L("alarm.speakNoteIOS"))
                    }
                    Section {
                        Picker(L("alarm.pre"), selection: $item.preNotify) {
                            ForEach(AlarmItem.preNotifyChoices, id: \.self) { m in
                                Text(m == 0 ? L("alarm.preOff") : T("alarm.preMin", m)).tag(m)
                            }
                        }
                    } footer: {
                        Text(L("alarm.preNote"))
                    }
                    // Sakin ön alarm (2. analiz #42): kısık yağmur ve çan sesli bildirim
                    Section {
                        Picker(L("alarm.preAlarm"), selection: $item.preAlarm) {
                            ForEach(AlarmItem.preAlarmChoices, id: \.self) { m in
                                Text(m == 0 ? L("alarm.preOff") : T("alarm.preMin", m)).tag(m)
                            }
                        }
                    } footer: {
                        Text(L("alarm.preAlarmNote"))
                    }
                    // Akıllı alarm (SmartSleep): Uyku ekranındaki oturum açıkken pencerede hafif uyku algılanırsa erken çalar
                    Section {
                        Picker(L("alarm.smart"), selection: $item.smart) {
                            ForEach(AlarmItem.smartChoices, id: \.self) { m in
                                Text(m == 0 ? L("alarm.snoozeOff") : T("alarm.smartMin", m)).tag(m)
                            }
                        }
                    }
                    // Gün doğumu ışığı (rakip analizi #17): tam ekran saat açıkken alarmdan önce ekran yavaşça aydınlanır
                    Section {
                        Picker(L("alarm.sunrise"), selection: $item.sunrise) {
                            ForEach(AlarmItem.sunriseChoices, id: \.self) { m in
                                Text(m == 0 ? L("alarm.snoozeOff") : T("alarm.sunriseMin", m)).tag(m)
                            }
                        }
                    }
                    // Kapatma görevi ve uyanma denetimi (rakip analizi #13)
                    Section {
                        Picker(L("alarm.task"), selection: $item.task) {
                            ForEach(WakeTask.kinds + ["random"], id: \.self) { k in Text(L("alarm.task.\(k)")).tag(k) }
                        }
                        // Görev zinciri (2. analiz #34): asıl görevden sonra en çok iki görev daha
                        if item.task != "none" {
                            ForEach(0..<min(2, item.chain.count + 1), id: \.self) { i in
                                Picker(L("alarm.chain"), selection: Binding(get: { item.chain.indices.contains(i) ? item.chain[i] : "none" }, set: { v in
                                    var list = item.chain
                                    if list.indices.contains(i) { list[i] = v } else { list.append(v) }
                                    item.chain = Array(list.prefix(i + 1).filter { $0 != "none" }) + (v == "none" ? [] : Array(list.dropFirst(i + 1)))
                                })) {
                                    ForEach(WakeTask.kinds + ["random"], id: \.self) { k in Text(L("alarm.task.\(k)")).tag(k) }
                                }
                            }
                            // Önizleme (2. analiz #35): alarm çalmadan görevi dene
                            Button { tried = false; tryTask = true } label: { Label(L("alarm.taskTry"), systemImage: "play.circle") }
                        }
                        if item.task == "math" || item.task == "memory" {
                            Picker(L("alarm.task.level"), selection: $item.taskLevel) {
                                ForEach(1...3, id: \.self) { n in Text(L("alarm.task.level.\(n)")).tag(n) }
                            }
                        }
                        if item.task == "code" {
                            if CodeScannerView.available {
                                Button(L(item.code.isEmpty ? "alarm.code.save" : "alarm.code.change")) { scanning = true }
                            } else {
                                Text(L("wake.noCamera")).font(.footnote).foregroundStyle(Theme.muted)
                            }
                            Text(L(item.code.isEmpty ? "alarm.code.missing" : "alarm.code.saved")).font(.footnote).foregroundStyle(Theme.muted)
                        }
                        if item.task == "photo" {
                            if CameraShot.available {
                                Button(L(item.code.hasPrefix(PhotoHash.prefix) ? "alarm.photo.change" : "alarm.photo.save")) { shootingRef = true }
                            } else {
                                Text(L("wake.noCamera")).font(.footnote).foregroundStyle(Theme.muted)
                            }
                            Text(L(item.code.hasPrefix(PhotoHash.prefix) ? "alarm.photo.saved" : "alarm.photo.missing")).font(.footnote).foregroundStyle(Theme.muted)
                        }
                        if item.task == "object" {
                            Picker(L("alarm.task.objectTarget"), selection: Binding(
                                get: { item.code.hasPrefix(ObjectTarget.prefix) ? String(item.code.dropFirst(ObjectTarget.prefix.count)) : "" },
                                set: { item.code = $0.isEmpty ? "" : ObjectTarget.prefix + $0 }
                            )) {
                                Text(L("alarm.task.object.random")).tag("")
                                ForEach(WakeTask.objectIds, id: \.self) { id in Text(L("wake.object." + id)).tag(id) }
                            }
                        }
                        Toggle(L("alarm.wakeCheck"), isOn: $item.wakeCheck).tint(Theme.accent)
                    } header: {
                        Text(L("alarm.task.heading"))
                    }
                }
            }
            .themedBackground()
            .sheet(isPresented: $scanning) {
                CodeScannerView { value in
                    item.code = value
                    scanning = false
                }
                .ignoresSafeArea()
            }
            .fullScreenCover(isPresented: $shootingRef) {
                CameraShot { img in
                    shootingRef = false
                    if let img, let h = PhotoHash.of(img) { item.code = PhotoHash.encode(h) }
                }
                .ignoresSafeArea()
            }
            .sheet(isPresented: $tryTask) {
                NavigationStack {
                    Group {
                        if tried {
                            Text(L("alarm.taskTryDone")).font(.title3).multilineTextAlignment(.center).padding(32)
                        } else {
                            WakeTaskView(task: previewTask, preview: true) { tried = true }
                        }
                    }
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) { Button(L(tried ? "common.done" : "alarm.cancel")) { tryTask = false } }
                    }
                }
            }
            .navigationTitle(L("alarm.new"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(L("alarm.cancel")) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(L("alarm.save")) {
                        let c = Calendar.current.dateComponents([.hour, .minute], from: time)
                        item.hour = c.hour ?? 0
                        item.minute = c.minute ?? 0
                        item.enabled = true
                        item.skipUntil = nil
                        if item.days.isEmpty { item.skipHolidays = false }
                        if item.task == "none" { item.chain = [] }
                        onSave(item)
                        dismiss()
                    }
                }
            }
            .onAppear {
                var c = DateComponents()
                c.hour = item.hour
                c.minute = item.minute
                time = Calendar.current.date(from: c) ?? Date()
            }
        }
    }
}

/// Alarmın nasıl çalacağı (iOS sürümüne göre)
private struct AlarmNote: View {
    var body: some View {
        Text(L(AlarmCenter.hasRealAlarms ? "alarm.realNote" : "alarm.fallbackNote"))
            .font(.footnote)
            .foregroundStyle(Theme.muted)
            .padding(.horizontal, 4)
    }
}

// MARK: Pomodoro

struct PomodoroView: View {
    @AppStorage("pomo.work") private var workMin = 25
    @AppStorage("pomo.short") private var shortMin = 5
    @AppStorage("pomo.long") private var longMin = 15
    @AppStorage("pomo.rounds") private var rounds = 4
    @AppStorage("pomo.phase") private var phase = "work"
    @AppStorage("pomo.round") private var round = 1
    @AppStorage("pomo.end") private var end = 0.0
    @AppStorage("pomo.left") private var left = 0.0
    @AppStorage("pomo.done") private var done = 0
    @AppStorage("pomo.id") private var alarmID = ""
    // Görev adı, istatistik ve arka plan sesi (rakip analizi #16; sitedeki pomodoroyla aynı)
    @AppStorage("pomo.task") private var task = ""
    @AppStorage("pomo.noise") private var noise = "off"
    @AppStorage("pomo.vol") private var volume = 40
    // Günlük hedef ve etiket (rakip analizi #27)
    @AppStorage("pomo.goal") private var goal = 4
    @AppStorage("pomo.tag") private var tag = "none"
    @State private var log = PomoLog.load()

    private var phaseSeconds: Double { Double((phase == "work" ? workMin : phase == "short" ? shortMin : longMin) * 60) }
    private var running: Bool { end > 0 }

    /// Araç kalıbı (2. analiz #200): Sayaç / İstatistik, altta tek ana düğme, ikincil işler "…" menüsünde,
    /// görev ve ses ile süre ayarları alt sayfada
    @State private var page = 0
    @State private var sheet: PomoSheet?

    enum PomoSheet: String, Identifiable {
        case task, settings
        var id: String { rawValue }
    }

    private var primaryTitle: String {
        if running { return L("tools.pomodoro.pause") }
        return L(left > 0 ? "tools.pomodoro.resume" : "tools.pomodoro.start")
    }

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let remaining = running ? max(0, end - context.date.timeIntervalSinceReferenceDate) : (left > 0 ? left : phaseSeconds)
            ScrollView {
                VStack(spacing: 18) {
                    Picker("", selection: $page) {
                        Text(L("tool.tab.timer")).tag(0)
                        Text(L("tool.tab.stats")).tag(1)
                    }
                    .pickerStyle(.segmented)
                    .padding(.top, 8)
                    if page == 0 {
                        timerPage(remaining: remaining, now: context.date)
                    } else {
                        statsPage
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 12)
            }
            .safeAreaInset(edge: .bottom) {
                ToolPrimaryBar(primary: primaryTitle) { toggle(remaining: remaining) }
            }
            .onChange(of: running && remaining <= 0) { _, finished in
                if finished { advance(completed: true) }
            }
        }
        .background(AppBackdrop().ignoresSafeArea())
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                MoreMenu(items: [
                    MoreItem(title: L("tools.pomodoro.skip"), icon: "forward.end") { advance() },
                    MoreItem(title: L("tools.pomodoro.reset"), icon: "arrow.counterclockwise", destructive: true) { reset() },
                    MoreItem(title: L("tools.pomodoro.settings"), icon: "slider.horizontal.3") { sheet = .settings },
                ])
            }
        }
        .sheet(item: $sheet) { s in
            NavigationStack {
                sheetContent(s)
                    .toolbar { ToolbarItem(placement: .confirmationAction) { Button(L("common.done")) { sheet = nil } } }
            }
            .presentationDetents([.medium, .large])
            .tint(Theme.accent)
        }
        // Kilit ekranı / Dynamic Island: çalışan pomodoro aşaması; arka plan sesi çalışma süresince
        .onChange(of: end) { LiveActivities.sync(); PomoNoise.sync(); FocusBlock.sync() }
        .onChange(of: phase) { PomoNoise.sync(); FocusBlock.sync() }
        .onChange(of: noise) { PomoNoise.sync() }
        .onChange(of: volume) { if NoisePlayer.shared.source == "pomo" { NoisePlayer.shared.setVolume(volume) } }
        .onAppear { log = PomoLog.load(); PomoNoise.sync() }
        .navigationTitle(L("tools.hub.cards.pomodoro.name"))
        .navigationBarTitleDisplayMode(.large)
    }

    private func toggle(remaining: Double) {
        if running {
            left = remaining
            end = 0
            cancelAlarm()
        } else {
            start(seconds: left > 0 ? left : phaseSeconds)
        }
    }

    private func reset() {
        cancelAlarm()
        end = 0; left = 0; phase = "work"; round = 1; done = 0
    }

    private var phaseColor: Color { phase == "work" ? Theme.accent : CalendarData.color("religious") }

    @ViewBuilder private func timerPage(remaining: Double, now: Date) -> some View {
        Text(L("tools.pomodoro.\(phase)"))
            .font(.title3.weight(.semibold))
            .foregroundStyle(phaseColor)
        // Kalan süre halkası ve tur domatesleri (2. analiz #101)
        ProgressRing(progress: remaining / max(1, phaseSeconds), color: phaseColor) {
            BigTime(text: clock(Int(remaining.rounded(.up))))
        }
        .frame(maxWidth: 320)
        RoundDots(total: rounds, done: phase == "long" ? rounds : round - 1, current: phase == "work" ? round : 0,
                  label: T("tools.pomodoro.round", round, rounds))
        goalLine(now: now)
        Text(T("tools.pomodoro.completed", done)).font(.footnote).foregroundStyle(Theme.muted)
        // Görev ve ses tek satır: dokununca alt sayfa
        Button { sheet = .task } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(L("tool.taskSound")).font(.caption).foregroundStyle(Theme.muted)
                    Text(taskSummary).foregroundStyle(Theme.text).lineLimit(1)
                }
                Spacer()
                Image(systemName: "chevron.forward").foregroundStyle(Theme.muted)
            }
            .card()
        }
        .buttonStyle(.plain)
    }

    private var taskSummary: String {
        let name = task.isEmpty ? L("tools.pomodoro.noTask") : task
        let sound = noiseName(noise)
        let label = tag == "none" ? "" : " · " + L("tools.pomodoro.tags.\(tag)")
        return name + " · " + sound + label
    }

    private func goalLine(now: Date) -> some View {
        let todayCount = log.filter { $0.d == BadgeStore.day(now) }.count
        let progress = T("tools.pomodoro.goalProgress", todayCount, goal)
        let reached = todayCount >= goal
        return Text(reached ? L("tools.pomodoro.goalDone") + " · " + progress : progress)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(reached ? Theme.accent : Theme.muted)
    }

    @ViewBuilder private var statsPage: some View {
        FocusStatsCard(log: log) {
            PomoLog.save([])
            log = []
        }
        FocusForest(log: log)
    }

    @ViewBuilder private func sheetContent(_ s: PomoSheet) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                switch s {
                case .task: taskSheet
                case .settings: settingsSheet
                }
            }
            .padding(16)
        }
        .background(AppBackdrop().ignoresSafeArea())
        .navigationTitle(s == .task ? L("tool.taskSound") : L("tools.pomodoro.settings"))
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder private var taskSheet: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L("tools.pomodoro.taskLabel")).font(.subheadline).foregroundStyle(Theme.muted)
            TextField(L("tools.pomodoro.taskPlaceholder"), text: Binding(get: { task }, set: { task = String($0.prefix(60)) }))
                .textFieldStyle(.roundedBorder)
                .submitLabel(.done)
            Picker(L("tools.pomodoro.tagLabel"), selection: $tag) {
                ForEach(PomoLog.tags, id: \.self) { Text(L("tools.pomodoro.tags.\($0)")).tag($0) }
            }
            .pickerStyle(.segmented)
        }
        PomoTodoCard(current: $task).id(log.count) // tur bitince sayaçlar yeniden okunur
        VStack(alignment: .leading, spacing: 8) {
            SectionTitle(text: L("tools.pomodoro.noise"))
            NoiseControls(kind: $noise, volume: $volume)
            Text(L("pomo.noiseNote")).font(.caption).foregroundStyle(Theme.muted)
        }
        .card()
    }

    @ViewBuilder private var settingsSheet: some View {
        VStack(alignment: .leading, spacing: 4) {
            Stepper("\(L("tools.pomodoro.workMin")): \(workMin)", value: $workMin, in: 5...90, step: 5)
            Stepper("\(L("tools.pomodoro.shortMin")): \(shortMin)", value: $shortMin, in: 1...30)
            Stepper("\(L("tools.pomodoro.longMin")): \(longMin)", value: $longMin, in: 5...60, step: 5)
            Stepper("\(L("tools.pomodoro.roundsLabel")): \(rounds)", value: $rounds, in: 2...8)
            Stepper("\(L("tools.pomodoro.goalLabel")): \(goal)", value: $goal, in: 1...24)
        }
        .card()
        FocusBlockCard()
        FlipFocusCard()
    }

    private func start(seconds: Double) { PomoEngine.start(seconds: seconds) }

    private func cancelAlarm() { PomoEngine.cancelAlarm() }

    /// Sonraki aşamaya geçer (duraklatılmış başlar)
    private func advance(completed: Bool = false) {
        PomoEngine.advance(completed: completed)
        if completed { log = PomoLog.load() }
    }
}

/// Pomodoro durumunu değiştiren ortak işlemler: araç sayfası ve tam ekrandaki pomodoro sayfası (2. analiz #26)
/// aynı kayıtları (`pomo.*`) kullanır
@MainActor
enum PomoEngine {
    private static var d: UserDefaults { .standard }
    private static func int(_ key: String, _ def: Int) -> Int { (d.object(forKey: key) as? Int) ?? def }
    static var phase: String { d.string(forKey: "pomo.phase") ?? "work" }
    static var phaseSeconds: Double {
        let p = phase
        return Double((p == "work" ? int("pomo.work", 25) : p == "short" ? int("pomo.short", 5) : int("pomo.long", 15)) * 60)
    }

    static func remaining(at now: Date) -> Double {
        let end = d.double(forKey: "pomo.end"), left = d.double(forKey: "pomo.left")
        return end > 0 ? max(0, end - now.timeIntervalSinceReferenceDate) : (left > 0 ? left : phaseSeconds)
    }

    static func start(seconds: Double) {
        let id = UUID()
        d.set(id.uuidString, forKey: "pomo.id")
        let end = Date().timeIntervalSinceReferenceDate + seconds
        d.set(end, forKey: "pomo.end")
        d.set(0.0, forKey: "pomo.left")
        let title = T("tools.pomodoro.phaseDone", L("tools.pomodoro.\(phase)"))
        Task { await AlarmCenter.schedule(id: id, at: Date(timeIntervalSinceReferenceDate: end), title: title) }
        sync()
    }

    static func pause(at now: Date) {
        d.set(remaining(at: now), forKey: "pomo.left")
        d.set(0.0, forKey: "pomo.end")
        cancelAlarm()
        sync()
    }

    static func cancelAlarm() {
        if let id = UUID(uuidString: d.string(forKey: "pomo.id") ?? "") { AlarmCenter.cancel(id: id) }
        d.set("", forKey: "pomo.id")
    }

    /// Sonraki aşamaya geçer (duraklatılmış başlar); tamamlanan çalışma turu geçmişe yazılır (atlanan sayılmaz)
    static func advance(completed: Bool = false) {
        if !completed { cancelAlarm() }
        let end = d.double(forKey: "pomo.end")
        var round = int("pomo.round", 1)
        if phase == "work" {
            if completed {
                d.set(d.integer(forKey: "pomo.done") + 1, forKey: "pomo.done")
                BadgeStore.shared.hit(.pomodoro) // çalışma süresi sonuna kadar tamamlandı
                let task = d.string(forKey: "pomo.task") ?? ""
                PomoLog.record(minutes: int("pomo.work", 25), task: task, tag: d.string(forKey: "pomo.tag") ?? "none",
                               at: end > 0 ? Date(timeIntervalSinceReferenceDate: end) : Date())
                PomoTodo.save(PomoTodo.credit(PomoTodo.load(), task: task))
            }
            d.set(round >= int("pomo.rounds", 4) ? "long" : "short", forKey: "pomo.phase")
        } else {
            if phase == "long" { round = 1 } else { round += 1 }
            d.set(round, forKey: "pomo.round")
            d.set("work", forKey: "pomo.phase")
        }
        d.set(0.0, forKey: "pomo.end")
        d.set(0.0, forKey: "pomo.left")
        sync()
    }

    private static func sync() {
        LiveActivities.sync()
        PomoNoise.sync()
        FocusBlock.sync()
    }
}

// MARK: Tarih hesaplama

struct DateCalcView: View {
    @State private var start = Date()
    @State private var finish = Date().addingTimeInterval(30 * 86400)
    @State private var base = Date()
    @State private var amount = 30
    @State private var unit = "days"
    @State private var birth = Calendar.current.date(byAdding: .year, value: -30, to: Date()) ?? Date()
    @State private var hijriFrom = Date()
    @State private var hYear = Hijri.date(Date())?.year ?? 1448
    @State private var hMonth = Hijri.date(Date())?.month ?? 1
    @State private var hDay = Hijri.date(Date())?.day ?? 1

    private let off = CalendarData.shared.offDays

    var body: some View {
        let today = CalendarDay(Date())
        Form {
            Section(L("datecalc.diffHeading")) {
                DatePicker(L("datecalc.startLabel"), selection: $start, displayedComponents: .date)
                DatePicker(L("datecalc.endLabel"), selection: $finish, displayedComponents: .date)
                let a = min(CalendarDay(start), CalendarDay(finish))
                let b = max(CalendarDay(start), CalendarDay(finish))
                let n = DateCalc.days(from: a, to: b)
                let r = DateCalc.ymd(from: a, to: b)
                Text(T("datecalc.diffDays", num(n))).font(.headline)
                Text(T("datecalc.diffWeeks", n / 7, n % 7))
                Text(T("datecalc.diffYmd", r.years, r.months, r.days))
                Text(T("datecalc.diffWork", workText(DateCalc.workdays(from: a, to: b, offDays: off))))
                Text(L("datecalc.diffNote")).font(.footnote).foregroundStyle(Theme.muted)
            }
            // Hicri–miladi çevirici (rakip analizi #13): Diyanet tablosu, dışında Umm al-Qura
            Section(L("hijriConv.h1")) {
                DatePicker(L("hijriConv.gregLabel"), selection: $hijriFrom, displayedComponents: .date)
                let c = Calendar.current.dateComponents([.year, .month, .day], from: hijriFrom)
                if let r = Hijri.convert(year: c.year ?? 2026, month: c.month ?? 1, day: c.day ?? 1) {
                    Text(r.date.text).font(.headline)
                    if r.outside { Text(L("hijriConv.outsideApp")).font(.footnote).foregroundStyle(Theme.muted) }
                }
                Stepper("\(L("hijriConv.yearLabel")): \(hYear)", value: $hYear, in: 1300...1600)
                Picker(L("hijriConv.monthLabel"), selection: $hMonth) {
                    ForEach(1...12, id: \.self) { Text(L("hijri.month.\($0)")).tag($0) }
                }
                Stepper("\(L("hijriConv.dayLabel")): \(hDay)", value: $hDay, in: 1...30)
                switch Hijri.gregorian(year: hYear, month: hMonth, day: hDay) {
                case let .date(y, m, d, outside):
                    Text(Fmt.longDate(CalendarDay(y, m, d).noon(in: .current))).font(.headline)
                    if outside { Text(L("hijriConv.outsideApp")).font(.footnote).foregroundStyle(Theme.muted) }
                case let .tooLong(n):
                    Text(T("hijriConv.dayMax", n)).foregroundStyle(Theme.muted)
                case .none:
                    Text("–")
                }
            }
            Section(L("datecalc.addHeading")) {
                DatePicker(L("datecalc.startLabel"), selection: $base, displayedComponents: .date)
                Stepper("\(L("datecalc.amountLabel")): \(amount)", value: $amount, in: -3650...3650)
                Picker(L("datecalc.unitLabel"), selection: $unit) {
                    ForEach(["days", "weeks", "months", "years", "workdays"], id: \.self) { Text(L("datecalc.units.\($0)")).tag($0) }
                }
                Text(T("datecalc.addResult", Fmt.day(added(CalendarDay(base))))).font(.headline)
                Text(L("datecalc.addHint")).font(.footnote).foregroundStyle(Theme.muted)
            }
            Section(L("datecalc.ageHeading")) {
                DatePicker(L("datecalc.birthLabel"), selection: $birth, in: ...Date(), displayedComponents: .date)
                let b = CalendarDay(birth)
                let r = DateCalc.ymd(from: b, to: today)
                let next = DateCalc.nextBirthday(b, today: today)
                Text(T("datecalc.ageResult", r.years, r.months, r.days)).font(.headline)
                Text(T("datecalc.ageDays", num(DateCalc.days(from: b, to: today))))
                if next == today {
                    Text(L("datecalc.ageToday"))
                } else {
                    Text(T("datecalc.ageNext", Fmt.day(next), DateCalc.days(from: today, to: next)))
                }
            }
            Section {
                Text(T("datecalc.range", CalendarData.shared.yearRange.lowerBound, CalendarData.shared.yearRange.upperBound))
                    .font(.footnote)
                    .foregroundStyle(Theme.muted)
            }
        }
        .themedBackground()
        .navigationTitle(L("tools.hub.cards.datecalc.name"))
        .navigationBarTitleDisplayMode(.large)
        // Rozet: kullanıcı bir hesap girdisini değiştirince
        .onChange(of: [start, finish, base, birth]) { BadgeStore.shared.hit(.tarihHesap) }
        .onChange(of: amount) { BadgeStore.shared.hit(.tarihHesap) }
        .onChange(of: unit) { BadgeStore.shared.hit(.tarihHesap) }
    }

    private func workText(_ w: Double) -> String {
        w == w.rounded() ? num(Int(w)) : String(format: "%.1f", w).replacingOccurrences(of: ".", with: AppLocale.code == "en" ? "." : ",")
    }

    private func added(_ d: CalendarDay) -> CalendarDay {
        switch unit {
        case "weeks": return d.adding(days: amount * 7)
        case "months": return d.adding(months: amount)
        case "years": return d.adding(months: amount * 12)
        case "workdays": return DateCalc.adding(workdays: amount, to: d, offDays: off)
        default: return d.adding(days: amount)
        }
    }
}
