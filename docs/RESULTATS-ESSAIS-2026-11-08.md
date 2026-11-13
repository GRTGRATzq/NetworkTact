# NetworkTact : résultats des essais du 8 novembre 2026

APK : run 72 de `fork-apk` (`feat/accueil-insignes`, commit `9196e76`).
Postes : OA = PC (A), CDS TIR = Terrain 1 (B), Drone = Terrain 2 (C) ; CDU rejoint en soirée (poste isolé), PCR resté hors service.
Radios remises à zéro, région EU_868, préréglage LongFast, canal principal LongFast.
Équipes : `PC;Obs;Tir` (CDS TIR → Tir, Drone → Obs).

## Bilan

Check-list `TESTS-A-FAIRE.md` entièrement parcourue (sauf l'essai 9, sans objet). Messagerie, équipes,
coordonnées, carte hors ligne, organigramme par QR, autodiagnostic et thème fonctionnent sur radios
réelles. 8 défauts et 1 amélioration relevés, dont un seul bloquant pour le terrain (D2).

## Priorités pour les corrections (une branche par sujet, commits petits)

| Ordre | Sujet | Branche proposée |
|---|---|---|
| 1 | D2 renommage non propagé (nom et équipe) | `fix/diffusion-nom` |
| 2 | D1 retirer le « Mode Démo » Meshtastic de la liste des appareils | `fix/demo-unique` |
| 3 | D3 + D4 + D5 formulaire Fait observé (limite 200 octets, « (veille) », clavier) | `fix/fait-observe` |
| 4 | D8 écart LoRa attendu non signalé | `fix/diag-lora-attendu` |
| 5 | D7 + A1 organigramme (limite 39 octets, choix des radios connues) | `fix/organigramme-saisie` |
| 6 | D6 mention d'attribution de la carte | `fix/carte-attribution` |
| 7 | Documents : ramener `TESTS-A-FAIRE.md`, `HISTORIQUE.md`, `FLASHER-LA-RADIO.md` et `FORK.md` à jour de la branche `claude/intelligent-meitner-ntczv3` (partie de `main`) sur la pile, ajouter la procédure de préparation des radios (P1) et ces résultats | `docs/essais-2026-11-08` |

Après chaque correction : APK via `fork-apk`, puis réessai du seul point concerné.

## Résultats

| Essai | Résultat | Remarque |
|---|---|---|
| 0.1 à 0.4 Installation, bienvenue, thème sombre, police agrandie | OK | |
| 1.1 Bandeau mode démo | OK | Après correction du chemin (voir défaut D1) |
| 1.2 Envoi simulé à BRAVO-2 | OK | En attente de ma radio → Transmis à ma radio → Relayé par le réseau → Accusé par BRAVO-2 |
| 1.3 Échec CHARLIE-3 | OK | ≈ 8 s, « Échec, aucun nœud n'a confirmé la réception » |
| 1.4 Écrans permis / indisponibles | OK | |
| 1.5 Quitter la démo | OK | |
| 2.1 Ouverture sur la vue PC, fraîcheur | OK | Noms : voir D2 |
| 2.2 Priorités [URG] / [CR] / info, filtres | OK | En-têtes faux tant que les noms n'étaient pas propagés (D2) |
| 2.3 Message de canal jamais « accusé » | OK | |
| 2.4 Message direct accusé | OK | Délai ≤ 10 s, radios côte à côte |
| 3.1 à 3.4 Liste d'équipes, adoption, choix d'équipe | OK | Après contournement D2 pour CDS TIR |
| 3.5 Radio déconnectée : choix grisé | OK | |
| 3.6 Nom long trop long avec équipe | OK | Avertissement de troncature, équipe conservée |
| 4.1 Convertisseur DMS → MGRS / UTM, erreurs | OK | 48 51 30 N 2 17 40 E = 31U DQ 48247 11947 |
| 4.2 Ma position | OK | Radio sans fix GPS (intérieur) : position du téléphone, signalée |
| 4.3 Partager le point après envoi | OK | Contrôle dans l'appli officielle : sans objet (essai 9) |
| 4.4 Fait observé, heure, (veille) | KO | Heure 14:05 OK ; « (veille) » absent avec 23h50 (D4) ; aucune limite de 200 octets (D3) ; clavier difficile à fermer (D5) |
| 4.5 Voir sur la carte depuis OA | OK | |
| 5.1 Import : refus d'un mauvais fichier | OK | |
| 5.2 Appui long : coordonnées et 4 actions | OK | |
| 5.3 Recaler, centrer Radio / Téléphone | OK | Radio OA avec fix GPS : écart radio / téléphone 58 m (intérieur, à revoir dehors) |
| 5.4 Diffuser ma position : refus sans heure de relevé | OK | Refus prévu ; TIMESTAMP absent par défaut après remise à zéro (P1) |
| 5.4 Diffuser ma position (TIMESTAMP coché) | OK | Envoi unique, reçu sur CDS TIR avec l'heure du relevé ; refus radio éteinte |
| 5.5 Carte hors ligne Cincu : import, annulation, mode avion | OK | `cincu-30km.pmtiles`, 15 Mo, voir « Carte de Cincu » |
| 5.6 Diacritiques roumains (ș, ț, ă, â, î) | OK | |
| 5.7 Mentions d'attribution | KO mineur | Affiche « © OpenStreetMap » seul (D6) |
| 5.8 Suppression de la carte | OK | |
| 6.1 à 6.2 Organigramme : sections, membres, radios associées | OK | Indicatif = nom long exact sans suffixe d'équipe (A1) |
| 6.3 Refus doublon / indicatif trop long | KO partiel | Doublon cohérent ; indicatif de 40 caractères accepté sans message (D7) |
| 6.4 Monter / descendre, retrait confirmé | OK | |
| 6.5 à 6.8 QR : affichage, lecture seule, scan, Ignorer / Adopter, rescan identique | OK | |
| 6.9 QR qui n'est pas un organigramme | OK | QR quelconque ; QR de canal non trouvé dans les réglages |
| 6.10 à 6.13 Navigation, conflits de radio, taille du QR, démo | OK | |
| 7.1 Bandeau « Radio non connectée », croix, retour | OK | |
| 7.2 Écran de diagnostic, aucun verdict | OK | |
| 7.3 LoRa attendu différent du réel | KO | MEDIUM_FAST attendu contre LONG_FAST réel : aucune pastille d'écart (D8) |
| 7.4 Tester la liaison | OK | Traceroute OA → CDS TIR : aller 6,5 dB, retour 5,5 dB, 1,7 s ; blocage 30 s ; refus radio éteinte |
| 7.5 Isolement : « Aucun nœud entendu », seuils, Diagnostic après échec | OK | Observé au fil de la journée, non chronométré |
| 8.1 à 8.4 À propos, thèmes, icône Ma radio, hors ligne | OK | |
| 9 Compatibilité avec l'application Meshtastic officielle | Non testé, sans objet | Plus aucun téléphone ne l'a ; NetworkTact seul sur le réseau |

## Défauts et améliorations

- **D1, mode démo en double** : la liste des appareils (onglet Ma radio) propose encore le « Mode Démo »
  hérité de Meshtastic (fausse radio, nœuds Demo Handset, Riverside Base…), qui remplace la vraie
  radio et n'a pas de bouton Quitter. Confusion directe avec le mode démo NetworkTact (Réglages).
  → Retirer ou masquer cette entrée dans la version NetworkTact.
- **D2, renommage non propagé (prioritaire)** : un changement de nom long / court, y compris le choix
  d'équipe, est bien enregistré sur la radio mais les autres postes gardent l'ancien nom. Le
  redémarrage de la radio a suffi pour Drone, pas pour CDS TIR (diffusion limitée par le firmware).
  Contournement : sur le poste qui voit l'ancien nom, fiche du nœud → « Infos utilisateur ».
  → Après un changement de nom ou d'équipe, faire diffuser le nouveau nom à tout le réseau
  automatiquement, ou proposer un bouton « Diffuser mon nom ».
- **D3, fait observé sans limite de taille** : une description très longue est acceptée et le message
  part en entier ; ni taille affichée ni bouton grisé (attendu : blocage au-delà de 200 octets). À
  vérifier : ce qui arrive réellement côté réception (message coupé, refusé par la radio ?).
- **D4, « (veille) » absent** : une heure d'observation pas encore atteinte (23h50 saisi vers 18 h)
  n'est pas marquée « (veille) » dans le message.
- **D5, clavier du formulaire Fait observé** : pour sortir de la saisie de la description, il faut
  toucher le champ des coordonnées ; pas de moyen évident de fermer le clavier.
- **D6, mention d'attribution incomplète** : sur une carte `.pmtiles` Protomaps, le bouton ⓘ affiche
  « © OpenStreetMap » (texte des métadonnées du fichier) au lieu de « © contributeurs OpenStreetMap ·
  Protomaps » prévu. Mineur, mais à corriger avant diffusion.
- **D7, indicatif trop long accepté** : dans l'organigramme, un indicatif de 40 caractères est accepté
  sans message (attendu : refus au-delà de 39 octets, avec la taille).
- **A1, saisie des membres de l'organigramme** : l'indicatif doit être retapé exactement comme le nom
  long de la radio (ici « CDS TIR - a4cc »), sinon « Aucune radio associée ». → Proposer la liste des
  radios connues à l'ajout d'un membre.
- **D8, écart LoRa non signalé** : sur l'écran de diagnostic d'OA (mode PC), un réglage attendu
  MEDIUM_FAST alors que la radio est en LONG_FAST n'affiche pas de pastille d'écart.

- **A2, fluidité** : navigation jugée peu fluide. Cause principale : l'APK de test est une version
  debug (débogable, sans R8, instrumentée, sans profil de démarrage ; identifiant
  `com.geeksville.mesh.fdroid.debug`). → Tâche `feat/apk-release` après les corrections : variante
  `fdroidRelease` signée par une clé propre, profil de démarrage fdroid.

Compte rendu pour la hiérarchie : https://claude.ai/artifact/YD594aqoBrFpjmS6cFWSXL

## Carte de Cincu

Rayon de 30 km autour de Cincu (45,917 N, 24,800 E) : `--bbox=24.41,45.64,25.19,46.19 --maxzoom=15`,
édition Protomaps `https://build.protomaps.com/20261107.pmtiles`, fichier `cincu-30km.pmtiles` de 15 Mo.
Outil : `go-pmtiles-1.31.2_Darwin_x86_64.zip` (MacBook Air Intel : la version arm64 donne « bad CPU type »).

## Préparation des radios (constats)

- **P1** : après une remise à zéro, il faut sur chaque radio, dans NetworkTact (Réglages → Réglages de
  l'appareil) : LoRa → Région EU_868, préréglage LongFast ; Position → GPS activé, « Champs de
  position » → cocher TIMESTAMP en plus des cases déjà cochées ; Utilisateur → nom long et nom court ;
  puis Réglages → Équipes. Fait sur toutes les radios le 8/11 à 19 h 26.
- **P2** : le téléphone CDU affichait une application « Meshtastic » ; réglé en désinstallant puis
  réinstallant l'APK 72. Origine non établie (ancienne version ou application officielle).
  → Ne garder que NetworkTact sur les téléphones de terrain (plus aucun téléphone n'a l'application officielle en fin de journée).

## Suite

Corriger dans l'ordre ci-dessus, puis rejouer les essais concernés (2.1, 3.4, 4.4, 6.3, 7.3, 5.7) et
faire un essai à distance réelle (relais entre radios, portée autour de Cincu).
