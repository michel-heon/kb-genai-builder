SHELL := /bin/sh

.DEFAULT_GOAL := help

GENERATED_MAKE_CONFIG := env/generated/make.mk
EXECUTABLE_JAR := target/kb-genai-builder.jar
JAVA_BIN ?= java
MAVEN_BIN ?= mvn
PYTHON_BIN ?= python3
JAVA_SOURCES := $(shell find src/main/java src/test/java -type f -name '*.java' 2>/dev/null)
# La configuration générée est lue à l'exécution, sans nécessiter de reconstruire le JAR.
# Le package Maven ne redémarre donc que lorsque le code Java (ou le pom Maven) est plus récent.
BUILD_INPUTS := pom.xml $(JAVA_SOURCES)
ENVIRONMENT_PROFILES := $(patsubst env/.env.%,%,$(filter-out env/.env.local env/.env.local.example env/.env.user.local env/.env.user.local.example env/.env.user.% env/.env.%.local,$(wildcard env/.env.*)))
REQUESTED_BOOTSTRAP_ENVIRONMENT := $(firstword $(filter $(ENVIRONMENT_PROFILES),$(MAKECMDGOALS)))

-include $(GENERATED_MAKE_CONFIG)

.PHONY: bootstrap bootstrap-bash bootstrap-java bootstrap-make bootstrap-maven bootstrap-python build config-check generated-config help pdf-to-markdown run-pdf-to-markdown run-extract-neosante-relevant-articles teams-app-sso-configure teams-app-publish test workspace-mount $(ENVIRONMENT_PROFILES) bootstrap-%

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

test: generated-config
	@$(MAVEN_BIN) --no-transfer-progress test

teams-app-sso-configure: bootstrap-python
	@$(PYTHON_BIN) ./scripts/teams-app-sso-configure.py $(if $(DRY_RUN),--dry-run) $(if $(GRANT_ADMIN_CONSENT),--grant-admin-consent)

teams-app-publish: bootstrap-python
	@$(PYTHON_BIN) ./scripts/teams-app-publish.py $(if $(DRY_RUN),--dry-run)

config-check:
	@./scripts/workspace-mount.sh --check

workspace-mount:
	@./scripts/workspace-mount.sh
