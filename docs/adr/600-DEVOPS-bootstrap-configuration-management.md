---
adr: 600
title: "Bootstrap local et gestion de la configuration"
status: "proposed"
date: 2026-08-27
superseded_by: null
replaces: null
related_adrs: [0, 601, 602, 608, 611]
related_issues: []
classification:
  lifecycle: "proposed"
  domain: "devops"
  impact: "high"
  quality:
    - "maintainability"
    - "security"
    - "reliability"
    - "portability"
  reversibility: "moderate"
  scope: "tactical"
  tech_areas:
    - "configuration"
    - "security"
    - "knowledge-base"
    - "deployment"
tags: ["bootstrap", "configuration", "environment", "secrets"]
stakeholders: ["@architecture-team", "@content-team", "@operations-team"]
effort: "medium"
---

# ADR 600 : Bootstrap local et gestion de la configuration

## Vue d’ensemble

| Attribut | Valeur |
|---|---|
| Statut | Proposé |
| Date de décision | 2026-08-27 |
| Impact | Élevé |
| Effort | Moyen |
| Risque principal | Fuite de secrets ou divergence de configuration |

## Contexte et problème

La bibliothèque **kb-genai-builder** aura besoin de paramètres pour localiser les sources, sélectionner les formats acceptés, configurer l’extraction et l’OCR, contrôler le découpage, choisir les formats de sortie et exécuter les évaluations. Les adaptateurs optionnels vers des services externes pourront aussi exiger des identifiants, endpoints ou secrets.

Lors de l’import de cet ADR, aucun script, `Makefile`, fichier de configuration ni implémentation de la bibliothèque n’est présent dans le répertoire cible. Les chemins décrits ci-dessous sont donc des artefacts **à créer après acceptation**, et non un inventaire de l’existant.

Sans convention commune, une même valeur peut diverger entre poste local, CI et production ; un secret peut être committé ; un test peut devenir non reproductible ; ou une publication peut viser le mauvais environnement.

## Décision

Adopter une configuration en couches, validée avant exécution, avec séparation stricte entre valeurs publiques et secrets.

### Sources et précédence

Du niveau le plus faible au plus fort :

1. `config/default.env` — valeurs publiques, sûres et versionnées ;
2. `.env.local` — surcharges propres au poste, ignorées par Git ;
3. variables du processus — surcharges CI ou opérateur ;
4. arguments explicites de la commande — uniquement pour l’exécution courante.

Le fichier `.env.local.example` est versionné et décrit les noms de variables sans valeur sensible. Une valeur requise n’obtient pas de valeur factice susceptible de déclencher un appel externe par erreur.

### Catégories de configuration

| Catégorie | Exemples de décisions configurables | Secret possible |
|---|---|---|
| Acquisition | répertoires d’entrée, extensions autorisées, taille maximale | Non |
| Transformation | langues OCR, paramètres de chunking, règles de normalisation | Non |
| Qualité | corpus de référence, seuils d’évaluation | Non |
| Export | format, répertoire cible, options de sérialisation | Non |
| Publication optionnelle | environnement cible, identifiants non sensibles | Parfois |
| Services externes | endpoint, modèle, délai, quota | Oui pour l’authentification |

Les règles structurantes — schéma de métadonnées, sémantique de provenance, politique de droits — ne deviennent pas de simples variables si leur changement constitue une décision architecturale. Elles restent gouvernées par un ADR et un contrat versionné.

### Secrets

- aucun jeton, mot de passe, certificat privé ou chaîne d’accès n’est versionné ;
- `.env.local` et tout artefact généré contenant un secret sont ignorés par Git ;
- la CI injecte les secrets depuis son mécanisme sécurisé, sans les recopier dans un artefact publié ;
- les journaux affichent le nom d’une variable manquante, jamais sa valeur ;
- les exemples utilisent des marqueurs inertes ;
- un secret n’est pas transmis à une étape qui n’en a pas besoin.

### Bootstrap

La commande stable est `make bootstrap`, conformément à l’ADR-602. Elle doit :

1. vérifier les outils requis par l’implémentation réelle ;
2. créer `.env.local` depuis l’exemple uniquement s’il est absent ;
3. positionner des permissions restrictives lorsque la plateforme le permet ;
4. valider les noms, types, formats, chemins et combinaisons de paramètres ;
5. créer les répertoires de travail locaux explicitement prévus ;
6. ne télécharger aucun corpus et ne contacter aucun service externe sans option explicite ;
7. être idempotente et ne modifier aucune configuration globale du poste.

Une commande séparée `make config-check` valide la configuration sans lancer l’ingestion ni la publication.

### Validation et sûreté

- une variable inconnue produit au minimum un avertissement ;
- une valeur obligatoire absente arrête l’exécution avant tout effet externe ;
- l’environnement de publication est explicite et n’a pas de valeur de production par défaut ;
- les chemins de sortie sont résolus et contrôlés avant écriture ;
- les paramètres affectant le chunking ou l’indexation sont enregistrés avec le lot produit afin de permettre la reproduction et le retour arrière.

