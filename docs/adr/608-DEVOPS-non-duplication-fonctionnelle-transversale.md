---
adr: 608
title: "Non-duplication fonctionnelle transversale"
status: "proposed"
date: 2026-08-27
superseded_by: null
replaces: null
related_adrs: [0, 600, 601, 602, 611]
related_issues: []
classification:
  lifecycle: "proposed"
  domain: "devops"
  impact: "medium"
  quality:
    - "maintainability"
    - "reliability"
    - "traceability"
  reversibility: "easy"
  scope: "tactical"
  tech_areas:
    - "automation"
    - "configuration"
    - "metadata"
    - "documentation"
tags: ["dry", "non-duplication", "refactoring", "single-source"]
stakeholders: ["@architecture-team", "@content-team", "@operations-team"]
effort: "medium"
---

# ADR 608 : Non-duplication fonctionnelle transversale

## Vue d’ensemble

| Attribut | Valeur |
|---|---|
| Statut | Proposé |
| Date | 2026-08-27 |
| Impact | Moyen |
| Réversibilité | Facile |

## Contexte et problème

Dans un système documentaire, une même règle peut apparaître dans la configuration, les scripts, le schéma de métadonnées, la validation, les tests et les guides. Les duplications sensibles concernent notamment les types de documents admis, les identifiants de provenance, les règles de découpage, les filtres de sécurité et les seuils d’évaluation.

Deux dangers opposés existent : laisser diverger des copies représentant la même règle, ou créer trop tôt une abstraction générique qui mélange des responsabilités et rend chaque changement plus risqué.

## Décision

Appliquer la règle suivante :

> Évaluer toute logique fonctionnelle à sa deuxième occurrence. À partir de trois occurrences, l’extraire vers une source canonique ou justifier explicitement pourquoi les copies doivent rester indépendantes.

### Éléments à mutualiser

- valeurs de configuration partagées entre plusieurs consommateurs ;
- schémas, vocabulaires et règles de validation des métadonnées ;
- calcul d’identifiants de document, version, lot ou chunk ;
- redaction des secrets et données sensibles dans les journaux ;
- construction d’un même contrat d’export ou d’intégration ;
- métriques et corpus utilisés comme référence d’évaluation ;
- texte normatif répété dans plusieurs documents.

La source canonique doit être identifiable, versionnée et testée. Les consommateurs dérivés sont générés ou validés contre elle lorsque c’est raisonnable.

### Duplication acceptable

- idiomes courts propres à un langage ;
- assertions de test locales dont la répétition améliore la lecture ;
- résumés documentaires pointant vers la règle complète ;
- représentations produites pour des consommateurs différents lorsque la génération coûterait plus que leur validation ;
- valeurs identiques par hasard mais ayant des cycles de vie distincts.

### Cas spécifique du contenu documentaire

Le principe DRY ne signifie pas dédupliquer aveuglément les documents métiers. Deux contenus identiques peuvent posséder des propriétaires, droits, versions ou dates d’expiration différents. La déduplication physique ne doit jamais supprimer la provenance ni fusionner les autorisations. Une décision DATA ou SEC est requise avant toute mutualisation qui affecte le contenu publié.

### Frontières

1. Le `Makefile` orchestre ; les scripts et outils réalisent le travail.
2. La configuration publique possède une source canonique selon l’ADR-600.
3. Un module partagé n’est créé qu’avec plusieurs appelants réels et un contrat stable.
4. Les ADR portent la règle normative ; les guides la résument et la référencent.
5. Une duplication intentionnelle est commentée lorsque sa raison n’est pas évidente.

## Matrice de décision

| Critère | Poids | Seuil 3 occurrences | Extraction dès 2 | Aucune règle | Zéro duplication absolue |
|---|---:|---:|---:|---:|---:|
| Réduction des divergences | 30 % | 9 | 9 | 3 | 10 |
| Stabilité des abstractions | 25 % | 9 | 6 | 7 | 4 |
| Lisibilité locale | 20 % | 8 | 7 | 8 | 4 |
| Coût d’entretien | 15 % | 8 | 6 | 5 | 3 |
| Respect des frontières | 10 % | 9 | 7 | 5 | 3 |
| **Total pondéré** | **100 %** | **8,65** | **7,20** | **5,50** | **5,55** |

```text
Seuil 3       = 9×0,30 + 9×0,25 + 8×0,20 + 8×0,15 + 9×0,10 = 8,65
Extraction 2  = 9×0,30 + 6×0,25 + 7×0,20 + 6×0,15 + 7×0,10 = 7,20
Aucune règle  = 3×0,30 + 7×0,25 + 8×0,20 + 5×0,15 + 5×0,10 = 5,50
Zéro absolu   = 10×0,30 + 4×0,25 + 4×0,20 + 3×0,15 + 3×0,10 = 5,55
```

## Conséquences

### Positives

- sources canoniques explicites pour les règles critiques ;
- moins de divergence entre bibliothèque, tests et documentation ;
- seuil concret empêchant les abstractions prématurées ;
- provenance et droits préservés lors de la déduplication documentaire.

### Négatives

- le seuil nécessite du jugement ;
- une abstraction partagée augmente son rayon d’impact ;
- certaines copies inter-langages restent nécessaires ;
- la génération d’artefacts ajoute un contrôle à maintenir.

## Alternatives considérées

### Extraire dès la deuxième occurrence

Réduit rapidement les copies, mais deux exemples ne suffisent pas toujours à révéler un contrat stable.

### Ne fixer aucune règle

Laisse le jugement local primer, au prix de divergences découvertes tardivement.

### Interdire toute duplication

Confond similitude syntaxique, duplication fonctionnelle et représentations propres aux consommateurs.

## Plan d’implémentation

| Phase | Action | Validation | Statut |
|---|---|---|---|
| 1 | Faire accepter cet ADR | Revue des stakeholders | À faire |
| 2 | Identifier les sources canoniques lors de la création de la bibliothèque | Revue d’architecture | À faire |
| 3 | Ajouter des validations de cohérence aux frontières | Tests de divergence | À faire |
| 4 | Revoir chaque troisième occurrence détectée | Extraction ou justification | Continu |

## Critères de succès

- toute logique non triviale présente au moins trois fois est extraite ou justifiée ;
- aucun helper partagé sans plusieurs appelants réels ;
- chaque règle critique possède une source canonique identifiable ;
- aucune déduplication de contenu ne perd provenance, version ou droits ;
- un changement normatif ne nécessite pas de corrections contradictoires dans plusieurs guides.

## Traçabilité et liens

- [ADR-600](./600-DEVOPS-bootstrap-configuration-management.md) — configuration canonique.
- [ADR-601](./601-DEVOPS-nomenclature-scripts.md) — scripts et modules partagés.
- [ADR-602](./602-DEVOPS-makefile-orchestrateur.md) — frontière d’orchestration.
- [ADR-611](./611-DEVOPS-gestion-couleurs-scripts-make.md) — mutualisation des sorties.

## Historique

| Date | Auteur | Changement | Raison |
|---|---|---|---|
| 2026-08-27 | Équipe kb-genai-builder | Import et adaptation de l’ADR-608 source | Étendre la non-duplication aux métadonnées, règles documentaires et exigences de provenance |
