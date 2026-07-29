package io.github.torrenkt.tslmwebui.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.github.torrenkt.tslmwebui.routers.AuthState
import kotlinx.coroutines.flow.MutableStateFlow

object AuthStorage {
    private val tokenFlow = MutableStateFlow<String?>(null)
    val tokenState @Composable get() = tokenFlow.collectAsState()
    var token: String?
        get() = tokenFlow.value
        set(value) { tokenFlow.value = value }


    private val userInfoFlow = MutableStateFlow<AuthState.Resp.Info?>(null)
    val userInfoState @Composable get() = userInfoFlow.collectAsState()
    var userInfo: AuthState.Resp.Info?
        get() = userInfoFlow.value
        set(value) { userInfoFlow.value = value }
}
