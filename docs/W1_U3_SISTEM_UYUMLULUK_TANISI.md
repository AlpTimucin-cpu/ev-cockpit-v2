# W1/U3 — OBD Test sistem uyumluluk tanısı

## Kapsam
`OBD Test` içinde açılır/kapanır **Sistem uyumluluk tanısı** eklendi. Önceden var olan gösterge, rota, plan ve Mola ekranlarına müdahale yoktur.

## Yerel değerlendirilen alanlar
- Android sürüm/API, ROM türü (yalnız `LineageOS` / `MIUI` işareti; tam build fingerprint veya cihaz ID'si **yazılmaz**).
- GMS, Android Auto (sadece PackageManager paket görünürlüğü; kurulmuş olması çalıştığını göstermez).
- TTS servis görünürlüğü ve kullanıcı paneli açınca `tr-TR` çevrimdışı ses listesi: `getVoices()`, `!isNetworkConnectionRequired`, `KEY_FEATURE_NOT_INSTALLED` yokluğu. Bu **motor beyanıdır**, gerçek ses üretimi/kullanılabilirlik **DOĞRULANMADI**.
- Klasik Bluetooth donanım özelliği, Bluetooth_CONNECT izni ve anahtarın açık olması (adapter/ELM327 bağlantısı hâlâ ayrı test).
- GPS/NETWORK sağlayıcıları ve izin durumu; koordinat, hassas enlem-boylam yok.
- WebView mevcut paket **sürüm bilgisi** (uyumluluk veya YouTube Error 153 sonucu garanti edilmez).
- Linux sayfa boyutu `Os.sysconf(_SC_PAGESIZE)`; CI native APK denetiminden ayrı.
- Bildirim durumu ve pil optimizasyon muafiyeti.

## Gizlilik ve rapor
`SafeTestReportExporter` mevcut `SafeTestReportText.sanitizeLine` filtresinden geçirdiği uyumluluk özetini ekler; son 200 OBD log satırı sınırı korunur. ROM build `FINGERPRINT`, VIN, koordinat, API anahtarı ve MAC **yazılmaz**. TTS test sonucu yalnız son yerel durumdan alınır; bilinmiyorsa **KONTROL EDİLMEDİ** gösterilir.

## İzin ve UX
Yeni runtime/ayrıcalıklı izin yok. `<queries>` içinde `com.google.android.gms` ve `com.google.android.projection.gearhead` paket görünürlük tanımı var; bu bir veri erişim izni değildir. TTS düğmesi yalnız sistem ses paketi/ayar ekranını açar, hiçbir ayarı otomatik değiştirmez.

## Kabul
1. Telefonda OBD Test'i aç, "Sistem uyumluluk tanısı" panelini genişlet.
2. Android/API/ROM, GMS/AA, WebView, TTS, Bluetooth, konum, Linux sayfa ve pil durumlarının anlamlı/doğrulanmadı etiketleriyle görüntülendiğini kontrol et.
3. Türkçe ses paketi yoksa düğmeden **sistem ayarını aç**; otomatik indirme yok.
4. "Güvenli test raporunu dışa aktar" sonrasında uyumluluk bloğu görülmeli ve gizli veriler bulunmamalı.

**Cihaz kabulü:** POCO F5 / LineageOS 24 Android 17 **KULLANICI TESTİ BEKLENİYOR**.
Kaynak: https://developer.android.com/reference/android/speech/tts/TextToSpeech.Engine ; https://developer.android.com/reference/android/webkit/WebView ; https://developer.android.com/about/versions/17/summary .
