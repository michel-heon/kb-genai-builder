SHELL := /bin/sh

.DEFAULT_GOAL := help
MAKEFLAGS += -j5

MAKEFILE_DIR := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))
PROJECT_ROOT := $(abspath $(MAKEFILE_DIR)/..)
GENERATED_MAKE_CONFIG := $(PROJECT_ROOT)/env/generated/make.mk
MAX_PARALLEL_JOBS := 5
CODEBIO_PARALLEL_JOBS ?= 4
JAVA_BIN ?= java

-include $(GENERATED_MAKE_CONFIG)

SOURCE_DIR ?= $(KB_GENAI_BUILDER_MARKDOWN_INPUT_DIR)
OUTPUT_DIR ?= $(KB_GENAI_BUILDER_MARKDOWN_OUTPUT_DIR)
CODEBIO_WORK_DIR ?= $(KB_GENAI_BUILDER_CODEBIO_WORK_DIR)
CODEBIO_TABLE_OF_CONTENTS_PDF ?= $(KB_GENAI_BUILDER_CODEBIO_TABLE_OF_CONTENTS_PDF)
CODEBIO_TABLE_OF_CONTENTS_MARKDOWN ?= $(KB_GENAI_BUILDER_CODEBIO_TABLE_OF_CONTENTS_MARKDOWN)
CODEBIO_DICTIONARY_PDF ?= $(KB_GENAI_BUILDER_CODEBIO_DICTIONARY_PDF)
CODEBIO_ENTRY_STRUCTURE ?= $(KB_GENAI_BUILDER_CODEBIO_ENTRY_STRUCTURE)
CODEBIO_ENTRY ?= $(KB_GENAI_BUILDER_CODEBIO_ENTRY)
CODEBIO_ENTRY_OUTPUT_DIR ?= $(KB_GENAI_BUILDER_CODEBIO_ENTRY_OUTPUT_DIR)
CODEBIO_ENTRY_MARKDOWN ?= $(CODEBIO_ENTRY_OUTPUT_DIR)/$(CODEBIO_ENTRY).md
CODEBIO_INDEX_RESOLUTIONS ?= $(KB_GENAI_BUILDER_CODEBIO_INDEX_RESOLUTIONS)
CODEBIO_FAILURE_LIST ?= $(KB_GENAI_BUILDER_CODEBIO_FAILURE_LIST)
CODEBIO_FORCE ?= false

.PHONY: help markdown codebio-table-of-contents-markdown codebio-entry-markdown codebio-all-entries-markdown codebio-%-entries-markdown neosante-article-markdown neosante-articles-markdown

help:
	@printf '%s\n' \
		'Commandes disponibles :' \
		'  markdown                           Transformer tous les PDF de la collection Néosanté vers Markdown' \
		'                                     (5 transformations parallèles maximum)' \
		'  codebio-table-of-contents-markdown Transformer la table des matières CodeBio avec le transformateur dédié' \
		'  codebio-entry-markdown             Extraire une entrée CodeBio (défaut : Amylase)' \
		'  codebio-all-entries-markdown        Extraire toutes les entrées CodeBio, lettre par lettre' \
		'  codebio-<lettre>-entries-markdown  Extraire toutes les entrées CodeBio commençant par <lettre> (ex. codebio-b-entries-markdown) et lister les échecs' \
		'                                     ($(CODEBIO_PARALLEL_JOBS) extractions parallèles maximum)' \
		'  neosante-article-markdown          Extraire par IA un PDF nommé avec PDF_NAME=<fichier.pdf>' \
		'  neosante-articles-markdown         Extraire par IA les pages pertinentes du corpus de qualification' \
		'                                     sur la biologie totale et le décodage biologique' \
		'' \
		'Variables :' \
		'  SOURCE_DIR=<répertoire PDF>             Défaut : profil KB_GENAI_BUILDER_MARKDOWN_INPUT_DIR' \
		'  OUTPUT_DIR=<répertoire Markdown>        Défaut : profil KB_GENAI_BUILDER_MARKDOWN_OUTPUT_DIR' \
		'  CODEBIO_WORK_DIR=<répertoire>           Défaut : profil KB_GENAI_BUILDER_CODEBIO_WORK_DIR' \
		'  CODEBIO_TABLE_OF_CONTENTS_PDF=<fichier> Surcharge ponctuelle du profil CodeBio' \
		'  CODEBIO_TABLE_OF_CONTENTS_MARKDOWN=<fichier> Surcharge ponctuelle du profil CodeBio' \
		'  CODEBIO_DICTIONARY_PDF=<fichier>        Surcharge ponctuelle du profil CodeBio' \
		'  CODEBIO_ENTRY_STRUCTURE=<fichier>       Surcharge ponctuelle du profil CodeBio' \
		'  CODEBIO_ENTRY=<terme>                   Surcharge ponctuelle de l’entrée du profil' \
		'  CODEBIO_ENTRY_OUTPUT_DIR=<répertoire>   Surcharge du répertoire de sortie du profil' \
		'  CODEBIO_ENTRY_MARKDOWN=<fichier>        Surcharge ponctuelle de la cible calculée' \
		'  CODEBIO_INDEX_RESOLUTIONS=<fichier>     Registre des renvois CodeBio vérifiés' \
		'  CODEBIO_FAILURE_LIST=<fichier>          Base du journal d’échecs ; les lots ajoutent -<LETTRE> avant .md' \
		'  CODEBIO_FORCE=true                      Régénérer les entrées même si leurs dépendances sont à jour' \
		'  PDF_NAME=<fichier.pdf>                  Requis par neosante-article-markdown'

