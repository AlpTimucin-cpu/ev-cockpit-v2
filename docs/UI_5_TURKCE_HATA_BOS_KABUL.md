# UI-5 — Türkçe boş/hata/eski durumlar ve telefon kabulü

21.11.2026. Fiziksel POCO F5 / Android 17 / VW ID.4 kabulü: **DOĞRULANMADI**.

## Geliştirme sınırı ve mevcut sistem
- UI-1: TeslaTokens / Material3 sabit koyu tema, 14sp+ etiket, 56dp hedefler.
- UI-2: 26/54/20 üç panel, MapLibre OpenFreeMap koyu stil, yeşil/gri rota, bir sonraki durak, TAHMİN metinleri.
- UI-3: OBD/GPS taze hız, 5 km/sa eşik, OBD tazelik 3s, bilinmiyor durumda video kapalı.
- UI-4: Mola için yalnız Android PackageManager, kurulu değil/onaylı kurulum rehberi, dönüşte yeniden kontrol.
- UI-5: DashboardGuidancePolicy ile gerçek StateFlow durumlarından türetilen tek öncelikli mesaj kartı; yeni ağ/OBD erişimi yok.

## Boş/hata durumları ve doğrudan eylemleri
| Durum | Görünen metin | Dokununca |
| --- | --- | --- |
| SoC/OBD yok, manuel yok | OBD bağlı değil | Ayarlar / Manuel SoC |
| GPS alınamıyor veya eski | Konum alınamıyor | Android konum izin isteği |
| Gerekli HeiGIT anahtarı olmadığı rota hata mesajıyla doğrulandı | Rota anahtarını Ayarlar'a gir | Ayarlar |
| MapLibre yükleme başarısız callback'i geldi | Harita yüklenemedi | Kullanıcı dokunursa haritayı yeniden oluştur |
| SoC veri kaynağı ESKİ | ESKİ • OBD verisi yenilenmeli | OBD Test |
| PlanResult.Failure (uygun aday yok / menzil yetersiz) | Bu rota için durak bulunamadı + en yakın ulaşılabilir nokta TAHMİN | Plan filtreleri |
| Rota hesaplanıyor | Rota hesaplanıyor | Yeniden ağ isteği tetiklenmez |
| Henüz rota yok | Rota henüz oluşturulmadı | Rota ekranı |
| Mola kurulu değil | Mola kurulu değil | Resmî GitHub yayın rehberi, kullanıcı onayı |

## Harita durumunun doğruluk seviyesi
- Harita hata kartı yalnız MapLibre `MapView.addOnDidFailLoadingMapListener` olayından sonra gösterilir. Hatanın mutlaka internet yokluğundan kaynaklandığı iddia edilmez; 'Bağlantını kontrol et, çevrimiçi değilken açılamayabilir' denir.
- `onStyleReady` ile hata kaldırılır. Kullanıcı yalnız `Haritayı yeniden dene`ye dokunduğunda Compose `key` üzerinden MapView yeniden başlatılır; görünmez tekrarlı internet isteği yoktur.
- Sürüş modunda haritayı manuel yeniden dene düğmesi pasif kalır. OpenFreeMap / OSM atıfları korunur.
- Harita API kaynağı: https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.maps/-map-view/add-on-did-fail-loading-map-listener.html

## Dikey telefon düzeni
- Manifest yalnız telefon `MainActivity` için `sensorLandscape` yerine `fullSensor` kullanır. Böylece cihaz dikey/yatay döndürülebilir; UI-2 küçük ekran kaydırılabilir düzene geçer.
- Android 17/POCO F5 fiziksel yön değiştirme, klavye ve erişilebilirlik testi hâlâ kullanıcı kabulü bekler.

## Mola — resmî yol / doğrulama matrisi
| Adım | Resmî Android yolu mu? | Telefon/araç doğrulaması |
| --- | --- | --- |
| Paket görünürlüğü ve public launcher | EVET — PackageManager / `<queries>` | DOĞRULANMADI |
| Mola yoksa yayın sayfası | EVET — kullanıcı dokunuşlu ACTION_VIEW | DOĞRULANMADI |
| Mola uygulama ayarı | EVET — ACTION_APPLICATION_DETAILS_SETTINGS | DOĞRULANMADI |
| EV Cockpit dönüş | Sadece var olan 6 güvenli kendi deep linkimiz ve normal sistem geri dönüşü | DOĞRULANMADI |
| Mola APK/arayüz/izin/dahili servise müdahale | HAYIR — YASAK | İşlem yapılmadı |
| Android Auto araç yüzeyinde video ve Mola yansıtma | YOK — kapsam dışı | DOĞRULANMADI |

## UI kabul noktası: sert durma
- UI-1–UI-5 PR'ları bağımsız Actions çalışmasında tüm testler yeşil, APK derlenmiş ve imzalanmış, native 64-bit/ZIP denetimi geçmiş olmalı.
- Son `main` Actions ve `nightly` yayın meta bilgisi doğru commit'e işaret etmeli. Nightly **yalnız test APK**, üretim/araç doğrulaması değildir.
- Kullanıcı yalnız telefonda son Nightly APK'yı kurup güvenli bir yerde **yatay ve dikey iki ekran görüntüsünü** paylaşacak. Tek kabule bağlanan deneyim: hız/SoC okunabilirliği, büyük harita, altı eylem, Mola kartı (kurulu/kurulu değil), mod durumu ve hata mesajlarının Türkçesi.
- Gerçek Android Auto/ID.4 ekranı, W7, sürüşte video ve şarj BMS PID doğrulaması bu kabulün içinde yapılmaz.
- Kullanıcı UI kabulü onaylamadan W7'ye geçme. D1/D2 önceki araç OBD tanıları hâlâ gerçek cihaz kanıtı gerektirir.

## Risk ve geri alma
- Gerçek cihazda MapLibre stil yüklemesi, Android 17 sensör döndürme, Mola başlatma intent'i, paket görünürlüğü, önizleme yazı boyutları **DOĞRULANMADI**.
- Native ABI kalite kapısı 64-bit PASS; 32-bit MapLibre 16 KB riskleri 2 adet devam ediyor.
- Yeni kütüphane/ayrıcalıklı Android izin/auto media/video/araç ekranı değişikliği yok.
- Bu PR revert edilirse hata kartları ve orientation ayarı kaldırılır; Mola APK'sı ve verileri değişmez.
