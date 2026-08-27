# Taxonomie ADR — Guide de classification

**Version** : 1.0  
**Date** : 2026-08-27  
**Projet** : kb-genai-builder

## Documents complémentaires

Cette taxonomie doit rester cohérente avec [ADR-000](./000-META-processus-creation-adr.md), le [template](./adr-template-ai-optimized.md) et l’[index](./README.md).

## Objectif

Chaque ADR est classé selon sept dimensions dans son frontmatter YAML. Les valeurs contrôlées permettent aux humains, aux scripts et aux agents IA de rechercher les décisions sans déduire leur sens depuis le texte libre.

## 1. Lifecycle

| Valeur | Description |
|---|---|
| `draft` | Rédaction en cours |
| `proposed` | Prêt pour revue |
| `accepted` | Décision approuvée et en vigueur |
| `rejected` | Proposition refusée |
| `deprecated` | Obsolète sans remplacement direct |
| `superseded` | Remplacé par un autre ADR |

`status` et `classification.lifecycle` doivent avoir la même valeur. Pour `superseded`, le champ `superseded_by` est obligatoire ; pour une décision qui en remplace une autre, renseigner `replaces`.

## 2. Domain

Un seul domaine principal est autorisé. Les impacts secondaires sont exprimés par `tech_areas`, `quality` et `related_adrs`.

| Préfixe | Plage | Valeur YAML | Périmètre kb-genai-builder |
|---|---:|---|---|
| `META` | 000–099 | `meta` | Gouvernance, processus ADR, règles d’usage de l’IA |
| `ARCH` | 100–199 | `architecture` | Architecture de la bibliothèque, composants, chaîne de transformation, frontières |
| `INFRA` | 200–299 | `infrastructure` | Environnements, stockage, déploiement, réseau, observabilité |
| `SEC` | 300–399 | `security` | Identités, autorisations, confidentialité, secrets, conformité |
| `DATA` | 400–499 | `data` | Sources, extraction, OCR, normalisation, chunking, métadonnées, index |
| `API` | 500–599 | `api` | API publique, connecteurs, interfaces et contrats d’intégration |
| `DEVOPS` | 600–699 | `devops` | Automatisation, CI/CD, configuration, versionnement, exploitation |
| `TEST` | 700–799 | `test` | Tests, corpus de référence, qualité de recherche, évaluations IA |
| `BIZ` | 800–899 | `business` | Périmètre produit, règles métier, valeur, coûts, responsabilités |
| `DOC` | 900–999 | `documentation` | Conventions éditoriales, guides, documentation opératoire |

Exemples :

- le choix d’un modèle commun de métadonnées est `DATA` ;
- le choix du mécanisme exposant ces données au consommateur cible est `API` ;
- les règles empêchant l’exposition d’un document confidentiel sont `SEC` ;
- le découpage global entre ingestion, contrôle et publication est `ARCH`.

## 3. Impact

| Valeur | Critères indicatifs | Exemple |
|---|---|---|
| `low` | Local, réversible en moins d’un jour | Ajustement d’une règle de journalisation |
| `medium` | Plusieurs fichiers ou une étape, 1 à 5 jours | Nouveau parseur pour un type documentaire |
| `high` | Transversal, migration ou plus d’une semaine | Nouveau schéma de métadonnées ou changement de chunking global |
| `critical` | Fondamental, fort risque de perte, fuite ou rupture | Modèle d’autorisation ou architecture de publication |

L’impact mesure l’ampleur des conséquences, pas l’importance subjective du sujet.

## 4. Quality

Une ou plusieurs valeurs sont requises.

| Valeur | Sens dans kb-genai-builder | Mesures possibles |
|---|---|---|
| `performance` | Vitesse et capacité de la transformation ou de la recherche | durée de traitement, latence, débit |
| `security` | Confidentialité, intégrité, identités et droits | tests d’accès, incidents, secrets détectés |
| `reliability` | Reproductibilité, reprise et cohérence | taux d’échec, idempotence, documents orphelins |
| `maintainability` | Facilité d’évolution et de diagnostic | complexité, couverture, temps de correction |
| `cost` | Coûts de calcul, stockage, licences et appels externes | coût par document, stockage, consommation |
| `usability` | Expérience des contributeurs et utilisateurs | temps de publication, satisfaction, taux d’échec |
| `compliance` | Respect des politiques, licences et obligations | contrôles, durée de conservation, audits |
| `portability` | Dépendance aux formats, outils et fournisseurs | effort de migration, formats ouverts |
| `traceability` | Capacité à relier une réponse au contenu source | taux de chunks avec provenance complète |
| `retrieval-quality` | Pertinence du contenu retrouvé pour l’agent | precision@k, recall@k, nDCG, réponses fondées |
| `freshness` | Actualité du contenu publié | délai source-index, documents périmés |

