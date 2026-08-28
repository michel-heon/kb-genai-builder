---
adr: 103
title: "CLI Picocli et architecture hexagonale"
status: "proposed"
date: 2026-08-27
superseded_by: null
replaces: null
related_adrs: [101, 102, 600, 602, 608]
related_issues: []
classification:
  lifecycle: "proposed"
  domain: "architecture"
  impact: "medium"
  quality:
    - "maintainability"
    - "usability"
    - "reliability"
  reversibility: "moderate"
  scope: "tactical"
  tech_areas:
    - "cli"
    - "pdf"
    - "content-extraction"
    - "configuration"
tags: ["kb-genai-builder", "cli", "picocli", "hexagonal-architecture"]
stakeholders: ["@project-owner", "@architecture-team"]
effort: "medium"
---

# ADR 103 : CLI Picocli et architecture hexagonale

## Vue d’ensemble

| Attribut | Valeur |
|---|---|
| Statut | Proposé |
| Date de décision | 2026-08-27 |
| Impact | Moyen |
| Effort | Moyen |
| Risque principal | Refaire remonter la logique métier ou LangChain4j dans la CLI |

## Contexte et problème

La première commande du JAR regroupait l’analyse des arguments, la construction du parseur, l’exécution de la transformation, l’écriture du fichier et la gestion des erreurs. Cette forme est suffisante pour une commande isolée, mais ne donne pas de structure répétable pour les futures opérations documentaires.

Les ADR-101 et 102 imposent déjà un port projet et un adaptateur LangChain4j isolé. L’ADR-600 impose la validation de `env/generated/java.properties` au démarrage des outils Java, et l’ADR-602 maintient Make comme façade d’automatisation.

### Contraintes

- La syntaxe doit offrir de l’aide, des sous-commandes et des erreurs de saisie cohérentes.
- Aucun module applicatif partagé ne doit être créé avant plusieurs appelants réels, conformément à l’ADR-608.
- LangChain4j ne peut être choisi qu’au point de composition.
- La transformation locale reste sans réseau ni secret.
- `make pdf-to-markdown PDF=... OUTPUT=...` demeure l’interface d’automatisation.

## Décision

Nous adoptons **Picocli comme adaptateur entrant et une architecture hexagonale légère**. La CLI et LangChain4j sont deux adaptateurs autour du port documentaire. Aucun package `application` n’est créé à ce stade: la transformation n’a qu’une commande appelante et une délégation supplémentaire dupliquerait une responsabilité sans apporter de contrat stable.

| Zone | Package | Responsabilité |
|---|---|---|
| Entrée et composition | `cli` | Racine Picocli, sous-commandes, contrôle de `java.properties`, sorties console et écriture du fichier |
| Cœur | `document` | Contrat `PdfToMarkdownTransformer`, modèle et erreur stable |
| Infrastructure | `document.langchain4j` | Adaptateur PDFBox fourni par LangChain4j |

La racine `PdfToMarkdownCommand` construit `LangChain4jPdfToMarkdownTransformer` et l’injecte dans `TransformPdfCommand` via le port `PdfToMarkdownTransformer`. La sous-commande publique est :

```text
kb-genai-builder transform-pdf SOURCE [--output FICHIER]
```

Picocli renvoie `2` pour une erreur de syntaxe. La sous-commande renvoie `1` pour une erreur de configuration, de lecture, de transformation ou d’écriture, et `0` après avoir affiché le chemin absolu du Markdown créé.

## Matrice de décision

| Critère | Poids | CLI manuelle | Picocli avec adaptateurs | CLI Spring Boot |
|---|---:|---:|---:|---:|
| Cohérence aide, options et erreurs | 25 % | 4 | 9 | 9 |
| Isolation des dépendances | 25 % | 6 | 9 | 8 |
| Coût d’exécution local | 20 % | 10 | 9 | 4 |
| Testabilité des cas d’usage | 20 % | 6 | 9 | 9 |
| Évolutivité des sous-commandes | 10 % | 5 | 9 | 9 |
| **Total pondéré** | **100 %** | **6,20** | **9,00** | **7,75** |

