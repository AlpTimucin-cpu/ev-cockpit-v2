# Aşama B — değişen 16 dosyanın TAM içeriği

Temel dal: `feature/stage-a-hilt-map-20261123`. Bu belge B aşamasında değişen dosyaların **eksiksiz tam sürümlerini** içerir. `...` ile atlanmış kod yoktur. Dosyalar zaten `feature/stage-b-osrm-photon-comparison-20261123` dalına doğrudan yüklendi; telefondan kopyalayıp tekrar yüklemek gerekmez.

- Aşama: B (adres otomatik tamamlama, OSRM gerçek alternatifler, enerji/SoC ön karşılaştırması)
- Gerçek donanım testleri: yapılmadı
- Resmî trafik, canlı şarj fiyatı, şarj istasyonu rotası: bu aşamanın kapsamı dışı
- Anahtarlar: derlemeye gömülmez
- Lisans/atfı: dosya içindeki ve `NOTICE` bildirimi
- Mola: değiştirilmedi


---

## 1. `app/src/main/assets/tr_province_district_centres.txt`

```text
# Türkiye il/ilçe koordinatları; kaynak: AEKHer/il-ilce-koordinat (MIT), https://github.com/AEKHer/il-ilce-koordinat
# Koordinat kaynağı: OpenStreetMap contributors, ODbL 1.0; il/ilçe merkezleri yaklaşık; rota öncesi kontrol edin
# il|ilce|lat|lon
Adana|Aladağ|37.545247|35.394442
Adana|Ceyhan|37.028883|35.812443
Adana|Çukurova|37.046886|35.282328
Adana|Feke|37.814761|35.911653
Adana|İmamoğlu|37.257713|35.661337
Adana|Karaisalı|37.257184|35.058641
Adana|Karataş|36.564601|35.384142
Adana|Kozan|37.447786|35.816631
Adana|Pozantı|37.422908|34.873167
Adana|Saimbeyli|37.984564|36.088826
Adana|Sarıçam|37.019741|35.398992
Adana|Seyhan|37.102420|35.306109
Adana|Tufanbeyli|38.260300|36.222074
Adana|Yumurtalık|36.767587|35.791587
Adana|Yüreğir|36.989458|35.340883
Adıyaman|Adıyaman|37.760298|38.277299
Adıyaman|Besni|37.691090|37.862274
Adıyaman|Çelikhan|38.033309|38.242389
Adıyaman|Gerger|38.029339|39.033320
Adıyaman|Gölbaşı|37.784405|37.639560
Adıyaman|Kahta|37.786072|38.621718
Adıyaman|Samsat|37.578498|38.480917
Adıyaman|Sincik|38.029133|38.619433
Adıyaman|Tut|37.794449|37.914949
Afyonkarahisar|Afyonkarahisar|38.756860|30.538704
Afyonkarahisar|Başmakçı|37.897358|30.009778
Afyonkarahisar|Bayat|38.984423|30.925576
Afyonkarahisar|Bolvadin|38.711587|31.045486
Afyonkarahisar|Çay|38.628342|31.035549
Afyonkarahisar|Çobanlar|38.688234|30.738870
Afyonkarahisar|Dazkırı|37.902113|29.862394
Afyonkarahisar|Dinar|38.065591|30.156363
Afyonkarahisar|Emirdağ|39.019501|31.150258
Afyonkarahisar|Evciler|38.027054|29.921358
Afyonkarahisar|Hocalar|38.580813|29.966880
Afyonkarahisar|İhsaniye|39.013490|30.403195
Afyonkarahisar|İscehisar|38.862085|30.750971
Afyonkarahisar|Kızılören|38.257834|30.149161
Afyonkarahisar|Sandıklı|38.458473|30.260798
Afyonkarahisar|Sinanpaşa|38.745188|30.245212
Afyonkarahisar|Sultandağı|38.547927|31.268241
Afyonkarahisar|Şuhut|38.533961|30.546129
Ağrı|Ağrı|39.719125|43.050489
Ağrı|Diyadin|39.539769|43.671203
Ağrı|Doğubayazıt|39.548337|44.079359
Ağrı|Eleşkirt|39.798379|42.675470
Ağrı|Hamur|39.609335|42.988203
Ağrı|Patnos|39.233420|42.861234
Ağrı|Taşlıçay|39.633330|43.377676
Ağrı|Tutak|39.539356|42.772750
Aksaray|Ağaçören|38.868972|33.915577
Aksaray|Aksaray|38.370542|34.026907
Aksaray|Eskil|38.401703|33.412823
Aksaray|Gülağaç|38.394089|34.346052
Aksaray|Güzelyurt|38.277870|34.371416
Aksaray|Ortaköy|38.736624|34.041219
Aksaray|Sarıyahşi|38.984935|33.845537
Aksaray|Sultanhanı|38.247417|33.548980
Amasya|Amasya|40.650325|35.832915
Amasya|Göynücek|40.397147|35.523693
Amasya|Gümüşhacıköy|40.873623|35.215897
Amasya|Hamamözü|40.783196|35.024215
Amasya|Merzifon|40.872139|35.463538
Amasya|Suluova|40.836413|35.645557
Amasya|Taşova|40.760349|36.321973
Ankara|Akyurt|40.130753|33.087068
Ankara|Altındağ|39.952359|32.866202
Ankara|Ayaş|40.015144|32.332422
Ankara|Bala|39.553364|33.123753
Ankara|Beypazarı|40.165649|31.920455
Ankara|Çamlıdere|40.491480|32.475826
Ankara|Çankaya|39.885332|32.855497
Ankara|Çubuk|40.238896|33.028947
Ankara|Elmadağ|39.917320|33.234395
Ankara|Etimesgut|39.949133|32.663880
Ankara|Evren|39.022120|33.807857
Ankara|Gölbaşı|39.792529|32.806665
Ankara|Güdül|40.210461|32.243225
Ankara|Haymana|39.434135|32.498792
Ankara|Kalecik|40.076398|33.445043
Ankara|Kazan|40.205444|32.681315
Ankara|Keçiören|39.977713|32.866980
Ankara|Kızılcahamam|40.470095|32.652856
Ankara|Mamak|39.931749|32.910871
Ankara|Nallıhan|40.188822|31.350345
Ankara|Polatlı|39.585670|32.141730
Ankara|Pursaklar|40.038105|32.896908
Ankara|Sincan|39.965035|32.583083
Ankara|Şereflikoçhisar|38.938933|33.543884
Ankara|Yenimahalle|39.966106|32.808779
Antalya|Akseki|37.045639|31.789676
Antalya|Aksu|36.947505|30.847705
Antalya|Alanya|36.886573|30.703024
Antalya|Demre|36.244569|29.987615
Antalya|Döşemealtı|37.022956|30.601333
Antalya|Elmalı|36.738379|29.918362
Antalya|Finike|36.304615|30.144533
Antalya|Gazipaşa|36.268271|32.317507
Antalya|Gündoğmuş|36.814338|31.998333
Antalya|İbradı|37.096944|31.596935
Antalya|Kaş|36.199375|29.641337
Antalya|Kemer|36.601382|30.563856
Antalya|Kepez|36.917630|30.714991
Antalya|Konyaaltı|36.872613|30.650365
Antalya|Korkuteli|37.066652|30.197050
Antalya|Kumluca|36.366905|30.285842
Antalya|Manavgat|36.787012|31.440667
Antalya|Muratpaşa|36.888864|30.720891
Antalya|Serik|36.916868|31.104748
Ardahan|Ardahan|41.110297|42.703558
Ardahan|Çıldır|41.126584|43.134597
Ardahan|Damal|41.342331|42.840995
Ardahan|Göle|40.793055|42.607840
Ardahan|Hanak|41.236770|42.844903
Ardahan|Posof|41.508378|42.727952
Artvin|Ardanuç|41.122288|42.068180
Artvin|Arhavi|41.352070|41.309353
Artvin|Artvin|41.183081|41.828745
Artvin|Borçka|41.360245|41.674674
Artvin|Hopa|41.385387|41.463193
Artvin|Kemalpaşa|41.481083|41.527053
Artvin|Murgul|41.279681|41.564126
Artvin|Şavşat|41.252488|42.356886
Artvin|Yusufeli|40.810829|41.527085
Aydın|Bozdoğan|37.673913|28.313204
Aydın|Buharkent|37.952658|28.738526
Aydın|Çine|37.612472|28.062310
Aydın|Didim|37.369687|27.268484
Aydın|Efeler|37.847175|27.833725
Aydın|Germencik|37.873232|27.595753
Aydın|İncirliova|37.853718|27.725088
Aydın|Karacasu|37.730684|28.606265
Aydın|Karpuzlu|37.559029|27.836092
Aydın|Koçarlı|37.762218|27.707338
Aydın|Köşk|37.851547|28.051478
Aydın|Kuşadası|37.863240|27.266873
Aydın|Kuyucak|37.909768|28.459672
Aydın|Nazilli|37.914061|28.327086
Aydın|Söke|37.751976|27.405629
Aydın|Sultanhisar|37.887318|28.155731
Aydın|Yenipazar|37.823293|28.195730
Balıkesir|Altıeylül|39.644633|27.883441
Balıkesir|Ayvalık|39.318096|26.691671
Balıkesir|Balya|39.749865|27.579661
Balıkesir|Bandırma|40.355471|27.969760
Balıkesir|Bigadiç|39.391305|28.131958
Balıkesir|Burhaniye|39.503018|26.980739
Balıkesir|Dursunbey|39.547293|28.652987
Balıkesir|Edremit|39.593835|27.015684
Balıkesir|Erdek|40.397435|27.790987
Balıkesir|Gömeç|39.390149|26.841155
Balıkesir|Gönen|40.104412|27.656398
Balıkesir|Havran|39.557622|27.100254
Balıkesir|İvrindi|39.582016|27.485259
Balıkesir|Karesi|39.646417|27.885341
Balıkesir|Kepsut|39.689869|28.152884
Balıkesir|Manyas|40.047692|27.967990
Balıkesir|Marmara|40.621641|27.629427
Balıkesir|Savaştepe|39.382701|27.650191
Balıkesir|Sındırgı|39.238705|28.174913
Balıkesir|Susurluk|39.918380|28.153189
Bartın|Amasra|41.748917|32.386696
Bartın|Bartın|41.635046|32.336621
Bartın|Kurucaşile|41.843890|32.720789
Bartın|Ulus|41.583939|32.641022
Batman|Batman|37.883574|41.127756
Batman|Beşiri|37.916246|41.292768
Batman|Gercüş|37.568309|41.385236
Batman|Hasankeyf|37.730469|41.416086
Batman|Kozluk|38.192585|41.488290
Batman|Sason|38.333947|41.420234
Bayburt|Aydıntepe|40.388986|40.147388
Bayburt|Bayburt|40.255161|40.220504
Bayburt|Demirözü|40.163028|39.891286
Bilecik|Bilecik|40.143510|29.975291
Bilecik|Bozüyük|39.904260|30.037278
Bilecik|Gölpazarı|40.283937|30.316536
Bilecik|İnhisar|40.050293|30.384846
Bilecik|Osmaneli|40.364310|30.027436
Bilecik|Pazaryeri|39.995406|29.903126
Bilecik|Söğüt|40.015552|30.181427
Bilecik|Yenipazar|40.176683|30.519076
Bingöl|Adaklı|39.228533|40.482658
Bingöl|Bingöl|38.886127|40.497233
Bingöl|Genç|38.751847|40.557639
Bingöl|Karlıova|39.294600|41.010238
Bingöl|Kiğı|39.310367|40.349628
Bingöl|Solhan|38.965935|41.051815
Bingöl|Yayladere|39.227513|40.068584
Bingöl|Yedisu|39.433959|40.545119
Bitlis|Adilcevaz|38.804306|42.734637
Bitlis|Ahlat|38.754653|42.483327
Bitlis|Bitlis|38.402054|42.108457
Bitlis|Güroymak|38.574172|42.025327
Bitlis|Hizan|38.225009|42.426324
Bitlis|Mutki|38.408974|41.921941
Bitlis|Tatvan|38.493196|42.287811
Bolu|Bolu|40.733295|31.611048
Bolu|Dörtdivan|40.720645|32.062598
Bolu|Gerede|40.798755|32.200907
Bolu|Göynük|40.398317|30.786617
Bolu|Kıbrıscık|40.408795|31.848718
Bolu|Mengen|40.939662|32.074833
Bolu|Mudurnu|40.465696|31.211231
Bolu|Seben|40.408509|31.571345
Bolu|Yeniçağa|40.770385|32.033641
Burdur|Ağlasun|37.649661|30.533192
Burdur|Altınyayla|36.996179|29.546769
Burdur|Bucak|37.456745|30.585539
Burdur|Burdur|37.724839|30.288729
Burdur|Çavdır|37.154989|29.690003
Burdur|Çeltikçi|37.533798|30.482606
Burdur|Gölhisar|37.151087|29.510715
Burdur|Karamanlı|37.370928|29.822595
Burdur|Kemer|37.353895|30.061130
Burdur|Tefenni|37.313249|29.775976
Burdur|Yeşilova|37.506438|29.755238
Bursa|Büyükorhan|39.770909|28.885410
Bursa|Gemlik|40.430165|29.157070
Bursa|Gürsu|40.217584|29.193563
Bursa|Harmancık|39.677224|29.153821
Bursa|İnegöl|40.080036|29.509653
Bursa|İznik|40.430344|29.722373
Bursa|Karacabey|40.215984|28.359059
Bursa|Keles|39.912887|29.234165
Bursa|Kestel|40.200754|29.212870
Bursa|Mudanya|40.375258|28.883793
Bursa|Mustafakemalpaşa|40.035048|28.413271
Bursa|Nilüfer|40.216997|28.984791
Bursa|Orhaneli|39.902656|28.986686
Bursa|Orhangazi|40.495535|29.310832
Bursa|Osmangazi|40.198220|29.061210
Bursa|Yenişehir|40.262632|29.652195
Bursa|Yıldırım|40.186636|29.128191
Çanakkale|Ayvacık|39.601185|26.403131
Çanakkale|Bayramiç|39.808624|26.613004
Çanakkale|Biga|40.226979|27.242836
Çanakkale|Bozcaada|39.834734|26.070192
Çanakkale|Çan|40.028946|27.051163
Çanakkale|Çanakkale|40.146271|26.402889
Çanakkale|Eceabat|40.185214|26.359097
Çanakkale|Ezine|39.801944|26.304250
Çanakkale|Gelibolu|40.405406|26.672254
Çanakkale|Gökçeada|40.200387|25.908462
Çanakkale|Lapseki|40.344367|26.684597
Çanakkale|Yenice|39.929752|27.255536
Çankırı|Atkaracalar|40.798087|33.082993
Çankırı|Bayramören|40.943071|33.203957
Çankırı|Çankırı|40.597195|33.621270
Çankırı|Çerkeş|40.811394|32.884442
Çankırı|Eldivan|40.530679|33.496617
Çankırı|Ilgaz|40.924887|33.625341
Çankırı|Kızılırmak|40.345976|33.987241
Çankırı|Korgun|40.734837|33.518488
Çankırı|Kurşunlu|40.840725|33.261348
Çankırı|Orta|40.627026|33.107749
Çankırı|Şabanözü|40.483160|33.282615
Çankırı|Yapraklı|40.757090|33.778770
Çorum|Alaca|40.169012|34.841477
Çorum|Bayat|40.578974|34.926489
Çorum|Boğazkale|40.021537|34.609226
Çorum|Çorum|40.549911|34.953734
Çorum|Dodurga|40.855307|34.810331
Çorum|İskilip|40.730691|34.471011
Çorum|Kargı|41.132750|34.491224
Çorum|Laçin|40.773801|34.884353
Çorum|Mecitözü|40.520586|35.295345
Çorum|Oğuzlar|40.753370|34.704330
Çorum|Ortaköy|40.351715|34.402878
Çorum|Osmancık|40.971484|34.801041
Çorum|Sungurlu|40.163824|34.375335
Çorum|Uğurludağ|40.446308|34.452489
Denizli|Acıpayam|37.427378|29.350510
Denizli|Babadağ|37.807712|28.856383
Denizli|Baklan|37.976676|29.606968
Denizli|Bekilli|38.229487|29.420825
Denizli|Beyağaç|37.235684|28.896027
Denizli|Bozkurt|37.827381|29.612650
Denizli|Buldan|38.044244|28.832672
Denizli|Çal|38.081351|29.397294
Denizli|Çameli|37.076111|29.344722
Denizli|Çardak|37.821539|29.678942
Denizli|Çivril|38.300588|29.737381
Denizli|Güney|38.153933|29.065225
Denizli|Honaz|37.752386|29.269696
Denizli|Kale|37.443787|28.846308
Denizli|Merkezefendi|37.784387|29.084172
Denizli|Pamukkale|37.920038|29.121753
Denizli|Sarayköy|37.924434|28.923084
Denizli|Serinhisar|37.580129|29.263892
Denizli|Tavas|37.572926|29.071260
Diyarbakır|Bağlar|37.917321|40.208291
Diyarbakır|Bismil|37.852769|40.661992
Diyarbakır|Çermik|38.135487|39.449684
Diyarbakır|Çınar|37.723677|40.415109
Diyarbakır|Çüngüş|38.211743|39.288170
Diyarbakır|Dicle|38.372813|40.071817
Diyarbakır|Eğil|38.257553|40.084520
Diyarbakır|Ergani|38.232780|39.754879
Diyarbakır|Hani|38.411567|40.395142
Diyarbakır|Hazro|38.253830|40.780853
Diyarbakır|Kayapınar|37.943783|40.184579
Diyarbakır|Kocaköy|38.290572|40.504348
Diyarbakır|Kulp|38.500427|41.011997
Diyarbakır|Lice|38.460313|40.647688
Diyarbakır|Silvan|38.141151|41.005610
Diyarbakır|Sur|37.911072|40.236650
Diyarbakır|Yenişehir|37.920046|40.229899
Düzce|Akçakoca|41.088228|31.123983
Düzce|Cumayeri|40.874689|30.950414
Düzce|Çilimli|40.893252|31.046630
Düzce|Düzce|40.839153|31.159536
Düzce|Gölyaka|40.776719|30.996441
Düzce|Gümüşova|40.846577|30.938601
Düzce|Kaynaşlı|40.772098|31.319201
Düzce|Yığılca|40.959995|31.444668
Edirne|Edirne|41.675933|26.558722
Edirne|Enez|40.725028|26.084567
Edirne|Havsa|41.549304|26.820564
Edirne|İpsala|40.921706|26.383985
Edirne|Keşan|40.854879|26.630306
Edirne|Lalapaşa|41.839039|26.736102
Edirne|Meriç|41.191341|26.420080
Edirne|Süloğlu|41.765698|26.908007
Edirne|Uzunköprü|41.296039|26.689612
Elazığ|Ağın|38.943909|38.713977
Elazığ|Alacakaya|38.462168|39.862523
Elazığ|Arıcak|38.563431|40.134504
Elazığ|Baskil|38.567769|38.823763
Elazığ|Elazığ|38.674716|39.222713
Elazığ|Karakoçan|38.956446|40.038027
Elazığ|Keban|38.794312|38.744442
Elazığ|Kovancılar|38.718925|39.865913
Elazığ|Maden|38.401297|39.669050
Elazığ|Palu|38.693485|39.928881
Elazığ|Sivrice|38.448693|39.306856
Erzincan|Çayırlı|39.804082|40.037184
Erzincan|Erzincan|39.746755|39.491030
Erzincan|İliç|39.472172|38.556529
Erzincan|Kemah|39.603625|39.033689
Erzincan|Kemaliye|39.262097|38.496527
Erzincan|Otlukbeli|39.971948|40.021676
Erzincan|Refahiye|39.902753|38.768175
Erzincan|Tercan|39.776487|40.383000
Erzincan|Üzümlü|39.710115|39.701031
Erzurum|Aşkale|39.921850|40.682683
Erzurum|Aziziye|39.945467|41.106005
Erzurum|Çat|39.611949|40.977421
Erzurum|Hınıs|39.358486|41.700327
Erzurum|Horasan|40.040798|42.162450
Erzurum|İspir|40.483378|40.999961
Erzurum|Karaçoban|39.350490|42.111626
Erzurum|Karayazı|39.701673|42.143121
Erzurum|Köprüköy|39.968466|41.868188
Erzurum|Narman|40.345200|41.871138
Erzurum|Oltu|40.545941|41.995977
Erzurum|Olur|40.826818|42.133243
Erzurum|Palandöken|39.931893|41.171483
Erzurum|Pasinler|39.977313|41.674491
Erzurum|Pazaryolu|40.414307|40.773831
Erzurum|Şenkaya|40.561141|42.345255
Erzurum|Tekman|39.646746|41.509384
Erzurum|Tortum|40.297082|41.550825
Erzurum|Uzundere|40.532682|41.548334
Erzurum|Yakutiye|39.918327|41.279387
Eskişehir|Alpu|39.768025|30.954108
Eskişehir|Beylikova|39.704227|31.189252
Eskişehir|Çifteler|39.375741|31.037866
Eskişehir|Günyüzü|39.384274|31.809677
Eskişehir|Han|39.158432|30.863439
Eskişehir|İnönü|39.816044|30.142753
Eskişehir|Mahmudiye|39.494704|30.987551
Eskişehir|Mihalgazi|40.026391|30.578225
Eskişehir|Mihalıççık|39.866642|31.496072
Eskişehir|Odunpazarı|39.765554|30.523765
Eskişehir|Sarıcakaya|40.038948|30.618948
Eskişehir|Seyitgazi|39.444322|30.696018
Eskişehir|Sivrihisar|39.451034|31.536797
Eskişehir|Tepebaşı|39.780989|30.514055
Gaziantep|Araban|37.425286|37.689419
Gaziantep|İslahiye|37.030684|36.636656
Gaziantep|Karkamış|36.831081|37.999956
Gaziantep|Nizip|37.001184|37.789031
Gaziantep|Nurdağı|37.177913|36.740945
Gaziantep|Oğuzeli|36.965108|37.508549
Gaziantep|Şahinbey|37.057590|37.379401
Gaziantep|Şehitkamil|37.072759|37.394977
Gaziantep|Yavuzeli|37.317868|37.566935
Giresun|Alucra|40.319666|38.765185
Giresun|Bulancak|40.938923|38.231878
Giresun|Çamoluk|40.135168|38.733976
Giresun|Çanakçı|40.916992|39.009316
Giresun|Dereli|40.739040|38.449132
Giresun|Doğankent|40.809918|38.914729
Giresun|Espiye|40.948323|38.711722
Giresun|Eynesil|41.064688|39.143405
Giresun|Giresun|40.917445|38.384786
Giresun|Görele|41.033521|38.998624
Giresun|Güce|40.894030|38.808027
Giresun|Keşap|40.915148|38.513856
Giresun|Piraziz|40.952573|38.124757
Giresun|Şebinkarahisar|40.288601|38.423612
Giresun|Tirebolu|41.007201|38.814631
Giresun|Yağlıdere|40.860535|38.625158
Gümüşhane|Gümüşhane|40.461784|39.475734
Gümüşhane|Kelkit|40.125523|39.435978
Gümüşhane|Köse|40.209617|39.653016
Gümüşhane|Kürtün|40.701864|39.085711
Gümüşhane|Şiran|40.189826|39.125232
Gümüşhane|Torul|40.557658|39.292636
Hakkari|Çukurca|37.246819|43.611042
Hakkari|Derecik|37.081280|44.311931
Hakkari|Hakkari|37.576696|43.737786
Hakkari|Şemdinli|37.306763|44.572508
Hakkari|Yüksekova|37.571769|44.282170
Hatay|Altınözü|36.114338|36.249560
Hatay|Antakya|36.202557|36.164099
Hatay|Arsuz|36.419625|35.990524
Hatay|Belen|36.491674|36.194446
Hatay|Defne|36.195185|36.147664
Hatay|Dörtyol|36.835312|36.227408
Hatay|Erzin|36.974589|36.130486
Hatay|Hassa|36.800110|36.517224
Hatay|İskenderun|36.590227|36.171035
Hatay|Kırıkhan|36.498955|36.362173
Hatay|Kumlu|36.364791|36.454090
Hatay|Payas|36.755932|36.215196
Hatay|Reyhanlı|36.268448|36.567231
Hatay|Samandağ|36.085160|35.979937
Hatay|Yayladağı|35.902954|36.062599
Iğdır|Aralık|39.873699|44.516037
Iğdır|Iğdır|39.921878|44.046796
Iğdır|Karakoyunlu|39.971146|44.173656
Iğdır|Tuzluca|40.040216|43.663788
Isparta|Aksu|37.798895|31.071111
Isparta|Atabey|37.950935|30.637407
Isparta|Eğirdir|37.874126|30.849041
Isparta|Gelendost|38.121471|31.013711
Isparta|Gönen|37.957545|30.512863
Isparta|Isparta|37.763672|30.555057
Isparta|Keçiborlu|37.947735|30.307042
Isparta|Senirkent|38.107803|30.550349
Isparta|Sütçüler|37.494994|30.980560
Isparta|Şarkikaraağaç|38.080712|31.366093
Isparta|Uluborlu|38.078741|30.449176
Isparta|Yalvaç|38.300287|31.174346
Isparta|Yenişarbademli|37.707269|31.387841
İstanbul|Adalar|40.874166|29.129325
İstanbul|Arnavutköy|41.184471|28.741245
İstanbul|Ataşehir|40.992938|29.113519
İstanbul|Avcılar|40.979939|28.721669
İstanbul|Bağcılar|41.034547|28.856756
İstanbul|Bahçelievler|41.003050|28.865770
İstanbul|Bakırköy|40.978259|28.874446
İstanbul|Başakşehir|41.107579|28.795071
İstanbul|Bayrampaşa|41.034555|28.911842
İstanbul|Beşiktaş|41.042847|29.007528
İstanbul|Beykoz|41.134300|29.092038
İstanbul|Beylikdüzü|41.003815|28.637288
İstanbul|Beyoğlu|41.028423|28.973681
İstanbul|Büyükçekmece|41.021654|28.579757
İstanbul|Çatalca|41.143680|28.460515
İstanbul|Çekmeköy|41.035158|29.173915
İstanbul|Esenler|41.037618|28.882452
İstanbul|Esenyurt|41.034286|28.680111
İstanbul|Eyüpsultan|41.047836|28.932738
İstanbul|Fatih|41.019285|28.947930
İstanbul|Gaziosmanpaşa|41.057831|28.912245
İstanbul|Güngören|41.025283|28.872650
İstanbul|Kadıköy|40.991295|29.024563
İstanbul|Kağıthane|41.079654|28.973120
İstanbul|Kartal|40.888504|29.185890
İstanbul|Küçükçekmece|40.991874|28.771196
İstanbul|Maltepe|40.924754|29.131078
İstanbul|Pendik|40.876872|29.234967
İstanbul|Sancaktepe|40.990520|29.228862
İstanbul|Sarıyer|41.168580|29.057262
İstanbul|Silivri|41.074248|28.248171
İstanbul|Sultanbeyli|40.967024|29.267131
İstanbul|Sultangazi|41.104334|28.861437
İstanbul|Şile|41.174407|29.612522
İstanbul|Şişli|41.063789|28.983164
İstanbul|Tuzla|40.816173|29.303419
İstanbul|Ümraniye|41.025636|29.096305
İstanbul|Üsküdar|41.026550|29.015132
İstanbul|Zeytinburnu|40.989865|28.903747
İzmir|Aliağa|38.803427|26.971373
İzmir|Balçova|38.395313|27.057870
İzmir|Bayındır|38.212820|27.649558
İzmir|Bayraklı|38.463914|27.164403
İzmir|Bergama|39.118948|27.177364
İzmir|Beydağ|38.087032|28.210692
İzmir|Bornova|38.466190|27.219194
İzmir|Buca|38.383493|27.168651
İzmir|Çeşme|38.324404|26.302960
İzmir|Çiğli|38.491658|27.063435
İzmir|Dikili|39.074971|26.889249
İzmir|Foça|38.668916|26.754775
İzmir|Gaziemir|38.326276|27.139966
İzmir|Güzelbahçe|38.375516|26.875553
İzmir|Karabağlar|38.386530|27.134722
İzmir|Karaburun|38.635442|26.509803
İzmir|Karşıyaka|38.503445|27.113455
İzmir|Kemalpaşa|38.469465|27.400214
İzmir|Kınık|39.087598|27.380475
İzmir|Kiraz|38.230372|28.201579
İzmir|Konak|38.418717|27.128267
İzmir|Menderes|38.255157|27.138140
İzmir|Menemen|38.603362|27.076431
İzmir|Narlıdere|38.395441|27.001098
İzmir|Ödemiş|38.222053|27.965566
İzmir|Seferihisar|38.195029|26.834219
İzmir|Selçuk|37.947965|27.368502
İzmir|Tire|38.092572|27.728037
İzmir|Torbalı|38.170119|27.347338
İzmir|Urla|38.322818|26.767300
Kahramanmaraş|Afşin|38.243855|36.915287
Kahramanmaraş|Andırın|37.575017|36.355248
Kahramanmaraş|Çağlayancerit|37.749966|37.292785
Kahramanmaraş|Dulkadiroğlu|37.579774|36.931240
Kahramanmaraş|Ekinözü|38.060669|37.189415
Kahramanmaraş|Elbistan|38.202229|37.190301
Kahramanmaraş|Göksun|38.021313|36.494563
Kahramanmaraş|Nurhak|37.966147|37.442189
Kahramanmaraş|Onikişubat|37.577757|36.923735
Kahramanmaraş|Pazarcık|37.490848|37.289969
Kahramanmaraş|Türkoğlu|37.375418|36.861967
Karabük|Eflani|41.422944|32.958188
Karabük|Eskipazar|40.956758|32.533503
Karabük|Karabük|41.195540|32.623115
Karabük|Ovacık|41.075887|32.920009
Karabük|Safranbolu|41.245666|32.692957
Karabük|Yenice|41.200666|32.327925
Karaman|Ayrancı|37.371061|33.666261
Karaman|Başyayla|36.753031|32.679849
Karaman|Ermenek|36.638904|32.888872
Karaman|Karaman|37.180850|33.219455
Karaman|Kazımkarabekir|37.226525|32.958257
Karaman|Sarıveliler|36.698526|32.618221
Kars|Akyaka|40.739846|43.623412
Kars|Arpaçay|40.603701|43.692311
Kars|Digor|40.376509|43.411947
Kars|Kağızman|40.140677|43.120463
Kars|Kars|40.607076|43.094752
Kars|Sarıkamış|40.335890|42.576944
Kars|Selim|40.433571|42.804111
Kars|Susuz|40.779227|43.128688
Kastamonu|Abana|41.978501|34.007723
Kastamonu|Ağlı|41.687153|33.553795
Kastamonu|Araç|41.241201|33.324933
Kastamonu|Azdavay|41.642076|33.300936
Kastamonu|Bozkurt|41.958798|34.012628
Kastamonu|Cide|41.891571|33.003712
Kastamonu|Çatalzeytin|41.953111|34.222675
Kastamonu|Daday|41.475156|33.463749
Kastamonu|Devrekani|41.602564|33.837092
Kastamonu|Doğanyurt|42.005597|33.460457
Kastamonu|Hanönü|41.625572|34.468001
Kastamonu|İhsangazi|41.204012|33.555917
Kastamonu|İnebolu|41.978576|33.759903
Kastamonu|Kastamonu|41.376536|33.777009
Kastamonu|Küre|41.805882|33.710262
Kastamonu|Pınarbaşı|41.603603|33.110875
Kastamonu|Seydiler|41.618845|33.718625
Kastamonu|Şenpazar|41.808599|33.232873
Kastamonu|Taşköprü|41.507478|34.212834
Kastamonu|Tosya|41.016496|34.038608
Kayseri|Akkışla|39.001463|36.170990
Kayseri|Bünyan|38.845821|35.857744
Kayseri|Develi|38.387929|35.490100
Kayseri|Felahiye|39.090713|35.567492
Kayseri|Hacılar|38.644270|35.449766
Kayseri|İncesu|38.628811|35.196490
Kayseri|Kocasinan|38.736258|35.494981
Kayseri|Melikgazi|38.719906|35.505662
Kayseri|Özvatan|39.105926|35.699208
Kayseri|Pınarbaşı|38.721446|36.394132
Kayseri|Sarıoğlan|39.077060|35.967249
Kayseri|Sarız|38.480238|36.497026
Kayseri|Talas|38.692949|35.549455
Kayseri|Tomarza|38.447687|35.800914
Kayseri|Yahyalı|38.100343|35.354101
Kayseri|Yeşilhisar|38.365307|35.084374
Kırıkkale|Bahşili|39.813252|33.471792
Kırıkkale|Balışeyh|39.909720|33.718343
Kırıkkale|Çelebi|39.463699|33.524414
Kırıkkale|Delice|39.945517|34.031297
Kırıkkale|Karakeçili|39.559931|33.357723
Kırıkkale|Keskin|39.673994|33.615167
Kırıkkale|Kırıkkale|39.885075|33.578531
Kırıkkale|Sulakyurt|40.157677|33.715667
Kırıkkale|Yahşihan|39.849754|33.452421
Kırklareli|Babaeski|41.430262|27.091779
Kırklareli|Demirköy|41.824151|27.765147
Kırklareli|Kırklareli|41.737022|27.223552
Kırklareli|Kofçaz|41.944962|27.157854
Kırklareli|Lüleburgaz|41.404136|27.355465
Kırklareli|Pehlivanköy|41.346018|26.918159
Kırklareli|Pınarhisar|41.625495|27.515775
Kırklareli|Vize|41.572868|27.767038
Kırşehir|Akçakent|39.625024|34.096216
Kırşehir|Akpınar|39.447968|33.963927
Kırşehir|Boztepe|39.271614|34.263992
Kırşehir|Çiçekdağı|39.603596|34.415803
Kırşehir|Kaman|39.358898|33.724396
Kırşehir|Kırşehir|39.146114|34.160559
Kırşehir|Mucur|39.059874|34.380006
Kilis|Elbeyli|36.675039|37.466866
Kilis|Kilis|36.716555|37.114607
Kilis|Musabeyli|36.886129|36.915586
Kilis|Polateli|36.840843|37.142879
Kocaeli|Başiskele|40.712928|29.928697
Kocaeli|Çayırova|40.810534|29.347373
Kocaeli|Darıca|40.791404|29.391909
Kocaeli|Derince|40.757449|29.830763
Kocaeli|Dilovası|40.775606|29.526096
Kocaeli|Gebze|40.800670|29.431767
Kocaeli|Gölcük|40.716925|29.819588
Kocaeli|İzmit|40.772114|29.950562
Kocaeli|Kandıra|41.070391|30.152330
Kocaeli|Karamürsel|40.691250|29.616586
Kocaeli|Kartepe|40.745436|30.011283
Kocaeli|Körfez|40.760756|29.783940
Konya|Ahırlı|37.238711|32.118947
Konya|Akören|37.452329|32.372524
Konya|Akşehir|38.358850|31.420196
Konya|Altınekin|38.307306|32.869147
Konya|Beyşehir|37.680229|31.722835
Konya|Bozkır|37.189874|32.247202
Konya|Cihanbeyli|38.658990|32.923730
Konya|Çeltik|39.001570|31.846832
Konya|Çumra|37.572011|32.784598
Konya|Derbent|38.013746|32.017247
Konya|Derebucak|37.391501|31.510678
Konya|Doğanhisar|38.146017|31.676864
Konya|Emirgazi|37.903549|33.835954
Konya|Ereğli|37.514072|34.047342
Konya|Güneysınır|37.298065|32.721141
Konya|Hadim|36.986089|32.455804
Konya|Halkapınar|37.433497|34.186605
Konya|Hüyük|37.954776|31.597332
Konya|Ilgın|38.281774|31.917867
Konya|Kadınhanı|38.240050|32.212206
Konya|Karapınar|37.717758|33.547627
Konya|Karatay|37.871044|32.503045
Konya|Kulu|39.037057|33.028629
Konya|Meram|37.867953|32.494651
Konya|Sarayönü|38.255880|32.404007
Konya|Selçuklu|37.876866|32.487395
Konya|Seydişehir|37.419371|31.848333
Konya|Taşkent|36.923925|32.492617
Konya|Tuzlukçu|38.472923|31.627061
Konya|Yalıhüyük|37.300571|32.085419
Konya|Yunak|38.818004|32.013959
Kütahya|Altıntaş|39.060353|30.107576
Kütahya|Aslanapa|39.215223|29.869627
Kütahya|Çavdarhisar|39.194385|29.619524
Kütahya|Domaniç|39.801400|29.611854
Kütahya|Dumlupınar|38.854363|29.977641
Kütahya|Emet|39.341462|29.258593
Kütahya|Gediz|38.989844|29.393957
Kütahya|Hisarcık|39.250510|29.231291
Kütahya|Kütahya|39.419911|29.985789
Kütahya|Pazarlar|38.994628|29.123122
Kütahya|Simav|39.088675|28.980015
Kütahya|Şaphane|39.025409|29.219954
Kütahya|Tavşanlı|39.540218|29.486341
Malatya|Akçadağ|38.290898|38.044429
Malatya|Arapgir|39.042242|38.489834
Malatya|Arguvan|38.782062|38.264288
Malatya|Battalgazi|38.425049|38.365498
Malatya|Darende|38.557310|37.492652
Malatya|Doğanşehir|38.093410|37.878439
Malatya|Doğanyol|38.309952|39.038077
Malatya|Hekimhan|38.810719|37.927774
Malatya|Kale|38.417495|38.768710
Malatya|Kuluncak|38.884858|37.670741
Malatya|Pütürge|38.197344|38.870232
Malatya|Yazıhan|38.595763|38.181695
Malatya|Yeşilyurt|38.295488|38.247452
Manisa|Ahmetli|38.516222|27.935939
Manisa|Akhisar|38.924053|27.840190
Manisa|Alaşehir|38.350747|28.516575
Manisa|Demirci|39.047417|28.658368
Manisa|Gölmarmara|38.712187|27.915700
Manisa|Gördes|38.933036|28.288649
Manisa|Kırkağaç|39.115501|27.685978
Manisa|Köprübaşı|38.748023|28.402953
Manisa|Kula|38.546891|28.647434
Manisa|Salihli|38.482940|28.130905
Manisa|Sarıgöl|38.238225|28.696577
Manisa|Saruhanlı|38.732741|27.577392
Manisa|Selendi|38.743431|28.870208
Manisa|Soma|39.198702|27.624166
Manisa|Şehzadeler|38.645859|27.502838
Manisa|Turgutlu|38.509999|27.707173
Manisa|Yunusemre|38.608654|27.419456
Mardin|Artuklu|37.320980|40.725130
Mardin|Dargeçit|37.545948|41.720601
Mardin|Derik|37.364698|40.269883
Mardin|Kızıltepe|37.191655|40.584797
Mardin|Mazıdağı|37.477321|40.486491
Mardin|Midyat|37.415269|41.373313
Mardin|Nusaybin|37.066740|41.216620
Mardin|Ömerli|37.402127|40.953883
Mardin|Savur|37.536840|40.887375
Mardin|Yeşilli|37.339606|40.823006
Mersin|Akdeniz|36.802059|34.633210
Mersin|Anamur|36.080323|32.831211
Mersin|Aydıncık|36.145203|33.322423
Mersin|Bozyazı|36.104822|32.974286
Mersin|Çamlıyayla|37.166540|34.593443
Mersin|Erdemli|36.605709|34.310287
Mersin|Gülnar|36.338664|33.399013
Mersin|Mezitli|36.749529|34.530702
Mersin|Mut|36.643421|33.437263
Mersin|Silifke|36.377817|33.926037
Mersin|Tarsus|36.916483|34.895149
Mersin|Toroslar|36.820763|34.610419
Mersin|Yenişehir|36.782277|34.592449
Muğla|Bodrum|37.034399|27.430651
Muğla|Dalaman|36.767214|28.800274
Muğla|Datça|36.726340|27.687386
Muğla|Fethiye|36.622115|29.115338
Muğla|Kavaklıdere|37.445631|28.364925
Muğla|Köyceğiz|36.958501|28.688944
Muğla|Marmaris|36.852255|28.274266
Muğla|Menteşe|37.217878|28.364594
Muğla|Milas|37.316271|27.779976
Muğla|Ortaca|36.838904|28.765417
Muğla|Seydikemer|36.648160|29.362179
Muğla|Ula|37.102923|28.416366
Muğla|Yatağan|37.341670|28.139511
Muş|Bulanık|39.092386|42.270834
Muş|Hasköy|38.682616|41.688529
Muş|Korkut|38.738805|41.783000
Muş|Malazgirt|39.146141|42.540946
Muş|Muş|38.732222|41.489893
Muş|Varto|39.172087|41.454664
Nevşehir|Acıgöl|38.550155|34.508966
Nevşehir|Avanos|38.870906|34.853666
Nevşehir|Derinkuyu|38.400309|34.699588
Nevşehir|Gülşehir|38.762233|34.502657
Nevşehir|Hacıbektaş|39.001335|34.642700
Nevşehir|Kozaklı|39.211147|34.782579
Nevşehir|Nevşehir|38.587156|34.719663
Nevşehir|Ürgüp|38.592812|34.959455
Niğde|Altunhisar|37.999013|34.371506
Niğde|Bor|37.887607|34.562503
Niğde|Çamardı|37.834410|34.986392
Niğde|Çiftlik|38.174971|34.484796
Niğde|Niğde|37.969849|34.676449
Niğde|Ulukışla|37.545708|34.480360
Ordu|Akkuş|40.792495|37.017744
Ordu|Altınordu|40.978659|37.896430
Ordu|Aybastı|40.684623|37.398808
Ordu|Çamaş|40.902561|37.528114
Ordu|Çatalpınar|40.878986|37.453177
Ordu|Çaybaşı|41.017194|37.098661
Ordu|Fatsa|41.030881|37.500221
Ordu|Gölköy|40.687349|37.615602
Ordu|Gülyalı|40.966251|38.058576
Ordu|Gürgentepe|40.788617|37.600861
Ordu|İkizce|41.054426|37.077799
Ordu|Kabadüz|40.859805|37.890886
Ordu|Kabataş|40.752391|37.449932
Ordu|Korgan|40.829558|37.344141
Ordu|Kumru|40.871981|37.261819
Ordu|Mesudiye|40.463562|37.773569
Ordu|Perşembe|41.066948|37.773632
Ordu|Ulubey|40.873034|37.758542
Ordu|Ünye|41.126241|37.285362
Osmaniye|Bahçe|37.187068|36.558913
Osmaniye|Düziçi|37.240122|36.453407
Osmaniye|Hasanbeyli|37.130932|36.555539
Osmaniye|Kadirli|37.461770|36.169409
Osmaniye|Osmaniye|37.073359|36.250767
Osmaniye|Sumbas|37.453623|36.024760
Osmaniye|Toprakkale|37.066642|36.145554
Rize|Ardeşen|41.191875|40.989418
Rize|Çamlıhemşin|41.045571|41.005697
Rize|Çayeli|41.087833|40.723697
Rize|Derepazarı|41.024327|40.421580
Rize|Fındıklı|41.271235|41.141416
Rize|Güneysu|40.977154|40.613591
Rize|Hemşin|41.047521|40.898798
Rize|İkizdere|40.777992|40.559574
Rize|İyidere|41.011978|40.360970
Rize|Kalkandere|40.927948|40.442509
Rize|Pazar|41.180258|40.886848
Rize|Rize|41.024825|40.519914
Sakarya|Adapazarı|40.775521|30.402030
Sakarya|Akyazı|40.681419|30.624611
Sakarya|Arifiye|40.713143|30.355836
Sakarya|Erenler|40.756363|30.410894
Sakarya|Ferizli|40.940175|30.484618
Sakarya|Geyve|40.509055|30.290251
Sakarya|Hendek|40.795503|30.745273
Sakarya|Karapürçek|40.642184|30.537518
Sakarya|Karasu|41.095609|30.692462
Sakarya|Kaynarca|41.034047|30.305535
Sakarya|Kocaali|41.054281|30.850745
Sakarya|Pamukova|40.505623|30.169305
Sakarya|Sapanca|40.693058|30.273417
Sakarya|Serdivan|40.760406|30.362961
Sakarya|Söğütlü|40.903921|30.471664
Sakarya|Taraklı|40.396578|30.492161
Samsun|19 Mayıs|41.493655|36.081049
Samsun|Alaçam|41.606864|35.597332
Samsun|Asarcık|41.036029|36.234821
Samsun|Atakum|41.332259|36.270465
Samsun|Ayvacık|40.989063|36.630488
Samsun|Bafra|41.566595|35.902478
Samsun|Canik|41.267128|36.355627
Samsun|Çarşamba|41.198291|36.727020
Samsun|Havza|40.965707|35.667103
Samsun|İlkadım|41.290200|36.334676
Samsun|Kavak|41.068537|36.026884
Samsun|Ladik|40.908217|35.894565
Samsun|Salıpazarı|41.081291|36.825741
Samsun|Tekkeköy|41.213498|36.457804
Samsun|Terme|41.209039|36.972169
Samsun|Vezirköprü|41.143390|35.460472
Samsun|Yakakent|41.632951|35.531590
Siirt|Baykan|38.162873|41.785685
Siirt|Eruh|37.750599|42.180836
Siirt|Kurtalan|37.928423|41.697364
Siirt|Pervari|37.932504|42.547991
Siirt|Siirt|37.927362|41.942180
Siirt|Şirvan|38.061793|42.029931
Siirt|Tillo|37.950064|42.012517
Sinop|Ayancık|41.946072|34.588309
Sinop|Boyabat|41.468974|34.767227
Sinop|Dikmen|41.651624|35.265057
Sinop|Durağan|41.417967|35.055810
Sinop|Erfelek|41.879362|34.908051
Sinop|Gerze|41.803270|35.199616
Sinop|Saraydüzü|41.329517|34.848602
Sinop|Sinop|42.026580|35.151151
Sinop|Türkeli|41.948301|34.339586
Sivas|Akıncılar|40.078717|38.346840
Sivas|Altınyayla|39.271668|36.751548
Sivas|Divriği|39.371173|38.115536
Sivas|Doğanşar|40.214232|37.536510
Sivas|Gemerek|39.183122|36.071104
Sivas|Gölova|40.061289|38.608431
Sivas|Gürün|38.799008|37.157207
Sivas|Hafik|39.854585|37.388960
Sivas|İmranlı|39.876001|38.112930
Sivas|Kangal|39.276030|37.450865
Sivas|Koyulhisar|40.301321|37.832276
Sivas|Sivas|39.750357|37.014517
Sivas|Suşehri|40.162734|38.085566
Sivas|Şarkışla|39.364093|36.403158
Sivas|Ulaş|39.443194|37.035388
Sivas|Yıldızeli|39.867186|36.593311
Sivas|Zara|39.897201|37.759073
Şanlıurfa|Akçakale|36.708295|38.948682
Şanlıurfa|Birecik|37.031532|37.980027
Şanlıurfa|Bozova|37.361958|38.525358
Şanlıurfa|Ceylanpınar|36.843323|40.051410
Şanlıurfa|Eyyübiye|37.148136|38.782466
Şanlıurfa|Halfeti|37.230922|37.945736
Şanlıurfa|Haliliye|37.167487|38.795560
Şanlıurfa|Harran|36.871006|39.025136
Şanlıurfa|Hilvan|37.586169|38.954687
Şanlıurfa|Karaköprü|37.204652|38.795851
Şanlıurfa|Siverek|37.754090|39.317743
Şanlıurfa|Suruç|36.975185|38.424295
Şanlıurfa|Viranşehir|37.232895|39.762014
Şırnak|Beytüşşebap|37.570949|43.170219
Şırnak|Cizre|37.332437|42.185470
Şırnak|Güçlükonak|37.471163|41.911127
Şırnak|İdil|37.340186|41.892403
Şırnak|Silopi|37.249158|42.470750
Şırnak|Şırnak|37.548722|42.361879
Şırnak|Uludere|37.417439|42.933578
Tekirdağ|Çerkezköy|41.286248|27.999468
Tekirdağ|Çorlu|41.159087|27.804107
Tekirdağ|Ergene|41.355763|27.202574
Tekirdağ|Hayrabolu|41.214671|27.108176
Tekirdağ|Kapaklı|41.326858|27.976151
Tekirdağ|Malkara|40.893082|26.902380
Tekirdağ|Marmaraereğlisi|40.969370|27.955001
Tekirdağ|Muratlı|41.172293|27.501511
Tekirdağ|Saray|41.442679|27.921399
Tekirdağ|Süleymanpaşa|40.977173|27.503855
Tekirdağ|Şarköy|40.614873|27.112186
Tokat|Almus|40.375013|36.903732
Tokat|Artova|40.114067|36.300789
Tokat|Başçiftlik|40.546907|37.168322
Tokat|Erbaa|40.672798|36.571506
Tokat|Niksar|40.591328|36.943493
Tokat|Pazar|40.275779|36.282290
Tokat|Reşadiye|40.392069|37.338322
Tokat|Sulusaray|39.997498|36.083607
Tokat|Tokat|40.323353|36.552162
Tokat|Turhal|40.389589|36.078043
Tokat|Yeşilyurt|40.007361|36.216735
Tokat|Zile|40.281462|35.931406
Trabzon|Akçaabat|41.021628|39.570673
Trabzon|Araklı|40.935821|40.058041
Trabzon|Arsin|40.953730|39.932591
Trabzon|Beşikdüzü|41.052731|39.228038
Trabzon|Çarşıbaşı|41.082581|39.378486
Trabzon|Çaykara|40.747855|40.242043
Trabzon|Dernekpazarı|40.799628|40.247895
Trabzon|Düzköy|40.873831|39.426109
Trabzon|Hayrat|40.889173|40.368047
Trabzon|Köprübaşı|40.807214|40.124241
Trabzon|Maçka|40.812042|39.612715
Trabzon|Of|40.947642|40.269407
Trabzon|Ortahisar|41.006215|39.725759
Trabzon|Sürmene|40.912771|40.113481
Trabzon|Şalpazarı|40.941810|39.193840
Trabzon|Tonya|40.886348|39.290808
Trabzon|Vakfıkebir|41.047236|39.276191
Trabzon|Yomra|40.959246|39.847125
Tunceli|Çemişgezek|39.062860|38.910426
Tunceli|Hozat|39.107587|39.219392
Tunceli|Mazgirt|39.019546|39.605911
Tunceli|Nazımiye|39.179434|39.828832
Tunceli|Ovacık|39.358383|39.214447
Tunceli|Pertek|38.865556|39.327653
Tunceli|Pülümür|39.487607|39.899072
Tunceli|Tunceli|39.106064|39.548269
Uşak|Banaz|38.739973|29.753190
Uşak|Eşme|38.398376|28.968960
Uşak|Karahallı|38.320633|29.531341
Uşak|Sivaslı|38.500031|29.683952
Uşak|Ulubey|38.419312|29.290948
Uşak|Uşak|38.674040|29.405842
Van|Bahçesaray|38.123716|42.807734
Van|Başkale|38.044911|44.016267
Van|Çaldıran|39.135520|43.902938
Van|Çatak|38.006907|43.059198
Van|Edremit|38.425809|43.260234
Van|Erciş|39.028989|43.359106
Van|Gevaş|38.295286|43.108271
Van|Gürpınar|38.323265|43.407901
Van|İpekyolu|38.503817|43.395510
Van|Muradiye|38.990295|43.761175
Van|Özalp|38.653352|43.989727
Van|Saray|38.649354|44.169550
Van|Tuşba|38.512311|43.375282
Yalova|Altınova|40.696348|29.509944
Yalova|Armutlu|40.519620|28.828011
Yalova|Çınarcık|40.643400|29.119302
Yalova|Çiftlikköy|40.664523|29.322921
Yalova|Termal|40.606180|29.174289
Yalova|Yalova|40.658253|29.269992
Yozgat|Akdağmadeni|39.782147|35.892093
Yozgat|Aydıncık|40.130252|35.285161
Yozgat|Boğazlıyan|39.246695|35.302296
Yozgat|Çandır|39.244605|35.513860
Yozgat|Çayıralan|39.304801|35.646372
Yozgat|Çekerek|40.064957|35.507250
Yozgat|Kadışehri|39.997469|35.791756
Yozgat|Saraykent|39.693159|35.510792
Yozgat|Sarıkaya|39.493590|35.375510
Yozgat|Sorgun|39.809868|35.185431
Yozgat|Şefaatli|39.498544|34.749690
Yozgat|Yenifakılı|39.212775|35.002441
Yozgat|Yerköy|39.687101|34.465736
Yozgat|Yozgat|39.822197|34.808097
Zonguldak|Alaplı|41.180375|31.386178
Zonguldak|Çaycuma|41.426946|32.072821
Zonguldak|Devrek|41.218882|31.955835
Zonguldak|Ereğli|41.279552|31.422967
Zonguldak|Gökçebey|41.306749|32.138467
Zonguldak|Kilimli|41.486951|31.838421
Zonguldak|Kozlu|41.433874|31.747285
Zonguldak|Zonguldak|41.452677|31.787598
```

