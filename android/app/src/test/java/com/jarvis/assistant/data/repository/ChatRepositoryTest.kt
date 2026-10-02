package com.jarvis.assistant.data.repository

import android.content.SharedPreferences
import com.jarvis.assistant.data.api.OpenAIMessage
import com.jarvis.assistant.data.error.ApiKeyInvalidException
import com.jarvis.assistant.data.error.ApiKeyNotConfiguredException
import com.jarvis.assistant.data.error.NetworkTimeoutException
import com.jarvis.assistant.data.error.NoInternetException
import com.jarvis.assistant.data.error.RateLimitException
import com.jarvis.assistant.data.error.ServerErrorException
import com.jarvis.assistant.data.local.MessageDao
import com.jarvis.assistant.data.model.AIProvider
import com.jarvis.assistant.data.model.MessageRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Unit tests para ChatRepository.
 * Cubren la validación de API keys, el guardado de mensajes y el mapeo de errores de red/HTTP.
 */
class ChatRepositoryTest {

    private lateinit var openAIService: OpenAIServiceImpl
    private lateinit var geminiService: GeminiServiceImpl
    private lateinit var messageDao: MessageDao
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var repository: ChatRepository

    @Before
    fun setUp() {
        openAIService = mock()
        geminiService = mock()
        messageDao = mock()
        prefs = mock()
        editor = mock()

        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)

        repository = ChatRepository(openAIService, geminiService, messageDao, prefs)
    }

    @Test
    fun `sendMessage falla con ApiKeyNotConfiguredException si no hay API key`() = runTest {
        // Ningún getString stubbeado devuelve null → no hay clave configurada
        val result = repository.sendMessage("Hola JARVIS")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ApiKeyNotConfiguredException)
        // El mensaje del usuario no debe guardarse si no hay clave
        verifyBlocking(messageDao, times(0)) { insertMessage(any()) }
    }

    @Test
    fun `sendMessage exitoso guarda mensaje del usuario y respuesta del asistente`() = runTest {
        whenever(prefs.getString(eq("openai_api_key"), anyOrNull())).thenReturn("sk-test")
        wheneverBlocking {
            openAIService.sendMessage(any(), any(), any())
        }.thenReturn(OpenAIMessage(role = "assistant", content = "Buenos días, señor."))

        val result = repository.sendMessage("Hola JARVIS")

        result.exceptionOrNull()?.printStackTrace()
        assertTrue("Excepción inesperada: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals(MessageRole.ASSISTANT, result.getOrNull()?.role)
        assertEquals("Buenos días, señor.", result.getOrNull()?.content)

        // Se usa el servicio de OpenAI con la clave guardada y el modelo por defecto
        verifyBlocking(openAIService) {
            sendMessage(eq("sk-test"), eq("gpt-4o"), any())
        }
        // Mensaje del usuario + respuesta del asistente
        verifyBlocking(messageDao, times(2)) { insertMessage(any()) }
    }

    @Test
    fun `sendMessage mapea HTTP 401 a ApiKeyInvalidException`() = runTest {
        whenever(prefs.getString(eq("openai_api_key"), anyOrNull())).thenReturn("sk-invalida")
        wheneverBlocking {
            openAIService.sendMessage(any(), any(), any())
        }.thenThrow(httpException(401))

        val result = repository.sendMessage("Hola")

        val exception = result.exceptionOrNull()
        assertTrue("Excepción inesperada: $exception", exception is ApiKeyInvalidException)
    }

    @Test
    fun `sendMessage mapea HTTP 429 a RateLimitException`() = runTest {
        whenever(prefs.getString(eq("openai_api_key"), anyOrNull())).thenReturn("sk-test")
        wheneverBlocking {
            openAIService.sendMessage(any(), any(), any())
        }.thenThrow(httpException(429))

        val result = repository.sendMessage("Hola")

        val exception = result.exceptionOrNull()
        assertTrue("Excepción inesperada: $exception", exception is RateLimitException)
    }

    @Test
    fun `sendMessage mapea HTTP 500 a ServerErrorException`() = runTest {
        whenever(prefs.getString(eq("openai_api_key"), anyOrNull())).thenReturn("sk-test")
        wheneverBlocking {
            openAIService.sendMessage(any(), any(), any())
        }.thenThrow(httpException(503))

        val result = repository.sendMessage("Hola")

        val exception = result.exceptionOrNull()
        assertTrue("Excepción inesperada: $exception", exception is ServerErrorException)
    }

    @Test
    fun `sendMessage mapea timeout a NetworkTimeoutException`() = runTest {
        whenever(prefs.getString(eq("openai_api_key"), anyOrNull())).thenReturn("sk-test")
        wheneverBlocking {
            openAIService.sendMessage(any(), any(), any())
        }.thenAnswer { throw SocketTimeoutException("timeout") }

        val result = repository.sendMessage("Hola")

        val exception = result.exceptionOrNull()
        assertTrue("Excepción inesperada: $exception", exception is NetworkTimeoutException)
    }

    @Test
    fun `sendMessage mapea IOException a NoInternetException`() = runTest {
        whenever(prefs.getString(eq("openai_api_key"), anyOrNull())).thenReturn("sk-test")
        wheneverBlocking {
            openAIService.sendMessage(any(), any(), any())
        }.thenAnswer { throw IOException("network down") }

        val result = repository.sendMessage("Hola")

        val exception = result.exceptionOrNull()
        assertTrue("Excepción inesperada: $exception", exception is NoInternetException)
    }

    @Test
    fun `getSelectedProvider hace fallback a OPENAI con valor inválido guardado`() {
        whenever(prefs.getString(anyOrNull(), anyOrNull())).thenReturn("PROVIDER_INEXISTENTE")

        assertEquals(AIProvider.OPENAI, repository.getSelectedProvider())
    }

    @Test
    fun `getSelectedProvider devuelve el provider guardado`() {
        whenever(prefs.getString(anyOrNull(), anyOrNull())).thenReturn(AIProvider.GEMINI.name)

        assertEquals(AIProvider.GEMINI, repository.getSelectedProvider())
    }

    @Test
    fun `isSetupComplete es false sin API key y true con ella`() {
        whenever(prefs.getString(eq("openai_api_key"), anyOrNull())).thenReturn(null)
        assertEquals(false, repository.isSetupComplete())

        whenever(prefs.getString(eq("openai_api_key"), anyOrNull())).thenReturn("sk-test")
        assertEquals(true, repository.isSetupComplete())
    }

    @Test
    fun `sendMessageStream lanza ApiKeyNotConfiguredException al recolectar sin API key`() = runTest {
        val exception = runCatching {
            repository.sendMessageStream("Hola").first()
        }.exceptionOrNull()

        assertTrue(exception is ApiKeyNotConfiguredException)
        verifyBlocking(messageDao, times(0)) { insertMessage(any()) }
    }

    @Test
    fun `saveApiKey persiste la clave cifrada del provider`() {
        repository.saveApiKey(AIProvider.OPENAI, "sk-nueva")

        verify(editor).putString(eq("openai_api_key"), eq("sk-nueva"))
        verify(editor).apply()
    }

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, ResponseBody.create(null, "")))
}
