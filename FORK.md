# NetworkTact

NetworkTact est un fork de [Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android),
modifié à partir du 26 septembre 2026 (dernière mise à jour de ce fichier : 6 octobre 2026, branche `feat/navigation`).

- **Licence** : GPL-3.0-or-later (voir le fichier `LICENSE`, inchangé).
- **Origine** : code de Meshtastic-Android, © Meshtastic LLC. Les mentions de copyright de
  Meshtastic LLC présentes dans les fichiers d'origine sont conservées, ainsi que l'écran
  À propos (mention Meshtastic LLC et lien vers le code source) et l'écran des licences.
- **Code source** : https://github.com/GRTGRATzq/NetworkTact
- **Radios** : le firmware des radios n'est pas modifié. Aucun nouveau protocole radio ni
  nouveau type de paquet : toutes les conventions ci-dessous sont du texte ordinaire, lisible
  par l'application Meshtastic officielle.

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
| Liste des nœuds, fiche d'un nœud, carte, connexions, configuration radio et modules, administration à distance, et tout autre écran | « Indisponible en mode démo » |

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

## Marques

Fonctionne avec les nœuds Meshtastic®. Meshtastic® est une marque déposée de Meshtastic LLC.
NetworkTact n'est ni affilié au projet Meshtastic ni approuvé par celui-ci.
