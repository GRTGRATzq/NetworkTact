# Plan : guide utilisateur visuel (branche `docs/guide-utilisateur`)

Branche partie de `feat/demande-de-tir` (`1bca152`, run `fork-apk` 106 vert). Ce document prépare la **matière** du guide :
inventaire des fonctions, structure en deux profils (Administrateur, Utilisateur), fiches réflexes, annexe « Configuration
d'exercice », glossaire et production des captures. Le guide
sera mis en page ailleurs. **Aucun code n'est écrit avant les décisions du § 7.**

Sources : le code de la branche (écrans, textes français `values-fr/strings.xml`, données de démo `DemoDataSet`),
`FORK.md` § 1 à 30, `docs/SPEC-DEMANDE-DE-TIR.md`, `CARTE-HORS-LIGNE.md`, `PREPARER-LES-RADIOS.md`. Les textes cités entre
guillemets sont ceux de l'application en français ; le guide ne doit rien décrire qui ne soit dans l'application.

## 1. Inventaire des fonctions

Légende de la colonne « Radio » : **émet** (ce qui part par radio, et quand), **n'émet jamais**. Tout ce qui n'est pas
marqué « émet » ne part jamais par radio. Rien n'est jamais émis automatiquement, sauf la diffusion du nom après un
changement de nom ou d'équipe (§ 1.13).

### 1.1 Navigation générale

| Élément | Où | À quoi il sert | Gestes et boutons | Radio | États et messages visibles |
|---|---|---|---|---|---|
| Barre de navigation | bas de l'écran (rail sur écran large) | 5 onglets : **Messages, Réseau, Carte, Réglages, Ma radio** | toucher un onglet ; badge de non-lus sur Messages | n'émet jamais | icône Ma radio : deux ondes (connectée, vert), une onde (en connexion), croissant (en veille), barrée avec « ! » (déconnectée) |
| Écran de bienvenue | au premier lancement ; Réglages → Information → « Afficher l'introduction » | présentation, insignes de la 4ème batterie et de la batterie d'acquisition spécialisée | « Commencer » | n'émet jamais | ligne verrouillée en mode démo |
| Bandeau de diagnostic | haut des Messages, de la liste des nœuds et de la vue PC | prévenir d'une liaison perdue ou d'un silence | toucher : ouvre le diagnostic ; croix : fermer | n'émet jamais | « Radio non connectée », « Aucun nœud entendu depuis 18 min », « Aucun autre nœud entendu » ; revient au seuil suivant (30 min, 1 h, puis chaque heure) |
| Thème | Réglages → Thème | Clair, Sombre, Valeur par défaut du système, Dynamique | choix dans la liste | n'émet jamais | couleurs : rouge réservé à URGENT, ambre aux comptes rendus, vert à la position fraîche |

### 1.2 Vue Terrain et vue PC (`FORK.md` § 3, 10, 14)

| Élément | Où | À quoi il sert | Gestes et boutons | Radio | États et messages visibles |
|---|---|---|---|---|---|
| Choix Terrain / PC | Réglages → « Mode poste de commandement » | Terrain : affichage d'origine ; PC : l'onglet Réseau s'ouvre sur la vue PC | interrupteur | n'émet jamais | « Mode d'affichage : désactivé pour l'affichage Terrain (inchangé), activé pour ouvrir la vue PC (lecture seule) au démarrage. » |
| Vue PC « Poste de commandement » | onglet Réseau (en mode PC) | tous les nœuds connus, lecture seule : nom, équipe, dernier contact, âge de la position | défilement ; toucher un nœud : sa fiche ; icônes du haut : organigramme, diagnostic | n'émet jamais | recalculée toutes les 15 s ; « POSITION ANCIENNE : relevé il y a … » (au-delà de 10 min), « Reçue il y a …, heure de relevé inconnue », « Horodatage incohérent (… dans le futur) », « Aucune position », « Aucun nœud connu pour l'instant. » ; pastille inversée pour ANCIENNE / incohérente, vert pour une position fraîche |
| Liste des nœuds | onglet Réseau (en mode Terrain) | écran d'origine de Meshtastic | toucher un nœud : sa fiche | selon les actions de la fiche (traceroute, demande d'infos…) | — |

### 1.3 Messagerie (`FORK.md` § 4, 8)

