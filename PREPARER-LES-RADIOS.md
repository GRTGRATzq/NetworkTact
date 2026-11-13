# Préparer une radio pour NetworkTact

Dernière mise à jour : 13 novembre 2026 (réglages pour un maillage de plus de 50 radios, correction C0 de
`docs/AUDIT-PASSAGE-A-L-ECHELLE.md`).

À faire sur **chaque radio, une par une**, **après une remise à zéro** (voir `FLASHER-LA-RADIO.md`), une fois la radio
appairée en Bluetooth avec son téléphone dans NetworkTact.

## Où sont enregistrés ces réglages

- **Dans la radio.** Le firmware Meshtastic n'est pas modifié. NetworkTact ne fait que lui envoyer les réglages, avec
  les écrans de configuration hérités de l'application d'origine. La radio les garde après un redémarrage ou un
  changement de téléphone.
- **Une remise à zéro de la radio les efface** : il faut alors refaire toute la liste.
- **Le mode démo doit être désactivé** (Réglages → « Mode démo ») : en démo, ces écrans sont grisés.

## Avant de commencer

- **Noms des écrans.** Ils sont écrits tels que NetworkTact les affiche en français. Quelques libellés hérités ne sont
  pas traduits et s'affichent en anglais : ils sont recopiés tels quels, entre guillemets.
- **Chemins.**
  - L'onglet **Réglages** est celui de la barre du bas.
  - Sous Réglages, la section **Configuration** donne directement accès à **Utilisateur**, **LoRa**, **Canaux** et
    **Sécurité**.
  - Les autres écrans de la radio (Appareil, Position…) sont dans **Configuration de l'appareil**, et les modules
    (MQTT, Télémétrie…) dans **Réglages du module**.
- **Enregistrement.** Enregistrer chaque écran avant de passer au suivant. La radio peut redémarrer après un
  changement, et le Bluetooth se reconnecte alors tout seul : attendre la reconnexion avant de continuer.
- **Préparer à l'écart du réseau en service.** Quand le canal change, la radio demande à tous ses voisins de renvoyer
  leurs informations (audit § 2.2). Faire la préparation avant le déploiement, et ne pas changer de canal sur le
  terrain.
- **Sources.** La colonne « Source » renvoie à l'audit (`docs/AUDIT-PASSAGE-A-L-ECHELLE.md`), au code du firmware
  Meshtastic (version 2.8.2) ou à sa documentation. La liste complète est au § 9 de l'audit.

## 1. LoRa

Chemin : Réglages → Configuration → **LoRa**.

- [ ] **Région** : `EU_868`.
  - Raison : bande 869,4 à 869,65 MHz, 10 % d'émission par heure au plus.
  - Source : audit § 2.4 ; firmware `RadioInterface.cpp`.
- [ ] **Utiliser un préréglage** : activé.
  - Raison : le préréglage fixe tous les paramètres radio d'un coup, identiques partout.
  - Source : documentation `lora.mdx`.
- [ ] **Préréglages** : **`LONG_FAST`**, conservé **jusqu'à l'essai de portée à Cincu** (voir § 7).
  - Raison : la plus grande portée ; MEDIUM_FAST divise le temps d'antenne par 3,3, mais sa portée reste à vérifier
    sur le terrain.
  - Source : audit § 3.2 et § 6 bis.
- [ ] **Nombre de sauts** : **3**.
  - Raison : chaque saut de plus multiplie les réémissions ; 3 est la valeur par défaut, à ne pas augmenter sans
    qu'un traceroute l'exige.
  - Source : audit § 7.1 ; documentation `lora.mdx` ; article « Choosing The Right Device Role ».

Région et préréglage doivent être **identiques sur toutes les radios** ; sinon, elles ne s'entendent pas.

## 2. Canal principal privé

Chemin : Réglages → Configuration → **Canaux**, puis toucher le **canal principal** (le premier de la liste).

