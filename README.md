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

## Aşama 2 / İterasyon 1 — Türkçe sesli tarif (PR-7)
- `Rota` sekmesinden **kullanıcı tercihiyle** Türkçe TTS aç/kapat; varsayılan kapalı. Gerçek GPS rota üzerinde taze ise 200 m / 50 m yaklaşımlarında birer kez manevra seslendirme ve elle sıradaki manevrayı okutma.
- Android `TextToSpeech` `tr-TR`, Türkçe dil paketi yoksa ses verilmez ve durum mesajı gösterilir. Geçici `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` alınır, sözcük bitince bırakılır.
- AudioFocus/TTS ve navigasyon **telefon ve araç üzerinde henüz doğrulanmadı**. Uzun yol boyunca ekran kapalı GPS servisi ve ses motoru davranışları saha testine tabidir.
- BRouter, PMTiles, Car App Library ve canlı trafik iterasyona dahil değildir.

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
4. [Aşama 2 harita/rota araştırması](../../issues/4) — İterasyon 1 onaylandı (harita/rota/TTS geliştirmesi), cihaz kabul testi henüz yapılmadı
5. Media3 video penceresi / kullanıcı tarafından ayarlanan hız kilidi
6. ID.4 ekranına yansıtma fizibilitesi — yalnızca araştırma
7. Şarj, hava ve motor sesi

## Sınırlar

- MEB PID'lerinin tümü **`verified=false`** kalır; scale/offset kendi araç logu olmadan değiştirilmez.
- Topluluk kaynaklı `rx 17FE007B` varsayımı ve diğer UDS yanıtları gerçek araçta doğrulanmadı.
- ELM327 cihazı başka uygulamayla aynı anda kullanılamaz.
- `app.evcockpit`, `minSdk 28`, tek Android modülü (`app`).
- Konum foreground service yalnızca kullanıcı başlatmasıyla çalışacak şekilde eklendi; telefon üzerinde doğrulama bekleniyor.

## M1 — Kontrollü OBD tanı modu (saha DOĞRULANMADI)
- OBD Test > Tanı modunu çalıştır: eşleşmiş adaptörle normal sorgulamayı kesip ayrı obd_diagnosis_*.txt dosyası oluşturur. Her komut ham yanıt ve yaklaşık süreyle kaydedilir; bir ekranlık özet ve paylaşım bulunur.
- ELM bilgileri: ATZ, ATI, ATRV, ATDP, ATDPN. Standart SAE OBD-II: ATSP0 + 0100/0120/0140/0902/010D; 11-bit ATSP6 + 0100; ATSP7/ATH1 + 3 sn ATMA/CR; 29-bit MEB başlık varyantları ve 11-bit ECU örnekleri.
- ATMA istem (>) dönmezse soket kapatılıp en fazla bir defa yeniden bağlanma denenir. Kırpılmış/eksik tanı sonucu başarı sayılmaz. ATMA çerçevesi yoksa Ready bilinmiyorsa donanım arızası kanıtlanmış değildir.
- 5 art arda tam PID turunda hiçbir yanıt alınmazsa sürekli sorgu durur; Bluetooth soketi açık bekler ve kullanıcıya Tanı modu önerilir.
- NO DATA satırları PID başına sayılarak 22028C NO DATA x5 biçiminde özetlenir. Normal ve tanı logları 1 MB üst sınıra sahiptir (sınır aşıldığında yeni veri eklenmez).
- Tanıda pozitif 41 0D varsa ve MEB yoksa yalnız SAE 010D hız okuyan Standart mod iletişim profili kaydedilir. 29-bit ECU pozitifse başlık profili kaydedilir. Bu profiller PID verified durumunu değiştirmez.
- UYARI: Tanıdaki 0902 yanıtı VIN içerebilir. Ham tanı dosyasını paylaşmadan önce kontrol edin; M3 güvenli test raporunda VIN ve koordinat bulunmayacak.
- 29-bit 8 hane kaynak: https://github.com/nickn17/evDash/blob/master/src/CarVWID3.cpp
- 29-bit ATCP17 / 6 hane / CAF0 kaynak: https://github.com/Iternio-Planning-AB/ev-obd-pids/blob/main/volkswagen/MEB.json
- 11-bit 710 kaynak: https://cellpulse.app/blog/meb-soh-atsp6
- ELM AT komut söz dizimi: https://docs.meatpi.com/wican-obd/reference/elm327-commands/
- Fiziksel ID.4 ve Temu ELM327 üzerinde sonuç DOĞRULANMADI.

