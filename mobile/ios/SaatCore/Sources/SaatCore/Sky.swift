import Foundation

/// Gezegenler ve tutulma ayrıntısı (3 Kasım 2026, 2. analiz #157, #159). Sitedeki `src/js/sky-calc.js` ve
/// `src/js/nature-calc.js` (`solarLocal`, `lunarContacts`) dosyalarının karşılığı; `mobile/shared/testdata/sky.json`
/// ile doğrulanır. Zamanlar UTC milisaniye.
public enum Sky {
    static let unixJDs = 2440587.5
    static let deltaTs = 69.0
    public static let planets = ["mercury", "venus", "mars", "jupiter", "saturn"]
    static let elements: [String: [Double]] = [
        "mercury": [0.38709927, 0.00000037, 0.20563593, 0.00001906, 7.00497902, -0.00594749, 252.25032350, 149472.67411175, 77.45779628, 0.16047689, 48.33076593, -0.12534081],
        "venus": [0.72333566, 0.00000390, 0.00677672, -0.00004107, 3.39467605, -0.00078890, 181.97909950, 58517.81538729, 131.60246718, 0.00268329, 76.67984255, -0.27769418],
        "earth": [1.00000261, 0.00000562, 0.01671123, -0.00004392, -0.00001531, -0.01294668, 100.46457166, 35999.37244981, 102.93768193, 0.32327364, 0, 0],
        "mars": [1.52371034, 0.00001847, 0.09339410, 0.00007882, 1.84969142, -0.00813131, -4.55343205, 19140.30268499, -23.94362959, 0.44441088, 49.55953891, -0.29257343],
        "jupiter": [5.20288700, -0.00011607, 0.04838624, -0.00013253, 1.30439695, -0.00183714, 34.39644051, 3034.74612775, 14.72847983, 0.21252668, 100.47390909, 0.20469106],
        "saturn": [9.53667594, -0.00125060, 0.05386179, -0.00050991, 2.48599187, 0.00193609, 49.95424423, 1222.49362201, 92.59887831, -0.41897216, 113.66242448, -0.28867794],
    ]

    static func helio(_ name: String, _ T: Double) -> [Double] {
        let q = elements[name] ?? elements["earth"]!
        let a = q[0] + q[1] * T, e = q[2] + q[3] * T, I = q[4] + q[5] * T, L = q[6] + q[7] * T, w = q[8] + q[9] * T, O = q[10] + q[11] * T
        var M = L - w
        let om = w - O
        M = (((M + 180).truncatingRemainder(dividingBy: 360)) + 360).truncatingRemainder(dividingBy: 360) - 180
        let eDeg = e / deg2rad
        var E = M + eDeg * dsin(M)
        for _ in 0..<10 {
            let dE = (M - (E - eDeg * dsin(E))) / (1 - e * dcos(E))
            E += dE
            if abs(dE) < 1e-8 { break }
        }
        let xp = a * (dcos(E) - e), yp = a * (1 - e * e).squareRoot() * dsin(E)
        let x1 = (dcos(om) * dcos(O) - dsin(om) * dsin(O) * dcos(I)) * xp
        let x2 = (-dsin(om) * dcos(O) - dcos(om) * dsin(O) * dcos(I)) * yp
        let y1 = (dcos(om) * dsin(O) + dsin(om) * dcos(O) * dcos(I)) * xp
        let y2 = (-dsin(om) * dsin(O) + dcos(om) * dcos(O) * dcos(I)) * yp
        let z1 = (dsin(om) * dsin(I)) * xp
        let z2 = (dcos(om) * dsin(I)) * yp
        return [x1 + x2, y1 + y2, z1 + z2]
    }

    static func magnitude(_ name: String, _ i: Double) -> Double {
        switch name {
        case "mercury": return -0.42 + 0.0380 * i - 0.000273 * i * i + 0.000002 * i * i * i
        case "venus": return -4.40 + 0.0009 * i + 0.000239 * i * i - 0.00000065 * i * i * i
        case "mars": return -1.52 + 0.016 * i
        case "jupiter": return -9.40 + 0.005 * i
        default: return -8.88
        }
    }

    public struct Planet: Sendable {
        public let ra: Double, dec: Double, dist: Double, r: Double, elong: Double, phase: Double, illum: Double, mag: Double
    }

