# Mola ile Rota • EV Cockpit açma — 22.11.2026

## Neden ayrı başlatıcı?
Kullanıcının paylaştığı Mola ana ekranı TV, Youtube, Ekran Yansıtma, Radyo,
Medya, Telefonu Kullan, Web, Yol Tarifi, Arabam Nerede, Favoriler,
Kısayol Ekle ve Ayarlar gibi **Mola'nın kendi** kartlarını içeriyor.
Mola'nın yayınlanan APK'sında bir eklenti/özel ana menü kartı ekleme
sözleşmesi yok. Bu PR Mola menüsüne yeni kart eklediğini iddia etmez.

## Uygulanan, doğrulanabilir kapsam
- EV Cockpit'e yeni bir **Android MAIN/LAUNCHER activity** eklendi:
  `app.evcockpit.integration.RouteLauncherActivity`.
- Sistem uygulama listesinde ayrı `Rota • EV Cockpit` başlatıcı girişi
  gösterebilir. Mola'nın kendi Telefonu Kullan/uygulama seçicisi Android
  launcher etkinliklerini listeliyorsa bu giriş orada da görünebilir.
- Bu giriş var olan `evcockpit://open/route` güvenli deep linkiyle
  mevcut `MainActivity` rota sekmesine yönlendirir.
- `MainActivity` mevcut tek-sekme harici açma filtresi ile hız/sürüş
  güvenlik kurallarını uygulamaya devam eder.
- Yeni Bluetooth/GPS izni, servis, internet isteği, medya çalma veya
  Android Auto custom template yoktur.
- Mola APK'sı, ana kartları, TV, video, ekran yansıtma ve ayarları
  **değiştirilmedi**.

## POCO F5 / LineageOS / ID.4 park halinde kabul
1. PR'ın Actions `:app:testDebugUnitTest :app:assembleDebug` ve 16 KB /
   APK imza denetimleri başarılı olmadan APK'nın çalıştığını varsayma.
2. Test APK'yı yükle; telefonda `Rota • EV Cockpit` simgesini bul.
   Dokununca mevcut **Rota** ekranını açtığını doğrula.
3. Mola'daki `Telefonu Kullan` > uygulama seçicisini aç; ayrı
   `Rota • EV Cockpit` girişi varsa seç. Yoksa Mola uygulama
   listesinin launcher alias/etkinlikleri göstermediği not edilir.
4. `Kısayol ekle` **web sitesi** özelliğinin `evcockpit://`
   URI'sini Android uygulamasına geçirebildiği kanıtlanmadı; test
   edilmeden kullanıcıya çalışır bağlantı olarak sunulmaz.
5. Aracın Android Auto ekranında rota görünmesi, üçüncü taraf uygulama
   ekranının otomatik aktarılması anlamına gelmez. Mola'nın gösterme
   davranışı ancak gerçek cihazda kabul edilebilir.

## Geri alma
Bu dalın PR'ını tek revert ile geri al; Mola'ya müdahale gerekmez.
