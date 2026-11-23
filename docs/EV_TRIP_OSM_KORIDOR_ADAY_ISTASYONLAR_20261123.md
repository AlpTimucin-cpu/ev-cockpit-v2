# EV Trip 5 — rota koridorunun tüm dönen OSM aday istasyonları

Tarih: 23.11.2026. Test cihazı VW ID.4 Pure / POCO F5 / Android 17: DOĞRULANMADI.

## Sorun
- Önceki EV Plan ekranı haritada yalnız çözücünün seçtiği şarj duraklarını gösteriyordu. Overpass sorgusu daha fazla OSM istasyon bulsa da kullanıcı tüm dönen adaylara bakamıyordu.
- Plan filtreleri (ör. CCS2 etiketi, min kW, dışlanan ağlar) arama sonucunun görünür sayısını düşürüyordu.

## Uygulama
- OverpassRepository arama sonucunda filtreli 'chargers' yanında, tekrar eden istasyonları birleştirilmiş ve güzergâha izdüşümü hesaplanmış 'allCandidates' döndürür. Bu yalnız ilgili rota koridoru sorgusunun fiilen döndürdüğü OSM/özel kayıtları kapsar.
- PlannerUiState içinde 'allCandidateStations' StateFlow üzerinden UI'a sunulur. PlanSolver ve mevcut güvenli filtreler yalnız filtreli 'candidates' kullanmaya devam eder.
- Plan ekranındaki CorridorChargerList kartı tüm dönen adayları rota boyunca mesafeye göre sıralar, 30'ar öğe sayfalar, operatör, OSM kW tahmini, CCS2 etiketi/güven ve güncellik/doluluk/fiyat bilinmiyor notlarını gösterir.
- Harita katmanı: 'bütün OSM adayları / yalnız planlanan duraklar' seçimi. Çok sayıda ikonun mobil cihazda arayüzü yavaşlatmasını önlemek için haritada ilk 100 rota sıralı aday işaretlenir. Tam dönen liste sayfalanmış kartta erişilebilir kalır.
- İstasyon kartından 'Haritada göster' açık bir kullanıcı eylemidir; koordinatlar başka bir servise ek istekle gönderilmez. MapLibre, mevcut rota geometrisi ve işaretler kullanılır.
- İstasyon detayında mevcut ChargerEvidence provenance metni korunur: OSM etiketli CCS2 donanımı canlı çalışır sayılmaz.

## Dürüst kapsam
- 'Bütün adaylar' yalnızca o anda sorulan ve yanıt alınan **OSM güzergâh koridoru** içindedir; bütün Türkiye'deki lisanslı şarj cihazları **değildir**.
- Overpass verisi eksik veya 7 günlük cache olabilir. API 429, bölüm eksikliği ve erişim hatası mevcut durum metinleriyle gösterilir.
- Şarj istasyonu güncel fiyatı, işletmeci anlık kW, müsaitlik, doluluk, arıza, yeni istasyonlar ve gerçek rota üzerindeki istasyon varlığı garanti edilmez.
- PR106 ile kullanıcının elle girdiği operatör TL/kWh değerleri Plan maliyet kartında hesaplanır; OSM 'fee=yes' bir sayı/tarife olarak yorumlanmaz.
- EPDK Şarj@TR'nin fiyat gösteriminden otomatik veri kazınmaz; belgelenmiş ve izinli otomatik API erişimi DOĞRULANMADI.
- Yeni Android izni, ücretli SDK veya Mola/Fermata müdahalesi yok. Sürüşte araç ekranında video yok.

## Test ve risk
- Overpass fake HTTP testi: iki gerçek dönen adaydan 'onlyCCS2' filtreli bir tanesi çözücü için kalırken, UI iki adayın tamamına erişebilmeli.
- CorridorStationPolicy JUnit: sıralama, 30 sayfalama, 100 harita işareti sınırı, doğrulanmamış güç/CCS2.
- GitHub Actions testDebugUnitTest + assembleDebug + public test imza + ELF/ZIP 16KB kapısı. CI yeşil olsa da gerçek POCO F5 ve şarj istasyonu veri kapsamı DOĞRULANMADI.
- Geri alma: tek PR revert. Harita kaynağı © OpenStreetMap/OpenFreeMap atıfları korunur.
