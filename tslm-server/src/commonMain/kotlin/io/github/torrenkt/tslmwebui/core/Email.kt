package io.github.torrenkt.tslmwebui.core

private const val MAX_EMAIL_LENGTH = 320
private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

fun String.isValidEmail(): Boolean {
    val email = trim()
    return email.isNotEmpty() && email.length <= MAX_EMAIL_LENGTH && emailPattern.matches(email)
}
