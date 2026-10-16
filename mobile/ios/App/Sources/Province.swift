import Foundation

/// Türkiye'nin 81 ili (src/data/provinces.json'dan üretilir: mobile/shared/gen-strings.mjs).
struct Province: Decodable, Identifiable, Hashable {
    let slug: String
    let name: String
    let lat: Double
    let lon: Double
    let ar: String

    var id: String { slug }

    /// Arapça arayüzde Arapça ad, diğer dillerde Türkçe ad.
    var displayName: String {
        Locale.current.language.languageCode?.identifier == "ar" ? ar : name
    }

    static let all: [Province] = {
        guard let url = Bundle.main.url(forResource: "provinces", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let list = try? JSONDecoder().decode([Province].self, from: data) else { return [] }
        return list.sorted { $0.name.compare($1.name, locale: Locale(identifier: "tr")) == .orderedAscending }
    }()

    /// En yakın il merkezi ve uzaklığı (km)
    static func nearest(latitude: Double, longitude: Double) -> (Province, Double) {
        func km(_ p: Province) -> Double {
            let r = Double.pi / 180
            let dLat = (p.lat - latitude) * r, dLon = (p.lon - longitude) * r
            let a = sin(dLat / 2) * sin(dLat / 2) + cos(latitude * r) * cos(p.lat * r) * sin(dLon / 2) * sin(dLon / 2)
            return 6371 * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
        let best = all.min { km($0) < km($1) } ?? find("istanbul")
        return (best, km(best))
    }

    static func find(_ slug: String) -> Province {
        all.first { $0.slug == slug } ?? all.first { $0.slug == "istanbul" } ?? all[0]
    }
}
