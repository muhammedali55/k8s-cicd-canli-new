# GitOps — Yol B (Argo CD)

Yol A'da (`.github/workflows/cd.yml`) pipeline cluster'a **kendisi** `kubectl set image` yapar (push).
Yol B'de pipeline yalnızca **Git'e** yeni imaj etiketini yazar; cluster'daki Argo CD
Git'i izler ve değişikliği **kendisi çeker** (pull).

```
commit → CI (test, build, push imaj) → gitops-update job'u kustomization.yaml'ı commit'ler
       → Argo CD değişikliği görür → k8s/overlays/dev'i cluster'a uygular
```

> **Önemli — Yol A ile Yol B aynı anda açık olmamalı.**
> `argocd-app.yaml`'da `selfHeal: true` var: Argo CD, cluster'da Git'ten farklı olan her şeyi
> Git'teki hâline geri alır. Yol A'nın yaptığı `kubectl set image` (ve Jenkins'in Deploy aşaması)
> birkaç saniye içinde geri alınır. Bu yüzden GitOps modunda `cd.yml` çalışmaz
> (`vars.DEPLOY_MODE == 'gitops'` iken atlanır).

## 1. Argo CD'yi kur

Önce namespace, sonra sabit sürümlü resmi kurulum manifest'i (tek `kubectl apply`):

```bash
kubectl create namespace argocd
kubectl apply -n argocd --server-side --force-conflicts \
  -f https://raw.githubusercontent.com/argoproj/argo-cd/v3.1.0/manifests/install.yaml
```

> `--server-side`: Argo CD'nin CRD'leri büyük olduğu için istemci tarafı apply'ın
> annotation sınırına takılmamak için.

Pod'ların hazır olmasını bekle:

```bash
kubectl -n argocd rollout status deploy/argocd-server --timeout=180s
```

## 2. Arayüze eriş (port-forward)

```bash
kubectl -n argocd port-forward svc/argocd-server 8443:443
```

Tarayıcıda <https://localhost:8443> aç (self-signed sertifika uyarısını geç).

## 3. Admin şifresi

Kullanıcı adı `admin`. İlk şifre bir Secret'ta durur; ekrana yazdır, hiçbir dosyaya kaydetme:

```bash
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath='{.data.password}' | base64 -d; echo
```

## 4. Application'ı oluştur

```bash
kubectl apply -f gitops/argocd-app.yaml
```

Arayüzde `k8s-cicd-canli-dev` uygulaması görünür; birkaç saniye içinde **Synced / Healthy** olmalı.

## 5. GitOps modunu aç

GitHub'da repo → **Settings → Secrets and variables → Actions → Variables → New repository variable**:

- Name: `DEPLOY_MODE`
- Value: `gitops`

Artık `main`'e gelen her servis değişikliğinde:

1. `ci.yml` test eder, imajı build edip GHCR'a push eder.
2. `gitops-update` job'u `k8s/overlays/dev/kustomization.yaml`'daki `images:` bloğunu
   `kustomize edit set image` ile yeni kısa SHA'ya çevirir ve commit'ler.
3. Argo CD (varsayılan ~3 dk'da bir Git'i yoklar; arayüzden **Refresh** ile hızlandırılabilir)
   yeni etiketi cluster'a uygular.

Yol A'ya dönmek için `DEPLOY_MODE` değişkenini sil **ve** Application'ı kaldır
(ya da en azından otomatik sync'i kapat):

```bash
kubectl -n argocd delete application k8s-cicd-canli-dev
```

> Not: Application silindiğinde, üzerinde `resources-finalizer` olmadığı için
> `demo` namespace'indeki kaynaklar silinmez; yalnızca Argo CD izlemeyi bırakır.

## 6. Self-heal'i canlı göster

```bash
kubectl -n demo scale deploy/product-service --replicas=0
kubectl -n demo get deploy product-service -w
```

Argo CD birkaç saniye içinde replica sayısını Git'teki değere geri getirir.
