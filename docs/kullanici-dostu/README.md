# Kullanıcı dostu Saat Tarih: gezinme ve sayfa düzeni (3 Kasım 2026)

**Soru (sahip):** "Bütün özellikler ekli hâlde uygulamada çok fazla düğme olacak. Bunu nasıl kullanıcı dostu yaparız? Sayfaları nasıl kullanıcı dostu yaparız?"

**Karar (sahip, 3 Kasım 2026):** önerilen **Seçenek B** seçildi: kişisel alt çubuk ve "Tümü" sayfası. İşleri 2. rakip analizi listesinde #194–#206.

**Kaynaklar:**
- Görsel sunum: [`sunum.html`](sunum.html). Yedi ekranın önce/sonra çizimleri burada. Yayımlanmış hâli claude.ai'de "Kullanıcı Dostu Saat Tarih" sayfası (sahibe özel).
- "Önce" görüntüleri Android ekran görüntülerimizdir (31 Ekim 2026). "Sonra" çizimleri bizim renk belirteçlerimizle HTML/CSS ile çizildi.
- Rakip görselleri telif nedeniyle kullanılmadı.

## Bugün ne var (koddan ve ekran görüntülerinden sayıldı)

| Ekran | Bugün | Sorun |
|---|---|---|
| Alt çubuk | Hepsi açıkken 7 sekme (Saat, Hava, Takvim, Dünya, Mevsimler, Vakitler, Araçlar), 10 pt yazı; seçili sekme yalnız renkle belli | Platform önerisinin (3–5) üstünde. Bazı simgeler dolu, bazıları çizgi görünüyor |
| Arama | Genel arama yok; yalnız konum, Kur'an ve Esma içinde arama var | Bir aracı bulmak için hangi sekmede olduğunu bilmek gerekiyor |
| Araçlar | 3 kartta 11 satır: 8 araç + Rozetler + Ayarlar + Hakkında | Ayarlar ve Hakkında araç değil. Kıble ve ibadet araçları Vakitler'in dibinde 12 bağlantı olarak duruyor |
| Saat | Saat, ‹ 1/20 ›, Tam ekran, Düzenle, dişli | Ekranın ~%58'i boş. "1/20" neye baktığını söylemiyor |
| Vakitler | Paylaş, zil, konum, sayaç, 6 vakit, Güneş ve Ay, kerahat, kilit ekranı anahtarı, sonra 12 bağlantı kartı | Sayfa kabaca 3 ekran boyu. Kur'an ve Zikirmatik en altta kayboluyor |
| Ayarlar | Android'de 13, iPhone'da 15 öğe; 8 kart, yalnız 3'ünün başlığı var | Gruplanmamış, arama yok. Uzun açıklamalar listenin içinde |
| Pomodoro | Sayaç, eşit iki düğme, Sıfırla, görev, 5 etiket, yapılacaklar, ses, istatistik | İlk ekranda 15 dokunulabilir öğe. Ana iş (başlatmak) öne çıkmıyor |
| Zamanlayıcı | 3 sekme, süren her sayaçta 3 düğme, 6 ok, hazır süreler, ad alanı | ~25 dokunuş noktası. Süren sayaçlarla yeni sayaç kurma aynı ekranda |
| Alarm düzenleyicisi | 15'ten çok seçenek | Hepsi açık duruyor. Çoğu kişi yalnız saati ve günleri değiştirir |
| İlk açılış | Yalnız saat ve gri bir satır ("özellikleri aç") | Uygulamanın ne yaptığı anlatılmıyor |
| Tam ekran saat | Android'de düğmeler hep görünür | Sitede düğmeler dokununca 4 saniyeliğine çıkıyor |
| Dil | "Açtığın…", "sen silene kadar", "Ayarlarınız…" | Hitap tutarsız |

## Dayanılan ilkeler (kaynaklarıyla)

1. **Alt çubukta 3–5 hedef.**
   - Android Developers, Navigation bar: "Three to five destinations of equal importance".
   - Apple HIG, Tab bars.
