# Mola 4.8.3 içinde EV Cockpit'e geçiş — Mola tasarımı %100 korunur

## Kullanıcı kararı
**Mola ana uygulamadır.** Mola'nın orijinal APK'sı, ekran düzeni, renkleri, butonları, navigasyon menüleri, iconları, oynatıcısı ve tüm sınıfları değiştirilmez. EV Cockpit'in kendi Tesla-benzeri arayüzü de korunur. Android Auto sürüş kısıtları aşılmaz.

## Gerçekte mevcut olan entegrasyon yüzeyleri

Mola 4.8.3 kullanıcı APK'sında statik olarak doğrulandı:
- Paket: `me.app.fermataX.auto.mola`.
- `PhoneScreenView` içinde `AppsAdapter`, `toggleApps`, `showChooser`, `launch`, `launchPendingOrOfferApps`, `tryCreateVirtualDisplay`, `fallbackToMirror` metotları.
- `WebBrowserAddon` ve `WebBrowserFragment` sınıfları; yer imleri ve adres açma kodları.
- Mola yayın deposunda kaynak kod/halka açık eklenti sözleşmesi bulunmuyor: https://github.com/moris545454/mola-apk
- Bu kod izleri Mola'nın kendi var olan telefon uygulaması seçicisi ve web tarayıcısı ile uyumluluk potansiyelini gösterir. POCO F5 + LineageOS + Android Auto'da üçüncü taraf EV Cockpit penceresinin başarıyla görüntülendiği **doğrulanmadı**.

## Bu PR ile EV Cockpit tarafında yapılanlar

- `MolaOpenContract` adlı **bağımsız, izin kısıtlamalı** bağlantı protokolü eklendi.
- EV Cockpit mevcut `MainActivity` şu `ACTION_VIEW` deep linklerini kabul eder:
  - `evcockpit://open/dashboard` — mevcut ana gösterge
  - `evcockpit://open/obd` — mevcut OBD Test
  - `evcockpit://open/route` — mevcut rota
  - `evcockpit://open/plan` — mevcut şarj planlama
  - `evcockpit://open/passenger` — mevcut, **telefonla sınırlı** yolcu video sekmesi
  - `evcockpit://open/settings` — mevcut ayarlar
- Açık bağlantı sadece **mevcut sekmeye geçer**. Sahte GPS verisi üretmez, CAN komutu göndermez, OBD bağlantısı başlatmaz, videoyu otomatik oynatmaz, konum/rota/veri paylaşmaz ve Mola'nın sistem izinlerini kullanmaz.
- `onNewIntent` ve `singleTop` sayesinde uygulama açıkken tekrar açılma da ele alındı.
- `MainActivity` içindeki mevcut ekranlar, `CockpitDesign.kt` ve `PassengerVideoScreen.kt` görsel olarak değişmedi. Yeni Mola butonu, menü, sekme, ikon, overlay veya tema eklenmedi.
- Mola APK'sı depoya eklenmedi, yeniden derlenmedi veya imzalanmadı. Bu **Mola kodunu kendi uygulamamıza kopyalama** işi değildir.

## POCO F5 üzerinde manuel deneme (araç park hâlinde)

1. Orijinal Mola 4.8.3 ve bu PR'ın CI tarafından derlenmiş yeni EV Cockpit debug APK'sı telefonda kurulu olmalı.
2. Mola'yı normal yoldan başlat. Mola'nın zaten var olan **Telefon / Telefon uygulamaları** seçicisini aç (isim sürüme göre farklı olabilir).
3. Listedeyse `ID.4 Yol Arkadaşı` uygulamasını seç. Mola'nın kendi seçici/ekran aktarım mekanizması uygulamayı açmayı deneyebilir. Mola'nın herhangi bir menüsü değiştirilmez.
4. EV Cockpit açılırsa OBD / Rota / Plan sekmelerini deneyerek dokunma, ekran oranı ve geri tuşu davranışını gözlemle. Araca bağlı OBD/GPS doğruluğu ayrıca test edilir.
5. Harici bağlantı gönderebilen bir uygulama varsa `evcockpit://open/plan` URI'si EV Cockpit'i doğrudan Plan sekmesinde açmalıdır.
6. Mola ekranında EV Cockpit listelenmiyorsa veya açılmıyorsa bu gerçek bir uyumluluk engelidir; **Mola'nın onayı olmayan özel API'lerini taklit ederek** güvenlik kısıtları kaldırılmayacak.

## Sınırlar / bir sonraki eşik

- **Gerçek anlamda Mola içindeki yerleşik bir sekme olmak** için Mola geliştiricisinin yayımlayacağı bir SDK, desteklenen eklenti API'si veya kaynak kod ve yeniden kullanım izni gerekir. Bunlar mevcut değildir.
- `PhoneScreenView` üzerinden açma, Mola'nın tekil APK'sına EV Cockpit bytecode'unun gömüldüğü anlamına gelmez.
- Mola'nın kendi tarayıcısının özel `evcockpit://` şemasını yönlendirip yönlendirmediği doğrulanmadı; güvenilir yol onun mevcut uygulama seçicisidir.
- Android Auto ekranında hareket sırasında video, DRM atlama, erişilebilirlik üzerinden otomatik izin onayı ve Shizuku ayrıcalıklarını kopyalama bu işin kapsamında değildir.
- Android 17 ve ID.4 12.9 inç ekran üzerinde **saha kabul testi** gerekir. Bu testler yapılmadan kusursuz entegrasyon iddiasında bulunulamaz.

## Kaynak kod/bütünlük

Kaynak Mola APK SHA-256 (20.11.2026 yayını):
`124e9121c41a624893b0f0794854f3d5ba4fc8a29a15b051c3f4bc376d8fd149`

Bu PR Mola APK'sını değiştirmediği için bu SHA aynı kalır. Test PR'ı doğrudan `main` dalı üzerine açılır; daha önce kullanıcı isteği dışında eklenen Mola/Radio UI değişikliklerini içermez.
