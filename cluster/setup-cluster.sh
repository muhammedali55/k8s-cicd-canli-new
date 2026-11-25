#!/usr/bin/env bash
# kind üzerinde "k8s-cicd" cluster'ını sıfırdan kurar ve demo uygulamasını ayağa kaldırır:
# kind -> Gateway API CRD'leri -> Traefik -> metrics-server -> demo namespace + Secret -> kustomize apply -> duman testi.
#
# Kullanım:
#   ./cluster/setup-cluster.sh                     # imajlar GHCR'dan çekilir
#   IMAGE_SOURCE=local ./cluster/setup-cluster.sh  # lokal docker imajları kind'e yüklenir (internetsiz B planı)
#
# Ayarlanabilir ortam değişkenleri:
#   IMAGE_SOURCE       ghcr (varsayılan) | local
#   POSTGRES_USER      varsayılan: postgres   (yalnızca lokal demo için)
#   POSTGRES_PASSWORD  varsayılan: postgres   (yalnızca lokal demo için)
#   WAIT_TIMEOUT       pod'ların hazır olması için bekleme süresi, varsayılan: 600s

set -euo pipefail

# ---------------------------------------------------------------------------
# Sabitler (sürümler bilerek sabit; "latest" kullanılmaz)
# ---------------------------------------------------------------------------
CLUSTER_NAME="k8s-cicd"
NAMESPACE="demo"
GATEWAY_API_VERSION="v1.6.3"        # Traefik chart 42.4.3'ün önerdiği Gateway API sürümü
TRAEFIK_CHART_VERSION="42.4.3"      # Traefik Proxy v3.7.20
METRICS_SERVER_CHART_VERSION="3.14.0"  # metrics-server v0.9.0
APP_TAG="0.1.0"
GHCR_USER="muhammedali55"

IMAGE_SOURCE="${IMAGE_SOURCE:-ghcr}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-postgres}"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-600s}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# ---------------------------------------------------------------------------
# Renkli çıktı yardımcıları (canlı yayında ekranda okunacak)
# ---------------------------------------------------------------------------
MAVI=$'\033[1;34m'
YESIL=$'\033[1;32m'
SARI=$'\033[1;33m'
KIRMIZI=$'\033[1;31m'
SIFIRLA=$'\033[0m'

ADIM=0
baslik() {
  ADIM=$((ADIM + 1))
  echo
  echo "${MAVI}==> [${ADIM}] $*${SIFIRLA}"
}
bilgi()  { echo "    $*"; }
basari() { echo "${YESIL}    ✔ $*${SIFIRLA}"; }
uyari()  { echo "${SARI}    ! $*${SIFIRLA}"; }
hata()   { echo "${KIRMIZI}    ✘ $*${SIFIRLA}" >&2; }

# ---------------------------------------------------------------------------
baslik "Gerekli araçlar kontrol ediliyor"
# ---------------------------------------------------------------------------
eksik=0
for arac in docker kind kubectl helm curl; do
  if command -v "$arac" >/dev/null 2>&1; then
    basari "$arac bulundu"
  else
    hata "$arac bulunamadı. Lütfen kurup tekrar deneyin."
    eksik=1
  fi
done
if [[ "$eksik" -ne 0 ]]; then
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  hata "Docker çalışmıyor. Docker Desktop'ı başlatıp tekrar deneyin."
  exit 1
fi
basari "Docker çalışıyor"

# ---------------------------------------------------------------------------
baslik "kind cluster'ı kuruluyor: ${CLUSTER_NAME}"
# ---------------------------------------------------------------------------
if kind get clusters 2>/dev/null | grep -qx "${CLUSTER_NAME}"; then
  uyari "Cluster zaten var, kurulum atlanıyor (sıfırdan kurmak için: ./cluster/reset.sh)"
else
  kind create cluster --config "${SCRIPT_DIR}/kind-config.yaml" --wait 120s
fi
kubectl config use-context "kind-${CLUSTER_NAME}" >/dev/null
basari "kubectl bağlamı: kind-${CLUSTER_NAME}"

# ---------------------------------------------------------------------------
if [[ "${IMAGE_SOURCE}" == "local" ]]; then
  baslik "Lokal imajlar kind'e yükleniyor (IMAGE_SOURCE=local)"
  # Servis imajları zorunlu: yoksa uygulama ayağa kalkamaz.
  for servis in product-service order-service; do
    imaj="ghcr.io/${GHCR_USER}/${servis}:${APP_TAG}"
    if ! docker image inspect "$imaj" >/dev/null 2>&1; then
      hata "Lokal imaj yok: $imaj"
      hata "Önce imajı build edin: docker build -t $imaj services/${servis}"
      exit 1
    fi
    kind load docker-image "$imaj" --name "${CLUSTER_NAME}"
    basari "$imaj yüklendi"
  done
  # Yardımcı imajlar: lokalde varsa yükle, yoksa cluster internetten çekmeyi dener.
  for imaj in "postgres:17" "docker.io/traefik:v3.7.20"; do
    if docker image inspect "$imaj" >/dev/null 2>&1; then
      kind load docker-image "$imaj" --name "${CLUSTER_NAME}"
      basari "$imaj yüklendi"
    else
      uyari "$imaj lokalde yok; cluster internetten çekmeyi deneyecek"
    fi
  done
else
  baslik "İmajlar GHCR'dan çekilecek (IMAGE_SOURCE=${IMAGE_SOURCE})"
  bilgi "ghcr.io/${GHCR_USER}/product-service ve order-service paketlerinin public olması gerekir."
