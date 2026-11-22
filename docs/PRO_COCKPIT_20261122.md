# Pro Cockpit — Tesla tarzı gerçek ürün ekranı / 22.11.2026

## Tasarım incelemesi
Mevcut UI-2 "üç panelli Tesla" uygulamasında sol kart uzun ve ağır,
96–112sp hız her panel genişliğine uymuyor, sağ eylemler 6 düz
metin düğmesinden oluşuyor; rota üzerindeki **sonraki manevra**
ana ekranda gösterilmiyor. Bu PR yalnızca taslak veya ekran görüntüsü
değil, doğrudan Kotlin/Compose UI kod değişikliğidir.

## Değişiklik
1. **Üst durum barı:** EV COCKPIT / ID.4 Pure, PARK/SÜRÜŞ/HIZ BELİRSİZ,
   OBD, GPS; kaynakta `verified=false` olan değerler kesinleştirilmez.
2. **Araç paneli:** değişken genişliğe göre 72/96sp hız, büyük batarya
   yüzdesi ve CANVAS yay grafik, SoH ile hesaplanan tahmini enerji,
   güç/regen şeridi ve kaynak uyarıları. Araç ekranında doğrulanmamış
   menzil uydurulmaz.
3. **Harita merkezi:** gerçek MapLibre, önceki rota ve istasyon
   işaretleri korunur; plan durum şeridi ve Open-Meteo hava özeti
   öne çıkarıldı.
4. **Sonraki manevra:** yalnız taze GPS mevcut rotaya 250 m içinde
   projekte olabiliyor ve ORS `RouteManeuver` geçerliyse
   yönlendirme kartı gösterilir. Konum eski/rota dışındaysa kart
   otomatik kaybolur. Yeni ağ isteği/rota hesabı yapılmaz.
5. **Kontrol merkezi:** Rota, Plan, Medya, Video, Mola, Ayarlar için
   kısa açıklamalı modern tuş kartları. Sürüş güvenlik kilidi,
   eski `DriveModePolicy.allowedAction` üzerinden **aynen** uygulanır.
6. **Altı mevcut sekme / Mola:** orijinal Mola APK, menü, tema,
   oynatıcı ve bağlantı kuralları değişmez. Mola halen ancak
   Android uygulama seçicisi veya resmî Android launcher ile
   açılabilir; Mola'ya yerleşik özel sekme eklendiği iddiası yok.
7. **Tema ve preview:** `TeslaTokens` koyu paleti korunur.
   API/bağımlılık/izin/manifest/araç protokolü değişmez.

## Veri güveni / kabul
- CAN/UDS ve MEB ölçekleri sahada doğrulanmadı.
- `OBD BAĞLI` hiçbir zaman `PID DOĞRULANDI` anlamına gelmez.
- Ekran gerçek zamanlı trafik, şarj istasyonu doluluğu veya
  kesin varış tüketimi varmış gibi davranmaz.
- GitHub CI `testDebugUnitTest` + `assembleDebug` + 16KB +
  test APK imzası yeşil olmadan sonuç başarılı ilan edilmez.
- Telefon/Android Auto ekranı davranışı ancak POCO F5 ve
  park hâlindeki ID.4 kabul testiyle doğrulanabilir.
- Main / Nightly / Mola ikili dosyası değiştirilmez.

## Manuel kabul senaryosu
- Telefon dikey: üst durum barı + okunabilir araç verisi + harita +
  kontrol merkezi kaydırılarak erişilebilir olmalı.
- Telefon yatay: harita merkezde kalmalı; kartlar bağımsız
  kaymalı; hız ve yüzde panellerin dışına taşmamalı.
- GPS olmadan: manevra kartı ve sahte mesafe yok.
- Manuel SoC: "MANUEL", kayıp/eskimiş OBD: "YOK/ESKİ",
  DEMO: "DEMO" yazmalı.
- Hareket halinde: video, medya, Mola, ayar/plan
  kapalı; yalnız mevcut güvenli rota erişimi açık.
- Park halinde: mevcut altı eylem mevcut ekranlara yönelmeli;
  Mola'nın açılması telefon üzerinde ayrı doğrulanmalı.
