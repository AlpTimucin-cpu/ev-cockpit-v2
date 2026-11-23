# Aşama F — KESİN ana ekran / HMI şartnamesi (v3, 23.11.2026)

**Durum: Kullanıcının son kararı, önceki “madde 10” ve iki eski EK'in yerine geçer.**
Bu belge ekranın **hedef tasarım ve kabul koşullarıdır**; şu aşamada A–C kodunun F görünümüne taşındığını iddia etmez.

**Depo:** AlpTimucin-cpu/ev-cockpit-v2
**Araç profili:** VW ID.4 Pure, kullanıcı profili kullanılabilir **52 kWh**; tahrik gücü 125 kW ile şarj gücü birbirine karıştırılmaz.
**Platform:** Jetpack Compose telefon Activity + MapLibre. Telefon dikey ve 12,9 inç benzeri geniş yatay pencere adaptif desteklenecek.
**Görsel dil:** Tesla benzeri harita-öncelikli, EV Cockpit'e özgü, koyu; mavi vurgu; yuvarlatılmış kartlar; büyük Türkçe tipografi; **hız birimi yalnız `km/sa`**.

## 0. Yetki ve sürüm önceliği
- Bu, **tek geçerli ana ekran HMI hedefidir.** Önceki “Araç, Harita, Müzik, Enerji, Ayarlar” menüsü ve önceki klima/müzik/araç görseli içeren görsel taslaklar HÜKÜMSÜZDÜR.
- EV Cockpit yalnız bilgi, tahmin ve rota/şarj planı gösterir. Klima, kilit, koltuk, kamera, medya/telefon kontrolü veya durum paneli **yer almaz**.
- Mola APK, menüler, ikonlar veya arayüzü değiştirilmez. Uygulamanın F ana ekranına Mola kontrol paneli eklenmez.
- Önceki B/C hizmetlerini kaldırma: OSRM gerçek alternatifleri, Photon/ilçe önerileri, MapLibre katmanları, OCM kayıtları, enerji modeli ve şarj planı arka planda çalışmaya devam eder.

## 1. Yatay yerleşim: 12,9 inç / görsel oran 1672 × 941 (~16:9)
```text
┌────────────────────────────────────────────────────────────────────────────┐
│ SAAT           DIŞ SICAKLIK (Open-Meteo)        BATARYA % · MENZİL        │
├─────────────────────────────────────────────────────┬──────────────────────┤
│  MAPLIBRE HARİTA — ~%65                            │ BİLGİ PANELİ — ~%35 │
│ ┌───────────────────────────────────────────────┐   │                      │
│ │ NEREDEN ······       NEREYE ······            │   │ HIZ   — km/sa       │
│ ├───────────────────────────────────────────────┤   │                      │
│ │ [ok] xxx km sonra / yol adı                  │   │ SoC   Menzil        │
│ │ [+][-][Konum][Katman]                         │   │ kWh/100 km · kW      │
│ │          mavi ana rota / gri alternatifler   │   │ [batarya çubuğu]    │
│ │          şarj istasyonu gerçek işaretçileri  │   │                      │
│ │          trafik yalnız kaynak varsa         │   │ HAVA DURUMU ŞERİDİ  │
│ ├───────────────────────────────────────────────┤   │ rota boyunca        │
│ │ Varış | Kalan km | Varış SoC | Şarj TL      │   │                      │
│ └───────────────────────────────────────────────┘   │ SIRADAKİ ŞARJ DURAĞI│
├─────────────────────────────────────────────────────┴──────────────────────┤
│         HARİTA       ROTA       ŞARJ       ENERJİ       AYARLAR           │
└────────────────────────────────────────────────────────────────────────────┘
```
- Pencere genişliği **dp** ve gerçek kullanılabilir alan üzerinden hesaplanır; 1672×941 piksel yalnız görsel referanstır, dp ile birebir eşitlenmez.
- Geniş yatay pencerede sol harita `weight(0.65f)`, sağ bilgi `weight(0.35f)`. Gerekirse tekil kart boyutları en az 56dp tıklanabilir alanı korumak için yeniden dağıtılır.
- Telefon dikeyde: **harita en üstte**, arama ve alt özet harita ile ilişkilendirilir, ardından sırayla hız, dört metrik, rota havası ve şarj kartı; en altta beş sekme. Alt çubuğu ekran yönüne göre kaybolmayan güvenli alan içinde tut.
- `WindowSizeClass`/WindowManager ile `compact`, `medium`, `expanded` ayrımı; yalnız `isLandscape` değil **kullanılabilir dp** esaslı. Gece/gündüz harita kaynaklarına tek temadan geçilir.
- Tüm birincil butonlar ve dokunulabilir filtre hedefleri **en az 56dp**.
- Ekran okuyucu, odak, büyütülmüş sistem fontu ve boş veri durumları desteklenir; yatayda dahi bir bilgi kartının içeriği taşmaz.