---

## 2. `app/src/main/java/app/evcockpit/navigation/OfflineTurkishPlaces.kt`

```kotlin
package app.evcockpit.navigation

import java.util.Locale

/**
 * Bounded offline first-letter suggestions for Türkiye's provinces and districts.
 * Coordinates are approximate locality centres, NOT street-address matches.
 * Dataset: AEKHer/il-ilce-koordinat (MIT), sourced from OpenStreetMap (ODbL).
 */
class OfflineTurkishPlaces private constructor(
    private val records: List<Record>,
) {
    private data class Record(val province: String, val district: String, val point: GeoPoint) {
        val provinceKey = normalize(province)
        val districtKey = normalize(district)
    }

    fun suggest(query: String, maxResults: Int = 8): List<PhotonPlace> {
        val normalized = normalize(query)
        if (normalized.length < 2 || normalized.length > 120 || maxResults <= 0)
            return emptyList()

        fun rank(item: Record): Int = when {
            item.provinceKey == normalized && item.districtKey == normalized -> 0
            item.districtKey == normalized -> 1
            item.provinceKey == normalized -> 2
            item.districtKey.startsWith(normalized) -> 3
            item.provinceKey.startsWith(normalized) -> 4
            (item.provinceKey + " " + item.districtKey).startsWith(normalized) -> 5
            else -> 100
        }

        return records.asSequence()
            .map { it to rank(it) }
            .filter { it.second < 100 }
            .sortedWith(compareBy<Pair<Record, Int>> { it.second }
                .thenBy { it.first.provinceKey }
                .thenBy { it.first.districtKey })
            .take(maxResults.coerceAtMost(10))
            .map { (record, _) ->
                PhotonPlace("${record.district}, ${record.province} (il/ilçe merkezi)", record.point)
            }
            .toList()
    }

    companion object {
        private val tr = Locale.forLanguageTag("tr-TR")
        fun normalize(text: String): String = text.trim().lowercase(tr)
            .replace('ı', 'i').replace('ğ', 'g')
            .replace('ş', 's').replace('ç', 'c')
            .replace('ö', 'o').replace('ü', 'u')

        fun fromDelimited(content: String): OfflineTurkishPlaces {
            val records = content.lineSequence().take(1_500).mapNotNull { line ->
                if (line.isBlank() || line.startsWith("#") || line.length > 240)
                    return@mapNotNull null
                val parts = line.split('|')
                if (parts.size != 4) return@mapNotNull null
                val lat = parts[2].toDoubleOrNull() ?: return@mapNotNull null
                val lon = parts[3].toDoubleOrNull() ?: return@mapNotNull null
                if (lat !in 35.0..43.0 || lon !in 25.0..46.0) return@mapNotNull null
                val province = parts[0].trim().take(80)
                val district = parts[1].trim().take(80)
                if (province.isBlank() || district.isBlank()) return@mapNotNull null
                val point = runCatching { GeoPoint(lat, lon) }.getOrNull()
                    ?: return@mapNotNull null
                Record(province, district, point)
            }.toList()
            return OfflineTurkishPlaces(records)
        }

        fun empty(): OfflineTurkishPlaces = OfflineTurkishPlaces(emptyList())
    }
}
```

