# CI Docker Hub Handover

## Scope

The CI image build scope covers the internal YAS services that have source
directories and Dockerfiles in this repository:

| Service | Workflow | Docker image |
| --- | --- | --- |
| backoffice-bff | `.github/workflows/backoffice-bff-ci.yaml` | `docker.io/mingpham/yas-backoffice-bff` |
| backoffice-ui | `.github/workflows/backoffice-ci.yaml` | `docker.io/mingpham/yas-backoffice-ui` |
| cart | `.github/workflows/cart-ci.yaml` | `docker.io/mingpham/yas-cart` |
| customer | `.github/workflows/customer-ci.yaml` | `docker.io/mingpham/yas-customer` |
| inventory | `.github/workflows/inventory-ci.yaml` | `docker.io/mingpham/yas-inventory` |
| media | `.github/workflows/media-ci.yaml` | `docker.io/mingpham/yas-media` |
| order | `.github/workflows/order-ci.yaml` | `docker.io/mingpham/yas-order` |
| product | `.github/workflows/product-ci.yaml` | `docker.io/mingpham/yas-product` |
| sampledata | `.github/workflows/sampledata-ci.yaml` | `docker.io/mingpham/yas-sampledata` |
| search | `.github/workflows/search-ci.yaml` | `docker.io/mingpham/yas-search` |
| storefront-bff | `.github/workflows/storefront-bff-ci.yaml` | `docker.io/mingpham/yas-storefront-bff` |
| storefront-ui | `.github/workflows/storefront-ci.yaml` | `docker.io/mingpham/yas-storefront-ui` |
| tax | `.github/workflows/tax-ci.yaml` | `docker.io/mingpham/yas-tax` |

`swagger-ui` uses the upstream image `swaggerapi/swagger-ui` in the Kubernetes
chart and does not have an application source directory or Dockerfile in this
repository.

## Required GitHub Secrets

Repository Actions secrets:

| Secret | Purpose |
| --- | --- |
| `DOCKERHUB_USERNAME` | Docker Hub account with push permission to the `mingpham` namespace |
| `DOCKERHUB_TOKEN` | Docker Hub personal access token with read/write permission |

## Tagging Contract

Each Docker Hub workflow publishes:

| Event | Tags |
| --- | --- |
| Push to any branch | Full commit SHA |
| Push to `main` | `main` and full commit SHA |
| Push tag `v*` | Release tag, for example `v1.2.3` |

The workflows use `docker/metadata-action` with:

```yaml
tags: |
  type=sha,format=long,prefix=
  type=raw,value=main,enable=${{ github.ref == 'refs/heads/main' }}
  type=ref,event=tag
```

## Loop Prevention

Service CI workflows use `paths` filters for their own service directory,
their own workflow file, and shared build inputs. Manifest-only commits under
`k8s/`, `argocd/`, `istio/`, or `docs/` do not trigger these Docker image
workflows.

## Local Validation

The following checks were run locally on branch `feature/ci-pipeline`:

- YAML parsing for all 13 Docker Hub workflow files.
- Static workflow contract check for branch trigger, release tag trigger,
  workflow dispatch, Docker Hub login, full SHA tags, `main` tag, release tag,
  and `push: true`.
- `git diff --check HEAD -- .github/workflows`.

Full GitHub Actions image build and Docker Hub push validation must be run after
the branch is pushed to GitHub, because the local machine does not expose the
GitHub Actions runtime secrets and Docker Desktop is not currently running.
