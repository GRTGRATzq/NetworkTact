# NetworkTact

NetworkTact est un fork de [Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android),
modifié à partir du 26 septembre 2026 (dernière mise à jour de ce fichier : 3 octobre 2026, branche `feat/coordonnees`).

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

## Marques

Fonctionne avec les nœuds Meshtastic®. Meshtastic® est une marque déposée de Meshtastic LLC.
NetworkTact n'est ni affilié au projet Meshtastic ni approuvé par celui-ci.
