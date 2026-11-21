# Revue du code ajouté par NetworkTact (point 6)

Revue en lecture seule de `test/correctifs-13-11-d` (`b2705c4`), le 21 novembre 2026. **Rien n'est supprimé ni modifié** :
ce document propose, l'utilisateur décide.

## 0. Périmètre et méthode

- **Code revu** : uniquement ce que le fork a ajouté ou changé depuis le dernier commit amont, `64cd7e26`
  (James Rich, 26 septembre 2026, « feat(store): capture the store screenshots… »). Aucun commit amont après lui sur la
  branche. `git diff 64cd7e26 HEAD` : 619 fichiers, environ 530 fichiers de code ou de ressources hors traductions.
- **Ce qui a été fait** (sans SDK Android, sans Gradle) :
  1. Recherche mécanique des déclarations ajoutées (`fun`, `class`, `object`, `val`, `const`…) dans le diff, puis comptage
     de leurs références dans tout le dépôt (code de production et tests séparés). Script jetable dans l'espace de travail
     de la session, hors du dépôt. 986 déclarations relevées ; 47 sans aucune référence, 121 référencées seulement par des
     tests. Chaque cas retenu ci-dessous a été relu à la main.
  2. Chaînes ajoutées dans `values/strings.xml` (557) comparées aux `Res.string.*` / `Res.plurals.*` du code Kotlin.
  3. Dépendances ajoutées (`build.gradle.kts`, `libs.versions.toml`) comparées aux `import` des modules.
  4. Recherche dans le diff de `TODO`, `FIXME`, `@Ignore`, `@Deprecated`, drapeaux `DEMO`/`demo = true` en dur, `println`.
  5. Relecture des workflows `fork-*` et `ios-check`, et des documents du fork.
- **Ce que seule la CI peut vérifier** : que chaque suppression proposée compile (Android et JVM), que detekt et spotless
  restent verts, que le graphe Koin est complet (les classes `@Single` et les fonctions de module Koin ne sont jamais
  appelées par leur nom : elles ressortent à tort comme « sans référence » et ne sont pas proposées), et les tests.
