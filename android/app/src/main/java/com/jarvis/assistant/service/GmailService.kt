package com.jarvis.assistant.service

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GmailService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(
            Scope(GmailScopes.GMAIL_SEND),
            Scope(GmailScopes.GMAIL_READONLY),
            Scope(GmailScopes.GMAIL_MODIFY)
        )
        .build()

    fun getSignInIntent(): Intent {
        val client = GoogleSignIn.getClient(context, gso)
        return client.signInIntent
    }

    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun signOut(onComplete: () -> Unit) {
        val client = GoogleSignIn.getClient(context, gso)
        client.signOut().addOnCompleteListener { onComplete() }
    }

    suspend fun getGmailClient(account: GoogleSignInAccount): Gmail = withContext(Dispatchers.IO) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(GmailScopes.GMAIL_SEND, GmailScopes.GMAIL_READONLY, GmailScopes.GMAIL_MODIFY)
        )
        credential.selectedAccount = account.account

        Gmail.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
        .setApplicationName("JARVIS Assistant")
        .build()
    }

    suspend fun readEmails(limit: Int = 5, query: String = "is:unread"): String = withContext(Dispatchers.IO) {
        val account = getLastSignedInAccount() ?: return@withContext "Error: No hay cuenta de Google conectada."
        
        try {
            val service = getGmailClient(account)
            val response = service.users().messages().list("me")
                .setQ(query)
                .setMaxResults(limit.toLong())
                .execute()

            val messages = response.messages
            if (messages.isNullOrEmpty()) {
                return@withContext "No se encontraron correos que coincidan con la búsqueda: $query"
            }

            val resultBuilder = StringBuilder("He encontrado ${messages.size} correos relevantes:\n")
            
            for (msg in messages) {
                val fullMsg = service.users().messages().get("me", msg.id).execute()
                val snippet = fullMsg.snippet
                val headers = fullMsg.payload.headers
                val from = headers.find { it.name == "From" }?.value ?: "Desconocido"
                val subject = headers.find { it.name == "Subject" }?.value ?: "(Sin asunto)"
                
                resultBuilder.append("- De: $from | Asunto: $subject\n")
            }

            resultBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            "Error al leer correos: ${e.localizedMessage}"
        }
    }

    suspend fun createDraft(recipient: String, subject: String, body: String): String = withContext(Dispatchers.IO) {
        val account = getLastSignedInAccount() ?: return@withContext "Error: No hay cuenta de Google conectada."
        
        try {
            val service = getGmailClient(account)
            
            val emailContent = "To: $recipient\n" +
                    "Subject: $subject\n\n" +
                    body

            val bytes = emailContent.toByteArray(java.nio.charset.StandardCharsets.UTF_8)
            val encodedEmail = java.util.Base64.getUrlEncoder().encodeToString(bytes)
            
            val message = com.google.api.services.gmail.model.Message()
            message.raw = encodedEmail

            val draft = com.google.api.services.gmail.model.Draft()
            draft.message = message

            service.users().drafts().create("me", draft).execute()
            "Borrador creado exitosamente para $recipient."
        } catch (e: Exception) {
            e.printStackTrace()
            "Error al crear borrador: ${e.message}"
        }
    }

    suspend fun sendEmail(recipient: String, subject: String, body: String): String = withContext(Dispatchers.IO) {
        val account = getLastSignedInAccount() ?: return@withContext "Error: No hay cuenta de Google conectada."

        try {
            val service = getGmailClient(account)
            
            val emailContent = "To: $recipient\n" +
                    "Subject: $subject\n\n" +
                    body

            val bytes = emailContent.toByteArray(java.nio.charset.StandardCharsets.UTF_8)
            val encodedEmail = java.util.Base64.getUrlEncoder().encodeToString(bytes)
            
            val message = com.google.api.services.gmail.model.Message()
            message.raw = encodedEmail

            service.users().messages().send("me", message).execute()
            "Correo enviado exitosamente a $recipient."
        } catch (e: Exception) {
            e.printStackTrace()
            "Error al enviar correo: ${e.message}"
        }
    }
}