markdown:
	@$(MAKE) -s -C "$(PROJECT_ROOT)" package
	@find "$(SOURCE_DIR)" -type f -name '*.pdf' -print0 | xargs -0 -r -P "$(MAX_PARALLEL_JOBS)" -n 1 sh -c '\
		source_path=$$1; \
		relative_path=$${source_path#"$(SOURCE_DIR)"/}; \
		output_path="$(OUTPUT_DIR)/$${relative_path%.*}.md"; \
		if [ -f "$$output_path" ] && ! [ "$$source_path" -nt "$$output_path" ]; then \
			printf "%s" "Déjà à jour : $$output_path"; printf "\n"; \
			exit 0; \
		fi; \
		mkdir -p "$$(dirname "$$output_path")"; \
		$(MAKE) -s -C "$(PROJECT_ROOT)" run-pdf-to-markdown PDF="$$source_path" OUTPUT="$$output_path" \
	' sh

codebio-table-of-contents-markdown:
	@test -f "$(CODEBIO_TABLE_OF_CONTENTS_PDF)" || { printf 'ERROR: PDF CodeBio introuvable : %s\n' "$(CODEBIO_TABLE_OF_CONTENTS_PDF)" >&2; exit 2; }
	@$(MAKE) -s -C "$(PROJECT_ROOT)" package
	@$(JAVA_BIN) -jar "$(PROJECT_ROOT)/target/kb-genai-builder.jar" transform-codebio-table-of-contents "$(CODEBIO_TABLE_OF_CONTENTS_PDF)" --output "$(CODEBIO_TABLE_OF_CONTENTS_MARKDOWN)"

codebio-entry-markdown:
	@test -f "$(CODEBIO_DICTIONARY_PDF)" || { printf 'ERROR: dictionnaire CodeBio introuvable : %s\n' "$(CODEBIO_DICTIONARY_PDF)" >&2; exit 2; }
	@test -f "$(CODEBIO_TABLE_OF_CONTENTS_MARKDOWN)" || { printf 'ERROR: table des matières CodeBio introuvable : %s\n' "$(CODEBIO_TABLE_OF_CONTENTS_MARKDOWN)" >&2; exit 2; }
	@test -f "$(CODEBIO_ENTRY_STRUCTURE)" || { printf 'ERROR: référentiel structurel CodeBio introuvable : %s\n' "$(CODEBIO_ENTRY_STRUCTURE)" >&2; exit 2; }
	@test -z "$(CODEBIO_INDEX_RESOLUTIONS)" || test -f "$(CODEBIO_INDEX_RESOLUTIONS)" || { printf 'ERROR: registre des résolutions CodeBio introuvable : %s\n' "$(CODEBIO_INDEX_RESOLUTIONS)" >&2; exit 2; }
	@if [ "$(CODEBIO_SKIP_PACKAGE)" != "true" ]; then $(MAKE) -s -C "$(PROJECT_ROOT)" package; fi
	@$(JAVA_BIN) -jar "$(PROJECT_ROOT)/target/kb-genai-builder.jar" extract-codebio-entry "$(CODEBIO_DICTIONARY_PDF)" "$(CODEBIO_ENTRY)" --table-of-contents "$(CODEBIO_TABLE_OF_CONTENTS_MARKDOWN)" --structure "$(CODEBIO_ENTRY_STRUCTURE)" --output "$(CODEBIO_ENTRY_MARKDOWN)" $(if $(CODEBIO_INDEX_RESOLUTIONS),--index-resolutions "$(CODEBIO_INDEX_RESOLUTIONS)") $(if $(CODEBIO_FAILURE_LIST),--failure-list "$(CODEBIO_FAILURE_LIST)")

# Traite toutes les sections alphabétiques réellement présentes dans la table des matières.
# La compilation est faite une seule fois ; les lettres sont séquentielles et chaque lettre conserve
# son parallélisme interne ainsi que son propre journal d'échecs.
codebio-all-entries-markdown:
	@test -f "$(CODEBIO_TABLE_OF_CONTENTS_MARKDOWN)" || { printf 'ERROR: table des matières CodeBio introuvable : %s\n' "$(CODEBIO_TABLE_OF_CONTENTS_MARKDOWN)" >&2; exit 2; }
	@$(MAKE) -s -C "$(PROJECT_ROOT)" package
	@letters="$$(sed -n 's/^## \([[:alpha:]]\)$$/\1/p' "$(CODEBIO_TABLE_OF_CONTENTS_MARKDOWN)")"; \
		test -n "$$letters" || { printf '%s\n' 'ERROR: aucune section alphabétique CodeBio trouvée.' >&2; exit 2; }; \
		for letter in $$letters; do \
			printf 'Extraction CodeBio — lettre %s\n' "$$letter"; \
			$(MAKE) -s -C "$(MAKEFILE_DIR)" "codebio-$$letter-entries-markdown" CODEBIO_SKIP_PACKAGE=true CODEBIO_FORCE="$(CODEBIO_FORCE)" || exit $$?; \
		done

