#!/usr/bin/env bash
# "k8s-cicd" kind cluster'ını tamamen siler ve setup-cluster.sh ile sıfırdan kurar.
# Sonda toplam süreyi yazar. Ortam değişkenleri (ör. IMAGE_SOURCE=local) setup-cluster.sh'e aynen geçer.
#
# Kullanım:
#   ./cluster/reset.sh
#   IMAGE_SOURCE=local ./cluster/reset.sh

set -euo pipefail

CLUSTER_NAME="k8s-cicd"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

MAVI=$'\033[1;34m'
YESIL=$'\033[1;32m'
SIFIRLA=$'\033[0m'

BASLANGIC=$SECONDS

echo "${MAVI}==> Mevcut '${CLUSTER_NAME}' cluster'ı siliniyor${SIFIRLA}"
# Cluster yoksa da hata vermez.
kind delete cluster --name "${CLUSTER_NAME}"

echo "${MAVI}==> Cluster sıfırdan kuruluyor (setup-cluster.sh)${SIFIRLA}"
bash "${SCRIPT_DIR}/setup-cluster.sh"

SURE=$((SECONDS - BASLANGIC))
echo
echo "${YESIL}==> Sıfırlama tamamlandı. Toplam süre: $((SURE / 60)) dk $((SURE % 60)) sn${SIFIRLA}"