## M2 — Kalıcı debug imzası (yalnızca test APK)
- CI, depodaki **kamusal EC P-256 TEST sertifikasını** (ci/public-debug-ec.p12.b64) kullanır. Bu anahtar gizli değildir; herkes APK'yı aynı sertifikayla yeniden imzalayabilir. **Üretim/Play Store için kullanılmaz.**
- GitHub Actions base64 dosyasını geçici konuma çözer, Gradle debug signingConfig ile imzalar, Android SDK apksigner verify --print-certs çalıştırır ve bilinen SHA-256 sertifika parmak izini kontrol eder.
- Sabit sertifika SHA-256: 82:9F:81:DE:4A:87:11:1E:0B:7B:50:52:B8:7D:E7:7B:03:38:59:FB:D0:3D:45:CB:38:43:3C:F5:7C:7B:89:FB.
- Önceki rastgele debug imzası ile imzalanmış APK'dan ilk kez geçerken Android 'Uygulama yüklenmedi' diyebilir; önce veri yedeği al, gerekirse eski uygulamayı kaldır ve yeni APK'yı kur. Bundan sonraki aynı paket+imza derlemeleri üzerine kurulabilir (Android sürüm kodu artmalı).
- Aynı parmak izinin iki ayrı GitHub CI çalışmasında doğrulanması zorunludur; fiziksel Android yüklemesi ayrı saha testidir.

## M3 / PR1 — Manuel SoC ve veri kaynağı
- Rota Ayarları ekranında kullanıcı araç göstergesinden 0-100 % SoC girebilir veya kaydı silebilir. Preferences DataStore içinde kalıcı saklanır, API anahtarı gibi bir sır değildir.
- SoC kaynağı: OBD pozitif ve geçerli ise OBD (PID DOĞRULANMADI), aksi halde manuel, ikisi yoksa veri yok. Demo açıkça DEMO olarak kalır.
- Gösterge ekranı ve varış kWh TAHMİN aynı SoC kaynak seçimini kullanır; kullanıcı ortalama tüketimini girmeden kWh uydurulmaz. Formül değişmedi.
- GPS hız yedeği ve mevcut hız kaynağı etiketi korunur. SoC kaynak tercihi ile MEB PID verified işaretleri ilişkisizdir; verified=false kalır.
- Manuel girişin fiziksel Android DataStore kalıcılığı ve araç ekranı karşılaştırması DOĞRULANMADI.

## M3 / PR2 — Arka plan rehberi ve GÜVENLİ test raporu
- Rota Ayarları > pil optimizasyonu ve otomatik başlatma / uygulama bilgileri: resmî Android Settings intent'leri açılır; Xiaomi/POCO cihazlarında bilinen MIUI bileşenine erişme denenir, bulunamazsa Android uygulama detaylarına düşer. Ayar otomatik değiştirilmez. Kaynaklar: https://developer.android.com/reference/android/provider/Settings ; https://www.mi.com/uk/support/user-guide-pdf/generic-user-guide-for-hyperos-3/
- OBD Test > **Güvenli test raporunu dışa aktar**: uygulama sürümü/paket, Android SDK ve model, Bluetooth/konum/bildirim izinleri, son Tanı özeti ve son 200 OBD log satırı.
- Test raporu kaynağı yalnız OBD logudur; GPS/Route/Keystore API anahtarları okunmaz. VIN 0902, koordinat dizileri, token/Authorization/API key şüpheli satırlar tamamen gizlenir. **Ayrı orijinal ham tanı logu gizlilik filtresi uygulanmaz**; paylaşmadan önce kontrol edin.
- Rapor Android FileProvider ve sistem paylaşım seçicisiyle açılır; fiziksel telefonda dışa aktarma ve OEM ayarları DOĞRULANMADI.
- M1, M2, M3 ardından Durma Noktası 1: araç park halindeyken Ready, adaptör tam oturmuş şekilde tanı çalıştırılıp güvenli test raporu ve gerekiyorsa kontrollü ham tanı dosyası paylaşılacak. M4-M7 için saha sonucu / ayrıca onay beklenir.

## Araç bağımsız İterasyon 1 — yerel hata günlüğü
- Yakalanmamış istisna sınıfı, zaman ve sadece sınırlı/temizlenmiş uygulama stack frame'leri yerel crash.log dosyasına yazılır. İstisna mesajı, URL, anahtar, VIN ve koordinat dahil edilmez. 64 KiB sınırı.
- Güvenli test raporuna son hata kaydı mevcut gizleme filtresinden geçirilerek eklenir. Fiziksel Android üzerinde istisna yakalama **DOĞRULANMADI**.

