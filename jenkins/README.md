# Jenkins — lokal kurulum

Bu klasör, `.github/workflows/ci.yml` + `cd.yml` ile **aynı adımları** (Checkout → Test →
Build Image → Push → Deploy) Jenkins üzerinde çalıştırmak içindir.

| Dosya | Ne işe yarar? |
| --- | --- |
| `Dockerfile` | Jenkins LTS + docker CLI + kubectl + eklentiler |
| `plugins.txt` | İmaj build edilirken kurulan eklentiler |
| `docker-compose.yml` | Jenkins'i kalıcı volume, Docker socket ve `kind` ağıyla başlatır |
| `Jenkinsfile` | Declarative pipeline |

## 0. Ön koşul: kind cluster'ı ayakta olmalı

Jenkins container'ı, kind'in oluşturduğu `kind` adlı Docker ağına katılır. Bu ağ ancak cluster
kurulduktan sonra vardır:

```bash
docker network ls | grep kind     # "kind" satırını görmelisin
```

> **Neden?** kind'in normal kubeconfig'i API sunucusunu `https://127.0.0.1:<port>` olarak yazar.
> Container içinde `127.0.0.1` Jenkins'in kendisidir, host değil. Bu yüzden Jenkins'i `kind`
> ağına bağlıyoruz ve kubeconfig'i `--internal` ile (adres: `k8s-cicd-control-plane:6443`) üretiyoruz.

## 1. Jenkins'i başlat

```bash
cd jenkins
docker compose up -d --build
docker compose logs -f jenkins      # "Jenkins is fully up and running" görünce Ctrl+C
```

Linux'ta Docker socket izni için docker grubunun GID'ini ver:

```bash
DOCKER_GID=$(getent group docker | cut -d: -f3) docker compose up -d --build
```

## 2. İlk açılış ve admin şifresi

1. Tarayıcıda <http://localhost:8080> aç.
2. İlk kurulum şifresini al (her kurulumda farklıdır, hiçbir yere kaydetme):

   ```bash
   docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
   ```

3. Şifreyi yapıştır → **Install suggested plugins** (gerekli eklentiler zaten imajda).
4. Kendi admin kullanıcını oluştur → Jenkins URL'i `http://localhost:8080/` olarak bırak.

## 3. Credential'ları ekle

**Manage Jenkins → Credentials → System → Global credentials → Add Credentials**

### 3a. `ghcr-creds` — GHCR'a push için

1. GitHub'da **Settings → Developer settings → Personal access tokens → Tokens (classic)**
   → `write:packages` (ve `read:packages`) yetkili bir token üret.
2. Jenkins'te:
   - Kind: **Username with password**
   - Username: `muhammedali55`
   - Password: (ürettiğin token — yalnızca bu forma yapıştır)
   - ID: `ghcr-creds`

### 3b. `kubeconfig` — kind cluster'ına deploy için

1. Host'ta, container içinden erişilebilen adresle kubeconfig üret:

   ```bash
   kind get kubeconfig --internal --name k8s-cicd > /tmp/kind-internal.kubeconfig
   ```

2. Jenkins'te:
   - Kind: **Secret file**
   - File: `/tmp/kind-internal.kubeconfig`
   - ID: `kubeconfig`
3. Yükledikten sonra geçici dosyayı sil: `rm /tmp/kind-internal.kubeconfig`

Jenkins container'ının kind API sunucusunu görebildiğini test etmek için:

```bash
docker exec jenkins getent hosts k8s-cicd-control-plane
```

`k8s-cicd-control-plane` için bir IP dönüyorsa ağ bağlantısı doğrudur.

## 4. Pipeline job'unu oluştur

1. **New Item** → ad: `k8s-cicd-canli` → **Pipeline** → OK.
2. **Build Triggers**: "GitHub hook trigger for GITScm polling" kutusunu işaretle.
3. **Pipeline** bölümü:
   - Definition: **Pipeline script from SCM**
   - SCM: **Git**
   - Repository URL: `https://github.com/muhammedali55/k8s-cicd-canli-new.git`
   - Branch: `*/main`
   - Script Path: `jenkins/Jenkinsfile`
4. **Save** → **Build Now**. İlk build iki servisi de derler (önceki başarılı build olmadığı için).

## 5. GitHub webhook

GitHub, `localhost`'a ulaşamaz. Jenkins'i dışarı açmak için bir tünel gerekir
(ör. `ngrok http 8080` ya da <https://smee.io>).

1. Tünel adresini al, ör. `https://abcd-1234.ngrok-free.app`.
2. GitHub'da repo → **Settings → Webhooks → Add webhook**:
   - Payload URL: `https://<tünel-adresi>/github-webhook/` (sondaki `/` önemli)
   - Content type: `application/json`
   - Events: **Just the push event**
3. Kaydet; "Recent Deliveries" altında yeşil tik görmelisin.
4. Bir servis dosyasını değiştirip `main`'e push et → Jenkins build'i kendiliğinden başlar.

Tünel kullanmak istemezsen eğitimde **Build Now** ile elle tetiklemek yeterlidir.

## 6. Dikkat edilecekler

- **GitOps modu açıksa** (Argo CD, `selfHeal: true`) Deploy aşaması anlamsızdır: Argo CD,
  `kubectl set image` ile yapılan değişikliği Git'teki hâline geri alır.
- GHCR paketleri varsayılan olarak private'tır; cluster'ın imajı çekebilmesi için paketi
  public yap ya da `demo` namespace'ine imagePullSecret ekle.
- Her şeyi sıfırlamak için: `docker compose down -v` (volume'u da siler; credential'lar gider).
