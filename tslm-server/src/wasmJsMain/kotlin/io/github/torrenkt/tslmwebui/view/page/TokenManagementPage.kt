package io.github.torrenkt.tslmwebui.view.page

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.torrenkt.tslmwebui.Res
import io.github.torrenkt.tslmwebui.component.Pagination
import io.github.torrenkt.tslmwebui.core.TslmApiClient
import io.github.torrenkt.tslmwebui.cancel_selection
import io.github.torrenkt.tslmwebui.create_token
import io.github.torrenkt.tslmwebui.delete_token
import io.github.torrenkt.tslmwebui.disable_token
import io.github.torrenkt.tslmwebui.enable_token
import io.github.torrenkt.tslmwebui.refresh_token
import io.github.torrenkt.tslmwebui.search
import io.github.torrenkt.tslmwebui.search_email
import io.github.torrenkt.tslmwebui.selected_tokens
import io.github.torrenkt.tslmwebui.token_created_at
import io.github.torrenkt.tslmwebui.token_updated_at
import io.github.torrenkt.tslmwebui.view.LocalSnackbarHostState
import io.github.torrenkt.tslmwebui.view.dialog.CreateTokenDialog
import io.github.torrenkt.tslmwebui.view.dialog.ConfirmRefreshTokenDialog
import io.github.torrenkt.tslmwebui.view.dialog.DeleteTokenDialog
import io.github.torrenkt.tslmwebui.view.dialog.RefreshTokenDialog
import io.github.torrenkt.tslmwebui.routers.CreateToken
import io.github.torrenkt.tslmwebui.routers.DeleteToken
import io.github.torrenkt.tslmwebui.routers.ListToken
import io.github.torrenkt.tslmwebui.routers.ListTokenResp
import io.github.torrenkt.tslmwebui.routers.RefreshToken
import io.github.torrenkt.tslmwebui.routers.TokenState
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

@Serializable
@SerialName("tokens")
data object TokenManagement

