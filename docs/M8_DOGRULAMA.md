# M8 doğrulama defteri — 20.11.2026

Model parametreleri tek kaynak: app.evcockpit.planner.model.VehicleProfile ve ModelParameters. Tüm başlangıç araç değerleri verified=false (sahada DOĞRULANMADI).

| Parametre | Varsayılan | Kaynak | Güven |
|---|---:|---|---|
| usableKwh | 52 | https://binekarac.vw.com.tr/tr/modeller-fiyatlar/arac-modelleri/id4.html | araçta DOĞRULANMADI |
| SoH, ek yük | %100, 0 kg | kullanıcı/M8 master | VARSAYILAN |
| Kütle | 2000 kg | M8 master yaklaşık | TAHMİN |
| CdA | 0.756 m2 | Cd 0.28 × 2.7 alan, M8 master yaklaşık | TAHMİN |
| Crr, aktarma, rejen verimi | 0.009, 0.90, 0.65 | M8 master | TAHMİN |
| Max rejen | 65 kW | Senaryo varsayımı | TAHMİN |
| Max DC | 145 kW | https://autocatalogarchive.com/wp-content/uploads/2024/12/VW-ID.4-2024-TR.pdf ; 2023 VW 115 kW için https://www.volkswagen-newsroom.com/en/the-new-id4-and-id5-17756/the-new-high-efficiency-drive-all-the-details-17757 | model yılına bağlı; DOĞRULANMADI |
| Max AC | 11 kW | VW TR broşürü | araçta DOĞRULANMADI |
| Aux/HVAC | 0.5/0.5/2.0 kW | M8 master test senaryosu | TAHMİN |
| Şarj eğrisi | 0–100% 7 nokta | M8 master örnek | TAHMİN EĞRİ |
| Soğuk çarpan | −20..40°C 5 nokta | M8 master örnek | TAHMİN |
| consumo adjust | 0%, ±30% | kullanıcı tercihi | VARSAYILAN |
| g | 9.80665 m/s2 | https://physics.nist.gov/cgi-bin/cuu/Value?gn | standart tanım |
| rho | 1.225 kg/m3 | ISA yakınsaması | TAHMİN |

Hava, rüzgâr, SoH/şarj eğrisi ve DC güç doğruluğu araçta ölçülmedi. M8.1 model testleri cihaz/araç doğrulaması değildir.

## M8.2 tüketim modeli belirsizliği
Hava yoğunluğu ISA, CdA, yuvarlanma, eğim, rejen sınırı ve yardımcı yük M8 formülleriyle hesaplanır; hız değişimi/ivmelenme ve rüzgâr dahil değildir. Soğuk sürüş çarpanı (−20°C 1.25, −5°C 1.15, 20°C 1.0) kaynaklı saha doğrulaması olmayan varsayımdır. ±10% iyimser, +15% kötümser bandı istatistiksel güven aralığı değildir. Sanity birim testleri model kalibrasyon kanıtı DEĞİLDİR.

## M8.5A
- Şarj süresi, parçalı doğrusal güç eğrisinin kapalı-form 1/P(soc) integralidir; batarya soğuk çarpanı uygulanır. Soğuk güç tablosu/şarj eğrisi sadece TAHMİN.
- Büyük aday kümesinde 40 km başına 3 aday budama kararı hesaplama bütçesi varsayımıdır; gerçek dünya optimumu garantilenmez; küçük ≤8 adayda eksiksiz alan kullanılır.

## M8.5C algoritma testleri
- Kesikli SoC ızgarasında (kaba kuvvet DFS) en iyi toplam süre küçük sentetik aday kümeleriyle kontrol edilir; **sürekli SoC optimumu veya gerçek yol optimumu iddiası YOK**.
- 510 sabit tohumlu güvenlik senaryosu, 1000 km / 300 aday JUnit performans ölçümü ve şehirler arası isimli **sahte 700/450 km** senaryoları Actions JUnit XML kanıtından raporlanır.
- En hızlı plana ek olarak daha az durak ve yalnız OSM CCS2 etiketli +5 yüzde SoC güvenlik alternatifi aranır. Yedekler yalnız yakın adaylardır, gerçek erişim/doluluk/sapma DOĞRULANMADI.

## M8.5D rota ikinci geçiş
- Sağlayıcı seçilen ara duraklar için yeni güzergâh üretebilir. %2 enerji farkı aşıldığında aday izdüşümleri güncellenip çözücü en fazla bir kez yeniden çağrılır. İkinci >%2 fark açık uyarıdır; sessizce 'gerçek plan' kabul edilmez.
- Testler fake route servislerle; gerçek HeiGIT, harita sunucusu veya ID.4 sonucu DOĞRULANMADI. UI/anahtarlı gerçek hesaplama M8.6–M8.7'de bağlanır, Durma Noktası A onayı gerekir.