Le canal se règle **une seule fois, sur la radio du PC**. Il est ensuite recopié sur les autres radios par QR code
(§ 2 bis) : la clé ne se recopie pas à la main.

- [ ] **Nom du canal** : un nom propre au projet, sans espace, 11 caractères au plus.
  - Raison : ne pas utiliser le canal public `LongFast`, que partagent toutes les radios Meshtastic du secteur.
  - Source : audit § 4.4 et § 7.1.
- [ ] **PSK (clé)** : toucher l'icône **« Réinitialiser »** (flèche circulaire) au bout du champ. Elle tire une
      **clé aléatoire de 256 bits**.
  - Raison : les radios étrangères ne déchiffrent plus nos paquets et n'entrent pas dans la vue PC.
  - Source : audit § 7.1 ; code `Channel.getRandomKey` (32 octets).
- [ ] **Position activée** : activé ; **Emplacement précis** : activé.
  - Raison : une position imprécise fait passer la radio pour immobile, et sa position n'est alors diffusée que
    toutes les 6 h.
  - Source : audit § 2.2 ; firmware `PositionModule.cpp`.
- [ ] Ne garder **aucun autre canal** qui porte la clé publique par défaut.
  - Raison : même motif que le nom du canal.
  - Source : audit § 7.1.
- [ ] Enregistrer avec **« Envoyer »**.

**La clé est un secret.** Le QR du § 2 bis la contient : ne pas le photographier, ni le diffuser hors de la
préparation.

## 2 bis. Recopier le canal sur les autres radios

Sur le **téléphone du PC** (radio du PC connectée) :

- [ ] Onglet **Messages** → bouton flottant **« Importer / Exporter »** → **« Share Channels »** (non traduit) →
      **« Générer un QR Code »**.

Sur **chaque autre téléphone** (sa radio connectée) :

- [ ] Onglet **Messages** → **« Importer / Exporter »** → **« Scan Shared Channels QR Code »** (non traduit) → scanner
      le code du PC → choisir **« Remplacer »**.
- [ ] Vérifier dans Réglages → Configuration → **Canaux** que le nom du canal est le bon.

D'après le texte de confirmation de l'application, « Remplacer » remplace les canaux **et** les paramètres radio
(LoRa) par ceux du code. Ce point n'a pas encore été essayé : vérifier ensuite l'écran LoRa (§ 1).

## 3. Appareil : rôle et réémission

Chemin : Réglages → Configuration → **Configuration de l'appareil** → **Appareil**.

- [ ] **Rôle de l'appareil** : **Client** pour toute radio portée.
  - Raison : un client relaie, mais renonce s'il entend un voisin relayer avant lui.
  - Source : audit § 7.1 ; documentation `device.mdx` ; article « Choosing The Right Device Role ».
- [ ] **Radios groupées** (plusieurs radios au PC, dans un même véhicule, au même endroit) : **Client muet** pour
      toutes **sauf une**, qui reste Client.
  - Raison : elles émettent et reçoivent leurs propres messages sans réémettre ceux des autres.
  - Source : idem.
- [ ] **Routeur** : seulement pour **1 à 3 radios fixes sur des points hauts dégagés** (mât, sommet), jamais pour une
      radio portée. L'application demande de cocher « J'ai compris que ce rôle change la façon dont cette radio
      relaie le trafic pour tout le réseau maillé. »
  - Raison : un routeur mal placé consomme des sauts, et un routeur réémet tout ; il approche donc la limite de 10 %
    en LONG_FAST.
  - Source : audit § 3.2 et § 7.1 ; firmware `Router.cpp` ; article « Choosing The Right Device Role ».
- [ ] **Mode de réémission** : **Local uniquement**.
  - Raison : la radio ne réémet pas le trafic des autres réseaux, qu'elle ne peut pas déchiffrer.
  - Source : audit § 7.1 ; documentation `device.mdx`, « Rebroadcast Mode ».
