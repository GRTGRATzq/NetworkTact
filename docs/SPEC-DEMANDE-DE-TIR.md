# Demande de tir (v1)

Validée pour une v1 par le porteur du projet le 11/11/2026. Source : fiche « Demande de tir » du
68e RAA, EOC, février 2024. La maquette validée (4 écrans) fait foi pour la disposition :
`docs/maquette-demande-de-tir.pdf` (export du 11/11/2026, ligne L R U avec Fuseau, Bande, Carré).
Décisions complémentaires du 11/11/2026 : § 10.

## 1. Principe
- Formulaire qui reproduit la fiche papier, case par case, dans le même ordre, avec ses libellés,
  abréviations et codes. Rien d'inventé.
- Ouvert depuis la zone de saisie d'une conversation, à côté de « Ma position », « Fait observé » et
  « Convertisseur » : bouton « Demande de tir », contour rouge, badge DT.
- Un seul geste « Envoyer la demande » : la demande complète part en message direct au destinataire,
  avec accusé de réception, découpée automatiquement si elle dépasse 200 octets (§ 4).
- Texte ordinaire uniquement. Toute station peut envoyer une demande (poste Drone compris).

## 2. En-tête
- Origine : écrite à la main par l'opérateur (pas de préremplissage).
- Destinataire : choisi parmi les nœuds connus ; envoi en message direct à ce nœud.
- GDH demande : prérempli à l'heure du téléphone, modifiable ; format choisi par l'opérateur :
  loc (heure locale), Z (zoulou) ou fra (heure française), choix mémorisé. Forme : 112250 loc NOV 26.
  Zoulou à l'usage militaire, Z collé : 112150Z NOV 26 ; loc et fra gardent l'espace : 112150 loc NOV 26,
  112150 fra NOV 26. Mois OTAN : JAN FEB MAR APR MAY JUN JUL AUG SEP OCT NOV DEC.
- GDH attribution, GDH fin : vides à l'envoi ; renvoyés par le destinataire (§ 6).

## 3. Les 11 cases
Aucune case obligatoire en v1. Case vide = « <n> non rempli ». Les gisements sont en millièmes
(m barré sur la fiche) ; tout le reste (distances, site S, X, Y, Z, front, profondeur, rayon) en mètres.
1. N° de tir : lettres et chiffres, de 1 à 10 caractères.
2. Type de tir : sélection multiple. Lettres A B C D E F G H (cohérentes avec les lignes de la case 7) ;
   Emblée · Mettre en place [ ] · Mise en place décentrée [ ] · Préparer · Renouveler [ ] · Accrochage.
3. Type d'effet : Neutralisation · Destruction · Arrêt · Semonce · Aveuglement · Éclairement [ ]
   (1 : Visible ou 2 : IR) · Panaché [ ] et [ ] (1. Explo ou 2. Fumigène, ou 3 : ACED ou 4 : ECL et
   A : VISIB ou B : IR ou 5 : Semonce).
4. Observation : Nb de cps [ ] / [ ] · Durée [ ] / [ ] min · Intervalle entre les coups [ ] / [ ] s.
5. Type de charge : Lanceurs [ ] (1 : Mo 81 mm ou 2 : Mo 120 ou 3 : 155 mm ou 4 : LRU) ·
   Trajectoire [ ] / [ ] (1 : Plongeante ou 2 : Verticale ou 3 : Nominale) · Mode [ ] et [ ]
   (1 : Percutant instantané ou 2 : Percutant court retard … ms ou 3 : Fusant … m ou 4 : Retard … m).
6. Objectifs : Chars · Blindés légers · Pick-up (en mouvement ou à l'arrêt) · Personnel (à découvert
   ou abrité) · Artillerie (observateur ou pièces) · Bâtiment ou [ ].
7. Localisation de l'impact :
   - POLAIRES A, B, C : G [ ] (millièmes) · D [ ] m · S [ ] m ;
   - RECTANG. D, E, F : X [ ] · Y [ ] · Z [ ] m ;
   - Rep [ ] [ ] (deux cases libres) : Plus à l'est - ouest [ ] m · Plus au nord - sud [ ] m ;
     Pour moi : Plus à Dte - Gche [ ] m · Plus près - loin [ ] m · Plus haut / Plus bas [ ] m ;
   - L R U, lignes G et H : Fuseau · Bande · Carré · X · Y · Z m, écrites au format MGRS
     35T LL 33000 - 90300 : 35 = fuseau UTM, T = bande de latitude, LL = identification du carré de
     100 km (champ imposé par le MGRS), 33000 = X (abscisse, est-ouest), 90300 = Y (ordonnée,
     nord-sud), X et Y toujours à 5 chiffres (précision au mètre) ; Z (altitude ou élévation)
     facultatif, en mètres. Chaque coordonnée est vérifiée par le convertisseur existant ;
   - TLE (précision de localisation) : 1, 2, 3 ou 4 ;
   - facultatif : remplir une ligne L R U par « Coller une coordonnée » (texte copié par l'appui long
     sur la carte ou par le convertisseur, MGRS) ; tout reste saisissable à la main.
8. Forme de l'impact : Convergent · Linéaire ou Surfacique (Point central [ ], LG [ ] ou LD [ ],
   Gisement [ ] millièmes, Front [ ] m, Profondeur [ ] m, Ouvert ou fermé) · Circulaire (Centre [ ], Rayon [ ] m).
