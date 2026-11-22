# FISHURAI 🐟🐱⚔️

## Description 📖

"FISHURAI" puts your skills to the ultimate test! 🎣 Compete against friends or AI opponents in a thrilling race to catch
the most fish. Sharpen your reflexes, develop your fishing strategy, and outmaneuver your rivals to claim the
top spot. 🏆 Are you ready to prove your worth as the supreme Samurai Cat angler? 😼

> 🎓 This is a **school project** and the **first game we ever made as a team**: three of us, two teammates and me.

## Table of Contents 📑

* [Description 📖](#description-)
* [Features 😸](#features-)
* [Requirements 🐱](#requirements-)
* [Installation 😻](#installation-)
* [Game Controls 🎮](#game-controls-)
* [Multiplayer 👯](#multiplayer-)
* [Gradle 🐘](#gradle-)
* [LibGDX 🧰](#libgdx-)
* [Platforms 💻](#platforms-)
* [Technologies 🔧](#technologies-)
* [Project status ✅](#project-status-)

## Features 😸

* 🎯 **Genre:** Arcade, Multiplayer Action
* 👥 **Players:** 1-4 players
* 🌐 **Network mode:** Online competition where cats try to catch as many fish as possible within a limited time (one round lasts 120 seconds ⏱️).
* ⚙️ **Mechanics implemented:** Physics engine integration, Artificial intelligence (AI) implementation, Level design and implementation, Animation implementation.
* 📜 **Story:** In historical Japan, cat-samurai hold fishing competitions. A large pond is full of rare fish, but they are not easy to catch. Players, taking on the role of cat-samurai, compete with each other to become the best fisherman. 🎏

| Catch | Points |
|-------|--------|
| 🐟 Common fish | +100 |
| 🐠 Uncommon fish | +250 |
| 🦈 Rare fish | +500 |
| 🥾 Trash | -100 |

## Requirements 🐱

To just **play** 🎮 the game you need nothing: the Windows and Linux packages include their own Java runtime. ☕

To **build and run from source** 🛠 you need:

* Java 23 (JDK)
* Gradle (the Gradle wrapper is included, so a separate install is not required)
* IntelliJ IDEA (optional, but recommended)

## Installation 😻

> **NB!** You may have to wait around 5-10 minutes for the server to start. The game server runs on a free Render plan and goes to sleep when nobody is playing. The game keeps retrying to connect, so just leave it open. 😴➡️😺

### 🎲 Option 1: itch.io

Download the game from the itch.io page

### 📦 Option 2: GitHub Releases

Open the [Releases](../../releases) page and download the file for your system from **Assets**:

| File | For | Notes |
|------|-----|-------|
| 🪟 `FISHURAI-<version>-windows-setup.exe` | Windows | Installer with Start menu and desktop shortcuts. |
| 🪟 `FISHURAI-<version>-windows-x64.zip` | Windows | Portable version. Unzip it fully, then run the `.exe` inside the folder. |
| 🐧 `FISHURAI-<version>-linux-amd64.deb` | Debian / Ubuntu / Mint | See the commands below. |
| 🐧 `FISHURAI-<version>-linux-x64-portable.zip` | Other Linux (x64) | Unzip and run the executable in `bin/`. |
| ☕ `FISHURAI-<version>.jar` | Any system with **Java 23+** | Run with `java -jar FISHURAI-<version>.jar`. |

**Windows:** run the installer (or unzip the portable version and start the `.exe`). If Windows SmartScreen shows "Windows protected your PC", click **More info → Run anyway**. The game is not code-signed. 🔏

**Linux (.deb):**

```bash
sudo apt install ./FISHURAI-<version>-linux-amd64.deb
/opt/fishurai/bin/fishurai
```

To remove it: `sudo apt remove fishurai`.

### 🧑‍💻 Option 3: Build from source

1. Clone the repository.

   Open your terminal or command prompt (or the terminal in your IDE) and use `git clone`:

   ```
   git clone <REPOSITORY URL>
   ```

2. Open the `game` folder in one window and the `server` folder in another.
3. Run the server: `ee/taltech/game/server/GameServer.java` >> RUN (or `./gradlew run` in the `server` folder).
   It listens on port 8080 unless the `PORT` environment variable is set.
4. Run the game: open Gradle in the `game` folder and choose `application` >> `run` (or `./gradlew lwjgl3:run`).
   To connect to your local server instead of the public one, set the address with the `FISHURAI_SERVER`
   environment variable or the `fishurai.server` system property:

   ```
   FISHURAI_SERVER=ws://localhost:8080
   ```

   Without it, the game connects to the public server on Render.

## Game Controls 🎮

You can move your character using arrow keys on the keyboard. ⌨️

| Key | Action |
|-----|--------|
| ⇧ | 🐾 Forward |
| ⇩ | 🐾 Back |
| ⇨ | 🐾 Right |
| ⇦ | 🐾 Left |
| SPACE | 🎣 Catch fish |
| ENTER | 🚀 Start game |
| T | ⚙️ Open options |
| ESC | 🚪 End the game |

## Multiplayer 👯

To play with friends, everybody just starts the game and joins the same room. 🤝

To run multiplayer / a few games at once on one computer (for testing in IntelliJ), check these settings:

* RUN >> right click
* Edit configurations
* Modify options
* Allow multiple instances

## Gradle 🐘

This project uses [Gradle](https://gradle.org/) to manage dependencies. The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands. Useful Gradle tasks and flags:

* `--continue`: when using this flag, errors will not stop the tasks from running.
* `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
* `--offline`: when using this flag, cached dependency archives will be used.
* `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
* `build`: builds sources and archives of every project.
* `cleanEclipse`: removes Eclipse project data.
* `cleanIdea`: removes IntelliJ project data.
* `clean`: removes `build` folders, which store compiled classes and built archives.
* `eclipse`: generates Eclipse project data.
* `idea`: generates IntelliJ project data.
* `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
* `lwjgl3:run`: starts the application.
* `lwjgl3:packageWinX64` / `lwjgl3:packageLinuxX64`: builds a standalone package with its own Java runtime (Construo).
* `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project. For example, `core:clean` removes `build` folder only from the `core` project.

## LibGDX 🧰

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

## Platforms 💻

* `core`: Main module with the application logic shared by all platforms.
* `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.
* `server`: Separate WebSocket game server (hosted on Render).

## Technologies 🔧

**🎮 Game client**

* Java 23
* [libGDX](https://libgdx.com/) (rendering, input, audio)
* Box2D and [box2dlights](https://github.com/libgdx/box2dlights)
* [gdx-ai](https://github.com/libgdx/gdx-ai)
* FreeType (fonts)

**🌐 Game server and networking**

* [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket)
* [Jackson](https://github.com/FasterXML/jackson) (JSON messages)
* SLF4J (logging)
* [Render](https://render.com/) (hosting)

**📦 Build and release**

* [Gradle](https://gradle.org/)
* [Construo](https://github.com/fourlastor-playground/construo) and `jlink` (packages the game with its own Java runtime)
* [Inno Setup](https://jrsoftware.org/isinfo.php) (Windows installer)
* GitHub Actions (automatic releases)
* [Lombok](https://projectlombok.org/)
