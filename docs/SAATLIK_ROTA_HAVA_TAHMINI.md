# Rota boyunca geçiş saatine göre hava (EV Trip 2)

Tarih: 23.11.2026. Gerçek cihaz test durumu: DOĞRULANMADI.

## Uygulanan
- Önceki Open-Meteo current hava özetinin yerine yeni RouteHourlyForecastPolicy, OpenMeteoHourlyRouteClient ve RouteHourlyWeatherViewModel ile saatlik hava tahmini akışı.
- Başlangıç, geometrik orta, hedef için NavigationRoute.durationSeconds'a göre yaklaşık geçiş saati.
- Her noktanın saatlik sıcaklığı, rüzgâr hızı ve yağış olasılığı, Europe/Istanbul saat dilimi ve TAHMİN ile gösterilir; trafik dahil değildir.
- 90 dakikadan uzak saatlik tahmin veya bozuk HTTP sonucu reddedilir; sahte hava üretilmez. 250k karakter veri sınırı ve 8 sn isteği.
- HTTP yalnız ViewModel/repository katmanında çalışır; Compose yalnız rota değişiminde ViewModel.refresh ister.
- Seçilen gerçek alternatif güzergâh değiştiğinde yeni hava tahmini istenir. Tesla harita hava şeridi ile Rota ekranı aynı StateFlow'u kullanır.
- Yeni kütüphane/izin/anahtar, Mola veya Android Auto değişikliği yok.

## Sınırlar ve kaynaklar
- Open-Meteo API: https://open-meteo.com/en/docs
- Ticari olmayan ücretsiz erişim şartları: https://open-meteo.com/en/pricing
- Rota saati, şarj molası, canlı trafik ve güzergâh düzensiz hızını hesaba katmayan yaklaşık saattir.
- Bu PR rüzgârı otomatik menzil hesabına sokmaz; yalnız bilgi verir. Hava verisi TOA/sağlayıcı, ölçüm değil TAHMİN.
- Gerçek POCO F5/Android 17 internet erişimi, API yanıtı ve sonuç ekranda çalışması henüz DOĞRULANMADI.
- Sonraki PR: tahmini enerji ve şarj TL/kWh/bedel, kanıtlı fiyat kaynağı, harita katman seçimi.
- CI tüm testler+assembleDebug+16KB+imza yeşil olmadan main'e merge yok.
