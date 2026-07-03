# ArgoCD GitOps Handover

## Scope

This work implements the GitOps/ArgoCD part of Project 2:

- ArgoCD app-of-apps for `yas-dev`, `yas-staging`, and `yas-developer`.
- Auto sync, prune, and self-heal for root and service Applications.
- GitHub Actions workflows that update Git manifests instead of applying
  Kubernetes resources directly.
- `developer_build` workflow for branch-specific developer deployments.
- `delete_developer_deploy` workflow for resetting the shared developer environment to `main`.

## Existing ArgoCD Structure

Root Applications:

| Environment | Root Application | Child Application path |
| --- | --- | --- |
| dev | `argocd/root/yas-dev.yaml` | `argocd/applications/dev` on `main` |
| staging | `argocd/root/yas-staging.yaml` | `argocd/applications/staging` on `gitops-staging` |
| developer | `argocd/root/yas-developer.yaml` | `argocd/applications/developer` on `gitops-developer` |

Each service Application uses the service Helm chart in `k8s/charts/<service>`
and the environment-specific values file in
`k8s/environments/<environment>/<service>.values.yaml`.

## GitOps Workflows

| Workflow | Purpose |
| --- | --- |
| `.github/workflows/gitops-dev-update.yaml` | After a service CI workflow succeeds on `main`, update that dev service image tag to the short SHA on `main` using `ADMIN_PAT`. |
| `.github/workflows/gitops-staging-promote.yaml` | On release tag `v*`, promote staging service image tags to that release tag on `gitops-staging`. |
| `.github/workflows/developer-build.yaml` | Manual `developer_build` workflow for branch-specific `yas-developer` deployments on `gitops-developer`. |
| `.github/workflows/delete-developer-deploy.yaml` | Manual reset workflow that returns all developer service image tags to `main`. |

All workflows commit manifest changes back to Git. They do not run
`kubectl apply` or deploy directly to Kubernetes.

## Image Tag Rules

| Environment | Tag source |
| --- | --- |
| `yas-dev` | Short SHA from the successful service CI run on `main` |
| `yas-staging` | Release tag, for example `v1.2.3` |
| `yas-developer` | `main` for default services, short SHA of the selected branch for services under test |

The helper script `.github/scripts/gitops_update_images.py` updates the
environment values files consistently for backend charts and UI charts.

## Developer Build Example

For the demo case:

```text
tax_branch=dev_tax_service
all other service inputs=main
```

`developer_build` resolves `origin/dev_tax_service` to its short SHA and
updates:

```text
k8s/environments/developer/tax.values.yaml
```

The tax image tag becomes the branch short SHA, while other services stay on
`main`. ArgoCD then syncs the `yas-developer-tax` Application and rolls out the
selected image.

## Developer Delete Flow

`delete_developer_deploy` does not remove the shared `yas-developer`
namespace and does not remove ArgoCD Applications. It resets all developer
service image tags in:

```text
k8s/environments/developer/*.values.yaml
```

back to `main`. ArgoCD then syncs `yas-developer` back to the baseline image
set while the environment stays available for the team.

## Notes

`swagger-ui` uses the upstream image `swaggerapi/swagger-ui`. It is managed by
ArgoCD as an Application, but it is not updated by DockerHub image promotion
workflows because this repository does not build a `yas-swagger-ui` image.
