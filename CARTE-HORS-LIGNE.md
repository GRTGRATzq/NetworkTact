# Carte hors ligne : produire et installer un fichier .pmtiles

NetworkTact (variante fdroid) affiche sans Internet une carte chargée à l'avance depuis un
fichier `.pmtiles`, avec les positions des nœuds par-dessus. Le style, les polices et les icônes
de la carte sont dans l'application ; seul le fichier de la zone est à produire, une fois, sur un
ordinateur relié à Internet.

Les données viennent d'OpenStreetMap, via le fond de carte Protomaps : « © contributeurs
OpenStreetMap », licence ODbL. L'application affiche cette mention sur la carte.

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

Protomaps publie chaque jour le monde entier dans un seul fichier (plus de 100 Go) ; `pmtiles
extract` n'en télécharge que la zone demandée. Les éditions disponibles sont listées sur
https://maps.protomaps.com/builds/ : prendre une date récente, par exemple :

```sh
SOURCE=https://build.protomaps.com/20261007.pmtiles
```

Si l'adresse répond « 404 », l'édition a été retirée : prendre une date plus récente de la liste.

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
- La carte montre l'état d'OpenStreetMap à la date de l'édition choisie à l'étape 2.
- Au-delà du zoom 15, la carte est agrandie, sans nouveaux détails.
- Diffuser le fichier à d'autres équipes est permis par l'ODbL, à condition de garder la
  mention « © contributeurs OpenStreetMap ».
