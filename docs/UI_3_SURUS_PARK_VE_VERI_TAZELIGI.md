# UI-3 — Sürüş/Park modu, veri tazeliği, güvenli ekran kapatma

21 Kasım 2026. CI doğrulaması tamamlanmadan çalışır denmez; gerçek araç testi DOĞRULANMADI.

## Uygulama
- ObdRepository VehicleState'e speedUpdatedElapsedMs, socUpdatedElapsedMs, voltageUpdatedElapsedMs, currentUpdatedElapsedMs alanları eklendi. Uygulama bunları sadece gerçek pozitif OBD veri ayrıştırmasında monotonic Android SystemClock ile günceller; demo verisi ayrı.
- DriveModeViewModel proses OBD/GPS StateFlow'larını 250ms saat akışıyla birleştirir; değerler bir daha gelmese bile 3 sn sonra eski sayılır. UI yalnız StateFlow'u okur.
- DriveModePolicy: taze OBD hızı 0'dan büyük ya da taze GPS hızı 5 km/sa üstü ise SÜRÜŞ. Hız sinyali varsa ama eşik aşılmıyorsa PARK. Her iki hız da yok/eskimişse UNKNOWN ve sürüş güvenliği açısından video kilitli.
- Source priority: taze OBD ≤3 sn > manuel SoC > demo > yok. OBD SoC varsa ama eskimiş ve manual değer yoksa veri değeri gizlenir, ESKİ rozeti gösterilir. OBD PID verified=false; başarı gerçek araç doğrulaması değildir.
- Güç kW ancak hem voltaj hem akım ayrı ayrı ≤3 sn tazeyse gösterilir; farklı yaşlı örnekler çarpılıp güncel sonuç gibi gösterilmez.
- SÜRÜŞ anında yazı büyük: 112sp hız; PARK/UNKNOWN'da 96sp. Layout sabittir; animasyon kullanılmaz.
- SÜRÜŞ modunda Plan/Medya/Yolcu Video/Mola/Ayarlar sağ eylemleri kilitli. UNKNOWN modunda da ekran videosu kapalı. Rota giriş noktası korunur; karmaşık hedef girişi UI-5'te ek olarak kontrol edilecek.
- Mod sürüşe geçerse açık Plan/Yolcu Video/Ayarlar ekrandan çıkılır, oyuncu pause edilir. Var olan video hız kilidi ayarı (varsayılan kapalı) silinmedi; ana ekran güvenlik kilidi ayrı bir koşuldur.
- App tarafındaki ilk altı sabit sekme ve onlara ait dış açılışlar aynı kalır; sürüş moduna aykırı dış bağlantı istekleri de atlanır.

## Mola resmî yol / doğrulama
| Mola işlemi | Sonuç |
| --- | --- |
| APK, UI, izin, iç bileşen | DEĞİŞMEDİ |
| Resmî Android launchIntent çağrısı | UI-3'te YOK; UI-4'te kurulacak |
| Telefon üzerinde launch intent / dönüş | DOĞRULANMADI |
| Android Auto araç ekranında video veya yansıtma | YOK / YASAK |

## Güvenlik/test
- Testler: 5 km/sa GPS eşiği, OBD >0, tam 3000ms tazelik, demo izolasyonu, volt+akım ayrı tazelik, OBD>MANUEL>DEMO, ESKİ ve mod değişikliğinde panel izinleri.
- Gerçek ELM327 PID değeri ve sürüş kilidi fiziksel davranışı telefon/ID.4 saha testine tabidir; test senaryoları sentetiktir.
- Yeni izin, üçüncü taraf bağımlılık, ağ isteği, cihaz tanımlayıcısı veya kalıcı konum kaydı eklenmedi.
- PR merge: Actions testDebugUnitTest, assembleDebug, 16KB ZIP/ELF ve public test sertifikası PASS olmadan yapılmaz.

## Risk ve geri alma
- GPS/OBD hızı 0 bildirse bile gerçek araç Ready/Park durumu her zaman kesin belirlenemez; hız UNKNOWN iken karmaşık ekranlar güvenli kapalı tutulur.
- Android 17 foreground service kısıtları ve fiziksel Bluetooth klonu DOĞRULANMADI.
- OBD timestamp alanı yalnız volatil durumdur; mevcut datalogs formatı değiştirilmedi. PR revert ile geri alınabilir.
