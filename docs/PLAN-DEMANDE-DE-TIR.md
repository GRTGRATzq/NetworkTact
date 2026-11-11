# Plan : demande de tir (tâche 8, `feat/demande-de-tir`)

Branche partie de `feat/carte-telechargement` (`801af88`). Spécification : `docs/SPEC-DEMANDE-DE-TIR.md` (validée le
11/11/2026), qui fait foi ; ce plan dit comment la réaliser. **Aucun code n'est écrit avant la validation des décisions du
§ 12.** Futurs documents : `FORK.md` § 30 (et état d'avancement), `TESTS-A-FAIRE.md` § 18.

## 1. Objectif et contraintes

- Reproduire la fiche « Demande de tir » (68e RAA, EOC, février 2024), case par case, sans rien inventer, et l'envoyer
  d'un seul geste en message direct, avec accusé, découpée en parties de 200 octets UTF-8 au plus.
- **Texte ordinaire uniquement** : aucun nouveau type de paquet, aucun champ protobuf, aucun port. Les parties partent par
  la fonction d'envoi existante (`SendMessageUseCase`), comme un message tapé. Un client Meshtastic ordinaire lit les
  parties comme du texte.
- Côté réception, l'application reconnaît les parties, les regroupe et affiche la fiche reconstituée ; les messages bruts
  restent stockés tels quels (rien n'est réécrit en base).
- La v1 ne doit contenir **aucun libellé ni abréviation absent de la fiche ou de la spécification**. Partout où la
  spécification ne fixe pas l'écriture exacte d'une case, le plan propose une écriture et la soumet à décision (§ 12) au
  lieu de la choisir en silence.

## 2. Ce qui existe (lu dans le code)

| Besoin | Existant réutilisé | Où |
|---|---|---|
| Boutons au-dessus de la zone de saisie | `FieldShortcuts` / `FieldShortcutsRow` (« Ma position », « Fait observé », « Convertisseur ») : puces `AssistChip`, aucune n'envoie, elles remplissent la zone de saisie | `feature/messaging/.../coordinates/FieldShortcuts.kt` |
| Formulaire modèle | `ObservedFactDialog` + `observedFactDraft` (calcul pur, taille en octets, `tooLong`, estimation tant que le lieu ne se lit pas, `ImeAction.Done`) | `coordinates/ObservedFactDialog.kt`, `ObservedFactDraft.kt` |
| Taille en octets | `String.utf8Size()` ; plafond `PositionMessage.MAX_MESSAGE_BYTES` = 200 | `core/model/.../HamName.kt`, `geo/PositionMessage.kt` |
| Coordonnées | `CoordinateParser` (MGRS, UTM, DMS validés), `FormattedCoordinates`, `MessageCoordinate.find` (MGRS `\d{5} ?\d{5}`, UTM, DMS ; jamais un numéro, une heure ou une date) | `core/model/.../geo/` |
| Coordonnée à copier | Appui long sur la carte : « Copier » donne `Point carte · MGRS … · UTM … · DMS …` ; le convertisseur copie aussi | `MapPointCoordinate`, `CoordinateConverterScreen` |
| Voir sur la carte | `MapFocusPoint` + `MessageListHandlers.onShowOnMap` (affichage local, rien n'est émis) ; « Carte indisponible en mode démo » | `MessageItem.ShowOnMapButton`, `core/ui` |
| Envoi | `SendMessageUseCase(text, contactKey, replyId): Int` : enregistre la ligne `QUEUED`, met en file durable (`MessageQueue`), partage la clé du destinataire (PKI) ; homoglyphes : cyrillique seulement, sans effet sur nos textes | `core/repository/.../usecase/SendMessageUseCase.kt` |
| Clé de conversation directe | `"${8 si PKC sinon node.channel}${node.user.id}"` | `NodeDetailViewModel` l. 265, `CommandPostViewModel` l. 108 |
| États d'envoi et accusés | `SentStatus` (en file, remis à la radio, relayé, **accusé par le destinataire** = `RECEIVED` sur un envoi direct, preuve d'accusé, échec avec raison) | `feature/messaging/.../status/SentStatus.kt`, `SentStatusUi.kt` |
| Renvoi d'un message | « Renvoyer » = supprime la ligne puis renvoie le même texte (`MessageListPaged`, l. 170) | `MessageListPaged.kt` |
| Liste des messages | Paging (`LazyPagingItems`), `pagingData.filter` déjà utilisé pour les filtres de fil (`MessageViewModel.filteredPagedMessages`) ; regroupement visuel `isSameGroup` (même expéditeur, 10 min) | `MessageViewModel.kt`, `MessageListPaged.kt` |
| Priorités | `MessagePriority` : `[URG]`, `[CR]`, sinon info ; teintes d'en-tête par priorité | `priority/MessagePriority.kt`, `component/MessagePriorityUi.kt` |
| Préférences | Modèle `OrgChartPrefs` : interface dans `core/repository`, `OrgChartPrefsImpl` dans `UiDataStore` (une valeur JSON, `edit{}` transactionnel), `DemoOrgChartPrefs` + `ScreenOrgChartPrefs` sous `@Named(SCREEN_DATA)` | `core/prefs/.../orgchart/`, `core/demo/.../repository`, `screen/` |
| Mode démo | `DemoRoutes.isAvailable` (liste fermée), `DemoSendMessageUseCase` (en file → remis 1,5 s → relayé 3 s → accusé 5 s, échec 8 s pour CHARLIE-3, rien ne part), `DemoDataSet.history` (messages `Received`/`Sent`), `ScreenFacadesTest` | `core/demo/` |
| CI | `fork-apk` lance `core:model`, `core:prefs`, `feature:messaging`, `core:demo` ; une poussée de Markdown seul ne lance rien | `.github/workflows/fork-apk.yml` |

Maquette validée (4 écrans) : `docs/maquette-demande-de-tir.pdf` (export du 11/11/2026). Elle fixe la disposition :
puce « DT Demande de tir » sur sa propre ligne sous « Ma position · Fait observé · Convertisseur » ; formulaire
« Brouillon gardé sur le téléphone » avec « Renouveler » en haut ; aperçu « Message k / N · X octets sur 200 » ; fiche
reconstituée (bandeau rose, badge DT bordeaux, « 3 / 3 reçus », boutons GDH, « Voir les 3 messages bruts ») ; fiche
incomplète (« Partie 3/3 manquante (cases 9 à 11) », « Dernière partie reçue il y a 40 s ») ; carte émetteur
(« 1/3 Accusé par CDS TIR », « GDH attribution reçu : … », « GDH fin : en attente »). Les textes d'interface du § 5 sont
repris de la maquette.

## 3. Architecture

Tout ce qui est format, découpage, lecture et regroupement est du **Kotlin commun pur**, sans Android, testé en
`commonTest` ; l'interface ne fait qu'afficher.

```
core/model/.../firerequest/          (pur, commonMain)
  FireRequest            modèle de la fiche : en-tête + 11 cases (types fermés, valeurs saisies en texte)
  FireRequestSheet       libellés, abréviations et codes de la fiche (constantes, une seule source)
  FireRequestText        case -> ligne(s) « <n> <contenu> » ; ligne -> case (lecture)
  FireRequestSplitter    lignes -> parties [DT <n°> k/N] de 200 octets au plus
  FireRequestPart        lecture d'une partie reçue ; lecture d'une réponse GDH
  FireRequestAssembly    regroupement par instance, doublons, manquantes, réponses GDH
  Gdh                    GDH : heure du téléphone -> « 112250 loc NOV 26 », lecture, mois
  LruCoordinate          « 35T LL 33000 - 90300 » -> point (via CoordinateParser, MGRS)
core/repository   FireRequestPrefs (interface)
core/prefs        FireRequestPrefsImpl (UiDataStore, JSON)
core/demo         DemoFireRequestPrefs, ScreenFireRequestPrefs (@Named(SCREEN_DATA)),
                  demande fictive reçue, réponse GDH simulée
core/navigation   ContactsRoute.FireRequest(contactKey)
feature/messaging/.../firerequest/
  FireRequestViewModel   brouillon, aperçu, envoi, Renouveler
  FireRequestScreen      formulaire (en-tête + 11 cases), aperçu des parties
  FireRequestCard        carte émetteur « DT <n°> · N messages » et fiche reconstituée (récepteur)
  FireRequestRawSheet    « Voir les messages bruts »
  GdhReplyDialog         envoi du GDH attribution / fin
```

### 3.1 Modèle de la demande (`FireRequest`)

- En-tête : `origin` (texte), `recipientLabel` (texte écrit dans le message, voir D6), destinataire (nœud, hors modèle
  texte), `requestGdh` (`Gdh`).
- Une classe par case, chaque option de la fiche représentée par un type fermé (`enum` ou booléen) dont le libellé vient
  de `FireRequestSheet`, et chaque « [ ] » par un champ texte court. Exemple, case 5 : `launcher: Code?` (1 à 4),
  `trajectory: Pair<Code?, Code?>` (1 à 3), `mode: Pair<ModeValue?, ModeValue?>` (code 1 à 4 + valeur ms / m).
- Une case est **vide** quand rien n'y est saisi : elle s'écrit `<n> non rempli`. Aucune case n'est obligatoire.
- Le modèle est `@Serializable` (brouillon et dernière demande envoyée).

### 3.2 Codage du message (`FireRequestText`)

- Une ligne par case : `<n> <éléments séparés par « · »>` (U+00B7, 2 octets). Les libellés et codes sont ceux de la fiche,
  écrits comme sur le papier : `5 Lanceurs 2 · Trajectoire 1 · Mode 1`. Les 11 cases sont toujours présentes, dans
  l'ordre.
- En-tête (première partie seulement) : `Origine OA · Destinataire CDS TIR · GDH demande 112250 loc NOV 26`.
- Séparateur de lignes : `\n` (1 octet). L'exemple de la spécification, recompté : 166 + 154 + 168 = **488 octets**, avec
  `\n` entre les lignes et rien en fin de partie ; le découpage glouton du § 3.3 redonne exactement ces 3 parties.
- Écritures validées le 11/11/2026 (cases 2, 3, 7, 9 et exemple de la spécification) :

| Case | Règle | Exemple |
|---|---|---|
| 2 | lettres cochées d'abord, séparées par une espace, puis les types | `2 A G Emblée · Mettre en place 2 · Préparer` |
| 3 | `Éclairement 2` ; Panaché : code, et pour 4 la lettre collée | `3 Neutralisation · Panaché 4A et 2` |
| 7 | **une ligne de texte par ligne de la fiche**, chacune commençant par `7` puis la lettre de ligne ; TLE en dernier élément | `7 A G 1600 · D 2500 m · S 20 m` / `7 D X 34550 · Y 88800 · Z 640 m` / `7 Rep R1 R2 · Plus à l'est 200 m · Plus près 100 m` / `7 G 35T LL 33000 - 90300 · Z 610 m · TLE 2` |
| 9 | comme l'exemple (Z sans « m », comme l'exemple) | `9 Grille · Gisement d'observ. 4800 · Observateur X 34550 Y 88800 Z 640` |

- **Cases 4, 5, 6, 8, 10 et 11 : tableau D26, validé case par case le 11/11/2026**, règles communes comprises. Règles
  communes : un élément dont tous les champs sont vides n'est pas écrit ; une case sans aucun élément s'écrit
  `<n> non rempli` ; gisements écrits sans unité dans le message, suivis de « m̶ » (millièmes, m barré) à l'affichage,
  comme la maquette ; à l'affichage, un élément par ligne, sauf quand la maquette les met sur une ligne (cases 8 et 10).

| Case | Message envoyé (octets) | Affichage en clair chez le destinataire |
|---|---|---|
| 4 | `4 Nb de cps 2 / 6 · Durée 1 / 3 min · Intervalle entre les coups 10 / 20 s` (77) | Nb de cps 2 / 6 ⏎ Durée 1 / 3 min ⏎ Intervalle entre les coups 10 / 20 s |
| 4 | `4 Nb de cps / 6 · Intervalle entre les coups 10 / s` (52) : moitié vide laissée vide, Durée absente | Nb de cps / 6 ⏎ Intervalle entre les coups 10 / s |
| 4 | `4 non rempli` | non rempli (gris) |
| 5 | `5 Lanceurs 2 · Trajectoire 1 / 3 · Mode 2 et 3 · Court retard 30 ms · Fusant 10 m` (85) : valeurs dans les champs « Court retard … ms », « Fusant … m », « Retard … m » de la maquette | Lanceurs 2 : Mo 120 ⏎ Trajectoire 1 : Plongeante / 3 : Nominale ⏎ Mode 2 : Percutant court retard 30 ms et 3 : Fusant 10 m |
| 5 | `5 Lanceurs 1 · Trajectoire 1 · Mode 1` | Lanceurs 1 : Mo 81 mm ⏎ Trajectoire 1 : Plongeante ⏎ Mode 1 : Percutant instantané |
| 5 | `5 Lanceurs 4 · Mode 4 · Retard 20 m` (37) | Lanceurs 4 : LRU ⏎ Mode 4 : Retard 20 m |
| 6 | `6 Pick-up en mouvement · Personnel abrité · Artillerie pièces` (65) | Pick-up en mouvement ⏎ Personnel abrité ⏎ Artillerie pièces |
| 6 | `6 Chars · Bâtiment` (20) | Chars ⏎ Bâtiment |
| 6 | `6 Blindés légers · Pont` (26) : « Bâtiment ou [ ] », la case libre remplace « Bâtiment » | Blindés légers ⏎ Pont |
| 8 | `8 Convergent` (12) | Convergent |
| 8 | `8 Linéaire · Point central P1 · LG 200 · Gisement 1600 · Front 400 m · Profondeur 100 m · Ouvert` (103) | Linéaire · Point central P1 · LG 200 ⏎ Gisement 1600 m̶ · Front 400 m · Profondeur 100 m · Ouvert |
| 8 | `8 Circulaire · Centre P2 · Rayon 100 m` (40) | Circulaire · Centre P2 · Rayon 100 m |
| 10 | `10 Distance 800 m · Gisement 1600 · Niveau de protection 1` (exemple validé) | Distance 800 m · Gisement 1600 m̶ ⏎ Niveau de protection 1 : à découvert |
| 10 | `10 Distance 1200 m · Gisement N · Niveau de protection 3` (58) | Distance 1200 m · Gisement N ⏎ Niveau de protection 3 : sous blindage |
| 11 | `11 Aussitôt prêt` (exemple validé) | Aussitôt prêt |
| 11 | `11 Aussitôt prêt limité NLT 14H30` (36) | Aussitôt prêt limité NLT 14H30 |
| 11 | `11 HSO limité début 14H30 fin 14H45` (37) | HSO limité début 14H30 fin 14H45 |
| 11 | `11 Synchro S1` · `11 Heure sur objectif 14H30` · `11 Heure de tir 14H25` · `11 Au commandement de OA` · `11 À mon commandement` | identiques |

  Réponses du 11/11/2026 :
  - case 6 : « en mouvement / à l'arrêt » vaut pour **Chars, Blindés légers et Pick-up** (l'accolade de la fiche couvre
    les trois lignes) : `6 Chars à l'arrêt · Blindés légers en mouvement · Pick-up` (l'état reste facultatif) ;
  - case 8 : LG et LD **sans unité**, comme sur la fiche ;
  - case 11 : **sélection multiple** en v1, éléments séparés par « · » : `11 Aussitôt prêt · Heure de tir 14H25`
    (les restrictions viendront avec le verrouillage des cases) ;
  - heures `..H..` écrites **telles que saisies, sans fuseau** (`14H30`), comme sur la fiche.

Pourquoi une ligne par ligne de la fiche en case 7 : dans `A G 1600 · D 2500 m`, « D » est la distance, mais D est aussi
une lettre de ligne (RECTANG. D) ; et « G » est à la fois le gisement polaire et la ligne L R U G. Écrire chaque ligne de
la fiche sur sa propre ligne de texte, préfixée `7 <lettre>`, rend la lecture sans ambiguïté et reste conforme à
l'exemple (une seule ligne G).

### 3.3 Découpage en parties (`FireRequestSplitter`)

- Entrée : la ligne d'en-tête et les lignes de cases. Sortie : `N` parties, chacune `[DT <n°> k/N]\n` + lignes.
- Glouton : on ajoute les lignes tant que la partie reste ≤ 200 octets ; sinon on ouvre une partie. Coupure **entre deux
  lignes**, jamais au milieu.
- Une case seule trop longue est coupée **à un « · »** ; la suite part dans la partie suivante sur une ligne qui
  **recommence par le numéro de la case** (`7 Z 610 m · TLE 2`), sans mot ajouté (D9). À la lecture, deux lignes de même
  numéro consécutives se recollent par « · » (sauf case 7, où chaque ligne de fiche est déjà une ligne).
- `N` dépend de la taille de l'en-tête de partie (`1/9` contre `1/10`) : calcul en deux passes ; `N` limité à 9 (D10),
  ce qui borne aussi la saisie.
- Un élément seul qui ne tiendrait pas dans une partie vide est impossible : les champs libres ont une longueur maximale
  (D11), vérifiée par test sur le pire cas (toutes les cases remplies au maximum).

### 3.4 Lecture des parties reçues (`FireRequestPart`)

- Une partie : texte qui **commence** par `[DT <n°> <k>/<N>]` puis un saut de ligne, avec `n°` = `[A-Z0-9]{1,10}` (D3),
  `1 ≤ k ≤ N ≤ 9`. Tout autre texte reste un message ordinaire (pas de faux positif sur `[DT` au milieu d'un texte).
- Réponse GDH : texte exactement `[DT <n°>] GDH attribution <gdh>` ou `[DT <n°>] GDH fin <gdh>`.
- Lecture tolérante : une ligne de case qui ne se lit pas (autre version, saisie à la main) est gardée et **affichée
  telle quelle** ; les codes ne sont mis en clair que si l'élément correspond exactement à la fiche.
- Aller-retour garanti par test : pour toute demande `d`, `lire(découper(écrire(d))) == d`.

### 3.5 Regroupement (`FireRequestAssembly`)

- Clé : **(correspondant, n° de tir)**, le correspondant étant l'expéditeur pour une demande reçue, le destinataire pour
  une demande envoyée (une conversation directe n'a qu'un correspondant, donc la clé est en pratique le n° de tir dans
  la conversation).
- **Instance** : une même clé peut revenir (« Renouveler » garde le n°). Une partie ouvre une nouvelle instance quand
  son `N` diffère de l'instance en cours, ou quand son rang `k` y est déjà avec un **texte différent**, ou au-delà d'un
  délai depuis la dernière partie de l'instance (D12, proposé : 2 h).
- **Doublon** (même `k`, même texte, par exemple partie renvoyée alors que la première était arrivée sans accusé) : compté
  une fois ; les deux restent visibles dans « Voir les messages bruts ».
- **Désordre** : sans effet, les parties sont rangées par `k`.
- **Partie jamais reçue** : la fiche reste « k / N reçus », parties manquantes nommées (« Partie 2 manquante »), cases
  reçues lisibles ; les cases des parties manquantes affichées « non reçu » (distinct de « non rempli », D13). Aucun
  délai d'expiration : l'émetteur voit l'échec de la partie et peut la renvoyer seule.
- **Réponse GDH** : rattachée à l'instance la plus récente de même clé dans la même conversation, dont la première partie
  est antérieure à la réponse ; à défaut, à la plus récente. Sans aucune instance : affichée comme petite carte seule
  « DT <n°> · GDH attribution … ». Plusieurs GDH du même type : le plus récent s'affiche, les autres dans les messages bruts.
- Place dans la liste : la carte s'affiche **à la place du message le plus récent** de l'instance (elle descend quand
  une partie arrive) ; les autres lignes de l'instance sont masquées par `pagingData.filter` (D14). Les lignes restent en
  base, intactes.

### 3.6 Fiche reconstituée (récepteur) et carte émetteur

- **Récepteur** : badge DT, présentation propre, **jamais le rouge de [URG]** (teinte neutre). En-tête (Origine,
  Destinataire, GDH demande, GDH attribution, GDH fin), puis cases 1 à 11 ; codes en clair d'après les légendes de la
  fiche (`Lanceurs 2 : Mo 120`, `Trajectoire 1 : Plongeante`, `Niveau de protection 1 : à découvert`) ; `non rempli`
  en gris. TLE affiché sans légende (la spécification n'en donne pas). Boutons : « Voir sur la carte » (case 7),
  « Voir les messages bruts », « GDH attribution », « GDH fin ».
- **Émetteur** : une carte « DT <n°> · N messages » ; une ligne par partie avec son état (`SentStatus`, mêmes mots que
  les autres messages) ; une partie en échec a « Renvoyer » (chemin existant : supprimer puis renvoyer le même texte) ;
  GDH attribution et GDH fin reçus affichés sur la carte ; « Renouveler », « Voir les messages bruts ».

### 3.7 Voir sur la carte (case 7) sans faux positif

- Écriture (D15, validée) : `35T LL 33000 - 90300` = fuseau UTM `35`, bande de latitude `T`, carré de 100 km `LL`,
  X (abscisse, est-ouest) `33000`, Y (ordonnée, nord-sud) `90300`, **toujours 5 chiffres** (au mètre). Z (altitude ou
  élévation, facultatif, en mètres) est un élément à part : `· Z 610 m`.
- Formulaire, ligne L R U : champs Fuseau, Bande, Carré, X, Y, Z. **Chaque coordonnée est vérifiée par le convertisseur
  existant** (`CoordinateParser`, MGRS) dès la saisie et à la lecture : zone, bande et carré qui existent, X et Y à
  5 chiffres. Une ligne incomplète ou invalide est signalée sous la ligne (sans bloquer l'envoi, aucune case obligatoire).
- `LruCoordinate` lit **seulement** les lignes L R U (G et H) de la case 7 d'une demande lue : `^(\d{1,2})([C-X])
  ([A-Z]{2}) (\d{5}) - (\d{5})$`, remis en MGRS `35T LL 33000 90300` et validé par `CoordinateParser`. Une ligne
  invalide n'a pas de bouton « Voir sur la carte ».
- `MessageCoordinate` **n'est pas modifié** : le tiret ne fait pas partie de ses formats, donc aucun autre texte (un
  message ordinaire, `Observateur X 34550 Y 88800`, un X/Y de la ligne RECTANG.) n'est jamais pris pour une coordonnée.
  Test de non-régression : aucune partie de l'exemple n'est lue par `MessageCoordinate.find`.
- Deux lignes G et H remplies : deux boutons « Voir G sur la carte », « Voir H sur la carte » (D16). Mode démo : la carte
  suit la règle existante (« Carte indisponible en mode démo » sur la variante google).

### 3.8 GDH (`Gdh`)

- Forme (D17, validée) : `JJHHMM loc MOIS AA` et `JJHHMM fra MOIS AA` avec une espace (`112150 loc NOV 26`,
  `112150 fra NOV 26`) ; zoulou à l'usage militaire, Z collé : `112150Z NOV 26`.
- Conversion depuis l'heure du téléphone (kotlinx-datetime) : `loc` = fuseau du téléphone
  (`TimeZone.currentSystemDefault()`), `Z` = UTC, `fra` = `Europe/Paris` (heure légale française, été/hiver gérés par la
  base de fuseaux). Le jour, le mois et l'année sont ceux **du fuseau choisi** (à 00 h 30 heure française, le Z est
  encore la veille).
- Mois (D18, validé) : **OTAN** `JAN FEB MAR APR MAY JUN JUL AUG SEP OCT NOV DEC`.
- Saisie : prérempli, modifiable ; lecture stricte (jour 01–31 valide pour le mois, heure 00–23, minute 00–59) ; un GDH
  qui ne se lit pas est signalé sous le champ mais n'empêche pas l'envoi (aucune case obligatoire) — à confirmer (D19).
- Le choix du fuseau est mémorisé (`FireRequestPrefs`).

### 3.9 Brouillon et « Renouveler »

- Brouillon : la demande en cours est enregistrée à chaque modification (JSON dans `UiDataStore`), **un seul brouillon**
  pour le téléphone (D20), effacé après envoi réussi de toutes les parties à la file.
- « Renouveler » : recopie la dernière demande envoyée dans le formulaire, modifiable ; GDH demande remis à l'heure
  actuelle (D21). N'écrase pas un brouillon non vide sans confirmation.
- En mode démo, brouillon et dernière demande sont ceux de la démo (`DemoFireRequestPrefs`, en mémoire), jamais les vrais.

### 3.10 Une seule notification par demande (D25, validé)

Lu dans le code : `MeshDataHandlerImpl.updateNotification` (`core:data`) appelle, pour chaque texte reçu,
`MeshNotificationManager.updateMessageNotification` avec un drapeau `isSilent` ; sous Android,
`MeshNotificationManagerImpl.showConversationNotification` (`core:service`) **reconstruit une notification par
conversation** (même identifiant `contactKey.hashCode()`) à partir de l'historique non lu. D'où :

- **Contenu** : dans l'historique affiché par la notification, les parties d'une même demande sont fondues en une seule
  ligne « Demande de tir AB1234 · 2 / 3 reçus » (puis « 3 / 3 reçus ») ; une réponse GDH en une ligne « DT AB1234 · GDH
  attribution 112252 loc NOV 26 ». Fonction pure dans `core:model` (`FireRequestNotificationLines`), testée en
  `commonTest`, appelée par `core:service`.
- **Une seule alerte** : la première partie reçue d'une demande sonne ; les suivantes de la même demande mettent la
  notification à jour **en silence** (`isSilent = true`), décision prise par une fonction pure de `core:model`
  (`FireRequestAnnouncement`) appelée par `core:data`. Les autres messages ne changent pas.
- `core:data` n'est pas testé dans `fork-apk` : la règle est donc dans `core:model` (testée), l'appel dans `core:data`
  reste d'une ligne. Bureau (`DesktopMeshNotificationManager`) : inchangé en v1.

## 4. Écrans

1. **Puce « Demande de tir »** dans `FieldShortcutsRow` (après « Fait observé »), contour rouge, badge `DT`. Contrairement
   aux autres puces, elle ouvre un écran et **envoie** (geste prévu par la spécification) ; elle ne touche pas la zone de
   saisie.
2. **Formulaire** (écran plein, route `ContactsRoute.FireRequest`, D2) : en-tête, puis cases 1 à 11 dans l'ordre de la
   fiche, chacune repliable, avec son numéro et son titre de la fiche ; champs numériques au clavier numérique ; unités
   affichées comme sur la fiche (millièmes, m, min, s, ms). Barre du bas : taille totale et nombre de parties en direct,
   « Aperçu ». Case 7 : bouton « Coller une coordonnée » en tête de case, comme la maquette ; le texte copié (appui
   long sur la carte ou convertisseur) remplit la ligne L R U choisie (G ou H) : Fuseau, Bande, Carré, X, Y (D22).
   Le bouton « Depuis la carte » (aller-retour vers la carte) est noté pour une suite, hors v1.
3. **Aperçu** : toutes les parties, telles qu'elles partiront, avec « Partie k/N · X octets » ; « Modifier » ou
   « Envoyer la demande ». Rappel du destinataire.
4. **Conversation** : carte émetteur ou fiche reconstituée (§ 3.6) ; feuille « Messages bruts » ; dialogue GDH.

Destinataire : liste des nœuds connus (nom long sans suffixe d'équipe, nom court). Ouvert depuis une conversation
directe, il est présélectionné (D5). Après l'envoi, ouverture de la conversation directe avec le destinataire (D5).
Radio non connectée : le formulaire s'ouvre (brouillon), « Envoyer la demande » est grisé avec la raison (D23).

## 5. Textes (EN / FR)

- **Libellés, abréviations et codes de la fiche** : constantes françaises dans `FireRequestSheet`, **identiques sur tous les
  téléphones** et dans toutes les langues, comme `[CR] FAIT OBSERVÉ` (D24). Ils ne passent pas par `strings.xml`.
- **Textes d'interface** (EN / FR, dans `values/strings.xml` et `values-fr/strings.xml`, puis
  `python3 scripts/sort-strings.py`), préfixe `firereq_`, environ 30 clés :

| Clé (proposée) | FR | EN |
|---|---|---|
| `firereq_title` | Demande de tir | Fire request |
| `firereq_recipient` | Destinataire (nœud) | Recipient (node) |
| `firereq_gdh_format` | Fuseau du GDH | DTG time zone |
| `firereq_gdh_invalid` | GDH illisible : JJHHMM fuseau MOIS AA | Unreadable DTG: DDHHMM zone MON YY |
| `firereq_size` | %1$d octets · %2$d messages | %1$d bytes · %2$d messages |
| `firereq_preview` | Aperçu | Preview |
| `firereq_part_size` | Partie %1$d/%2$d · %3$d octets | Part %1$d/%2$d · %3$d bytes |
| `firereq_edit` | Modifier | Edit |
| `firereq_send` | Envoyer la demande | Send the request |
| `firereq_send_disconnected` | Radio non connectée : envoi impossible | Radio not connected: cannot send |
| `firereq_card_sent` | DT %1$s · %2$d messages | DT %1$s · %2$d messages |
| `firereq_received_count` | %1$d / %2$d reçus | %1$d / %2$d received |
| `firereq_part_missing` | Partie %1$d manquante | Part %1$d missing |
| `firereq_not_received` | non reçu | not received |
| `firereq_show_on_map` | Voir %1$s sur la carte | Show %1$s on the map |
| `firereq_raw` | Voir les messages bruts | Show raw messages |
| `firereq_resend_part` | Renvoyer la partie %1$d | Resend part %1$d |
| `firereq_renew` | Renouveler | Renew |
| `firereq_renew_replace` | Remplacer le brouillon par la dernière demande ? | Replace the draft with the last request? |
| `firereq_gdh_send_title` | Envoyer le GDH %1$s | Send the %1$s DTG |
| `firereq_paste_coordinate` | Coller une coordonnée | Paste a coordinate |
| `firereq_paste_invalid` | Aucune coordonnée MGRS dans le presse-papiers | No MGRS coordinate on the clipboard |
| `firereq_demo_sent` | Mode démo : rien n'est parti | Demo mode: nothing was sent |

  « non rempli » est un mot de la fiche/spécification (constante), pas un texte d'interface. Le badge `DT` est une
  constante.

## 6. Mode démo

- `ContactsRoute.FireRequest` ajouté à `DemoRoutes` (+ `DemoRoutesTest`).
- Envoi : par `@Named(SCREEN_DATA) SendMessageUseCase`, donc `DemoSendMessageUseCase` en démo : rien ne part, les parties
  passent par les états simulés (accusées pour BRAVO-2, en échec pour CHARLIE-3, ce qui montre « Renvoyer la partie »).
- **Demande fictive reçue** : trois parties de ALPHA-1 dans sa conversation directe, ajoutées à `DemoDataSet.history`
  (l'exemple de la spécification, avec une partie arrivée dans le désordre), et sa réponse GDH attribution envoyée par
  PC-0 (montre la fiche reconstituée complète).
- **Réponse GDH simulée** : après l'envoi d'une demande en démo à BRAVO-2, un `[DT <n°>] GDH attribution …` reçu de
  BRAVO-2 environ 8 s après l'accusé de la dernière partie, puis `GDH fin` 20 s plus tard (petit simulateur dans
  `core:demo`, à la manière de `DemoSendMessageUseCase`, arrêté à la sortie de la démo).
- Préférences de démo en mémoire ; `ScreenFacadesTest` vérifie que rien de la démo n'atteint les vraies préférences ni
  la vraie radio.

## 7. Découpage en commits (un sujet chacun, messages en français)

| # | Commit | Module |
|---|---|---|
| 1 | Spécification enregistrée telle quelle (**fait**, `d03c32b`) | docs |
| 2 | Ce plan | docs |
| 3 | GDH : écriture et lecture, fuseaux loc/Z/fra, mois + tests | core:model |
| 4 | Fiche : modèle des 11 cases et constantes de la fiche + tests | core:model |
| 5 | Écriture des cases en lignes de texte + tests (dont l'exemple de la spécification à l'octet près) | core:model |
| 6 | Découpage en parties de 200 octets + tests | core:model |
| 7 | Lecture d'une partie et d'une réponse GDH, aller-retour + tests | core:model |
| 8 | Regroupement : instances, doublons, désordre, manquantes, réponses + tests | core:model |
| 9 | Coordonnée L R U de la case 7 + tests (dont non-régression `MessageCoordinate`) | core:model |
| 10 | Préférences de la demande de tir (fuseau, brouillon, dernière demande), réelles et de démo + tests | core:repository, core:prefs, core:demo |
| 11 | Route `FireRequest`, ouverte en démo | core:navigation, core:demo, androidApp |
| 12 | Formulaire : en-tête et cases 1 à 6 (textes EN/FR) | feature:messaging |
| 13 | Formulaire : cases 7 à 11, « Coller une coordonnée » | feature:messaging |
| 14 | Aperçu et envoi des parties + tests du ViewModel | feature:messaging |
| 15 | Puce « Demande de tir » au-dessus de la zone de saisie | feature:messaging |
| 16 | Carte émetteur : états des parties, renvoi d'une partie | feature:messaging |
| 17 | Fiche reconstituée : parties manquantes, codes en clair, messages bruts, Voir sur la carte | feature:messaging |
| 18 | Réponse GDH : envoi par le destinataire, affichage chez l'émetteur | feature:messaging |
| 19 | Brouillon et « Renouveler » | feature:messaging |
| 20 | Mode démo : demande reçue fictive, réponse GDH simulée + tests | core:demo |
| 21 | Une seule notification par demande, mise à jour en silence + tests | core:model, core:data, core:service |
| 22 | Tests Compose du formulaire et des cartes | feature:messaging |
| 23 | `FORK.md` § 30 et état d'avancement ; `TESTS-A-FAIRE.md` § 18 ; passation | docs |

Avant chaque commit : `spotlessApply spotlessCheck detekt` et les tests JVM du module touché, en local. Modifications
ciblées (Edit), jamais de remplacement sur un fichier entier.

## 8. Tests

Tous en `commonTest` (lancés par `jvmTest` et `testAndroidHostTest` en CI), sauf les tests Compose (`jvmTest`).

- **Format** : chaque case vide → `<n> non rempli` ; les 11 cases toujours présentes et dans l'ordre ; l'exemple de la
  spécification reproduit **caractère pour caractère** (3 parties, 166 / 154 / 168 octets, 488 au total) ; aucun libellé
  hors `FireRequestSheet` (test qui compare chaque élément produit à la liste des libellés de la fiche).
- **Découpage à 200 octets UTF-8** : partie de 200 octets acceptée, 201 refusée ; accents et « · » comptés 2 octets ;
  coupure entre deux cases ; case seule trop longue coupée à un « · », reprise avec son numéro ; passage de 9 à 10 parties
  refusé (borne) ; pire cas (tous les champs au maximum) ≤ 9 parties.
- **Aller-retour** : `lire(découper(écrire(d))) == d` sur une série de demandes (vides, pleines, chaque case seule,
  accents, champs au maximum) ; génération aléatoire bornée avec graine fixe.
- **Regroupement** : parties dans l'ordre, dans le désordre (toutes permutations de 3 parties), une partie en double
  (identique : comptée une fois), une partie absente (« 2 / 3 reçus », partie 2 nommée, cases de 1 et 3 lisibles), une
  partie jamais reçue puis renvoyée, deux demandes de même n° (Renouveler), deux expéditeurs avec le même n°, `N`
  différent, délai dépassé, texte qui ressemble (`[DT` au milieu, `[DT AB 0/3]`, `[DT AB 4/3]`, `[DT AB 1/10]`) non pris.
- **Réponse GDH** : rattachement à la bonne instance (expéditeur + n°), réponse avant les parties, réponse sans demande,
  deux GDH attribution (le plus récent affiché), GDH fin avant attribution.
- **GDH** : `112250 loc NOV 26`, `112150Z NOV 26`, `112250 fra NOV 26` ; mois OTAN ; changement de jour et de mois entre Z et fra ; passage heure
  d'été/hiver à Paris ; 31 décembre → janvier de l'année suivante ; lecture stricte (32, 24 h, 60 min refusés).
- **Coordonnée L R U** : `35T LL 33000 - 90300` lu ; zone, bande ou carré inexistants refusés ; `Observateur X 34550 Y
  88800 Z 640`, `X 34550 · Y 88800` et les lignes POLAIRES / RECTANG. jamais lus ; `MessageCoordinate.find` ne trouve
  rien dans les parties de l'exemple.
- **Préférences** : fuseau mémorisé, brouillon écrit et relu, dernière demande ; démo isolée (`ScreenFacadesTest`).
- **ViewModel** : envoi = N appels à `SendMessageUseCase` dans l'ordre, vers la clé directe du destinataire (8 si PKC) ;
  brouillon effacé après envoi ; Renouveler.
- **Démo** : `DemoDataSetTest` (demande fictive complète reconstituée), réponse GDH simulée, `DemoRoutesTest`.
- **Compose** : formulaire (case vide, taille et nombre de parties en direct, bouton grisé radio déconnectée), carte
  émetteur (états, « Renvoyer la partie »), fiche reconstituée (k / N, « non rempli » gris, codes en clair).

## 9. CI

- Aucun nouveau module : `core:model`, `core:prefs`, `core:demo`, `feature:messaging` tournent déjà dans `fork-apk`.
  `core:navigation` n'y tourne pas (connu, FORK.md) : la route est couverte par `DemoRoutesTest` et la compilation de l'APK.
- Runs groupés pour ne pas en annuler : un run après le commit 9 (code commun), un après le 15 (formulaire et envoi), un
  après le 22. Pendant un run, poussée sur `wip/demande-de-tir`, puis sur `feat/demande-de-tir` une fois le run terminé.
  Les poussées de Markdown seul ne lancent pas `fork-apk`.

## 10. Essais sur téléphone (futur `TESTS-A-FAIRE.md` § 18)

Deux radios, deux téléphones (A émetteur, B destinataire), puis la radio du poste Drone.

- [ ] Puce « Demande de tir » (contour rouge, DT) visible à côté de « Ma position », « Fait observé », « Convertisseur ».
- [ ] Formulaire : chaque case conforme à la fiche papier (relecture case par case avec la fiche à côté) ; clavier
      numérique ; défilement avec le clavier ouvert.
- [ ] GDH : prérempli à l'heure du téléphone dans les trois fuseaux ; choix gardé après redémarrage.
- [ ] Exemple de la spécification saisi : aperçu identique à la spécification, 3 parties, 166 / 154 / 168 octets.
- [ ] Envoi : les 3 parties partent, accusées une à une ; carte unique « DT AB1234 · 3 messages ».
- [ ] B : fiche reconstituée, codes en clair, « non rempli » en gris ; « Voir sur la carte » centre sur 35T LL 33000 90300.
- [ ] B hors de portée pendant la partie 2 : « 2 / 3 reçus », partie 2 nommée ; A voit l'échec, « Renvoyer la partie 2 »,
      B complète la fiche.
- [ ] B envoie GDH attribution puis GDH fin : ils s'affichent sur la carte de A.
- [ ] Brouillon gardé après fermeture de l'application ; « Renouveler » reprend la dernière demande.
- [ ] Un client Meshtastic officiel reçoit les parties en texte lisible.
- [ ] Notifications : B reçoit 3 parties, **une seule** notification « Demande de tir AB1234 », qui sonne une fois et passe
      de « 1 / 3 reçus » à « 3 / 3 reçus » ; la réponse GDH chez A : une ligne.
- [ ] Mode démo : formulaire, envoi simulé, demande fictive reçue, réponse GDH simulée.

## 11. Risques

| Risque | Parade |
|---|---|
| Écriture d'une case inventée faute d'exemple | Propositions soumises (§ 3.2, D7 à D14) ; test « aucun libellé hors fiche » |
| Liste paginée : masquer des lignes peut faire sauter le défilement ou fausser le compteur de non-lus | Filtre au même endroit que les filtres de fil existants ; non-lus comptés par message (D14) ; essai sur téléphone |
| Notification : parties suivantes en silence, mais une partie d'une autre demande ou un autre message doit toujours sonner | Règle pure testée (`FireRequestAnnouncement`) ; essai sur téléphone |
| Aperçu de la liste des conversations montre le texte brut de la dernière partie | Accepté en v1 sauf décision contraire |
| Recherche : un résultat peut pointer une ligne masquée | Le résultat mène à la carte de l'instance |
| Temps d'antenne : N messages de 200 octets d'affilée, chacun avec accusé et renvois | N ≤ 9 ; la file existante espace les envois ; essai à mesurer |
| Même n° réutilisé sans « Renouveler » | Règle d'instance (§ 3.5) ; doublon exact compté une fois |
| Version ancienne ou saisie à la main côté émetteur | Lecture tolérante, ligne affichée telle quelle |
| Fuseau `Europe/Paris` absent de la base du téléphone | Base de fuseaux d'Android ; test JVM ; repli signalé |
| Maquette absente | D1 |

## 12. Décisions à prendre (questions ouvertes)

Réponses du porteur du projet le 11/11/2026 : **toutes les propositions acceptées**, avec les précisions ci-dessous (colonne
« Proposition » mise à jour). D26 validé case par case le 11/11/2026 : toutes les décisions sont prises.

| N° | Question | Proposition |
|---|---|---|
| D1 | Maquette validée (4 écrans) : la déposer dans `docs/` (images) pour aligner la disposition ? | **Fait** : `docs/maquette-demande-de-tir.pdf` (ligne L R U avec Fuseau, Bande, Carré) |
| D2 | Formulaire en écran plein (route) plutôt qu'en dialogue comme « Fait observé » | Écran plein |
| D3 | Longueur maximale du n° de tir (lettres et chiffres) | **1 à 10**, majuscules |
| D4 | Majuscules forcées dans le n° de tir et l'origine ? | n° oui, origine non |
| D5 | Destinataire présélectionné quand on vient d'une conversation directe ; après l'envoi, ouvrir la conversation directe avec lui | Oui / oui |
| D6 | « Destinataire » écrit dans le message (`CDS TIR`) : texte libre saisi, ou nom du nœud choisi ? | Texte libre prérempli avec le nom du nœud, modifiable |
| D7 | Case 2 : écriture des lettres A à H (avant les types, séparées par une espace) ; valeur des « [ ] » de Mettre en place, Mise en place décentrée, Renouveler | `2 A G Emblée · Mettre en place 2` |
| D8 | Case 3 : Panaché avec 4 (ECL) + A ou B : `4A` ? | `Panaché 4A et 2` |
| D9 | Suite d'une case coupée : la ligne recommence par le numéro de la case, sans mot ajouté | Oui |
| D10 | Nombre maximal de parties | 9 |
| D11 | Longueur maximale des champs libres (Origine, Destinataire, Bâtiment ou [ ], Rep, Point central, Centre, Synchro, Au commandement de) | 24 octets chacun |
| D12 | Délai au-delà duquel une partie de même n° ouvre une nouvelle demande | 2 h |
| D13 | Cases d'une partie manquante : « non reçu » (texte d'interface, distinct de « non rempli ») | Oui |
| D14 | Carte placée au dernier message reçu de la demande (elle descend), autres lignes masquées | Oui |
| D15 | Ligne L R U : X et Y toujours à 5 chiffres, ou 1 à 5 chiffres (précision MGRS réduite) ? « LL » est bien le carré de 100 km ? | **5 chiffres (au mètre) ; LL = carré de 100 km ; Z facultatif ; champs Fuseau, Bande, Carré, X, Y, Z ; vérification par le convertisseur** |
| D16 | Deux lignes G et H : deux boutons « Voir G / Voir H sur la carte » | Oui |
| D17 | Z écrit `112150 Z NOV 26` (espace, comme loc) ou `112150Z NOV 26` (usage militaire) | **`112150Z NOV 26`** (Z collé) ; `112150 loc NOV 26`, `112150 fra NOV 26` |
| D18 | Mois : OTAN (`FEB APR MAY AUG`) ou français (`FEV AVR MAI AOU`) | **OTAN** |
| D19 | GDH illisible : signalé sans bloquer l'envoi | Oui (aucune case obligatoire en v1) |
| D20 | Un seul brouillon pour le téléphone (pas un par conversation) | Oui |
| D21 | « Renouveler » : GDH demande remis à l'heure actuelle, n° de tir gardé | Oui |
| D22 | Remplir une ligne L R U depuis la carte ou le convertisseur : en v1 par « Coller une coordonnée » (le texte copié par l'appui long ou le convertisseur), sans aller-retour de navigation | Oui ; **« Depuis la carte » noté en suite** |
| D23 | Radio non connectée : formulaire ouvert, envoi grisé | Oui |
| D24 | Libellés de la fiche en français sur tous les téléphones, interface traduite ; badge `DT` gardé en anglais | Oui |
| D25 | Notifications : une par partie (comme aujourd'hui) ou une seule « Demande de tir AB1234 » | **Une seule par demande, mise à jour à l'arrivée des parties** (§ 3.10) |
| D26 | Écritures proposées pour les cases 4, 5, 6, 8, 10, 11 (§ 3.2) | **Validé** case par case (tableau du § 3.2 et réponses qui le suivent) |
| D27 | Case 7 : une ligne de texte par ligne de la fiche, préfixée `7 <lettre>` ; TLE en fin de la dernière ligne | Oui |
| D28 | Le filtre de fil « Info » montre aussi les demandes de tir, ou un filtre « DT » à part | Info en v1 |
