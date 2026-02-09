package com.jarvis.assistant.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jarvis.assistant.ui.components.GlowingCard
import com.jarvis.assistant.ui.effects.CircuitAnimation
import com.jarvis.assistant.ui.effects.ParticleSystem
import com.jarvis.assistant.ui.theme.*
import androidx.compose.ui.graphics.Color
import com.jarvis.assistant.ui.viewmodel.SetupViewModel
import androidx.core.app.NotificationManagerCompat
import android.provider.Settings
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import android.Manifest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel<SetupViewModel>()
) {
    val uiState by viewModel.uiState.collectAsState()
    var apiKey by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var currentStep by remember { mutableIntStateOf(0) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.isSetupComplete) {
        if (uiState.isSetupComplete) {
            onSetupComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(JarvisBlack, JarvisDeepBlue, JarvisBlack)
                )
            )
    ) {
        // Background effects
        ParticleSystem(
            modifier = Modifier.fillMaxSize(),
            particleCount = 30
        )
        CircuitAnimation(modifier = Modifier.fillMaxSize())

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo/Title
            Text(
                text = "J.A.R.V.I.S.",
                style = MaterialTheme.typography.displayMedium,
                color = JarvisCyan
            )
            Text(
                text = "Just A Rather Very Intelligent System",
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisTextSecondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Setup Card
            GlowingCard(
                modifier = Modifier.widthIn(max = 400.dp)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "step"
                ) { step ->
                    when (step) {
                        0 -> WelcomeStep(onContinue = { currentStep = 1 })
                        1 -> PermissionsStep(onContinue = { currentStep = 2 })
                        2 -> NotificationAccessStep(onContinue = { currentStep = 3 })
                        3 -> ApiKeyStep(
                            apiKey = apiKey,
                            onApiKeyChange = { apiKey = it },
                            showApiKey = showApiKey,
                            onToggleVisibility = { showApiKey = !showApiKey },
                            onContinue = { currentStep = 4 },
                            isValid = apiKey.startsWith("sk-") && apiKey.length > 20
                        )
                        4 -> UserNameStep(
                            userName = userName,
                            onUserNameChange = { userName = it },
                            onComplete = {
                                focusManager.clearFocus()
                                viewModel.completeSetup(apiKey, userName.ifBlank { "señor" })
                            },
                            isLoading = uiState.isLoading
                        )
                    }
                }
            }

            // Error message
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = JarvisError,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Step indicator
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(5) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentStep) 12.dp else 8.dp)
                            .background(
                                color = if (index <= currentStep) JarvisCyan else JarvisTextMuted,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.RocketLaunch,
            contentDescription = null,
            tint = JarvisCyan,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Bienvenido",
            style = MaterialTheme.typography.headlineSmall,
            color = JarvisTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Configure su asistente personal inteligente en solo unos pasos.",
            style = MaterialTheme.typography.bodyMedium,
            color = JarvisTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(
                containerColor = JarvisCyan,
                contentColor = JarvisBlack
            )
        ) {
            Text("Comenzar")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = JarvisCyan,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionsStep(onContinue: () -> Unit) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_CONTACTS
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = JarvisWarning,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Permisos Necesarios",
            style = MaterialTheme.typography.titleLarge,
            color = JarvisTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Para funcionar correctamente, JARVIS necesita acceso al micrófono, notificaciones y contactos.",
            style = MaterialTheme.typography.bodySmall,
            color = JarvisTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (permissionsState.allPermissionsGranted) {
            Text(
                text = "¡Permisos concedidos!",
                color = JarvisSuccess,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = JarvisBlack)
            ) {
                Text("Continuar")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        } else {
            Button(
                onClick = { permissionsState.launchMultiplePermissionRequest() },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisWarning, contentColor = JarvisBlack)
            ) {
                Text("Conceder Permisos")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.LockOpen, contentDescription = null)
            }
            
            // Allow skipping if absolutely necessary for testing (optional)
            // Spacer(modifier = Modifier.height(8.dp))
            // TextButton(onClick = onContinue) { Text("Saltar por ahora", color = JarvisTextMuted) }
        }
    }
}

@Composable
private fun NotificationAccessStep(onContinue: () -> Unit) {
    val context = LocalContext.current
    var hasAccess by remember { mutableStateOf(false) }
    
    // Check periodically or on resume logic conceptually
    fun checkAccess(): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    // Initial check
    LaunchedEffect(Unit) {
        hasAccess = checkAccess()
    }
    
    // Poll for changes when app resumes (simulated here with a refresh button or loop)
    // A simplified approach: Just a button to open settings effectively
    
    DisposableEffect(Unit) {
        val listener = object : androidx.lifecycle.LifecycleEventObserver {
            override fun onStateChanged(source: androidx.lifecycle.LifecycleOwner, event: androidx.lifecycle.Lifecycle.Event) {
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    hasAccess = checkAccess()
                }
            }
        }
        val lifecycle = (context as? androidx.activity.ComponentActivity)?.lifecycle
        lifecycle?.addObserver(listener)
        onDispose { lifecycle?.removeObserver(listener) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = if (hasAccess) JarvisSuccess else JarvisError,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Acceso a Notificaciones",
            style = MaterialTheme.typography.titleLarge,
            color = JarvisTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Para leer mensajes de WhatsApp, necesita activar el 'Acceso a notificaciones' para JARVIS.",
            style = MaterialTheme.typography.bodySmall,
            color = JarvisTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (hasAccess) {
             Text(
                text = "¡Acceso concedido!",
                color = JarvisSuccess,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = JarvisBlack)
            ) {
                Text("Continuar")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        } else {
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisError, contentColor = Color.White)
            ) {
                Text("Abrir Ajustes")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Settings, contentDescription = null)
            }
        }
    }
}

@Composable
private fun ApiKeyStep(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    showApiKey: Boolean,
    onToggleVisibility: () -> Unit,
    onContinue: () -> Unit,
    isValid: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Key,
            contentDescription = null,
            tint = JarvisGold,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "API Key de OpenAI",
            style = MaterialTheme.typography.titleLarge,
            color = JarvisTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ingrese su clave de API de OpenAI para habilitar las capacidades de IA.",
            style = MaterialTheme.typography.bodySmall,
            color = JarvisTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("API Key") },
            placeholder = { Text("sk-...") },
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showApiKey) "Hide" else "Show"
                    )
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JarvisCyan,
                unfocusedBorderColor = JarvisNavy,
                cursorColor = JarvisCyan
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onContinue,
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = JarvisCyan,
                contentColor = JarvisBlack
            )
        ) {
            Text("Continuar")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun UserNameStep(
    userName: String,
    onUserNameChange: (String) -> Unit,
    onComplete: () -> Unit,
    isLoading: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = JarvisSuccess,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "¿Cómo debo llamarle?",
            style = MaterialTheme.typography.titleLarge,
            color = JarvisTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Opcional. Por defecto usaré \"señor\" o \"señora\".",
            style = MaterialTheme.typography.bodySmall,
            color = JarvisTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = userName,
            onValueChange = onUserNameChange,
            label = { Text("Nombre (opcional)") },
            placeholder = { Text("Tony") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onComplete() }),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = JarvisCyan,
                unfocusedBorderColor = JarvisNavy,
                cursorColor = JarvisCyan
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onComplete,
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = JarvisSuccess,
                contentColor = JarvisBlack
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = JarvisBlack
                )
            } else {
                Text("Activar J.A.R.V.I.S.")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Check, contentDescription = null)
            }
        }
    }
}
