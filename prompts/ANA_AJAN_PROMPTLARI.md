# Ana ajan prompt'ları

Claude Code'u repo kökünde aç (`claude`) ve aşağıdaki adımları sırayla yapıştır.
Her adımın sonunda çıktıyı gözden geçir; onaylamadan bir sonrakine geçme. Bu duraklar kayıtta "insan kontrolü" anları olacak.

---

## Adım 1 — Plan (kod yok)

```
SPEC.md ve CLAUDE.md dosyalarını oku. Henüz hiçbir kod yazma.

Bu projeyi paralel çalışacak alt ajanlara bölmek istiyorum. .claude/agents/ altında 5 alt ajan tanımı var:
product-service-agent, order-service-agent, platform-agent, pipeline-agent, docs-agent.

Bana şunları ver:
1. Ajanların paralel çalışabilmesi için ÖNCE senin yazman gereken ortak dosyalar (sözleşme, ortak ayarlar) neler?
2. Hangi ajanlar aynı anda çalışabilir, hangisi beklemeli? Bağımlılıkları kısa bir tabloyla göster.
3. SPEC'te belirsiz ya da çelişkili gördüğün noktalar.
4. Her ajan bittiğinde nasıl doğrulayacağız?

Kısa tut. Plan onaylanmadan hiçbir dosya oluşturma.
```

**Kontrol:** Plan SPEC'teki isim ve port'lara uyuyor mu? Belirsizlik listesi varsa cevapla, SPEC.md'yi güncelle.

---

## Adım 2 — Sözleşme

```
Plan onaylı. Şimdi yalnızca ortak sözleşmeyi yaz:

contracts/product-api.yaml — OpenAPI 3.1, SPEC.md'deki product-service endpoint'leri, request/response şemaları,
ProblemDetail hata şeması ve örnek değerler.

Başka dosya oluşturma. Bitince sözleşmeyi bana özetle.
```

**Kontrol:** Endpoint yolları, HTTP kodları (201, 404, 409, 400) ve alan adları SPEC ile aynı mı?

---

## Adım 3 — Paralel alt ajanlar

```
Sözleşme onaylı. Şimdi şu dört alt ajanı AYNI ANDA (paralel) başlat:

- product-service-agent
- order-service-agent
- platform-agent
- pipeline-agent

Her birine yalnızca kendi tanımındaki görevi ver. Sen kod yazma; ajanların raporlarını topla.
Hepsi bitince bana tek bir özet tablo ver: ajan, oluşturduğu dosya sayısı, doğrulama sonucu, SPEC'ten sapmalar.
```

**Kayıt notu:** Ajanların aynı anda çalıştığı an kaydın en değerli kısmı. Terminali geniş tut. Başlangıç ve bitiş saatini not et.

---

## Adım 4 — Birleştirme ve review

```
Şimdi bütünü doğrula:

1. İki serviste `./mvnw verify` çalıştır.
2. order-service'in ProductClient'ı ile contracts/product-api.yaml ve product-service controller'ı uyumlu mu? Uyumsuzluk varsa listele.
3. k8s manifest'lerindeki port, env değişkeni ve health yolları servis kodlarıyla uyumlu mu?
4. ci.yml ve Jenkinsfile'daki imaj adları k8s/overlays/dev/kustomization.yaml ile aynı mı?

Ayrıca şu üç düzeltmeyi planla:
a) APP_VERSION imaja Docker build-arg ile gömülsün; ci.yml ve Jenkinsfile kısa git SHA'yı build-arg olarak geçsin. ConfigMap'teki APP_VERSION kaldırılsın.
b) /api/info dışarıdan görünsün: Gateway'e iki HTTPRoute kuralı ekle, URLRewrite ile /info/products -> product-service /api/info, /info/orders -> order-service /api/info.
c) order-service HPA ile yönetildiği için Deployment'ında replicas alanı olmasın (Argo CD ile HPA çakışmasın).

Önce bulguları listele, düzeltmeleri ben onaylayınca yap.
```

**Kontrol:** Burada bulunan her hata canlıda anlatılacak malzeme: "AI hızlandırır ama review'u insan yapar." Hataları not et.

---

## Adım 5 — Cluster'da çalıştır

```
Servislerin Docker imajlarını build et ve şunu çalıştır:
IMAGE_SOURCE=local ./cluster/reset.sh

Hata çıkarsa önce nedenini açıkla, sonra düzelt. Sonunda:
curl http://localhost/api/products
curl -X POST http://localhost/api/orders -H 'Content-Type: application/json' -d '{"productId":1,"quantity":2}'
çıktılarını göster.
```

---

## Adım 6 — Eğitmen notları

```
Tüm kod hazır. Şimdi docs-agent alt ajanını başlat ve eğitmen notlarını yazdır.
Bitince oluşan dosyaların listesini ve docs-agent'ın kodda bulduğu sorunları göster.
```

---

## Adım 7 — Ölçüm

```
Bu oturumda üretilen dosya sayısını ve toplam satır sayısını (Java, YAML, shell, Markdown ayrı ayrı) çıkar.
Kısa bir tablo yap.
```

Toplam süreyi ekran kaydından al. Canlıda "elle yazsaydık ne sürerdi?" sorusu için kullanılacak.
