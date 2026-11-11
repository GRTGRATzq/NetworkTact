# Plan : télécharger une carte hors ligne depuis l'application (tâche 7, `feat/carte-telechargement`)

État : **plan seulement, aucun code**. Rédigé le 11 novembre 2026 ; à valider avant toute implémentation.
Variante fdroid (carte MapLibre) uniquement. Rien n'est émis par radio.

## 1. Objectif et besoins

Remplacer la procédure du Mac (`pmtiles extract`, `CARTE-HORS-LIGNE.md`) par un téléchargement dans l'application :

- choisir la zone (centre + rayon en km, ou rectangle) et le zoom maximal ;
- voir la **taille estimée avant** de télécharger ;
- n'extraire que les tuiles utiles d'une édition Protomaps (`https://build.protomaps.com/AAAAMMJJ.pmtiles`, plus de
  100 Go), par requêtes HTTP `Range`, et écrire un `.pmtiles` valide dans le téléphone ;
- Internet seulement pendant le téléchargement ; plusieurs cartes gardées, ajoutées et supprimées librement ;
- progression, annulation, reprise après coupure si raisonnable ; aucun fichier résiduel en cas d'échec ou d'annulation ;
- mention « © contributeurs OpenStreetMap · Protomaps » ; mode démo : aucun téléchargement.

Cas de référence : 30 km autour de Cincu (45,917 N, 24,800 E), zoom 15, 15 Mo obtenus sur Mac.

## 2. Ce qui existe (lu dans le code)

| Élément | Où | Utile pour |
|---|---|---|
| `OfflineMapLibrary` (liste, import, suppression, `pmtiles/<id>.pmtiles` + `<id>.name`, copie temporaire dans `pmtiles-import/` puis `renameTo`) | `androidApp/src/fdroid/.../map/offline/` | le point d'entrée du téléchargement : même dossier, même liste, même mutex |
| `PmTilesHeader` (lecture et contrôle de l'en-tête v3, `minimumFileSize`, refus du raster) | `feature/map/.../offline/` | contrôle du fichier produit |
| `PmTilesMetadata` (lecture gzip, crédit Protomaps, §27) | idem | crédit automatique si les métadonnées sont copiées de la source |
| `requiredFreeBytes` / `hasRoomForImport` | idem | contrôle d'espace libre |
| `OfflineMapImport` (`Copying`, `Done`, `Cancelled`, `Failed`…) et `OfflineMapFiles` | `feature/map-maplibre/.../component/OfflineMapFiles.kt` | état et écran « Cartes sur le téléphone » |
| Extracteur **google** (`OfflineRegionExtractor`, `PmTilesDailyBuild`) | `androidApp/src/google/.../offline/pmtiles/` | sondage des éditions par `HEAD` ; **une requête par tuile**, plafonné à 2 000 tuiles, écrit du MBTiles |
| `ch.poole.geo.pmtiles:Reader` 0.3.7 | `libs.versions.toml`, utilisé par `feature:map-terrain` et la variante google seulement | voir §3 |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `networkSecurityConfig` | `androidApp/src/main/AndroidManifest.xml` | aucune permission nouvelle (§9) |
| Ktor client (`core:network`, `feature:map`, OkHttp dans `androidApp`), Okio (`GzipSource` déjà utilisé en commonMain) | `libs.versions.toml` | HTTP et gzip sans `java.*` en commonMain |

## 3. Bibliothèque ou extracteur maison ?

Vérifié en lisant les sources de `Reader` 0.3.7 (Maven Central) et la spécification PMTiles v3 :

- `Reader` est un **lecteur de tuiles** : `getTile(z, x, y)` descend dans les répertoires et lit la tuile. Il n'expose ni
  les répertoires ni les entrées, n'a **aucun écrivain**, et ne regroupe pas les lectures : un aller-retour réseau par
  tuile (`HttpUrlConnectionChannel` : une requête `Range` par lecture). Pour Cincu : 6 822 tuiles (zooms 0 à 15, rectangle
  circonscrit) à 100 à 300 ms l'une, soit 11 à 35 minutes en série contre quelques dizaines de requêtes regroupées.
  C'est la limite de 2 000 tuiles du README google, que ce dernier présente lui-même comme à lever par le regroupement.
- Seuls `Hilbert.zxyToIndex` (public) et `VarInt.getVarLong` (lecture) serviraient : une cinquantaine de lignes.
- Licence : le POM de `Reader` déclare **BSD 3-Clause**, alors que les commentaires du dépôt (`libs.versions.toml`,
  README google) écrivent « MIT ». À corriger à part (hors tâche 7), car l'écran des licences en dépend.
- La référence de l'algorithme est `go-pmtiles` (`pmtiles extract`, BSD 3-Clause, spécification dans le domaine public
  ou CC0) : lue dans `pmtiles/extract.go`. Aucune bibliothèque Kotlin/Java trouvée dans le projet ni sur Maven Central
  (consultation limitée à `Reader`) ne fait l'extraction.

