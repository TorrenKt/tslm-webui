package io.github.torrenkt.tslmwebui.view

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.torrenkt.tslmwebui.Res
import io.github.torrenkt.tslmwebui.app_name
import io.github.torrenkt.tslmwebui.identity_admin
import io.github.torrenkt.tslmwebui.identity_guest
import io.github.torrenkt.tslmwebui.core.AuthStorage
import io.github.torrenkt.tslmwebui.core.TslmApiClient
import io.github.torrenkt.tslmwebui.core.logger
import io.github.torrenkt.tslmwebui.invalid_token
import io.github.torrenkt.tslmwebui.page_home
import io.github.torrenkt.tslmwebui.page_token_management
import io.github.torrenkt.tslmwebui.routers.AuthState
import io.github.torrenkt.tslmwebui.view.dialog.TokenDialog
import io.github.torrenkt.tslmwebui.view.page.Home
import io.github.torrenkt.tslmwebui.view.page.HomePage
import io.github.torrenkt.tslmwebui.view.page.TokenManagement
import io.github.torrenkt.tslmwebui.view.page.TokenManagementPage
import io.github.torrenkt.tslmwebui.welcome_user
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.http.HttpStatusCode
import kotlinx.browser.localStorage
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.w3c.dom.get

private val log by logger("App")

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    onNavHostReady: suspend (NavController) -> Unit = {},
) {
    val userInfo by AuthStorage.userInfoState
    val token by AuthStorage.tokenState
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var showTokenDialog by remember { mutableStateOf(false) }

    val snackbarHost = remember { SnackbarHostState() }
    val scaffold: @Composable () -> Unit = {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.app_name)) },
                    actions = {
                        val email = userInfo?.email
                        val identity = when {
                            userInfo?.isAdmin == true -> stringResource(Res.string.identity_admin)
                            email != null -> email
                            else -> stringResource(Res.string.identity_guest)
                        }
                        TextButton(
                            onClick = { showTokenDialog = true },
                        ) {
                            Text(stringResource(Res.string.welcome_user, identity))
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHost) },
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                CompositionLocalProvider(
                    LocalSnackbarHostState provides snackbarHost
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Home,
                    ) {
                        composable<Home> { HomePage() }
                        if (userInfo?.isAdmin == true) {
                            composable<TokenManagement> { TokenManagementPage() }
                        }
                    }
                }
            }
        }
    }

    if (userInfo?.isAdmin == true) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                item(
                    selected = currentDestination?.hasRoute<Home>() == true,
                    onClick = { navController.navigate(Home) { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text(stringResource(Res.string.page_home)) },
                )
                item(
                    selected = currentDestination?.hasRoute<TokenManagement>() == true,
                    onClick = { navController.navigate(TokenManagement) { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.Key, null) },
                    label = { Text(stringResource(Res.string.page_token_management)) },
                )
            },
        ) {
            scaffold()
        }
    } else {
        scaffold()
    }

    if (showTokenDialog) {
        TokenDialog(
            token = token,
            onDismiss = { showTokenDialog = false },
            onSave = { editedToken ->
                val newToken = editedToken.trim().takeIf { it.isNotEmpty() }
                AuthStorage.token = newToken
                AuthStorage.userInfo = null
                if (newToken == null) {
                    localStorage.removeItem("tslm-token")
                } else {
                    localStorage.setItem("tslm-token", newToken)
                }
                showTokenDialog = false
            },
        )
    }

    LaunchedEffect(Unit) {
        localStorage["tslm-token"]?.let {
            AuthStorage.token = it
        }
    }

    LaunchedEffect(navController) {
        onNavHostReady(navController)
    }

    LaunchedEffect(token) {
        if (token == null) {
            return@LaunchedEffect
        }

        val resp = TslmApiClient.get(AuthState())
        if (resp.status == HttpStatusCode.Forbidden) {
            AuthStorage.userInfo = null
            launch {
                val str = getString(Res.string.invalid_token)
                snackbarHost.showSnackbar(str)
            }
            return@LaunchedEffect
        }
        if (resp.status != HttpStatusCode.OK) {
            launch { snackbarHost.showSnackbar(resp.status.description) }
            return@LaunchedEffect
        }
        val userInfo = resp.body<AuthState.Resp>()
        AuthStorage.userInfo = userInfo.data
    }
}
