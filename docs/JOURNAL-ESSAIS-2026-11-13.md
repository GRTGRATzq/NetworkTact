# Journal des essais NetworkTact 13-14/11/2026
APK : apk-fdroid-release, run fork-apk 106 (commit 120d0f3), empreinte attendue f4ad3e41…79205818
Postes : CDU, PCR, OA, CDS TIR, Drone. Radios sans réglages C0 (fausses alertes POSITION ANCIENNE connues).

| Heure | Essai | Résultat | Remarque |
|---|---|---|---|
| 21:18 | Début de séance | | TESTS-A-FAIRE récupéré du dépôt (feat/demande-de-tir) |
| 21:36 | P1 Désinstallation debug (5 postes) | OK | |
| 22:17 | P2 Installation release (universal), 5 postes | OK | Premier lancement RAS ; démarrage plus rapide qu’en debug (§16 fluidité) |
| 22:21 | P3 Empreinte du certificat | OK | Journal run 106 « Check the release APK » : f4ad3e41…979205818, signature v2 seule (keytool sur Mac : « Fichier JAR non signé », normal). Contrôle CI fait sur l'APK arm64, même clé pour universal |
| 22:25 | P4 Configuration retenue | — | OA = vue PC ; équipes PC (OA, CDU, PCR), OBS (Drone), TIR (CDS TIR) ; liste `PC;OBS;TIR` (majuscules) |
| 22:35 | Remarque R1 (OA, Réglages) | Remarque | Libellés encore en anglais, ex. « Show full message timestamps ». Cosmétique. Autres libellés à relever au fil de l'eau |
| 22:43 | D9 (Drone, Équipes) | KO gênant | Ancien nom « Drone [Obs] » (liste du 8/11). Nouvelle liste PC;OBS;TIR adoptée, OBS choisi : nom resté « [Obs] » sur Drone et vue PC. Contournement : redémarrage, équipe « aucune », puis OBS : nom correct « [OBS] » partout. Cause probable : comparaison d'équipe insensible à la casse, pas de renommage |
| 22:43 | P4 Radios reconnectées, liste PC;OBS;TIR, équipes | OK | CDU, PCR, OA [PC] ; Drone [OBS] (après contournement D9) ; CDS TIR sans équipe ; vue PC à jour |
| 22:47 | 10.1 Nom de CDS TIR vu sur OA avant changement | OK | « CDS TIR [Tir] » (nom du 8/11) |
| 22:49 | 10.2 CDS TIR choisit TIR | OK avec remarque | « Nom envoyé à ma radio » vu, radio redémarre ; « Nom diffusé à tous les postes » NON vu (R2, à revérifier). Nom passe à [TIR] : D9 non reproduit (à requalifier) |
| 22:49 | 10.3 OA voit le nouveau nom sans « Infos utilisateur » | OK | « CDS TIR [TIR] » en ≈ 30 s. D2 corrigé |
| 22:56 | 10.4 Nom court CDS TIR changé, vu sur OA | OK | ≈ 15 s, sans « Infos utilisateur » |
| 22:57 | R2 requalifiée | Remarque | « Diffusé à tous les postes » vu en 10.4 (nom court) ; non vu en 10.2 (choix d'équipe, avec redémarrage radio) : à revoir une fois, sinon message manquant après redémarrage |
| 22:58 | 10.5 « Diffuser mon nom » : confirmation, blocage 30 s sur les deux écrans, radio éteinte | OK | |
| 22:59 | 10.6 Seul « Non joignable » changé : rien diffusé | OK | Pas de « diffusé à tous les postes » ; bouton manuel toujours disponible (normal) |
| 23:02 | 10.7 Démo : diffusion simulée, OA ne reçoit rien, retour [TIR] | OK | |
| 23:02 | 10.8 Si OA garde l'ancien nom | Sans objet | Nom reçu à chaque fois |
| 23:03 | 11.1 Plus de « Mode Démo » Meshtastic dans la liste des appareils | OK | D1 corrigé |
| 23:03 | 11.2 Mise à jour par-dessus une debug en « Mode Démo » | Sans objet | Debug désinstallée avant |
| 23:03 | 11.3 Mode démo NetworkTact toujours fonctionnel | OK | |
| 23:09 | 12.1 Description de 300 caractères : rouge, taille, Insérer grisé, retour normal à 200 | OK | D3 corrigé. Mais pour quitter la description il faut toujours toucher « Lieu » (D5 ?, voir 12.4) |
| 23:11 | 12.4 Touche d'action du clavier (description) | KO gênant (D5 persiste, D10) | Poste Drone, clavier Crosscall d'origine : touche ✓ affichée mais sans effet dans la description ; fonctionne dans « Lieu ». Appui = rien du tout (pas de retour à la ligne). Reproduit sur un autre modèle de téléphone (touche « OK »). Idem dans le champ d'heure (23:15) |
| 23:13 | 12.2 Taille affichée avant saisie du lieu | OK | |
| 23:15 | 12.3 « (veille) » : 23h50 → (veille) ; 2300 → rien | OK | D4 corrigé. Heure acceptée sans « : » ni « h » (apprécié) |
| 23:15 | Rejeu 4.4 du 8/11 (Fait observé) | OK sauf clavier | Taille, (veille) corrigés ; clavier : D10 |
| 23:17 | 13.1 Attendu MEDIUM_FAST contre LONG_FAST réel : pastille dans la carte + Constats | OK | D8 corrigé (rejeu 7.3 OK) |
| 23:18 | 13.2 LONG_FAST identique, Effacer | OK | |
| 23:18 | 13.3 Autre région : écart signalé | OK | |
| 23:18 | 13.4 Si pastille manquante | Sans objet | |
| 23:20 | 14.1 Indicatif 40 caractères : rouge, message, bouton grisé, 39 OK | OK | D7 corrigé (rejeu 6.3 OK). Bouton libellé « D'accord » (doc : « OK »), sans gravité |
| 23:21 | 14.2 Indicatif 20 × « é » (40 octets) et fonction 25 octets refusés | OK | |
| 23:22 | 14.3 « Radios connues » : sans suffixe, filtre, remplissage ; organigramme refait | OK | A1 fait |
| 23:23 | 14.4 Saisie libre acceptée | OK | |
| 23:23 | 14.5 Doublon (casse différente) : alerte | OK | |
| 23:25 | 14.6 Modifier un membre : pas de liste, taille contrôlée | OK | |
| 23:25 | 14.7 Démo : radios fictives, organigramme réel intact | OK | |
| 23:29 | 15.1 Crédit ⓘ d'une carte Protomaps | OK | « © contributeurs OpenStreetMap · Protomaps ». D6 corrigé (rejeu 5.7 OK). Fichier .pmtiles importé depuis le téléphone ; import simple |
| 23:30 | 15.2 / 15.3 Autre .pmtiles avec ou sans attribution | Sans objet | Pas d'autre fichier disponible |
| 23:30 | 15.4 Fond en ligne : crédit propre | OK | « OpenStreetMap » |
| 23:30 | 16.1 Installation release, premier lancement | OK | Vu en P2 |
| 23:30 | 16.3 Fluidité | OK | Démarrage plus rapide qu'en debug |
| 23:32 | 16.2 Fumée R8 : appui long carte, QR affiché, QR scanné et adopté | OK | |
| 23:32 | 16.4 Plantage | Sans objet | Aucun plantage |
| 23:32 | Rejeu 3.4 du 8/11 (choix d'équipe) | OK | Couvert par P4 et 10.2 (sauf D9 sur Drone) |
| 23:34 | Rejeu 2.1 du 8/11 : ouverture sur vue PC, noms et équipes, fraîcheur | OK | |

Point d'étape 23:34 : préparation, §10 à §16 et rejeux 2.1, 3.4, 4.4, 5.7, 6.3, 7.3 et D4 faits. Reste §17 (cartes téléchargées) et §18 (demande de tir, validation de la maquette).
| 23:34 | Début §17 (cartes téléchargées) | | |
| 23:40 | R3 Accès au téléchargement | Remarque | Entrée « Télécharger une carte » difficile à trouver (confondue avec « Gérer les calques de la carte ») ; ergonomie, cosmétique |
| 23:42 | 17.1a Cadre ≈ 60 × 60 km Cincu, zoom 15 : estimation et taille calculée | OK | Estimation grossière 15,5 à 58,5 Mo ; calculée 18,3 Mo, 7 101 tuiles. Poignées très faciles, dimensions km justes |
| 23:45 | 17.1b Téléchargement Cincu | OK | ≈ 15 s, taille réelle 18,2 Mo (annoncée 18,3), pas de confirmation, ligne « Édition du …, build.protomaps.com · L × H km » présente |
| 23:47 | 17.2 Carte téléchargée en mode avion, crédit ⓘ | OK | |
| 23:50 | 17.3 Écran source : mention lisible, http:// refusé, https:// gardé | OK | |
| 23:52 | 17.4 Grande zone (Sibiu-Cincu) : confirmation > 50 Mo, Annuler sans fichier résiduel, espace inchangé | OK | Taille annoncée non relevée |
| 23:54 | 17.5 Coupure (mode avion) puis Reprendre : fichier complet | OK | |
| 23:54 | 17.6 Appli fermée pendant le téléchargement : « Téléchargement interrompu », Reprendre depuis la liste | OK | |
| 23:56 | 17.7 Ligne d'édition, ancienne carte importée, démo désactivée | OK | Licences : écran non trouvé (en cours) |
| 00:00 (14/11) | 17.7.4 Écran des licences (pmtiles-reader BSD-3) | KO cosmétique (D11) | Écran des licences introuvable dans Réglages (Information compris). À vérifier : absent de la variante fdroid ou mal placé |
| 00:00 | A3 Demande : Réglages en menus déroulants | Amélioration | Sections repliables avec leurs sous-parties : Configuration ; Sauvegarder et restaurer ; Avancé ; Mode démo ; Permissions ; App ; Information |
| 00:01 | 17.9 Source injoignable (mode avion) / sans édition (example.com) : messages attendus | OK | Source remise sur build.protomaps.com |
| 00:04 | 17.8.2 Export : sélecteur Android avec <nom>.pmtiles | OK | A4 demandée : pouvoir renommer une carte déjà téléchargée (dans « Cartes sur le téléphone ») |
| 00:05 | 17.8.3-4 Export annulé sans fichier ; export dans Download, même taille, aucune permission | OK | |
| 00:07 | 17.8 B Mac : pmtiles verify + show (export) face à cincu-30km | OK | Verify sans erreur, même type et zooms ; nombres de tuiles différents (zones différentes, attendu). Comparaison à zone égale non faite |
| 00:07 | 17.0 Import de l'artefact cartes-cincu (fork-carte-interop) | Non fait | Facultatif, couvert par 17.8 |
| 00:07 | 17.x Espace presque plein | Non fait | Difficile à reproduire |

Point d'étape 14/11 00:07 (arrêt pour la nuit) : préparation, §10 à §17 et rejeux du 8/11 faits. Reste §18 demande de tir (démo, envoi A→B OA→CDS TIR, partie manquante, notifications, GDH, brouillon, Renouveler) et remarques sur la maquette ; puis le compte rendu.
Défauts et remarques à ce stade : D9 gênant (casse Obs/OBS, non reproduit), D10 gênant (touche ✓ sans effet dans description et heure du Fait observé, 2 modèles), D11 cosmétique (écran des licences introuvable), R1 libellés anglais, R2 message « diffusé à tous les postes » absent après choix d'équipe, R3 entrée « Télécharger une carte » difficile à trouver, A3 Réglages en menus déroulants, A4 renommer une carte téléchargée.
| 00:17 | 18.1 Démo : fiche reçue d'ALPHA-1 (3/3, cases 1-11, codes en clair, GDH attribution, pas de rouge URG) | OK | Aucune remarque de rendu |
| 00:18 | 18.2 Démo : envoi vers BRAVO-2 (accusés, GDH attribution puis fin) et CHARLIE-3 (échec) | OK | Formulaire à modifier plus tard (remarques à venir, maquette) |
| 00:26 | 18.3 Sortie de démo / 18.4 formulaire réel OA → CDS TIR, exemple de la spec, aperçu | OK (écart expliqué) | Aperçu 173 / 163 / 168 au lieu de 166 / 154 / 168 : saisie différente de l'exemple. Partie 1 : Destinataire prérempli « CDS TIR - a4cc » (nom long réel de la radio, +7). Partie 2 : Trajectoire 1 / 1 et Mode 2 et 1 saisis (+9). R4 : suffixe Meshtastic « - a4cc » dans les noms longs, alourdit les messages |
| 00:28 | 18.4b Exemple exact : 166 / 154 / 168 octets ; Modifier revient au formulaire intact | OK | |
| 00:30 | 18.5 Envoi réel OA → CDS TIR | KO gênant (D12) | Partie 1 accusée ; parties 2 et 3 refusées sur l'OA avec « envoi trop rapide » (limite de débit de la radio / firmware probable : parties envoyées à la suite sans espacement). Contournement : « Renvoyer » sur chaque partie refusée, passe très bien |
| 00:33 | 18.5 Réception CDS TIR : fiche 1/3 avec parties manquantes nommées, puis 3/3 après renvoi ; Voir sur la carte ; messages bruts | OK | Couvre aussi l'essai « partie manquante » (renvoi depuis A, fiche complétée) |
| 00:34 | 18.6 GDH attribution puis GDH fin depuis CDS TIR, affichés et notifiés chez OA | OK | |
| 00:36 | 18.7 Brouillon gardé après fermeture de l'appli | OK | |
| 00:36 | 18.8 Renouveler (confirmation, GDH remis à l'heure), nouvel envoi, une seule notification 1/3 → 3/3 chez CDS TIR, nouvelle fiche | OK | |
| 00:40 | 18.9 Coller une coordonnée (lignes G et H) | KO gênant (D13) | Collage en case 7 L R U : Fuseau, Bande, Carré, X, Y ne se remplissent pas, saisie manuelle nécessaire. Idem ligne H. Collé dans le champ Fuseau : tout le texte copié entre dans ce seul champ, espaces retirés (« pointcarteMGRS35TLL… »), sans découpage ni message. Texte copié depuis l'appui long : « Point carte … MGRS 35T LL … » (libellé + coordonnée). Boutons « Coller une coordonnée » présents sur la ligne G mais le bouton répond « Aucune coordonnée complète dans le presse-papier » avec le texte copié depuis la carte (et depuis le convertisseur) : l'analyse ne reconnaît pas le format copié par l'application elle-même. Texte copié (appui long carte) : « Point carte · MGRS 35TLL… · UTM 35T… · DMS 45°… » (plusieurs systèmes à la suite, MGRS possiblement sans espaces) |
| 00:46 | 18.10 GDH Z et fra, choix gardé après redémarrage ; Envoyer grisé avec raison si n° de tir vide | OK | Avertissement espace : à refaire (confusion avec le bouton Coller) |
| 00:48 | 18.10.3 Espace dans X (ligne G) | OK | Clavier numérique : espace impossible à saisir, donc avertissement sans objet à la main |
| 00:48 | 18.11 Envoyer grisé (Rep sans direction, radio éteinte) ; ouverture depuis un canal : destinataire à choisir | OK | |
| 00:49 | 18.12 Demande longue Drone → CDS TIR (découpage, case coupée, fiche complète) ; message ordinaire, sonnerie, non-lus | OK | |
| 00:49 | 18.x Partie en double | Non fait | Pas de moyen simple de renvoyer une partie déjà accusée |
| 00:49 | 18.x Client Meshtastic officiel | Sans objet | Plus aucun téléphone ne l'a |
| 00:50 | Fin de séance | | Compte rendu RESULTATS-ESSAIS-2026-11-13.md rédigé ; cases à verrouiller et maquette reportées |
