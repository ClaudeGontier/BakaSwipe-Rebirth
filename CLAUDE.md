# BakaSwipe — contexte projet

App Android perso (Tinder-like) pour remplir sa liste MyAnimeList en swipant.
Démarré dans une conversation claude.ai ; ce fichier reprend la demande initiale et les choix faits.
L'utilisateur parle français, style direct et concis : réponds en français, pas de blabla.

## Demande initiale (specs)
- Nom : **BakaSwipe**, APK Android, se connecte à l'API MyAnimeList (v2, OAuth2).
- Affiche des animes de manière **semi-aléatoire**, une carte à la fois.
- **Swipe droite** → Completed + saisie de la note.
- **Swipe gauche** → "Pas vu" = aucun statut sur MAL.
- **Swipe haut** → sélecteur entre Watching / On-Hold / Dropped / Plan to Watch.
- Menus pour consulter ce qui est vu, pas vu, les notes, etc.
- Filtres : pouvoir éviter le "full random", et filtrer avant/après une année.
- Logo fourni = icône de l'app (c'est l'icône officielle MAL : OK en perso, à remplacer avant toute diffusion publique, licence API MAL).
- Licence : **GPLv3 uniquement** (GPL-3.0-only, `LICENSE` à la racine). Icône hors GPL : fond par @picociko_cr (domaine public), logo MAL au premier plan (propriété MAL).

## Ce qui est implémenté
- Swipe avec animation + boutons ✕ / ↑ / ✓, bouton ↶ pour annuler le dernier swipe, tap carte = synopsis déplié.
- Completed : note 1–10 (ou sans note), `num_watched_episodes` = nb d'épisodes si connu.
- "Pas vu" : MAL n'a pas ce statut → stocké **en local** (SharedPreferences, JSON) pour ne plus le reproposer. Pas de DELETE au swipe (les animes du deck ne sont pas dans la liste MAL) ; depuis les Listes, « Pas vu (retirer de MAL) » fait un DELETE (404 ignoré) et ajoute aux Pas vu.
- Onglets : Swipe / Listes (un onglet par statut + Pas vu, recherche, tri récent/note/titre/année, édition statut+note ou retrait) / Stats (moyenne, répartition des notes, épisodes, genres, décennies) / Réglages.
- Réglages : mode Populaires / Mix / Full random, filtre d'années (RangeSlider + raccourcis), formats (tv, movie, ova, ona, special, music), titres anglais, NSFW, déconnexion, vider les Pas vu.
- Animes déjà dans la liste MAL ou en Pas vu exclus du deck.

## Tirage semi-aléatoire (AppViewModel.fetchBatch)
- Sans filtre d'années : `/anime/ranking?ranking_type=bypopularity`, offset aléatoire dans le top 500 / 3000 / 14000 selon le mode.
- Avec filtre d'années : saison aléatoire dans l'intervalle, `/anime/season/{y}/{s}?sort=anime_num_list_users`, limit 25 / 80 / 400 selon le mode, puis filtre client sur `start_season.year`.

## Architecture
- Kotlin, Jetpack Compose Material 3, OkHttp, Coil 2, org.json (pas de lib de sérialisation). Pas de navigation-compose : onglet via state.
- `data/MalApi.kt` : OAuth PKCE (**MAL ne supporte que `plain`**), refresh auto sur 401, endpoints. Appels bloquants → Dispatchers.IO.
- `data/Prefs.kt` : tokens, client ID, filtres, liste Pas vu.
- `ui/AppViewModel.kt` : état global, deck, écritures MAL sérialisées via Mutex.
- OAuth redirect : `bakaswipe://oauth` (intent-filter sur MainActivity, singleTask).
- Client ID : saisi au login, ou via `-PMAL_CLIENT_ID=` au build (BuildConfig). App type MAL = Android (pas de secret).

## Build
- AGP 8.7.3, Kotlin 2.1.0, Gradle 8.11.1, compileSdk 35, minSdk 26, JDK 17.
- CI : `.github/workflows/build.yml` → `./gradlew assembleRelease`, artifact `BakaSwipe-apk`. Release signé avec une clé fixe fournie par les secrets `SIGNING_KEYSTORE_B64` / `SIGNING_STORE_PASSWORD` / `SIGNING_KEY_ALIAS` (sinon fallback clé debug jetable → mises à jour impossibles).
- Le build CI passe (pas de compilation possible en local dans la sandbox : pas d'accès à Google Maven).
- Release : ajouter une section `## vX.Y.Z` dans `CHANGELOG.md`, bumper `versionCode`/`versionName`, puis push sur `main` (release auto si le tag `vX.Y.Z` n'existe pas), ou lancer le workflow avec `release=vX.Y.Z`, ou pousser le tag ; la section du changelog devient le corps de la release, l'APK `BakaSwipe-vX.Y.Z.apk` y est attaché directement. Release refusée sans keystore.

## Idées pas encore faites
- Pas de cache offline, pas de tests.
- Possibles : saisie d'épisodes pour Watching, recherche manuelle d'un anime, filtre par genre, icône monochrome (thème Android 13).