| Élément | Où | À quoi il sert | Gestes et boutons | Radio | États et messages visibles |
|---|---|---|---|---|---|
| Liste des conversations | onglet Messages | canaux et messages directs | onglets par canal (« Tous », un par canal) ; toucher une conversation | n'émet jamais | — |
| Conversation | toucher une conversation | lire et envoyer | zone de saisie, bouton d'envoi, **sélecteur de priorité** (« Info », « CR », « Urgent ») à gauche du champ ; toucher le nom de l'expéditeur : sa fiche | **émet** le message, sur le canal ou en direct, au toucher du bouton d'envoi | en-tête « [URG] ALPHA-1 · Équipe Alpha · Direct » ; « Moi → BRAVO-2 · Direct » ; ligne « Reçu · il y a 2 min », « heure inconnue », « heure incohérente (… min dans le futur) » ; badges URGENT / COMPTE RENDU / INFO |
| États d'envoi | sous chaque message envoyé | savoir où en est le message | « Diagnostic » sous un échec ; « Renvoyer » | **émet** au « Renvoyer » | « En attente de ma radio », « Transmis à ma radio », « Relayé par le réseau », « Accusé par X » (« (vérifié) »), « Échec : … » avec la raison (aucun nœud n'a confirmé la réception, aucune route vers le destinataire, aucune réponse dans le délai, message trop long, envoi trop rapide, refusé par un nœud, interface radio indisponible) ; « ALERTE : preuve de livraison invalide, falsification possible ». Un message de canal n'est jamais « accusé ». |
| Filtres | haut d'une conversation (« Filtres des messages ») | n'afficher qu'une priorité, les messages directs « Sans accusé », une « Équipe » | puces de filtre | n'émet jamais | « Aucun message ne correspond aux filtres » |
| Priorités | préfixe en tête du message | `[URG]` urgent, `[CR]` compte rendu, sans préfixe : information | choix par le sélecteur | — | barre rouge (urgent), ambre (CR), neutre (info) |
| Voir sur la carte | sous un message reçu qui contient une coordonnée complète | centrer la carte sur ce point | bouton | n'émet jamais | bandeau sur la carte : « Position relevée 14:32 · ALPHA-1 », « Position ANCIENNE relevée … », « Fait observé 14:05 · … », « Point d'un message · heure de relevé inconnue · … » ; ✕ pour retirer le marqueur ; « Carte indisponible en mode démo » (variante google) |
| Partager ce point sur la carte | encart après l'envoi d'un message qui contient une coordonnée | publier un point de repère | « Partager » / « Ne pas partager » | **émet** un point de repère (24 h) seulement sur « Partager » | « Partage simulé (mode démo) : aucun point émis. » |

### 1.4 Ma position, Fait observé, convertisseur (`FORK.md` § 7, 12, 23)

Puces au-dessus de la zone de saisie : « Ma position · Fait observé · Convertisseur », et sur sa ligne « DT Demande de
tir » (§ 1.12). Aucune de ces puces n'envoie : elles remplissent la zone de saisie, sauf la demande de tir.

| Élément | À quoi il sert | Gestes et boutons | Radio | États et messages visibles |
|---|---|---|---|---|
| Ma position | remplit `[POS] ALPHA-1 · MGRS … · UTM … · DMS … · relevée 14:32` | toucher la puce, relire, envoyer | **émet** seulement à l'envoi du message | « Ma radio n'a pas de position : celle du téléphone est utilisée et le message le précise. » ; « Raccourci pour tenir en 200 octets : DMS retiré, puis UTM si nécessaire. » ; « Aucune position : … Rien n'a été rempli. » ; « POSITION ANCIENNE, relevée il y a 25 min (14:07) » ; « heure de relevé inconnue » |
| Fait observé | formulaire : lieu (UTM, MGRS ou DMS), « Observé à » (`14:05`, `14h05`, `1405`), description → `[CR] FAIT OBSERVÉ · 14:05 · Lieu … = MGRS … · description` | « Insérer » ; « Terminé » ferme le clavier | **émet** seulement à l'envoi | « Trop long : N/200 octets. Raccourcissez la description. » (bouton grisé) ; `(veille)` pour une heure pas encore atteinte ; erreurs de lieu du convertisseur |
| Convertisseur de coordonnées | saisie dans un format, affichage des deux autres ; hors ligne, WGS84 | onglets MGRS / UTM / DMS ; copier ; DMS tapé sans symboles (`48 51 24 N 2 21 3 E`) | n'émet jamais | « Zone inconnue : zones 1 à 60 ; 32X, 34X et 36X n'existent pas. », « Bande de latitude inconnue : lettres C à X, sans I ni O. » ; limites : « UTM et MGRS s'arrêtent à 84° N et 80° S » |

### 1.5 Équipes (`FORK.md` § 6, 21)

| Élément | Où | À quoi il sert | Gestes et boutons | Radio | États et messages visibles |
|---|---|---|---|---|---|
| Liste des équipes | Réglages → Équipes | la liste `[EQUIPES] Alpha;Bravo;Charlie` adoptée par ce téléphone | adopter une liste reçue (dialogue avec émetteur et heure) ; saisie manuelle de secours | n'émet jamais (adoption) | « Aucune liste d'équipes pour l'instant. Attendez celle du PC, ou saisissez-la ci-dessous. » ; « Aucun nom d'équipe. » ; « Aucune authentification : … » |
| Diffuser la liste (PC) | même écran, en mode PC | envoyer la liste aux autres téléphones | choix du « Canal », « Diffuser » | **émet** un message texte `[EQUIPES] …` sur le canal choisi | « Liste placée dans la conversation …, où s'affiche son état d'envoi. » ; « Non diffusée. » |
| Mon équipe | même écran | suffixe `[Alpha]` ajouté au nom long de sa radio | choix de l'équipe | **émet** : la radio reçoit le nouveau nom, puis l'application diffuse une fois le nom (NodeInfo) | « Trop long pour la radio (N octets) : la partie nom est raccourcie, l'équipe est conservée. » ; rien si la radio n'est pas connectée |
| Diffuser mon nom | même écran, et Réglages de l'appareil → Utilisateur | renvoyer son nom aux autres radios | bouton, confirmation, puis 30 s d'attente avec décompte | **émet** un NodeInfo sur le canal principal | « Aucune radio connectée : diffusion impossible. » ; « Le nom de ma radio n'est pas encore connu. Attendez la fin de la connexion. » ; en démo « diffusion simulée, rien n'est envoyé » |
| Affichage de l'équipe | messagerie, vue PC | « ALPHA-1 · Équipe Alpha » ; filtre « Équipe » | — | — | « Aucune liste d'équipes » (filtre) |

