# M8 veri akışı (kullanıcı isteğiyle)

| Hizmet | Giden veri | Ne zaman | Politika/limit |
|---|---|---|---|
| HeiGIT ORS | GPS başlangıç/hedef, izinli ara noktalar | Kullanıcı rota isteği | https://giscience.github.io/openrouteservice/hosted-api-restrictions (50 waypoint) |
| Photon | Arama metni | Kullanıcı Arama düğmesi | https://github.com/komoot/photon (kamu demo; kota garantisi yok) |
| Open-Meteo Forecast | En fazla üç rota noktası | Rota hesaplandığında | https://open-meteo.com/en/terms (ticari olmayan ücretsiz kullanım) |
| Open-Meteo Elevation | Rota örnek konumları | M8.3 plan isteği | https://open-meteo.com/en/docs/elevation-api (en çok 100 koordinat/istek) |
| Overpass | Koridor geometrisi | M8.4 plan isteği | https://dev.overpass-api.de/overpass-doc/en/preface/commons.html (yaklaşık 10000 sorgu/gün genel üst rehber) |

Kullanıcı anahtarları yalnız Keystore ile; koordinat/VIN/anahtar log, çökme dosyası ve güvenli test raporuna yazılmaz. Ağ servisleri fiziksel telefonda DOĞRULANMADI.
