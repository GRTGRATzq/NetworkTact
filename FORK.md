# NetworkTact

NetworkTact est un fork de [Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android),
modifié à partir du 26 septembre 2026 (dernière mise à jour de ce fichier : 9 novembre 2026, branche `feat/apk-release`).

- **Licence** : GPL-3.0-or-later (voir le fichier `LICENSE`, inchangé).
- **Origine** : code de Meshtastic-Android, © Meshtastic LLC. Les mentions de copyright de
  Meshtastic LLC présentes dans les fichiers d'origine sont conservées, ainsi que l'écran
  À propos (mention Meshtastic LLC et lien vers le code source) et l'écran des licences.
- **Code source** : https://github.com/GRTGRATzq/NetworkTact
- **Radios** : le firmware des radios n'est pas modifié. Aucun nouveau protocole radio ni
  nouveau type de paquet : toutes les conventions ci-dessous sont du texte ordinaire, lisible
  par l'application Meshtastic officielle. Quatre exceptions, qui utilisent des paquets que
  Meshtastic sait déjà envoyer : le partage d'un point sur la carte (section 12, point de repère),
  « Diffuser ma position maintenant » (section 15, position standard), « Tester la liaison »
  (section 19, traceroute) et « Diffuser mon nom » (section 21, NodeInfo standard).

Le détail de chaque changement est dans l'historique Git de ce dépôt.

## Modifications, branche par branche

### 1. Identité (`feat/identite-fork`)

- Identifiant d'application `io.github.grtgratzq.networktact` (`config.properties`).
- Nom affiché « NetworkTact » (« NetworkTact Debug » en version de test).
- Ajout de ce fichier.

### 2. Textes (`feat/marque-textes`)

- « NetworkTact » remplace le nom de l'application en anglais, en français et dans 24 autres
  langues, ainsi que dans le widget, le titre de l'accueil et le service NFC de partage.
- Workflow `fork-apk` : lint (spotless, detekt) avant la construction de l'APK.

### 3. Vue Poste de commandement (`feat/vue-pc`)

- Réglage d'affichage Terrain / PC. En mode PC, l'application s'ouvre sur une vue en lecture
  seule de tous les nœuds connus.
- Fraîcheur, recalculée toutes les 15 s : contact récent jusqu'à 15 min, position ancienne
  au-delà de 10 min, tolérance d'horloge de 2 min. Une position sans heure de relevé n'est
  jamais présentée comme fraîche.

### 4. Messagerie (`feat/messagerie-filtres`)

- **Priorités**, portées par un préfixe en tête du message : `[URG]` (urgent), `[CR]`
  (compte rendu), sans préfixe = information. Rouge réservé à `[URG]`, ambre à `[CR]`.
- **États d'envoi**, sans rien inventer : « En attente de ma radio », « Transmis à ma radio »,
  « Relayé par le réseau », « Accusé par X » (« vérifié » si la preuve est valide, en message
  direct seulement), « Échec : raison ». Un message de canal n'est jamais affiché « accusé ».
- **Filtres** en haut d'une conversation (priorité, messages directs non accusés) et onglets
  par canal dans la liste des conversations.

### 5. Logo (`feat/logo`)

- Symbole vectoriel NetworkTact (`ic_networktact`) dans la barre d'application, l'icône du
  lanceur (adaptative, monochrome), le widget, l'écran de bienvenue et l'écran de démarrage.
  Icônes distinctes pour les versions de test. Source du logo : `docs/networktact-logo.jpg`.

### 6. Équipes (`feat/equipes`)

- **Liste des équipes** : message texte `[EQUIPES] Alpha;Bravo;Charlie`, diffusé depuis le
  mode PC sur un canal choisi (200 octets UTF-8 au plus, 16 octets par nom, sans `[`, `]` ni
  `;`). Un téléphone qui la reçoit demande à son utilisateur de l'adopter, en montrant le nom et
  l'identifiant de l'émetteur et l'heure. Saisie manuelle de secours possible.
- **Appartenance** : suffixe `[Équipe]` à la fin du nom long de sa propre radio
  (`ALPHA-1 [Alpha]`), envoyé par la fonction existante de modification du propriétaire. Seul le
  nom long change, dans la limite de 39 octets UTF-8 (prévenu avant toute troncature). Rien n'est
  envoyé si la radio n'est pas connectée.
- **Affichage** : le suffixe est retiré du nom et l'équipe est affichée après lui
  (« ALPHA-1 · Équipe Alpha ») dans la messagerie et la vue PC. Filtre « Équipe » dans la
  messagerie.
- **Limite** : aucune authentification. N'importe quel nœud peut envoyer une liste ou se
  déclarer d'une équipe.

### 7. Coordonnées (`feat/coordonnees`)

- **Convertisseur** WGS84 ↔ UTM, MGRS et DMS, hors ligne, écrit pour ce fork en Kotlin commun
  (`core/model`, paquet `geo`) : aucune bibliothèque Kotlin Multiplatform éprouvée n'existait.
  Séries de Krüger à l'ordre 6 (Karney 2011) ; exceptions Norvège (32V) et Svalbard (31X à
  37X). MGRS à 1 m (10 chiffres, tronqué comme le veut la norme), UTM au mètre (arrondi), DMS à
  la seconde. **Limite** : UTM et MGRS s'arrêtent à 84° N et 80° S ; au-delà (calottes
  polaires), seul le DMS est donné. Testé contre les valeurs publiées d'IBM (j-coordconvert) et
  de la suite de Chris Veness.
- **Écran convertisseur**, ouvert depuis une conversation : saisie dans un format, validation
  stricte avec un message d'erreur précis, affichage des deux autres formats. Le DMS peut être
  tapé sans les symboles (`48 51 24 N 2 21 3 E`).
- **Ma position** (bouton au-dessus de la zone de saisie) : remplit, sans l'envoyer, un message
  `[POS] ALPHA-1 · MGRS … · UTM … · DMS … · relevée 14:32` avec la dernière position de sa
  radio, ou celle que le téléphone détient déjà si la radio n'en a pas (`ALPHA-1 (téléphone)`).
  L'heure suit la règle de la vue PC, déplacée pour cela dans `core/model` (paquet
  `freshness`) : « POSITION ANCIENNE, relevée il y a 25 min (14:07) » au-delà de 10 min,
  « heure de relevé inconnue » sans heure GPS. Au-delà de 200 octets, le DMS puis l'UTM sont
  retirés ; le MGRS, le nom et l'heure restent.
- **Fait observé** (bouton au-dessus de la zone de saisie) : lieu saisi en UTM, MGRS ou DMS et
  description, qui donnent `[CR] FAIT OBSERVÉ · Lieu DMS … = MGRS … · description`. Classé
  comme compte rendu par son préfixe `[CR]`. Insertion bloquée au-delà de 200 octets.
- Les mots de ces messages sont une convention française fixe, identique sur tous les
  téléphones. Rien n'est envoyé automatiquement. Aucune photo n'est prise ni transmise.

### 8. Conversations conformes à la maquette (`feat/conversation-maquette`)

- **Carte de message** : barre de priorité de 4 dp sur le bord de début, coins droits de ce
  côté ; fond légèrement teinté pour urgent et CR, neutre pour info. Les messages envoyés sont
  alignés à droite (85 % de la largeur au plus), avec une barre neutre.
- **En-tête** : « [URG] ALPHA-1 · Équipe Alpha » et, à droite, le nom du canal ou « Direct » ;
  pour un envoi, « Moi → BRAVO-2 · Direct » ou « Moi → Général ». L'étiquette [URG] / [CR]
  n'est plus répétée dans le corps affiché (le texte copié et la recherche restent complets).
  Teintes de texte d'en-tête (contraste mesuré sur le fond de la carte, minimum 4,5:1) :
  urgent #8C1D18 (clair) / #FFB4AB (sombre), CR #6B4100 / #FFD08A, info texte neutre ; repli
  sur le texte neutre si une palette dynamique du système ne tient pas 4,5:1.
- **Ligne d'état** : « Reçu · il y a 2 min » pour un message reçu (heure réseau du message,
  recalculée toutes les 15 s ; « heure inconnue » sans heure, « heure incohérente » au-delà de
  2 min dans le futur). Les libellés d'envoi ne changent pas : un message de canal n'est jamais
  « accusé ».
- La pastille du nom court de l'expéditeur est retirée ; toucher son nom ouvre sa fiche.

### 9. Mode démo (`feat/mode-demo`)

Réglages → « Mode démo ». Le mode montre l'application sur des données fictives, sans radio et
sans rien envoyer. Il est désactivé à chaque lancement : l'état n'est gardé qu'en mémoire, et
quitter l'écran de l'application y met fin. La version bureau n'a pas d'interrupteur.

- **Données fictives** : PC-0 (ce téléphone), ALPHA-1 [Alpha], BRAVO-2 [Bravo] et
  CHARLIE-3 [Alpha] ; équipes Alpha;Bravo ; canaux Général, Équipe Alpha et PC. Les positions
  sont récente, ancienne (25 min) et sans heure. Il y a des messages Info, CR et Urgent, un
  [POS] et un [CR] FAIT OBSERVÉ, et des envois dans chaque état (en file, en route, relayé,
  accusé, échec).