### 1.6 Organigramme (`FORK.md` § 17, 26)

| Élément | Où | À quoi il sert | Gestes et boutons | Radio | États et messages visibles |
|---|---|---|---|---|---|
| Organigramme | onglet Réseau → icône « Ouvrir l'organigramme » | PC, équipes, membres (indicatif, fonction), avec dernier contact et âge de position de chaque radio | toucher un membre : sa fiche ; « Message » : sa conversation | n'émet jamais | « Aucune radio associée », « Aucun membre », « La radio ne déclare aucune équipe », « Aucun organigramme. En mode PC, ajoutez des membres ; sinon, scannez le QR du PC. » |
| Édition (mode PC) | même écran | ajouter, modifier, retirer, monter, descendre | boîte de saisie ; « Radios connues » à l'ajout | n'émet jamais | « Indicatif trop long : N octets, 39 au plus », « Fonction trop longue : N octets, 24 au plus » |
| Partage par QR | PC : afficher le code ; autres : scanner | diffuser l'organigramme sans radio | afficher, scanner, confirmer l'adoption ; « Revenir au précédent » | n'émet jamais (écran et caméra) | « Organigramme de PC-0, créé à HH:MM » ; « Organigramme trop grand pour un QR lisible (…) » ; code abîmé refusé en entier ; « Aucune authentification : … N'adoptez que celui que montre votre PC. » |

### 1.7 Carte (`FORK.md` § 12 à 15, 27, 29 ; variante fdroid)

| Élément | Où | À quoi il sert | Gestes et boutons | Radio | États et messages visibles |
|---|---|---|---|---|---|
| Carte | onglet Carte | nœuds, points de repère, marqueur « Voir sur la carte » | toucher une puce : encart « Relevée à 14:32 », « Détails » ; menu des fonds de carte | n'émet jamais | étiquettes « ANCIENNE », « HEURE INCOHÉRENTE » sur les puces |
| Coordonnée à l'appui long | appui long sur la carte | lire le point en MGRS, UTM, DMS (« lieu choisi, sans heure ») | « Copier », « Insérer dans un message », « Ouvrir dans le convertisseur », « Créer un point de repère ici » | **émet** seulement « Créer un point de repère ici » (point de repère) ; les autres n'émettent jamais | texte copié `Point carte · MGRS … · UTM … · DMS …` |
| Recaler | bouton en haut à droite | centrer sur Ma radio ou Ce téléphone, comparer les deux GPS | « Centrer sur : Radio / Téléphone » | n'émet jamais | âge, heure de relevé, satellites, HDOP, PDOP, précision ; écart en mètres ; « Pas de position du téléphone (…) », « Précision inconnue. » |
| Diffuser ma position maintenant | encart de « Recaler » | envoyer une fois la position de la radio | confirmation, puis 30 s d'attente (« Nouvelle diffusion possible dans N s ») | **émet** une position standard sur le canal principal, sans accusé | « Aucune radio connectée : diffusion impossible. », « La radio n'a pas de position à diffuser. … », « La radio n'a pas donné l'heure de son relevé GPS … Activez l'horodatage … », « L'heure de relevé de la radio est dans le futur … », « Cette position est ANCIENNE : relevée il y a … », « Mode démo : rien n'est diffusé. » ; « Position diffusée à 14:35 (relevée à 14:34) » |
| Cartes sur le téléphone | menu des fonds → « Cartes sur le téléphone » | importer, lister, supprimer, exporter des cartes hors ligne | « Importer un fichier .pmtiles », « Télécharger une carte », « Exporter », « Reprendre », supprimer (confirmation) | n'émet jamais (Internet seulement pour le téléchargement) | « Aucune carte sur ce téléphone. » ; refus d'un fichier qui n'est pas un PMTiles v3, d'une archive raster, espace insuffisant ; ligne « Édition du …, hôte · L × H km » ; « Téléchargement interrompu » |
| Télécharger une carte | même écran | cadre à poignées, « Utiliser la vue », zoom 12 à 15, taille, source | « Calculer la taille », confirmation au-delà de 50 Mo, « Annuler », « Reprendre » | n'émet jamais ; passe par Internet (le serveur voit la zone et l'adresse du téléphone) | « La zone est trop petite ou inversée. », « La zone est trop grande : … », « Impossible de joindre la source … », « Aucune édition de carte n'a été trouvée … » ; plafond 500 Mo ; refusé en démo |
| Crédit ⓘ | coin de la carte | mentions des données | toucher ⓘ | — | « © contributeurs OpenStreetMap · Protomaps » |

### 1.8 Diagnostic de liaison (`FORK.md` § 19, 25)

| Élément | Où | À quoi il sert | Gestes et boutons | Radio | États et messages visibles |
|---|---|---|---|---|---|
| Diagnostic | onglet Réseau → icône antenne relais ; bandeau ; « Diagnostic » sous un échec | constats, sans verdict | lecture | n'émet jamais | « Liaison Bluetooth : radio non connectée », « Aucun paquet reçu de ma radio depuis le lancement de l'application », nœuds entendus en 5, 15, 60 min, « Aucun nœud entendu depuis … Dernier : … à … », réglages LoRa, batterie ; suggestions (vérifier la radio, la région, se rapprocher d'un point haut, l'antenne) |
| Réglage LoRa attendu (PC) | même écran | comparer la radio au réglage voulu | listes région / préréglage, reprise, effacement | n'émet jamais | « Ma radio est en EU_868 · LONG_FAST : écart avec ce réglage. », « Identique au réglage de ma radio. », « Aucun réglage saisi » |
| Tester la liaison | même écran | traceroute vers un nœud entendu | choix du nœud, confirmation, 30 s d'attente | **émet** un traceroute | « Aucun nœud à tester : aucun n'a été entendu. », « Radio non connectée : rien ne peut être émis. », « Demande remise à la radio à 14:05 » |