---

## 3. `app/src/main/java/app/evcockpit/navigation/PlaceAutocomplete.kt`

```kotlin
package app.evcockpit.navigation

import android.app.Application
import app.evcockpit.CockpitApplication
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Place names/cities: Open-Meteo Geocoding; streets/POI: fair-use Photon.
 * Neither provider is a guaranteed navigation-grade address index.
 */
enum class PlaceField { ORIGIN, DESTINATION }

object AutocompletePolicy {
    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 120
    const val DEBOUNCE_MS = 250L
    const val CITY_REQUEST_GAP_MS = 2_500L
    const val CACHE_MAX = 16

    fun eligible(value: String): Boolean =
        value.trim().length in MIN_LENGTH..MAX_LENGTH

    fun normalized(value: String): String = value.trim().lowercase(
        java.util.Locale.forLanguageTag("tr-TR")
    )
}

/** HTTP requests are restricted to a single fixed HTTPS host, not a user-supplied URL. */
class OpenMeteoCityGeocoder {
    suspend fun search(query: String): List<PhotonPlace> = withContext(Dispatchers.IO) {
        require(AutocompletePolicy.eligible(query))
        val name = URLEncoder.encode(query.trim(), "UTF-8")
        val conn = URL(
            "https://geocoding-api.open-meteo.com/v1/search?name=$name&count=8&language=tr&format=json"
        ).openConnection() as HttpURLConnection
        conn.connectTimeout = 6_000
        conn.readTimeout = 6_000
        conn.instanceFollowRedirects = false
        conn.setRequestProperty("User-Agent", "ID4-Yol-Arkadasi/0.3 (personal-use)")
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext emptyList()
            val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val buffer = CharArray(96_000)
                val count = reader.read(buffer)
                if (count < 1) "" else String(buffer, 0, count)
            }
            OpenMeteoCityParser.parse(raw)
        } finally {
            conn.disconnect()
        }
    }
}

object OpenMeteoCityParser {
    fun parse(raw: String): List<PhotonPlace> = runCatching {
        val cities = JSONObject(raw).optJSONArray("results") ?: return@runCatching emptyList()
        buildList {
            for (i in 0 until minOf(8, cities.length())) {
                val item = cities.optJSONObject(i) ?: continue
                val lat = item.optDouble("latitude", Double.NaN)
                val lon = item.optDouble("longitude", Double.NaN)
                val point = runCatching { GeoPoint(lat, lon) }.getOrNull() ?: continue
                val label = listOf(
                    item.optString("name"),
                    item.optString("admin1"),
                    item.optString("country"),
                ).filter { it.isNotBlank() && it != "null" }.distinct()
                    .joinToString(", ").take(140)
                if (label.isNotBlank()) add(PhotonPlace(label, point))
            }
        }
    }.getOrDefault(emptyList())
}

interface PlaceSuggestionSource {
    suspend fun suggest(query: String): List<PhotonPlace>
}

/**
 * A single-query cache and conservative server-side pacing. Never uses public
 * Nominatim's autocomplete-prohibited endpoint.
 */
class PublicPlaceSuggestionSource(
    private val cities: OpenMeteoCityGeocoder = OpenMeteoCityGeocoder(),
    private val photon: PhotonGeocoder = PhotonGeocoder(),
) : PlaceSuggestionSource {
    private val mutex = Mutex()
    private val cache = LinkedHashMap<String, List<PhotonPlace>>()
    private var lastCityAtMs = -AutocompletePolicy.CITY_REQUEST_GAP_MS
    private var lastPhotonAtMs = -PhotonPolicy.MIN_INTERVAL_MS

    override suspend fun suggest(query: String): List<PhotonPlace> {
        if (!AutocompletePolicy.eligible(query)) return emptyList()
        val key = AutocompletePolicy.normalized(query)
        return mutex.withLock {
            cache[key]?.let { return@withLock it }
            val now = SystemClock.elapsedRealtime()
            val wait = (AutocompletePolicy.CITY_REQUEST_GAP_MS -
                (now - lastCityAtMs)).coerceAtLeast(0)
            if (wait > 0) delay(wait)
            lastCityAtMs = SystemClock.elapsedRealtime()

            // Photon is a public fair-use demo. Keep its original 10-second budget,
            // while city suggestions remain available at a slower, bounded cadence.
            val address = if (lastCityAtMs - lastPhotonAtMs >= PhotonPolicy.MIN_INTERVAL_MS) {
                lastPhotonAtMs = lastCityAtMs
                safeFetch { photon.search(query) }
            } else emptyList()
            val places = safeFetch { cities.search(query) }
            val combined = (address + places).distinctBy {
                it.label + "|" + it.point.latitude + "|" + it.point.longitude
            }.take(10)
            cache[key] = combined
            if (cache.size > AutocompletePolicy.CACHE_MAX) {
                cache.remove(cache.keys.first())
            }
            combined
        }
    }

    private suspend fun safeFetch(block: suspend () -> List<PhotonPlace>): List<PhotonPlace> =
        try { block() } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            emptyList()
        }
}

data class PlaceSuggestionState(
    val originText: String = "",
    val destinationText: String = "",
    val originResults: List<PhotonPlace> = emptyList(),
    val destinationResults: List<PhotonPlace> = emptyList(),
    val originStatus: String = "GPS konumunu kullanabilir veya başlangıç yazabilirsin.",
    val destinationStatus: String = "Şehir, semt veya adresin ilk 3 harfini yaz.",
    val searchingOrigin: Boolean = false,
    val searchingDestination: Boolean = false,
)

/** UI only reads StateFlow, no network calls within a Composable. */
class PlaceAutocompleteViewModel @JvmOverloads constructor(
    application: Application,
    private val source: PlaceSuggestionSource = PublicPlaceSuggestionSource(
        photon = PhotonGeocoder {
            val gps = (application as? CockpitApplication)?.locationRepository?.state?.value
            gps?.point?.takeIf {
                gps.isFresh(SystemClock.elapsedRealtime()) &&
                    (gps.accuracyMeters ?: 999f) <= 100f
            }
        },
    ),
) : AndroidViewModel(application) {
    // Available instantly and without network, even if the fair-use geocoder is throttled.
    private val offline = runCatching {
        application.assets.open("tr_province_district_centres.txt")
            .bufferedReader(Charsets.UTF_8)
            .use { OfflineTurkishPlaces.fromDelimited(it.readText()) }
    }.getOrElse { OfflineTurkishPlaces.empty() }

    private val mutable = MutableStateFlow(PlaceSuggestionState())
    val state = mutable.asStateFlow()
    private var originJob: Job? = null
    private var destinationJob: Job? = null

    fun onTextChanged(field: PlaceField, raw: String) {
        val text = raw.take(AutocompletePolicy.MAX_LENGTH)
        val local = offline.suggest(text)
        val onlineEligible = AutocompletePolicy.eligible(text)
        val status = when {
            local.isNotEmpty() && onlineEligible ->
                "İl/ilçe önerileri hazır • tam adres internetten aranıyor."
            local.isNotEmpty() ->
                "Çevrim dışı il/ilçe önerileri • adres için 3 veya daha fazla harf."
            onlineEligible -> "İnternet üzerinden adres önerileri aranacak…"
            else -> "İl/ilçe için en az 2, adres için en az 3 harf yaz."
        }
        if (field == PlaceField.ORIGIN) {
            originJob?.cancel()
            mutable.value = mutable.value.copy(
                originText = text, originResults = local, searchingOrigin = false,
                originStatus = status,
            )
        } else {
            destinationJob?.cancel()
            mutable.value = mutable.value.copy(
                destinationText = text, destinationResults = local,
                searchingDestination = false, destinationStatus = status,
            )
        }
        // Never wait for the network to display an already known province/district.
        if (!onlineEligible) return
        val job = viewModelScope.launch {
            delay(AutocompletePolicy.DEBOUNCE_MS)
            markLoading(field)
            try {
                val online = source.suggest(text)
                val combined = (online + local).distinctBy {
                    it.label + "|" + it.point.latitude + "|" + it.point.longitude
                }.take(10)
                setResults(field, combined, when {
                    online.isNotEmpty() -> "Adres / şehir önerileri • seçmeden önce haritada doğrula."
                    local.isNotEmpty() -> "İl/ilçe önerileri • çevrim içi adres yanıtı yok."
                    else -> "Öneri bulunamadı veya ücretsiz servis sınırlı. Tekrar dene."
                })
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                setResults(field, local, if (local.isNotEmpty())
                    "İl/ilçe önerileri kullanılabilir • tam adres çevrim dışı."
                else "Adres önerileri alınamadı. İnternet bağlantısını kontrol et.")
            }
        }
        if (field == PlaceField.ORIGIN) originJob = job else destinationJob = job
    }

    fun selected(field: PlaceField, place: PhotonPlace) {
        if (field == PlaceField.ORIGIN) {
            originJob?.cancel()
            mutable.value = mutable.value.copy(
                originText = place.label, originResults = emptyList(), searchingOrigin = false,
                originStatus = "Başlangıç seçildi • haritada doğrula.",
            )
        } else {
            destinationJob?.cancel()
            mutable.value = mutable.value.copy(
                destinationText = place.label, destinationResults = emptyList(),
                searchingDestination = false,
                destinationStatus = "Hedef seçildi • haritada doğrula.",
            )
        }
    }

    fun useGpsOrigin() {
        originJob?.cancel()
        mutable.value = mutable.value.copy(
            originText = "", originResults = emptyList(), searchingOrigin = false,
            originStatus = "Başlangıç: GPS (gerçek konum yoksa rota hesaplanmaz)",
        )
    }

    private fun markLoading(field: PlaceField) {
        mutable.value = if (field == PlaceField.ORIGIN)
            mutable.value.copy(searchingOrigin = true)
        else mutable.value.copy(searchingDestination = true)
    }

    private fun setResults(field: PlaceField, places: List<PhotonPlace>, status: String) {
        mutable.value = if (field == PlaceField.ORIGIN)
            mutable.value.copy(originResults = places, originStatus = status,
                searchingOrigin = false)
        else mutable.value.copy(destinationResults = places, destinationStatus = status,
            searchingDestination = false)
    }
}
```

