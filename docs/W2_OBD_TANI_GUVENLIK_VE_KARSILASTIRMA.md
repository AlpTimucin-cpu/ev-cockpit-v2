# W2 — OBD tanı modu incelemesi, gizlilik onarımı ve Car Scanner karşılaştırması

**Kaynak:** Birleşik Ana Görev v2 §7/W2, depodaki `ObdDiagnosticEngine`, `ObdRepository`, `ObdTransport`, `ObdProfileStore`, `SafeTestReportText` ve ilgili birim testleri. **Saha yanıtı DOĞRULANMADI.**

## 10 satır özet
1. ELM tanımı: ATZ, ATI, ATRV, ATDP, ATDPN var — **KODDA DOĞRULANDI**.
2. Otomatik standart: ATSP0, 0100, 0120, 0140, 0902, 010D var — **KODDA DOĞRULANDI**.
3. Standart CAN: ATSP6 + 0100 var — **KODDA DOĞRULANDI**.
4. 29-bit pasif: ATSP7 + ATH1 + ATMA/3 sn mevcut — **KODDA DOĞRULANDI**.
5. 29-bit MEB: 8 haneli ATSH17FC007B, ATCP17 + 6 haneli ATSHFC007B, CAF0 padded ve 17FC0076 mevcut — **KODDA DOĞRULANDI**.
6. 11-bit: 710/77A, 7E4/220105 denemeleri var — **KODDA DOĞRULANDI**.
7. Normal veri sorgusunda beş ardışık tam tur yanıtsızsa sorgu yükü durduruluyor; `NoResponseGuard` var.
8. Tanı tek dokunuşta sıralı, en fazla iki bağlantı denemesi; her komut prompt süre aşımına tabi.
9. **Düzeltildi:** Mode09 `0902` VIN cevabı, ATMA ham payload'u ve prompt geri alma verisi artık paylaşılabilir tanı kayıtlarına yazılmıyor; yalnız çerçeve sayısı kalıyor.
10. **Eklendi:** Kullanıcı aynı adaptörle Car Scanner'da okuduğu protokol/hız/SoC gözlemini elle not edip güvenli raporun içine dahil edebiliyor; veri çekme/kazıma yok.

## Algılama ve durma kararları
| Gözlem | İşlem |
| --- | --- |
| Standart 010D pozitif, MEB NO DATA | Kalıcı iletişim profili `STANDARD_SPEED`; sadece hız, SoC manuel |
| ATMA Ready durumunda hiç çerçeve yok | Hazır olma/klon/CAN hattı nedenini **DOĞRULANMADI** bırak, donanım ve kontak bağlantısı kontrolü |
| ATMA çerçeveleri var ama UDS NO DATA | Başlık varyantlarını ve klon 29-bit sınırlamasını incele; yazılım sonucu uydurma |
| 29-bit/11-bit bir varyant pozitifi var | OBD iletişim profili kaydedilebilir, **PID verified=false** kalır |
| Beş yanıtsız tam tur | Yeniden sorgu yok, tek satır sayısal özet, araç verisi yok |

## Gizlilik
- Yeni tanı dosyalarında Mode09 0902 ham veri **gizlenir**, ATMA ham CAN payload'u dosyaya/loga yazılmaz.
- ELM yeniden senkronizasyon yanıtları da ham yazılmaz.
- İsim/etiket içermeyen 17 karakterlik VIN benzeri dize, koordinat hassasiyeti yüksek ondalık ve API/kimlik satırları, `SafeTestReportText.sanitizeLine` filtresinde gizlenir.
- **Önceden oluşturulmuş eski ham tanı dosyaları otomatik silinmez veya değiştirilemez:** cihazdaki eski ham dosyalar VIN içerebilir, paylaşmadan önce kullanıcı kontrol etmelidir.
- Car Scanner notu kullanıcı beyanıdır, bir PID doğrulaması sayılmaz; hassas bilgi yazılmaması ekranda belirtilir.
- Test raporu yalnız son 200 OBD satırını, toplam 1 MB günlük sınırı ise kayıt katmanında korur. Bluetooth MAC ve dosya yolu **ham tanı logunun okunabilir adres satırlarında bulunabileceği için** ham log paylaşımı dikkat gerektirir; güvenli rapor tercih edilir.

## Kaynak / doğrulama düzeyi
Uygulama protokolleri kamuya açık OBD ve topluluk referanslarından esinlenmiştir; kullanıcının VW ID.4'ünde çalıştığı **DOĞRULANMADI**. İlgili kaynaklar:
- [ABRP halka açık MEB profili](https://github.com/Iternio-Planning-AB/ev-obd-pids)
- [evDash araştırma](https://github.com/nickn17/evDash)
- SAE J1979 Mode 01 PID 0D hız / Mode 09 PID 02 VIN (standart OBD, **araç desteği ayrı**)

## D1 — SERT DURMA NOKTASI
Araç park hâlinde, Ready, OBDII adaptörü tam oturmuş şekilde yeni **nightly** debug test APK'sını kullan.
`OBD Test → Tanı modunu çalıştır → Güvenli test raporunu dışa aktar` akışıyla tek rapor paylaş.
Aynı adaptörle Car Scanner'da okunan protokol ve geçerli değerler varsa not alanına **elle** gir.
**Kullanıcı saha kanıtı gelmeden W6 PID kalibrasyonuna geçme**. W3–W5 araçtan bağımsız kod/CI işleri yapılabilir.

## Risk
D1 olmadan `Ready`, gerçek ECU yanıtı, BMS PID doğru ölçeği, sorunsuz 29-bit klon iletişimi veya Android Auto araç ekranı çalışır sonucu verilemez. Testte sahte veri kabul edilmez.