### 1.9 Demande de tir (`FORK.md` § 30, `docs/SPEC-DEMANDE-DE-TIR.md`)

| Élément | Où | À quoi il sert | Gestes et boutons | Radio | États et messages visibles |
|---|---|---|---|---|---|
| Puce « DT Demande de tir » | sous les puces d'une conversation | ouvrir le formulaire | toucher | n'émet jamais | — |
| Formulaire | écran plein « Demande de tir · Brouillon gardé sur le téléphone » | en-tête et 11 cases de la fiche du 68e RAA | « Renouveler » (confirmation si brouillon), « Coller une coordonnée », fuseau loc / Z / fra, barre « N octets · N messages », « Aperçu » | n'émet jamais | raisons de l'envoi grisé : n° de tir vide, destinataire, direction Rep manquante, plus de 9 parties, « Radio non connectée … » ; GDH illisible ; avertissement d'espace dans un champ |
| Aperçu avant envoi | « Aperçu » | parties exactes, « Message k / N · X octets sur 200 » | « Modifier », « Envoyer la demande » | **émet** N messages directs avec accusé, à « Envoyer la demande » | — |
| Carte émetteur | conversation directe | « DT AB1234 · 3 messages », état de chaque partie, GDH reçus | « Renvoyer la partie k », « Voir les N messages bruts » | **émet** au renvoi d'une partie | « GDH attribution reçu : … », « GDH fin : en attente » |
| Fiche reconstituée | conversation directe du destinataire | la fiche en clair (codes expliqués) | « Voir sur la carte » (case 7), « GDH attribution », « GDH fin », messages bruts | **émet** un court message par GDH envoyé | « k / N reçus », « Partie(s) k manquante(s) (cases …) », « non rempli », « non reçu » |
| Notification | système | une seule par demande | — | — | « Demande de tir AB1234 · 2 / 3 reçus » |

### 1.10 Mode démo (`FORK.md` § 9, 22)

| Élément | Où | À quoi il sert | Gestes et boutons | Radio | États et messages visibles |
|---|---|---|---|---|---|
| Mode démo | Réglages → « Mode démo » | montrer l'application sur des données fictives | interrupteur ; « Quitter » dans le bandeau | n'émet jamais (envois simulés) | bandeau « MODE DÉMO, données fictives · envois simulés », « Messages réels reçus : N » ; « Indisponible en mode démo » sur les écrans non permis ; onglet Ma radio : « Radio de démo : rien n'est envoyé » |

Données fictives (servent aussi aux captures) : PC-0 (ce téléphone), ALPHA-1 [Alpha], BRAVO-2 [Bravo], CHARLIE-3 [Alpha] ;
canaux Général, Équipe Alpha, PC ; positions récente, ancienne et sans heure ; messages Info, CR, Urgent, `[POS]`,
`[CR] FAIT OBSERVÉ`, envois dans chaque état, CHARLIE-3 en échec ; organigramme avec DELTA-4 sans radio ; diagnostic
EU_868 / LONG_FAST ; demande de tir AB1234 reçue d'ALPHA-1 avec GDH attribution.

### 1.11 Réglages et radio (écrans d'origine, cités seulement)

Réglages de l'appareil (Utilisateur : nom long, nom court ; Position ; LoRa ; Canaux…), Ma radio (connexion Bluetooth,
liste des appareils), mise à jour du firmware radio, À propos, Remerciements (licences). Le guide renvoie pour ces écrans à
`PREPARER-LES-RADIOS.md` (réglages à faire) et `FLASHER-LA-RADIO.md`, sans les détailler.

## 2. Structure du guide (décision du 13 novembre 2026)

Le guide est **général** : il ne dépend ni du nombre de radios (le réseau visé dépasse 50 radios), ni d'une organisation
particulière. Il est organisé selon **deux profils** :

