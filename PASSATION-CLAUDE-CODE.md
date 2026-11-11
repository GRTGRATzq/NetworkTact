# Passation Claude Code : où en est NetworkTact

État au **11 novembre 2026, vers 20 h 45 UTC**, pour reprendre dans une conversation neuve. À lire avec `FORK.md` (section de
chaque tâche, état d'avancement), `TESTS-A-FAIRE.md` et, pour la tâche 7, `docs/PLAN-CARTE-TELECHARGEMENT.md`.

## 1. Règles du projet (rappel)

- Dépôt `GRTGRATzq/NetworkTact`, fork Android de Meshtastic-Android. **Jamais de pull request vers
  `meshtastic/Meshtastic-Android`.** Aucune pull request ouverte, rien n'est fusionné dans `main`.
- Une branche par tâche, empilée sur la précédente ; petits commits d'un seul sujet, **messages en français** ; jamais de
  réécriture de l'historique poussé (pas de force push) : on corrige par un nouveau commit.
- Mettre à jour `FORK.md` (section de la tâche + état d'avancement) et `TESTS-A-FAIRE.md`.
- Aucun secret dans le dépôt ni dans les journaux de CI.
- Pas de SDK Android en local : la CI `fork-apk` compile l'APK et lance les tests Android ; `spotlessCheck` et `detekt`
  tournent en local (miroir Maven Central dans `~/.gradle/init.d/`, hors dépôt).

## 2. Branches

| Branche | Contenu | État |
|---|---|---|
| `feat/apk-release` | Tâche 28 : APK de release signé | **Close.** Run 89 vert, empreinte du certificat vérifiée par l'utilisateur (`f4ad3e41…79205818`). |
| `feat/carte-telechargement` | Tâche 7 (FORK.md §29) : téléchargement de carte hors ligne. Partie de `feat/apk-release`, contient tout. | Code de la v1 complet ; restent les essais sur téléphone. Derniers commits de code : `6930d88` et `3fa9397` (liste, commit 12), `cced8d3` (export, commit 15), `62b54e6` (test Compose), `dc5b8b1` (plafond de 500 Mo) ; puis documentation (`2207bec`, `9ee7d3e`) et ce fichier. |
| `wip/carte-telechargement` | Branche d'attente (voir §5), au même commit que `feat/carte-telechargement` (`749ad65`). | **À supprimer par l'utilisateur sur GitHub** (onglet Branches) : la suppression par `git push --delete` est coupée par le proxy de l'environnement Claude Code (11 novembre au soir). Rien n'y est perdu. Pour une prochaine attente pendant un run, la recréer depuis `feat/`. |

Les tâches 1 à 27 sont sur les branches `feat/…`, `docs/…` et `fix/…` listées dans `FORK.md` (état d'avancement).

## 3. Runs

| Run | Branche, commit | Résultat |
|---|---|---|
| `fork-apk` 89 | `feat/apk-release`, `a14a7f3` | vert, empreinte conforme |
| `fork-apk` 90, 92, 96, 97 | `feat/carte-telechargement` | verts (96 : 78 tests de l'extracteur dans `jvmTest` et `testAndroidHostTest`) |
| `fork-apk` 91, 93, 94, 95 | idem | annulés par des poussées suivantes (avant la règle du §5) |
| `fork-apk` 98 | `4f29b8b` (contrôleur, branchement Android, écran) | **vert** en entier (lint, compilation, tests, `feature:map`, APK debug, `feature:map-maplibre`, APK release et son contrôle) |
| `fork-apk` 99 | `3fa9397` (liste enrichie, « Reprendre », commit 12) | **vert** |
| **`fork-apk` 100** | `9ee7d3e` (export, test Compose, plafond de 500 Mo, documentation) | **vert** en entier (19 h 42 – 20 h 09 UTC) : lint, compilation, tests, `core:demo`, `feature:map`, APK debug, `feature:map-maplibre`, APK release et son contrôle. Artefact **`apk-fdroid-release`** à installer pour les essais. `fork-carte-interop` 7, même commit : vert. |
| `fork-carte-sonde` 1, 2 | sonde D1 | verts ; résultats au §15 du plan |
| `fork-carte-interop` 1 à 3, 5 | extracteur sur la vraie édition | verts : **identique tuile par tuile à `pmtiles extract`** (Cincu, 6 822 tuiles, 14,65 Mo) |
| `fork-carte-interop` 4 | `4f29b8b` | rouge : le script `build.sh` ne compilait pas `OfflineMapFiles.kt`, dont le contrôleur dépend ; corrigé par `c6ad820` (run 5 vert) |

Pour lire un journal complet de run : `gh api repos/GRTGRATzq/NetworkTact/actions/jobs/<job>/logs` (lecture seule,
autorisé par l'utilisateur) ; l'outil GitHub de la session ne rend que les 5 000 dernières lignes.

## 4. Tâche 7 : ce qui est fait

Code commun, sans Android (`feature/map/src/commonMain/.../offline/extract/`), **95 tests** en `commonTest` :

- Extracteur PMTiles v3 : identifiants de Hilbert, couverture d'un rectangle, répertoires et en-tête, plan d'extraction,
  fusion des plages (5 %), source HTTP par plages (Ktor, `If-Match`, essais 1/3/9 s, **`User-Agent` neutre
  `pmtiles-client/1.0`**), écriture à sa place, journal de reprise, contrôle final, orchestration parallèle.
- Édition la plus récente sans date codée en dur (J à J-7 UTC, recalage par l'en-tête `Date` du serveur), source réglable
  https, contrôle du schéma (4.x, 9 couches du style 5.7.2), estimation grossière (coefficients Cincu et Draguignan).
- `MapDownloadController` : estimation exacte, téléchargement, pause sur coupure, reprise, annulation, installation dans
  `pmtiles/` (`<id>.pmtiles`, `<id>.name`, `<id>.json`), mode démo refusé, espace libre, `frameOpen`.
- Logique du cadre (`DownloadFrame`, `DownloadArea`) : coins, bornes, taille en km, zone trop petite.

Android (variante fdroid) :

- `OfflineMapLibrary` : `downloads` branché, **client HTTP dédié** (pas celui de l'application, qui nomme l'application dans
  son `User-Agent`, cache sur disque et journalise en debug), dossier `pmtiles-download/`, mode démo, nettoyage `.json`.
- Écran (`feature/map-maplibre/.../component/MapDownload*.kt`) : bouton « Télécharger une carte » dans « Cartes sur le
  téléphone » ; cadre à poignées sur la carte (remise au nord et à plat), « Utiliser la vue », zoom 12 à 15 (15 par défaut),
  taille grossière puis exacte, mention de confidentialité toujours affichée, source modifiable, confirmation au-delà de
  50 Mo, progression, annulation, reprise d'un téléchargement interrompu, messages d'erreur. Textes EN/FR (39 clés).
- Liste « Cartes sur le téléphone » (`OfflineMapFiles.kt`, morceaux dans `OfflineMapFileParts.kt`) : ligne « Édition du …,
  hôte · L × H km » lue dans `<id>.json` (`DownloadedMapInfo.decode`, testé) ; téléchargement interrompu avec « Reprendre »
  (ouvre le cadre et reprend) ; **« Exporter »** (`rememberPmTilesExport`, `ACTION_CREATE_DOCUMENT`, nom par
  `exportFileName`, testé ; `OfflineMapLibrary.startExport` copie sous le mutex et supprime le document en cas d'échec).
  `OfflineMapFilesMenuItem` reçoit `downloads = <contrôleur>` et `onExport`.
- Plafond de **500 Mo** par carte (D4) appliqué au calcul de la taille (`MapDownloadController.maxFileBytes`, testé).
- Test Compose du panneau : `feature/map-maplibre/src/jvmTest/.../MapDownloadPanelTest.kt` (7 cas ; `Dispatchers.setMain`
  obligatoire, faute de dispatcher principal sur la JVM). Les poignées du cadre ne sont pas testées (il faut une carte MapLibre).

Outils : `scripts/carte-sonde/` (sonde, workflow `fork-carte-sonde`), `scripts/carte-interop/` (extracteur compilé seul,
`pmtiles verify`, comparaison avec `pmtiles extract`, workflow `fork-carte-interop`, artefact `cartes-cincu`).

## 5. Méthode de travail en vigueur

- **Ne pas annuler les runs `fork-apk`** : grouper les commits ; pendant un run, pousser sur `wip/carte-telechargement`
  (aucun workflow ne s'y déclenche), puis pousser les mêmes commits sur `feat/carte-telechargement` une fois le run fini.
  Raison : le crochet d'arrêt de l'environnement Claude Code (`~/.claude/stop-hook-git-check.sh`, hors dépôt) refuse de finir
  une réponse avec des commits présents sur aucune branche distante.
- Une poussée qui ne change que des `.md` ne lance pas `fork-apk` (ni les fichiers de la sonde et de l'essai
  d'interopérabilité).
- Vérifications locales avant poussée : `./gradlew :<module>:spotlessApply :<module>:spotlessCheck :<module>:detekt` ;
  **`./gradlew :feature:map-maplibre:compileKotlinJvm` compile l'écran en local** (cible bureau, sans SDK Android) ;
  **les tests JVM tournent aussi en local, sans SDK** : `./gradlew :feature:map:jvmTest --tests '*Nom*'` et
  `./gradlew :feature:map-maplibre:jvmTest --tests '*Nom*'` (vérifié le 11 novembre au soir). Seul `androidApp` (fdroid)
  n'est compilé que par la CI.
- Prévenir l'utilisateur à chaque run `fork-apk` vert ou rouge.
- **Consignes de l'utilisateur (11 novembre 2026)** : plus aucun remplacement automatique sur des fichiers entiers (script
  Python ou `sed`) pour l'écran : seulement des modifications ciblées (outil d'édition) ; `detekt` et `spotless` en local
  **avant chaque commit**. Raison : un remplacement par script a abîmé `MapDownloadPanel.kt` le 11 novembre (fichier non
  commité, réécrit à la main avant le commit `4f29b8b`).
- La tâche planifiée qui relisait le run 98 (`trig_01PztYWhuaGwvNDpHmsGy9zo`) a été supprimée le 11 novembre au soir.
- Le crochet `pre-push` de l'environnement lance detekt : s'il échoue sur « Gradle build daemon disappeared », relancer la
  poussée (ce n'est pas une erreur de lint).

## 6. Reste à faire (tâche 7)

Faits le 11 novembre au soir : run 98 vert, commit 12 (liste), commit 15 (export), test Compose, plafond de 500 Mo,
documentation (`FORK.md` §29, `CARTE-HORS-LIGNE.md`, `TESTS-A-FAIRE.md` §17).

Run `fork-apk` 100 vert (`fork-carte-interop` 7 aussi) : l'export et la liste enrichie compilent pour Android.

1. **Essais sur téléphone** (`TESTS-A-FAIRE.md` §17), par l'utilisateur, avec l'artefact **`apk-fdroid-release` du run
   `fork-apk` 100** (empreinte du certificat à vérifier : `f4ad3e41…79205818`). Ordre conseillé : (1) Cincu environ
   60 × 60 km, zoom 15 (tailles annoncée et réelle, durée, mode avion, mention ⓘ) ; (2) le cadre au doigt ; (3) mention de
   confidentialité et refus de `http://` ; (4) coupure puis « Reprendre », fermeture de l'application puis « Reprendre »
   depuis la liste, « Annuler » sans fichier résiduel ; (5) « Exporter » puis comparaison sur Mac avec
   `cincu-30km.pmtiles`. Le reste de la §17 ensuite. Corriger selon les retours, en commits ciblés.
2. Fin de tâche : vérifier que `wip/carte-telechargement` a bien été supprimée (voir §2).

Plus tard (décidé) : cartes raster (tâche distincte, plan §17) ; WorkManager seulement si l'essai le justifie (D6) ;
saisie des dimensions au clavier.

## 7. Points d'attention

- **Rien de l'écran n'a tourné sur un téléphone ni dans un émulateur** : il compile (JVM local, Android par la CI) et sa
  logique est testée, mais l'ergonomie du cadre, la projection écran ↔ carte et la mention de confidentialité sont à voir.
- `build.protomaps.com` est refusé par le proxy de l'environnement Claude Code : toute mesure du vrai serveur passe par un
  workflow de la CI (sonde, interop).
- Les éditions Protomaps ne vivent que 7 jours : une reprise n'est possible que tant que l'édition existe.
- `values-fr/strings.xml` n'est pas trié (le script ne trie que l'anglais) : les textes du fork y sont ajoutés à la fin.
