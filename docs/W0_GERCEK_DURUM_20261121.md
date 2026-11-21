# W0 — Gerçek durum denetimi (21.11.2026)

Bu belge Birleşik Ana Görev Tanımı v2'ye tabidir. Bu satırlarda yalnızca GitHub API, kaynak ağaç ve Actions verileri doğrulandı; araç/telefon kullanımı ayrıca **DOĞRULANMADI**.

## 10 satır özet
1. Depo: `AlpTimucin-cpu/ev-cockpit-v2`, ana dal `main`, görünürlük public.
2. W0 öncesi main: `cee2d07cfd0fb1b7c42a5afe84d69147d60397a0`; PR #71 squash merge: `006ab4e40bc3c3d9a2c3b8bbfcd4d478a36a0084`.
3. Gerçek uygulama sürümü: `versionName=0.3.21-stability`, `versionCode=44`; eski `0.3.10-m8-A` artık güncel değil.
4. Main W0 öncesi Actions: https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44817047321 — 177/177 test, başarılı, nightly yayımlanmış.
5. PR #71 Actions: https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44821579591 — 180/180 test, başarı, public debug parmak izi kontrolü başarılı.
6. #71 altı parametresiz `evcockpit://open/...` URI'si, MainActivity, manifest, negatif test ve belge içerir; main'e birleştirildi.
7. #67–70 medya/arayüz/Mola-yı açma kapsam dışı notuyla kapatıldı, birleştirilmedi.
8. Issue #52 M8.5e, #54 M8.6, #57 M8.7, #62 telefon kabulü açık; main'de bu akışlara ait Kotlin/Compose kodları var.
9. M0–M3 tanı/OBD güvenli çıktı, manuel SoC, M8.0–8.7 paketleri dosya ağacında; **işlevsel saha kabulü DOĞRULANMADI**.
10. Mola ayrı uygulamadır; kapalı APK kodu, medya özellikleri, Shizuku, MediaProjection ve ayrıcalıklı işlemleri EV Cockpit'e aktarılmaz.

## Kanıt tablosu

| Konu | Kanıt | Doğrulama |
| --- | --- | --- |
| Main 177 test + nightly | [Actions main](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44817047321) | CI doğrulandı |
| PR #71 180 test | [Actions PR71](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/actions/runs/44821579591) | CI doğrulandı |
| W0 #71 merge | [PR71](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/pull/71) | Merge SHA `006ab4e40...` |
| W0 #67–70 | [Pull Requests](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/pulls?q=is%3Apr+is%3Aclosed) | Kapsam dışı, unmerged |
| M8.5e | [Issue #52](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/issues/52) | Kod var, saha yok |
| M8.6 | [Issue #54](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/issues/54) | Kod var, saha yok |
| M8.7 | [Issue #57](https://github.com/AlpTimucin-cpu/ev-cockpit-v2/issues/57) | Kod var, saha yok |

## Riskler
- APK imzasının public test anahtarıyla üretilmesi güvenlik garantisi değildir; sürüm ve uygulama kimliğine bak.
- İlk araç ELM327 v2.1 klon oturumunda AT komutları OK, UDS NO DATA: neden ve desteklenebilir PID/SoC **DOĞRULANMADI**.
- Android Auto ve Mola birlikte görüntüleme sahada **DOĞRULANMADI**.
- GPS, VIN, API anahtarı, cihaz ID'si paylaşılmamalı; test raporları maskeleme kontrolünden geçmeli.
- `nightly` yayını sadece yeşil `main` CI sonrası; PR debug ve nightly farklı artefaktlar olabilir.

## Öncelik sırası, gün/teslim sözü olmadan
W0 (temizlik) → W1 (uyumluluk) → W2 (OBD tanı) → **D1** araç kanıtı → W3 (M8.5e mevcut kod farkları) → W4 (M8.6 mevcut kod farkları) → W5 (M8.7 mevcut kod farkları) → **D2** telefon kanıtı.

**Sonraki tek telefon adımı:** D1'de telefonundan araç park hâlindeyken Tanı Modu ve güvenli test raporunu paylaş. W1-W2 kodları tamamlanmadan kullanıcı testine geçilmez.
