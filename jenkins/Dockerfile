# Jenkins LTS imajını, pipeline'ın ihtiyaç duyduğu araçlarla genişletir:
# docker CLI (imaj build/push), kubectl (deploy) ve plugins.txt'teki eklentiler.

# Sabit LTS sürümü, JDK 21 ile (latest yasak).
FROM jenkins/jenkins:2.516.3-lts-jdk21

# Paket kurmak için geçici olarak root'a geç.
USER root

# kubectl sürümünü sabitliyoruz.
ARG KUBECTL_VERSION=v1.33.4

# Docker CLI: Docker'ın resmi apt deposundan yalnızca istemci (daemon değil).
# Daemon, host'taki Docker'dır; socket üzerinden konuşuruz.
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl gnupg \
 && install -m 0755 -d /etc/apt/keyrings \
 && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
 && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
      > /etc/apt/sources.list.d/docker.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends docker-ce-cli docker-buildx-plugin \
 && rm -rf /var/lib/apt/lists/*

# kubectl: resmi Kubernetes release'inden, makine mimarisine göre (amd64/arm64).
RUN ARCH="$(dpkg --print-architecture)" \
 && curl -fsSL -o /usr/local/bin/kubectl \
      "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/${ARCH}/kubectl" \
 && chmod +x /usr/local/bin/kubectl

# Tekrar normal jenkins kullanıcısına dön.
USER jenkins

# Eklentileri imaj build anında kur; Jenkins ilk açılışta hazır gelsin.
COPY --chown=jenkins:jenkins plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