## Araç bağımsız İterasyon 2 — atıf ve lisans
- Harita yüzeyinin sağ altında © OpenStreetMap katkıcıları / OpenFreeMap metni görüntülenecek şekilde kodlandı (fiziksel cihaz DOĞRULANMADI).
- Projeye MIT LICENSE ve bağımlılık kaynaklarıyla NOTICE eklendi. MapLibre Native BSD-2-Clause; AndroidX/Kotlin Apache-2.0; OSM veri ODbL. Transitive APK lisanslarının tam analizi ayrıca gerekebilir. Mola/Fermata GPL kaynak kodu depoya kopyalanmadı.

## Araç bağımsız İterasyon 3 — Photon adres arama, son hedef ve Ev/İş
- Photon (komoot) kamusal demo API, sadece kullanıcının Arama düğmesine basmasıyla HTTPS talebi; 10 saniyelik asgari istek aralığı ve en fazla 5 sonuç, User-Agent ile uygulama kimliği. Kota garantisi yok, yüksek trafikli/ücretli üründe farklı anlaşma veya kendi sunucusu gerekir.
- Kaynak: https://github.com/komoot/photon/blob/master/README.md ; https://github.com/komoot/photon/blob/master/docs/usage.md . Son hedefler (en fazla 10) ve Ev/İş kayıtları sadece uygulama DataStore içinde; koordinatlar debug/güvenli test raporlarına eklenmez. Gerçek API ve Android testi DOĞRULANMADI.

## Araç bağımsız İterasyon 4 — Nightly uygulama içi bildirim
- Ayarlar ekranına girilince ve kullanıcı düğmeye basınca halka açık GitHub Releases nightly sürümü kontrol edilir. API anahtarı kullanılmaz; 15 dakikalık otomatik oturum önbelleği vardır.
- Yalnız beklenen repo/tag/app-debug.apk yolu ve 40 haneli commit doğrulanır; yeni Nightly farklı ise indirme bağlantısı açılır. APK otomatik indirilmez veya kurulmaz.
- Kaynak: https://docs.github.com/en/rest/releases/releases#get-a-release-by-tag-name . Android uygulama içi bildirim ve gerçek ağ sonucu **DOĞRULANMADI**.

## Araç bağımsız İterasyon 5 — tema ve ekranı açık tut
- Koyu/açık Material3 tema tercihi DataStore ile kalıcı. İlk varsayılan koyu tema.
- Ekranı açık tut (Android FLAG_KEEP_SCREEN_ON) tercihi varsayılan kapalı. Kullanıcı ayarından açılabilir, yalnız Activity aktifken uygular. Telefon ve pil etkisi DOĞRULANMADI.

## Yolcu modu M6 / PR-çekirdek (yalnız telefon)
- Media3 1.10.0 ExoPlayer, tek ViewModel oturumu; SAF content:// ve DRM'siz doğrudan HTTPS URL; YouTube yalnız resmî IFrame video kimliği. Netflix/Disney+/Prime yok, Android Auto video yok.
- AudioFocus Media3 otomatik yönetim, navigasyon Türkçe TTS'nin MAY_DUCK odağı ile bir arada. Gerçek telefon ses kısma davranışı DOĞRULANMADI.
- Hız kilidi tercihi default false, kullanıcı açıkça seçmeden video hıza göre kapatılmaz.
- Resmî kaynak: https://developer.android.com/jetpack/androidx/releases/media3 ; https://developers.google.com/youtube/iframe_api_reference

- CI düzeltmesi: Actions AAR metadata 9 hatada Media3 1.10.0 için compileSdk≥36 gerektiğini kanıtladı. Resmî AGP 8.10.1 + Gradle 8.11.1 ve compileSdk36 kullanılır; targetSdk35/minSdk28 korunur. Kanıt: https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44709546273 ; https://developer.android.com/build/releases/agp-8-10-0-release-notes