---

## 4. `app/src/main/java/app/evcockpit/navigation/PhotonGeocoder.kt`

```kotlin
package app.evcockpit.navigation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class PhotonPlace(val label: String, val point: GeoPoint)

/**
 * Photon'un genel örnek servisi sınırsız değildir; her harfte ağ isteği gönderilmez.
 * Başlangıçtaki öneriler çevrim dışı il/ilçe kaydından anında gelir.
 */
object PhotonPolicy {
    const val MIN_INTERVAL_MS = 10_000L
    fun canRequest(query: String, elapsedSinceLastMs: Long): Boolean =
        query.trim().length >= 3 && query.length <= 120 &&
            elapsedSinceLastMs >= MIN_INTERVAL_MS

    /** Koordinat yanlılığı sadece sıralamayı etkiler, Türkiye dışını engellemez. */
    fun requestUrl(query: String, currentPosition: GeoPoint?): String {
        require(query.trim().length in 3..120)
        val bias = currentPosition?.takeIf {
            it.latitude in 35.0..43.0 && it.longitude in 25.0..46.0
        } ?: GeoPoint(39.0, 35.0)
        val lat = String.format(Locale.US, "%.5f", bias.latitude)
        val lon = String.format(Locale.US, "%.5f", bias.longitude)
        return "https://photon.komoot.io/api/?q=" +
            URLEncoder.encode(query.trim(), "UTF-8") +
            "&limit=8&lang=tr&lat=$lat&lon=$lon&zoom=6&location_bias_scale=0.4"
    }
}

object PhotonParser {
    fun parse(raw: String): List<PhotonPlace> {
        val features = JSONObject(raw).getJSONArray("features")
        return (0 until minOf(features.length(), 8)).mapNotNull { i ->
            runCatching {
                val feature = features.getJSONObject(i)
                val props = feature.getJSONObject("properties")
                val coords = feature.getJSONObject("geometry").getJSONArray("coordinates")
                val point = GeoPoint(coords.getDouble(1), coords.getDouble(0))
                val streetNumber = listOf(props.optString("street"), props.optString("housenumber"))
                    .filter { it.isNotBlank() && it != "null" }.joinToString(" ")
                val label = listOf(
                    props.optString("name"), streetNumber,
                    props.optString("district"), props.optString("city"),
                    props.optString("state"), props.optString("country"),
                ).filter { it.isNotBlank() && it != "null" }.distinct()
                    .joinToString(", ").take(180)
                if (label.isBlank()) null else PhotonPlace(label, point)
            }.getOrNull()
        }
    }
}

class PhotonGeocoder(
    private val focusPoint: () -> GeoPoint? = { null },
) {
    private var lastRequestAtMs = -PhotonPolicy.MIN_INTERVAL_MS

    suspend fun search(rawQuery: String): List<PhotonPlace> = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        val now = android.os.SystemClock.elapsedRealtime()
        synchronized(this@PhotonGeocoder) {
            if (!PhotonPolicy.canRequest(query, now - lastRequestAtMs)) {
                throw IllegalArgumentException("Photon hız sınırı: ağdan en erken 10 saniyede bir sorgu.")
            }
            lastRequestAtMs = now
        }
        val connection = URL(PhotonPolicy.requestUrl(query, focusPoint())).openConnection()
            as HttpURLConnection
        connection.connectTimeout = 6_000
        connection.readTimeout = 6_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent",
            "EV-Cockpit-v2-Personal/0.4 (https://github.com/AlpTimucin-cpu/ev-cockpit-v2)")
        try {
            if (connection.responseCode != 200) {
                throw IllegalStateException("Photon adres servisine ulaşılamadı veya kota doldu.")
            }
            val raw = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                app.evcockpit.planner.data.BoundedResponseReader.read(reader, 96_000)
            }
            PhotonParser.parse(raw)
        } finally {
            connection.disconnect()
        }
    }
}
```

---

## 5. `app/src/main/java/app/evcockpit/navigation/OsrmRouteProvider.kt`

```kotlin
package app.evcockpit.navigation

import app.evcockpit.planner.data.BoundedResponseReader
import app.evcockpit.planner.route.RouteDistance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

/**
 * FOSSGIS'in OSRM örnek servisi: gerçek yol ağı ve gerçek alternatifler.
 * Ücretsiz örnek servis için saniyede en fazla bir istek, kullanıcı tetiklemesi,
 * açık kaynak atfı ve otomatik yeniden denemesiz çalışma zorunludur.
 * Bu servis üretim SLA'sı veya garantili 3 alternatif sağlamaz.
 */
class OsrmRouteProvider : RouteProvider {
    override suspend fun calculate(
        start: GeoPoint,
        destination: GeoPoint,
        userApiKey: String,
    ): NavigationRoute = calculateAlternatives(start, destination, userApiKey).first()

    override suspend fun calculateAlternatives(
        start: GeoPoint,
        destination: GeoPoint,
        userApiKey: String,
    ): List<NavigationRoute> {
        require(start != destination) { "Başlangıç ve hedef aynı." }
        val url = OsrmRequest.url(start, destination)
        return OsrmRequest.oneAtATime {
            withContext(Dispatchers.IO) {
                val connection = URL(url).openConnection() as HttpsURLConnection
                connection.connectTimeout = 12_000
                connection.readTimeout = 30_000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent",
                    "EV-Cockpit-v2-Private/0.4 (https://github.com/AlpTimucin-cpu/ev-cockpit-v2)")
                try {
                    when (val http = connection.responseCode) {
                        200 -> {
                            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                                BoundedResponseReader.read(it, 3_000_000)
                            }
                            OsrmResponseParser.parse(body)
                        }
                        429 -> throw RouteServiceException(
                            "OSRM sunucusu hız sınırı uyguluyor. Bir süre sonra yeniden dene.")
                        400, 404 -> throw RouteServiceException(
                            "Bu noktalar arasında OSRM yol güzergâhı bulunamadı.")
                        in 300..399 -> throw RouteServiceException(
                            "OSRM sunucusu beklenmeyen yönlendirme yaptı.")
                        else -> throw RouteServiceException(
                            "OSRM geçici olarak kullanılamıyor (HTTP $http).")
                    }
                } finally {
                    connection.disconnect()
                }
            }
        }
    }
}

object OsrmRequest {
    private const val BASE = "https://routing.openstreetmap.de/routed-car/route/v1/driving/"
    private const val MIN_GAP_MS = 1_100L
    private val guard = Mutex()
    private var lastCallMs = -MIN_GAP_MS

    fun url(start: GeoPoint, end: GeoPoint): String {
        fun point(p: GeoPoint) = String.format(
            Locale.US, "%.6f,%.6f", p.longitude, p.latitude)
        return BASE + point(start) + ";" + point(end) +
            "?alternatives=3&steps=true&geometries=geojson&overview=full"
    }

    suspend fun <T> oneAtATime(request: suspend () -> T): T = guard.withLock {
        val now = android.os.SystemClock.elapsedRealtime()
        val wait = (MIN_GAP_MS - (now - lastCallMs)).coerceAtLeast(0)
        if (wait > 0) delay(wait)
        lastCallMs = android.os.SystemClock.elapsedRealtime()
        request()
    }
}

/** Yalnız API'den dönen yol geometrilerini kabul eder; sahte alternatif çizmez. */
object OsrmResponseParser {
    fun parse(json: String): List<NavigationRoute> {
        val root = JSONObject(json)
        require(root.optString("code") == "Ok") {
            "OSRM rota yanıtı geçersiz veya güzergâh bulunamadı."
        }
        val routes = root.optJSONArray("routes")
            ?: throw IllegalArgumentException("OSRM rota listesi eksik.")
        val parsed = (0 until minOf(routes.length(), 3)).mapNotNull { i ->
            runCatching { parseSingle(routes.getJSONObject(i)) }.getOrNull()
        }.distinctBy { it.geometry }
        require(parsed.isNotEmpty()) { "OSRM kullanılabilir yol geometrisi döndürmedi." }
        return parsed
    }

    private fun parseSingle(route: JSONObject): NavigationRoute {
        val distance = route.getDouble("distance")
        val duration = route.getDouble("duration")
        require(distance.isFinite() && distance > 0.0 &&
            duration.isFinite() && duration > 0.0) { "OSRM süre ve mesafe geçersiz." }
        val geometry = route.getJSONObject("geometry")
        require(geometry.getString("type") == "LineString")
        val coordinates = geometry.getJSONArray("coordinates")
        require(coordinates.length() in 2..40_000) { "Rota geometri boyutu geçersiz." }
        val positions = (0 until coordinates.length()).map { index ->
            val item = coordinates.getJSONArray(index)
            GeoPoint(item.getDouble(1), item.getDouble(0))
        }
        val instructions = buildList {
            val legs = route.optJSONArray("legs")
            if (legs != null) {
                for (legIndex in 0 until legs.length()) {
                    val steps = legs.getJSONObject(legIndex).optJSONArray("steps") ?: continue
                    for (stepIndex in 0 until minOf(steps.length(), 400)) {
                        val step = steps.getJSONObject(stepIndex)
                        val maneuver = step.optJSONObject("maneuver") ?: continue
                        val location = maneuver.optJSONArray("location") ?: continue
                        if (location.length() < 2) continue
                        val point = runCatching {
                            GeoPoint(location.getDouble(1), location.getDouble(0))
                        }.getOrNull() ?: continue
                        val nearest = positions.indices.minByOrNull {
                            RouteDistance.haversine(positions[it], point)
                        } ?: continue
                        val type = maneuver.optString("type")
                        val modifier = maneuver.optString("modifier")
                        val roadName = step.optString("name", "").trim()
                        val text = instruction(type, modifier, roadName)
                        if (text.isNotBlank()) {
                            add(RouteManeuver(text, nearest, type.hashCode()))
                        }
                    }
                }
            }
        }
        return NavigationRoute(
            geometry = positions,
            distanceMeters = distance,
            durationSeconds = duration,
            maneuvers = instructions,
            provider = "OSRM / FOSSGIS (trafik yok)",
        )
    }

    /** OSRM'nin manevra türlerini sürücüye okunabilir Türkçe biçime dönüştürür. */
    fun instruction(type: String, modifier: String, road: String): String {
        val direction = when (modifier) {
            "left" -> "sola"
            "slight left" -> "hafif sola"
            "sharp left" -> "keskin sola"
            "right" -> "sağa"
            "slight right" -> "hafif sağa"
            "sharp right" -> "keskin sağa"
            "uturn" -> "geriye"
            "straight" -> "düz"
            else -> ""
        }
        val action = when (type) {
            "depart" -> "Yola çıkın"
            "arrive" -> "Hedefe ulaştınız"
            "turn", "end of road", "fork", "continue", "new name" ->
                if (direction.isEmpty()) "Yola devam edin"
                else "$direction yönüne devam edin"
            "roundabout", "rotary", "roundabout turn" -> "Dönel kavşağa girin"
            "exit roundabout", "exit rotary" -> "Dönel kavşaktan çıkın"
            "merge" -> "Yola katılın"
            "on ramp" -> "Bağlantı yoluna girin"
            "off ramp" -> "Bağlantı yolundan çıkın"
            "notification" -> "Yol uyarısı"
            else -> "Yola devam edin"
        }
        return if (road.isBlank() || type == "arrive") action else "$action: ${road.take(85)}"
    }
}
```