# Règle générique : « codebio-x-entries-markdown » traite la section « ## X » de la table des matières.
# $* est la lettre demandée, normalisée en majuscule ; chaque terme devient un Markdown dans entries/X.
# Les extractions sont parallélisées (CODEBIO_PARALLEL_JOBS), mais chaque lot écrit son propre journal
# echecs-extraction-X.md afin que les échecs restent traçables sans être écrasés par le lot suivant.
# Elle délègue chaque terme à codebio-entry-markdown : cette règle unitaire est l’unique point d’appel au JAR.
codebio-%-entries-markdown: CODEBIO_LETTER = $(shell printf '%s' '$*' | tr '[:lower:]' '[:upper:]')
codebio-%-entries-markdown: CODEBIO_LETTER_FAILURE_LIST = $(CODEBIO_FAILURE_LIST:.md=-$(CODEBIO_LETTER).md)
codebio-%-entries-markdown:
	@test -n "$(CODEBIO_LETTER)" || { printf '%s\n' 'ERROR: précisez une lettre, ex. codebio-b-entries-markdown.' >&2; exit 2; }
	@test -f "$(CODEBIO_TABLE_OF_CONTENTS_MARKDOWN)" || { printf 'ERROR: table des matières CodeBio introuvable : %s\n' "$(CODEBIO_TABLE_OF_CONTENTS_MARKDOWN)" >&2; exit 2; }
	@test -n "$(CODEBIO_FAILURE_LIST)" || { printf '%s\n' 'ERROR: CODEBIO_FAILURE_LIST doit être défini dans le profil.' >&2; exit 2; }
	@if [ "$(CODEBIO_SKIP_PACKAGE)" != "true" ]; then $(MAKE) -s -C "$(PROJECT_ROOT)" package; fi
	@mkdir -p "$(CODEBIO_ENTRY_OUTPUT_DIR)/$(CODEBIO_LETTER)" "$$(dirname "$(CODEBIO_LETTER_FAILURE_LIST)")"
	@: > "$(CODEBIO_LETTER_FAILURE_LIST)"
	@printf '%s\n\n' '# Échecs d’extraction CodeBio — entrées $(CODEBIO_LETTER)' >> "$(CODEBIO_LETTER_FAILURE_LIST)"
	@sed -n '/^## $(CODEBIO_LETTER)$$/,/^## /{s/^- \(.*\) — p\. [0-9][0-9]*$$/\1/p;}' "$(CODEBIO_TABLE_OF_CONTENTS_MARKDOWN)" | tr '\n' '\0' | xargs -0 -r -P "$(CODEBIO_PARALLEL_JOBS)" -n 1 sh -c '\
		term="$$1"; \
		output="$(CODEBIO_ENTRY_OUTPUT_DIR)/$(CODEBIO_LETTER)/$$term.md"; \
		stale=true; \
		if [ -f "$$output" ]; then \
			stale=false; \
			for dependency in "$(CODEBIO_DICTIONARY_PDF)" "$(CODEBIO_TABLE_OF_CONTENTS_MARKDOWN)" "$(CODEBIO_ENTRY_STRUCTURE)" "$(CODEBIO_INDEX_RESOLUTIONS)" "$(MAKEFILE_DIR)Makefile"; do \
				if [ -n "$$dependency" ] && [ "$$dependency" -nt "$$output" ]; then stale=true; break; fi; \
			done; \
			if [ "$$stale" = false ] && find "$(PROJECT_ROOT)/src/main/java/net/cotechnoe/kb/genai/document/codebio" "$(PROJECT_ROOT)/src/main/java/net/cotechnoe/kb/genai/cli/ExtractCodeBioDictionaryEntryCommand.java" -type f -newer "$$output" -print -quit | grep -q .; then stale=true; fi; \
		fi; \
		if [ -f "$$output" ] && [ "$(CODEBIO_FORCE)" != "true" ] && [ "$$stale" = false ]; then \
			printf "%s" "Déjà extraite : $$output"; printf "\n"; \
			exit 0; \
		fi; \
		$(MAKE) -s -C "$(MAKEFILE_DIR)" codebio-entry-markdown CODEBIO_SKIP_PACKAGE=true CODEBIO_ENTRY="$$term" CODEBIO_ENTRY_MARKDOWN="$$output" CODEBIO_FAILURE_LIST="$(CODEBIO_LETTER_FAILURE_LIST)" || true \
	' sh
	@if [ "$$(wc -l < "$(CODEBIO_LETTER_FAILURE_LIST)")" -eq 2 ]; then rm "$(CODEBIO_LETTER_FAILURE_LIST)"; fi

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