## Yolcu modu M6 / PR-arayüz (yalnız telefon)
- Yolcu video sekmesinde SAF yerel dosya, doğrudan HTTPS ve YouTube resmî IFrame (WebView) girişi; DRM/Netflix/Disney+/Prime yönlendirmesi/çözmesi yok.
- Tam ekran (varsayılan), bölünmüş (video üstte, harita altta), yüzen (sürüklenebilir) düzenler DataStore ile kalıcı. Media3 player ViewModel'da tutulur, böylece Compose düzen değişiminde oynatma pozisyonunu korumak hedeflenir (sahada DOĞRULANMADI).
- Video hız kilidi kullanıcı ayarıdır, varsayılan kapalı; kullanıcı açmadıkça hıza göre video durdurulmaz. Kulaklık ipucu mevcut. AudioFocus/MAY_DUCK ses kısma, YouTube WebView ve arka plan hizmetlerinin fiziksel davranışı DOĞRULANMADI.
- Araç ekranı Android Auto video yok, planlanmıyor. Kaynak: https://developers.google.com/youtube/iframe_api_reference

## Araç bağımsız İterasyon 7 — rota üzeri hava tahmini (Open-Meteo)
- Yalnız kullanıcı rota hesapladığında en fazla üç örnek nokta (başlangıç/orta/hedef) Open-Meteo Forecast API'ye gönderilir. GPS veya rota arka planda sürekli sorgulanmaz. Kullanıcı rota koordinatlarının üçüncü taraf tahmin servisine iletildiğini bilir; API anahtarı gerekmez.
- Ana ekrandaki harita üstünde kısa Open-Meteo (TAHMİN) sıcaklık ve hava özeti; veri yoksa DOĞRULANMADI. Meteoroloji, GPS ve cihaz gerçek API yanıtı DOĞRULANMADI.
- Ücretsiz API yalnız ticari olmayan kullanım içindir, günlük 10.000 çağrı sınırı ve SLA garantisi yok; ticari kullanım lisansı ayrı. Veri CC BY 4.0 atıf gerekir. Kaynak: https://open-meteo.com/en/terms ; https://open-meteo.com/en/pricing ; https://open-meteo.com/
- Durma Noktası 1 sürer; M4/M5, motor sesi ve şarj durağı katmanı eklenmedi.

## M8.0 — GPS başlangıç, kullanıcı seçimiyle hedef
- Rota ekranında adres arama, Ev/İş ve son hedefler ile haritada uzun basarak hedef seçme korunur. Koordinat girişi sadece Gelişmiş bölümündedir; alanlar −90..90 / −180..180 doğrulanır. Rota hesapla seçilmiş hedef olmadan etkin olmaz.
- CI JUnit XML test sayısını gösterir. Fiziksel telefon: DOĞRULANMADI. M4/M5 Durma Noktası 1 devam eder.

## M8.1 — kaynaklı araç profili
- Saf Kotlin VehicleProfile, ModelParameters; her değer source, verified ve note taşır; tüm araç varsayımları false. Profil DataStore JSON kaydı/reset API'si hazır, düzenleme paneli M8.6. Doğrulama defteri: [docs/M8_DOGRULAMA.md](docs/M8_DOGRULAMA.md). Cihaz ve araç DOĞRULANMADI.

## M8.2 — Fiziksel enerji tahmini
- Saf Kotlin PhysicsConsumptionModel, aerodinamik/yuvarlanma/eğim/kapalı-oto-ısıtma/yardımcı yük, negatif eğimde sınırlı rejen, düşük hızda duruş süresi, soğuk çarpan ve belirsizlik bandı. Rüzgâr ve ivmelenme v1'de yok. Parametreler kaynaklı ve araç DOĞRULANMADI.

## M8.3 — Rota/eğim/hız
- ORS Directions GeoJSON elevation=true; 3D yükseklik varsa öncelik, yoksa kullanıcı plan isteğiyle Open-Meteo Elevation (100 koordinat/istek, en çok 3 istek). 400m örnekleme en fazla 4000 nokta, DEM seyrek arası enterpolasyon; eğim yoksa EĞİM VERİSİ YOK uyarısı, asla doğrulanmış diye sıfır eğim sunulmaz. GPS/rotada gerçek test DOĞRULANMADI.
- Sağlayıcı ortalama hızı baz alır; yol sınıfı/satır bazlı hız yoksa kanıtsız Türkiye hız limiti uygulanmaz.

## M8.4A — OSM şarj veri ayrıştırma
- Overpass node/way/relation center, CCS2 tagleri, gücü, operatör, access, fee, opening_hours ve authentication alanları, 75m dedupe, filtreler ve ayrı kaynak/güven. Saha/gerçek istasyon DOĞRULANMADI.
- [Şarj verisi güven notu](docs/M8_SARJ_VERI.md). Canlı doluluk/fiyat YOK.
