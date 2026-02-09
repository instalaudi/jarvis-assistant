package com.jarvis.assistant.data.model

enum class JarvisState {
    IDLE,           // Waiting for "Hey JARVIS" (Wake Word Service Active)
    LISTENING,      // Listening to User Command (Voice Recognition Active)
    PROCESSING,     // Thinking/Executing Tools (Mic Closed)
    SPEAKING,       // TTS Active (Mic Closed)
    COOLDOWN        // Conversation Window (Mic Active for Follow-up)
}
