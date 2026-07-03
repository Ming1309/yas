# ArgoCD GitOps Test Guide

Use this guide to verify the GitOps/ArgoCD task.

## 1. Local Static Validation

Run from the repository root:

```powershell
git branch --show-current
git status --short --branch
```

Expected branch:

```text
feature/gitops-argocd
```

Validate YAML parsing:

```powershell
@'
from pathlib import Path
import yaml
paths = [
    *Path('.github/workflows').glob('*gitops*.yaml'),
    Path('.github/workflows/developer-build.yaml'),
    Path('.github/workflows/delete-developer-deploy.yaml'),
    *Path('argocd').rglob('*.yaml'),
    *Path('k8s/environments').rglob('*.yaml'),
]
for path in paths:
    with path.open('r', encoding='utf-8') as fh:
        yaml.safe_load(fh)
print(f'YAML parsing passed for {len(paths)} files')
'@ | python -
```

Validate the helper script:

```powershell
python .github/scripts/gitops_update_images.py staging --all --tag v0.0.0-test
git diff -- k8s/environments/staging
git checkout -- k8s/environments/staging
```

Expected: staging values are changed to `v0.0.0-test`, then reverted.

Validate dev image update for one service:

```powershell
python .github/scripts/gitops_update_images.py dev --service tax --tag 1234567890abcdef1234567890abcdef12345678
git diff -- k8s/environments/dev/tax.values.yaml
git checkout -- k8s/environments/dev
```

Expected: only `dev/tax.values.yaml` changes to the supplied SHA, then is
reverted.

Validate developer branch mapping with a known full SHA:

```powershell
$sha = git rev-parse HEAD
$json = '{"tax":"' + $sha + '","product":"main","cart":"main","customer":"main","inventory":"main","media":"main","order":"main","search":"main","sampledata":"main","storefront-bff":"main","storefront-ui":"main","backoffice-bff":"main","backoffice-ui":"main"}'
$json | Set-Content -LiteralPath service-branches.test.json -Encoding UTF8
python .github/scripts/gitops_update_images.py developer --service-branches-file service-branches.test.json
git diff -- k8s/environments/developer/tax.values.yaml
git checkout -- k8s/environments/developer
Remove-Item -LiteralPath service-branches.test.json
```

Expected: only selected developer values move away from `main`, then are
reverted.

## 2. Test ArgoCD Root Applications

Apply or verify root Applications:

```bash
kubectl apply -f argocd/root/yas-dev.yaml
kubectl apply -f argocd/root/yas-staging.yaml
kubectl apply -f argocd/root/yas-developer.yaml
```

Check ArgoCD:

```bash
kubectl get applications -n argocd
```

Expected:

```text
yas-dev
yas-staging
yas-developer
```

Each root app should show auto sync, prune, and self-heal in the ArgoCD UI.

## 3. Test developer_build

Before running, make sure the selected branch image exists on DockerHub. Example
for a branch named `dev_tax_service`:

```text
docker.io/mingpham/yas-tax:<full_commit_sha_of_dev_tax_service>
```

Run GitHub Actions workflow:

```text
Actions -> developer_build -> Run workflow
```

Inputs:

```text
tax_branch=dev_tax_service
all other branches=main
```

Expected Git result:

```text
k8s/environments/developer/tax.values.yaml
```

has:

```yaml
tag: <full_commit_sha_of_dev_tax_service>
```

Expected ArgoCD/Kubernetes result:

```bash
kubectl -n yas-developer get deploy tax -o jsonpath="{.spec.template.spec.containers[0].image}"
```

Expected image:

```text
docker.io/mingpham/yas-tax:<full_commit_sha_of_dev_tax_service>
```

## 4. Test dev environment

After this branch is merged and a service change lands on `main`,
the service CI workflow should run first. When it succeeds,
`gitops_dev_update` should commit a GitOps update for that same service.

Expected:

```text
k8s/environments/dev/<service>.values.yaml
```

has:

```yaml
tag: <full_main_commit_sha>
```

ArgoCD syncs the related `yas-dev-<service>` Application.

## 5. Test staging environment

Create or use a release tag:

```bash
git tag v0.0.0-gitops-test
git push origin v0.0.0-gitops-test
```

Expected workflow:

```text
gitops_staging_promote
```

Expected Git result:

```text
k8s/environments/staging/*.values.yaml
```

DockerHub-built services have:

```yaml
tag: v0.0.0-gitops-test
```

ArgoCD syncs the `yas-staging-*` Applications.

Clean up the Git tag if it was only for testing:

```bash
git push --delete origin v0.0.0-gitops-test
git tag -d v0.0.0-gitops-test
```

## 6. Test delete_developer_deploy

Run GitHub Actions workflow:

```text
Actions -> delete_developer_deploy -> Run workflow
confirm=delete
```

Expected Git result:

```text
argocd/applications/developer/*.yaml
```

is moved to:

```text
argocd/applications/developer-disabled/*.yaml
```

Expected ArgoCD result:

```bash
kubectl get applications -n argocd | grep yas-developer-
```

No child developer Applications should remain after ArgoCD sync/prune.

Expected Kubernetes result:

```bash
kubectl get deploy -n yas-developer
```

Developer service deployments should be removed by ArgoCD pruning.

## 7. Evidence for Report

Capture these screenshots:

1. ArgoCD root app `yas-dev` with auto sync/prune/self-heal.
2. ArgoCD root app `yas-staging` with auto sync/prune/self-heal.
3. ArgoCD root app `yas-developer` with auto sync/prune/self-heal.
4. `developer_build` workflow inputs and success result.
5. Git commit produced by `developer_build`.
6. ArgoCD synced developer service with the selected image SHA.
7. `delete_developer_deploy` workflow success.
8. ArgoCD/Kubernetes evidence that developer resources were pruned.
