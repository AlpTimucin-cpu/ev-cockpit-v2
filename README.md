# ID.4 Yol Arkadaşı (ev-cockpit v2)

Kotlin + Jetpack Compose ile bağımsız Android uygulaması. Mola/Fermata kodu içermez.

## Aşama 1

- Klasik Bluetooth SPP ile ELM327; 29-bit CAN MEB PID tablosu (`app/src/main/assets/pids_meb.json`)
- **Klon toleransı:** Opsiyonel AT komutları (`ATCRA`, `ATFC*`, `ATAT1`, `ATST64`) desteklenmiyorsa uyarı loglanır, zorunlu protokol/header başarısızsa oturum durdurulup yeniden denenir.
- **Dayanıklılık:** Cevap istemi (`>`) zaman aşımı, soket kapanması ve diğer IO hatalarında 1/2/4/8/16/30 saniyeye kadar kontrollü yeniden bağlanma.
- **OBD Test:** ham komut/yanıt kayıtları, FileProvider ve Android paylaşım penceresi ile log gönderme. Sol kontrol paneli kaydırılabilir; log paylaşımı/kayıt yolu ekran boyundan bağımsız erişilebilir. Eşleşmiş cihaz yoksa Bluetooth ayarlarına geçiş düğmesi gösterilir.
- **Ekran güvenli alanı:** `WindowInsets.safeDrawing` ile sekmeler durum çubuğu, navigasyon çubuğu ve ekran kesimi alanlarından uzak tutulur. Fiziksel cihaz arayüz testi henüz yapılmadı.
- **Foreground service:** Android 14+ `connectedDevice` türü ile OBD oturumu Activity'den bağımsız. Android 12+ Bluetooth Connect izni, Android 13+ bildirim izni istenir.
- **Gösterge:** hız / çekiş veya rejenerasyon güç çubuğu / batarya %, ayrı demo veri modu.
- **Trafik özelliği yoktur ve eklenmeyecektir.**

**Doğrulama sınırı:** CI testleri ve APK derlemesi araç testi değildir. ELM327 klon davranışı, gerçek PID sonuçları, soket kopma senaryoları ve foreground service davranışı **telefon/araç üzerinde henüz doğrulanmadı**.

## Aşama 2 / İterasyon 1 — online harita (PR-1)
- MapLibre Native Android 11.13.0 + OpenFreeMap Liberty çevrimiçi stil entegrasyonu. Türkiye merkezli başlangıç görünümü kullanır; **gerçek GPS konumu değildir**.
- Harita internet gerektirir; **PMTiles/BRouter yoktur**. OpenFreeMap çevrimdışı karo indirme özelliği uygulanmadı.
- Haritanın cihazda görüntülenmesi henüz test edilmedi. OSM/OpenFreeMap atıfları korunmalıdır.
- OpenFreeMap TOS (2026-10-14): https://openfreemap.org/tos/

## Aşama 2 / İterasyon 1 — API anahtarı (PR-2)
- Kullanıcı kendi HeiGIT/openrouteservice API anahtarını **Rota Ayarları** ekranına girer.
- Anahtar APK'ya/BuildConfig'e gömülmez. DataStore'da yalnızca AES-256-GCM ile şifreli metin+IV saklanır; Android Keystore'daki anahtar dışarı aktarılmaz.
- Şifreleme/geri okuma fiziksel Android cihazda **doğrulanmadı**; cihazın ele geçirilmesine karşı mutlak gizlilik taahhüdü yoktur.
- Yeni HeiGIT API host: `https://api.heigit.org/openrouteservice/`. Kullanıcı kendisi https://account.heigit.org/ hesabında anahtar oluşturur.
- https://giscience.github.io/openrouteservice/frequently-asked-questions.html

## Aşama 2 / İterasyon 1 — RouteProvider + openrouteservice (PR-3)
- `RouteProvider` arayüzü ve `OrsRouteProvider` ile sabit HTTPS `api.heigit.org/openrouteservice/v2/directions/driving-car/geojson` yolu.
- Rota isteğinde kullanıcının kendi anahtarı sadece `Authorization` başlığına yazılır; yönlendirme isteği bilinmeyen sunucuya takip edilmez. Türkçe `language=tr`, `instructions=true`.
- GeoJSON LineString koordinatları [lon,lat], toplam metre/saniye ve Türkçe manevraları ayrıştırılır; hata kodlarında anahtar veya yanıt gövdesi loglanmaz.
- Birim testleri **örnek (sentetik) JSON** üstündedir; HeiGIT anahtarı olmadan gerçek rota/API erişimi **doğrulanmadı**.
- Varsayılan ücretsiz Directions kotası 2000/24 saat, 40/dk; lisans ve koşulları kontrol edin: https://giscience.github.io/openrouteservice/frequently-asked-questions.html

