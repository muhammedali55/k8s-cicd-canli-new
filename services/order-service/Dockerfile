# order-service imajı — iki aşamalı (multi-stage) build.
#   1. aşama (build): JDK + Maven Wrapper ile jar üretilir.
#   2. aşama (run):   yalnızca JRE ve jar; daha küçük ve daha güvenli imaj.
# Kullanım: docker build --build-arg APP_VERSION=0.1.0 -t ghcr.io/muhammedali55/order-service:0.1.0 .

# ---------- 1. aşama: build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Önce yalnızca wrapper ve pom kopyalanır: bağımlılıklar ayrı bir katmanda önbelleğe alınır.
# Kaynak kod değişse bile pom değişmediyse bu katman yeniden indirilmez.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

# Kaynak kodu kopyala ve jar'ı üret. Testler CI'da ayrı adımda koşar.
COPY src src
RUN ./mvnw -q -B package -DskipTests

# ---------- 2. aşama: run ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Uygulama root olarak çalışmaz
RUN groupadd --system app && useradd --system --gid app --no-create-home app

COPY --from=build /workspace/target/order-service-*.jar app.jar
RUN chown app:app app.jar

USER app

# Container içinde port her zaman 8080 (lokal 8082 yalnızca IDE için)
ENV SERVER_PORT=8080
EXPOSE 8080

# Bellek limitini container'a göre ayarla (K8s limits ile uyumlu)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# Sürüm bilgisi imaja gömülür: CI kısa git SHA'sını --build-arg ile verir, GET /api/info bunu gösterir.
# En sona koyduk: SHA her build'de değiştiği için yalnızca bu son katman yeniden oluşur.
ARG APP_VERSION=local
ENV APP_VERSION=${APP_VERSION}

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
