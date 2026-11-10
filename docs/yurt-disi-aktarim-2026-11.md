# Kişisel verilerin yurt dışına aktarımı (KVKK md. 9): hesap e-postaları ve gece yedekleri

10 Kasım 2026. Sahibin isteği: "yurtdışı hukuk işini, geniş çaplı metni sen hazırla". Bu belge hukuki görüş
değildir; resmî metinlere (Kanun, Yönetmelik, Kurul duyuruları, standart sözleşme) dayanan bir hazırlık dosyasıdır.
İmzadan önce bir avukatın bakması önerilir.

## 1. Özet

- Saat Tarih'in uygulamalardaki isteğe bağlı hesabı iki yerde **sürekli** olarak ABD'ye kişisel veri gönderir:
  1. **Resend** (Plus Five Five, Inc., San Francisco): hesap e-postalarının gönderimi (alıcı adresi, konu, metin,
     doğrulama/sıfırlama bağlantısı).
  2. **Cloudflare R2** (Cloudflare, Inc., San Francisco; kova konumu ENAM, ABD): veritabanının gece yedekleri, 30 gün
     (hesap tablolarının tamamı: e-posta, görünen ad, dil, şifre özeti, oturumlar, IP adresleri, user agent).
- 1 Haziran 2024'ten beri (7499 sayılı Kanun md. 34) yurt dışı aktarım için açık rıza **yalnız arızi** (ara sıra,
  sürekli olmayan) aktarımlarda dayanak olabilir; geçiş süresi 1 Eylül 2024'te bitti. Bu iki aktarım sürekli olduğu
  için açık rıza ya da md. 9/6'daki diğer istisnalar kullanılamaz.
- ABD için Kurul'un yeterlilik kararı yoktur. Kalan yol **uygun güvence**: Kurul'un ilan ettiği **standart sözleşme**,
  bizim durumumuzda **Standart Sözleşme 2 (Veri Sorumlusundan Veri İşleyene)**. İki ayrı sözleşme gerekir
  (Bumba Teknoloji ↔ Resend, Bumba Teknoloji ↔ Cloudflare). Her biri imzaların tamamlanmasından itibaren **5 iş günü**
  içinde Kurum'a bildirilir.
