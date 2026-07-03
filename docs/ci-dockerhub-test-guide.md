# Huong dan test CI Docker Hub

File nay dung de test va chung minh task CI/Docker image/Docker Hub da hoan
thanh tren branch `feature/ci-pipeline`.

## 1. Muc tieu can chung minh

Task CI duoc xem la xong khi chung minh duoc:

| Yeu cau | Cach kiem tra |
| --- | --- |
| CI chay khi push branch bat ky | Push branch `feature/ci-pipeline`, xem GitHub Actions |
| Workflow chi build image khi event la `push` | Kiem tra `Build` job trong Actions |
| Docker Hub login bang GitHub Secrets | Step `Login to Docker Hub` pass |
| Image duoc push len Docker Hub | Step `Build and push Docker images` pass |
| Branch image tag bang short SHA | Docker Hub co tag trung voi `git rev-parse --short HEAD` |
| Branch `main` co tag `main` va short SHA | Merge vao `main`, xem Docker Hub |
| Release tag `v*` co image tag release | Push tag test nhu `v0.0.0-ci-test` |
| Manifest/doc commit khong gay CI loop | Thay doi `docs/`, `k8s/`, `argocd/` khong trigger service workflows |

## 2. Trang thai local hien tai

Branch local:

```powershell
git branch --show-current
```

Ket qua mong doi:

```text
feature/ci-pipeline
```

Mot so commit chinh cua task:

```powershell
git log --oneline --decorate --max-count=5
```

Can thay cac commit lien quan den CI:

```text
docs: add ci docker hub test guide
8fe3ddf7 docs: add ci docker hub handover
2c7db0c9 ci: use short sha tags for docker images
```

Lay short SHA de doi chieu Docker Hub:

```powershell
git rev-parse --short HEAD
```

Ket qua se la short SHA cua commit moi nhat tai thoi diem ban push, vi du:

```text
<short_commit_sha>
```

Neu co them commit moi truoc khi push, phai dung SHA moi nhat cua `HEAD`.

## 3. Test local truoc khi push

### 3.1. Kiem tra working tree

```powershell
git status --short --branch
```

Ket qua chap nhan duoc:

```text
## feature/ci-pipeline
?? DEVOPS_PROJECT_HANDOVER.md
```

File `DEVOPS_PROJECT_HANDOVER.md` la file untracked co san tu truoc, khong
thuoc task CI nay.

### 3.2. Kiem tra YAML workflow parse duoc

Chay lenh:

```powershell
@'
from pathlib import Path
import yaml
names = ['backoffice-bff','backoffice','cart','customer','inventory','media','order','product','sampledata','search','storefront-bff','storefront','tax']
for name in names:
    path = Path('.github/workflows') / f'{name}-ci.yaml'
    with path.open('r', encoding='utf-8') as fh:
        yaml.safe_load(fh)
print('YAML parsing passed for 13 DockerHub workflows')
'@ | python -
```

Ket qua mong doi:

```text
YAML parsing passed for 13 DockerHub workflows
```

### 3.3. Kiem tra contract workflow CI

Chay lenh:

```powershell
$files = @('backoffice-bff','backoffice','cart','customer','inventory','media','order','product','sampledata','search','storefront-bff','storefront','tax') | ForEach-Object { ".github\workflows\$_-ci.yaml" }
$failed = @()
foreach ($f in $files) {
  $text = Get-Content -LiteralPath $f -Raw -Encoding UTF8
  $checks = [ordered]@{
    push_all_branches = $text -match 'branches:\s*\["\*\*"\]'
    tag_v_star = $text -match 'tags:\s*\r?\n\s*-\s*"v\*"'
    workflow_dispatch = $text -match 'workflow_dispatch:'
    push_only_build = $text -match "if:\s*\$\{\{ github\.event_name == 'push' \}\}"
    dockerhub_login = $text -match 'docker/login-action@v3' -and $text -match 'secrets\.DOCKERHUB_USERNAME' -and $text -match 'secrets\.DOCKERHUB_TOKEN'
    dockerhub_image = $text -match 'images:\s*docker\.io/mingpham/yas-'
    short_sha_tag = $text -match 'type=sha,format=short,prefix='
    main_tag = $text -match "type=raw,value=main,enable=\$\{\{ github\.ref == 'refs/heads/main' \}\}"
    release_tag = $text -match 'type=ref,event=tag'
    push_true = $text -match 'push:\s*true'
  }
  foreach ($k in $checks.Keys) {
    if (-not $checks[$k]) { $failed += "$f missing $k" }
  }
}
if ($failed.Count) {
  $failed | ForEach-Object { Write-Error $_ }
  exit 1
} else {
  'All CI workflow contract checks passed for 13 DockerHub workflows.'
}
```

Ket qua mong doi:

```text
All CI workflow contract checks passed for 13 DockerHub workflows.
```

### 3.4. Kiem tra diff khong loi whitespace