- **Envois simulés** : un message envoyé passe « en file » → « en route » (1,5 s) → « relayé »
  (3 s), puis « accusé » à 5 s pour un message direct à BRAVO-2, ou « échec » à 8 s pour
  CHARLIE-3. Rien n'est transmis à la radio, et aucun réglage radio n'est modifié (choix
  d'équipe compris).
- **Bandeau permanent** : « MODE DÉMO, données fictives · envois simulés », le nombre de vrais
  messages reçus depuis l'activation, et un bouton Quitter. Dans la barre de navigation,
  l'onglet Connexions montre « Radio de démo : rien n'est envoyé ».
- **La vraie radio continue** : les paquets reçus sont enregistrés et notifiés comme
  d'habitude, et les alertes de sécurité restent affichées. Une liste [EQUIPES] reçue pendant
  la démo est proposée à la sortie. Ouvrir une notification, ou tout autre lien, quitte la
  démo puis affiche les vraies données.

| Écran | En mode démo |
|---|---|
| Conversations, une conversation, convertisseur de coordonnées, choix de la conversation où insérer un texte (depuis la section 15) | données fictives |
| Vue PC (l'onglet Nœuds s'ouvre dessus), Équipes | données fictives |
| Réglages du téléphone | fictifs. Réglages directs grisés « Indisponible en mode démo », sauf Terrain/PC (gardé en mémoire, le vrai choix est rétabli à la sortie) |
| À propos, Remerciements, Aide | inchangés |
| Carte (variante fdroid, depuis la section 14) | nœuds et points fictifs ; point GPS du téléphone, création et partage de points de repère, Site Planner masqués ; coordonnée à l'appui long et « Recaler » sur les positions fictives, sans diffusion (section 15) |
| Liste des nœuds, fiche d'un nœud, carte de la variante google, connexions, configuration radio et modules, administration à distance, et tout autre écran | « Indisponible en mode démo » |

La liste des écrans permis est fermée (`core/demo/.../DemoRoutes.kt`) : un écran ajouté plus
tard est indisponible tant qu'il n'y est pas inscrit.

**Réalisation.** Le module `core:demo` contient les données, un magasin en mémoire et des
façades. Chaque façade sert le vrai dépôt hors démo et le dépôt de démo pendant la démo, et
bascule en direct un écran déjà ouvert. Les écrans permis en démo reçoivent ces façades par le
qualificatif Koin `@Named(SCREEN_DATA)`. Les liaisons sans qualificatif, celles du service
radio, restent les vraies : le code d'envoi réel n'est pas modifié, et rien de la démo n'est
écrit dans les vrais dépôts (testé dans `ScreenFacadesTest`).

Liaisons Koin ajoutées (`CoreDemoModule`, `ScreenDataModule`) :

- sans qualificatif : `DemoMode` (`DemoModeController`), et sous leur seule classe
  `DemoStore`, `DemoNodeRepository`, `DemoPacketRepository`, `DemoRadioConfigRepository`,
  `DemoTeamRosterPrefs`, `DemoSendMessageUseCase`, `DemoMessagingController` ;
- sous `@Named(SCREEN_DATA)` : `NodeRepository`, `PacketRepository`,
  `RadioConfigRepository`, `TeamRosterPrefs`, `SendMessageUseCase`, `MessagingController`,
  `ConnectionStateProvider`, `UiPrefs`, `RadioConfigUseCase`, et `PhonePositionSource` (depuis
  la section 15, avec `DemoPhonePositionSource` sous sa seule classe).

Exceptions detekt `@Suppress("TooManyFunctions")`, au niveau de la classe, avec le commentaire
« Tous les membres sont imposés par l'interface » : `DemoNodeRepository`,
`ScreenNodeRepository`, `DemoPacketRepository`, `ScreenPacketRepository`,
`DemoRadioConfigRepository`, `ScreenRadioConfigRepository`, `ScreenUiPrefs`. Ces sept classes
implémentent des interfaces amont de plus de 11 membres. Il n'y a aucune autre exception ni
ligne de référence (baseline) ajoutée.

### 10. Thème clair / sombre (`feat/theme`)

Le choix du thème existait déjà (Réglages → Thème : Clair, Sombre, Valeur par défaut du système,
Dynamique), traduit en français ; il n'est pas modifié. Les couleurs ajoutées par NetworkTact
ont été mesurées dans les deux thèmes (contraste WCAG, 4,5:1 pour le texte, 3:1 pour une barre)
et corrigées :

- **Teintes propres à chaque thème** (`core/ui/.../theme/TactColors.kt`) : rouge (urgent), ambre
  (compte rendu), vert (position fraîche), un ton pour le clair et un pour le sombre. Le thème
  est déduit de la palette affichée, pas du système. Avec la palette « Dynamique », le ton est
  vérifié sur le vrai fond et, s'il ne tient pas le seuil, remplacé par le ton fixe de la même
  teinte le plus contrasté.
- **Couleurs d'état d'origine** (`StatusGreen`, `StatusYellow`, `StatusOrange`, `StatusRed`,
  `StatusBlue`) : elles suivaient le thème du système au lieu de celui choisi dans
  l'application. Elles suivent désormais la palette affichée ; valeurs et usages inchangés.
- **Messagerie** : barre de priorité urgent `#B3261E` / `#FF8A80` et compte rendu `#8A5300` /
  `#F2B33D` (clair / sombre), au lieu de `#E05252` et `#E8A33E` (ambre à 1,49:1 en clair).
  Le texte secondaire d'une carte passe au texte principal quand il lirait moins de 4,5:1 sur
  le fond teinté.
- **Texte secondaire en thème clair** : `onSurfaceVariant` passe de `#5C5E78` à `#54566F`
  (4,94:1 sur une carte au lieu de 4,37:1), pour toute l'application.
- **Vue PC** : position ANCIENNE ou incohérente et contact à l'heure incohérente en pastille
  inversée avec icône d'alerte, au lieu du rouge ; position fraîche en vert `#1E6B3C` /
  `#67EA94` (4,51:1 et 6,88:1 sur une carte).
- **Puce de nœud** : 16 couleurs fixes (bleus, cyans, sarcelles, verts, violets, ardoise), sans
  rouge, rose, orange, ambre, jaune ni brun, choisies par le numéro du nœud ; texte noir ou
  blanc à 4,64:1 au moins. Elle remplace la couleur tirée des octets du numéro. Les marqueurs de
  carte et les raccourcis de conversation suivent.
- Déjà conformes, inchangés : en-têtes des cartes de message, alertes et bandeau du mode démo en
  couleurs inversées (8,97:1 et 11,61:1), écran « Indisponible en mode démo », logo.

Les captures d'écran Compose (`screenshot-tests`) ne sont pas utilisées : elles demandent le SDK
Android et des images de référence, et ne tournent pas dans `fork-apk`. Les contrastes sont
vérifiés par des tests (`PriorityBarContrastTest`, `FreshnessToneTest`, `NodeColorsTest`), et des
aperçus clair / sombre existent pour les cartes de message, la vue PC, les Équipes et le mode démo.

### 11. Barre de navigation (`feat/navigation`)

Ordre des onglets, routes, liste blanche du mode démo et badges de non-lus inchangés.

- **Libellés** : Messages, Réseau, Carte, Réglages, Ma radio (EN : Messages, Network, Map,
  Settings, My radio), chaînes `tactnav_*`. Ils s'affichent sous les icônes aussi en portrait,
  plus seulement sur le rail des écrans larges. Les autres langues affichent l'anglais.
- **Icônes dessinées pour le fork** (`core/resources/.../drawable/ic_tactnav_*.xml`), dans le
  style du logo : grille 24 dp, trait 1,8 dp arrondi, nœuds pleins, ondes extérieures à 60 %.
  Elles remplacent les icônes d'origine de la barre (`ic_forum`, `ic_nodes`, `ic_map`,
  `ic_settings`, `ic_wifi`), qui restent utilisées ailleurs.
- **Onglet sélectionné** : vert `#1E6B3C` en clair (4,51:1 sur la pastille de sélection
  `#D5D6E0`) et `#67EA94` en sombre (6,88:1), au lieu du vert primaire du clair, à 2,81:1
  (`TactColors.navigationActive`, seuil 3:1). Icônes non sélectionnées 6,12:1 / 8,30:1,
  libellés 11,61:1 / 12,96:1.
- **État de ma radio** (onglet Ma radio et badge du nœud local) : la forme porte l'état
  (connectée : deux ondes ; en connexion : une onde ; en veille : croissant de lune ;
  déconnectée : barrée). Connectée en vert NetworkTact, les autres états en couleur neutre ;
  déconnectée avec une pastille « ! » en couleurs inversées. Le rouge (déconnectée), l'orange
  (connexion) et le jaune (veille) d'origine disparaissent. La petite icône du moyen de liaison
  passe dans la description d'accessibilité (« Ma radio : connectée (Bluetooth) »).

Aperçu clair / sombre de la barre et des quatre états de la radio. Pas de test automatique
des contrastes de la barre : `core:ui` ne tourne pas dans `fork-apk` ; les valeurs ci-dessus
sont mesurées sur la palette fixe.

### 12. Fait observé avec heure, points partagés sur la carte (`feat/fait-observe-carte`)

- **Heure d'observation** : le formulaire « Fait observé » a un champ « Observé à », à l'heure
  actuelle par défaut, modifiable (`14:05`, `14h05`, `1405`). Le message devient
  `[CR] FAIT OBSERVÉ · 14:05 · Lieu … = MGRS … · description`. Une heure pas encore atteinte
  est comptée comme la veille et le message le dit : `· 23:50 (veille) ·`. Au-delà de 200
  octets, le bouton reste grisé et la taille s'affiche. L'ancien format, sans heure, reste
  reconnu et classé comme compte rendu. Le fait observé du mode démo suit le nouveau format.
- **Coordonnée dans un message** (`core/model/.../geo/MessageCoordinate.kt`) : seule une
  coordonnée complète compte, vérifiée par le convertisseur (MGRS avec zone, bande, carré et
  5 + 5 chiffres ; UTM avec zone, bande et abscisse à six chiffres ; DMS avec un hémisphère sur
  les deux axes). Un numéro de téléphone, une heure, une date ou une référence ne sont jamais
  pris pour une position (tests dédiés).
- **Partager ce point sur la carte** : après l'envoi d'un message qui contient une coordonnée
  (`[POS]`, fait observé, coordonnée collée depuis le convertisseur), un encart propose
  « Partager » ou « Ne pas partager ». Rien n'est émis sans ce geste. Accepté, un point de
  repère Meshtastic existant (`WAYPOINT_APP`, envoyé par la fonction d'envoi de l'application)
  part sur le canal du message, ou au même correspondant pour un message direct. Il est visible
  dans l'application officielle.

  | Message | Nom du point (29 octets au plus) | Description (99 octets au plus) |
  |---|---|---|
  | Fait observé | `FO 14:05 ALPHA-1`, `FO --:-- ALPHA-1` (ancien format) | la description du fait |
  | `[POS]` récente | `POS 14:32 ALPHA-1` | `relevée 14:32` |
  | `[POS]` ancienne | `POS ANC 14:07 ALPHA-1`, `POS ANC --:-- ALPHA-1` | `POSITION ANCIENNE, …` |
  | `[POS]` sans heure | `POS --:-- ALPHA-1` | `heure de relevé inconnue` |
  | Autre message | `PT ALPHA-1` | `envoyé 14:05 · texte` |

  Les longueurs sont celles du protobuf (`name` 30 et `description` 100 octets, fin de chaîne
  comprise), coupées par point de code. Le point est valable 24 h, verrouillé à son auteur
  (lui seul peut le modifier ou le retirer), avec l'icône 👁, 📍 ou 📌. Son identifiant est
  dérivé de l'auteur et du texte : un même message partagé deux fois met à jour le même point.
  Un point partagé par erreur se retire par la fonction existante de la carte (Supprimer, puis
  « pour tout le monde », avec confirmation).
- **Voir sur la carte** : un message reçu qui contient une coordonnée complète affiche ce
  bouton, qui ouvre la carte centrée sur le point (`MapFocusRequests`, `core/ui`). Affichage
  local seulement, rien n'est émis. Le point n'est pas marqué sur la carte : seul le centrage
  l'indique, tant que la carte hors ligne (P1-5) n'existe pas.
- **Fenêtre d'un point** (carte MapLibre, variante fdroid et bureau) : « De ALPHA-1 · reçu
  06/10/26 14:05 », « De moi » pour un point de cette radio, « heure inconnue » sans heure. La
  carte Google (variante google) centre aussi sur le point, mais sa fenêtre n'a pas changé.
- **Mode démo** : la carte reste indisponible. « Partager » ne fait rien partir (le contrôleur
  d'écran est celui de la démo, testé dans `ScreenFacadesTest`), l'encart et la confirmation
  le disent ; « Voir sur la carte » répond « Carte indisponible en mode démo ».
- CI : les tests de `feature:map` tournent dans `fork-apk`, dans une étape à part.

### 13. Carte hors ligne, lot 1 (`feat/carte-hors-ligne`)

Variante fdroid (carte MapLibre). Rien n'est émis par radio et rien ne passe par Internet.

- **Cartes sur le téléphone** : menu des fonds de carte → « Cartes sur le téléphone ». Import
  d'un fichier `.pmtiles` par le sélecteur de fichiers Android, liste des cartes installées
  (nom, taille, zooms) et suppression après confirmation. La procédure pour produire un
  fichier sur un Mac (une ville avec `--bbox`, un département avec `--region`) est dans
  `CARTE-HORS-LIGNE.md`.
- **Import contrôlé** (`OfflineMapLibrary`, `androidApp/src/fdroid/.../map/offline/`) :
  en-tête PMTiles v3 vérifié avant toute écriture (refus d'un fichier qui n'en est pas un,
  d'une autre version ou d'une archive raster), espace libre vérifié (taille du fichier plus
  50 Mo ou 5 %), copie avec progression et annulation dans `pmtiles-import/`, longueur vérifiée,
  puis déplacement dans `pmtiles/`. Un échec ou une annulation ne laisse aucun fichier ; chaque
  message le précise.
- **Affichage sans réseau** : la carte est un fond vectoriel local (`Basemap.LocalVector`) dont
  le style pointe vers le téléphone : `pmtiles://file://…` pour les tuiles, `file://…` pour les
  polices et les sprites, installés depuis l'APK au premier usage, seulement si une carte existe.
  Style Protomaps clair ou sombre selon le thème choisi dans l'application (même test que
  `TactColors`), libellés en français. Elle apparaît dans le menu de toutes les cartes MapLibre
  de l'application (carte principale, fiche d'un nœud, trace, traceroute, découverte). Au
  démarrage, la carte attend la liste des cartes locales avant de choisir son fond, pour ne pas
  basculer d'un fond intégré au fond mémorisé.
