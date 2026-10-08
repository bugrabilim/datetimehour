# Saat Tarih (datetimehour)

Canlı saat, bugünün tarihi ve dünya saatleri. Statik site, Türkçe (varsayılan) ve İngilizce. Bir **Bumba Life** ürünü (kişinin zamanını harcadığı ürün; bumbagroup.com'da henüz listelenmiyor, marka kitindeki "Hangi kola girer?" sırasıyla Life seçildi).

Bu dosya master dosyadır. Her oturumda okunur.

## Proje
- Tür: **statik site** (sunucu tarafı ve veritabanı yok). Madde 7 (üyelik) ve 10 (panel) uygulanmaz.
- Teknoloji: bağımlılıksız Node (≥20) derleyici `build.mjs` → `dist/`. Çalışma zamanı bağımlılığı yok. Sunum: `Dockerfile` (node derler, nginx sunar) + `nginx.conf`.
- Komutlar: `npm run build`, `npm test` (derle + denetle), `npm run serve` (dist'i :8080'de sunar).
- Yapı: `site.config.json` (ad, asıl adres, Umami kimliği, liste anahtarı, güncelleme tarihi) · `src/i18n/{tr,en}.json` (bütün metinler) · `src/data/cities.json` (şehirler) · `src/styles.css` (tema token'ları) · `src/js/` (theme.js: flash önleme, main.js: saat, tema, dil, arama, liste sonucu) · `src/assets/` (ikonlar, paylaşım görseli; elle üretilmiş PNG'ler commit'lidir) · `scripts/test.mjs`.
- Sayfalar (TR / EN): `/` · `/en/`, `/dunya-saatleri/` · `/en/world-clock/`, her şehir için `/dunya-saatleri/<şehir>/` · `/en/world-clock/<city>/` (17 şehir), `/gizlilik/` · `/en/privacy/`. `404.html` ve `500.html` iki dil bir arada, noindex.
- Yeni şehir: `src/data/cities.json`'a ekle (TR ekli hâli `loc` zorunlu). Yeni metin: iki i18n dosyasına aynı anahtarla ekle (`npm test` eşliği denetler).
- Asıl adres (önerilen, DNS ana oturumda): `https://saat.bumba.tr`. Değişirse yalnız `site.config.json` → `origin` (ya da derleme ortamında `SITE_ORIGIN`).
- Umami kimliği ve liste anahtarı: `site.config.json` (`umamiId` boş: ana oturum kaydı açınca doldurulur ya da `UMAMI_ID` derleme değişkeni). Liste anahtarı önerisi: `saat-tarih`.
- Durum: Coolify uygulaması **henüz yok** (uuid yok); ana altyapı oturumu açacak. Derleme türü Dockerfile, port 80.

## Kararlar
- Tasarım ilk günden iki temalı: açık ("kâğıt") ve koyu ("gece") palet var. Tek merkez: `src/styles.css` token'ları, kontrast `npm test` ile denetlenir.
- Satır içi stil/betik yok; CSP `script-src 'self' https://istatistik.bumba.tr`. Tema flash'ı harici, bloklayan `theme.js` ile önlenir.
- E-posta listesi formu: ortak servis CORS vermediği için JS'siz, doğrudan form gönderimi + `donus`. Sonuç `#liste-tamam` / `#liste-hata` ile döner; `liste-katil` Umami olayı döndüğünde tetiklenir.
- Çerez yok; tercihler localStorage'da (`sth-theme`, `sth-lang`, `sth-prefs`), gizlilik sayfasında açıklı. Çerez onayı penceresi yok.
- Saat tarayıcıdan okunur; sunucuya veri gitmez. Umami olaylarında arama sözcüğü gönderilmez, yalnız "sonuç var/yok".
- Dil: ana sayfada yalnızca kullanıcı dil düğmesiyle seçim yaptıysa (ve siteye dışarıdan geldiyse) kayıtlı dile yönlendirilir.
- Arama: istemci tarafı dizin (`/assets/search-<dil>.json`, derlemede üretilir), TR karakter ve büyük/küçük harf duyarsız.
- HSTS `max-age=86400` ile başladı (8 Ekim 2026). **Bir hafta sorun çıkmazsa `31536000` yap** ve tarihi buraya yaz. includeSubDomains/preload yok.

## SEO / GEO kaydı
- Başlık ve açıklamalar `src/i18n/*.json` içinde (`home`, `world`, `city`, `privacy` anahtarları); sayfa başına benzersiz, `npm test` denetler.
- Hedef sorgular: "saat kaç", "şu an saat kaç", "bugün ayın kaçı", "hafta numarası", "dünya saatleri", "<şehir>'da saat kaç" (TR); "what time is it", "world clock", "time in <city>" (EN).
- Şemalar: Organization (parentOrganization: Bumba Group), WebSite, WebPage, WebApplication (ana sayfa), BreadcrumbList (iç sayfalar), FAQPage (ana sayfa ve şehirler).
- `robots.txt`: herkese ve yapay zekâ botlarına açık (GPTBot, OAI-SearchBot, ChatGPT-User, ClaudeBot, Claude-SearchBot, Claude-User, PerplexityBot, Perplexity-User, Google-Extended, Applebot-Extended, Googlebot, Bingbot; OpenAI, Anthropic ve Perplexity belgelerinden doğrulandı, 8 Ekim 2026). `Sitemap:` satırı tam adres. `sitemap.xml` iki dilli (hreflang), `llms.txt` iki dilli, hepsi derlemede üretilir.
- Sosyal hesap yok: şemada `sameAs` yok (uydurulmaz).

## Altyapı
- Sunucu, Coolify, Traefik/HTTPS, Resend, R2 yedekleri, Umami: ana altyapı oturumunun işi. Bu proje veritabanı, kalıcı disk ve ortam değişkeni gerektirmez (isteğe bağlı: `SITE_ORIGIN`, `UMAMI_ID` derleme değişkenleri).
- Statik sitede başlıkları `nginx.conf` verir (HSTS, nosniff, Referrer-Policy, CSP). Ana oturum kendi nginx ayarını kuracaksa aynı başlıkları vermeli.

## Yetki
Standart işleri için main'e birleştirme ve canlıya alma konusunda sahibin kalıcı onayı vardır; PR aç, kontrolleri geçir, birleştir, yayını izle. Ortamın izin sistemi durdurursa aşma, PR adresini bildir. (Projede "sahibi demeden birleştirilmez" kuralı yok.)

---

# Genel Standartlar
**Sürüm 3.2 · 8 Ekim 2026** (Bumba Genel Standartlar; daha yeni sürüm gelirse bu bölümü bütünüyle onunla değiştir, sürüm satırını koru.)

Her madde zorunlu; varsa koru ve tamamla, yoksa oluştur. Tasarım ya da eski karar bir maddeye engelse maddeyi atlama, tasarımı/kararı değiştir ve burayı güncelle. Tek ayrık: bir ürünü ya da bilgiyi gizleyen kararlar korunur.

- **Bumba Group bilgileri:** tek doğru kaynak https://bumbagroup.com (kollar, ürünler, adresler). Farklıysa sitedeki bilgiyi ona göre düzelt.
- **Sabit bilgiler:** veri sorumlusu Bumba Teknoloji Limited Şirketi, Ataşehir / İstanbul · iletişim ve KVKK: bilgi@bumbagroup.com · e-posta göndericisi bilgi@bumbagroup.com (gönderen adı ürün adı) · Umami https://istatistik.bumba.tr · ortak liste https://bumbagroup.com/api/liste/katil · marka kiti https://bumbagroup.com/marka.
- **Sunucu:** tek sunucuda Coolify (https://panel.bumba.tr), Netverim VPS İstanbul, 194.62.52.201, Ubuntu 22.04, 4 vCPU, 6 GB RAM, ~12 GB boş disk, ~28 uygulama. SSH yok. Erişim yalnız Coolify API (`COOLIFY_URL`, `COOLIFY_API_TOKEN`; değerleri ve `/envs` içeriğini yazdırma). Yayın: Coolify izlenen dalı (genelde main) her push'ta GitHub'dan çekip derler; canlıya almak = main'e birleştirmek. Sunucu tek derleme yapar, yayın 2–15 dk, sıra https://durum.bumba.tr "Canlı" kartında. Önceki sürüm ve derleme önbelleği saklanır. Bağlı hesaplar: GitHub, Cloudflare (DNS, R2 yedek), Resend, Google Workspace gelen kutusu. Vercel, Neon, Supabase kullanılmıyor.
- **Ana altyapı oturumunun işleri** (kendin yapma, rapora yaz): yeni uygulama, alan adı ve DNS, www yönlendirmesi, ortam değişkeni, veritabanı, kalıcı disk, zamanlanmış görev, Umami site kaydı ve paylaşım kimliği, statik sitede nginx ve güvenlik başlıkları, ortak e-posta listesi, e-posta gönderim anahtarı, tanıtım/duyuru e-postası, İYS.
- **Coolify komutları:** uygulamayı bul: `curl -s -H "Authorization: Bearer $COOLIFY_API_TOKEN" "$COOLIFY_URL/api/v1/applications" | jq --arg d DEPO '.[] | select(.git_repository | ascii_downcase | endswith("/" + ($d | ascii_downcase))) | {name, uuid, fqdn, git_branch, build_pack}'` · son yayınlar: `.../api/v1/deployments/applications/$U?take=3` · hata günlüğü: aynı uçta `take=1` ve `.deployments[0].logs | fromjson` · yeniden yayın: `POST $COOLIFY_URL/api/v1/deploy {"uuid":"$U"}` (yalnız sırada/süren yayın yokken).
- **Altyapı kuralı:** önce sunucuda olanı kullan. Veritabanı merkezi PostgreSQL 18 (proje başına ayrı DB/rol; başka DB kurma). Kalıcı dosya için kalıcı disk iste. Yedek gece R2. Arama: statikte istemci tarafı dizin. İstatistik Umami. Liste ortak servis. Zamanlanmış iş CRON_SECRET'lı uç. Derlemede ağır iş yapma (tarayıcı/LLM/dış veri indirme), derleme yalnız repodakiyle çalışsın; ortak ayarlara dokunma.
- **E-posta:** liste e-postaları (onay, çıkış) ortak servisten gider. Kendi otomatik e-postaları Resend ile (bilgi@bumbagroup.com, gönderen adı ürün adı; anahtar ortam değişkeninde). Siteler tanıtım/kampanya/duyuru e-postası ve SMS göndermez (İYS, ortak servis).
- **1 Bumba imzası:** her sayfa altbilgisinde, telefon rozeti (düz metin değil), kol bumbagroup.com'daki gibi; tema düğmesi varsa açık ve `-koyu` iki img, temaya göre biri gizli; TR title "Bumba Life, bir Bumba Group kolu", alt "Bir Bumba Life ürünü"; EN "part of Bumba Group" / "A Bumba Life product"; genişlik 164, yükseklik 28. Birleştirmede rozet ve Umami sayacı silinmez. Kaynak: https://bumbagroup.com/marka.
- **2 SEO/GEO:** benzersiz title/description, tek H1, canonical, hreflang (tr, en, x-default), OG/Twitter, ikonlar, otomatik sitemap (iki dil) ve robots (`Sitemap:` tam adres), JSON-LD (Organization + parentOrganization Bumba Group, WebSite, BreadcrumbList, içeriğe göre Article/FAQPage/Service), alt metin/boyut/lazy-load, Core Web Vitals, gerçek 404. GEO: içerik JS'siz HTML'de, robots yapay zekâ botlarını engellemez (adları doğrula), `/llms.txt`, sayfa başında kısa özet ve Q&A, marka bilgisi her yerde aynı, güncelleme tarihi görünür. Yapılandırmayı bu dosyaya işle; Search Console/Bing doğrulaması ve sitemap gönderimi sahibin işi.
- **3 Tema:** sistem tercihine uyar, seçimi hatırlar, her sayfada düğme, flash yok, renkler tek merkezden, iki temada her bileşen okunur (WCAG AA).
- **4 Site içi arama:** görünen bütün içerik, tek sayfada bile; etkin dilde; TR karakter ve büyük/küçük harf duyarsız; klavye; boş durum; yeni içerikle dizin kendiliğinden güncellenir.
- **5 TR / EN:** bütün metinler çeviri dosyasından (e-posta, meta, alt, 404 dahil), her sayfada dil değiştirici (seçimi hatırlar), her dilin kendi URL'si, doğru `<html lang>`, varsayılan dil projenin dili, tarih/sayı dile göre, doğal İngilizce.
- **6 E-posta listesi:** ortak servis formu (alanlar: `site`, `eposta`, `dil`, `riza`, `kaynak`, `web_sitesi` bot tuzağı, JS'siz `donus`), onay kutusu işaretli gelmez, ortak aydınlatma metni (https://bumbagroup.com/liste/aydinlatma, EN `?dil=en`), iki dil ve iki tema, Umami `liste-katil` (adres olay verisine girmez). İYS merkezde.
- **7 Üyelik** ve **10 Panel:** yalnız sunuculu uygulamalar (bu proje statik: uygulanmaz).
- **8 HTTPS:** http→https 301 (Traefik, ana oturum; canlıda `curl -sI http://ADRES/` ile doğrula), HSTS zorunlu (`max-age=86400` ile başla, bir hafta sonra `31536000`, tarihi yaz, includeSubDomains/preload yok), www/www'suz biri asıl adres diğeri 301, karışık içerik yok, nosniff + Referrer-Policy + CSP (Umami script/connect; bumbagroup.com img/connect/form-action).
- **9 Umami:** `<script defer src="https://istatistik.bumba.tr/script.js" data-website-id="KİMLİK" data-domains="ASIL-ADRES">`; olaylar: kayıt, giriş, liste-katil, arama, dil değişimi, tema değişimi; URL ve olay verisinde kişisel bilgi yok; çerez yok → onay penceresi yerine çerez politikası. Yeni site için kayıt ve liste anahtarını ana oturum açar.
- **11 Taban kalite:** mobil uyum, erişilebilirlik (klavye, odak, etiket, alt), iki dilli ve iki temalı 404/500, sırlar repoda yok, girdiler sunucuda doğrulanır, KVKK (aydınlatma, gizlilik ve çerez politikası; aydınlatma yayında olmayan form canlıya çıkmaz).
- **12 Kalıcılık:** bu bölüm master dosyada durur; çelişen eski kararlar güncellenir, gizlilik kararları korunur.

## Kontrol listesi (her yeni ya da değişen sayfa/özellik için)
- [ ] TR ve EN metin (oyun ve uygulama ekranları dahil)
- [ ] Açık ve koyu tema
- [ ] SEO/GEO: meta, şema, sitemap, hreflang, llms.txt ve bu dosyadaki kayıt
- [ ] Erişim: herkese açık mı, üyeye özel mi, yöneticiye özel mi; buna göre index ya da noindex
- [ ] Arama dizini
- [ ] Umami: sayfa sayılıyor mu, yeni önemli eylem için olay tanımlı mı
- [ ] E-posta listesi formu
- [ ] Mobil uyum ve erişilebilirlik
- [ ] Alt bilgide Bumba rozeti (birleştirmelerde kaybolmadı mı)
- [ ] Push'lar toplu mu, derleme yalnız repodakiyle mi çalışıyor
