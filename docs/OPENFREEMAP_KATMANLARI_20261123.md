# EV Trip 4 — seçilebilir güncel OpenFreeMap katmanları

23.11.2026. Telefon/araç durumları: DOĞRULANMADI.

## Uygulanan
- MapLibre haritasına Katman seçimi eklendi: Koyu (dark), Yol/sokak (liberty), Açık (bright), Sade (positron).
- Harita veri kaynağı OpenFreeMap / OpenStreetMap; bunlar görünüm katmanlarıdır. Uydu, canlı trafik veya arazi görüntüsü olduğu iddia edilmez.
- Tek sağlayıcının belgelenmiş sabit HTTPS OpenFreeMap stil adresleri kullanılır; kullanıcının girebileceği özel URL yok.
- DataStore ile son stil seçimi korunur. Harita stili değiştiğinde MapLibre üzerinde yüklenir; aynı harita kamerası korunur, çizgiler/şarj işaretleri mevcut kodla yeniden kurulur.
- Sürüş modunda katman menüsü pasif, park/plan modunda etkindir. Kullanan herkes sonuçları telefonda doğrulamalıdır.
- OSM ve OpenFreeMap atıfları ekrandan kaldırılmaz; Mola APK/menü değişmez.

## Kaynaklar
- Resmî stil seçenekleri: https://openfreemap.org/quick_start/
- Resmî ücretsiz tile servisi: https://openfreemap.org/
- Taban harita verisi OpenStreetMap katılımcılarından; veri güncelleme zamanı sağlayıcıdan ayrıca edinilmediği için gerçek zamanlı güncellik DOĞRULANMADI.
- Gerçek uydu katmanı, lisanslı canlı trafik ve anlık fiyat API bağlantısı YOK.

## Saha kabul ve risk
- POCO F5/LineageOS 24'te Dark/Liberty/Bright/Positron arasında geçiş, katman yükleme, rota çizgisi ve şarj noktası işaretleri test edilmeli.
- Stil yükleme hatası mevcut retry kartında gösterilir, başarısız stil için otomatik sahte harita çizilmez.
- Yeni ağır bağımlılık/izin yok; 16KB 32-bit MapLibre riski önceki W1'den devralınır.
- CI + gerçek telefon kabulü ayrı; W7 araç ekranı çalışmasına kullanıcı onayı olmadan geçilmez.
