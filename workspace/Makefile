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

.PHONY: help markdown neosante-article-markdown neosante-articles-markdown

help:
	@printf '%s\n' \
		'Commandes disponibles :' \
		'  markdown                    Transformer tous les PDF de la collection Néosanté vers Markdown' \
		'                              (5 transformations parallèles maximum)' \
		'  neosante-article-markdown   Extraire par IA un PDF nommé avec PDF_NAME=<fichier.pdf>' \
		'  neosante-articles-markdown  Extraire par IA les pages pertinentes du corpus de qualification' \
		'                              sur la biologie totale et le décodage biologique' \
		'' \
		'Variables :' \
		'  SOURCE_DIR=<répertoire PDF>      Défaut : KB_GENAI_BUILDER_MARKDOWN_INPUT_DIR' \
		'  OUTPUT_DIR=<répertoire Markdown> Défaut : KB_GENAI_BUILDER_MARKDOWN_OUTPUT_DIR' \
		'  PDF_NAME=<fichier.pdf>            Requis par neosante-article-markdown'

markdown:
	@$(MAKE) -s -C "$(PROJECT_ROOT)" package
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

neosante-article-markdown:
	@test -n "$(PDF_NAME)" || { printf '%s\n' 'ERROR: indiquez PDF_NAME=<fichier.pdf>.' >&2; exit 2; }
	@source_path="$(SOURCE_DIR)/$(PDF_NAME)"; \
	case "$(PDF_NAME)" in \
		*.pdf) ;; \
		*) printf '%s\n' 'ERROR: PDF_NAME doit désigner un fichier .pdf.' >&2; exit 2 ;; \
	esac; \
	test -f "$$source_path" || { printf 'ERROR: PDF introuvable : %s\\n' "$$source_path" >&2; exit 2; }; \
	relative_path=$${source_path#"$(SOURCE_DIR)"/}; \
	output_directory="$(OUTPUT_DIR)/$${relative_path%.*}"; \
	rm -rf "$$output_directory"; \
	$(MAKE) -s -C "$(PROJECT_ROOT)" run-extract-neosante-relevant-articles PDF="$$source_path" OUTPUT_DIRECTORY="$$output_directory"

neosante-articles-markdown:
	@$(MAKE) -s -C "$(PROJECT_ROOT)" package
	@find "$(SOURCE_DIR)" -type f -name '*.pdf' -print0 | xargs -0 -r -P "$(MAX_PARALLEL_JOBS)" -n 1 sh -c '\
		source_path=$$1; \
		relative_path=$${source_path#"$(SOURCE_DIR)"/}; \
		output_directory="$(OUTPUT_DIR)/$${relative_path%.*}"; \
		completion_file="$$output_directory/.complete"; \
		if [ -f "$$completion_file" ] && ! [ "$$source_path" -nt "$$completion_file" ]; then \
			printf "%s" "Déjà à jour : $$output_directory"; printf "\\n"; \
			exit 0; \
		fi; \
		rm -rf "$$output_directory"; \
		$(MAKE) -s -C "$(PROJECT_ROOT)" run-extract-neosante-relevant-articles PDF="$$source_path" OUTPUT_DIRECTORY="$$output_directory" \
	' sh