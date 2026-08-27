---
adr: 3
title: "Création et usage des GitHub Issues"
status: "proposed"
date: 2026-08-27
superseded_by: null
replaces: null
related_adrs: [0]
related_issues: []
classification:
  lifecycle: "proposed"
  domain: "meta"
  impact: "medium"
  quality:
    - "maintainability"
    - "reliability"
    - "traceability"
    - "usability"
  reversibility: "easy"
  scope: "tactical"
  tech_areas:
    - "git"
    - "documentation"
    - "adr"
tags: ["github", "issues", "triage", "workflow", "templates"]
stakeholders: ["@project-owner", "@architecture-team", "@content-team"]
effort: "low"
---

# ADR 003 : Création et usage des GitHub Issues

## Vue d’ensemble

| Attribut | Valeur |
|---|---|
| Statut | Proposé |
| Date de décision | 2026-08-27 |
| Impact | Moyen |
| Réversibilité | Facile |
| Portée | Gouvernance du travail |

## Contexte et problème

Le projet **kb-genai-builder** doit coordonner des travaux de nature différente : acquisition de documents, extraction, normalisation, contrôle de provenance, création d’artefacts de base de connaissances, évaluation, sécurité et intégrations optionnelles. Une liste informelle ne suffit pas à relier chaque travail à sa justification, à son périmètre, à ses validations et aux décisions durables consignées dans les ADR.

Le répertoire observé lors de l’import ne contient pas de métadonnées Git ni de configuration GitHub. Cet ADR définit donc le workflow cible pour le moment où le projet sera hébergé dans un dépôt GitHub avec les Issues activées ; il ne prétend pas que ce dépôt ou ses labels existent déjà.

La documentation GitHub consultée le 2026-08-27 confirme que les Issues servent à planifier et suivre le travail, peuvent porter des métadonnées, être décomposées en sous-issues et exprimer des dépendances. GitHub permet aussi de fournir des templates ou formulaires dans `.github/ISSUE_TEMPLATE/`.

Sans règles locales, les risques sont les suivants :

- demandes trop larges telles que « construire la base de connaissances » ;
- confusion entre travail actionnable, décision durable et exploration ;
- critères d’acceptation absents ou non mesurables ;
- oubli des exigences de provenance, de droits et de suppression ;
- clôture sans preuve de test ni lien vers l’ADR ou le changement associé ;
- création par un agent IA à partir de capacités ou d’états non vérifiés.

## Décision

Lorsque le dépôt GitHub du projet est disponible, utiliser **GitHub Issues comme registre canonique du travail actionnable**. Une issue décrit un résultat à produire ; un ADR explique une décision durable ; une discussion ou une note d’exploration cadre une question qui n’est pas encore actionnable.

### Vérification avant création

Avant d’ouvrir une issue :

1. rechercher les issues ouvertes et fermées avec les termes discriminants ;
2. lire les ADR, fichiers et résultats de tests concernés ;
3. vérifier que le problème ou le besoin est observé ;
4. préférer la mise à jour d’une issue existante si son résultat attendu est identique ;
5. signaler les hypothèses restantes, sans les transformer en faits.

Un agent IA ne crée aucune issue externe sans demande explicite ou autorisation inscrite dans le workflow courant. Il vérifie le dépôt, les droits et les métadonnées disponibles avant toute création.

### Granularité

Une issue vise un seul résultat principal, validable indépendamment. Un sujet couvrant plusieurs résultats est découpé en sous-issues ou en dépendances explicites. Une checklist interne suffit lorsque les étapes partagent le même livrable et la même validation.

Exemples adaptés au projet :

- bon : « conserver l’identifiant de source dans chaque chunk produit » ;
- bon : « mesurer recall@5 sur le corpus documentaire de référence » ;
- trop large : « implémenter la bibliothèque documentaire » ;
- non actionnable : « réfléchir aux intégrations avec les agents d’IA ».

### Contenu minimal

Toute issue non triviale contient :

- un titre orienté résultat ;
- le contexte vérifié et les références utilisées ;
- le périmètre inclus et le hors-périmètre ;
- les dépendances et éventuels ADR liés ;
- un plan de réalisation proportionné au risque ;
- des critères d’acceptation observables ;
- les contrôles de provenance, sécurité, fraîcheur et qualité applicables ;
- les preuves attendues avant clôture.

Modèle recommandé :

````markdown
## Objectif
[Résultat attendu en une phrase]

## Contexte vérifié
- [ADR, fichier, test ou observation]

## Périmètre
- [ ] [Livrable]

## Hors périmètre
- [Élément exclu]

## Plan
1. [Étape et sortie]
2. [Étape et sortie]

## Critères d’acceptation
- [ ] [Comportement ou mesure vérifiable]
- [ ] [Contrôle de provenance ou de droits, si applicable]

## Preuves de clôture
- [ ] Tests ou mesures joints
- [ ] Documentation mise à jour
- [ ] ADR relié si une décision durable a été prise

## Références
- [Lien vers l’artefact vérifié]
````

### Métadonnées

Utiliser les assignees, labels, milestones, types et Projects lorsqu’ils sont réellement configurés et utiles. Le corps conserve le sens fonctionnel de l’issue ; les métadonnées facilitent le filtrage mais ne le remplacent pas.

Les noms de labels, types et milestones doivent être lus depuis le dépôt avant emploi. Cet ADR ne crée pas implicitement une taxonomie GitHub.

