# Odak engeli için Apple izni (FamilyControls)

Pomodoro'daki "Odakta engelle" (iOS) Apple'ın Ekran Süresi altyapısını (FamilyControls + ManagedSettings) kullanır.
Apple bu yetkiyi App Store'daki uygulamalara yalnız başvuru üzerine verir. Kod hazır (`App/Sources/FocusBlock.swift`),
izin gelene kadar ekranda gizli (`App/Info.plist` `SaatFamilyControls = false`).

**Ne zaman:** sahibin kararı (31 Ekim 2026): TestFlight yapı 25'in App Store onayı geldikten sonra, iCloud ve HealthKit
yetkileriyle birlikte.

## Başvuru (hesap sahibi yapar)

1. https://developer.apple.com/contact/request/family-controls-distribution adresine Apple Developer hesabıyla girin
   (hesap sahibi olmak gerekir; yönetici yetkisi yetmeyebilir).
2. Uygulama: **Saat Tarih**, Bundle ID **tr.bumba.time**, Team ID **3J9XN2P93D**. Uzantı yok (yalnız ana uygulama).
3. Açıklama alanına yapıştırın:

> Saat Tarih is a clock, prayer times and productivity app. Its Pomodoro focus timer has an optional "Block while
> focusing" setting. The user authorizes Family Controls for themselves (individual authorization) and picks, with
> FamilyActivityPicker, the apps, categories and websites to shield on their own device. The app applies a
> ManagedSettings shield only while a focus session the user started is running and removes it when the session ends.
> Selections stay on the device as opaque tokens; no usage data is collected or sent anywhere. It is not a
> parental-control app and does not monitor other people.

4. Gönderin. Yanıt birkaç günden birkaç haftaya sürebilir; form durum takibi vermez.

## Onaydan sonra

- developer.apple.com › Certificates, Identifiers & Profiles › Identifiers › `tr.bumba.time` › Capabilities: **Family
  Controls (Distribution)** işaretlenir. Aynı yerde **iCloud** (CloudKit gerekmez) ve **HealthKit** de işaretli olmalı.
- Kodda: `App/SaatTarih.entitlements`'a `com.apple.developer.family-controls` = true eklenir, `App/Info.plist`
  `SaatFamilyControls` true yapılır, TestFlight iş akışının yetki denetimine eklenir.
