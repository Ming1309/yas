# YAS on GCP Minikube - Happy Path

This guide records the working path we used to deploy YAS on a GCP VM with
Minikube. It also records the errors that happened during the first run and the
fix for each one.

The goal is to be able to delete the cluster/configuration and start again
without repeating the same mistakes.

## Scope

This guide deploys the useful demo path first:

- Postgres
- Redis
- Keycloak
- `yas-configuration`
- Core services:
  - `product`
  - `cart`
  - `order`
  - `customer`
  - `inventory`
  - `tax`
  - `media`
  - `sampledata`
- BFF/UI:
  - `storefront-bff`
  - `storefront-ui`
  - `backoffice-bff`
  - `backoffice-ui`
- `swagger-ui`

Kafka, Elasticsearch, and `search` are still best deployed after the core
services are healthy, but this guide now includes the working Kafka/Search path.

## Important Decisions

### Use domain names, not the raw VM IP

The GCP VM external IP changes when the VM is recreated or restarted without a
static IP. Do not store raw IPs in Keycloak redirect URIs.

Use these local domains instead:

```txt
identity.yas.local.com
storefront.yas.local.com
backoffice.yas.local.com
```

Whenever the VM IP changes, update only your local machine `/etc/hosts`.

Example on your Mac:

```bash
sudo nano /etc/hosts
```

Add or update:

```txt
<VM_EXTERNAL_IP> identity.yas.local.com
<VM_EXTERNAL_IP> storefront.yas.local.com
<VM_EXTERNAL_IP> backoffice.yas.local.com
```

Flush DNS on macOS:

```bash
sudo dscacheutil -flushcache
sudo killall -HUP mDNSResponder
```

Verify:

```bash
ping identity.yas.local.com
```

### Correct public entrypoints

YAS uses the BFF pattern. The UI ports are only useful for debugging the UI
container.

Use these URLs for real browser testing:

```txt
Storefront: http://storefront.yas.local.com:30081
Backoffice: http://backoffice.yas.local.com:30087
Keycloak:   http://identity.yas.local.com
```

Debug-only UI ports:

```txt
storefront-ui:  http://storefront.yas.local.com:30080
backoffice-ui:  http://backoffice.yas.local.com:30086
```

If you open `30080` or `30086`, API calls such as `/api/product/...` will hit
the UI service and return HTML/404. Use `30081` and `30087` for the full flow.

## 1. Create GCP VM

Run from Cloud Shell.

```bash
gcloud config set project yas-k8s
gcloud config set compute/zone asia-southeast1-b
gcloud services enable compute.googleapis.com

gcloud compute instances create yas-minikube \
  --zone=asia-southeast1-b \
  --machine-type=e2-standard-8 \
  --image-family=ubuntu-2204-lts \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=80GB \
  --boot-disk-type=pd-balanced \
  --tags=yas-minikube
```

Open the required firewall ports:

```bash
gcloud compute firewall-rules create yas-allow-ssh \
  --allow=tcp:22 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=yas-minikube \
  --project=yas-k8s

gcloud compute firewall-rules create yas-allow-nodeport \
  --allow=tcp:30080-30091 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=yas-minikube \
  --project=yas-k8s

gcloud compute firewall-rules create yas-allow-http \
  --allow=tcp:80 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=yas-minikube \
  --project=yas-k8s

gcloud compute firewall-rules create yas-allow-argocd-ui \
  --allow=tcp:8080 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=yas-minikube \
  --project=yas-k8s
```

If a rule already exists, use `gcloud compute firewall-rules update ...`.

Get the VM IP:

```bash
gcloud compute instances describe yas-minikube \
  --zone=asia-southeast1-b \
  --project=yas-k8s \
  --format='get(networkInterfaces[0].accessConfigs[0].natIP)'
```

SSH into the VM:

```bash
gcloud compute ssh yas-minikube --zone=asia-southeast1-b --project=yas-k8s
```

## 2. Install base tools on the VM

Run inside the VM.

```bash
sudo apt update
sudo apt upgrade -y
sudo apt install -y ca-certificates curl gnupg lsb-release git vim wget unzip apt-transport-https
```

