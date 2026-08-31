#!/usr/bin/env python3
"""Configure Microsoft Entra ID SSO for a Teams app registration via Azure CLI.

Suit https://learn.microsoft.com/fr-fr/microsoft-copilot-studio/configure-sso-teams.
Conforme à l'ADR-600 (configuration en couches) et à l'ADR-601 (nomenclature) :
aucun secret n'est requis, l'authentification az repose sur la session `az login`
de l'opérateur.
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
import uuid
from pathlib import Path

PROJECT_DIR = Path(__file__).resolve().parent.parent
ENV_DIR = PROJECT_DIR / "env"

GRAPH_API_ID = "00000003-0000-0000-c000-000000000000"
GRAPH_OPENID_PERMISSION_ID = "37f7f235-527c-4136-accd-4a02d197296e"
GRAPH_PROFILE_PERMISSION_ID = "14dad69e-099b-42c9-810b-d002981feec1"
# Identifiants clients Teams fournis par Microsoft, identiques pour tous les tenants.
TEAMS_DESKTOP_MOBILE_CLIENT_ID = "1fec8e78-bce4-4aaf-ab1b-5451cc387264"
TEAMS_WEB_CLIENT_ID = "5e3ce6c0-2b1f-4285-8d4b-75ee78787346"
DEFAULT_SCOPE_NAME = "access_as_user"


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


def require(values: dict[str, str], name: str) -> str:
    value = values.get(name)
    if not value:
        raise RuntimeError(f"Variable requise absente : {name}")
    return value


def run_az(args: list[str], *, dry_run: bool, capture: bool = True) -> str:
    command = ["az", *args]
    if dry_run:
        print(f"[dry-run] {' '.join(command)}")
        return ""
    completed = subprocess.run(command, check=True, capture_output=capture, text=True)
    return completed.stdout.strip() if capture else ""


def require_az() -> None:
    if not shutil.which("az"):
        raise RuntimeError("Azure CLI (az) est introuvable ; installez-la avant d'exécuter ce script.")


def ensure_logged_in(dry_run: bool) -> None:
    if dry_run:
        return
    result = subprocess.run(["az", "account", "show"], capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError("Aucune session az active ; exécutez 'az login' avant ce script.")


def fetch_application(app_id: str, dry_run: bool) -> dict:
    if dry_run:
        return {"id": "<object-id>", "api": {"oauth2PermissionScopes": [], "knownClientApplications": []}}
    raw = run_az(["ad", "app", "show", "--id", app_id], dry_run=False)
    return json.loads(raw)


def build_scope_patch(application: dict, scope_name: str) -> dict:
    api = application.get("api") or {}
    scopes = list(api.get("oauth2PermissionScopes") or [])
    if not any(scope.get("value") == scope_name for scope in scopes):
        scopes.append(
            {
                "id": str(uuid.uuid4()),
                "adminConsentDescription": "Allows the app to sign the user in.",
                "adminConsentDisplayName": scope_name,
                "userConsentDescription": "Allows the app to sign you in.",
                "userConsentDisplayName": scope_name,
                "value": scope_name,
                "type": "User",
                "isEnabled": True,
            }
        )
    known_clients = set(api.get("knownClientApplications") or [])
    known_clients.update({TEAMS_DESKTOP_MOBILE_CLIENT_ID, TEAMS_WEB_CLIENT_ID})
    return {"api": {"oauth2PermissionScopes": scopes, "knownClientApplications": sorted(known_clients)}}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app-id", help="Remplace KB_GENAI_BUILDER_TEAMS_APP_ID")
    parser.add_argument("--scope-name", help="Remplace KB_GENAI_BUILDER_TEAMS_SSO_SCOPE_NAME")
    parser.add_argument(
        "--grant-admin-consent",
        action="store_true",
        help="Accorder le consentement administrateur à l'échelle du tenant",
    )
    parser.add_argument("--dry-run", action="store_true", help="Afficher les commandes az sans les exécuter")
    args = parser.parse_args()

    try:
        require_az()
        ensure_logged_in(args.dry_run)

        values = load_configuration()
        app_id = args.app_id or require(values, "KB_GENAI_BUILDER_TEAMS_APP_ID")
        scope_name = args.scope_name or values.get("KB_GENAI_BUILDER_TEAMS_SSO_SCOPE_NAME") or DEFAULT_SCOPE_NAME
        identifier_uri = f"api://botid-{app_id}"

        application = fetch_application(app_id, args.dry_run)
        object_id = application["id"]

        run_az(["ad", "app", "update", "--id", app_id, "--identifier-uris", identifier_uri], dry_run=args.dry_run, capture=False)

        run_az(
            [
                "ad", "app", "permission", "add",
                "--id", app_id,
                "--api", GRAPH_API_ID,
                "--api-permissions",
                f"{GRAPH_OPENID_PERMISSION_ID}=Scope",
                f"{GRAPH_PROFILE_PERMISSION_ID}=Scope",
            ],
            dry_run=args.dry_run,
            capture=False,
        )

        patch_body = build_scope_patch(application, scope_name)
        run_az(
            [
                "rest",
                "--method", "PATCH",
                "--uri", f"https://graph.microsoft.com/v1.0/applications/{object_id}",
                "--body", json.dumps(patch_body),
                "--headers", "Content-Type=application/json",
            ],
            dry_run=args.dry_run,
            capture=False,
        )

        if args.grant_admin_consent:
            run_az(["ad", "app", "permission", "admin-consent", "--id", app_id], dry_run=args.dry_run, capture=False)

        token_exchange_url = f"{identifier_uri}/{scope_name}"
        print("Configuration SSO terminée.")
        print(f"  ID client de l'application AAD : {app_id}")
        print(f"  URI de la ressource             : {identifier_uri}")
        print(f"  URL d'échange de jeton (Teams)   : {token_exchange_url}")
        print("Reportez ces valeurs dans Copilot Studio > Sécurité > Authentification, puis republiez l'agent.")
        return 0
    except (RuntimeError, OSError, subprocess.CalledProcessError, json.JSONDecodeError, KeyError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