- [ ] **Intervalle de diffusion des infos nœud** : ne pas modifier (3 h).
  - Raison : NetworkTact diffuse lui-même le nom après un changement (`FORK.md` § 21).
  - Source : audit § 7.1.

## 4. Position

Chemin : Réglages → Configuration → **Configuration de l'appareil** → **Position**.

- [ ] **Mode GPS (matériel physique)** : activé.
- [ ] **Champs de position** : cocher **`TIMESTAMP`** en plus des cases déjà cochées. Ne rien décocher.
  - Raison : sans `TIMESTAMP`, la position ne porte pas l'heure du relevé ; « Diffuser ma position maintenant » est
    alors refusé, et le récepteur ne sait pas de quand date la position.
  - Source : `FORK.md` § 15.
- [ ] **Intervalle de diffusion** : **15 minutes** (900 s).
  - Raison : un relevé régulier pour la vue PC. Au-delà de 40 radios, le firmware espace de lui-même cet intervalle,
    d'environ 27 min à 50 radios en LONG_FAST.
  - Source : audit § 2.3 et § 7.1 ; firmware `Default.h`.
- [ ] **Position Intelligente** : activé.
- [ ] **Distance intelligente** : **150** (mètres).
- [ ] **Intervalle intelligent** : **5 minutes** (300 s).
  - Raison (pour ces trois réglages) : une radio en mouvement envoie sa position après 150 m parcourus, au plus toutes
    les 5 minutes. C'est la principale source de trafic automatique.
  - Source : audit § 3.3 et § 7.1 ; firmware `PositionModule.cpp`.

## 5. Utilisateur : nom et équipe, réglés pendant la préparation

Chemin : Réglages → Configuration → **Utilisateur**, puis Réglages → **Équipes**.

**Nom et équipe se règlent ici, pendant la préparation, et non sur le terrain.** Si des dizaines de radios changent
d'équipe en même temps, leurs annonces de nom saturent le canal (audit § 4.2).

- [ ] **Nom long** : l'**indicatif seul**, par exemple `ALPHA-1` ou `PC-0`. Ne pas écrire l'équipe entre crochets :
      c'est l'écran Équipes qui l'ajoute.
- [ ] **Nom court** : quelques caractères qui identifient la radio sur la carte et dans la liste des nœuds, par
      exemple `A1`.
- [ ] Réglages → **Équipes** : adopter la liste d'équipes reçue du PC (message `[EQUIPES] …`) ; à défaut, la saisir
      avec la saisie manuelle de secours.
- [ ] Choisir l'équipe de la radio, puis confirmer « Envoyer à ma radio ». Le nom long devient `ALPHA-1 [Alpha]` ; le
      nom court et les autres réglages ne changent pas. Le nom est ensuite diffusé une fois, automatiquement.
- [ ] Radio du PC : c'est elle qui diffuse la liste (« Diffuser la liste (PC) »).

Source : `FORK.md` § 6 et § 21 ; audit § 4.2 et § 6 bis.

## 6. Radio du PC en favori

À faire sur **chaque radio autre que celle du PC**, une fois que la radio du PC a été entendue : elle apparaît dans
l'onglet Réseau.

- [ ] Onglet **Réseau** → toucher la radio du PC → fiche du nœud → **Favoris** (étoile). En mode Terrain, on peut
      aussi faire un appui long sur la radio dans la liste des nœuds → **« Ajouter aux favoris »**.
- [ ] Faire de même pour les **radios fixes en Routeur** et pour la radio qui reçoit les demandes de tir, si ce n'est
      pas celle du PC.
  - Raison : la radio garde 200 nœuds au plus et retire les plus anciens, sauf les favoris. Un favori reste
    joignable en message direct, demande de tir comprise.
  - Source : audit § 2.1 et § 4.8 ; firmware `NodeDB.cpp`.

Le favori est enregistré dans la radio (pas seulement dans le téléphone).

## 7. Après l'essai de portée à Cincu : passer à MEDIUM_FAST ou MEDIUM_SLOW