### Génération multi-format

Ne pas générer plusieurs projections tant qu’au moins deux consommateurs incompatibles n’en ont pas besoin. Si ce besoin apparaît, une source canonique produit des fichiers dérivés ignorés par Git, conformément à l’ADR-608. Les fichiers générés ne sont jamais modifiés manuellement.

## Matrice de décision

| Critère | Poids | Configuration en couches | Fichier unique suivi | Variables implicites | Configuration par outil |
|---|---:|---:|---:|---:|---:|
| Sécurité | 30 % | 9 | 3 | 6 | 7 |
| Reproductibilité | 25 % | 9 | 8 | 4 | 6 |
| Maintenabilité | 20 % | 9 | 7 | 5 | 4 |
| Simplicité | 15 % | 7 | 9 | 7 | 5 |
| Évolutivité | 10 % | 8 | 5 | 5 | 6 |
| **Total pondéré** | **100 %** | **8,60** | **6,15** | **5,35** | **5,75** |

```text
En couches       = 9×0,30 + 9×0,25 + 9×0,20 + 7×0,15 + 8×0,10 = 8,60
Fichier suivi    = 3×0,30 + 8×0,25 + 7×0,20 + 9×0,15 + 5×0,10 = 6,15
Variables seules = 6×0,30 + 4×0,25 + 5×0,20 + 7×0,15 + 5×0,10 = 5,35
Par outil        = 7×0,30 + 6×0,25 + 4×0,20 + 5×0,15 + 6×0,10 = 5,75
```

## Conséquences

### Positives

- configuration locale et CI reproductible ;
- secrets séparés des paramètres publics ;
- échecs précoces avant ingestion ou publication ;
- paramètres de traitement rattachables aux lots produits ;
- ajout progressif de consommateurs sans duplication immédiate.

### Négatives et mitigations

| Risque | Impact | Mitigation |
|---|---|---|
| Nombre de variables croissant | Moyen | Schéma documenté, validation et regroupement par préfixe |
| Écart entre exemple et validation | Moyen | Test automatique de complétude |
| Secret écrit dans un log | Élevé | Redaction centralisée et test avec valeurs sentinelles |
| Mauvaise cible de publication | Élevé | Cible explicite, dry-run et absence de défaut production |

## Alternatives considérées

### Fichier unique versionné

Simple, mais incompatible avec la présence de secrets ou de paramètres propres aux environnements.

### Variables d’environnement uniquement

Adaptées aux secrets et à la CI, mais peu découvrables et insuffisantes comme documentation des valeurs par défaut.

### Configuration indépendante par outil

Réduit le travail initial, mais crée rapidement des divergences entre ingestion, tests et publication.

## Plan d’implémentation

| Phase | Livrable | Validation | Statut |
|---|---|---|---|
| 1 | Faire accepter cet ADR et l’ADR-602 | Revue des stakeholders | À faire |
| 2 | Définir le schéma minimal des variables réellement consommées | Revue sans variable spéculative | À faire |
| 3 | Créer `config/default.env` et `.env.local.example` | Analyse de secrets et documentation | À faire |
| 4 | Implémenter `bootstrap` et `config-check` | Deux exécutions idempotentes | À faire |
| 5 | Intégrer la validation à la CI | Configuration invalide correctement rejetée | À faire |

## Critères de succès et validation

| Critère | Cible | Méthode |
|---|---:|---|
| Secrets versionnés | 0 | Analyse automatisée et revue |
| Variables consommées mais non documentées | 0 | Test de complétude |
| Bootstrap idempotent | 2 exécutions sans différence inattendue | Test d’intégration |
| Publication implicite vers la production | 0 | Tests des valeurs par défaut |
| Lots sans empreinte de configuration de traitement | 0 | Contrôle des métadonnées de lot |

Réévaluer lors de l’ajout du premier service externe, d’un second format de configuration consommateur ou d’un gestionnaire de secrets imposé par la plateforme.

## Traçabilité et liens

- [ADR-000](./000-META-processus-creation-adr.md) — gouvernance des décisions.
- [ADR-601](./601-DEVOPS-nomenclature-scripts.md) — nommage des scripts.
- [ADR-602](./602-DEVOPS-makefile-orchestrateur.md) — commandes de bootstrap et validation.
- [ADR-608](./608-DEVOPS-non-duplication-fonctionnelle-transversale.md) — seuil de mutualisation.
- [ADR-611](./611-DEVOPS-gestion-couleurs-scripts-make.md) — diagnostics sûrs et lisibles.

## Historique

| Date | Auteur | Changement | Raison |
|---|---|---|---|
| 2026-08-27 | Équipe kb-genai-builder | Import et adaptation de l’ADR-600 source | Définir le bootstrap et la configuration de la bibliothèque sans imposer ses futurs consommateurs |