Install Docker:

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"
newgrp docker
docker run hello-world
```

Install `kubectl`:

```bash
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/
kubectl version --client
```

Install Minikube:

```bash
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
minikube version
```

Install Helm:

```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
helm version
```

Clone the repo:

```bash
git clone https://github.com/Ming1309/yas.git
cd yas
```

Use the branch that contains the K8s/Helm fixes before following this guide.

## 3. Start Minikube

```bash
minikube start \
  --driver=docker \
  --cpus=6 \
  --memory=24000 \
  --disk-size=70g
```

Enable useful addons:

```bash
minikube addons enable metrics-server
minikube addons enable ingress
```

Verify:

```bash
minikube status
kubectl get nodes -o wide
kubectl get pods -A
```

Create namespaces:

```bash
kubectl create namespace yas-dev --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace keycloak --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace postgres --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace redis --dry-run=client -o yaml | kubectl apply -f -
```

## 4. Install Postgres

Add Helm repositories:

```bash
helm repo add postgres-operator-charts https://opensource.zalando.com/postgres-operator/charts/postgres-operator
helm repo update
```

Install operator:

```bash
helm upgrade --install postgres-operator postgres-operator-charts/postgres-operator \
  --create-namespace \
  --namespace postgres
```

Install Postgres cluster:

```bash
helm upgrade --install postgres k8s/deploy/postgres/postgresql \
  --create-namespace \
  --namespace postgres \
  --set replicas=1 \
  --set username=yasadminuser \
  --set password=admin
```

Verify:

```bash
kubectl get pods -n postgres
kubectl get postgresql -n postgres
```

Wait until Postgres is running before installing app services.

### Error avoided

Old chart template bug:

```txt
YAML parse error on postgres/templates/postgresql.yaml:
invalid map key: map[interface {}]interface {}{".Values.username":interface {}(nil)}
```

Fix in the chart:

```yaml
recommendation: {{ .Values.username }}
webhook: {{ .Values.username }}
```

## 5. Install Redis

```bash
helm upgrade --install redis \
  --set auth.password=redis \
  oci://registry-1.docker.io/bitnamicharts/redis \
  -n redis \
  --create-namespace
```

Verify:

```bash
kubectl get pods -n redis
```

## 6. Install Keycloak

Install Keycloak CRDs/operator:

```bash
kubectl apply -f https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/26.0.2/kubernetes/keycloaks.k8s.keycloak.org-v1.yml
kubectl apply -f https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/26.0.2/kubernetes/keycloakrealmimports.k8s.keycloak.org-v1.yml
kubectl apply -f https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/26.0.2/kubernetes/kubernetes.yml -n keycloak
```

Install Keycloak and import the YAS realm:

```bash
helm upgrade --install keycloak k8s/deploy/keycloak/keycloak \
  --namespace keycloak \
  --set hostname=identity.yas.local.com \
  --set postgresql.username=yasadminuser \
  --set postgresql.password=admin \
  --set bootstrapAdmin.username=admin \
  --set bootstrapAdmin.password=admin \
  --set backofficeRedirectUrl=http://backoffice.yas.local.com:30087 \
  --set storefrontRedirectUrl=http://storefront.yas.local.com:30081
