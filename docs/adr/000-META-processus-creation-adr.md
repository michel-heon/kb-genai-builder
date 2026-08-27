---
adr: 0
title: "Processus de création et de gestion des ADR"
status: "accepted"
date: 2026-08-27
superseded_by: null
replaces: null
related_adrs: []
related_issues: []
classification:
  lifecycle: "accepted"
  domain: "meta"
  impact: "critical"
  quality:
    - "maintainability"
    - "compliance"
    - "reliability"
  reversibility: "hard"
  scope: "strategic"
  tech_areas:
    - "documentation"
    - "adr"
    - "knowledge-base"
    - "generative-ai"
tags: ["kb-genai-builder", "process", "documentation", "governance", "ai-ready"]
stakeholders: ["@project-owner", "@architecture-team", "@content-team"]
effort: "low"
---

# ADR 000 : Processus de création et de gestion des ADR

## Documents complémentaires obligatoires

Le système ADR de **kb-genai-builder** est constitué de quatre fichiers à maintenir ensemble :

1. [ADR-000](./000-META-processus-creation-adr.md) — processus et règles ;
2. [TAXONOMY.md](./TAXONOMY.md) — classification détaillée ;
3. [adr-template-ai-optimized.md](./adr-template-ai-optimized.md) — modèle de rédaction ;
4. [README.md](./README.md) — index et guide rapide.

Toute modification des statuts, domaines, plages de numérotation ou champs obligatoires doit être répercutée dans les quatre fichiers.

## Vue d’ensemble

| Attribut | Valeur |
|---|---|
| Statut | Accepté |
| Date de décision | 2026-08-27 |
| Impact | Critique |
| Portée | Stratégique |
| Effort d’adoption | Faible |

## Contexte

**kb-genai-builder** est une bibliothèque d’utilitaires qui transforme des documents en bases de connaissances exploitables par des systèmes d’IA générative. Elle couvre les briques réutilisables d’acquisition, d’extraction, de normalisation, de découpage, d’enrichissement des métadonnées, de validation et d’export. Les moteurs d’indexation, applications RAG et agents consommateurs s’intègrent par des contrats explicites sans être imposés par le cœur de la bibliothèque.

Ces choix ont un effet direct sur la capacité d’un système consommateur à retrouver une information fiable, récente, autorisée et rattachée à sa source. Sans historique formel, une évolution de la chaîne de transformation, de l’API publique ou du modèle de contenu peut dégrader silencieusement les bases produites, rompre la compatibilité avec leurs consommateurs ou exposer des contenus à un public non prévu.

Les ADR capturent le **pourquoi** des décisions structurantes. Ils complètent le code, les configurations et la documentation opératoire, sans les remplacer.

## Décision

Nous adoptons un processus ADR inspiré du modèle de Michael Nygard et adapté à un système documentaire assisté par IA.

Chaque ADR doit :

- posséder un frontmatter YAML conforme à la taxonomie ;
- décrire le problème, la décision, les alternatives et les conséquences ;
- identifier les sources projet effectivement consultées ;
- expliciter l’effet sur la provenance, la sécurité et la qualité de récupération lorsque ces aspects sont concernés ;
- définir des critères de succès vérifiables ;
- être référencé dans [README.md](./README.md).

### Principes propres à kb-genai-builder

1. **Provenance avant génération** : toute information publiée doit rester rattachable à son document source et, si possible, à son emplacement dans ce document.
2. **Droits conservés de bout en bout** : une transformation ou une indexation ne doit pas élargir implicitement le lectorat d’un contenu.
3. **Contrats vérifiables** : les affirmations sur un format, une API ou un service externe doivent être vérifiées dans la documentation officielle en vigueur et datées dans l’ADR.
4. **Séparation des responsabilités** : acquisition, transformation, contrôle qualité, export, indexation et consommation sont distingués. Le cœur de la bibliothèque ne dépend pas d’un consommateur particulier.
5. **Mesure avant optimisation** : toute modification de recherche ou de découpage définit un corpus d’évaluation et des métriques avant d’être acceptée.
6. **Pas d’invention par l’IA** : un agent peut aider à rédiger un ADR, mais doit signaler toute hypothèse ou information non vérifiée.

### Structure obligatoire

Le modèle canonique est [adr-template-ai-optimized.md](./adr-template-ai-optimized.md). Les sections minimales sont :

- Vue d’ensemble ;
- Contexte et problème ;
- Décision ;
- Matrice de décision, ou justification explicite de son absence ;
- Conséquences positives et négatives ;
- Alternatives considérées ;
- Plan d’implémentation ;
- Critères de succès et de réévaluation ;
- Traçabilité et liens.

### Convention de nommage

Format : `XXX-CATEGORIE-titre-kebab-case.md`.

| Préfixe | Plage | Domaine | Exemples de décisions |
|---|---:|---|---|
| `META` | 000–099 | Gouvernance | Processus ADR, règles d’usage de l’IA |
| `ARCH` | 100–199 | Architecture | Découpage de la bibliothèque et de la chaîne de transformation |
| `INFRA` | 200–299 | Infrastructure | Stockage, exécution, déploiement, observabilité |
| `SEC` | 300–399 | Sécurité | Classification, droits, secrets, données sensibles |
| `DATA` | 400–499 | Données | Extraction, normalisation, chunking, métadonnées, index |
| `API` | 500–599 | Interfaces | API publique, connecteurs et contrats d’échange |
| `DEVOPS` | 600–699 | DevOps | CI/CD, automatisation, configuration, versionnement |
| `TEST` | 700–799 | Tests et qualité | Corpus d’évaluation, pertinence, non-régression |
| `BIZ` | 800–899 | Produit et métier | Périmètre, valeur, gouvernance métier, coûts |
| `DOC` | 900–999 | Documentation | Conventions éditoriales, guides, documentation opératoire |

