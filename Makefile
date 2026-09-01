SHELL := /bin/sh

.DEFAULT_GOAL := help

GENERATED_MAKE_CONFIG := env/generated/make.mk
EXECUTABLE_JAR := target/kb-genai-builder.jar
JAVA_BIN ?= java
MAVEN_BIN ?= mvn
PYTHON_BIN ?= python3
RCLONE_BIN ?= rclone
CODEBIO_ENTRIES_SOURCE ?= workspace/KB-WORK/entries
NEOSANTE_ENTRIES_TARGET ?= workspace/08-KB-NéoSanté/markdown/entries
NEOSANTE_EXTRACTED_ENTRIES_SOURCE ?= workspace/KB-WORK/neosante-entries
NEOSANTE_EXTRACTED_ENTRIES_TARGET ?= workspace/08-KB-NéoSanté/markdown/neosante-entries
NEOSANTE_COLLECTION_LIMIT ?= 0
NEOSANTE_PARALLEL_JOBS ?= 1
JAVA_SOURCES := $(shell find src/main/java src/test/java -type f -name '*.java' 2>/dev/null)
# La configuration générée est lue à l'exécution, sans nécessiter de reconstruire le JAR.
# Le package Maven ne redémarre donc que lorsque le code Java (ou le pom Maven) est plus récent.
BUILD_INPUTS := pom.xml $(JAVA_SOURCES)
ENVIRONMENT_PROFILES := $(patsubst env/.env.%,%,$(filter-out env/.env.local env/.env.local.example env/.env.user.local env/.env.user.local.example env/.env.user.% env/.env.%.local,$(wildcard env/.env.*)))
REQUESTED_BOOTSTRAP_ENVIRONMENT := $(firstword $(filter $(ENVIRONMENT_PROFILES),$(MAKECMDGOALS)))

-include $(GENERATED_MAKE_CONFIG)

.PHONY: bootstrap bootstrap-bash bootstrap-java bootstrap-make bootstrap-maven bootstrap-python build codebio-entries-sync config-check extract-neosante-collection generated-config help neosante-entries-sync pdf-to-markdown run-pdf-to-markdown run-extract-neosante-relevant-articles teams-app-sso-configure teams-app-publish test workspace-mount $(ENVIRONMENT_PROFILES) bootstrap-%

help:
	@printf '%s\n' \
		'Commandes disponibles :' \
		'  bootstrap        Préparer et valider l’environnement local' \
		'  bootstrap <env>  Charger .env.<env> et .env.user.<env>, ex. make bootstrap neosante' \
		'  bootstrap-bash   Vérifier Bash' \
		'  bootstrap-python Vérifier Python 3' \
		'  bootstrap-java   Vérifier Java 21+' \
		'  bootstrap-make   Vérifier Make' \
		'  bootstrap-maven  Vérifier Maven' \
		'  build            Transformer un PDF déclaré par PDF en Markdown' \
		'  extract-neosante-collection Extraire les articles pertinents de la collection configurée' \
		'                       variables: NEOSANTE_COLLECTION_LIMIT=10 NEOSANTE_PARALLEL_JOBS=5' \
		'                       répertoires: KB_GENAI_BUILDER_MARKDOWN_INPUT_DIR=<source> KB_GENAI_BUILDER_MARKDOWN_OUTPUT_DIR=<cible> KB_GENAI_BUILDER_MARKDOWN_LOG_DIR=<journaux>' \
		'  codebio-entries-sync Synchroniser les entrées CodeBio vers le montage Néosanté avec rclone' \
		'                       variables: CODEBIO_ENTRIES_SOURCE=<source> NEOSANTE_ENTRIES_TARGET=<cible> RCLONE_BIN=<binaire>' \
		'  neosante-entries-sync Synchroniser les entrées extraites Néosanté vers OneDrive avec rclone' \
		'                       variables: NEOSANTE_EXTRACTED_ENTRIES_SOURCE=<source> NEOSANTE_EXTRACTED_ENTRIES_TARGET=<cible> RCLONE_BIN=<binaire>' \
		'  config-check     Valider la configuration rclone sans monter le dossier' \
		'  package          Construire le JAR exécutable' \
		'  pdf-to-markdown  Alias de build; variables: PDF=<source> OUTPUT=<cible>' \
		'  teams-app-sso-configure  Configurer le SSO Entra ID du canal Teams; variables: DRY_RUN=true GRANT_ADMIN_CONSENT=true' \
		'  teams-app-publish       Publier le manifeste Teams via Microsoft Graph (az rest); variables: DRY_RUN=true' \
		'  test             Exécuter les tests Maven sans réseau' \
		'  workspace-mount  Monter la KB dans workspace/08-KB-NéoSanté/ (lecture-écriture)'

