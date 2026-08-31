---
adr: 400
title: "Transformation locale et traçable des dictionnaires CodeBio"
status: "proposed"
date: 2026-08-29
superseded_by: null
replaces: null
related_adrs: [0, 102, 104, 600]
related_issues: [1]
classification:
  lifecycle: "proposed"
  domain: "data"
  impact: "high"
  quality:
    - "data-integrity"
    - "traceability"
    - "reliability"
    - "portability"
  reversibility: "easy"
  scope: "tactical"
  tech_areas:
    - "knowledge-base"
    - "pdf"
    - "ocr"
    - "markdown"
tags: ["codebio", "dictionary", "pdf", "ocr", "provenance"]
stakeholders: ["@project-owner", "@content-team", "@architecture-team"]
effort: "high"
---

# ADR 400 : Transformation locale et traçable des dictionnaires CodeBio

## Vue d’ensemble

| Attribut | Valeur |
|---|---|
| Statut | Proposé |
| Date de décision | 2026-08-29 |
| Corpus canonique | `workspace/08-KB-NéoSanté/Dictionnaire CodeBio 2026/` |
| Corpus d’échantillonnage | `workspace/08-KB-NéoSanté/Dictionnaires par parties/` |
| Sortie configurée | `workspace/dico-test/` |
| Traitement | Local, déterministe, sans LLM ni service réseau |
| Impact | Élevé |
| Effort | Élevé |
| Risque principal | Altérer un terme ou perdre sa page source sans signaler l’incertitude |

## Contexte et problème

L’issue GitHub no 1 demande de mesurer le comportement de PDFBox sur le corpus de qualification et de conserver des résultats reproductibles avant de choisir une solution OCR. Les dictionnaires CodeBio doivent ensuite pouvoir alimenter une base de connaissances qui recherche des termes exacts et ne doit pas inventer de contenu absent de la source.

Le transformateur défini par l’ADR-102 utilise actuellement `ApachePdfBoxDocumentParser` et produit un `MarkdownDocument` composé d’un chemin source et d’une chaîne globale. Ce contrat convient à un PDF possédant une couche texte fiable, mais il ne conserve ni les limites de pages, ni la méthode d’extraction, ni un indicateur d’incertitude.

Les dictionnaires existent sous deux formes locales différentes :

- deux tomes canoniques qui constituent la source de validation complète ;
- vingt PDF découpés par parties, plus rapides à échantillonner et déjà enrichis d’une couche OCR par I.R.I.S.

Ces formes ne sont pas interchangeables : les deux tomes totalisent 960 pages, tandis que les parties en totalisent 954. Le corpus découpé est donc un corpus complémentaire de qualification, pas la référence canonique.

### Structure éditoriale observée

L’analyse visuelle et textuelle de `DictionnaireDesCodesBiologiquesDesMaladies_Partie4.pdf` montre les éléments suivants :

- les entrées sont ordonnées alphabétiquement ;
- une entrée commence généralement par un titre dans un bandeau encadré ;
- une entrée peut commencer sur une page et se poursuivre sur la suivante ;
- une page peut contenir la fin d’une entrée et plusieurs nouvelles entrées ;
- les rubriques ne sont pas toutes obligatoires et comprennent notamment `Étym`, `Syn`, `DÉFINITION`, `CAUSES`, `SYMPTÔMES`, `CONFLITS`, `FONCTION`, `ÉTHOLOGIE`, `MOTS`, `VERBES` et `REMÈDES` ;
- des références abrégées entre parenthèses attribuent certains passages à leurs sources ;
- des illustrations peuvent être intégrées au milieu d’une entrée ;
- le numéro imprimé de la page fait partie de la preuve documentaire, mais ne suffit pas à identifier une page sans le fichier source.

La structure logique ne coïncide donc ni avec une page, ni avec un bloc de texte global. Le modèle de sortie doit représenter séparément le document, les pages, les entrées et leurs rubriques.

### Faits observés le 2026-08-29

| Élément | Résultat | Portée |
|---|---|---|
| Tome canonique 1 | 490 pages | Corpus complet |
| Tome canonique 2 | 470 pages | Corpus complet |
| Texte natif des tomes | 0 mot sur 960 pages | Extraction PDFBox/Poppler sans OCR insuffisante |
| Images des tomes | Au moins une image par page | OCR nécessaire pour les tomes canoniques |
| Chiffrement des tomes | Aucun | Pas de blocage par mot de passe |
| Corpus découpé | 19 parties de 50 pages et une partie de 4 pages, soit 954 pages | Échantillonnage uniquement |
| Partie 4 | 50 pages, 24 499 mots et 173 263 octets extraits avec `pdftotext -layout` | Couche OCR existante |
| Partie 4 | Une image CCITT en niveaux de gris par page, à 300 dpi | Image source préservée sous la couche OCR |
| Partie 4 | Créateur et producteur I.R.I.S., PDF non balisé | Origine de la couche OCR |
| Partie 4 | Ruptures observées comme `A athie` et `A hasie` | Une couche texte non vide n’est pas une preuve de fidélité |
| Partie 1 découpée | 14 047 mots extraits | Variabilité entre parties |
| Partie 20 découpée | 13 mots extraits sur 4 pages | Le volume seul ne qualifie pas la qualité |