## 2. Üst durum çubuğu
- **Saat:** cihazın gerçek yerel saati (Europe/Istanbul dâhil cihaz zaman dilimi), Türkçe `HH:mm`.
- **Dış sıcaklık:** yalnız gerçek Open-Meteo en yakın uygun konum/son hava kaydından; veri stale veya yoksa `— °C`.
- **Batarya:** doğrulanmış kaynaktan veya belirtilen tahmin modelinden; `%78` türü rakamlar sadece gerçek/demo modunda gerçek kökenden gelir, yoksa `—`.
- **Menzil:** hesaplandığı zaman `xxx km (tahmini)`, yoksa `— km`.
- Ana sayfa, müzik, medya, kamera, klima, kilit, telefon vb. ek eylemler **yok**. Ağ bağlantısı simgeleri zorunlu değil; eklenirse yalnız sistemden gerçek mevcut durum.

## 3. Sol alan: harita
- Gerçek **MapLibre** haritası; demo modunda dahi konum bilgisi gerçekmiş gibi gösterilmez.
- Üstte iki kompakt, açık etiketli alan: **Nereden** ve **Nereye**. En az **2 harfte çevrim dışı il/ilçe önerileri**; 3 harfte **250 ms debounce** ile Photon/Open-Meteo çevrim içi arama. Dış sağlayıcı kotası ve sonuç kalitesi nedeniyle öneri gelmeyebilir, UI açıkça söyler. Öneri seçiminden sonra koordinat istemeden rota.
- Sol üst manevra kartı: doğrulanmış `RouteManeuver` bilgisi varsa yön oku, `xxx km sonra` ve yol adı; yoksa kartı gizle ya da `Yönlendirme yok` boş hâlini göster. Sırf ekran dolsun diye manevra üretme.
- Harita üzerinde **yakınlaştır +**, **uzaklaştır −**, **konuma git**, **katman seçici**. Katmanlar: **Sokak**, **Uydu**, **Arazi**, **Koyu**. Uydu/arazi kullanımında MapTiler anahtar gereksinimi veya kaynak kullanılamıyor durumları açıkça gösterilir; çalışmayan katman aktif gösterilmez.
- Seçili gerçek rota: **mavi** (`#1677FF`), diğer sağlayıcıdan gelen gerçek alternatifler: **soluk gri**; bu, B aşamasında seçili rota için kullanılan yeşilin F aşamasında değiştirilmesidir.
- Gerçek envanterdeki koridor istasyonları: **mavi şimşek rozetli**, `Şarj İstasyonu xx km` (mesafe gerçek rota projeksiyonundan tahmin ise etiketi `≈xx km`). Çalışma/doluluk teyitsizse çalışma garantisi verilmez; istasyon verisi yoksa işaretçi üretilmez.
- **Trafik göstergesi sadece erişilebilen, lisansı uygun, fiilen çalışan trafik katmanı mevcutsa** gösterilir. Aksi halde `Akıcı/Yoğun/Çok yoğun` lejandı dâhil hiçbir renk/sınıf üretilmez. OSRM süresi trafik verisi değildir.
- Harita alt şeridi: **varış süresi, kalan mesafe, tahmini varış SoC, tahmini şarj ücreti**. Eksik veri için `—`, asla sahte sıfır. Ücret yalnız plan + geçerli/tarihli doğrulanabilir tarife kapsaması yeterliyse; kısmi hesap toplam olarak sunulmaz.
- Harita, rota ve charger seçimi etkili; gerçek rota sağlayıcıları hata verdiğinde gösterilen eski rota, yeni sonuç olarak etiketlenmez.

