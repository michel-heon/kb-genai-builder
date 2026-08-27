---
adr: 602
title: "Makefile comme orchestrateur standard"
status: "proposed"
date: 2026-08-27
superseded_by: null
replaces: null
related_adrs: [0, 600, 601, 608, 611]
related_issues: []
classification:
  lifecycle: "proposed"
  domain: "devops"
  impact: "medium"
  quality:
    - "maintainability"
    - "usability"
    - "reliability"
    - "portability"
  reversibility: "moderate"
  scope: "tactical"
  tech_areas:
    - "automation"
    - "ci-cd"
    - "configuration"
    - "evaluation"
tags: ["devops", "makefile", "orchestration", "automation"]
stakeholders: ["@architecture-team", "@content-team", "@operations-team"]
effort: "medium"
---

# ADR 602 : Makefile comme orchestrateur standard

## Vue d’ensemble

| Attribut | Valeur |
|---|---|
| Statut | Proposé |
| Date | 2026-08-27 |
| Impact | Moyen |
| Réversibilité | Modérée |

## Contexte et problème

Le développement de la bibliothèque comportera plusieurs outils et étapes. Les commandes de validation, test, construction d’exemples, évaluation et export risquent d’être copiées dans la documentation et la CI avec des paramètres divergents.

Aucun `Makefile` ni script n’est présent lors de l’import. Cet ADR décide d’une façade à introduire progressivement ; il ne présume ni du langage principal ni des commandes internes futures.

## Décision

Adopter un **`Makefile` racine comme interface d’orchestration standard** pour les opérations répétables de développement du projet. Cette façade ne remplace ni l’API publique de la bibliothèque ni sa future interface en ligne de commande.

### Responsabilités

Le `Makefile` :

- expose des cibles stables et documentées ;
- exprime les dépendances simples entre opérations ;
- vérifie les prérequis immédiats ;
- transmet les paramètres de manière explicite ;
- délègue la logique réelle aux outils et scripts adaptés.

Le `Makefile` ne contient pas :

- de parsing documentaire ;
- de logique de chunking, d’indexation ou d’évaluation ;
- de gestion de secrets ;
- de règles métier complexes ;
- de duplication d’une commande maintenue par l’outil de build ou de test.

Une recette qui dépasse un enchaînement court ou nécessite des branches complexes est déplacée dans un script nommé selon l’ADR-601.

### Interface cible

| Cible | Intention | Effet externe par défaut |
|---|---|---|
| `help` | Afficher les commandes et variables publiques | Aucun |
| `bootstrap` | Préparer l’environnement local | Local uniquement |
| `config-check` | Valider la configuration | Aucun |
| `validate` | Contrôler documents, schémas et ADR | Aucun |
| `test` | Exécuter les tests automatisés | Aucun service externe par défaut |
| `build` | Transformer les sources configurées en artefacts locaux de base de connaissances | Écriture locale |
| `evaluate` | Mesurer la chaîne de transformation sur le corpus de référence | Lecture/écriture locale |
| `export` | Produire un format d’échange local explicitement configuré | Écriture locale |
| `clean` | Supprimer uniquement les artefacts recréables connus | Local, périmètre borné |

Ces cibles sont un contrat de nommage pour les contributeurs. Elles ne doivent être ajoutées que lorsqu’un besoin réel existe. Chaque mécanisme de publication distante vers un consommateur sera défini par un ADR API dédié et fourni par un adaptateur optionnel.

### Sécurité et reproductibilité

- aucune cible ne contacte un service externe par défaut ;
- toute publication distante exige une cible explicite et un mode de prévisualisation sans effet ;
- `clean` ne reçoit jamais un chemin large ou non résolu ;
- les secrets proviennent du mécanisme défini par l’ADR-600 ;
- la CI appelle les mêmes cibles publiques que les développeurs lorsque leurs contraintes sont identiques ;
- les sorties suivent l’ADR-611 et restent analysables sans couleur.

### Portabilité

`make bootstrap` vérifie la présence d’une implémentation compatible de Make et fournit un diagnostic clair. Si l’environnement de développement retenu ne permet pas raisonnablement cette dépendance, un nouvel ADR devra remplacer ou compléter ce choix par une façade équivalente ; les scripts sous-jacents restent directement exécutables.

