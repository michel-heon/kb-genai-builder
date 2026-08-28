---
adr: 102
title: "Structure Java pour la transformation locale de PDF en Markdown"
status: "proposed"
date: 2026-08-27
superseded_by: null
replaces: null
related_adrs: [0, 101, 600, 602, 608]
related_issues: []
classification:
  lifecycle: "proposed"
  domain: "architecture"
  impact: "high"
  quality:
    - "maintainability"
    - "reliability"
    - "portability"
    - "traceability"
  reversibility: "moderate"
  scope: "tactical"
  tech_areas:
    - "java"
    - "pdf"
    - "content-extraction"
    - "cli"
    - "generative-ai"
tags: ["kb-genai-builder", "java", "pdf", "markdown", "langchain4j"]
stakeholders: ["@project-owner", "@architecture-team", "@content-team"]
effort: "medium"
---

# ADR 102 : Structure Java pour la transformation locale de PDF en Markdown

## Vue d’ensemble

| Attribut | Valeur |
|---|---|
| Statut | Proposé |
| Date de décision | 2026-08-27 |
| Impact | Élevé |
| Effort | Moyen |
| Risque principal | Couplage de l’API de la bibliothèque à LangChain4j ou à une stratégie de parsing unique |

## Contexte et problème

Le projet fournit désormais une première transformation locale d’un fichier PDF vers un artefact Markdown. Les sources Java sont organisées sous `src/main/java/net/cotechnoe/kb/genai`, le build est Maven avec Java 21, et le JAR exécutable expose la commande `PdfToMarkdownCommand`.

L’ADR-101 impose que LangChain4j reste une implémentation optionnelle derrière des ports appartenant au projet. L’ADR-602 désigne le `Makefile` comme façade des opérations répétables. Sans une structure claire, la CLI, le modèle de sortie et le parseur pourraient se coupler directement, ce qui rendrait l’ajout d’OCR, d’un parseur de repli ou de métadonnées de provenance coûteux.

### Faits observés

| Élément | Type | Source ou validation |
|---|---|---|
| Java 21 et Maven constituent le build | Fait | `pom.xml` |
| LangChain4j Core et le parseur Apache PDFBox sont déclarés | Fait | `pom.xml` |
| La CLI est le point d’entrée du JAR ombré | Fait | `pom.xml`, `PdfToMarkdownCommand` |
| Le parseur PDFBox extrait du texte du PDF `neosanté1.pdf` | Fait | Exécution locale du 2026-08-27, sortie de 212795 octets sous `target/verification/` |
| Le corpus complet contient des PDF image-only, chiffrés ou à mise en page complexe | Hypothèse | À VÉRIFIER — qualifier un échantillon représentatif et mesurer les échecs et la fidélité |

### Contraintes

- Aucun appel réseau ne doit être requis pour la transformation par défaut.
- L’API publique ne doit pas exposer de type `dev.langchain4j`.
- La source du résultat doit rester disponible afin d’ajouter la provenance granulaire avant toute publication dans une base de connaissances.
- La première version ne promet ni OCR ni restitution sémantique de la mise en page; elle produit du texte Markdown valide.

## Décision

Adopter une structure en trois couches sous le package racine `net.cotechnoe.kb.genai` :

| Couche | Package | Responsabilité | Dépendances autorisées |
|---|---|---|---|
| Cœur documentaire | `document` | Contrat `PdfToMarkdownTransformer`, résultat `MarkdownDocument` et erreur stable | Bibliothèque Java standard |
| Adaptateur | `document.langchain4j` | Implémentation locale avec `ApachePdfBoxDocumentParser` | Cœur documentaire et LangChain4j |
| Entrée utilisateur | `cli` | Adaptateur Picocli, écriture UTF-8, configuration générée et code de sortie | Cœur documentaire et Picocli |

La CLI construit l’adaptateur concret au point de composition. Les interfaces et records du package `document` n’exposent que `Path`, `String` et des types appartenant au projet. L’adaptateur normalise les fins de ligne et retourne un Markdown textuel; il ne contacte aucun fournisseur et ne journalise pas le contenu extrait. La structure et le contrat de la CLI sont précisés par l’ADR-103.

Le `Makefile` expose `test`, `package`, `pdf-to-markdown` et son alias `build`. Les sorties Maven et de validation restent sous `target/`, ignoré par Git.

### Effet sur la chaîne documentaire

| Étape | Entrée | Sortie | Contrôle |
|---|---|---|---|
| Acquisition | Chemin de PDF explicite | Aucun artefact intermédiaire | Le fichier doit être régulier et porter l’extension `.pdf` |
| Extraction | Flux du PDF | Texte extrait | Erreur stable en cas de parsing impossible |
| Normalisation | Texte extrait | Markdown UTF-8 avec fins de ligne normalisées | Test unitaire déterministe |
| Publication | Markdown local | Fichier `.md` local | Hors périmètre: aucune indexation ni publication distante |

### Principes appliqués

- **Traçabilité** : `MarkdownDocument` conserve le `Path` source normalisé. La provenance par page ou fragment reste à concevoir avant indexation.
- **Moindre privilège** : la transformation lit un fichier local et n’utilise ni secret ni réseau.
- **Réversibilité** : remplacer l’adaptateur ne modifie ni le contrat du cœur ni la CLI; les sorties peuvent être régénérées depuis les PDF.
- **Non-duplication** : aucun module partagé supplémentaire n’est créé sans plusieurs appelants réels, conformément à l’ADR-608.

## Matrice de décision

