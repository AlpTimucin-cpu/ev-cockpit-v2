# UI-1 — Tesla tarzı tasarım temeli

Tarih: 21.11.2026 | Depo: AlpTimucin-cpu/ev-cockpit-v2 | Telefon ve araç saha sonucu: **DOĞRULANMADI**.

## 1. Tasarım kararları
- Sabit koyu tema, dinamik renk yok. Arka plan #0B0D10; panel #151A20; kenarlık #22272E; beyaz metin; ikincil #8A949E; vurgu/rejenerasyon #3DDC84; uyarı #FFB300; hata #FF5252; tahmin #FF8A4C.
- Sistem yazı tipi; büyük hız 112sp (kompakt 96sp), batarya 56sp; metin en az 14sp, dokunma hedefi hedefi 56dp.
- Merkezi TeslaTokens: 26/54/20 yatay pane oranları; 5 km/sa sürüş ve 3000ms tazelik eşikleri UI-3 için tanımlandı, henüz uygulama sürüş kurallarına bağlanmadı.
- Pure TeslaDisplayPolicy: eksik hız ve SoC '--'; enerji/menzil tahmin etiketli; güç pozitif çekiş beyaz, negatif rejenerasyon yeşil.
- UI bileşenleri: VehicleStatePanel, PowerBar, SocGauge, SourceBadge; dört panel önizlemesi NORMAL / EMPTY / LOADING / ERROR. Örnek sayılar DEMO ve gerçek sürüş kanıtı değildir.
- CockpitColors eski kullanım yerlerine yeni TeslaTokens alias'ı oldu. CockpitTheme tek koyu renk paletine geçti. Daha önce kaydedilen gece teması tercihi silinmedi, ancak açık tema artık etkin değil; kullanıcının karşısındaki eski anahtar kaldırıldı.

## 2. Kapsam ve güvenlik
- Bu PR MapLibre haritasına, mevcut altı sekmeye, OBD bağlantılarına, rota anahtarına veya Mola uygulamasına müdahale etmez. Yeni Android izin/intent-filter eklenmez.
- UI-1 yeni veri akışı veya ağ çağrısı yapmaz. Atomlar yalnız görüntü modelinden veriyi okur. Asıl StateFlow adaptasyonu UI-2 ve UI-3'te yapılacak.
- Enerji ve menzil hesaplaması burada YOK; UI-2 mevcut model veya repository hesabını parametre olarak geçmelidir. UI’da yeni gerçeklik iddiası yok.
- @Preview kaynaklarını derlemek için yalnız mevcut Compose BOM altındaki ui-tooling-preview kütüphanesi eklendi; yeni medya/harita veya ağır bağımlılık yok.

## 3. Mola resmî yol ve doğrulama matrisi
| Konu | Durum |
| --- | --- |
| Mola'nın APK/menü/tasarım/izin değişikliği | HAYIR |
| Mola iç servis, aktiviteleri veya özel URI çağrısı | HAYIR |
| UI-1'de Mola bağlantısı | YOK — UI-4 kapsamı |
| UI-4 resmi Android yöntemi | PackageManager.getLaunchIntentForPackage ve query görünürlüğü |
| Mola telefon açılışının sürümlerde çalışması | DOĞRULANMADI |
| Android Auto görünürlüğü / araç ekranı | DOĞRULANMADI |

## 4. Kanıt ve kalite kapısı
- Başlangıç main commit: 44c106c559bedeedb32af476c4b33df593cb4121.
- Başlangıç yeşil Actions: https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44892288499 (199/199).
- Bu PR için GitHub Actions testDebugUnitTest + assembleDebug + Compose Preview derlemesi + native ELF/ZIP 16KB ve debug imzası kanıtı zorunludur.
- PR CI başarılı olmadan main'e birleştirilmez. Cihaz açılışı veya Mola entegrasyonu CI ile fiziksel doğrulanmış sayılamaz.

## 5. Kalan aşamalar ve geri alma
- UI-2: sol araç paneli / ortada mevcut MapLibre harita / sağ eylem şeridi ve dikey uyarlama.
- UI-3: güncel OBD/GPS kaynak önceliği, sürüş/park ayrımı, sürüşte karmaşık işlemlerin güvenli sınırı.
- UI-4: Mola için yalnız resmî Android uygulama başlatma, kurulu değil ve açılamadı durumları, dönüşte yeniden tarama.
- UI-5: Türkçe hata/boş/eski veri eylemleri, telefon Nightly kabul testi.
- Geri alma: UI-1 PR değişikliklerini revert et; uygulama DataStore değeri korunur.
- Durma noktası: UI-5 ardından Nightly yatay/dikey telefon ekran görüntüleri ve kullanıcı onayı, bundan önce W7 araç ekranı kodlaması yok.
