# EV Cockpit — A0 · 0.7.0: Tam kaynak kod indeksi ve kabul raporu

**Kaynak dalı:** `feature/a0-f1-product-main-20261124`  
**Baz:** `fix/f1-ui-polish2-20261124` (F1 0.6.3, versionCode 50)  
**A0 sürümü:** `0.7.0` (versionCode 51)  
**PR:** [#118 — F1 ana ürün](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/pull/118)

## Değişen tüm dosyalar — EKSİKSİZ sürümler

Bu işte **20 yeni/değişen kod/kaynak dosyasının TAM içeriği** ve **4 silinmiş dosyanın temel daldaki eski TAM içeriği**, satır atlamadan şu üç belgede bulunur:

1. **[Bölüm 1: Ana Activity, NavHost, Ayarlar, Gradle, manifest, CI ve OBD](./A0_070_TAM_KAYNAKLAR_BOLUM_1.md)** — 7 dosya.
2. **[Bölüm 2: Demo / gerçek mapper ayrımı, debug Preview, çeviri kaynakları](./A0_070_TAM_KAYNAKLAR_BOLUM_2.md)** — 7 dosya.
3. **[Bölüm 3: Release/demo testleri ve silinen ekranların eski içerikleri](./A0_070_TAM_KAYNAKLAR_BOLUM_3.md)** — 6 güncel/test + 4 silinmiş dosya.

Her kod bloğunun üzerinde **tam depo yolu** bulunur. Kullanıcının bilgisayarı gerekmez; kodların tamamı GitHub'daki PR dalına commit edilmiştir.

## Önce oku: gerçek dosya/başvuru haritası

| Dosya | Başlangıçtaki rol | A0 kararı |
| --- | --- | --- |
| `MainActivity.kt` | `when(tab)` ve ilk `TeslaDashboardScreen` | F1'i `NavHost` başlangıç hedefi yapan giriş |
| `ui/DashboardScreen.kt` | Önceki gösterge, görülen çağrılarda kullanılmıyor | SİLİNDİ |
| `ui/tesla/TeslaDashboard.kt` | Eski PARK/ŞARJ, medya/Mola UI | SİLİNDİ |
| `ui/tesla/TeslaLayoutPolicy.kt` | `DashboardPresentationTest` ve `DriveModePolicy`'nin kullandığı eski tiplerin kalması gerekiyor | Yalnız küçük enum/policy, görsel UI yok |
| `ui/CockpitDesign.kt`, `ui/tesla/DriveModeState.kt` | Diğer JVM testleri ve yardımcı kurallar | KORUNDU |
| `ui/finalhmi/MapFirstDashboardActivity.kt` | F1 demo için ayrı Activity | SİLİNDİ; manifestte launcher hâlen MainActivity |
| `ui/finalhmi/MapFirstDashboardScreen.kt` | Önizleme ve boş sekmeler | F1 ana NavHost + 5 sekme; A–C Rota/Şarj, Ayarlar, OBD tanı |
| `ui/RouteSettingsScreen.kt` | Eski tek uzun Ayarlar, DEMO önizleme düğmesi | F1 temalı beş bölüm; eski DataStore işlevleri korunur |
| `ui/finalhmi/DashboardUiState.kt` | Gerçek ve DEMO mapper aynı derleme kaynağında | DemoMapper yalnız `src/debug` |
| `ui/finalhmi/MapFirstDashboardViewModel.kt` | Varsayılan örnek telemetri | Varsayılan **boş gerçek** durum; debug kullanıcı seçeneği |
| `ui/finalhmi/MapFirstDashboardPreview.kt` | `src/main`'de DEMO preview | `src/debug`'a TAŞINDI |
| `ObdTestScreen.kt` | Tanıda örnek OBD düğmesi | Örnek düğme yalnız DEBUG |
| OBD / konum / planlama / OCM / MapLibre / Mola ve Android Auto entegrasyon modülleri | Çalışan ve paylaşılan veri kaynakları | KORUNDU, A0 ile yeniden yazılmadı |

**Kanıt:** Basit bir metin araması tüm referans grafiğini kesin kanıtlamaz; bu harita okunan gerçek Kotlin dosyaları, tanımlı çağrılar ve derleyici testleriyle desteklenir. Kaynakların silinmesinden sonra CI derleyicinin işaret ettiği `TeslaAction` eksikliği ayrı enum olarak geri eklenmiştir.

## Çalışma kapsamı ve dürüst durum

- Uygulama ana sekmesi **Harita**, mavi vurgu ve F1 kartları; eski Mola/medya butonları veya eski Tesla dashboard üretim akışında yok.
- **Rota:** eski A–C gerçek RouteScreen; **Şarj:** PlannerScreen/OCM ve gelişmiş enerji hesapları; **Enerji:** henüz doğrulanmış yolculuk kaydı yok boş durumu; **Ayarlar:** yeni F1 panelleri; **OBD tanı:** Ayarlar'dan erişilir.
- GPS hızı, SoC ve enerji için F1 yeni ana ekranın gerçek depolara bağlanması A5 aşamasıdır; A0'da `—` göstermek bilinçli doğruluk kuralıdır. Kayıtlı SoC silinmedi, Ayarlar üzerinden girilip okunabilir.
- `src/debug` içinde örnek sınıflar ve Compose Preview, **release** `BuildTypeDemoProvider` yalnız boş değer döndürür. Release CI derlemesi ve DEX kontrolü, örnek mapper'ın yayın APK'sına girmediğini sınar.
- **Yeni bağımlılık:** AndroidX Navigation Compose 2.8.5. Apache 2.0, yayımlanan AAR yaklaşık 1,2 MB; bu saf dosya boyutudur, APK artışı ve transitif boyut ayrı ölçülür.
- Yeni A0 Ayarlar metinleri çeviriye hazır `strings.xml` içinde. Eski A–C/F1 bileşenlerindeki tüm sabit metinlerin çevrilmesi ve Play dil paketleri A6/A7'ye kalır.
- Yeni release kodu otomatik Play'e yüklenmez, imzasız derlenir. Store yayını, Play Console yetkisi/politika/GDPR/Test kapıları A7 kapsamındadır.

## CI / derleme kanıtı
Son commit yeşil sonuç vermeden burada **başarılı** yazılmaz. Kontrol edilecek görevler: `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:testReleaseUnitTest`, `:app:assembleRelease`; imzalı public debug APK test sertifikası ve `DemoDashboardMapper` release DEX yokluğu.

## POCO F5 üzerinde doğrulanacaklar

- [ ] İlk açılış doğrudan yeni F1 Harita ve yalnız gerçek/boş `—` metriklerle başlıyor; hiçbir demo sayı yok.
- [ ] Debug'da **Demo modu kapalı**, elle açılırsa belirgin **DEMO — GERÇEK VERİ DEĞİL** bandı var. Release'de demo düğmesi yok.
- [ ] Eski PARK/ŞARJ, Mola/medya ve eski altı sekme yok; **Harita / Rota / Şarj / Enerji / Ayarlar** var.
- [ ] Ayarlar içinde Bağlantılar, Araç profili, Harita, Birimler/ekran, Hakkında/Gizlilik başlıkları okunaklı ve butonlar metinli.
- [ ] Manuel SoC kaydet/sil, DC gözlemi, MapTiler/HeiGIT anahtarı kaydet/sil, GPS başlat/durdur, açık ekran anahtarı, Nightly kontrolü.
- [ ] OBD tanı ekranı Ayarlar'dan açılıyor ve geri dönüyor; Bluetooth izni reddedildiğinde çökme yok.
- [ ] Rota Nereden/Nereye ve alternatifler, Şarj OCM istasyonları ve gelişmiş planlayıcı hâlâ erişilebilir; boş/hata durumlarında fiyat uydurulmuyor.
- [ ] POCO F5 dikey metin kırpılmıyor, MapLibre koyu stil/logo/(i) atıfları açık; yatay geniş pencere için ayrı kontrol gerekli.
- [ ] Android Auto/Mola APK veya ekranı üzerinde değişiklik yapılmadı.

## F1 → A1 ilk adımı
ELM327 Classic Bluetooth, BLE ve Wi-Fi için izin/transport tanılarını **yeni bir PR** olarak tasarla. A0 altında bilinmeyen OBD PID'lerinden araç metriği uydurma.
