# SBF 1.1.9 (226) referans incelemesinden EV Cockpit için bağımsız kazanımlar

Tarih: 22.11.2026 · İlgili kullanıcı ZIP'i: `SBF-1.1.9-226-universal.zip`.

## Kaynak ayrımı ve lisans
Kullanıcının verdiği arşiv, Flutter tabanlı SBF APK'sını ve
`assets/flutter_assets/assets/sbf-ev-data-05-26.json` adlı 389 kayıtlı
araç veri dosyasını içeriyor. Bu dosyadaki açık hak bildirimi, SBF'nin
derlediği veritabanının başka ürüne topluca kopyalanmasını ve kullanılmasını
yasaklıyor. Bu nedenle **EV Cockpit'e tek bir özel SBF araç profili,
şarj eğrisi, operatör kaydı, özel algoritma, telifli ikon veya kaynak
kod aktarılmadı.** Bu PR, yalnızca incelemenin ortaya çıkardığı genel
veri-kalite fikrini EV Cockpit'in kendi veri modeline uyguluyor.

SBF paketindeki 389 araç kaydından 331'inin DC eğrisi var. Bunların
157'sinde tanımlı eğri tepe gücü, aynı kayıttaki `max_power_kw`
değerinden büyük. Bu fark geçici/nominal güç ayrımından kaynaklanabilir;
**otomatik veri hatası** şeklinde kabul edilmez.

## Bu PR'da GERÇEKTEN yapılanlar
1. `ChargingCurveConsistency`: EV Cockpit'in **kendi** `VehicleProfile`
   ve `SourcedCurve` yapısında DC azami kW ile çizelge tepe kW
   tutarsızlıklarını bilgi/uyarı olarak işaretler.
2. Eğri kaynak verisi `verified=false` ise `TAHMİN` etiketini korur.
3. Kullanıcının araç ekranından girdiği tek DC güç ölçümünün tüm SOC
   aralığını doğrulamadığını ayrıca işaretler.
4. Plan ekranının mevcut durum metnine bu kanıt notlarını bağlar; yeni
   sekme/tema/ekran eklemez.
5. Dört saf Kotlin/JUnit testi ekler: varsayılan, tepe farkı, kullanıcı
   gözlemi ve sentetik lisans-bağımsız veri.
6. M8 hesap motorunu değiştirmez: zaten var olan
   `ChargingTimeCalculator` istasyon ve araç DC üst sınırını uygular.

## SBF'den öğrenilebilecek fakat BU PR'DA YAPILMAYANLAR
- BLE ve Classic SPP ELM327 eşgüdümü: EV Cockpit Classic SPP'ye sahip;
  BLE eklenmedi. SBF'nin özel PID kayıtları alınmadı.
- PID keşif/geri bildirim: mevcut EV Cockpit OBD tanılama yapısını
  salt okunur doğrulama/kaynak güveniyle geliştirmek ayrı çalışma.
- Çoklu otomobil veritabanı: araç profil şeması geliştirme fikri var,
  SBF 389 profilinin aktarımı YOK.
- İstasyon ve fiyat: SBF'nin `istasyonlar.bin` dosyası, resimleri ve
  proprietary istasyon kaynakları aktarılmadı.
- Yolculuk kaydı: konum/güzergâh depolama için açık onay, veri minimizasyonu,
  silme ve anonimleştirme doğrulaması olmadan yeni telemetri YOK.
- MapLibre/hava/yükseklik: EV Cockpit'in mevcut bağımsız açık servisleri
  korunur; SBF harita stilleri kopyalanmadı.
- Mola menü entegrasyonu: ayrı `feature/mola-route-launcher-20261122`
  dalında; Mola APK'sına müdahale YOK.

## Kabul şartı
- Bu PR `testDebugUnitTest` + `assembleDebug` + 16KB + imza kontrolü
  Actions'ta PASS olana kadar derlendi iddiası yapılamaz.
- ID.4 Pure şarj eğrisi/OBD gerçek ölçümü hâlâ DOĞRULANMADI.
- POCO F5 / LineageOS / ID.4 gerçek testleri ayrı tutulur.
