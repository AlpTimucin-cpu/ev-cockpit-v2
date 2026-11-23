# EV Trip 3 — durak bazında enerji ve şarj bedeli (23.11.2026)

## Kaynak ve dürüstlük
- Türkiye'de güncel şarj fiyatı EPDK Şarj@TR'de soket ve istasyon bazında görüntülenebilir: https://www.epdk.gov.tr/Detay/Icerik/1-3428/enerji-donusumuserbest-erisim-platformu--sarj%40tr
- Bu bilgi tek başına herkese açık, lisanslı, belgelenmiş üçüncü taraf API erişimi değildir. Bu PR hiçbir EPDK veya operatör uygulaması fiyat verisini kazımaz.
- Kullanıcı her işletmeci için operatör sayfasından/Şarj@TR'den kontrol ettiği **MANUEL TL/kWh** gözlemini girer. Bu fiyatlar doğrulanmış canlı tarife değildir.
- Kaydedilen fiyat 14 günden eskiyse ESKİ işaretlenir. Her tarifede yerel kayıt zamanı, operatör anahtarı ve verified=false tutulur. GPS, VIN veya API anahtarı kaydedilmez.

## Mühendislik hesabı
- Bataryaya durakta eklendiği TAHMİN edilen enerji (kWh) = kullanılabilir kapasite × (çıkış SoC – giriş SoC)/100. SoC/şarj eğrisi BMS ölçümü değil, mevcut PlanSolver çıktısı.
- Durak ücreti TAHMİN = batarya enerji artışı × kullanıcı TL/kWh. Bu ilk hesapta ek kayıp/pay eklenmez; ölçülen fatura enerjisi istasyona göre farklı olabilir.
- Rota tüketimi (kWh): mevcut RouteEnergyPrefix fizik modelinden gelir; UI içinde yeniden hesaplama YOK.
- Bir durakta fiyat bilinmiyorsa genel toplam NULL/--, eksik durak 0 TL sayılmaz. Kısmi fiyat biliniyorsa yalnız bilinen ara toplam var.
- Başlangıçta bataryada mevcut elektriğin alış bedeli, işgal ücretleri, zaman tarifeleri ve fiyat güncellemeleri toplamda değildir. Sıfır durakta yolda şarj ödemesi 0 TL ama toplam ekonomik seyahat maliyeti 0 değildir.
- 150 TL/kWh üstü veya sıfır/geçersiz fiyat kabul edilmez. Ek şarj ücretleri istenirse ayrıca modellenmeli.

## Kod ve test
- TripChargeCost.kt: pure hesap, fiyat kaynağı, 14 gün eskime, toplam/kısmi ayrımı.
- ManualChargeTariffStore.kt: DataStore özel alanda en çok 30 operatör/tarife.
- TripChargeCostViewModel.kt: StateFlow, model route kWh hesapları ve tarifeyi saklama; UI ağ çağrısı yapmaz.
- TripChargeCostPanel.kt: Plan ekranında durak listesi, kaynaklı manuel fiyat girişi, EPDK resmî rehber açma ve toplam TL.
- TripChargeCostTest: batarya delta, bilinmeyen fiyatı sıfır saymama, 14 gün, kodlama, aşırı fiyat ve kaynak.
- Yeni runtime Android izni, büyük bağımlılık, Mola değişikliği, araç ekranında video yok.
- Actions ve telefon kabulünden önce UI/Android 17/POCO F5 fonksiyonel çalışma DOĞRULANMADI.

## Sonraki doğrulanabilir iş
- Harita katmanları: OpenFreeMap Dark/Bright/Liberty, persist edilen seçim, zorunlu OSM/MapLibre atıfı.
- Güzergâh şarj koridorunda Overpass'tan dönen adayların hepsinin ayrı listesi, mesafe/kW/CCS2/güven bilgisi; OSM kapsama eksikliği açık.
- Canlı trafik: ücretsiz planın yeniden dağıtım, kota ve lisans izinleri doğrulanmadan sahte trafik şeridi yok.
