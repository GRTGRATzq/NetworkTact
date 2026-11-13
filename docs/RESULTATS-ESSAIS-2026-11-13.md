# NetworkTact : résultats des essais des 13 et 14 novembre 2026

APK : `apk-fdroid-release` du run 106 de `fork-apk` (`feat/demande-de-tir`, commit `120d0f3`), fichier
`androidApp-fdroid-universal-release.apk`. Certificat SHA-256 `f4ad3e41…79205818`, signature v2 seule (vérifié dans le
journal du run, étape « Check the release APK » ; `keytool -printcert` répond « Fichier JAR non signé », ce qui est normal pour
une signature v2 seule).
Postes : OA = vue PC (A) ; CDU, PCR, CDS TIR, Drone en Terrain. Équipes : `PC;OBS;TIR` (PC : OA, CDU, PCR ; OBS : Drone ;
TIR : CDS TIR). Radios du 8/11 (EU_868, LongFast), **sans les réglages C0** : fausses alertes « POSITION ANCIENNE » connues,
non comptées. Séance du 13/11 21 h 18 au 14/11 0 h 50, heure de Roumanie. Journal minute par minute : `JOURNAL-ESSAIS-2026-11-13.md`.

## Bilan

Check-list `TESTS-A-FAIRE.md` §10 à §18 parcourue, ainsi que les essais KO du 8/11. **Tous les défauts du 8/11 sont corrigés
(D1, D2, D3, D4, D6, D7, D8) et A1 est fait, sauf D5 (clavier), qui ne l'est qu'à moitié (D10).** La release est plus rapide
au démarrage et ne montre aucun plantage lié à R8. Le téléchargement de cartes fonctionne de bout en bout (Cincu 60 × 60 km,
zoom 15 : 18,2 Mo en 15 s environ). La demande de tir fonctionne sur radios réelles (découpage exact 166 / 154 / 168 octets,
fiche reconstituée, GDH, brouillon, Renouveler, une seule notification), avec deux défauts : l'envoi en un geste bute sur la
limite de débit de la radio (D12) et « Coller une coordonnée » ne reconnaît pas le texte copié par l'application (D13).
**Aucun défaut bloquant** ; 5 défauts, 4 remarques, 2 améliorations demandées.

## Priorités pour les corrections (une branche par sujet, commits petits)

| Ordre | Sujet | Branche proposée |
|---|---|---|
| 1 | D12 demande de tir : espacer automatiquement les parties (ou les renvoyer seules) pour ne plus buter sur « envoi trop rapide » | `fix/dt-espacement` |
| 2 | D13 demande de tir : « Coller une coordonnée » doit lire le texte copié par l'appui long sur la carte et par le convertisseur | `fix/dt-coller-coordonnee` |
| 3 | D10 Fait observé : la touche d'action du clavier (✓ / OK) doit fermer le clavier dans la description et l'heure | `fix/fait-observe-clavier` |
| 4 | D11 écran des licences introuvable (mention BSD-3-Clause de `pmtiles-reader` obligatoire) | `fix/ecran-licences` |
| 5 | D9 équipe dont seule la casse change (Obs → OBS) : renommer la radio ; à reproduire d'abord | `fix/equipe-casse` |
| 6 | R1 libellés anglais, R2 message « diffusé à tous les postes » après un choix d'équipe, R3 accès à « Télécharger une carte » | `fix/textes-et-acces` |
| 7 | A3 Réglages en sections repliables ; A4 renommer une carte téléchargée | `feat/reglages-sections`, `feat/carte-renommer` |
| 8 | R4 noms longs sans le suffixe « - a4cc » : à traiter dans la préparation des radios (C0, `PREPARER-LES-RADIOS.md`), pas dans le code | `docs/preparer-les-radios` |

Après chaque correction : APK via `fork-apk`, puis réessai du seul point concerné. D12 et D13 sont à corriger avant l'essai
à distance réelle de Cincu.

## Résultats

