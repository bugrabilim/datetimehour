import XCTest
@testable import SaatCore

private struct SkyVectors: Decodable {
    struct P: Decodable { let ms: Double; let name: String; let ra: Double; let dec: Double; let dist: Double; let elong: Double; let mag: Double }
    struct L: Decodable { let ut: Double; let p1: Int64?; let u1: Int64?; let u2: Int64?; let max: Int64; let u3: Int64?; let u4: Int64?; let p4: Int64?; let umag: Double }
    struct R: Decodable { let c1: Int64?; let max: Int64; let c4: Int64?; let obsc: Double; let central: String }
    struct S: Decodable { let ut: Double; let lat: Double; let lon: Double; let r: R? }
    let planets: [P]
    let lunar: [L]
    let solar: [S]
}

final class SkyTests: XCTestCase {
    /// Gezegen konumları ve tutulma ayrıntısı sitedeki hesapla aynı (src/js/sky-calc.js, nature-calc.js)
    func testMatchesWebCalculation() throws {
        let file = try Vectors.load("sky.json", as: SkyVectors.self)
        XCTAssertGreaterThan(file.planets.count, 100)
        for c in file.planets {
            let p = Sky.planet(c.name, ms: c.ms)
            XCTAssertEqual(p.ra, c.ra, accuracy: 1e-7, c.name)
            XCTAssertEqual(p.dec, c.dec, accuracy: 1e-7, c.name)
            XCTAssertEqual(p.dist, c.dist, accuracy: 1e-9, c.name)
            XCTAssertEqual(p.elong, c.elong, accuracy: 1e-7, c.name)
            XCTAssertEqual(p.mag, c.mag, accuracy: 1e-7, c.name)
        }
        func close(_ want: Int64?, _ got: Int64?, _ id: String) {
            switch (want, got) {
            case (nil, nil): break
            case let (w?, g?): XCTAssertLessThanOrEqual(abs(w - g), 2000, id)
            default: XCTFail("\(id): \(String(describing: want)) / \(String(describing: got))")
            }
        }
        for c in file.lunar {
            let l = Sky.lunarContacts(ut: c.ut)
            close(c.p1, l.p1, "p1"); close(c.u1, l.u1, "u1"); close(c.u2, l.u2, "u2"); close(c.max, l.max, "max")
            close(c.u3, l.u3, "u3"); close(c.u4, l.u4, "u4"); close(c.p4, l.p4, "p4")
            XCTAssertEqual(l.umag, c.umag, accuracy: 1e-6)
        }
        for c in file.solar {
            let got = Sky.solarLocal(ut: c.ut, lat: c.lat, lon: c.lon)
            guard let r = c.r else { XCTAssertNil(got); continue }
            guard let g = got else { XCTFail("görülmeli \(c.ut)"); continue }
            close(r.c1, g.c1, "c1"); close(r.c4, g.c4, "c4"); close(r.max, g.max, "max")
            XCTAssertEqual(g.obsc, r.obsc, accuracy: 1e-4)
            XCTAssertEqual(g.central, r.central)
        }
    }
}
