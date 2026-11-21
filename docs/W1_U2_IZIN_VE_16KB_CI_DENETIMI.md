# W1/U2 — SDK, manifest izinleri ve 16 KB CI denetimi

## Derleme kararı
`compileSdk=36`, `targetSdk=35`, `minSdk=28` **korundu**. Android 17 API 37 çalıştırma testinden önce sürümü körlemesine yükseltmek güvenilir değildir. Google/Android yönergeleri: https://developer.android.com/about/versions/17/summary

## Manifest izin denetimi (v2 ana görev)
| İzin | Gerekçe | Otomatik veri/erişim anlamına gelir mi? |
| --- | --- | --- |
| `BLUETOOTH` / `BLUETOOTH_ADMIN` (maxSdk=30) | Eski Android Bluetooth eşleştirme | Hayır |
| `BLUETOOTH_CONNECT` | Android 12+ ELM327 SPP | Hayır; kullanıcı izni gerekir |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | GPS, rota başlangıcı | Hayır; açık kullanıcı izni gerekir |
| `FOREGROUND_SERVICE_LOCATION` ve `FOREGROUND_SERVICE` | Kullanıcı başlattığı GPS bildirimli servis | Hayır |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | ELM327 bağlantı servisi | Hayır |
| `POST_NOTIFICATIONS` | Bildirim (API33+) | Hayır; ayrıca izin gerekir |
| `INTERNET` | ORS, OSM, Open-Meteo vb. | Gizli takip izni değildir |

**YOK:** root, `WRITE_SETTINGS`, `WRITE_SECURE_SETTINGS`, `SYSTEM_ALERT_WINDOW`, `MediaProjection`, `BIND_ACCESSIBILITY_SERVICE`, Shizuku; Mola APK'sı/servisleri de bu uygulamaya taşınmaz.

## 16 KB CI kalite kapısı
`ci/native_page_size_audit.py`: APK ZIP local data offset ve her `.so` ELF PT_LOAD segmentini okur.
- APK'nın **tüm sıkıştırılmamış .so dosyaları** ZIP 16 KB offset ile hizalanmalıdır (CI sert kapı).
- `arm64-v8a` ve `x86_64` ELF PT_LOAD >= 16384 byte olmalı (CI sert kapı).
- `armeabi-v7a` ve `x86` için `PT_LOAD<16384` riski ayrı satırda gösterilir. **Bu ABI'ler 16 KB cihazda güvenilir ilan edilmez**, derleme sırf rapor uyumlu görünsün diye değiştirilmez.
- Google `zipalign -c -P 16 4` ile APK paket hizalaması ayrıca doğrulanır.
- Metin kanıtı Actions artifact `ev-cockpit-v2-native-16k-audit` olarak yayımlanır.
- U1 bulgusu: MapLibre `armeabi-v7a/x86` 4 KB `p_align`, ancak POCO F5 için gereken `arm64-v8a` ELF 16 KB. Geliştirici belgesi https://developer.android.com/guide/practices/page-sizes

Bu kontrol ancak APK statik paket uyumluluğunu kanıtlar; POCO F5 LineageOS 24/Android 17 uygulama kullanım testi **DOĞRULANMADI**.

## Lisans / geri alma
Yeni üçüncü taraf bağımlılık eklenmedi, `NOTICE` değişikliği gerekmedi. CI çıktısına iki standart-kütüphane Python scripti eklendi. Geri alma: ilgili tek PR revert.

Kaynak: https://developer.android.com/guide/practices/page-sizes • W1/U1 raporu.
