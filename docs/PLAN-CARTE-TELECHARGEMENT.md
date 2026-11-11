# Plan : télécharger une carte hors ligne depuis l'application (tâche 7, `feat/carte-telechargement`)

État : **plan seulement, aucun code d'application**. Rédigé le 11 novembre 2026, **mis à jour le même jour avec les décisions D1 à D11
validées** (voir §20 : zone par rectangle, confirmation à 50 Mo quelle que soit la connexion, source réglable et mention de
confidentialité dès la v1, zoom maximal 15 par défaut, portée applicative). La sonde de l'édition (D1) passe avant l'extracteur ;
ses résultats seront ajoutés au §15.
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
- **Licence** : le POM de `Reader` déclare **BSD 3-Clause** ; les commentaires du dépôt (`libs.versions.toml`, README de la
  variante google, commentaire de `TerrainTileFetcher`) écrivent « MIT ». Les deux licences sont compatibles avec la
  GPL-3.0-or-later du projet : aucun obstacle. **Écran des licences** (vérifié dans le dépôt) : il est produit par le greffon
  AboutLibraries à partir des POM des dépendances, sans surcharge manuelle pour `pmtiles-reader`
  (`config/aboutlibraries/libraries/` ne contient que OpenStreetMap, Noto Sans et Protomaps) ; il affiche donc ce que dit le
  POM, **BSD 3-Clause** (le texte `config/aboutlibraries/licenses/BSD-3-Clause.json` existe, en modèle générique). Le mot
  « MIT » n'est que dans des commentaires de code, jamais affiché. `Reader` est bien dans l'APK fdroid
  (`feature:map-maplibre` → `feature:map-terrain` → `pmtiles-reader`). Non vérifié : l'affichage réel de l'écran (le greffon ne
  tourne pas en local, faute de SDK) ; ajouté à l'essai sur téléphone (§16). **Correction des commentaires « MIT » :
  commit séparé et minuscule (§14, commit 1).** Si l'extracteur maison reprend un jour du code de `go-pmtiles` (BSD-3-Clause),
  il faudra y ajouter la mention ; le plan l'évite (la spécification est dans le domaine public).
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

## 6. Zone, zoom et interface (rectangle dès la v1)

- **Écran « Télécharger une carte »**, ouvert depuis « Cartes sur le téléphone » (bouton à côté d'« Importer un fichier »).
  Ne pas confondre avec la section « Téléchargements hors ligne » du menu des couches (`OfflineMapsSection`), qui remplit le
  cache natif de MapLibre à partir d'un fond en ligne et ne produit pas de `.pmtiles` : elle reste telle quelle.
- **Le rectangle** part de la **zone visible de la carte** (avec une marge de 10 %) et se redimensionne **en tirant ses quatre
  coins**. Le cadre est dessiné par Compose au-dessus de la carte, en coordonnées d'écran (poignées de 48 dp qui reçoivent
  les gestes : la carte dessous ne bouge pas pendant qu'on tire une poignée) ; on le positionne en déplaçant et en zoomant la
  carte, qui reste manipulable. Les bornes géographiques se déduisent des limites visibles de la carte (`target.bounds()`
  existe déjà), par interpolation en projection Mercator. Pendant l'édition, la carte est remise au nord et à plat (rotation
  et inclinaison à zéro), sans quoi la zone visible n'est plus un rectangle.
- **Dimensions affichées en direct** : largeur et hauteur en km (largeur prise à la latitude du centre), surface, nombre de
  tuiles et taille (§7) ; si le cadre est inversé ou trop petit (moins de 200 m), message et bouton « Télécharger » désactivé.
- **Repli sans geste précis** (accessibilité, TalkBack, clavier) : bouton « Utiliser la vue » (la zone visible entière, comme
  `BoxAuthoringBar`) et champs de dimensions à saisir si le temps le permet (lot ultérieur).
- **Zoom maximal** : **15 par défaut**, réglable de 12 à 15 (Protomaps s'arrête au zoom 15, la carte agrandit au-delà :
  `CARTE-HORS-LIGNE.md`) ; chaque valeur affiche sa taille.
- **Nom** : proposé (« Carte 45,917 N 24,800 E · 60 × 45 km · z15 »), modifiable. **Source** : une ligne toujours visible,
  « Source : build.protomaps.com · Modifier » (§9). **Bouton « Télécharger »** : désactivé en mode démo, sans réseau, si
  l'espace manque ou si le cadre est invalide.