---

## 6. `app/src/main/java/app/evcockpit/navigation/RouteAlternativePreview.kt`

```kotlin
package app.evcockpit.navigation

import app.evcockpit.planner.energy.DrivingSegment
import app.evcockpit.planner.energy.HvacMode
import app.evcockpit.planner.energy.PhysicsConsumptionModel
import app.evcockpit.planner.model.VehicleProfile

/**
 * B aşamasının şarj istasyonu bağımsız ön hesabı.
 * Gerçek güzergâh mesafesi/süresi kullanılır; hava ve yükselti D aşamasında eklenecek.
 * Tek bir düz-yol/20°C/otomatik klima yaklaşımıdır, araçtan ölçülmüş değildir.
 */
data class AlternativeEnergyPreview(
    val routeEnergyKwh: Double,
    val averageKwhPer100Km: Double,
    val startSocPct: Double?,
    val arrivalSocPct: Double?,
    val energyDeficitKwh: Double?,
    val extraKwhForMinimum: Double?,
    val requiresCharging: Boolean?,
    val chargingMinutes: Double?,
    val chargingCostTl: Double?,
    val note: String = "FİZİK ÖN TAHMİNİ • 20°C, düz yol; trafik/eğim/rüzgâr yok",
)

object RouteAlternativePreview {
    fun calculate(
        route: NavigationRoute,
        vehicle: VehicleProfile,
        startSocPct: Double?,
        minArrivalSocPct: Double = 15.0,
    ): AlternativeEnergyPreview? {
        if (vehicle.validate().isNotEmpty() ||
            !route.distanceMeters.isFinite() || route.distanceMeters <= 0.0 ||
            !route.durationSeconds.isFinite() || route.durationSeconds <= 0.0 ||
            !minArrivalSocPct.isFinite() || minArrivalSocPct !in 0.0..100.0)
            return null
        val averageSpeed = route.distanceMeters / route.durationSeconds * 3.6
        if (averageSpeed !in 5.0..160.0) return null
        val power = runCatching {
            PhysicsConsumptionModel().compute(
                DrivingSegment(
                    lengthMeters = route.distanceMeters,
                    speedKmh = averageSpeed,
                    slopeRadians = 0.0,
                    temperatureC = 20.0,
                ), vehicle, HvacMode.AUTO,
            ).kwh
        }.getOrNull() ?: return null
        if (!power.isFinite() || power < 0.0) return null
        val capacity = vehicle.capacityAtSohKwh
        if (!capacity.isFinite() || capacity <= 0.0) return null
        val soc = startSocPct?.takeIf { it.isFinite() && it in 0.0..100.0 }
        val rawArrival = soc?.minus(power / capacity * 100.0)
        val needsCharge = rawArrival?.let { it < minArrivalSocPct }
        val extra = rawArrival?.let {
            (minArrivalSocPct - it).coerceAtLeast(0.0) / 100.0 * capacity
        }
        return AlternativeEnergyPreview(
            routeEnergyKwh = power,
            averageKwhPer100Km = power * 100_000.0 / route.distanceMeters,
            startSocPct = soc,
            arrivalSocPct = rawArrival?.takeIf { it >= 0.0 },
            energyDeficitKwh = rawArrival?.takeIf { it < 0.0 }?.let {
                -it / 100.0 * capacity
            },
            extraKwhForMinimum = extra,
            requiresCharging = needsCharge,
            chargingMinutes = if (needsCharge == false) 0.0 else null,
            chargingCostTl = if (needsCharge == false) 0.0 else null,
        )
    }
}
```

---

## 7. `app/src/main/java/app/evcockpit/navigation/NavigationViewModel.kt`

```kotlin
package app.evcockpit.navigation

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.evcockpit.CockpitApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VoiceUiState(
    val enabled: Boolean = false,
    val status: String = "Sesli tarif kapalı",
)

data class RouteUiState(
    val route: NavigationRoute? = null,
    val destination: GeoPoint? = null,
    val busy: Boolean = false,
    val message: String = "Nereden – nereye noktalarını seç ve rota hesapla.",
    val origin: GeoPoint? = null,  // null means current fresh GPS
    val originLabel: String = "Konumum (GPS)",
    val destinationLabel: String = "",
    val alternatives: List<NavigationRoute> = emptyList(),
    val activeAlternativeIndex: Int = 0,
    val alternativesNote: String = "Rota seçenekleri henüz hesaplanmadı.",
)

/** No hidden or automatic network requests. The user explicitly requests routing. */
class NavigationViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CockpitApplication
    private val provider: RouteProvider = OrsRouteProvider()
    private val keys = EncryptedRouteKeyStore(app)
    private val mutable = MutableStateFlow(RouteUiState())
    val state = mutable.asStateFlow()
    private val voiceMutable = MutableStateFlow(VoiceUiState())
    val voice = voiceMutable.asStateFlow()
    private val deliveredCues = mutableSetOf<String>()
    private var speaker: TurkishNavigationSpeech? = null

    init {
        viewModelScope.launch {
            app.locationRepository.state.collect { gps ->
                if (!voiceMutable.value.enabled) return@collect
                val route = mutable.value.route ?: return@collect
                if (!gps.isFresh(SystemClock.elapsedRealtime()) ||
                    gps.point == null || (gps.accuracyMeters ?: 999f) > 100f
                ) return@collect
                val position = RouteProgress.locate(route, gps.point) ?: return@collect
                val index = position.nextManeuverIndex ?: return@collect
                val step = route.maneuvers.getOrNull(index) ?: return@collect
                val cue = VoiceAnnouncementPolicy.select(
                    index, position.distanceToManeuverMeters, step.instructionTr, deliveredCues,
                ) ?: return@collect
                if (speaker?.speak(cue.text) == true) deliveredCues.add(cue.id)
            }
        }
    }

    fun toggleVoice() {
        if (voiceMutable.value.enabled) {
            voiceMutable.value = VoiceUiState()
            speaker?.stop()
            return
        }
        if (speaker == null) {
            val newSpeaker = TurkishNavigationSpeech(app)
            speaker = newSpeaker
            viewModelScope.launch {
                newSpeaker.status.collect { message ->
                    if (voiceMutable.value.enabled) {
                        voiceMutable.update { it.copy(status = message) }
                    }
                }
            }
        }
        voiceMutable.value = VoiceUiState(
            enabled = true,
            status = speaker?.status?.value ?: "Türkçe TTS bekleniyor",
        )
    }

    fun testNextManeuver() {
        val gps = app.locationRepository.state.value
        val route = mutable.value.route
        if (route == null || gps.point == null ||
            !gps.isFresh(SystemClock.elapsedRealtime()) ||
            (gps.accuracyMeters ?: 999f) > 100f
        ) {
            voiceMutable.update { it.copy(status = "Gerçek GPS/rota yok; sesli manevra okunamıyor") }
            return
        }
        val pos = RouteProgress.locate(route, gps.point)
        val step = pos?.nextManeuverIndex?.let { route.maneuvers.getOrNull(it) }
        if (step == null) {
            voiceMutable.update { it.copy(status = "Sıradaki manevra belirlenemedi") }
            return
        }
        if (speaker == null) {
            voiceMutable.update { it.copy(status = "Önce sesli tarifi aç") }
            return
        }
        val metres = pos.distanceToManeuverMeters?.toInt()?.coerceAtLeast(0)
        speaker?.speak(
            if (metres == null) step.instructionTr else
                "$metres metre sonra ${step.instructionTr}",
        )
    }

    override fun onCleared() {
        speaker?.shutdown()
        super.onCleared()
    }

    /**
     * Rota için kullanıcıdan enlem/boylam istenmez.
     * Hedef, arama önerisi / kayıtlı favori / haritadan seçimle belirlenir.
     */
    fun calculateSelected() {
        if (mutable.value.busy) return
        val previous = mutable.value
        val destination = previous.destination ?: run {
            mutable.value = previous.copy(message = "Önce Nereye alanından bir hedef seç.")
            return
        }
        val gps = app.locationRepository.state.value
        val gpsValid = gps.point != null &&
            gps.isFresh(SystemClock.elapsedRealtime()) &&
            (gps.accuracyMeters == null || gps.accuracyMeters <= 100f)
        val start = previous.origin ?: gps.point.takeIf { gpsValid }
        if (start == null) {
            mutable.value = previous.copy(
                route = null, alternatives = emptyList(),
                message = "Başlangıç için bir yer seç veya kesin GPS konumunu aç.")
            return
        }
        if (start == destination) {
            mutable.value = previous.copy(message = "Başlangıç ve hedef aynı; başka yer seç.")
            return
        }

        deliveredCues.clear()
        speaker?.stop()
        mutable.value = previous.copy(
            route = null, alternatives = emptyList(),
            busy = true, message = "OSRM yol ağı ve alternatifler sorgulanıyor…")
        viewModelScope.launch {
            try {
                var usingFallback = false
                val options = try {
                    OsrmRouteProvider().calculateAlternatives(start, destination, "")
                } catch (cancel: kotlinx.coroutines.CancellationException) {
                    throw cancel
                } catch (osrmError: Exception) {
                    // Kullanıcı kendi ORS anahtarını kaydetmişse eski servise geçilir.
                    // ORS'nin uzun yol alternatif sınırı olduğundan uydurma seçenek eklenmez.
                    val key = runCatching { keys.read() }.getOrNull()
                    if (key.isNullOrBlank()) throw RouteServiceException(
                        "Ücretsiz OSRM rotası alınamadı. Bağlantıyı kontrol edip tekrar dene. " +
                        "İstersen Ayarlar'a kendi ORS anahtarını ekle.")
                    val fallback = provider.calculateAlternatives(start, destination, key)
                    usingFallback = true
                    fallback
                }
                require(options.isNotEmpty()) { "Sağlayıcı rota döndürmedi." }
                val note = if (usingFallback)
                    "OSRM kullanılamadı: ORS yedek rota kullanıldı. " +
                    "Uzun yolda tek rota dönebilir; canlı trafik yok."
                else if (options.size == 1)
                    "OSRM tek gerçek güzergâh döndürdü; 2-3 alternatif garanti değil."
                else "OSRM'den ${options.size} gerçek alternatif rota alındı • trafik yok."
                mutable.value = mutable.value.copy(
                    route = options.first(), alternatives = options,
                    activeAlternativeIndex = 0, alternativesNote = note,
                    busy = false, message = note)
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                mutable.value = mutable.value.copy(busy = false,
                    message = "Rota isteği iptal edildi.")
                throw cancel
            } catch (error: RouteServiceException) {
                mutable.value = mutable.value.copy(
                    route = null, alternatives = emptyList(), busy = false,
                    message = error.message ?: "Rota servisi kullanılamıyor.")
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(
                    route = null, alternatives = emptyList(), busy = false,
                    message = "Rota alınamadı. İnternet veya harita sağlayıcısı kullanılamıyor.")
            }
        }
    }

    fun selectOrigin(point: GeoPoint, name: String) {
        if (mutable.value.busy) return
        deliveredCues.clear()
        speaker?.stop()
        mutable.value = mutable.value.copy(
            origin = point, originLabel = name.take(140),
            route = null, alternatives = emptyList(), activeAlternativeIndex = 0,
            message = "Başlangıç seçildi: ${name.take(80)}. Hedef seçip hesapla.")
    }

    fun useGpsOrigin() {
        if (mutable.value.busy) return
        deliveredCues.clear()
        speaker?.stop()
        mutable.value = mutable.value.copy(
            origin = null, originLabel = "Konumum (GPS)",
            route = null, alternatives = emptyList(), activeAlternativeIndex = 0,
            message = "Başlangıç GPS'e ayarlandı; GPS kapalıysa rota hesaplanamaz.")
    }

    fun clearDestinationSelection() {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(
            destination = null, destinationLabel = "",
            route = null, alternatives = emptyList(), activeAlternativeIndex = 0,
            message = "Hedef için adres önerilerinden bir yer seç.",
        )
    }

    fun selectNamedDestination(point: GeoPoint, name: String) {
        selectDestination(point)
        mutable.value = mutable.value.copy(destinationLabel = name.take(140))
    }

    fun selectAlternative(index: Int) {
        val options = mutable.value.alternatives
        if (mutable.value.busy || index !in options.indices) return
        deliveredCues.clear()
        speaker?.stop()
        mutable.value = mutable.value.copy(
            route = options[index], activeAlternativeIndex = index,
            message = "Güzergâh ${index + 1} seçildi • enerji tahmini hesaplanabilir, canlı trafik yok.")
    }

    /** Explicit user action: fetch actual waypoint route; no auto-OBD recalculation. */
    fun navigateVia(stops:List<GeoPoint>,destination:GeoPoint) {
        if(mutable.value.busy)return
        if(stops.size>48) {
            mutable.value=RouteUiState(destination=destination,
                message="En çok 48 şarj ara noktası kullanılabilir.")
            return
        }
        val gps=app.locationRepository.state.value
        if(gps.point==null || !gps.isFresh(SystemClock.elapsedRealtime()) ||
            (gps.accuracyMeters?:999f)>100f) {
            mutable.value=RouteUiState(destination=destination,
                message="Önce Ayarlar'dan GPS'i başlat; taze konum olmadan navigasyon yok.")
            return
        }
        deliveredCues.clear()
        speaker?.stop()
        mutable.value=RouteUiState(destination=destination,busy=true,
            message="Duraklı rota açık kullanıcı isteğiyle hesaplanıyor...")
        viewModelScope.launch {
            try {
                val key=keys.read()?:throw RouteServiceException(
                    "HeiGIT anahtarı yok. Ayarlar > API anahtarı bölümüne git.")
                val origin=app.locationRepository.state.value.point
                    ?:throw RouteServiceException("GPS konumu kayboldu; yeniden dene.")
                val via=listOf(origin)+stops+listOf(destination)
                val calculated=provider.calculateVia(via,key)
                mutable.value=RouteUiState(route=calculated,destination=destination,
                    message="Şarj ara duraklı ORS rota yüklendi — TAHMİN, sahada DOĞRULANMADI.")
            } catch(error:RouteServiceException) {
                mutable.value=RouteUiState(destination=destination,
                    message=error.message?:"Rota sağlayıcı hatası.")
            } catch(_:Exception) {
                mutable.value=RouteUiState(destination=destination,
                    message="Duraklı rota alınamadı; internet, GPS, kota ve anahtarı kontrol et.")
            }
        }
    }

    fun selectDestination(point: GeoPoint) {
        if (mutable.value.busy) return
        deliveredCues.clear()
        speaker?.stop()
        mutable.value = mutable.value.copy(
            route = null, destination = point, destinationLabel = "Haritadan seçilen nokta",
            alternatives = emptyList(), activeAlternativeIndex = 0,
            message = "Haritada hedef seçildi. Rota sekmesinde Rota hesapla düğmesine bas.",
        )
    }

    fun clear() {
        deliveredCues.clear()
        speaker?.stop()
        mutable.value = RouteUiState(message = "Rota temizlendi.")
    }
}
```

---

## 8. `app/src/main/java/app/evcockpit/ui/RouteScreen.kt`

