# W5 — M8.7 harita/rota/plan entegrasyonu ve saha kalibrasyonu

21.11.2026 · Referans: Birleşik Ana Görev v2 §7/W5 ve GitHub `main` kaynak ağacı. **POCO F5 / ID.4 saha kabulü DOĞRULANMADI.**

## W5 gerçek durum denetimi
| Gereksinim | Kodda doğrulama | Bu PR müdahalesi |
| --- | --- | --- |
| Şarj haritası, durak işaretleri, OSM/OpenFreeMap/ORS/Open-Meteo atıfları | `OnlineMap` + `PlannerScreen` mevcut | Değişmedi |
| Planlanan durakları navigasyona aktarma | `PlanToNavigation.intermediateStops` ve `NavigationViewModel.navigateVia` mevcut | Değişmedi |
| Ara noktalı rotayla ikinci geçiş, en çok bir yeniden plan, %2 eşik | `PlanRevalidator.check` iki sağlayıcı isteği, en fazla bir yeniden çözüm ile mevcut | Değişmedi |
| Planı kaydet/paylaş (koordinatsız metin) | `SavedPlansStore`, `SavedPlanBuilder.shareText` mevcut | Değişmedi |
| Güncel GPS/SoC ile isteğe bağlı yeniden planlama | `PlannerViewModel.replanFromCurrentGps` mevcut | Değişmedi |
| Saha tüketim/varış SoC → önerilen tüketim ayarı | **Eksikti** | `ConsumptionCalibration` ve `CalibrationPanel` eklendi |
| Doğrulama defteri | Genel M8 dokümanı vardı; kişisel numaralı saha defteri yoktu | `CalibrationJournalStore` eklendi, son 10 sayısal kayıt |
| Lisans ve atıf / NOTICE | MapLibre, OSM, OpenFreeMap, ORS, Open-Meteo atıfları ekranda ve NOTICE içinde | Yeni bağımlılık yok, NOTICE değiştirilmedi |

## Güvenli kalibrasyon yöntemi
Plan **başarılı** ve kullanıcı yolculuktan sonra araç ekranında gerçek tüketimi görmüşse:
1. Plan içindeki **Saha tüketim / varış SoC kalibrasyonu** panelini **park hâlinde** aç.
2. Araçta görünen **kWh/100 km** ile **gerçek varış SoC (%)** değerini elle gir.
3. `RouteEnergyPrefix` mevcut `PlanRequest` üzerinden model kWh / mesafe hesaplar; bu veri **TAHMİN**.
4. Önerilen düzeltme yaklaşık olarak `mevcutAyarlama + (gerçekTüketim / modelTüketim − 1)`; **±%30** sınırında tutulur. SoC farkı 5 puanı geçerse tutarsızlık uyarısı verilir; varış SoC sahte matematiksel ölçüm olarak güç verisine karıştırılmaz.
5. Düzeltme **otomatik uygulanmaz**. Kullanıcı isterse Plan parametresindeki mevcut tüketim düzeltmesini **kendisi** değiştirir.
6. Sadece `model kWh/100km`, `gerçek kWh/100km`, `plan SoC`, `gerçek SoC`, `önerilen %` sayıları özel DataStore'a en çok **10 kayıt** olarak girilir. Koordinat, VIN, API anahtarı, cihaz kimliği, istasyon ID veya yolculuk saati yoktur.
7. Kalibrasyon kaydı **verified=false** kalır. Kullanıcı gözlemi olsa bile ELM327 BMS PID ölçeği doğrulanmış sayılmaz.

## Testler
- Sentetik fizik modelinden kWh/100km varlığı.
- Geçerli tüketim/SoC sapmasında yaklaşık öneri, mevcut ayarın değişmemesi.
- NaN/negatif/aralık dışı ve `±%30` sınırlandırma.
- Günlük JSON kodlaması: en fazla 10, yalnız sayısal alanlar, bozuk/çok büyük dosyada güvenli varsayılan, `verified=false`.
- CI: `testDebugUnitTest`, `assembleDebug`, arm64 ELF/ZIP 16KB, public debug test sertifikası. **Telefon sonucu ayrı.**

## Kaynaklar ve riskler
- Birleşik Ana Görev v2, `docs/M8_DOGRULAMA.md`, `docs/M8_SARJ_VERI.md`, `docs/M8_VERI_AKISI.md`.
- İstatistiksel kalibrasyon yapılmaz: bir veya az sayıda yolculuk, SOC/güç/hava modelinin doğruluğunu göstermez.
- Canlı trafik, canlı fiyat/doluluk yok; ABRP modelini ve şarj ağı verilerini kazıma yok.
- Mola APK/tasarımı ve mevcut radyo/YouTube/Android Auto medya işlevleri değişmedi.
- **D2 hard gate:** Kullanıcı POCO F5 üzerinde kabul listesini tamamlayana kadar “telefon üzerinde çalışıyor” denemez. D2 sonrası W6/W7/W8/W9 otomatik başlamaz.

## Geri alma
W5 PR'ını revert etmek, yalnız `CalibrationPanel`, kalibrasyon modelini ve defteri kaldırır. DataStore'daki kişisel sayıların silinmesi için panelin "defteri temizle" düğmesi ayrıca kullanılabilir; otomatik veri temizleme yapılmaz.
