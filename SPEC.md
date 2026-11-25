# SPEC — k8s-cicd-canli demo projesi

> Bu dosya projenin tek doğruluk kaynağıdır. Ajanlar buradaki kararların dışına çıkmaz;
> çıkmaları gerekiyorsa durur ve sorar.

## 1. Amaç

Canlı eğitimde "commit'ten pod'a" yolculuğunu göstermek için küçük ama gerçekçi bir sistem:
iki Spring Boot servisi, PostgreSQL, lokal Kubernetes (kind), GitHub Actions, Jenkins ve Argo CD.
Odak iş mantığı değil, uygulamayı taşıyan boru hattıdır. İş mantığı bilerek basit tutulur.

## 2. Teknoloji kararları

| Konu | Karar |
| --- | --- |
| Dil / JDK | Java 21 (Eclipse Temurin) |
| Framework | Spring Boot 4.1.x — start.spring.io'daki güncel kararlı sürüm |
| Build | Maven + Maven Wrapper (`./mvnw`), her servis kendi `pom.xml`'i ile bağımsız |
| Veritabanı | PostgreSQL 17, tek instance, iki veritabanı: `products`, `orders` |
| Migration | Flyway |
| Servisler arası çağrı | Spring `RestClient` |
| Test | JUnit 5; repository testleri H2 ile (`test` profili). Testcontainers KULLANILMAZ (Jenkins'te Docker-in-Docker derdi olmasın) |
| Container | Multi-stage Dockerfile, çalışma imajı `eclipse-temurin:21-jre`, non-root kullanıcı |
| Registry | GHCR: `ghcr.io/<GITHUB_KULLANICI>/<servis-adı>:<git-sha-kısa>` |
| Lokal cluster | kind, cluster adı `k8s-cicd`, tek node |
| Dışarıdan erişim | Gateway API (`Gateway` + `HTTPRoute`), implementasyon: Traefik (Helm). ingress-nginx KULLANILMAZ — Mart 2026'da emekliye ayrıldı |
| Manifest yönetimi | Kustomize: `k8s/base` + `k8s/overlays/dev` |
| CI | GitHub Actions (`ci.yml`, `cd.yml`) ve Jenkins (`Jenkinsfile`) — aynı adımlar |
| GitOps | Argo CD, `k8s/overlays/dev` yolunu izler |

`<GITHUB_KULLANICI>`: Ajanları başlatmadan önce bu yer tutucuyu kendi GitHub kullanıcı adınla değiştir.

## 3. Ortak isimler ve port'lar (sözleşme — değiştirilemez)

| Öğe | product-service | order-service |
| --- | --- | --- |
| Maven groupId | `com.muhammedalihoca.demo` | `com.muhammedalihoca.demo` |
| artifactId / imaj adı | `product-service` | `order-service` |
| Kök paket | `com.muhammedalihoca.demo.product` | `com.muhammedalihoca.demo.order` |
| Container port | 8080 | 8080 |
| Lokal (docker run / IDE) port | 8081 | 8082 |
| K8s Service adı | `product-service` | `order-service` |
| Veritabanı | `products` | `orders` |
| Dış yol (HTTPRoute) | `/api/products` | `/api/orders` |

- Kubernetes namespace: `demo`
- Dışarıdan erişim adresi: `http://localhost` (kind port mapping 80 → Traefik)
- order-service, product-service'e `PRODUCT_SERVICE_URL` ortam değişkeniyle ulaşır (K8s'te `http://product-service:8080`).

## 4. Fonksiyonel kapsam

### product-service

Entity `Product`: `id (Long)`, `name (String, zorunlu)`, `price (BigDecimal, > 0)`, `stock (Integer, >= 0)`.

| Metot | Yol | Açıklama |
| --- | --- | --- |
| GET | `/api/products` | Tüm ürünler |
| GET | `/api/products/{id}` | Tek ürün, yoksa 404 |
| POST | `/api/products` | Ürün oluştur, 201 |
| POST | `/api/products/{id}/decrease-stock` | Gövde `{ "quantity": n }`; stok yetmezse 409 |

Flyway ile 3 örnek ürün yüklenir.

### order-service

Entity `Order`: `id (Long)`, `productId (Long)`, `quantity (Integer, > 0)`, `totalPrice (BigDecimal)`, `status (CREATED | REJECTED)`, `createdAt`.

| Metot | Yol | Açıklama |
| --- | --- | --- |
| POST | `/api/orders` | Gövde `{ "productId", "quantity" }`. Ürünü çeker, stok düşürür, siparişi kaydeder. Stok yetmezse `REJECTED` ile 409 |
| GET | `/api/orders` | Tüm siparişler |
| GET | `/api/orders/{id}` | Tek sipariş, yoksa 404 |

product-service erişilemezse 503 ve anlaşılır hata mesajı döner.

### Her iki serviste ortak

- `GET /api/info` → `{ "service": "...", "version": "<APP_VERSION env, yoksa 'local'>" }` — canlıda yeni sürümün geldiğini göstermek için.
- Actuator: `health`, `info`, `prometheus` açık; liveness ve readiness grupları aktif (`/actuator/health/liveness`, `/actuator/health/readiness`).
- **Bozuk sürüm anahtarı:** `DEMO_BROKEN=true` ise readiness `DOWN` döner (özel `HealthIndicator`). Canlıda "probe yakalar → rollback" senaryosu için.
- Loglar JSON değil düz metin, tek satır; her istekte `method path status süre` loglanır.
- Hata yanıtları `ProblemDetail` (RFC 9457) formatında.

## 5. Klasör yapısı

```
k8s-cicd-canli/
├── SPEC.md
├── CLAUDE.md
├── contracts/
│   └── product-api.yaml          # OpenAPI 3.1 — ana ajan yazar
├── services/
│   ├── product-service/
│   └── order-service/
├── cluster/
│   ├── kind-config.yaml
│   ├── setup-cluster.sh
│   └── reset.sh
├── k8s/
│   ├── base/
│   └── overlays/dev/
├── .github/workflows/
│   ├── ci.yml
│   └── cd.yml
├── jenkins/
│   ├── docker-compose.yml
│   ├── plugins.txt
│   └── Jenkinsfile
├── gitops/
│   └── argocd-app.yaml
└── docs/
```

## 6. "Bitti" tanımı

1. `cd services/<servis> && ./mvnw verify` iki serviste de yeşil.
2. `docker build` iki serviste de çalışıyor.
3. `./cluster/reset.sh` sıfırdan cluster kurup iki servisi + PostgreSQL'i ayağa kaldırıyor.
4. `curl http://localhost/api/products` ürünleri, `curl -X POST http://localhost/api/orders ...` sipariş oluşturuyor.
5. `ci.yml` ve `Jenkinsfile` aynı adımları içeriyor: test → build → imaj push.
6. Eğitmen notları `docs/` altında, her bölüm için ayrı dosya.

## 7. Kapsam dışı

Kimlik doğrulama, mesaj kuyruğu (Kafka), service mesh, Helm chart yazmak (Traefik ve Argo CD kurulumu hariç), bulut sağlayıcı.