**Recommandation : écrire un extracteur maison**, en Kotlin pur, qui suit les étapes de `extract.go`, sans reprendre son
code (les structures sont celles de la spécification). Taille estimée : environ 1 200 à 1 500 lignes de production et
800 lignes de tests, dont environ 60 % de logique pure testable en JVM. Risques propres : un fichier écrit
incorrectement (parades au §11) et la mémoire (parade : plafond de tuiles, §8).

## 4. Algorithme (fidèle à `pmtiles extract`)

1. **En-tête** de la source : un `Range: bytes=0-16383` (127 octets d'en-tête + répertoire racine, que la spécification
   limite à 16 384 octets). Refuser si : version ≠ 3 ; source non « clustered » ; type de tuile ≠ MVT ; compression
   interne autre que gzip ; compression des tuiles inconnue.
2. **Pertinence** : énumérer les tuiles de la zone pour chaque zoom de `minzoom` à `min(maxzoom, zoom max de la source)` ;
   pour un rayon, ne garder que les tuiles dont le carré coupe le cercle (environ 21 % de moins que le rectangle
   circonscrit) ; calculer leur identifiant de Hilbert (spécification §4.1) ; trier. Zone traversant l'antiméridien :
   hors sujet pour NetworkTact, refusée avec un message.
3. **Répertoires** : à partir de la racine, ne lire que les répertoires feuilles dont la plage d'identifiants contient
   une tuile pertinente (requêtes `Range` regroupées), un second niveau seulement s'il existe. Garder les entrées
   pertinentes (avec leur longueur d'exécution `RunLength`).
4. **Réencodage** : nouvelles entrées avec décalages cumulés ; une même tuile référencée plusieurs fois (mer, vide) n'est
   téléchargée et écrite qu'une fois ; liste triée des plages source → destination.
5. **Fusion des plages** : regrouper les plages proches en une requête, avec un excédent toléré (`overfetch`) ; valeurs
   par défaut de la ligne de commande `pmtiles` : 5 % d'excédent, 4 téléchargements en parallèle.
6. **Nouvelle mise en page connue d'avance** : en-tête, répertoire racine, métadonnées (copiées de la source),
   répertoires feuilles, puis tuiles. Les répertoires étant calculés **sans lire de tuile**, la taille finale exacte et la
   position de chaque tuile dans le fichier sont connues avant de télécharger la première (c'est le `--dry-run` de la
   ligne de commande) ; c'est ce qui rend possible l'estimation exacte (§7) et la reprise (§10).
7. **Téléchargement** des plages, chacune écrite à sa place dans un fichier temporaire ; contrôle final ; déplacement.

## 5. Architecture

Trois couches, pour que le maximum soit testé par la CI (le code de `androidApp/src/fdroid` n'est compilé que par elle,
sans test) :

1. **`feature:map` (commonMain), paquet `org.meshtastic.feature.map.offline.extract`** : logique pure, sans réseau ni
   fichier : `TileId` (Hilbert), `TileCover` (rectangle, cercle), `DirectoryCodec` (décodage et encodage, varints,
   gzip par Okio), `RangeMerger`, `ExtractPlan` (entrées pertinentes, réencodage, taille finale, liste de plages),
   `PmTilesHeaderWriter`. Les nombres sont des `Long` ; aucun `java.*`.
2. **`feature:map` (commonMain), E/S abstraites** : interface `RangeSource` (`suspend fun read(offset, length): ByteArray`,
   avec ETag) et son implémentation Ktor ; `ArchiveWriter` sur Okio (`FileSystem`, donc testable avec
   `okio-fakefilesystem`). Orchestrateur `MapDownloader` : sondage de l'édition, plan, estimation, téléchargement en
   parallèle, journal de reprise, contrôle final ; il émet un `Flow<DownloadState>`.
3. **`androidApp/src/fdroid`** : `OfflineMapLibrary` reçoit `startDownload(zone, zoom, nom)`, `cancelDownload()`,
   `resumeDownload()` ; état `OfflineMapDownload` à côté de `OfflineMapImport` ; liaison avec les préférences et le
   stockage privé. **`feature:map-maplibre`** : écran « Télécharger une carte ».

Les paquets existants `PmTilesHeader`/`PmTilesMetadata` sont réutilisés tels quels ; le contrôle d'un fichier téléchargé
passe par les mêmes fonctions que l'import.

## 6. Zone, zoom et interface

- **Écran « Télécharger une carte »**, ouvert depuis « Cartes sur le téléphone » (bouton à côté d'« Importer un fichier »).
- **Centre** : au choix, le centre de la carte affichée, ma position, ou une coordonnée saisie (mêmes formats que la
  tâche 15). **Rayon** en km (curseur 5 à 100, saisie possible). **Zoom maximal** de 12 à 15 (Protomaps s'arrête au
  zoom 15, la carte agrandit au-delà : `CARTE-HORS-LIGNE.md`), chaque valeur affichée avec sa taille.
- **Aperçu** : cercle (rayon) ou rectangle dessiné sur la carte en vue ; la poignée du rectangle à la main est un lot
  ultérieur (commit 14), car elle demande un geste sur la carte MapLibre que rien ne fait aujourd'hui.
- **Nom** : proposé (« Cincu · 30 km · z15 »), modifiable. **Taille** : voir §7. **Bouton « Télécharger »** : désactivé en
  mode démo, sans réseau, ou si l'espace manque.
- **Progression** : octets reçus / total (même présentation que l'import), vitesse, bouton « Annuler » ; écran de la
  liste : la carte en cours apparaît avec sa progression.

## 7. Estimation de taille avant téléchargement

Deux niveaux, pour que le chiffre ne soit jamais présenté comme plus sûr qu'il n'est :

1. **Immédiate, hors ligne** : nombre de tuiles (calculé, exact) × octets moyens par tuile, affichée comme une
   **fourchette** (« environ 10 à 40 Mo »). Nombre de tuiles du cas de référence (rectangle circonscrit, calcul de ce
   plan) : zooms 0 à 12 : 143 ; à 13 : 485 ; à 14 : 1 781 ; à 15 : 6 822 (5 041 au seul zoom 15). 15 Mo pour 6 822 tuiles
   donne **2,2 Ko par tuile** : c'est la campagne roumaine. Une ville est beaucoup plus dense : `CARTE-HORS-LIGNE.md` annonce
   « quelques Mo » pour Draguignan, soit une centaine de tuiles au zoom 15 (calcul sommaire, à mesurer), donc de l'ordre
   de dix fois plus par tuile. **La densité fait varier le résultat d'un facteur 10** : d'où le niveau 2.
2. **Exacte, après « Calculer la taille »** : exécute les étapes 1 à 4 de l'algorithme (en-tête, répertoires, réencodage),
   sans télécharger une seule tuile, et donne la taille finale du fichier à l'octet près (plus l'excédent de fusion).
   Coût réseau : le répertoire racine, quelques répertoires feuilles, probablement quelques centaines de Ko (à mesurer).
   C'est ce chiffre qui commande le contrôle d'espace libre et l'avertissement de réseau mobile.

Le bouton « Télécharger » lance le niveau 2 s'il n'a pas été fait, puis demande confirmation au-dessus d'un seuil
(décision D5). Sur réseau facturé à l'usage (`isActiveNetworkMetered`), avertissement au-delà de 50 Mo.

## 8. Édition Protomaps

- **Source** : `https://build.protomaps.com/AAAAMMJJ.pmtiles`. Il n'existe pas d'alias « dernière » : on **sonde** en
  arrière à partir de la date du jour (UTC) par `HEAD`, comme `PmTilesDailyBuild` (16 jours au plus), et on garde la
  première qui répond 200. La publication quotidienne peut manquer quelques heures : c'est la raison du retour arrière.
- **Mémorisation** : l'édition (date, taille, ETag) est écrite avec la carte (`<id>.json`) et affichée dans la liste
  (« édition du 5 novembre 2026 »).
- **Si l'édition disparaît** (404 ou 403 : durée de conservation des anciennes éditions non documentée à ma
  connaissance, à vérifier) : (1) nouveau sondage vers une date plus récente, sans toucher aux cartes installées ;
  (2) si aucune ne répond : message « Aucune édition disponible » et pas de téléchargement ; (3) **adresse de source
  réglable** (réglages avancés : une URL `.pmtiles` quelconque, par exemple un fichier hébergé par l'équipe), avec les mêmes
  contrôles d'en-tête ; (4) la procédure du Mac reste valable et documentée.
- **Changement en cours de route** : on envoie `If-Range`/`If-Match` avec l'ETag de la première réponse ; une réponse 412 ou
  un ETag différent arrête le téléchargement avec « L'édition a changé pendant le téléchargement », et propose de
  recommencer (le journal est invalidé).
- **Compatibilité de schéma (risque majeur)** : le style embarqué est Protomaps Basemaps **5.7.2** ; une édition plus
  récente peut changer les couches. Les éditions essayées jusqu'ici avec ce style sont celles de la procédure du Mac (`CARTE-HORS-LIGNE.md` cite celle du
  7 octobre 2026 en exemple) ; je ne sais pas si une édition plus récente change le schéma. À faire en premier (commit 1) : lire la clé de version des métadonnées d'une édition réelle,
  et la comparer à celle que le style connaît ; une version inconnue bloque le téléchargement avec un message, au lieu
  de produire une carte vide.
- **Option** : figer la date par défaut dans l'application et la mettre à jour à la main ? Rejetée : elle expirerait.

## 9. Réseau, permissions, confidentialité

- **Aucune permission nouvelle** (`INTERNET` et `ACCESS_NETWORK_STATE` y sont). HTTPS seulement, trafic en clair refusé
  par `network_security_config` (à relire en commit 4). Un seul hôte : `build.protomaps.com` (ou l'hôte de la source réglable).
- **Le serveur voit la zone demandée** : les plages d'octets demandées révèlent, avec l'adresse IP, la région d'intérêt.
  Pour un usage tactique c'est une information à connaître : à écrire dans l'écran (« Le serveur Protomaps peut savoir
  quelle région vous téléchargez ») et dans `FORK.md` ; la parade est la source réglable (miroir de l'équipe). Aucune
  adresse ni position n'est journalisée par l'application ; ni Datadog ni Firebase dans la variante fdroid, à confirmer en
  commit 8.
- **Arrière-plan** : première version dans la portée applicative (comme l'import), écran allumé (`FLAG_KEEP_SCREEN_ON`
  pendant le téléchargement) ; le journal de reprise (§10) couvre une interruption. WorkManager est déjà dans le
  catalogue : passage à une tâche avec notification (`POST_NOTIFICATIONS` déjà demandée) si les fichiers dépassent
  100 Mo (décision D6).
- Délais : connexion 15 s, lecture 30 s ; au plus 4 requêtes simultanées ; trois essais par plage avec attente croissante
  (1, 3, 9 s) sur erreur réseau, 429 et 503.

## 10. Stockage, reprise, nettoyage

- Carte terminée : `pmtiles/<id>.pmtiles` et `<id>.name`, **comme une carte importée** ; ajout d'un `<id>.json`
  facultatif (source, date d'édition, zone, zoom, taille) lu par la liste ; les cartes importées n'en ont pas, rien ne change
  pour elles. La suppression retire les trois fichiers ; `refresh()` supprime les orphelins comme il le fait pour `.name`.
- Téléchargement en cours : dossier **séparé** `pmtiles-download/<id>.part` + `<id>.journal.json` (plan, ETag, plages
  terminées). `OfflineMapLibrary` vide aujourd'hui `pmtiles-import/` au démarrage ; il ne doit **pas** toucher
  `pmtiles-download/` (sinon la reprise est perdue), mais en supprime les journaux de plus de 7 jours (D7).
- Comme la mise en page est connue d'avance (§4, point 6), chaque plage terminée est écrite **à sa place** dans le
  fichier et notée dans le journal : reprendre revient à relire le journal, vérifier l'ETag (une requête `HEAD`) et ne
  télécharger que les plages manquantes. Reprise proposée par un bouton « Reprendre » dans la liste.
- Fin : `fd.sync()`, contrôle (§11), puis `renameTo` dans `pmtiles/` ; la carte téléchargée devient le fond choisi
  (comme après un import). **Annulation ou échec définitif** : `.part` et journal supprimés, message précisant que rien n'est
  conservé ; **coupure réseau** : le fichier partiel est gardé (reprise possible), message explicite.
- Espace libre : `requiredFreeBytes(taille exacte)` avant la première écriture, comme l'import.
- Plafonds : 3 000 000 tuiles par plan (mémoire des entrées) et 500 Mo par carte dans la première version (D4).

## 11. Gestion des erreurs

| Situation | Comportement |
|---|---|
| Pas de réseau / DNS / TLS | message précis, rien écrit |
| Coupure pendant le téléchargement | trois essais par plage, puis pause « reprise possible » |
| 404 / 403 sur l'édition | nouveau sondage, sinon « Aucune édition disponible » |
| 416, ou 200 au lieu de 206 | « Ce serveur ne gère pas les téléchargements partiels » (arrêt) |
| 429 / 503 | attente croissante, puis pause |
| ETag changé, 412 | arrêt, proposer de recommencer |
| Source non « clustered », compression interne ≠ gzip, version ≠ 3, raster | refus avant tout fichier, message |
| Version de schéma inconnue | refus, message |
| Espace insuffisant | refus avant écriture (taille nécessaire et libre affichées) ; disque plein en cours : suppression du `.part` |
| Annulation | tâche arrêtée entre deux plages, fichiers supprimés |
| Mort du processus | journal conservé, « Reprendre » à la réouverture |
| Contrôle final en échec (longueur, en-tête, tuile d'essai illisible) | fichier supprimé, message « Carte incomplète, rien n'a été gardé » |

Contrôle final : `PmTilesHeader.check` sur le fichier, longueur = fin de la section des tuiles, répertoire racine
décodable, trois tuiles prises au hasard décodables (début `1f 8b` pour du gzip) ; **ouverture par MapLibre** à la
première utilisation (essai téléphone, §13).

## 12. Textes (EN / FR)

Clés `offline_map_download_*` dans `values/strings.xml` et `values-fr/strings.xml` (le français est géré par le fork,
§24), triées par `scripts/sort-strings.py`. Liste prévue :

| Clé | EN | FR |
|---|---|---|
| `…_title` | Download a map | Télécharger une carte |
| `…_centre` | Centre | Centre |
| `…_centre_map` / `_gps` / `_coords` | Map centre / My position / Coordinates | Centre de la carte / Ma position / Coordonnées |
| `…_radius` | Radius: %1$s km | Rayon : %1$s km |
| `…_max_zoom` | Maximum zoom | Zoom maximal |
| `…_estimate_range` | About %1$s to %2$s | Environ %1$s à %2$s |
| `…_compute` | Calculate the exact size | Calculer la taille exacte |
| `…_exact` | %1$s (%2$s tiles) | %1$s (%2$s tuiles) |
| `…_start` | Download | Télécharger |
| `…_confirm_mobile` | You are on mobile data: %1$s will be downloaded. | Vous êtes sur données mobiles : %1$s seront téléchargés. |
| `…_privacy` | The Protomaps server can tell which area you download. | Le serveur Protomaps peut savoir quelle zone vous téléchargez. |
| `…_progress` | %1$s of %2$s | %1$s sur %2$s |
| `…_cancel` / `…_resume` | Cancel / Resume | Annuler / Reprendre |
| `…_done` | “%1$s” is ready: choose it in the map style menu. | « %1$s » est prête : choisissez-la dans le menu des fonds de carte. |
| `…_edition` | Edition of %1$s | Édition du %1$s |
| `…_error_offline` | No Internet connection. Nothing was kept. | Pas de connexion Internet. Rien n'a été conservé. |
| `…_error_no_edition` | No map edition is available right now. | Aucune édition de carte n'est disponible pour le moment. |
| `…_error_changed` | The map edition changed during the download. | L'édition de la carte a changé pendant le téléchargement. |
| `…_error_server` | This server does not allow partial downloads. | Ce serveur ne permet pas les téléchargements partiels. |
| `…_error_schema` | This map edition is not compatible with the app yet. | Cette édition de la carte n'est pas encore compatible avec l'application. |
| `…_error_too_big` | The area is too large: reduce the radius or the zoom. | La zone est trop grande : réduisez le rayon ou le zoom. |
| `…_error_paused` | Connection lost. The download can be resumed. | Connexion perdue. Le téléchargement peut être repris. |
| `…_error_incomplete` | The map is incomplete. Nothing was kept. | La carte est incomplète. Rien n'a été conservé. |
| `…_demo` | Downloads are off in demo mode. | Les téléchargements sont désactivés en mode démo. |

Crédit : aucun texte nouveau. Les métadonnées de la source sont copiées telles quelles (`name` = « Protomaps Basemap »),
donc la règle de la tâche 27 impose déjà « © contributeurs OpenStreetMap · Protomaps » ; un test le vérifie.

## 13. Tests et CI

- **Logique pure** (`feature:map`, `jvmTest`, déjà lancé par l'étape « Tests feature:map » de `fork-apk`, aucun
  coût de CI en plus) : identifiants de Hilbert (valeurs de la spécification : zoom 1 → 1 à 4 ; zoom 12, 3423, 1763 →
  19 078 479) ; couverture du rectangle et du cercle (Cincu : 5 041 tuiles au zoom 15 pour le rectangle, calculées
  indépendamment) ; codec de répertoires (aller-retour, exemples de la spécification) ; fusion de plages ; plan sur une
  **archive source synthétique** construite par le test (deux niveaux de répertoires, tuiles dupliquées, tuiles
  absentes) ; écriture, longueur et contrôle final avec `okio-fakefilesystem`.
- **Réseau** : `ktor-client-mock` (déjà au catalogue) : 206 normal, 200 au lieu de 206, ETag changé, 429/503 puis succès,
  coupure puis reprise par journal, annulation sans fichier résiduel, erreur disque.
- **Interopérabilité** (le point où un écrivain maison peut se tromper sans que rien ne le dise) : workflow **manuel**
  `fork-carte-interop.yml` (`workflow_dispatch` seulement, pour ne pas ajouter aux 27 minutes de `fork-apk`) : un test JVM écrit
  un `.pmtiles` à partir d'une source synthétique, puis `pmtiles verify` et `pmtiles show` (go-pmtiles, BSD-3, binaire de la
  version publiée) le valident. Le même workflow peut, en option, sonder `build.protomaps.com` depuis l'exécuteur (en-tête,
  répertoires, métadonnées d'une vraie édition, mesure du coût de l'estimation exacte) : **ce que je n'ai pas pu faire d'ici**
  (§15).
- **Android** (`androidApp/src/fdroid`, compilé par la CI seulement, comme `OfflineMapLibrary`) : le moins de logique
  possible ; l'orchestrateur est testé en `feature:map`.
- Lint : `spotlessCheck` et `detekt` en local avant chaque poussée ; `python3 scripts/sort-strings.py` après les textes.

## 14. Découpage en commits (après validation, un sujet chacun, messages en français)

1. Édition réelle : sonde manuelle (workflow `workflow_dispatch`) de l'en-tête et des métadonnées d'une édition ; consigne
   la clé de version du schéma et la compression (**avant tout autre code**, voir D1).
2. Identifiants de Hilbert et couverture d'une zone (rectangle, cercle) + tests.
3. Codec de répertoires PMTiles v3 et écrivain d'en-tête + tests.
4. Fusion de plages et plan d'extraction (entrées pertinentes, réencodage, taille finale) + tests sur archive synthétique.
5. Source HTTP par plages (Ktor, ETag, essais, délais) + tests `MockEngine`.
6. Écriture du fichier, journal de reprise, contrôle final + tests.
7. Sondage de l'édition Protomaps, mémorisation, source réglable + tests.
8. Estimation de taille (fourchette et exacte) + tests.
9. `OfflineMapLibrary` : téléchargement, annulation, reprise, nettoyage, `<id>.json` (fdroid).
10. Écran « Télécharger une carte » (centre, rayon, zoom, taille, progression), textes EN/FR, mode démo.
11. Liste « Cartes sur le téléphone » : informations d'édition, « Reprendre », cartes téléchargées.
12. Workflow d'interopérabilité manuel (`pmtiles verify`).
13. Documentation : `FORK.md` §29, `CARTE-HORS-LIGNE.md` (nouvelle procédure, celle du Mac gardée en secours),
    `TESTS-A-FAIRE.md` §17.
14. (lot suivant) rectangle tracé à la main sur la carte.

Chaque commit est vérifié avant la poussée (lint local, tests du module) ; la CI `fork-apk` fait foi pour la
compilation fdroid. Branche `feat/carte-telechargement`, partie de `feat/apk-release`, aucune pull request.

## 15. Ce que je n'ai pas pu vérifier

- **`build.protomaps.com` est refusé depuis mon environnement** (le proxy répond 403 au tunnel HTTPS ; `maps.protomaps.com`
  de même). Je n'ai donc ni lu l'en-tête d'une édition réelle, ni vérifié que le serveur accepte les `Range` multiples,
  répond 206, fournit un ETag, ni mesuré la taille des répertoires ou la durée. Je n'ai pas cherché à contourner ce refus.
  Ces points sont des **hypothèses** tirées de la spécification, de `go-pmtiles` et du code google existant. Le commit 1
  les remplace par des mesures, lancées depuis la CI ou le téléphone.
- GitHub (spécification, `go-pmtiles`) et Maven Central étaient accessibles : lus.
- Pas d'accès à un téléphone : aucune ouverture dans MapLibre du fichier produit.

## 16. Essais sur téléphone (futur `TESTS-A-FAIRE.md` §17)

- Cincu, 30 km, zoom 15 : taille annoncée avant, taille réelle après ; durée ; la carte s'affiche, mode avion compris.
- Comparaison avec le fichier de 15 Mo fait sur le Mac : même rendu aux mêmes zooms, mêmes libellés.
- Mention ⓘ : « © contributeurs OpenStreetMap · Protomaps ».
- Annulation en cours : aucun fichier résiduel (`Cartes sur le téléphone`, espace libre inchangé).
- Coupure (mode avion) en cours, puis reprise : le fichier final est complet.
- Fermer l'application pendant le téléchargement, la rouvrir : « Reprendre ».
- Espace presque plein : refus avec les tailles.
- Deux cartes de zones différentes, choix de l'une puis de l'autre, suppression d'une.
- Mode démo : bouton désactivé, rien téléchargé. Données mobiles : avertissement.
- Un ancien fichier importé du Mac continue de fonctionner à côté.

## 17. Cartes raster, plus tard (prévu, pas fait)

Aujourd'hui `PmTilesCheck.NotVectorTiles` refuse tout `.pmtiles` raster. Le plan d'extraction ci-dessus ne dépend pas du
type de tuile (il recopie des octets) ; l'import d'un raster (GeoTIFF converti sur le Mac avec GDAL puis `pmtiles`)
demanderait : accepter les types PNG, JPEG, WebP (2, 3, 4) à l'import ; un style raster minimal (une source `raster`, sans
polices ni sprites, taille de tuile lue dans les métadonnées) ; un fond `Basemap.LocalRaster` ; le crédit pris dans les
métadonnées du fichier (`importedCredit` le fait déjà pour un fichier non Protomaps) ; une mention dans `CARTE-HORS-LIGNE.md`
sur la production avec GDAL. Une tâche à part, après celle-ci.

## 18. Risques

1. **Schéma de tuiles** d'une édition plus récente que le style 5.7.2 : carte vide ou partielle (parade : contrôle de version).
2. **Écrivain incorrect** : fichier que MapLibre ouvre mal (parades : tests, `pmtiles verify` en CI, essai téléphone).
3. **Serveur** : suppression des anciennes éditions, limitation de débit, refus de `Range` sur certains chemins réseau
   (proxys d'entreprise) : sondage, source réglable, messages.
4. **Terme d'usage** : `build.protomaps.com` n'a pas de condition d'usage documentée distincte (README google). Un volume
   important d'utilisateurs réclamerait un hébergement propre ; à surveiller.
5. **Confidentialité** de la zone demandée (§9).
6. **Mémoire** pour de grandes zones (parade : plafonds) ; **autonomie** (écran allumé).
7. **Licence** : `Reader` déclaré BSD-3 mais décrit MIT dans le dépôt (non utilisé ici) ; l'extracteur maison suit une
   spécification dans le domaine public ; la mention BSD-3 de `go-pmtiles` n'est requise que si du code en est repris, ce que le
   plan évite.

## 19. Questions ouvertes

- Taille réelle des répertoires nécessaires à l'estimation exacte (à mesurer, commit 1).
- Valeur exacte du paramètre de version du schéma dans les métadonnées d'une édition réelle (commit 1).
- Durée de conservation des anciennes éditions sur `build.protomaps.com`.
- Faut-il aussi remplacer l'extracteur google (MBTiles, 2 000 tuiles) par celui-ci ? Hors sujet : NetworkTact est publié
  sur Android fdroid.

## 20. Décisions à prendre

- **D1** : accepter qu'un premier commit (sonde manuelle depuis la CI, `workflow_dispatch`) mesure une vraie édition, puisque je ne
  peux pas joindre `build.protomaps.com` ?
- **D2** : extracteur maison (recommandé) ou rester sur `Reader` (une requête par tuile, plafond 2 000, sans écrivain) ?
- **D3** : choix de la zone dès la première version : centre (carte, position, coordonnées) + rayon (recommandé) ; rectangle
  tracé à la main plus tard (commit 14) ?
- **D4** : plafonds : 3 000 000 tuiles et 500 Mo par carte ; zoom maximal proposé de 12 à 15 ?
- **D5** : confirmation avant téléchargement à partir de quelle taille (proposé : 50 Mo, toujours sur réseau mobile) ?
- **D6** : téléchargement en portée applicative écran allumé (recommandé pour 15 Mo) ou tout de suite WorkManager avec notification ?
- **D7** : durée de conservation d'un téléchargement interrompu avant suppression (proposé : 7 jours) ?
- **D8** : source réglable par l'utilisateur (URL d'un `.pmtiles`) dès la première version, comme parade à la disparition
  d'une édition et à la confidentialité de la zone ?
- **D9** : crédit : se contenter de la règle de la tâche 27 (recommandé) ?
- **D10** : le workflow d'interopérabilité manuel (go-pmtiles `verify`, binaire téléchargé sur l'exécuteur) est-il acceptable,
  ou préférez-vous le faire vous-même sur le Mac ?
- **D11** : raster (§17) : tâche 8 distincte après celle-ci ?
