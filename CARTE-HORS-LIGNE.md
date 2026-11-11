# Carte hors ligne : produire et installer un fichier .pmtiles

NetworkTact (variante fdroid) affiche sans Internet une carte chargée à l'avance depuis un
fichier `.pmtiles`, avec les positions des nœuds par-dessus. Le style, les polices et les icônes
de la carte sont dans l'application ; seul le fichier de la zone est à produire, une fois, sur un
ordinateur relié à Internet.

Les données viennent d'OpenStreetMap, via le fond de carte Protomaps : « © contributeurs
OpenStreetMap », licence ODbL. L'application affiche cette mention sur la carte.

## Méthode principale : télécharger la carte depuis le téléphone

Le téléphone doit avoir Internet **pendant le téléchargement seulement** ; ensuite la carte
fonctionne en mode avion.

1. Onglet **Carte** → bouton des fonds de carte → **Cartes sur le téléphone** → **Télécharger
   une carte**. La liste se ferme ; un cadre à quatre poignées apparaît sur la carte, qui est
   remise au nord et à plat.
2. Placer la zone : déplacer et zoomer la carte, tirer les coins du cadre, ou **Utiliser la
   vue** (toute la zone visible). Le panneau affiche la largeur et la hauteur en km.
3. Choisir le **zoom maximal** (15 par défaut ; 14 divise la taille par deux à quatre). Une
   estimation grossière s'affiche aussitôt, sans réseau.
4. **Calculer la taille** : le téléphone lit l'index de la carte (environ 1 Mo) et affiche la
   taille exacte, le nombre de tuiles et l'édition (la plus récente des 8 derniers jours).
5. Modifier le nom si besoin, puis **Télécharger**. Au-delà de 50 Mo, une confirmation est
   demandée, en Wi-Fi comme en données mobiles. Garder l'écran allumé ; **Annuler** ne laisse
   rien sur le téléphone.
6. Une fois terminée, la carte devient le fond choisi et apparaît dans **Cartes sur le
   téléphone**, avec son édition, sa source et ses dimensions.

**Ce que voit le serveur source** : la zone demandée et l'adresse du téléphone (ou celle du
réseau, du VPN). L'application ne lui envoie rien d'autre (ni nom d'application, ni compte). Pour
ne rien révéler à Protomaps, régler une autre source (**Modifier**, à côté de « Source ») : un
miroir interne en `https://`, soit un modèle d'éditions (`https://hôte/chemin/AAAAMMJJ.pmtiles`),
soit l'adresse d'un seul fichier `.pmtiles` ; le serveur doit accepter les requêtes par plages
(`Range`).

**Coupure de réseau** : le téléchargement s'arrête et se garde ; **Reprendre** (dans le panneau
ou dans **Cartes sur le téléphone**) ne télécharge que ce qui manque. Une reprise n'est possible
que tant que l'édition existe (environ 7 jours, voir l'étape 2 ci-dessous) ; sinon, recommencer.

**Exporter une carte** : dans **Cartes sur le téléphone**, l'icône d'export ouvre le sélecteur de
fichiers Android ; la copie ne va qu'à l'endroit choisi (par exemple `Download`), d'où elle peut
être copiée sur un ordinateur ou un autre téléphone (garder la mention « © contributeurs
OpenStreetMap »).

## Méthode de secours : produire le fichier sur un Mac

À utiliser si le téléphone n'a pas Internet, ou pour une zone non rectangulaire (un
département, étape 4).

## 1. Installer l'outil `pmtiles` sur le Mac

```sh
brew install pmtiles
pmtiles version
```

Sans Homebrew : télécharger `go-pmtiles_…_Darwin_arm64.zip` (Mac Apple Silicon) ou
`…_Darwin_x86_64.zip` (Mac Intel) sur https://github.com/protomaps/go-pmtiles/releases,
décompresser, puis lancer `./pmtiles` depuis ce dossier (au premier lancement : clic droit →
Ouvrir, ou `xattr -d com.apple.quarantine pmtiles`).

## 2. Choisir l'édition du fond Protomaps

Protomaps publie chaque jour le monde entier dans un seul fichier (environ 140 Go) ; `pmtiles
extract` n'en télécharge que la zone demandée. **Chaque édition n'est conservée qu'environ 7 jours**
(mesuré le 11 novembre 2026 : les éditions du 5 au 11 novembre répondaient, celle du 4 et les plus
anciennes non). Il faut donc **prendre une date de la semaine en cours** : celle du jour (publiée en
cours de matinée, heure UTC) ou celle de la veille. Les éditions disponibles sont listées sur
https://maps.protomaps.com/builds/ ; on peut aussi composer la date soi-même, par exemple pour hier :