bootstrap: bootstrap-bash bootstrap-python bootstrap-java bootstrap-make bootstrap-maven
	@$(PYTHON_BIN) ./scripts/environment-bootstrap.py $(if $(REQUESTED_BOOTSTRAP_ENVIRONMENT),--environment "$(REQUESTED_BOOTSTRAP_ENVIRONMENT)")

$(ENVIRONMENT_PROFILES):
	@:

bootstrap-%:
	@$(PYTHON_BIN) ./scripts/environment-bootstrap.py --environment "$*"

bootstrap-bash:
	@$(PYTHON_BIN) ./scripts/environment-bootstrap.py --check-bash

bootstrap-python:
	@$(PYTHON_BIN) ./scripts/environment-bootstrap.py --check-python

bootstrap-java:
	@$(PYTHON_BIN) ./scripts/environment-bootstrap.py --check-java

bootstrap-make:
	@$(PYTHON_BIN) ./scripts/environment-bootstrap.py --check-make

bootstrap-maven:
	@$(PYTHON_BIN) ./scripts/environment-bootstrap.py --check-maven

build: pdf-to-markdown

generated-config:
	@test -f "$(GENERATED_MAKE_CONFIG)" || { printf '%s\n' 'ERROR: configuration générée absente; exécutez make bootstrap.' >&2; exit 1; }
	@command -v "$(JAVA_BIN)" >/dev/null 2>&1 && command -v "$(MAVEN_BIN)" >/dev/null 2>&1 && command -v "$(PYTHON_BIN)" >/dev/null 2>&1 || { printf '%s\n' 'ERROR: prérequis introuvable; exécutez make bootstrap.' >&2; exit 1; }

package: $(EXECUTABLE_JAR)

$(EXECUTABLE_JAR): $(BUILD_INPUTS) | generated-config
	@$(MAVEN_BIN) --no-transfer-progress package

pdf-to-markdown: package run-pdf-to-markdown

run-pdf-to-markdown: generated-config
	@test -n "$(PDF)" || { printf '%s\n' 'ERROR: indiquez PDF=<chemin-du-fichier.pdf>.' >&2; exit 2; }
	@test -f "$(EXECUTABLE_JAR)" || { printf '%s\n' 'ERROR: JAR absent; exécutez make package.' >&2; exit 1; }
	@$(JAVA_BIN) -jar "$(EXECUTABLE_JAR)" transform-pdf "$(PDF)" $(if $(OUTPUT),--output "$(OUTPUT)")

run-extract-neosante-relevant-articles: package generated-config
	@test -n "$(PDF)" || { printf '%s\n' 'ERROR: indiquez PDF=<chemin-du-fichier.pdf>.' >&2; exit 2; }
	@test -n "$(OUTPUT_DIRECTORY)" || { printf '%s\n' 'ERROR: indiquez OUTPUT_DIRECTORY=<répertoire-cible>.' >&2; exit 2; }
	@$(JAVA_BIN) -jar "$(EXECUTABLE_JAR)" extract-neosante-relevant-articles "$(PDF)" --output-directory "$(OUTPUT_DIRECTORY)"

