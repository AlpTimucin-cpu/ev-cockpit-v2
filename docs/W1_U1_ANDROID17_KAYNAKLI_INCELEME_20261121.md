# W1 / U1 — Android 17 / POCO F5 uyumluluk araştırması

Tarih: 21.11.2026 • Araç: POCO F5, LineageOS 24.0 (Android 17 / API 37, **kullanıcı beyanı**). Bu not **saha testi değildir**.

## Mevcut uygulama sürümü ve teknik kararlar
- `app.evcockpit`, `versionCode=44`, `versionName=0.3.21-stability`; `minSdk=28`, `compileSdk=36`, `targetSdk=35` (`app/build.gradle.kts` — **kaynakta doğrulandı**).
- Android 17 API 37 **çalışma zamanı** davranışları bazı yerlerde targetSdk'den bağımsız uygulanır. Hedef SDK'yı körlemesine yükseltme; API 37 derleme/cihaz uyumluluğu ayrıca test edilmelidir.
- Android 17 medya: uygulama görünür değilse ve geçerli foreground service yoksa audio focus/oynatma kısıtlıdır; navigasyon TTS ses odağı yalnız konuşmada alınır. **Cihaz testi gerekli.** Kaynak: https://developer.android.com/about/versions/17/behavior-changes-all
- Android 17 RFCOMM/Bluetooth socket davranışı, otomatik yeniden eşleştirme ve uygulama bellek sınırları değişebilir; OBD SPP sıralı okuma/yazma/iptal/EOF testini güçlendir. Kaynak: https://developer.android.com/about/versions/17/summary
- Yerel ağ erişimi için Android 17'yi hedefleyen uygulamalarda yeni izin davranışı olabilir; bugün targetSdk=35, HTTP/ORS/OSM internet bağlantısı ayrı, Bluetooth SPP ile karıştırılmamalı. Kaynak: https://developer.android.com/about/versions/17/summary
- Android Auto, Google Play Services, TTS: Kullanıcı cihazda **KURULU** dedi, uygulamanın API ile tespit etmesi ve erişilemezse `BİLİNMİYOR` gösterip AOSP yedeğinde çökmeden çalışması gerekli.
- Konum: FINE/COARSE, FGS location, bildirim izni manifeste ekli. Sistem ayarları yazma, overlay, MediaProjection, Accessibility, Shizuku, root **YOK**.
- SAF `OpenDocument` kullanılıyor; geniş dosya/depolama izni yok. Android 17 URI paylaşımında `FLAG_GRANT_READ_URI_PERMISSION` gibi açık yetkiler gerekebilir: https://developer.android.com/about/versions/17/behavior-changes-all
- Edge-to-edge / yatay ekran: mevcut `safeDrawing` ve `sensorLandscape` kodu gözden geçirildi; ekran ölçeği ve gesture-back **telefon testi DOĞRULANMADI**.

## 16 KB native hizalama — gerçek APK denetimi
Kanıt: [W0 sonrası main Actions](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44889877281) debug artefaktı. APK içindeki ELF `PT_LOAD.p_align` alanları okunarak **yalnız statik** denetlendi:

| ABI / kütüphane | PT_LOAD hizalaması | Durum |
| --- | ---: | --- |
| `arm64-v8a` MapLibre, DataStore, AndroidX Graphics | 16384 | **ELF 16KB UYUMLU — yalnız bu koşul doğrulandı** |
| `x86_64` MapLibre, DataStore, AndroidX Graphics | 16384 | **ELF 16KB UYUMLU — yalnız bu koşul doğrulandı** |
| `armeabi-v7a/libmaplibre.so` | 4096 | **16KB uyumsuz ELF segmenti** |
| `x86/libmaplibre.so` | 4096 | **16KB uyumsuz ELF segmenti** |

**ÖNEMLİ:** APK içi ZIP hizalamasına ilişkin `zipalign -c -P 16 4` sonucu bu U1 notunda henüz doğrulanmadı; U2'de CI'a eklenecek. `arm64` ELF başarılı olması POCO F5'te çalışma garantisi değildir. Harici kütüphanelere değişiklik yapılmadı. Kaynak: https://developer.android.com/guide/practices/page-sizes

## TTS ve WebView
- `TurkishNavigationSpeech`: `tr-TR` `setLanguage` sonucunu ve TTS hata kodunu değerlendiriyor; çevrimdışı Türkçe ses paketinin gerçekten kurulu olduğunu ayrı doğrulamıyor. `TextToSpeech.getVoices()` ve `Voice.isNetworkConnectionRequired` desteği denetlenebilir, ancak çevrimdışı ses varlığı `isLanguageAvailable` ile tek başına kanıtlanmaz. Kaynak: https://developer.android.com/reference/android/speech/tts/TextToSpeech.Engine
- WebView: `WebView.getCurrentWebViewPackage` Android API 26+ mevcut. YouTube IFrame mevcut uygulamada yalnız telefon yolcu görünümünde; IFrame error 153 gibi sorunlar kullanıcıya bildirilmeli. Kaynak: https://developer.android.com/reference/android/webkit/WebView
- Kullanıcı web kökeni URL'si veya oturum bilgisi güvenlik raporuna yazılmayacak.

## U2/U3/U4 için iş ayrımı
U2: CI 16KB statik + zipalign denetimi; manifest/izinleri kaynakla yeniden karşılaştırma; native ABI riski. U3: OBD Test'e bağlı güvenli sistem tanısı ve rapor, GMS/AA/TTS/WebView ve sayfa boyutu; kullanıcıya gerekirse TTS ayarlarını **sadece açma**. U4: tek adımlık telefon kabul kontrol listesi Issue; burada cihaz sonucu olmadan U1/U2/U3'ü **sahada doğrulandı** diye işaretleme.

**Kaynaklar:** yalnız resmî Android Developers belgeleri, repo Kotlin/manifest ve W0 main Actions APK içeriği. Üçüncü taraf Mola kaynakları/kodu eklenmedi.
