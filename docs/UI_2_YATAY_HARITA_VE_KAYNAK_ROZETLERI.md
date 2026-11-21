# UI-2 — Tesla üç panelli yatay ana ekran

21.11.2026. POCO F5, ID.4 ve Mola çalışma zamanı: **DOĞRULANMADI**.

## Yapılanlar
- MainActivity artık ana Gösterge sekmesinde TeslaDashboardScreen'i açıyor, altı mevcut sekmenin indeksleri değişmiyor. Ana göstergeye dönüldüğünde üstteki eski CockpitTabs gizleniyor, diğer sayfalarda sekmeler korunuyor.
- DriveModeScaffold yatay 700dp+ genişlikte 26% araç paneli / 54% harita / 20% eylem şeridi kullanır. Küçük/dikey cihazlarda kaydırılabilir dikey düzen. UI-3 sürüş kilidi ayrıca uygulanacak.
- Araç paneli canlı VehicleState, SocReading ve mevcut SpeedSelector kaynağıyla besleniyor; batarya kullanılabilir kapasitesi kullanıcının VehicleProfileStore ve SoH ayarından geliyor. Enerji hesaplaması yüzde × kapasite temel özdeşliği; yeni menzil algoritması yok; menzil değeri kanıt yoksa boş.
- Harita mevcut MapLibre ve OpenFreeMap servisini kullanır; koyu harita stili https://tiles.openfreemap.org/styles/dark ; servis sağlayıcı kaynak: https://github.com/hyperknot/openfreemap-styles . Gerçek internet erişimi ve katman yükleme saha testine bağlı.
- Rota çizgisi 8px yeşil, geçilen bölüm 8px gri. Yalnız taze GPS rotaya 250m içinde eşleştiyse geometri bölünür; değilse yolun tamamı gelecek rota olarak bırakılır. Geçilen kısım oranı tahmini geometri mesafesidir, hassas şerit/otonom sürüş verisi değildir.
- MapHeadlineBar: hedef, kalan/ilk rota mesafesi, sağlayıcı sürelerinden türetilen tahmini varış saati, plan varsa tahmini varış SoC. Trafik yok, tazelik veya konum doğrulanmadıysa toplam rota olduğu yazılır.
- NextStopBar: planlanan bir sonraki şarj durak adını, model kW ve şarj süresini, hedef SoC'yi ve zorunlu 'doluluk bilinmiyor' açıklamasını gösterir. Tıklama istasyon veri kaynağı ayrıntısını açar.
- ActionRail: Rota, Plan, Medya, Yolcu video, Mola, Ayarlar. Medya/Yolcu video bu ara aşamada mevcut telefon yolcu ekranını açar; bağımsız medya servisi eklenmedi. Mola eylemi UI-4'ten önce pasif bırakıldı.
- Akışlar mevcut ObdViewModel / NavigationViewModel / PlannerViewModel StateFlow okumalarından geliyor; yeni internet isteği, APK, şarj verisi kazıma ve üçüncü taraf kütüphane yok.

## Mola resmî yol ve doğrulama durumu
| Ölçüt | UI-2 kararı |
| --- | --- |
| Mola uygulamasına çağrı | HAYIR, Mola kartı pasif |
| Resmî Android entegrasyonu | UI-4 için planlı, UI-2'de YOK |
| Mola APK/tasarım/menu/izin değişti mi? | HAYIR |
| Telefon açılış intent'i doğrulandı mı? | HAYIR / DOĞRULANMADI |
| Android Auto araç ekranında açılma | DOĞRULANMADI, bu geliştirmede yok |

## Riskler
- Android 17/LineageOS 24 cihaz görüntüsü ve MapLibre dark stilinin cihazda açılması DOĞRULANMADI.
- Tüm ETA, varış SoC, rota mesafesi ve istasyon bilgileri TAHMİN; canlı trafik, anlık doluluk veya fiyat kaynağı yok.
- 32-bit MapLibre 16 KB uyumsuzluğu önceki W1 denetiminde kalan bilinen risktir; UI-2 değiştirmedi.
- Daha önceki OnlineMap içinde stil/harita yükleme AndroidView tarafından yönetilir; yeni internet istemcisi eklenmedi. Harita hata kartları UI-5'te.
- Sürüşte eylem kısıtlama UI-3 sorumluluğu; mevcut yolcu ekranı hız kontrolü korunuyor.

## Test ve kabul
- DashboardPresentationTest: kaynakları ayırma, eksik/DEMO durumları, SoH ile enerji, ETA ve sonraki istasyon seçimi.
- RoutePolylineProgressTest: konum yoksa hiçbir yol geçmiş sayılmaz, yarım yolda bölümleme ve bitiş.
- Compose @Preview: yatay üç panel ve dikey düzen, statik önizleme DEMO olarak etiketli.
- TestDebugUnitTest + assembleDebug + 16KB audit yeşil olmadan main'e birleştirme yok.
- Fiziksel kabul UI-5 sonrası Nightly yatay/dikey ekran görüntüleriyle alınacaktır.