```powershell
git diff --check origin/main..HEAD -- .github/workflows docs/ci-dockerhub-handover.md docs/ci-dockerhub-test-guide.md
```

Ket qua mong doi: khong in loi nao.

## 4. Test that tren GitHub Actions

Phan nay bat buoc push branch len GitHub vi Docker Hub secret chi co trong
GitHub Actions runtime.

### 4.1. Push branch

```powershell
git push -u origin feature/ci-pipeline
```

Sau khi push, vao GitHub repo:

```text
Actions
```

Vi push nay co thay doi cac file workflow, 13 service workflows du kien se
duoc trigger.

### 4.2. Kiem tra tung workflow

Mo tung workflow service va kiem tra:

| Job/step | Ket qua can thay |
| --- | --- |
| `Test` job | Pass |
| `Build` job | Chay, khong bi skip |
| `Login to Docker Hub` | Pass |
| `Extract Docker metadata` | Co tag short SHA |
| `Build and push Docker images` | Pass |

Quan trong: khong dung `workflow_dispatch` de test Docker push, vi cac workflow
hien cau hinh `Build` job chi push image khi event la `push`.

## 5. Test Docker Hub tag

Sau khi GitHub Actions pass, lay short SHA cua commit da push:

```powershell
git rev-parse --short HEAD
```

Vao Docker Hub namespace `mingpham`, kiem tra tag trong cac repository:

| Service | Docker Hub repository |
| --- | --- |
| product | `mingpham/yas-product` |
| cart | `mingpham/yas-cart` |
| tax | `mingpham/yas-tax` |
| storefront-bff | `mingpham/yas-storefront-bff` |
| storefront-ui | `mingpham/yas-storefront-ui` |
| order | `mingpham/yas-order` |
| customer | `mingpham/yas-customer` |
| inventory | `mingpham/yas-inventory` |
| media | `mingpham/yas-media` |
| search | `mingpham/yas-search` |
| backoffice-bff | `mingpham/yas-backoffice-bff` |
| backoffice-ui | `mingpham/yas-backoffice-ui` |
| sampledata | `mingpham/yas-sampledata` |

Moi repo phai co tag bang short SHA, vi du:

```text
docker.io/mingpham/yas-tax:<short_commit_sha>
```

## 6. Test tag `main`

Chi thuc hien sau khi PR duoc merge vao `main`.

Sau khi merge, GitHub Actions tren `main` phai push:

```text
docker.io/mingpham/yas-tax:main
docker.io/mingpham/yas-tax:<short_main_commit_sha>
```

Lap lai voi cac service can chung minh trong demo.

## 7. Test release tag

Chi lam khi nhom dong y tao tag test.

```powershell
git checkout main
git pull
git tag v0.0.0-ci-test
git push origin v0.0.0-ci-test
```

Kiem tra GitHub Actions va Docker Hub co tag:

```text
v0.0.0-ci-test
```

Neu muon xoa tag test tren Git:

```powershell
git push --delete origin v0.0.0-ci-test
git tag -d v0.0.0-ci-test
```

Tag tren Docker Hub neu da push thi xoa thu cong tren Docker Hub neu can.

## 8. Evidence can chup cho bao cao

Chup cac man hinh sau:

1. GitHub Actions list co workflow CI pass.
2. Chi tiet workflow: `Test` job pass.
3. Chi tiet workflow: `Build` job pass.
4. Step `Extract Docker metadata` hien short SHA tag.
5. Step `Build and push Docker images` pass.
6. Docker Hub repository co tag short SHA.
7. Sau khi merge main: Docker Hub co tag `main`.
8. Neu test release: Docker Hub co tag `v0.0.0-ci-test` hoac tag release that.

## 9. Cach ket luan task da xong

Co the ket luan task CI/DockerHub da xong khi:

- Local checks o muc 3 deu pass.
- Push branch len GitHub va 13 workflow can test pass.
- Docker Hub co image tag short SHA.
- Sau khi merge vao `main`, Docker Hub co tag `main`.
- Neu nhom demo release, tag `v*` tao image tag release thanh cong.

## 10. Loi thuong gap

| Loi | Nguyen nhan hay gap | Cach xu ly |
| --- | --- | --- |
| `Login to Docker Hub` fail | Secret sai ten hoac token sai | Kiem tra `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN` |
| `denied: requested access` | Token khong co quyen push vao `mingpham` | Dung token co quyen read/write voi namespace nay |
| `Build` job bi skip | Dang chay `workflow_dispatch` | Test bang `push`, khong test bang manual dispatch |
| Khong thay tag short SHA | Workflow chua co `format=short` tren branch da push | Kiem tra file workflow tren GitHub branch |
| Maven/npm test fail | Loi test/build cua service | Mo log step fail dau tien va sua theo service |
| Docker Hub chi co tag `main` | Dang xem run tren branch `main` | Branch thuong can doi chieu tag short SHA |