À faire **seulement si l'essai de portée le permet** (audit § 6 bis, ordre 4).

**Choix du préréglage.**

- `MEDIUM_FAST` divise le temps d'antenne par 3,3, au prix de 5 dB de bilan de liaison.
- `MEDIUM_SLOW` le divise par 1,8, au prix de 2,5 dB.
- Prendre `MEDIUM_FAST` si les liaisons les plus longues de l'essai passent encore, sinon `MEDIUM_SLOW`.

Source : audit § 3 et § 7.1 ; article Meshtastic « Is LongFast Holding Your Mesh Back? » du 22 avril 2025.

**Toutes les radios doivent changer ensemble.** Une radio restée en `LONG_FAST` n'entend plus les autres. Faire le
changement en une seule séance, toutes les radios réunies, avant le départ, jamais sur le terrain. Procédure :

- [ ] Sur la radio du PC : Réglages → Configuration → **LoRa** → **Préréglages** → `MEDIUM_FAST` (ou
      `MEDIUM_SLOW`). Enregistrer et attendre la reconnexion.
- [ ] Sur chaque autre radio, au choix :
  - même réglage, à la main ;
  - ou nouveau QR de canal du PC, avec « Remplacer » (§ 2 bis), qui reprend aussi les paramètres LoRa. Vérifier
    ensuite l'écran LoRa.
- [ ] En mode PC, mettre à jour le réglage attendu : onglet **Réseau** → icône **« Ouvrir le diagnostic de
      liaison »** → carte **« Réglage LoRa attendu »**. Le diagnostic signale alors toute radio restée sur l'ancien
      préréglage (`FORK.md` § 19 et § 25).
- [ ] Faire un essai de messages entre deux radios éloignées avant de partir.

## 8. Vérifier

- [ ] Ouvrir l'écran de diagnostic de liaison : radio connectée, réglage LoRa réel conforme (`EU_868`, `LONG_FAST`,
      ou le préréglage retenu après Cincu), autres nœuds entendus.
- [ ] Sur une autre radio, le nœud apparaît avec son nom long `ALPHA-1 [Alpha]`, et sa position porte une heure de
      relevé.
- [ ] La radio du PC porte l'étoile des favoris.

Une radio **immobile** peut s'afficher « POSITION ANCIENNE » sur la vue PC alors qu'elle fonctionne. C'est connu, et
sera corrigé par C2 (`TESTS-A-FAIRE.md` § 2).

## 9. Ce qui ne se règle pas depuis NetworkTact

- **La version du firmware.** Elle s'installe avec l'outil de flashage officiel (`FLASHER-LA-RADIO.md`). Mettre la
  **même version** sur toutes les radios.
- **Les limites internes du firmware**, qui ne sont pas des réglages :
  - 200 nœuds au plus dans la base d'une T-Beam Supreme ;
  - au-delà de 40 nœuds, les intervalles s'allongent automatiquement ;
  - positions et télémétrie retenues au-delà de 25 % d'occupation du canal, NodeInfo au-delà de 40 % ;
  - limite légale de 10 % d'émission par heure.
  Source : audit § 2.
- **Le réglage de plusieurs radios d'un coup.** Chaque radio se règle depuis son propre téléphone, une par une. Seuls
  les canaux, et les paramètres LoRa, se recopient par QR (§ 2 bis).
  L'administration à distance existe dans l'application d'origine, mais elle demande une clé d'administration sur
  chaque radio : elle n'est pas couverte ici.
- **L'emplacement et l'antenne des relais fixes** : choix de terrain (point haut, dégagement).
- **Le démarrage échelonné** et la discipline d'emploi (`[POS]`, traceroute, canaux) : ce sont des consignes, voir
  audit § 7.2.
- **Les autres réglages conseillés par l'audit** ne sont pas dans cette liste. Il s'agit de la télémétrie de
  l'appareil sur le réseau et de MQTT (Réglages → Configuration → Réglages du module). Voir audit § 7.1.