- Resend ve Cloudflare'in sözleşmelerinde (DPA) yalnız AB ve İngiltere standart maddeleri var, **KVKK standart
  sözleşmesi yok** (Resend DPA 31 Aralık 2025 sürümü; Cloudflare müşteri DPA'sı). Türk sözleşmesinin onlarla ayrıca
  imzalanması gerekir; imzalamazlarsa bölüm 8'deki B planı.
- Gizlilik sayfasındaki aydınlatma metni (19 dil) bu yola göre yazıldı: alıcılar, ülke, aktarılan veri, amaç,
  standart sözleşme ve bildirim, ABD'deki koruma düzeyi uyarısı. **Bu metnin doğru olması için sözleşmeler hesap
  sunucusunun ilk yayınından önce imzalanıp bildirilmiş olmalıdır** (bölüm 9).
- Bu iki hizmet Bumba'nın bütün ürünlerinde ortaktır (standart: "E-posta gönderimi" Resend; "Altyapı kuralı" yedekler
  her gece R2'ye). Sözleşmeyi Bumba Teknoloji bir kez imzalar; Ek I'de bütün ürünler sayılırsa her ürün için ayrı
  sözleşme gerekmez.

## 2. Hukuki çerçeve

| Kaynak | Ne diyor |
|---|---|
| 6698 sayılı KVKK md. 9 (7499 sayılı Kanunla değişik, 1.6.2024) | Aktarım: (1) yeterlilik kararı varsa; (4) yoksa uygun güvencelerle: kamu kurumları arası anlaşma, bağlayıcı şirket kuralları, **standart sözleşme**, Kurul izniyle yazılı taahhütname; (5) standart sözleşme imzadan itibaren 5 iş günü içinde Kurum'a bildirilir; (6) ikisi de yoksa yalnız arızi aktarımlarda istisnalar (açık rıza dahil). |
| Kişisel Verilerin Yurt Dışına Aktarılmasına İlişkin Usul ve Esaslar Hakkında Yönetmelik (RG 10.7.2024, 32598) | Standart sözleşmelerin kullanımı, bildirim usulü (fiziki, KEP ya da Kurul'un belirlediği yöntem); md. 14: taraf değişikliği, içerik değişikliği ve sona erme de bildirilir. |
| Kurul'un standart sözleşme metinleri (kvkk.gov.tr › Standart Sözleşmeler) | Dört tür: 1 sorumludan sorumluya, **2 sorumludan işleyene**, 3 işleyenden işleyene, 4 işleyenden sorumluya. İngilizce çevirisi de yayımlandı (bilgi amaçlı; imza Türkçe metne). |
| Standart Sözleşme Bildirim Modülü duyurusu (Kurul kararı 17.10.2024, 2024/1793) | Bildirim internetten modülle de yapılabilir. |
| Standart sözleşmelerde dikkat edilecek hususlar duyurusu (5.2.2025) | Aşağıdaki bölüm 6'daki şartlar. |
| KVKK md. 18 | Bildirim yükümlülüğüne uymamaya idari para cezası; tutar her yıl yeniden değerlenir (güncel tutar kvkk.gov.tr'den). |

## 3. Aktarım envanteri

| Aktarım | Alıcı ve rolü | Ülke | Veri | Amaç | Sıklık | Dayanak |
|---|---|---|---|---|---|---|
| Hesap e-postaları | Plus Five Five, Inc. ("Resend"), veri işleyen | ABD (alt işleyenleri de ABD: AWS gönderim altyapısı vb.) | Alıcı e-posta adresi, e-postanın konusu ve metni, doğrulama/sıfırlama/değişiklik bağlantısı (tek kullanımlık belirteç içerir), gönderim zamanı; Resend'in gönderim kayıtları | Doğrulama, şifre sıfırlama, e-posta değişikliği, kullanılmayan hesap uyarısı, silme bildirimi (KVKK md. 5/2-c) | Sürekli | Standart Sözleşme 2 |
| Gece yedekleri | Cloudflare, Inc. (R2), veri işleyen | ABD (kova ENAM) | Veritabanının tamamı: e-posta, görünen ad, dil, şifre özeti (scrypt), hesap tarihleri, oturumlar (belirteç, IP, user agent, zamanlar), doğrulama kayıtları, deneme sayaçları (IP), kullanım günü | Veri kaybına karşı yedek (md. 5/2-c ve 5/2-f; md. 12 güvenlik yükümlülüğü) | Sürekli (her gece, 30 gün saklanır) | Standart Sözleşme 2 |

Aktarım sayılmayan ya da ayrı ele alınanlar (hukukçuya not):
- **Ana sunucu** İstanbul'da (Coolify VPS): aktarım yok.
- **Sitedeki dış hizmetler** (Open-Meteo, Overpass, OpenStreetMap karoları, YouTube gelişmiş gizlilik oynatıcısı):
  bağlantıyı kullanıcının tarayıcısı, kullanıcının izniyle (çerez tercihleri › Dış hizmetler) doğrudan kurar; veri
  bizim sunucumuzdan geçmez. Gizlilik sayfasında anlatılıyor. Bunların md. 9 kapsamında "bizim aktarımımız" sayılıp
  sayılmayacağı tartışmalıdır; şu anki uygulama izin + aydınlatma.
- **Uygulamalarda Apple ve Google'ın platform hizmetleri** (WeatherKit, iCloud anahtar-değer eşitlemesi, Health,
  Google yedeği): veriyi kullanıcının kendi Apple/Google hesabı işler, bize gelmez.
- **Umami** (istatistik) kendi sunucumuzda, İstanbul'da; yalnız izinle.
- **Ortak e-posta listesi** bumbagroup.com'un işidir (ayrı aydınlatma metni); bu dosyanın kapsamı dışında.

## 4. Seçilen yol

- **Tür:** Standart Sözleşme 2 (Veri Sorumlusundan Veri İşleyene). Veri aktaran: Bumba Teknoloji Limited Şirketi
  (veri sorumlusu, standarttaki sabit bilgi). Veri alıcıları: Plus Five Five, Inc. ve Cloudflare, Inc.
- **Kapsam:** Ek I'de yalnız Saat Tarih değil, Resend ve R2'yi kullanan bütün Bumba ürünleri sayılır (ürün listesi
  bumbagroup.com'dan). Böylece her yeni üründe sözleşme değil, yalnız Ek I değişikliği (ve bildirimi) gerekir.
- **Seçimlik maddeler** (değiştirilebilen yalnız bunlar):
  - Madde 8 Alt veri işleyen: **2. seçenek, genel yetkilendirme**, bildirim süresi **30 gün** (iki sağlayıcı da alt
    işleyen listesini sitesinde yayımlıyor ve değişiklikte bildirim yapıyor; özel yetkilendirmeyi kabul etmezler).
  - Madde 10 isteğe bağlı paragraf (bağımsız uyuşmazlık çözüm kuruluşu): **konmaz** (sağlayıcıların böyle bir
    kuruluşu yok).
  - Madde 16 Kurum'a bildirim: **konur, bildirimi veri aktaran yapar** ("Veri aktaran bu Sözleşmeyi, imzaların
    tamamlanmasından itibaren beş iş günü içinde Kurum'a bildirir.").
- **Değiştirilemeyenler** (sağlayıcının itiraz edebileceği yerler, önceden bilinmeli): Türk hukuku ve Türk
  mahkemeleri (md. 17–18), Kurul'un yetkisine tabi olma (md. 12 sonu), ulusal hukukun sözleşmeye aykırı olmadığı
  beyanı (md. 13), kamu makamı talebinde derhâl bildirim (md. 14), denetim hakkı (md. 7.9/12), fesihte iade ya da
  yok etme (md. 15). Bunlar pazarlıkla değişmez; sağlayıcı kabul etmezse sözleşme yapılamaz.

## 5. Ek I, Ek II, Ek III taslakları

Aşağıdakiler Kurul metnindeki ek başlıklarının birebir sırasıyla doldurulmuştur. İmzalanacak nüsha kvkk.gov.tr'deki
güncel Türkçe metinden alınır; yalnız ekler ve seçimlik maddeler doldurulur.

### 5.1 Sözleşme A: Bumba Teknoloji ↔ Plus Five Five, Inc. (Resend)

**Taraflar**
- Veri aktaran: Bumba Teknoloji Limited Şirketi, [ticaret sicilindeki tam adres], Ataşehir / İstanbul, Türkiye.
  İrtibat: [ad soyad, unvan], bilgi@bumbagroup.com. İmzalayan: [şirket müdürü ad soyad, unvan].
- Veri alıcısı: Plus Five Five, Inc. ("Resend"), 2261 Market Street #5039, San Francisco, CA 94114, ABD.
  İrtibat: [Resend'in bildirdiği kişi]. İmzalayan: [Resend'in yetkilisi].

**Ek I. Aktarımın detayları**
- *Veri aktaranın faaliyetleri:* Bumba Teknoloji, ürünlerinin (Saat Tarih ve [diğer ürünler]) kullanıcılarına hesap
  ve işlem e-postaları gönderir; kampanya ve tanıtım e-postası göndermez.
- *Veri alıcısının faaliyetleri:* Veri aktaranın API ile ilettiği işlem e-postalarını alıcılara iletmek, iletim
  durumunu (teslim, geri dönme, şikâyet) kaydetmek ve veri aktarana göstermek.
- *İlgili kişi grupları:* Ürünlerde hesap açan ya da hesap açmaya çalışan kullanıcılar; e-posta değiştirme talebinde
  eski ve yeni adres sahipleri.
- *Kişisel veri kategorileri:* İletişim (e-posta adresi); müşteri işlem (e-postanın konusu ve metni, doğrulama, şifre
  sıfırlama ve adres değişikliği bağlantıları, gönderim zamanı); işlem güvenliği (iletim kayıtları: teslim, geri dönme
  ve şikâyet olayları; Resend'in kendi kayıtlarına aldığı alıcı sunucu bilgileri).
- *Özel nitelikli kişisel veri:* Yok. (Saat Tarih'in ileride açacağı zikir/hatim halkaları din verisi sayılabilir;
  e-postalarda halka adı ya da üyelik geçecekse önce bu ek güncellenir ve Kurum'a bildirilir.)
- *Aktarımın hukuki sebebi:* KVKK md. 5/2-c (hesap sözleşmesinin kurulması ve ifası); aktarım KVKK md. 9/4 ve bu
  standart sözleşme.
- *Aktarım sıklığı:* Sürekli (her hesap işleminde).
- *İşleme faaliyetinin niteliği:* İletim, geçici saklama, kayıt tutma.
- *Amaçlar:* Hesap e-postalarının iletilmesi ve iletim sorunlarının giderilmesi.
- *Saklama süresi:* Resend'in hizmet kayıtları için kendi saklama süresi ([Resend'in bildirdiği süre]); sözleşme
  sona erince veri aktaranın tercihiyle iade ya da yok etme (md. 7.4).
- *Alt veri işleyene aktarımlar:* Ek III'teki şirketlere, yalnız iletim ve hizmet altyapısı için, sözleşme süresince.
- *Alıcı grupları:* E-postanın alıcısı (ilgili kişinin kendi e-posta sağlayıcısı).
- *VERBİS bilgileri:* [Bumba Teknoloji kayıt yükümlüsüyse sicil numarası; değilse "Kayıt yükümlülüğü yoktur
  (Kurul'un istisna kararı)"].

**Ek II. Teknik ve idari tedbirler (veri alıcısı)**
- Aktarım yalnız TLS ile şifreli API çağrısıyla; API anahtarı yalnız sunucunun ortam değişkeninde, koda ve depoya
  yazılmaz.
- Veri alıcısının yayımladığı güvenlik önlemleri (DPA Ek C, SOC 2 Tip II raporu): erişim denetimi, çalışanların
  gizlilik yükümlülüğü, şifreli saklama, kayıt tutma, olay müdahalesi.
- Veri aktaran tarafı: e-postalara yalnız gerekli bilgi konur (görünen ad yok, yalnız e-posta adresi ve bağlantı);
  bağlantılardaki belirteçler kısa ömürlü ve tek kullanımlık; tanıtım e-postası yok.
- İlgili kişi taleplerine yardım (md. 9): veri alıcısı, veri aktaranın talebiyle belirli bir adrese ait gönderim
  kayıtlarını siler ya da dışa aktarır.

**Ek III. Alt veri işleyenler** (Resend'in 5 Kasım 2026 tarihli listesinden; imza günü güncel liste konur)
Amazon Web Services, Inc. (barındırma ve gönderim), Cloudflare, Inc. (web uygulama güvenlik duvarı), Datadog, Inc.,
Elastic N.V., Google, Inc., Inngest Inc., PlanetScale, Inc., Supabase, Inc., Svix Inc., Tinybird Inc., Vercel Inc.
ve listedeki diğerleri (ABD). Her birinin adresi, irtibat noktası ve işleme ayrıntısı imzadan önce Resend'den alınır.

### 5.2 Sözleşme B: Bumba Teknoloji ↔ Cloudflare, Inc. (R2)

**Taraflar**
- Veri aktaran: aynı.
- Veri alıcısı: Cloudflare, Inc., 101 Townsend St., San Francisco, CA 94107, ABD. İrtibat: [Cloudflare'in veri
  koruma irtibatı]. İmzalayan: [Cloudflare'in yetkilisi].

**Ek I. Aktarımın detayları**
- *Veri aktaranın faaliyetleri:* Bumba Teknoloji, ürünlerinin veritabanlarını ve kalıcı disklerini her gece yedekler.
- *Veri alıcısının faaliyetleri:* Yedek dosyalarını R2 nesne depolamada saklamak, yalnız veri aktaranın isteğiyle
  geri vermek, süresi dolanları veri aktaranın kuralıyla silmek.
- *İlgili kişi grupları:* Ürünlerin hesap sahibi kullanıcıları (Saat Tarih: uygulamalarda hesap açanlar) ve
  [diğer ürünlerin kayıtlı kullanıcıları].
- *Kişisel veri kategorileri (Saat Tarih için):* Kimlik (görünen ad), iletişim (e-posta adresi), müşteri işlem (dil,
  hesap açılış ve son kullanım tarihi), işlem güvenliği (şifre özeti, oturum belirteçleri, IP adresleri, user agent,
  deneme sayaçları, doğrulama kayıtları). Diğer ürünler için ilgili ürünün aydınlatma metnindeki kategoriler.
- *Özel nitelikli kişisel veri:* Şu an yok (Saat Tarih). Halka özellikleri açılırsa (din verisi olabilir) önce bu ek
  güncellenir ve bildirilir.
- *Hukuki sebep:* KVKK md. 5/2-c ve 5/2-f, md. 12 (veri güvenliği); aktarım md. 9/4 ve bu sözleşme.
- *Aktarım sıklığı:* Sürekli (her gece).
- *İşleme faaliyetinin niteliği:* Depolama, geri yükleme için okuma, silme.
- *Amaçlar:* Veri kaybına ve sunucu arızasına karşı yedek.
- *Saklama süresi:* Her yedek **30 gün**, sonra kendiliğinden silinir (kova yaşam döngüsü kuralı).
- *Alt veri işleyene aktarımlar:* Ek III.
- *Alıcı grupları:* Yok (yalnız veri aktaran erişir).
- *VERBİS bilgileri:* Sözleşme A ile aynı.

**Ek II. Teknik ve idari tedbirler**
- Yedekler TLS ile gönderilir; Cloudflare R2 nesneleri kendi tarafında şifreli saklar (AES-256).
- R2 erişim anahtarları yalnız yedek uygulamasında; kova herkese kapalı, erişim yalnız anahtarla.
- Dökümler ayrıca bizim parolamızla şifrelenmiyor (ana altyapı oturumu, 10 Kasım 2026). **Öneri:** dökümler
  yüklemeden önce bizim anahtarımızla şifrelenirse (ör. age ya da gpg) sağlayıcı içeriği okuyamaz ve sözleşmede
  güçlü bir ek tedbir olur; ana altyapı oturumuna sorulacak.
- 30 günlük yaşam döngüsü; silinen hesabın kopyası en geç 30 günde yedeklerden de gider (gizlilik ve hesap silme
  sayfalarında yazıyor).

**Ek III. Alt veri işleyenler:** Cloudflare'in yayımladığı alt işleyen listesinden R2'yle ilgili olanlar
(cloudflare.com › Trust Hub › Subprocessors); imza günü güncel liste konur.

## 6. İmza ve bildirim şartları (Kurul duyurusu, 5 Şubat 2025)

1. İki taraf da (ya da temsile ve imzaya yetkili kişileri) imzalar; biri eksikse sözleşme geçersiz.
2. İmzalar Türk Borçlar Kanunu'na uygun olmalı: ıslak imza ya da güvenli elektronik imza. Sıradan "tıkla kabul et"
   ya da DocuSign tipi basit e-imzanın yeterli olmayabileceğini hukukçuya sorun.
3. Yabancı dilde de yapılırsa iki taraf da **Türkçe metni** imzalar; çift sütunluysa imzalar Türkçe sütunda.
4. İmzalayanların yetki belgeleri sözleşmeyle Kurum'a sunulur; belgede imzalayanın adı geçmeli. Yabancı resmî
   belgeler **apostilli** (ABD Lahey Apostil Sözleşmesi'ne taraf) ve **noter onaylı Türkçe çevirili** olmalı.
5. Taraf adları sözleşmede ve yetki belgelerinde aynı yazılmalı (Plus Five Five, Inc.; Cloudflare, Inc.).
6. Her iki tarafın imza tarihi sözleşmede yazılmalı; geriye dönük "yürürlük tarihi" ibaresi konmaz.
7. Adres, irtibat noktası, imzacı adları açıkça yazılır.
8. İmzaların tamamlanmasından itibaren **5 iş günü** içinde Kurum'a bildirilir: Standart Sözleşme Bildirim Modülü
   (internet), KEP ya da fiziki. Sözleşmenin bütün sayfaları eklenir.
9. Sonradan taraf değişikliği, ek değişikliği (yeni ürün, yeni veri kategorisi, alt işleyen değişikliği) ve sona erme
   de bildirilir (Yönetmelik md. 14; sözleşme md. 8).
10. Yalnız seçimlik ve alternatif maddeler değiştirilebilir; başka ekleme, çıkarma, değişiklik yapılmaz.

## 7. Sağlayıcılara gönderilecek yazı (İngilizce taslak)

Resend için privacy@resend.com ya da destek; Cloudflare için hesap panelindeki destek talebi ya da
privacyquestions@cloudflare.com. Bumba Teknoloji adına bilgi@bumbagroup.com'dan gönderilir.

> Subject: Request to execute the Turkish standard contract (KVKK Standard Contract 2, controller to processor)
>
> Hello,
>
> Bumba Teknoloji Limited Şirketi (Istanbul, Türkiye) uses [Resend / Cloudflare R2] to [send transactional
> account emails / store nightly database backups] for its products. Under Article 9 of Türkiye's Personal Data
> Protection Law No. 6698 (as amended on 1 June 2024), transfers of personal data from Türkiye to the United States
> require the standard contract announced by the Turkish Personal Data Protection Board. Your DPA includes the EU
> SCCs and the UK Addendum, which are not recognised as a transfer tool under Turkish law.
>
> We would like to sign "Standard Contract 2 (Controller to Processor)" with you. The text is fixed by the Board and
> may not be modified except for the optional clauses; an English translation is published by the Authority
> (kvkk.gov.tr › Standart Sözleşmeler). We have prepared Annexes I–III based on your DPA and sub-processor list.
>
> Requirements set by the Authority: the Turkish text must be signed by an authorised signatory of each party with a
> legally valid signature and the signature date; we will need a document showing your signatory's authority (e.g. a
> certificate of incumbency or board resolution) with an apostille. We will file the contract with the Authority
> within five business days of signature.
>
> Could you tell us whether you can sign it, and whom we should send the documents to?
>
> Kind regards,
> [Ad Soyad], Bumba Teknoloji Limited Şirketi, bilgi@bumbagroup.com

## 8. Sağlayıcı imzalamazsa (B planı; karar sahibin)

Standart sözleşme olmadan bu iki aktarım md. 9'a uygun olmaz. Seçenekler, en kolaydan:

1. **Yedekleri ABD yerine Türkiye'de tutmak** (ör. Türkiye'deki bir nesne depolama ya da ikinci bir yurt içi sunucu).
   Aktarım sorunu yedeklerde tamamen kalkar. Ana altyapı oturumunun işi; bütün Bumba ürünlerini etkiler.
2. **Dökümleri yüklemeden önce bizim anahtarımızla şifrelemek.** Sağlayıcı içeriği okuyamaz. Tek başına aktarımı
   md. 9'un dışına çıkardığı kesin değildir (şifreli veri de kişisel veri sayılabilir); hukukçuya sorulmalı. Her
   durumda Ek II'yi güçlendirir.
3. **İşlem e-postalarını Türkiye'de işleyen bir sağlayıcıyla göndermek** (yurt içinde barındırılan bir e-posta
   gönderim hizmeti ya da kendi SMTP'miz). Standart "E-posta gönderimi" maddesi Resend dediği için bu bir istisna
   talebidir; sahibin ve ana altyapının kararı.
4. **Hesap sunucusunu açmamak** (hesap özelliği bekler). Hesap isteğe bağlı olduğu için uygulamanın geri kalanı
   etkilenmez; ama hesap ekranları uygulamada "sunucuya ulaşılamadı" der, o sürüm yayımlanmadan gizlenmeli.

## 9. Yayın sırasına etkisi

1. Site (PR bugrabilim/datetimehour#60) canlıya çıkabilir: aydınlatma metni yayında olur, ama hesap sunucusu kapalı
   olduğu için henüz hiçbir veri işlenmez ve aktarılmaz.
2. **Standart sözleşmeler (A ve B) imzalanıp Kurum'a bildirilmeden hesap sunucusunun ilk yayını yapılmaz.**
   Gizlilik metni "standart sözleşmeye dayanır, imzadan sonra beş iş günü içinde bildirilir" diyor; sözleşme yokken
   sunucu açılırsa metin yanlış olur ve aktarım hukuka aykırı olur.
3. Sağlayıcılardan biri imzalamazsa bölüm 8'den seçim yapılır ve gizlilik metni (19 dil) o seçime göre yeniden yazılır.
4. Bumba'nın Resend ve R2 kullanan diğer ürünleri için de aynı durum geçerlidir; sözleşmeler Ek I'de onları da
   sayarsa hepsi birlikte düzelir. Bu bilgi ana altyapı oturumuna da iletilmeli.

## 10. Diğer KVKK yükümlülükleri (kontrol listesi)

- **VERBİS:** Kurul'un istisna kararına göre yıllık çalışan sayısı ve yıllık mali bilanço eşiklerinin altındaki ve
  ana faaliyeti özel nitelikli veri işleme olmayan veri sorumluları sicile kayıt yükümlüsü değildir. Bumba
  Teknoloji'nin durumu güncel eşiklerle kvkk.gov.tr'den kontrol edilmeli; halka özellikleri (din verisi) gelirse
  yeniden bakılmalı.
- **Kişisel veri işleme envanteri ve saklama-imha politikası:** sicile kayıt yükümlüsü değilse zorunlu değildir; ama
  bu dosya ve `docs/uyelik-plani-2026-11.md` envanterin temelini oluşturur.
- **İlgili kişi başvuruları:** bilgi@bumbagroup.com; 30 gün içinde yanıt (md. 13). Uygulamada "Verilerimi indir" ve
  "Hesabı sil" var.
- **Veri ihlali:** öğrenildikten sonra en kısa sürede (Kurul kararına göre 72 saat) Kurul'a ve ilgili kişilere
  bildirim; sağlayıcıların ihlal bildirimi Ek II ve sözleşme md. 7'de.

## Kaynaklar

- Kişisel Verilerin Yurt Dışına Aktarılmasında Kullanılacak Standart Sözleşme - 2 (Veri Sorumlusundan Veri İşleyene),
  Kişisel Verileri Koruma Kurumu: https://www.kvkk.gov.tr/Icerik/7929/Standart-Sozlesmeler
- Yurt Dışına Kişisel Veri Aktarımında Kullanılacak Standart Sözleşmelerde Dikkat Edilmesi Gereken Hususlara İlişkin
  Kamuoyu Duyurusu (5.2.2025): https://www.kvkk.gov.tr/Icerik/8170/
- Standart Sözleşme Bildirim Modülü Hakkında Kamuoyu Duyurusu: https://www.kvkk.gov.tr/Icerik/8043/
- Standart Sözleşmeler ve Bağlayıcı Şirket Kurallarına İlişkin Dokümanlar Hakkında Kamuoyu Duyurusu:
  https://www.kvkk.gov.tr/Icerik/7938/
- Resend DPA (son güncelleme 31 Aralık 2025): https://resend.com/legal/dpa ; alt işleyenler (5 Kasım 2026):
  https://resend.com/legal/subprocessors
- Cloudflare Customer DPA: https://www.cloudflare.com/cloudflare-customer-dpa/
- Bumba Genel Standartlar 5.11: "E-posta gönderimi", "Altyapı kuralı", madde 11 (KVKK).