- **Administrateur** : prépare et tient le réseau. Préparation des radios (renvoi à `PREPARER-LES-RADIOS.md`), liste des
  équipes, organigramme, réglages du téléphone (vue Terrain ou PC, réglage LoRa attendu), vue PC et diagnostic, cartes
  hors ligne à préparer et à distribuer. Les fonctions réservées au mode PC (diffuser la liste des équipes, modifier
  l'organigramme, saisir le réglage LoRa attendu) sont dans ce profil.
- **Utilisateur** : se sert de l'application en mission. Messages, position, fait observé, convertisseur, carte, demande
  de tir, adoption de la liste des équipes et de l'organigramme, choix de son équipe, premier diagnostic.

Les postes d'un exercice (CDU, PCR, OA, CDS TIR, Drone) ne figurent que dans l'annexe **« Configuration d'exercice »**
(§ 2.2), remplaçable sans toucher au reste. Le corps du guide parle de profils et de fonctions, jamais de postes ; les mots
de la fiche de demande de tir (OA, CDS TIR dans l'exemple validé) restent cités comme exemples de la fiche.

1. **Prise en main** (les deux profils) : installer l'APK, écran de bienvenue, associer sa radio (Ma radio), lire la barre
   de navigation et l'icône d'état, thème.
2. **Les règles de base** (les deux profils) : ce qui part par radio et ce qui n'y part jamais (tableau unique tiré du § 1),
   200 octets par message, rien d'automatique, aucune authentification (équipes, organigramme), heures et âges (fraîche,
   ANCIENNE, inconnue, incohérente), temps d'antenne partagé par tout le réseau.

**Partie A, Administrateur**

3. **Préparer les radios** : renvoi à `PREPARER-LES-RADIOS.md` et `FLASHER-LA-RADIO.md` (D12), sans les reprendre.
4. **Préparer les téléphones** : vue Terrain ou PC (« Mode poste de commandement »), mode démo pour former.
5. **Équipes** : composer et diffuser la liste, suivre son adoption, diffuser son nom.
6. **Organigramme** : construire, partager par QR, revenir au précédent.
7. **Vue PC et diagnostic** : suivre le réseau, bandeau, diagnostic, réglage LoRa attendu, tester la liaison.
8. **Cartes à distribuer** : importer, télécharger, exporter, faire passer les fichiers aux autres téléphones (procédure
   de `CARTE-HORS-LIGNE.md`), supprimer.

**Partie B, Utilisateur**

9. **Messagerie** : conversations, priorités, états d'envoi et raisons d'échec, filtres, renvoi.
10. **Positions et coordonnées** : Ma position, Fait observé, convertisseur, Voir sur la carte, partager un point.
11. **Carte** : lecture des puces, appui long, Recaler et Diffuser ma position, importer une carte reçue.
12. **Équipes et organigramme côté utilisateur** : adopter la liste, choisir son équipe, adopter l'organigramme.
13. **Demande de tir** : formulaire case par case, aperçu, envoi, carte émetteur, fiche reçue, GDH, brouillon, Renouveler.
14. **Premier diagnostic** : bandeau, « Diagnostic » sous un échec, quoi vérifier avant d'appeler l'administrateur.

**Communs et annexes**

15. **Mode démo** : entrer, ce qui est simulé, quitter.
16. **Dépannage** : tous les messages d'erreur, classés par écran, avec la cause, le geste et le profil concerné (repris
    du § 1).
17. **Annexes** : glossaire (§ 2.4), fiches réflexes (§ 2.1), **Configuration d'exercice** (§ 2.2).

### 2.1 Fiches réflexes (une page chacune)