    public static func planet(_ name: String, ms: Double) -> Planet {
        let T = (ms / 86400000 + unixJDs + deltaTs / 86400 - 2451545) / 36525
        let ea = helio("earth", T)
        var p = helio(name, T)
        var dx = p[0] - ea[0], dy = p[1] - ea[1], dz = p[2] - ea[2]
        var dist = (dx * dx + dy * dy + dz * dz).squareRoot()
        let T2 = T - dist * 0.0057755183 / 36525
        p = helio(name, T2)
        dx = p[0] - ea[0]; dy = p[1] - ea[1]; dz = p[2] - ea[2]
        dist = (dx * dx + dy * dy + dz * dz).squareRoot()
        let r = (p[0] * p[0] + p[1] * p[1] + p[2] * p[2]).squareRoot()
        let R = (ea[0] * ea[0] + ea[1] * ea[1] + ea[2] * ea[2]).squareRoot()
        let lam = atan2(dy, dx) / deg2rad + 1.3969713 * T
        let bet = asin(dz / dist) / deg2rad
        let eps = 23.4392911 - 0.0130042 * T
        let ra = atan2(dsin(lam) * dcos(eps) - tan(bet * deg2rad) * dsin(eps), dcos(lam)) / deg2rad
        let dec = asin(dsin(bet) * dcos(eps) + dcos(bet) * dsin(eps) * dsin(lam)) / deg2rad
        let ci = (r * r + dist * dist - R * R) / (2 * r * dist)
        let i = acos(max(-1, min(1, ci))) / deg2rad
        let ce = (R * R + dist * dist - r * r) / (2 * R * dist)
        let elong = acos(max(-1, min(1, ce))) / deg2rad
        let raN = (ra.truncatingRemainder(dividingBy: 360) + 360).truncatingRemainder(dividingBy: 360)
        return Planet(ra: raN, dec: dec, dist: dist, r: r, elong: elong, phase: i, illum: (1 + ci) / 2, mag: magnitude(name, i) + 5 * log10(r * dist))
    }

    /// Parlak yıldızlar (J2000): ad, sağ açıklık, dik açıklık, kadir (sitedeki listeyle aynı)
    public static let stars: [(name: String, ra: Double, dec: Double, mag: Double)] = [
        ("Sirius", 101.287, -16.716, -1.46), ("Canopus", 95.988, -52.696, -0.74), ("Arcturus", 213.915, 19.182, -0.05),
        ("Vega", 279.234, 38.784, 0.03), ("Capella", 79.172, 45.998, 0.08), ("Rigel", 78.634, -8.202, 0.13),
        ("Procyon", 114.825, 5.225, 0.34), ("Achernar", 24.429, -57.237, 0.46), ("Betelgeuse", 88.793, 7.407, 0.50),
        ("Hadar", 210.956, -60.373, 0.61), ("Altair", 297.696, 8.868, 0.76), ("Acrux", 186.650, -63.099, 0.76),
        ("Aldebaran", 68.980, 16.509, 0.86), ("Antares", 247.352, -26.432, 0.96), ("Spica", 201.298, -11.161, 0.97),
        ("Pollux", 116.329, 28.026, 1.14), ("Fomalhaut", 344.413, -29.622, 1.16), ("Deneb", 310.358, 45.280, 1.25),
        ("Mimosa", 191.930, -59.689, 1.25), ("Regulus", 152.093, 11.967, 1.35), ("Adhara", 104.656, -28.972, 1.50),
        ("Castor", 113.650, 31.888, 1.58), ("Shaula", 263.402, -37.104, 1.62), ("Bellatrix", 81.283, 6.350, 1.64),
        ("Elnath", 81.573, 28.608, 1.65), ("Alnilam", 84.053, -1.202, 1.69), ("Alnitak", 85.190, -1.943, 1.77),
        ("Alioth", 193.507, 55.960, 1.77), ("Dubhe", 165.932, 61.751, 1.79), ("Mirfak", 51.081, 49.861, 1.79),
        ("Alkaid", 206.885, 49.313, 1.86), ("Polaris", 37.955, 89.264, 1.98), ("Mintaka", 83.002, -0.299, 2.23),
        ("Mizar", 200.981, 54.925, 2.23), ("Merak", 165.460, 56.383, 2.37), ("Phecda", 178.458, 53.695, 2.44),
        ("Megrez", 183.857, 57.033, 3.31), ("Schedar", 10.127, 56.537, 2.24), ("Caph", 2.295, 59.150, 2.28),
    ]