```

Verify:

```bash
kubectl get pods -n keycloak
kubectl get svc -n keycloak
kubectl get ingress -n keycloak
```

Expose Keycloak to your browser. Keep this command running in a separate VM
terminal or a `tmux` session:

```bash
sudo -E kubectl port-forward -n keycloak svc/keycloak-service 80:80 --address 0.0.0.0
```

On your Mac, update `/etc/hosts` with the current VM IP:

```txt
<VM_EXTERNAL_IP> identity.yas.local.com
<VM_EXTERNAL_IP> storefront.yas.local.com
<VM_EXTERNAL_IP> backoffice.yas.local.com
```

Test from your Mac:

```bash
curl -i http://identity.yas.local.com/realms/Yas/.well-known/openid-configuration
```

Expected:

```txt
HTTP/1.1 200 OK
```

### Error avoided: `DNS_PROBE_FINISHED_NXDOMAIN`

Cause: the browser runs on your Mac, not in Kubernetes. CoreDNS inside the
cluster does not help your Mac resolve `identity.yas.local.com`.

Fix: update `/etc/hosts` on the machine running the browser.

### Error avoided: `Invalid parameter: redirect_uri`

Cause: using raw IP URLs such as:

```txt
http://34.x.x.x:30087/login/oauth2/code/api-client
```

without adding them to Keycloak Valid Redirect URIs.

Fix: use stable local domains:

```txt
http://backoffice.yas.local.com:30087/*
http://storefront.yas.local.com:30081/*
```

The install command above sets these redirect URLs.

## 7. Install yas-configuration

Add Stakater Helm repo:

```bash
helm repo add stakater https://stakater.github.io/stakater-charts
helm repo update
```

Build dependency and install config:

```bash
helm dependency build k8s/charts/yas-configuration

helm upgrade --install yas-configuration k8s/charts/yas-configuration \
  -n yas-dev \
  --create-namespace \
  --set mediaApplicationConfig.yas.publicUrl=http://storefront.yas.local.com:30081/api/media
```

Verify that gateway routes use the Spring Boot 4 / Spring Cloud Gateway WebFlux
property path:

```bash
kubectl get configmap yas-gateway-routes-config-configmap -n yas-dev -o yaml | grep -n "server:\|webflux:\|product_api\|uri:"
```

Expected shape:

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: product_api
              uri: http://product
```

### Error avoided: BFF routes to `nginx`

Symptom:

```txt
500 Server Error for HTTP GET "/api/product/..."
java.net.UnknownHostException: Failed to resolve 'nginx'
```

Cause: old config used:

```yaml
spring.cloud.gateway.routes
```

but current BFF images use:

```yaml
spring.cloud.gateway.server.webflux.routes
```

Fix: use the corrected `yas-configuration` chart.

### Error avoided: BFF CrashLoopBackOff from bad YAML

Symptom:

```txt
while parsing a flow sequence
filters: [RewritePath=/api/(?<segment>.*), /${segment}, TokenRelay=]
```

Cause: inline YAML list breaks because `RewritePath` contains comma and regex
characters.

Fix: keep filters in block list form:

```yaml
filters:
  - RewritePath=/api/(?<segment>.*), /${segment}
  - TokenRelay=
```

## 8. Make cluster DNS resolve Keycloak for pods

BFF pods must resolve `identity.yas.local.com` inside Kubernetes.

Patch CoreDNS to rewrite the public Keycloak host to the Keycloak service:

```bash
kubectl -n kube-system patch configmap coredns --type merge -p '{
  "data": {
    "Corefile": ".:53 {\n    log\n    errors\n    health {\n       lameduck 5s\n    }\n    ready\n    rewrite name identity.yas.local.com keycloak-service.keycloak.svc.cluster.local\n    kubernetes cluster.local in-addr.arpa ip6.arpa {\n       pods insecure\n       fallthrough in-addr.arpa ip6.arpa\n       ttl 30\n    }\n    prometheus :9153\n    forward . /etc/resolv.conf {\n       max_concurrent 1000\n    }\n    cache 30 {\n       disable success cluster.local\n       disable denial cluster.local\n    }\n    loop\n    reload\n    loadbalance\n}\n"
  }
}'

kubectl rollout restart deployment/coredns -n kube-system
kubectl rollout status deployment/coredns -n kube-system
```

Test from a pod:

```bash
kubectl run dns-test -n yas-dev --rm -it --image=curlimages/curl --restart=Never -- \
  curl -m 10 -v http://identity.yas.local.com/realms/Yas/.well-known/openid-configuration
```

Expected:

```txt
HTTP/1.1 200 OK
```

### Error avoided: `UnknownHostException identity.yas.local.com`

Symptom:

```txt
Unable to resolve Configuration with issuer
http://identity.yas.local.com/realms/Yas
java.net.UnknownHostException: identity.yas.local.com
```

