# Passation Claude Code : où en est NetworkTact

État au **11 novembre 2026, 18 h 52 UTC**, pour reprendre dans une conversation neuve. À lire avec `FORK.md` (section de
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
| `feat/carte-telechargement` | Tâche 7 (FORK.md §29) : téléchargement de carte hors ligne. Partie de `feat/apk-release`, contient tout. | En cours. Dernier commit de code `4f29b8b` (écran du rectangle), puis `c6ad820` (script de l'essai d'interopérabilité) et ce fichier. |
| `wip/carte-telechargement` | Branche d'attente (voir §5), au même commit que `feat/carte-telechargement`. | **À supprimer à la fin de la tâche 7.** |

Les tâches 1 à 27 sont sur les branches `feat/…`, `docs/…` et `fix/…` listées dans `FORK.md` (état d'avancement).

## 3. Runs

| Run | Branche, commit | Résultat |
|---|---|---|
| `fork-apk` 89 | `feat/apk-release`, `a14a7f3` | vert, empreinte conforme |
| `fork-apk` 90, 92, 96, 97 | `feat/carte-telechargement` | verts (96 : 78 tests de l'extracteur dans `jvmTest` et `testAndroidHostTest`) |
| `fork-apk` 91, 93, 94, 95 | idem | annulés par des poussées suivantes (avant la règle du §5) |
| **`fork-apk` 98** | `4f29b8b` (contrôleur, branchement Android, écran) | **L'écran compile en CI** : « Lint », « Compile (Kotlin, fdroid debug) » et « Tests » verts (18 h 36 – 18 h 52 UTC). À la fin de la session, la suite tournait encore (tests `core:demo`, tests `feature:map` avec le contrôleur et le cadre, APK debug, tests `feature:map-maplibre`, APK release) : **lire son résultat final en premier.** |
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
  tests du code commun avec `kotlinc` seul (compilateur 2.4.20 et bibliothèques de Maven Central dans le dossier temporaire
  de la session, à recréer : voir `scripts/carte-interop/build.sh`, même méthode).
- Prévenir l'utilisateur à chaque run `fork-apk` vert ou rouge.
- **Consignes de l'utilisateur (11 novembre 2026)** : plus aucun remplacement automatique sur des fichiers entiers (script
  Python ou `sed`) pour l'écran : seulement des modifications ciblées (outil d'édition) ; `detekt` et `spotless` en local
  **avant chaque commit**. Raison : un remplacement par script a abîmé `MapDownloadPanel.kt` le 11 novembre (fichier non
  commité, réécrit à la main avant le commit `4f29b8b`).
- Une tâche planifiée de vérification (`trig_01PztYWhuaGwvNDpHmsGy9zo`, 19 h 10 UTC le 11 novembre) a été créée pour relire
  le run 98 ; dans une nouvelle conversation elle ne sert plus : la supprimer si elle existe encore.

## 6. Reste à faire (tâche 7)

1. **Lire le résultat final du run 98** : la compilation fdroid de l'écran est verte ; restent à confirmer les tests, l'APK
   debug, les tests `feature:map-maplibre` et l'APK release. Si rouge, corriger par un nouveau commit.
   Fichiers de l'écran : `feature/map-maplibre/.../component/MapDownloadOverlay.kt` (cadre, poignées, emplacement
   `MapDownloadSlot`), `MapDownloadPanel.kt` (panneau), `MapDownloadParts.kt` (textes d'erreur, source, formats) ;
   ouverture par `MapDownloadController.openFrame()` depuis `OfflineMapFilesMenuItem(onDownload = …)`
   (`androidApp/src/fdroid/.../map/GetMapViewProvider.kt`).
2. Commit 12 du plan : liste « Cartes sur le téléphone » enrichie (édition, zone et taille lues dans `<id>.json`).
3. Commit 15 : « Exporter une carte » par le sélecteur de fichiers Android (D12).
4. Test Compose de l'écran (poignées, boutons selon l'état), dans `feature:map-maplibre` (`jvmTest`).
5. Documentation : `FORK.md` §29 (état final), `CARTE-HORS-LIGNE.md` (nouvelle procédure, celle du Mac en secours),
   `TESTS-A-FAIRE.md` §17 (compléter).
6. **Essais sur téléphone** (`TESTS-A-FAIRE.md` §17), dont la comparaison sur Mac avec `cincu-30km.pmtiles` ; l'artefact
   `cartes-cincu` du run `fork-carte-interop` peut déjà être importé pour vérifier le fichier.
7. Fin de tâche : supprimer `wip/carte-telechargement`.

Plus tard (décidé) : cartes raster (tâche distincte, plan §17) ; WorkManager seulement si l'essai le justifie (D6) ;
saisie des dimensions au clavier.

## 7. Points d'attention

- **Rien de l'écran n'a tourné sur un téléphone ni dans un émulateur** : il compile (JVM local, Android par la CI) et sa
  logique est testée, mais l'ergonomie du cadre, la projection écran ↔ carte et la mention de confidentialité sont à voir.
- `build.protomaps.com` est refusé par le proxy de l'environnement Claude Code : toute mesure du vrai serveur passe par un
  workflow de la CI (sonde, interop).
- Les éditions Protomaps ne vivent que 7 jours : une reprise n'est possible que tant que l'édition existe.
- `values-fr/strings.xml` n'est pas trié (le script ne trie que l'anglais) : les textes du fork y sont ajoutés à la fin.
