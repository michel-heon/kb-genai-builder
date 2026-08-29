---
adr: 104
title: "Transformation des revues Néosanté de la collection PDF vers Markdown"
status: "proposed"
date: 2026-08-28
superseded_by: null
replaces: null
related_adrs: [0, 102, 103, 600, 602, 608, 611]
related_issues: []
classification:
  lifecycle: "proposed"
  domain: "architecture"
  impact: "high"
  quality:
    - "maintainability"
    - "traceability"
    - "reliability"
    - "portability"
  reversibility: "easy"
  scope: "tactical"
  tech_areas:
    - "knowledge-base"
    - "pdf"
    - "markdown"
    - "automation"
tags: ["neosante", "reviews", "pdf", "markdown", "make"]
stakeholders: ["@project-owner", "@content-team", "@architecture-team"]
effort: "medium"
---

# ADR 104 : Transformation des revues Néosanté de la collection PDF vers Markdown

## Vue d’ensemble

| Attribut | Valeur |
|---|---|
| Statut | Proposé |
| Date de décision | 2026-08-28 |
| Périmètre | `workspace/08-KB-NéoSanté/collection/` |
| Sortie | `workspace/08-KB-NéoSanté/markdown/collection/` |
| Corpus de test admis | `workspace/08-KB-NéoSanté/collection-test/` |
| Impact | Élevé |
| Effort | Moyen |
| Risque principal | Produire un Markdown incomplet ou obsolète pour une revue source |

## Contexte et problème

Les revues de Néosanté sont disponibles sous forme de fichiers PDF dans la collection montée sous `workspace/08-KB-NéoSanté/collection/`. Elles doivent être transformées localement en fichiers Markdown afin d’être relues, versionnées par le système de fichiers source et préparées pour les futurs traitements de base de connaissances.

Une transformation globale naïve présente plusieurs problèmes :

- elle retraiterait inutilement les revues déjà transformées ;
- elle doit supporter les noms de fichiers contenant des espaces ou des caractères accentués ;
- elle peut lancer trop de conversions simultanées sur un montage rclone ;
- elle doit distinguer les sources PDF des artefacts Markdown produits ;
- elle doit conserver la correspondance entre chaque revue et son fichier de sortie.

### Faits observés

| Élément | Type | Source ou validation |
|---|---|---|
| La collection contient 151 PDF | Fait | Comptage local le 2026-08-28 |
| `collection-test` contient 10 PDF de qualification | Fait | Comptage local le 2026-08-28 |
| Certains noms contiennent des espaces, par exemple `Recueil1-Jeune 2.pdf` | Fait | Liste locale de la collection |
| Le répertoire d’entrée est configuré par `KB_GENAI_BUILDER_MARKDOWN_INPUT_DIR` | Fait | `env/default.env` |
| Le répertoire de sortie est configuré par `KB_GENAI_BUILDER_MARKDOWN_OUTPUT_DIR` | Fait | `env/default.env` |
| La transformation locale PDF vers Markdown est fournie par la CLI Java | Fait | ADR-102 et code Java |
| Tous les PDF possèdent une couche texte exploitable | Inconnu | À VÉRIFIER sur le corpus complet |

## Décision

La transformation des revues Néosanté est orchestrée par le `Makefile` du répertoire `workspace/`, avec les règles suivantes :

1. `KB_GENAI_BUILDER_MARKDOWN_INPUT_DIR` désigne le répertoire source PDF.
2. `workspace/08-KB-NéoSanté/collection-test/` est un corpus de test admis pour qualifier la transformation avant ou à la place du corpus complet. Il est sélectionné par une surcharge ponctuelle de `SOURCE_DIR` et n’est pas la source par défaut.
3. `KB_GENAI_BUILDER_MARKDOWN_OUTPUT_DIR` désigne le répertoire cible Markdown.
4. Chaque PDF conserve son chemin relatif et son nom, avec l’extension remplacée par `.md`.
5. Un Markdown est généré s’il n’existe pas ou si son PDF source est plus récent.
6. Un Markdown existant est conservé lorsque son PDF source est plus ancien ou de même date.
7. La découverte des fichiers utilise un flux compatible avec les espaces dans les noms.
8. Le `Makefile` limite l’exécution à cinq transformations parallèles (`-P5`).
9. Une erreur de transformation provoque un échec de la cible `markdown`.

La règle est destinée aux fichiers sous `collection/`; elle ne transforme pas les fichiers Markdown déjà présents comme s’ils étaient des sources. La transformation générale reste locale et utilise la CLI Java décrite par les ADR-102 et 103. L’extraction sémantique spécialisée décrite ci-dessous constitue un second flux, explicitement limité aux revues Néosanté.

