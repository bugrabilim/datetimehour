import AppIntents
import WidgetKit

/// Widget'ın il seçimi: "Otomatik (konumum)" ya da 81 ilden biri.
struct ProvinceEntity: AppEntity {
    static let autoID = "auto"
    let id: String
    let name: String

    static var typeDisplayRepresentation: TypeDisplayRepresentation { TypeDisplayRepresentation(name: "prayer.province") }
    static var defaultQuery = ProvinceQuery()
    var displayRepresentation: DisplayRepresentation { DisplayRepresentation(title: "\(name)") }

    static var auto: ProvinceEntity { ProvinceEntity(id: autoID, name: L("widget.province.auto")) }
    static var all: [ProvinceEntity] { [auto] + Province.all.map { ProvinceEntity(id: $0.slug, name: $0.displayName) } }
}

struct ProvinceQuery: EntityStringQuery {
    func entities(for identifiers: [String]) async throws -> [ProvinceEntity] {
        ProvinceEntity.all.filter { identifiers.contains($0.id) }
    }

    func entities(matching string: String) async throws -> [ProvinceEntity] {
        let q = string.folding(options: [.caseInsensitive, .diacriticInsensitive], locale: Locale(identifier: "tr"))
        return ProvinceEntity.all.filter {
            $0.name.folding(options: [.caseInsensitive, .diacriticInsensitive], locale: Locale(identifier: "tr")).contains(q)
        }
    }

    func suggestedEntities() async throws -> [ProvinceEntity] { ProvinceEntity.all }

    func defaultResult() async -> ProvinceEntity? { .auto }
}

struct ProvinceIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "prayer.province"
    static var description = IntentDescription("widget.config.desc")

    @Parameter(title: "prayer.province")
    var province: ProvinceEntity?
}

extension ProvinceIntent {
    /// Seçime göre yer; "Otomatik"te konum (izin yoksa ya da bulunamazsa İstanbul)
    func resolvePlace() async -> Place {
        let id = province?.id ?? ProvinceEntity.autoID
        if id != ProvinceEntity.autoID { return Place(province: Province.find(id)) }
        let finder = await OneShotLocation()
        if let loc = await finder.fetch(requestPermission: false, forWidget: true) {
            return Place(location: loc)
        }
        return Place(province: Province.find("istanbul"))
    }
}
