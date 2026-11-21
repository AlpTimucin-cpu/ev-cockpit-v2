# W3 — M8.5e gerçek durum ve küçük eksik: tercih edilen ağlar

Tarih: 21.11.2026. Referans: Birleşik Ana Görev v2 W3 ve repo main. **Saha/telefon performansı DOĞRULANMADI.**

## Denetim sonucu
1. **DC varsayılanı:** `VehicleProfile.defaults()` içinde `maxDcKw=100.0 kW`, `verified=false`, kullanıcı tarafından girilen `observedDcKw` ile eğriyi ölçekler. Araç ve üretim yılı kesinleşmediği için bu tutucu tahmini değiştirilmiyor. VW 2023 resmi basın metni 52 kWh için 115 kW ve daha önce 110 kW değerlerini, Türkiye 2024 katalogu ise farklı yapılandırma için 145 kW ilan eder; **tek doğru araç değeri çıkmaz**.
2. **Aday budama:** `ModelParameters.pruneMaxPerWindow=6` zaten mevcut; 40 km dilim başına en çok 6, sentetik ve oracle testleri var. Yeniden yazılmadı.
3. **Hesap süresi:** `PlannerUiState.solverElapsedMs` ve `PlannerScreen` üzerinde `ms` etiketli gösterim zaten var; POCO F5 gerçek ölçümü kullanıcıya kalıyor.
4. **Kalan eksik tamamlandı:** dışlanan ağlara ek olarak kullanıcı tanımlı **tercih edilen ağlar**, `0–5 dk` kullanıcı ayarlı, varsayılan **2 dk** sıralama bonusu. Bu sayı mühendislik ayarı/T A H M İ N, gerçek sürüş veya şarj süresi değildir. `PlannerOptionsStore` ile kalıcı kaydedilir.
5. Tercih yalnız `PlanSolver` maliyet karşılaştırmasını ve `CandidatePruner` altı aday seçimini etkiler; `physicalMinutes`, SoC güvenlik koşulları, belirsizlik bandı ve istasyonun fiziksel kW gücü değiştirilmez.

## Operatör DC varlığı için **resmî kaynak** tablosu
| Ağ | Resmî açık DC kanıtı | Kanıtın sınırı |
| --- | --- | --- |
| ZES | https://zes.net/tr/sarj-istasyonlari | Ağ işletiyor; her OSM noktası doğrulanmadı |
| Eşarj | https://trugo.com.tr/individual/news/trugo-ve-esarj-is-birligiyle-hizli-ve-kesintisiz-sarj-81-ilde-hizmetinizde | Eşarj/Trugo işbirliği, istasyon bazında anlık kanıt değil |
| Trugo | https://trugo.com.tr/individual/news/12-aydir-kesintisiz-pazar-lideriyiz | Ağ faaliyeti, doluluk ve araç gücü değil |
| Voltrun | https://www.voltrun.com/tr/sss | İşletmeci DC CCS2 hizmetini açıklıyor |
| Aksa Şarj | https://www.aksasarj.com.tr/tr/haberler | DC istasyon duyuruları |
| Powerşarj | https://powersarj.com/fiyatlandirma/ | DC şarj tarifesinin varlığı |
| Tesla Supercharger | https://www.tesla.com/tr_tr/support/charging/supercharging-other-evs | Araç erişimi istasyon/ülke bazında ayrıca kontrol edilmeli |

**Kaynağı burada net doğrulanamayan RHG Enertürk, Sharz.net, Beefull vb.** otomatik DC güven kanıtı tablosuna eklenmedi. Kullanıcı manuel tercih metnine bunları yazabilir ama `OperatorHeuristics` bir istasyonun DC olduğunu **asla otomatik onaylamaz**.

## Güvenlik ve kaynaklar
- `SourcedValue` `verified=false` kalır; OSM CCS2 etiketi ayrı değerlendirilmeye devam eder.
- Her kullanıcı tercihi `0–5 dk` sınırlı, 250 karakter ağ filtresi, en çok 12 ağ girişidir.
- Canlı şarj fiyatı, doluluk, özel istasyon uygulaması verisi, ABRP kapalı algoritması yoktur.
- Yeni üçüncü taraf bağımlılık yok, NOTICE değişmedi. İşlemler geri alınabilir; PR revert.
- Android CI ve solver oracle / performans çıktıları geçmeden birleştirme yapılmaz.

## Kaynak belirsizliği (VW DC)
- Volkswagen Newsroom: https://www.volkswagen-newsroom.com/en/press-releases/upgrade-for-the-id4-and-id5-new-infotainment-system-with-intuitive-operation-new-electric-drive-with-more-power-and-efficiency-17728
- Türkiye 2024 ID.4 katalog kopyası (VW kaynak metni, üçüncü taraf arşivi): https://autocatalogarchive.com/wp-content/uploads/2024/12/VW-ID.4-2024-TR.pdf
- ADAC bazı ID.4 Pure varyantları farklı azami değer bildirir: https://www.adac.de/rund-ums-fahrzeug/autokatalog/marken-modelle/vw/id4/1generation/327809/
- Bunlar aynı araç VIN/üretim hafta/sürümünü kanıtlamaz; kullanıcı gözlemi doğrulanmış azami DC sertifikası değildir.