```kotlin
package app.evcockpit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.DestinationsStore
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.PhotonPlace
import app.evcockpit.navigation.PlaceAutocompleteViewModel
import app.evcockpit.navigation.PlaceField
import app.evcockpit.navigation.RouteAlternativePreview
import app.evcockpit.navigation.RouteProgress
import app.evcockpit.navigation.RouteUiState
import app.evcockpit.navigation.SavedDestination
import app.evcockpit.navigation.VoiceUiState
import app.evcockpit.planner.model.VehicleProfile
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * B aşaması: adres seçimi -> gerçek OSRM alternatifleri -> enerji/SoC karşılaştırması.
 * Enlem/boylam kullanıcı alanı değildir; seçimler öneriden veya haritadan yapılır.
 */
@Composable
fun RouteScreen(
    state: RouteUiState,
    gps: GpsSnapshot,
    socPct: Double?,
    socSource: String,
    vehicleProfile: VehicleProfile,
    voice: VoiceUiState,
    onToggleVoice: () -> Unit,
    onSpeakNextManeuver: () -> Unit,
    onCalculate: () -> Unit,
    onSelectDestination: (GeoPoint) -> Unit,
    onSelectNamedDestination: (GeoPoint, String) -> Unit,
    onSelectOrigin: (GeoPoint, String) -> Unit,
    onUseGpsOrigin: () -> Unit,
    onClearDestinationSelection: () -> Unit,
    onSelectAlternative: (Int) -> Unit,
    onManualSoc: (Double) -> Unit,
    searchVm: PlaceAutocompleteViewModel,
    routeWeather: String,
    onClear: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { DestinationsStore(context.applicationContext) }
    val saved by store.entries.collectAsState(initial = emptyList())
    val suggestions by searchVm.state.collectAsState()
    val scope = rememberCoroutineScope()
    var manualSocText by remember { mutableStateOf("") }
    var minArrivalText by remember { mutableStateOf("15") }
    var inputError by remember { mutableStateOf<String?>(null) }
    val minimumArrival = minArrivalText.trim().replace(',', '.').toDoubleOrNull()
        ?.takeIf { it in 0.0..100.0 } ?: 15.0

    val position = state.route?.takeIf { rememberGpsFreshness(gps) && gps.point != null }
        ?.let { route -> gps.point?.let { RouteProgress.locate(route, it) } }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("EV ROTA PLANLAYICI", style = MaterialTheme.typography.titleLarge)
        Text("OSRM gerçek yol ağı • Türkiye öncelikli Photon • trafik verisi yok",
            style = MaterialTheme.typography.bodySmall)
        Text(state.message, style = MaterialTheme.typography.bodyMedium)
        Text(routeWeather, style = MaterialTheme.typography.bodySmall)

        Text("NEREDEN", style = MaterialTheme.typography.titleMedium)
        Text("Seçili başlangıç: ${state.originLabel}")
        OutlinedTextField(
            value = suggestions.originText,
            onValueChange = {
                onUseGpsOrigin()
                searchVm.onTextChanged(PlaceField.ORIGIN, it)
            },
            label = { Text("Şehir, ilçe, cadde veya yer adı") },
            placeholder = { Text("Örn. Niğde, Bor veya Kayseri Otogar") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (suggestions.searchingOrigin) Text("Adres kaynakları sorgulanıyor…")
        else Text(suggestions.originStatus)
        suggestions.originResults.forEach { place ->
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    searchVm.selected(PlaceField.ORIGIN, place)
                    onSelectOrigin(place.point, place.label)
                },
            ) { Text(place.label) }
        }
        OutlinedButton(onClick = {
            searchVm.useGpsOrigin()
            onUseGpsOrigin()
        }) { Text("Başlangıç: konumum (GPS)") }

        Text("NEREYE", style = MaterialTheme.typography.titleMedium)
        Text("Seçili hedef: ${state.destinationLabel.ifBlank { "Henüz seçilmedi" }}")
        OutlinedTextField(
            value = suggestions.destinationText,
            onValueChange = {
                onClearDestinationSelection()
                searchVm.onTextChanged(PlaceField.DESTINATION, it)
            },
            label = { Text("Şehir, otel, işletme veya açık adres") },
            placeholder = { Text("Örn. Antalya, Konyaaltı") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (suggestions.searchingDestination) Text("Adres kaynakları sorgulanıyor…")
        else Text(suggestions.destinationStatus)
        suggestions.destinationResults.forEach { place ->
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    searchVm.selected(PlaceField.DESTINATION, place)
                    onSelectNamedDestination(place.point, place.label)
                    scope.launch {
                        store.save(SavedDestination("RECENT", place.label, place.point))
                    }
                },
            ) { Text(place.label) }
        }
        Text("İl/ilçe merkezleri 2 harften itibaren çevrim dışı gösterilir. " +
            "Tam adres için 250 ms yazma beklemesi vardır; Photon genel sunucusuna " +
            "en fazla 10 saniyede bir istek gönderilir. Öneriyi haritada doğrula.",
            style = MaterialTheme.typography.bodySmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = state.destination != null, onClick = {
                state.destination?.let { point ->
                    scope.launch { store.save(SavedDestination("HOME", "Ev", point)) }
                }
            }) { Text("Ev olarak kaydet") }
            OutlinedButton(enabled = state.destination != null, onClick = {
                state.destination?.let { point ->
                    scope.launch { store.save(SavedDestination("WORK", "İş", point)) }
                }
            }) { Text("İş olarak kaydet") }
        }
        saved.forEach { item ->
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    searchVm.selected(PlaceField.DESTINATION, PhotonPlace(item.label, item.point))
                    onSelectNamedDestination(item.point, item.label)
                },
            ) {
                Text((if (item.type == "HOME") "Ev" else if (item.type == "WORK") "İş"
                    else "Son arama") + ": " + item.label)
            }
        }

        Text("BATARYA VE HEDEF REZERVİ", style = MaterialTheme.typography.titleMedium)
        Text("Araç: ID.4 Pure • kullanılabilir kapasite: " +
            fmt(vehicleProfile.capacityAtSohKwh) + " kWh")
        Text("Başlangıç SoC: ${socPct?.let { fmt(it) + " %" } ?: "bilinmiyor"} • kaynak: $socSource")
        OutlinedTextField(
            value = manualSocText,
            onValueChange = { manualSocText = it.take(6) },
            label = { Text("Manuel başlangıç SoC (%) — OBD gerekmez") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(onClick = {
            val soc = manualSocText.replace(',', '.').toDoubleOrNull()
            if (soc == null || soc !in 0.0..100.0) {
                inputError = "SoC için 0 ile 100 arasında sayı gir."
            } else {
                onManualSoc(soc)
                manualSocText = ""
                inputError = null
            }
        }) { Text("Başlangıç SoC kaydet") }
        OutlinedTextField(
            value = minArrivalText,
            onValueChange = { minArrivalText = it.take(6) },
            label = { Text("Minimum varış SoC (%)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        inputError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = state.destination != null && !state.busy, onClick = {
                onCalculate()
                state.destination?.let { dest ->
                    scope.launch {
                        store.save(SavedDestination("RECENT",
                            state.destinationLabel.ifBlank { "Son hedef" }, dest))
                    }
                }
            }) { Text(if (state.busy) "Rotalar hesaplanıyor…" else "Rotaları karşılaştır") }
            OutlinedButton(onClick = onClear) { Text("Temizle") }
        }

        if (state.route != null) {
            Text("HARİTADAKİ GERÇEK ALTERNATİFLER",
                style = MaterialTheme.typography.titleMedium)
            Text(state.alternativesNote)
            OnlineMap(
                modifier = Modifier.fillMaxWidth().height(310.dp),
                route = state.route,
                destination = state.destination,
                gps = gps,
                onSelectDestination = onSelectDestination,
                alternativeRoutes = state.alternatives,
                canChangeLayer = true,
            )
            state.alternatives.forEachIndexed { index, candidate ->
                val selected = index == state.activeAlternativeIndex
                val preview = RouteAlternativePreview.calculate(
                    candidate, vehicleProfile, socPct, minimumArrival)
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text((if (selected) "✓ SEÇİLİ • " else "") + "Rota ${index + 1}",
                            style = MaterialTheme.typography.titleMedium)
                        Text("Mesafe: ${fmt(candidate.distanceMeters / 1000.0)} km" +
                            "  •  Süre: ${time(candidate.durationSeconds)} (trafiksiz)")
                        Text("Enerji: ${preview?.let { fmt(it.routeEnergyKwh) + " kWh" } ?: "—"}" +
                            "  •  Ort. tüketim: " +
                            (preview?.let { fmt(it.averageKwhPer100Km) + " kWh/100 km" } ?: "—"))
                        val arrival = when {
                            preview == null || preview.startSocPct == null -> "Başlangıç SoC bilinmiyor"
                            preview.arrivalSocPct == null -> "Şarj olmadan varış mümkün görünmüyor"
                            else -> "${fmt(preview.arrivalSocPct)} %"
                        }
                        Text("Tahmini varış SoC: $arrival")
                        val charge = when (preview?.requiresCharging) {
                            false -> "Ön modele göre durak gerekmiyor"
                            true -> "Şarj gerekli • gereken ek enerji en az " +
                                "${fmt(preview?.extraKwhForMinimum ?: 0.0)} kWh"
                            null -> "SoC bilinmiyor; durak planı hesaplanamaz"
                        }
                        Text("Şarj: $charge")
                        Text(if (preview?.requiresCharging == false)
                            "Yol üzerinde şarj: 0 dk • 0 TL (önden tahmin)"
                            else "Şarj süresi: — • Şarj ücreti: fiyat/istasyon planı yok",
                            style = MaterialTheme.typography.bodySmall)
                        Button(
                            enabled = !state.busy && !selected,
                            onClick = { onSelectAlternative(index) },
                        ) { Text(if (selected) "Seçili rota" else "Bu rotayı seç") }
                    }
                }
            }
            Text("Enerji: 20°C, düz yol, ortalama hız ve otomatik klima ile ÖN TAHMİN. " +
                "Yükselti, rüzgâr, şarj istasyonu ve tarifeler D/C/E aşamalarında eklenir. " +
                "Yetersiz SoC hiçbir zaman gerçek varış garantisi değildir.",
                style = MaterialTheme.typography.bodySmall)
            Text("GPS: " + (position?.let {
                "Kalan mesafe ~${fmt(it.remainingMeters / 1000.0)} km"
            } ?: "Rota üzerinde doğrulanmış güncel konum yok"))
            Text("© OpenStreetMap katkıcıları • OSRM/FOSSGIS; canlı trafik yok",
                style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Başlangıç ve hedef seçildikten sonra gerçek alternatifler haritada " +
                "karşılaştırılır. Sunucu yalnız tek yol döndürürse diğer yollar uydurulmaz.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onToggleVoice) {
                Text(if (voice.enabled) "Sesli tarifi kapat" else "Türkçe sesli tarifi aç")
            }
            OutlinedButton(onClick = onSpeakNextManeuver, enabled = voice.enabled) {
                Text("Sıradaki manevrayı oku")
            }
        }
        Text("Sesli tarif: ${voice.status}", style = MaterialTheme.typography.bodySmall)
    }
}

private fun fmt(value: Double): String =
    String.format(Locale.forLanguageTag("tr-TR"), "%.1f", value)

private fun time(seconds: Double): String {
    val minutes = (seconds / 60.0).toInt().coerceAtLeast(0)
    return if (minutes < 60) "$minutes dk"
    else "${minutes / 60} sa ${minutes % 60} dk"
}
```

---

## 9. `app/src/main/java/app/evcockpit/ui/OnlineMap.kt`

```kotlin
package app.evcockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.awaitCancellation
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import kotlinx.coroutines.launch
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.annotations.IconFactory
import app.evcockpit.planner.charging.ChargerCandidate
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import kotlin.math.abs
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.navigation.RouteProgress
import app.evcockpit.ui.tesla.RoutePolylineProgress

/**
 * MapLibre Native + OpenFreeMap public online tiles. No offline tile download.
 * Initial camera shows Türkiye; it is NOT a claimed GPS location.
 * Provider availability / phone rendering have not yet been device-verified.
 */
@Composable
fun OnlineMap(
    modifier: Modifier = Modifier,
    route: NavigationRoute?,
    destination: GeoPoint?,
    gps: GpsSnapshot,
    weatherSummary: String = "",
    onSelectDestination: (GeoPoint) -> Unit,
    stopMarkers: List<ChargerCandidate> = emptyList(),
    onStopSelected: (ChargerCandidate) -> Unit = {},
    focusedStop: ChargerCandidate? = null,
    onLoadFailed: () -> Unit = {},
    onStyleReady: () -> Unit = {},
    canChangeLayer: Boolean = true,
    alternativeRoutes: List<NavigationRoute> = emptyList(),
) {
    val context = LocalContext.current
    val layerPreferences = remember(context) { MapLayerPreferences(context) }
    val layer by layerPreferences.selected.collectAsState(initial = MapLayer.DARK)
    val mapTilerStore = remember(context) { MapTilerKeyStore(context) }
    val mapTilerKey by mapTilerStore.key.collectAsState(initial = null)
    val styleUrl = MapStyleResolver.url(layer, mapTilerKey)
        ?: MapLayer.DARK.styleUrl
    val chosenStyleUrl = rememberUpdatedState(styleUrl)
    val scope = rememberCoroutineScope()
    val mapStyleApplied = remember { mutableStateOf("") }
    var followCamera by remember { mutableStateOf(false) }
    var tiltedCamera by remember { mutableStateOf(false) }
    var northUp by remember { mutableStateOf(false) }
    val layerMenu = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapReady = remember { mutableStateOf<MapLibreMap?>(null) }
    val chosenDestination = rememberUpdatedState(onSelectDestination)
    val chosenStop = rememberUpdatedState(onStopSelected)
    val displayedStops = rememberUpdatedState(stopMarkers)
    val mapFailureCallback = rememberUpdatedState(onLoadFailed)
    val mapReadyCallback = rememberUpdatedState(onStyleReady)
    val mapView = remember(context) {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            addOnDidFailLoadingMapListener(
                MapView.OnDidFailLoadingMapListener { _ ->
                    mapFailureCallback.value()
                },
            )
            getMapAsync { map ->
                map.addOnMapLongClickListener { location ->
                    chosenDestination.value(GeoPoint(location.latitude, location.longitude))
                    true
                }
                map.setOnMarkerClickListener { marker ->
                    val found = displayedStops.value.firstOrNull {
                        abs(it.latitude - marker.position.latitude) < 0.00001 &&
                        abs(it.longitude - marker.position.longitude) < 0.00001
                    }
                    if(found!=null) { chosenStop.value(found);true } else false
                }
                mapStyleApplied.value = chosenStyleUrl.value
                map.setStyle(chosenStyleUrl.value) {
                    mapReady.value = map
                    mapReadyCallback.value()
                }
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(39.0, 35.0), 5.2))
            }
        }
    }

    DisposableEffect(mapView, lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        var destroyed = false
        fun destroyOnce() {
            if (!destroyed) {
                destroyed = true
                mapView.onDestroy()
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> destroyOnce()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            mapReady.value = null
            if (!destroyed) {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
                destroyOnce()
            }
        }
    }

    val loadedMap = mapReady.value
    LaunchedEffect(styleUrl) {
        val map = mapReady.value
        if (map != null && styleUrl != mapStyleApplied.value) {
            mapReady.value = null
            map.setStyle(styleUrl) {
                mapStyleApplied.value = styleUrl
                mapReady.value = map
                mapReadyCallback.value()
            }
        }
    }
    LaunchedEffect(loadedMap, focusedStop) {
        if(loadedMap!=null && focusedStop!=null) {
            followCamera = false
            loadedMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(focusedStop.latitude,focusedStop.longitude),12.0))
        }
    }
    val gpsFresh = rememberGpsFreshness(gps)
    // Takip kapalıyken de 2B/3B değişimi mevcut harita merkezinde uygulanır.
    LaunchedEffect(loadedMap, tiltedCamera, northUp) {
        val map = loadedMap ?: return@LaunchedEffect
        if (followCamera) return@LaunchedEffect
        val current = map.cameraPosition
        val camera = CameraPosition.Builder()
            .target(current.target)
            .zoom(current.zoom)
            .bearing(if (northUp) 0.0 else current.bearing)
            .tilt(if (tiltedCamera) 45.0 else 0.0)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(camera))
    }
    // Takip isteğe bağlıdır; GPS güncel değilse kamera sahte konuma gitmez.
    // GNSS course hareket yönüdür; pusula okumak gibi yorumlanmaz.
    LaunchedEffect(loadedMap, gps.point, gpsFresh, gps.courseDegrees,
        followCamera, tiltedCamera, northUp) {
        val map = loadedMap ?: return@LaunchedEffect
        val point = gps.point ?: return@LaunchedEffect
        if (!followCamera || !gpsFresh) return@LaunchedEffect
        val camera = CameraPosition.Builder()
            .target(LatLng(point.latitude, point.longitude))
            .zoom(15.0)
            .bearing(if (northUp) 0.0 else gps.courseDegrees ?: 0.0)
            .tilt(if (tiltedCamera) 45.0 else 0.0)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(camera))
    }
    // Rota değiştiğinde kamera sadece GERÇEK seçilen yol geometrisini çerçeveler.
    LaunchedEffect(loadedMap, route) {
        val map = loadedMap ?: return@LaunchedEffect
        val path = route?.geometry?.takeIf { it.size >= 2 } ?: return@LaunchedEffect
        if (followCamera) return@LaunchedEffect
        runCatching {
            val bounds = LatLngBounds.Builder()
            val step = (path.size / 1_000).coerceAtLeast(1)
            path.filterIndexed { index, _ -> index % step == 0 || index == path.lastIndex }
                .forEach { point -> bounds.include(LatLng(point.latitude, point.longitude)) }
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 72))
        }
    }
    LaunchedEffect(loadedMap, route, alternativeRoutes, destination, stopMarkers,
        gps.point, gpsFresh) {
        val currentMap = loadedMap ?: return@LaunchedEffect
        var currentLine: Polyline? = null
        var passedLine: Polyline? = null
        var currentMarker: Marker? = null
        val chargingMarkers = mutableListOf<Marker>()
        val secondaryLines = mutableListOf<Polyline>()
        try {
            // Alternatifler soluk; yalnız seçilen güzergâh canlı yeşildir.
            alternativeRoutes.filter { it != route }.take(2).forEach { alternative ->
                if (alternative.geometry.size >= 2) {
                    val step = (alternative.geometry.size / 2_500).coerceAtLeast(1)
                    val faint = alternative.geometry.filterIndexed { index, _ ->
                        index % step == 0 || index == alternative.geometry.lastIndex
                    }
                    secondaryLines += currentMap.addPolyline(
                        PolylineOptions()
                            .addAll(faint.map { LatLng(it.latitude, it.longitude) })
                            .color(android.graphics.Color.rgb(111, 129, 146))
                            .width(5f),
                    )
                }
            }
            if (route != null) {
                val position = if (gpsFresh && gps.point != null)
                    RouteProgress.locate(route, gps.point) else null
                val progress = if (position != null && route.distanceMeters > 0.0)
                    ((route.distanceMeters - position.remainingMeters) / route.distanceMeters)
                        .coerceIn(0.0, 1.0) else 0.0
                val parts = RoutePolylineProgress.split(route.geometry, progress)
                if (parts.passed.size >= 2) {
                    passedLine = currentMap.addPolyline(
                        PolylineOptions()
                            .addAll(parts.passed.map { LatLng(it.latitude, it.longitude) })
                            .color(android.graphics.Color.rgb(100, 110, 117))
                            .width(8f),
                    )
                }
                if (parts.remaining.size >= 2) {
                    currentLine = currentMap.addPolyline(
                        PolylineOptions()
                            .addAll(parts.remaining.map { LatLng(it.latitude, it.longitude) })
                            .color(android.graphics.Color.rgb(61, 220, 132))
                            .width(8f),
                    )
                }
            }
            if (destination != null) {
                currentMarker = currentMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(destination.latitude, destination.longitude))
                        .title("Seçilen hedef"),
                )
            }
            stopMarkers.forEach { station ->
                val color=PlannerMarkerPolicy.color(station)
                val bitmap=Bitmap.createBitmap(44,44,Bitmap.Config.ARGB_8888)
                val draw=AndroidCanvas(bitmap)
                val paint=Paint(Paint.ANTI_ALIAS_FLAG)
                paint.color=android.graphics.Color.WHITE
                draw.drawCircle(22f,22f,21f,paint)
                paint.color=color
                draw.drawCircle(22f,22f,17f,paint)
                val icon=IconFactory.getInstance(context).fromBitmap(bitmap)
                chargingMarkers+=currentMap.addMarker(MarkerOptions()
                    .position(LatLng(station.latitude,station.longitude))
                    .title(PlannerMarkerPolicy.label(station).take(120))
                    .snippet("doluluk bilinmiyor • veri TAHMİN").icon(icon))
            }
            awaitCancellation()
        } finally {
            currentLine?.let { currentMap.removePolyline(it) }
            passedLine?.let { currentMap.removePolyline(it) }
            currentMarker?.let { currentMap.removeMarker(it) }
            chargingMarkers.forEach { currentMap.removeMarker(it) }
            secondaryLines.forEach { currentMap.removePolyline(it) }
        }
    }

    val position = if (route != null && gpsFresh &&
        gps.point != null
    ) RouteProgress.locate(route, gps.point) else null
    val maneuver = position?.nextManeuverIndex?.let { route?.maneuvers?.getOrNull(it) }
    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.align(Alignment.TopStart)) {
            OutlinedButton(onClick = { layerMenu.value = true }, enabled = canChangeLayer) {
                Text("Katman: " + layer.titleTr, fontSize = 14.sp, color = Color.White)
            }
            DropdownMenu(expanded = layerMenu.value,
                onDismissRequest = { layerMenu.value = false }) {
                MapLayer.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.titleTr + if (option == layer) " ✓" else "") },
                        enabled = MapStyleResolver.url(option, mapTilerKey) != null,
                        onClick = {
                            layerMenu.value = false
                            scope.launch { layerPreferences.choose(option) }
                        },
                    )
                }
            }
        }
        if (canChangeLayer) {
            Column(
                modifier = Modifier.align(Alignment.CenterEnd)
                    .background(Color(0x99000000)).padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                OutlinedButton(
                    enabled = gpsFresh && gps.point != null,
                    onClick = { followCamera = !followCamera },
                ) { Text(if (followCamera) "Takibi kapat" else "Konumumu izle") }
                OutlinedButton(onClick = { tiltedCamera = !tiltedCamera }) {
                    Text(if (tiltedCamera) "2B görünüm" else "3B açı (45°)")
                }
                OutlinedButton(onClick = { northUp = !northUp }) {
                    Text(if (northUp) "Hareket yönü" else "Kuzey yukarı")
                }
                if (!gpsFresh || gps.point == null) {
                    Text("GPS güncel değil", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        Text(
            when {
                route != null -> "OpenFreeMap • rota çizgisi (sahada doğrulanmadı)"
                destination != null -> "Hedef seçildi • Rota sekmesinde hesapla"
                else -> "OpenFreeMap • hedef seçmek için haritaya uzun bas"
            },
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.TopCenter)
                .background(Color(0xC9000000)).padding(8.dp),
        )
        if (weatherSummary.isNotBlank()) {
            Text(weatherSummary, color = Color.White, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.TopEnd)
                    .background(Color(0xD9000000)).padding(6.dp))
        }
        Text(MapAttribution.LABEL, color = Color.White, fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomEnd)
                .background(Color(0xDD000000)).padding(horizontal = 7.dp, vertical = 4.dp))
        if (maneuver != null) {
            Text(
                "SONRAKİ MANEVRA • ${maneuver.instructionTr}\n" +
                    "Yaklaşık: ${position.distanceToManeuverMeters?.toInt() ?: "--"} m",
                color = Color.White, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.BottomStart)
                    .background(Color(0xD9000000)).padding(10.dp),
            )
        }
    }
}
```

