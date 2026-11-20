# EV Cockpit — Android Auto sesli medya katmanı

## Temel karar
Mola 4.8.3 sürümünde medya servisi ve direksiyon tuşu kullanımı tespit edilmiştir; bu geliştirme **Mola kodlarını aktarmaz**. Android'in resmî ve sürüşle uyumlu sesli medya kategorisi temel alınmıştır.

Kaynaklar:
- https://developer.android.com/media/media3/session/serve-content
- https://developer.android.com/training/cars
- https://docs.radio-browser.info/
- https://github.com/moris545454/mola-apk/releases/tag/v2026.11.20.3

## Yapılanlar
- Telefonun Medya Merkezi sekmesi Türkiye istasyonlarını Radio Browser HTTPS uç noktasından alır.
- Yalnızca geçerli HTTPS ve kullanıcı bilgisi içermeyen istasyon URL'leri, uygulamanın özel SharedPreferences alanında en fazla 80 istasyon olarak saklanır.
- `RadioMediaLibraryService` Media3 MediaLibrarySession/ExoPlayer üzerinden Android Auto sesli medya istemcilerine istasyon listesi sunar.
- `onAddMediaItems` gelen medya kimliklerini yalnızca daha önce doğrulanmış yerel istasyon listesiyle eşleştirir. İstemcinin gönderdiği keyfî URL çalıştırılmaz.
- Servis arka planda müzik/radyo sesli içerik için manifest `mediaPlayback` özniteliğini kullanır.
- Audio category descriptor tanımlandı. Ne videoyu araç ekranına yansıtır ne hareket hâlinde videonun kilidini açar.

## Bilinen sınırlamalar
- İlk radyo listesi için telefonu güvenle **park hâlindeyken** açıp Radyo / IPTV > Radyoları getir seçilmelidir.
- Telefon ekranındaki MediaHub oynatıcısı ve Android Auto MediaLibraryService başlangıçta ayrı Media3 player örnekleridir. Eşzamanlı oynatma / tekil MediaSession senkronizasyonu için sonraki mimari değişiklik gereklidir; mevcut PR bunları eşit sanmaz.
- Android Auto görünürlük/kanal değiştirme, POCO F5 LineageOS ve ID.4 üzerinde test edilmedi.
- Canlı radyo URL'lerinin oynatma uyumluluğu sunucunun HLS/MP3/AAC formatına ve bağlantı durumuna bağlıdır.
- IPTV bu MediaLibraryService içinde **yoktur**. Video yalnızca telefon ekranında kullanıcı seçimiyle açılır.
- Uygulamanın Play dağıtımı / Android Auto onayı garanti edilmez.

## Kabul testi
1. Debug APK kurulumu sonrasında telefonda Medya Merkezi > Radyoları getir.
2. Türkiye istasyonları listeleniyor mu? HTTP akışlar atlanıyor mu?
3. Mümkünse araç park hâlinde bağlıyken Android Auto uygulamalarından ID.4 Yol Arkadaşı medya girişini kontrol et.
4. Listeden bir radyo seçip ses, duraklat, sonra/önce kontrollerini dene.
5. Geri döndüğünde telefonun OBD/navigasyon durumunu doğrula.
6. Hata olursa GitHub Actions logu ve telefondaki sürüm, sistem ekranı notu paylaş; sürüş sırasında ekran deneme.

## Henüz eklenmeyenler
Korumalı platformların DRM atlatımı, sürüşte video gösterme, otomatik ekran izin tıklama, Shizuku/erişilebilirlik üzerinden ayrıcalıklı işlem, Xtream/Stalker kaynak kimlik bilgileri, AI API anahtarları ve Mola'nın telifli APK içeriği.
