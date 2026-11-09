# NetworkTact : essais à faire sur téléphone

Dernière mise à jour : 9 novembre 2026. APK : artefact `apk-fdroid-debug` du dernier run vert de
`fork-apk` sur la branche `fix/fait-observe` (qui contient tout). Rien de ce qui suit n'a encore
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

## Avant fusion dans `main`
- [ ] Les runs 70 et 71 ont été annulés par une poussée plus récente (rien à corriger).
- [ ] Compiler la variante google et lancer `core:ui` / `core:navigation` à la main si nécessaire.
