import SaatCore
import SwiftUI
import UIKit
import UserNotifications
import UserNotificationsUI

/// Vakit bildiriminde uzun basınca açılan içerik (2. analiz #164; 4 Kasım 2026): vakte kalan süre (sistem sayacı) ve
/// bulunduğunuz yerden kıble açısı (kuzeye göre ok). Veriler bildirimin içinden gelir (uygulamanın kayıtlarına
/// erişilmez); hesap SaatCore'da. Ağ yok.
final class NotificationViewController: UIViewController, UNNotificationContentExtension {
    private var host: UIHostingController<NotifyCard>?

    func didReceive(_ notification: UNNotification) {
        let info = notification.request.content.userInfo
        let at = (info["prayerAt"] as? String).flatMap(TimeInterval.init).map { Date(timeIntervalSince1970: $0) }
        let lat = (info["lat"] as? String).flatMap(Double.init)
        let lon = (info["lon"] as? String).flatMap(Double.init)
        let card = NotifyCard(prayer: info["prayer"] as? String ?? notification.request.content.title,
                              place: info["place"] as? String ?? "",
                              at: at,
                              bearing: (lat != nil && lon != nil) ? Qibla.direction(latitude: lat!, longitude: lon!).bearing : nil)
        if let host {
            host.rootView = card
            return
        }
        let h = UIHostingController(rootView: card)
        h.view.backgroundColor = .clear
        addChild(h)
        h.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(h.view)
        NSLayoutConstraint.activate([
            h.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            h.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            h.view.topAnchor.constraint(equalTo: view.topAnchor),
            h.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        h.didMove(toParent: self)
        host = h
        preferredContentSize = CGSize(width: view.bounds.width, height: 200)
    }
}

struct NotifyCard: View {
    let prayer: String
    let place: String
    let at: Date?
    let bearing: Double?

    var body: some View {
        HStack(spacing: 18) {
            VStack(alignment: .leading, spacing: 6) {
                Text(prayer).font(.headline)
                if !place.isEmpty { Text(place).font(.subheadline).foregroundStyle(.secondary) }
                if let at, at > Date() {
                    Text(timerInterval: Date()...at, countsDown: true)
                        .font(.system(size: 34, weight: .bold, design: .rounded)).monospacedDigit()
                }
                if let bearing {
                    Text(String(format: "%.1f°", bearing)).font(.subheadline.monospacedDigit()).foregroundStyle(.secondary)
                }
            }
            Spacer()
            if let bearing {
                ZStack {
                    Circle().stroke(Color.secondary.opacity(0.5), lineWidth: 1.5)
                    Text("N").font(.caption.weight(.bold)).foregroundStyle(.red).offset(y: -44)
                    Image(systemName: "location.north.fill")
                        .resizable().scaledToFit().frame(width: 26, height: 60)
                        .foregroundStyle(.orange)
                        .rotationEffect(.degrees(bearing))
                    Text("🕋").font(.title3)
                        .offset(y: -40)
                        .rotationEffect(.degrees(bearing))
                }
                .frame(width: 110, height: 110)
            }
        }
        .padding(16)
        .environment(\.layoutDirection, .leftToRight)
    }
}
