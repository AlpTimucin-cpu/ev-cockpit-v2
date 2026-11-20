# EV Cockpit — Car Scanner, ABRP 8.0 ve Kilovati 2.3.0 APK/XAPK teknik incelemesi

**Tarih:** 20.11.2026 · **Yöntem:** Kullanıcının sağladığı üç paketin **çalıştırılmadan** ZIP/XAPK içeriği ve binary AndroidManifest.xml kaynaklarının salt okunur analizi. Ayrı olarak üreticilerin kamuya açık yardım/ürün açıklamalarına bakıldı. Paketlerdeki hiçbir uygulama çalıştırılmadı; dex/özel algoritma/anahtar çıkarılmadı; APK değiştirilmedi.

**Önemli ayrım:** Bu belge iki kaynak grubunu ayrı tutar:
1. **PAKETTEN DOĞRULANDI:** Kullanıcının verdiği sürümün manifesti, APK bileşeni, izinler, kaynak adları.
2. **ÜRETİCİ AÇIKLAMASI:** Güncel ürün sayfalarında tanıtılan özellikler; gönderilen APK'nın bunları gerçekten çalıştırdığını kanıtlamaz.

## A. Tam dosya envanteri ve bütünlük

| Kullanıcı dosyası | Arşiv SHA-256 | Kurulum / manifest |
|---|---|---|
| `Car+Scanner+ELM+OBD2_2.2.52_APKPure.zip` | `4b1f08518050c95cc80f3e10b38cd13e0c169379e955fe31c523c82d83609aa0` | ZIP içinde tek 79.787.919 bayt APK; `com.ovz.carscanner`, versionName **2.2.52**, versionCode **52002052**, minSdk **23**, targetSdk **36** |
| `A+Better+Routeplanner+(ABRP)_8.0.0_APKPure.zip` | `8260d5072a8f4bbb3bcb3066f091642ce252d7d8ae91d5aec2acfb342e7e318e` | ZIP içinde XAPK; base APK + arm64/en/xhdpi split; `com.iternio.abrpapp`, **8.0.0**, versionCode **6087**, minSdk **26**, targetSdk **36** |
| `Kilovati-EV+Charging+Stations_2.3.0_APKPure.xapk` | `4a6c5073c4ee77d5b13c8be5de74e45864b84ba7758aaef4d1dff53d21ec7390` | XAPK base + arm64/es/fr/tr/de/en/xxhdpi split; `com.kilovati.app`, **2.3.0**, versionCode **28**, minSdk **26**, targetSdk **36** |

**Güvenlik kapsamı:** Arşiv yapısı ve manifest analizi, APK'nın güvenli veya zararlı yazılımdan arındırılmış olduğunu ispatlamaz. Dijital imza sahipliği, çalışma zamanı ağ trafiği, gizlilik ve abonelik erişimleri **DOĞRULANMADI**. XAPK split'leri tek temel APK gibi kurulmamalıdır.

## B. Car Scanner ELM OBD2 2.2.52

