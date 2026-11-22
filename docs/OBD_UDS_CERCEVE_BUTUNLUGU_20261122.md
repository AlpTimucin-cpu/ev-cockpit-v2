# MEB UDS yanıt ayrıştırma: CAN çerçevesi ayırma / salt-okunur tanılama

22.11.2026 · PR baz: `quality/obd-value-validation-20261122`

## Hata riski

Önceki `Elm327.parseUds()`, ELM327 yanıtındaki tüm okunabilir
heksadesimal satırları **tek bir metinde birleştiriyordu**. Farklı
CAN yanıtlarının sonu ve diğerinin başı, beklenen UDS `62 + DID`
örüntüsünü tesadüfen oluşturabilirdi. Ayrıca bir pozitif SID/DID
görüldüğünde sonrasındaki başka bir satırdan gelen baytlar veri
olarak kullanılabiliyordu.

## Yapılan iş

- Her okunabilir yanıt satırı **ayrı değerlendirilir**, komşu
  satırların baytları birleştirilmez.
- ELM327 komut echo'su, `0:` gibi satır numarası, 11-bit
  (`7E8`) ve 29-bit (`17FE007B`) yanıt başlığı ayıklanır.
- Beklenen servis (UDS 0x62) + DID **bayt hizalı** ve tam olarak
  bulunmalı; ardından en az 1 tam veri baytı gelmelidir.
- Geçerli tek satır bulunduğunda, başka ECU/çerçevenin verileri
  onun içine katılmaz.
- Tanı motorundaki pozitif UDS kanıtı da aynı filtreden geçer.
  `OK`, isteğin echo'su, `NO DATA`, eksik `62+DID` artık
  başarılı ECU PID kanıtı sayılmaz.
- `Mode01` pozitif yanıtlarında da farklı satırları birleştirme
  kaldırıldı.

## Kapsam dışı ve sınırlar

- ISO-TP çoklu çerçeve reassembly bu değişiklikte **yapılmadı**.
  Çoklu paket yanıtında ek araç/ELM klon doğrulaması gerekir.
- Bu ayrıştırıcı ECU başlıklarını kriptografik olarak doğrulamaz;
  araca özgü 29-bit RX filtre desteği yine ELM klonuna bağlıdır.
- Her PID'nin ölçek ve veri tipinin doğruluğu **verified=false**
  kalır. Sayısal kalite filtresi (önceki PR) ayrıca çalışır.
- Yeni CAN/UDS komutu, ECU yazma/kodlama, yeni sistem izni yoktur.
  Mola APK, Mola menüleri, oynatıcı ve EV Cockpit tasarımı değişmez.
- SBF APK'sından özel PID/algoritma/veritabanı taşınmadı.

## Test

- Farklı satırlarda birleşerek sahte `62028C` oluşturan örnekler RED
- Tek 11-bit/29-bit çerçeveyle doğru `62028C + veri` OK
- İstek echo'su, eksik veri, yanlış DID, bayt kayması RED
- Pozitif tanı raporu için aynı koşullar
- Github Actions JUnit, APK, 16KB ve test imza kontrolleri
- Gerçek POCO F5 / ID.4 / ELM327 veri doğruluğu **saha testi bekler**