Fix: CoreDNS rewrite above.

## 9. Deploy app services from Docker Hub

The environment values in this repo point to Docker Hub images published by the
main CI/CD pipeline, for example `docker.io/mingpham/yas-product:main`.

Use the values files as the source of truth. Do not override to public GHCR
images unless you are doing an isolated fallback test.

Deploy core backend services:

```bash
for svc in product cart order customer inventory media tax; do
  helm dependency build k8s/charts/$svc || true
  helm upgrade --install $svc k8s/charts/$svc \
    -n yas-dev \
    -f k8s/environments/dev/$svc.values.yaml
done
```

Deploy sampledata:

```bash
helm dependency build k8s/charts/sampledata || true

helm upgrade --install sampledata k8s/charts/sampledata \
  -n yas-dev \
  -f k8s/environments/dev/sampledata.values.yaml \
  --set backend.replicaCount=1
```

Deploy BFFs:

```bash
helm dependency build k8s/charts/storefront-bff || true
helm dependency build k8s/charts/backoffice-bff || true

helm upgrade --install storefront-bff k8s/charts/storefront-bff \
  -n yas-dev \
  -f k8s/environments/dev/storefront-bff.values.yaml

helm upgrade --install backoffice-bff k8s/charts/backoffice-bff \
  -n yas-dev \
  -f k8s/environments/dev/backoffice-bff.values.yaml
```

Deploy UIs:

```bash
helm dependency build k8s/charts/storefront-ui || true
helm dependency build k8s/charts/backoffice-ui || true

helm upgrade --install storefront-ui k8s/charts/storefront-ui \
  -n yas-dev \
  -f k8s/environments/dev/storefront-ui.values.yaml

helm upgrade --install backoffice-ui k8s/charts/backoffice-ui \
  -n yas-dev \
  -f k8s/environments/dev/backoffice-ui.values.yaml
```

Deploy Swagger UI:

```bash
helm upgrade --install swagger-ui k8s/charts/swagger-ui \
  -n yas-dev \
  -f k8s/environments/dev/swagger-ui.values.yaml
```

Verify:

```bash
kubectl get pods -n yas-dev
kubectl get svc -n yas-dev
kubectl get endpoints -n yas-dev
kubectl get deploy -n yas-dev -o custom-columns=NAME:.metadata.name,IMAGE:.spec.template.spec.containers[0].image
```

### Error avoided: `ErrImagePull`

Cause: the CI/CD pipeline has not pushed the expected Docker Hub image/tag yet,
for example:

```txt
docker.io/mingpham/yas-product:main
```

Fix: wait for the pipeline to publish the image, or temporarily override to a
known public image only for local testing:

```txt
ghcr.io/nashtech-garage/yas-product:latest
```

## 10. Test BFF routing

Storefront product route:

```bash
curl -i http://storefront.yas.local.com:30081/api/product/storefront/categories
```

Expected before sample data:

```txt
HTTP/1.1 200 OK
[]
```

Backoffice redirects to Keycloak if not logged in:

```bash
curl -I http://backoffice.yas.local.com:30087
```

Expected:

```txt
HTTP/1.1 302 Found
Location: /oauth2/authorization/api-client
```

Open in browser:

```txt
http://storefront.yas.local.com:30081
http://backoffice.yas.local.com:30087
```

## 11. Create/login user for backoffice

Open Keycloak Admin:

```txt
http://identity.yas.local.com/admin
```

Admin credentials:

```txt
admin / admin
```

Use realm:

```txt
Yas
```

Create a user:

```txt
Users -> Create user
Username: admin@yastest.com
Email: admin@yastest.com
Email verified: ON
```

Set password:

```txt
Credentials -> Set password
Password: admin
Temporary: OFF
```

Assign the YAS app role:

```txt
Role mapping -> Assign role -> Filter by realm roles -> ADMIN -> Assign
```

Important: do not assign only `realm-management / realm-admin`. That is a
Keycloak admin role, not the YAS app role.

If access is still denied, make sure the token contains:

```json
"realm_access": {
  "roles": ["ADMIN"]
}
```