Les trois dernières valeurs étendent ISO 25010 pour refléter le caractère documentaire et génératif du projet.

## 5. Reversibility

| Valeur | Définition |
|---|---|
| `easy` | Retour arrière local, sans migration |
| `moderate` | Quelques composants ou une réindexation limitée |
| `hard` | Migration large, réingestion ou coordination externe |
| `irreversible` | Retour prohibitif, perte d’information ou engagement non annulable |

Un choix de format persistant, de modèle de droits ou de granularité de provenance est généralement plus difficile à inverser qu’un paramètre d’exécution.

## 6. Scope

| Valeur | Portée | Horizon indicatif |
|---|---|---|
| `strategic` | Vision et principes transversaux | plusieurs années |
| `tactical` | Choix à l’échelle du projet | 6 à 18 mois |
| `operational` | Choix local d’implémentation | 1 à 6 mois |

## 7. Tech areas

`tech_areas` est une liste ouverte de mots-clés en kebab-case. Réutiliser les termes existants avant d’en créer de nouveaux.

### Documents et acquisition

- `pdf`, `office-documents`, `html`, `email`, `ocr`, `source-discovery`, `content-extraction`

### Transformation et données

- `normalization`, `chunking`, `metadata`, `taxonomy`, `deduplication`, `language-detection`, `pii-detection`, `knowledge-base`

### Recherche et IA

- `full-text-search`, `vector-search`, `hybrid-search`, `embeddings`, `reranking`, `retrieval`, `grounding`, `generative-ai`, `evaluation`

### Intégrations et consommateurs

- `connector`, `api`, `cli`, `rag`, `vector-database`, `search-engine`, `identity`, `permissions`

> Ces mots-clés décrivent une zone technique ; ils ne prouvent pas qu’une technologie est déjà implémentée. Un ADR doit distinguer l’existant, la cible et les hypothèses.

### Ingénierie et exploitation

- `python`, `powershell`, `git`, `ci-cd`, `configuration`, `logging`, `monitoring`, `storage`, `deployment`

### Gouvernance

- `documentation`, `adr`, `records-management`, `retention`, `compliance`, `security`

## Exemple complet

```yaml
---
adr: 400
title: "Adopter un modèle canonique de métadonnées documentaires"
status: "proposed"
date: 2026-08-27
superseded_by: null
replaces: null
related_adrs: []
related_issues: []
classification:
  lifecycle: "proposed"
  domain: "data"
  impact: "high"
  quality:
    - "traceability"
    - "retrieval-quality"
    - "maintainability"
  reversibility: "hard"
  scope: "tactical"
  tech_areas:
    - "metadata"
    - "knowledge-base"
    - "generative-ai"
tags: ["kb-genai-builder", "metadata", "provenance"]
stakeholders: ["@content-team", "@architecture-team"]
effort: "high"
---
```

## Convention de nommage

Pattern : `XXX-CATEGORIE-titre-kebab-case.md`.

```text
100-ARCH-decoupage-bibliotheque-documentaire.md
300-SEC-propagation-droits-sources.md
400-DATA-modele-metadonnees-canoniques.md
500-API-contrat-export-base-connaissances.md
700-TEST-corpus-reference-recherche.md
```

Le nom du fichier n’utilise ni accent, ni espace. Le titre affiché dans l’ADR reste rédigé normalement en français.

## Requêtes utiles

```bash
# ADR de données
rg -l 'domain: "data"' docs/adr/*.md

# ADR à impact critique
rg -l 'impact: "critical"' docs/adr/*.md

# Décisions concernant les contrats d'intégration
rg -l 'connector\|api\|rag' docs/adr/*.md
```

## Checklist de classification

- [ ] `status` et `classification.lifecycle` sont identiques.
- [ ] Le numéro appartient à la plage du domaine principal.
- [ ] Un seul domaine principal est choisi.
- [ ] L’impact est justifié par les conséquences.
- [ ] Au moins un attribut qualité est présent.
- [ ] La réversibilité tient compte des migrations et réindexations.
- [ ] La portée correspond à l’horizon réel.
- [ ] Au moins une zone technique observée ou proposée est indiquée.

## Gouvernance de la taxonomie

Une nouvelle valeur contrôlée doit répondre à un besoin de filtrage récurrent, être documentée ici et être ajoutée simultanément à ADR-000 et au template. Une simple variante lexicale ne justifie pas une nouvelle valeur.

**Maintenu par** : équipe kb-genai-builder  
**Dernière mise à jour** : 2026-08-27
