# OBD okuma kalite kapısı (POCO F5 / VW ID.4 Pure)

Bu özellik üçüncü taraf SBF APK'sından **hiçbir PID, algoritma, özel veri
veya kod kopyalamaz**. SBF incelemesinden çıkan genel veri-güveni ilkesini,
EV Cockpit'in kendi ELM327/VehicleState veri yoluna uygular.

## Amaç

Ucuz ELM327 klonundan gelen UDS yanıtı mevcut yazılım tarafından
çözümlense bile MEB PID adresleri/ölçekleri araç üzerinde henüz
**DOĞRULANMADI**. Ham bayttan üretilen rastgele veya bozulan sayısal
değerlerin canlı SoC, hız, voltaj ve akım kaynağına aktarılması engellenir.

`ObdValueQuality.assess` fiziksel imkânsızlıkları **geniş toleranslarla**
kontrol eder. Kabul edilen değer yine **PID DOĞRULANMADI** statüsündedir;
doğrulama için tek başına yeterli değildir.

## Filtre

| Alan | Kabul edilen aralık | Ek |
|---|---:|---|
| BMS SoC | %0–100 | Son 10 saniyede 20 puanın üstünde sıçrama reddedilir |
| Hız | 0–300 km/s | Test açısından geniş üst sınır |
| Yüksek gerilim | 0–1200 V | 400 V / 800 V mimarileri dışlanmaz |
| Akım | −5000 … +5000 A | Şarj/çekiş işareti korunur |
| Tanımlanmamış rol | Kabul edilmez | Yeni/ham `log` rolü UI telemetrisi değildir |
| NaN / Infinity | Kabul edilmez | Yerine değer üretilmez |

Bunlar üreticinin ID.4 resmî sınırları değil, test amaçlı geniş yazılım
sağlamlık sınırlarıdır. Şüpheli değer hiçbir hesap için yazılmaz.
Önceki geçerli değerin **zaman damgası yenilenmez**; mevcut 3 saniyelik
`DriveModePolicy` veri eskime değerlendirmesi çalışmaya devam eder.

## Testler ve güvenlik

- Sentetik normal aralık, negatif akım, NaN, infinity, aşırı değerler
- Anlık SoC sıçrama, eşik değeri ve 10 saniye üstü yeniden bağlanma
- OBD gönderim komutları değişmez, yeni ECU yazma/servis/izin eklenmez
- Mola'nın kendi APK'sı, ekranı, medya ve Android Auto bileşenleri değişmez
- GitHub Actions `:app:testDebugUnitTest :app:assembleDebug`, 16 KB, imza
- Kullanıcının POCO F5 / araç / ELM327 gerçek ölçüm doğrulaması **ayrıdır**

## Saha kabulü

1. Önce PR'nin GitHub Actions sonucu yeşil olmalıdır.
2. POCO F5'te debug APK'yı kur, araç **park hâlindeyken** OBD Test'i aç.
3. Gerçek adaptörle salt-okunur bağlantı dene. Aracın ekranındaki SoC
   ile OBD değerini ve sıcaklık/hız verisini karşılaştır.
4. `OBD ÖLÇÜMÜ REDDEDİLDİ` varsa veri tipi/rolü raporla; bu red
   adaptörün veya aracın kesin arızalı olduğunu kanıtlamaz.
5. VIN, koordinat, API anahtarı ve ham CAN içeriğini herkese açık
   GitHub issue'suna yapıştırma.
