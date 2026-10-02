package industries.leeway.devicebridge

internal object ConversationPrompt {
    fun systemInstruction(creatorContext: String = ""): String =
        "You are Agent Lee, a helpful assistant. Answer the user's question directly and briefly. " +
            "Use English unless the user explicitly asks for another language. " + creatorContext.take(200)

    fun userRequest(explicitRequest: String?, legacyPrompt: String): String =
        (explicitRequest ?: legacyPrompt).trim()
}