```text
CLI manuelle = 4×0,25 + 6×0,25 + 10×0,20 + 6×0,20 + 5×0,10 = 6,20
Picocli      = 9×0,25 + 9×0,25 + 9×0,20 + 9×0,20 + 9×0,10 = 9,00
Spring Boot  = 9×0,25 + 8×0,25 + 4×0,20 + 9×0,20 + 9×0,10 = 7,75
```

Picocli est retenu pour son meilleur total pondéré.

## Conséquences

### Positives

- `--help`, les paramètres `Path` et les erreurs de saisie sont fournis par une bibliothèque stable sans framework serveur.
- Les tests de commande peuvent injecter un `PdfToMarkdownTransformer` déterministe.
- Une nouvelle sous-commande peut réutiliser le contrat documentaire sans modifier l’adaptateur LangChain4j.
- L’adaptateur LangChain4j reste hors des packages `application` et `document`.

### Négatives, risques et mitigations

| Risque ou coût | Impact | Mitigation |
|---|---|---|
| Dépendance supplémentaire dans le JAR | Faible | Verrouiller Picocli 4.7.7 dans Maven et couvrir la syntaxe critique |
| Sous-commandes trop couplées au parseur | Moyen | Injection du port projet depuis le point de composition |
| Abstractions de sortie prématurées | Faible | Conserver l’écriture NIO dans la CLI tant qu’un seul adaptateur de sortie existe, conformément à ADR-608 |

## Alternatives considérées

### CLI manuelle avec sous-commandes

Elle évite une dépendance mais réimplique l’analyse des arguments, l’aide et leurs tests. Rejetée car son coût augmente à chaque commande.

### Spring Boot en mode CLI

Elle apporte l’injection de dépendances, mais ajoute un conteneur et un temps de démarrage non justifiés par une transformation locale. À réévaluer pour un service long-vivant.

## Plan d’implémentation

| Phase | Livrable | Validation | Statut |
|---|---|---|---|
| 1 | Dépendance Picocli et sous-commande `transform-pdf` | `mvn test`, `java -jar ... --help` | Réalisé le 2026-08-27 |
| 2 | Port injecté depuis le lanceur | Test de commande avec port déterministe | Réalisé le 2026-08-27 |
| 3 | Extraire un cas d’usage à partir de plusieurs appelants réels | Test du contrat partagé | À faire |

## Critères de succès et validation

| Métrique | Cible | Méthode |
|---|---:|---|
| Imports Picocli hors `cli` | 0 | Recherche dans `src/main/java` |
| Imports LangChain4j hors infrastructure et composition | 0 | Recherche dans `src/main/java` |
| Code de sortie en cas de syntaxe invalide | 2 | Exécution du JAR |
| Transformation Néosanté par Make | Succès | `make pdf-to-markdown` avec sortie non vide |

### Déclencheurs de réévaluation

- besoin d’un serveur HTTP ou d’une exécution longue durée;
- plus de deux adaptateurs de sortie réels;
- contrat CLI versionné pour des consommateurs externes;
- ajout d’OCR ou d’un autre parseur.

## Traçabilité et liens

| Référence | Relation | Description |
|---|---|---|
| ADR-101 | applique | LangChain4j derrière un port projet |
| ADR-102 | complète | Structure de la transformation PDF |
| ADR-600 | respecte | Vérification de la configuration Java générée |
| ADR-602 | respecte | Make reste la façade répétable |
| ADR-608 | applique | Pas de port de sortie prématuré |

### Sources projet consultées

| Ressource | Date | Raison |
|---|---|---|
| `pom.xml` | 2026-08-27 | Dépendances et JAR ombré |
| `src/main/java/net/cotechnoe/kb/genai/` | 2026-08-27 | Frontières des packages et composition |
| `Makefile` | 2026-08-27 | Contrat d’automatisation |
| https://picocli.info/ | 2026-08-27 | Sous-commandes, `Callable<Integer>` et codes de sortie |

## Historique

| Date | Auteur | Changement | Raison |
|---|---|---|---|
| 2026-08-27 | Équipe kb-genai-builder | Création initiale | Formaliser les options 3 et 4 retenues |

## Métadonnées IA

```json
{
  "adr_id": "103",
  "project": "kb-genai-builder",
  "parsing_version": "1.0",
  "validation_status": "proposed",
  "depends_on": ["000", "101", "102", "600", "602", "608"],
  "blocks": [],
  "related": []
}
```