2. **Kullanıcı kendi sekmelerini seçebilir; varsayılan liste 5 ya da daha az.** Apple HIG, Tab bars: "If you let people select their own tabs, aim for a default list of five or fewer".
3. **"Diğer" (More) sekmesi kötü çözüm.** Apple HIG, Tab bars. Bu yüzden "Tümü" aramalı ve kategorili gerçek bir sayfa, bir taşma listesi değil.
4. **Sekmeler sabit kalsın.** Apple HIG, Tab bars: sekmeyi bazen gösterip bazen gizlemek arayüzü kararsız gösterir.
5. **Aşamalı gösterim.** Önce birkaç önemli seçenek gösterilir, gelişmişi bir dokunuş ötededir (NN/g, Progressive Disclosure).
6. **Hick yasası.** Seçenek çoğaldıkça karar uzar; uzun listeler gruplanır (NN/g, Hick's Law).
7. **Görünür gezinme gizli menüden daha çok kullanılır.** NN/g, How to Make Navigation (Even a Hamburger) Discoverable on Mobile.
8. **Her ekranda tek belirgin düğme; ana iş için tam genişlik.** Apple HIG, Buttons.
9. **Dokunma alanı.** En az 44×44 pt (Apple HIG, Buttons) ya da 48×48 dp (Android, Make apps more accessible).
10. **Başparmak bölgesi.** Gözlenen kullanıcıların %49'u telefonu tek elle kullanıyor (Hoober, UXmatters 2013).
11. **Alt sayfa kısa işler içindir, gezinme için değil.** Apple HIG, Sheets.
12. **iPad ve Mac'te kenar çubuğu.** Apple HIG, Sidebars.

**Rakipler:**
- Apple Saat 4 sekme; Google Saat 5; Samsung Saat 4; Alarmy 5 (yüzen hap).
- Muslim Pro 5 sekme.
- Vakit 5 sekme: arama ve kategorili "Keşfet" ızgarası.
- Codeida'da ana ekranda 4 kısayol karesi.

Hiçbiri 5'ten çok sekme kullanmıyor. Çok araçlı olanlar ikinci düzeyi ızgara ve aramayla çözüyor.

**Adresler:**
- https://developer.apple.com/design/human-interface-guidelines/tab-bars
- https://developer.apple.com/design/human-interface-guidelines/sidebars
- https://developer.apple.com/design/human-interface-guidelines/sheets
- https://developer.apple.com/design/human-interface-guidelines/buttons
- https://developer.android.com/develop/ui/compose/components/navigation-bar
- https://m3.material.io/components/navigation-bar/overview
- https://developer.android.com/guide/topics/ui/accessibility/apps
- https://www.nngroup.com/articles/progressive-disclosure/
- https://www.nngroup.com/videos/hicks-law-long-menus/
- https://www.nngroup.com/articles/find-navigation-mobile-even-hamburger/
- https://www.nngroup.com/articles/mobile-navigation-patterns/
- https://www.uxmatters.com/mt/archives/2013/02/how-do-users-really-hold-mobile-devices.php

## Üç seçenek

| Seçenek | Ne | Artı | Eksi |
|---|---|---|---|
| A. Sabit beş sekme | Herkese aynı 5 sekme (Saat, Vakitler, Takvim, Hava, Araçlar); Dünya ve Mevsimler Araçlar'a girer | Anlatması kolay; platform sınırına uyar | Namaz kullanmayana boşa yer; yeni işlerle Araçlar yine şişer |
| **B. Kişisel alt çubuk + "Tümü" (seçildi)** | Saat ilk, Tümü son; aradaki en çok 3 yeri kullanıcı seçer; gerisi Tümü'nde, kategorili ve aranabilir | Apple'ın önerdiği yol; özellik aç/kapa ekranının doğal devamı; yeni işler çubuğu büyütmez | Özellikler ekranına bir seçim daha eklenir; iki kişi farklı çubuk görebilir |
| C. Tek ana ekran + kısayollar | Alt çubuk yok; saatin altında kartlar ve kısayol kareleri | Her şey tek ekranda | "Saat ekranında yalnız saat" kararıyla çelişir; zamanla uzun pano olur; iPad'de iyi büyümez |

## Seçenek B'nin kuralları

1. Saat her zaman ilk sekme, "Tümü" her zaman son. Aradaki en çok 3 yer kullanıcının.
2. Özellikler ekranı (30 Ekim 2026 kararı) kalır. Açık her özelliğin yanında "Alt çubukta" işareti çıkar; sıra sürükleyerek değişir.
3. Yeni kurulumda yalnız Saat açık ve alt çubuk yok (bugünkü karar). İlk açılan 3 özellik kendiliğinden çubuğa girer.
4. Araçlar (alarm, zamanlayıcı…) her zaman Tümü'nde. Tek tek araçlar Tümü'nün üstündeki "Sık kullanılanlar"a eklenebilir.
5. Ayarlar saat ekranındaki dişliden ve Tümü'nün sağ üstündeki dişliden açılır. Ayarlar ve Hakkında araç listesinden çıkar.
6. Alt çubuk kendi kendine değişmez. Kapalı bir özellik bildirimden ya da widget'tan açılırsa geri düğmeli sayfa olarak açılır (bugünkü geçici sekmenin, `barTabs`, yerine).
7. iPad ve Mac'te aynı liste kenar çubuğuna döner; Tümü'nün kategorileri orada bölüm olur.

**Tümü sayfası:**
- En üstte arama: araç, ayar ve içerik tek kutudan; sitedeki 15 dilli arama sözcükleri kullanılır.
- Altında 4 karelik "Sık kullanılanlar". Boşsa son kullanılanlar görünür.
- Sonra kategoriler: Süre ve alarm, Gün ve plan, Gök ve dünya, İbadet.

## Ekran ekran öneriler (sunumdaki çizimler)

1. **Alt çubuk ve Özellikler:**
   - En çok 5 sekme.
   - Seçili sekme ana rengin açık tonunda hap içinde, yazı 11 pt, simgeler tek üslupta.
   - Özellikler ekranında "Alt çubukta / Tümü'nde" işareti.
2. **Tümü:**
   - Araçlar listesinin yerine kategorili ızgara, arama ve sık kullanılanlar.
   - Ayarlar sağ üstte dişliyle açılır; Hakkında Ayarlar'ın içine girer; Rozetler sayfanın sonunda kalır.
3. **Ayarlar:**
   - 5 başlıklı grup: Genel, Renk ve görünüm, Saat, Yedek ve kısayollar, Destek.
   - Satırda şimdiki değer görünür ("İstanbul", "24 saat"); en üstte arama.
   - Seçim düğmeleri listede yer kaplamaz, satıra dokununca açılır.
   - Uzun açıklamalar alt sayfaya iner.
4. **Saat ekranı:**
   - Saat dikeyde ortalanır ve büyür.
   - "1/20" yerine "Klasik analog · 1/20".
   - Tam ekran, Düzenle ve Ayarlar düğmeleri alt çubuğun üstüne, başparmak bölgesine iner.
5. **Vakitler:**
   - Her vakte çizgi simge; sıradaki vakit vurgulu.
   - 12 bağlantı kartı yerine 4 karelik İbadet satırı ve "Hepsi ›".
   - Paylaş, aylık çizelge, hesap yöntemi ve imsakiye "…" menüsüne geçer.
   - Kilit ekranı anahtarı Bildirimler'e taşınır.
6. **Pomodoro (bütün araç ekranlarına örnek):**
   - Tek ana düğme "Başlat", tam genişlikte ve altta.
   - Kalan süre halkası ve tur noktaları.
   - Görev ve ses tek satır; dokununca alt sayfa açılır.
   - İstatistikler ayrı sekmede.
   - Sıfırla, süreler, CSV ve silme "…" menüsünde.
7. **İlk açılış:**
   - 3 adım: (1) tanıtım ("Reklamsız, ücretsiz, hesap yok"), (2) "Neleri kullanacaksınız?" seçimi ve canlı alt çubuk önizlemesi, (3) yalnız gerekiyorsa konum ve bildirim izni.
   - "Geç" her adımda görünür; geçen kişi bugünkü gibi yalnız saatle başlar.

## Her sayfa için kurallar

- **Tek ana eylem:** her ekranda dolu renkli tek düğme.
- **İkincil işler "…" menüsünde:** paylaş, dışa aktar, sıfırla, sil. Silme her zaman onay ister.
- **Gelişmiş ayarlar katlanır.** Örneğin alarmda görev, kademeli ses, erteleme sınırı, müzik, gün doğumu ışığı, akıllı alarm.
- **Tutarlı başlık:** sekme sayfalarında büyük başlık; alt sayfalarda geri oku ve küçük başlık; sağ üstte en çok iki simge.
- **Boş durumlar:** simge, bir cümle ve tek düğme.
- **Ayarlar gruplu ve aranabilir.**
- **Tam ekran saatte düğmeler yalnız dokununca belirir.**
- **Tek elle kullanım:** ana düğmeler alt yarıda; dokunma alanı en az 44 pt / 48 dp.
- **Tek hitap:** "siz".
- **Yeni düğme kuralı:** her yeni işte "bu, ekranın ana işi mi?" diye sorulur. Değilse "…" menüsüne ya da alt sayfaya gider. Yeni araç Tümü'nde bir kategoriye ve aramaya girer, alt çubuğa girmez.

## İşler (2. analiz listesinde)

| No | İş | Çaba |
|---|---|---|
| 194 | Kişisel alt çubuk (en çok 5), Özellikler'de "Alt çubukta", iPad/Mac kenar çubuğu | O |
| 195 | "Tümü" sayfası (kategorili ızgara) | O |
| 196 | Uygulama içi genel arama | O |
| 197 | Sık kullanılanlar | K |
| 198 | Gruplu ve aranabilir Ayarlar | K |
| 199 | Vakitler sadeleştirme | K |
| 200 | Araç ekranları için ortak kalıp (tek ana düğme, "…" menüsü) | O |
| 201 | Alarmda "Gelişmiş" bölümü kapalı | K |
| 202 | Saat ekranında model adı, düğmeler alta | K |
| 203 | Geçici sekme yerine geri düğmeli sayfa | K |
| 204 | Tam ekranda düğmeler dokununca belirsin | K |
| 205 | Tek hitap "siz" | K |
| 206 | "Yeni düğme" kuralı | K |

Listede zaten olan ilgili işler: #4 ilk açılış, #94 saat ekranı dengesi, #95 alt çubuk, #96 boş durumlar, #98 başlıklar, #99 simge kutuları, #65 vakit simgeleri, #6/#81 destek grubu, #101 ilerleme halkası.

**Önerilen sıra:**
1. #194, #195 ve #196 birlikte (gezinmenin iskeleti).
2. #199 ve #200 (en kalabalık ekranlar).
3. Küçük işler.