## 4. Sağ alan: yalnız bilgi
1. **GPS HIZI:** büyük `— km/sa` veya güncel/izinli GPS hızından türetilmiş `n km/sa` (GPS m/s × 3,6). Hız için OBD yerine GPS kullanılır; erişim/kayıt eskiliği varsa `—`.
2. **Dört metrik:** Batarya %, Tahmini menzil (km), Enerji tüketimi (kWh/100 km), Anlık güç (kW). Batarya çubuğu gerçek/tahmin SoC ile uyumlu. Rejenerasyon **yeşil**, çekiş **mavi**; **doğrulanmamış OBD güç sinyalinde `—`**, grafiğe rastgele güç verilmez. Tüketim OBD doğrulanmamışsa model tahmini `tahmini` diye işaretlenir. Doğrulanmış OBD yüzdesi ve manuel/hesaplanan yüzdeler birbirine karıştırılmaz.
3. **ROTA BOYUNCA HAVA ŞERİDİ:** gerçek rota geometrisinde yaklaşık **20–30 km** arayla örneklenen noktalar, rota ilerleme/ETA karşılığı Open-Meteo saatlik tahmin, sıcaklık ve yağış uyarısı. API kota/sınır/boş kayıt varsa kısmi uyarı; başlangıç/orta/hedef tekil verilerini yirmi nokta verisiymiş gibi tekrarlama. Veri yoksa kart `Hava verisi yok` der, uydurulmuş ikon/sıcaklık çizilmez.
4. **ŞARJ PLANI:** planlanmış sıradaki gerçek durak, rotadaki yaklaşık mesafesi, önerilen şarj süresi, hedef SoC, tahmini ücret. OCM envanteri ve şarj fiyatı tek başına şarj süre/modeli değildir; durak/ücret yoksa ilgili alan `—`. İstasyon sağlayıcısı dönmediğinde dolu görünen dekoratif kart oluşturma.

**Kesin yasak:** araç görseli/silüeti, lastik basıncı, araç durumu, vites, kamera, müzik, çalar, telefon, iklim/koltuk/kapı/lock kontrolü, keyfi telemetri. Yalnız güvenli bilgi ve rota işlevleri.

## 5. Alt sekme çubuğu — kesin 5 sekme
| Sıra | Etiket | Görev |
| --- | --- | --- |
| 0 | **Harita** | Harita ana ekranı; varsayılan sekme ve mavi vurgulu |
| 1 | **Rota** | Nereden/Nereye, Photon + il/ilçe, alternatif rota/enerji karşılaştırması |
| 2 | **Şarj** | Koridordaki tüm bulunabilen istasyon kayıtları, operatör, güç, soket, fiyat |
| 3 | **Enerji** | Tüketim/şarj maliyeti, SoC grafiği, bütçe ve kaynak güveni |
| 4 | **Ayarlar** | ID.4 Pure 52kWh profil, OBD bağlantısı/testleri, API anahtarları, tariffs.json yenileme ve harita katmanları |

Mevcut uygulamada alt sekme ve bazı araç aksiyonları hâlen farklıdır; F aşamasındaki yeniden düzenleme kod gerektirir. Geçici ara derlemelerde bu belgeyi “zaten uygulanmış” gibi göstermeyin.