- **Progression** : octets reçus / total (même présentation que l'import), vitesse, bouton « Annuler » ; la carte en cours
  apparaît aussi dans la liste.

### Chiffrage de l'écart : rectangle à poignées contre « deux touchers »

Demandé : si le rectangle est nettement plus coûteux ou risqué, le dire et le chiffrer. Estimation (aucune ligne écrite) :

| | A. deux touchers sur les coins opposés + « Utiliser la vue » (modèle du pavé géographique existant) | B. cadre à quatre poignées (retenu) |
|---|---|---|
| Code d'interface (`feature:map-maplibre`) | environ 150 à 200 lignes, 1 commit | environ 450 à 600 lignes, 3 commits |
| Dont logique pure testée en JVM (carte → bornes, dimensions en km, validité) | environ 60 lignes | environ 120 lignes |
| Tests | tests de logique ; Compose UI sans geste | tests de logique + test Compose des poignées (Robolectric, dans `feature:map-maplibre`) |
| Risques | faible : gestes existants | gestes (poignées au-dessus de la carte), décalage du cadre par rapport à la carte si le conteneur a des marges (le cadre se mesure sur le composable de la carte, pas sur l'écran), carte inclinée ou tournée, précision au doigt ; aucun essai possible en local (compilation et tests par la CI, geste par le téléphone) |
| Essais téléphone | un tour | un à deux tours de réglage |

Écart : environ 2,5 à 3 fois le travail d'interface, un à deux tours d'essai en plus. Ce n'est ni bloquant ni risqué pour le
reste du plan : la logique de téléchargement ne dépend que d'un rectangle `west, south, east, north`. **Recommandation : B**, avec
« Utiliser la vue » conservé comme repli. Si les essais montrent un cadre trop difficile à régler au doigt, on retombe sur A sans
toucher à l'extracteur.

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
   Coût réseau : le répertoire racine, quelques répertoires feuilles, probablement quelques centaines de Ko (**à mesurer par la sonde**, §15).
   C'est ce chiffre qui commande le contrôle d'espace libre et l'avertissement de réseau mobile.

Le bouton « Télécharger » lance le niveau 2 s'il n'a pas été fait, puis **demande confirmation si la taille dépasse 50 Mo**
(« Ce téléchargement fera environ X. Continuer ? »). **La règle est la même sur Wi-Fi et sur données mobiles** : le
téléchargement est permis sur les deux, l'application ne distingue pas le type de connexion et n'en lit pas l'état
(`ACCESS_NETWORK_STATE` n'est pas utilisé par cette tâche). Sous 50 Mo, il démarre sans confirmation.

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
  7 octobre 2026 en exemple) ; je ne sais pas si une édition plus récente change le schéma. Fait en premier, par la sonde (§15) : lire la clé de version des métadonnées d'une édition réelle,
  et la comparer à celle que le style connaît ; une version inconnue bloque le téléchargement avec un message, au lieu
  de produire une carte vide.
- **Option** : figer la date par défaut dans l'application et la mettre à jour à la main ? Rejetée : elle expirerait.

## 9. Réseau, permissions, confidentialité (source réglable dès la v1)

- **Aucune permission nouvelle** (`INTERNET` est déjà demandée). HTTPS seulement : une source `http://` est refusée à la
  saisie ; `network_security_config` à relire en commit 5. Un seul hôte par téléchargement : celui de la source.
- **Ce que le serveur source voit** : l'**adresse IP du téléphone** (ou celle du réseau, d'un VPN, d'un relais), l'heure, et les
  plages d'octets demandées, qui révèlent la **zone** téléchargée. Pour un usage militaire c'est un sujet de confidentialité, et
  l'écran le dit **en clair, sans replier ni abréger**, avant le premier téléchargement et chaque fois qu'on change de source :
  « Le serveur source voit la zone demandée et l'adresse du téléphone. » (texte complet au §12). L'application ne journalise ni
  adresse ni zone, n'envoie rien d'autre (pas de cookie, pas de compte) et utilise un en-tête `User-Agent` fixe et générique
  (« NetworkTact »), sans version d'application ni modèle de téléphone.
- **Autre adresse de source, dès la v1** : champ « Adresse de la source » (réglage avancé, mais **la source en cours est
  toujours affichée sur l'écran de téléchargement**). Deux formes acceptées : un modèle d'éditions
  (`https://hôte/chemin/AAAAMMJJ.pmtiles`, sondé comme aujourd'hui) ou l'URL directe d'un seul `.pmtiles`. Par défaut
  `https://build.protomaps.com`. Les mêmes contrôles d'en-tête, de version et de schéma s'appliquent à toute source ; un
  **miroir interne** de l'équipe est donc un simple changement d'adresse, et le seul moyen de ne rien révéler à Protomaps. Le
  miroir doit gérer les requêtes `Range` (la sonde, ou son script, peut être lancée contre lui : `--base`).
- **Arrière-plan** : première version dans la **portée applicative** (comme l'import), écran allumé
  (`FLAG_KEEP_SCREEN_ON` pendant le téléchargement) ; le journal de reprise (§10) couvre une interruption. **WorkManager
  seulement si l'essai sur téléphone le justifie** (WorkManager est déjà au catalogue, `POST_NOTIFICATIONS` déjà demandée).
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
- Plafonds : 3 000 000 tuiles par plan (mémoire des entrées) et 500 Mo par carte dans la première version (D4, validé).

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
| `…_area` | Area: %1$s × %2$s km | Zone : %1$s × %2$s km |
| `…_use_view` | Use the view | Utiliser la vue |
| `…_max_zoom` | Maximum zoom | Zoom maximal |
| `…_estimate_range` | About %1$s to %2$s | Environ %1$s à %2$s |
| `…_compute` | Calculate the exact size | Calculer la taille exacte |
| `…_exact` | %1$s (%2$s tiles) | %1$s (%2$s tuiles) |
| `…_start` | Download | Télécharger |
| `…_confirm_size` | This download is about %1$s. Continue? | Ce téléchargement fera environ %1$s. Continuer ? |
| `…_privacy` | The source server sees the area you request and your phone's address. | Le serveur source voit la zone demandée et l'adresse du téléphone. |
| `…_source` | Source: %1$s | Source : %1$s |
| `…_source_change` | Change | Modifier |
| `…_source_field` | Source address (https://…) | Adresse de la source (https://…) |
| `…_source_https` | Only https:// addresses are accepted. | Seules les adresses https:// sont acceptées. |
| `…_error_tiny` | The area is too small or reversed. | La zone est trop petite ou inversée. |
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
  19 078 479) ; couverture d'un rectangle (rectangle de Cincu, 30 km de rayon : 5 041 tuiles au zoom 15 et 6 822 au total,
  calculées indépendamment par la sonde) ; correspondance cadre d'écran ↔ bornes géographiques (rectangle inchangé, coins
  inversés, hors limites) et dimensions en km ; codec de répertoires (aller-retour, exemples de la spécification) ; fusion de plages ; plan sur une
  **archive source synthétique** construite par le test (deux niveaux de répertoires, tuiles dupliquées, tuiles
  absentes) ; écriture, longueur et contrôle final avec `okio-fakefilesystem`.
- **Réseau** : `ktor-client-mock` (déjà au catalogue) : 206 normal, 200 au lieu de 206, ETag changé, 429/503 puis succès,
  coupure puis reprise par journal, annulation sans fichier résiduel, erreur disque.
- **Interopérabilité dans la CI (D10, validé)** (le point où un écrivain maison peut se tromper sans que rien ne le dise) :
  workflow **séparé** `fork-carte-interop.yml`, lancé à la main et par poussée des seuls fichiers de l'extracteur, **pas dans
  `fork-apk`** (qui dure déjà 27 minutes) : un test JVM écrit un `.pmtiles` à partir d'une source synthétique, puis le binaire
  `pmtiles` de go-pmtiles (version publiée, BSD-3, téléchargé sur l'exécuteur) lance `pmtiles verify` et `pmtiles show` ; le
  workflow échoue si l'un des deux refuse le fichier. Une variante **sur une vraie petite zone** (quelques km, quelques Mo)
  pourra comparer, sur l'exécuteur, notre fichier à `pmtiles extract` de la même édition et de la même zone (mêmes nombres
  de tuiles et de contenus).
- **Sonde (D1)** : `scripts/carte-sonde/sonde.py`, avec son essai local `test_sonde.py` (archive synthétique, serveur local), et
  le workflow `fork-carte-sonde.yml` (§15).
- **Android** (`androidApp/src/fdroid`, compilé par la CI seulement, comme `OfflineMapLibrary`) : le moins de logique
  possible ; l'orchestrateur est testé en `feature:map`.
- Lint : `spotlessCheck` et `detekt` en local avant chaque poussée ; `python3 scripts/sort-strings.py` après les textes.

## 14. Découpage en commits (un sujet chacun, messages en français)

Déjà faits sur cette branche : 3d4bc76 (`fork-apk` ignore les poussées qui ne changent que des `.md`, lancement manuel intact),
le plan, sa mise à jour, puis la sonde D1 (script, essai local, workflow) et ses résultats. **Rien d'autre avant la validation
des résultats de la sonde.** Ensuite :

1. Commentaires « MIT » de `pmtiles-reader` corrigés en « BSD-3-Clause » (3 fichiers de commentaires, aucun code).
2. Identifiants de Hilbert et couverture d'un rectangle (tuiles par zoom) + tests.
3. Codec de répertoires PMTiles v3 et écrivain d'en-tête + tests.
4. Fusion de plages et plan d'extraction (entrées pertinentes, réencodage, taille finale) + tests sur archive synthétique.
5. Source HTTP par plages (Ktor, ETag/`If-Match`, essais, délais, `User-Agent` fixe) + tests `MockEngine`.
6. Écriture du fichier, journal de reprise, contrôle final + tests.
7. Sondage de l'édition, mémorisation, **source réglable** (validation https, modèle ou URL directe) + tests.
8. Estimation de taille (fourchette et exacte) + règle des 50 Mo + tests.
9. `OfflineMapLibrary` : téléchargement, annulation, reprise, nettoyage, `<id>.json` (fdroid).
10. Logique du cadre : écran → bornes, dimensions en km, validité + tests.
11. Écran « Télécharger une carte » : cadre à poignées sur la carte, « Utiliser la vue », zoom, taille, progression, source et
    mention de confidentialité ; textes EN/FR ; mode démo.
12. Liste « Cartes sur le téléphone » : informations d'édition et de zone, « Reprendre ».
13. Workflow d'interopérabilité (`pmtiles verify` et `pmtiles show`).
14. Documentation : `FORK.md` §29, `CARTE-HORS-LIGNE.md` (nouvelle procédure ; celle du Mac gardée en secours),
    `TESTS-A-FAIRE.md` §17 complété.
15. (selon D12) « Exporter une carte » vers un fichier choisi par l'utilisateur.
16. (lot ultérieur) saisie des dimensions au clavier ; raster (§17).

Chaque commit est vérifié avant la poussée (lint local, tests du module) ; la CI `fork-apk` fait foi pour la compilation fdroid.
Branche `feat/carte-telechargement`, partie de `feat/apk-release`, aucune pull request.

## 15. Sonde de l'édition (D1) : ce qui est vérifié et ce qui ne l'est pas encore

- **`build.protomaps.com` est refusé depuis l'environnement de travail** (le proxy répond 403 au tunnel HTTPS ; `maps.protomaps.com`
  de même). Aucun contournement n'a été cherché. La sonde est donc écrite pour tourner **sur un exécuteur GitHub**
  (`fork-carte-sonde.yml`) ; en attendant ses résultats, tout ce qui suit est une **hypothèse** tirée de la spécification, de
  `go-pmtiles` et du code google existant.
- **Ce que la sonde établit** (`scripts/carte-sonde/sonde.py`, lecture seule par `Range`, aucune tuile de la zone n'est
  téléchargée, aucun secret) : (1) les éditions qui répondent (16 jours) et la conservation des anciennes (20, 30, 45, 60, 90
  jours) ; (2) statut 206, `Content-Range`, `ETag`, `If-Match` bon et faux (412 attendu), plages multiples ; (3) l'en-tête réel
  (version, « clustered », compressions, type, zooms, bornes, compteurs, taille des sections) ; (4) les métadonnées (clés,
  `version`, `attribution`, couches `vector_layers`) **comparées aux couches du style 5.7.2 embarqué** ; (5) pour la zone de Cincu
  (30 km, zoom 15) : nombre de tuiles (rectangle et cercle), nombre et taille des répertoires lus, niveaux, durée, tuiles
  présentes, octets exacts, moyenne par tuile et nombre de requêtes après fusion à 5 % ; (6) trois tuiles d'essai (gzip).
- **Essayée en local** (`scripts/carte-sonde/test_sonde.py`) sur une archive synthétique servie par un serveur local qui gère
  `HEAD`, `Range`, `ETag` et `If-Match` : deux niveaux de répertoires, tuile partagée, résultats recomptés. Cela vérifie le
  script, pas le serveur Protomaps. Les identifiants de Hilbert coïncident avec la spécification (zoom 12, 3423, 1763 → 19 078 479 ;
  zoom 1 → 1 à 4).
- **Résultats de la sonde** : à ajouter ici après son premier run (section « Résultats de la sonde » ci-dessous).
- Pas d'accès à un téléphone : aucune ouverture dans MapLibre du fichier produit.

### Résultats de la sonde

(en attente du premier run de `fork-carte-sonde`)

## 16. Essais sur téléphone (futur `TESTS-A-FAIRE.md` §17)

- Cincu, 30 km, zoom 15 : taille annoncée avant, taille réelle après ; durée ; la carte s'affiche, mode avion compris.
- Comparaison avec le fichier de 15 Mo fait sur le Mac : même rendu aux mêmes zooms, mêmes libellés.
- Mention ⓘ : « © contributeurs OpenStreetMap · Protomaps ».
- Annulation en cours : aucun fichier résiduel (`Cartes sur le téléphone`, espace libre inchangé).
- Coupure (mode avion) en cours, puis reprise : le fichier final est complet.
- Fermer l'application pendant le téléchargement, la rouvrir : « Reprendre ».
- Espace presque plein : refus avec les tailles.
- Deux cartes de zones différentes, choix de l'une puis de l'autre, suppression d'une.
- Mode démo : bouton désactivé, rien téléchargé. Confirmation au-delà de 50 Mo, la même sur Wi-Fi et sur données mobiles.
- Cadre : poignées faciles à saisir au doigt, dimensions en km justes, zone téléchargée = zone du cadre ; carte tournée ou inclinée
  remise au nord à l'ouverture.
- Écran de confidentialité : texte complet lisible ; source modifiée (miroir) ; une adresse `http://` est refusée.
- **Comparaison sur Mac** (D10) : voir `TESTS-A-FAIRE.md` §17.
- Écran des licences : `pmtiles-reader` affiché avec la licence BSD-3-Clause.
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
7. **Licence** : voir §3 (BSD-3 contre « MIT » en commentaires, compatibles GPL-3.0 ; écran des licences à vérifier sur
   téléphone ; extracteur maison sans code repris de `go-pmtiles`).
8. **Cadre à poignées** (§6) : plus coûteux que deux touchers, risque de réglage au doigt ; repli prévu.

## 19. Questions ouvertes

- Taille réelle des répertoires nécessaires à l'estimation exacte (à mesurer, commit 1).
- Valeur exacte du paramètre de version du schéma dans les métadonnées d'une édition réelle (commit 1).
- Durée de conservation des anciennes éditions sur `build.protomaps.com`.
- Faut-il aussi remplacer l'extracteur google (MBTiles, 2 000 tuiles) par celui-ci ? Hors sujet : NetworkTact est publié
  sur Android fdroid.

## 20. Décisions

Validées le 11 novembre 2026 (D1 à D11), avec les précisions de l'utilisateur :

| | Décision |
|---|---|
| D1 | La sonde passe en premier : en-tête et métadonnées réels (dont version du schéma de tuiles contre le style 5.7.2), acceptation des `Range`, taille des répertoires pour Cincu (45,917 N 24,800 E, 30 km, zoom 15). Le plan est mis à jour avec ses résultats **avant** de coder l'extracteur. |
| D2 | Extracteur maison. |
| D3 | **Rectangle dès la v1** (départ : zone visible, redimensionné par les coins, dimensions en km affichées), pas de centre + rayon ; écart chiffré au §6. |
| D4 | Zoom maximal **15 par défaut** ; plafonds 3 000 000 tuiles et 500 Mo par carte. |
| D5 | **Aucune distinction Wi-Fi / données mobiles** : même règle partout, confirmation au-delà de **50 Mo**. |
| D6 | Portée applicative ; WorkManager seulement si l'essai le justifie. |
| D7 | Téléchargement interrompu conservé 7 jours. |
| D8 | **Source réglable dès la v1** ; l'écran dit clairement que le serveur source voit la zone demandée et l'adresse du téléphone, et propose une autre adresse (miroir interne plus tard). |
| D9 | Crédit par la règle de la tâche 27. |
| D10 | Interopérabilité `pmtiles verify` / `show` dans la CI ; comparaison sur Mac dans `TESTS-A-FAIRE.md` §17 entre le fichier du téléphone et `cincu-30km.pmtiles` (produit le 8 novembre). |
| D11 | Raster dans une tâche distincte après celle-ci. |

Reste à décider :

- **D12** : la comparaison sur Mac demande de sortir le fichier du téléphone. Avec l'APK de release (non débogable), `adb run-as`
  est impossible ; avec l'APK de debug (`com.geeksville.mesh.fdroid.debug`) il fonctionne. Faut-il ajouter « Exporter une carte »
  (enregistrement dans un dossier choisi, commit 15, environ 60 lignes), utile aussi pour partager une carte entre équipes (ODbL
  : mention à conserver) ? Recommandation : oui.
