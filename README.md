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

**GitHub Actions (rien à installer)** : push le repo, ajoute éventuellement le secret `MAL_CLIENT_ID`,
l'APK sort dans l'onglet Actions → artifact `BakaSwipe-apk`.

**En local** (JDK 17 + Android SDK) :
```sh
./gradlew assembleRelease            # -> app/build/outputs/apk/release/app-release.apk
adb install app/build/outputs/apk/release/app-release.apk
```
Ou ouvre simplement le dossier dans Android Studio → Run.

L'APK release est signé avec la clé debug : installable direct, pas publiable sur le Play Store tel quel.

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