## 6. Kaynak doğruluğu, SoC, menzil
- **OBD: yalnız doğrulanmış ve taze PID** değerleri geçerli. Bilinmeyen/klon ELM327 `NO DATA` cevapları `0` değil `unavailable`.
- Başlangıç SoC kullanıcı tarafından manuel girildiyse `MANUEL` olarak kaydedilir. Rota ilerlemesinden model tüketimi **hesaplanabiliyorsa** ekrandaki SoC tahmini:
  `SoC_tahmini(%) = SoC_baslangıç(%) - 100 × model_harcama_kWh / 52 kWh`.
  Model harcaması güvenilir ilerleme olmadan tahmin edilmiyorsa başlangıç SoC değeri yalnız `manuel` olarak kalır; zaman geçtikçe keyfi düşürülmez.
- Tahmini menzil:
  `menzil_km = (SoC_tahmini / 100 × 52 kWh) / (tüketim_kWh/100km) × 100`.
  `SoC_tahmini` ve `tüketim` geçerli/sıfırdan büyük değilse `— km`. Kapasite SoH ile değişmişse aktif araç profilindeki kullanılabilir kapasite kullanılır; 52 kWh temel fabrika profilidir.
- Tüketim modelinin hava, eğim, hız veya HVAC girdi eksiklikleri tahmin kalitesi ve kaynak etiketiyle açıkça gösterilir. `tahmini` ibaresi yalnız bilgi notunda değil, metrik etiketinde görünür.
- Şarj maliyeti: operatör + AC/DC sınıfı + tarihli tarife ve hesaplanan satın alınacak kWh. Bilinmeyen tarife **0 TL değildir**. Fiyatı bulunamayan duraklar varsa **toplam maliyet bilinmiyor/kısmi**.
- Dış sıcaklık/hava için Open-Meteo; hız/konum için GPS; OBD güç için kanıtlı taze değer; konum yoksa simülasyon olmadan yolculuk başlamış gibi gösterme.
- `DEMO` modu gerçek moddan görsel olarak ayrı; demo telemetrisi gerçek menzile, rotaya, maliyet kaydına veya sürüş kanıtına sızmaz. Ana ekranın demo önizlemesinde açık `DEMO — GERÇEK VERİ DEĞİL` etiketi zorunludur.

## 7. Compose mimarisi / F aşaması teslimi
Önerilen dosya sınırları:
- `ui/finalhmi/CockpitDesignTokens.kt`: **tek tasarım kaynağı**: renkler, `16–20dp` kart yarıçapları, metin ölçeği, spacing ve durum renkleri.
- `ui/finalhmi/MapFirstDashboardScreen.kt`: tamamen Compose; `WindowSizeClass` değerine göre yatay/dikey yerleşim, ilk beş sekme.
- `ui/finalhmi/MapHeroPane.kt`: gerçek MapLibre, arama alanları, manevra, alt şerit.
- `ui/finalhmi/TripMetricsPane.kt`: GPS hız, SoC, tüketim, güç, menzil.
- `ui/finalhmi/RouteWeatherStrip.kt` ve `ChargePlanCard.kt`: gerçek data modelinden okunur.
- `ui/finalhmi/DashboardUiState.kt`: nullable, kaynak ve timestamp içeren saf durum modeli; ayrık demo/production mapper.
- `ui/finalhmi/MapFirstDashboardPreview.kt`: 1672×941 tasarım referansı, gerçek Android dp önizlemeleri (ör. 960×540 yatay; 393×873 dikey), gerçek veriden kesin ayrılmış demo.
- Ayrı ViewModel/repository kaynakları: doğrudan Composable'da HTTP veya OBD sorgusu yok; StateFlow + yaşam döngüsüne uygun toplama.

