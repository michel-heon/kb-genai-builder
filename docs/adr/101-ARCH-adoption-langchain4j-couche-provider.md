---
adr: 101
title: "Adoption de LangChain4j comme couche d’implémentation des fournisseurs LLM et embeddings"
status: "proposed"
date: 2026-08-27
superseded_by: null
replaces: null
related_adrs: [0, 600, 608]
related_issues: []
classification:
  lifecycle: "proposed"
  domain: "architecture"
  impact: "high"
  quality:
    - "maintainability"
    - "portability"
    - "security"
    - "reliability"
  reversibility: "moderate"
  scope: "tactical"
  tech_areas:
    - "java"
    - "llm"
    - "embeddings"
    - "connector"
    - "generative-ai"
tags: ["kb-genai-builder", "langchain4j", "llm-provider", "embeddings"]
stakeholders: ["@project-owner", "@architecture-team", "@dev-team"]
effort: "medium"
---

# ADR 101 : Adoption de LangChain4j comme couche d’implémentation des fournisseurs LLM et embeddings

## Vue d’ensemble

| Attribut | Valeur |
|---|---|
| Statut | Proposé |
| Date de décision | 2026-08-27 |
| Impact | Élevé |
| Effort | Moyen |
| Risque principal | Couplage du cœur de la bibliothèque à un framework ou à un fournisseur d’IA |

## Contexte et problème

`kb-genai-builder` transforme des documents en artefacts de base de connaissances. Certaines transformations pourront nécessiter un modèle de langage ou d’embeddings, par exemple pour vectoriser des chunks, enrichir des métadonnées ou produire des résumés. Ces capacités doivent rester optionnelles : l’extraction, la normalisation, la provenance et les validations déterministes ne doivent pas dépendre d’un service d’IA externe.

Le répertoire observé lors de l’import contient uniquement le système ADR. Aucun langage d’implémentation, module Java, fichier Maven, port fournisseur ou client HTTP n’y est encore présent. Les composants décrits dans cet ADR sont donc des éléments à créer après acceptation et non un inventaire de l’existant.

L’ADR source provenait de `jena-graphrag-project` et décrivait une implémentation Java/Jena déjà livrée. Les faits propres à Jena, ses classes, ses autres ADR, son issue GitHub et sa version LangChain4j ont été retirés : ils ne sont pas transférables à `kb-genai-builder`.

Sans frontière architecturale, la bibliothèque risque :

- de réimplémenter pour chaque fournisseur la sérialisation HTTP, l’authentification et la gestion des erreurs ;
- d’exposer les types d’un SDK tiers dans son API publique ;
- de rendre obligatoires le réseau, une clé d’API ou un fournisseur pour des traitements qui peuvent rester locaux ;
- de mélanger la transformation documentaire avec la configuration et les politiques d’appel des modèles.

La documentation officielle consultée le 2026-08-27 présente LangChain4j comme une bibliothèque Java modulaire : chaque intégration possède sa dépendance Maven, le BOM permet d’aligner les versions, et le module OpenAI fournit une intégration dédiée. La même documentation indique Java 17 comme version minimale au moment de la consultation. Ces caractéristiques rendent LangChain4j pertinent si l’implémentation de `kb-genai-builder` est retenue sur la JVM.

## Décision

Adopter **LangChain4j comme couche d’implémentation optionnelle des appels LLM et embeddings si le cœur de `kb-genai-builder` est implémenté sur la JVM**, derrière des interfaces appartenant au projet.

L’acceptation et l’implémentation de cet ADR restent conditionnées à la décision fondatrice sur le langage et le système de build. Une implémentation non-JVM devra réévaluer cet ADR plutôt que d’introduire Java uniquement pour LangChain4j.

### Frontières obligatoires