9. Mise en place : Grille ou Déplacement du point moyen · Gisement d'observ. [ ] (millièmes) ·
   Observateur X [ ] Y [ ] Z [ ] (saisis par l'opérateur, aucun préremplissage).
10. Ami(s) le(s) plus proche(s) : Distance [ ] m et Gisement (objectif/amis) [ ] (millièmes) ou
    N O E S · Niveau de protection (1 : à découvert ou 2 : abrité ou 3 : sous blindage).
11. À mon commandement : À mon commandement · Aussitôt prêt · Aussitôt prêt limité NLT [..H..] ·
    Synchro [ ] · Heure sur objectif [..H..] · HSO limité début [..H..] fin [..H..] · Heure de tir [..H..] ·
    Au commandement de [ ].

## 4. Format du message et découpage
- Chaque partie commence par [DT <n° de tir> <k>/<N>]. La première porte aussi l'en-tête :
  Origine OA · Destinataire CDS TIR · GDH demande 112250 loc NOV 26
- Puis une ligne par case : « <n> <contenu> », éléments séparés par « · », libellés et codes de la
  fiche écrits comme sur le papier (5 Lanceurs 2 · Trajectoire 1 · Mode 1). Les 11 cases sont toujours
  présentes.
- Au plus 200 octets UTF-8 par partie. Coupure entre deux cases, jamais au milieu, sauf si une case
  seule dépasse : elle est alors coupée à un « · » et continue dans la partie suivante.
- Exemple validé (3 parties, 488 octets) :
  [DT AB1234 1/3]
  Origine OA · Destinataire CDS TIR · GDH demande 112250 loc NOV 26
  1 AB1234
  2 G Emblée · Préparer
  3 Neutralisation · Panaché 1 et 2
  4 non rempli
  ---
  [DT AB1234 2/3]
  5 Lanceurs 2 · Trajectoire 1 · Mode 1
  6 Personnel à découvert
  7 G 35T LL 33000 - 90300 · Z 610 m · TLE 2
  8 Circulaire · Rayon 100 m
  ---
  [DT AB1234 3/3]
  9 Grille · Gisement d'observ. 4800 · Observateur X 34550 Y 88800 Z 640
  10 Distance 800 m · Gisement 1600 · Niveau de protection 1
  11 Aussitôt prêt

## 5. Envoi
- Aperçu de toutes les parties avant envoi, avec leur taille ; « Modifier » ou « Envoyer la demande ».
- Un seul geste : les parties partent à la suite, en message direct, avec demande d'accusé.
- Côté émetteur, une seule carte « DT <n°> · N messages » avec l'état de chaque partie ; une partie
  en échec peut être renvoyée seule.

## 6. Réception et réponse
- Les parties d'un même expéditeur et d'un même n° de tir sont regroupées en une fiche reconstituée
  (badge DT, présentation propre, jamais le rouge de [URG]) : en-tête puis cases 1 à 11, codes
  affichés en clair (Lanceurs 2 : Mo 120), « non rempli » en gris.
- Tant qu'une partie manque : « k / N reçus », parties manquantes nommées, cases reçues lisibles.
- « Voir sur la carte » sur la localisation (case 7) ; « Voir les messages bruts ».
- Le destinataire renvoie le GDH attribution puis le GDH fin, chacun en un court message direct :
  [DT AB1234] GDH attribution 112252 loc NOV 26 ; ils s'affichent sur la demande de l'émetteur.
- Notification : une seule par demande, mise à jour à l'arrivée des parties (pas une par partie).

## 7. Brouillon et « Renouveler »
- La demande en cours de saisie est gardée sur le téléphone.
- « Renouveler » reprend la dernière demande envoyée dans le formulaire, modifiable.

## 8. Mode démo
- Formulaire disponible ; envoi simulé (rien ne part) ; une demande fictive reçue et une réponse GDH
  simulée pour montrer la fiche reconstituée.

## 9. Reporté
- Cases verrouillées ou obligatoires (à fixer par le porteur du projet après validation).
- Tout ajout à la fiche : uniquement sur décision du porteur du projet.
- Bouton « Depuis la carte » dans la case 7 (aller-retour vers la carte pour remplir une ligne L R U) :
  après la v1, qui se contente de « Coller une coordonnée ».

## 10. Décisions du 11/11/2026 (plan, § 12)
- Formulaire en écran plein ; maquette : `docs/maquette-demande-de-tir.pdf`.
- N° de tir : 1 à 10 caractères, en majuscules ; Origine telle que saisie.
- Destinataire présélectionné quand le formulaire est ouvert depuis une conversation directe ; après
  l'envoi, ouverture de la conversation directe avec lui. « Destinataire » écrit dans le message :
  texte libre prérempli avec le nom du nœud, modifiable.
- Case 2 : lettres d'abord, puis les types (2 A G Emblée · Mettre en place 2). Case 3 : Panaché 4A et 2.
- Case coupée entre deux parties : la suite recommence par le numéro de la case, sans mot ajouté.
- 9 parties au plus ; champs libres de 24 octets au plus.
- Même n° de tir après plus de 2 h, ou rang déjà reçu avec un autre texte : nouvelle demande.
- Cases d'une partie manquante affichées « non reçu », distinct de « non rempli ».
- Fiche placée au dernier message reçu de la demande, les autres messages masqués dans la liste.
- Case 7 : une ligne de message par ligne de la fiche (7 A G 1600 · D 2500 m · S 20 m) ; TLE en fin de
  la dernière ligne ; lignes G et H remplies : deux boutons « Voir sur la carte ».
- GDH illisible : signalé, sans bloquer l'envoi. Un seul brouillon pour le téléphone. « Renouveler » :
  GDH remis à l'heure actuelle, même n° de tir.
- Radio non connectée : formulaire ouvert, envoi grisé.
- Libellés de la fiche en français sur tous les téléphones ; interface traduite (EN/FR) ; badge DT.
- Demandes de tir rangées sous le filtre « Info ».
- Écriture des cases 4, 5, 6, 8, 10 et 11 : validée case par case par le porteur du projet avant tout
  code (plan, § 3.2).
