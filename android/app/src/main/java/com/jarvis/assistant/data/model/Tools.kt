package com.jarvis.assistant.data.model

import com.jarvis.assistant.data.api.Tool
import com.jarvis.assistant.data.api.ToolFunction

object ToolDefinitions {
    val allTools = listOf(
        Tool(
            type = "function",
            function = ToolFunction(
                name = "toggle_flashlight",
                description = "Turn the device flashlight on or off. Use this when the user asks to see in the dark or turn on the light.",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "state" to mapOf(
                            "type" to "boolean",
                            "description" to "True to turn on, false to turn off."
                        )
                    ),
                    "required" to listOf("state")
                )
            )
        ),
        Tool(
            type = "function",
            function = ToolFunction(
                name = "set_brightness",
                description = "Set the screen brightness level. Use this when the user says the screen is too bright or too dark.",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "level" to mapOf(
                            "type" to "integer",
                            "description" to "The brightness level from 0 to 100.",
                            "minimum" to 0,
                            "maximum" to 100
                        )
                    ),
                    "required" to listOf("level")
                )
            )
        ),
        Tool(
            type = "function",
            function = ToolFunction(
                name = "set_volume",
                description = "Set the device media volume level. Use this when the user wants to adjust the volume.",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "level" to mapOf(
                            "type" to "integer",
                            "description" to "The volume level from 0 to 100.",
                            "minimum" to 0,
                            "maximum" to 100
                        )
                    ),
                    "required" to listOf("level")
                )
            )
        ),
        Tool(
            type = "function",
            function = ToolFunction(
                name = "open_app",
                description = "Open an installed application on the device. Use this when the user wants to launch a specific app.",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "app_name" to mapOf(
                            "type" to "string",
                            "description" to "The name of the application to open (e.g., 'YouTube', 'WhatsApp')."
                        )
                    ),
                    "required" to listOf("app_name")
                )
            )
        ),
        Tool(
            type = "function",
            function = ToolFunction(
                name = "play_music",
                description = "Play music, artist, album or song on a specific music app. Use this when the user wants to hear music.",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "query" to mapOf(
                            "type" to "string",
                            "description" to "The music search query (e.g., 'Nirvana', 'Shape of You', 'Rock playlist')."
                        ),
                        "app_name" to mapOf(
                            "type" to "string",
                            "description" to "The music app to use (e.g., 'Spotify', 'YouTube Music'). Defaults to Spotify if not specified.",
                            "enum" to listOf("spotify", "youtube music", "youtube", "apple music")
                        )
                    ),
                    "required" to listOf("query")
                )
            )
        ),
        Tool(
            type = "function",
            function = ToolFunction(
                name = "read_emails",
                description = "Read or search for emails in the user's Gmail inbox. Use this to check unread emails OR search for specific info (e.g. 'emails from Tony', 'emails about Avengers').",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "limit" to mapOf(
                            "type" to "integer",
                            "description" to "The maximum number of emails to retrieve. Default is 5.",
                            "default" to 5
                        ),
                        "query" to mapOf(
                            "type" to "string",
                            "description" to "The Gmail search query (e.g., 'is:unread', 'from:Tony', 'newer_than:1d'). Defaults to 'is:unread'."
                        )
                    )
                )
            )
        ),
        Tool(
            type = "function",
            function = ToolFunction(
                name = "create_email_draft",
                description = "Create a draft email in the user's Gmail without sending it immediately. Use this when the user wants to 'write an email' or 'compose a message' but not send it yet.",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "recipient" to mapOf(
                            "type" to "string",
                            "description" to "The email address of the recipient."
                        ),
                        "subject" to mapOf(
                            "type" to "string",
                            "description" to "The subject of the email."
                        ),
                        "body" to mapOf(
                            "type" to "string",
                            "description" to "The body content of the email."
                        )
                    ),
                    "required" to listOf("recipient", "subject", "body")
                )
            )
        ),
        Tool(
            type = "function",
            function = ToolFunction(
                name = "send_email",
                description = "Send an email to a specific recipient. Use this when the user asks to send an email.",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "recipient" to mapOf(
                            "type" to "string",
                            "description" to "The email address of the recipient."
                        ),
                        "subject" to mapOf(
                            "type" to "string",
                            "description" to "The subject of the email."
                        ),
                        "body" to mapOf(
                            "type" to "string",
                            "description" to "The body content of the email."
                        )
                    ),
                    "required" to listOf("recipient", "subject", "body")
                )
            )
        ),
        Tool(
            type = "function",
            function = ToolFunction(
                name = "send_whatsapp",
                description = "Send a WhatsApp message to a specific phone number or contact name. Use this when the user asks to send a WhatsApp message.",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "phone_number" to mapOf(
                            "type" to "string",
                            "description" to "The phone number (with country code) OR the exact contact name as saved in the phone (e.g., 'Mom', 'Juan Perez')."
                        ),
                        "message" to mapOf(
                            "type" to "string",
                            "description" to "The message content to send."
                        )
                    ),
                    "required" to listOf("phone_number", "message")
                )
            )
        )
    )
}
