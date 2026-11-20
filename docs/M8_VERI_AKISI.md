# M8 veri akışı (kullanıcı isteğiyle)

| Hizmet | Giden veri | Ne zaman | Politika/limit |
|---|---|---|---|
| HeiGIT ORS | GPS başlangıç/hedef, izinli ara noktalar | Kullanıcı rota isteği | https://giscience.github.io/openrouteservice/hosted-api-restrictions (50 waypoint) |
| Photon | Arama metni | Kullanıcı Arama düğmesi | https://github.com/komoot/photon (kamu demo; kota garantisi yok) |
| Open-Meteo Forecast | En fazla üç rota noktası | Rota hesaplandığında | https://open-meteo.com/en/terms (ticari olmayan ücretsiz kullanım) |
| Open-Meteo Elevation | Rota örnek konumları | M8.3 plan isteği | https://open-meteo.com/en/docs/elevation-api (en çok 100 koordinat/istek) |
| Overpass | Koridor geometrisi | M8.4 plan isteği | https://dev.overpass-api.de/overpass-doc/en/preface/commons.html (yaklaşık 10000 sorgu/gün genel üst rehber) |

Kullanıcı anahtarları yalnız Keystore ile; koordinat/VIN/anahtar log, çökme dosyası ve güvenli test raporuna yazılmaz. Ağ servisleri fiziksel telefonda DOĞRULANMADI.

M8.3: ORS GeoJSON isteğine elevation=true parametresi eklendi; kullanıcı rota istediğinde yaklaşık 3D rota koordinatları dönebilir. Planlayıcı DEM fallback Open-Meteo Elevation'a en fazla 300 örneği 100'erli isteklerle iletir. Günlük ücret/garanti kaynağı: https://open-meteo.com/en/docs/elevation-api ; ticari kullanım ayrıca lisanslı olmalıdır. Kullanıcı anahtarı loglanmaz. Yalnız kullanıcı tetiklemeli plan isteğinde DEM çağrısı yapılabilir; bu M8.3 saf kütüphane tek başına ağ çağırmaz.

M8.4B: Overpass https://overpass-api.de/api/interpreter uç noktasına kullanıcı plan isteğinde **sadeleştirilmiş rota koordinatları** ile sıralı POST istekleri gider. Yerel JSON cache app-private dosyası 7 gün, dosya adı SHA-256 (ham koordinat değil). Yanıtlarda OSM istasyon koordinatları olabilir; güvenli log, VIN ve anahtar aktarımı yok. Resmî limitler: https://dev.overpass-api.de/overpass-doc/en/preface/commons.html .
