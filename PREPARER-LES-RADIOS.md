# Préparer une radio pour NetworkTact

Dernière mise à jour : 9 novembre 2026.

À faire sur chaque radio **après une remise à zéro** (voir `FLASHER-LA-RADIO.md`), une fois la
radio appairée en Bluetooth avec son téléphone dans NetworkTact. Tous les réglages se font depuis
l'application, dans **Réglages → Réglages de l'appareil**, puis dans **Réglages → Équipes**.

Les noms des écrans et des champs sont ceux de l'application en français. Enregistrer chaque écran
avant de passer au suivant ; la radio peut redémarrer après un changement et le Bluetooth se
reconnecte alors tout seul. Attendre la reconnexion avant de continuer.

## 1. LoRa

Réglages → Réglages de l'appareil → **LoRa**

- [ ] **Région** : `EU_868`.
- [ ] **Préréglages** : `LongFast`.

Ces deux réglages doivent être **identiques sur toutes les radios**, sinon elles ne s'entendent pas.

## 2. Position

Réglages → Réglages de l'appareil → **Position**

- [ ] **GPS** activé (« Mode GPS (matériel physique) »).
- [ ] **Champs de position** : cocher **`TIMESTAMP`** en plus des cases déjà cochées. Ne rien
      décocher.

Sans `TIMESTAMP`, la position ne porte pas l'heure du relevé : « Diffuser ma position maintenant »
est alors refusé et le récepteur ne sait pas de quand date la position.

## 3. Utilisateur

Réglages → Réglages de l'appareil → **Utilisateur**

- [ ] **Nom long** : l'**indicatif seul**, par exemple `ALPHA-1` ou `PC-0`. Ne pas écrire l'équipe
      entre crochets : c'est l'écran Équipes (§4) qui l'ajoute.
- [ ] **Nom court** : quelques caractères qui identifient la radio sur la carte et dans la liste
      des nœuds, par exemple `A1`.

## 4. Équipes

Réglages → **Équipes**

- [ ] Adopter la liste d'équipes reçue du PC (message `[EQUIPES] …`) ; à défaut, la saisir avec la
      saisie manuelle de secours.
- [ ] Choisir l'équipe de la radio, puis confirmer « Envoyer à ma radio ». Le nom long devient
      `ALPHA-1 [Alpha]` ; le nom court et les autres réglages ne changent pas.
- [ ] Radio du PC : c'est elle qui diffuse la liste (« Diffuser la liste (PC) »).

## 5. Vérifier

- [ ] Ouvrir l'écran de diagnostic de liaison : radio connectée, réglage LoRa réel conforme
      (`EU_868`, `LongFast`), autres nœuds entendus.
- [ ] Sur une autre radio, le nœud apparaît avec son nom long `ALPHA-1 [Alpha]`, et sa position porte
      une heure de relevé.
