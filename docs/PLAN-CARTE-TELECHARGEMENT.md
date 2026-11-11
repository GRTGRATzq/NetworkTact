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
- **Nom** : proposé (« Carte 45,917 N 24,800 E · 60 × 60 km · z15 »), modifiable. **Source** : une ligne toujours visible,
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

**Mesures de la sonde (§15, édition du 11 novembre 2026)** qui commandent la conception :

- Le coût d'une estimation **exacte** est **à peu près constant** : le répertoire racine (15 567 octets, déjà dans les 16 Ko du premier
  appel) et **8 à 9 répertoires feuilles, 1,05 à 1,10 Mo au total**, en 8 ou 9 requêtes, 2,7 à 3,4 s depuis un exécuteur GitHub. Même
  volume pour 8 km, 30 km et 60 km : chaque niveau de zoom tombe dans une feuille, et ce sont les mêmes feuilles.
- La taille **ne suit pas le nombre de tuiles** : les tuiles de bas zoom sont énormes (zoom 7 : 220 à 490 Ko pièce, zoom 8 : 220 à
  350 Ko) et celles du zoom 15 petites en campagne (1,0 Ko) mais grosses en ville (6,4 Ko). Cincu, 30 km : zooms 0 à 11 = **4,1 Mo
  pour 48 tuiles**, zoom 12 : 0,92 Mo, 13 : 1,35, 14 : 2,20, 15 : 4,15 ; total **12,7 Mo** pour 5 485 tuiles (2,3 Ko en moyenne).
  Draguignan, 8 km : zooms 0 à 11 = 1,6 Mo, 12 : 0,21, 13 : 0,35, 14 : 0,70, 15 : 1,87 ; total **4,7 Mo** pour 428 tuiles
  (**11 Ko** en moyenne, 4,8 fois plus par tuile que Cincu).
- Cohérence avec le fichier du Mac : le rectangle circonscrit de Cincu compte 6 822 tuiles contre 5 485 pour le cercle ; les
  1 337 tuiles de plus (zooms 13 à 15, environ 1,5 Ko) ajoutent environ 2 Mo : de l'ordre des **15 Mo** obtenus sur le Mac.

Deux niveaux, pour que le chiffre ne soit jamais présenté comme plus sûr qu'il n'est :

1. **Immédiate, hors ligne, indicative** : nombre de tuiles du cadre (calculé, exact) et **fourchette** de taille
   `[campagne, ville]` : par zoom, 1,0 / 2,1 / 4,9 / 12 Ko par tuile aux zooms 15 / 14 / 13 / 12 (campagne, Cincu) contre
   6,4 / 8,5 / 13,5 / 23 Ko (ville, Draguignan), plus une part fixe de 1,5 à 4 Mo pour les zooms 0 à 11. Ces coefficients viennent
   de **deux zones seulement** : une grande ville peut dépasser la borne haute. L'écran l'écrit (« estimation grossière »).
2. **Exacte, après « Calculer la taille »** (bouton explicite, pas automatique, pour ne rien envoyer à chaque mouvement du cadre) :
   étapes 1 à 4 de l'algorithme, sans télécharger une tuile ; **taille finale à l'octet près** (plus l'excédent de fusion de
   5 %). Les répertoires lus sont gardés en mémoire pour la séance : un second calcul sur une zone voisine ne coûte presque rien.
   C'est ce chiffre qui commande le contrôle d'espace libre et la confirmation.

