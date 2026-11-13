# Audit : passage de 5 à plus de 50 radios

Audit sans code, du 13 novembre 2026. Il porte sur la branche `docs/guide-utilisateur` (commit `c330b66`), qui contient
tout le fork (`FORK.md` § 1 à 30). Radios : LILYGO T-Beam Supreme, firmware Meshtastic non modifié. L'application a été
essayée avec 5 radios ; le but est un maillage de plus de 50 radios. Chaque fonction est évaluée à 50, 100 et 200 nœuds.

Rien n'a été codé. Les chiffres viennent de trois sources :

- le code du fork, lu à ce commit ;
- le code source du firmware Meshtastic aux versions **2.7.26** (`v2.7.26.54e0d8d`) et **2.8.2** (`v2.8.2.d5f1619`),
  lu fichier par fichier (liens au § 9) ;
- un calcul du temps d'antenne avec la formule LoRa de Semtech (§ 3).

Quand un chiffre est une **estimation**, le texte le dit et donne ses hypothèses. Aucun essai à plus de 5 radios n'a eu
lieu.

## 0. Résumé

| Fonction (section de `FORK.md`) | 50 nœuds | 100 nœuds | 200 nœuds |
|---|---|---|---|
| Organigramme par QR (§ 17, 26) | **bloquant** | **bloquant** | **bloquant** |
| Vue PC (§ 3) | gênant | gênant | gênant, presque inutilisable |
| Diffusion automatique du nom (§ 21) | gênant | gênant | **bloquant** si beaucoup de radios changent d'équipe en même temps |
| Liste `[EQUIPES]` (§ 6) | sans effet | sans effet | gênant au-delà de 11 à 20 équipes |
| Listes de choix (Tester la liaison, Radios connues) (§ 19, 26) | sans effet | gênant | gênant |
| Diagnostic de liaison (§ 19, 25) | sans effet | sans effet | sans effet (traceroute : discipline) |
| Ma position, fait observé, partage de point (§ 7, 12, 15) | gênant si usage fréquent | gênant | gênant |
| Demande de tir (§ 30) | sans effet en MEDIUM_FAST, gênant en LONG_FAST | gênant | **bloquant** en LONG_FAST, et si la base de nœuds déborde |
| Notifications (§ 30 et application d'origine) | gênant | gênant | gênant |
| Carte : étiquettes, points partagés (§ 12 à 15) | sans effet | gênant | gênant |
| Mode démo (§ 9, 22) | sans effet | sans effet | sans effet |
| Autres sections (§ 1, 2, 4, 5, 8, 10, 11, 13, 16, 18, 20, 22 à 24, 27 à 29) | sans effet | sans effet | sans effet |

Ce qui limite le plus n'est pas dans le fork : c'est **le préréglage LONG_FAST**. Au-delà d'environ 50 radios
(estimation, § 3), il remplit le canal, et les relais atteignent la limite légale de 10 % d'émission par heure. Le
firmware les fait alors taire.

Trois actions comptent le plus :

1. **Réglages radio**, sans code (§ 7) : MEDIUM_FAST (ou MEDIUM_SLOW) après un essai de portée, canal privé, rôles
   choisis, relais `LOCAL_ONLY`, radio du PC en favori sur toutes les radios.
2. **Organigramme en plusieurs QR**, ou par fichier, avec la limite de 40 membres relevée (correction C1).
3. **Seuils de fraîcheur de la vue PC** adaptés aux intervalles réels du firmware, et filtre par équipe (C2, C3).

## 1. Méthode et hypothèses

- **Radio** : la T-Beam Supreme a un ESP32-S3 avec 8 Mo de mémoire flash (ESP32-S3FN8). Le firmware fixe la taille de
  la base de nœuds d'après cette flash (§ 2.1). À confirmer sur une radio : le journal série indique la taille.
- **Firmware** : `FORK.md` § 21 mentionne les versions 2.7.9 et 2.8.3. Les valeurs ci-dessous sont lues dans les
  versions 2.7.26 et 2.8.2, les dernières étiquetées dans le dépôt du firmware. Elles sont identiques entre les deux,
  sauf mention « 2.8 ».
- **Temps d'antenne** : il dépend de la taille du paquet sur l'air. Ce qui est ajouté au contenu (en-tête Meshtastic
  de 16 octets et enveloppe protobuf) est estimé : environ 24 octets pour un accusé, 60 pour une position, 120 pour
  un NodeInfo (nom et clé publique), 225 pour un texte de 200 octets.
- **Diffusion par inondation** : on suppose qu'un paquet diffusé est émis en moyenne **3 fois** à portée d'un même
  point (l'auteur et deux relais). C'est l'hypothèse la plus incertaine. Elle dépend du terrain et de la répartition
  des radios. Un déploiement très dispersé fait moins, un groupe serré où tout le monde s'entend aussi. Le pire cas
  est une zone moyennement dense avec beaucoup de relais.

## 2. Ce que le firmware et l'application d'origine imposent

### 2.1 Taille de la base de nœuds de la radio

La base contient **200 nœuds** sur un ESP32-S3 à 8 Mo de flash. Le firmware en garde 250 à partir de 15 Mo, 200 à
partir de 7 Mo, et 100 en dessous (`mesh-pb-constants.h`, `get_max_num_nodes`). Une fois la base pleine, le firmware
retire le nœud entendu le moins récemment. **Les favoris, les nœuds ignorés et les clés vérifiées sont protégés**
(`NodeDB.cpp`). La version 2.8 garde aussi une partie des nœuds retirés dans une réserve en flash (« warm tier »).

Ce que cela change :

- **À 50 et 100 radios**, la base suffit, à condition que des nœuds étrangers (réseau public Meshtastic voisin) ne la
  remplissent pas. Le canal privé et `LOCAL_ONLY` évitent cela (§ 7).
- **À 200 radios**, la base est pleine dès qu'un seul autre nœud s'ajoute (PC, relais, radio étrangère). Un message
  direct chiffré (demande de tir, message à une personne) demande la clé publique du destinataire. Si son nœud a été
  retiré de la base de la radio émettrice, l'envoi échoue tant que son NodeInfo n'a pas été réentendu. Il faut le
  confirmer par essai, car la réserve de la 2.8 garde peut-être les clés.
- **Côté application**, la base locale (Room) n'a pas de limite. À chaque connexion Bluetooth, la radio renvoie toute
  sa base au téléphone : à 200 nœuds, cela prend un peu plus de temps, sans aucun temps d'antenne.

### 2.2 Messages diffusés automatiquement par chaque radio

Rôle `CLIENT`, valeurs par défaut (`Default.h`, `PositionModule.cpp`, `DeviceTelemetry.cpp`, `NodeInfoModule.cpp`) :

| Message | Fréquence par défaut | Remarques |
|---|---|---|
| NodeInfo (nom, clé publique) | toutes les **3 h** (minimum réglable : 1 h) | Aussi **30 s après chaque démarrage**. Après un **changement de canal**, le NodeInfo demande une réponse à tous les nœuds qui l'entendent. Une réponse à une demande n'est pas renvoyée moins de 30 min après la précédente, délai multiplié par le coefficient du § 2.3. |
| Position | toutes les **60 min** (minimum : 1 h si réglé plus bas pour un rôle routeur) | Multiplié par le coefficient du § 2.3. |
| Position « intelligente » (déplacement) | dès **100 m** parcourus, au plus toutes les **5 min** | **Non réduite** par le coefficient. C'est la principale source de trafic pour des radios en mouvement. |
| Position d'une radio immobile (2.8) | au plus toutes les **6 h 06** | Une radio qui ne quitte pas sa cellule de précision du canal (90 m par défaut) est considérée immobile. Avec la précision complète sur un canal privé, la cellule est minuscule et ce plancher ne joue presque jamais. |
| Télémétrie de l'appareil (batterie, tension) | toutes les **60 min** (minimum : 30 min) | Multipliée par le coefficient. Envoyée sur le réseau seulement si `device_telemetry_enabled` est actif ; sinon elle ne va qu'au téléphone. À vérifier radio par radio. |
| NeighborInfo | 6 h | Module désactivé par défaut. |
| Accusés, routage | à chaque message direct | Un accusé pour chaque message direct reçu. |

L'application d'origine **n'émet rien sur l'antenne d'elle-même**. Si « fournir la position du téléphone » est actif,
elle envoie toutes les 30 s la position du téléphone **à sa propre radio** (`MeshConnectionManagerImpl`,
`LocationRepositoryImpl`). Le paquet passe par Bluetooth et n'est pas diffusé.

### 2.3 Réduction automatique au-delà de 40 nœuds en ligne

Au-delà de 40 nœuds entendus dans les 2 dernières heures, le firmware espace les positions, la télémétrie et les
réponses NodeInfo d'un coefficient `1 + (N − 40) × 2^SF / (BW × 100)` (`Default.h`, `congestionScalingCoefficient`).
Les rôles `ROUTER` et `ROUTER_LATE` ne sont pas concernés : leurs intervalles sont déjà de 12 h. `TRACKER` et `SENSOR`
ne le sont pas non plus.

| Préréglage | 50 nœuds | 100 nœuds | 200 nœuds |
|---|---|---|---|
| LONG_FAST (SF11, 250 kHz) | × 1,82 | × 5,92 | × 14,1 |
| MEDIUM_SLOW (SF10) | × 1,41 | × 3,46 | × 7,55 |
| MEDIUM_FAST (SF9) | × 1,20 | × 2,23 | × 4,28 |

Exemple : à 200 nœuds en LONG_FAST, une position réglée à 60 min ne part plus que toutes les 14 h, sauf déplacement.
Le firmware se protège ainsi, mais **la vue PC voit alors des positions de plusieurs heures** (§ 4.4).

### 2.4 Garde-fous du temps d'antenne (`airtime.h`, `airtime.cpp`, `Router.cpp`)

- **Canal occupé à plus de 25 %** (mesure sur la dernière minute) : positions et télémétrie ne partent pas.
- **Canal occupé à plus de 40 %** : les réponses NodeInfo ne partent pas.
- **Limite d'émission EU_868 : 10 % de chaque heure par radio** (bande 869,4 à 869,65 MHz, `RDEF(EU_868…, 10, 27…)`).
  - Au-delà de 5 % (la moitié), les envois périodiques (télémétrie, NodeInfo) cessent.
  - Au-delà de 10 %, **tout envoi est refusé, relais compris**. L'application reçoit « Duty cycle limit exceeded.
    You can send again in N mins ». Un relais très sollicité devient donc muet une partie de l'heure.
- La bande EU_868 ne fait que 250 kHz. **Il n'existe qu'un seul canal radio** en LONG_FAST ou MEDIUM_FAST : toutes
  les radios du secteur, y compris un réseau public voisin, partagent le même temps d'antenne.

### 2.5 Sauts

- Valeur par défaut : 3 ; maximum : 7 (`MeshTypes.h`, documentation LoRa).
- En 2.8, le firmware peut **réduire de lui-même** le nombre de sauts de ses positions, télémétries et NodeInfo quand
  le canal est chargé (`HopScalingModule`, appliqué dans `Router.cpp`). Les messages texte ne sont pas concernés.
- Un `ROUTER` mal placé « mange » des sauts : documentation des rôles, article « Choosing The Right Device Role ».

### 2.6 Autres limites de l'application d'origine

- **8 canaux au plus** par radio : un canal par équipe est impossible au-delà de 7 équipes.
- **Une notification par message reçu** sur un canal non mis en sourdine.

## 3. Ce qui consomme du temps d'antenne

### 3.1 Durée d'un paquet sur l'air

Calcul avec la formule LoRa de Semtech : préambule de 16 symboles, en-tête explicite, CRC, codage 4/5 (4/8 pour
LONG_MODERATE).

| Paquet (taille sur l'air) | LONG_FAST | MEDIUM_SLOW | MEDIUM_FAST | SHORT_FAST |
|---|---|---|---|---|
| Accusé, routage (~24 o) | 0,44 s | 0,22 s | 0,12 s | 0,04 s |
| Traceroute (~30 o) | 0,48 s | 0,26 s | 0,13 s | 0,04 s |
| Position (~60 o) | 0,68 s | 0,38 s | 0,20 s | 0,06 s |
| NodeInfo (~120 o) | 1,13 s | 0,63 s | 0,33 s | 0,10 s |
| Texte court de 50 o (~75 o) | 0,81 s | 0,44 s | 0,23 s | 0,07 s |
| **Texte de 200 o (~225 o)** : `[POS]`, fait observé, partie de demande de tir, `[EQUIPES]` plein | **1,91 s** | 1,06 s | 0,58 s | 0,18 s |

Chaque paquet diffusé est réémis par les relais : la durée est multipliée par le nombre d'émissions (hypothèse : 3).

### 3.2 Charge estimée du canal

**Estimation.** Hypothèses :

- la moitié des radios immobiles, au réglage par défaut ; l'autre moitié en mouvement, avec en moyenne une position
  toutes les 10 min ;
- télémétrie active ; NodeInfo toutes les 3 h ;
- par radio et par heure : 2 messages de canal de 50 octets et 1 message direct (avec accusé, 2 sauts) ;
- 3 émissions par paquet diffusé.

| Préréglage | 5 radios | 50 radios | 100 radios | 200 radios |
|---|---|---|---|---|
| LONG_FAST | 2 % | **22 %** | **42 %** | **82 %** |
| MEDIUM_SLOW | 1 % | 13 % | 24 % | 46 % |
| MEDIUM_FAST | 1 % | 7 % | 13 % | 25 % |

En LONG_FAST, la moitié environ vient des messages automatiques, surtout les positions des radios en mouvement.

Lecture :

- **LONG_FAST, 50 radios** : le canal approche le seuil de 25 % qui retient positions et télémétrie. Un relais qui
  réémet presque tout émet environ 7 % de l'heure, près de la limite de 10 %. Une rafale (début d'exercice,
  changements d'équipe, demandes de tir) le fait dépasser.
- **LONG_FAST, 100 et 200 radios** : le canal dépasse 40 %. Les relais dépassent 10 % et se taisent par moments ; les
  collisions augmentent. **Bloquant.**
- **MEDIUM_FAST** divise le temps d'antenne par 3,3, au prix d'un bilan de liaison plus faible de 5 dB (148 dB contre
  153 dB, article Meshtastic du 22 avril 2025). La documentation conseille d'envisager ce changement au-delà de 60
  nœuds proches.

### 3.3 Ce qui consomme le plus, dans l'ordre

1. Positions des radios en mouvement : position intelligente toutes les 5 min, non réduite par le coefficient.
2. Messages texte longs (200 octets) diffusés sur un canal : `[POS]`, fait observé.
3. Demandes de tir : jusqu'à 9 parties de 200 octets, plus un accusé par partie et par saut.
4. Rafales de NodeInfo : démarrage simultané, changement de canal, changements d'équipe en série (§ 4.2).
5. Télémétrie et NodeInfo périodiques : faibles une fois réduits par le coefficient.

## 4. Fonction par fonction

### 4.1 Liste `[EQUIPES]` (§ 6)

**Taille.** Le message fait 200 octets au plus, dont 10 pour `[EQUIPES] `, et chaque nom 16 octets au plus. Il n'y a
**pas de découpage** : un message trop long est refusé à la saisie (`TeamRoster.parseInput`, `MessageTooLong`), et un
message reçu de plus de 200 octets est ignoré en entier.

| Noms | Équipes au plus |
|---|---|
| 16 octets (maximum) | 11 |
| 8 octets (`Section1`) | 21 |
| 5 octets (`Alpha`) | 31 |

**Coût radio.** Une diffusion, environ 1,9 s en LONG_FAST par émission, sans accusé.

**Verdict.** Le nombre de radios ne compte pas, seul le nombre d'équipes compte. 50 à 200 radios en 5 à 15 équipes
tiennent dans un message. **Sans effet** à 50 et 100 ; **gênant** à 200 si l'organisation dépasse 11 à 20 équipes
(un niveau « section » ou « groupe » en plus, par exemple).

**À noter, sans lien avec l'échelle :**

- **Livraison.** Un message diffusé n'est jamais accusé : le PC ne sait pas qui a reçu la liste. À 200 radios, il faut
  s'attendre à des absents, et donc la rediffuser ou passer par le QR.
- **Authentification.** Il n'y en a pas (limite connue, § 6). Plus il y a de radios, plus il y a d'émetteurs
  possibles d'une fausse liste.

### 4.2 Appartenance et diffusion automatique du nom (§ 6, 21)

**Coût radio.** Chaque changement d'équipe modifie le nom long. La radio redémarre, puis l'application diffuse un
NodeInfo : environ 1,1 s en LONG_FAST par émission, réémis par les relais. Le blocage de 30 s ne vaut que **par
téléphone**.

**Scénario à risque.** Le PC diffuse une liste au début d'un exercice, et chacun choisit son équipe dans les minutes
qui suivent. Estimation, si tout le monde le fait en 10 minutes :

| Radios | LONG_FAST | MEDIUM_FAST |
|---|---|---|
| 50 | environ 170 s d'antenne, soit **28 %** du canal | environ 50 s, soit 8 % |
| 100 | environ 56 % | environ 17 % |
| 200 | **plus de 100 %** : impossible, des diffusions se perdent | environ 33 % |

À cette charge s'ajoutent les NodeInfo de démarrage du firmware (30 s après le redémarrage), et parfois sa propre
diffusion du nouveau nom (§ 21).

**Verdict.** **Gênant** à 50 et 100 ; **bloquant** à 200 en LONG_FAST si les changements sont simultanés. Le mécanisme
reste juste : une diffusion par changement, sans demande de réponse.

**Parade sans code.** Régler le nom et l'équipe **pendant la préparation des radios** (`PREPARER-LES-RADIOS.md`),
radio par radio, et non sur le terrain.

### 4.3 Organigramme par QR (§ 17, 26)

**Capacité.** Deux limites, vérifiées dans le code :

- **`OrgChart.MAX_MEMBERS = 40`** (`OrgChart.kt`, ligne 68, commentaire « Well above the 9 radios in use »). Le
  41e membre est refusé à l'ajout, et un QR qui en contient plus est refusé au décodage (`OrgChartCode.isConsistent`).
- **770 caractères** au plus (QR version 22, correction moyenne). Chaque membre prend
  `M|<indicatif>|<fonction>` plus un retour à la ligne. Un caractère non ASCII s'écrit `%XX` : « É » prend 6
  caractères.

| Contenu typique | Caractères par membre | Membres au plus (5 équipes) |
|---|---|---|
| Indicatif de 8 caractères, sans fonction | 12 | 40 (limite du modèle) |
| Indicatif de 8, fonction de 10 (« Chef group ») | 22 | environ 32 |
| Indicatif de 8, fonction de 14 avec accents (« Équipier tir ») | 31 | environ 23 |
| Indicatif et fonction de longueur maximale | 69 | 9 (valeur de `FORK.md`) |

**Verdict.** **Bloquant dès 50 radios** : 50 membres ne peuvent pas être saisis. Le partage par QR est impossible à
100 et 200 : il faudrait 4 à 7 codes de 770 caractères. De plus, saisir 50 à 200 membres un par un sur le téléphone
du PC est long, même avec les « Radios connues » (§ 26).

**Ce qui tient.** L'affichage est une liste paresseuse (`LazyColumn`), et le rattachement par indicatif est un calcul
`membres × radios` négligeable à 200. **Aucune émission radio.**

**Lisibilité.** Agrandir le QR n'est pas une solution sûre. La version 40 contient 2 331 octets en correction moyenne
(2 953 en correction faible), mais un QR de 177 × 177 modules se lit mal sur l'écran d'un autre téléphone. Ce point
n'a pas été essayé.

### 4.4 Vue PC (§ 3)

**Performances.**

- C'est une `LazyColumn` avec une clé par nœud, et la liste est recalculée à chaque changement de la base et toutes
  les 15 s (`CommandPostViewModel`).
- Un tri de 200 éléments toutes les 15 s est négligeable.
- **Aucun problème de performance** à 200.

**Affichage (gênant à toutes les tailles visées).** Trois défauts :

1. **Seuils de fraîcheur incompatibles avec les intervalles du firmware.**
   - « Position ancienne » au-delà de 10 min et « contact récent » jusqu'à 15 min supposent qu'une radio émette au
     moins tous les quarts d'heure.
   - Or une radio immobile au réglage par défaut n'envoie sa position que toutes les 60 min, multipliées par le
     coefficient (§ 2.3) : 1 h 50 à 50 nœuds, 6 h à 100, 14 h à 200 en LONG_FAST. Sa télémétrie suit le même
     rythme.
   - Résultat : la plupart des radios immobiles s'affichent **« POSITION ANCIENNE » et sans contact récent alors
     qu'elles fonctionnent**. L'alerte perd son sens.
   - C'est déjà le cas à 5 radios si l'intervalle de position n'a pas été réduit. `PREPARER-LES-RADIOS.md` ne le
     règle pas.
2. **Liste unique triée par dernier contact**, sans regroupement ni filtre par équipe et sans recherche. À 100 ou 200
   lignes, retrouver une équipe ou une radio précise demande de faire défiler la liste. Le filtre « Équipe » existe
   dans la messagerie (§ 6), pas dans la vue PC.
3. **Nœuds étrangers et anciens.** La vue montre tous les nœuds connus, sauf ceux qui sont ignorés. Sur le canal par
   défaut, des radios d'un réseau public voisin s'y ajoutent ; des radios éteintes depuis la veille y restent.

**Verdict.** **Gênant** à 50 et 100 ; **gênant au point d'être peu utilisable** à 200, sans les corrections C2 et C3.

### 4.5 Listes des destinataires et listes de choix

| Liste | Réalisation | 50 | 100 | 200 |
|---|---|---|---|---|
| Conversations (application d'origine, onglets par canal du § 4) | liste paresseuse | sans effet | sans effet | sans effet |
| Liste des nœuds (origine, avec recherche) : pour ouvrir un message direct | liste paresseuse, recherche | sans effet | sans effet | sans effet |
| Choix de la conversation où insérer un texte (§ 15) | écran d'origine | sans effet | sans effet | sans effet |
| **« Tester la liaison » : choix du nœud** (§ 19) | menu déroulant **non paresseux, sans recherche**, tous les nœuds entendus (`LinkTestCard`, `ChoiceField`, `choices.forEach`) | sans effet | gênant (long défilement) | gênant |
| **« Radios connues »** à l'ajout d'un membre (§ 26) | puces dans un `FlowRow`, **sans plafond** : toutes les radios connues s'affichent avant toute frappe (`KnownRadios`) | gênant (50 puces dans une boîte de dialogue) | gênant | gênant |
| Destinataire d'une demande de tir (§ 30) | la conversation directe ouverte | sans effet | sans effet | sans effet |

**Performances.** 200 éléments composés d'un coup dans un menu ou une boîte de dialogue restent supportables sur un
téléphone récent. Le défaut est l'ergonomie.

### 4.6 Diagnostic de liaison (§ 19, 25)

**Constats.** Ils sont calculés sur la base de nœuds : nombre de nœuds entendus en 5, 15 et 60 min, dernier nœud
entendu. Le coût est négligeable à 200.

**Bandeau « Aucun nœud entendu ».** À 50 radios et plus, il ne s'affiche presque jamais, car quelqu'un est toujours
entendu. C'est conforme à son but. En revanche, « nœuds entendus en 5 min » sera bas même dans un réseau sain, à
cause des intervalles du § 2.3. Il faut le dire dans le guide, pour qu'un chiffre bas ne passe pas pour une panne.

**« Tester la liaison ».** Un traceroute coûte environ 0,5 s par saut à l'aller et au retour, en LONG_FAST. Il faut
une confirmation, le blocage de 30 s vaut par téléphone, et rien n'est automatique. Si 50 opérateurs testent en même
temps, la charge reste modérée (environ 50 × 3 s).

**Verdict.** **Sans effet** à toutes les tailles, avec une consigne : réserver le test au PC et à la recherche d'une
panne.

### 4.7 Ma position, fait observé, diffuser ma position, partage de point (§ 7, 12, 15)

**Coût de chaque geste.**

| Geste | Paquet | Durée par émission en LONG_FAST |
|---|---|---|
| `[POS]` complet (MGRS, UTM, DMS, environ 150 à 200 octets) | texte | 1,5 à 1,9 s |
| Fait observé | texte de 200 octets au plus | jusqu'à 1,9 s |
| « Partager ce point sur la carte » | point de repère d'environ 100 octets, en plus du message | environ 1,1 s |
| « Diffuser ma position maintenant » | position | 0,7 s |

Rien n'est automatique : chaque envoi demande un geste, et la diffusion de position est bloquée 30 s après chaque
envoi.

**Risque.** Le risque est l'usage, pas la fonction. Si 50 opérateurs envoient un `[POS]` complet toutes les 10 min
sur un canal commun, cela fait environ 50 × 6 × 1,9 s × 3 ≈ 29 min d'antenne par heure, soit **48 % du canal** en
LONG_FAST. C'est plus que tout le trafic automatique.

**Verdict.**

- **Gênant** dès 50 en usage fréquent ; sans effet en usage ponctuel.
- La position automatique du firmware (paquet de 60 octets) est **3 fois moins chère** qu'un `[POS]`, et elle alimente
  déjà la vue PC et la carte.
- Le `[POS]` sert à donner une position lisible dans un message, pas à suivre une position dans le temps.

### 4.8 Demande de tir (§ 30)

**Envoi.** Jusqu'à 9 parties de 200 octets, toutes remises à la radio d'un coup (`FireRequestViewModel`,
`forEach { sendMessageUseCase }`). Chacune part en message direct avec accusé, et le firmware réessaie jusqu'à 3 fois.

**Coût estimé** (estimation : 2,3 s par partie avec son accusé, en LONG_FAST) :

| Demande | LONG_FAST | MEDIUM_FAST |
|---|---|---|
| 3 parties, 2 sauts | environ 14 s d'antenne | environ 4 s |
| 9 parties, 3 sauts, sans réessai | environ 63 s | — |

**Risques à l'échelle.**

1. **Congestion.** Au-delà de 25 à 40 % d'occupation (LONG_FAST à 100 ou 200 radios), les collisions et les réessais
   allongent l'arrivée des parties. Le regroupement tolère les parties en désordre ou manquantes, et « Renvoyer la
   partie k » existe : la fonction reste **juste**, mais lente.
2. **Clé du destinataire.** Un message direct chiffré a besoin de la clé publique du destinataire dans la base de la
   radio émettrice (§ 2.1). À 200 radios, une radio retirée de la base ne peut plus recevoir de demande tant que son
   NodeInfo n'a pas été réentendu. Parade : mettre la radio du PC (et celle de la batterie qui reçoit les demandes)
   **en favori** sur toutes les radios, car les favoris ne sont jamais retirés.
3. **Lecture.** Le regroupement charge toute la conversation directe (point ouvert du § 30). Cela dépend de la
   longueur de cette conversation, pas du nombre de radios.

**Verdict.**

- **Sans effet** à 50 en MEDIUM_FAST ; **gênant** à 50 en LONG_FAST et à 100.
- **Bloquant** à 200 en LONG_FAST, ou si les clés sont perdues (à confirmer par essai).

### 4.9 Notifications

**Origine.** L'application d'origine notifie chaque message d'un canal non mis en sourdine. Avec 2 messages par radio
et par heure sur le canal commun, chaque téléphone reçoit **100 notifications par heure à 50 radios** et 400 à 200.

**Apport du fork.** Le fork regroupe seulement les parties d'une demande de tir, en une notification qui ne sonne
qu'une fois (§ 30). La liste `[EQUIPES]` donne une proposition d'adoption par réception, ce qui est acceptable.

**Limite des canaux.** 8 canaux au plus par radio (§ 2.6) : un canal par équipe ne tient que jusqu'à 7 équipes.

**Verdict.** **Gênant** dès 50. Parade sans code : un canal commun réservé au PC et aux urgences, des canaux de
groupe, et la mise en sourdine des canaux non suivis (fonction d'origine).

### 4.10 Mode démo (§ 9, 22)

- Rien n'est émis, et le mode est indépendant du nombre de radios réelles : **sans effet** à toutes les tailles.
- La vraie radio continue de recevoir. À 200 radios, le bandeau compte beaucoup de vrais messages, ce qui est
  attendu.
- Limite pour l'instruction : la démo n'a que 4 nœuds fictifs. Elle ne montre pas une vue PC ou un organigramme
  chargés (amélioration facultative C10).

### 4.11 Carte (§ 12 à 15, 29)

**Ce qui ne change pas avec l'échelle.**

- 200 marqueurs ne gênent pas MapLibre.
- Carte hors ligne, téléchargement, coordonnée au toucher, Recaler : aucune émission radio, donc **sans effet**.

**Ce qui gêne à 100 et 200.**

- **Étiquettes.** L'étiquette « ANCIENNE » est **toujours visible, même quand les puces se chevauchent** (§ 14). Avec
  les intervalles du § 2.3, la plupart des radios immobiles la portent : la carte se couvre d'étiquettes.
- **Points partagés.** Chaque point partagé vit 24 h : à 200 opérateurs, la carte s'encombre.

**Verdict.** **Gênant** à 100 et 200, pour la même cause que la vue PC.

### 4.12 Sections sans effet

Ces sections n'émettent rien et ne dépendent pas du nombre de nœuds :

- identité, textes et logo (§ 1, 2, 5) ;
- priorités et états d'envoi (§ 4). « Relayé par le réseau » vient de la réémission entendue et reste vrai à toute
  échelle ;
- cartes de message (§ 8), thème (§ 10), barre de navigation (§ 11) ;
- convertisseur (§ 7), carte hors ligne (§ 13), détachement de Meshtastic (§ 16) ;
- CI (§ 18), écran de bienvenue (§ 20), mode démo unique (§ 22) ;
- fait observé : taille, veille, clavier (§ 23) ;
- textes français (§ 24), crédit de carte (§ 27), APK de release (§ 28), téléchargement de carte (§ 29).

## 5. Constats classés

### Bloquants

| N° | Constat | À partir de | Section |
|---|---|---|---|
| B1 | L'organigramme refuse plus de 40 membres, et le QR de 770 caractères en contient environ 25 à 40 selon la longueur des fonctions. | 50 | 4.3 |
| B2 | En LONG_FAST, le canal dépasse 40 % d'occupation et les relais dépassent la limite de 10 % d'émission par heure : le firmware les fait taire par moments (réglage, pas le fork). | 100 (50 en rafale) | 2.4, 3.2 |
| B3 | La base de nœuds de la radio (200) déborde : risque d'échec des messages directs chiffrés, dont la demande de tir, vers une radio retirée de la base. À confirmer par essai. | 200 | 2.1, 4.8 |
| B4 | Changements d'équipe simultanés : la rafale de NodeInfo dépasse la capacité du canal en LONG_FAST. | 200 (gênant dès 50) | 4.2 |

### Gênants

| N° | Constat | À partir de | Section |
|---|---|---|---|
| G1 | Les seuils de la vue PC (10 et 15 min) ne correspondent pas aux intervalles réels du firmware : de fausses alertes « ANCIENNE » et des contacts « non récents » sur des radios en état de marche. Même cause sur la carte. | 5 (aggravé au-delà de 40) | 4.4, 4.11 |
| G2 | La vue PC n'a ni filtre par équipe, ni regroupement, ni recherche ; les nœuds étrangers et anciens y restent. | 100 | 4.4 |
| G3 | Le menu « Tester la liaison » n'est pas paresseux et n'a pas de recherche ; « Radios connues » n'a pas de plafond. | 50 à 100 | 4.5 |
| G4 | Un `[POS]` complet coûte environ 3 fois une position automatique ; en usage fréquent, il dépasse tout le trafic automatique. | 50 | 4.7 |
| G5 | Une demande de tir en LONG_FAST occupe 14 à 63 s d'antenne et devient lente quand le canal est chargé. | 50 en LONG_FAST | 4.8 |
| G6 | Une notification par message de canal, et 8 canaux au plus par radio. | 50 | 4.9 |
| G7 | Liste `[EQUIPES]` limitée à 11 à 31 équipes selon la longueur des noms, sans découpage ni accusé de réception. | 200 | 4.1 |
| G8 | Saisie de l'organigramme membre par membre : long pour 50 à 200 membres. | 50 | 4.3 |

### Sans effet

Diagnostic de liaison (sauf consigne sur le traceroute), mode démo, carte hors ligne et téléchargement, convertisseur,
messagerie (priorités, états, filtres), et toutes les sections listées au § 4.12.

## 6. Corrections proposées et leur coût

Coût indicatif en jours de travail d'un développeur sur ce dépôt, tests compris, hors essais sur radios. Aucune de ces
corrections n'ajoute de type de paquet : le firmware reste non modifié, et les messages restent lisibles par
l'application officielle.

| N° | Correction | Répond à | Coût | Priorité |
|---|---|---|---|---|
| C0 | **Réglages radio du § 7** et mise à jour de `PREPARER-LES-RADIOS.md` : canal privé, préréglage, rôles, `LOCAL_ONLY`, intervalles, favoris, nom et équipe réglés à la préparation. **Sans code.** | B2, B3, B4, G1 (en partie), G5, G6 | 0,5 j de documentation, plus la préparation des radios | 1 |
| C1 | **Organigramme en plusieurs QR** (format `NTORGA2 k/N`, qui peut aussi porter le réglage LoRa attendu, reporté au § 19) : le lecteur accumule les codes dans n'importe quel ordre, un code manquant se signale, l'adoption n'a lieu qu'avec tous les codes. Limite de membres portée à 250. Lecture de `NTORGA1` conservée. | B1 | 3 à 4 j | 1 |
| C1 bis | En plus ou à la place de C1 : **export et import de l'organigramme en fichier texte** par le sélecteur de fichiers Android (comme l'export de carte, § 29), échangé par USB ou par partage Android, sans radio. Il permet aussi de **préparer l'organigramme sur un ordinateur** (G8). | B1, G8 | 1,5 à 2 j | 1 |
| C2 | **Seuils de fraîcheur réglables en mode PC** (« position ancienne après », « contact récent jusqu'à ») avec une valeur proposée d'après l'intervalle de position attendu, et une mention « intervalle réel du firmware à N nœuds » dans l'aide. Les mêmes seuils servent à la carte et au diagnostic (`FreshnessThresholds` est déjà commun). | G1 | 1,5 j | 1 |
| C3 | **Vue PC : filtre par équipe, regroupement par équipe, recherche par indicatif, et masquage des nœuds non entendus depuis X heures** (réglable). | G2 | 2 j | 2 |
| C4 | **Listes de choix** : recherche dans le choix du nœud de « Tester la liaison », plafond de 12 puces dans « Radios connues » (les plus récentes, la frappe filtre le reste). | G3 | 0,5 j | 3 |
| C5 | **`[POS]` court** : option « MGRS seul » (`[POS] ALPHA-1 · MGRS … · relevée 14:32`, environ 60 octets, 0,8 s au lieu de 1,9 s en LONG_FAST). Le format complet reste au choix. Rappel dans l'aide : la position automatique suffit au suivi. | G4 | 0,5 à 1 j | 2 |
| C6 | **Demande de tir plus compacte** : abréviations déjà dans la fiche et suppression des cases vides dans le texte envoyé, pour viser 1 à 3 parties. À valider avec le format de la fiche (spécification du § 30), car le texte doit rester lisible par un client ordinaire. | G5 | 1 à 2 j, plus validation | 3 |
| C7 | **Espacement de la diffusion automatique du nom** : délai aléatoire de 0 à 120 s avant la diffusion, et pas de diffusion si le canal est signalé occupé (la radio envoie au téléphone son taux d'occupation, déjà affiché par l'application d'origine). | B4 | 1 j | 2 |
| C8 | **Liste `[EQUIPES]` en deux messages** (`[EQUIPES 1/2]`), seulement si l'organisation dépasse 11 à 20 équipes. Sinon, garder des noms courts. | G7 | 1 j | 4 |
| C9 | **Notifications** : réglage « ne notifier que `[URG]`, les messages directs et les demandes de tir » sur un canal donné. | G6 | 1 j | 3 |
| C10 | Mode démo : jeu de données fictif « grand réseau » (50 nœuds, 6 équipes), pour l'instruction. | — | 1 j | 4 |

## 6 bis. Ordre retenu

Décidé le 13 novembre 2026. Les autres corrections (C3 à C10) ne sont pas programmées.

| Ordre | Action | Échéance |
|---|---|---|
| 1 | **C0** : réglages radio du § 7, sous forme de documentation (`PREPARER-LES-RADIOS.md`). | Avant l'essai réel de la semaine prochaine (semaine du 16 novembre). |
| 2 | **C2** : seuils de fraîcheur de la vue PC réglables. | Juste après les essais des 13 et 14 novembre. |
| 3 | **C1** : organigramme au-delà de 40 membres. **Variante à décider** : plusieurs QR (C1) ou fichier (C1 bis). | Avant tout déploiement de plus de 40 radios. |
| 4 | Passage au préréglage **MEDIUM_FAST**. | Seulement après l'essai de portée à Cincu. D'ici là, le préréglage reste `LongFast`. |

Conséquence pour les essais : tant que C2 n'est pas fait, la vue PC affiche de fausses alertes « POSITION ANCIENNE »
pour les radios immobiles (§ 4.4, constat G1). C'est connu et noté dans `TESTS-A-FAIRE.md`.

## 7. Réglages radio recommandés pour 50 radios

### 7.1 Réglages

À appliquer à **toutes** les radios, avec **une seule version de firmware** (une 2.8.x stable : elle ajoute la
réduction automatique des sauts et le plancher des positions immobiles). Sources au § 9.

| Réglage | Valeur recommandée | Pourquoi | Source |
|---|---|---|---|
| Région | `EU_868` | Bande 869,4 à 869,65 MHz : 10 % d'émission par heure, 500 mW PAR (27 dBm). | `RadioInterface.cpp` (`RDEF(EU_868…)`), ERC/REC 70-03 annexe 1 |
| Préréglage | **`MEDIUM_FAST`** si l'essai de portée le permet, sinon `MEDIUM_SLOW` ; `LONG_FAST` seulement si le terrain l'impose, avec une discipline d'usage stricte | Temps d'antenne divisé par 3,3 (MEDIUM_FAST) ou 1,8 (MEDIUM_SLOW) ; bilan de liaison plus faible de 5 dB ou 2,5 dB. À 50 radios, charge estimée de 7 % au lieu de 22 % (§ 3.2). Toutes les radios doivent avoir le même préréglage ; le diagnostic le vérifie (§ 19, 25). | Article Meshtastic du 22 avril 2025 ; § 3 de cet audit |
| Canal principal | **privé** : nom propre au projet et clé aléatoire de 256 bits ; **précision de position complète** ; aucun canal `LongFast` public | Les radios publiques voisines ne déchiffrent pas les paquets et n'entrent pas dans la vue PC. La précision complète évite le plancher de 6 h des radios « immobiles » (§ 2.2). | documentation des canaux ; `PositionModule.cpp` |
| MQTT | désactivé, « OK to MQTT » désactivé | Pas de passerelle Internet ; le fork n'en utilise pas. | — |
| Rôle des radios portées | **`CLIENT`** (par défaut) | Un `CLIENT` relaie, mais renonce s'il entend un voisin relayer avant lui. | documentation des rôles ; article « Choosing The Right Device Role » |
| Radios groupées (plusieurs au PC, dans un même véhicule, à la même position) | **`CLIENT_MUTE`** pour toutes sauf une | Elles reçoivent et émettent leurs propres messages sans réémettre ceux des autres. | même source |
| Relais fixes | **`ROUTER`** seulement pour 1 à 3 points hauts dégagés (mât, sommet) ; `ROUTER_LATE` pour couvrir une zone d'ombre ; jamais pour une radio portée | Un `ROUTER` mal placé consomme des sauts et peut rendre la liaison asymétrique. Un `ROUTER` réémet tout : en LONG_FAST, il approche la limite de 10 % dès 50 radios (§ 3.2). | article « Choosing The Right Device Role » ; `Router.cpp` |
| `TRACKER`, `SENSOR` | à éviter | Leurs positions et leur télémétrie échappent à la réduction automatique (§ 2.3). | `Default.cpp` |
| Mode de relais | **`LOCAL_ONLY`** | La radio ne réémet pas le trafic d'autres réseaux. `KNOWN_ONLY` n'est pas conseillé : il ignorerait une radio de l'exercice pas encore dans sa base. | documentation, « Rebroadcast Mode » |
| Sauts | **3** (par défaut) ; 4 seulement si un traceroute montre qu'il en faut 4, et seulement en MEDIUM_FAST | Chaque saut de plus multiplie les réémissions. | documentation LoRa ; article sur les rôles |
| NodeInfo | 10 800 s (3 h, valeur par défaut) | Le fork diffuse lui-même le nom après un changement (§ 21) ; inutile de raccourcir. | `Default.h` |
| Position : intervalle | **900 s** (15 min) | En pratique 18 min en MEDIUM_FAST ou 27 min en LONG_FAST à 50 nœuds, à cause du coefficient du § 2.3. Si le PC n'a pas besoin de positions toutes les 15 min, garder 3 600 s. | `Default.h`, `PositionModule.cpp` |
| Position intelligente | activée ; **150 m** et **300 s** en MEDIUM_FAST, **200 m** et **600 s** en LONG_FAST | Principale source de trafic des radios en mouvement (§ 3.3). | `PositionModule.cpp`, `PositionModule.h` |
| Champs de position | `TIMESTAMP` coché (déjà demandé par le fork) | Âge de la position, et condition de « Diffuser ma position maintenant ». | `PREPARER-LES-RADIOS.md` |
| Télémétrie de l'appareil sur le réseau | **désactivée**, ou 3 600 s si le PC veut voir la batterie de chaque radio | La batterie de sa propre radio reste visible par le lien Bluetooth (diagnostic, § 19). | `DeviceTelemetry.cpp` |
| NeighborInfo | désactivé (par défaut) | — | `Default.h` |
| Favoris | **radio du PC, radios qui reçoivent les demandes de tir, relais fixes** en favori sur toutes les radios | Ils ne sont jamais retirés de la base, donc les messages directs vers eux restent possibles (§ 2.1). | `NodeDB.cpp` |
| Nom et équipe | réglés **à la préparation**, pas sur le terrain | Évite la rafale de NodeInfo (§ 4.2). | § 4.2 |

### 7.2 Consignes d'emploi

- **Démarrage échelonné.** Ne pas allumer 50 radios dans la même minute : chacune diffuse son NodeInfo 30 s après le
  démarrage. Allumer par groupe, à une ou deux minutes d'intervalle.
- **Pas de changement de canal sur le terrain.** Après un changement de canal, chaque radio demande une réponse à
  tous ses voisins.
- **Canaux.** Un canal commun réservé au PC et aux urgences, et des canaux de groupe (7 au plus en plus du principal).
  Mettre en sourdine les canaux non suivis.
- **Traceroute** (« Tester la liaison ») réservé au PC et à la recherche de panne.
- **`[POS]`** pour donner une position dans un échange ; le suivi passe par la position automatique.

### 7.3 Au-delà de 50 radios

**À 100 radios :**

- MEDIUM_FAST devient nécessaire (estimation : 13 % d'occupation, contre 42 % en LONG_FAST) ;
- passer la position à 1 800 s et la position intelligente à 200 m et 600 s ;
- garder la télémétrie désactivée.

**À 200 radios :**

- la base de nœuds est à sa limite (§ 2.1). Il faut des favoris stricts, `LOCAL_ONLY`, et éventuellement plusieurs
  réseaux séparés (canaux principaux distincts) reliés par le PC ;
- MEDIUM_FAST reste à environ 25 % d'occupation estimée, au seuil où le firmware retient les positions ;
- un essai de charge est indispensable avant l'emploi.

## 8. Essais à faire pour confirmer

Les chiffres des § 3 et 4 sont des estimations, à confirmer sur le terrain :

1. **Occupation du canal.** Relever « utilisation du canal » et « temps d'émission » (`channel_utilization` et
   `air_util_tx`, affichés dans la fiche de chaque nœud par l'application d'origine) sur la radio du PC et sur un
   relais pendant un exercice. Comparer avec le § 3.2.
2. **Taille de la base de nœuds** de la T-Beam Supreme : journal série au démarrage.
3. **Portée de MEDIUM_FAST.** Refaire en MEDIUM_FAST les liaisons les plus longues de l'essai à 5 radios, avec
   « Tester la liaison ».
4. **Demande de tir** de 3 parties sur 2 sauts, avec le canal chargé : délai d'arrivée de chaque partie.
5. **QR de version supérieure à 22** (si la correction C1 n'est pas retenue) : lecture d'un écran à l'autre, en
   lumière du jour.

## 9. Sources

**Firmware Meshtastic**, code source aux étiquettes `v2.7.26.54e0d8d` et `v2.8.2.d5f1619`
(https://github.com/meshtastic/firmware). Remplacer `<v>` par l'étiquette :

- `src/mesh/mesh-pb-constants.h` : taille de la base de nœuds
  (https://github.com/meshtastic/firmware/blob/v2.8.2.d5f1619/src/mesh/mesh-pb-constants.h)
- `src/mesh/Default.h`, `src/mesh/Default.cpp` : intervalles par défaut, coefficient de réduction, plancher des
  positions immobiles, réduction des sauts (2.8)
- `src/modules/PositionModule.cpp`, `src/modules/PositionModule.h` : position périodique, intelligente, immobile
- `src/modules/NodeInfoModule.cpp` : NodeInfo au démarrage, après un changement de canal, délai de réponse
- `src/modules/Telemetry/DeviceTelemetry.cpp` : télémétrie de l'appareil
- `src/airtime.h`, `src/airtime.cpp` : seuils de 25 % et 40 %, limite d'émission
- `src/mesh/Router.cpp` : refus d'émettre au-delà de la limite légale, application de la réduction des sauts
- `src/mesh/FloodingRouter.cpp` : `CLIENT_MUTE`, mode de relais
- `src/mesh/RadioInterface.cpp` : définition de `EU_868` (bande, 10 %, 27 dBm) et préréglages permis
- `src/mesh/NodeDB.cpp` : retrait des nœuds, favoris protégés, `NUM_ONLINE_SECS` (2 h)

**Documentation Meshtastic**, dépôt https://github.com/meshtastic/meshtastic (le site meshtastic.org n'était pas
joignable depuis l'environnement de cet audit) :

- `docs/configuration/radio/device.mdx` : rôles, mode de relais
- `docs/configuration/radio/lora.mdx` : sauts (3 par défaut, 7 au plus)
- `blog/2025/April/why-your-mesh-should-switch-from-longfast.mdx` : « Is LongFast Holding Your Mesh Back? »,
  22 avril 2025 (tableau des préréglages, seuil de 60 nœuds)
- `blog/2024/November/2024-11-10-choosing-a-device-role.mdx` : « Choosing The Right Device Role », mis à jour le
  24 septembre 2025
- `blog/2025/November/zero-cost-hops-for-favorite-routers.mdx` : sauts gratuits entre routeurs favoris

**Réglementation** : Recommandation ERC 70-03 (CEPT), annexe 1, bande 869,4 à 869,65 MHz : 500 mW PAR, 10 %
d'émission ; norme ETSI EN 300 220.

**Formule du temps d'antenne** : Semtech, note d'application AN1200.13, « LoRa Modem Designer's Guide ».

**Code du fork** (commit `c330b66`) :

- `core/model/.../team/TeamRoster.kt`
- `core/model/.../orgchart/OrgChart.kt` et `OrgChartCode.kt`
- `feature/node/.../commandpost/CommandPostViewModel.kt` et `CommandPostScreen.kt`
- `feature/node/.../diagnostic/LinkTestCard.kt` et `DiagnosticViewModel.kt`
- `feature/node/.../orgchart/OrgChartEditing.kt`
- `feature/settings/.../identity/NameBroadcaster.kt`
- `core/model/.../firerequest/FireRequestSplitter.kt`
- `feature/messaging/.../firerequest/FireRequestViewModel.kt`
- `core/data/.../manager/MeshConnectionManagerImpl.kt`
- `core/data/.../repository/LocationRepositoryImpl.kt`
