# Saat Tarih mobil uygulamaları

Tamamen yerel (native) uygulamalar: iPhone, iPad ve Apple Watch için **Swift / SwiftUI**, Android telefon, tablet ve Wear OS için **Kotlin / Jetpack Compose**. Sitenin kodu uygulamalara gömülmez; ortak olan yalnız hesap kuralları ve veri dosyalarıdır (`src/data/`).

## Yapı

| Klasör | İçerik |
|---|---|
| `shared/` | Ortak test verisi. `gen-vectors.mjs` sitedeki hesaplardan (`src/js/prayer-calc.js`, `src/js/nature-calc.js`) `testdata/*.json` üretir |
| `ios/SaatCore/` | Swift paketi: namaz vakitleri, mevsimler, Ay. Arayüz yok; uygulama, widget ve saat aynı paketi kullanır. Linux'ta da derlenir |
| `android/` | Gradle projesi. `:core` modülü saf Kotlin/JVM (Android'e bağlı değil): aynı hesaplar |

Sonraki adımlarda eklenecek: `ios/App` (iPhone/iPad uygulaması, widget ve watchOS hedefleri; Xcode projesi XcodeGen ile `project.yml`'den üretilir), `android/app` (telefon/tablet), `android/wear` (Wear OS).

## Kural: hesaplar üç yerde aynı

Sitedeki JS hesapları doğrulanmış kaynaktır (namaz vakitleri 81 ilde Diyanet ile en çok 1 dk fark). Swift ve Kotlin sürümleri aynı formülleri aynı işlem sırasıyla uygular ve ortak test verisiyle karşılaştırılır (tolerans: vakitlerde 10⁻⁶ saat, anlarda 1 ms; bugünkü fark Swift'te 10⁻¹⁴ saat, Kotlin'de 0).

JS hesabı değişirse:

```sh
node mobile/shared/gen-vectors.mjs        # test verisini yeniden üret (ya da npm run mobile:vectors)
```

sonra Swift ve Kotlin kodu da aynı değişikliği almalı; CI (`.github/workflows/mobile.yml`) verinin güncel olduğunu ve iki dildeki testleri denetler.

## Komutlar

```sh
swift test --package-path mobile/ios/SaatCore     # Swift çekirdek (macOS ya da Linux, Swift 6)
cd mobile/android && ./gradlew :core:test         # Kotlin çekirdek (JDK 17+)
```

Mac gerekmez: iOS derlemeleri GitHub Actions'ın macOS makinelerinde çalışır.

## Sürümler

- Swift 6 (paket araç sürümü 6.0), iOS 17+, watchOS 10+
- Kotlin 2.4.21, Gradle 9.8.1, JVM hedefi 17
- Android: en az API 26 (Android 8), hedef API 36 (Google Play şartı); Wear OS en az API 30
- Paket kimliği (geçici, ilk mağaza yüklemesine kadar değişebilir): `tr.bumba.time`