extract-neosante-collection: package generated-config
	@test -d "$(KB_GENAI_BUILDER_MARKDOWN_INPUT_DIR)" || { printf 'ERROR: répertoire source Néosanté introuvable : %s\n' "$(KB_GENAI_BUILDER_MARKDOWN_INPUT_DIR)" >&2; exit 2; }
	@case "$(NEOSANTE_COLLECTION_LIMIT)" in ''|*[!0-9]*) printf '%s\n' 'ERROR: NEOSANTE_COLLECTION_LIMIT doit être un entier positif ou 0.' >&2; exit 2;; esac
	@case "$(NEOSANTE_PARALLEL_JOBS)" in ''|*[!0-9]*|0) printf '%s\n' 'ERROR: NEOSANTE_PARALLEL_JOBS doit être un entier strictement positif.' >&2; exit 2;; esac
	@mkdir -p "$(KB_GENAI_BUILDER_MARKDOWN_OUTPUT_DIR)" "$(KB_GENAI_BUILDER_MARKDOWN_LOG_DIR)"
	@export JAVA_BIN='$(JAVA_BIN)' EXECUTABLE_JAR='$(EXECUTABLE_JAR)' KB_GENAI_BUILDER_MARKDOWN_OUTPUT_DIR='$(KB_GENAI_BUILDER_MARKDOWN_OUTPUT_DIR)' KB_GENAI_BUILDER_MARKDOWN_LOG_DIR='$(KB_GENAI_BUILDER_MARKDOWN_LOG_DIR)'; bash -ec ' \
	find "$(KB_GENAI_BUILDER_MARKDOWN_INPUT_DIR)" -maxdepth 1 -type f -iname "*.pdf" -printf "%p\\n" | sort | \
	while IFS= read -r pdf; do filename=$${pdf##*/}; review=$${filename%.pdf}; [ -f "$$KB_GENAI_BUILDER_MARKDOWN_OUTPUT_DIR/$$review/.complete" ] || printf "%s\\n" "$$pdf"; done | \
	if [ "$(NEOSANTE_COLLECTION_LIMIT)" -gt 0 ]; then head -n "$(NEOSANTE_COLLECTION_LIMIT)"; else cat; fi | \
	xargs -d "\\n" -r -n 1 -P "$(NEOSANTE_PARALLEL_JOBS)" sh -ec '"'"'pdf="$$1"; filename=$${pdf##*/}; review=$${filename%.pdf}; destination="$$KB_GENAI_BUILDER_MARKDOWN_OUTPUT_DIR/$$review"; printf "%s\\n" "[Néosanté] $$filename"; "$$JAVA_BIN" -jar "$$EXECUTABLE_JAR" extract-neosante-relevant-articles "$$pdf" --output-directory "$$destination" >"$$KB_GENAI_BUILDER_MARKDOWN_LOG_DIR/$$review.log" 2>&1'"'"' sh'

test: generated-config
	@$(MAVEN_BIN) --no-transfer-progress test

teams-app-sso-configure: bootstrap-python
	@$(PYTHON_BIN) ./scripts/teams-app-sso-configure.py $(if $(DRY_RUN),--dry-run) $(if $(GRANT_ADMIN_CONSENT),--grant-admin-consent)

teams-app-publish: bootstrap-python
	@$(PYTHON_BIN) ./scripts/teams-app-publish.py $(if $(DRY_RUN),--dry-run)

# rclone sync rend la cible identique à la source : les fichiers supprimés de la source sont supprimés de la cible.
# La cible est normalement disponible après « make workspace-mount ».
codebio-entries-sync:
	@command -v "$(RCLONE_BIN)" >/dev/null 2>&1 || { printf 'ERROR: rclone est introuvable : %s\\n' "$(RCLONE_BIN)" >&2; exit 1; }
	@test -d "$(CODEBIO_ENTRIES_SOURCE)" || { printf 'ERROR: répertoire source CodeBio introuvable : %s\\n' "$(CODEBIO_ENTRIES_SOURCE)" >&2; exit 2; }
	@test -d "$(NEOSANTE_ENTRIES_TARGET)" || { printf 'ERROR: répertoire cible Néosanté introuvable : %s (exécutez « make workspace-mount »)\\n' "$(NEOSANTE_ENTRIES_TARGET)" >&2; exit 2; }
	@$(RCLONE_BIN) sync "$(CODEBIO_ENTRIES_SOURCE)" "$(NEOSANTE_ENTRIES_TARGET)" --create-empty-src-dirs

# rclone sync rend la cible identique à la source : les fichiers supprimés de la source sont supprimés de la cible.
# La cible est créée dans le montage OneDrive si elle n'existe pas encore.
neosante-entries-sync:
	@command -v "$(RCLONE_BIN)" >/dev/null 2>&1 || { printf 'ERROR: rclone est introuvable : %s\\n' "$(RCLONE_BIN)" >&2; exit 1; }
	@test -d "$(NEOSANTE_EXTRACTED_ENTRIES_SOURCE)" || { printf 'ERROR: répertoire source des entrées Néosanté introuvable : %s\\n' "$(NEOSANTE_EXTRACTED_ENTRIES_SOURCE)" >&2; exit 2; }
	@test -d "$$(dirname "$(NEOSANTE_EXTRACTED_ENTRIES_TARGET)")" || { printf 'ERROR: montage OneDrive introuvable : %s (exécutez « make workspace-mount »)\\n' "$$(dirname "$(NEOSANTE_EXTRACTED_ENTRIES_TARGET)")" >&2; exit 2; }
	@$(RCLONE_BIN) sync "$(NEOSANTE_EXTRACTED_ENTRIES_SOURCE)" "$(NEOSANTE_EXTRACTED_ENTRIES_TARGET)" --create-empty-src-dirs

config-check:
	@./scripts/workspace-mount.sh --check

workspace-mount:
	@./scripts/workspace-mount.sh