@Composable
fun TokenManagementPage() {
    val snackbarHost = LocalSnackbarHostState.current
    var pageSize by rememberSaveable { mutableStateOf(10) }
    var currentPage by rememberSaveable { mutableStateOf(0L) }
    var searchInput by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var totalPage by remember { mutableStateOf(0L) }
    var list by remember { mutableStateOf<List<ListTokenResp.Data.Item>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var reloadVersion by remember { mutableStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var creatingToken by remember { mutableStateOf(false) }
    var createdToken by remember { mutableStateOf<String?>(null) }
    var refreshedToken by remember { mutableStateOf<String?>(null) }
    var pendingRefreshTokenId by remember { mutableStateOf<Int?>(null) }
    var processingTokenIds by remember { mutableStateOf(emptySet<Int>()) }
    var selectedTokenIds by remember { mutableStateOf(emptySet<Int>()) }
    var pendingDeleteIds by remember { mutableStateOf<Set<Int>?>(null) }
    var deletingTokens by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun updateTokenState(ids: Set<Int>, enabled: Boolean, clearSelection: Boolean) {
        scope.launch {
            processingTokenIds += ids
            try {
                val response = TslmApiClient.post(TokenState()) {
                    setBody(TokenState.Req(ids = ids.toList(), enabled = enabled))
                }.body<TokenState.Resp>()
                if (response.code != 200) {
                    snackbarHost.showSnackbar(response.message)
                    return@launch
                }
                if (clearSelection) {
                    selectedTokenIds -= ids
                }
                reloadVersion++
            } catch (e: Throwable) {
                snackbarHost.showSnackbar(e.message ?: "Unknown error")
            } finally {
                processingTokenIds -= ids
            }
        }
    }

    fun refreshToken(tokenId: Int) {
        scope.launch {
            processingTokenIds += tokenId
            try {
                val response = TslmApiClient.post(RefreshToken()) {
                    setBody(RefreshToken.Req(tokenId))
                }.body<RefreshToken.Resp>()
                val newToken = response.data
                if (response.code != 200 || newToken == null) {
                    snackbarHost.showSnackbar(response.message)
                    return@launch
                }
                pendingRefreshTokenId = null
                refreshedToken = newToken
                reloadVersion++
            } catch (e: Throwable) {
                snackbarHost.showSnackbar(e.message ?: "Unknown error")
            } finally {
                processingTokenIds -= tokenId
            }
        }
    }

    LaunchedEffect(pageSize, currentPage, query, reloadVersion) {
        loading = true
        try {
            val response = TslmApiClient.get(ListToken(pageSize, currentPage, query))
                .body<ListTokenResp>()
            val data = response.data
            if (response.code != 200 || data == null) {
                snackbarHost.showSnackbar(response.message)
                return@LaunchedEffect
            }
            totalPage = data.totalPage
            list = data.list
        } catch (e: Throwable) {
            snackbarHost.showSnackbar(e.message ?: "Unknown error")
        } finally {
            loading = false
        }
    }

    if (loading && list.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    label = { Text(stringResource(Res.string.search_email)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                TextButton(
                    onClick = {
                        query = searchInput.trim()
                        currentPage = 0
                        reloadVersion++
                    },
                ) {
                    Text(stringResource(Res.string.search))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectedTokenIds.isNotEmpty()) {
                        Text(stringResource(Res.string.selected_tokens, selectedTokenIds.size))
                        TextButton(
                            enabled = processingTokenIds.isEmpty(),
                            onClick = { updateTokenState(selectedTokenIds, enabled = false, clearSelection = true) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFFEF6C00),
                            ),
                        ) {
                            Text(stringResource(Res.string.disable_token))
                        }
                        TextButton(
                            enabled = processingTokenIds.isEmpty(),
                            onClick = { updateTokenState(selectedTokenIds, enabled = true, clearSelection = true) },
                        ) {
                            Text(stringResource(Res.string.enable_token))
                        }
                        TextButton(
                            enabled = processingTokenIds.isEmpty(),
                            onClick = { pendingDeleteIds = selectedTokenIds },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text(stringResource(Res.string.delete_token))
                        }
                        TextButton(
                            enabled = processingTokenIds.isEmpty(),
                            onClick = { selectedTokenIds = emptySet() },
                        ) {
                            Text(stringResource(Res.string.cancel_selection))
                        }
                    } else {
                        Spacer(
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = { showCreateDialog = true }
                        ) {
                            Text(stringResource(Res.string.create_token))
                        }
                    }
                }
            }
            Pagination(
                list = list,
                totalPage = totalPage,
                currentPage = currentPage,
                pageSize = pageSize,
                pagingChanged = { newPageSize, newPage ->
                    pageSize = newPageSize
                    currentPage = newPage
                },
                itemContent = { token ->
                    TokenListItem(
                        token = token,
                        processing = token.id in processingTokenIds,
                        selected = token.id in selectedTokenIds,
                        selectMode = selectedTokenIds.isNotEmpty(),
                        onSelected = {
                            selectedTokenIds = if (token.id in selectedTokenIds) {
                                selectedTokenIds - token.id
                            } else {
                                selectedTokenIds + token.id
                            }
                        },
                        onEnabledChanged = { enabled ->
                            updateTokenState(setOf(token.id), enabled, clearSelection = false)
                        },
                        onRefresh = {
                            pendingRefreshTokenId = token.id
                        },
                        onDelete = { pendingDeleteIds = setOf(token.id) },
                    )
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }

    if (showCreateDialog) {
        CreateTokenDialog(
            creating = creatingToken,
            createdToken = createdToken,
            onDismiss = {
                showCreateDialog = false
                createdToken = null
            },
            onCreate = { email ->
                scope.launch {
                    creatingToken = true
                    try {
                        val response = TslmApiClient.post(CreateToken()) {
                            setBody(CreateToken.Req(email.trim()))
                        }.body<CreateToken.Resp>()
                        if (response.code != 200) {
                            snackbarHost.showSnackbar(response.message)
                            return@launch
                        }
                        createdToken = response.data
                        reloadVersion++
                    } catch (e: Throwable) {
                        snackbarHost.showSnackbar(e.message ?: "Unknown error")
                    } finally {
                        creatingToken = false
                    }
                }
            },
        )
    }

    refreshedToken?.let { token ->
        RefreshTokenDialog(
            token = token,
            onDismiss = { refreshedToken = null },
        )
    }

    pendingRefreshTokenId?.let { tokenId ->
        ConfirmRefreshTokenDialog(
            refreshing = tokenId in processingTokenIds,
            onDismiss = { pendingRefreshTokenId = null },
            onConfirm = { refreshToken(tokenId) },
        )
    }

    pendingDeleteIds?.let { ids ->
        DeleteTokenDialog(
            count = ids.size,
            deleting = deletingTokens,
            onDismiss = { pendingDeleteIds = null },
            onConfirm = {
                scope.launch {
                    deletingTokens = true
                    processingTokenIds += ids
                    try {
                        val response = TslmApiClient.post(DeleteToken()) {
                            setBody(DeleteToken.Req(ids.toList()))
                        }.body<DeleteToken.Resp>()
                        if (response.code != 200) {
                            snackbarHost.showSnackbar(response.message)
                            return@launch
                        }
                        selectedTokenIds -= ids
                        pendingDeleteIds = null
                        reloadVersion++
                    } catch (e: Throwable) {
                        snackbarHost.showSnackbar(e.message ?: "Unknown error")
                    } finally {
                        deletingTokens = false
                        processingTokenIds -= ids
                    }
                }
            },
        )
    }
}

@Composable
private fun TokenListItem(
    token: ListTokenResp.Data.Item,
    processing: Boolean,
    selected: Boolean,
    selectMode: Boolean,
    onSelected: () -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onSelected),
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = token.email,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                )
                Switch(
                    checked = token.enabled,
                    enabled = !processing && !selectMode,
                    onCheckedChange = onEnabledChanged,
                )
            }
            Text(token.token)
            Text(stringResource(Res.string.token_created_at, token.createdAt))
            Text(stringResource(Res.string.token_updated_at, token.updatedAt))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(
                    enabled = !processing && !selectMode,
                    onClick = onRefresh,
                ) {
                    Text(stringResource(Res.string.refresh_token))
                }
                TextButton(
                    enabled = !processing && !selectMode,
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(Res.string.delete_token))
                }
            }
        }
    }
}