1. Le cœur définit ses propres ports pour la génération de texte et les embeddings. Leurs contrats utilisent uniquement des types du projet ou de la bibliothèque standard.
2. Aucun type `dev.langchain4j` n’apparaît dans l’API publique, le modèle documentaire canonique ou les formats de sortie.
3. Les adaptateurs LangChain4j résident dans un paquet ou module d’intégration identifiable et remplaçable.
4. Un adaptateur fournisseur est ajouté uniquement lorsqu’un besoin et un test d’intégration réels existent.
5. Les versions LangChain4j sont alignées avec le BOM et figées dans le fichier de dépendances reproductible du projet.
6. Les modules expérimentaux ou en version bêta ne sont pas introduits sans justification et validation séparées.

### Comportement par défaut

- les appels externes sont désactivés par défaut ;
- la configuration doit activer explicitement un adaptateur et sa cible ;
- les secrets sont fournis selon l’ADR-600 et ne transitent pas dans les artefacts produits ;
- les tests unitaires utilisent des implémentations locales déterministes, sans réseau ;
- les timeouts, limites de taille et politiques de retry sont explicites ;
- une erreur fournisseur est traduite vers une erreur stable du projet sans exposer de secret ni le contenu documentaire complet ;
- l’absence de fournisseur n’empêche pas les transformations qui n’exigent ni LLM ni embeddings.

### Périmètre fonctionnel

LangChain4j peut être utilisé pour :

- calculer des embeddings de chunks ou de documents ;
- appeler un modèle de langage pour un enrichissement explicitement configuré ;
- normaliser l’accès à plusieurs fournisseurs à travers des adaptateurs séparés.

LangChain4j ne devient pas :

- le modèle canonique des documents, chunks ou métadonnées ;
- l’orchestrateur de toute la chaîne documentaire ;
- une dépendance obligatoire des parseurs et transformations déterministes ;
- le contrat d’export des bases de connaissances.

### Politique de version

Cet ADR ne fige pas la version `1.8.0` utilisée par le projet source. Au moment de l’implémentation, la version stable retenue doit être vérifiée dans la documentation officielle, verrouillée par le système de build et accompagnée d’un audit des dépendances transitives, licences et vulnérabilités. Toute montée de version majeure exige des tests de compatibilité des adaptateurs.

## Matrice de décision

| Critère | Poids | LangChain4j derrière ports | Clients maison | LangChain4j exposé directement | Service externe séparé |
|---|---:|---:|---:|---:|---:|
| Coût de maintenance | 25 % | 9 | 4 | 8 | 6 |
| Portabilité entre fournisseurs | 20 % | 9 | 4 | 9 | 8 |
| Isolation du cœur | 20 % | 9 | 8 | 3 | 9 |
| Sécurité et contrôle réseau | 20 % | 8 | 8 | 7 | 8 |
| Effort d’implémentation | 15 % | 7 | 6 | 8 | 4 |
| **Total pondéré** | **100 %** | **8,50** | **5,90** | **7,00** | **7,10** |

```text
LangChain4j derrière ports = 9×0,25 + 9×0,20 + 9×0,20 + 8×0,20 + 7×0,15 = 8,50
Clients maison             = 4×0,25 + 4×0,20 + 8×0,20 + 8×0,20 + 6×0,15 = 5,90
Exposition directe         = 8×0,25 + 9×0,20 + 3×0,20 + 7×0,20 + 8×0,15 = 7,00
Service séparé             = 6×0,25 + 8×0,20 + 9×0,20 + 8×0,20 + 4×0,15 = 7,10
```

## Conséquences

### Positives

- les détails HTTP et fournisseurs sont délégués à une bibliothèque spécialisée ;
- le cœur et ses formats restent indépendants de LangChain4j ;
- un fournisseur peut être remplacé sans modifier les transformations documentaires ;
- les tests locaux peuvent rester rapides, déterministes et sans réseau ;
- les appels externes et leurs coûts demeurent explicites.

### Négatives, risques et mitigations

| Risque ou coût | Impact | Mitigation |
|---|---|---|
| Dépendances transitives, licences ou vulnérabilités | Élevé | Audit du graphe de dépendances et mise à jour contrôlée |
| Évolution rapide de certains modules | Moyen | BOM figé, modules stables privilégiés et tests de contrat |
| Fuite de types LangChain4j dans le cœur | Élevé | Test architectural interdisant les imports hors du module adaptateur |
| Comportements implicites de retry ou timeout | Moyen | Valeurs explicites et tests simulant erreurs et délais |
| Choix JVM non encore établi | Élevé | Bloquer l’implémentation jusqu’à la décision sur le langage |