- **Faux positifs écartés** : les 26 fonctions de `CoreDemoModule`, `DiagnosticDemoModule`, `FireRequestDemoModule` et
  `ScreenDataModule` (Koin) ; `AndroidPhonePositionSource` (`@Single`) ; les classes `*PrefsImpl` (`@Single`) ; les
  `Fake*` de `core:testing` (servent aux tests, c'est leur rôle) ; les constantes utilisées dans leur propre fichier.

## 1. Synthèse

| # | Proposition | Classe | Taille |
|---|---|---|---|
| A1 | Lancer les tests de `core:ui` et `core:network` dans `fork-apk` (tests du cadre ami jamais exécutés) | à vérifier | +2 lignes |
| A2 | Mettre à jour cinq documents périmés ou faux | sûr | ~30 lignes de texte |
| A3 | Retirer la chaîne `firereq_reply_sent` et la constante `PROGRESS_DP` | sûr | −4 lignes |
| A4 | Retirer la sonde et l'essai d'interopérabilité de la carte, une fois leur usage terminé | à vérifier | −980 lignes |
| A5 | Fusionner les deux lecteurs d'en-tête PMTiles | à vérifier | −80 à −120 lignes |
| — | **Alerte 50 radios** : l'organigramme est plafonné à 40 membres et le QR à 9 membres longs | à ne pas toucher (décision) | — |

Le reste : § 2 (sûr), § 3 (à vérifier), § 4 (à ne pas toucher), § 5 (50 radios), § 6 (fragilités des points 1 et 4).

## 2. Sûr

### S1. Chaîne `firereq_reply_sent` jamais utilisée

- `core/resources/src/commonMain/composeResources/values/strings.xml:804` (`%1$s sent`) et
  `values-fr/strings.xml:1841` (`%1$s envoyé`) ; entrée `.skills/compose-ui/strings-index.txt:774`.
- **Pourquoi c'est sûr** : aucune référence `Res.string.firereq_reply_sent` dans le code ; `git log -S` sur les `.kt`
  ne trouve aucun commit où elle ait été utilisée (ajoutée avec `26596b63`, carte de l'émetteur de la demande de tir v1).
  Aucune autre langue ne la porte. C'est la **seule** des 557 chaînes ajoutées par le fork sans usage.
- **Risque** : nul (l'accesseur généré disparaît, personne ne l'appelle). CI : compilation.
- **Taille** : 2 lignes de XML, plus l'index (régénéré par `python3 scripts/sort-strings.py`).

### S2. Constante `PROGRESS_DP` inutilisée

- `feature/map-maplibre/src/commonMain/kotlin/org/meshtastic/feature/map/maplibre/component/MapDownloadPanel.kt:362`
  (`private const val PROGRESS_DP = 20`).
- **Pourquoi c'est sûr** : privée au fichier, aucune occurrence ailleurs dans le fichier ; restée depuis `4f29b8b3`
  (écran « Télécharger une carte »).
- **Risque** : nul. Curieux que detekt (`UnusedPrivateProperty` actif dans `config/detekt/detekt.yml:818`) ne la signale
  pas : la CI dira si la règle couvre les constantes de premier niveau.
- **Taille** : 1 ligne.

### S3. Documents périmés ou faux (aucun code)

| Fichier, ligne | Problème |
|---|---|
| `TESTS-A-FAIRE.md:281-282` (§ 18) | « Ouvert depuis un canal : destinataire à choisir » : contredit la décision 1 du point 1 (plus de choix dans le formulaire, « Envoyer à » dans l'aperçu ; § 23, ligne 405, le dit). Un testeur suivant le § 18 cherchera un bouton qui n'existe plus. |
| `docs/PLAN-CARTE-TELECHARGEMENT.md:3` | « État : plan seulement, aucun code d'application » : le code existe (`feature/map/.../offline/extract/`, FORK.md § 29). |
| `FORK.md` § 22 (ligne ~741) | Affirme que le run 79 a passé `NoInheritedDemoTransportTest`. `fork-apk.yml` n'a **jamais** lancé `:core:network` (`git log -S":core:network" -- .github/workflows/fork-apk.yml` : aucun commit). Ce test n'a donc probablement jamais tourné en CI (voir A1). |
| `FORK.md:4`, `TESTS-A-FAIRE.md:3`, `HISTORIQUE.md:3` | Dates et branches de référence de début novembre (`feat/apk-release`, `feat/accueil-insignes`, run 72). |
| `FORK.md:1264-1308` | « État d'avancement au 11 novembre 2026 » : remplacé de fait par le § 000 de `PASSATION-CLAUDE-CODE.md`. Proposition : un renvoi, ou le titre « (historique) ». |
| `PASSATION-CLAUDE-CODE.md` § 00, § 0.1 à 0.3 | Empilés sous le § 000, chacun « remplacé là où ils diffèrent ». À déplacer dans `HISTORIQUE.md` pour qu'une session neuve lise moins. |
| `TESTS-A-FAIRE.md` | Numérotation : § 18 puis § 21 (pas de § 19 ni § 20). |

**Risque** : nul pour l'application. **Taille** : une trentaine de lignes de texte.

### S4. Déclencheurs de workflow sur une branche terminée

- `.github/workflows/fork-carte-sonde.yml` et `fork-carte-interop.yml` (`on.push.branches: ["feat/carte-telechargement"]`).
- La tâche 7 est fusionnée plus loin ; ces déclencheurs ne partent plus jamais. Le lancement manuel reste.
- **Risque** : nul. **Taille** : 2 × 4 lignes. Faible intérêt ; à faire seulement avec A4 ou à la place d'A4.

## 3. À vérifier

### A1. Tests du fork que la CI ne lance pas (le plus utile)

- `fork-apk.yml:86-115` lance `feature:messaging`, `feature:node`, `feature:settings`, `core:model`, `core:prefs`,
  `core:service`, `core:demo`, `feature:map`, `feature:map-maplibre`. Il ne lance **pas** :
  - `core:ui` : `FriendlyUnitFrameTest.kt` (`FriendlyFrameLabelTest`, `DashRunsTest`, **point 4**), `AnnotatedStringsTest`,
    `ConnectionsViewModelTest` (modifié par le fork) ;
  - `core:network` : `NoInheritedDemoTransportTest` (§ 22, la porte de l'ancien mode démo) ;
  - `core:navigation` : `TabsResetOnSwitchTest`, `NavigationConfigTest` (exclu volontairement : il a déjà fait tomber
    le runner, `fork-apk.yml:96`).
- FORK.md § 33 cite `FriendlyFrameLabelTest` et `DashRunsTest` comme tests du cadre ami : ils compilent avec le module
  mais ne s'exécutent pas.
- **Proposition** : ajouter `:core:ui:jvmTest :core:network:jvmTest` à une étape existante (ou une étape à eux, avec
  `./gradlew --stop` avant, comme les autres). Pas `core:navigation`.
- **Risque** : mémoire du runner (les runs 28, 33, 37, 38 ont gelé) ; quelques minutes de CI en plus ; un test
  jamais exécuté peut échouer au premier passage (c'est le but). **Taille** : 1 à 2 lignes de YAML.

### A4. Sonde et essai d'interopérabilité de la carte

- `scripts/carte-sonde/` (`sonde.py` 483 lignes, `test_sonde.py` 161), `scripts/carte-interop/` (`ExtractCli.kt` 129,
  `compare.py` 61, `build.sh` 25), workflows `fork-carte-sonde.yml` (46) et `fork-carte-interop.yml` (79), et les
  lignes `fork-apk.yml:12-13` qui les excluent.
- **Ce qui les appelle** : rien dans l'application. `compare.py` importe `sonde.py` (ligne 9-10) : les deux vont
  ensemble. `TileCoverTest.kt:27` ne cite la sonde qu'en commentaire. Le résultat de la sonde est consigné
  (`PmTilesCompression.kt` : « probe, 11 November 2026 » ; FORK.md § 29).
- **Risque** : on perd le seul essai de l'extracteur contre la **vraie** édition Protomaps (`fork-carte-interop`). Si
  Protomaps change son format, plus rien ne le verra avant un téléphone. À garder si l'utilisateur relance cet essai ;
  sinon, à retirer, l'historique Git le garde.
- **Taille** : environ −980 lignes, 9 fichiers.

### A5. Deux lecteurs de l'en-tête PMTiles

- `feature/map/src/commonMain/kotlin/org/meshtastic/feature/map/offline/PmTilesHeader.kt` (135 lignes, contrôle à
  l'import d'un fichier) et `.../offline/extract/PmTilesArchiveHeader.kt` (169 lignes, téléchargement). Même en-tête de
  127 octets, même constante `MAGIC = "PMTiles"`. Le second le dit lui-même (ligne 21-23) : le premier « reste le
  contrôle léger de l'import ».
- **Proposition** : que l'import lise `PmTilesArchiveHeader` et garde seulement ses vérifications (type de tuile,
  taille minimale du fichier, limites). `PmTilesHeaderTest` (112 lignes) se replie sur les tests de l'autre.
- **Ce qui l'appelle** : l'import fdroid (`androidApp/src/fdroid/.../offline/PmTilesCopy.kt`, `OfflineMapLibrary.kt`)
  et `extract/ArchiveCheck.kt` (contrôle d'une carte téléchargée). Vérifié par recherche, pas compilé.
- **Risque** : moyen. L'import d'un vrai `.pmtiles` n'a jamais tourné sur téléphone (FORK.md « Pas encore vérifié ») ;
  un écart de lecture refuserait des cartes valides. Les tests de `feature:map` tournent en CI, pas ceux de l'app
  fdroid. **Taille** : −80 à −120 lignes.

### V1. Code de production qui ne sert qu'aux tests

- `core/model/src/commonMain/kotlin/org/meshtastic/core/model/NodeColors.kt:70` `nodePalette` (« for tests ») : 3 lignes.
- `PmTilesCompression.BROTLI` / `ZSTD` (`PmTilesCompression.kt:30-31`) : codes de la spécification, servent de
  documentation et aux tests de refus. À garder.
- `MessageCardPreviews.kt` (173 lignes, `commonMain`) : données d'aperçu réutilisées par `MessageItemTest`.
- **Avis** : rien à gagner, ne pas toucher sauf nettoyage général.

### V2. Coût de structure du mode démo

- Chaque méthode ajoutée à `PacketRepository` (par ex. `getDirectMessagesStartingWith`, point 1) demande quatre
  implémentations : `PacketRepositoryImpl`, `DemoPacketRepository`, `ScreenPacketRepository`, plus le faux de test. C'est
  voulu (FORK.md § 9 : façades qui basculent en direct) et testé (`ScreenFacadesTest`). **Pas de proposition**, mais à
  savoir avant d'ajouter des fonctions pour 50 radios.

## 4. À ne pas toucher

| Élément | Raison |
|---|---|
| `core/ui/.../viewmodel/ConnectionsViewModel.kt:349` `FIRMWARE_UPDATE_NOTICES_ENABLED = false` | Coupe volontairement l'avis de mise à jour du firmware ; retirer le code amont derrière compliquerait les reprises de Meshtastic. |
| Porte `mockTransportEnabled` (`BaseRadioTransportFactory`), chaînes `demo_mode*` | Code amont gardé fermé (§ 22). Même raison. |
| `OrgChartCode.isOrgChartCode` (`OrgChartCode.kt:61`), utilisé seulement par des tests | Organigramme (`NTORGA1`, `NTORGA3`). |
| Les quatre limites de 200 octets : `MessageScreenComponents.kt:737`, `TeamRoster.kt:44`, `FireRequestSplitter.kt:31`, `PositionMessage.kt:49` | Doublon réel, mais les unifier touche les modules de `[EQUIPES]`, `[DT]`, `[POS]`. |
| ~20 fonctions `*Preview` sans appelant | Aperçus Compose : leur rôle est d'être sans appelant. |
| `.github/workflows/ios-check.yml` | Seul garde-fou des cibles iOS, manuel et hebdomadaire, ne bloque rien. |
| `SendChannels.isFireTeam` (`FireRequestTarget.kt:90`) | Reconnaissance tolérante voulue. |
| Dépendances ajoutées | Toutes utilisées : `core:demo` importe bien `navigation3`, `paging`, `core.domain`, `core.navigation` ; `okio` (fdroid) et `ktor-client-mock`, `okio-fakefilesystem` (tests de `feature:map`) servent. |

Aucun `TODO`, `FIXME`, `@Ignore`, `@Deprecated` ni drapeau de démo forcé dans le code ajouté. `MAP_AVAILABLE_IN_DEMO`
vaut `true` en fdroid et `false` en google : voulu (FORK.md § 9).

## 5. Ce qui compliquerait le passage à 50 radios

1. **Organigramme plafonné** (bloquant). `core/model/.../orgchart/OrgChart.kt:74` `MAX_MEMBERS = 40` et
   `OrgChartCode.kt:54` `MAX_CODE_CHARS = 770` : le commentaire dit que **neuf** membres aux indicatifs et rôles les plus
   longs tiennent dans le QR. Avec 50 radios, l'organigramme ne passe ni dans la limite, ni dans un QR lisible. Toute
   solution (plusieurs QR, texte compressé, envoi radio) touche `NTORGA1`/`NTORGA3` : **décision de l'utilisateur**.
2. **Noms courts de 4 caractères** : à 50 radios, les doublons deviennent probables. Ils sont l'identité affichée du
   cadre ami (FORK.md § 33 : « deux radios de même nom partagent la même image » sur la carte, plus de couleur par
   nœud), des lignes « Accusé DT : <nom court> », et le repli de l'attribution
   (`FireRequestAllocation.kt:124`, `firstOrNull { it.shortName == named }`).
3. **Menu « Tir attribué à »** : `MessageListPaged.kt:155-160` prend **tous** les nœuds connus ayant un id, triés par
   nom, sans filtre (hors ligne, MQTT, probablement ce téléphone lui-même : à vérifier) ni recherche. 50 radios plus les
   nœuds de passage font un très long `DropdownMenu`.
4. **Chargement de toute la conversation pour les DT** : `MessageViewModel.kt:261-287`. `getMessagesFrom(key)` est appelé
   sans limite (l'amont passe un `limit`), et sur un canal ce même flux est collecté **deux fois** (une fois dans
   `combine`, une fois pour calculer `firstPartTime`) : deux requêtes Room identiques ouvertes, tout l'historique du canal
   en mémoire à chaque nouveau message. Sans effet à 9 radios, sensible sur un canal chargé à 50.
5. **Temps d'antenne** : sur un canal, chaque poste peut envoyer « Accusé DT » en direct à l'émetteur (≈ 40 octets) ; à
   50 postes, c'est une rafale de messages directs vers une seule radio, chacun avec ses réessais. « Les deux » double
   tout. À mesurer aux essais, pas à simplifier.
6. **Calculs par carte** : `fireRequestNetwork(...)` et `directRecipientName(...)` (deux fois) sont recalculés pour
   chaque carte DT à chaque recomposition (`MessageListPaged.kt:451-454`), avec un tri de tous les nœuds. Bon marché à
   50, mais à remonter dans l'état le jour où la liste grossit.
7. Requête `getDirectMessagesStartingWith` (`PacketDao.kt:208-224`) : `substr` sans index, bornée par `received_time`
   (index existant, `Packet.kt:122`) et 7 jours au plus. La mesure prévue (`TESTS-A-FAIRE.md` § 23) suffit.

## 6. Fragilités des points 1 et 4 (jamais tournés sur téléphone : signalées, pas de simplification proposée)

- **Texte anglais avec un libellé français** : `values/strings.xml:818` `firereq_target_form_note` dit « under « Envoyer
  à » » alors que le titre anglais est « Send to » (`firereq_target_title`).
- **Texte en contradiction avec la décision 2** : `firereq_resend_to_note` (EN 810, FR 2081) dit « pour une radio absente
  de ce canal », alors que « Renvoyer la fiche à <radio> » est désormais toujours affiché chez le chef.
- **Tests du cadre ami non exécutés en CI** (point 4) : voir A1.
- **Attribution relue par nom court** : si le message d'attribution ne retrouve pas la radio par son id
  (`message.to`), le repli par nom court peut désigner la mauvaise radio en cas de doublon (§ 5.2).
- **Reconnaissance du canal Équipe TIR** (comportement voulu, seulement deux angles morts) : `isFireTeam` ne remplace que
  `É È Ê Ë` ; un nom écrit en Unicode décomposé (`E` + accent combinant, possible depuis un autre client) ou avec un
  tiret (`EQUIPE-TIR`) ne serait pas reconnu, et le repli irait sur Principal sans erreur visible autre que
  `firereq_target_no_fire_team`. « Équipe TIR » fait exactement 11 octets en UTF-8 : la limite d'un nom de canal
  Meshtastic, sans marge.
- **Menu déroulant du chef non testé en interface** (connu, PASSATION § 000) : seuls les messages sont testés
  (`FireRequestAllocationTest`).

## 7. Ce qui reste à la CI ou au téléphone

- CI : compilation, detekt et spotless après S1, S2, A4, A5 ; premier passage des tests `core:ui` et `core:network`
  (A1) ; graphe Koin inchangé.
- Téléphone : A5 (import d'un vrai `.pmtiles`), § 5.3 à 5.5 (menu, mémoire, temps d'antenne), § 6.
