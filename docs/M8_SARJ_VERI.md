# M8.4 — Şarj verisi güven puanı

- OSM "socket:type2_combo" pozitif etiketi: OSM_TAGGED_CCS2 (fiziksel soket DOĞRULANMADI).
- Etiket yok ama ZES/Trugo/Eşarj/Tesla: PROBABLE_CCS2; operatörün hızlı DC sunması her istasyonun CCS2/erişilebilir olduğunu garanti etmez.
- Diğer: UNKNOWN; eleme yerine ceza ve filtre. Fiyat ve canlı doluluk yok: doluluk bilinmiyor.
- Güç: socket:type2_combo:output etiketi varsa etiket gücü; yoksa ilgili operatör için 60 kW varsayımı (TAHMİN), diğerleri 11 kW varsayımı (TAHMİN).
- Resmî ağ kaynakları:
  - ZES https://zes.net/tr/sarj-istasyonlari
  - Trugo https://trugo.com.tr/individual/news/12-aydir-kesintisiz-pazar-lideriyiz
  - Eşarj https://trugo.com.tr/individual/news/trugo-ve-esarj-is-birligiyle-hizli-ve-kesintisiz-sarj-81-ilde-hizmetinizde
  - Tesla https://www.tesla.com/tr_tr/support/charging/supercharging-other-evs
- OSM verisi: https://www.openstreetmap.org/copyright ; istasyon bilgilerinin güncelliği DOĞRULANMADI.