Le numéro progresse chronologiquement dans la plage du domaine principal. Si plusieurs domaines sont touchés, le frontmatter et les liens vers les ADR connexes rendent les impacts secondaires explicites.

### Cycle de vie

| Statut | Valeur YAML | Usage |
|---|---|---|
| Brouillon | `draft` | Rédaction en cours |
| Proposé | `proposed` | Prêt pour revue |
| Accepté | `accepted` | Décision approuvée et en vigueur |
| Rejeté | `rejected` | Proposition refusée et conservée |
| Déprécié | `deprecated` | Décision obsolète, sans remplacement direct |
| Supersédé | `superseded` | Décision remplacée par un autre ADR |

Les champs `status` et `classification.lifecycle` doivent toujours être identiques.

## Processus de création

### 1. Déterminer si un ADR est requis

Créer un ADR pour un choix qui :

- structure plusieurs composants de la bibliothèque ou étapes de la chaîne documentaire ;
- modifie un contrat, un format persistant ou un modèle de métadonnées ;
- affecte la provenance, les autorisations, la confidentialité ou la conformité ;
- modifie l’API publique, un format d’entrée ou de sortie, ou les attentes envers un consommateur ;
- change la stratégie de découpage, d’indexation, de recherche ou d’évaluation ;
- engage durablement des coûts, un fournisseur ou une dépendance ;
- présente plusieurs alternatives raisonnables.

Un ADR n’est généralement pas requis pour une correction locale, une mise à jour éditoriale sans effet sémantique, une tâche réversible de routine ou une expérimentation explicitement jetable.

### 2. Créer et indexer le fichier

```bash
cd docs/adr

# Exemple : prochain ADR DATA
ls -1 4??-DATA-*.md 2>/dev/null | tail -1
cp adr-template-ai-optimized.md 400-DATA-titre-decision.md

# Ajouter immédiatement l’ADR à README.md avec son statut courant.
```

### 3. Rédiger sur la base de faits observés

Avant de rédiger, inspecter les fichiers, configurations, schémas et jeux de tests concernés. Pour une décision dépendant d’un produit externe, consulter sa documentation officielle actuelle. Consigner les références et distinguer clairement :

- les faits vérifiés ;
- les objectifs ou contraintes du projet ;
- les hypothèses à valider ;
- la décision proposée.

Ne pas inventer de composant, d’API, de licence, de limite de service ou de comportement du produit cible. Une incertitude restante est marquée `À VÉRIFIER` et empêche l’acceptation si elle est déterminante.

### 4. Revoir et accepter

Un ADR peut passer à `accepted` lorsque :

- le YAML est complet et cohérent ;
- aucun placeholder ni hypothèse déterminante non résolue ne subsiste ;
- au moins une alternative crédible a été évaluée, sauf décision purement procédurale ;
- les impacts sur provenance, droits d’accès, cycle de vie documentaire et qualité de recherche ont été évalués lorsqu’ils s’appliquent ;
- des critères de succès mesurables sont définis ;
- une revue par au moins un stakeholder est réalisée pour un impact `high` ou `critical` ;
- l’index est à jour.

### 5. Mettre en œuvre et vérifier

L’acceptation autorise la mise en œuvre ; elle ne prouve pas que celle-ci est terminée. Le plan d’implémentation et les critères de succès doivent permettre de relier la décision aux changements, tests et mesures correspondants.

### 6. Faire évoluer une décision

Une correction de forme ou l’ajout d’un lien peut modifier un ADR accepté. En revanche, ne pas réécrire sa décision : créer un nouvel ADR, passer l’ancien à `superseded`, renseigner `superseded_by` et ajouter les liens croisés.

## Conséquences

### Positives

- décisions retrouvables par les humains et les agents IA ;
- traçabilité entre architecture, contenu, sécurité et qualité ;
- réduction des hypothèses implicites lors des évolutions de la bibliothèque et de sa chaîne de transformation ;
- meilleure maîtrise des effets sur les réponses fondées sur la base de connaissances.

### Négatives

- effort de rédaction et de revue ;
- nécessité de maintenir quatre documents cohérents ;
- risque de produire des ADR trop lourds pour des décisions locales.

### Mitigations

- utiliser le template et supprimer les sections non applicables en justifiant leur retrait ;
- réserver les ADR aux décisions durables ;
- automatiser ultérieurement la validation du frontmatter et de l’index.

## Critères de succès

| Métrique | Cible |
|---|---:|
| ADR possédant un frontmatter conforme | 100 % |
| ADR présents dans l’index | 100 % |
| ADR acceptés avec critères de succès mesurables | 100 % |
| ADR DATA/API/SEC évaluant provenance et droits lorsque pertinents | 100 % |
| Placeholders dans les ADR acceptés | 0 |

## Références

- [Architecture Decision Records](https://adr.github.io/)
- [Michael Nygard — Documenting Architecture Decisions](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
- [Taxonomie du projet](./TAXONOMY.md)
- [Template du projet](./adr-template-ai-optimized.md)

## Historique

| Date | Auteur | Changement | Raison |
|---|---|---|---|
| 2026-08-27 | Équipe kb-genai-builder | Import et adaptation du cadre ADR source | Gouverner les décisions d’une bibliothèque transformant des documents en bases de connaissances pour l’IA générative |
