# Referans APK kanıt envanteri — 2026-11-25

Kaynaklar kullanıcı arşivlerinden salt okunur ZIP envanteri ile incelendi; APK çalıştırılmadı. Mola orijinal dosyası değişmedi. Bu belge özel kod, anahtar, istasyon veritabanı veya lisanslı araç veri seti içermez.

| Arşiv | SHA-256 (orijinal arşiv) | Gözlenen kanıt |
|---|---|---|
| SBF 1.1.9-226 ZIP | b2033cdf9d4dcf5617d2611b6eb0404b97affdfe5c652afb92359f526da549ef | Tek APK, 942 ZIP girdisi; Flutter |
| ABRP 8.0.0 ZIP | 8260d5072a8f4bbb3bcb3066f091642ce252d7d8ae91d5aec2acfb342e7e318e | XAPK, com.iternio.abrpapp.apk ve 3 split |
| Car Scanner 2.2.52 ZIP | 4b1f08518050c95cc80f3e10b38cd13e0c169379e955fe31c523c82d83609aa0 | APK, 1923 girdi |
| Kilovati 2.3.0 XAPK | 4a6c5073c4ee77d5b13c8be5de74e45864b84ba7758aaef4d1dff53d21ec7390 | com.kilovati.app.apk ve dil/ABI/yoğunluk splitleri |
| Mola 4.8.3 APK | 124e9121c41a624893b0f0794854f3d5ba4fc8a29a15b051c3f4bc376d8fd149 | 1759 girdi; önceki PR #71'deki orijinal SHA ile aynı |

## Somut dosyalar
- SBF: assets/flutter_assets/assets/sbf-ev-data-05-26.json (813463 B), schema_version, generated_at, vehicle_count=389, vehicles. Veri alanları: marka/model, batarya kimyası ve net/brüt kapasite, termal yönetim, şarj portu, AC/DC, menzil, aerodinamik ve ağırlık, boyutlar. ID.4 içeren kayıt bulundu; Türkiye Pure varyantına eşleşme doğrulanmadı. _hak alanı veri tabanının toplu kopyalanmasını ve başka ürünlerde kullanımını açıkça yasaklar; açık lisanslı bileşenler kendi lisanslarına tabidir. Veri tabanı kopyalanmayacak.
- SBF: istasyonlar.bin (820310 B), dark_tr.json / positron_tr.json, ilceler.json, sehirler.json, mahalleler-1..4.json, tire-data.json, tr-provinces.json. Lisans dosyaları: Apache-2.0, CDLA-Permissive-2.0, ODbL-1.0, font OFL. Bunların varlığı her veri dosyasının yeniden dağıtım izni değildir.
- ABRP: assets/index.android.bundle (19748912 B), app.config, app.manifest, Mapbox SDK sürüm dosyaları. Kapalı rota/enerji algoritması veya özel servis erişimi doğrulanmadı ve kopyalanmadı.
- Car Scanner: assets/cs_profiles.dat (744 B), cs_profiles.sig (71 B); CSINT1 başlangıcı. Bunlar tek başına MEB PID kataloğu olduğunun kanıtı değil. Tam profil ve ölçekler bağımsız doğrulanmalı.
- Kilovati: assets/dark-splash.gif, light-splash.gif; Türkçe dil split'i mevcut. Paket envanterinde açık bir istasyon veya fiyat veri tabanı tespit edilmedi. Özel servisler kullanılmayacak.
- Mola: PR #71 önceki statik PhoneScreenView / AppsAdapter / launch / showChooser bulgusu, standart Android uygulama açılışına aday. Şu anki APK SHA eşleşti. Mola içindeki üçüncü taraf ekranın fiziksel araçta çalışması henüz kanıtlanmadı.

## EV Cockpit veri güveni ve uygulama planı
| Alan | Doğru kaynak / kabul şartı | Mevcut durum |
|---|---|---|
| ID.4 Pure araç profili | VIN/model yılı/üretici teknik belgesi; net kapasite, DC eğrisi, güç ve sıcaklık ayrı | 52 kWh kullanıcı profili; model yılı ve DC eğrisi doğrulanmadı |
| MEB OBD | Salt okunur UDS; ECU başlığı, yanıt, byte offset, ölçek, birim, sign ve tazelik; Car Scanner/araç ekranı karşılaştırması | pids_meb.json altı giriş, tamamı verified=false. 22028C, 221E3B, 221E3D, 22F40D, 222A0B, 22210E. Ham sıcaklık °C diye sunulamaz. |
| Rota | OSRM/ORS gerçek geometri, kaynak ve alternatif sayısı | Aktif; canlı trafik yok |
| Şarj | OCM/OSM açık lisans koşulları; operatör resmî fiyatı/tarihi; güncel doluluk ayrı | OCM kota/anahtar, OSM kapsama sınırlı; gerçek zamanlı doluluk yok |
| Hava | Open-Meteo zaman/konum eşlemesi; model tahmini olarak | En çok üç örnek; ölçüm değil |
| Enerji | Bağımsız fizik modeli, rakım, hız, hava, sıcaklık, kayıplar ve şarj eğrisi; gerçek sürüş kalibrasyonu | Ön tahmin sınırlı; gelişmiş plan ayrı |
| Mola | Orijinal APK; public launcher/deep link; fiziksel test | 0.7.1 ana Activity regresyonu düzeltildi; Mola ekran içi görüntü doğrulanmadı |

## Mola kabul testi
1. Orijinal Mola ve yeni EV Cockpit APK ayrı kurulu.
2. Mola'nın mevcut telefon uygulamaları seçicisinden ID.4 Yol Arkadaşı seçilir (park halinde).
3. EV Cockpit açılır, harita ve geri davranışı doğrulanır.
4. EV Cockpit alt çubuğundaki Mola düğmesi mevcut Mola'yı standart launcher ile açar; park durumu yoksa kısayol engellenir.
5. evcockpit://open/dashboard, /route, /plan, /obd, /settings soğuk ve sıcak açılışta kontrol edilir. /passenger bu sürümde desteklenmez, otomatik medya yok.
6. Mola APK, kaynak, imza, manifest, tema veya AA bileşeni değiştirilmez. Yerleşik native Mola sekmesi ancak desteklenen eklenti API'si veya yetkili kaynak değişikliğiyle mümkündür.
