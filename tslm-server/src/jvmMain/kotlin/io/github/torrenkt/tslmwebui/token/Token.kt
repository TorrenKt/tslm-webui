package io.github.torrenkt.tslmwebui.token

import java.security.SecureRandom

private val SUPER_TOKEN_PATTERN = Regex("^[A-Za-z0-9]{128}$")
private const val TOKEN_LENGTH = 128
private const val TOKEN_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
private val secureRandom = SecureRandom()

fun String.checkToken(): Boolean {
    return SUPER_TOKEN_PATTERN.matches(this)
}

fun newToken(): String {
    return buildString(TOKEN_LENGTH) {
        repeat(TOKEN_LENGTH) {
            append(TOKEN_CHARACTERS[secureRandom.nextInt(TOKEN_CHARACTERS.length)])
        }
    }
}