---

## 10. `app/src/main/java/app/evcockpit/ui/tesla/TeslaDashboard.kt`

```kotlin
package app.evcockpit.ui.tesla

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.evcockpit.location.GpsSnapshot
import app.evcockpit.location.SpeedSelector
import app.evcockpit.navigation.GeoPoint
import app.evcockpit.navigation.NavigationRoute
import app.evcockpit.navigation.RouteProgress
import app.evcockpit.navigation.RouteUiState
import app.evcockpit.obd.SocReading
import app.evcockpit.obd.VehicleState
import app.evcockpit.planner.charging.ChargerCandidate
import app.evcockpit.planner.charging.ChargerEvidence
import app.evcockpit.planner.model.PlanResult
import app.evcockpit.ui.CockpitTheme
import app.evcockpit.ui.OnlineMap

enum class TeslaAction(val title: String, val symbol: String) {
    ROTA("Rota", "⌖"),
    PLAN("Plan", "⚡"),
    MEDYA("Medya", "♫"),
    VIDEO("Yolcu video", "▶"),
    MOLA("Mola", "M"),
    AYARLAR("Ayarlar", "⚙"),
}

object TeslaLayoutPolicy {
    fun useThreePanes(widthDp: Int, heightDp: Int): Boolean =
        widthDp >= 700 && widthDp > heightDp
}

@Composable
fun DriveModeScaffold(
    left: @Composable () -> Unit,
    center: @Composable () -> Unit,
    right: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier.fillMaxSize().background(TeslaTokens.background)
            .padding(TeslaTokens.contentPadding),
    ) {
        if (TeslaLayoutPolicy.useThreePanes(maxWidth.value.toInt(), maxHeight.value.toInt())) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.weight(TeslaTokens.leftPaneWeight)
                    .fillMaxHeight().verticalScroll(rememberScrollState())) { left() }
                Box(Modifier.weight(TeslaTokens.mapWeight).fillMaxHeight()) { center() }
                Box(Modifier.weight(TeslaTokens.actionWeight)
                    .fillMaxHeight().verticalScroll(rememberScrollState())) { right() }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.fillMaxWidth()) { left() }
                Box(Modifier.fillMaxWidth().height(420.dp)) { center() }
                Box(Modifier.fillMaxWidth()) { right() }
            }
        }
    }
}

@Composable
fun ActionRail(
    onAction: (TeslaAction) -> Unit,
    molaAvailable: Boolean = false,
    mode: DriveMode = DriveMode.UNKNOWN,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TeslaAction.entries.forEach { action ->
            val enabled = DriveModePolicy.allowedAction(action, mode, molaAvailable)
            OutlinedButton(
                onClick = { onAction(action) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
                    .heightIn(min = TeslaTokens.touchTarget)
                    .semantics {
                        contentDescription = action.title + when {
                            action == TeslaAction.MOLA && !molaAvailable ->
                                " • kurulu değilse resmî kurulum rehberi"
                            action == TeslaAction.VIDEO && mode != DriveMode.PARKED ->
                                " • sürüşte kapalı"
                            else -> ""
                        }
                    },
                shape = RoundedCornerShape(TeslaTokens.cornerRadius),
                border = BorderStroke(1.dp, TeslaTokens.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TeslaTokens.text,
                    disabledContentColor = TeslaTokens.secondaryText,
                ),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 9.dp),
            ) {
                Text(action.symbol, fontSize = 21.sp)
                Spacer(Modifier.width(5.dp))
                Text(
                    action.title,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

    }
}

@Composable
fun MapHeadlineBar(summary: MapHeadline) {
    Column(
        Modifier.fillMaxWidth().background(TeslaTokens.surface)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(summary.title, color = TeslaTokens.secondaryText,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(summary.distance, color = TeslaTokens.text,
            style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(summary.eta, color = TeslaTokens.secondaryText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f), maxLines = 2)
            Text(summary.arrivalSoc, color = TeslaTokens.estimate,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f), maxLines = 2)
        }
    }
}

@Composable
fun NextStopBar(stop: app.evcockpit.planner.model.PlannedStop, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(TeslaTokens.cornerRadius),
        color = TeslaTokens.surfaceRaised,
        modifier = Modifier.fillMaxWidth().heightIn(min = TeslaTokens.touchTarget),
        border = BorderStroke(1.dp, TeslaTokens.outline),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("PLANLANAN ŞARJ DURAĞI • DETAY",
                color = TeslaTokens.secondaryText,
                style = MaterialTheme.typography.labelMedium)
            Text(stop.charger.name, color = TeslaTokens.text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "~" + stop.charger.powerKw.value.toInt() + " kW • hedef %" +
                    stop.departureSocPct + " • şarj ~" +
                    stop.chargeMinutes.toInt() + " dk • TAHMİN • doluluk bilinmiyor",
                color = TeslaTokens.secondaryText, style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
        }
    }
}

@Composable
fun MapHero(
    route: NavigationRoute?,
    destination: GeoPoint?,
    gps: GpsSnapshot,
    gpsFresh: Boolean,
    plan: PlanResult.Success?,
    weatherSummary: String,
    onSelectDestination: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier,
    previewMap: Boolean = false,
    allowRetry: Boolean = true,
    alternatives: List<NavigationRoute> = emptyList(),
) {
    val position = if (gpsFresh && route != null && gps.point != null) {
        RouteProgress.locate(route, gps.point)
    } else null
    val headline = MapHeadlinePolicy.create(
        route, destination != null, position, plan, System.currentTimeMillis(),
    )
    val next = MapHeadlinePolicy.nextStop(plan, route, position)
    var selectedStation by remember { mutableStateOf<ChargerCandidate?>(null) }
    var mapFailed by remember { mutableStateOf(false) }
    var mapRetry by remember { mutableIntStateOf(0) }
    Column(
        modifier.clip(RoundedCornerShape(TeslaTokens.cornerRadius))
            .background(TeslaTokens.surface),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MapHeadlineBar(headline)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (previewMap) {
                Box(
                    Modifier.fillMaxSize().background(TeslaTokens.surfaceRaised),
                    contentAlignment = Alignment.Center,
                ) { Text("HARİTA ÖNİZLEME • DEMO", color = TeslaTokens.secondaryText) }
            } else {
                key(mapRetry) {
                    OnlineMap(
                        modifier = Modifier.fillMaxSize(),
                        route = route,
                        destination = destination,
                        gps = gps,
                        weatherSummary = weatherSummary,
                        onSelectDestination = onSelectDestination,
                        stopMarkers = plan?.stops?.map { it.charger } ?: emptyList(),
                        onStopSelected = { selectedStation = it },
                        onLoadFailed = { mapFailed = true },
                        onStyleReady = { mapFailed = false },
                        canChangeLayer = allowRetry,
                        alternativeRoutes = alternatives,
                    )
                }
                if (mapFailed) {
                    Column(
                        Modifier.align(Alignment.Center)
                            .background(TeslaTokens.surface)
                            .padding(TeslaTokens.contentPadding),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Harita yüklenemedi",
                            color = TeslaTokens.error,
                            style = MaterialTheme.typography.labelLarge)
                        Text("Bağlantını kontrol et. Çevrimiçi değilken harita açılamayabilir.",
                            color = TeslaTokens.text,
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(
                            enabled = allowRetry,
                            onClick = {
                                mapFailed = false
                                mapRetry++
                            },
                            modifier = Modifier.heightIn(min = TeslaTokens.touchTarget),
                        ) { Text("Haritayı yeniden dene") }
                    }
                }
            }
        }
        if (next != null) NextStopBar(next, onClick = {
            selectedStation = next.charger
        })
    }
    selectedStation?.let { charger ->
        AlertDialog(
            onDismissRequest = { selectedStation = null },
            title = { Text("Şarj durağı • TAHMİN") },
            text = {
                Text(ChargerEvidence.details(charger).joinToString("\n"),
                    style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                TextButton(onClick = { selectedStation = null }) { Text("Kapat") }
            },
        )
    }
}

@Composable
fun TeslaDashboardScreen(
    s: VehicleState,
    soc: SocReading,
    usableKwhAtSoh: Double,
    gps: GpsSnapshot,
    gpsFresh: Boolean,
    navigation: RouteUiState,
    plan: PlanResult.Success?,
    driveMode: DriveModeSnapshot,
    weatherSummary: String,
    onSelectDestination: (GeoPoint) -> Unit,
    molaInstalled: Boolean,
    guidance: DashboardGuidance = DashboardGuidance.NONE,
    onGuidanceAction: (GuidanceAction) -> Unit = {},
    onAction: (TeslaAction) -> Unit,
) {
    val speed = SpeedSelector.select(
        obdConnected = s.connected && driveMode.obdSpeedFresh, demo = s.demo, obdKmh = s.speedKmh,
        gpsKmh = gps.speedKmh, gpsFresh = gpsFresh,
    )
    val panel = DashboardPresentation.vehicle(s, soc, speed, usableKwhAtSoh,
        powerFresh = driveMode.obdPowerFresh)
    DriveModeScaffold(
        left = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(driveMode.label, color = if (driveMode.mode == DriveMode.DRIVING)
                    TeslaTokens.warning else TeslaTokens.secondaryText,
                    style = MaterialTheme.typography.labelMedium)
                VehicleStatePanel(panel, Modifier.fillMaxWidth(),
                    compact = driveMode.mode != DriveMode.DRIVING)
            }
        },
        center = {
            MapHero(navigation.route, navigation.destination, gps, gpsFresh,
                plan, weatherSummary, onSelectDestination, Modifier.fillMaxSize(),
                allowRetry = driveMode.mode != DriveMode.DRIVING,
                alternatives = navigation.alternatives)
        },
        right = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionRail(onAction, molaAvailable = molaInstalled,
                    mode = driveMode.mode,
                    modifier = Modifier.fillMaxWidth())
                TeslaGuidanceCard(
                    guidance, onGuidanceAction,
                    enabled = driveMode.mode != DriveMode.DRIVING,
                )
            }
        },
    )
}

@Preview(name = "Tesla üçlü düzen - DEMO", widthDp = 1000, heightDp = 540,
    showBackground = true)
@Composable
private fun ThreePanePreview() {
    CockpitTheme(dark = true) {
        DriveModeScaffold(
            left = {
                VehicleStatePanel(
                    VehiclePanelUiState(speedKmh = 74.0, socPercent = 60.0,
                        source = SourceKind.DEMO, status = PanelStatus.NORMAL,
                        message = "ÖNİZLEME / DEMO"),
                    compact = true,
                )
            },
            center = {
                MapHero(null, null, GpsSnapshot(), false, null, "",
                    {}, Modifier.fillMaxSize(), previewMap = true)
            },
            right = { ActionRail(onAction = {}) },
        )
    }
}

@Preview(name = "Tesla dikey düzen - DEMO", widthDp = 400, heightDp = 850,
    showBackground = true)
@Composable
private fun VerticalDashboardPreview() {
    CockpitTheme(dark = true) {
        DriveModeScaffold(
            left = { VehicleStatePanel(VehiclePanelUiState(), compact = true) },
            center = {
                MapHero(null, null, GpsSnapshot(), false, null, "",
                    {}, Modifier.fillMaxSize(), previewMap = true)
            },
            right = { ActionRail(onAction = {}) },
        )
    }
}
```

---

## 11. `app/src/main/java/app/evcockpit/MainActivity.kt`

```kotlin
package app.evcockpit

import dagger.hilt.android.AndroidEntryPoint
import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.evcockpit.obd.ObdLogShare
import app.evcockpit.obd.SafeTestReportExporter
import app.evcockpit.obd.ObdViewModel
import app.evcockpit.obd.ManualSocStore
import app.evcockpit.navigation.NavigationViewModel
import app.evcockpit.navigation.PlaceAutocompleteViewModel
import app.evcockpit.weather.RouteHourlyWeatherViewModel
import app.evcockpit.passenger.PassengerPlayerModel
import app.evcockpit.location.SpeedSelector
import app.evcockpit.integration.MolaOpenContract
import kotlinx.coroutines.flow.MutableStateFlow
import app.evcockpit.ui.PassengerVideoScreen
import app.evcockpit.ui.RouteScreen
import app.evcockpit.ui.PlannerScreen
import app.evcockpit.planui.PlannerViewModel
import app.evcockpit.ui.tesla.TeslaDashboardScreen
import app.evcockpit.ui.tesla.TeslaAction
import app.evcockpit.ui.tesla.DriveMode
import app.evcockpit.ui.tesla.DriveModeViewModel
import app.evcockpit.ui.tesla.DriveModePolicy
import app.evcockpit.integration.MolaLinkViewModel
import app.evcockpit.integration.MolaStatus
import app.evcockpit.ui.tesla.MolaLaunchCard
import app.evcockpit.ui.tesla.DashboardGuidancePolicy
import app.evcockpit.ui.tesla.GuidanceAction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.evcockpit.ui.tesla.SafeSocSelection
import app.evcockpit.storage.VehicleProfileStore
import app.evcockpit.planner.model.VehicleProfile
import app.evcockpit.planner.model.PlanResult
import app.evcockpit.ui.ObdTestScreen
import app.evcockpit.ui.RouteSettingsScreen
import app.evcockpit.ui.CockpitTheme
import app.evcockpit.ui.CockpitTabs
import app.evcockpit.ui.DisplayPreferences
import kotlinx.coroutines.launch

private data class ExternalOpenRequest(val generation: Int = 0, val tab: Int? = null)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val externalOpenRequest = MutableStateFlow(ExternalOpenRequest())

    private fun acceptOpenIntent(value: Intent?) {
        if (value?.action != Intent.ACTION_VIEW) return
        val target = MolaOpenContract.tabFor(value.dataString) ?: return
        val previous = externalOpenRequest.value
        externalOpenRequest.value = ExternalOpenRequest(previous.generation + 1, target)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptOpenIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acceptOpenIntent(intent)
        setContent {
            val openRequest by externalOpenRequest.collectAsState()
            val displayPreferences = remember { DisplayPreferences(applicationContext) }
            val darkTheme by displayPreferences.darkTheme.collectAsState(
                initial = true,
            )
            val keepScreenOn by displayPreferences.keepScreenOn.collectAsState(
                initial = false,
            )
            DisposableEffect(keepScreenOn) {
                if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                onDispose { /* next effect handles updates; Activity teardown clears the flag */ }
            }
            CockpitTheme(dark = darkTheme) {
                val obdViewModel: ObdViewModel = viewModel()
                val navViewModel: NavigationViewModel = viewModel()
                val placeSearchModel: PlaceAutocompleteViewModel = viewModel()
                val routeWeatherModel: RouteHourlyWeatherViewModel = viewModel()
                val passengerModel: PassengerPlayerModel = viewModel()
                val plannerModel: PlannerViewModel = viewModel()
                val driveModel: DriveModeViewModel = viewModel()
                val molaModel: MolaLinkViewModel = viewModel()
                App(obdViewModel, navViewModel, placeSearchModel, routeWeatherModel, passengerModel, plannerModel,
                    driveModel, molaModel, displayPreferences, darkTheme, keepScreenOn, openRequest)
            }
        }
    }
}

@Composable
private fun App(vm: ObdViewModel, navVm: NavigationViewModel,
    placeSearchModel: PlaceAutocompleteViewModel,
    routeWeatherModel: RouteHourlyWeatherViewModel,
    passengerModel: PassengerPlayerModel, plannerModel: PlannerViewModel,
    driveModel: DriveModeViewModel,
    molaModel: MolaLinkViewModel,
    displayPreferences: DisplayPreferences,
    darkTheme: Boolean, keepScreenOn: Boolean,
    openRequest: ExternalOpenRequest) {
    val context = LocalContext.current
    val molaState by molaModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, molaModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) molaModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
            molaModel.refresh()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val vehicleProfileStore = remember(context) { VehicleProfileStore(context.applicationContext) }
    val vehicleProfile by vehicleProfileStore.profile.collectAsStateWithLifecycle(
        initialValue = VehicleProfile.defaults())
    val planUi by plannerModel.state.collectAsStateWithLifecycle()
    val manualStore = remember(context) { ManualSocStore(context.applicationContext) }
    val manualSoc by manualStore.socPct.collectAsStateWithLifecycle(initialValue = null)
    val saveScope = rememberCoroutineScope()
    val state by vm.state.collectAsStateWithLifecycle()
    val drive by driveModel.state.collectAsStateWithLifecycle()
    val socReading = SafeSocSelection.select(state, manualSoc, drive)
    val logs by vm.log.collectAsStateWithLifecycle()
    val logPath by vm.logFilePath.collectAsStateWithLifecycle()
    val diagnosisPath by vm.diagnosisFilePath.collectAsStateWithLifecycle()
    val diagnosisSummary by vm.diagnosisSummary.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val gps by vm.gps.collectAsStateWithLifecycle()
    val gpsFresh = drive.gpsFresh
    val navState by navVm.state.collectAsStateWithLifecycle()
    val weatherSummary by routeWeatherModel.summary.collectAsStateWithLifecycle()
    LaunchedEffect(navState.route) {
        // Only ViewModel/repository makes the network call, not this Composable.
        routeWeatherModel.refresh(navState.route)
    }
    val voiceState by navVm.voice.collectAsStateWithLifecycle()
    var shareError by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    var showMolaCard by remember { mutableStateOf(false) }
    LaunchedEffect(drive.mode) {
        if (drive.mode != DriveMode.PARKED) showMolaCard = false
    }
    LaunchedEffect(openRequest.generation) {
        openRequest.tab?.let { requested ->
            if (DriveModePolicy.allowedTab(requested, drive.mode)) tab = requested
        }
    }
    LaunchedEffect(drive.mode, tab) {
        val mustClose = (drive.mode == DriveMode.DRIVING && tab in setOf(3,4,5)) ||
            (drive.mode != DriveMode.PARKED && tab == 4)
        if (mustClose) {
            passengerModel.pause()
            tab = 0
        }
    }

    val permissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { vm.refreshBonded() }
    LaunchedEffect(vm) { launcher.launch(permissions) }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) vm.startGps() else vm.denyGps()
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)) {
        if (tab != 0) CockpitTabs(selected = tab, onSelect = { selected ->
            if (DriveModePolicy.allowedTab(selected, drive.mode)) tab = selected
        })
        when (tab) {
            0 -> TeslaDashboardScreen(
                s = state,
                soc = socReading,
                usableKwhAtSoh = vehicleProfile.capacityAtSohKwh,
                gps = gps,
                gpsFresh = gpsFresh,
                navigation = navState,
                plan = (planUi.plan as? PlanResult.Success).takeIf {
                    planUi.route == navState.route
                },
                driveMode = drive,
                weatherSummary = weatherSummary,
                onSelectDestination = navVm::selectDestination,
                molaInstalled = molaState.status == MolaStatus.READY ||
                    molaState.status == MolaStatus.CANNOT_LAUNCH,
                guidance = DashboardGuidancePolicy.evaluate(
                    state, socReading, gps, gpsFresh, navState,
                    planUi.plan.takeIf { planUi.route == navState.route }),
                onGuidanceAction = { action ->
                    if (drive.mode != DriveMode.DRIVING) when (action) {
                        GuidanceAction.NONE -> Unit
                        GuidanceAction.SETTINGS -> tab = 5
                        GuidanceAction.LOCATION_PERMISSION -> gpsPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION))
                        GuidanceAction.OBD_TEST -> tab = 1
                        GuidanceAction.ROUTE -> tab = 2
                        GuidanceAction.PLAN -> tab = 3
                    }
                },
                onAction = { action ->
                    when (action) {
                        TeslaAction.ROTA -> tab = 2
                        TeslaAction.PLAN -> tab = 3
                        TeslaAction.MEDYA, TeslaAction.VIDEO -> tab = 4
                        TeslaAction.MOLA -> {
                            if (drive.mode == DriveMode.PARKED) {
                                molaModel.refresh()
                                showMolaCard = true
                            }
                        }
                        TeslaAction.AYARLAR -> tab = 5
                    }
                },
            )
            1 -> ObdTestScreen(
                state = state,
                log = logs,
                logFilePath = logPath,
                diagnosisFilePath = diagnosisPath,
                diagnosisSummary = diagnosisSummary,
                devices = devices,
                message = shareError ?: message,
                onRefresh = vm::refreshBonded,
                onConnect = vm::connect,
                onDiagnose = vm::diagnose,
                onExportTestReport = { carScannerNote ->
                    shareError = null
                    val lastLogs = logs.takeLast(200)
                    val lastSummary = diagnosisSummary
                    saveScope.launch {
                        try {
                            val path = withContext(Dispatchers.IO) {
                                SafeTestReportExporter.create(context, lastSummary, lastLogs, carScannerNote)
                            }
                            ObdLogShare.launch(context, path)
                        } catch (error: Exception) {
                            shareError = "Test raporu dışa aktarılamadı: " +
                                error.javaClass.simpleName
                        }
                    }
                },
                onShareDiagnosis = {
                    shareError = null
                    try {
                        if (!ObdLogShare.launch(context, diagnosisPath)) {
                            shareError = "Tanı logu bulunamadı."
                        }
                    } catch (error: Exception) {
                        shareError = "Tanı logu paylaşılamadı: " +
                            error.javaClass.simpleName
                    }
                },
                onDemo = vm::startDemo,
                onStop = vm::stop,
                onShareLog = {
                    shareError = null
                    try {
                        if (!ObdLogShare.launch(context, logPath)) {
                            shareError = "Paylaşılacak kayıt henüz oluşturulmadı."
                        }
                    } catch (error: Exception) {
                        shareError = "Kayıt paylaşılamadı: ${error.message ?: "Bilinmeyen hata"}"
                    }
                },
                onOpenBluetoothSettings = {
                    shareError = null
                    try {
                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    } catch (error: Exception) {
                        shareError = "Bluetooth ayarları açılamadı: ${error.message ?: "Bilinmeyen hata"}"
                    }
                },
            )
            2 -> RouteScreen(
                state = navState,
                gps = gps,
                socPct = socReading.pct,
                socSource = socReading.source,
                vehicleProfile = vehicleProfile,
                voice = voiceState,
                onToggleVoice = navVm::toggleVoice,
                onSpeakNextManeuver = navVm::testNextManeuver,
                onCalculate = navVm::calculateSelected,
                onSelectDestination = navVm::selectDestination,
                onSelectNamedDestination = navVm::selectNamedDestination,
                onSelectOrigin = navVm::selectOrigin,
                onUseGpsOrigin = navVm::useGpsOrigin,
                onClearDestinationSelection = navVm::clearDestinationSelection,
                onSelectAlternative = navVm::selectAlternative,
                onManualSoc = { value -> saveScope.launch { manualStore.save(value) } },
                searchVm = placeSearchModel,
                routeWeather = weatherSummary,
                onClear = navVm::clear,
            )
            3 -> PlannerScreen(
                vm = plannerModel, navigation = navState.route, soc = socReading,
                onOpenRoute = { tab = 2 }, onOpenSettings = { tab = 5 },
                onNavigate = { stops, destination ->
                    navVm.navigateVia(stops,destination)
                    tab = 2
                },
                gps = gps,
                onSelectDestination = navVm::selectDestination,
            )
            4 -> PassengerVideoScreen(
                vm = passengerModel,
                gps = gps,
                route = navState.route,
                destination = navState.destination,
                speedKmh = SpeedSelector.select(
                    obdConnected = state.connected && drive.obdSpeedFresh, demo = state.demo,
                    obdKmh = state.speedKmh, gpsKmh = gps.speedKmh,
                    gpsFresh = gpsFresh).kmh,
                onSelectDestination = navVm::selectDestination,
            )
            else -> RouteSettingsScreen(
                gps = gps,
                darkTheme = darkTheme,
                keepScreenOn = keepScreenOn,
                onDarkTheme = { value -> saveScope.launch { displayPreferences.setDarkTheme(value) } },
                onKeepScreenOn = { value -> saveScope.launch { displayPreferences.setKeepScreenOn(value) } },
                manualSocPct = manualSoc,
                onSaveManualSoc = { value -> saveScope.launch { manualStore.save(value) } },
                onClearManualSoc = { saveScope.launch { manualStore.clear() } },
                onStartGps = {
                    gpsPermissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                },
                onStopGps = vm::stopGps,
            )
        }
    }
    if (showMolaCard && drive.mode == DriveMode.PARKED) {
        MolaLaunchCard(
            state = molaState,
            onOpen = molaModel::openMola,
            onInstallGuide = molaModel::openInstallGuide,
            onAppSettings = molaModel::openSettings,
            onRefresh = molaModel::refresh,
            onDismiss = { showMolaCard = false },
        )
    }

}
```