Le bouton « Télécharger » lance le niveau 2 s'il n'a pas été fait, puis **demande confirmation si la taille dépasse 50 Mo**
(« Ce téléchargement fera environ X. Continuer ? »). **La règle est la même sur Wi-Fi et sur données mobiles** : le
téléchargement est permis sur les deux, l'application ne distingue pas le type de connexion et n'en lit pas l'état
(`ACCESS_NETWORK_STATE` n'est pas utilisé par cette tâche). Sous 50 Mo, il démarre sans confirmation. Repère : 60 km de rayon
autour de Cincu donne 43,7 Mo (45,8 Mo transférés), sous le seuil ; un département dense le dépasserait.

## 8. Édition Protomaps

- **Source** : `https://build.protomaps.com/AAAAMMJJ.pmtiles` (139,4 Go le 11 novembre 2026). Pas d'alias « dernière ».
- **Conservation mesurée** : le 11 novembre, **sept éditions répondent (11 au 5 novembre) ; celle du 4 novembre et toutes les
  plus anciennes (jusqu'à 90 jours) répondent 404.** Les éditions ne vivent donc qu'**environ une semaine**. Conséquences :
  l'exemple `20261007` de `CARTE-HORS-LIGNE.md` n'existe plus (à remplacer par « une date des sept derniers jours » au commit de
  documentation) ; l'édition du jour est publiée en cours de matinée (celle du 11 novembre : `Last-Modified` 09:03 UTC) ;
  chaque édition de cette semaine est complète (139,3 à 139,4 Go).
- **Trouver l'édition la plus récente, sans aucune date codée en dur** :
  1. Les candidates sont **calculées** : date du jour en UTC, puis J-1, …, J-7 (8 dates), au format `AAAAMMJJ`. Aucune date
     n'est écrite dans le code ni dans les ressources ; seul le modèle d'URL l'est (et il est réglable, §9).
  2. Une requête `HEAD` par date, **de la plus récente à la plus ancienne**, arrêt à la première qui répond 200 (avec
     `Accept-Ranges: bytes` et une longueur) : c'est l'édition disponible la plus récente. La nuit, J répond 404 et J-1 est prise.
  3. **Horloge du téléphone fausse** : chaque réponse, même 404, porte un en-tête `Date` du serveur. Si les 8 dates échouent et
     que ce `Date` diffère de plus de 24 h de l'horloge du téléphone, les 8 candidates sont recalculées une fois à partir du
     `Date` du serveur. Aucune autre tentative.
  4. **Messages distincts** (§12) : aucune réponse du tout (pas de réseau, DNS, TLS) → « Impossible de joindre la source
     <hôte>. Vérifiez la connexion Internet. » ; le serveur répond mais aucune des 8 dates n'existe → « Aucune édition de carte
     n'a été trouvée sur <hôte> pour les 8 derniers jours. Réessayez plus tard ou changez l'adresse de la source. » ; source
     réglée sur une URL directe (un seul fichier) qui répond 404 → « Le fichier <URL> est introuvable. » Rien n'est écrit sur le
     téléphone dans ces trois cas.
  5. Tests (commit 7) : dates calculées autour d'un changement de mois et d'année, J absent et J-1 présent, toutes absentes,
     horloge en avance ou en retard de plusieurs jours (recalcul par `Date`), réseau injoignable, URL directe.
- **Mémorisation** : l'édition (date, taille, ETag) est écrite avec la carte (`<id>.json`) et affichée dans la liste
  (« édition du 11 novembre 2026 »).
- **Si l'édition disparaît** : (1) en cours de téléchargement, c'est un 404 ou un 412 : le téléchargement s'arrête proprement, rien
  n'est gardé (ou reprise impossible, §10) ; (2) au sondage, si aucune édition ne répond : message du point 4 ci-dessous ;
  (3) **adresse de source réglable** (miroir) ; (4) la procédure du Mac reste documentée.
- **Changement en cours de route** : chaque requête de tuile porte `If-Match` avec l'ETag du premier appel (**mesuré : 206 avec le
  bon ETag, 412 avec un faux**, l'ETag est de type multipart S3, `"…-520"`) ; un 412 arrête le téléchargement
  (« L'édition a changé pendant le téléchargement ») et invalide le journal. `If-Range` est écarté : en cas d'écart il
  renverrait le fichier entier (139 Go).
- **Compatibilité de schéma** : mesurée sur l'édition du 11 novembre : métadonnées `version` = **4.15.2**, `name` = « Protomaps
  Basemap », 9 couches (`boundaries`, `buildings`, `earth`, `landcover`, `landuse`, `places`, `pois`, `roads`, `water`) **identiques
  aux 9 couches du style embarqué 5.7.2** (clair et sombre), et **tous les champs lus par le style sont déclarés dans les
  métadonnées, sauf `pgf:name`, `pgf:name2` (couche `earth`) et `pgf:name3` (quatre couches)**, qui ne servent qu'au texte
  devanagari. La règle de l'application : accepter si les couches du style sont toutes présentes et si `version` est 4.x ;
  sinon refuser avec un message au lieu de produire une carte vide. Reste à vérifier à l'œil : l'aspect de la carte, sur téléphone.
- **Option** : figer la date par défaut dans l'application ? Rejetée : elle expirerait en une semaine.

## 9. Réseau, permissions, confidentialité (source réglable dès la v1)

- **Aucune permission nouvelle** (`INTERNET` est déjà demandée). HTTPS seulement : une source `http://` est refusée à la
  saisie ; `network_security_config` à relire en commit 5. Un seul hôte par téléchargement : celui de la source.
- **Ce que le serveur source voit** : l'**adresse IP du téléphone** (ou celle du réseau, d'un VPN, d'un relais), l'heure, et les
  plages d'octets demandées, qui révèlent la **zone** téléchargée. Pour un usage militaire c'est un sujet de confidentialité, et
  l'écran le dit **en clair, sans replier ni abréger**, avant le premier téléchargement et chaque fois qu'on change de source :
  « Le serveur source voit la zone demandée et l'adresse du téléphone. » (texte complet au §12). L'application ne journalise ni
  adresse ni zone, n'envoie rien d'autre (pas de cookie, pas de compte) et utilise un en-tête `User-Agent` fixe et **neutre**
  (`pmtiles-client/1.0`), identique sur tous les téléphones : **il ne nomme pas NetworkTact** (décidé le 11 novembre 2026, même
  sujet que D8 : le serveur voit déjà l'adresse et la zone, il n'apprend pas en plus quelle application les demande), ni sa
  version, ni le modèle du téléphone. La sonde de la CI (`scripts/carte-sonde`) s'est présentée sous son propre nom depuis un
  exécuteur GitHub, pas depuis un téléphone.
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
  `pmtiles-download/` (sinon la reprise est perdue), mais en supprime les journaux de plus de 7 jours (D7) **et ceux dont l'édition n'existe plus** (les éditions ne durent qu'environ une semaine, §8 : une reprise n'est possible que tant que l'édition répond ; sinon « L'édition n'existe plus : recommencer », sans rien garder).
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
| Aucune réponse au sondage | « Impossible de joindre la source » |
| 404 sur les 8 dates (après recalcul éventuel par `Date`) | « Aucune édition de carte n'a été trouvée… » |
| 404 / 412 en cours de téléchargement ou à la reprise | arrêt ; « Cette édition de la carte n'existe plus : recommencez », journal supprimé |
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
| `…_error_unreachable` | Cannot reach the source %1$s. Check the Internet connection. | Impossible de joindre la source %1$s. Vérifiez la connexion Internet. |
| `…_error_no_edition` | No map edition was found on %1$s for the last 8 days. Try again later or change the source address. | Aucune édition de carte n'a été trouvée sur %1$s pour les 8 derniers jours. Réessayez plus tard ou changez l'adresse de la source. |
| `…_error_file_missing` | The file %1$s cannot be found. | Le fichier %1$s est introuvable. |
| `…_error_edition_gone` | This map edition no longer exists: start again. | Cette édition de la carte n'existe plus : recommencez. |
| `offline_map_export` | Export | Exporter |
| `offline_map_exported` | “%1$s” exported. | « %1$s » exportée. |
| `offline_map_export_failed` | The export failed. Nothing was written. | L'export a échoué. Rien n'a été écrit. |
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

Déjà faits sur cette branche : 3d4bc76 (`fork-apk` ignore les poussées qui ne changent que des `.md`), le plan et ses mises à
jour, la sonde D1, puis, le 11 novembre 2026 : commit 1 (licence, `6366b40`) ; extracteur en logique pure et testée
(`7a10756` identifiants et couverture, `7eee4df` répertoires et en-tête, `befebaf` plan et fusion des plages, `04dae02` source
HTTP, `9e53bd5` écriture, journal et contrôle, `776a072` orchestration, `49c6596` édition et schéma) ; essai sur la vraie
édition (`842bd12`, `6b691d2`) ; estimation grossière (`f0bcbf6`), `User-Agent` neutre (`71edb88`), contrôleur commun
(`32a9af7`, commit 9), branchement Android (`0acec5a`), logique du cadre (`15a7930`, commit 10), textes (`c9afd93`) et écran
(`4f29b8b`, commit 11), puis le soir même : commit 12 (`6930d88`, `3fa9397`), commit 15 (`cced8d3`), test Compose du
panneau (`62b54e6`), plafond de 500 Mo de D4 (`dc5b8b1`, oublié jusque-là), commit 14 (`2207bec`, `9ee7d3e`). Le 13
(interopérabilité) est fait. **Tous les commits du plan sont faits ; restent les essais sur téléphone (§16).** État détaillé et reprise : `PASSATION-CLAUDE-CODE.md`. **Ordre changé par rapport à la liste ci-dessous** : l'orchestration et l'essai
d'interopérabilité (prévu en 13) sont venus avant l'édition, pour vérifier tôt l'extracteur contre le vrai serveur.

1. Commentaires « MIT » de `pmtiles-reader` corrigés en « BSD-3-Clause » (3 fichiers de commentaires, aucun code).
2. Identifiants de Hilbert et couverture d'un rectangle (tuiles par zoom) + tests.
3. Codec de répertoires PMTiles v3, écrivain d'en-tête et découpage en répertoires feuilles (racine ≤ 16 257 octets) + tests.
4. Fusion de plages et plan d'extraction (entrées pertinentes, réencodage, taille finale) + tests sur archive synthétique.
5. Source HTTP par plages (Ktor, ETag/`If-Match`, essais, délais, `User-Agent` fixe et neutre `pmtiles-client/1.0`) + tests `MockEngine`.
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
15. « Exporter une carte » par le sélecteur de fichiers Android (D12) + essai dans `TESTS-A-FAIRE.md` §17.
16. (lot ultérieur) saisie des dimensions au clavier ; raster (§17).

Chaque commit est vérifié avant la poussée (lint local, tests du module) ; la CI `fork-apk` fait foi pour la compilation fdroid.

**Branche d'attente `wip/carte-telechargement`** (décidée le 11 novembre 2026) : pendant qu'un run de `fork-apk` tourne sur
`feat/carte-telechargement`, les commits suivants sont poussés sur `wip/carte-telechargement` (le crochet d'arrêt de
l'environnement Claude Code refuse de finir une réponse avec des commits présents sur aucune branche distante) ; une fois le run
terminé, les mêmes commits sont poussés sur `feat/carte-telechargement`, sans réécriture. Vérifié : aucun workflow ne se
déclenche sur `wip/**` (`fork-apk` : `feat/**`, `fix/**`, `docs/**` ; `fork-carte-sonde` et `fork-carte-interop` :
`feat/carte-telechargement` ; `docs-deploy`, `main-check`, `verify-flatpak` : `main` ; `docs-release` : étiquettes `v*.*.*`).
**La branche `wip/carte-telechargement` est supprimée à la fin de la tâche 7.**
Branche `feat/carte-telechargement`, partie de `feat/apk-release`, aucune pull request.

## 15. Sonde de l'édition (D1) : ce qui est vérifié et ce qui ne l'est pas encore

- **`build.protomaps.com` est refusé depuis l'environnement de travail de Claude** (le proxy répond 403 au tunnel HTTPS ; `maps.protomaps.com`
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
- **Résultats de la sonde** : section « Résultats de la sonde » ci-dessous (run 2, 11 novembre 2026).
- Pas d'accès à un téléphone : aucune ouverture dans MapLibre du fichier produit.

### Résultats de la sonde

Run `fork-carte-sonde` n° 2 (11 novembre 2026, 15:25 UTC, exécuteur GitHub, 8 à 9 s par zone, 32 à 38 requêtes, 1,6 à 2,3 Mo lus par
zone, aucune limitation de débit rencontrée). Le journal et l'artefact `sonde` (JSON) sont dans le run. Les zones : Cincu 30 km
(cas de référence), Draguignan 8 km (ville dense), Cincu 60 km.

| | Résultat |
|---|---|
| Éditions | 11 au 5 novembre : HTTP 200 (139,3 à 139,4 Go) ; du 4 novembre à 90 jours : 404. **Sept éditions conservées.** |
| `Range` | 206, `Content-Range`, `Accept-Ranges: bytes`, **ETag** `"3026a200…-520"`, serveur Cloudflare (`cf-cache-status: DYNAMIC`). **`If-Match`** : bon ETag 206, faux 412. **Plages multiples : 400** (inutilisées : une plage par requête). |
| En-tête | v3 ; **clustered** ; compression interne **gzip** ; tuiles **gzip** ; type **MVT** ; zooms 0 à 15 ; monde entier ; 1 431 655 765 tuiles adressées, 178 630 665 entrées, 136 716 699 contenus. Sections : répertoire racine 15 567 octets (en-tête + racine = 15 694 ≤ 16 384, **tient dans le premier appel**), **tuiles au début (décalage 16 384)**, métadonnées (1 179 octets) et répertoires feuilles (353 548 914 octets, 2 917 feuilles) **à la fin**. L'extracteur doit donc suivre les décalages de l'en-tête, pas l'ordre de la spécification. |
| Métadonnées | `version` 4.15.2 ; `name` « Protomaps Basemap » ; `description` « Basemap layers derived from OpenStreetMap and Natural Earth » ; `attribution` en HTML (`<a href=…>&copy; OpenStreetMap</a>`) ; `type` `baselayer` ; clés `planetiler:*` et `pgf:devanagari:*`. La règle de crédit de la tâche 27 (nom « Protomaps Basemap ») s'applique donc sans rien changer. |
| Schéma / style 5.7.2 | 9 couches identiques ; champs lus par le style tous déclarés, sauf `pgf:name`, `pgf:name2`, `pgf:name3` (devanagari). |
| Répertoires pour la zone | 2 niveaux (racine puis feuilles). **Cincu 30 km : 9 feuilles, 1 095 110 octets, 3,1 s ; Draguignan 8 km : 8 feuilles, 1 053 086 octets, 2,7 s ; Cincu 60 km : 9 feuilles (les mêmes), 1 095 110 octets, 3,4 s.** |
| Tuiles (cercle, zooms 0 à 15) | **Cincu 30 km : 5 485 tuiles, toutes présentes, 5 468 contenus distincts, 12 716 161 octets (12,7 Mo), 63 requêtes après fusion à 5 % (13,3 Mo transférés).** Draguignan 8 km : 428 tuiles, 4 713 674 octets (4,7 Mo), 33 requêtes (4,9 Mo). Cincu 60 km : 21 410 tuiles, 43 674 532 octets (43,7 Mo), 95 requêtes (45,8 Mo). |
| Par zoom, Ko par tuile (Cincu 30 km) | z0 à 6 : 67 à 143 ; z7 : 486 ; z8 : 352 ; z9 : 195 ; z10 : 79 ; z11 : 27 ; z12 : 12,2 ; z13 : 4,9 ; z14 : 2,1 ; z15 : 1,0. Draguignan : z15 : 6,4 ; z14 : 8,5 ; z13 : 13,5 ; z12 : 23. |
| Tuiles d'essai | trois tuiles lues, toutes en gzip valide (début `1f 8b`), 70 Ko à 750 Ko décompressées. |

Ce que cela change dans le plan : estimation exacte constante à environ 1,1 Mo (§7) ; fourchette hors ligne à deux jeux de
coefficients (§7) ; durée de vie des éditions d'une semaine, sonde réduite à 8 jours, reprise limitée par l'édition (§8, §10) ;
`If-Match` plutôt que `If-Range` (§8) ; écrivain de répertoires à prévoir avec **découpage en répertoires feuilles** si le
répertoire racine dépasse 16 257 octets (cas probable pour 20 000 tuiles ou plus) : dans le commit 3 (§14).

### Essai de l'extracteur sur la vraie édition (workflow `fork-carte-interop`, run 1, 11 novembre 2026, 17:16 UTC)

Le code de `feature/map/.../offline/extract` (celui du téléphone), compilé seul avec `kotlinc` et un moteur HTTP de la JVM, a
extrait le rectangle de Cincu (30 km, zooms 0 à 15) de l'édition du jour, trouvée par `EditionFinder` (J, `20261111`) :

| | NetworkTact | `pmtiles extract` (go-pmtiles 1.31.2) |
|---|---|---|
| Lecture de l'en-tête et des répertoires | 12 requêtes, 1 111 983 octets, 1,3 s | 9 répertoires, 8 requêtes |
| Requêtes de tuiles | 60 (15 330 196 octets, 4,7 % d'excédent), 4,1 s | 61 (15 Mo), 5,0 s |
| Tuiles adressées / entrées / contenus | 6 822 / 6 813 / 6 787 | 6 822 / 6 813 / 6 787 |
| Données des tuiles | 14 636 351 octets | 14 636 351 octets |
| Taille du fichier | **14 650 172 octets** | 14 650 085 octets |
| `pmtiles verify` | réussi | — |
| Schéma (`BasemapSchema`) | Compatible (4.15.2) | — |

**Comparaison tuile par tuile (`scripts/carte-interop/compare.py`) : mêmes 6 822 tuiles, même contenu pour chacune.** Les
87 octets d'écart viennent seulement de la compression gzip du répertoire racine. La taille (14,65 Mo) est celle du fichier de
15 Mo fait sur le Mac le 8 novembre. Les deux fichiers sont dans l'artefact `cartes-cincu` du run : celui de NetworkTact peut
déjà être importé sur un téléphone par l'import existant (`TESTS-A-FAIRE.md` §17). **Aucune surprise par rapport à la sonde.**

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

- ~~Taille des répertoires pour l'estimation exacte~~ : mesurée, 1,1 Mo constants (§7, §15).
- ~~Clé de version du schéma~~ : `version` 4.15.2, couches identiques au style (§8).
- ~~Durée de conservation des éditions~~ : sept jours (§8).
- Le débit réel d'un téléphone sur 4G pour 60 à 100 requêtes en parallèle (la sonde mesure un exécuteur de centre de données).
- Un comportement du serveur sous charge (limitation de débit non observée sur environ 110 requêtes) : à surveiller si l'usage se répand.
- Densité d'une très grande ville (la borne haute de la fourchette vient de Draguignan seulement).
- Ne pas proposer de zoom minimal : les zooms 0 à 6 coûtent environ 0,7 Mo au total et leur retrait empêcherait de dézoomer.
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

Validées ensuite (même jour) :

| | Décision |
|---|---|
| D3 | Confirmé : rectangle à poignées, « Utiliser la vue » en repli. |
| D12 | **« Exporter une carte »** : bouton dans la liste « Cartes sur le téléphone », qui ouvre le **sélecteur de fichiers Android** (`ACTION_CREATE_DOCUMENT`, type `application/octet-stream`, nom proposé `<nom>.pmtiles`) ; la copie va **uniquement** à l'emplacement choisi par l'utilisateur, rien n'est envoyé ailleurs, aucune permission nouvelle. Utile aussi pour la comparaison sur Mac et pour partager une carte (ODbL : mention à conserver). Commit 15. |
| D13 | Calcul exact de la taille par un **bouton explicite** « Calculer la taille », jamais automatique au déplacement du cadre. |

D7 est précisé par la durée de vie des éditions : un téléchargement interrompu est gardé au plus 7 jours **et** tant que son
édition répond (§10).
