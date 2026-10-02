# Contribuir a J.A.R.V.I.S. 🤖

¡Gracias por querer mejorar JARVIS! Esta guía resume todo lo necesario para trabajar en el proyecto.

## 📱 Requisitos

- **Android Studio** Ladybug (2024.2.1) o superior
- **JDK 17**
- **Android SDK 35** (compileSdk/targetSdk)
- Dispositivo o emulador con **Android 8.0+** (API 26)

## 🚀 Setup rápido

```bash
git clone https://github.com/Instalaudi/jarvis-assistant.git
# Abre la carpeta android/ en Android Studio (File > Open)
# Sincroniza Gradle y ejecuta en tu dispositivo
```

La API key de OpenAI/Gemini se configura dentro de la app, en la pantalla de Setup
(se guarda cifrada con `EncryptedSharedPreferences`; nunca la subas al repo).

## 🧪 Tests

Los tests unitarios corren en la JVM (sin dispositivo):

```bash
cd android
./gradlew testDebugUnitTest
```

- `ChatRepositoryTest` — validación de API keys, guardado de mensajes y mapeo de errores HTTP/red.
- `MainViewModelTest` — máquina de estados (IDLE/LISTENING/PROCESSING) y flujo de envío.

Añade tests para cualquier lógica nueva de `data/` o `viewmodel/`. El CI (`.github/workflows/android-ci.yml`)
compila y ejecuta los tests en cada push y Pull Request.

## 🔀 Pull Requests

1. Crea una rama descriptiva: `fix/wake-word-anr`, `feat/tts-streaming`.
2. Mantén los cambios enfocados; evita mezclar refactors con features.
3. Asegúrate de que el CI pasa (tests + build debug).
4. Describe en el PR: qué cambia, por qué y cómo lo probaste (dispositivo/modelo Android).

## 📦 Releases automáticos

Al hacer push de un tag `v*` (ej. `v2.3.0`), el workflow `release.yml` compila y publica el APK
en GitHub Releases.

- **Con firma**: configura los secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` y
  `KEY_PASSWORD` (Settings → Secrets and variables → Actions). Genera el base64 con:
  `base64 -w0 release.keystore`
- **Sin firma**: se publica un APK **debug** (instalable, pero no apto para distribución).

> ⚠️ Nunca subas el keystore ni `keystore.properties` al repositorio (ya están en `.gitignore`).

## 🎨 Convenciones

- **Kotlin oficial** (`kotlin.code.style=official`), arquitectura MVVM + Hilt existente.
- Textos de UI y prompts en **español**; código e identificadores en inglés.
- No introduzcas dependencias nuevas sin comentarlo antes en un Issue.