Backoffice BFF requires:

```java
hasAnyRole("ADMIN")
```

## 12. Seed sample data

Make sure `sampledata` is running:

```bash
kubectl get deploy,pod,svc,endpoints -n yas-dev | grep sampledata
```

If it is scaled to `0/0`, scale it:

```bash
kubectl scale deploy/sampledata -n yas-dev --replicas=1
kubectl rollout status deploy/sampledata -n yas-dev
```

Seed data with `POST`, not `GET`:

```bash
curl -i -X POST http://storefront.yas.local.com:30081/api/sampledata/storefront/sampledata \
  -H 'Content-Type: application/json' \
  -d '{"message":"seed"}'
```

Expected:

```json
{"message":"Insert Sample Data successfully!"}
```

Check products:

```bash
curl -i http://storefront.yas.local.com:30081/api/product/storefront/categories
curl -i "http://storefront.yas.local.com:30081/api/product/storefront/products/featured?pageNo=0"
```

### Error avoided: `GET is not supported`

Symptom:

```txt
Request method 'GET' is not supported
```

Fix: call the sampledata endpoint with `POST` and a JSON body.

## 13. Make sample images available to media

Sampledata inserts media database records with paths such as:

```txt
/images/sample/product/ip15/iphone15_thumbnail.jpg
```

The image files must also exist inside the `media` pod. For a quick demo,
copy them from the repo into the running pod:

```bash
MEDIA_POD=$(kubectl get pod -n yas-dev -l app.kubernetes.io/instance=media -o jsonpath='{.items[0].metadata.name}')
echo "$MEDIA_POD"

kubectl exec -n yas-dev "$MEDIA_POD" -- mkdir -p /images
kubectl cp sampledata/images/. yas-dev/"$MEDIA_POD":/images/
```

Verify:

```bash
kubectl exec -n yas-dev "$MEDIA_POD" -- ls -lah /images/sample/product/ip15/iphone15_thumbnail.jpg
```

Test image with `GET`, not `HEAD`:

```bash
curl -s -D - \
  "http://storefront.yas.local.com:30081/api/media/medias/7/file/iphone15_thumbnail.jpg" \
  -o /tmp/iphone15_thumbnail.jpg
```

Expected:

```txt
HTTP/1.1 200 OK
Content-Type: image/jpeg
```

### Error avoided: images do not load although media service is running

Cause 1: `mediaApplicationConfig.yas.publicUrl` defaults to:

```txt
http://api.yas.local.com/media
```

That domain is not exposed in this NodePort setup.

Fix: install `yas-configuration` with:

```bash
--set mediaApplicationConfig.yas.publicUrl=http://storefront.yas.local.com:30081/api/media
```

Cause 2: sampledata inserts DB rows, but the actual image files are not mounted
inside the media pod.

Fix for demo: `kubectl cp sampledata/images/. ...:/images/`.

Cause 3: testing with `curl -I` sends `HEAD`, while media permits public `GET`
for `/medias/**`.

Fix: use `curl -s -D - URL -o file`.

Note: `kubectl cp` is a temporary demo fix. If the media pod restarts, copied
files disappear. For a durable setup, use a PVC/initContainer or build the
sample assets into an image.

## 14. ArgoCD

Install ArgoCD:

```bash
kubectl apply -n argocd \
  -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

If you see:

```txt
applicationsets.argoproj.io metadata.annotations: Too long
```

but all ArgoCD pods become `Running`, continue. The main `Application` CRD is
enough for this flow.

Check:

```bash
kubectl get pods -n argocd
```

Get initial password:

```bash
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d; echo
```

Expose UI:

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443 --address 0.0.0.0
```

Open:

```txt
https://<VM_EXTERNAL_IP>:8080
```

## 15. Kafka, Elasticsearch, and Search

Deploy Kafka/Search only after the core services are healthy. The original YAS
Kafka chart used ZooKeeper, but Strimzi `0.47.0` supports only KRaft clusters.
This repo keeps the API at `kafka.strimzi.io/v1beta2`, but the chart is migrated
to `KafkaNodePool` + KRaft.

