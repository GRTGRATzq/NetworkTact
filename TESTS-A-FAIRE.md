# NetworkTact : essais à faire sur téléphone

Dernière mise à jour : 14 novembre 2026 (essais des 13 et 14 novembre cochés, §10 à §18 ; voir
`docs/RESULTATS-ESSAIS-2026-11-13.md`). Texte d'origine du 9 novembre : APK : artefact `apk-fdroid-debug` du dernier run vert de
`fork-apk` sur la branche `feat/apk-release` (qui contient tout). Rien de ce qui suit n'a encore
été essayé sur un téléphone, sauf le défaut corrigé au §10 ; la CI ne prouve que la compilation et
les tests unitaires.

**Matériel** : 2 téléphones (A en mode PC, B en mode Terrain), 2 radios allumées avec le même
réglage LoRa, un troisième nœud si possible. Noms longs : `PC-0` (radio de A) et `ALPHA-1 [Alpha]`
(radio de B). Liste d'équipes à adopter sur les deux : `Alpha;Bravo`.

Pour chaque essai, noter : OK / KO / remarque. Ne jamais tester la diffusion ou le traceroute en
boucle (temps d'antenne partagé).

## 0. Installation
- [ ] Installer l'APK (« NetworkTact Debug »). Nom et icône NetworkTact, pas Meshtastic.
- [ ] Premier lancement : écran de bienvenue (§20) avec les deux insignes, leurs légendes et la
      phrase « projet porté conjointement par deux batteries ». Tester thème clair et sombre,
      police agrandie, petit écran : la colonne défile, « Commencer » reste visible.
- [ ] Réglages → Information → « Afficher l'introduction » : l'écran revient, rien n'est effacé.

## 1. Mode démo (§9), à faire en premier : aucune radio nécessaire
- [ ] Réglages → Mode démo : bandeau « MODE DÉMO » permanent, bouton Quitter.
- [ ] Conversations fictives : envoi simulé qui passe en file → en route → relayé → accusé
      (message direct à BRAVO-2) ou échec (CHARLIE-3).
- [ ] Écrans non permis : « Indisponible en mode démo ».
- [ ] Quitter : les vraies données reviennent, rien de la démo n'est resté.

## 2. Vue PC et messagerie (§3, §4, §8)
- [ ] Mode PC : l'application s'ouvre sur la vue PC ; fraîcheur du contact et de la position
      (récente, ancienne > 10 min, sans heure).
- **Remarque (connu, ne pas relever comme défaut)** : une radio **immobile** s'affiche
  « POSITION ANCIENNE » sur la vue PC, alors qu'elle fonctionne. Le firmware n'envoie la position
  d'une radio immobile que toutes les 60 min par défaut, plus que le seuil de 10 min de la vue PC.
  Ces fausses alertes sont attendues, et seront corrigées par C2 (seuils réglables) :
  `docs/AUDIT-PASSAGE-A-L-ECHELLE.md`, § 4.4 et § 6 bis.
- [ ] Envoyer `[URG]` et `[CR]` : barre de priorité rouge / ambre, filtres par priorité.
- [ ] États d'envoi réels : un message de canal n'est jamais affiché « accusé ».
- [ ] Message direct accusé : « Accusé par X », « vérifié » si la preuve est valide.

## 3. Équipes (§6)
- [ ] A diffuse `[EQUIPES] Alpha;Bravo` ; B propose l'adoption avec émetteur et heure.
- [ ] B choisit son équipe : le nom long devient `ALPHA-1 [Alpha]`, affiché « ALPHA-1 · Équipe Alpha ».
- [ ] Nom long trop long (> 39 octets) : prévenu avant troncature. Radio non connectée : rien envoyé.

## 4. Coordonnées et faits observés (§7, §12)
- [ ] Convertisseur : un point connu en UTM, MGRS et DMS, saisie DMS sans symboles, erreur précise.
- [ ] « Ma position » : message `[POS] …` rempli sans être envoyé ; position ancienne signalée.
- [ ] « Fait observé » avec heure `14h05` ; heure future = « (veille) » ; blocage au-delà de 200 octets.
- [ ] Après envoi : l'encart « Partager sur la carte » ; Partager envoie un point de repère visible
      dans l'application Meshtastic officielle ; « Ne pas partager » n'émet rien.
- [ ] Message reçu avec coordonnée : « Voir sur la carte » centre et marque le point avec son bandeau.

## 5. Carte hors ligne (§13, §14, §15), variante fdroid
- [ ] Produire un `.pmtiles` (voir `CARTE-HORS-LIGNE.md`), l'importer : refus d'un mauvais fichier,
      progression, annulation sans fichier résiduel. Mode avion : la carte s'affiche.
- [ ] Suppression d'une carte après confirmation ; retour au fond intégré.
- [ ] Mentions d'attribution OpenStreetMap / Protomaps visibles.
- [ ] Appui long : MGRS / UTM / DMS ; Copier, Insérer dans un message (rien n'est envoyé),
      Ouvrir dans le convertisseur, Créer un point de repère.
- [ ] « Recaler » : GPS radio contre GPS téléphone, âges, précision, écart en mètres. Centrer sur
      Radio / Téléphone mémorisé.
- [ ] « Diffuser ma position maintenant » : confirmation obligatoire, un seul paquet, blocage 30 s ;
      refusé sans heure de relevé, hors connexion ou en démo. Vérifier côté récepteur l'heure du relevé.

## 6. Organigramme (§17), procédure complète
1. **A** : Réseau → icône organigramme. Sections PC, Alpha, Bravo vides.
2. **A** : ajouter PC-0 (Chef PC) au PC ; ALPHA-1 (Chef d'équipe) et CHARLIE-3 à Alpha. ALPHA-1 montre
   sa radio et ses âges ; CHARLIE-3 « Aucune radio associée » ; PC-0 « Ma radio ».
3. **A** : ajouter « alpha-1 » à Bravo : refusé. Indicatif de 40 caractères : refusé avec les octets.
4. **A** : monter / descendre CHARLIE-3 ; retirer un membre demande confirmation.
5. **A** : « Afficher le QR » (résumé, pleine luminosité, rien émis par radio).
6. **B** : lecture seule (« Seul le PC peut modifier »). Scanner le QR de A à la caméra :
   confirmation avec auteur, heure, équipes, membres, avertissement. Ignorer = rien ; Adopter = adopté.
7. **B** : rescanner = « identique, rien à adopter » ; QR de canal = « pas un organigramme valide ».
8. **B** : toucher ALPHA-1 ouvre sa fiche, Message sa conversation ; « Revenir au précédent » échange
   les deux organigrammes, réversible.
9. Renommer une autre radio `ALPHA-1 [Alpha]` : elle prend la place ; deux radios visibles =
   « 2 radios portent cet indicatif ».
10. ALPHA-1 déclaré équipe Bravo : « La radio déclare l'équipe Bravo ».
11. Limite : ajouter des membres longs jusqu'au refus « trop grand pour un QR lisible (N, 770 au
    plus) ». Un QR de 9 membres longs doit être lu par B.
12. Démo sur A : organigramme fictif (DELTA-4 « Aucune radio associée »), modifs perdues à la sortie.

## 7. Autodiagnostic (§19)
- [ ] Éteindre la radio de A : bandeau « Radio non connectée » (Messages, Réseau, vue PC) ;
      croix = fermé et ne revient pas tant que la situation ne change pas.
- [ ] Radio connectée, aucun autre nœud pendant 15 min : « Aucun nœud entendu depuis N min » ;
      seuils suivants 30 min, 1 h.
- [ ] Envoi en échec : bouton « Diagnostic » sous la carte.
- [ ] Écran de diagnostic : liaison, dernier paquet, nœuds entendus (5, 15, 60 min), dernier nœud,
      LoRa attendu contre réel, batterie. Aucun « réseau OK » ni « isolé ».
- [ ] Réglage LoRa attendu (mode PC seulement), volontairement différent du réglage réel : constat signalé.
- [ ] « Tester la liaison » : confirmation, un seul traceroute, attente 30 s, « Demande remise à la
      radio à HH:MM » ; refusé radio non connectée.

## 8. Détachement et thème (§10, §11, §16)
- [ ] À propos : lien vers `GRTGRATzq/NetworkTact`, ligne « Basé sur Meshtastic-Android », copyright
      Meshtastic LLC et licences conservés. Plus de liens d'achat ni de documentation Meshtastic.
- [ ] Thème clair, sombre, dynamique : lisibilité de la barre de navigation, des cartes de message,
      de la vue PC. Rouge seulement pour « urgent ».
- [ ] Icône d'état de ma radio : connectée, connexion, veille, déconnectée, distinguables sans couleur.
- [ ] Hors ligne : aucune erreur visible due aux requêtes de fond vers api.meshtastic.org.

## 9. Compatibilité
- [ ] Une radio sous l'application Meshtastic officielle lit les messages `[URG]`, `[EQUIPES]`,
      `[POS]`, `[CR]` comme du texte ordinaire.

> **Essais des 13 et 14 novembre 2026** (APK `apk-fdroid-release`, run 106 de `fork-apk`, commit `120d0f3`) : §10 à §18
> parcourus, cases cochées ci-dessous. Les essais KO (D9 à D13), sans objet ou non faits sont annotés « → … » ; détail et
> priorités de correction dans [`docs/RESULTATS-ESSAIS-2026-11-13.md`](docs/RESULTATS-ESSAIS-2026-11-13.md), journal
> minute par minute dans [`docs/JOURNAL-ESSAIS-2026-11-13.md`](docs/JOURNAL-ESSAIS-2026-11-13.md). D9 (équipe dont seule
> la casse change, Obs → OBS : nom non renommé) est apparu pendant la préparation, hors check-list.

## 10. Diffusion du nom (§21), défaut constaté le 8 novembre

Deux radios préparées (`PREPARER-LES-RADIOS.md`) : la radio **CDS TIR** et la radio **OA**, qui se
voient déjà. Sur le téléphone de OA, ne jamais utiliser « Infos utilisateur » pendant cet essai.

- [x] Sur OA, noter le nom affiché pour CDS TIR (liste des nœuds et fiche du nœud).
- [x] Téléphone de CDS TIR : Réglages → Équipes, choisir une équipe, confirmer. Message « Nom … envoyé
      à ma radio », la radio redémarre, puis « Nom … diffusé à tous les postes » dès sa reconnexion.
      → « diffusé à tous les postes » non vu après le choix d'équipe (R2).
- [x] Sur OA, **sans** « Infos utilisateur » : le nom long `CDS TIR [Équipe]` apparaît, en moins d'une
      minute après la diffusion. Noter le délai.
      → ≈ 30 s, D2 corrigé.
- [x] Téléphone de CDS TIR : Réglages de l'appareil → Utilisateur, changer le nom court, enregistrer.
      Même vérification sur OA pour le nom court.
      → ≈ 15 s.
- [x] « Diffuser mon nom » (Utilisateur ou Équipes) : confirmation obligatoire ; après envoi, bouton
      bloqué 30 s avec décompte, sur les deux écrans. Radio éteinte : « Aucune radio connectée ».
- [x] Changer seulement « Non joignable par message » dans Utilisateur : rien n'est diffusé.
- [x] Mode démo, Réglages → Équipes : choisir une équipe puis « Diffuser mon nom » : « diffusion
      simulée, rien n'est envoyé » ; OA ne reçoit rien.
- [ ] Si OA garde l'ancien nom : noter la version du firmware des deux radios et si un message
      « Ma radio n'a pas confirmé le nom » est apparu.
      → sans objet le 13/11 : nom reçu à chaque fois.

## 11. Un seul mode démo (§22), défaut D1 du 8 novembre

- [x] Ma radio, liste des appareils, en Bluetooth, réseau et USB : plus aucune section « Mode démo »,
      ni « Mode Démo » ni « Mode démo (Rejouer) ». Aucun nœud Demo Handset ou Riverside Base.
      → D1 corrigé.
- [ ] Après mise à jour par-dessus une version où « Mode Démo » avait été choisi : la vraie radio
      est de nouveau proposée et s'y reconnecte, sans repasser par la fausse.
      → sans objet le 13/11 : debug désinstallée avant.
- [x] Réglages → Mode démo (celui de NetworkTact) fonctionne toujours : bandeau et bouton Quitter.

## 12. Formulaire « Fait observé » (§23), défauts D3, D4 et D5 du 8 novembre

À faire sur le téléphone, clavier ouvert (c'est là que les défauts sont apparus).

- [x] Description très longue (coller 300 caractères) avec un lieu valide : le champ devient rouge et
      « Trop long : N/200 octets » apparaît **juste sous le champ**, sans défiler ; « Insérer dans le
      message » est grisé ; rien n'est coupé. Raccourcir : tout redevient normal à 200 octets.
      → D3 corrigé.
- [x] Même description avant d'avoir saisi le lieu : la taille s'affiche déjà.
- [x] Heure pas encore atteinte (saisir `23h50` ou `2350` vers 18 h) : « (veille) » sous le champ
      d'heure et `23:50 (veille)` dans le rapport. Heure déjà passée : pas de « (veille) ».
      → D4 corrigé.
- [ ] Clavier de la description : la touche d'action est « Terminé » et ferme le clavier sans
      toucher un autre champ. Idem pour le champ d'heure. Pas de retour à la ligne ajouté.
      → **KO le 13/11 (D10)** : touche ✓ / OK sans effet dans la description et l'heure, voir `docs/RESULTATS-ESSAIS-2026-11-13.md`.

## 13. Diagnostic : écart LoRa (§25), défaut D8 du 8 novembre

À faire en mode PC, sur une radio réglée en LONG_FAST.

- [x] Réseau → Diagnostic → carte « Réglage LoRa attendu » → Modifier : choisir le préréglage MEDIUM_FAST
      (même région que la radio), Enregistrer. Une pastille « Ma radio est en … LONG_FAST : écart avec ce
      réglage » apparaît **dans cette carte**, sans défiler ; la carte « Constats » en haut montre aussi
      l'écart et la suggestion de vérifier région et préréglage.
      → D8 corrigé.
- [x] Modifier de nouveau pour LONG_FAST : « Identique au réglage de ma radio », plus de pastille ni de
      suggestion. « Effacer » : plus rien dans la carte.
- [x] Même essai avec une autre région : l'écart apparaît aussi.
- [ ] Si la pastille manque encore : noter ce que dit la ligne « Réglages LoRa » de la carte « Constats »
      (« pas encore reçus de la radio » indiquerait que la configuration LoRa n'arrive pas).
      → sans objet le 13/11 : pastille présente.

## 14. Organigramme : saisie (§26), défaut D7 et amélioration A1 du 8 novembre

En mode PC, Réseau → organigramme → Ajouter un membre, clavier ouvert.

- [x] Taper un indicatif de 40 caractères : le champ devient rouge, « Indicatif trop long : 40 octets, 39 au plus »
      s'affiche **sous le champ**, « OK » est grisé. En retirer un : 39 octets, tout redevient normal.
      → D7 corrigé ; bouton nommé « D'accord ».
- [x] Même chose avec des accents (20 fois « é » = 40 octets) et avec une fonction de plus de 24 octets.
- [x] « Radios connues » : les radios vues, sans suffixe d'équipe, sans celles déjà dans l'organigramme ; en
      toucher une remplit le champ. Taper « alp » ne garde que les indicatifs qui contiennent « alp ».
      → A1 fait.
- [x] Saisie libre : taper un indicatif absent de la liste (PCR, radio éteinte) et valider : accepté.
- [x] Ajouter un membre déjà présent (même casse différente) : alerte en haut de la boîte.
- [x] Modifier un membre existant : pas de liste de radios, taille contrôlée de la même façon.
- [x] Mode démo : les propositions sont les radios fictives ; rien n'est gardé à la sortie.

## 15. Crédit d'une carte importée (§27), défaut D6 du 8 novembre

Variante fdroid. Le crédit imposé ne dépend que des métadonnées du fichier.

- [x] Carte Protomaps (le fichier de Cincu, `name` = « Protomaps Basemap »), choisie comme fond : le bouton ⓘ affiche
      « © contributeurs OpenStreetMap · Protomaps » (téléphone en français) ou « © OpenStreetMap contributors ·
      Protomaps » (anglais), même si le fichier ne déclare que « © OpenStreetMap ». Le logo MapLibre reste à gauche.
      → D6 corrigé.
- [ ] Un autre `.pmtiles` vectoriel dont les métadonnées déclarent une attribution (autre `name`, sans Protomaps dans la
      description) : le bouton ⓘ affiche **celle du fichier**, pas celle de l'application.
      → sans objet le 13/11 : pas d'autre fichier.
- [ ] Un autre `.pmtiles` qui ne déclare aucune attribution : **pas de bouton ⓘ** (rien n'est affiché).
      → sans objet le 13/11 : pas d'autre fichier.
- [ ] Un fond en ligne (OpenFreeMap, etc.) garde son propre crédit ; un fichier `.mbtiles` importé garde le crédit de
      ses métadonnées, ou celui d'OpenStreetMap à défaut.
      → 13/11 : fond en ligne OK (« OpenStreetMap ») ; `.mbtiles` non essayé.

## 16. APK de release (§28)

APK : artefact `apk-fdroid-release` du dernier run vert de `fork-apk`. Il remplace l'APK de debug pour l'usage courant ;
R8 n'a été vérifié que par la compilation, jamais à l'exécution.

- [x] Installation : l'APK est signé (sinon Android refuse de l'installer). Désinstaller d'abord la version de debug
      (`com.geeksville.mesh.fdroid.debug`) : la release (`io.github.grtgratzq.networktact`) repart sans données,
      refaire la liste d'équipes et l'organigramme (§28). Premier lancement sans plantage, écran de bienvenue.
- [x] Parcours de fumée, en cherchant ce que R8 casserait : Réglages, Mode démo, vue PC, Conversations ; carte (fond en
      ligne, import d'un `.pmtiles`, crédit ⓘ, appui long) ; QR de l'organigramme (affichage et lecture à la caméra) ;
      Bluetooth (recherche et connexion d'une radio) ; Réglages de la radio.
      → appui long carte, QR affiché, scanné et adopté.
- [x] Fluidité : le démarrage à froid est-il plus rapide qu'avec l'APK de debug ? À noter (ressenti, ou
      `adb shell am start -W`).
      → démarrage plus rapide qu'en debug.
- [x] Un plantage au lancement ou dans une fonction qui marchait en debug : noter l'écran et l'heure, récupérer le
      journal (`adb logcat`), c'est probablement une règle R8 manquante.
      → aucun plantage.

## 17. Téléchargement d'une carte (tâche 7)

Branche `feat/carte-telechargement`, variante fdroid (`docs/PLAN-CARTE-TELECHARGEMENT.md`, procédure dans `CARTE-HORS-LIGNE.md`).
**Rien de l'écran n'a encore tourné sur un téléphone** : il compile, sa logique et ses boutons sont testés (JVM), le fichier
produit est identique à `pmtiles extract` (CI) ; tout ce qui suit est à voir à l'œil et au doigt.

- [ ] **Possible dès maintenant** : télécharger l'artefact `cartes-cincu` du dernier run vert de `fork-carte-interop`, copier
      `networktact-cincu.pmtiles` (écrit par l'extracteur de NetworkTact, 14,65 Mo) sur le téléphone, l'importer par « Importer
      un fichier .pmtiles » : il est accepté, s'affiche comme le fichier du Mac (mêmes tuiles), mode avion compris, avec la
      mention « © contributeurs OpenStreetMap · Protomaps ». Cela vérifie sur téléphone le fichier qu'écrira le téléchargement.
      → non fait le 13/11 (facultatif, couvert par l'export).

- [x] Rectangle de Cincu (zone visible puis coins tirés au doigt, environ 60 × 60 km), zoom 15 : la taille annoncée avant, la taille
      réelle après, la durée ; la carte s'affiche, mode avion compris ; mention ⓘ « © contributeurs OpenStreetMap · Protomaps ».
      → 18,3 Mo annoncés, 18,2 Mo réels, ≈ 15 s.
- [x] Le cadre : poignées faciles à saisir, dimensions en km justes, zone téléchargée = zone du cadre.
- [x] Au-delà de 50 Mo : confirmation, la même sur Wi-Fi et sur données mobiles ; en dessous : pas de confirmation.
      → confirmation vue au-delà de 50 Mo ; Wi-Fi et données mobiles non comparés.
- [x] Écran de la source : le texte « Le serveur source voit la zone demandée et l'adresse du téléphone » est lisible en entier ;
      une adresse `http://` est refusée ; une adresse `https://` valide est gardée.
- [x] Annulation en cours : aucun fichier résiduel, espace libre inchangé. Coupure (mode avion) puis reprise : fichier complet.
      Fermeture de l'application pendant le téléchargement, réouverture : « Reprendre », dans le panneau et dans « Cartes sur
      le téléphone » (ligne « Téléchargement interrompu », pourcentage fait) ; « Reprendre » depuis la liste ouvre le cadre et
      la progression.
- [x] « Cartes sur le téléphone » : sous une carte téléchargée, la ligne « Édition du AAAA-MM-JJ, build.protomaps.com · L × H km »
      (dimensions du cadre) ; une carte importée n'a que la ligne taille et zooms.
- [ ] Mode démo : bouton désactivé, rien téléchargé. Espace presque plein : refus avec les tailles.
      → 13/11 : démo OK ; espace presque plein non fait.
- [x] Un ancien fichier importé du Mac (`cincu-30km.pmtiles`) fonctionne toujours à côté.
- [x] **Comparaison sur Mac** : récupérer le fichier du téléphone (APK de debug : `adb exec-out run-as
      com.geeksville.mesh.fdroid.debug cat files/pmtiles/<identifiant>.pmtiles > telephone.pmtiles` ; avec l'APK de release, qui
      n'est pas débogable, passer par « Exporter une carte », essai ci-dessous). Puis `pmtiles verify telephone.pmtiles` et
      `pmtiles show telephone.pmtiles`, à comparer à `pmtiles show cincu-30km.pmtiles` (produit le 8 novembre) : type de
      tuile, zooms, nombres de tuiles adressées, d'entrées et de contenus, taille des tuiles. Les bornes et nombres diffèrent
      si les zones diffèrent. **Pour une comparaison à zone égale**, refaire sur le Mac, avec **l'édition affichée par le
      téléphone**, `pmtiles extract <source>/<édition>.pmtiles essai.pmtiles --bbox=<ouest,sud,est,nord du cadre>
      --maxzoom=15` et comparer `pmtiles show essai.pmtiles` : mêmes nombres de tuiles et de contenus attendus.
      → verify sans erreur, même type et zooms ; comparaison à zone égale non faite.
- [x] **Exporter une carte** (D12) : « Cartes sur le téléphone » → Exporter sur la carte téléchargée : le sélecteur de fichiers
      Android s'ouvre avec le nom `<nom>.pmtiles` ; choisir `Download` ; le fichier apparaît là, de même taille que dans la
      liste ; rien d'autre n'est demandé (aucune permission, aucun partage vers une application). Annuler le sélecteur : rien
      n'est écrit. Le fichier exporté, copié sur le Mac, sert à la comparaison ci-dessus (`pmtiles verify`, `pmtiles show`) et
      se réimporte dans NetworkTact (« Importer un fichier .pmtiles »).
      → A4 demandée : renommer une carte téléchargée.
- [x] Aucune édition joignable (mode avion juste avant « Calculer la taille ») : « Impossible de joindre la source … » ;
      source réglée sur une adresse sans édition : « Aucune édition de carte n'a été trouvée … pour les 8 derniers jours ».
- [ ] Écran des licences : `pmtiles-reader` apparaît avec la licence BSD-3-Clause.
      → **KO le 13/11 (D11)** : écran des licences introuvable, voir `docs/RESULTATS-ESSAIS-2026-11-13.md`.

## 18. Demande de tir (tâche 8, FORK.md §30)

Branche `feat/demande-de-tir` (`docs/SPEC-DEMANDE-DE-TIR.md`, `docs/PLAN-DEMANDE-DE-TIR.md`, maquette
`docs/maquette-demande-de-tir.pdf`). Deux radios, deux téléphones : **A** émetteur (l'OA), **B** destinataire (CDS TIR) ;
puis la radio du poste Drone. **Rien n'a encore tourné sur un téléphone** : formulaire, cartes et notifications sont testés
en JVM, à voir à l'œil et au doigt. Fiche papier à côté pour la relecture.

**Formulaire (A)**

- [ ] Puce « DT Demande de tir » (contour rouge, badge DT) sur sa ligne, sous « Ma position · Fait observé · Convertisseur ».
      → non relevé le 13/11.
- [x] Ouvert depuis la conversation directe avec B : destinataire présélectionné, « Destinataire » prérempli avec le nom
      de B (sans équipe), modifiable. Ouvert depuis un canal : destinataire à choisir.
      → destinataire prérempli « CDS TIR - a4cc » (R4).
- [ ] Chaque case conforme à la fiche (libellés, ordre, codes, unités : millièmes, m, min, s, ms) ; clavier numérique pour
      les nombres ; défilement avec le clavier ouvert ; rien qui ne soit sur la fiche.
      → revue de la maquette reportée.
- [x] GDH : prérempli à l'heure du téléphone en loc, Z (`112150Z NOV 26`) et fra ; le choix du fuseau est gardé après
      redémarrage ; un GDH illisible est signalé sans bloquer l'envoi.
      → GDH illisible non essayé.
- [x] « Envoyer » grisé avec sa raison : n° de tir vide ; décalage Rep sans direction ; plus de 9 parties ; radio
      déconnectée. Avertissement sous un champ saisi avec une espace (Rep, heures `..H..`, morceaux L R U).
      → plus de 9 parties non essayé ; espace dans X impossible au clavier numérique.
- [ ] **Coller une coordonnée** : appui long sur la carte → Copier, ou copie depuis le convertisseur ; dans la case 7, coller
      dans le champ de la ligne G (puis H) : Fuseau, Bande, Carré, X, Y remplis ; texte sans MGRS : message d'erreur ; une
      ligne incomplète est signalée sous la ligne.
      → **KO le 13/11 (D13)** : « Aucune coordonnée complète dans le presse-papier » avec le texte copié par la carte et le convertisseur, voir `docs/RESULTATS-ESSAIS-2026-11-13.md`.

**Envoi et découpage**

- [x] Exemple de la spécification (§4) saisi : aperçu identique à la spécification, 3 messages, 166 / 154 / 168 octets
      (488), « Modifier » revient au formulaire intact.
- [ ] « Envoyer la demande » : la conversation directe avec B s'ouvre ; une seule carte « DT AB1234 · 3 messages », chaque
      partie passe « en file » → « Accusé par … » ; aucune bulle de partie isolée.
      → **KO le 13/11 (D12)** : parties 2 et 3 refusées « envoi trop rapide », « Renvoyer » efficace, voir `docs/RESULTATS-ESSAIS-2026-11-13.md`.
- [x] Demande longue (plusieurs cases pleines) : découpage annoncé par l'aperçu = parties reçues par B ; une case coupée
      reprend son numéro dans la partie suivante.
- [ ] Un client Meshtastic officiel (troisième téléphone, ou B sans NetworkTact) reçoit les parties en texte lisible.
      → sans objet le 13/11 : plus aucun téléphone ne l'a.

**Réception (B)**

- [x] Fiche reconstituée à la place des 3 messages : badge DT, « 3 / 3 reçus », en-tête, cases 1 à 11, codes en clair
      (« Lanceurs 2 : Mo 120 »), « non rempli » en gris, jamais le rouge de `[URG]`. Comparer à la maquette (bandeau rose,
      badge bordeaux).
      → comparaison à la maquette reportée.
- [x] « Voir sur la carte » sous la ligne L R U : la carte centre sur 35T LL 33000 90300 (deux boutons si G et H remplies).
- [x] « Voir les 3 messages bruts » : le texte reçu, tel quel.
- [x] **Partie manquante** : B hors de portée pendant la partie 2 (radio éteinte ou éloignée) : chez B « 2 / 3 reçus »,
      « Partie(s) 2 manquante(s) (cases 5 à 8) », ces cases « non reçu », les autres lisibles ; chez A la partie 2 en échec avec
      « Renvoyer la partie 2 » ; après le renvoi, la fiche de B se complète.
      → vu à l'occasion de D12 (renvoi depuis A, fiche complétée).
- [ ] **Partie en double** : A renvoie une partie déjà reçue (accusé perdu) : B la compte une fois ; les deux restent dans
      les messages bruts.
      → non fait le 13/11 : pas de moyen simple de renvoyer une partie déjà accusée.
- [x] Défilement de la conversation et compteur de non-lus justes malgré les messages masqués ; l'aperçu de la liste des
      conversations montre le texte brut de la dernière partie (connu, accepté en v1).

**Réponses GDH**

- [x] B appuie sur « GDH attribution » : dialogue prérempli à l'heure de B, modifiable, envoi d'un message
      `[DT AB1234] GDH attribution …` ; le bouton devient « Renvoyer le GDH attribution ». Puis « GDH fin ».
- [x] Chez A, la carte affiche « GDH attribution reçu : … » puis le GDH fin, au lieu de « en attente ».

**Brouillon et Renouveler**

- [x] Formulaire rempli à moitié, application fermée (balayée) puis rouverte : le brouillon est là ; il disparaît après
      l'envoi.
- [x] « Renouveler » : reprend la dernière demande envoyée, même n° de tir, GDH remis à l'heure actuelle ; si un brouillon
      est en cours, confirmation avant de le remplacer. Envoyée à nouveau, elle forme une nouvelle carte chez B (rang déjà
      reçu avec un autre texte, ou plus de 2 h après).

**Notifications (B, application en arrière-plan)**

- [x] 3 parties reçues : **une seule** notification de la conversation, qui sonne une fois et passe de « Demande de tir
      AB1234 · 1 / 3 reçus » à « 3 / 3 reçus » ; aucune ligne de texte brut `[DT …`.
- [x] Un autre message ordinaire, ou une autre demande, sonne normalement.
- [x] Chez A, la réponse GDH : une notification d'une ligne « DT AB1234 · GDH attribution … ».

**Mode démo**

- [x] Conversation directe d'ALPHA-1 : la demande de l'exemple, reçue en 3 parties dans le désordre, s'affiche en fiche
      complète avec le GDH attribution `112252 loc NOV 26` renvoyé.
- [x] Formulaire ouvert en démo, demande envoyée à BRAVO-2 : rien ne part par radio ; parties accusées ; environ 13 s après
      l'envoi, le GDH attribution arrive, puis le GDH fin 20 s plus tard. Vers CHARLIE-3 : parties en échec, aucune réponse.
- [x] Sortie de la démo : brouillon et dernière demande de la démo oubliés, ceux du vrai mode intacts.

## Avant fusion dans `main`
- [ ] Les runs 70 et 71 ont été annulés par une poussée plus récente (rien à corriger).
- [ ] Compiler la variante google et lancer `core:ui` / `core:navigation` à la main si nécessaire.
