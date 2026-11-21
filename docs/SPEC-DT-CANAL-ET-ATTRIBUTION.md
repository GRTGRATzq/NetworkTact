# Demande de tir : envoi sur un canal et attribution du tir

Spécification validée par le porteur du projet le 21/11/2026. Complète `docs/SPEC-DEMANDE-DE-TIR.md` (v1), qui reste
valable partout où ce document ne dit rien. Le formulaire, ses 11 cases et le découpage restent ceux de la v1. Aucune
case verrouillée.

## 1. Deux modes d'envoi

- Sur un canal (usage normal) ou en direct à une radio (v1, conservé).
- Sélecteur « Envoyer à » dans l'aperçu : Équipe TIR (par défaut si ce canal existe, sinon Principal) · Principal ·
  Les deux (avertissement : trafic doublé) · Une radio (menu des radios).
- Champ « Destinataire » de l'en-tête = nom du canal ou de la radio.
- Les doublons (radio sur les deux canaux) sont déjà gérés.

## 2. Diffusion

- Mêmes messages `[DT <n°> k/N]` que la v1.
- Sur un canal, pas d'accusé par destinataire : la carte de l'émetteur montre « diffusée sur <canal> » et la liste des
  accusés DT reçus.
- Chez le récepteur : fiche reconstituée, « k / N reçus », une seule notification, pas de demande de renvoi
  automatique.
- L'émetteur peut renvoyer la fiche ou une partie sur le canal.

## 3. Accusé DT

- Bouton manuel (jamais automatique), actif seulement quand la fiche est complète (N / N).
- Il part en direct à l'émetteur seulement : `[DT AB1234] Accusé DT 112251 loc NOV 26` (environ 40 octets, GDH au
  format mémorisé).
- Carte de l'émetteur : une ligne « Accusé DT : <nom court> <GDH> » par poste.

## 4. Attribution

- Le chef de tir est le poste désigné comme tel dans l'organigramme (rôle explicite, pas forcément le détenteur de
  l'organigramme).
- Seul ce poste voit « Tir attribué à : » + menu déroulant des radios du réseau (nom court et nom long) + « Attribuer »,
  qui remplace le bouton « GDH attribution envoyé ».
- Sans chef désigné : attribution non proposée, message « Aucun chef de tir dans l'organigramme ».
- Le GDH attribution est généré à l'attribution (heure du téléphone, format mémorisé).
- Les autres postes du canal ne voient pas l'attribution : messages directs seulement, à l'émetteur et à la radio
  désignée.
- Le GDH fin reste un court message direct de la radio désignée à l'émetteur (v1).
- Réattribution permise : la nouvelle radio désignée et l'émetteur sont prévenus ; l'ancienne radio désignée ne l'est
  pas.

## 5. Messages

Texte ordinaire, aucun changement de protocole, noms courts de 4 caractères au plus ; l'identité réelle vient de
l'expéditeur et du destinataire du message direct.

- À l'émetteur : `[DT AB1234] GDH attribution 112252 loc NOV 26 · Tir attribué à CDS` (environ 68 octets).
- À la radio désignée : `[DT AB1234] Tir attribué à vous par OA · GDH attribution 112252 loc NOV 26` (environ
  76 octets).
- Si le chef est l'émetteur, un seul message part (celui de la radio désignée).
- Radio désignée absente du canal utilisé : bouton « Renvoyer la fiche à <radio> » chez le chef ou l'émetteur, en
  message direct (mécanisme de la v1).
- Une autre application Meshtastic doit afficher ces messages comme du texte lisible.