- **Embarqué dans l'APK** (`androidApp/src/fdroid/assets/offline-map/`, régénéré par
  `scripts/offline-map-assets.mjs`) : styles Protomaps Basemaps 5.7.2 (BSD-3-Clause), polices
  Noto Sans Regular, Medium et Italic en glyphes, plages latines, grecques et cyrilliques
  (SIL OFL 1.1, `OFL.txt` joint), sprites Protomaps v4 (MIT). Environ 2,6 Mo, 1,4 Mo compressés.
- **Mentions** : « © contributeurs OpenStreetMap · Protomaps » sur une carte `.pmtiles` ; pour un
  fichier MBTiles importé, la mention de sa table `metadata` (champ `attribution`, lu à
  l'import), ou à défaut « © contributeurs OpenStreetMap ». Elles s'affichent dans le bouton
  d'attribution de MapLibre (ouvert à l'affichage de la carte). Écran des licences : données
  OpenStreetMap (ODbL 1.0), style et sprites Protomaps, polices Noto Sans
  (`config/aboutlibraries/libraries/`, textes ODbL et OFL dans `config/aboutlibraries/licenses/`).
- **Sans carte installée**, la carte est inchangée : seule l'entrée « Cartes sur le
  téléphone » s'ajoute au menu, et aucun fichier n'est écrit.
- **Tests** (`feature/map/.../offline/`, commonTest) : lecture et refus d'en-têtes PMTiles,
  espace libre, nom de la carte, mention par défaut, style entièrement local, glyphes vides ;
  relecture d'une source MBTiles enregistrée avant le champ `attribution`.
- La variante google n'a pas l'import `.pmtiles`. Le lot 2 est décrit à la section 14.

### 14. Carte hors ligne, lot 2 (`feat/carte-hors-ligne`)

Carte MapLibre (variante fdroid ; bureau pour ce qui est commun). Tout vaut aussi pour un
fichier MBTiles et sans carte locale. Rien n'est émis par radio.

