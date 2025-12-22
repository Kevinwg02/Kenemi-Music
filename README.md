<div align="center">

# 🎵 KenemiMusic

### Un lecteur audio local pour Android, sans pub et sans collecte de données

<p>
J'en avais marre des lecteurs audio bourrés de pubs, alors j'ai fait le mien avec Material3 et Jetpack Compose.
</p>

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5+-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

</div>

---

## ✨ Fonctionnalités principales

<table>
<tr>
<td width="33%" align="center">
<h3>📂 Bibliothèque locale</h3>
<p>Scan automatique et organisation intelligente</p>
</td>
<td width="33%" align="center">
<h3>🎵 Lecteur complet</h3>
<p>Shuffle, repeat, file d'attente modifiable</p>
</td>
<td width="33%" align="center">
<h3>🎤 Paroles synchronisées</h3>
<p>Recherche auto/manuelle, édition, cache</p>
</td>
</tr>
<tr>
<td width="33%" align="center">
<h3>⭐ Favoris</h3>
<p>Accès rapide à vos titres préférés</p>
</td>
<td width="33%" align="center">
<h3>🎨 Interface moderne</h3>
<p>Material3, thèmes, animations fluides</p>
</td>
<td width="33%" align="center">
<h3>📱 Notification</h3>
<p>Contrôle complet en arrière-plan</p>
</td>
</tr>
</table>

---

## 📖 Table des matières

