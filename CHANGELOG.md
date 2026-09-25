# Changelog

## v1.0.3

### Corrections
- **Mises à jour impossibles (« App not installed »)** : l'APK est maintenant signé avec une clé fixe. Avant, chaque build avait une signature différente et Android refusait la mise à jour.

> ⚠️ Pour passer à cette version, **désinstaller une dernière fois** l'ancienne. Les versions suivantes s'installeront par-dessus sans problème.

## v1.0.2

### Corrections
- **Icône de l'app** : icône adaptive (Android 8+), le logo remplit maintenant tout le cercle au lieu d'être réduit dans un rond blanc.

## v1.0.1

### Corrections
- **Listes → « Pas vu (retirer de MAL) »** : l'anime est maintenant ajouté aux « Pas vu » locaux, il ne revient plus dans le deck.
- **Passer un « Pas vu » à un statut MAL** : l'entrée n'est retirée des « Pas vu » qu'après la réussite de l'écriture MAL (plus de perte si l'appel échoue).
- **Annuler (↶) puis échec MAL** : plus de carte en double dans le deck.
- **Session expirée intempestive** : le renouvellement du token est synchronisé, deux requêtes simultanées ne se déconnectent plus mutuellement.
- **Déconnexion** : l'indicateur de chargement du deck est bien réinitialisé.
- **Swipe** : la taille de la carte est relue à chaque geste (seuils corrects après une rotation).

### Sécurité
- `allowBackup` désactivé : les tokens MAL ne partent plus dans la sauvegarde Google.

## v1.0.0
- Première version.