- **Carte locale par défaut** : une carte importée (`.pmtiles` ou `.mbtiles`) devient le fond
  choisi ; un fond choisi ensuite est respecté. Si le fond mémorisé a été supprimé, la carte
  prend une autre carte locale, sinon le fond intégré (`resolveBasemap`, testé). La carte
  attend que ses sources soient lues sur le disque avant de choisir son fond (auparavant un
  fichier MBTiles mémorisé s'ouvrait sur un fond intégré puis basculait).
- **Polices locales pour un fichier MBTiles** : sur une archive stockée sur le téléphone, les
  libellés (points de repère) utilisent les polices de l'APK au lieu de celles d'OpenFreeMap.
- **Marqueur « Voir sur la carte »** : le point reste marqué (rond à la couleur principale du
  thème, sans texte), avec un bandeau « Position relevée 14:32 · ALPHA-1 », « Position
  ANCIENNE relevée 14:07 · … », « Fait observé 14:05 · … » ou « Point d'un message · heure
  de relevé inconnue · …, reçu … », et ✕ pour le retirer (`MapFocusRequests.marked`,
  `MessagePoint` dans `core/model/.../geo`, testé).
- **Puces des nœuds** (règle de la vue PC, `NodeFreshness`) : étiquette inversée
  « ANCIENNE » au-delà de 10 min (ou reçue depuis plus de 10 min sans heure de relevé),
  « HEURE INCOHÉRENTE » si l'heure est en avance ; toujours visible, même quand les puces se
  chevauchent. L'heure est donnée au toucher, dans un encart : ligne de position de la vue
  PC dans son style, « Relevée à 14:32 », « Relevée le <date> » si ce n'est pas aujourd'hui,
  ou « Heure de relevé inconnue », et « Détails » vers la fiche du nœud. Aussi sur la
  mini-carte d'un nœud et la carte de traceroute. Tests : `NodePositionTimeTest`.
- **Mode démo** : la carte est permise en variante fdroid (`DemoRoutes`, constante
  `MAP_AVAILABLE_IN_DEMO`), la variante google la garde « Indisponible en mode démo ».
  `SharedMapViewModel` lit nœuds, points et configuration par les façades
  `@Named(SCREEN_DATA)`. En démo : nœuds fictifs avec heure de relevé (CHARLIE-3 sans heure),
  point GPS du téléphone et suivi masqués, création, modification et suppression « pour tout
  le monde » des points de repère masquées, Site Planner masqué ; `sendWaypoint` n'envoie
  rien (testé). « Voir sur la carte » ouvre la carte aussi en démo.
- CI : les tests de `feature:map-maplibre` tournent dans `fork-apk`, en dernière étape.
- **Règle CI : Android uniquement.** NetworkTact est publié sur Android. `fork-apk` ne lance
  plus que les tests Android et JVM (`jvmTest` partout, plus `testAndroidHostTest` là où le
  module déclare `withHostTest`) : plus aucune tâche iOS, donc une erreur propre à iOS ne
  bloque plus l'APK. Les cibles iOS restent déclarées (`build-logic` inchangé) et sont
  compilées à part par `.github/workflows/ios-check.yml`, lancé à la main et chaque lundi
  (le déclenchement hebdomadaire ne fonctionne que depuis la branche par défaut), sans jamais
  bloquer `fork-apk`.

### 15. Coordonnée au toucher et recalage GPS (`feat/carte-coordonnees`)

Carte MapLibre (variante fdroid ; bureau pour ce qui est commun). Rien ne dépend du fond de
carte : `.pmtiles`, `.mbtiles` ou fond en ligne. La carte Google (variante google) ne change pas.

- **Coordonnée à l'appui long** : un appui long ouvre un encart avec la coordonnée du point en
  MGRS à 1 m, UTM et DMS (`MapPointCoordinate`, `core/model/.../geo`), marquée par le marqueur
  de « Voir sur la carte ». L'encart rappelle que c'est un lieu choisi, sans heure, et non une
  position GPS. Le simple toucher reste réservé à la sélection des marqueurs. Actions :
  - « Copier » et « Insérer dans un message » donnent le texte
    `Point carte · MGRS … · UTM … · DMS …` (MGRS en premier, relu par `MessageCoordinate`).
    L'insertion passe par l'écran existant de choix d'une conversation, qui s'ouvre avec le texte
    dans la zone de saisie ; rien n'est envoyé ;
  - « Ouvrir dans le convertisseur » : la route `ContactsRoute.CoordinateConverter` prend un texte
    de départ (le MGRS, ou le DMS au-delà de 84° N et 80° S) et s'ouvre sur l'onglet du format ;
  - « Créer un point de repère ici » : l'ancienne action de l'appui long, gardée dans l'encart,
    proposée seulement quand elle était possible (radio connectée, hors démo, hors tracé de zone).

  La carte demande ces écrans par `MapNavigationRequests` (`core/ui`), que son entrée de
  navigation ouvre ; le contrat `MapViewProvider` ne change pas.
- **Bouton « Recaler »** (en haut à droite) : centre la carte sur la position choisie, au moins
  au zoom de détail, et ouvre un encart rafraîchi chaque seconde (`RecenterViewModel`,
  `feature/map/.../recenter`) :
  - côte à côte, **Ma radio** (« GPS de la radio : la position que reçoit le réseau ») et **Ce
    téléphone** (« GPS du téléphone : le point bleu de la carte »), chacune avec son âge dans les
    mots et le style de la vue PC, son heure de relevé (« Relevée à 14:32 », la date si ce n'est
    pas aujourd'hui, ou heure inconnue) et sa précision : satellites, HDOP et PDOP de la radio
    (`RadioFixQuality`, « précision non transmise » s'ils manquent, rayon de la précision réduite
    par le canal le cas échéant), précision horizontale d'Android pour le téléphone (`PhoneFix`
    porte désormais `accuracyMeters` ; `PhonePositionSource` est passé dans `core/repository`) ;
  - l'écart entre les deux en mètres (`PositionComparison`), et le rappel que diffuser ne rend
    pas le GPS plus précis : la précision dépend des satellites reçus par chaque GPS ;
  - « Centrer sur : Radio / Téléphone », mémorisé (`MapPrefs.recenterTarget`, radio par défaut) ;
    sans la position choisie, la carte se centre sur l'autre et l'encart le dit.
- **« Diffuser ma position maintenant »** : un seul paquet, jamais automatique ni périodique.
  - Contenu (`RadioPositionPacket`) : une position Meshtastic standard (`POSITION_APP`, aucun
    nouveau type), en diffusion sur le canal principal, sans demande d'accusé, envoyée par la
    fonction d'envoi existante (celle des points de repère). Elle contient la dernière position
    de **la radio** telle quelle, son heure de relevé GPS (`timestamp`) recopiée dans `time` :
    un récepteur lit l'âge du relevé, jamais l'heure de diffusion. Le firmware peut réduire la
    précision selon le réglage du canal. La position du téléphone n'est **jamais** diffusée.
  - Refus (`RadioPositionBroadcast`), expliqués dans l'encart : mode démo, radio non connectée,
    radio sans position, heure de relevé inconnue (une position sans `timestamp` passerait pour
    actuelle ; l'encart propose d'activer l'horodatage dans les réglages de position de la radio)
    ou dans le futur.
  - Confirmation obligatoire : avertissement sur le temps d'antenne, heure de relevé, et, pour
    une position ancienne (plus de 10 min), son âge.
  - Après l'envoi : « Position diffusée à 14:35 (relevée à 14:34) » ; « diffusée » veut dire
    remise à la radio pour diffusion, sans accusé. Si la radio refuse le paquet, l'encart le dit
    et l'attente ne démarre pas. Ensuite le bouton est bloqué 30 s avec un compte à rebours.
- **Mode démo** : coordonnée au toucher, insertion (écran de choix de la conversation ajouté à la
  liste blanche, lu par les façades) et convertisseur disponibles ; « Recaler » sur PC-0
  (9 satellites, HDOP 1,2) et une position fictive du téléphone à environ 27 m, relevée il y a
  20 s, précision 8 m (façade `@Named(SCREEN_DATA)` `PhonePositionSource`) ; la diffusion est
  refusée, et le contrôleur d'envoi de la démo ne transmettrait rien (testé dans
  `ScreenFacadesTest`).
- **Tests** : `MapPointCoordinateTest`, `PositionComparisonTest`, `RadioPositionBroadcastTest`,
  `RadioPositionPacketTest` (`core:model`) ; `RecenterViewModelTest` (`feature:map` : refus
  sans heure, sans connexion, sans position de la radio et en démo, position ancienne après
  confirmation, attente de 30 s, échec d'envoi, contenu du paquet) ; `MapPointStateTest`
  (`feature:map-maplibre`) ; `ScreenFacadesTest`, `DemoDataSetTest`, `DemoRoutesTest`
  (`core:demo`) ; `MapPrefsImplTest` (`core:prefs`) ; `CoordinateConverterContentTest`
  (`feature:messaging`). Tous ces modules tournent déjà dans `fork-apk`.
- **CI** : `fork-apk` compile le Kotlin de l'APK fdroid (`:androidApp:compileFdroidDebugKotlin`)
  juste après le Lint, avant les tests : une erreur de compilation dans un module que les
  étapes de tests ne compilent pas pour Android échoue en quelques minutes, plus à l'étape APK.

### 16. Détachement de Meshtastic (`feat/detachement`)

Plus de marque ni de service en ligne Meshtastic dans l'usage courant, dans les limites de la
licence : l'écran À propos, l'écran des licences, les en-têtes de copyright, `LICENSE` et ce
fichier sont conservés. Le firmware des radios, la configuration des radios et MQTT ne changent pas.

- **À propos** : le lien « Dépôt GitHub » ouvre le code source de NetworkTact
  (`GRTGRATzq/NetworkTact`) et non plus celui de Meshtastic-Android ; nouvelle ligne
  « Basé sur Meshtastic-Android, © Meshtastic LLC, GPL-3.0. Modifié par le projet NetworkTact. »
  (`networktact_based_on`) au-dessus du copyright Meshtastic LLC et de l'avis de licence ;
  rangées « Besoin de matériel », « Site web » et « Documentation » (meshtastic.org) retirées.
- **Visuels** : l'encart mPWRD-OS de l'écran Wi-Fi montre le symbole NetworkTact au lieu de la
  marque M-PWRD ; `img_mpwrd_logo.png`, `ic_meshtastic.xml` et `ic_launcher-114x114.png`,
  non référencés, sont supprimés ; `ic_launcher2-playstore.png` (512 px) est redessiné à partir
  du symbole.
- **Services en ligne masqués** :
  - encart et notification « Mise à jour du firmware disponible » de l'écran Connexion
    (`FIRMWARE_UPDATE_NOTICES_ENABLED` dans `ConnectionsViewModel`) ; le dialogue « firmware trop
    ancien / mise à jour conseillée », qui signale une radio sous la version minimale prise en
    charge, est gardé ;
  - réglages : section « Aide et documentation » (documentation Meshtastic, assistant Chirpy,
    déjà inactif en fdroid) et « Liens vers les appareils » (boutiques, affiliation) ;
  - fiche d'un nœud : section « J'en veux un » (liens d'achat) ;
  - carte : planificateur de site (site.meshtastic.org), bouton et entrée de la fiche d'un nœud.
- **Restent en ligne** (aucun affichage ne les annonce) : les requêtes de fond vers
  api.meshtastic.org (catalogue du matériel, liste des versions de firmware, firmware des
  événements), nécessaires à l'écran de mise à jour du firmware, conservé et renommé « Mise à
  jour du firmware radio ». Sans Internet, elles échouent sans message et l'application garde
  les listes embarquées dans l'APK ou déjà en cache ; seule la mise à jour du firmware, qui
  télécharge le fichier, a besoin d'Internet.
- **Textes** (anglais et français ; les 24 autres langues gardent leur traduction) : liens vers
  meshtastic.org retirés (firmware trop ancien, application trop ancienne, confirmation d'un rôle
  de relais, maintenance) ; « Meshtastic » remplacé par « radio », « réseau maillé » ou
  « firmware radio » (recherche d'appareils, distances, firmware événement, mode radioamateur,
  nom par défaut d'un appareil découvert, licences, partages, SNR, prise en charge du matériel,
  serveur TAK). Les noms « Meshtastic xxxx » donnés par le firmware ne changent pas.
- **Couleurs d'état** (rouge réservé à « urgent », ambre aux comptes rendus) :
  - le vert d'état prend le vert « fraîche » de `TactColors` ;
  - nouvelle pastille inversée réutilisable (`Modifier.statusAlert`, `StatusAlertContent`) ;
  - signal LoRa, SNR et RSSI : bon en vert, moyen et mauvais en neutre, aucun en pastille
    inversée ; barres Bluetooth 0-1 inversées, 2-3 neutres, 4 vertes ; batterie sous 20 %
    inversée, 20-39 % neutre ;
  - sécurité : clé incohérente et canal non chiffré avec position précise en pastille inversée,
    cadenas ouvert neutre (textes d'aide adaptés) ;
  - traceroutes et voisins : même échelle que le signal (`alertBackground` dans
    `annotateTraceroute` / `annotateNeighborInfo`) ;
  - favori, nœud non entendu sur ce préréglage, boussole au nord, version du firmware d'un
    nœud (vert seulement si identique à la stable), matériel « communauté » : neutre ;
  - « Ignorer » et « Supprimer » d'un nœud : neutre, toujours précédés d'une confirmation.
  Le rouge d'erreur Material (`colorScheme.error`) des écrans d'origine (erreurs de saisie,
  actions destructives des réglages) n'est pas modifié.
- **Tests** : `ConnectionsViewModelTest` (aucun encart ni notification de mise à jour) et
  `AnnotatedStringsTest` (fond d'alerte du SNR « aucun »), dans `core:ui`, qui ne tourne pas
  dans `fork-apk`.

### 17. Organigramme (`feat/organigramme`)

Onglet Réseau → icône « Ouvrir l'organigramme » (barre du haut de la liste des nœuds et de la vue
PC) : qui est qui et où chacun se situe dans la chaîne de commandement. Rien n'est émis par radio.

- **Structure** : PC, puis les équipes (celles de la liste adoptée en section 6, dans son ordre),
  puis leurs membres. Chaque membre a un indicatif (39 octets UTF-8 au plus), une fonction libre
  (24 octets au plus) et un rang, son ordre dans la liste. Modèle `OrgChart`, `OrgChartEditor`
  dans `core/model/.../orgchart`.
- **Édition** : en mode PC seulement (ajouter, modifier, retirer avec confirmation, monter,
  descendre). Chaque modification est enregistrée sur le téléphone, signée de l'indicatif de sa
  radio et datée. Une équipe absente de la liste des équipes reste affichée et signalée.
- **Rattachement aux radios** : par l'indicatif seul, c'est-à-dire le nom long sans son suffixe
  d'équipe (`ALPHA-1 [Alpha]` → `ALPHA-1`), sans tenir compte de la casse ; jamais par
  l'identifiant matériel : une radio remplacée qui reprend l'indicatif retrouve sa place
  (`OrgChartLinks`). Pour chaque radio : dernier contact et âge de la position, mêmes règles et
  mêmes couleurs que la vue PC (rafraîchis toutes les 15 s). Sans radio : « Aucune radio
  associée ». Plusieurs radios au même indicatif : toutes listées, aucune choisie. Une radio qui
  déclare une autre équipe que celle de l'organigramme : signalé, rien n'est corrigé. Les radios
  sans membre sont listées à part. Toucher un membre ouvre sa fiche, Message sa conversation.
- **Partage par QR** (aucune émission radio) : le PC affiche le code (texte `NTORGA1`, une ligne
  par entrée, ASCII pur, correction d'erreur moyenne), avec « Organigramme de PC-0, créé à
  HH:MM ». Limite de 770 caractères, soit un QR de version 22 (105 × 105 modules) : 9 membres aux
  indicatifs et fonctions de longueur maximale tiennent en 720 caractères ; au-delà, l'ajout est
  refusé avec la longueur atteinte. Les autres téléphones scannent le code ; une confirmation
  montre l'auteur, l'heure de création, les équipes et leurs membres avant toute adoption. Un
  code abîmé est refusé en entier.
- **Organigramme précédent** : l'adoption garde l'organigramme qu'elle remplace ; « Revenir au
  précédent » échange les deux (et peut donc être annulé).
- **Limite** : aucune authentification. N'importe quel téléphone peut afficher un QR
  d'organigramme ; la confirmation le rappelle. Voir l'écran du PC limite le risque sans
  l'éliminer.
- **Stockage** : `OrgChartPrefs` (`core/repository`), `OrgChartPrefsImpl` (préférences
  d'interface, JSON).
- **Mode démo** : écran permis (`DemoRoutes`), sur un organigramme fictif (PC-0 ; Alpha :
  ALPHA-1, CHARLIE-3 ; Bravo : BRAVO-2, et DELTA-4 sans radio) servi par la façade
  `ScreenOrgChartPrefs` sous `@Named(SCREEN_DATA)` ; modifications et adoptions restent en
  mémoire et disparaissent à la sortie. Liaisons Koin ajoutées : `DemoOrgChartPrefs` (sous sa
  seule classe) et `OrgChartPrefs` sous `@Named(SCREEN_DATA)`.
- **Autres changements** : `rememberQrCodePainter` accepte un niveau de correction d'erreur
  (`QrErrorCorrection`, HIGH par défaut, inchangé ailleurs) ; `ContactLine` et `PositionLine` de
  la vue PC passent `internal` pour être réutilisées ; icône `ic_tactnav_orgchart`.
- **Tests** : `OrgChartCodeTest`, `OrgChartTest`, `OrgChartLinksTest`, `OrgChartEditorTest`,
  `OrgChartHistoryTest` (`core:model`), `OrgChartPrefsImplTest` (`core:prefs`),
  `OrgChartStateTest`, `OrgChartViewModelTest` (`feature:node`), et dans `core:demo`
  `DemoDataSetTest`, `DemoRepositoriesTest`, `ScreenFacadesTest`, `DemoRoutesTest`.
  `NavigationConfigTest` (`core:navigation`) couvre la route mais ne tourne pas dans `fork-apk`.

### 18. Gels de Gradle en configuration dans la CI (`feat/ci-gel`)

- **Constat** : 5 runs de `fork-apk` sur une quinzaine (45, 47, 48, 54, 58) se sont figés pendant
  la configuration. Le vidage de threads montrait le thread principal du démon Gradle en attente
  sans fin dans `TaskPathProjectEvaluator.scheduleProjectsJustInTime`, c'est-à-dire dans la
  configuration parallèle d'Isolated Projects. La mémoire et le réseau étaient normaux.
- **Changement** : dans `fork-apk.yml` seulement, chaque commande Gradle passe
  `-Dorg.gradle.unsafe.isolated-projects=false --no-configuration-cache`. Les deux drapeaux vont
  ensemble, car Gradle refuse `--no-configuration-cache` tant qu'Isolated Projects est actif.
  L'ancien nom de propriété (`unsafe`) reste pris en compte par Gradle 9.8 (vérifié en local).
  `gradle.properties` et `build-logic` ne changent pas : les postes de développement gardent
  Isolated Projects et le cache de configuration. Les limites de durée par étape et l'étape de
  diagnostic sont conservées.
- **Validation** : 6 runs de suite sur le même commit (59 au push, 60 à 64 lancés à la main),
  tous verts, sans aucun gel. Durées en min:s :

  | Run | Lint | Compile | Tests | core:demo | map | APK | map-maplibre | Job |
  |---|---|---|---|---|---|---|---|---|
  | 59 | 3:48 | 4:17 | 6:27 | 0:54 | 1:17 | 3:41 | 1:27 | 22:24 |
  | 60 | 3:54 | 4:41 | 7:06 | 0:58 | 1:18 | 3:55 | 1:31 | 23:59 |
  | 61 | 3:49 | 4:42 | 6:53 | 0:58 | 1:21 | 3:55 | 1:29 | 23:48 |
  | 62 | 3:49 | 4:38 | 6:37 | 0:54 | 1:20 | 4:06 | 1:28 | 23:32 |
  | 63 | 3:37 | 4:25 | 6:37 | 0:55 | 1:19 | 3:50 | 1:27 | 22:43 |
  | 64 | 3:58 | 4:12 | 6:10 | 0:51 | 1:14 | 3:38 | 1:23 | 22:04 |
  | Moyenne | 3:49 | 4:29 | 6:38 | 0:55 | 1:18 | 3:51 | 1:28 | 23:05 |
  | Avant (56, 57) | 3:17 | 4:21 | 6:27 | 0:53 | 1:17 | 3:34 | 1:27 | 21:47 |

  Coût : environ 1 min 20 de plus par run, surtout au Lint (+30 s) et à l'APK (+17 s), où la
  configuration n'est plus mise en cache.
- **Limite** : 6 runs sans gel ne prouvent pas que le gel a disparu. Avec le taux observé
  (environ 1 run sur 3), une telle série sans gel avait environ 9 % de chances d'arriver par
  hasard. L'argument principal est ailleurs : le code où le démon restait bloqué n'est plus
  exécuté. Si un gel revient, l'étape de diagnostic le montrera.

### 19. Autodiagnostic quand aucun relais n'est disponible (`feat/autodiagnostic`)

Onglet Réseau → icône « Ouvrir le diagnostic de liaison » (antenne relais), bandeau des Messages et
du Réseau, ou bouton « Diagnostic » sous un envoi en échec. Le diagnostic n'émet rien : seul
« Tester la liaison » émet, à la main, après confirmation.

- **Déclencheurs** :
  - un message envoyé passé en échec (aucun relais entendu, délai dépassé, etc.) affiche
    « Diagnostic » sous sa carte ;
  - un bandeau en haut des Messages, de la liste des nœuds et de la vue PC dit « Radio non
    connectée » si ma radio n'est pas connectée en Bluetooth, sinon « Aucun nœud entendu depuis
    18 min » (ou « Aucun autre nœud entendu ») quand aucun autre nœud n'a été entendu depuis le
    seuil « contact récent » de la vue PC (15 min). Une connexion en cours ou une radio en veille
    n'affichent rien.
  - Le bandeau se ferme (croix) et ne revient que si la situation change : seuil suivant franchi
    (30 min, 1 h, puis chaque heure), nœud entendu à nouveau puis nouveau silence, liaison perdue
    puis retrouvée (`LinkAlerts`, clé de situation). La fermeture est gardée en mémoire, partagée
    par les onglets, oubliée au lancement suivant.
- **Constats** (`LinkDiagnostics`, `core/model/.../diagnostic`), en faits, sans conclusion :
  liaison Bluetooth (OK, connexion en cours, radio en veille, non connectée), avec l'icône d'état de
  la barre de navigation ; dernier paquet reçu de ma radio, avec son heure et son âge ; nombre de
  nœuds entendus en 5, 15 et 60 min (sans ma radio, ni les nœuds ignorés) ; dernier nœud entendu,
  par son indicatif sans suffixe d'équipe, et son heure (« Aucun nœud entendu depuis 18 min.
  Dernier : BRAVO-2 à 14:02. ») ; réglages LoRa de ma radio (région et préréglage, ou
  « paramètres personnalisés ») identiques ou non au réglage attendu ; batterie de la radio
  (pourcentage, « sur alimentation externe » ou « non transmise »). Une heure de contact en avance
  de plus de 2 min est écartée, comme dans la vue PC. Un constat à surveiller est en pastille
  inversée (`statusAlert`), jamais en rouge ni en ambre.
- **Suggestions**, jamais de verdict : vérifier la radio et la liaison Bluetooth, régler la région,
  vérifier région et préréglage, recharger la radio, se rapprocher d'un point haut, vérifier
  l'antenne. L'écran rappelle que ne rien entendre ne prouve pas que la radio est coupée du réseau.
  Ni « réseau OK » ni « isolé » ne sont jamais affichés.
- **Dernier paquet reçu de ma radio** : aucune heure de ce genre n'existait. `RadioActivityProvider`
  (`core/repository`) est tenu par `ServiceRepositoryImpl.emitMeshPacket`, par où passent tous les
  paquets que la radio transmet au téléphone. Gardé en mémoire : « aucun paquet depuis le
  lancement de l'application » après chaque démarrage, jusqu'au premier paquet.
- **Réglage LoRa attendu** : région et préréglage, saisis sur l'écran de diagnostic en mode PC
  seulement (choix dans les listes, reprise du réglage de ma radio, effacement), affichés à tous.
  Stocké sur le téléphone seulement, en texte (noms des énumérations du protobuf), par
  `ExpectedLoraPrefs` / `ExpectedLoraPrefsImpl` (préférences d'interface). Rien n'est envoyé aux
  radios. Le partager par le QR de l'organigramme demanderait un format `NTORGA2` (le décodeur
  `NTORGA1` refuse en entier un code qu'il ne connaît pas) : reporté à une tâche suivante.
- **Tester la liaison** : choix d'un nœud déjà entendu, puis Traceroute, la fonction existante de
  la fiche d'un nœud (`NodeRequestActions.requestTraceroute`). Confirmation obligatoire :
  temps d'antenne partagé par tout le réseau, réémis par chaque relais, limité par la
  réglementation dans certaines régions. Un seul traceroute par confirmation, jamais automatique
  ni périodique ; refusé si la radio n'est pas connectée ; puis 30 s d'attente, la même que sur la
  fiche d'un nœud (temps du dernier traceroute de l'application). Après l'envoi : « Demande remise
  à la radio à 14:05 », sans conclure de l'absence de réponse ; la réponse s'affiche comme pour
  tout traceroute. **La demande de position n'est pas proposée** : la fonction existante joint la
  position de ma radio datée de l'heure du téléphone, et non de son relevé.
- **Mode démo** : écran permis (`DemoRoutes`). Données fictives : PC-0 réglé EU_868 / LONG_FAST,
  le même réglage attendu (modifiable en mémoire seulement), batterie 76 %, dernier paquet il y a
  1 min ; ALPHA-1, CHARLIE-3 et BRAVO-2 entendus il y a 1, 5 et 20 min. « Tester la liaison » est
  simulé : rien n'atteint la radio. Le bandeau est calculé sur ces données seulement. Liaisons Koin
  ajoutées, dans `DiagnosticDemoModule` (inclus par `CoreDemoModule`, pour rester sous la limite
  detekt de 11 fonctions par classe) : `DemoExpectedLoraPrefs` et `DemoRadioActivityProvider` sous
  leur seule classe, `ExpectedLoraPrefs` et `RadioActivityProvider` sous `@Named(SCREEN_DATA)`.
  `DemoRadioConfigRepository` sert désormais la configuration LoRa de PC-0.
- **Autres changements** : route `NodesRoute.Diagnostic` ; `formatElapsed` (`core/ui`), le format
  d'âge de la vue PC ; `RadioActivityProvider` lié dans Koin sur Android et sur bureau ; textes
  `diag_*` en anglais et en français.
- **Tests** : `LinkDiagnosticsTest`, `LinkAlertsTest` (`core:model`) ;
  `ServiceRepositoryImplTest` (`core:service`) ; `ExpectedLoraPrefsImplTest` (`core:prefs`) ;
  `DiagnosticViewModelTest`, `LinkTestViewModelTest` (`feature:node`) ; `DemoDataSetTest`
  (diagnostic construit sur les données fictives), `ScreenFacadesTest`, `DemoRoutesTest`
  (`core:demo`). Tous ces modules tournent déjà dans `fork-apk`. `NavigationConfigTest`
  (`core:navigation`) couvre la route mais ne tourne pas dans `fork-apk` ; le bandeau
  (`LinkAlertViewModel`, `core:ui`) n'a pas de test propre, sa logique est dans `LinkAlerts`.

### 20. Écran de bienvenue : insignes des deux batteries (`feat/accueil-insignes`)

- **Bloc ajouté** (`WelcomeScreen`, `feature:intro`) sous les trois avantages, au-dessus de
  « Commencer » : l'insigne de la 4ème batterie à gauche, celui de la batterie d'acquisition
  spécialisée à droite, séparés par un trait vertical fin, chacun avec sa légende ; en dessous,
  centré, « NetworkTact, un projet d'innovation porté conjointement par deux batteries. » Le logo,
  les avantages et le bouton ne changent pas.
- **Affichage** : 88 dp de haut pour les deux insignes, couleurs d'origine (aucune teinte) en
  thème clair et sombre. Légendes et phrase en couleur de texte secondaire (`onSurfaceVariant`) :
  contraste de 6,6:1 en clair et 9,4:1 en sombre sur le fond de l'application. La colonne défile
  sur les petits écrans ou avec une police agrandie ; « Commencer » reste dans la barre du bas.
- **Ressources** : `insigne_4e_batterie.png` (320 × 512) et `insigne_bas.png` (567 × 512), PNG
  transparents, dans `core/resources` (`composeResources/drawable`), octets d'origine sans
  recompression : environ 456 Kio de plus dans l'APK. Textes `intro_insignia_*` en anglais et en
  français, avec les descriptions d'accessibilité des deux insignes.
- **Mode démo** : rien de nouveau ; le bloc n'affiche que des images et des textes fixes.
- **Réafficher l'écran** : Réglages, section Information, « Afficher l'introduction » (`intro_show`)
  remet seulement l'indicateur d'introduction terminée ; nœuds, messages et réglages sont gardés
  (ligne verrouillée en mode démo).

### 21. Diffusion du nom après un changement (`fix/diffusion-nom`)

- **Défaut corrigé** (essai du 8 novembre 2026) : après un changement de nom long ou court
  (Réglages de l'appareil → Utilisateur) ou d'équipe (Réglages → Équipes), la radio enregistre le
  nouveau nom, mais les autres postes gardent l'ancien ; seul « Infos utilisateur » sur la fiche du
  nœud, poste par poste, le corrigeait.
- **Cause, vérifiée dans le firmware Meshtastic** (2.7.9 et 2.8.3, `AdminModule::handleSetOwner`,
  `MeshService::reloadOwner`, `NodeInfoModule::allocReply`) : à un changement de nom, le firmware
  diffuse bien le nouveau `User`, mais seulement s'il n'a pas diffusé de NodeInfo récemment (5 min en
  2.7 ; au moins 30 min en 2.8, délai conservé à travers le redémarrage qui suit le changement). Sinon
  il ne diffuse rien jusqu'à sa prochaine annonce périodique.
- **Pourquoi la diffusion par le téléphone marche** (même vérification, `PhoneAPI`, `Router`,
  `NodeInfoModule::handleReceivedProtobuf`, `NodeDB::updateUser`) : un paquet `NODEINFO_APP` venu
  du téléphone n'est soumis à aucune limite de fréquence ; le firmware l'émet avec le numéro de la
  radio comme expéditeur (et le signe en 2.8, comme ses propres diffusions) ; chaque nœud qui le
  reçoit enregistre le nouveau nom et le transmet à son téléphone. Condition : la charge utile porte
  la clé publique de la radio, sinon un nœud qui la connaît déjà rejette le paquet. Aucune
  alternative n'a donc été nécessaire.
- **Diffusion automatique** : après un changement réussi de nom ou d'équipe fait depuis ce
  téléphone, sur ma propre radio, l'application attend que la radio rapporte les nouveaux noms (elle
  applique le `User`, redémarre, puis se reconnecte), puis diffuse une fois ce `User`, tel que la
  radio le rapporte (clé publique comprise) : paquet `NODEINFO_APP` en broadcast, canal principal,
  sans `want_response` (sinon chaque nœud répondrait) ni accusé. Abandon, avec un message, si la
  radio n'a pas rapporté le nouveau nom en 3 minutes. Rien n'est diffusé quand seuls d'autres champs
  changent, ni en mode radioamateur (le firmware compose lui-même le nom long), ni pour une radio
  administrée à distance.
- **Bouton « Diffuser mon nom »**, dans Réglages de l'appareil → Utilisateur (ma radio seulement)
  et dans Réglages → Équipes, sous le choix d'équipe : confirmation obligatoire (temps d'antenne
  partagé), un seul envoi, refus si la radio n'est pas connectée ou si son nom n'est pas encore
  connu, puis 30 s de blocage avec décompte, comme « Diffuser ma position maintenant ». Le blocage
  est commun aux deux écrans et à la diffusion automatique, qui attend sa fin si besoin.
- **Mode démo** : le choix d'équipe renomme le nœud fictif, la diffusion automatique et le bouton
  sont simulés (« diffusion simulée, rien n'est envoyé ») avec la même confirmation et le même
  blocage ; aucun paquet n'est remis à la radio.
- **Code** : règle et paquet dans `core:model` (`identity/NameBroadcast`), diffusion partagée et
  carte dans `feature:settings` (`identity/NameBroadcaster`, `NameBroadcastViewModel`,
  `NameBroadcastCard`), branchements dans `UserConfigScreen` et `TeamsScreen` / `TeamsViewModel`.
  Textes `name_broadcast_*` en anglais et en français. Tests JVM : `NameBroadcastTest`,
  `NameBroadcasterTest`, `TeamsViewModelTest`.
- **CI** : `fork-apk` se déclenche désormais aussi sur les poussées vers `fix/**` et `docs/**`.

### 22. Un seul mode démo (`fix/demo-unique`)

- **Défaut corrigé** (essai du 8 novembre 2026, D1) : la liste des appareils (onglet Ma radio)
  proposait encore « Mode Démo » et « Mode démo (Rejouer) », la fausse radio héritée de Meshtastic
  (nœuds Demo Handset, Riverside Base…). Elle remplaçait la vraie radio, n'avait pas de bouton
  Quitter, et se confondait avec le mode démo de NetworkTact (section 9, interrupteur dans Réglages).
- **Correction** : la porte `mockTransportEnabled` de `BaseRadioTransportFactory` (`core:network`)
  reste fermée. Elle commandait deux choses, qui lisent la même valeur : l'affichage des entrées
  dans la liste (la variante « Rejouer » n'existe qu'à l'intérieur) et l'admission des adresses `m`
  et `r`. Une ancienne sélection mémorisée ou un lien profond vers ces adresses est donc refusé
  aussi, sans retombée sur la fausse radio. L'ancienne règle de `AndroidRadioTransportFactory`
  (ouverte en debug, après cinq appuis sur la version en release) est supprimée. La version bureau
  garde la sienne, hors périmètre.
- **Inchangé** : le mode démo de la section 9 (Réglages → Mode démo, bandeau, bouton Quitter) est
  le seul mode démo. Aucun texte ajouté : les textes `demo_mode*` hérités ne sont plus affichés.
- **Tests** : `NoInheritedDemoTransportTest` (porte fermée par défaut, `m`, `r` et `m1` refusés
  même si la capture existe, radios réelles admises) ; `MockTransportAddressAdmissionTest` et les
  tests de `ScannerViewModel` continuent de couvrir le comportement quand la porte est ouverte.
- **CI** : run 79 vert sur `fix/demo-unique` (lint, compilation, tests dont `NoInheritedDemoTransportTest`,
  APK). Le crochet `.claude/hooks/pre-bash-guard.sh` laisse désormais passer la poussée, avec un avertissement,
  quand Gradle ne peut pas démarrer pour une raison d'environnement (JDK ou dépôts inaccessibles) ; une vraie
  violation detekt bloque toujours.

### 23. Formulaire « Fait observé » : taille, veille, clavier (`fix/fait-observe`)

- **Défauts relevés** (essai du 8 novembre 2026) : D3, une description très longue partait en
  entier au lieu d'afficher la taille et de griser le bouton au-delà de 200 octets ; D4, une
  heure pas encore atteinte (23h50 saisi vers 18 h) n'était pas marquée « (veille) » ; D5, le
  clavier de la description ne se fermait qu'en touchant le champ des coordonnées.
- **Constat avant correction** : la règle (`observedFactDraft`, `ObservedTime.of`) était déjà
  juste. Les tests ajoutés à 18 h (« 23h50 », « 23:50 », « 2350 », « 23 50 » donnent
  `23:50 (veille)` ; 17h59 et 18:00 restent du jour ; 18:01 passe à la veille) et à la frontière
  de taille (200 octets acceptés, 201 refusés, rien n'est coupé) passent sans changement de la
  règle. Les défauts étaient donc dans l'affichage, et non reproduits sur le code seul : seul
  l'essai sur téléphone confirmera. Ce qui a été changé :
  - **Taille** : l'indication « Trop long : N/200 octets. Raccourcir la description. » est
    maintenant sous le champ de description (qui devient rouge), et non plus en bas du
    formulaire défilant, hors de vue avec le clavier ouvert. Elle s'affiche aussi avant que le
    lieu soit complet : tant que le lieu ou l'heure ne se lit pas, la taille est estimée avec un
    lieu MGRS typique (`ObservedFactDraft.sizeIsEstimate`). Le bouton « Insérer » reste grisé
    au-delà de 200 octets ; rien n'est jamais raccourci.
  - **Veille** : règle inchangée, testée sous toutes les formes de saisie. Le message sous le
    champ d'heure et le rapport affiché la disent.
  - **Clavier** : la description et l'heure ont l'action « Terminé » (`ImeAction.Done`), qui
    ferme le clavier. Le rapport tient sur une ligne (les blancs sont réduits), donc le retour à
    la ligne n'est plus proposé.
- **Mode démo** : le formulaire ne touche pas à la radio (il remplit seulement la zone de saisie) ;
  il se comporte pareil en démo, rien n'est envoyé.
- **Textes** : aucun texte nouveau, les textes existants `coords_fact_*` (anglais et français)
  suffisent. Le fichier français est protégé en lecture dans ce dépôt, donc non modifié.
- **Code** : `ObservedFactDraft` (`tooLong`, `sizeIsEstimate`), `ObservedFactDialog`
  (`DescriptionField`). Tests JVM : `ObservedFactDraftTest` (14 tests). Vérifié en local : spotless,
  detekt, tests JVM de `feature:messaging`. La variante Android n'a pas été compilée en local (pas de
  SDK Android sur ce poste) : la CI le fait.

### 24. Textes français gérés par le fork, actions Node.js 24 (`fix/textes-fr`)

- **Textes français** : les traductions françaises (`values-fr/strings.xml`) sont gérées par le fork, plus par
  Crowdin. `.claude/settings.json` n'interdit donc plus leur lecture ni leur modification aux sessions Claude
  Code ; l'interdiction reste pour les autres langues, désormais écrite langue par langue (une langue ajoutée
  plus tard doit y être ajoutée). Les textes ajoutés par les sections 22 et 23 : aucun. Vérifié par script : les
  380 textes ajoutés par le fork à `values/strings.xml` ont tous un équivalent français ; les 17 identiques à
  l'anglais sont des mots communs aux deux langues (Description, Info, Urgent, HDOP, Traceroute…).
- **Actions de `fork-apk`** : `actions/checkout` v4 → v5, `actions/setup-java` v4 → v5,
  `gradle/actions/setup-gradle` v4 → v5, `actions/upload-artifact` v4 → v6. Chaque version exécute Node.js 24
  (`runs.using: node24` dans son `action.yml`), ce qui supprime l'avertissement « Node.js 20 is deprecated ».
  Aucun paramètre du workflow n'a changé. `upload-artifact` v5 aurait gardé Node.js 20, d'où v6.

### 25. Diagnostic : l'écart LoRa à côté du réglage attendu (`fix/diag-lora-attendu`)

- **Défaut relevé** (essai du 8 novembre 2026, D8) : sur l'écran de diagnostic en mode PC, un réglage LoRa
  attendu MEDIUM_FAST alors que la radio est en LONG_FAST n'affichait aucune pastille d'écart.
- **Constat avant correction** : la règle (`LinkDiagnostics.loraCheck`) et le trajet complet étaient justes.
  Un test avec le vrai `DiagnosticViewModel` et de faux dépôts (radio EU_868 · LONG_FAST, attendu
  EU_868 · MEDIUM_FAST) donne `LoraCheck.Different` avec `presetDiffers`, la suggestion « vérifier la région et
  le préréglage », et le passage à `Different` dès que l'attendu est modifié. Le défaut n'a pas été reproduit sur
  le code seul ; seul l'essai sur téléphone confirmera.
- **Cause retenue** : la pastille d'écart n'existait que dans la carte « Constats », en haut de l'écran, alors que
  le réglage attendu se modifie dans la carte « Réglage LoRa attendu », plus bas : après l'enregistrement, aucun
  écart n'était visible dans ce qu'on regardait.
- **Correction** : la carte « Réglage LoRa attendu » affiche elle aussi le résultat, sous le réglage : une pastille
  inversée (`statusAlert`) « Ma radio est en EU_868 · LONG_FAST : écart avec ce réglage. » quand ils diffèrent, et
  « Identique au réglage de ma radio. » quand ils sont identiques ; rien tant que la radio n'a pas envoyé sa
  configuration ou qu'aucun réglage n'est saisi. La carte « Constats » est inchangée.
- **Mode démo** : la radio fictive et le réglage attendu fictif sont identiques (EU_868 · LONG_FAST), la carte
  affiche donc « Identique » ; modifier l'attendu en démo fait apparaître l'écart, sans rien écrire dans le vrai
  réglage (`ScreenFacadesTest`).
- **Textes** : `diag_expected_gap` et `diag_expected_matches`, en anglais et en français.
- **Code** : `ExpectedLoraCard` (paramètre `lora`, `LoraGap`), `DiagnosticScreen`. Tests JVM :
  `DiagnosticViewModelTest` (deux tests de bout en bout). Lint (spotless, detekt) et tests de `feature:node` et
  `core:demo` passés en local.

### 26. Organigramme : taille de l'indicatif, radios connues (`fix/organigramme-saisie`)

- **D7** (essai du 8 novembre 2026) : un indicatif de 40 caractères était accepté sans message. Le modèle
  (`OrgChart.checkMember`) le refusait déjà au-delà de 39 octets (`MemberInput.CallsignTooLong`, testé), mais
  le refus n'apparaissait qu'après avoir touché « OK », en bas d'une boîte de dialogue qui ne défilait pas : hors de
  vue avec le clavier ouvert, on voyait seulement que rien ne se passait. Non reproduit sur le code seul.
  Correction : la taille est contrôlée à la frappe, sous le champ. L'indicatif (comme la fonction, limite 24 octets)
  devient rouge avec « Indicatif trop long : 40 octets, 39 au plus », le bouton « OK » est grisé tant que
  l'indicatif est vide ou trop long, et l'alerte d'un refus à l'envoi (doublon, QR trop grand) est en haut de la
  boîte, qui défile. Les octets comptés sont ceux de l'indicatif nettoyé, comme à l'enregistrement
  (`OrgChart.callsignBytes`, `roleBytes`) : « é » × 20 fait 40 octets.
- **A1** : à l'ajout d'un membre, la boîte propose « Radios connues » : l'indicatif (nom long sans suffixe d'équipe ni
  « (MQTT) ») de chaque radio connue, une fois chacun, la plus récemment entendue d'abord, sans les membres déjà
  présents (casse ignorée), sans les radios ignorées ni les noms qui dépasseraient 39 octets. Un indicatif tapé
  filtre la liste ; toucher une proposition remplit le champ. **La saisie libre n'est pas gênée** : n'importe quel
  indicatif se tape, y compris celui d'une radio éteinte, absente de la liste (par exemple PCR). Pas de liste à la
  modification d'un membre. Code : `Callsign.suggestions` (`core:model`), `OrgChartUiState.suggestions`,
  `KnownRadios` / `matchingSuggestions`.
- **Mode démo** : les propositions viennent des nœuds fictifs (ceux de la démo) ; rien n'est écrit dans le vrai
  organigramme (façade déjà en place, section 17).
- **Textes** : `orgchart_known_radios`, en anglais et en français ; les messages d'erreur existants sont réutilisés.
- **Tests** : `CallsignSuggestionsTest`, `OrgChartTest` (limite en octets, accents), `OrgChartStateTest`
  (propositions, filtrage). Lint (spotless, detekt) et tests de `core:model`, `feature:node` et `core:demo` passés
  en local.

### 27. Crédit d'une carte importée (`fix/carte-attribution`)

- **Défaut relevé** (essai du 8 novembre 2026, D6) : sur une carte `.pmtiles` Protomaps importée, le bouton ⓘ affichait
  « © OpenStreetMap » au lieu de « © contributeurs OpenStreetMap · Protomaps ».
- **Cause** : le style de la carte importée déclarait déjà le bon crédit (`offlineMapStyle`, source `protomaps`), mais
  la bibliothèque de carte (maplibre-compose 0.18.0, `MlnFfiStyleBinding.sourceDefinition`) prend d'abord l'attribution
  que le moteur lit dans le fichier lui-même, et ne retombe sur celle du style que si le fichier n'en déclare aucune.
  Un fichier dont les métadonnées disent seulement « © OpenStreetMap » l'emporte donc sur le style. Vérifié dans le
  code source de la bibliothèque, pas sur un téléphone.
- **Correction** : `Basemap.LocalVector` porte le crédit à imposer (`attribution`). `meshMapOrnaments(archiveCredit)`
  garde le logo et le bouton ⓘ de la bibliothèque, mais en remplace le contenu déplié
  (`ExpandingAttributionButton.expandedContent`) par ce crédit seul (`shownAttributions`), quelles que soient les
  métadonnées du fichier. Les cartes sans crédit imposé (fonds en ligne, autres formats) gardent les crédits que leur
  style ou leur fichier déclare.
- **Seulement pour un fond Protomaps** : le crédit « © contributeurs OpenStreetMap · Protomaps » (anglais : « ©
  OpenStreetMap contributors · Protomaps », texte `map_attribution_osm_protomaps`, déjà traduit) n'est imposé que si les
  métadonnées JSON du fichier le désignent comme tel : `name` égal à « Protomaps Basemap » (casse et espaces ignorés) ou
  `description` qui mentionne Protomaps (`PmTilesMetadata.isProtomapsBasemap`, `importedCredit`). Le fichier de Cincu a
  `name` = « Protomaps Basemap ». Toute autre carte `.pmtiles` garde **le crédit déclaré dans ses métadonnées**, ou
  **aucun** s'il n'en déclare pas : le style qu'on lui construit déclare alors un crédit vide au lieu de retomber sur celui
  de l'application, et le bouton ⓘ n'apparaît pas. C'est ce qu'il faut pour les cartes raster converties depuis des GeoTIFF
  (à venir), qui auront leur propre source.
- **Lecture des métadonnées** : `PmTilesHeader` donne maintenant l'emplacement de la section de métadonnées
  (`metadataOffset`, `metadataLength`) et la compression interne (octet 97). `OfflineMapLibrary.readMetadata`, appelée
  quand les cartes du téléphone sont listées, lit cette section (1 Mio au plus), la décompresse (aucune ou gzip, comme
  les fichiers Protomaps ; brotli, zstd ou inconnue : le fichier est alors traité comme ne déclarant rien) et
  `PmTilesMetadata.parse` en extrait `name`, `description` et `attribution`. Un fichier dont les métadonnées sont
  illisibles n'est donc pas pris pour un fond Protomaps.
- **Mode démo** : sans effet, la carte de démo n'a pas de fichier importé.
- **Textes** : aucun nouveau.
- **Tests** : `PmTilesMetadataTest` (un test par cas : fond Protomaps par son nom ou par sa description, autre fichier
  avec crédit déclaré, autre fichier sans crédit, métadonnées illisibles ; lecture gzip, non compressée, compression non
  gérée ou octets cassés), `PmTilesHeaderTest` (emplacement des métadonnées, compression), `OfflineMapFilesTest` (style
  sans crédit), `MapOrnamentsTest` (crédit imposé ou conservé, `LocalVector`). Lint (spotless, detekt) et tests de
  `feature:map` et `feature:map-maplibre` passés en local ; `OfflineMapLibrary` et `OfflineMapBasemaps` (variante
  fdroid) ne sont compilés que par la CI.

### 28. APK de release signé (`feat/apk-release`)

- **Ce qui change** : `fork-apk` construit aussi `fdroidRelease` (R8, non débogable, profil de démarrage), en dernière
  étape pour qu'un échec ne coûte ni l'APK de debug ni les résultats de tests, et publie l'artefact
  `apk-fdroid-release`. L'APK de debug (`apk-fdroid-debug`, identifiant `….fdroid.debug`) reste construit comme avant ;
  la release porte l'identifiant `APPLICATION_ID` de `config.properties` (`io.github.grtgratzq.networktact`), sans
  suffixe, alors que le debug garde `com.geeksville.mesh.fdroid.debug` : Android les voit comme deux applications
  (voir « Installation et passage depuis la version debug » plus bas).
- **Durée** : un run vert dure environ 27 minutes avant cette tâche ; la construction de la release s'y ajoute, et la
  limite du job passe de 40 à 60 minutes (celle de l'étape : 30 minutes).
- **Signature** : si les quatre secrets existent, l'étape décode la clé dans un fichier temporaire hors du dépôt
  (`$RUNNER_TEMP`, droits 600), vérifie avec `keytool` que le fichier, le mot de passe et l'alias sont bons (sinon l'étape s'arrête en affichant la
  cause : « keystore password was incorrect », « Alias <…> does not exist », fichier illisible ; `keytool` écrit ses erreurs
  sur la sortie standard, qu'une première version du script jetait : run 85 rouge sans message), écrit
  `keystore.properties` (ignoré par Git, relu par `androidApp/build.gradle.kts`), construit, puis supprime les deux
  fichiers à la fin du run. Aucun secret n'est affiché, et rien n'en révèle non plus d'information (ni longueur, ni empreinte du
  fichier décodé) : en cas d'échec, seul le message de `keytool` apparaît. Une version intermédiaire du workflow
  (commit `7b34a16`, runs 85 à 87) affichait ces longueurs pour trouver la cause de l'échec ; elle a été retirée une
  fois la cause connue (voir « Signature et passation »). L'étape « Check the release APK » affiche la taille de
  l'APK, vérifie que le profil de démarrage est dedans, puis, si l'APK est signé, lance `apksigner verify` et n'en
  garde que la vérification (`Verifies`, `Verified using v2 scheme … true`) et l'empreinte SHA-256 du certificat
  (public : à comparer avec celle de la clé du porteur). **Si un des secrets est vide** (un fork, par exemple), l'APK est construit **non signé** avec une
  note dans le journal, sans faire échouer le run ; un APK non signé ne s'installe pas.
- **R8** : les règles de la variante fdroid sont celles de toutes les variantes (`androidApp/proguard-rules.pro`,
  `config/proguard/shared-rules.pro`, déjà éprouvées en `googleRelease`) plus les règles que chaque bibliothèque livre
  avec elle. Le moteur de carte natif de la variante fdroid (maplibre-compose 0.18.0 et maplibre-native-ffi
  0.202609.4, JNI via JavaCPP) en livre trois, lues dans les AAR : conservation du pont Vulkan, du paquet JavaCPP et du
  vérificateur de certificats rustls. Le code propre à la variante (`androidApp/src/fdroid`) s'appuie sur Koin
  Annotations (code généré, pas de réflexion). Aucune règle n'a donc été ajoutée. **Non vérifié** : la CI prouve que R8
  passe (sans classe manquante) ; elle ne lance pas l'application. Seul l'essai de l'APK sur téléphone (carte,
  import d'un `.pmtiles`, QR, Bluetooth) dira si R8 casse quelque chose à l'exécution (`TESTS-A-FAIRE.md` §16). En cas
  de plantage, `androidApp/build/outputs/mapping/r8-merged-config.txt` (écrit par la construction) donne la
  configuration complète.
- **Profil de démarrage** : le profil existant (`androidApp/src/googleRelease/generated/baselineProfiles`, 45 997
  lignes, produit par `:baselineprofile`, qui n'a que la variante google) peut servir à `fdroidRelease`, avec une
  couverture partielle : environ 60 % des lignes (androidx, kotlin, code partagé `org.meshtastic`) existent aussi côté
  fdroid ; Google Maps (7 %), Firebase (5 %) et Datadog (9 %) n'y sont pas et sont ignorés à l'installation. Le code
  propre à fdroid (MapLibre) n'est pas couvert. `androidApp/build.gradle.kts` donne donc ce dossier au jeu de sources
  `fdroidRelease` (`baselineProfiles.srcDir`). Pour un profil complet : ajouter la variante fdroid à
  `:baselineprofile` et le générer sur un appareil ou un émulateur (Gradle Managed Device, avec accélération matérielle,
  possible sur un exécuteur GitHub ; sinon sur un téléphone branché), puis committer le fichier produit. À décider
  plus tard.
- **`DEBUG_KEYSTORE_BASE64`** (secret d'une tâche abandonnée en octobre) : **n'est utilisé nulle part** : introuvable dans les fichiers du dépôt, dans
  l'historique et dans les 19 branches locales et distantes ; aucun workflow ne le lit (les secrets de signature de
  `release.yml` hérité de Meshtastic sont `KEYSTORE`, `KEYSTORE_FILENAME` et `KEYSTORE_PROPERTIES`). Les secrets du
  dépôt ne peuvent pas être listés d'ici : c'est à confirmer dans Settings → Secrets, où il peut être supprimé sans
  conséquence pour le code. Rien n'a été supprimé.

#### Installation et passage depuis la version debug

- La release s'installe sous l'identifiant **`io.github.grtgratzq.networktact`** ; la version debug, sous
  `com.geeksville.mesh.fdroid.debug`. Android les traite comme deux applications distinctes, chacune avec ses propres
  données : rien n'est repris de l'une à l'autre.
- **Il faut donc désinstaller la version debug** (`com.geeksville.mesh.fdroid.debug`) avant de passer à la release :
  techniquement les deux peuvent coexister, mais deux applications cherchant la même radio en Bluetooth se gênent, et
  la debug garderait des données qui ne servent plus.
- **Après l'installation, tout est à refaire** dans la release : la liste d'équipes, l'organigramme (réimport du
  QR ou ressaisie) et les réglages ; les cartes `.pmtiles` importées sont aussi à réimporter. Les radios, elles, ne
  changent pas.
- Une mise à jour de la release par-dessus une release n'est possible que si elle est signée avec la **même clé** :
  d'où la garde de la clé (voir plus bas). Un APK non signé ne s'installe pas.

#### Signature et passation

- **Les quatre secrets du dépôt** (Settings → Secrets and variables → Actions) :

  | Secret | Contenu |
  |---|---|
  | `NT_KEYSTORE_BASE64` | le fichier PKCS12 de la clé, encodé en base64 sur une seule ligne |
  | `NT_KEYSTORE_PASSWORD` | le mot de passe du fichier |
  | `NT_KEY_PASSWORD` | le mot de passe de la clé (le même, en général, avec PKCS12) |
  | `NT_KEY_ALIAS` | l'alias de la clé : `networktact` |

- **Les recréer dans un autre dépôt** :
  1. Créer la clé, une seule fois, en saisissant le mot de passe quand `keytool` le demande (format PKCS12, alias
     `networktact`). **Lettres et chiffres seulement** : un accent fait échouer PKCS12, et une saisie dans le navigateur peut altérer les guillemets, les
     apostrophes et les tirets (le secret ne vaut alors plus le mot de passe du fichier : « keystore password was
     incorrect » : incident des runs 85 à 87, résolu au run 87 avec un mot de passe en lettres et chiffres). Le fichier de référence est `networktact-release-v2.p12` :
     ```bash
     keytool -genkeypair -v -storetype PKCS12 -keystore networktact-release-v2.p12 -alias networktact \
       -keyalg RSA -keysize 4096 -validity 10000
     ```
  2. Encoder le fichier (macOS) : `base64 -i networktact-release-v2.p12 | tr -d '\n' | pbcopy` ; Linux :
     `base64 -w0 networktact-release-v2.p12`.
  3. Dans le dépôt, créer les quatre secrets du tableau (interface web, ou
     `gh secret set NT_KEYSTORE_PASSWORD` qui demande la valeur sans l'écrire dans l'historique du terminal).
  4. Lancer `fork-apk` (poussée vers `fix/**`, `feat/**` ou `docs/**`, ou lancement manuel) : l'étape « Check the
     release APK » doit afficher `Verified using v2 scheme … true` et une ligne `certificate SHA-256 digest` : elle
     doit être **égale, aux deux-points près, à** l'empreinte attendue ci-dessous (c'est la même, sans les deux-points,
     en minuscules). Empreinte SHA-256 du certificat
     de la clé de référence (public) :
     `F4:AD:3E:41:C3:54:6D:59:6D:A1:47:62:AB:58:47:4F:E8:06:00:10:E5:3B:24:FB:B4:D4:01:89:79:20:58:18`.
- **Construction locale signée** : créer, à la racine du dépôt, `keystore.properties` (déjà dans `.gitignore`, ne jamais
  le committer) :
  ```properties
  storeFile=/chemin/absolu/vers/networktact-release-v2.p12
  storePassword=<mot de passe du fichier>
  keyAlias=networktact
  keyPassword=<mot de passe de la clé>
  ```
  puis `./gradlew :androidApp:assembleFdroidRelease` ; l'APK est dans `androidApp/build/outputs/apk/fdroid/release/`.
  Sans `keystore.properties`, la construction réussit mais l'APK est non signé (jamais signé avec la clé de debug,
  qui est publique).
- **Le fichier de la clé et son mot de passe sont gardés hors du dépôt par le porteur du projet, dans son dossier de
  passation.** Aucun mot de passe n'est écrit dans ce dépôt, ni dans un fichier, ni dans un message de commit. Perdre
  la clé interdit toute mise à jour des applications déjà installées (Android refuse une mise à jour signée par une
  autre clé) : il faut en garder au moins deux copies, hors du dépôt.

### 29. Téléchargement d'une carte hors ligne : plan et sonde (`feat/carte-telechargement`)

- **État** : **plan validé ; extracteur écrit et testé, sans écran**. `docs/PLAN-CARTE-TELECHARGEMENT.md` décrit l'extracteur (lecture par
  requêtes `Range` d'une édition Protomaps, écriture d'un `.pmtiles` v3), le rectangle à poignées, la source réglable et les
  décisions D1 à D11. Rien n'est codé avant la validation des résultats de la sonde.
- **Sonde** (`scripts/carte-sonde/`, workflow `fork-carte-sonde`, lancé à la main ou par poussée de ses fichiers sur cette branche) :
  lit l'en-tête, les métadonnées et les répertoires d'une vraie édition, sans télécharger de tuile ni utiliser de secret.
  Résultats du 11 novembre 2026 (détail au §15 du plan) : édition de 139,4 Go, `Range` accepté (206, ETag, `If-Match` 412),
  plages multiples refusées (400), sept éditions conservées seulement, schéma 4.15.2 aux couches identiques au style embarqué,
  zone de Cincu (30 km, zoom 15) : 12,7 Mo de tuiles, 1,1 Mo de répertoires à lire.
- **Extracteur** (`feature/map/src/commonMain/.../offline/extract/`, logique commune sans Android, 78 tests en `commonTest`) :
  identifiants de tuile et couverture d'un rectangle, répertoires et en-tête PMTiles v3, plan d'extraction et fusion des
  plages, source HTTP par plages (Ktor, `If-Match`, essais 1/3/9 s, `User-Agent` neutre `pmtiles-client/1.0` : le serveur source n'apprend pas quelle application demande, en plus de l'adresse et de la zone qu'il voit déjà), écriture à sa place
  avec journal de reprise et contrôle final, orchestration parallèle, édition la plus récente sans date codée en dur,
  contrôle du schéma, estimation grossière. **Pas encore d'écran** : l'intégration au téléphone vient ensuite.
- **Essai sur la vraie édition** (workflow `fork-carte-interop`) : le même code, compilé seul, extrait Cincu (30 km, zoom 15) ;
  `pmtiles verify` réussi ; **identique tuile par tuile à `pmtiles extract`** (6 822 tuiles, 14,65 Mo) ; détail au §15 du plan.
- **CI** : `fork-apk` ne se lance plus pour une poussée qui ne change que des fichiers `.md`, ni pour les fichiers de la sonde et
  de l'essai d'interopérabilité ; le lancement manuel est inchangé.
- **Licence** : `pmtiles-reader` (déjà dans l'APK) est BSD 3-Clause d'après son POM, « MIT » seulement dans des commentaires du
  dépôt ; les deux sont compatibles avec la GPL-3.0.

## État d'avancement au 11 novembre 2026

- **Branches** : les sections 1 à 20 sont chacune sur une branche `feat/…`, empilées les unes
  sur les autres ; la dernière, `feat/accueil-insignes` (commit `9196e76`), contient tout leur code.
  La branche `docs/essais-2026-11-08`, partie de `feat/accueil-insignes`, n'ajoute que de la
  documentation : `TESTS-A-FAIRE.md`, `HISTORIQUE.md`, `FLASHER-LA-RADIO.md`,
  `PREPARER-LES-RADIOS.md` et ce fichier. La branche `fix/diffusion-nom` (section 21), partie de
  `docs/essais-2026-11-08`, contient tout jusqu'à la section 21. La branche `fix/demo-unique` (section 22), partie de
  `fix/diffusion-nom`, puis `fix/fait-observe` (section 23), partie de `fix/demo-unique`, puis `fix/textes-fr` (section 24), partie de
  `fix/fait-observe`, puis `fix/diag-lora-attendu` (section 25), puis `fix/organigramme-saisie` (section 26), puis `fix/carte-attribution` (section 27), puis `feat/apk-release` (section 28), puis `feat/carte-telechargement` (section 29, documentation et sonde seulement), la dernière, qui contient
  tout. Rien n'est fusionné dans `main`,
  qui ne porte que l'identité du fork et le workflow `fork-apk`. Aucune pull request n'est ouverte,
  ni vers `main` ni vers `meshtastic/Meshtastic-Android`.
- **CI** : `fork-apk` est vert sur `feat/accueil-insignes` (run 72, 23 min 38 s). Les runs 70 et 71
  ont été annulés parce qu'une poussée plus récente est arrivée sur la même branche ; le run 68,
  rouge, était une erreur de compilation de l'écran de diagnostic, corrigée au run 69. Bilan des
  72 runs : 38 verts, 23 rouges, 11 annulés (détail dans `HISTORIQUE.md`). Depuis : run 73 vert sur
  `docs/essais-2026-11-08` (lancé à la main, même code que le run 72) ; runs 74 à 76 rouges sur
  `fix/diffusion-nom`, tous au lint (formatage ktfmt d'un `when` et d'un commentaire, puis detekt
  `ViewModelForwarding`), chacun corrigé par un commit ; run 77 vert (lint, compilation, tests dont
  `NameBroadcastTest` et `NameBroadcasterTest`, APK), 18 min. Run 79 vert sur `fix/demo-unique` (section 22), run 80 vert sur `fix/fait-observe` (section 23). Runs suivants (tous sur la branche indiquée) : 78 vert (`fix/diffusion-nom`, documentation) ; 79 vert (`fix/demo-unique`) ; 80 vert (`fix/fait-observe`, section 23) ; 81 vert (`fix/textes-fr`, section 24) ; 82 vert (`fix/diag-lora-attendu`, section 25) ; 83 vert (`fix/organigramme-saisie`, section 26) ; 84 vert (`fix/carte-attribution`, section 27) ; sur `feat/apk-release` (section 28) : 85 rouge, 86 rouge, 87 vert (relancé trois fois), 88 vert (documentation). Les échecs des runs 85 et 86 venaient d'un caractère du mot de passe modifié à la saisie dans GitHub (« keystore password was incorrect »), pas de la version de Java ; le run 87 a construit l'APK de debug et l'APK de release signé. Le diagnostic de la clé (longueurs, empreinte du fichier, version de Java) a ensuite été retiré du workflow : plus aucune information sur les secrets n'est affichée, et le contrôle de l'APK n'affiche que la signature et l'empreinte du certificat.
  Le lint (spotless, detekt) tourne en local depuis le 9 novembre : accès réseau ouvert et miroir Maven Central
  en script d'initialisation Gradle hors du dépôt. Aucune compilation locale n'a été
  possible pour la section 21 (pas d'accès aux dépôts Maven) : seule la CI l'a vérifiée.
- **Vérifié** : lint (spotless, detekt), compilation fdroid debug, tests Android et JVM des modules
  listés dans chaque section, contrastes des couleurs ajoutées.
- **Pas encore vérifié sur téléphone** : tout l'usage réel. En particulier, la lecture du QR
  d'organigramme à la caméra, l'import d'un `.pmtiles` réel, le recalage et la diffusion de position
  avec une vraie radio, le traceroute de « Tester la liaison », les bandeaux de diagnostic,
  l'affichage de l'écran de bienvenue sur de petits écrans, et la diffusion du nom (section 21) entre
  deux radios, la disparition de l'ancien « Mode Démo » de la liste des appareils (section 22), et le
  formulaire « Fait observé » (taille, veille, clavier, section 23) l'écart LoRa du diagnostic (section 25), la saisie de l'organigramme (section 26), le crédit d'une carte importée (section 27), et l'APK de release signé (section 28).
  La liste des essais est dans
  `TESTS-A-FAIRE.md`.
- **Non couvert par la CI** : la variante google, `core:ui` (dont le bandeau de diagnostic),
  `core:navigation` (`NavigationConfigTest`) et les cibles iOS (`ios-check.yml`, à la main).
- **Reporté** : partage du réglage LoRa attendu par le QR d'organigramme (format `NTORGA2`) ;
  authentification des listes d'équipes et des organigrammes ; ouverture de pull requests vers `main`.
- **Documents** : historique complet dans `HISTORIQUE.md` (chronologie, structure des branches, les
  72 runs et chaque commit) ; essais à faire dans `TESTS-A-FAIRE.md` ; carte hors ligne dans
  `CARTE-HORS-LIGNE.md`.
- **Radios** : remise à zéro dans `FLASHER-LA-RADIO.md`, puis réglages à faire dans NetworkTact
  (LoRa `EU_868` / `LongFast`, GPS et champ de position `TIMESTAMP`, nom long = indicatif seul, nom
  court, équipe) dans `PREPARER-LES-RADIOS.md`. Le firmware reste celui de Meshtastic, non modifié.

## Marques

Fonctionne avec les nœuds Meshtastic®. Meshtastic® est une marque déposée de Meshtastic LLC.
NetworkTact n'est ni affilié au projet Meshtastic ni approuvé par celui-ci.
