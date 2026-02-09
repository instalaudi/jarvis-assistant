package com.jarvis.assistant.data.error

/**
 * Custom exceptions for J.A.R.V.I.S. app with Spanish error messages
 */

sealed class JarvisException(
    override val message: String,
    val userMessage: String
) : Exception(message)

// ============ API Key Exceptions ============

class ApiKeyNotConfiguredException : JarvisException(
    message = "API key not configured",
    userMessage = "API key no configurada. Por favor, configure su clave de OpenAI en Ajustes."
)

class ApiKeyInvalidException : JarvisException(
    message = "API key invalid (401)",
    userMessage = "La API key es inválida. Por favor, verifique su clave de OpenAI."
)

// ============ Rate Limit Exceptions ============

class RateLimitException : JarvisException(
    message = "Rate limit exceeded (429)",
    userMessage = "Ha alcanzado el límite de solicitudes. Por favor, espere un momento antes de intentar de nuevo."
)

class QuotaExceededException : JarvisException(
    message = "Quota exceeded",
    userMessage = "Ha excedido su cuota de uso de OpenAI. Verifique su plan de suscripción."
)

// ============ Network Exceptions ============

class NoInternetException : JarvisException(
    message = "No internet connection",
    userMessage = "Sin conexión a Internet. Por favor, verifique su conexión."
)

class NetworkTimeoutException : JarvisException(
    message = "Network timeout",
    userMessage = "La solicitud tardó demasiado. Por favor, intente de nuevo."
)

class ServerErrorException(statusCode: Int) : JarvisException(
    message = "Server error ($statusCode)",
    userMessage = "Error del servidor OpenAI. Por favor, intente más tarde."
)

// ============ Model Exceptions ============

class ModelNotAvailableException : JarvisException(
    message = "Model not available",
    userMessage = "El modelo GPT-4o no está disponible actualmente. Intente más tarde."
)

class ContextTooLongException : JarvisException(
    message = "Context too long",
    userMessage = "La conversación es muy larga. Se limpió el historial para continuar."
)

// ============ Generic Exception ============

class UnknownApiException(originalMessage: String?) : JarvisException(
    message = "Unknown API error: $originalMessage",
    userMessage = "Ha ocurrido un error inesperado. Por favor, intente de nuevo."
)
