package com.jarvis.assistant.ui.screens

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.jarvis.assistant.ui.components.*
import com.jarvis.assistant.ui.effects.*
import com.jarvis.assistant.ui.theme.*
import com.jarvis.assistant.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val chatState by viewModel.chatState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Audio & Contacts permissions
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS
        )
    )

    val sendMessage = {
        if (inputText.isNotBlank()) {
            viewModel.sendMessage(inputText)
            inputText = ""
            focusManager.clearFocus()
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
            particleCount = 40
        )
        HudGridOverlay(modifier = Modifier.fillMaxSize())

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding() // Push content up when keyboard opens
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusChip(label = "EN LÍNEA", isActive = true)
                        Text(
                            text = "J.A.R.V.I.S.",
                            style = MaterialTheme.typography.titleMedium,
                            color = JarvisCyan
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = "Clear history",
                            tint = JarvisTextSecondary
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = JarvisTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = JarvisBlack.copy(alpha = 0.8f)
                )
            )

            // Chat area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (chatState.messages.isEmpty()) {
                    EmptyStateMessage(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    ChatList(
                        messages = chatState.messages,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Voice waveform overlay when speaking
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 32.dp)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = chatState.isSpeaking,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        VoiceWaveform(
                            isActive = chatState.isSpeaking,
                            color = JarvisGold
                        )
                    }
                }
            }

            // Transcript display
            TranscriptDisplay(
                transcript = chatState.currentTranscript,
                isListening = chatState.isListening
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Voice button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                VoiceButton(
                    isListening = chatState.isListening,
                    isSpeaking = chatState.isSpeaking,
                    isEnabled = true,
                    onClick = {
                        if (!permissionsState.allPermissionsGranted) {
                            permissionsState.launchMultiplePermissionRequest()
                        } else {
                            // Toggle using state machine logic
                            viewModel.onMicButtonClicked()
                        }
                    }
                )
            }

            // Text input
            Surface(
                color = JarvisSurface.copy(alpha = 0.95f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "Escriba un mensaje...",
                                color = JarvisTextMuted
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisNavy,
                            cursorColor = JarvisCyan,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        ),
                        enabled = !chatState.isProcessing
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    FilledIconButton(
                        onClick = { sendMessage() },
                        enabled = inputText.isNotBlank() && !chatState.isProcessing,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = JarvisCyan,
                            contentColor = JarvisBlack,
                            disabledContainerColor = JarvisNavy,
                            disabledContentColor = JarvisTextMuted
                        )
                    ) {
                        if (chatState.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = JarvisTextMuted
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }

        // Error snackbar
        chatState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK", color = JarvisCyan)
                    }
                },
                containerColor = JarvisCard,
                contentColor = JarvisTextPrimary
            ) {
                Text(error)
            }
        }
    }
}
