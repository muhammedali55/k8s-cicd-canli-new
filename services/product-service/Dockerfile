# product-service imajı (multi-stage).
# 1. aşama: JDK ile jar'ı derler. 2. aşama: sadece JRE + jar, non-root kullanıcı ile çalışır.
# Lokal build: docker build --build-arg APP_VERSION=0.1.0 -t ghcr.io/muhammedali55/product-service:0.1.0 .

# ---------- 1. aşama: build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Önce sadece Maven dosyaları: bağımlılıklar değişmedikçe bu katman cache'ten gelir.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

# Sonra kaynak kod. Testler CI'da ayrı adımda koşar; burada sadece paketliyoruz.
COPY src src
RUN ./mvnw -q -B package -DskipTests && cp target/product-service-*.jar app.jar

# ---------- 2. aşama: çalışma ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Root olmayan kullanıcı. Sayısal UID, K8s'teki runAsNonRoot kontrolü için gerekli.
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home app

COPY --from=build --chown=app:app /workspace/app.jar app.jar

USER 10001

# Container içinde servis 8080'de dinler (SPEC §3).
ENV SERVER_PORT=8080
EXPOSE 8080

# Container'ın bellek limitine göre heap ayarlanır.
# Sürüm bilgisi imaja gömülür: CI kısa git SHA'sını --build-arg ile verir, GET /api/info bunu gösterir.
# En sona koyduk: SHA her build'de değiştiği için yalnızca bu son katman yeniden oluşur.
ARG APP_VERSION=local
ENV APP_VERSION=${APP_VERSION}

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