    /// Yükseklik ve azimut (derece), havasız
    public static func altAz(ms: Double, ra: Double, dec: Double, lat: Double, lon: Double) -> (alt: Double, az: Double) {
        Nature.altAz(ms, ra: ra, dec: dec, lat: lat, lon: lon)
    }
    /// Güneş'in sağ açıklığı ve dik açıklığı (derece)
    public static func sun(ms: Double) -> (ra: Double, dec: Double) {
        let s = Nature.sunPosition(atMilliseconds: ms)
        return (s.rightAscension, s.declination)
    }

    // MARK: Tutulma ayrıntısı
    static let auKm = 149597870.7
    static let rEarth = 6378.14
    static func vec3(_ ra: Double, _ dec: Double, _ r: Double) -> [Double] {
        [r * dcos(dec) * dcos(ra), r * dcos(dec) * dsin(ra), r * dsin(dec)]
    }
    static func gmst(_ ms: Double) -> Double {
        let d = ms / 86400000 + unixJDs - 2451545, t = d / 36525
        return 280.46061837 + 360.98564736629 * d + 0.000387933 * t * t
    }

    public struct Disks: Sendable { public let d: Double, rs: Double, rm: Double, alt: Double }

    public static func solarDisks(ms: Double, lat: Double, lon: Double) -> Disks {
        let m = Nature.moonPosition(atMilliseconds: ms), sp = Nature.sunPosition(atMilliseconds: ms)
        let mdist = rEarth / sin(m.parallax * deg2rad)
        let M = vec3(m.rightAscension, m.declination, mdist), S = vec3(sp.rightAscension, sp.declination, sp.distance * auKm)
        let th = gmst(ms) + lon, cl = dcos(lat)
        let o = [rEarth * cl * dcos(th), rEarth * cl * dsin(th), rEarth * dsin(lat)]
        let sx = S[0] - o[0], sy = S[1] - o[1], sz = S[2] - o[2]
        let mx = M[0] - o[0], my = M[1] - o[1], mz = M[2] - o[2]
        let ds = (sx * sx + sy * sy + sz * sz).squareRoot(), dm = (mx * mx + my * my + mz * mz).squareRoot()
        let c = (sx * mx + sy * my + sz * mz) / (ds * dm)
        let alt = Nature.altAz(ms, ra: sp.rightAscension, dec: sp.declination, lat: lat, lon: lon).alt
        return Disks(d: acos(max(-1, min(1, c))) / deg2rad, rs: asin(696000 / ds) / deg2rad, rm: asin(1737.4 / dm) / deg2rad, alt: alt)
    }

    static func overlapFrac(_ d: Double, _ rs: Double, _ rm: Double) -> Double {
        if d >= rs + rm { return 0 }
        if d <= abs(rs - rm) { return rm >= rs ? 1 : (rm * rm) / (rs * rs) }
        let a1 = acos((d * d + rs * rs - rm * rm) / (2 * d * rs)), a2 = acos((d * d + rm * rm - rs * rs) / (2 * d * rm))
        let area = rs * rs * (a1 - sin(2 * a1) / 2) + rm * rm * (a2 - sin(2 * a2) / 2)
        return area / (Double.pi * rs * rs)
    }

    public struct SolarLocal: Sendable { public let c1: Int64?, max: Int64, c4: Int64?, mag: Double, obsc: Double, central: String, altMax: Double }

