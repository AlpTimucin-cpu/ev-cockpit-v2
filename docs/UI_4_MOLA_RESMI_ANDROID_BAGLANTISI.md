# UI-4 — Mola resmî Android uygulama bağlantısı

21 Kasım 2026 · Uygulama: ID.4 Yol Arkadaşı (`app.evcockpit`) · Fiziksel telefon/araç sonucu **DOĞRULANMADI**.

## Sınır
Bu PR Mola uygulamasına **HİÇBİR** değişiklik yapmaz. Mola APK/manifest/izin/menü/oynatıcı/ikon/bileşen/içerik sağlayıcı/dahili URI/bellek/veri erişimi yoktur. Fermata/Mola bytecode veya kaynak kod kopyalanmaz. Ekran yansıtma/araçta video ya da sürüş kısıtlarını kaldırma yoktur.

## Resmî Android mekanizması
- AndroidManifest `<queries>` yalnız `me.app.fermataX.auto.mola` ve `me.app.fermataX.auto.mola.debug` paketlerinin **görünürlüğünü** ilan eder. Yeni runtime izni verilmez.
- `PackageManager.getPackageInfo` ile kurulu paket tespit edilir; `PackageManager.getLaunchIntentForPackage` ile yalnız kamuya açık MAIN/LAUNCHER bileşeni istenir. Başlatma standart `Context.startActivity` içindir; Mola özel aktivitesinin adı veya eylemi hiç kodlanmaz.
- Paket kurulu ve başlatılabilir ise `Mola'yı aç`; kurulu değilse `Mola kurulu değil` ve yalnız kullanıcının dokunmasıyla `https://github.com/moris545454/mola-apk/releases` açılır. APK **otomatik indirilmez veya kurulmaz**.
- Android `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` yalnız kurulu/başlatılamayan Mola için kullanıcı dokunmasıyla açılır. İzinler otomatik değiştirilmez.
- Mola'dan dönüş sistem geri/uygulama seçici yoluyla gerçekleşebilir; `Lifecycle ON_RESUME` geldiğinde EV Cockpit Mola kurulum durumunu yeniden okur.
- EV Cockpit'e harici dönüş için var olan yalnız altı `evcockpit://open/...` parametresiz bağlantı ve `MolaOpenContract` whitelist'i geçerlidir; yeni rota/OBD komutu/medya otomatik başlatılmaz.
- Mola kartı yalnız PARK modunda; hız UNKNOWN ise Mola açma işlemi başlatılmaz. Android Auto arayüzüne uygulama/medya/yolcu video eklenmez.

## Test matrisi
| Durum | Beklenen | Kanıt türü |
| --- | --- | --- |
| Orijinal Mola paketi kurulu | Public launcher isteği, kullanıcı eylemi | Fake gateway JUnit; cihaz DOĞRULANMADI |
| Eski `.debug` paketi kurulu | Ayrı kimlikle algılanır | Fake gateway JUnit; cihaz DOĞRULANMADI |
| İki paket yok | `Mola kurulu değil` ve resmî rehber | Fake gateway JUnit |
| Paket var ama launcher yok | `Mola açılamadı`, Android uygulama ayarları | Fake gateway JUnit |
| startActivity hata verdi | Türkçe hata, EV Cockpit kapanmaz | Fake gateway JUnit |
| Dışarıdan geçersiz paket istendi | Başlatma reddi | Fake gateway JUnit |
| Mola'dan geri dönüldü | ON_RESUME durum yenilemesi | Kod incelemesi, cihaz DOĞRULANMADI |
| Hareket hâlindeyken Mola kartı | Kapalı | DriveModePolicy JUnit |

## Resmî Android kaynakları
- https://developer.android.com/training/package-visibility/declaring
- https://developer.android.com/reference/android/content/pm/PackageManager#getLaunchIntentForPackage(java.lang.String)
- https://developer.android.com/reference/android/provider/Settings#ACTION_APPLICATION_DETAILS_SETTINGS
- Mola kamuya açık yalnız APK yayını: https://github.com/moris545454/mola-apk/releases . Kaynak kod/eklentiler yayımlanmadı.

## Riskler / DOĞRULANMADI
- Mola 4.8.x sürümlerinde herkese açık launcher intent’i telefonun ROM sürümüne göre farklı davranabilir. Debug paket launchable olmayabilir.
- Android 17 LineageOS 24 / POCO F5 gerçek `startActivity` sonucu; Android Auto'da çoklu navigasyon listesi; EV Cockpit’in araç ekranında görünmesi **DOĞRULANMADI**.
- UI resmi launcher'a yalnız dokunur, Mola oynatması/izinlerine müdahale etmez. Başarılı `startActivity` gerçek çalışma veya birlikte ekran görünümü kanıtı değildir.
- Google Play hizmetleri, Shizuku, Accessibility, overlay, MediaProjection, `WRITE_SETTINGS`, root izni gerekmez/istenmez.
- Geri alma: UI-4 PR revert. Mola dosyası değişmediğinden geri alma sırasında Mola uygulaması/ayarları olduğu gibi kalır.