| Essai | Résultat | Remarque |
|---|---|---|
| P1 Désinstallation de la debug (5 postes) | OK | |
| P2 Installation de la release universal (5 postes) | OK | Premier lancement sans souci |
| P3 Empreinte du certificat | OK | Journal du run 106 ; signature v2 seule |
| P4 Radios reconnectées, liste `PC;OBS;TIR`, équipes | OK | Drone après le contournement de D9 |
| 10.1 à 10.3 Choix d'équipe sur CDS TIR, nom reçu par OA sans « Infos utilisateur » | OK | Environ 30 s. **D2 corrigé**. Message « diffusé à tous les postes » non vu (R2) |
| 10.4 Nom court | OK | Environ 15 s, message de diffusion vu |
| 10.5 « Diffuser mon nom » : confirmation, blocage 30 s, radio éteinte | OK | |
| 10.6 « Non joignable » seul : rien diffusé | OK | |
| 10.7 Démo : diffusion simulée | OK | |
| 10.8 Si OA garde l'ancien nom | Sans objet | Nom reçu à chaque fois |
| 11.1, 11.3 Plus de « Mode Démo » Meshtastic ; démo NetworkTact intacte | OK | **D1 corrigé** |
| 11.2 Mise à jour par-dessus une debug en « Mode Démo » | Sans objet | Debug désinstallée avant |
| 12.1, 12.2 Fait observé au-delà de 200 octets | OK | **D3 corrigé** |
| 12.3 « (veille) » | OK | **D4 corrigé**. Heure acceptée sans « : » ni « h », apprécié |
| 12.4 Touche d'action du clavier | KO | Sans effet dans la description et l'heure (D10) |
| 13.1 à 13.3 Écart LoRa attendu / réel, autre région, Effacer | OK | **D8 corrigé** |
| 13.4 Si pastille manquante | Sans objet | |
| 14.1, 14.2 Indicatif 40 caractères, 20 × « é », fonction 25 octets | OK | **D7 corrigé**. Bouton nommé « D'accord » (doc : « OK ») |
| 14.3 « Radios connues » | OK | **A1 fait** ; organigramme refait à partir de cette liste |
| 14.4 à 14.7 Saisie libre, doublon, modification, démo | OK | |
| 15.1 Crédit ⓘ d'une carte Protomaps importée | OK | **D6 corrigé** |
| 15.2, 15.3 Autre `.pmtiles` avec ou sans attribution | Sans objet | Pas d'autre fichier |
| 15.4 Fond en ligne | OK | « OpenStreetMap » |
| 16.1 à 16.4 Release : installation, fumée R8, fluidité, plantages | OK | Démarrage plus rapide qu'en debug ; aucun plantage |
| Rejeu 2.1 Vue PC : ouverture, noms et équipes, fraîcheur | OK | |
| Rejeu 3.4 Choix d'équipe | OK | Couvert par P4 et 10.2 |
| Rejeux 4.4, 5.7, 6.3, 7.3 | OK | Voir 12, 15, 14 et 13 ; 4.4 sauf clavier (D10) |
| 17.1 Cincu 60 × 60 km, zoom 15 | OK | Estimation 15,5 à 58,5 Mo ; calculée 18,3 Mo, 7 101 tuiles ; réelle 18,2 Mo en 15 s environ ; poignées très faciles, dimensions justes |
| 17.2 Mode avion, crédit ⓘ | OK | |
| 17.3 Écran de la source : mention, `http://` refusé, `https://` gardé | OK | |
| 17.4 Grande zone Sibiu-Cincu : confirmation au-delà de 50 Mo, Annuler sans fichier résiduel | OK | |
| 17.5 Coupure puis Reprendre | OK | |
| 17.6 Application fermée pendant le téléchargement, Reprendre depuis la liste | OK | |
| 17.7 Ligne d'édition, ancienne carte importée, démo | OK | |
| 17.7 Écran des licences | KO | Introuvable (D11) |
| 17.8 Export (sélecteur Android, Retour, Download), puis `pmtiles verify` et `show` sur le Mac | OK | Mêmes type et zooms que `cincu-30km.pmtiles`. Comparaison à zone égale non faite |
| 17.9 Source injoignable / sans édition | OK | |
| 17 Import de l'artefact `cartes-cincu`, espace presque plein | Non fait | Facultatif / difficile à reproduire |
| 18.1, 18.2 Démo : fiche reçue d'ALPHA-1 ; envoi vers BRAVO-2 et CHARLIE-3 | OK | |
| 18.3 Sortie de démo | OK | |
| 18.4 Formulaire OA → CDS TIR, exemple de la spécification, aperçu | OK | 166 / 154 / 168 octets après saisie exacte ; d'abord 173 / 163 / 168 à cause de la saisie (destinataire « CDS TIR - a4cc », R4 ; deux cases remplies en 5) |
| 18.5 Envoi et réception | OK avec défaut | Parties 2 et 3 refusées « envoi trop rapide », renvoi manuel efficace (D12). Fiche 1 / 3 puis 3 / 3, Voir sur la carte, messages bruts OK. Couvre l'essai « partie manquante » |
| 18.6 GDH attribution et GDH fin | OK | Notification chez OA |
| 18.7 Brouillon | OK | |
| 18.8 Renouveler, une seule notification 1 / 3 → 3 / 3 | OK | |
| 18.9 Coller une coordonnée (lignes G et H) | KO | D13 |
| 18.10 GDH Z et fra, choix gardé ; Envoyer grisé (n° de tir vide) | OK | Espace dans X impossible au clavier numérique : avertissement sans objet à la main |
| 18.11 Envoyer grisé (Rep sans direction, radio éteinte) ; ouverture depuis un canal | OK | |
| 18.12 Demande longue Drone → CDS TIR, message ordinaire, non-lus | OK | |
| 18 Partie en double | Non fait | Pas de moyen simple de renvoyer une partie déjà accusée |
| 18 Client Meshtastic officiel | Sans objet | Plus aucun téléphone ne l'a |

