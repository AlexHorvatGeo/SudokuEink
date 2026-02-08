# Sudoku E-ink

![Release](https://img.shields.io/github/v/release/ktacrack/sudokueink)
![License](https://img.shields.io/badge/license-MIT-blue)
![Downloads](https://img.shields.io/github/downloads/ktacrack/sudokueink/total)
![Platform](https://img.shields.io/badge/platform-Android-green)
![Language](https://img.shields.io/badge/language-Kotlin-purple)

**🌍 Languages / Idiomes:** [Català](README.md) | [English](README.en.md)

---

⚠️ **Repositori oficial**

Aquest és l'únic repositori oficial de Sudoku E-ink. Només descarrega l'aplicació des d'aquí o des de les releases verificades. Qualsevol fork o còpia pot contenir modificacions no autoritzades.

---

🔒 **Autor:** ktacrack  
📄 **Llicència:** MIT License  
🔗 **Official URL:** [https://github.com/ktacrack/SudokuEink](https://github.com/ktacrack/SudokuEink)

---

## Característiques

### 🎮 Gameplay
- **3 nivells de dificultat:** Fàcil, Mitjà i Difícil
- **Generador de Sudokus intel·ligent** amb algoritmes optimitzats
- **Sistema de pistes** limitat per dificultat (5/3/1)
- **Mode notes** per anotar possibles números
- **Desfer moviments** amb historial il·limitat
- **Reiniciar partida** en qualsevol moment

### ⏱️ Timer i Gestió de Partides
- **Cronòmetre amb controls:** pausa, reprendre i reiniciar temps
- **Partides guardades independents** per cada nivell de dificultat
- **Autoguardat** en sortir de la partida
- **Recuperació automàtica** de partides en curs

### ✏️ Reconeixement d'Escriptura
- **Mode llapis global** per reconeixement ràpid
- **Reconeixement de dígits escrits a mà** amb TensorFlow Lite
- **Canvas de dibuix escalat** adaptatiu a tot tipus de pantalles
- **Integració perfecta** amb el mode notes

### 📊 Estadístiques
- **Partides completades** per dificultat
- **Millor temps** registrat per cada nivell
- **Històric persistent** de resultats

### 🎨 Optimitzat per E-ink
- **Disseny alt contrast** específic per pantalles de tinta electrònica
- **Diferenciació visual clara:** números fixos (negre + negreta) vs usuari (gris + light)
- **Colors optimitzats** per a blanc i negre
- **Fons de cel·les diferenciats** per millor llegibilitat
- **Interfície neta** sense distraccions

### 📱 Experiència Adaptativa
- **Escalat intel·ligent** per tot tipus de pantalles (telèfons, tablets)
- **Layout adaptatiu:** vertical per telèfons, horitzontal per tablets
- **Controls optimitzats** per cada mida de pantalla
- **Botons escalats** proporcionals al dispositiu

### 🌍 Multiidioma
- **Català** (CA)
- **Español** (ES)
- **English** (EN)

## Instal·lació

### Opció 1: Des de Releases
1. Descarrega l'APK des de [Releases](https://github.com/ktacrack/SudokuEink/releases)
2. Instal·la l'APK al teu dispositiu e-ink (Boox, Kindle, etc.)
3. Obre l'aplicació i comença a jugar!

### Opció 2: Compilar des del codi
1. Clona el repositori
```
bash
git clone [https://github.com/ktacrack/sudokueink.git](https://github.com/ktacrack/sudokueink.git)
cd sudokueink
```
2. Obre el projecte amb Android Studio
3. Compila i instal·la al teu dispositiu

## Requisits
- Android 8.0 (API 26) o superior
- Pantalla e-ink recomanada (funciona en qualsevol pantalla)
- ~15 MB d'espai lliure

## Captures de pantalla
<p>
  <img src="images/Menu_catala.png" width="200">
  <img src="images/Joc_catala.png" width="200">
  <img src="images/Stats_catala.png" width="200">
</p>

## Desenvolupament
**Tecnologies utilitzades:**
**Llenguatge:** Kotlin
**Framework UI:** Jetpack Compose
**IA:** TensorFlow Lite (generació de Sudokus)
**SDK mínima:** Android 26 (Oreo)
**IDE recomanat:** Android Studio

## Estructura del projecte:
```
sudoku-eink/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/ktacrack/sudokueink/
│           │   ├── ui.theme/
│           │   │   ├── Color.kt
│           │   │   ├── Theme.kt
│           │   │   └── Type.kt
│           │   ├── MainActivity.kt
│           │   ├── MainScreen.kt
│           │   ├── GameScreen.kt
│           │   ├── GameState.kt
│           │   ├── StatisticsScreen.kt
│           │   ├── Statistics.kt
│           │   ├── StatisticsManager.kt
│           │   ├── SudokuGenerator.kt
│           │   ├── SudokuGame.kt
│           │   ├── Strings.kt
│           │   ├── Navigation.kt
│           │   ├── DrawingCanvas.kt
│           │   ├── DigitRecognizer.kt
│           │   ├── AdaptiveSizes.kt
│           │   ├── EinkOptimizations.kt
│           │   └── ThemeManager.kt
│           ├── res/
│           │   └── mipmap/
│           │       ├── ic_launcher.png
│           │       └── ic_launcher_round.png
│           ├── assets/
│           │   └── mnist.tflite
│           └── AndroidManifest.xml
├── gradle/
├── images/
│   ├── Menu_catala.png
│   ├── Menu_english.png
│   ├── Joc_catala.png
│   ├── Joc_english.png
│   ├── Stats_catala.png
│   └── Stats_english.png
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
├── LICENSE
├── CHANGELOG.md
├── README.md
└── README.en.md

```

## Com jugar
1. **Selecciona la dificultat:** Fàcil, Mitjà o Difícil
2. **Omple la graella:** Toca una casella buida i escriu el número a mà o selecciona un número
3. **Completa el Sudoku:** Quan s'omple correctament, guanya!
4. **Revisa les estadístiques:** Consulta el teu progrés i millor temps

## Contribucions
Les contribucions són benvingudes! Si vols millorar l'aplicació:
1. Fes un fork del repositori
2. Crea una branca per a la teva funcionalitat (git checkout -b feature/nova-funcio)
3. Fes commit dels canvis (git commit -m 'Afegeix nova funcionalitat')
4. Puja els canvis (git push origin feature/nova-funcio)
5. Obre un Pull Request

## Llicència
Aquest projecte està llicenciat sota la llicència MIT. Consulta el fitxer LICENSE per més detalls.
Ets lliure d'usar, modificar i distribuir aquest codi, sempre mantenint l'atribució a l'autor original.

## Contacte
- **Autor:** ktacrack
- **GitHub:** @ktacrack

## Agraïments
Desenvolupat per oferir una experiència de Sudoku optimitzada per a dispositius e-ink, amb interfície clara i alt contrast.

### ⭐ Si t'ha estat útil, deixa una estrella al repositori!
