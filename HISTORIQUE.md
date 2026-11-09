# NetworkTact : historique complet

Établi le 18 octobre 2026 à partir de l'historique Git du dépôt `GRTGRATzq/NetworkTact` et des 72 runs du workflow `fork-apk`. Dernier état : branche `feat/accueil-insignes`, commit `9196e76`, run 72 vert.

## 1. Chronologie

| Date | Événement |
|---|---|
| 26/09/2026 | Création du fork. Dernier commit amont : `64cd7e2` (Meshtastic-Android, #7378). Workflow `fork-apk.yml` créé (`3ec7d47`), run 1 vert. |
| 30/09/2026 | `config.properties` modifié deux fois sur `main`, puis `MIN_SDK` rétabli (`f10739c`, état actuel de `main`). Début des branches `feat/…`. |
| 30/09 → 02/10 | Identité, textes, vue PC, messagerie, logo (sections 1 à 5). |
| 02/10 → 03/10 | Équipes, coordonnées, conversations conformes à la maquette (6 à 8). |
| 04/10 | Mode démo (9). |
| 06/10 | Thème, navigation, fait observé et points partagés (10 à 12). |
| 08/10 → 10/10 | Carte hors ligne lots 1 et 2, coordonnée au toucher et recalage (13 à 15) ; règle « CI Android uniquement » et `ios-check.yml`. |
| 11/10 | Détachement de Meshtastic, organigramme, gel de la CI, autodiagnostic, écran de bienvenue (16 à 20). |
| 18/10 | Documents de référence, essais, remise à zéro de la radio. PR n°7666 ouverte par erreur vers `meshtastic/Meshtastic-Android`, fermée ensuite par l'utilisateur. |

## 2. Structure des branches

Les branches `feat/…` sont **empilées en ligne droite** : chacune contient la précédente (vérifié avec `git merge-base --is-ancestor`). `main` ne contient aucune d'elles. Les nombres sont les commits propres à chaque branche.

| # | Branche | Commits propres | Session de travail (titre) | Section FORK.md |
|---|---|---|---|---|
| 1 | `feat/identite-fork` | 5 | saisie à la main sur GitHub | 1 |
| 2 | `feat/marque-textes` | 7 | NetworkTact fork setup | 2 |
| 3 | `feat/vue-pc` | 9 | Vue PC NetworkTact | 3 |
| 4 | `feat/messagerie-filtres` | 12 | Messagerie et filtres | 4 |
| 5 | `feat/logo` | 9 | NetworkTact instructions for Claude Code | 5 |
| 6 | `feat/equipes` | 14 | NetworkTact document de référence | 6 |
| 7 | `feat/coordonnees` | 15 | NetworkTact fork setup | 7 |
| 8 | `feat/conversation-maquette` | 10 | NetworkTact fork setup | 8 |
| 9 | `feat/mode-demo` | 28 | NetworkTact fork setup | 9 |
| 10 | `feat/theme` | 8 | NetworkTact project setup | 10 |
| 11 | `feat/navigation` | 8 | NetworkTact project setup | 11 |
| 12 | `feat/fait-observe-carte` | 11 | NetworkTact project setup | 12 |
| 13 | `feat/carte-hors-ligne` | 27 | Étude carte observée | 13, 14 |
| 14 | `feat/carte-coordonnees` | 16 | Carte coordonnées | 15 |
| 15 | `feat/detachement` | 21 | NetworkTact document de référence | 16 |
| 16 | `feat/organigramme` | 13 | NetworkTact document de référence | 17 |
| 17 | `feat/ci-gel` | 2 | CI gel branch creation | 18 |
| 18 | `feat/autodiagnostic` | 18 | Autodiagnostic NetworkTact | 19 |
| 19 | `feat/accueil-insignes` | 5 | Renommer et déplacer insignes | 20 |

## 3. Runs de `fork-apk` (1 à 72)

Bilan : 72 runs, 38 verts, 23 rouges, 11 annulés. « Annulé » : annulation automatique par une poussée plus récente sur la même branche (règle du run 34) ou manuelle quand un run est resté figé.

| Run | Date | Branche | Résultat | Objet |
|---|---|---|---|---|
| 1 | 26/09 | main | vert | lancement manuel |
| 2 | 30/09 | identite-fork | rouge | identifiant d'application |
| 3 | 30/09 | identite-fork | vert | rétablir MIN_SDK |
| 4 | 30/09 | identite-fork | vert | nom affiché NetworkTact |
| 5 | 30/09 | identite-fork | vert | strings.xml |
| 6 | 30/09 | identite-fork | vert | mentions de fork et de marque |
| 7 | 30/09 | marque-textes | rouge | service NFC |
| 8 | 30/09 | marque-textes | vert | historique complet pour le lint |
| 9 | 30/09 | vue-pc | rouge | detekt, seuils |
| 10 | 30/09 | vue-pc | rouge | spotless |
| 11 | 30/09 | vue-pc | vert | violations detekt corrigées |
| 12 | 30/09 | messagerie-filtres | rouge | onglets par canal |
| 13 | 30/09 | messagerie-filtres | rouge | étape Tests ajoutée |
| 14 | 30/09 | messagerie-filtres | vert | nom de test Kotlin/Native |
| 15 | 02/10 | messagerie-filtres | vert | logo, source |
| 16 | 02/10 | logo | vert | icône du widget |
| 17 | 02/10 | logo | vert | écran de démarrage |
| 18 | 02/10 | equipes | rouge | écran Équipes |
| 19 | 02/10 | equipes | vert | messages Wire par builder |
| 20 | 03/10 | equipes | rouge | tests core:navigation, machine arrêtée |
| 21 | 03/10 | equipes | vert | core:navigation retiré de l'étape Tests |
| 22 | 03/10 | coordonnees | rouge | écran convertisseur |
| 23 | 03/10 | coordonnees | vert | FORK.md coordonnées |
| 24 | 03/10 | conversation-maquette | vert | carte de message 4 dp |
| 25 | 03/10 | conversation-maquette | rouge | spotless, aperçus |
| 26 | 03/10 | conversation-maquette | annulé | aperçus, bloqué plus de 45 min |
| 27 | 03/10 | conversation-maquette | vert | arrêt à 30 min |
| 28 | 04/10 | mode-demo | rouge | tests core:demo |
| 29 | 04/10 | mode-demo | annulé | réglage en mémoire |
| 30 | 04/10 | mode-demo | annulé | dépôts de démo |
| 31 | 04/10 | mode-demo | rouge | envoi simulé, compilation iOS |
| 32 | 04/10 | mode-demo | rouge | façades d'écran |
| 33 | 04/10 | mode-demo | annulé | import manquant |
| 34 | 04/10 | mode-demo | vert | une poussée annule le run en cours |
| 35 | 04/10 | mode-demo | rouge | FORK.md section 9 |
| 36 | 04/10 | mode-demo | vert | test lien invalide |
| 37 | 06/10 | theme | vert | FORK.md thème, 2 tentatives |
| 38 | 06/10 | navigation | rouge | FORK.md navigation, machine arrêtée |
| 39 | 06/10 | navigation | vert | mémoire Gradle limitée |
| 40 | 06/10 | fait-observe-carte | annulé | encart partager le point |
| 41 | 06/10 | fait-observe-carte | vert | FORK.md fait observé |
| 42 | 08/10 | carte-hors-ligne | vert | FORK.md lot 1 |
| 43 | 08/10 | carte-hors-ligne | rouge | FORK.md lot 2, compilation |
| 44 | 08/10 | carte-hors-ligne | rouge | imports manquants |
| 45 | 10/10 | carte-hors-ligne | rouge | ios-check, figé en configuration |
| 46 | 10/10 | carte-hors-ligne | vert | WaypointInfoSlotTest |
| 47 | 10/10 | carte-hors-ligne | annulé | règle CI Android, figé |
| 48 | 10/10 | carte-hors-ligne | annulé | démon Gradle neuf, figé |
| 49 | 10/10 | carte-hors-ligne | vert | limite de durée par étape |
| 50 | 10/10 | carte-coordonnees | vert | appui long |
| 51 | 10/10 | carte-coordonnees | rouge | FORK.md section 15, lint |
| 52 | 10/10 | carte-coordonnees | rouge | règles Compose de detekt |
| 53 | 10/10 | carte-coordonnees | annulé | import DETAIL_ZOOM |
| 54 | 10/10 | carte-coordonnees | vert | FORK.md étape de compilation, 2 tentatives |
| 55 | 11/10 | detachement | rouge | FORK.md section 16, test AboutScreen |
| 56 | 11/10 | detachement | vert | AboutScreenTest corrigé |
| 57 | 11/10 | organigramme | vert | organigramme en mode démo |
| 58 | 11/10 | organigramme | vert | FORK.md organigramme, 2 tentatives |
| 59 | 11/10 | ci-gel | vert | sans Isolated Projects ni cache de configuration |
| 60-64 | 11/10 | ci-gel | vert ×5 | série de validation, lancement manuel |
| 65 | 11/10 | ci-gel | vert | FORK.md section 18 |
| 66 | 11/10 | autodiagnostic | annulé | mode démo du diagnostic |
| 67 | 11/10 | autodiagnostic | rouge | FORK.md section 19, compilation |
| 68 | 11/10 | autodiagnostic | rouge | compilation, barre du haut |
| 69 | 11/10 | autodiagnostic | vert | attente arrondie juste |
| 70 | 11/10 | accueil-insignes | annulé | remplacé par le run 71 |
| 71 | 11/10 | accueil-insignes | annulé | remplacé par le run 72 |
| 72 | 11/10 | accueil-insignes | vert | FORK.md écran de bienvenue |

## 4. Détail des commits, branche par branche

### `feat/identite-fork` (section 1), 5 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `8af8464` | 30/09 10:40 | GRTGRATzq | chore: identifiant d'application du fork |
| `3332256` | 30/09 10:48 | GRTGRATzq | fix: rétablir MIN_SDK |
| `69e4055` | 30/09 10:59 | GRTGRATzq | chore: nom affiché NetworkTact |
| `1a4a099` | 30/09 11:00 | GRTGRATzq | Update strings.xml |
| `b50a231` | 30/09 11:10 | GRTGRATzq | docs: mentions de fork et de marque |

### `feat/marque-textes` (section 2), 7 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `048e8d9` | 30/09 13:24 | Claude | textes : NetworkTact pour le nom de l'application (EN, FR) |
| `5de8994` | 30/09 13:24 | Claude | textes : NetworkTact pour le nom de l'application (autres langues) |
| `e4a8eff` | 30/09 13:24 | Claude | widget : libellé NetworkTact |
| `74e95fe` | 30/09 13:24 | Claude | accueil : titre tiré du nom de l'application |
| `8499a54` | 30/09 13:29 | Claude | ci: ajouter le lint au workflow fork-apk |
| `40b2827` | 30/09 14:00 | Claude | textes : NetworkTact pour le service NFC de partage |
| `5d6136a` | 30/09 14:02 | Claude | ci: historique complet pour le lint de fork-apk |

### `feat/vue-pc` (section 3), 9 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `ce6f0bf` | 30/09 15:15 | Claude | préférences : mode d'affichage Terrain / PC |
| `611d98c` | 30/09 15:16 | Claude | paramètres : interrupteur du mode d'affichage Terrain / PC |
| `f64a2d4` | 30/09 15:18 | Claude | vue PC : route et logique de fraîcheur des nœuds |
| `7dde25e` | 30/09 15:20 | Claude | vue PC : écran en lecture seule et ViewModel |
| `880d53d` | 30/09 15:21 | Claude | vue PC : navigation, écran de départ et accès depuis la liste |
| `0b7be95` | 30/09 15:22 | Claude | vue PC : test Compose de l'écran (jvmTest) |
| `7106394` | 30/09 15:23 | Claude | vue PC : conformité detekt (seuils en constantes, modifier) |
| `018d842` | 30/09 15:31 | Claude | vue PC : mise en forme exigée par spotless (ktfmt + ktlint) |
| `7a1e8fa` | 30/09 15:37 | Claude | vue PC : corrige les deux violations detekt |

### `feat/messagerie-filtres` (section 4), 12 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `a20b404` | 30/09 16:02 | Claude | messagerie : analyse des préfixes de priorité [URG] / [CR] |
| `af21b6a` | 30/09 16:04 | Claude | messagerie : état honnête des messages envoyés |
| `dc630b0` | 30/09 16:05 | Claude | ressources : chaînes EN/FR de la messagerie (priorités, états, filtres) |
| `472f2b1` | 30/09 16:07 | Claude | messagerie : sélecteur Info / CR / Urgent dans la zone de saisie |
| `85fbf46` | 30/09 16:25 | Claude | messagerie : priorité visible dans la bulle, couleur réservée à la priorité |
| `153f66c` | 30/09 16:32 | Claude | messagerie : état des messages envoyés en icône + texte |
| `ea5b033` | 30/09 16:35 | Claude | messagerie : filtres en haut du fil (type, sans accusé, équipe) |
| `a999e52` | 30/09 16:38 | Claude | conversations : onglets par canal (Tous + canaux configurés) |
| `310b9b3` | 30/09 18:27 | Claude | messagerie : icône d'état composable (corrige la compilation) |
| `3045c5d` | 30/09 18:27 | Claude | ci: lancer les tests des modules modifiés dans fork-apk |
| `41d4247` | 30/09 18:40 | Claude | tests : nom de test compatible Kotlin/Native (CommandPostRowsTest) |
| `186f2df` | 02/10 22:07 | GRTGRATzq | docs: logo NetworkTact (source) |

### `feat/logo` (section 5), 9 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `58121aa` | 02/10 19:26 | Claude | docs : logo NetworkTact déplacé vers docs/networktact-logo.jpg |
| `2558ccd` | 02/10 19:27 | Claude | ressources : symbole vectoriel NetworkTact (ic_networktact) |
| `970c4fa` | 02/10 19:30 | Claude | barre d'application : symbole NetworkTact à la place du logo Meshtastic |
| `b7967ca` | 02/10 19:30 | Claude | lanceur : icône adaptative NetworkTact |
| `ccc299e` | 02/10 19:31 | Claude | lanceur : icônes distinctes pour les versions de test |
| `3aae211` | 02/10 19:35 | Claude | ui : composant NetworkTactLogo (symbole + mot bicolore) |
| `877bfa3` | 02/10 19:35 | Claude | accueil : logo NetworkTact sur l'écran de bienvenue |
| `21d48ce` | 02/10 19:35 | Claude | widget : icône NetworkTact |
| `ecaaf6f` | 02/10 19:58 | Claude | démarrage : symbole NetworkTact sur l'écran de démarrage |

### `feat/equipes` (section 6), 14 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `7adabce` | 02/10 20:34 | Claude | ci : tests des modules touchés par les équipes dans fork-apk |
| `24e3873` | 02/10 20:37 | Claude | équipes : lecture et écriture du message [EQUIPES] |
| `b717099` | 02/10 20:38 | Claude | équipes : suffixe d'équipe dans le nom long |
| `5dc17f4` | 02/10 20:42 | Claude | équipes : stockage local de la liste et de la liste en attente |
| `a7367fe` | 02/10 20:44 | Claude | équipes : réception passive des listes [EQUIPES] |
| `dd6d9e2` | 02/10 20:51 | Claude | équipes : écran Équipes et choix de mon équipe |
| `b89b6c1` | 02/10 20:52 | Claude | équipes : demande de confirmation d'une liste reçue, partout |
| `72c30a8` | 02/10 20:54 | Claude | équipes : diffusion de la liste depuis le PC |
| `5ffbcc8` | 02/10 20:56 | Claude | équipes : équipe affichée après le nom (messagerie, vue PC) |
| `ed579e3` | 02/10 20:58 | Claude | équipes : filtre « Équipe » de la messagerie |
| `f404645` | 02/10 20:59 | Claude | équipes : construction des messages Wire par builder |
| `f131e15` | 03/10 06:21 | Claude | docs : FORK.md décrit toutes les modifications, branche par branche |
| `3390232` | 03/10 06:21 | Claude | ci : tests de :core:navigation dans fork-apk |
| `6545c9d` | 03/10 06:45 | Claude | ci : retire :core:navigation de l'étape Tests de fork-apk |

### `feat/coordonnees` (section 7), 15 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `156ff34` | 03/10 18:31 | Claude | coordonnées : conversion WGS84 ↔ UTM |
| `2fd81f8` | 03/10 18:31 | Claude | coordonnées : conversion UTM ↔ MGRS |
| `fe246f6` | 03/10 18:31 | Claude | coordonnées : DMS et saisie stricte des trois formats |
| `543d8cc` | 03/10 18:34 | Claude | vue PC : logique de fraîcheur déplacée dans core:model |
| `890f290` | 03/10 18:35 | Claude | coordonnées : une référence saisie se relit à l'identique |
| `e12ab59` | 03/10 18:37 | Claude | Ma position : construction du message [POS] |
| `666be54` | 03/10 18:38 | Claude | Fait observé : construction du message [CR] FAIT OBSERVÉ |
| `c3faecb` | 03/10 18:41 | Claude | coordonnées : DMS saisissable sans les symboles ° ' " |
| `4ab137c` | 03/10 18:45 | Claude | coordonnées : textes EN et FR |
| `4df97e1` | 03/10 18:45 | Claude | convertisseur : écran accessible depuis la messagerie |
| `0f5fd99` | 03/10 18:46 | Claude | Ma position : dernière position connue du téléphone |
| `bc84bbb` | 03/10 18:50 | Claude | Ma position : raccourci dans la zone de saisie |
| `3267043` | 03/10 18:52 | Claude | Fait observé : formulaire depuis la zone de saisie |
| `1d0d632` | 03/10 18:53 | Claude | convertisseur : test Compose de l'écran |
| `76c1167` | 03/10 18:53 | Claude | docs : FORK.md décrit la branche coordonnées |

### `feat/conversation-maquette` (section 8), 10 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `94cec60` | 03/10 19:47 | Claude | fraîcheur : âge d'un message reçu |
| `9e581b8` | 03/10 19:47 | Claude | conversations : textes EN et FR de la carte de message |
| `cc4e2b2` | 03/10 19:49 | Claude | priorités : couleur d'en-tête de carte, contraste mesuré |
| `0c2c88e` | 03/10 19:51 | Claude | conversations : carte de message à barre latérale de 4 dp |
| `2512690` | 03/10 19:54 | Claude | conversations : ligne d'en-tête de la carte |
| `cba41e6` | 03/10 19:56 | Claude | conversations : ligne d'état « Reçu · il y a 2 min » |
| `6d07037` | 03/10 19:57 | Claude | conversations : aperçus Compose clair et sombre de la carte |
| `d2fba5e` | 03/10 19:57 | Claude | docs : FORK.md décrit la branche conversation-maquette |
| `747503d` | 03/10 20:10 | Claude | aperçus : mise en forme acceptée par spotless |
| `1aed6aa` | 03/10 21:00 | Claude | ci : fork-apk s'arrête au bout de 30 minutes |

### `feat/mode-demo` (section 9), 28 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `d2cc8b3` | 04/10 05:54 | Claude | mode démo : interface DemoMode et qualificatif SCREEN_DATA |
| `13307a6` | 04/10 05:55 | Claude | mode démo : module core:demo |
| `ee105b2` | 04/10 05:58 | Claude | mode démo : jeu de données fictives |
| `1f814db` | 04/10 05:58 | Claude | ci : tests de :core:demo dans fork-apk |
| `a0a8bac` | 04/10 06:00 | Claude | mode démo : réglage en mémoire et compteur de messages réels |
| `3c68dc5` | 04/10 06:06 | Claude | mode démo : dépôts de démo en mémoire |
| `12fa1fc` | 04/10 06:08 | Claude | mode démo : envoi simulé, rien ne part vers la radio |
| `d7b3d43` | 04/10 06:14 | Claude | mode démo : façades d'écran et liaisons Koin |
| `a72e947` | 04/10 06:15 | Claude | mode démo : module core:demo dans les graphes Koin des applications |
| `093543b` | 04/10 06:16 | Claude | mode démo : textes EN et FR |
| `b97a27c` | 04/10 06:16 | Claude | tests : FakeDemoMode dans core:testing |
| `ba4af9b` | 04/10 06:18 | Claude | mode démo : la messagerie lit ses données par les façades |
| `94548db` | 04/10 06:19 | Claude | mode démo : vue PC, Équipes, Réglages et badge des non-lus par les façades |
| `e418e08` | 04/10 06:23 | Claude | mode démo : écran « Indisponible en mode démo » et liste blanche des routes |
| `e93f60e` | 04/10 06:25 | Claude | mode démo : import manquant dans le test de l'envoi simulé |
| `0751ed6` | 04/10 07:03 | Claude | ci : fork-apk affiche chaque test au démarrage |
| `ebc1916` | 04/10 07:06 | Claude | ci : tests de :core:demo dans une étape à part |
| `edc036e` | 04/10 07:06 | Claude | ci : une nouvelle poussée annule le run en cours de la même branche |
| `463e591` | 04/10 07:23 | Claude | Main : verrouillage de la radio extrait dans LockdownHost |
| `222252f` | 04/10 07:23 | Claude | façades : opt-in explicite pour l'héritage de StateFlow |
| `3c1bbb3` | 04/10 07:26 | Claude | mode démo : bandeau permanent, compteur de messages réels, bouton Quitter |
| `1045936` | 04/10 09:57 | Claude | mode démo : interrupteur dans Réglages, réglages directs grisés |
| `3fefb59` | 04/10 10:01 | Claude | mode démo : un lien réel (notification, partage) fait quitter la démo |
| `029aac1` | 04/10 10:01 | Claude | mode démo : quitter l'écran de l'application met fin à la démo |
| `42d0e6c` | 04/10 10:02 | Claude | FORK.md : section 9, mode démo |
| `1fcd183` | 04/10 10:13 | Claude | mode démo : formes stables pour Spotless, signature de handleDeepLink rétablie |
| `097713c` | 04/10 10:32 | Claude | mode démo : un lien invalide laisse la démo active |
| `5ad6d78` | 04/10 10:33 | Claude | test : un lien invalide passé à handleDeepLink appelle onInvalid |

### `feat/theme` (section 10), 8 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `8bfbf9c` | 06/10 18:36 | Claude | thème : les couleurs d'état suivent le thème de l'application |
| `b6839c3` | 06/10 18:39 | Claude | thème : teintes NetworkTact propres à chaque thème (TactColors) |
| `b579cd3` | 06/10 18:42 | Claude | messagerie : barre de priorité et texte secondaire lisibles dans les deux thèmes |
| `145907a` | 06/10 18:43 | Claude | thème clair : texte secondaire assombri pour tenir 4,5:1 sur les cartes |
| `b6b6fef` | 06/10 18:44 | Claude | vue PC : plus de rouge pour la fraîcheur, vert « fraîche » lisible |
| `9da61a1` | 06/10 18:46 | Claude | puce de nœud : palette fixe sans rouge ni ambre |
| `238e82e` | 06/10 18:47 | Claude | vue PC : aperçu clair / sombre des états de fraîcheur |
| `85091c4` | 06/10 18:47 | Claude | FORK.md : thème clair / sombre (feat/theme) |

### `feat/navigation` (section 11), 8 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `028f2e1` | 06/10 19:35 | Claude | barre de navigation : textes EN et FR |
| `e5dd22a` | 06/10 19:35 | Claude | barre de navigation : icônes vectorielles NetworkTact |
| `82c998a` | 06/10 19:39 | Claude | barre de navigation : icônes et libellés NetworkTact |
| `ea1b873` | 06/10 19:40 | Claude | barre de navigation : état de ma radio sans rouge ni ambre |
| `526e157` | 06/10 19:41 | Claude | barre de navigation : libellés sous les icônes en portrait |
| `27eaa6e` | 06/10 19:41 | Claude | barre de navigation : aperçus clair / sombre |
| `71ee741` | 06/10 19:42 | Claude | FORK.md : barre de navigation (feat/navigation) |
| `2903fd9` | 06/10 20:25 | Claude | CI : limiter la mémoire de Gradle et Kotlin dans fork-apk |

### `feat/fait-observe-carte` (section 12), 11 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `d7a55f7` | 06/10 21:06 | Claude | fait observé : heure d'observation dans le message |
| `a1604ab` | 06/10 21:08 | Claude | fait observé : champ « Observé à » dans le formulaire |
| `7527bf7` | 06/10 21:09 | Claude | mode démo : fait observé fictif au nouveau format |
| `da6cf80` | 06/10 21:10 | Claude | coordonnées : repérer une position complète dans un message |
| `ac87748` | 06/10 21:13 | Claude | carte : point de repère partagé pour un message envoyé |
| `be7c296` | 06/10 21:18 | Claude | messagerie : encart « Partager ce point sur la carte » après l'envoi |
| `e0501b5` | 06/10 21:23 | Claude | carte : centrer la carte sur un point demandé par un autre écran |
| `a042b2c` | 06/10 21:26 | Claude | messagerie : bouton « Voir sur la carte » sur un message reçu |
| `4557c62` | 06/10 21:28 | Claude | carte : émetteur et heure de réception dans la fenêtre d'un point |
| `9acbabd` | 06/10 21:28 | Claude | CI : tests de feature:map dans fork-apk, en étape séparée |
| `c00a580` | 06/10 21:28 | Claude | FORK.md : fait observé avec heure et points partagés (feat/fait-observe-carte) |

### `feat/carte-hors-ligne` (section 13, 14), 27 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `ea203dc` | 08/10 07:38 | Claude | carte hors ligne : style Protomaps, polices et sprites embarqués (fdroid) |
| `6274a27` | 08/10 07:41 | Claude | carte : fond vectoriel local (Basemap.LocalVector) |
| `7760c10` | 08/10 07:44 | Claude | carte hors ligne : contrôle des fichiers .pmtiles et style local |
| `e31e7b7` | 08/10 07:47 | Claude | carte hors ligne : écran « Cartes sur le téléphone » |
| `1b3f2b3` | 08/10 07:52 | Claude | carte : attendre que l'hôte ait listé ses fonds avant d'ouvrir la carte |
| `e07428d` | 08/10 07:53 | Claude | carte hors ligne : import, liste et affichage des cartes .pmtiles (fdroid) |
| `225dd72` | 08/10 07:54 | Claude | carte : mention de la source pour les fichiers MBTiles importés |
| `e57fb49` | 08/10 07:54 | Claude | licences : données OpenStreetMap (ODbL), polices et style des cartes hors ligne |
| `cc39d33` | 08/10 07:57 | Claude | documentation : produire et installer une carte hors ligne (.pmtiles) |
| `0f7ff5b` | 08/10 07:57 | Claude | FORK.md : carte hors ligne, lot 1 (feat/carte-hors-ligne) |
| `f285f9d` | 08/10 12:42 | Claude | carte : une carte locale importée devient le fond par défaut |
| `fada048` | 08/10 12:44 | Claude | carte : polices de l'APK pour les libellés sur un fichier MBTiles |
| `c91fca7` | 08/10 12:46 | Claude | coordonnées : heure de relevé d'un point lu dans un message |
| `8fa1fb4` | 08/10 12:49 | Claude | carte : marqueur sur le point demandé par « Voir sur la carte » |
| `8b2b84e` | 08/10 12:54 | Claude | carte : état ANCIENNE sur la puce d'un nœud, heure de relevé au toucher |
| `959feef` | 08/10 12:55 | Claude | carte : données des écrans (SCREEN_DATA) et aucun envoi en mode démo |
| `c4914cd` | 08/10 12:58 | Claude | mode démo : carte en liste blanche (variante fdroid), rien de réel ni d'envoyé |
| `b1fb8ad` | 08/10 12:58 | Claude | CI : tests de feature:map-maplibre dans fork-apk, en dernière étape |
| `56c09cf` | 08/10 12:58 | Claude | FORK.md : carte hors ligne, lot 2 (feat/carte-hors-ligne) |
| `7a68776` | 08/10 13:11 | Claude | carte et messagerie : rétablir deux imports manquants |
| `9aa4bf6` | 10/10 14:10 | Claude | carte : nodesToFeatureCollection interne au module |
| `5d1d518` | 08/10 13:23 | Claude | CI : fork-apk ne lance plus que les tests Android et JVM |
| `36574bc` | 08/10 13:23 | Claude | CI : workflow ios-check, compilation iOS à la main et chaque semaine |
| `b4bf8fb` | 10/10 15:08 | Claude | carte : WaypointInfoSlotTest suit le paramètre canSend |
| `507db5b` | 10/10 15:29 | Claude | FORK.md : règle CI Android uniquement et workflow ios-check |
| `7d80a33` | 10/10 17:26 | Claude | CI : démon Gradle neuf à chaque étape, 15 min par étape, sans core:navigation |
| `557c916` | 10/10 18:18 | Claude | CI : limite de durée sur chaque étape Gradle, diagnostic aussi après annulation |

### `feat/carte-coordonnees` (section 15), 16 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `7be7736` | 10/10 19:03 | Claude | Position du téléphone : source déplacée dans core:repository, précision Android |
| `86dbd8e` | 10/10 19:07 | Claude | Coordonnée d'un point de la carte : MGRS, UTM, DMS et texte à insérer |
| `806cf2f` | 10/10 19:08 | Claude | Comparaison radio / téléphone : écart en mètres et qualité du relevé |
| `8ce7b2c` | 10/10 19:10 | Claude | Diffusion de la position de la radio : règles de refus, d'ancienneté et d'attente |
| `fa29831` | 10/10 19:11 | Claude | Convertisseur : ouverture avec une coordonnée de départ |
| `164af46` | 10/10 19:13 | Claude | Mode démo : position fictive du téléphone et qualité du relevé de PC-0 |
| `1abcd7b` | 10/10 19:13 | Claude | Mode démo : choix de la conversation permis pour insérer un texte |
| `8758dbe` | 10/10 19:15 | Claude | Carte : choix mémorisé « Centrer sur : radio / téléphone » |
| `b6aea24` | 10/10 19:19 | Claude | Carte : coordonnée du point à l'appui long (MGRS, UTM, DMS) |
| `fe3601b` | 10/10 19:23 | Claude | Recalage : comparaison radio / téléphone et diffusion manuelle (logique) |
| `8d90911` | 10/10 19:26 | Claude | Carte : bouton « Recaler », positions radio / téléphone et diffusion |
| `85e8852` | 10/10 19:27 | Claude | FORK.md : coordonnée au toucher et recalage GPS (section 15) |
| `56fdc67` | 10/10 19:47 | Claude | Carte « Recaler » : règles Compose de detekt (run 51) |
| `8e0e4cd` | 10/10 20:06 | Claude | Carte « Recaler » : import de DETAIL_ZOOM (compilation, run 52) |
| `ff7c4ed` | 10/10 20:11 | Claude | CI : compilation Kotlin de l'APK fdroid juste après le Lint |
| `20d927a` | 10/10 20:11 | Claude | FORK.md : étape de compilation rapide de fork-apk (section 15) |

### `feat/detachement` (section 16), 21 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `db58f4f` | 11/10 09:53 | Claude | À propos : lien du code source vers GRTGRATzq/NetworkTact |
| `e5cf52c` | 11/10 09:56 | Claude | Écran Wi-Fi : symbole NetworkTact à la place du logo M-PWRD |
| `32eda0b` | 11/10 09:56 | Claude | Suppression de ic_meshtastic.xml et ic_launcher-114x114.png |
| `bd8e1b9` | 11/10 09:57 | Claude | Icône de boutique 512x512 refaite à partir du symbole NetworkTact |
| `1605fa7` | 11/10 09:59 | Claude | Connexion : plus d'encart ni de notification « mise à jour disponible » |
| `ac4c8ec` | 11/10 09:59 | Claude | Mise à jour du firmware : titre neutre « Mise à jour du firmware radio » |
| `e251ff4` | 11/10 10:00 | Claude | À propos : mention « Basé sur Meshtastic-Android », liens en ligne retirés |
| `9c05637` | 11/10 10:01 | Claude | Réglages : « Aide et documentation » et « Liens vers les appareils » masqués |
| `8fee1e3` | 11/10 10:02 | Claude | Carte : planificateur de site retiré (service en ligne Meshtastic) |
| `7370aa6` | 11/10 10:03 | Claude | Fiche d'un nœud : section « J'en veux un » masquée |
| `e6bfd01` | 11/10 10:04 | Claude | Textes : liens vers meshtastic.org et flasher.meshtastic.org retirés |
| `35e57a6` | 11/10 10:04 | Claude | Textes : « Meshtastic » remplacé par des formulations neutres |
| `876f0ec` | 11/10 10:05 | Claude | Couleurs d'état : le vert « bon » devient le vert NetworkTact |
| `1af7131` | 11/10 10:08 | Claude | Couleurs d'état : signal LoRa, Bluetooth et batterie sans rouge ni ambre |
| `6bbe9fb` | 11/10 10:10 | Claude | Couleurs d'état : sécurité sans rouge ni jaune |
| `dda11af` | 11/10 10:13 | Claude | Couleurs d'état : SNR des traceroutes et des voisins sans rouge ni ambre |
| `0e1cb81` | 11/10 10:14 | Claude | Couleurs d'état : favori, nœud non entendu, boussole, versions en neutre |
| `02e4fb6` | 11/10 10:16 | Claude | Menu d'un nœud : Ignorer et Supprimer en neutre |
| `715de8f` | 11/10 10:17 | Claude | Fiche d'un nœud : Supprimer en neutre |
| `01d018d` | 11/10 10:18 | Claude | FORK.md : détachement de Meshtastic (section 16) |
| `9830cac` | 11/10 10:41 | Claude | Tests : AboutScreenTest suit le nouvel écran À propos |

### `feat/organigramme` (section 17), 13 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `32641a2` | 11/10 11:31 | Claude | Organigramme : modèle et codage QR |
| `ed9a803` | 11/10 11:33 | Claude | Organigramme : rattachement des membres aux radios par indicatif |
| `11a34e7` | 11/10 11:34 | Claude | Organigramme : modifications du PC, refus au-delà d'un QR lisible |
| `f95c6b1` | 11/10 11:36 | Claude | Organigramme : stockage local, avec l'organigramme précédent |
| `c4b8dee` | 11/10 11:38 | Claude | Mode démo : organigramme fictif derrière une façade SCREEN_DATA |
| `dedf28b` | 11/10 11:40 | Claude | Organigramme : textes EN et FR |
| `fbf80bf` | 11/10 11:41 | Claude | Organigramme : route NodesRoute.OrgChart, permise en mode démo |
| `6c7bb82` | 11/10 11:41 | Claude | QR : niveau de correction d'erreur au choix, HIGH par défaut |
| `f334e4e` | 11/10 11:45 | Claude | Organigramme : écran en lecture avec l'état réel des radios |
| `2d1d130` | 11/10 11:48 | Claude | Organigramme : édition en mode PC |
| `db47a26` | 11/10 11:51 | Claude | Organigramme : partage par QR, scan et confirmation d'adoption |
| `3c95481` | 11/10 15:14 | Claude | Organigramme : icône et accès depuis l'onglet Réseau |
| `8bd539a` | 11/10 15:14 | Claude | FORK.md : organigramme (section 17) |

### `feat/ci-gel` (section 18), 2 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `9e14cd3` | 11/10 16:30 | Claude | CI : fork-apk sans Isolated Projects ni cache de configuration |
| `32e9e27` | 11/10 19:03 | Claude | FORK.md : gels de Gradle en configuration dans la CI (section 18) |

### `feat/autodiagnostic` (section 19), 18 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `da59c55` | 11/10 19:47 | Claude | Autodiagnostic : calcul du diagnostic local et du bandeau |
| `fb4d1c7` | 11/10 19:48 | Claude | Autodiagnostic : heure du dernier paquet reçu de ma radio |
| `690f381` | 11/10 19:49 | Claude | Autodiagnostic : stockage local du réglage LoRa attendu |
| `e351cf5` | 11/10 19:51 | Claude | Autodiagnostic : textes EN et FR |
| `804f673` | 11/10 19:54 | Claude | Mode démo : données du diagnostic de liaison derrière des façades |
| `beb423e` | 11/10 19:57 | Claude | Autodiagnostic : ViewModels du diagnostic et de « Tester la liaison » |
| `dd9de90` | 11/10 19:57 | Claude | Autodiagnostic : textes de la demande de position retirés |
| `5143cda` | 11/10 19:58 | Claude | Autodiagnostic : route NodesRoute.Diagnostic, permise en mode démo |
| `159109a` | 11/10 19:59 | Claude | core:ui : format d'un temps écoulé dans les mots de la vue PC |
| `402d24a` | 11/10 20:00 | Claude | Autodiagnostic : signe pour cent de la batterie écrit comme ailleurs |
| `5399ed4` | 11/10 20:03 | Claude | Autodiagnostic : écran du diagnostic de liaison |
| `735586d` | 11/10 20:03 | Claude | Autodiagnostic : bouton « Tester la liaison » |
| `30d3b68` | 11/10 20:05 | Claude | Autodiagnostic : lien « Diagnostic » sous un envoi en échec |
| `ec442f1` | 11/10 20:08 | Claude | Autodiagnostic : bandeau « Radio non connectée » / « Aucun nœud entendu » |
| `1ae45ae` | 11/10 20:08 | Claude | Autodiagnostic : accès depuis l'onglet Réseau |
| `d339207` | 11/10 20:09 | Claude | FORK.md : autodiagnostic quand aucun relais n'est disponible (section 19) |
| `d05a6c9` | 11/10 20:21 | Claude | Autodiagnostic : barre du haut de l'écran sans action (compilation, run 67) |
| `4be2aeb` | 11/10 20:31 | Claude | Autodiagnostic : attente avant un nouveau test arrondie juste (run 68) |

### `feat/accueil-insignes` (section 20), 5 commits

| Commit | Date | Auteur | Message |
|---|---|---|---|
| `70f84fd` | 12/10 00:12 | GRTGRATzq | Insignes des deux batteries pour l'écran d'accueil |
| `f8bca61` | 11/10 21:17 | Claude | Insignes des deux batteries déplacés dans les ressources partagées |
| `48d964f` | 11/10 21:17 | Claude | Écran de bienvenue : textes EN et FR des insignes des deux batteries |
| `411d583` | 11/10 21:22 | Claude | Écran de bienvenue : insignes des deux batteries |
| `9196e76` | 11/10 21:23 | Claude | FORK.md : écran de bienvenue, insignes des deux batteries |

## 5. Hors dépôt

- 18/10/2026 : PR n°7666 ouverte par erreur vers `meshtastic/Meshtastic-Android` (5 commits, CLA non signé), fermée ensuite par l'utilisateur. Rien n'a été fusionné.
- Les sessions intitulées « Réinitialisation mdp supabase », « Module audit et fusion », « Correction complète du site par module », « Conversion Word vers texte » et « Audit contenu modules » ne concernent pas ce dépôt d'après leur titre ; je ne les ai pas lues.
