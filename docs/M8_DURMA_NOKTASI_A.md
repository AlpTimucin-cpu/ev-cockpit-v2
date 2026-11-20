# M8 — EV ROTA PLANLAYICI / DURMA NOKTASI A
**Tarih:** 20.11.2026 · **Depo:** AlpTimucin-cpu/ev-cockpit-v2 · **Hedef:** VW ID.4 Pure 52 kWh
**Durum:** M8.0–M8.5 kodu CI testlerinde geçti; gerçek araç/telefon/API çalışması DOĞRULANMADI.

## İlk sayfa — 10 satırlık özet
1. M8.0 hedef seçimi: GPS başlangıç, adres/favori/harita, gelişmiş koordinat doğrulama (PR #36).
2. M8.1 profil: tüm araç parametreleri kaynaklı, verified=false; DataStore JSON şeması (PR #38).
3. M8.2 fizik: sürtünme/aerodinamik/eğim/HVAC/rejen; belirsizlik aralığı TAHMİN (PR #40).
4. M8.3 rota: 4000 nokta sınırı, hız ve eğim örnekleme; verisiz eğim açık uyarı (PR #42).
5. M8.4 istasyon: OSM/Overpass, koridor, nazik sorgu, önbellek, CCS2 güven sınıfları (PR #44, #45).
6. M8.5 şarj: parçalı doğrusal eğri integralinden süre ve aday budama (PR #47).
7. M8.5 optimizasyon: deterministik SoC-durum dinamik programlama, alt SoC sınırları (PR #48).
8. M8.5 doğrulama: 510 sentetik SoC denemesi, brute-force karşılaştırması, alternatifler (PR #49).
9. M8.5 performans ve ikinci geçiş: 1000 km/300 aday CI 752 ms; ara durak %2 fark kontrolü (PR #49, #50).
10. Son CI 122/122 geçti; gerçek Niğde–İstanbul, şarj/SoC ve mobil ölçümler DOĞRULANMADI. M8.6 izinsiz YOK.

## Kanıt tablosu — PR ve GitHub Actions
| Aşama | PR | Actions | Geçen / başarısız / atlanan | Açıklama |
|---|---|---|---|---|
| M8.0 | [#36](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/pull/36) | [44736319820](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44736319820) | 72 / 0 / 0 | Hedef akışı |
| M8.1 | [#38](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/pull/38) | [44737260915](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44737260915) | 76 / 0 / 0 | Kaynaklı profil |
| M8.2 | [#40](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/pull/40) | [44737801801](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44737801801) | 82 / 0 / 0 | Fizik/enerji |
| M8.3 | [#42](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/pull/42) | [44738895265](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44738895265) | 88 / 0 / 0 | Eğim/hız |
| M8.4a | [#44](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/pull/44) | [44739489122](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44739489122) | 93 / 0 / 0 | İstasyon parser |
| M8.4b | [#45](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/pull/45) | [44741182041](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44741182041) | 99 / 0 / 0 | Overpass/önbellek |
| M8.5a | [#47](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/pull/47) | [44741825293](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44741825293) | 105 / 0 / 0 | Şarj/eleme |
| M8.5b | [#48](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/pull/48) | [44743131062](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44743131062) | 110 / 0 / 0 | DP |
| M8.5c | [#49](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/pull/49) | [44748718220](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44748718220) | 117 / 0 / 0 | Oracle/510/perf |
| M8.5d | [#50](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/pull/50) | [44749803953](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44749803953) | 122 / 0 / 0 | Reroute karşılaştırması |

Bu sayılar GitHub Actions `JUnit test count` satırlarından alınmıştır. Bir önceki fail kayıtları düzeltilmeden merge yapılmadı. CI; cihaz performansı, batarya/SoC doğruluğu ve gerçek servis erişimi testi değildir.

## 1. Algoritma mimarisi
- Çekirdek `app.evcockpit.planner`: Android import yok; ayrı birim testi var. Ağ istemcileri arayüz arkasında, gerçek ağ birim testlerinde çağrılmıyor.
- Araç profili: kWh, SoH, kütle/ek yük, CdA, Crr, verim/rejen, DC/AC, şarj eğrisi, soğuk çarpanı, HVAC. Her sayı `source`, `verified`, `note` içeriyor; araç profilindeki tüm gerçek dünya değerleri `verified=false`.
- Enerji: `P_aero = 0.5 × rho × CdA × v³`; `P_roll = Crr × m × g × v × cos(theta)`; `P_grade = m × g × v × sin(theta)`. Tekerlek gücü aktarma verimi, sınırlandırılmış rejen ve yardımcı/HVAC ile batarya kWh'ye dönüşür. Rüzgâr/ivmelenme yok.
- Rota: çizgi 400 m civarı örneklenir; üst nokta sınırı 4000; yükseklik yoksa açıkça düz-eğim varsayımı ve **EĞİM VERİSİ YOK**. Çıkış hızı gerçek yol hızı değil, sağlayıcı ortalamasından türetilmiş **TAHMİN**.
- Şarj verisi: Overpass şarj etiketleri ve koridor sorgusu, 7 gün önbellek; etiketli CCS2/olası/bilinmiyor ayrımı. Canlı doluluk/fiyat yok; `doluluk bilinmiyor`.
- Şarj: `∫ dSoC / min(istasyon_kW, araç eğrisi_kW)` kapalı form parçalı integrali; soğuk çarpanı. Şarj eğrisi örnek amaçlı, **TAHMİN EĞRİ**.
- Çözücü: sıralı duraklar üzerinde `(node, arrivalSoC%, stopCount)` DP, %1 SoC varış hücresi, %5 hedef şarj ızgarası. Fiziksel zaman: sürüş + şarj + stop/detour; risk cezası ayrı gösterilir. Leg boyunca ara SoC tepe tüketimi de kontrol edilir.
- Alternatifler: daha az durak, OSM CCS2 etiketli +%5 güvenlik, çevredeki 2 yedek aday. Gerçek şarj cihazı durumu ve detour doğrulaması **DOĞRULANMADI**.
- İkinci geçiş: seçilen duraklardan ORS ara noktalı rota isteme arayüzü (en çok 50), enerji farkı >%2 olursa **en çok bir yeniden planlama**, ikinci fark kullanıcıya uyarı. Testler fake sağlayıcıyladır; **UI bağlama M8.7 ve saha testi bekliyor**.

## 2. Sayısal algoritma kanıtları
### 2.1 Güvenlik ve optimumluk
- **510 sabit tohumlu sentetik senaryo**: 408'i plan verebildi. Plan verilenlerin tamamında örneklem üzerindeki SoC alt sınırları ve varış hesabı kontrol edildi; test ihlali **0**. Diğer 102 senaryo için ulaşılabilir plan bulunmaması hata değildir.
- **Küçük aday kümeleri:** bağımsız DFS ile tüm istasyon ziyaret sıraları ve %5 şarj seçenekleri tarandı, DP en iyi zamanla karşılaştırıldı. Bu, **kesikli ızgara optimumluğu** kanıtıdır; sürekli SoC veya gerçek yol optimumluğu değil.
- Birim testlerinde aynı girdinin aynı çıktı vermesi, tek sabit senaryoda SoC/şarj üst sınırı monotonluğu, eşik dışında veri/geçersiz girdi kontrol edildi.
### 2.2 Performans
- Sentetik yük: **1.000 km, 300 aday**. Actions JUnit süre ölçümü: **752 ms**, CI bütçesi **10 saniyenin altında**. İşlem gerçek telefon/araç testi değil; ağ/sunucu gecikmesi hariç.
- 300 adayda deterministik budama vardır: en iyi planın bütün gerçek istasyon kümesi üzerindeki küresel optimum olduğu iddia edilmez.

## 3. Senaryo tablosu (SADECE SENTETİK)
Aşağıdaki Niğde–İstanbul ve Ankara–İstanbul adları **test etiketi**dir. Modelde Niğde–İstanbul için **yer tutucu 700 km**, Ankara–İstanbul için **yer tutucu 450 km**, düz güzergâh, kurgu istasyonlar ve doğrulanmamış ID.4 parametreleri vardır; gerçek harita/rakam iddiası YOK.

| Senaryo | Hız | Sıcaklık ve HVAC | Başlangıç SoC | Durak | Varış SoC | Fiziksel toplam |
|---|---:|---|---:|---:|---:|---:|
| Niğde–İstanbul yaz (sentetik) | 110 km/sa | 25°C, AUTO | %90 | 4 | %11 | 455,8 dk |
| Niğde–İstanbul kış (sentetik) | 110 km/sa | −5°C, ısıtma | %90 | 8 | %10 | 581,7 dk |
| Niğde–İstanbul yüksek hız (sentetik) | 130 km/sa | 25°C, AUTO | %90 | 6 | %11 | 426,3 dk |
| Niğde–İstanbul düşük SoC (sentetik) | 110 km/sa | 25°C, AUTO | %30 | 5 | %11 | 480,5 dk |
| Ankara–İstanbul (sentetik) | 110 km/sa | 25°C, AUTO | %90 | 2 | %10 | 280,8 dk |

Kış senaryosu daha uzun süre ve çok durak verir; yüksek hız daha fazla durakla daha kısa toplam süre verebilir, çünkü bu sentetik senaryoda sürüş süresi tasarrufu şarj süresi artışını aşmaktadır. **Bu sonuçlar seyahat önerisi değildir.** Kaynak: [PR #49 Actions JUnit ölçümleri](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44748718220).

## 4. Dış servisler, kota ve gizlilik
| Sağlayıcı | Talep edilen veri | Gerçek kullanım / sınır | Kaynak |
|---|---|---|---|
| HeiGIT ORS | GPS başlangıç, hedef ve seçilen şarj ara noktaları | Kullanıcı başlatır; 50 waypoint, driving 6000 km, anahtar kullanıcıdan | https://giscience.github.io/openrouteservice/hosted-api-restrictions |
| Open-Meteo Elevation | En çok 100 rota konumu/istek | M8.3 istemcisi en fazla 3 batch; ticari olmayan ücretsiz hizmet; Copernicus atfı | https://open-meteo.com/en/docs/elevation-api |
| Overpass / OSM | Sadeleştirilmiş rota koridor koordinatları | Sıralı nazik istekler, 7 gün önbellek, 429/504 geri çekilme; kamu güvenli sınır rehberi yaklaşık 10.000 sorgu / 1 GB gün | https://dev.overpass-api.de/overpass-doc/en/preface/commons.html |
| Open-Meteo Forecast | En çok 3 rota noktası | Kullanıcı rota oluşturunca; ücretsiz hizmet ticari olmayan kullanımda | https://open-meteo.com/en/terms |
| Photon | Kullanıcının adres arama metni | Yalnız Ara düğmesi, en az 10 sn aralık | https://github.com/komoot/photon |

**Gizlilik sınırı:** anahtar sadece şifreli mevcut ayarda; koordinat/VIN/anahtarlar tanı raporunda, hata günlüğünde ve test logunda yer almamalıdır. Yerel Ev/İş verisi DataStore'da hassas konum içerebilir; paylaşılabilir raporlara eklenmez. Kamu servislerine konum gönderimi kullanıcı tarafından başlatılan hesapta gerçekleşir.

## 5. Varsayımlar ve önemli riskler
- VW Türkiye 2024 Pure 52 kWh için broşürde **145 kW DC** verisi bulunuyor: https://autocatalogarchive.com/wp-content/uploads/2024/12/VW-ID.4-2024-TR.pdf . VW'nin başka model yılı açıklaması **115 kW** diyor: https://www.volkswagen-newsroom.com/en/the-new-id4-and-id5-17756/the-new-high-efficiency-drive-all-the-details-17757 . Bu farklılık araç profili `verified=false` bırakılmasını gerektirir.
- Kullanılabilir kWh, SoH, gerçek kütle/CdA/Crr, hava/yol sıcaklığı, rejen verimi ve şarj eğrisi **sahada kalibre edilmedi**; sonucu belirgin etkiler.
- OSM istasyon etiketinin bulunması; şarj aletinin çalıştığını, o anda açık/uygun olduğunu, bağlantı/fiyatı ve CCS2 uyumluluğunu garanti etmez.
- ORS/Overpass/Open-Meteo gerçek rota yanıtları ve kota davranışı telefonda **DOĞRULANMADI**. Aşama M8.5 ikinci geçiş API'si hazır ancak M8.6/7 UI akışına bağlanmamıştır.
- Sürüşte dokunmaya dayalı planlama yok; canlı trafik/fiyat/doluluk yok; ABRP kapalı verisi/modelleri, Mola/Fermata kodu ve Android Auto plan ekranı yok.
- Normal sürüşte OBD/PID `verified=true` yapılmadı; M4/M5 için Durma Noktası 1 geçerlidir.

## 6. DURMA NOKTASI A — karar
**M8.0–M8.5 çekirdek ve sentetik testler CI ile tamamlandı.** Telefon / gerçek servis / araç doğruluğu **DOĞRULANMADI**. M8.6 plan ekranına kullanıcı açık onay vermeden geçilmeyecek. M8.7 ve M8.8 de başlamayacak. M4/M5 saha logu olmadan açılmayacak.

**Kullanıcının yapacağı tek adım:** Durma Noktası A raporunu inceleyip M8.6'ya geçiş için açık kararını bildirmek.