## Défauts, remarques et améliorations

- **D9, équipe dont seule la casse change (gênant)** : Drone portait « Drone [Obs] » (liste du 8/11). Nouvelle liste `PC;OBS;TIR`
  adoptée, OBS choisi : le nom reste « [Obs] » sur Drone et sur la vue PC. Contournement : redémarrage, équipe « aucune », puis OBS.
  **Non reproduit** sur CDS TIR (« [Tir] » est bien passé à « [TIR] »). Cause supposée : équipe comparée sans tenir compte de la
  casse. → À reproduire, puis renommer la radio dès que le texte de l'équipe diffère, même par la casse.
- **D10, touche d'action du clavier dans « Fait observé » (gênant ; suite de D5)** : la touche ✓ (clavier Crosscall) ou « OK »
  (autre modèle) est affichée mais sans aucun effet dans la description et dans le champ d'heure : ni fermeture du clavier, ni
  retour à la ligne. Elle fonctionne dans « Lieu ». Pour sortir de la description, il faut toujours toucher « Lieu ».
  → La touche doit fermer le clavier dans les deux champs.
- **D11, écran des licences introuvable (cosmétique, mais à corriger avant diffusion)** : aucun écran de licences trouvé dans
  Réglages (Information compris) ; `pmtiles-reader` et sa licence BSD-3-Clause doivent y figurer. → Vérifier s'il manque dans la
  variante fdroid ou s'il est mal placé, et le rendre accessible depuis Information.
- **D12, « envoi trop rapide » pendant une demande de tir (gênant)** : OA → CDS TIR, exemple de la spécification (3 parties) : la
  partie 1 est accusée, les parties 2 et 3 sont refusées sur l'OA avec « envoi trop rapide ». « Renvoyer » sur chaque partie
  refusée passe très bien. Cause supposée : limite de débit de la radio, parties envoyées à la suite sans pause. →
  Envoi en un seul geste : espacer les parties automatiquement, ou renvoyer seules celles que la radio refuse pour ce motif.
- **D13, « Coller une coordonnée » ne reconnaît pas le texte copié par l'application (gênant)** : le bouton de la ligne G (et H)
  répond « Aucune coordonnée complète dans le presse-papier » avec le texte copié par l'appui long sur la carte comme par le
  convertisseur. Le texte copié est de la forme « Point carte · MGRS 35TLL… · UTM 35T… · DMS 45°… » (plusieurs systèmes à la
  suite, MGRS peut-être sans espaces). Collé à la main dans « Fuseau », le texte entier y entre, sans les espaces, sans découpage
  ni message. → Retrouver le MGRS dans ce texte et remplir Fuseau, Bande, Carré, X et Y ; tester avec les textes réels copiés
  par la carte et le convertisseur.

- **R1, libellés en anglais** : Réglages de l'OA, par exemple « Show full message timestamps ». Relevé non exhaustif. → Traduire.
- **R2, message de diffusion après un choix d'équipe** : « Nom … diffusé à tous les postes » vu après un changement de nom court,
  pas après un choix d'équipe (la radio redémarre) ; le nom est pourtant bien reçu. → Vérifier que le message s'affiche après la
  reconnexion.
- **R3, accès au téléchargement de carte** : l'entrée « Cartes sur le téléphone › Télécharger une carte » est difficile à trouver ;
  confondue avec « Gérer les calques de la carte ». → À reprendre dans le guide utilisateur, ou par un accès plus visible.
- **R4, suffixe « - a4cc » des noms longs** : nom par défaut de Meshtastic laissé sur les radios ; il alourdit chaque message
  (+7 octets sur la première partie d'une demande de tir) et le destinataire prérempli. → Retirer à la préparation des radios (C0).

- **A3, Réglages en sections repliables** : menus déroulants, chacun avec ses sous-parties : Configuration ; Sauvegarder et
  restaurer ; Avancé ; Mode démo ; Permissions ; App ; Information.
- **A4, renommer une carte téléchargée** depuis « Cartes sur le téléphone » (aujourd'hui, le nom ne se change qu'avant le
  téléchargement, ou à l'export dans le sélecteur Android).

## Demande de tir : maquette et cases à verrouiller

- Aucune remarque de rendu relevée pendant ces essais ; le formulaire sera revu lors de l'essai avec la hiérarchie (« modification
  de maquette » dans la conversation de pilotage).
- **Cases à verrouiller (obligatoires) : décision reportée à une prochaine session** (en v1, seul le n° de tir est obligatoire).

## Suite

1. Confier à Claude Code D12 et D13, puis D10, D11, D9 ; R1 à R3 et A3, A4 ensuite (ordre du tableau ci-dessus).
2. C0, réglages radio pour 50 radios, avec R4 ; puis C2 (seuils de la vue PC réglables).
3. Réessai sur radios du seul point corrigé, puis essai à distance réelle sur le secteur de Cincu (semaine du 16/11).
4. Décider des cases à verrouiller de la demande de tir.
