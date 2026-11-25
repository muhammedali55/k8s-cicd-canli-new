# Başlangıç paketi — Cumartesi

Bu klasör, demo projeyi AI ajanlarla üretmek için gereken her şeyi içeriyor. Kod yok; kodu ajanlar yazacak.

## İçindekiler

| Dosya | Ne işe yarar |
| --- | --- |
| `SPEC.md` | Projenin tek doğruluk kaynağı: teknoloji, isimler, port'lar, endpoint'ler, "bitti" tanımı |
| `CLAUDE.md` | Tüm ajanların uyacağı proje kuralları (Claude Code bunu otomatik okur) |
| `.claude/agents/*.md` | 5 alt ajan tanımı (Claude Code otomatik tanır) |
| `prompts/ANA_AJAN_PROMPTLARI.md` | Ana ajana sırayla yapıştıracağın 7 adım |

## Başlamadan önce (10 dk)

1. `SPEC.md` içinde `<GITHUB_KULLANICI>` geçen yeri kendi GitHub kullanıcı adınla değiştir.
2. Araçlar kurulu mu kontrol et:
   ```
   docker version && kind version && kubectl version --client && helm version && java -version && claude --version
   ```
3. GitHub'da boş `k8s-cicd-canli` reposunu aç, bu klasörü içine koy, ilk commit'i at:
   ```
   git init && git add . && git commit -m "SPEC, kurallar ve ajan tanımları" && git branch -M main
   git remote add origin git@github.com:<GITHUB_KULLANICI>/k8s-cicd-canli.git && git push -u origin main
   ```
4. Base imajları önceden çek (canlıda internet yavaşlarsa diye):
   ```
   docker pull eclipse-temurin:21-jdk && docker pull eclipse-temurin:21-jre && docker pull postgres:17
   ```

## Kayıt

- Ekran kaydını Adım 1'den ÖNCE başlat, Adım 7 bitince durdur.
- Terminal yazı boyutunu büyüt; canlıda hızlandırılmış izletileceği için okunaklı olmalı.
- Ajanlar paralel çalışırken (Adım 3) acele etme; o anı kayıtta uzun tut, sonra hızlandırırsın.
- Her "Kontrol" durağında ne gördüğünü sesli söyle. Bu kısımlar hızlandırılmadan izletilecek.

## Akış

`prompts/ANA_AJAN_PROMPTLARI.md` dosyasını aç ve Adım 1'den başla. Takıldığın yerde hatayı bu sohbete yapıştır, birlikte çözelim.