### Extraction sélective spécialisée Néosanté

La cible `neosante-articles-markdown` traite les PDF du corpus de qualification afin d’extraire les passages dont le sujet principal est la **biologie totale** ou le **décodage biologique**. Elle appelle la cible racine `run-extract-neosante-relevant-articles`, laquelle invoque la sous-commande `extract-neosante-relevant-articles`.

Les classes de cette capacité appartiennent au package `net.cotechnoe.kb.genai.document.neosante`, afin d’éviter de présenter cette logique comme un service générique de traitement d’articles. Un modèle distant compatible OpenAI est utilisé uniquement pour classifier la pertinence sémantique des pages non vides ; l’extraction du texte PDF et l’écriture Markdown restent locales.

Chaque résultat conserve le fichier source, la plage de pages, les thèmes et le motif de sélection dans son en-tête Markdown. L’implémentation courante fusionne les pages pertinentes adjacentes : elle ne garantit donc pas encore la détection de frontières entre deux articles pertinents consécutifs.

Les identifiants d’accès au modèle sont lus depuis la configuration générée par le bootstrap ; ils ne sont ni journalisés, ni écrits dans les Markdown, ni versionnés. Une erreur de classification ou de lecture du PDF échoue la revue concernée et ne produit pas son marqueur `.complete`.

### Contrat d’arborescence

| Élément | Valeur par défaut | Contrat |
|---|---|---|
| Entrée | `workspace/08-KB-NéoSanté/collection/` | Fichiers réguliers avec extension `.pdf` |
| Sortie | `workspace/08-KB-NéoSanté/markdown/collection/` | Un `.md` par PDF, même chemin relatif |
| Test | `workspace/08-KB-NéoSanté/collection-test/` | Corpus admis, sélectionné par `SOURCE_DIR` |
| Commande | `make -C workspace markdown` | Transformation locale de la collection configurée |
| Extraction sélective | `make -C workspace neosante-articles-markdown` | Extraction sémantique dédiée aux revues Néosanté |
| Parallélisme | 5 | Cinq workers maximum |

Pour `collection/sous-dossier/revue.pdf`, la sortie attendue est `markdown/collection/sous-dossier/revue.md`.

### Effet sur la chaîne documentaire

| Étape | Entrée | Sortie | Contrôle |
|---|---|---|---|
| Découverte | Répertoire configuré | Liste des PDF | Support des espaces et chemins accentués |
| Dépendance | PDF et Markdown homonyme | Décision de transformation | Transformation seulement si le PDF est plus récent |
| Extraction | PDF local | Texte extrait | CLI Java et adaptateur LangChain4j local |
| Écriture | Texte extrait | Markdown UTF-8 | Création des répertoires parents |
| Publication | Markdown local | Aucun service distant | Hors périmètre de cet ADR |

### Principes appliqués

- **Traçabilité** : le chemin relatif du PDF est conservé dans le chemin du Markdown.
- **Actualité** : la comparaison des dates évite de conserver un artefact obsolète.
- **Moindre privilège** : la chaîne lit et écrit uniquement dans les répertoires configurés.
- **Robustesse** : les chemins sont transmis sans découpage sur les espaces.
- **Réversibilité** : les Markdown peuvent être régénérés depuis les PDF.
- **Non-duplication** : la dépendance et le parallélisme sont portés par Make, leur unique orchestrateur.

## Matrice de décision

| Critère | Poids | Script séquentiel | Make avec règles d’incrémentalité | Make avec cinq workers | Option retenue |
|---|---:|---:|---:|---:|---:|
| Respect des dépendances PDF/MD | 30 % | 5 | 9 | 9 | 9 |
| Gestion des noms de fichiers | 20 % | 6 | 5 | 8 | 8 |
| Temps de traitement | 20 % | 3 | 6 | 9 | 9 |
| Reproductibilité | 15 % | 6 | 9 | 9 | 9 |
| Simplicité d’exploitation | 15 % | 7 | 8 | 7 | 7 |
| **Total pondéré** | **100 %** | **5,20** | **7,55** | **8,45** | **8,45** |

```text
Script séquentiel = 5×0,30 + 6×0,20 + 3×0,20 + 6×0,15 + 7×0,15 = 5,20
Make incrémental  = 9×0,30 + 5×0,20 + 6×0,20 + 9×0,15 + 8×0,15 = 7,55
Make + workers    = 9×0,30 + 8×0,20 + 9×0,20 + 9×0,15 + 7×0,15 = 8,45
```

## Conséquences

### Positives

- les revues inchangées ne sont pas retraitées ;
- les noms contenant des espaces sont supportés ;
- le temps de traitement est réduit par cinq workers maximum ;
- la commande utilisateur reste courte et reproductible ;
- les sorties restent dans le même périmètre fonctionnel que les sources.

