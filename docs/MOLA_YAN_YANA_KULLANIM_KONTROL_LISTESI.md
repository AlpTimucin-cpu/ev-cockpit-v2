# Mola + ID.4 Yol Arkadaşı — yan yana kullanım kontrol listesi

**Kapsam:** Yalnız Mola'nın normal kurulu hâlinin yanında **ayrı** `app.evcockpit` uygulamasını kullanma. Mola'nın APK'sı, arayüzü, ayarları, veri alanı ve imzası **değiştirilmez**. Mola radyo, IPTV, YouTube, sesli komut ve Android Auto medyası EV Cockpit'te yeniden uygulanmaz.

## Paketleri karıştırma

| Ad | Paket kimliği | İşlem |
| --- | --- | --- |
| Mola 4.8.3 (kullanıcının referansı) | `me.app.fermataX.auto.mola` | Ayrı kurulu tut; güncelleme ve izinleri sürümden sürüme yeniden değerlendir |
| Eski Mola debug (olabilir) | `me.app.fermataX.auto.mola.debug` | **Aynı ürün gibi varsayma**; birden fazla uygulama/yanlış kısayol karışabilir |
| ID.4 Yol Arkadaşı | `app.evcockpit` | Yalnız resmi depodaki doğrulanmış `nightly` TEST APK; Mola'yı değiştirmez |

## Telefonda kurulum ve doğrulama (araç park hâlindeyken)
1. Mola'nın paket kimliğini telefon **Ayarlar → Uygulamalar → Mola → Uygulama bilgileri** üzerinden kontrol et; `.debug` ile karıştırma. Telefon arayüzü paket adını göstermiyorsa kimliği tahmin etme.
2. Mola'yı silmeden ya da üzerine farklı imzalı dosya yüklemeden, EV Cockpit'in **ayrı** `app.evcockpit` APK'sını yükle. Kullanılan APK'nın public debug imzası olduğu için yalnız test sürümüdür.
3. EV Cockpit'i bir kez telefon uygulama çekmecesinden aç; OBD/GPS/şarj verisinin gerçekten okunduğunu varsayma.
4. Mola'nın mevcut, **orijinal** Telefon/Uygulamalar seçicisinden ID.4 Yol Arkadaşı görünüyorsa seç. Bu yöntem Mola içinde görünebilir ama **saha kanıtı yoktur**. Yeni buton/menü eklenmez.
5. Tek başına EV Cockpit açılış kontrolü için izinli bağlantılardan birini kullanan uygulama `evcockpit://open/plan` URI'sini açabilir. Diğer izinli yollar: `dashboard`, `obd`, `route`, `passenger`, `settings`. Bağlantılar yalnız mevcut sekmeyi açar.
6. Ekran yansıtmada siyah ekran, izin sorusu, kesinti varsa Mola sistemi veya Android Auto sınırlarını aşmaya çalışma; **DOĞRULANMADI** olarak not et.

## İzin ve güncelleme riskleri
- Mola'yı kendi resmî yayınından güncelle; her güncellemede paket kimliği, imza ve erişilebilirlik/mikrofon/ekran paylaşımı gibi izinleri yeniden incele.
- `WRITE_SETTINGS`, `WRITE_SECURE_SETTINGS`, root, Shizuku, erişilebilirlik, MediaProjection, overlay veya otomatik izin tıklama bu projede kullanılmaz.
- Mola'nın veri dizini, hesapları, abonelikleri, ayarları ve güncelleyicisi EV Cockpit tarafından okunmaz/değiştirilmez.
- EV Cockpit debug imzası ve keystore'u `ci/public-debug-ec.p12.b64` ile public olduğu için doğrulanmış üretim/kimlik garantisi değildir.
- Android Auto'nun sürüş dikkat kısıtları geçerlidir; hareket hâlinde sürücü video izleme akışı sunulmaz.

## Güvenli geri alma
- Bu bağlantıyı istemiyorsan EV Cockpit'i kullanma veya cihazdan **yalnız `app.evcockpit` uygulamasını** kaldır. Mola aynen kalır.
- Mevcut EV Cockpit verilerini korumak istersen yeni APK kurmadan önce plan/adres/SoC yerel ayarlarının yedeğini kendin doğrula; uygulama kaldırma verileri silebilir.
- Yanlış `.mola.debug` sürümünü kaldırmadan önce bunun farklı paket olduğundan ve kendi verilerinden emin ol.
- PR sürümünü kaldırıp önceki Nightly'ye dönüş yalnız aynı paket kimliği ve imza ile ve Android'in sürüm düşürme kuralları uygunsa denenebilir; otomatik downgrade garantisi yok.

## Kabul
- Mola açılıyor ve tasarım değişmemiş.
- EV Cockpit ayrı uygulama olarak açılıyor.
- Mola uygulama seçicisi EV Cockpit'i gösteriyor mu? **KULLANICI TESTİ BEKLENİYOR.**
- Araç ekranında çalışıyor mu? **KULLANICI TESTİ BEKLENİYOR.**
- OBD ve PID ölçekleri doğrulandı mı? **HAYIR / DOĞRULANMADI.**

Kaynak: [Mola yayın deposu](https://github.com/moris545454/mola-apk) • [EV Cockpit PR #71](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/pull/71).
