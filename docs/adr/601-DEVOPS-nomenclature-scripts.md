---
adr: 601
title: "Nomenclature des scripts et commandes d’automatisation"
status: "proposed"
date: 2026-08-27
superseded_by: null
replaces: null
related_adrs: [0, 600, 602, 608, 611]
related_issues: []
classification:
  lifecycle: "proposed"
  domain: "devops"
  impact: "low"
  quality:
    - "maintainability"
    - "usability"
    - "portability"
  reversibility: "easy"
  scope: "tactical"
  tech_areas:
    - "automation"
    - "python"
    - "powershell"
    - "configuration"
tags: ["devops", "scripts", "naming", "automation"]
stakeholders: ["@architecture-team", "@operations-team"]
effort: "low"
---

# ADR 601 : Nomenclature des scripts et commandes d’automatisation

## Vue d’ensemble

| Attribut | Valeur |
|---|---|
| Statut | Proposé |
| Date | 2026-08-27 |
| Impact | Faible |
| Réversibilité | Facile |

## Contexte et problème

Le projet ne possède pas encore de répertoire `scripts/` observable. Le développement de la bibliothèque devra néanmoins automatiser des opérations telles que la découverte des sources, l’extraction, la validation des métadonnées, la construction d’artefacts et leur évaluation.

Sans convention, les noms risquent de mélanger verbes et objets, tirets et underscores, étapes métier et numéros temporaires. La découvrabilité baisse alors que les références depuis le `Makefile`, la CI, les issues et la documentation deviennent fragiles.

## Décision

### Scripts exécutables

Nommer les scripts selon :

```text
{objet}-{action}.{extension}
{objet}-{action}-{qualificatif}.{extension}
```

Exemples de cibles, à créer uniquement lorsque leur fonctionnalité existe :

```text
environment-validate.py
documents-extract.py
metadata-validate.py
corpus-evaluate.py
knowledge-base-build.py
knowledge-base-export.py
```

Règles :

1. caractères ASCII minuscules dans le nom de fichier ;
2. mots séparés par des tirets ;
3. objet ou périmètre en premier, action observable ensuite ;
4. extension correspondant à l’interpréteur réel (`.py`, `.sh`, `.ps1`) ;
5. aucun nom vague comme `utils`, `run` ou `process` sans objet ;
6. aucun numéro de phase ou de priorité dans le nom ;
7. l’ordre d’exécution est exprimé par l’orchestrateur, pas par le tri lexical ;
8. un renommage met à jour appels, tests et documentation dans le même changement.

Les verbes recommandés sont précis : `discover`, `extract`, `normalize`, `validate`, `build`, `evaluate`, `export`, `publish`, `purge`, `migrate`. `publish` est réservé à un effet distant explicite. Un nouveau terme est choisi pour décrire un comportement distinct, pas comme synonyme local.

### Bibliothèques et modules partagés

Un fichier importé ou sourcé mais non exécuté directement suit les conventions du langage et réside dans un sous-répertoire explicite, par exemple `scripts/lib/`. Il n’imite pas le nom d’une commande publique. Sa création respecte le seuil de mutualisation de l’ADR-608.

### Commandes Make

Les cibles publiques utilisent le kebab-case et expriment une intention stable :

```text
help
bootstrap
config-check
validate
test
build
evaluate
export
clean
```

Une cible et le script auquel elle délègue partagent le même vocabulaire métier. Une cible peut être plus courte lorsqu’elle représente l’action utilisateur principale, par exemple `make build` appelant `knowledge-base-build.py`. Une publication distante relève d’un adaptateur optionnel et utilise un nom qualifié propre à la cible.

### Options

Les options longues utilisent le kebab-case (`--source-dir`, `--dry-run`). Les variables d’environnement utilisent `KB_GENAI_BUILDER_` suivi de mots en majuscules séparés par des underscores. Une option destructive ou publiant vers un service externe possède un nom explicite et n’est jamais activée implicitement par un nom ambigu.

## Matrice de décision

| Critère | Poids | Objet-action | Action-objet | Noms libres | Numéros séquentiels |
|---|---:|---:|---:|---:|---:|
| Découvrabilité par domaine | 30 % | 9 | 7 | 4 | 5 |
| Cohérence Make/scripts | 25 % | 9 | 8 | 4 | 5 |
| Stabilité dans le temps | 20 % | 9 | 9 | 5 | 3 |
| Lisibilité | 15 % | 8 | 9 | 6 | 6 |
| Facilité de validation | 10 % | 9 | 9 | 3 | 8 |
| **Total pondéré** | **100 %** | **8,85** | **8,15** | **4,40** | **5,05** |

```text
Objet-action         = 9×0,30 + 9×0,25 + 9×0,20 + 8×0,15 + 9×0,10 = 8,85
Action-objet         = 7×0,30 + 8×0,25 + 9×0,20 + 9×0,15 + 9×0,10 = 8,15
Noms libres          = 4×0,30 + 4×0,25 + 5×0,20 + 6×0,15 + 3×0,10 = 4,40
Numéros séquentiels  = 5×0,30 + 5×0,25 + 3×0,20 + 6×0,15 + 8×0,10 = 5,05
```

## Conséquences

### Positives

- scripts regroupés naturellement par objet ;
- correspondance lisible entre commandes, documentation et automatisation ;
- renommages de phases ou priorités évités ;
- validation automatique possible sans connaître la logique interne.

### Négatives

- certains écosystèmes imposent leurs propres conventions ;
- les noms peuvent devenir longs pour un objet très qualifié ;
- une migration coordonnée est requise lors d’un renommage.

Une convention imposée par un outil externe prévaut dans son périmètre et doit être documentée localement.

## Alternatives considérées

### Action avant objet

Lisible comme commande, mais regroupe moins bien les scripts partageant un domaine documentaire. Les cibles Make conservent néanmoins naturellement cette forme courte.

### Noms libres

Réduit la réflexion initiale mais dégrade la recherche et l’automatisation des contrôles.

### Préfixes numériques

Acceptables uniquement lorsqu’un outil impose réellement l’ordre lexical. Ils ne doivent pas représenter un ordre métier que Make peut exprimer.

## Plan d’implémentation

| Phase | Action | Validation | Statut |
|---|---|---|---|
| 1 | Faire accepter la convention | Revue des stakeholders | À faire |
| 2 | Créer `scripts/` lors du premier besoin réel | Premier script conforme | À faire |
| 3 | Ajouter un contrôle de nommage | Cas conformes et non conformes testés | À faire |
| 4 | Vérifier les références lors des renommages | Aucun appel orphelin | Continu |

## Critères de succès

- 100 % des scripts publics respectent la convention ou documentent une contrainte externe ;
- aucun ordre de transformation ne dépend d’un préfixe numérique non contractuel ;
- toute cible publique apparaît dans `make help` ;
- aucun renommage ne laisse de référence orpheline dans la CI ou la documentation.

## Traçabilité et liens

- [ADR-600](./600-DEVOPS-bootstrap-configuration-management.md) — configuration et bootstrap.
- [ADR-602](./602-DEVOPS-makefile-orchestrateur.md) — interface de commandes.
- [ADR-608](./608-DEVOPS-non-duplication-fonctionnelle-transversale.md) — modules partagés.
- [ADR-611](./611-DEVOPS-gestion-couleurs-scripts-make.md) — sorties terminal.

## Historique

| Date | Auteur | Changement | Raison |
|---|---|---|---|
| 2026-08-27 | Équipe kb-genai-builder | Import et adaptation de l’ADR-601 source | Définir des noms adaptés aux utilitaires documentaires et indépendants d’un consommateur particulier |