Gabarit : **Profil** (Administrateur, Utilisateur ou les deux) · **Objectif** (une phrase) · **Avant de commencer** (radio
connectée, vue Terrain ou PC) · **Étapes numérotées** (6 au plus, un geste par étape, le bouton écrit tel qu'il apparaît) ·
**Capture** annotée (numéros des étapes sur l'image) · **Ce qui part par radio** · **Erreurs fréquentes** (message exact
→ que faire). Liste validée le 13 novembre 2026, profil ajouté ; la colonne « Postes » est passée dans l'annexe (§ 2.2).

| # | Fiche | Profil |
|---|---|---|
| FR1 | Envoyer un message urgent et vérifier l'accusé | Utilisateur |
| FR2 | Transmettre sa position (`[POS]`) | Utilisateur |
| FR3 | Rendre compte d'un fait observé | Utilisateur |
| FR4 | Convertir une coordonnée | Utilisateur |
| FR5 | Lire une coordonnée sur la carte et l'envoyer | Utilisateur |
| FR6 | Comparer GPS radio et téléphone, diffuser sa position | Utilisateur |
| FR7 | Adopter la liste des équipes et choisir son équipe | Utilisateur |
| FR8 | Diffuser la liste des équipes | Administrateur |
| FR9 | Construire et partager l'organigramme par QR | Administrateur |
| FR10 | Adopter l'organigramme du PC | Utilisateur |
| FR11 | Que faire quand un message est en échec (diagnostic) | Utilisateur et Administrateur |
| FR12 | Suivre le réseau depuis la vue PC | Administrateur |
| FR13 | Rédiger et envoyer une demande de tir | Utilisateur |
| FR14 | Recevoir une demande de tir et renvoyer les GDH | Utilisateur |
| FR15 | Renvoyer une partie manquante | Utilisateur |
| FR16 | Importer ou télécharger une carte hors ligne | Administrateur (préparer et distribuer) et Utilisateur (importer une carte reçue) |
| FR17 | Présenter l'application en mode démo | Utilisateur et Administrateur |

Proposées en plus, à valider (D14) : **FR18** « Préparer une radio » (Administrateur, une page qui renvoie aux étapes de
`PREPARER-LES-RADIOS.md`) ; **FR19** « Saisir le réglage LoRa attendu et repérer une radio en écart » (Administrateur).

### 2.2 Annexe « Configuration d'exercice »

Une annexe par exercice, remplaçable sans toucher au corps du guide. Elle donne, pour chaque poste : développé du sigle,
rôle, profil (Administrateur ou Utilisateur), vue (Terrain ou PC), fonctions utilisées dans l'ordre de la mission, fiches
réflexes à garder sous la main, et les réglages propres à l'exercice (équipes, organigramme, canaux, réglage LoRa attendu).
Le développé des sigles et le rôle de chaque poste seront fournis par le porteur du projet ; rien n'est déduit.

Première annexe, répartition validée le 13 novembre 2026 :

| Poste | Vue | Fonctions principales | Fiches |
|---|---|---|---|
| CDU | à préciser | messagerie, vue PC ou Terrain, organigramme, diagnostic | FR1, FR11, FR12 ou FR10 |
| PCR | PC | vue PC, équipes, organigramme, diagnostic et réglage LoRa attendu, messagerie | FR8, FR9, FR11, FR12 |
| OA | Terrain | Ma position, Fait observé, convertisseur, carte, demande de tir | FR2 à FR5, FR13, FR15 |
| CDS TIR | Terrain (ou PC) | réception des demandes de tir, GDH, Voir sur la carte | FR14, FR1, FR4 |
| Drone | Terrain | Fait observé, coordonnée sur la carte, demande de tir | FR3, FR5, FR13 |

### 2.3 Réseau de plus de 50 radios : limites actuelles à écrire dans le guide

Le guide décrit l'application telle qu'elle est. Limites relevées dans le code et `FORK.md`, à signaler (rien n'est
changé ici) :

- **Organigramme par QR** : 770 caractères au plus (QR de version 22) ; 9 membres aux indicatifs et fonctions de longueur
  maximale tiennent en 720 caractères. Avec des noms courts, davantage tiennent, mais un organigramme de plus de 50 radios
  ne tiendra probablement pas dans un seul QR : l'ajout est alors refusé avec la longueur atteinte. À mesurer sur les
  indicatifs réels ; une évolution (plusieurs QR, format compact) serait une tâche à part.
- **Liste des équipes** : un message de 200 octets, 16 octets par nom.
- **Temps d'antenne** : chaque envoi est partagé par tout le réseau ; diffusions de position et de nom, traceroutes et
  demandes de tir (jusqu'à 9 messages) à utiliser avec retenue.
- **Vue PC** : tous les nœuds connus en une liste, recalculée toutes les 15 s ; pas de regroupement par équipe.

### 2.4 Glossaire (abréviations affichées par l'application seulement)

`[URG]`, `[CR]`, INFO, `[POS]`, FAIT OBSERVÉ, `[EQUIPES]`, PC, DT, GDH, loc / Z / fra, mois OTAN (JAN … DEC), OA, CDS TIR,
TLE, L R U, Rep, NLT, HSO, MGRS, UTM, DMS, WGS84, HDOP, PDOP, SNR, RSSI, LoRa, EU_868, LONG_FAST, NodeInfo, traceroute,
point de repère, `.pmtiles`, `.mbtiles`, QR, PKI, millièmes (m barré), « non rempli », « non reçu ». Chaque définition
reprend la fiche, la spécification ou `FORK.md` ; aucune abréviation absente de l'application. OA et CDS TIR y figurent
comme mots de l'exemple de la fiche de demande de tir ; les sigles des postes d'un exercice (CDU, PCR…) sont définis dans
l'annexe « Configuration d'exercice », pas dans le glossaire.

## 3. Captures d'écran

### 3.1 Ce que le projet contient déjà

- **Compose Preview Screenshot Testing** (CST, plugin Google `com.android.compose.screenshot` `0.0.1-alpha16`, licence
  Apache-2.0, compatible avec la GPL-3.0), **déjà dans le dépôt** : modules `:screenshot-tests` (contrôle de régression
  amont, non lancé par `fork-apk`) et **`:docs-screenshots`** (« generate-only », jamais contrôlé en CI, compositions
  cadrées pour une documentation), convention `AndroidScreenshotConventionPlugin`, tâche `copyDocsScreenshots` qui renomme
  les images par un manifeste. Le rendu passe par **layoutlib** (le moteur d'aperçu d'Android Studio) sur la JVM : **sans
  émulateur**, mais avec le SDK Android, présent sur les machines GitHub. Commande : `./gradlew
  :docs-screenshots:updateDebugScreenshotTest` ; les PNG sortent dans `src/screenshotTestDebug/reference/`.
- **`:store-screenshots`** et `store-screenshots.yml` : captures de la vraie application, mais **sur émulateur** (KVM,
  `android-emulator-runner`) : écarté par la consigne.
- **Tests Compose JVM** (`runComposeUiTest`, déjà dans `feature:messaging` et `feature:map-maplibre`) : peuvent produire une
  image par `onRoot().captureToImage()`, sans SDK Android ; rendu de bureau (Skia), polices et métriques différentes d'un
  téléphone.
- Aucune Roborazzi ni Paparazzi.

### 3.2 Méthode proposée : CST dans un module du fork, sans bibliothèque nouvelle

- Nouveau module **`:guide-screenshots`** (sur le modèle de `:docs-screenshots`, mêmes plugins, aucune dépendance nouvelle),
  ou ajout au module `:docs-screenshots` existant (D4). Il dépend de `core:demo` pour les données fictives
  (`DemoDataSet.create(13 novembre 2026, 14:35, fuseau Europe/Bucharest)`, D9, donc des captures identiques d'un run à
  l'autre) et des modules
  d'écran.
- Une fonction de capture par écran, `@PreviewTest`, format téléphone (`device = "spec:width=411dp,height=891dp,dpi=420"`,
  proche d'un Pixel), **thème clair**, et **sombre** par `uiMode` (deux images par écran, coût négligeable). Langue
  française forcée (`locale = "fr"`).
- Les écrans du fork ont presque tous un contenu sans ViewModel : `CommandPostContent`, `OrgChartContent`,
  `DiagnosticContent`, `TeamsContent`, `CoordinateConverterContent`, `FireRequestPreviewScreen`, `FireRequestCard`,
  `FireRequestSizeBar`. Pour eux, la capture construit l'état à partir des données de démo. Pour les écrans liés à un
  ViewModel (conversation, liste des conversations, formulaire de demande de tir, Fait observé, réglages), deux options
  (D5) : (a) extraire un contenu sans ViewModel (petits commits de code, testés) ; (b) construire le vrai ViewModel avec
  les dépôts de démo (comme les tests JVM du § 22 de la demande de tir), possible mais plus fragile sous layoutlib.
- Les fonctions `internal` (cartes de la demande de tir, contenus de la vue PC…) ne sont pas visibles depuis un autre
  module : il faudra soit les rendre publiques, soit placer les fonctions de capture dans le source set `screenshotTest`
  d'un module qui les voit (D5).
- **Workflow séparé** `fork-guide-captures.yml`, lancé **à la main seulement** (`workflow_dispatch`), sur une branche
  choisie : `updateDebugScreenshotTest` du module, puis artefact `guide-captures` (PNG nommés par fiche : `FR13-1-formulaire-clair.png`),
  conservé 30 jours. `fork-apk` n'est pas touché ; ses chemins ignorés n'ont pas à changer, car le nouveau workflow ne se
  déclenche jamais seul.
- **Coût estimé** : compilation Android des modules d'écran (comme les étapes « Compile » et « Tests » de `fork-apk`, une
  quinzaine de minutes à froid, moins avec le cache Gradle), puis environ 1 à 2 s par image ; pour 60 écrans × 2 thèmes,
  15 à 20 minutes par run. À mesurer au premier run.
- **Risques** : CST est en version alpha (le seuil d'écart a déjà changé à alpha16 ; sans effet ici, car rien n'est
  comparé) ; la configuration isolée de Gradle (`isolated-projects`) peut demander les mêmes options que `store-screenshots`
  ; layoutlib ne rend pas tout (voir § 3.3) ; le rendu peut différer légèrement d'un vrai téléphone (polices identiques,
  Roboto, mais pas les barres système).

### 3.3 Ce qui ne se capture pas ainsi

| Écran | Pourquoi | Proposition (D6) |
|---|---|---|
| Carte MapLibre (fond, puces, appui long, Recaler, cadre de téléchargement) | vue native MapLibre, non rendue par layoutlib ni par le rendu de bureau | captures à la main par l'utilisateur, en mode démo (fdroid), selon une liste fournie ; ou plus tard un workflow sur émulateur (comme `store-screenshots`), hors consigne actuelle |
| Notifications, sélecteur de fichiers Android, caméra du QR | interface du système | captures à la main, ou schéma |
| Bandeau du mode démo et barre de navigation autour d'un écran | assemblés par l'application, pas par l'écran | composition de capture qui réunit barre, bandeau et contenu (cadre « téléphone ») |
| Encarts éphémères (« Position diffusée … », décomptes) | états transitoires | capture de l'encart seul, avec l'état fixé |

### 3.4 Autres méthodes, si CST ne convient pas

| Méthode | Licence | Ajout | Coût CI | Risques |
|---|---|---|---|---|
| Tests Compose JVM + `captureToImage` (Skia) | Apache-2.0, déjà présent | aucune bibliothèque | faible (tourne aussi en local, sans SDK) | rendu de bureau, polices et densité différentes d'un téléphone ; mêmes limites pour la carte |
| Roborazzi + Robolectric | Apache-2.0 (les deux) | **nouvelles bibliothèques** | moyen à élevé (Robolectric télécharge un Android complet) | réglages Compose Multiplatform et KMP à éprouver ; pourrait rendre l'application entière avec sa navigation |
| Paparazzi (Cash App) | Apache-2.0 | **nouvelle bibliothèque** | moyen | même moteur que CST (layoutlib), sans gain ici, risque de conflit avec le plugin CST |

Recommandation : **CST** (aucune bibliothèque nouvelle, déjà maintenu par l'amont, rendu Android), avec captures à la main
pour la carte.

## 4. Liste des captures (première version)

Par écran, en mode démo, clair et sombre : bienvenue ; barre de navigation (4 états de Ma radio) ; liste des
conversations ; conversation Général (Info, CR, Urgent, `[POS]`, fait observé, états d'envoi) ; conversation CHARLIE-3
(échec et « Diagnostic ») ; filtres ; puces de la zone de saisie ; sélecteur de priorité ; Ma position (message rempli) ;
Fait observé (formulaire, trop long, veille) ; convertisseur (trois onglets, une erreur) ; encart « Partager ce point » ;
vue PC ; Équipes (adoption, diffusion PC, mon équipe, Diffuser mon nom) ; organigramme (lecture, édition, QR, adoption) ;
diagnostic (constats, réglage attendu identique et en écart, Tester la liaison) ; bandeau de diagnostic ; demande de tir
(formulaire par groupes de cases, aperçu, carte émetteur, fiche complète, fiche incomplète, dialogue GDH) ; mode démo
(réglage, bandeau, « Indisponible en mode démo ») ; Réglages (Mode poste de commandement, Thème). Carte : liste à la main
(§ 3.3). Environ 60 images par thème.

## 5. Découpage en commits (après décisions)

| # | Commit | Contenu |
|---|---|---|
| 1 | Ce plan | docs |
| 2 | Module de captures (ou ajout à `:docs-screenshots`), premier écran, workflow manuel | build, CI |
| 3 | Run du workflow, mesure du temps, réglages | CI |
| 4 à 8 | Captures par chapitre (messagerie, positions, équipes et organigramme, vue PC et diagnostic, demande de tir, démo et réglages) ; extractions de contenus sans ViewModel si D5 (a), chacune testée | feature, captures |
| 9 | Matière du guide : `docs/guide/` (chapitres des deux profils, fiches réflexes, glossaire, dépannage ; l'annexe « Configuration d'exercice » dans un fichier à part, remplaçable), Markdown | docs |
| 10 | Liste des captures à faire à la main (carte, notifications, QR) | docs |

Règles : petits commits ; spotless et detekt en local avant tout commit de code ; tests locaux des seuls modules modifiés,
10 minutes au plus ; pendant un run `fork-apk`, poussée sur `wip/guide-utilisateur`.

## 6. Ce que ce plan ne fait pas

Aucune modification de l'application pour le guide, sauf l'extraction éventuelle de contenus sans ViewModel (D5) ; aucune
donnée réelle (mode démo seulement) ; aucune mise en page finale.

## 7. Décisions

**Réponses du 13 novembre 2026** : D2 à D13 validées telles que proposées, avec deux précisions : **D9**, données de démo
au **13 novembre 2026, 14:35, fuseau Europe/Bucharest** (et non Europe/Paris, § 3.2 à corriger au moment du code) ;
**D12**, simple renvoi à `PREPARER-LES-RADIOS.md`. D2 (tableau des postes) et D3 (17 fiches) validés après relecture.

**Changement de structure du 13 novembre 2026** : guide **général**, indépendant du nombre de radios (réseau visé : plus de
50 radios), en deux profils **Administrateur** et **Utilisateur** ; chaque fiche réflexe indique son profil (§ 2, § 2.1).
Les postes CDU, PCR, OA, CDS TIR et Drone passent dans l'annexe **« Configuration d'exercice »** (§ 2.2), remplaçable sans
toucher au reste : **D1 n'est plus nécessaire pour le corps du guide** ; le développé des sigles et le rôle des postes ne
seront demandés que pour remplir cette annexe. Le guide est **en attente** (étape 1 faite) pendant la tâche suivante
(cartes raster GeoTIFF).

Nouvelles questions, à poser à la reprise :

| N° | Question | Proposition |
|---|---|---|
| D14 | Ajouter les fiches FR18 « Préparer une radio » et FR19 « Saisir le réglage LoRa attendu et repérer une radio en écart » (Administrateur) | oui |
| D15 | Limites pour un réseau de plus de 50 radios (§ 2.3), surtout l'organigramme par QR (770 caractères) : les écrire telles quelles dans le guide, et ouvrir une tâche à part pour l'organigramme d'un grand réseau | oui / à décider |
| D16 | Contenu de l'annexe « Configuration d'exercice » : développé, rôle, profil, vue, fonctions et fiches de chaque poste (ancienne D1) | à fournir au moment de rédiger l'annexe |

Décisions de l'étape 1 (D1 remplacée par l'annexe, D16) :

| N° | Question | Proposition |
|---|---|---|
| D1 | Définition des postes **CDU, PCR, OA, CDS TIR, Drone** : rôle, vue (Terrain ou PC), fonctions utilisées. Le dépôt ne les définit pas (seuls OA, CDS TIR et Drone apparaissent, dans la demande de tir) | **sans objet pour le corps du guide** (13/11) : passe dans l'annexe « Configuration d'exercice » (D16) |
| D2 | Répartition des fonctions et des fiches par poste (§ 2.2) | **validée**, placée dans l'annexe |
| D3 | Structure du guide (§ 2) et liste des 17 fiches réflexes (§ 2.1) | **validée** ; structure refaite en deux profils le 13/11 |
| D4 | Captures : nouveau module `:guide-screenshots` ou ajout au module amont `:docs-screenshots` | nouveau module, pour ne pas mélanger avec l'amont |
| D5 | Écrans liés à un ViewModel : (a) extraire un contenu sans ViewModel, ou (b) vrai ViewModel sur les données de démo ; et rendre publiques les fonctions `internal` nécessaires ou capturer depuis le module qui les contient | (a), au cas par cas |
| D6 | Carte, notifications, sélecteur de fichiers, caméra : captures à la main par vous (liste fournie), ou workflow sur émulateur plus tard | à la main |
| D7 | Thème sombre aussi | oui (coût négligeable) |
| D8 | Format : 411 × 891 dp à 420 dpi (Pixel 6, PNG 1080 × 2340), ou autre | Pixel 6 |
| D9 | Heure et fuseau fixes des données de démo dans les captures (ex. 13 novembre 2026, 14:35, Europe/Paris) | oui |
| D10 | Où écrire la matière du guide : `docs/guide/` (un fichier par chapitre, fiches à part) | oui |
| D11 | Langue : français seulement, captures en français | oui |
| D12 | Écrans d'origine de Meshtastic (réglages de la radio, firmware) : renvoi à `PREPARER-LES-RADIOS.md` sans les détailler, ou les décrire aussi | renvoi |
| D13 | Workflow de captures : nom `fork-guide-captures`, `workflow_dispatch` seulement, artefact gardé 30 jours | oui |