## Matrice de décision

| Critère | Poids | Make | Scripts seuls | Documentation brute | Task runner additionnel |
|---|---:|---:|---:|---:|---:|
| Découvrabilité | 25 % | 9 | 6 | 4 | 9 |
| Faible couplage au langage | 20 % | 9 | 7 | 9 | 8 |
| Reproductibilité | 25 % | 9 | 7 | 4 | 9 |
| Dépendances à installer | 15 % | 7 | 8 | 10 | 5 |
| Simplicité CI | 15 % | 9 | 7 | 5 | 8 |
| **Total pondéré** | **100 %** | **8,70** | **6,90** | **6,05** | **8,05** |

```text
Make          = 9×0,25 + 9×0,20 + 9×0,25 + 7×0,15 + 9×0,15 = 8,70
Scripts       = 6×0,25 + 7×0,20 + 7×0,25 + 8×0,15 + 7×0,15 = 6,90
Documentation = 4×0,25 + 9×0,20 + 4×0,25 + 10×0,15 + 5×0,15 = 6,05
Task runner   = 9×0,25 + 8×0,20 + 9×0,25 + 5×0,15 + 8×0,15 = 8,05
```

## Conséquences

### Positives

- une porte d’entrée commune pour humains et CI ;
- commandes internes remplaçables sans modifier l’interface publique ;
- documentation plus courte et moins sujette à divergence ;
- séparation entre orchestration et traitement documentaire.

### Négatives et mitigations

| Risque | Mitigation |
|---|---|
| Make absent de certains postes | Vérification au bootstrap et prérequis documenté |
| Recettes devenant du code applicatif | Déporter dans `scripts/` |
| Cible cachant un effet externe | Nommage explicite, dry-run et aide |
| Divergence entre CI et local | Appeler les mêmes cibles lorsque possible |

## Alternatives considérées

### Scripts seuls

Ils restent nécessaires pour l’implémentation, mais offrent une interface moins structurée et moins découvrable.

### Commandes brutes dans la documentation

Sans dépendance supplémentaire, mais les commandes longues divergent rapidement entre guides et CI.

### Autre task runner

Ergonomie potentiellement supérieure, mais introduit un outil à sélectionner et installer avant que le langage principal du projet soit connu.

## Plan d’implémentation

| Phase | Livrable | Validation | Statut |
|---|---|---|---|
| 1 | Faire accepter cet ADR | Revue des stakeholders | À faire |
| 2 | Créer `Makefile` avec `help`, `bootstrap`, `config-check`, `validate` | Cibles documentées et testées | À faire |
| 3 | Ajouter `test`, `build`, `evaluate` et `export` avec leurs implémentations | Exécutions locales reproductibles | À faire |
| 4 | Ajouter des cibles de publication qualifiées après ADR API | Prévisualisation et contrôle de cible | Bloqué par décision API |
| 5 | Faire appeler les cibles pertinentes par la CI | Parité locale/CI mesurée | À faire |

## Critères de succès

- `make help` décrit 100 % des cibles publiques ;
- aucune logique métier non triviale ne réside dans le `Makefile` ;
- aucune cible sans suffixe explicite ne publie ou ne supprime à distance ;
- les procédures documentées utilisent les cibles plutôt que des copies divergentes ;
- chaque cible échoue tôt avec un diagnostic exploitable si un prérequis manque.

## Traçabilité et liens

- [ADR-600](./600-DEVOPS-bootstrap-configuration-management.md) — configuration.
- [ADR-601](./601-DEVOPS-nomenclature-scripts.md) — nomenclature.
- [ADR-608](./608-DEVOPS-non-duplication-fonctionnelle-transversale.md) — séparation et mutualisation.
- [ADR-611](./611-DEVOPS-gestion-couleurs-scripts-make.md) — sorties terminal.

## Historique

| Date | Auteur | Changement | Raison |
|---|---|---|---|
| 2026-08-27 | Équipe kb-genai-builder | Import et adaptation de l’ADR-602 source | Définir une façade d’orchestration indépendante de l’implémentation de la bibliothèque |