    public static func solarLocal(ut: Double, lat: Double, lon: Double) -> SolarLocal? {
        let f: (Double) -> Double = { t in let x = solarDisks(ms: t, lat: lat, lon: lon); return x.d - x.rs - x.rm }
        var best: Double? = nil
        var bt = 0.0
        var t = ut - 4 * 3600000
        while t <= ut + 4 * 3600000 {
            let g = f(t)
            if best == nil || g < best! { best = g; bt = t }
            t += 60000
        }
        guard let b0 = best, b0 < 0 else { return nil }
        var a = bt - 60000, b = bt + 60000
        for _ in 0..<30 {
            let m1 = a + (b - a) * 0.382, m2 = a + (b - a) * 0.618
            if solarDisks(ms: m1, lat: lat, lon: lon).d < solarDisks(ms: m2, lat: lat, lon: lon).d { b = m2 } else { a = m1 }
        }
        let tm = (a + b) / 2, X = solarDisks(ms: tm, lat: lat, lon: lon)
        func edge(_ dir: Double) -> Int64? {
            var lo = tm, hi = tm + dir * 4 * 3600000
            if f(hi) < 0 { return nil }
            for _ in 0..<40 { let mid = (lo + hi) / 2; if f(mid) < 0 { lo = mid } else { hi = mid } }
            return Int64(((lo + hi) / 2 + 0.5).rounded(.down))
        }
        let central = X.d < abs(X.rm - X.rs) ? (X.rm > X.rs ? "total" : "annular") : ""
        return SolarLocal(c1: edge(-1), max: Int64((tm + 0.5).rounded(.down)), c4: edge(1), mag: (X.rs + X.rm - X.d) / (2 * X.rs), obsc: overlapFrac(X.d, X.rs, X.rm), central: central, altMax: X.alt)
    }

    public struct PathPoint: Sendable { public let t: Double, x: Double, y: Double }
    public struct Lunar: Sendable {
        public let p1: Int64?, u1: Int64?, u2: Int64?, max: Int64, u3: Int64?, u4: Int64?, p4: Int64?
        public let umag: Double, pmag: Double, rU: Double, rP: Double, rM: Double
        public let path: [PathPoint]
    }
    struct Geo { let x: Double, y: Double, d: Double, rU: Double, rP: Double, rM: Double }
    static func geo(_ t: Double) -> Geo {
        let m = Nature.moonPosition(atMilliseconds: t), s = Nature.sunPosition(atMilliseconds: t)
        let ra0 = s.rightAscension + 180, dec0 = -s.declination
        let x = ((m.rightAscension - ra0 + 540).truncatingRemainder(dividingBy: 360) - 180) * dcos(dec0)
        let y = m.declination - dec0
        let pm = m.parallax, ps = 8.794 / 3600 / s.distance, ss = 0.2666 / s.distance, rm = 0.2725 * pm
        return Geo(x: x, y: y, d: (x * x + y * y).squareRoot(), rU: 1.0118 * pm + ps - ss, rP: 1.0118 * pm + ps + ss, rM: rm)
    }

    public static func lunarContacts(ut: Double) -> Lunar {
        var a = ut - 3 * 3600000, b = ut + 3 * 3600000
        for _ in 0..<40 {
            let m1 = a + (b - a) * 0.382, m2 = a + (b - a) * 0.618
            if geo(m1).d < geo(m2).d { b = m2 } else { a = m1 }
        }
        let tm = (a + b) / 2, G = geo(tm)
        func cross(_ lim: (Geo) -> Double, _ dir: Double) -> Int64? {
            let g: (Double) -> Double = { t in let q = geo(t); return q.d - lim(q) }
            if g(tm) >= 0 { return nil }
            var lo = tm, hi = tm + dir * 4 * 3600000
            for _ in 0..<40 { let mid = (lo + hi) / 2; if g(mid) < 0 { lo = mid } else { hi = mid } }
            return Int64(((lo + hi) / 2 + 0.5).rounded(.down))
        }
        let P: (Geo) -> Double = { $0.rP + $0.rM }
        let U: (Geo) -> Double = { $0.rU + $0.rM }
        let T: (Geo) -> Double = { $0.rU - $0.rM }
        let p1 = cross(P, -1), p4 = cross(P, 1)
        let from = p1.map(Double.init) ?? (tm - 3 * 3600000), to = p4.map(Double.init) ?? (tm + 3 * 3600000)
        var path: [PathPoint] = []
        var t = from - 1800000
        while t <= to + 1800000 { let q = geo(t); path.append(PathPoint(t: t, x: q.x, y: q.y)); t += 600000 }
        return Lunar(p1: p1, u1: cross(U, -1), u2: cross(T, -1), max: Int64((tm + 0.5).rounded(.down)), u3: cross(T, 1), u4: cross(U, 1), p4: p4,
                     umag: (G.rU + G.rM - G.d) / (2 * G.rM), pmag: (G.rP + G.rM - G.d) / (2 * G.rM), rU: G.rU, rP: G.rP, rM: G.rM, path: path)
    }
}
