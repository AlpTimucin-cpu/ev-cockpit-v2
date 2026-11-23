# EV Cockpit — profesyonel yolculuk planı ve gerçek veri sınırları

Tarih: 23.11.2026. POCO F5/ID.4 fiziksel kabul: **DOĞRULANMADI**. CI sonucu ayrıca denetlenecek.

## Önceki gerçek eksikler
- Rota ekranında yalnız hedef manuel araması vardı; başlangıç yalnız GPS, yazarken öneri yoktu.
- ORS GeoJSON parser ilk route feature'ını seçiyordu; alternatifler sunulmuyordu.
- Hava özeti üç rota noktasındaki **şimdiki hava** durumuydu, yolculuk saatinin hava tahmini değildi.
- OSM/Overpass şarj noktaları mevcut olsa da tam ulusal EPDK soket/fiyat API entegrasyonu yok.
- MEB PID verileri gerçek araç üzerinde DOĞRULANMADI.

## Bu PR
- Rota ekranında iki ayrı **Nereden / Nereye** şehir, semt, yer ve adres alanı.
- Üçüncü harften itibaren 750 ms gecikmeyle öneriler: Open-Meteo Geocoding şehirleri, Photon OSM adres/POI. Ağ çağrısı Compose'da değil, PlaceAutocompleteViewModel ve kaynak sınıflarında.
- Sağlayıcı adil kullanım için Open-Meteo şehir istekleri en erken 2,5 saniyede bir, Photon demo 10 saniyede bir; en çok 10 sonuç ve 16 sorguluk bellek önbelleği. Ücretsiz sunucu erişimi/bölge kapsamı/kota sınırsız değildir.
- Elle seçilmiş başlangıç GPS zorunluluğunu kaldırır. Yazılan ama seçilmeyen yer koordinata çevrilmez; sahte rota oluşturulmaz.
- Gerçek ORS GeoJSON içindeki en fazla 3 alternatif ayrıştırılıp karta dönüştürülür. Kullanıcı seçenek seçtiğinde harita ve navigasyon aynı gerçek geometriyi kullanır.
- Barındırılan ORS alternatifi gerçek yol mesafesinde 100 km ile sınırlı. Hata sayısını azaltmak için kuş uçuşu 80 km üstünde alternatif istenmez; uzun mesafede tek gerçek rota gösterilir, sahte alternatif üretilmez.
- Kullanıcının şifreli HeiGIT anahtarı HTTP Authorization başlığından başka hiçbir yere yazılmaz; arama sağlayıcıları anahtarı almaz.

## Sonraki somut teknik işler
1. **Yol boyu hava:** Open-Meteo hourly sıcaklık, rüzgâr/yağış; rota örnekleri ve tahmini geçiş saati (sadece TAHMİN).
2. **Harita katmanları:** MapLibre/OpenFreeMap Dark, Liberty ve Bright stilleri; ücretsiz uydu görüntüsü yoksa uydurma katman yok.
3. **Güzergâh şarjları:** Overpass koridorundan dönen tüm adayları ayrı kart/listede göster, güç/CCS2 güven düzeyi ve güncelleme tarihi. Eksik OSM noktalarını "tüm istasyonlar" diye iddia etme.
4. **Enerji ve ücret:** Model tahmini kWh, varış SoC, şarjda alınan kWh, kayıp, TL/kWh ve toplam TL. Fiyat operatör API'si doğrulanana kadar MANUEL/kaynak tarihli; EPDK Şarj@TR uygulama fiyatları kullanıcıya kaynak rehberidir.
5. **Trafik:** Serbest API varsa kullanım/yeniden dağıtım hakkı ve erişim kota şartlarını kontrol et; yoksa "CANLI TRAFİK YOK" göster.

## Lisans ve doğruluk
- Photon: https://github.com/komoot/photon (public demo, yoğun kullanım sınırlı)
- Open-Meteo Geocoding: https://open-meteo.com/en/docs/geocoding-api (ücretsiz planın ticari olmayan kullanım şartları var)
- ORS: https://giscience.github.io/openrouteservice/hosted-api-restrictions (alternatif rota 100km, en çok 3)
- Public Nominatim ile autocomplete **YOK**: https://operations.osmfoundation.org/policies/nominatim/
- EPDK: https://www.epdk.gov.tr/Detay/Icerik/1-3428/enerji-donusumuserbest-erisim-platformu--sarj%40tr
- Mola APK ve tasarımı **değişmedi**. Android Auto video ve Mola ekran yansıtması kapsam dışı.
- Gerçek araç, Android 17, rota servis erişimi ve yolculuk gideri **DOĞRULANMADI**; CI yeşil olmadan main'e merge yok.