---

## 12. `app/build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "app.evcockpit"
    compileSdk = 36
    defaultConfig {
        applicationId = "app.evcockpit"
        minSdk = 28
        targetSdk = 35
        versionCode = 45
        versionName = "0.4.0-B-test"
        val ciSha = (System.getenv("GITHUB_SHA") ?: "unknown")
            .take(40).replace(Regex("[^0-9a-f]"), "")
        buildConfigField("String", "CI_COMMIT", "\"$ciSha\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    // Public TEST certificate only. Never use it for production/release signing.
    System.getenv("EV_COCKPIT_PUBLIC_DEBUG_P12")?.takeIf { it.isNotBlank() }?.let { keyPath ->
        val shared = signingConfigs.create("persistentDebug") {
            storeFile = file(keyPath)
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        buildTypes.getByName("debug").signingConfig = shared
    }
}

dependencies {
    // Hilt, uygulama ve servisler arasında aynı konum deposunu paylaşır.
    implementation("com.google.dagger:hilt-android:2.57.1")
    kapt("com.google.dagger:hilt-compiler:2.57.1")
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.maplibre.gl:android-sdk:11.13.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.media3:media3-exoplayer:1.10.0")
    implementation("androidx.media3:media3-ui:1.10.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
```

---

## 13. `app/src/test/java/app/evcockpit/navigation/OfflineTurkishPlacesTest.kt`

```kotlin
package app.evcockpit.navigation

import org.junit.Assert.*
import org.junit.Test

class OfflineTurkishPlacesTest {
    private val sample = OfflineTurkishPlaces.fromDelimited(
        """
        # il|ilce|lat|lon
        Niğde|Niğde|37.969849|34.676449
        Kayseri|Melikgazi|38.731000|35.498000
        Kayseri|Kocasinan|38.745000|35.474000
        Antalya|Muratpaşa|36.879000|30.700000
        Isparta|Isparta|37.764000|30.557000
        missing|broken|nope|35
        """.trimIndent(),
    )

    @Test fun twoLettersProduceInstantOfflineSuggestions() {
        val list = sample.suggest("Ka")
        assertEquals(2, list.size)
        assertTrue(list.all { it.label.contains("Kayseri") })
        assertEquals(38.745, list.first { it.label.startsWith("Kocasinan") }.point.latitude, 0.0001)
    }

    @Test fun asciiAndTurkishLetterFormsMatchTheSameCity() {
        val a = sample.suggest("Nigde")
        val b = sample.suggest("Niğ")
        assertEquals(1, a.size)
        assertEquals(a, b)
        assertEquals(1, sample.suggest("ISPA").size)
        assertEquals(1, sample.suggest("ant").size)
    }

    @Test fun noInventedStreetOrInvalidCoordinates() {
        assertTrue(sample.suggest("istanbul sokak").isEmpty())
        assertTrue(sample.suggest("k").isEmpty())
        assertTrue(sample.suggest("ka", 0).isEmpty())
        assertTrue(sample.suggest("ka", 2).size <= 2)
        assertTrue(sample.suggest("no match").isEmpty())
    }

    @Test fun emptyOrMalformedInputNeverCreatesFakePlaces() {
        assertTrue(OfflineTurkishPlaces.fromDelimited("not|four").suggest("no").isEmpty())
        assertTrue(OfflineTurkishPlaces.empty().suggest("kayseri").isEmpty())
    }
}
```

---

## 14. `app/src/test/java/app/evcockpit/navigation/OsrmRouteProviderTest.kt`

```kotlin
package app.evcockpit.navigation

import org.junit.Assert.*
import org.junit.Test

class OsrmRouteProviderTest {
    @Test fun osrmUrlKoordinatlariBoylamEnlemSirasiIleYazar() {
        val url = OsrmRequest.url(GeoPoint(37.9698, 34.6764), GeoPoint(36.8969, 30.7133))
        assertTrue(url.startsWith("https://routing.openstreetmap.de/routed-car/"))
        assertTrue(url.contains("34.676400,37.969800;30.713300,36.896900"))
        assertTrue(url.contains("alternatives=3"))
        assertTrue(url.contains("steps=true"))
    }

    @Test fun gercekGeoJsonAlternatifleriAyrisiyorVeSecilebilir() {
        val raw = """{
          "code":"Ok",
          "routes":[
            {"distance":12000.0,"duration":720.0,
             "geometry":{"type":"LineString","coordinates":[[34.6764,37.9698],[34.7000,37.9500],[34.8000,37.9000]]},
             "legs":[{"steps":[{"name":"Ana Yol","maneuver":{"type":"depart","modifier":"straight","location":[34.6764,37.9698]}}]}]},
            {"distance":14000.0,"duration":850.0,
             "geometry":{"type":"LineString","coordinates":[[34.6764,37.9698],[34.6800,37.9400],[34.8000,37.9000]]},
             "legs":[{"steps":[]}]}
          ]}"""
        val actual = OsrmResponseParser.parse(raw)
        assertEquals(2, actual.size)
        assertEquals(12_000.0, actual[0].distanceMeters, 0.001)
        assertEquals(14_000.0, actual[1].distanceMeters, 0.001)
        assertEquals(37.95, actual[0].geometry[1].latitude, 0.0001)
        assertTrue(actual[0].maneuvers.first().instructionTr.contains("Yola çıkın"))
        assertEquals(0, actual[0].maneuvers.first().geometryIndex)
        assertTrue(actual.all { it.provider.contains("OSRM") })
    }

    @Test fun tekGercekRotaGeldiğindeSahteAlternatifYok() {
        val raw = """{"code":"Ok","routes":[{"distance":120.0,"duration":20.0,
          "geometry":{"type":"LineString","coordinates":[[34.5,38.0],[34.6,38.1]]},"legs":[]}]}"""
        assertEquals(1, OsrmResponseParser.parse(raw).size)
        assertThrows(IllegalArgumentException::class.java) {
            OsrmResponseParser.parse("""{"code":"NoRoute","routes":[]}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            OsrmResponseParser.parse("""{"code":"Ok","routes":[]}""")
        }
    }

    @Test fun turkceManevraVeFotonTurkiyeOnceligi() {
        assertEquals("Yola devam edin", OsrmResponseParser.instruction("continue", "", ""))
        assertEquals("sola yönüne devam edin", OsrmResponseParser.instruction("turn", "left", ""))
        val query = PhotonPolicy.requestUrl("Niğde otogar", GeoPoint(38.1, 34.7))
        assertTrue(query.contains("lang=tr"))
        assertTrue(query.contains("lat=38.10000"))
        assertTrue(query.contains("lon=34.70000"))
        assertTrue(query.contains("zoom=6"))
        assertFalse(query.contains("nominatim"))
        assertTrue(PhotonPolicy.requestUrl("Kay", null).contains("lat=39.00000"))
    }
}
```

---

## 15. `app/src/test/java/app/evcockpit/navigation/RouteAlternativePreviewTest.kt`

```kotlin
package app.evcockpit.navigation

import app.evcockpit.planner.model.VehicleProfile
import org.junit.Assert.*
import org.junit.Test

class RouteAlternativePreviewTest {
    private val car = VehicleProfile.defaults()
    private fun route(km: Double, hours: Double): NavigationRoute =
        NavigationRoute(
            geometry = listOf(GeoPoint(37.9, 34.7), GeoPoint(36.9, 30.7)),
            distanceMeters = km * 1000.0, durationSeconds = hours * 3600.0,
            maneuvers = emptyList(), provider = "TEST",
        )

    @Test fun enerjiKisaRotadaPozitifVeObdOlmadanHesaplanir() {
        val result = RouteAlternativePreview.calculate(route(20.0, 0.4), car, null)
        assertNotNull(result)
        assertTrue(result!!.routeEnergyKwh > 0.0)
        assertNull(result.arrivalSocPct)
        assertNull(result.chargingCostTl)
    }

    @Test fun uzunRotadaEksikEnerjiSifirGibiYazilmaz() {
        val result = RouteAlternativePreview.calculate(route(600.0, 7.0), car, 50.0, 15.0)!!
        assertTrue(result.requiresCharging == true)
        assertNull(result.arrivalSocPct)
        assertTrue(result.energyDeficitKwh!! > 0)
        assertTrue(result.extraKwhForMinimum!! > 0)
        assertNull(result.chargingMinutes)
        assertNull(result.chargingCostTl)
    }

    @Test fun kisaRotadaAraSarjGerekmiyorsaSifirYolUcreti() {
        val result = RouteAlternativePreview.calculate(route(8.0, 0.2), car, 80.0, 10.0)!!
        assertEquals(false, result.requiresCharging)
        assertEquals(0.0, result.chargingMinutes!!, 0.001)
        assertEquals(0.0, result.chargingCostTl!!, 0.001)
        assertTrue(result.arrivalSocPct!! in 10.0..80.0)
    }

    @Test fun gecersizParametrelerUydurmaSonucOlusturmaz() {
        assertNull(RouteAlternativePreview.calculate(route(0.0, 1.0), car, 70.0))
        assertNull(RouteAlternativePreview.calculate(route(20.0, 0.0), car, 70.0))
        assertNull(RouteAlternativePreview.calculate(route(20.0, 0.4), car, 70.0, 110.0))
        assertNull(RouteAlternativePreview.calculate(route(20.0, 0.4), car, 70.0, -1.0))
    }
}
```

---

## 16. `NOTICE`

```text
# NOTICE — third-party attribution and licensing (2026-11-20)

This repository's own original source is offered under MIT (see LICENSE).
No Mola or Fermata source was added or copied in this repository's recorded
source tree. This is a source-tree observation, NOT a complete software
composition analysis of every transitive binary in the APK.

## Runtime SDK/code dependencies (direct dependencies, verified upstream)
* MapLibre Native Android: BSD-2-Clause core; Android platform also carries
  third-party licenses. https://github.com/maplibre/maplibre-native/blob/main/LICENSE.md
  Full Android license disclosures:
  https://github.com/maplibre/maplibre-native/blob/main/platform/android/LICENSE.md
* AndroidX Activity, Lifecycle, Compose, Core, DataStore, Media3 (if installed
  later): Apache License 2.0; see https://source.android.com/docs/setup/about/licenses
  and https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt
* Kotlin and kotlinx.coroutines: Apache License 2.0.
  https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt
  https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt
* JUnit 4 (test only): Eclipse Public License 1.0.
  https://github.com/junit-team/junit4/blob/main/LICENSE-junit.txt
* org.json Java test fixture: check upstream Maven POM license metadata;
  not packaged as runtime dependency.

## Online services and map data (not bundled code)
* © OpenStreetMap contributors — OpenStreetMap geodata distributed under ODbL:
  https://www.openstreetmap.org/copyright
* OpenFreeMap tile/style service: https://openfreemap.org/tos/
  Visible runtime notice: "© OpenStreetMap katkıcıları / OpenFreeMap".
* openrouteservice by HeiGIT: use provider terms and OSM attribution:
  https://giscience.github.io/openrouteservice/frequently-asked-questions.html

GPL-3.0 Mola/Fermata application source is not imported into this project.
This does NOT imply all transitive licenses are MIT, that all transitive
dependencies were fully audited, or that proprietary source code can be copied.

* Photon by komoot: OSM tabanlı kamusal demo geocoding, fair-use limits, explicit search only. https://github.com/komoot/photon

* Jetpack Media3 (Apache License 2.0): https://developer.android.com/jetpack/androidx/releases/media3 ; https://github.com/androidx/media/blob/release/LICENSE

* Open-Meteo forecast data: © Open-Meteo; data CC BY 4.0 attribution, free hosted API subject to non-commercial use and rate limits: https://open-meteo.com/en/terms

## M8 çekirdek veri kaynakları (kopyalanmış ABRP kodu yok)
- ORS resmi hosted API: https://giscience.github.io/openrouteservice/hosted-api-restrictions ; kullanıcı-anahtarlı rota/waypoint.
- Open-Meteo Elevation: https://open-meteo.com/en/docs/elevation-api ; Copernicus/Open-Meteo atıf.
- OSM/Overpass: https://dev.overpass-api.de/overpass-doc/en/preface/commons.html ; OSM katkıcıları, ODbL veri atfı.
- Araç profilindeki tüm tahmini parametreler verified=false, sayısal kanıt: docs/M8_DOGRULAMA.md.

## B aşaması koordinat verisi ve OSRM altyapısı
* `app/src/main/assets/tr_province_district_centres.txt`, AEKHer/il-ilce-koordinat
  kaynak verisinin uyarlanmış il/ilçe merkezleri listesidir. Telif:
  Copyright (c) 2026 Abdurrahman Ebrar Koçak; MIT Lisansı.
  https://github.com/AEKHer/il-ilce-koordinat
* Kaynak koordinatlar: © OpenStreetMap contributors, Open Database License
  (ODbL 1.0), https://www.openstreetmap.org/copyright
  Yerleşim merkezi koordinatları bina kapısı ya da yol erişim noktası garantisi vermez.
* OSRM güzergâh hizmeti: © OpenStreetMap contributors / FOSSGIS,
  https://routing.openstreetmap.de/about.html
  Harita verisi ve yol süresi trafiksizdir; genel örnek servis saniyede
  en fazla bir istek ve makul kişisel kullanım için sunulur.
* Photon arama: komoot, https://github.com/komoot/photon
  Genel örnek serviste yoğun kullanım kısıtlanabilir. Kullanıcı koordinatı,
  sadece geçerli konum izni ve güncel GPS varsa arama yanlılığı için gönderilir.
* Başka bir uygulamanın kapalı API'si, fiyat verisi ya da algoritması kopyalanmadı.
```