L’empreinte SHA-256 de la partie 4 analysée est :

```text
ce0c2d77e45447cba0399e59d1e1349e55552169aa2147e914d7d6a74a66c2b0
```

### Limites de la mesure

- aucune précision OCR n’a été mesurée contre une transcription de référence ;
- aucune comparaison de moteurs OCR n’a encore été réalisée ;
- le corpus découpé ne couvre pas les six pages d’écart avec les tomes canoniques ;
- la confiance fournie par un moteur ne peut pas être reconstruite à partir de la couche I.R.I.S. existante ;
- les observations de structure portent principalement sur la partie 4 et devront être confirmées sur les deux tomes.

## Décision

Adopter une transformation **adaptative, locale et orientée page**, isolée derrière un port du projet. Cette décision fixe le contrat de données et la procédure de qualification ; elle ne choisit pas encore un moteur OCR définitif.

La chaîne suit les règles suivantes :

1. Identifier le fichier par son chemin relatif et son empreinte SHA-256.
2. Traiter les pages dans leur ordre physique en conservant leur numéro PDF et, lorsqu’il est lisible, leur numéro imprimé.
3. Examiner la couche texte de chaque page au lieu de décider une seule fois pour tout le document.
4. Réutiliser une couche texte existante uniquement si elle franchit les contrôles de fidélité définis sur le corpus de qualification.
5. Rasteriser et soumettre à un moteur OCR local toute page sans texte exploitable ou ayant échoué au contrôle.
6. Conserver la sortie brute par page avant toute normalisation ou structuration.
7. Construire les entrées et rubriques à partir des pages sans perdre les références vers les fragments sources.
8. Interdire toute correction sémantique silencieuse : la normalisation autorisée est déterministe, versionnée et réversible.
9. Ne faire appel à aucun LLM, service réseau ou secret pendant l’extraction et la structuration.
10. Produire les artefacts finaux de façon atomique ; une page manquante ou illisible met le document en échec ou en quarantaine, jamais en succès partiel silencieux.

### Contrat de données minimal

| Objet | Champs obligatoires | Rôle |
|---|---|---|
| Document | chemin source, SHA-256, nombre de pages, version du pipeline | Identité reproductible de la source |
| Page | numéro PDF, numéro imprimé si détecté, texte brut, méthode d’extraction, état de contrôle | Unité de preuve et de reprise |
| Fragment | page, plage ou coordonnées si disponibles, texte brut | Provenance fine d’un contenu |
| Entrée | titre brut, titre normalisé optionnel, fragments sources | Unité logique du dictionnaire |
| Rubrique | type brut, type normalisé optionnel, fragments sources | Structure interne d’une entrée |
| Diagnostic | page, code, message, version de l’outil | Échec ou incertitude exploitable |

Les méthodes d’extraction sont au minimum `native`, `embedded-ocr` et `local-ocr`. Une confiance est conservée lorsqu’elle est fournie par le moteur ; sinon elle vaut `unknown` et ne doit pas être inventée.

Le texte brut reste immuable. Un titre normalisé, par exemple pour rapprocher une rupture OCR, est un champ dérivé distinct qui référence toujours le titre brut et la page source.

### Artefacts attendus

| Artefact | Contenu | Usage |
|---|---|---|
| Manifeste | Identité de la source, outils, versions, paramètres, durées et résultat | Reproductibilité |
| Pages brutes | Texte ordonné et diagnostics par page | Audit et reprise |
| Entrées structurées | Titres, rubriques et références de fragments | Recherche et indexation futures |
| Markdown | Restitution lisible avec ancres de provenance | Relecture humaine |
| Quarantaine | Pages ou documents non qualifiés avec diagnostics | Empêcher une publication trompeuse |

Le format sérialisé et son schéma devront être versionnés avant l’implémentation. Le Markdown seul n’est pas le format de vérité, car il ne porte pas naturellement tous les diagnostics et liens de provenance.

### Qualification en deux niveaux

1. **Boucle rapide** : utiliser en priorité la partie 4, complétée par des pages de début, milieu et fin d’autres parties. Cette boucle mesure la structure, les termes, les rubriques, les illustrations et les erreurs de la couche OCR existante.
2. **Validation complète** : exécuter les deux tomes canoniques et vérifier les 960 pages avant d’accepter le pipeline.

