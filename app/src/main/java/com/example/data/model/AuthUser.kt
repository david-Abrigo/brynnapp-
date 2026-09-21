package com.example.data.model

enum class AuthProvider {
    EMAIL,
    GOOGLE,
    GUEST
}

data class AuthUser(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val whatsapp: String = "",
    val provider: AuthProvider = AuthProvider.GUEST,
    val isLoggedIn: Boolean = false,
    val token: String = "",
    val refreshToken: String = "",
    val plan: String = "FREE",
    val createdAt: Long = System.currentTimeMillis()
) {
    val initial: String
        get() = when {
            displayName.isNotBlank() -> displayName.first().uppercase()
            email.isNotBlank() -> email.first().uppercase()
            else -> "U"
        }
}