### Paketten doğrulananlar
- `AndroidManifest.xml` kullanımları: `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, hassas/yaklaşık konum, internet, foreground service / connectedDevice, bildirimler, wake lock.
- `KeepAliveService`, `BackgroundService`, `AACarService` servis adları; `car-app-api.level` dosyası `8`. Bu servislerin gerçek araç ekranında çalışması **kanıt değil**.
- `assets/cs_profiles.dat` ve `assets/cs_profiles.sig` dosyaları var: araç profili kaynağının **imzalı** olduğuna işaret eder; içerik, ölçek, adres veya lisans doğrulanmadan **kullanılmaz/kopyalanmaz**.
- `MauiMaterialAssets`, `mono.MonoRuntimeProvider` bileşenleri, paketin .NET MAUI/Mono altyapısıyla uyumlu olduğunu gösterir; EV Cockpit'in Compose/Kotlin mimarisi bağımsız kalır.
- Dosyadaki PID ölçeklerinden ID.4 Pure için doğrulanmış bir **yeni adres/ölçek çıkartılmadı**.

### Üretici açıklamalarına göre (APK ile ayrıca doğrulanmadı)
- Özel/extended PID tanımı, farklı bağlantı profilleri, ELM327 Bluetooth/BLE/Wi-Fi adaptörleri, canlı değer ve HUD/gösterge ekranları: https://www.carscanner.info/custompids/ ; https://play.google.com/store/apps/details?id=com.ovz.carscanner

### Bizim ürüne karşılığı
- ELM327 Bluetooth SPP, ham istek/yanıt, yeniden deneme ve güvenli tanı logları **zaten var**. Yeni eklenen OBD veri güveni bölümü, `bağlandı` ile `SoC/hız doğrulandı` durumlarını ayırır.
- BLE/Wi-Fi desteği, ID.4'te MEB PID doğrulaması ve ayrı Android Auto prototipi **bu PR'da eklenmez**.
- OBD-II DTC silme/ECU yazma işlemleri **eklenmez**: araç güvenliği / yanlış komut riski.

## C. A Better Routeplanner 8.0.0

### Paketten doğrulananlar
- `androidx.car.app.NAVIGATION_TEMPLATES`, `MAP_TEMPLATES`, `ACCESS_SURFACE`; `AndroidAutoService` ve `NitroBackgroundLocationService` servis adları.
- `io.enode.link.ble.BluetoothService`; `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN`, foreground location, `RECORD_AUDIO`, bildirim ve takvim izinleri.
- `assets/index.android.bundle` (React Native tabanını işaret eder), `assets/sdk_versions/com.mapbox.maps`, `mlkit_barcode_models`, çeşitli markalara ait OEM vektör simgeleri bulunur. Bunlar bizim uygulamaya aktarılmaz.
- Bu izin/bileşenler ABRP 8.0 paketinde var; gerçek zamanlı trafik/şarj doluluğu/kapalı harita API'lerine erişim hakkı vermez.

### Üretici açıklamaları (paket çalıştırılarak doğrulanmadı)
- ABRP 8.0: yeni harita stili, şarj işaretleri, sade sürüş ekranı, alternatif rota/şarj önerisi, araç verisiyle dinamik planlama: https://abetterrouteplanner.com/home/cs/resources/article/2026-11-12_abrp-8-0
- OBD canlı veri kaynağı (uyumlu BLE adaptör), tüketim ve SoC ile plan güncelleme: https://abrp.featurebase.app/help/articles/3055653-link-your-vehicle-for-live-data ; https://web.abetterrouteplanner.com/resources/article/2024-10-10_obd-connection
- Bazı Android Auto, canlı doluluk ve gelişmiş filtreler Premium'a bağlı olabilir: https://abetterrouteplanner.com/premium/

### Bizim ürüne karşılığı
- ORS sürüş rotası, fiziksel tahmin, parçalı SoC/şarj eğrisi, OSM duraklar, alternatifler, ikinci geçiş ve manuel yeniden planlama **zaten var**.
- Bu PR'da yeni **Rota/SoC/GPS/İstasyon/Enerji tahmini kaynak denetim kartı** eklenir: OBD PID doğrulanmadı, manuel SoC eskime riski, rota trafik yok, GPS güncelliği, istasyon canlı doluluğu yok ayrıştırılır.
- ABRP'nin özel EV tüketim modeli, istasyon fiyatları, premium/abonelik verisi, markalı varlıkları, algoritması, API anahtarları **kullanılmaz**. Otomatik arka plan rota/şarj sorgusu **yok**.

## D. Kilovati EV Charging Stations 2.3.0

### Paketten doğrulananlar
- `androidx.car.app.MAP_TEMPLATES` ve `ACCESS_SURFACE`, `com.kilovati.app.car.KilovatiCarAppService`, Firebase Messaging servisleri.
- Konum (fine/coarse), bildirim, internet ve ağ durumu izinleri.
- `config.tr.apk` Türkçe bölgesel dil paketi, `res/raw/map_style_dark.json` ve `map_style_light.json` koyu/açık harita stili kaynakları. Üçüncü taraf stil dosyası **kopyalanmadı**.

### Üretici açıklamaları (APK ile ayrıca doğrulanmadı)
- Türkiye çapında operatörler arası istasyon, güncel tarifeler, filtreler; yardım sayfası filtre tercihini cihazda saklama ve kaydedilen rotaları güncel veriyle yeniden hesaplama işlevlerini tarif ediyor: https://kilovati.com/support ; https://kilovati.com/
- Üretici kullanım şartları fiyat/istasyon bilgilerinin değişebileceğini açıkça belirtir: https://kilovati.com/terms

### Bizim ürüne karşılığı
- OSM/Overpass `ChargerCandidate` modeli CCS2 güven sınıfı, operatör, güç, erişim, saatler, ücret işareti, ödeme doğrulama, Type2/CHAdeMO ve kapasite alanlarına **zaten sahip**.
- Bu PR'da durak kartlarına **İstasyon veri kaynağı / soket / erişim** açılır bölüm eklenir. Bilgi yoksa `bilinmiyor`; `fee=yes` asla TL/kWh fiyatı diye yorumlanmaz.
- Üçüncü taraf canlı doluluk/fiyat/çalışırlık ve indirim kampanyası verilmez; bunun için lisanslı ve doğrulanmış ayrı veri kaynağı gerekir.

## E. Bağımsız entegrasyon / test planı

| Alan | Yeni özelliğin kapsamı | İşlem |
|---|---|---|
| OBD güveni | Bluetooth bağlı / ECU verisi alındı / PID doğrulanmadı / DEMO ayrımı | `ObdEvidence` + OBD Test UI |
| Rota kaynak denetimi | manuel/OBD/DEMO SoC, GPS yok-eski-hassasiyet, rota var/yok, servis adı | `TripDataEvidence` + Plan UI |
| Şarj güveni | OSM CCS2 etiketi, veri adedi, giriş alanları, doluluk ve fiyatın bilinmediği | `ChargerEvidence` + durak ayrıntısı |
| Güvenlik | API anahtarı/koordinat/VIN rapora yazılmaz; rota hesaplama bu kartla değiştirilmez | Birim test |
| Android Auto/BLE | Manifestteki referans parçaların projeye kopyalanması **yok** | Saha doğrulaması ve ayrı onay bekler |

**Sınırlar:** M8 Durma Noktası B ve önceki OBD Durma Noktası 1 açık kalır. Telefonda gerçek GPS/OBD/ORS/Overpass bağlantısı, şarj verisinin fiziksel doğruluğu ve Android Auto davranışı **DOĞRULANMADI**. Bu PR M8.8/M4/M5 geçişi sayılmaz.

## F. Sahada ilk deneme

1. POCO F5 / LineageOS üzerinde son Nightly debug APK'yı **park halinde** çalıştır.
2. OBD Test'te **ELM327 / ECU veri güveni** bölümünü gör: bağlı değilken doğrulanmış veri yazmamalı.
3. Rota oluştur ve Plan ekranındaki **Veri güveni • OBD / rota / şarj** panelini aç; verileri güncellemeden ve güncelledikten sonra karşılaştır.
4. Plan üretildiyse durak kartındaki **İstasyon veri kaynağı / soket / erişim** bölümünü aç; tüm bilinmeyenler açık kalmalı.
5. Koordinat, API anahtarı ve VIN paylaşmadan ekran görüntüsü / test sonucu bildir.

**Bu analiz şifreli servisleri açmaz, ücretsiz veri sınırlarını aşmaz ve üçüncü taraf kod/harita/ikon kopyalamaz.**
