# J.A.R.V.I.S. - Android 15 Native App

<div align="center">

![JARVIS Logo](app/src/main/res/drawable/ic_launcher_foreground.xml)

**Just A Rather Very Intelligent System**

*Asistente de IA personal para Android 15*

</div>

---

## 📱 Requisitos

- **Android Studio**: Ladybug (2024.2.1) o superior
- **JDK**: 17+
- **Dispositivo**: Android 8.0+ (API 26), optimizado para Android 15 (API 35)
- **API Key**: OpenAI API key válida

## 🚀 Instalación

### 1. Abrir en Android Studio

```bash
# Clona o copia el proyecto
# Abre Android Studio > File > Open > selecciona la carpeta android/
```

### 2. Sincronizar Gradle

Android Studio debería sincronizar automáticamente. Si no:
- `File > Sync Project with Gradle Files`

### 3. Configurar API Key

La API key se configura en la app durante el primer uso (pantalla de Setup).

### 4. Ejecutar

- Conecta tu Xiaomi Poco X7 Pro via USB
- Habilita Depuración USB en Opciones de Desarrollador
- Click en "Run" (▶️) en Android Studio

---

## ✨ Features

### Core
- 🤖 **IA con OpenAI GPT-4o** - Respuestas inteligentes y naturales
- 🎤 **Reconocimiento de Voz** - Comandos por voz en español
- 🔊 **Text-to-Speech** - Respuestas habladas con voz natural
- 🎨 **UI Holográfica** - Interfaz estilo Iron Man

### Integraciones
- 📧 **Gmail** - Redacta y envía correos por voz
- 💬 **WhatsApp** - Lee notificaciones de mensajes
- 🎵 **Música** - Controla tu reproductor

### Visual
- ✨ Sistema de partículas
- 🌐 Grid HUD animado
- ⚡ Animaciones de circuitos
- 🔵 Orbe pulsante interactivo

---

## 📁 Estructura del Proyecto

```
app/src/main/java/com/jarvis/assistant/
├── MainActivity.kt
├── JarvisApplication.kt
├── ui/
│   ├── theme/           # Colores, tipografía, tema
│   ├── screens/         # Setup, Main, Settings
│   ├── components/      # VoiceButton, ChatBubble, etc.
│   ├── effects/         # Partículas, waveform, etc.
│   ├── viewmodel/       # ViewModels
│   └── navigation/      # Navegación Compose
├── data/
│   ├── api/             # OpenAI API
│   ├── model/           # Data classes
│   └── repository/      # Repositorios
├── service/
│   ├── VoiceRecognitionService.kt
│   ├── TextToSpeechService.kt
│   ├── NotificationListenerService.kt
│   ├── MediaControlService.kt
│   └── GmailService.kt
├── widget/              # Widget de home screen
└── receiver/            # Boot receiver
```

---

## 🔧 Configuración Xiaomi/HyperOS

Para que JARVIS funcione correctamente en tu **Poco X7 Pro**:

### 1. Autostart
`Configuración > Apps > Gestionar apps > JARVIS > Autostart > Activar`

### 2. Ahorro de Batería
`Configuración > Batería > JARVIS > Sin restricciones`

### 3. Acceso a Notificaciones (para WhatsApp)
`Configuración > Notificaciones > Acceso a notificaciones > JARVIS > Activar`

---

## 🎤 Comandos de Voz

| Comando | Acción |
|---------|--------|
| "JARVIS, pon música" | Abre reproductor |
| "Pausa la música" | Pausa reproducción |
| "Siguiente canción" | Siguiente track |
| "Envía un correo a..." | Abre Gmail para componer |
| "¿Qué hora es?" | Responde la hora |
| Cualquier pregunta | Responde con IA |

---

## 📦 Dependencias Principales

| Librería | Uso |
|----------|-----|
| Jetpack Compose | UI moderna |
| Hilt | Inyección de dependencias |
| Retrofit | API calls |
| Room | Base de datos local |
| Lottie | Animaciones |

---

## 🔐 Seguridad

- API key almacenada con `EncryptedSharedPreferences`
- Comunicación HTTPS con OpenAI
- Permisos solicitados en runtime

---

## 📄 Licencia

Proyecto personal - Uso educativo

---

<div align="center">

**"Todos los sistemas están operativos, señor."**

</div>