Install Strimzi:

```bash
helm repo add strimzi https://strimzi.io/charts/ || true
helm repo update

helm upgrade --install kafka-operator strimzi/strimzi-kafka-operator \
  --namespace kafka \
  --create-namespace \
  --version 0.47.0

kubectl rollout status deploy/strimzi-cluster-operator -n kafka --timeout=180s
```

Build the Kafka Connect image with the Debezium PostgreSQL plugin when the
Docker Hub image is not available yet:

```bash
docker build -t mingpham/debezium-connect-postgresql:0.47.0 \
  -f k8s/deploy/kafka/debezium-connect/Dockerfile .

docker push mingpham/debezium-connect-postgresql:0.47.0
```

Deploy Kafka and Debezium:

```bash
helm upgrade --install kafka-cluster k8s/deploy/kafka/kafka-cluster \
  --namespace kafka \
  --set kafka.replicas=1 \
  --set postgresql.username=yasadminuser \
  --set postgresql.password=admin

kubectl get pods -n kafka -w
kubectl get kafka,kafkanodepool,kafkaconnect,kafkaconnector -n kafka
```

Expected pods:

```txt
kafka-cluster-dual-role-0
debezium-connect-cluster-connect-0
kafka-cluster-entity-operator-...
strimzi-cluster-operator-...
```

Expected connector:

```txt
debezium-connector-postgresql-product-db   READY=True
```

Install Elasticsearch `9.2.3`. If an old Elasticsearch `8.x` PVC exists, delete
it first in dev because Elasticsearch does not allow a direct data upgrade from
`8.8.1` to `9.2.3`.

```bash
helm upgrade --install elasticsearch-cluster k8s/deploy/elasticsearch/elasticsearch-cluster \
  --namespace elasticsearch \
  --create-namespace \
  --set elasticsearch.replicas=1

kubectl get elasticsearch -n elasticsearch
kubectl get pods -n elasticsearch -w
```

Deploy search:

```bash
helm dependency build k8s/charts/search

helm upgrade --install search k8s/charts/search \
  -n yas-dev \
  -f k8s/environments/dev/search.values.yaml

kubectl get pods -n yas-dev | grep search
```

The search service has context path `/search`, so the direct service URL is:

```bash
kubectl run search-test -n yas-dev --rm -it --image=curlimages/curl --restart=Never -- \
  curl -i "http://search/search/storefront/catalog-search?keyword=iphone&page=0"
```

Through storefront BFF:

```bash
curl -i "http://$VM_IP:30081/api/search/storefront/catalog-search?keyword=iphone&page=0"
```

If the BFF returns a Next.js HTML 404 page, restart it so it reloads
`yas-gateway-routes-config-configmap`:

```bash
kubectl rollout restart deploy/storefront-bff -n yas-dev
kubectl rollout status deploy/storefront-bff -n yas-dev
```

Cleanup if Kafka was partially installed:

```bash
helm uninstall kafka-cluster -n kafka || true
helm uninstall kafka-operator -n kafka || true
kubectl delete kafka,kafkanodepool,kafkaconnect,kafkaconnector --all -n kafka --ignore-not-found
kubectl delete namespace kafka --ignore-not-found
```

Cleanup old Elasticsearch dev data when recreating a `9.2.3` cluster from a
previous `8.x` attempt:

```bash
kubectl get pvc -n elasticsearch
kubectl delete pvc elasticsearch-data-elasticsearch-es-node-0 -n elasticsearch --ignore-not-found
```

## 16. Common recovery commands

Check pods/services:

```bash
kubectl get pods -n yas-dev
kubectl get svc -n yas-dev
kubectl get endpoints -n yas-dev
```

Check images:

```bash
kubectl get deploy -n yas-dev \
  -o custom-columns=NAME:.metadata.name,IMAGE:.spec.template.spec.containers[0].image
```

Logs:

