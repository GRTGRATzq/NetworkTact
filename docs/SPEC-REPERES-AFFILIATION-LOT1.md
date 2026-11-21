# Points de repère : cadres d'affiliation APP-6(C), lot 1 (point 3)

Spécification décidée le 21 novembre 2026. Branche : `feat/reperes-affiliation` (sur GitHub : `wip/reperes-affiliation`),
à partir de `test/correctifs-13-11-e`.

## Principe

Dans un premier temps, les points de repère n'ont que 4 cadres d'affiliation APP-6(C), sans icône à l'intérieur, et
plus aucun émoji ne reste sur les points de repère :

| Affiliation | Forme | Couleur |
|---|---|---|
| Ami | rectangle 3:2 | `#80E0FF` (déjà utilisé pour les radios, `drawFriendlyFrame`) |
| Ennemi (hostile) | losange | `#FF8080` |
| Neutre | carré | `#AAFFAA` |
| Inconnu | trèfle à quatre lobes | `#FFFF80` |

Bord noir. Les trois couleurs hors Ami sont **à confirmer dans l'APP-6(C)** par l'utilisateur avant la fusion : elles
sont dans une constante unique, faciles à changer.

## Composant

- Les quatre formes dans le même composant commun de `core:ui` que le cadre ami (point 4), rendu équivalent sur la
  carte MapLibre.

## Sélecteur

- Un seul choix parmi 4 boutons (Ami, Ennemi (hostile), Neutre, Inconnu) à la place de la liste d'émojis.
- Choix par défaut : Inconnu. Rien dans le cadre.

## Tailles

- Un point de repère n'est pas une radio. Son cadre est plus petit (environ 75 % du cadre d'une radio, soit environ
  36 × 24 dp pour le rectangle), sans texte dedans ; le nom du point est écrit à côté.
- Les quatre formes sont dimensionnées pour une surface visuelle comparable, avec le liseré blanc et le halo de
  sélection du point 4, lisibles sur fond clair, sombre et satellite.
- À 50 radios et plus : une image par apparence, plafond et regroupement de la carte inchangés.

## Anciens points et valeurs inconnues

- Aucun ancien point de repère à conserver (tout a été effacé) : pas de migration, la liste d'émojis disparaît.
- Une valeur d'icône non reconnue s'affiche comme un cadre Inconnu.

## Icônes automatiques

- « Fait observé » (👁) devient un cadre Inconnu par défaut, modifiable comme les autres points de repère.
- Les 📍 et 📌 des positions sont remplacés aussi, par un des 4 cadres : relevé de chaque usage et proposition du cadre
  par usage (piste : Ami pour les positions de nos propres postes, Inconnu sinon). **Décision de l'utilisateur après
  le relevé.**

## Transmission

- Le choix part dans le champ d'icône du point de repère avec 4 codes très courts, sans changer de protocole ni de
  firmware.
- Une autre application Meshtastic affiche un carré vide (accepté).

## Décisions après le relevé (21 novembre)

- **Usages automatiques** (`SharedPoint`) : fait observé (`FO …`, ancien 👁) → Inconnu ; toute `[POS]` (`POS …`,
  ancien 📍) → Ami ; autre message avec coordonnée (`PT …`, ancien 📌) → Inconnu ; point créé à la main, valeur 0 ou
  inconnue → Inconnu. Le 🔒 du titre (carte Google) devient le mot « verrouillé » à côté du nom.
- **Codes** : U+E001 Ami, U+E002 Ennemi, U+E003 Neutre, U+E004 Inconnu (zone à usage privé d'Unicode). Valeurs valides,
  donc pas remises à 0 à la réception. Le champ fait 4 octets quel que soit le code.
- **Dimensions** (forme, bord noir compris / image avec liseré 1 dp et halo 3 dp réservés) : rectangle 36 × 24 / 44 × 32 ;
  losange 40 × 40 pointe à pointe / 48 × 48 ; carré 28 × 28 / 36 × 36 ; trèfle 32 × 32 / 40 × 40. Bord noir 2 dp. Toutes
  dans `AffiliationFrames` (`core:ui`), ainsi que les couleurs.
- **Variante Google** : pas de cadres sur la carte ; le nom de l'affiliation en texte sur sa couleur, jamais d'émoji ni
  de caractère cassé. Le sélecteur et la fiche (communs aux deux variantes) montrent les cadres.
