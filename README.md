# Mac Remotely

A lightweight local file server built in Java that lets you access, browse, upload, and download files from your Mac using any browser on your phone or another device — no app installation required.

---

## Features

- **Browser-based terminal UI** — dark themed, mobile-friendly terminal interface
- **Full filesystem navigation** — browse your entire Mac from any browser
- **File download** — download any file directly to your device
- **File upload** — upload files from your phone to your Mac
- **File delete** — remove files remotely
- **Tab autocomplete** — press Tab to autocomplete file and folder names, just like a real terminal
- **Voice commands** — speak commands like "cd Desktop" or "ls" using the mic button
- **Remote access** — expose your server to the internet using Ngrok (free)
- **Request logging** — logs IP address of every incoming request to the console
- **Path traversal protection** — prevents access outside the root directory

---

## Tech Stack

| Layer      | Technology                        |
|------------|-----------------------------------|
| Backend    | Java 17                           |
| HTTP Server| Javalin 6.1.3                     |
| Frontend   | Plain HTML, CSS, JavaScript       |
| Speech     | Web Speech API (browser built-in) |
| Build Tool | Gradle                            |
| Remote Access | Ngrok (free tier)              |

---

## Project Structure

```
FileServerSystem/
├── src/
│   └── main/
│       ├── java/com/fileServer/FileServer/
│       │   └── FileServerApplication.java   # Server + all API handlers
│       └── resources/
│           └── public/
│               └── index.html               # Terminal web UI
├── build.gradle
└── settings.gradle
```

---

## API Endpoints

| Method   | Endpoint        | Description                          |
|----------|-----------------|--------------------------------------|
| GET      | `/api/files`    | List files and folders at given path |
| GET      | `/api/download` | Download a file                      |
| POST     | `/api/upload`   | Upload one or more files             |
| DELETE   | `/api/delete`   | Delete a file                        |

All endpoints accept a `path` query parameter relative to the root directory.

---

## Setup & Run

### Prerequisites
- Java 17+
- Gradle

### Run from Eclipse
1. Import the project as a Gradle project in Eclipse
2. Right-click project → **Gradle → Refresh Gradle Project**
3. Run `FileServerApplication.java`

### Run from Terminal
```bash
cd FileServerSystem
./gradlew shadowJar
java -jar build/libs/file-server.jar
```

### Access
On startup, the server prints your local IP:
```
=== File Server Running ===
Serving: /
Local   -> http://localhost:8080
Network -> http://192.168.x.x:8080
```

Open the **Network URL** in any browser on your phone (both devices must be on the same Wi-Fi).

---

## Remote Access via Ngrok

To access your Mac from outside your home network:

1. Install Ngrok:
```bash
brew install ngrok
```

2. Sign up free at [ngrok.com](https://ngrok.com) and add your auth token:
```bash
ngrok config add-authtoken YOUR_TOKEN
```

3. Start the tunnel (keep Mac awake with lid closed):
```bash
caffeinate -s ngrok http 8080
```

4. Use the generated URL (e.g. `https://abc123.ngrok-free.app`) from anywhere.

---

## Terminal Commands

| Command              | Description                        |
|----------------------|------------------------------------|
| `ls`                 | List files and folders             |
| `cd <folder>`        | Navigate into a folder             |
| `cd ..`              | Go up one level                    |
| `pwd`                | Show current path                  |
| `download <file>`    | Download a file to your device     |
| `upload`             | Upload a file to current folder    |
| `clear`              | Clear the terminal                 |
| `help`               | Show all commands                  |

**Tab autocomplete:** type a few letters and press Tab to autocomplete. Press Tab again to cycle through multiple matches.

**Voice commands:** tap the 🎤 mic button and speak any command. Requires internet and microphone permission in browser.

---

## Security

- All file paths are resolved using `getCanonicalFile()` and validated to prevent path traversal attacks
- No authentication (designed for personal local use only)
- Do not expose to public internet without adding authentication

---

## Author

**Harsimarpreet Singh**  
Email: harsimarpreet.singh05@gmail.com