Uygulama sırası:
1. **F1**: kaynaklardan bağımsız Compose HMI iskeleti; keskin ayrılmış DEMO verisiyle önizlemeler ve ekran karşılaştırması.
2. **F2**: beş sekme ve MapLibre gerçek rota/alternatifler, yer/adres önerilerini bağla.
3. **F3**: GPS/Open-Meteo ve manuel SoC/model menzil/kWh hesapları, tazelik/error/unknown durumları.
4. **F4**: koridor havası, OCM şarj işaretçileri, plan/maliyet kartı; traffic yoksa katman yok.
5. **F5**: taze/doğrulanmış OBD varsa güç/SoC; demo kaldırma/güvenli test; 393dp dikey ve 12,9 inç sınıfı yatay UI/erişilebilirlik regresyon testleri.

## 8. Android Auto vs. telefon Activity — doğrulanabilir teknik sınır
**Bu 65/35 tam özelleştirilebilir Compose ekranı bir Android telefon Activity'si veya uygulamanın kendi kontrol ettiği uyumlu büyük ekran penceresi içindir.**
Standart **Android Auto projeksiyonunda** host, sürüşe uygun templateleri kontrol eder; uygulama serbestçe kendi Compose Activity'sini 12,9 inç baş ünite ekranına tam ekran olarak çizemez. `NavigationTemplate` ve desteklenen harita çizimleriyle **uyumlu ayrı** Android Auto akışı gerekir. Doğrudan araç ekranında bu mockup'a birebir yansımayı garanti etme, AA sınırlarını dolanmak için riskli otomasyon veya Mola müdahalesi yapma.
Resmî teknik belgeler:
- https://developer.android.com/training/cars/apps/library
- https://developer.android.com/training/cars/apps/navigation
- https://developer.android.com/design/ui/cars/guides/foundations/overview

## 9. Kabul kapısı — F geliştirimi şu testlerden geçmeden tamamlanmış sayılmaz
- [ ] Yatay: ~%65 MapLibre harita / %35 metrik-hava-şarj; altta tam beş sekme, **Harita** seçili.
- [ ] Dikey: harita önce, bilgi kartları sonra; yatay kayıp/cutoff yok.
- [ ] Yasak sözcük/buton/ikonlar ana ekranda görünmüyor: müzik, Mola, klima, kilit, kamera, koltuk, telefon, araç görseli, lastik basıncı, araç durumu, vites.
- [ ] 56dp altı birincil etkileşim hedefi yok.
- [ ] Hız **km/sa**, yalnız güncel GPS; eksikse `—`.
- [ ] Açık demo filigranı; gerçek modda `DEMO` placeholder yok ve bilgi uydurulmuyor.
- [ ] SoC ve tahmini menzil kaynakları ayrı; formül + birim + SoH kapasiteleri testli.
- [ ] Open-Meteo sıcaklık taze/doğru noktadan; rota 20–30 km hava örneklemesi veya **kısmi** etiketi.
- [ ] Gerçek mavi rota, soluk alternatifler, şarj istasyonunda gerçek OCM etiketi, dört katman.
- [ ] Trafik kaynağı yoksa trafik lejandı/renkleri kesinlikle yok.
- [ ] Bilinmeyen fiyat `—`; kısmi toplam maliyet toplam diye gösterilmiyor.
- [ ] Büyük ekran dp + küçük ekran ekran görüntüleri gözle karşılaştırıldı.
- [ ] Java 17/AGP/Kotlin CI `testDebugUnitTest :app:assembleDebug` yeşil, APK imzası ve 16KB denetimi başarılı.
- [ ] POCO F5 (Android 17) ve ID.4 araç ekranı için **ayrı** saha test matrisi ve Android Auto template uyum raporu.
- [ ] Kullanıcı UI görselini gerçek cihazda onayladı; onay yoksa F kapanmaz.

**Bu doküman F için tek geçerli tasarım kaynağıdır. Önceki PNG mockuplarda görünen sayı/grafik/butonların hiçbiri veri kanıtı değildir.**