### Négatives, risques et mitigations

| Risque | Impact | Mitigation |
|---|---|---|
| La couche texte est absente de certains PDF | Élevé | Identifier les échecs et décider ultérieurement d’un adaptateur OCR |
| Cinq lectures simultanées saturent le montage rclone | Moyen | Conserver le plafond à 5 et le rendre configurable après mesure |
| Les timestamps du montage sont imprécis | Moyen | Ajouter une empreinte de source si la comparaison de dates est insuffisante |
| La sortie contient du texte sans structure visuelle complète | Moyen | Qualifier titres, tableaux et pages sur un corpus représentatif |
| Une revue est supprimée de la collection | Moyen | Prévoir une politique de purge des Markdown orphelins dans un ADR ultérieur |

## Alternatives considérées

### Script shell séquentiel

Simple à écrire, mais il duplique la logique de dépendance et ne tire pas parti de l’orchestration native de Make. Rejeté.

### Transformation systématique de toute la collection

Garantit une sortie recalculée, mais augmente le temps d’exécution et écrase inutilement des artefacts inchangés. Rejetée.

### Parallélisme illimité

Réduit potentiellement la durée, mais risque de saturer Java, le disque ou le montage rclone. Rejeté au profit d’un plafond de cinq.

## Plan d’implémentation

| Phase | Livrables | Validation | Statut |
|---|---|---|---|
| 1. Configuration | Variables d’entrée et de sortie dans `env/default.env` | Bootstrap réussi | Réalisé le 2026-08-28 |
| 2. Orchestration | Cible `make -C workspace markdown` et plafond de 5 workers | Dry-run et transformation locale | Réalisé le 2026-08-28 |
| 3. Qualification | Comparaison PDF/Markdown et noms avec espaces | 151 PDF, 151 Markdown | Réalisé le 2026-08-28 |
| 4. Qualité documentaire | Mesures sur scans, tableaux et structure de page | Corpus de qualification | À faire |
| 5. Nettoyage | Politique des Markdown orphelins | Décision séparée | À faire |

## Critères de succès et validation

- `make -C workspace markdown` découvre tous les PDF du répertoire configuré ;
- `collection-test` est accepté comme corpus de qualification avec `make -C workspace markdown SOURCE_DIR=...` ;
- le corpus de test contient actuellement 10 PDF et peut être exécuté sans retraiter les 151 PDF du corpus complet ;
- un PDF nommé `Recueil1-Jeune 2.pdf` produit `Recueil1-Jeune 2.md` ;
- une seconde exécution sans modification des sources ne relance aucune transformation ;
- un PDF modifié relance uniquement son propre Markdown ;
- le nombre de Markdown produits correspond au nombre de PDF transformables ;
- le nombre maximal de transformations simultanées reste égal à 5 ;
- une erreur est visible et fait échouer la commande globale ;
- l’extraction sélective ne retient que les pages dont le sujet principal concerne les thèmes cibles ;
- ses Markdown indiquent la source et les pages sélectionnées ;
- aucun secret n’est affiché ou versionné ;
- l’appel distant, limité à la classification sémantique du flux Néosanté, est explicitement configuré.

## Déclencheurs de réévaluation

- ajout d’un besoin OCR ou de restitution de tableaux ;
- passage à un autre format que PDF ;
- dépassement mesuré du plafond de cinq workers ;
- besoin de supprimer automatiquement les Markdown orphelins ;
- ajout d’une étape d’indexation ou de publication distante ;
- besoin de distinguer deux articles pertinents présents sur des pages adjacentes ;
- élargissement de l’extraction sémantique à une autre collection que Néosanté.

## Traçabilité et liens

- [ADR-102](./102-ARCH-structure-java-transformation-pdf-markdown.md) — structure Java et transformation locale.
- [ADR-103](./103-ARCH-cli-picocli-architecture-hexagonale.md) — contrat de la CLI.
- [ADR-600](./600-DEVOPS-bootstrap-configuration-management.md) — variables d’environnement et secrets.
- [ADR-602](./602-DEVOPS-makefile-orchestrateur.md) — Makefile comme orchestrateur.
- [ADR-608](./608-DEVOPS-non-duplication-fonctionnelle-transversale.md) — limitation de la duplication.
- [ADR-611](./611-DEVOPS-gestion-couleurs-scripts-make.md) — sorties terminal.

## Historique

| Date | Auteur | Changement | Raison |
|---|---|---|---|
| 2026-08-28 | Équipe kb-genai-builder | Création de l’ADR | Formaliser la transformation des revues présentes dans `collection/` |
