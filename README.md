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
- MapLibre Native Android 13.5.4 + OpenFreeMap Liberty çevrimiçi stil entegrasyonu. Türkiye merkezli başlangıç görünümü kullanır; **gerçek GPS konumu değildir**.
- Harita internet gerektirir; **PMTiles/BRouter yoktur**. OpenFreeMap çevrimdışı karo indirme özelliği uygulanmadı.
- Haritanın cihazda görüntülenmesi henüz test edilmedi. OSM/OpenFreeMap atıfları korunmalıdır.
- OpenFreeMap TOS (2026-10-14): https://openfreemap.org/tos/

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
