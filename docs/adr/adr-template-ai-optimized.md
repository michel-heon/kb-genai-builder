---
# Copier ce fichier, puis remplacer toutes les valeurs d’exemple.
# Lire ensemble ADR-000, TAXONOMY.md et README.md avant rédaction.
adr: XXX
title: "[Titre descriptif de la décision]"
status: "proposed"
date: YYYY-MM-DD
superseded_by: null
replaces: null
related_adrs: []
related_issues: []
classification:
  lifecycle: "proposed"
  domain: "architecture"
  impact: "medium"
  quality:
    - "maintainability"
    - "traceability"
  reversibility: "moderate"
  scope: "tactical"
  tech_areas:
    - "knowledge-base"
    - "generative-ai"
tags: ["kb-genai-builder"]
stakeholders: ["@project-owner", "@architecture-team"]
effort: "medium"
---

# ADR XXX : [Titre descriptif de la décision]

<!-- PLACEHOLDER: remplacer XXX, les crochets et toutes les valeurs d’exemple. -->

## Vue d’ensemble

| Attribut | Valeur |
|---|---|
| Statut | Proposé |
| Date de décision | YYYY-MM-DD |
| Stakeholders | [Responsables et réviseurs] |
| Impact | Faible / Moyen / Élevé / Critique |
| Effort | Faible / Moyen / Élevé |
| Risque principal | [Risque] |

## Contexte et problème

### Problème

[Décrire le problème observé, les utilisateurs ou étapes affectés et les preuves disponibles. Distinguer l’existant de la cible.]

### Contraintes

- **Sources** : [formats, volumes, langues, qualité, fréquence de changement]
- **Provenance** : [identifiants, version, emplacement d’origine, date d’acquisition]
- **Sécurité et conformité** : [classification, droits, données sensibles, conservation]
- **Consommation IA** : [besoin de récupération, citations, fidélité, limites connues]
- **Intégrations** : [API, format de sortie ou consommateur cible ; contrat et contraintes documentées]
- **Exploitation** : [délais, coûts, supervision, reprise]

### Impact de l’absence de décision

- **Court terme** : [impact]
- **Moyen terme** : [impact]
- **Long terme** : [impact]

### Faits, hypothèses et inconnues

| Élément | Type | Source ou validation prévue |
|---|---|---|
| [Élément vérifié] | Fait | [Fichier, test ou documentation officielle datée] |
| [Élément à confirmer] | Hypothèse | À VÉRIFIER — [méthode et responsable] |

## Décision

### Approche choisie

Nous adoptons **[solution]** afin de **[objectif mesurable]**.

[Décrire le fonctionnement, le périmètre et ce qui reste explicitement hors périmètre.]

### Effet sur la chaîne documentaire

| Étape | Changement | Entrée | Sortie | Contrôle |
|---|---|---|---|---|
| Acquisition | [Aucun / changement] | [Entrée] | [Sortie] | [Contrôle] |
| Extraction | [Aucun / changement] | [Entrée] | [Sortie] | [Contrôle] |
| Normalisation et chunking | [Aucun / changement] | [Entrée] | [Sortie] | [Contrôle] |
| Indexation / publication | [Aucun / changement] | [Entrée] | [Sortie] | [Contrôle] |
| Consommation par l’agent | [Aucun / changement] | [Entrée] | [Sortie] | [Contrôle] |

### Principes appliqués

- **Traçabilité** : [comment la source et sa version restent identifiables]
- **Moindre privilège** : [comment les droits sont conservés]
- **Actualité** : [comment les mises à jour, suppressions et expirations se propagent]
- **Qualité mesurable** : [corpus et métriques prévus]
- **Réversibilité** : [retour arrière et éventuelle réindexation]

### Composants et contrats concernés

| Élément | État observé / version | Rôle | Modification |
|---|---|---|---|
| [Fichier, module, schéma ou service] | [Valeur vérifiée] | [Rôle] | [Modification] |

Ne citer comme existant qu’un composant vérifié dans le projet ou dans une source officielle. Sinon écrire `À VÉRIFIER`.

## Matrice de décision

Attribuer une note de 0 à 10. La somme des poids doit valoir 100 %. Adapter les critères au problème et conserver le calcul reproductible.

| Critère | Poids | Option A | Option B | Option retenue | Justification |
|---|---:|---:|---:|---:|---|
| Provenance et auditabilité | 25 % | [0–10] | [0–10] | [0–10] | [Note] |
| Respect des droits | 25 % | [0–10] | [0–10] | [0–10] | [Note] |
| Qualité de récupération | 20 % | [0–10] | [0–10] | [0–10] | [Note] |
| Maintenabilité | 15 % | [0–10] | [0–10] | [0–10] | [Note] |
| Coût et exploitation | 15 % | [0–10] | [0–10] | [0–10] | [Note] |
| **Total pondéré** | **100 %** | **[score]** | **[score]** | **[score]** | |

```text
Option A = (note1 × poids1) + ... = [score]
Option B = (note1 × poids1) + ... = [score]
Option retenue = (note1 × poids1) + ... = [score]
```

Si une matrice n’apporte aucune information (par exemple pour une méta-décision procédurale), remplacer cette section par une justification explicite.

## Conséquences

### Positives

