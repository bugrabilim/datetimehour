import Foundation

/// Ortak test verisi: mobile/shared/testdata (sitedeki JS hesaplarından üretilir: node mobile/shared/gen-vectors.mjs).
enum Vectors {
    static let directory: URL = {
        var url = URL(fileURLWithPath: #filePath) // …/mobile/ios/SaatCore/Tests/SaatCoreTests/Vectors.swift
        for _ in 0..<5 { url.deleteLastPathComponent() } // → …/mobile
        return url.appendingPathComponent("shared/testdata")
    }()

    static func load<T: Decodable>(_ name: String, as type: T.Type) throws -> T {
        let data = try Data(contentsOf: directory.appendingPathComponent(name))
        return try JSONDecoder().decode(T.self, from: data)
    }
}

struct PrayerVectors: Decodable {
    struct Case: Decodable {
        let id: String
        let y: Int, m: Int, d: Int
        let lat: Double, lon: Double, tz: Double
        let t: [Double?]
    }
    let keys: [String]
    let cases: [Case]
}

struct NatureVectors: Decodable {
    struct Season: Decodable { let y: Int; let march: Int64; let june: Int64; let sept: Int64; let dec: Int64 }
    struct Phase: Decodable { let k: Int; let new: Int64; let full: Int64 }
    struct MoonSample: Decodable {
        let ms: Int64
        let age: Double, frac: Double, illum: Double
        let idx: Int
        let nextNew: Int64, nextFull: Int64
    }
    let seasons: [Season]
    let phases: [Phase]
    let moon: [MoonSample]
}
