# NetworkTact : essais à faire sur téléphone

Dernière mise à jour : 9 novembre 2026. APK : artefact `apk-fdroid-debug` du dernier run vert de
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

## 10. Diffusion du nom (§21), défaut constaté le 8 novembre

Deux radios préparées (`PREPARER-LES-RADIOS.md`) : la radio **CDS TIR** et la radio **OA**, qui se
voient déjà. Sur le téléphone de OA, ne jamais utiliser « Infos utilisateur » pendant cet essai.

- [ ] Sur OA, noter le nom affiché pour CDS TIR (liste des nœuds et fiche du nœud).
- [ ] Téléphone de CDS TIR : Réglages → Équipes, choisir une équipe, confirmer. Message « Nom … envoyé
      à ma radio », la radio redémarre, puis « Nom … diffusé à tous les postes » dès sa reconnexion.
- [ ] Sur OA, **sans** « Infos utilisateur » : le nom long `CDS TIR [Équipe]` apparaît, en moins d'une
      minute après la diffusion. Noter le délai.
- [ ] Téléphone de CDS TIR : Réglages de l'appareil → Utilisateur, changer le nom court, enregistrer.
      Même vérification sur OA pour le nom court.
- [ ] « Diffuser mon nom » (Utilisateur ou Équipes) : confirmation obligatoire ; après envoi, bouton
      bloqué 30 s avec décompte, sur les deux écrans. Radio éteinte : « Aucune radio connectée ».
- [ ] Changer seulement « Non joignable par message » dans Utilisateur : rien n'est diffusé.
- [ ] Mode démo, Réglages → Équipes : choisir une équipe puis « Diffuser mon nom » : « diffusion
      simulée, rien n'est envoyé » ; OA ne reçoit rien.
- [ ] Si OA garde l'ancien nom : noter la version du firmware des deux radios et si un message
      « Ma radio n'a pas confirmé le nom » est apparu.

## 11. Un seul mode démo (§22), défaut D1 du 8 novembre

- [ ] Ma radio, liste des appareils, en Bluetooth, réseau et USB : plus aucune section « Mode démo »,
      ni « Mode Démo » ni « Mode démo (Rejouer) ». Aucun nœud Demo Handset ou Riverside Base.
- [ ] Après mise à jour par-dessus une version où « Mode Démo » avait été choisi : la vraie radio
      est de nouveau proposée et s'y reconnecte, sans repasser par la fausse.
- [ ] Réglages → Mode démo (celui de NetworkTact) fonctionne toujours : bandeau et bouton Quitter.

## 12. Formulaire « Fait observé » (§23), défauts D3, D4 et D5 du 8 novembre

