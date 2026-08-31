#!/usr/bin/env python3
"""Validate layered environment configuration and generate consumer files."""

from __future__ import annotations

import argparse
import os
import shlex
import shutil
import subprocess
import sys
from pathlib import Path

PROJECT_DIR = Path(__file__).resolve().parent.parent
ENV_DIR = PROJECT_DIR / "env"
GENERATED_DIR = ENV_DIR / "generated"
ALLOWED_READ_ONLY = {"true", "false"}
SECRET_SUFFIXES = ("_API_KEY", "_PASSWORD", "_SECRET", "_TOKEN")


def parse_env_file(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[7:].lstrip()
        if "=" not in line:
            continue
        name, raw_value = line.split("=", 1)
        name = name.strip()
        if not name or not name.replace("_", "").isalnum():
            continue
        try:
            value_parts = shlex.split(raw_value, comments=True, posix=True)
        except ValueError as error:
            raise ValueError(f"Configuration illisible dans {path}: {error}") from error
        values[name] = value_parts[0] if value_parts else ""
    return values


def tool_version(command: str, *args: str) -> str:
    completed = subprocess.run([command, *args], check=True, capture_output=True, text=True)
    return completed.stdout.strip().splitlines()[0].split()[-1]


def require_command(command: str) -> str:
    resolved = shutil.which(command)
    if not resolved:
        raise RuntimeError(f"Prérequis introuvable : {command}")
    return resolved


def validate_tools() -> None:
    require_command("bash")
    require_command("python3")
    require_command("make")
    require_command("mvn")
    java = require_command("java")
    version = subprocess.run([java, "-version"], check=True, capture_output=True, text=True).stderr
    version_value = next((line.split('"')[1] for line in version.splitlines() if 'version "' in line), "")
    major = int(version_value.split(".")[0]) if version_value else 0
    if major < 21:
        raise RuntimeError(f"Java 21 ou supérieur est requis, version détectée : {version_value}")


def resolve_path(value: str) -> str:
    path = Path(value)
    return str(path if path.is_absolute() else PROJECT_DIR / path)


def load_configuration(environment_name: str) -> tuple[dict[str, str], list[Path]]:
    default_config = ENV_DIR / "default.env"
    if not default_config.is_file():
        raise RuntimeError(f"Configuration par défaut absente : {default_config}")

    if environment_name == "default":
        config_paths = [default_config, ENV_DIR / ".env.local", ENV_DIR / ".env.user.local"]
    else:
        profile = ENV_DIR / f".env.{environment_name}"
        user_profile = ENV_DIR / f".env.user.{environment_name}"
        if not profile.is_file():
            raise RuntimeError(f"Environnement absent : {profile}")
        if not user_profile.is_file():
            raise RuntimeError(f"Configuration utilisateur absente : {user_profile}")
        config_paths = [default_config, profile, user_profile]

    values: dict[str, str] = {}
    for path in config_paths:
        if path.is_file():
            values.update(parse_env_file(path))

    for name in list(values):
        if name in os.environ:
            values[name] = os.environ[name]
    return values, config_paths


def write_generated(values: dict[str, str]) -> None:
    GENERATED_DIR.mkdir(parents=True, exist_ok=True)
    java = require_command("java")
    java_version = subprocess.run([java, "-version"], check=True, capture_output=True, text=True).stderr
    version_value = next((line.split('"')[1] for line in java_version.splitlines() if 'version "' in line), "")

    java_properties = GENERATED_DIR / "java.properties"
    property_lines = [
        "# Généré par scripts/environment-bootstrap.py. Ne pas modifier manuellement.",
        f"kb.genai.builder.java.bin={java}",
        f"kb.genai.builder.java.version={version_value}",
    ]
    for name in sorted(values):
        if name.startswith("KB_GENAI_BUILDER_"):
            property_lines.append(f"{name}={values[name]}")
    java_properties.write_text("\n".join(property_lines) + "\n", encoding="utf-8")
    java_properties.chmod(0o600)

    maven_lines = ["# Généré par scripts/environment-bootstrap.py. Ne pas modifier manuellement."]
    for name in sorted(values):
        if name.startswith("KB_GENAI_BUILDER_"):
            property_name = f"kb.genai.builder.{name[len('KB_GENAI_BUILDER_'):].lower()}"
            maven_lines.append(f"{property_name}={values[name]}")
    maven_file = GENERATED_DIR / "maven.properties"
    maven_file.write_text("\n".join(maven_lines) + "\n", encoding="utf-8")
    maven_file.chmod(0o600)

    make_lines = ["# Généré par scripts/environment-bootstrap.py. Ne pas modifier manuellement."]
    for name in sorted(values):
        if not name.startswith("KB_GENAI_BUILDER_"):
            continue
        make_lines.append(f"{name} := {values[name]}")
    make_file = GENERATED_DIR / "make.mk"
    make_file.write_text("\n".join(make_lines) + "\n", encoding="utf-8")
    make_file.chmod(0o600)
    (GENERATED_DIR / "make-secrets.mk").unlink(missing_ok=True)

    python_lines = ["# Généré par scripts/environment-bootstrap.py. Ne pas modifier manuellement."]
    for name in sorted(values):
        if name.startswith("KB_GENAI_BUILDER_"):
            python_lines.append(f"{name} = {values[name]!r}")
    python_file = GENERATED_DIR / "python.py"
    python_file.write_text("\n".join(python_lines) + "\n", encoding="utf-8")
    python_file.chmod(0o600)

    shell_lines = ["# Généré par scripts/environment-bootstrap.py. Ne pas modifier manuellement."]
    for name in sorted(values):
        if name.startswith("KB_GENAI_BUILDER_"):
            shell_lines.append(f"export {name}={shlex.quote(values[name])}")
    shell_file = GENERATED_DIR / "environment.env"
    shell_file.write_text("\n".join(shell_lines) + "\n", encoding="utf-8")
    shell_file.chmod(0o600)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--environment", default="default")
    parser.add_argument("--check-bash", action="store_true")
    parser.add_argument("--check-python", action="store_true")
    parser.add_argument("--check-java", action="store_true")
    parser.add_argument("--check-make", action="store_true")
    parser.add_argument("--check-maven", action="store_true")
    args = parser.parse_args()

    try:
        if args.check_bash:
            require_command("bash")
            return 0
        if args.check_python:
            require_command("python3")
            return 0
        if args.check_java:
            validate_tools()
            return 0
        if args.check_make:
            require_command("make")
            return 0
        if args.check_maven:
            require_command("mvn")
            return 0

        validate_tools()
        values, config_paths = load_configuration(args.environment)
        required = ("KB_GENAI_BUILDER_WORKSPACE_DIR", "KB_GENAI_BUILDER_MARKDOWN_INPUT_DIR", "KB_GENAI_BUILDER_MARKDOWN_OUTPUT_DIR", "KB_GENAI_BUILDER_RCLONE_REMOTE", "KB_GENAI_BUILDER_RCLONE_READ_ONLY")
        if args.environment == "dico":
            required += (
                "KB_GENAI_BUILDER_CODEBIO_WORK_DIR",
                "KB_GENAI_BUILDER_CODEBIO_TABLE_OF_CONTENTS_PDF",
                "KB_GENAI_BUILDER_CODEBIO_TABLE_OF_CONTENTS_MARKDOWN",
                "KB_GENAI_BUILDER_CODEBIO_DICTIONARY_PDF",
                "KB_GENAI_BUILDER_CODEBIO_ENTRY_STRUCTURE",
                "KB_GENAI_BUILDER_CODEBIO_ENTRY",
                "KB_GENAI_BUILDER_CODEBIO_ENTRY_OUTPUT_DIR",
                "KB_GENAI_BUILDER_CODEBIO_INDEX_RESOLUTIONS",
                "KB_GENAI_BUILDER_CODEBIO_FAILURE_LIST",
            )
        for name in required:
            if not values.get(name):
                raise RuntimeError(f"Variable requise absente : {name}")
        if values["KB_GENAI_BUILDER_RCLONE_READ_ONLY"] not in ALLOWED_READ_ONLY:
            raise RuntimeError("KB_GENAI_BUILDER_RCLONE_READ_ONLY doit valoir true ou false")
        if ":" not in values["KB_GENAI_BUILDER_RCLONE_REMOTE"]:
            raise RuntimeError("KB_GENAI_BUILDER_RCLONE_REMOTE doit avoir la forme remote:chemin")

        workspace = resolve_path(values["KB_GENAI_BUILDER_WORKSPACE_DIR"])
        input_dir = resolve_path(values["KB_GENAI_BUILDER_MARKDOWN_INPUT_DIR"])
        output_dir = resolve_path(values["KB_GENAI_BUILDER_MARKDOWN_OUTPUT_DIR"])
        if values["KB_GENAI_BUILDER_RCLONE_READ_ONLY"] == "true" and (Path(output_dir) == Path(workspace) or Path(workspace) in Path(output_dir).parents):
            raise RuntimeError("KB_GENAI_BUILDER_MARKDOWN_OUTPUT_DIR doit être hors du workspace monté en lecture seule.")
        values["KB_GENAI_BUILDER_WORKSPACE_DIR"] = workspace
        values["KB_GENAI_BUILDER_MARKDOWN_INPUT_DIR"] = input_dir
        values["KB_GENAI_BUILDER_MARKDOWN_OUTPUT_DIR"] = output_dir
        if args.environment == "dico":
            for name in (
                "KB_GENAI_BUILDER_CODEBIO_WORK_DIR",
                "KB_GENAI_BUILDER_CODEBIO_TABLE_OF_CONTENTS_PDF",
                "KB_GENAI_BUILDER_CODEBIO_TABLE_OF_CONTENTS_MARKDOWN",
                "KB_GENAI_BUILDER_CODEBIO_DICTIONARY_PDF",
                "KB_GENAI_BUILDER_CODEBIO_ENTRY_STRUCTURE",
                "KB_GENAI_BUILDER_CODEBIO_ENTRY_OUTPUT_DIR",
                "KB_GENAI_BUILDER_CODEBIO_INDEX_RESOLUTIONS",
                "KB_GENAI_BUILDER_CODEBIO_FAILURE_LIST",
            ):
                values[name] = resolve_path(values[name])
            Path(values["KB_GENAI_BUILDER_CODEBIO_WORK_DIR"]).mkdir(parents=True, exist_ok=True)
            Path(values["KB_GENAI_BUILDER_CODEBIO_ENTRY_OUTPUT_DIR"]).mkdir(parents=True, exist_ok=True)
        Path(workspace).mkdir(parents=True, exist_ok=True)
        Path(output_dir).mkdir(parents=True, exist_ok=True)
        write_generated(values)
        print("Bootstrap terminé : prérequis et configuration locale validés.")
        return 0
    except (OSError, RuntimeError, ValueError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
