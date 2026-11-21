# W4 — M8.6 Plan ekranı: gerçek kapsam ve tamamlama

**Kaynak:** Birleşik Ana Görev v2 / W4, repo main. Araç ve POCO F5 çalışma zamanı **DOĞRULANMADI**.

## Gerçek durum denetimi
| W4 madde | Mevcut / bu PR | Doğrulama düzeyi |
| --- | --- | --- |
| Süre, mesafe, şarj, durak, varış SoC ve kWh özet şeridi | Mesafe/durak/SoC zaten vardı; üst şeride **toplam süre, şarj süresi, tahmini varış kWh** eklendi | Birim test; saha yok |
| Sürekli TAHMİN etiketi ve belirsizlik bandı | Vardı; gösterim korunuyor | Kod + test |
| Başlangıç GPS/adres/Ev-İş, hedef, ara noktalar | Var olan **Rota sekmesine yönlendirme** Plan parametre panelinden her zaman açılabilir; yeni rota/ara nokta giriş ekranı uydurulmadı | Kod; telefon testi yok |
| Başlangıç SoC OBD/MANUEL ve min SoC, hedef %, hız, sıcaklık, HVAC, yük, tüketim | Var; `PlannerOptions`/DataStore | Birim test; araç BMS yok |
| Ağ filtresi | W3'te tercih edilen ve dışlanan ağlar eklendi | Sentetik/oracle CI; operatör durumu güncel değil |
| Durak kartları, gücü/güveni, % giriş→çıkış, sapma, doluluk bilinmiyor ve yedek istasyon | Mevcut | Kod testi |
| SoC Canvas grafiği, iyimser/kötümser son nokta bandı | Mevcut | Kod testi |
| Aşama/iptal/hata, kota/ağ/API anahtarları | Mevcut mesajlar kısmen birleşik; daha spesifik sınıflandırma için gerçek sağlayıcı hata kanıtı gerekli; keyfi neden uydurulmaz | DOĞRULANMADI |
| Veri akışı ve servis atıfları | Mevcut Plan ve harita altındaki açıklamalar | Kod |
| Sürüşte yoğun etkileşim | Bu alan telefon için sürüş **öncesi** tasarlandı, Android Auto video/arayüz yok | Saha yok |

## Hesaplama ve kaynak
- `arrivalKwh = capacityAtSohKwh × arrivalSocPct / 100`; batarya modelinden gelen `TAHMİN`, ECU/SoC okuması değil.
- `totalMinutes = physicalMinutes`, `chargingMinutes` Solver sonucudur; trafik, şarj sırası, doluluk ve gerçek istasyon hızları bilinmez.
- `PlannerDisplay` hesapları JUnit ile doğrulanır; yanlış kapasite NaN/0 kapatılır.
- `PlannerScreen` UI değişikliği **sadece mevcut Plan şeridindeki alanların görünürlüğü** ve mevcut Rota ekranına kısa yol; Mola APK/tasarımı değişmez.
- Sürüş sırasında kullanıcıdan çok adım veya araç videosu istenmez.

## Sınırlar
M8.6 modelin sahada onaylandığı anlamına gelmez. Model `verified=false`, tahmin gücü kullanıcı gözlemiyle bile sertifikalı olmaz. Canlı trafik, şarj fiyatı, anlık doluluk yoktur. Yeni bağımlılık yok / NOTICE değişmedi.

**Geri alma:** W4 tek PR revert. **Sonraki:** W5 M8.7 mevcut harita/rota/kaydet/paylaş ve ikinci geçiş uygulamalarını denetle, sadece eksikleri tamamla.
