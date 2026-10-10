// swift-tools-version: 6.0
// SaatCore: uygulamaların ortak hesap çekirdeği (namaz vakitleri, mevsimler, Ay). Arayüz kodu yok;
// iPhone, iPad, Apple Watch ve widget'lar aynı paketi kullanır. Linux'ta da derlenir ve test edilir.
import PackageDescription

let package = Package(
    name: "SaatCore",
    platforms: [.iOS(.v17), .watchOS(.v10), .macOS(.v14)],
    products: [
        .library(name: "SaatCore", targets: ["SaatCore"]),
    ],
    targets: [
        .target(name: "SaatCore"),
        .testTarget(name: "SaatCoreTests", dependencies: ["SaatCore"]),
    ]
)
