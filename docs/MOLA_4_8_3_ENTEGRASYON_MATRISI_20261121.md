# Mola 4.8.3 → EV Cockpit: değişikliksiz eşlikçi ve bağımsız geliştirme haritası

## Doğrulanan kaynaklar
- Yayın deposu: https://github.com/moris545454/mola-apk
- Sürüm: https://github.com/moris545454/mola-apk/releases/tag/v2026.11.20.3
- APK: `mola-asistan.apk`, 118.743.235 bayt; SHA-256: `124e9121c41a624893b0f0794854f3d5ba4fc8a29a15b051c3f4bc376d8fd149`.
- Kullanıcının sağladığı APK'nın SHA-256 değeri GitHub sürümünün ilan edilen SHA-256 değeriyle aynıdır.
- APK manifesti: paket `me.app.fermataX.auto.mola`, `versionName=4.8.3`, `minSdk=28`, `targetSdk=36`.
- Yayın deposunda yalnızca README.md (ve Releases altında ikili APK'lar) bulunuyor; Mola uygulamasının kaynak dosyaları / açık yeniden kullanım lisansı yayımlanmıyor.
- APK incelemesi statiktir. Gerçek POCO F5, LineageOS ve VW ID.4 testleri yapılmadı.

## Sınır ve prensip
Derlenmiş üçüncü taraf APK/DEX/yerel .so dosyaları EV Cockpit kaynak koduna aktarılmaz. Mola ve EV Cockpit bağımsız uygulamalardır; APK'nın tüm sınıfları "aynen" bir Android projesine birleştirilemez: Android manifest, paket kimliği, uygulama bağlamı, yaşam döngüsü, izinler, native kütüphaneler, servisler ve imzalar farklıdır. Mola'nın orijinal kodlarına ve paketine dokunulmuyor. Mola'ya ait özel anahtarlar / servis uç noktaları bizim uygulamaya gömülmüyor.

## Bu dalda gerçekleştirilen tek çalışma zamanı entegrasyonu
- `passenger/MolaCompanionLauncher.kt`: Android PackageManager üzerinden **ayrı kurulmuş** Mola uygulamasının normal launcher intent'ini açar; kurulu değilse kontrollü sonuç döndürür.
- `ui/PassengerVideoScreen.kt`: `Mola'yı aç (harici)` düğmesi, hata ve bağımsız-app açıklaması.
- `AndroidManifest.xml`: Android 11+ paket görünürlüğü için `<queries><package android:name="me.app.fermataX.auto.mola" /></queries>`.
- EV Cockpit'in Media3 oynatıcısı, YouTube IFrame'i, navigasyonu, OBD yapısı, şarj planlaması ve mevcut düzenleri değiştirilmedi.
- Mola'nın sahip olmadığı telefon verisi / araç SoC için entegrasyon uydurulmadı.
- Bu launcher ayrı Mola APK'sını *telefon* üzerinde açar; Android Auto'da video yüzeyi kazandırmaz ve hareket hâlindeki video sınırlamalarını kaldırmaz.

## APK'da tespit edilen bileşenlere göre kapsamlı özellik ayrıştırması
| Mola APK kanıtı | İşlev | EV Cockpit karşılığı / eylem | Bu dalda durum |
| --- | --- | --- | --- |
| `CarService_mola`, `MainCarActivity` | Android Auto kokpit | Ayrı Car App prototipi, platform kurallarına uygun şablonlar | Eklenmedi; Mola kodu kopyalanmadı |
| `MapSplitScreen`, `SidePanelView` | Bölünmüş Mola/harita ekranı | Compose kokpit + MapLibre; mevcut FULL/SPLIT/FLOATING telefon düzenlerini koru | Mevcut bağımsız düzen korunuyor |
| `PhoneScreenView`, `InlineMirrorView`, `MirrorSurfaceLease` | Telefon görüntüsü/yansıtma | Yetki, gizlilik, cihaz ve platform destek analizi gerektirir | Eklenmedi |
| `ProjectionService`, `PhoneDisplayService` | MediaProjection / kontrol | Ayrı teknik ve kullanıcı izni değerlendirmesi | Eklenmedi |
| `AccessibilityEventDispatcherService`, Shizuku | Dokunma enjeksiyonu / ayrıcalıklı işlemler | Geniş erişim talep edilmez; sessiz kontrol uygulanmaz | Eklenmedi |
| `MolaTubeWebView`, `YoutubeAddon` | YouTube erişimi | Mevcut resmî YouTube IFrame yolculuğu korunur | Mevcut bağımsız kod |
| `libvlc`, Media3 | Yerel video / ağ medyası | Media3 zaten var; gerekçelendirilmiş lisanslı format desteği ayrıca değerlendirilebilir | Yeni VLC kopyalanmadı |
| `TvAddon`, M3U / Xtream / Stalker sınıfları | IPTV | Kullanıcının yetkili olduğu akışlar için bağımsız M3U mimarisi | Eklenmedi |
| `RadioAddon`, Radio Browser | Radyo | Bağımsız açık Radio Browser istemcisi, MediaSession | Eklenmedi |
| `SpeechToText`, `TextToSpeech`, Vosk TR | Türkçe sesli komut | Asistan komutları, isteğe bağlı özgür/lisanslı ASR modeli, TTS | Eklenmedi |
| Gemini / OpenAI / Groq / OpenRouter dize izleri | AI asistan sağlayıcıları | Kullanıcı tarafından girilen anahtarlarla güvenli entegrasyon | Eklenmedi |
| Nominatim / OSM izleri | Arama ve harita | Mevcut Photon, ORS, MapLibre altyapısı korunur | Mevcut bağımsız kod |
| `UpdateCheckJob` / `UpdateDownloadReceiver` | Güncellemeler | Mevcut NightlyUpdateChecker korunsun; hash/imza doğrula | Değiştirilmedi |
| `SubtitlesView`, `EqualizerCurveView` | Altyazı, ekolayzer | Media3 ile bağımsız yolcu medya iyileştirmesi | Eklenmedi |
| `PhoneScreenView.closeForSpeed` | Hız durumuna yönelik işlem | Android Auto sürüş kısıtlarını aşma amacıyla kod kopyalanmaz | Eklenmedi |

## Doğrulama kriterleri
1. Mola kurulu değil: buton kullanıcıya "kurulu değil" mesajı gösterir ve EV Cockpit oynatıcı çalışmaya devam eder.
2. Mola kurulu: buton ayrı Mola uygulamasını başlatır; Mola kendi imzası, izinleri, kimliğiyle çalışır.
3. POCO F5 / LineageOS: launcher görünürlüğü, ekran yönü, geri dönüş ve Android yaşam döngüsü manuel test edilir.
4. GitHub Actions: `testDebugUnitTest` ve `assembleDebug` doğrulanmalıdır. Bu belge testin geçtiğini iddia etmez.
5. Araçta sürüş hâlinde video, DRM aşma, Shizuku, erişilebilirlik ve otomatik MediaProjection izni bu değişiklikle etkinleştirilmez.

## İlerideki bağımsız geliştirme kararları
Mola'nın kodunu veya API anahtarlarını içe aktarmak yerine, işlevleri ayrı lisanslı ve test edilebilir Kotlin modülleri olarak geliştirmek gerekir. En yüksek öncelik: Radio Browser destekli radyo / MediaSession, ardından kullanıcı izinli içerikler için IPTV, ardından düşük kaynak tüketimli Türkçe sesli komut. Her modül için kaynak lisansı, API koşulları, Android Auto izinleri, sürüş güvenliği ve POCO F5 testi ayrı onay gerektirir.

**Sürüm politikası:** Mola APK'sı değiştirilmez, otomatik indirilmez, EV Cockpit içine gömülmez veya yeniden imzalanmaz.