```bash
kubectl logs -n yas-dev deploy/storefront-bff --tail=120
kubectl logs -n yas-dev deploy/backoffice-bff --tail=120
kubectl logs -n yas-dev deploy/product --tail=120
kubectl logs -n yas-dev deploy/media --tail=120
kubectl logs -n yas-dev deploy/sampledata --tail=120
```

Restart after config changes:

```bash
kubectl rollout restart deploy/storefront-bff deploy/backoffice-bff -n yas-dev
kubectl rollout restart deploy/product deploy/media -n yas-dev
```

Delete a stuck test pod:

```bash
kubectl delete pod dns-test -n yas-dev --ignore-not-found
kubectl delete pod bff-test -n yas-dev --ignore-not-found
kubectl delete pod sampledata-test -n yas-dev --ignore-not-found
```

Stop VM from Cloud Shell:

```bash
gcloud compute instances stop yas-minikube \
  --zone=asia-southeast1-b \
  --project=yas-k8s
```

Do not run that from inside the VM. It can fail with:

```txt
ACCESS_TOKEN_SCOPE_INSUFFICIENT
```

## 17. Final verification checklist

Cluster:

```bash
kubectl get pods -n postgres
kubectl get pods -n redis
kubectl get pods -n keycloak
kubectl get pods -n yas-dev
```

All required app pods should be `1/1 Running`.

Keycloak:

```bash
curl -i http://identity.yas.local.com/realms/Yas/.well-known/openid-configuration
```

Storefront:

```bash
curl -i http://storefront.yas.local.com:30081/api/product/storefront/categories
curl -i "http://storefront.yas.local.com:30081/api/product/storefront/products/featured?pageNo=0"
```

Media:

```bash
curl -s -D - \
  "http://storefront.yas.local.com:30081/api/media/medias/7/file/iphone15_thumbnail.jpg" \
  -o /tmp/iphone15_thumbnail.jpg
```

Browser:

```txt
http://storefront.yas.local.com:30081
http://backoffice.yas.local.com:30087
```

Backoffice login user:

```txt
username: admin@yastest.com
password: admin
role: ADMIN
```

## Quick Summary of Bugs We Hit

| Problem | Root cause | Fix |
| --- | --- | --- |
| `ErrImagePull` | CI/CD has not pushed the expected Docker Hub image/tag yet | Wait for CI/CD or temporarily override to a known public image for local testing |
| BFF `UnknownHostException identity.yas.local.com` | Pod DNS could not resolve Keycloak domain | CoreDNS rewrite to `keycloak-service.keycloak.svc.cluster.local` |
| Browser `DNS_PROBE_FINISHED_NXDOMAIN` | Mac `/etc/hosts` missing | Add VM IP for `identity`, `storefront`, `backoffice` domains |
| Keycloak `Invalid parameter: redirect_uri` | Raw IP callback not whitelisted | Use domain URLs in Keycloak redirect config |
| Backoffice `Access Denied` | User lacks YAS realm role `ADMIN` | Assign realm role `ADMIN`, not `realm-management/realm-admin` only |
| UI 404 / `Unexpected token '<'` | Browser opened UI port directly | Use BFF ports `30081` and `30087` |
| BFF route hits `nginx` | Old gateway config key | Use `spring.cloud.gateway.server.webflux.routes` |
| BFF CrashLoop YAML parser | Inline `filters: [...]` with comma regex | Use block YAML list |
| Sampledata GET 500 | Endpoint is POST-only | Use `POST` with JSON body |
| Media image 401 on `curl -I` | `HEAD` not public-permitted | Test with GET |
| Media image 500 | DB has image path but pod lacks files | Copy `sampledata/images` into `/images` or mount durable storage |
| Kafka CRD/version errors | Strimzi/YAS chart/K8s version mismatch | Use Strimzi `0.47.0` with the KRaft chart in this repo |
| KafkaConnector missing `PostgresConnector` | Kafka Connect image lacks Debezium plugin | Build/push `k8s/deploy/kafka/debezium-connect/Dockerfile` |
| Search crashes on Elasticsearch 400 | Elasticsearch version does not match search client | Use Elasticsearch `9.2.3`; delete old dev PVC if it was created by `8.x` |