- [📂 Bibliothèque musicale](#-bibliothèque-musicale)
- [🎵 Lecteur audio](#-lecteur-audio)
- [🎤 Paroles synchronisées](#-paroles-synchronisées)
- [⭐ Système de favoris](#-système-de-favoris)
- [🎧 Playlists](#-playlists)
- [🎨 Interface utilisateur](#-interface-utilisateur)
- [🛠️ Technologies](#️-technologies)
- [📥 Installation](#-installation)
- [🤝 Contribution](#-contribution)

---

## 📂 Bibliothèque musicale


- ✅ Scan automatique de tous les fichiers audio
- ✅ Organisation par chansons, albums, artistes, playlists
- ✅ Index alphabétique (A-Z) pour navigation rapide
- ✅ Filtre intelligent (exclut les fichiers corrompus)
- ✅ Recherche globale dans tous les contenus
- ✅ Affichage optimisé avec scrolling fluide



## 🎵 Lecteur audio

### 🎮 Contrôles de base

▶️ Lecture / Pause
⏭️ Suivant / ⏮️ Précédent
🔀 Mode aléatoire (shuffle)
🔁 Répétition : off / une chanson / playlist

### 📋 File d'attente
- Visualisation complète des titres en attente
- Réorganisation par glisser-déposer
- Suppression de titres à la volée
- Indicateur visuel du titre en cours de lecture

### 🔊 Lecture en arrière-plan
- Service dédié pour lecture continue
- Contrôles complets via notification système
- Reprise automatique après navigation
- Gestion intelligente de la mémoire


## 🎤 Paroles synchronisées

### 🔍 Recherche automatique
| Source | Type | Limitations |
|--------|------|-------------|
| Lyrics.ovh | API REST | ✅ Gratuite |
| ChartLyrics | SOAP API | ✅ Gratuite |
| Lyrist | Vercel API | ✅ Gratuite |
| Alternatives | Multiple | ✅ Gratuite |

- 🎯 **5 variantes** testées automatiquement par chanson
- 🔓 **Aucune clé API** requise
- ♾️ **Aucune limitation** de requêtes

### ✏️ Recherche manuelle
- 🔎 Recherche avec suggestions en temps réel
- ✍️ Saisie manuelle (coller ou taper)
- ✏️ Édition des paroles existantes
- 💾 Sauvegarde instantanée

### 💾 Cache intelligent
```
📝 Paroles manuelles → Prioritaires et permanentes (♾️)
🤖 Paroles auto      → Expiration après 90 jours (⏰)
```

## ⭐ Système de favoris

- ❤️ Ajout/retrait rapide depuis le lecteur
- 📂 Onglet dédié dans la section Playlists
- 🔄 Synchronisation automatique
- 💾 Sauvegarde persistante


## 🎧 Playlists


- ➕ Créer des playlists personnalisées
- ✏️ Modifier les playlists existantes
- 🗑️ Supprimer des playlists
- ➕➖ Ajouter ou retirer des titres
- ▶️ Lecture complète ou par titre
- 🎯 Gestion intuitive par dialogues


## 🎨 Interface utilisateur


### 🎨 Design
- 🏗️ **Jetpack Compose** - UI déclarative moderne
- 🎨 **Material3** - Design system Google
- 🌓 Thème **sombre/clair** personnalisable
- 📱 Navigation par **onglets** intuitive
- 📄 Fiches détaillées **albums/artistes**

### ✨ Animations
- 📊 Barres audio animées pendant la lecture
- 🏷️ Badge "En lecture" / "En pause"
- 🎯 Mise en évidence du titre actuel
- 🔄 Transitions fluides entre les écrans
- 💫 Effets de chargement élégants

### 🎭 États visuels
kotlin
▶️ En lecture    → Animation + Badge coloré
⏸️ En pause      → Badge grisé
⏭️ File d'attente → Numérotation + flèches


## 🖼️ Images et pochettes

- 🎨 Récupération automatique des pochettes d'albums
- 👤 Photos d'artistes depuis services externes
- 💾 Cache local pour performances optimales
- 🖼️ Affichage dans lecteur et fiches détaillées
- ⚡ Chargement asynchrone avec Coil


## ⚙️ Paramètres

- 🌓 Changement de thème (sombre/clair)
- 🔄 Force le scan de la bibliothèque
- 🔐 Gestion des permissions
- ⚙️ Accès rapide aux réglages système


## 🛠️ Technologies

<div align="center">

| Catégorie | Technologies |
|-----------|-------------|
| **Langage** | ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white) |
| **UI** | ![Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white) ![Material3](https://img.shields.io/badge/Material%203-757575?style=for-the-badge&logo=material-design&logoColor=white) |
| **Async** | ![Coroutines](https://img.shields.io/badge/Coroutines-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white) |
| **Audio** | ![MediaPlayer](https://img.shields.io/badge/MediaPlayer-3DDC84?style=for-the-badge&logo=android&logoColor=white) |
| **Images** | ![Coil](https://img.shields.io/badge/Coil-FF6F00?style=for-the-badge) |
| **Cache** | ![SharedPreferences](https://img.shields.io/badge/SharedPreferences-3DDC84?style=for-the-badge&logo=android&logoColor=white) |

</div>

---

## 📱 Captures d'écran

<div align="center">

> 📸 *Captures d'écran à venir...*

</div>

---

## 📥 Installation

### Prérequis
- Android 8.0+ (API 26)
- 30 MB d'espace libre
- Permission d'accès aux fichiers audio
```bash
### Installation
# Clone le repo
git clone https://github.com/Kevinwg02/Kenemi-Music.git
```
# Ouvre avec Android Studio
# Build & Run

> 🚀 *APK disponible sur les Releases*

---


## 🤝 Contribution

<div align="center">

**Des bugs ? Des idées ? N'hésite pas à contribuer !**

[![Issues](https://img.shields.io/badge/Issues-Signaler%20un%20bug-red?style=for-the-badge)](https://github.com/kevinwg02/kenemimusic/issues)
[![Pull Requests](https://img.shields.io/badge/PR-Contribuer-green?style=for-the-badge)](https://github.com/kevinwg02/kenemimusic/pulls)
[![Discussions](https://img.shields.io/badge/Discussions-Échanger-blue?style=for-the-badge)](https://github.com/kevinwg02/kenemimusic/discussions)

</div>

### Comment contribuer ?
1. 🍴 Fork le projet
2. 🌿 Crée une branche (`git checkout -b feature/amazing-feature`)
3. 💾 Commit tes changements (`git commit -m 'Add amazing feature'`)
4. 📤 Push sur la branche (`git push origin feature/amazing-feature`)
5. 🎉 Ouvre une Pull Request

---

## 📄 Licence

Ce projet est sous licence **MIT** - voir le fichier [LICENSE](LICENSE) pour plus de détails.

---

<div align="center">

### ⭐ N'oublie pas de star le projet si tu l'aimes !

**KenemiMusic** - Simple, rapide, sans pub. 🎵

</div>