fi

# ---------------------------------------------------------------------------
baslik "Gateway API CRD'leri yükleniyor (${GATEWAY_API_VERSION})"
# ---------------------------------------------------------------------------
kubectl apply --server-side -f \
  "https://github.com/kubernetes-sigs/gateway-api/releases/download/${GATEWAY_API_VERSION}/standard-install.yaml"
kubectl wait --for=condition=Established --timeout=60s \
  crd/gatewayclasses.gateway.networking.k8s.io \
  crd/gateways.gateway.networking.k8s.io \
  crd/httproutes.gateway.networking.k8s.io
basari "Gateway API CRD'leri hazır"

# ---------------------------------------------------------------------------
baslik "Traefik kuruluyor (Helm chart ${TRAEFIK_CHART_VERSION}, Gateway API sağlayıcısı açık)"
# ---------------------------------------------------------------------------
helm upgrade --install traefik oci://ghcr.io/traefik/helm/traefik \
  --version "${TRAEFIK_CHART_VERSION}" \
  --namespace traefik --create-namespace \
  --values "${SCRIPT_DIR}/traefik-values.yaml" \
  --wait --timeout 5m
basari "Traefik hazır (GatewayClass: traefik)"

# ---------------------------------------------------------------------------
baslik "metrics-server kuruluyor (HPA için CPU ölçümü, chart ${METRICS_SERVER_CHART_VERSION})"
# ---------------------------------------------------------------------------
# kind'deki kubelet sertifikaları self-signed; --kubelet-insecure-tls olmadan metrics-server veri toplayamaz.
helm upgrade --install metrics-server metrics-server \
  --repo https://kubernetes-sigs.github.io/metrics-server/ \
  --version "${METRICS_SERVER_CHART_VERSION}" \
  --namespace kube-system \
  --set 'args={--kubelet-insecure-tls}' \
  --wait --timeout 5m
basari "metrics-server hazır"

# ---------------------------------------------------------------------------
baslik "'${NAMESPACE}' namespace'i ve veritabanı Secret'ı oluşturuluyor"
# ---------------------------------------------------------------------------
# "create --dry-run | apply" kalıbı: script tekrar çalıştırılınca "zaten var" hatası vermez.
kubectl create namespace "${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -
# Secret git'e yazılmaz; burada oluşturulur. Değerler ortam değişkeninden gelir (lokal demo için postgres/postgres).
kubectl create secret generic postgres-credentials \
  --namespace "${NAMESPACE}" \
  --from-literal=username="${POSTGRES_USER}" \
  --from-literal=password="${POSTGRES_PASSWORD}" \
  --dry-run=client -o yaml | kubectl apply -f -
basari "namespace/${NAMESPACE} ve secret/postgres-credentials hazır"

# ---------------------------------------------------------------------------
baslik "Manifest'ler uygulanıyor (kubectl apply -k k8s/overlays/dev)"
# ---------------------------------------------------------------------------
kubectl apply -k "${REPO_ROOT}/k8s/overlays/dev"

# ---------------------------------------------------------------------------
baslik "Pod'ların hazır olması bekleniyor (en fazla ${WAIT_TIMEOUT})"
# ---------------------------------------------------------------------------
bilgi "PostgreSQL bekleniyor..."
kubectl rollout status statefulset/postgres -n "${NAMESPACE}" --timeout="${WAIT_TIMEOUT}"
basari "PostgreSQL hazır"

bilgi "Servisler bekleniyor (Spring açılışı biraz sürer)..."
if ! kubectl wait deployment --all -n "${NAMESPACE}" \
       --for=condition=Available --timeout="${WAIT_TIMEOUT}"; then
  hata "Servisler zamanında hazır olmadı. Pod durumları:"
  kubectl get pods -n "${NAMESPACE}" -o wide
  hata "İpucu: kubectl describe pod -n ${NAMESPACE} <pod>  |  kubectl logs -n ${NAMESPACE} <pod>"
  exit 1
fi
basari "Tüm Deployment'lar hazır"

kubectl wait gateway/demo-gateway -n "${NAMESPACE}" --for=condition=Programmed --timeout=120s
basari "Gateway hazır"

# ---------------------------------------------------------------------------
baslik "Duman testi: curl http://localhost/api/products"
# ---------------------------------------------------------------------------
# Traefik'in HTTPRoute'ları işlemesi birkaç saniye sürebilir; birkaç kez dene.
basarili=0
for deneme in $(seq 1 15); do
  if yanit="$(curl -fsS --max-time 5 http://localhost/api/products)"; then
    basarili=1
    break
  fi
  bilgi "Deneme ${deneme}/15 başarısız, 3 sn sonra tekrar..."
  sleep 3
done
if [[ "$basarili" -ne 1 ]]; then
  hata "http://localhost/api/products cevap vermedi."
  exit 1
fi
basari "Yanıt alındı:"
echo "${yanit}"

# ---------------------------------------------------------------------------
baslik "Özet"
# ---------------------------------------------------------------------------
kubectl get pods,svc,hpa -n "${NAMESPACE}"
echo
kubectl get gateway,httproute -n "${NAMESPACE}"
echo
echo "${YESIL}Cluster hazır!${SIFIRLA}"
echo "  Ürünler   : curl http://localhost/api/products"
echo "  Siparişler: curl http://localhost/api/orders"
echo "  Sipariş ver: curl -X POST http://localhost/api/orders -H 'Content-Type: application/json' -d '{\"productId\":1,\"quantity\":1}'"
