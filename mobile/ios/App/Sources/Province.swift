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

    static func find(_ slug: String) -> Province {
        all.first { $0.slug == slug } ?? all.first { $0.slug == "istanbul" } ?? all[0]
    }
}