## Aşama 2 / İterasyon 1 — GPS + hız yedeği (PR-4)
- Kullanıcı **Rota Ayarları > GPS takibini başlat** düğmesine basar; fine+coarse konum izni o anda istenir.
- Android LocationManager GPS_PROVIDER (ve geçerli ağ konumu yedeği) + `location` foreground service ve bildirim.
- OBD hızı mevcutsa öncelikli fakat PID `verified=false` olduğu için **OBD — PID doğrulanmadı** yazısı; OBD hızında veri yoksa son 15 saniyede alınmış geçerli GPS hızına düşer. Demo her zaman **DEMO** etiketlidir.
- GPS konumu/batarya/araç testleri **henüz fiziksel cihazda doğrulanmadı**.
- https://developer.android.com/develop/background-work/services/fgs/service-types

## Aşama 2 / İterasyon 1 — Rota paneli ve varış enerjisi (PR-5)
- Kullanıcı `Rota` sekmesinde hedef enlem/boylam girerek `RouteProvider` üzerinden açıkça rota hesaplatır; GPS mevcut ve tazeyse, varsa şifreli saklanan kişisel HeiGIT anahtarı çözülüp yalnızca istekte kullanılır.
- Türkçe manevra paneli, toplam rota km/süre, canlı GPS konumunun rota çizgisine uzaklığına göre yaklaşık kalan km, rota dışındaysa bilgi **gösterilemez**.
- `Varış enerjisi (TAHMİN)` formül: `usableKWh × SoC/100 − remainingKm × userAverageKWhPer100Km / 100`. SoC/ortalama tüketim/kalan mesafe yoksa sonuç **yok**; enerji değeri kesin değil.
- Geçerli bir API anahtarı ve cihaz/rota testi **henüz doğrulanmadı**. OpenFreeMap'e rota çizgisini çizme ve TTS sonraki ayrı PR.
- Canlı trafik, otomatik geocoding, BRouter/PMTiles, Android Auto özellikleri yok.

## Aşama 2 / İterasyon 1 — harita rota çizgisi (PR-6)
- Gerçek ORS GeoJSON geometry alındığında MapLibre haritada çizgi gösterir; **sahada henüz doğrulanmadı**.
- Harita uzun basışında gerçek seçilen koordinat hedef olur; Rota sekmesi hedef alanına aktarılır. Uygulama kendiliğinden rota istemez.
- Harita üzerindeki manevra kutusu yalnızca rota ve taze/rotaya yakın GPS ile görünür.
- MapLibre legacy annotation API bu ilk iterasyonda kullanıldı; ileride GeoJSON source/layer'a geçiş değerlendirilir.

## CI / telefondan APK indirme

- [Android CI](../../actions/workflows/android.yml): Gradle **8.10.2**, Java 17, `:app:testDebugUnitTest`, `:app:assembleDebug`.
- Sadece başarılı `main` CI tamamlanınca `nightly` ön sürümündeki aynı `app-debug.apk` güncellenir.
- [Nightly debug APK](../../releases/download/nightly/app-debug.apk)
- APK debug imzalıdır ve mağaza sürümü değildir; CI ortamları arasında debug imzası değişirse üzerine kurulum mümkün olmayabilir.
- Wrapper hâlâ yoktur; Gradle GitHub Actions üzerinde kurulur.

## Yol haritası

1. [CI / gitignore](../../issues/1)
2. [Nightly yayın](../../issues/2)
3. [ELM327 dayanıklılığı / log paylaşımı](../../issues/3)
4. [Aşama 2 harita/rota araştırması](../../issues/4) — kullanıcı onayı bekler
5. Media3 video penceresi / kullanıcı tarafından ayarlanan hız kilidi
6. ID.4 ekranına yansıtma fizibilitesi — yalnızca araştırma
7. Şarj, hava ve motor sesi

## Sınırlar

- MEB PID'lerinin tümü **`verified=false`** kalır; scale/offset kendi araç logu olmadan değiştirilmez.
- Topluluk kaynaklı `rx 17FE007B` varsayımı ve diğer UDS yanıtları gerçek araçta doğrulanmadı.
- ELM327 cihazı başka uygulamayla aynı anda kullanılamaz.
- `app.evcockpit`, `minSdk 28`, tek Android modülü (`app`).
- Navigasyon foreground service ve GPS konum izinleri, Aşama 2 onayından sonra tasarlanacaktır.
