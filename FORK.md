# NetworkTact

NetworkTact est un fork de [Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android),
modifié à partir du 26 septembre 2026 (dernière mise à jour de ce fichier : 8 octobre 2026, branche `feat/carte-hors-ligne`).

- **Licence** : GPL-3.0-or-later (voir le fichier `LICENSE`, inchangé).
- **Origine** : code de Meshtastic-Android, © Meshtastic LLC. Les mentions de copyright de
  Meshtastic LLC présentes dans les fichiers d'origine sont conservées, ainsi que l'écran
  À propos (mention Meshtastic LLC et lien vers le code source) et l'écran des licences.
- **Code source** : https://github.com/GRTGRATzq/NetworkTact
- **Radios** : le firmware des radios n'est pas modifié. Aucun nouveau protocole radio ni
  nouveau type de paquet : toutes les conventions ci-dessous sont du texte ordinaire, lisible
  par l'application Meshtastic officielle. Seule exception, le partage d'un point sur la carte
  (section 12) utilise le point de repère (waypoint) que Meshtastic sait déjà envoyer.

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
| Conversations, une conversation, convertisseur de coordonnées | données fictives |
| Vue PC (l'onglet Nœuds s'ouvre dessus), Équipes | données fictives |
| Réglages du téléphone | fictifs. Réglages directs grisés « Indisponible en mode démo », sauf Terrain/PC (gardé en mémoire, le vrai choix est rétabli à la sortie) |
| À propos, Remerciements, Aide | inchangés |
| Carte (variante fdroid, depuis la section 14) | nœuds et points fictifs ; point GPS du téléphone, création et partage de points de repère, Site Planner masqués |
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
  `ConnectionStateProvider`, `UiPrefs`, `RadioConfigUseCase`.

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

## Marques

Fonctionne avec les nœuds Meshtastic®. Meshtastic® est une marque déposée de Meshtastic LLC.
NetworkTact n'est ni affilié au projet Meshtastic ni approuvé par celui-ci.
