# ID.4 Yol Arkadaşı (ev-cockpit v2)

Sıfırdan yazılmış Kotlin + Jetpack Compose iskeleti. Mola APK'sına dokunmaz; bağımsız bir uygulamadır.

## Mevcut Aşama 1 (telefonda ve araçta henüz doğrulanmadı)
- ELM327 (klasik Bluetooth SPP) bağlantısı, MEB BMS sorguları (PID tablosu `app/src/main/assets/pids_meb.json`)
- **OBD Test** sekmesi: ham komut/yanıtları gösterir, `Android/data/app.evcockpit/files/obd_log_*.txt` dosyasına kaydeder
- **Gösterge** sekmesi: hız / güç çubuğu (rejenerasyon yeşil) / batarya %
- Demo verisi (araçsız arayüz denemesi)
- Trafik özelliği yoktur ve eklenmeyecektir.

## CI / derleme
- GitHub Actions: [Android CI](../../actions/workflows/android.yml)
- Gradle Wrapper henüz yoktur; iş akışı `gradle/actions/setup-gradle@v4` aracılığıyla Gradle **8.10.2** kullanır.
- Her PR için `:app:testDebugUnitTest` ve `:app:assembleDebug` çalışır. Yalnızca **başarılı** çalışmalarda debug APK artifact olarak yüklenir.
- Artifacts debug imzalı test APK'sıdır; mağaza dağıtımı veya sahada doğrulanmış sürüm değildir.
- CI geçmesi, ELM327 ve araç üstündeki PID uyumluluğunun kanıtı değildir.

## Yol haritası (GitHub Issues)
1. [CI ve .gitignore](../../issues/1) — PR-1
2. [Nightly pre-release APK](../../issues/2) — PR-2, CI yeşil olduktan sonra
3. [ELM327 dayanıklılığı ve log paylaşımı](../../issues/3) — PR-3
4. [Harita + rota ön araştırması](../../issues/4) — kullanıcı onayı olmadan kod yazılmaz
5. Video penceresi (Media3), kullanıcı tarafından ayarlanan hız kilidi
6. ID.4 ekranına yansıtma — sadece fizibilite araştırması, onaya bağlı
7. Şarj durağı, hava durumu, motor sesi

## Önemli
- `pids_meb.json` içinde PID'lerin tamamı `verified=false`. Gerçek araçtan ham log alınmadan değiştirilmemelidir.
- OBD cihazını aynı anda başka uygulama (CarScanner, ABRP) kullanamaz.
- Cihaz/araç testleri ve Bluetooth kopma/yeniden bağlanma davranışı **henüz doğrulanmadı**.
- Android projesi tek modül (`app`), `minSdk 28`, paket `app.evcockpit`.
