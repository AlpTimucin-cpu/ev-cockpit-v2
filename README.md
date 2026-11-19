# ID.4 Yol Arkadaşı (ev-cockpit v2)

Sıfırdan yazılmış Kotlin + Jetpack Compose iskeleti. Mola APK'sına dokunmaz; bağımsız bir uygulamadır.

## Şu an çalışan (Aşama 1)
- ELM327 (klasik Bluetooth SPP) bağlantısı, MEB BMS sorguları (PID tablosu `assets/pids_meb.json`)
- **OBD Test** sekmesi: ham komut/yanıtları gösterir, `Android/data/app.evcockpit/files/obd_log_*.txt` dosyasına kaydeder
- **Gösterge** sekmesi: Tesla tarzı hız / güç çubuğu (rejenerasyon yeşil) / batarya %
- Demo verisi (araçsız arayüz denemesi)
- Trafik yok (istek üzerine kapsam dışı)

## Yol haritası
2. Harita + rota (MapLibre veya osmdroid, çevrimdışı karolar), sesli tarif
3. Video penceresi (Media3), haritanın üstünde küçültülebilir; hız kilidi ayarı
4. Araç ekranına yansıtma (Mola'nın projeksiyon yöntemi denenecek)
5. Şarj durağı planlama, hava durumu, motor sesi

## Önemli
- Bu kod burada **derlenmedi**; Android Studio'da açıp Gradle sync yap, sürümleri güncelle.
- PID'lerin tamamı `verified=false`. İlk bağlantıdaki ham log ile doğrula; scale/offset'i JSON'dan düzelt.
- OBD cihazını aynı anda başka uygulama (CarScanner, ABRP) kullanamaz.
- Birim testleri: `./gradlew test` (ayrıştırma + ölçek mantığı).
