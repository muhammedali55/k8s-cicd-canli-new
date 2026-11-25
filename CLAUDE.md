# Proje kuralları (tüm ajanlar için)

Bu repo canlı bir eğitimde öğrencilere gösterilecek. Kod okunaklı ve öğretici olmalı; "zekice" değil.

## Temel kurallar

- `SPEC.md` tek doğruluk kaynağıdır. İsim, port, paket veya sürüm değiştirme; çelişki görürsen dur ve sor.
- Yalnızca sana verilen klasörde çalış. Başka ajanın klasörüne dosya yazma.
- `contracts/product-api.yaml` sözleşmedir; değiştirme. Yanlış olduğunu düşünüyorsan raporunda belirt.
- Yeni bağımlılık eklemeden önce gerçekten gerekli mi düşün; SPEC'te olmayan kütüphane ekliyorsan raporunda gerekçesini yaz.
- Gizli bilgi (token, şifre) asla dosyaya yazılmaz. Lokal geliştirme için `postgres/postgres` kabul; K8s'te Secret kullanılır.

## Kod stili

- Java 21 özellikleri serbest (record, switch pattern). DTO'lar `record`.
- Katmanlar: `controller` → `service` → `repository`. Entity'ler controller'dan dışarı sızmaz.
- Lombok KULLANILMAZ (öğrenciler ne olduğunu görsün).
- Her public sınıfın başında, ne işe yaradığını anlatan 1–2 satırlık Türkçe yorum.
- Testler `given / when / then` düzeninde, Türkçe `@DisplayName`.

## Kubernetes / YAML

- Her manifest'in başında, ne yaptığını anlatan Türkçe yorum satırları.
- Her container'da `resources.requests` ve `limits` (kind tek node; düşük tut: 250m/512Mi).
- Liveness ve readiness probe'ları Actuator uçlarına bağlı.
- `latest` tag'i yasak.

## Bitirince

Her ajan işini bitirince kısa bir rapor verir:
1. Oluşturduğu dosyalar
2. Çalıştırdığı doğrulama komutları ve sonuçları
3. SPEC'ten saptığı ya da emin olmadığı noktalar
