# BakaSwipe

Tinder-like pour ta liste MyAnimeList.

| Geste | Action MAL |
|---|---|
| → droite | Completed + note (1–10 ou sans note) |
| ← gauche | Pas vu : aucun statut sur MAL, gardé en local pour ne plus le revoir |
| ↑ haut | Choix Watching / Plan to Watch / On-Hold / Dropped |
| tap carte | Déplie le synopsis |

## 1. Client ID MAL
1. https://myanimelist.net/apiconfig → **Create ID**
2. App Type : **Android**
3. App Redirect URL : `bakaswipe://oauth`
4. Copie le **Client ID** (pas de secret en type Android)

Tu le tapes au premier lancement de l'app, ou tu le bakes au build (`-PMAL_CLIENT_ID=...`).

## 2. Build de l'APK

**GitHub Actions (rien à installer)** : chaque push build l'APK (onglet Actions → artifact `BakaSwipe-apk`).
Un push sur `main` avec un `versionName` pas encore tagué crée la release `vX.Y.Z` avec l'APK en pièce jointe
(téléchargeable direct, pas de zip) et la section correspondante de `CHANGELOG.md` en description.
Aussi possible : pousser un tag `vX.Y.Z`, ou lancer le workflow à la main avec `release=vX.Y.Z`.

Secrets du repo : `SIGNING_KEYSTORE_B64` (keystore en base64), `SIGNING_STORE_PASSWORD` (mot de passe du keystore
et de la clé), `SIGNING_KEY_ALIAS` (alias de la clé), et optionnellement `MAL_CLIENT_ID`.

**En local** (JDK 17 + Android SDK) :
```sh
./gradlew assembleRelease            # -> app/build/outputs/apk/release/app-release.apk
adb install app/build/outputs/apk/release/app-release.apk
```
Ou ouvre simplement le dossier dans Android Studio → Run.

Sans keystore (build local), l'APK release est signé avec la clé debug : installable, mais les mises à jour par-dessus une version CI échoueront.

## Tirage semi-aléatoire
- **Populaires** : top ~500 en popularité (ou top 25 de la saison si filtre d'années)
- **Mix** : top ~3000 (ou top 80 de la saison)
- **Full random** : jusqu'à ~14000 / toute la saison
- Filtre d'années : tire une saison au hasard dans l'intervalle
- Les animes déjà dans ta liste MAL ou marqués « Pas vu » sont exclus

## Stack
Kotlin, Jetpack Compose (Material 3), OkHttp, Coil. minSdk 26 (Android 8+).

## Licence
Copyright (C) 2026 ClaudeGontier

Ce programme est un logiciel libre : tu peux le redistribuer et/ou le modifier selon les termes de la
[GNU General Public License version 3](LICENSE) (GPL-3.0-only) telle que publiée par la Free Software Foundation.
Il est distribué SANS AUCUNE GARANTIE.

### Icône
L'icône n'est pas couverte par la GPL :
- **Image de fond** : par [@picociko_cr](https://x.com/picociko_cr), placée par son auteur dans le domaine public.
- **Logo au premier plan** : logo officiel MyAnimeList, propriété de MyAnimeList (à remplacer avant toute diffusion publique).
