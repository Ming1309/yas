#!/usr/bin/env python3
"""Update YAS Helm environment image tags for GitOps workflows."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

import yaml


REPO_ROOT = Path(__file__).resolve().parents[2]
ZERO_SHA = "0" * 40

SERVICES = {
    "backoffice-bff": {
        "source_dir": "backoffice-bff",
        "workflow": ".github/workflows/backoffice-bff-ci.yaml",
    },
    "backoffice-ui": {
        "source_dir": "backoffice",
        "workflow": ".github/workflows/backoffice-ci.yaml",
    },
    "cart": {
        "source_dir": "cart",
        "workflow": ".github/workflows/cart-ci.yaml",
    },
    "customer": {
        "source_dir": "customer",
        "workflow": ".github/workflows/customer-ci.yaml",
    },
    "inventory": {
        "source_dir": "inventory",
        "workflow": ".github/workflows/inventory-ci.yaml",
    },
    "media": {
        "source_dir": "media",
        "workflow": ".github/workflows/media-ci.yaml",
    },
    "order": {
        "source_dir": "order",
        "workflow": ".github/workflows/order-ci.yaml",
    },
    "product": {
        "source_dir": "product",
        "workflow": ".github/workflows/product-ci.yaml",
    },
    "sampledata": {
        "source_dir": "sampledata",
        "workflow": ".github/workflows/sampledata-ci.yaml",
    },
    "search": {
        "source_dir": "search",
        "workflow": ".github/workflows/search-ci.yaml",
    },
    "storefront-bff": {
        "source_dir": "storefront-bff",
        "workflow": ".github/workflows/storefront-bff-ci.yaml",
    },
    "storefront-ui": {
        "source_dir": "storefront",
        "workflow": ".github/workflows/storefront-ci.yaml",
    },
    "tax": {
        "source_dir": "tax",
        "workflow": ".github/workflows/tax-ci.yaml",
    },
}

SHARED_BUILD_INPUTS = {
    "pom.xml",
    ".github/workflows/actions/action.yaml",
}


def run_git(args: list[str]) -> str:
    result = subprocess.run(
        ["git", *args],
        cwd=REPO_ROOT,
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout.strip()


def changed_files(before: str, after: str) -> list[str]:
    before = before.strip()
    after = after.strip()
    if not after:
        raise SystemExit("Missing --after commit for changed service detection")

    if not before or before == ZERO_SHA:
        output = run_git(["diff-tree", "--no-commit-id", "--name-only", "-r", after])
    else:
        output = run_git(["diff", "--name-only", before, after])
    return [line.replace("\\", "/") for line in output.splitlines() if line.strip()]


def services_from_changed_files(files: list[str]) -> list[str]:
    selected: set[str] = set()
    if any(path in SHARED_BUILD_INPUTS for path in files):
        selected.update(SERVICES)

    for service, meta in SERVICES.items():
        source_prefix = f"{meta['source_dir']}/"
        workflow = meta["workflow"]
        if any(path == workflow or path.startswith(source_prefix) for path in files):
            selected.add(service)

    return sorted(selected)


def values_path(environment: str, service: str) -> Path:
    return REPO_ROOT / "k8s" / "environments" / environment / f"{service}.values.yaml"


def set_image_tag(data: dict, tag: str) -> None:
    if isinstance(data.get("backend"), dict) and isinstance(data["backend"].get("image"), dict):
        data["backend"]["image"]["tag"] = tag
        return
    if isinstance(data.get("ui"), dict) and isinstance(data["ui"].get("image"), dict):
        data["ui"]["image"]["tag"] = tag
        return
    if isinstance(data.get("image"), dict):
        data["image"]["tag"] = tag
        return
    raise ValueError("Could not find an image tag field")


def update_service_tag(environment: str, service: str, tag: str) -> bool:
    path = values_path(environment, service)
    if not path.exists():
        raise FileNotFoundError(path)

    data = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
    before = yaml.safe_dump(data, sort_keys=False)
    set_image_tag(data, tag)
    after = yaml.safe_dump(data, sort_keys=False)
    if before == after:
        print(f"{environment}/{service}: already at tag {tag}")
        return False

    path.write_text(after, encoding="utf-8")
    print(f"{environment}/{service}: set image tag to {tag}")
    return True


def resolve_branch_tag(branch: str) -> str:
    branch = branch.strip()
    if not branch or branch == "main":
        return "main"
    if re.fullmatch(r"[0-9a-fA-F]{40}", branch):
        return branch.lower()

    ref = branch
    if ref.startswith("origin/"):
        ref = ref.removeprefix("origin/")
    if ref.startswith("refs/heads/"):
        ref = ref.removeprefix("refs/heads/")

    output = run_git(["ls-remote", "--heads", "origin", ref])
    for line in output.splitlines():
        sha, remote_ref = line.split(None, 1)
        if remote_ref == f"refs/heads/{ref}":
            return sha
    raise SystemExit(f"Branch not found on origin: {branch}")


def update_developer(service_branches_json: str) -> int:
    service_branches = json.loads(service_branches_json)
    changed = 0
    for service in sorted(SERVICES):
        branch = service_branches.get(service, "main") or "main"
        tag = resolve_branch_tag(branch)
        changed += int(update_service_tag("developer", service, tag))
    return changed


def update_fixed_tag(environment: str, services: list[str], tag: str) -> int:
    changed = 0
    for service in services:
        changed += int(update_service_tag(environment, service, tag))
    return changed


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("environment", choices=["dev", "staging", "developer"])
    parser.add_argument("--tag")
    parser.add_argument("--all", action="store_true")
    parser.add_argument("--service", action="append", choices=sorted(SERVICES))
    parser.add_argument("--before", default="")
    parser.add_argument("--after", default="")
    parser.add_argument("--service-branches-json", default="")
    parser.add_argument("--service-branches-file", default="")
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    if args.all:
        if not args.tag:
            raise SystemExit("--tag is required with --all")
        changed = update_fixed_tag(args.environment, sorted(SERVICES), args.tag)
    elif args.service:
        if not args.tag:
            raise SystemExit("--tag is required with --service")
        changed = update_fixed_tag(args.environment, sorted(args.service), args.tag)
    elif args.environment == "developer":
        if args.service_branches_file:
            service_branches_json = (REPO_ROOT / args.service_branches_file).read_text(
                encoding="utf-8-sig"
            )
        else:
            service_branches_json = args.service_branches_json
        if not service_branches_json:
            raise SystemExit(
                "--service-branches-json, --service-branches-file, --all, or --service is required for developer"
            )
        changed = update_developer(service_branches_json)
    else:
        if not args.tag:
            raise SystemExit("--tag is required for changed service updates")
        files = changed_files(args.before, args.after)
        services = services_from_changed_files(files)
        if not services:
            print("No DockerHub service changes detected; no values updated.")
            return 0
        print("Changed DockerHub services: " + ", ".join(services))
        changed = update_fixed_tag(args.environment, services, args.tag)

    print(f"Updated {changed} values file(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