La partie 4 est obligatoire dans la boucle rapide parce qu’elle contient des entrées qui traversent les pages, plusieurs rubriques, plusieurs entrées par page et au moins une illustration intégrée.

### Port et adaptateurs

Le domaine du projet possède le contrat page-orienté. PDFBox, Tesseract, Tess4J ou tout autre outil restent des détails d’adaptateurs remplaçables.

PDFBox demeure candidat pour inspecter et rendre les pages. Tesseract 5 est un candidat de moteur OCR local sous licence Apache-2.0. Tess4J est un candidat d’intégration Java par JNA, également sous licence Apache-2.0. OCRmyPDF est conservé comme alternative de prétraitement produisant un PDF recherchable, mais ajoute une chaîne Python et des dépendances système.

Le choix entre ces adaptateurs sera pris après un benchmark reproductible sur les mêmes pages, avec versions et paramètres figés.

## Matrice de décision

Notes de 1 à 10, avant benchmark de précision OCR.

| Critère | Poids | Texte global PDFBox | OCR systématique | Extraction adaptative par page | OCRmyPDF en amont |
|---|---:|---:|---:|---:|---:|
| Fidélité potentielle | 30 % | 1 | 7 | 9 | 8 |
| Provenance par page | 25 % | 2 | 8 | 10 | 7 |
| Exécution locale | 20 % | 10 | 10 | 10 | 10 |
| Coût de traitement | 15 % | 10 | 3 | 8 | 5 |
| Simplicité d’exploitation | 10 % | 9 | 6 | 6 | 4 |
| **Total pondéré** | **100 %** | **5,05** | **7,15** | **8,95** | **7,25** |

L’extraction adaptative est retenue parce qu’elle préserve la preuve page par page, évite de refaire un OCR déjà qualifié et permet de changer de moteur sans modifier le modèle métier. Sa note de fidélité reste une hypothèse à confirmer par le benchmark.

## Alternatives considérées

### Conserver le texte global fourni par PDFBox

Rejeté. Les tomes canoniques ne contiennent pas de texte natif exploitable et le contrat global perd les limites de pages.

### Faire confiance à toute couche OCR existante

Rejeté. La partie 4 prouve qu’une couche non vide peut contenir des mots coupés ou substitués, incompatibles avec une recherche stricte de termes.

### Appliquer systématiquement un nouvel OCR

Non retenu par défaut. Cette option facilite l’homogénéité, mais augmente fortement le temps de traitement et peut dégrader une couche existante de meilleure qualité.

### Utiliser un service OCR distant ou un LLM

Rejeté pour ce flux. Cela contredirait les exigences d’exécution locale, introduirait des secrets et rendrait la reproductibilité dépendante d’un service externe.

### Choisir immédiatement Tesseract avec Tess4J

Différé. L’intégration est cohérente avec Java et les licences, mais sa précision sur les termes médicaux, accents, abréviations et mises en page du corpus n’a pas encore été mesurée.

## Conséquences

### Positives

- chaque contenu structuré reste relié au fichier et à la page qui le justifient ;
- les tomes image-only et les parties déjà OCRisées utilisent le même contrat ;
- les erreurs OCR ne sont pas confondues avec la vérité documentaire ;
- une page peut être reprise sans retraiter le document entier ;
- le moteur OCR reste remplaçable ;
- la boucle rapide sur la partie 4 réduit le coût des itérations.

### Négatives, risques et mitigations

| Risque | Impact | Mitigation |
|---|---|---|
| Le modèle page/entrée est plus complexe qu’une chaîne globale | Moyen | Limiter le contrat aux champs obligatoires et versionner le schéma |
| La détection des titres produit de faux découpages | Élevé | Conserver le texte page par page et tester les entrées traversant une page |
| Un score de confiance élevé masque une erreur de terme | Élevé | Évaluer sur une transcription de référence et contrôler les termes exacts |
| Le nouvel OCR est plus mauvais que la couche I.R.I.S. | Élevé | Comparer les deux sorties et autoriser la réutilisation qualifiée par page |
| Une illustration rompt l’ordre de lecture | Moyen | Inclure des pages illustrées dans le corpus de référence |
| Le traitement des 960 pages est long | Moyen | Boucle rapide sur la partie 4, cache par empreinte et reprise par page |
| Le corpus découpé diverge des tomes | Élevé | Réserver l’acceptation finale aux deux tomes canoniques |

## Plan d’implémentation