```sh
SOURCE=https://build.protomaps.com/$(date -u -v-1d +%Y%m%d).pmtiles   # macOS
echo $SOURCE
```

(sous Linux : `date -u -d yesterday +%Y%m%d`). Si l'adresse répond « 404 », l'édition a été
retirée ou pas encore publiée : prendre la date de la veille. Une adresse copiée d'une ancienne
procédure (par exemple `20261007`) ne fonctionne plus.

## 3. Petit fichier de test : une ville (avec `--bbox`)

Le rectangle s'écrit `ouest,sud,est,nord` en degrés décimaux (longitude, latitude). Exemple,
Draguignan :

```sh
pmtiles extract $SOURCE draguignan.pmtiles --bbox=6.40,43.50,6.52,43.57 --maxzoom=15
pmtiles show draguignan.pmtiles
```

Quelques mégaoctets, une minute environ. `pmtiles show` doit indiquer `tile type: mvt` et
`max zoom: 15`. Pour trouver le rectangle d'une autre ville : sur https://www.openstreetmap.org,
« Exporter », puis relever les quatre valeurs affichées.

## 4. La carte d'un département (avec `--region`)

Contour du département en GeoJSON, par exemple le Var (83) :

```sh
curl -o var.geojson https://raw.githubusercontent.com/gregoiredavid/france-geojson/master/departements/83-var/departement-83-var.geojson
pmtiles extract $SOURCE var.pmtiles --region=var.geojson --maxzoom=15 --dry-run
pmtiles extract $SOURCE var.pmtiles --region=var.geojson --maxzoom=15
pmtiles show var.pmtiles
```

Pour un autre département, remplacer `83-var` par le code et le nom du dossier correspondant
dans https://github.com/gregoiredavid/france-geojson/tree/master/departements. `--dry-run`
donne la taille avant de télécharger : compter quelques dizaines à quelques centaines de
mégaoctets selon la densité du département. Pour un camp ou un secteur, un rectangle
(`--bbox`) suffit.

Pour réduire le fichier, `--maxzoom=14` le divise environ par deux à quatre : les rues restent
lisibles, les numéros et petits bâtiments disparaissent.

## 5. Copier le fichier sur le téléphone, sans Internet

- Câble USB : sur le Mac, avec un logiciel de transfert Android (par exemple OpenMTP), copier
  le fichier dans le dossier `Download` du téléphone (sur le téléphone : mode USB « Transfert
  de fichiers ») ;
- ou carte microSD, ou clé USB-C branchée au téléphone.

## 6. Installer la carte dans NetworkTact

1. Onglet **Carte** → bouton des fonds de carte → **Cartes sur le téléphone**.
2. **Importer un fichier .pmtiles**, choisir le fichier. L'application vérifie le fichier et
   l'espace libre, puis copie le fichier dans son stockage (barre de progression, annulation
   possible). En cas d'échec, rien n'est conservé.
3. Choisir la carte dans le menu des fonds de carte (elle porte le nom du fichier).
4. Vérifier hors ligne : mode avion, Bluetooth réactivé pour la radio. La carte, ses libellés
   et la mention « © contributeurs OpenStreetMap · Protomaps » (bouton ⓘ en bas de la carte)
   s'affichent sans réseau.

Le fichier d'origine peut être supprimé du dossier `Download` après l'import. Pour retirer une
carte : **Cartes sur le téléphone**, icône corbeille, confirmer.

## Limites connues

- Seuls les fichiers `.pmtiles` vectoriels tirés du fond Protomaps s'affichent avec le style de
  l'application ; un `.pmtiles` raster est refusé à l'import.
- Les libellés sont en français quand OpenStreetMap a un nom français, sinon dans la langue
  locale ; les caractères hors alphabets latin, grec et cyrillique ne s'affichent pas.
- La carte montre l'état d'OpenStreetMap à la date de l'édition (choisie à l'étape 2, ou
  affichée dans la liste pour une carte téléchargée).
- Le téléchargement dans l'application prend un rectangle seulement, de 500 Mo au plus par carte
  (au-delà : « La zone est trop grande », réduire la zone ou le zoom).
- Au-delà du zoom 15, la carte est agrandie, sans nouveaux détails.
- Diffuser le fichier à d'autres équipes est permis par l'ODbL, à condition de garder la
  mention « © contributeurs OpenStreetMap ».