| Critère | Poids | CLI couplée au parseur | Cœur avec adaptateur | Service distant |
|---|---:|---:|---:|---:|
| Isolation de LangChain4j | 30 % | 3 | 9 | 9 |
| Exécution locale et reproductible | 25 % | 8 | 9 | 3 |
| Évolutivité vers OCR et autres parseurs | 20 % | 4 | 9 | 8 |
| Simplicité initiale | 15 % | 9 | 8 | 4 |
| Coût d’exploitation | 10 % | 9 | 9 | 4 |
| **Total pondéré** | **100 %** | **5,95** | **8,85** | **6,05** |

```text
CLI couplée       = 3×0,30 + 8×0,25 + 4×0,20 + 9×0,15 + 9×0,10 = 5,95
Cœur + adaptateur = 9×0,30 + 9×0,25 + 9×0,20 + 8×0,15 + 9×0,10 = 8,85
Service distant   = 9×0,30 + 3×0,25 + 8×0,20 + 4×0,15 + 4×0,10 = 6,05
```

## Conséquences

### Positives

- les transformations déterministes fonctionnent sans configuration de fournisseur ni secret;
- l’API stable peut recevoir un adaptateur OCR ou Docling sans propagation de types tiers;
- les tests unitaires ne demandent aucun corpus monté ni connexion réseau;
- la CLI est utilisable à partir du JAR ou de `make pdf-to-markdown`.

### Négatives, risques et mitigations

| Risque | Impact | Mitigation |
|---|---|---|
| Le texte seul perd la structure visuelle d’un PDF | Élevé | Qualifier la fidélité sur un corpus de référence avant de promettre des titres, tableaux ou citations de page |
| PDFBox ne traite pas les scans sans couche texte | Moyen | Ajouter un adaptateur OCR distinct après décision DATA et tests de contrat |
| Le parseur LangChain4j est distribué en version bêta | Moyen | Limiter son exposition à l’adaptateur, verrouiller les versions et évaluer sa stabilité avant acceptation |
| Provenance insuffisante pour une indexation fiable | Élevé | Définir un modèle DATA de fragments avec pages et empreinte source avant publication |

## Alternatives considérées

### CLI directement dépendante de PDFBox ou LangChain4j

Cette option réduit le nombre de classes, mais fige le parseur dans le contrat d’usage et mélange transformation, lecture de fichiers et écriture de sortie. Elle est rejetée car elle contrevient aux frontières de l’ADR-101.

### Service externe de conversion

Cette option peut fournir OCR et restitution de mise en page, mais introduit secrets, réseau, coûts et indisponibilités. Elle est rejetée pour la version initiale locale et reste une option si les mesures de qualité le justifient.

## Plan d’implémentation

| Phase | Livrable | Validation | Statut |
|---|---|---|---|
| 1 | Cœur, adaptateur PDFBox, CLI et Makefile | `mvn test` et exécution locale | Réalisé le 2026-08-27 |
| 2 | Corpus de qualification et métriques de fidélité | Tests sur PDF texte, scans et tableaux | À faire |
| 3 | Métadonnées de provenance par fragment | Tests de conservation de la source et des pages | À faire |
| 4 | Adaptateur OCR ou Docling, si justifié | Tests de contrat et intégration explicitement activée | À faire |

## Critères de succès et validation

| Métrique | Référence | Cible | Méthode |
|---|---:|---:|---|
| Imports LangChain4j hors adaptateur | 0 | 0 | Recherche automatisée dans `src/main/java` |
| Tests unitaires dépendant du réseau | 0 | 0 | Revue des tests et exécution locale |
| Transformation de `neosanté1.pdf` | 1 essai | Succès | `make pdf-to-markdown` ou JAR avec sortie non vide |
| Résultats avec provenance de page avant indexation | 0 | 100 % | ADR DATA et tests associés |

### Déclencheurs de réévaluation

- ajout d’un format source autre que PDF;
- besoin d’OCR, de tableaux ou de citations par page;
- exposition de l’API comme dépendance publique pour un consommateur tiers;
- échec mesuré de l’adaptateur sur le corpus de qualification;
- changement majeur de LangChain4j ou du parseur PDFBox.

## Traçabilité et liens

| Référence | Relation | Description |
|---|---|---|
| ADR-000 | respecte | Processus et exigences des ADR |
| ADR-101 | applique | Isolation de LangChain4j derrière des ports projet |
| ADR-600 | respecte | Aucune configuration externe ou secret pour le chemin par défaut |
| ADR-602 | applique | Makefile comme façade d’exécution |
| ADR-608 | applique | Pas d’abstraction partagée prématurée |

### Sources projet consultées

| Ressource | Date | Raison |
|---|---|---|
| `pom.xml` | 2026-08-27 | Versions Java, Maven, LangChain4j et point d’entrée du JAR |
| `src/main/java/net/cotechnoe/kb/genai/document/` | 2026-08-27 | Contrat et modèle du cœur |
| `src/main/java/net/cotechnoe/kb/genai/document/langchain4j/` | 2026-08-27 | Implémentation du parseur local |
| `src/main/java/net/cotechnoe/kb/genai/cli/` | 2026-08-27 | Composition et interface CLI |
| `src/test/java/` | 2026-08-27 | Tests déterministes de l’adaptateur |

## Historique

| Date | Auteur | Changement | Raison |
|---|---|---|---|
| 2026-08-27 | Équipe kb-genai-builder | Création initiale | Formaliser la structure Java mise en place pour la transformation PDF vers Markdown |

## Métadonnées IA

```json
{
  "adr_id": "102",
  "project": "kb-genai-builder",
  "parsing_version": "1.0",
  "validation_status": "proposed",
  "depends_on": ["000", "101", "600", "602", "608"],
  "blocks": [],
  "related": []
}
```