| Phase | Livrables | Validation | Statut |
|---|---|---|---|
| 1. Mesure | Inventaire des corpus, comportement PDFBox/Poppler, empreinte de la partie 4 | Commandes reproductibles et constats du présent ADR | Réalisé le 2026-08-29 |
| 2. Référence | Sélection et transcription manuelle de pages représentatives, dont la partie 4 | Double relecture des termes et rubriques | À faire |
| 3. Contrat | Modèle document/page/fragment/entrée/rubrique et schéma versionné | Tests de sérialisation et provenance | À faire |
| 4. Benchmark | Adaptateurs candidats exécutés avec paramètres figés | Précision, rappel structurel, durée et mémoire | À faire |
| 5. Décision outil | Choix du moteur et de son mode d’intégration | ADR amendé ou ADR technique lié | À faire |
| 6. Implémentation | Adaptateur OCR local et transformation structurée | Tests unitaires et corpus rapide | À faire |
| 7. Validation complète | Transformation des deux tomes canoniques | 960 pages comptées, aucune omission silencieuse | À faire |

## Critères de succès

- les outils, versions, paramètres, empreintes et codes de sortie sont consignés ;
- les 960 pages canoniques sont présentes dans le manifeste final ;
- chaque entrée et rubrique référence au moins un fragment et une page source ;
- une entrée traversant une page reste une seule entrée avec plusieurs fragments ;
- plusieurs entrées présentes sur une page restent distinctes ;
- les pages illustrées conservent un ordre de lecture contrôlé ;
- le texte brut n’est jamais remplacé par une correction silencieuse ;
- toute page vide, illisible ou en erreur est signalée et empêche un succès complet ;
- les termes, accents et abréviations du jeu de référence respectent les seuils décidés après constitution de la vérité terrain ;
- une seconde exécution avec les mêmes sources, versions et paramètres produit les mêmes artefacts textuels ;
- aucune requête réseau, aucun LLM et aucun secret ne sont nécessaires.

Les seuils numériques de qualité OCR seront ajoutés après création de la transcription de référence. Fixer un seuil avant de disposer de cette vérité terrain donnerait une précision non mesurée.

## Commandes de reproduction

Depuis la racine du dépôt :

```bash
CANONICAL="workspace/08-KB-NéoSanté/Dictionnaire CodeBio 2026"
SAMPLE="workspace/08-KB-NéoSanté/Dictionnaires par parties"
PART4="$SAMPLE/DictionnaireDesCodesBiologiquesDesMaladies_Partie4.pdf"

pdfinfo "$PART4"
pdffonts "$PART4"
pdfimages -list "$PART4"
pdftotext -layout "$PART4" /tmp/codebio-partie4.txt
wc -l -w -c /tmp/codebio-partie4.txt
sha256sum "$PART4"

find "$SAMPLE" -type f -iname '*.pdf' -print0 |
  while IFS= read -r -d '' pdf; do
    printf '%s\t' "${pdf#$SAMPLE/}"
    pdfinfo "$pdf" | awk '/^Pages:/ {print $2}'
  done

for pdf in "$CANONICAL"/*.pdf; do
  pdfinfo "$pdf" | grep -E '^(Pages|Encrypted):'
  pdftotext -layout "$pdf" - | wc -w
done
```

La sortie exacte dépend des versions installées. Le manifeste du futur pipeline devra donc capturer au minimum les versions de Java, PDFBox, du moteur OCR, de ses données de langue et des utilitaires de qualification.

## Déclencheurs de réévaluation

- obtention d’une version des tomes avec couche texte certifiée ;
- changement du corpus canonique ou de ses empreintes ;
- précision insuffisante du moteur retenu sur les termes médicaux ;
- besoin de restituer la position exacte des illustrations ou des tableaux ;
- ajout d’une correction éditoriale humaine ;
- changement du format cible d’indexation.

## Traçabilité et sources

- [Issue GitHub no 1](https://github.com/michel-heon/kb-genai-builder/issues/1) — demande de mesure reproductible avant choix OCR.
- [ADR-102](./102-ARCH-structure-java-transformation-pdf-markdown.md) — port de transformation et adaptateur PDFBox existants.
- [ADR-104](./104-ARCH-transformation-revues-neosante.md) — organisation de la transformation documentaire Néosanté.
- [Tesseract User Manual](https://tesseract-ocr.github.io/tessdoc/) — moteur OCR local et licence Apache-2.0.
- [Tess4J](https://github.com/nguyenq/tess4j) — enveloppe Java JNA pour Tesseract, licence Apache-2.0.
- [OCRmyPDF](https://ocrmypdf.readthedocs.io/en/latest/introduction.html) — ajout d’une couche OCR à un PDF scanné et limites déclarées.
- [Apache PDFBox dependencies](https://pdfbox.apache.org/2.0/dependencies.html) — composants de rendu et dépendances d’images optionnelles.

## Historique

| Date | Auteur | Changement | Raison |
|---|---|---|---|
| 2026-08-29 | Équipe kb-genai-builder | Création de l’ADR | Définir le contrat de transformation des dictionnaires avant le choix et l’implémentation d’un moteur OCR |