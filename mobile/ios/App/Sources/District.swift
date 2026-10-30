import Foundation

/// İlçe (src/data/districts.json, GeoNames CC BY 4.0): ad, ilçe merkezinin koordinatı, bağlı olduğu il. Kimlik "d:il:ilçe".
struct District: Identifiable, Hashable {
    let name: String
    let lat: Double
    let lon: Double
    let province: String

    var id: String { "d:\(province):\(name)" }
    /// İl merkezi ilçesi (adı ilin adı; "Hakkâri" / "Hakkari" gibi yazımlar da)
    var isCentre: Bool { Self.fold(name) == Self.fold(Province.find(province).name) }
    var label: String { isCentre ? Province.find(province).displayName : "\(name), \(Province.find(province).displayName)" }

    static let byProvince: [String: [District]] = {
        guard let url = Bundle.main.url(forResource: "districts", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let raw = try? JSONSerialization.jsonObject(with: data) as? [String: [[Any]]] else { return [:] }
        var out: [String: [District]] = [:]
        for (slug, list) in raw {
            out[slug] = list.compactMap { row in
                guard row.count == 3, let n = row[0] as? String, let lat = (row[1] as? NSNumber)?.doubleValue,
                      let lon = (row[2] as? NSNumber)?.doubleValue else { return nil }
                return District(name: n, lat: lat, lon: lon, province: slug)
            }
        }
        return out
    }()

    static func all(_ province: String) -> [District] { byProvince[province] ?? [] }

    static func find(_ id: String) -> District? {
        let parts = id.split(separator: ":", maxSplits: 2).map(String.init)
        guard parts.count == 3, parts[0] == "d" else { return nil }
        return all(parts[1]).first { $0.name == parts[2] }
    }

    static func fold(_ s: String) -> String {
        s.folding(options: [.caseInsensitive, .diacriticInsensitive], locale: Locale(identifier: "tr")).replacingOccurrences(of: "ı", with: "i")
    }
}

/// Kayıtlı yerler (il ya da ilçe kimlikleri, en çok 8; "|" ile ayrılır)
enum SavedPlaces {
    static let key = "places.saved"
    static var ids: [String] { (UserDefaults.standard.string(forKey: key) ?? "").split(separator: "|").map(String.init) }
    static func set(_ v: [String]) {
        var seen = Set<String>()
        UserDefaults.standard.set(v.filter { seen.insert($0).inserted }.prefix(8).joined(separator: "|"), forKey: key)
    }
}