| Bénéfice | Indicateur | Cible | Méthode de mesure |
|---|---|---:|---|
| [Bénéfice] | [Indicateur] | [Cible] | [Test, rapport ou observation] |

### Négatives, risques et mitigations

| Risque ou coût | Impact | Probabilité | Mitigation | Responsable | Échéance |
|---|---|---|---|---|---|
| [Risque] | [Niveau] | [Niveau] | [Action] | [Rôle] | [Date ou phase] |

## Alternatives considérées

### Option A : [Nom]

**Description** : [description]

**Avantages** :

- [avantage]

**Inconvénients** :

- [inconvénient]

**Conclusion** : [raison du rejet ou condition de réexamen]

### Option B : [Nom]

**Description** : [description]

**Avantages** :

- [avantage]

**Inconvénients** :

- [inconvénient]

**Conclusion** : [raison du rejet ou condition de réexamen]

## Plan d’implémentation

| Phase | Livrables | Dépendances | Validation | Responsable | Statut |
|---|---|---|---|---|---|
| 1. Préparation | [Livrables] | [Dépendances] | [Critères] | [Rôle] | À faire |
| 2. Mise en œuvre | [Livrables] | [Dépendances] | [Critères] | [Rôle] | À faire |
| 3. Qualification | [Livrables] | [Dépendances] | [Critères] | [Rôle] | À faire |
| 4. Déploiement / migration | [Livrables] | [Dépendances] | [Critères et retour arrière] | [Rôle] | À faire |

## Critères de succès et validation

| Métrique | Référence | Cible | Méthode | Date de mesure |
|---|---:|---:|---|---|
| Couverture de provenance | [Valeur] | [Cible] | [Requête ou test] | [Date] |
| Respect des autorisations | [Valeur] | [Cible] | [Tests positifs et négatifs] | [Date] |
| Qualité de récupération | [Valeur] | [Cible] | [Corpus et métrique] | [Date] |
| Fraîcheur | [Valeur] | [Cible] | [Mesure source-index] | [Date] |
| Taux d’erreur de la chaîne de transformation | [Valeur] | [Cible] | [Rapport d’exécution] | [Date] |

Supprimer les métriques non applicables et expliquer pourquoi. Une cible doit rester vérifiable indépendamment du ressenti de l’auteur.

### Déclencheurs de réévaluation

- changement de l’API publique, d’un format d’export ou d’un contrat d’intégration ;
- changement de schéma, format source ou modèle d’autorisation ;
- régression mesurée sur le corpus de référence ;
- évolution réglementaire ou de politique interne ;
- coût ou volume dépassant [seuil] ;
- incident de sécurité ou de provenance lié à la décision.

**Responsable de la revue** : [Rôle]  
**Fréquence** : [Événement ou périodicité]

## Traçabilité et liens

### ADR et travaux liés

| Référence | Relation | Description |
|---|---|---|
| [ADR / issue / changement] | [dépend de / remplace / implémente] | [Description] |

### Sources projet consultées

| Fichier ou ressource | Version / date | Raison |
|---|---|---|
| [Chemin local] | [Commit, version ou date] | [Ce que la source établit] |

### Documentation externe

| Source officielle | Consultée le | Élément vérifié |
|---|---|---|
| [Lien direct] | YYYY-MM-DD | [Contrainte ou comportement vérifié] |

## Notes et historique

| Date | Auteur | Changement | Raison |
|---|---|---|---|
| YYYY-MM-DD | [Auteur] | Création initiale | [Issue ou besoin] |

## Métadonnées IA

```json
{
  "adr_id": "XXX",
  "project": "kb-genai-builder",
  "parsing_version": "1.0",
  "validation_status": "draft",
  "depends_on": [],
  "blocks": [],
  "related": []
}
```

## Checklist avant revue

- [ ] Le numéro, le préfixe et le domaine sont cohérents avec TAXONOMY.md.
- [ ] `status` est identique à `classification.lifecycle`.
- [ ] Tous les placeholders sont remplacés.
- [ ] Les faits et composants cités ont été vérifiés.
- [ ] Les hypothèses restantes ont un plan de validation.
- [ ] Les effets sur provenance, droits, fraîcheur et suppression sont traités ou déclarés non applicables.
- [ ] Au moins une alternative crédible est analysée, sauf justification.
- [ ] Les scores et totaux de la matrice sont corrects.
- [ ] Les critères de succès ont une référence, une cible et une méthode.
- [ ] Le plan prévoit migration et retour arrière si nécessaire.
- [ ] Les liens sont directs et les consultations externes sont datées.
- [ ] L’ADR est ajouté à `docs/adr/README.md` et les statistiques sont mises à jour.

## Instructions pour les agents IA

- Lire les quatre documents du système ADR avant de proposer une décision.
- Inspecter les artefacts du projet concernés avant d’écrire des détails techniques.
- Ne jamais transformer une hypothèse en fait pour compléter une section.
- Marquer `À VÉRIFIER` toute information non confirmée et indiquer comment la confirmer.
- Pour tout format, service ou SDK externe, utiliser une documentation officielle actuelle et consigner la date de consultation.
- Ne pas déclarer un ADR `accepted` sans validation explicite des responsables désignés.

**Version du template** : 1.0  
**Dernière mise à jour** : 2026-08-27  
**Projet** : kb-genai-builder  
**Compatibilité** : humains, scripts et agents IA
