SHELL := /bin/sh

.DEFAULT_GOAL := help
MAKEFLAGS += -j5

MAKEFILE_DIR := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
PROJECT_ROOT := $(abspath $(MAKEFILE_DIR)/..)
GENERATED_MAKE_CONFIG := $(PROJECT_ROOT)/env/generated/make.mk
MAX_PARALLEL_JOBS := 5

-include $(GENERATED_MAKE_CONFIG)

SOURCE_DIR ?= $(KB_GENAI_BUILDER_MARKDOWN_INPUT_DIR)
OUTPUT_DIR ?= $(KB_GENAI_BUILDER_MARKDOWN_OUTPUT_DIR)

.PHONY: help markdown

help:
	@printf '%s\n' \
		'Commandes disponibles :' \
		'  markdown  Transformer tous les PDF de la collection Néosanté vers Markdown' \
		'            (5 transformations parallèles maximum)' \
		'' \
		'Variables :' \
		'  SOURCE_DIR=<répertoire PDF>       Défaut : 08-KB-NéoSanté/collection/' \
		'  OUTPUT_DIR=<répertoire Markdown>  Défaut : destination configurée dans env/default.env'

markdown:
	@find "$(SOURCE_DIR)" -type f -name '*.pdf' -print0 | xargs -0 -r -P "$(MAX_PARALLEL_JOBS)" -n 1 sh -c '\
		source_path=$$1; \
		relative_path=$${source_path#"$(SOURCE_DIR)"/}; \
		output_path="$(OUTPUT_DIR)/$${relative_path%.*}.md"; \
		if [ -f "$$output_path" ] && ! [ "$$source_path" -nt "$$output_path" ]; then \
			printf "%s" "Déjà à jour : $$output_path"; printf "\\n"; \
			exit 0; \
		fi; \
		mkdir -p "$$(dirname "$$output_path")"; \
		$(MAKE) -s -C "$(PROJECT_ROOT)" run-pdf-to-markdown PDF="$$source_path" OUTPUT="$$output_path" \
	' sh