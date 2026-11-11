# Demande de tir (v1)

Validée pour une v1 par le porteur du projet le 11/11/2026. Source : fiche « Demande de tir » du
68e RAA, EOC, février 2024. La maquette validée (4 écrans) fait foi pour la disposition.

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
- GDH attribution, GDH fin : vides à l'envoi ; renvoyés par le destinataire (§ 6).

## 3. Les 11 cases
Aucune case obligatoire en v1. Case vide = « <n> non rempli ». Les gisements sont en millièmes
(m barré sur la fiche) ; tout le reste (distances, site S, X, Y, Z, front, profondeur, rayon) en mètres.
1. N° de tir : lettres et chiffres.
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
   - L R U, lignes G et H : Fuseau · Bande · X · Y · Z m, écrites au format 35T LL 33000 - 90300 ;
   - TLE (précision de localisation) : 1, 2, 3 ou 4 ;
   - facultatif : remplir une ligne L R U depuis la carte (appui long) ou le convertisseur (MGRS) ;
     tout reste saisissable à la main.
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

## 7. Brouillon et « Renouveler »
- La demande en cours de saisie est gardée sur le téléphone.
- « Renouveler » reprend la dernière demande envoyée dans le formulaire, modifiable.

## 8. Mode démo
- Formulaire disponible ; envoi simulé (rien ne part) ; une demande fictive reçue et une réponse GDH
  simulée pour montrer la fiche reconstituée.

## 9. Reporté
- Cases verrouillées ou obligatoires (à fixer par le porteur du projet après validation).
- Tout ajout à la fiche : uniquement sur décision du porteur du projet.
