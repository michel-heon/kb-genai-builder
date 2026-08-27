---
adr: 611
title: "Gestion des couleurs et sorties terminal dans les scripts et Make"
status: "proposed"
date: 2026-08-27
superseded_by: null
replaces: null
related_adrs: [0, 601, 602, 608]
related_issues: []
classification:
  lifecycle: "proposed"
  domain: "devops"
  impact: "low"
  quality:
    - "maintainability"
    - "usability"
    - "portability"
    - "reliability"
  reversibility: "easy"
  scope: "operational"
  tech_areas:
    - "automation"
    - "logging"
    - "monitoring"
tags: ["makefile", "scripts", "ansi", "colors", "terminal", "logging"]
stakeholders: ["@operations-team", "@architecture-team"]
effort: "low"
---

# ADR 611 : Gestion des couleurs et sorties terminal dans les scripts et Make

## Vue d’ensemble

| Attribut | Valeur |
|---|---|
| Statut | Proposé |
| Date | 2026-08-27 |
| Impact | Faible |
| Réversibilité | Facile |

## Contexte et problème

Les futures commandes de développement et les utilitaires de la bibliothèque produiront des diagnostics dans des terminaux interactifs, la CI et des journaux redirigés. Les séquences ANSI forcées polluent les logs et les données destinées à être parsées. Des messages uniquement colorés sont également ambigus et peu accessibles.

Aucun script ou `Makefile` n’existe encore dans le répertoire observé. La convention est proposée avant leur création pour éviter une migration ultérieure.

## Décision

### Canaux de sortie

1. stdout contient les résultats normaux ou données explicitement demandées ;
2. stderr contient diagnostics, avertissements et erreurs ;
3. un message d’erreur indique textuellement l’étape, la cause connue et l’action possible ;
4. les données structurées ne contiennent ni décoration ANSI ni texte parasite ;
5. aucun secret, contenu documentaire sensible ou jeton n’est inclus dans un diagnostic.

### Format

Pour Bash et les recettes Make, utiliser `printf` avec format constant et fin de ligne explicite. Ne jamais utiliser `echo -e`.

```bash
printf 'Lot validé : %s\n' "$batch_id"
printf 'Configuration absente : %s\n' "$variable_name" >&2
```

```makefile
	@printf '%s\n' 'Validation terminée'
```

Dans les autres langages, utiliser leur API standard de sortie ou de journalisation sans reproduire artificiellement l’implémentation Bash.

### Couleurs

- la couleur est facultative et ne porte jamais seule le statut ;
- elle est désactivée pour une sortie non interactive ;
- elle est désactivée lorsque `NO_COLOR` est définie, quelle que soit sa valeur ;
- une option explicite peut forcer un mode sans couleur pour la CI ;
- les identifiants, chemins et valeurs destinés à être copiés restent non colorés ;
- des constantes ou helpers ne sont mutualisés qu’au seuil défini par l’ADR-608.

### Niveaux et préfixes

Si des préfixes sont nécessaires, utiliser un vocabulaire court et stable : `INFO`, `WARN`, `ERROR`. Les scripts ne doivent pas imiter un système complet de logging structuré ; lorsqu’un besoin de corrélation, timestamps ou JSON apparaît, il relève de l’API de journalisation du langage ou d’un ADR d’observabilité.

## Matrice de décision

| Critère | Poids | `printf` | `echo` | `echo -e` | Bibliothèque externe |
|---|---:|---:|---:|---:|---:|
| Portabilité shell | 30 % | 9 | 6 | 3 | 7 |
| Contrôle du format | 25 % | 10 | 5 | 5 | 9 |
| CI et redirection | 20 % | 9 | 7 | 4 | 9 |
| Simplicité | 15 % | 8 | 9 | 8 | 4 |
| Dépendances | 10 % | 10 | 10 | 10 | 3 |
| **Total pondéré** | **100 %** | **9,20** | **6,80** | **5,15** | **7,05** |

```text
printf      = 9×0,30 + 10×0,25 + 9×0,20 + 8×0,15 + 10×0,10 = 9,20
echo        = 6×0,30 + 5×0,25 + 7×0,20 + 9×0,15 + 10×0,10 = 6,80
echo -e     = 3×0,30 + 5×0,25 + 4×0,20 + 8×0,15 + 10×0,10 = 5,15
Bibliothèque = 7×0,30 + 9×0,25 + 9×0,20 + 4×0,15 + 3×0,10 = 7,05
```

## Conséquences

### Positives

- sorties lisibles en terminal, CI et redirection ;
- formats contrôlés et plus faciles à tester ;
- absence de couleur dans les données analysables ;
- messages accessibles même sans rendu visuel.

### Négatives

- `printf` est plus explicite donc légèrement plus verbeux ;
- la détection d’un terminal dépend du langage ;
- Make et les scripts peuvent conserver de petites implémentations séparées.

## Alternatives considérées

### `echo` ou `echo -e`

Plus courts, mais leur traitement des options et échappements est moins prévisible entre environnements.

### Couleurs toujours actives

Améliore certains terminaux, mais dégrade logs, accessibilité et sorties redirigées.

### Bibliothèque de logging externe

Disproportionnée pour l’orchestration initiale. Elle pourra être adoptée dans le code de la bibliothèque si des journaux structurés deviennent nécessaires.

## Plan d’implémentation

| Phase | Action | Validation | Statut |
|---|---|---|---|
| 1 | Faire accepter cet ADR | Revue des stakeholders | À faire |
| 2 | Appliquer la convention aux premiers scripts et au Makefile | Tests avec terminal et redirection | À faire |
| 3 | Ajouter un contrôle interdisant `echo -e` | Cas négatif détecté | À faire |
| 4 | Introduire des helpers uniquement si le seuil ADR-608 est atteint | Plusieurs appelants réels | Au besoin |

## Critères de succès

- aucun usage de `echo -e` ;
- aucune séquence ANSI lorsque `NO_COLOR` est définie ou que la sortie est non interactive ;
- toute erreur est textuellement identifiable sur stderr ;
- aucune donnée structurée n’est précédée d’un message décoratif ;
- les tests utilisent des valeurs sentinelles pour vérifier qu’aucun secret n’est journalisé.

## Traçabilité et liens

- [ADR-601](./601-DEVOPS-nomenclature-scripts.md) — scripts d’automatisation.
- [ADR-602](./602-DEVOPS-makefile-orchestrateur.md) — recettes Make.
- [ADR-608](./608-DEVOPS-non-duplication-fonctionnelle-transversale.md) — mutualisation des helpers.
- [NO_COLOR](https://no-color.org/) — convention publique de désactivation des couleurs.
- [POSIX `printf`](https://pubs.opengroup.org/onlinepubs/9699919799/utilities/printf.html) — spécification de l’utilitaire.

## Historique

| Date | Auteur | Changement | Raison |
|---|---|---|---|
| 2026-08-27 | Équipe kb-genai-builder | Import et adaptation de l’ADR-611 source | Standardiser les sorties des utilitaires pour les humains, la CI et les outils d’analyse |