À faire sur le téléphone, clavier ouvert (c'est là que les défauts sont apparus).

- [ ] Description très longue (coller 300 caractères) avec un lieu valide : le champ devient rouge et
      « Trop long : N/200 octets » apparaît **juste sous le champ**, sans défiler ; « Insérer dans le
      message » est grisé ; rien n'est coupé. Raccourcir : tout redevient normal à 200 octets.
- [ ] Même description avant d'avoir saisi le lieu : la taille s'affiche déjà.
- [ ] Heure pas encore atteinte (saisir `23h50` ou `2350` vers 18 h) : « (veille) » sous le champ
      d'heure et `23:50 (veille)` dans le rapport. Heure déjà passée : pas de « (veille) ».
- [ ] Clavier de la description : la touche d'action est « Terminé » et ferme le clavier sans
      toucher un autre champ. Idem pour le champ d'heure. Pas de retour à la ligne ajouté.

## 13. Diagnostic : écart LoRa (§25), défaut D8 du 8 novembre

À faire en mode PC, sur une radio réglée en LONG_FAST.

- [ ] Réseau → Diagnostic → carte « Réglage LoRa attendu » → Modifier : choisir le préréglage MEDIUM_FAST
      (même région que la radio), Enregistrer. Une pastille « Ma radio est en … LONG_FAST : écart avec ce
      réglage » apparaît **dans cette carte**, sans défiler ; la carte « Constats » en haut montre aussi
      l'écart et la suggestion de vérifier région et préréglage.
- [ ] Modifier de nouveau pour LONG_FAST : « Identique au réglage de ma radio », plus de pastille ni de
      suggestion. « Effacer » : plus rien dans la carte.
- [ ] Même essai avec une autre région : l'écart apparaît aussi.
- [ ] Si la pastille manque encore : noter ce que dit la ligne « Réglages LoRa » de la carte « Constats »
      (« pas encore reçus de la radio » indiquerait que la configuration LoRa n'arrive pas).

## 14. Organigramme : saisie (§26), défaut D7 et amélioration A1 du 8 novembre

En mode PC, Réseau → organigramme → Ajouter un membre, clavier ouvert.

- [ ] Taper un indicatif de 40 caractères : le champ devient rouge, « Indicatif trop long : 40 octets, 39 au plus »
      s'affiche **sous le champ**, « OK » est grisé. En retirer un : 39 octets, tout redevient normal.
- [ ] Même chose avec des accents (20 fois « é » = 40 octets) et avec une fonction de plus de 24 octets.
- [ ] « Radios connues » : les radios vues, sans suffixe d'équipe, sans celles déjà dans l'organigramme ; en
      toucher une remplit le champ. Taper « alp » ne garde que les indicatifs qui contiennent « alp ».
- [ ] Saisie libre : taper un indicatif absent de la liste (PCR, radio éteinte) et valider : accepté.
- [ ] Ajouter un membre déjà présent (même casse différente) : alerte en haut de la boîte.
- [ ] Modifier un membre existant : pas de liste de radios, taille contrôlée de la même façon.
- [ ] Mode démo : les propositions sont les radios fictives ; rien n'est gardé à la sortie.

## 15. Crédit d'une carte importée (§27), défaut D6 du 8 novembre

Variante fdroid. Le crédit imposé ne dépend que des métadonnées du fichier.

- [ ] Carte Protomaps (le fichier de Cincu, `name` = « Protomaps Basemap »), choisie comme fond : le bouton ⓘ affiche
      « © contributeurs OpenStreetMap · Protomaps » (téléphone en français) ou « © OpenStreetMap contributors ·
      Protomaps » (anglais), même si le fichier ne déclare que « © OpenStreetMap ». Le logo MapLibre reste à gauche.
- [ ] Un autre `.pmtiles` vectoriel dont les métadonnées déclarent une attribution (autre `name`, sans Protomaps dans la
      description) : le bouton ⓘ affiche **celle du fichier**, pas celle de l'application.
- [ ] Un autre `.pmtiles` qui ne déclare aucune attribution : **pas de bouton ⓘ** (rien n'est affiché).
- [ ] Un fond en ligne (OpenFreeMap, etc.) garde son propre crédit ; un fichier `.mbtiles` importé garde le crédit de
      ses métadonnées, ou celui d'OpenStreetMap à défaut.

## 16. APK de release (§28)

APK : artefact `apk-fdroid-release` du dernier run vert de `fork-apk`. Il remplace l'APK de debug pour l'usage courant ;
R8 n'a été vérifié que par la compilation, jamais à l'exécution.

- [ ] Installation : l'APK est signé (sinon Android refuse de l'installer). Désinstaller d'abord la version de debug
      (`com.geeksville.mesh.fdroid.debug`) : la release (`io.github.grtgratzq.networktact`) repart sans données,
      refaire la liste d'équipes et l'organigramme (§28). Premier lancement sans plantage, écran de bienvenue.
- [ ] Parcours de fumée, en cherchant ce que R8 casserait : Réglages, Mode démo, vue PC, Conversations ; carte (fond en
      ligne, import d'un `.pmtiles`, crédit ⓘ, appui long) ; QR de l'organigramme (affichage et lecture à la caméra) ;
      Bluetooth (recherche et connexion d'une radio) ; Réglages de la radio.
- [ ] Fluidité : le démarrage à froid est-il plus rapide qu'avec l'APK de debug ? À noter (ressenti, ou
      `adb shell am start -W`).
- [ ] Un plantage au lancement ou dans une fonction qui marchait en debug : noter l'écran et l'heure, récupérer le
      journal (`adb logcat`), c'est probablement une règle R8 manquante.

## 17. Téléchargement d'une carte (tâche 7) : pas encore disponible

À faire **quand la fonction existera** (`docs/PLAN-CARTE-TELECHARGEMENT.md`, branche `feat/carte-telechargement`). Variante fdroid.

- [ ] **Possible dès maintenant** : télécharger l'artefact `cartes-cincu` du dernier run vert de `fork-carte-interop`, copier
      `networktact-cincu.pmtiles` (écrit par l'extracteur de NetworkTact, 14,65 Mo) sur le téléphone, l'importer par « Importer
      un fichier .pmtiles » : il est accepté, s'affiche comme le fichier du Mac (mêmes tuiles), mode avion compris, avec la
      mention « © contributeurs OpenStreetMap · Protomaps ». Cela vérifie sur téléphone le fichier qu'écrira le téléchargement.

- [ ] Rectangle de Cincu (zone visible puis coins tirés au doigt, environ 60 × 60 km), zoom 15 : la taille annoncée avant, la taille
      réelle après, la durée ; la carte s'affiche, mode avion compris ; mention ⓘ « © contributeurs OpenStreetMap · Protomaps ».
- [ ] Le cadre : poignées faciles à saisir, dimensions en km justes, zone téléchargée = zone du cadre.
- [ ] Au-delà de 50 Mo : confirmation, la même sur Wi-Fi et sur données mobiles ; en dessous : pas de confirmation.
- [ ] Écran de la source : le texte « Le serveur source voit la zone demandée et l'adresse du téléphone » est lisible en entier ;
      une adresse `http://` est refusée ; une adresse `https://` valide est gardée.
- [ ] Annulation en cours : aucun fichier résiduel, espace libre inchangé. Coupure (mode avion) puis reprise : fichier complet.
      Fermeture de l'application pendant le téléchargement, réouverture : « Reprendre ».
- [ ] Mode démo : bouton désactivé, rien téléchargé. Espace presque plein : refus avec les tailles.
- [ ] Un ancien fichier importé du Mac (`cincu-30km.pmtiles`) fonctionne toujours à côté.
- [ ] **Comparaison sur Mac** : récupérer le fichier du téléphone (APK de debug : `adb exec-out run-as
      com.geeksville.mesh.fdroid.debug cat files/pmtiles/<identifiant>.pmtiles > telephone.pmtiles` ; avec l'APK de release, qui
      n'est pas débogable, passer par « Exporter une carte », essai ci-dessous). Puis `pmtiles verify telephone.pmtiles` et
      `pmtiles show telephone.pmtiles`, à comparer à `pmtiles show cincu-30km.pmtiles` (produit le 8 novembre) : type de
      tuile, zooms, nombres de tuiles adressées, d'entrées et de contenus, taille des tuiles. Les bornes et nombres diffèrent
      si les zones diffèrent. **Pour une comparaison à zone égale**, refaire sur le Mac, avec **l'édition affichée par le
      téléphone**, `pmtiles extract <source>/<édition>.pmtiles essai.pmtiles --bbox=<ouest,sud,est,nord du cadre>
      --maxzoom=15` et comparer `pmtiles show essai.pmtiles` : mêmes nombres de tuiles et de contenus attendus.
- [ ] **Exporter une carte** (D12) : « Cartes sur le téléphone » → Exporter sur la carte téléchargée : le sélecteur de fichiers
      Android s'ouvre avec le nom `<nom>.pmtiles` ; choisir `Download` ; le fichier apparaît là, de même taille que dans la
      liste ; rien d'autre n'est demandé (aucune permission, aucun partage vers une application). Annuler le sélecteur : rien
      n'est écrit. Le fichier exporté, copié sur le Mac, sert à la comparaison ci-dessus (`pmtiles verify`, `pmtiles show`) et
      se réimporte dans NetworkTact (« Importer un fichier .pmtiles »).
- [ ] Aucune édition joignable (mode avion juste avant « Calculer la taille ») : « Impossible de joindre la source … » ;
      source réglée sur une adresse sans édition : « Aucune édition de carte n'a été trouvée … pour les 8 derniers jours ».
- [ ] Écran des licences : `pmtiles-reader` apparaît avec la licence BSD-3-Clause.

## Avant fusion dans `main`
- [ ] Les runs 70 et 71 ont été annulés par une poussée plus récente (rien à corriger).
- [ ] Compiler la variante google et lancer `core:ui` / `core:navigation` à la main si nécessaire.
