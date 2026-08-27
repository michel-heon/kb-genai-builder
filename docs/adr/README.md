# Architecture Decision Records — kb-genai-builder

Cet index centralise les décisions architecturales de **kb-genai-builder**, une bibliothèque d’utilitaires qui transforme des documents en bases de connaissances exploitables par des systèmes d’IA générative. La bibliothèque reste indépendante d’un fournisseur, d’un moteur d’indexation et d’un agent consommateur particuliers.

## Documents du système ADR

| Document | Rôle |
|---|---|
| [ADR-000](./000-META-processus-creation-adr.md) | Processus, règles et cycle de vie |
| [TAXONOMY.md](./TAXONOMY.md) | Valeurs de classification et plages de numérotation |
| [adr-template-ai-optimized.md](./adr-template-ai-optimized.md) | Modèle à copier pour une nouvelle décision |
| [README.md](./README.md) | Index, numéros disponibles et guide rapide |

## Créer un ADR

```bash
# Depuis la racine du projet :
# 1. Choisir le domaine et trouver le prochain numéro de sa plage.
ls -1 docs/adr/4??-DATA-*.md 2>/dev/null | tail -1

# 2. Copier le template sous le numéro retenu.
cp docs/adr/adr-template-ai-optimized.md \
  docs/adr/400-DATA-titre-decision.md

# 3. Remplir l’ADR et ajouter immédiatement une ligne à cet index.
```

Avant acceptation, rechercher les marqueurs incomplets :

```bash
rg -n 'XXX|YYYY-MM-DD|PLACEHOLDER|À VÉRIFIER' docs/adr/XXX-CATEGORIE-titre-decision.md
```

## Index des ADR

### META — Méta-processus (000–099)

| ADR | Titre | Statut | Date | Domaine |
|---|---|---|---|---|
| [000](./000-META-processus-creation-adr.md) | Processus de création et de gestion des ADR | Accepté | 2026-08-27 | Gouvernance |
| [003](./003-META-creation-et-usage-des-github-issues.md) | Création et usage des GitHub Issues | Proposé | 2026-08-27 | Gouvernance du travail |

### ARCH — Architecture (100–199)

| ADR | Titre | Statut | Date | Domaine |
|---|---|---|---|---|
| [101](./101-ARCH-adoption-langchain4j-couche-provider.md) | Adoption de LangChain4j comme couche d’implémentation des fournisseurs LLM et embeddings | Proposé | 2026-08-27 | Architecture des fournisseurs IA |

### INFRA — Infrastructure (200–299)

Aucun ADR.

### SEC — Sécurité (300–399)

Aucun ADR.

### DATA — Données et base de connaissances (400–499)

Aucun ADR.

### API — Interfaces et intégrations (500–599)

Aucun ADR.

### DEVOPS — Automatisation et exploitation (600–699)

| ADR | Titre | Statut | Date | Domaine |
|---|---|---|---|---|
| [600](./600-DEVOPS-bootstrap-configuration-management.md) | Bootstrap local et gestion de la configuration | Proposé | 2026-08-27 | Configuration |
| [601](./601-DEVOPS-nomenclature-scripts.md) | Nomenclature des scripts et commandes d’automatisation | Proposé | 2026-08-27 | Automatisation |
| [602](./602-DEVOPS-makefile-orchestrateur.md) | Makefile comme orchestrateur standard | Proposé | 2026-08-27 | Orchestration |
| [608](./608-DEVOPS-non-duplication-fonctionnelle-transversale.md) | Non-duplication fonctionnelle transversale | Proposé | 2026-08-27 | Maintenabilité |
| [611](./611-DEVOPS-gestion-couleurs-scripts-make.md) | Gestion des couleurs et sorties terminal dans les scripts et Make | Proposé | 2026-08-27 | Sorties terminal |

### TEST — Tests et qualité (700–799)

Aucun ADR.

### BIZ — Produit et métier (800–899)

Aucun ADR.

### DOC — Documentation (900–999)

Aucun ADR.

## Numérotation

| Préfixe | Plage | Domaine | Prochain numéro disponible |
|---|---:|---|---:|
| `META` | 000–099 | Gouvernance | 004 |
| `ARCH` | 100–199 | Architecture de la bibliothèque | 102 |
| `INFRA` | 200–299 | Infrastructure | 200 |
| `SEC` | 300–399 | Sécurité et conformité | 300 |
| `DATA` | 400–499 | Données et base de connaissances | 400 |
| `API` | 500–599 | Interfaces et intégrations | 500 |
| `DEVOPS` | 600–699 | Automatisation et exploitation | 612 |
| `TEST` | 700–799 | Tests et qualité | 700 |
| `BIZ` | 800–899 | Produit et métier | 800 |
| `DOC` | 900–999 | Documentation | 900 |

Les trous de numérotation ne sont pas réutilisés. Le tableau est mis à jour lors de la création d’un ADR, quel que soit son statut.

## Statistiques

| Indicateur | Valeur |
|---|---:|
| Total | 8 |
| Brouillons | 0 |
| Proposés | 7 |
| Acceptés | 1 |
| Rejetés | 0 |
| Dépréciés | 0 |
| Supersédés | 0 |

## Points de contrôle propres au projet

Lorsqu’ils sont pertinents, les ADR doivent répondre explicitement aux questions suivantes :

- La provenance du document et de chaque unité indexée reste-t-elle disponible ?
- Les droits de la source sont-ils préservés jusqu’au consommateur ?
- La suppression, l’expiration et la republication sont-elles gérées ?
- L’effet sur la pertinence et la fidélité des réponses est-il mesurable ?
- Les formats d’entrée, les artefacts de sortie et leurs contrats de compatibilité sont-ils explicites et versionnés ?
- Toute intégration avec un service externe est-elle isolée derrière une interface et fondée sur sa documentation officielle actuelle ?
- Une réingestion ou une réindexation est-elle nécessaire pour revenir en arrière ?

## Statuts

| Statut | Valeur YAML |
|---|---|
| Brouillon | `draft` |
| Proposé | `proposed` |
| Accepté | `accepted` |
| Rejeté | `rejected` |
| Déprécié | `deprecated` |
| Supersédé | `superseded` |

**Dernière mise à jour** : 2026-08-27