## Alternatives considérées

### Clients fournisseurs développés dans le projet

Cette option réduit les dépendances tierces, mais duplique des fonctions non différenciantes et augmente le coût de prise en charge de plusieurs fournisseurs.

### Exposition directe de LangChain4j

Cette option simplifie les premiers adaptateurs, mais couple l’API de `kb-genai-builder` à un framework externe et rend les migrations plus coûteuses.

### Service d’IA séparé

Cette frontière isole fortement le cœur et reste une option si plusieurs langages doivent partager les mêmes fournisseurs. Elle ajoute toutefois un service, un protocole, un déploiement et une exploitation qui ne sont pas justifiés dans l’état observé.

### Aucun support LLM ou embeddings

Cette option convient au noyau déterministe, mais ne couvre pas les besoins d’enrichissement et de vectorisation attendus d’une bibliothèque destinée aux bases de connaissances pour l’IA générative. Ces fonctions restent néanmoins optionnelles.

## Plan d’implémentation

| Phase | Livrable | Validation | Statut |
|---|---|---|---|
| 1 | Décider du langage, du build et du découpage des modules | ADR d’architecture accepté | Prérequis |
| 2 | Définir les ports LLM et embeddings sans type tiers | Revue de l’API et tests avec doubles locaux | À faire |
| 3 | Sélectionner et verrouiller une version stable de LangChain4j | Audit dépendances, licences et vulnérabilités | À faire |
| 4 | Implémenter un premier adaptateur correspondant à un besoin réel | Tests de contrat et d’intégration opt-in | À faire |
| 5 | Ajouter les contrôles d’isolation et de non-réseau par défaut | Tests automatiques en CI | À faire |

## Critères de succès et validation

- aucun type LangChain4j dans l’API publique ou les formats persistants ;
- aucun appel réseau lors des tests unitaires et de la configuration par défaut ;
- 100 % des adaptateurs respectent les mêmes tests de contrat ;
- timeout, retry et limite de requête explicitement configurés pour chaque adaptateur ;
- aucun secret ni contenu documentaire complet dans les journaux ou exceptions ;
- construction et transformations déterministes possibles sans dépendance à un fournisseur actif ;
- audit de dépendances documenté avant l’acceptation définitive.

Réévaluer si le projet choisit un langage non-JVM, si l’API LangChain4j doit traverser une frontière publique, si un service partagé devient nécessaire ou si les dépendances transitives ne respectent plus les exigences du projet.

## Traçabilité et liens

### ADR liés

- [ADR-000](./000-META-processus-creation-adr.md) — gouvernance et exigence de faits vérifiés.
- [ADR-600](./600-DEVOPS-bootstrap-configuration-management.md) — configuration et secrets.
- [ADR-608](./608-DEVOPS-non-duplication-fonctionnelle-transversale.md) — non-duplication et frontières des abstractions.

### Sources projet consultées

- `/home/michel/00-GIT/jena-graphrag-project/docs/adr/101-ARCH-adoption-langchain4j-couche-provider.md` — ADR source importé et adapté.
- `docs/adr/000-META-processus-creation-adr.md` — règles du système ADR cible.
- `docs/adr/TAXONOMY.md` — classification cible.
- `docs/adr/README.md` — index et état observé du projet.

### Documentation externe

- [LangChain4j — Get Started](https://docs.langchain4j.dev/get-started) — dépendances par intégration, BOM et prérequis Java ; consulté le 2026-08-27.
- [LangChain4j — dépôt officiel](https://github.com/langchain4j/langchain4j) — modules, code source et licence ; consulté le 2026-08-27.

## Historique

| Date | Auteur | Changement | Raison |
|---|---|---|---|
| 2026-08-27 | Équipe kb-genai-builder | Import et adaptation de l’ADR-101 de jena-graphrag-project | Réutiliser la décision d’isolation des fournisseurs sans importer les hypothèses propres à Jena |
