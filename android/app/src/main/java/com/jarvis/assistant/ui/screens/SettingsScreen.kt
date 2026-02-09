package com.jarvis.assistant.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jarvis.assistant.ui.components.GlowingCard
import com.jarvis.assistant.ui.effects.ParticleSystem
import com.jarvis.assistant.ui.theme.*
import com.jarvis.assistant.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(JarvisBlack, JarvisDeepBlue, JarvisBlack)
                )
            )
    ) {
        ParticleSystem(
            modifier = Modifier.fillMaxSize(),
            particleCount = 20
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = {
                    Text(
                        "Configuración",
                        color = JarvisTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = JarvisCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = JarvisBlack.copy(alpha = 0.8f)
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Voice Settings
                SettingsSection(title = "Voz") {
                    SettingsToggleItem(
                        icon = Icons.Default.Mic,
                        title = "Reconocimiento de Voz",
                        subtitle = "Permite comandos por voz",
                        isChecked = uiState.voiceEnabled,
                        onCheckedChange = { viewModel.setVoiceEnabled(it) }
                    )
                    SettingsToggleItem(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Respuestas por Voz",
                        subtitle = "JARVIS responde hablando",
                        isChecked = uiState.ttsEnabled,
                        onCheckedChange = { viewModel.setTtsEnabled(it) }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.Hearing,
                        title = "Hey JARVIS",
                        subtitle = "Escuchar siempre la palabra de activación",
                        isChecked = uiState.wakeWordEnabled,
                        onCheckedChange = { viewModel.setWakeWordEnabled(context, it) }
                    )
                    
                    // Voice Selector
                    var expanded by remember { mutableStateOf(false) }
                    val currentVoice = remember { viewModel.getPreferredVoice() ?: "Predeterminada" }
                    var selectedVoiceName by remember { mutableStateOf(currentVoice) }
                    val voices = remember { viewModel.getAvailableVoices() }

                    SettingsClickableItem(
                        icon = Icons.Default.RecordVoiceOver,
                        title = "Voz del Sistema",
                        subtitle = if (selectedVoiceName == "Predeterminada" || selectedVoiceName.isEmpty()) "Automática (JARVIS)" else selectedVoiceName,
                        onClick = { expanded = true }
                    )

                    if (expanded) {
                         AlertDialog(
                            onDismissRequest = { expanded = false },
                            title = { Text("Seleccionar Voz", color = JarvisTextPrimary) },
                            text = {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    voices.forEach { voice ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clickable {
                                                    viewModel.setPreferredVoice(voice.name)
                                                    selectedVoiceName = voice.name
                                                    expanded = false
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = (voice.name == selectedVoiceName),
                                                onClick = null,
                                                colors = RadioButtonDefaults.colors(selectedColor = JarvisCyan)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = voice.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = JarvisTextPrimary
                                                )
                                                Text(
                                                    text = "${voice.locale.displayLanguage} (${voice.locale.displayCountry})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = JarvisTextSecondary
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.previewVoice(voice.name) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Preview",
                                                    tint = JarvisCyan
                                                )
                                            }
                                        }
                                    }
                                    if (voices.isEmpty()) {
                                        Text(
                                            "No se encontraron voces en español instaladas.",
                                            color = JarvisError
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { expanded = false }) {
                                    Text("Cerrar", color = JarvisCyan)
                                }
                            },
                            containerColor = JarvisSurface,
                            textContentColor = JarvisTextPrimary
                        )
                    }
                }

                // Notifications
                SettingsSection(title = "Notificaciones") {
                    SettingsToggleItem(
                        icon = Icons.Default.Notifications,
                        title = "Leer WhatsApp",
                        subtitle = "Anuncia mensajes de WhatsApp",
                        isChecked = uiState.whatsappEnabled,
                        onCheckedChange = { viewModel.setWhatsappEnabled(it) }
                    )
                    SettingsClickableItem(
                        icon = Icons.Default.NotificationsActive,
                        title = "Acceso a Notificaciones",
                        subtitle = "Requerido para leer mensajes",
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                }

                // Media
                SettingsSection(title = "Media") {
                    SettingsToggleItem(
                        icon = Icons.Default.MusicNote,
                        title = "Control de Música",
                        subtitle = "Controla tu reproductor por voz",
                        isChecked = uiState.mediaControlEnabled,
                        onCheckedChange = { viewModel.setMediaControlEnabled(it) }
                    )
                }

                // Artificial Intelligence
                SettingsSection(title = "Inteligencia Artificial") {
                    var providerExpanded by remember { mutableStateOf(false) }
                    var modelExpanded by remember { mutableStateOf(false) }
                    val providers = com.jarvis.assistant.data.model.AIProvider.values()
                    val selectedProvider = uiState.selectedProvider
                    val availableModels = com.jarvis.assistant.data.model.ModelDefinitions.getModelsForProvider(selectedProvider)
                    val selectedModel = com.jarvis.assistant.data.model.ModelDefinitions.getModelById(uiState.selectedModelId)

                    // Provider Selector
                    SettingsClickableItem(
                        icon = Icons.Default.Hub,
                        title = "Proveedor",
                        subtitle = selectedProvider.displayName,
                        onClick = { providerExpanded = true }
                    )

                    if (providerExpanded) {
                        AlertDialog(
                            onDismissRequest = { providerExpanded = false },
                            title = { Text("Seleccionar Proveedor", color = JarvisTextPrimary) },
                            text = {
                                Column {
                                    providers.forEach { provider ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setSelectedProvider(provider)
                                                    providerExpanded = false
                                                }
                                                .padding(vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = (provider == selectedProvider),
                                                onClick = null,
                                                colors = RadioButtonDefaults.colors(selectedColor = JarvisCyan)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(provider.displayName, color = JarvisTextPrimary)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { providerExpanded = false }) {
                                    Text("Cerrar", color = JarvisCyan)
                                }
                            },
                            containerColor = JarvisSurface
                        )
                    }

                    // Model Selector
                    SettingsClickableItem(
                        icon = Icons.Default.Memory,
                        title = "Modelo IA",
                        subtitle = selectedModel.name,
                        onClick = { modelExpanded = true }
                    )

                    if (modelExpanded) {
                        AlertDialog(
                            onDismissRequest = { modelExpanded = false },
                            title = { Text("Seleccionar Modelo", color = JarvisTextPrimary) },
                            text = {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    availableModels.forEach { model ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setSelectedModel(model.id)
                                                    modelExpanded = false
                                                }
                                                .padding(vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = (model.id == uiState.selectedModelId),
                                                onClick = null,
                                                colors = RadioButtonDefaults.colors(selectedColor = JarvisCyan)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(model.name, color = JarvisTextPrimary)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { modelExpanded = false }) {
                                    Text("Cerrar", color = JarvisCyan)
                                }
                            },
                            containerColor = JarvisSurface
                        )
                    }

                    // API Key Input for selected provider
                    var showKeyDialog by remember { mutableStateOf(false) }
                    val currentKey = when (selectedProvider) {
                        com.jarvis.assistant.data.model.AIProvider.OPENAI -> uiState.openaiKey
                        com.jarvis.assistant.data.model.AIProvider.GEMINI -> uiState.geminiKey
                        com.jarvis.assistant.data.model.AIProvider.COPILOT -> uiState.copilotKey
                    }

                    SettingsClickableItem(
                        icon = Icons.Default.Key,
                        title = "API Key de ${selectedProvider.displayName}",
                        subtitle = if (currentKey.isNotEmpty()) "••••••••••••" else "No configurada",
                        onClick = { showKeyDialog = true }
                    )

                    if (showKeyDialog) {
                        var tempKey by remember { mutableStateOf(currentKey) }
                        AlertDialog(
                            onDismissRequest = { showKeyDialog = false },
                            title = { Text("Configurar API Key", color = JarvisTextPrimary) },
                            text = {
                                Column {
                                    Text(
                                        "Ingresa la clave para ${selectedProvider.displayName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = JarvisTextSecondary,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    OutlinedTextField(
                                        value = tempKey,
                                        onValueChange = { tempKey = it },
                                        label = { Text("API Key") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = JarvisCyan,
                                            unfocusedBorderColor = JarvisNavy,
                                            focusedTextColor = JarvisTextPrimary,
                                            unfocusedTextColor = JarvisTextPrimary
                                        )
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.saveApiKey(selectedProvider, tempKey)
                                    showKeyDialog = false
                                }) {
                                    Text("Guardar", color = JarvisCyan)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showKeyDialog = false }) {
                                    Text("Cancelar", color = JarvisTextSecondary)
                                }
                            },
                            containerColor = JarvisSurface
                        )
                    }
                }

                // Gmail
                SettingsSection(title = "Gmail") {
                    val context = LocalContext.current
                    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) {
                            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                            viewModel.handleSignInResult(task)
                        } else {
                            // Helper to show toast or log failure
                        }
                    }

                    SettingsClickableItem(
                        icon = Icons.Default.Email,
                        title = "Conectar Gmail",
                        subtitle = if (uiState.gmailConnected) "Conectado" else "No conectado",
                        onClick = {
                            if (uiState.gmailConnected) {
                                viewModel.disconnectGmail()
                            } else {
                                launcher.launch(viewModel.getGmailSignInIntent())
                            }
                        }
                    )
                }

                // Xiaomi Optimization
                SettingsSection(title = "Optimización Xiaomi") {
                    SettingsClickableItem(
                        icon = Icons.Default.BatteryChargingFull,
                        title = "Autostart",
                        subtitle = "Permite iniciar con el dispositivo",
                        onClick = {
                            try {
                                val intent = Intent()
                                intent.component = android.content.ComponentName(
                                    "com.miui.securitycenter",
                                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Device doesn't have this setting
                            }
                        }
                    )
                    SettingsClickableItem(
                        icon = Icons.Default.PowerSettingsNew,
                        title = "Ahorro de Batería",
                        subtitle = "Configura las restricciones de batería",
                        onClick = {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                }

                // About
                SettingsSection(title = "Acerca de") {
                    SettingsInfoItem(
                        icon = Icons.Default.Info,
                        title = "Versión",
                        value = uiState.appVersion
                    )
                    SettingsInfoItem(
                        icon = Icons.Default.Memory,
                        title = "Modelo IA",
                        value = uiState.currentModelName
                    )

                    var showHelpDialog by remember { mutableStateOf(false) }
                    SettingsClickableItem(
                        icon = Icons.Default.Help,
                        title = "Ayuda y Guía de Uso",
                        subtitle = "Cómo hablar con JARVIS",
                        onClick = { showHelpDialog = true }
                    )

                    if (showHelpDialog) {
                        AlertDialog(
                            onDismissRequest = { showHelpDialog = false },
                            title = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Help, contentDescription = null, tint = JarvisCyan)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Manual de JARVIS", color = JarvisTextPrimary)
                                }
                            },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 8.dp)
                                ) {
                                    HelpContentSection("¿Qué es JARVIS?", "JARVIS es tu asistente inteligente personal, diseñado para ayudarte con tareas de voz, mensajería y control de medios.")
                                    
                                    HelpContentSection("Comandos de Voz", "Di \"Hey JARVIS\" seguido de tu orden:\n• \"Busca mi correo de Gmail\"\n• \"Pon música de Queen\"\n• \"Manda un WhatsApp a Juan diciendo Hola\"")
                                    
                                    HelpContentSection("WhatsApp", "JARVIS puede leer tus mensajes entrantes si activas el acceso a notificaciones en esta pantalla.")
                                    
                                    HelpContentSection("Acerca del Creador", "Esta aplicación es una obra de ingeniería personalizada.\n\nCreador: Juan Ramón\nCorreo: instalaudi@gmail.com\nJefe de Proyecto y Comandante: Juan Ramón")
                                    
                                    HorizontalDivider(color = JarvisCyan.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                                    
                                    Text(
                                        "Versión Actual: ${uiState.appVersion}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = JarvisTextSecondary
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showHelpDialog = false }) {
                                    Text("Entendido", color = JarvisCyan)
                                }
                            },
                            containerColor = JarvisSurface
                        )
                    }
                }

                // Danger Zone
                SettingsSection(title = "Zona de Peligro") {
                    SettingsClickableItem(
                        icon = Icons.Default.Delete,
                        title = "Borrar Datos",
                        subtitle = "Elimina toda la configuración",
                        iconColor = JarvisError,
                        onClick = { viewModel.clearAllData() }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = JarvisCyan,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        GlowingCard(glowColor = JarvisCyan.copy(alpha = 0.3f)) {
            content()
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = JarvisCyan,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = JarvisTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = JarvisTextSecondary
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = JarvisCyan,
                checkedTrackColor = JarvisCyan.copy(alpha = 0.3f),
                uncheckedThumbColor = JarvisTextMuted,
                uncheckedTrackColor = JarvisNavy
            )
        )
    }
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: androidx.compose.ui.graphics.Color = JarvisCyan,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = JarvisTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisTextSecondary
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = JarvisTextMuted
            )
        }
    }
}

@Composable
private fun SettingsInfoItem(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = JarvisTextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = JarvisTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = JarvisTextSecondary
        )
    }
}

@Composable
private fun HelpContentSection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = JarvisCyan
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = JarvisTextPrimary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