### Traçabilité et clôture

Pendant la réalisation, l’issue référence les décisions découvertes, changements, validations et risques restants. Avant clôture :

- le livrable est présent dans la branche d’intégration retenue par le projet ;
- les critères d’acceptation sont vérifiés ;
- les commandes, mesures ou preuves sont consignées ;
- tout écart ou limite connue est documenté ;
- l’ADR correspondant est lié si le travail a introduit une décision durable.

Une issue abandonnée, dupliquée ou devenue non pertinente est close avec la raison réelle, jamais comme si le résultat avait été livré.

### Issue, discussion et ADR

| Support | Usage |
|---|---|
| Issue | Résultat actionnable et critères d’acceptation |
| Discussion ou note | Exploration, question ouverte, collecte d’avis |
| ADR | Décision durable, alternatives et conséquences |

## Matrice de décision

| Critère | Poids | Liste libre | ADR seuls | Outil séparé | Issues structurées |
|---|---:|---:|---:|---:|---:|
| Traçabilité | 30 % | 4 | 6 | 7 | 9 |
| Actionnabilité | 25 % | 4 | 4 | 8 | 9 |
| Lien avec les changements | 20 % | 5 | 5 | 5 | 9 |
| Simplicité | 15 % | 9 | 6 | 4 | 8 |
| Contrôle des faits | 10 % | 4 | 7 | 6 | 9 |
| **Total pondéré** | **100 %** | **5,00** | **5,40** | **6,30** | **8,85** |

```text
Liste libre        = 4×0,30 + 4×0,25 + 5×0,20 + 9×0,15 + 4×0,10 = 5,00
ADR seuls          = 6×0,30 + 4×0,25 + 5×0,20 + 6×0,15 + 7×0,10 = 5,40
Outil séparé       = 7×0,30 + 8×0,25 + 5×0,20 + 4×0,15 + 6×0,10 = 6,30
Issues structurées = 9×0,30 + 9×0,25 + 9×0,20 + 8×0,15 + 9×0,10 = 8,85
```

## Conséquences

### Positives

- chaque travail important possède un objectif et une preuve de fin ;
- les liens entre besoin, ADR, changement et validation deviennent explicites ;
- les exigences documentaires et de sécurité sont examinées au bon moment ;
- le format est exploitable par les humains et les agents IA.

### Négatives et mitigations

| Risque | Impact | Mitigation |
|---|---|---|
| Temps de rédaction | Moyen | Adapter le niveau de détail au risque |
| Issues trop volumineuses | Moyen | Découper par résultats indépendants |
| Métadonnées obsolètes | Faible | Vérifier les valeurs dans GitHub avant usage |
| Processus appliqué mécaniquement | Moyen | Évaluer la qualité des critères, pas seulement leur présence |

## Alternatives considérées

### Liste libre

Simple au démarrage, mais insuffisante pour les dépendances, validations et liens avec les décisions.

### ADR comme unique outil de pilotage

Les ADR expliquent les décisions ; ils ne conviennent pas au suivi quotidien des tâches, anomalies et validations.

### Outil de suivi distinct

Possible si une contrainte d’organisation l’impose, mais ajoute une synchronisation avec le dépôt. Ce choix nécessiterait un ADR de remplacement.

## Plan d’implémentation

| Phase | Livrable | Validation | Statut |
|---|---|---|---|
| 1 | Faire accepter cet ADR | Revue par les stakeholders | À faire |
| 2 | Initialiser le dépôt GitHub et vérifier que les Issues sont activées | URL du dépôt et test de création | À faire |
| 3 | Créer les templates minimaux | Validation du YAML et rendu GitHub | À faire |
| 4 | Appliquer le format aux nouvelles issues | Revue de triage | À faire |
| 5 | Mesurer puis ajuster le processus | Bilan après 10 issues closes | À faire |

## Critères de succès et validation

| Critère | Cible | Mesure |
|---|---:|---|
| Issues importantes avec objectif et acceptation | 100 % | Échantillon de triage |
| Issues closes avec preuve ou motif réel | 100 % | Audit des clôtures |
| Travaux mettant en œuvre une décision reliés à un ADR | 100 % | Recherche des références |
| Doublons détectés après démarrage | Moins de 10 % | Motifs de clôture |
| Métadonnées inventées par automatisation | 0 | Audit des créations automatiques |

Réévaluer après les dix premières issues closes, ou si le projet quitte GitHub.

## Traçabilité et liens

### ADR liés

- [ADR-000](./000-META-processus-creation-adr.md) — processus de décision et vérification des faits.

### Sources projet consultées

| Source | Date | Observation |
|---|---|---|
| `docs/adr/` | 2026-08-27 | Seul ensemble d’artefacts présent dans le répertoire cible lors de l’import |

### Documentation externe

- [GitHub Docs — About issues](https://docs.github.com/en/issues/tracking-your-work-with-issues/learning-about-issues/about-issues), consulté le 2026-08-27.
- [GitHub Docs — Configuring issue templates](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/configuring-issue-templates-for-your-repository), consulté le 2026-08-27.

## Historique

| Date | Auteur | Changement | Raison |
|---|---|---|---|
| 2026-08-27 | Équipe kb-genai-builder | Import et adaptation de l’ADR-003 source | Définir un suivi actionnable pour la bibliothèque documentaire sans supposer l’existence préalable du dépôt GitHub |
