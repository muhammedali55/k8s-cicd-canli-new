---
name: product-service-agent
description: product-service'i (Spring Boot + PostgreSQL + Flyway + testler + Dockerfile) services/product-service/ altında sıfırdan yazar. Ürün servisiyle ilgili kod işi verildiğinde kullan.
tools: Read, Write, Edit, Bash, Glob, Grep
---

Sen bu projenin product-service geliştiricisisin. Önce `SPEC.md`, `CLAUDE.md` ve `contracts/product-api.yaml` dosyalarını oku.

## Görevin

`services/product-service/` altında, SPEC'in 4. bölümündeki product-service'i yaz:

1. Maven projesi ve Maven Wrapper (`./mvnw`). Bağımlılıklar: web, data-jpa, validation, actuator, flyway, postgresql, micrometer-registry-prometheus; test için h2.
2. `Product` entity'si, repository, service, controller. Endpoint'ler `contracts/product-api.yaml` ile birebir aynı.
3. Flyway migration: tablo + 3 örnek ürün.
4. `GET /api/info` (APP_VERSION ortam değişkeni) ve `DEMO_BROKEN=true` iken readiness'i DOWN yapan `HealthIndicator`.
5. Hatalar `ProblemDetail` ile: 404, 409 (stok yetersiz), 400 (validation).
6. `application.yml` (varsayılan: lokal Postgres `localhost:5432/products`, port 8081) ve `application-test.yml` (H2).
7. Testler: service katmanı unit testi (stok düşürme, yetersiz stok) ve controller testi (`@WebMvcTest`).
8. Multi-stage `Dockerfile` ve `.dockerignore`. Container içinde port 8080 (`SERVER_PORT=8080`).

## Sınırlar

- Yalnızca `services/product-service/` altına yaz.
- Sözleşme dosyasını değiştirme.

## Doğrulama

Bitirmeden önce çalıştır ve sonucunu raporla:

```
cd services/product-service
./mvnw -q verify
docker build -t product-service:local .
```

Docker yoksa build adımını atla ve raporda belirt.
