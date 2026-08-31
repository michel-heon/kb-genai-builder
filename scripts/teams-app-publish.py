#!/usr/bin/env python3
"""Publish a Teams app package to the organization's Teams app catalog via Azure CLI.

Suit https://learn.microsoft.com/fr-ca/microsoft-copilot-studio/publication-add-bot-to-microsoft-teams
(section « Télécharger le manifeste de l'application Teams pour un agent »), en
appelant Microsoft Graph (`appCatalogs/teamsApps`) via `az rest`.
Conforme à l'ADR-600 (aucun secret requis, `az login` porte l'authentification) et
à l'ADR-602 (publication distante explicite, avec mode `--dry-run`).
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

PROJECT_DIR = Path(__file__).resolve().parent.parent
ENV_DIR = PROJECT_DIR / "env"
GRAPH_BASE = "https://graph.microsoft.com/v1.0"


def parse_env_file(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.is_file():
        return values
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        name, _, raw_value = line.partition("=")
        values[name.strip()] = raw_value.strip().strip("'\"")
    return values


def load_configuration() -> dict[str, str]:
    values: dict[str, str] = {}
    for path in (ENV_DIR / "default.env", ENV_DIR / ".env.local", ENV_DIR / ".env.user.local"):
        values.update(parse_env_file(path))
    for name in list(values):
        if name in os.environ:
            values[name] = os.environ[name]
    return values


def require_az() -> None:
    if not shutil.which("az"):
        raise RuntimeError("Azure CLI (az) est introuvable ; installez-la avant d'exécuter ce script.")


def ensure_logged_in(dry_run: bool) -> None:
    if dry_run:
        return
    result = subprocess.run(["az", "account", "show"], capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError("Aucune session az active ; exécutez 'az login' avant ce script.")


def resolve_package_path(value: str) -> Path:
    path = Path(value)
    return path if path.is_absolute() else PROJECT_DIR / path


def app_exists_in_catalog(app_id: str) -> bool:
    result = subprocess.run(
        ["az", "rest", "--method", "GET", "--uri", f"{GRAPH_BASE}/appCatalogs/teamsApps/{app_id}"],
        capture_output=True,
        text=True,
    )
    return result.returncode == 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app-id", help="Remplace KB_GENAI_BUILDER_TEAMS_APP_ID")
    parser.add_argument("--package", help="Remplace KB_GENAI_BUILDER_TEAMS_APP_PACKAGE_ZIP")
    parser.add_argument("--dry-run", action="store_true", help="Afficher l'appel Graph sans publier")
    args = parser.parse_args()

    try:
        require_az()
        ensure_logged_in(args.dry_run)

        values = load_configuration()
        app_id = args.app_id or values.get("KB_GENAI_BUILDER_TEAMS_APP_ID")
        if not app_id:
            raise RuntimeError("Variable requise absente : KB_GENAI_BUILDER_TEAMS_APP_ID")
        package_value = args.package or values.get("KB_GENAI_BUILDER_TEAMS_APP_PACKAGE_ZIP")
        if not package_value:
            raise RuntimeError("Variable requise absente : KB_GENAI_BUILDER_TEAMS_APP_PACKAGE_ZIP")
        package_path = resolve_package_path(package_value)

        if not args.dry_run and not package_path.is_file():
            raise RuntimeError(f"Package Teams introuvable : {package_path}")

        existing = False if args.dry_run else app_exists_in_catalog(app_id)
        if existing:
            method, uri = "PUT", f"{GRAPH_BASE}/appCatalogs/teamsApps/{app_id}/appDefinition"
            print(f"Mise à jour de l'application Teams existante {app_id}.")
        else:
            method, uri = "POST", f"{GRAPH_BASE}/appCatalogs/teamsApps"
            print(f"Publication d'une nouvelle application Teams (id manifeste attendu : {app_id}).")

        command = [
            "az", "rest",
            "--method", method,
            "--uri", uri,
            "--headers", "Content-Type=application/zip",
            "--body", f"@{package_path}",
        ]
        if args.dry_run:
            print(f"[dry-run] {' '.join(command)}")
        else:
            subprocess.run(command, check=True)
            print("Publication Teams terminée.")
        return 0
    except (RuntimeError, OSError, subprocess.CalledProcessError, json.JSONDecodeError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
