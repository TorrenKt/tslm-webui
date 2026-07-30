package io.github.torrenkt.tslmwebui.view.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.unit.dp
import io.github.torrenkt.tslmwebui.Res
import io.github.torrenkt.tslmwebui.component.LabelDisplayer
import io.github.torrenkt.tslmwebui.component.Pagination
import io.github.torrenkt.tslmwebui.core.AuthStorage
import io.github.torrenkt.tslmwebui.core.GlobalJson
import io.github.torrenkt.tslmwebui.core.TslmApiClient
import io.github.torrenkt.tslmwebui.core.logger
import io.github.torrenkt.tslmwebui.routers.ListRecognitionRecord
import io.github.torrenkt.tslmwebui.routers.ListRecognitionRecordResp
import io.github.torrenkt.tslmwebui.routers.PredictTorrent
import io.github.torrenkt.tslmwebui.routers.TslmDisplayEntity
import io.github.torrenkt.tslmwebui.submit
import io.github.torrenkt.tslmwebui.recognition_called_at
import io.github.torrenkt.tslmwebui.recognition_records
import io.github.torrenkt.tslmwebui.no_recognition_records
import io.github.torrenkt.tslmwebui.view
import io.github.torrenkt.tslmwebui.view.LocalSnackbarHostState
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.resources.get
import io.ktor.client.request.setBody
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import kotlin.getValue

private val log by logger("HomePage")

@Serializable
@SerialName("home")
data object Home

@Composable
fun HomePage() {
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHostState.current
    val userInfo by AuthStorage.userInfoState
    val canViewHistory = userInfo?.email != null
    var history by remember { mutableStateOf(emptyList<ListRecognitionRecordResp.Data.Item>()) }
    var historyPageSize by rememberSaveable { mutableStateOf(10) }
    var historyPageIndex by rememberSaveable { mutableStateOf(0L) }
    var historyTotalPage by remember { mutableStateOf(0L) }
    var historyReloadVersion by remember { mutableStateOf(0) }

    LaunchedEffect(canViewHistory, historyPageSize, historyPageIndex, historyReloadVersion) {
        if (!canViewHistory) {
            history = emptyList()
            historyTotalPage = 0
            return@LaunchedEffect
        }
        try {
            val response = TslmApiClient.get(
                ListRecognitionRecord(historyPageSize, historyPageIndex),
            ).body<ListRecognitionRecordResp>()
            if (response.code != 200 || response.data == null) {
                snackbarHost.showSnackbar(response.message)
                return@LaunchedEffect
            }
            history = response.data.list
            historyTotalPage = response.data.totalPage
        } catch (e: Throwable) {
            snackbarHost.showSnackbar(e.message ?: "Unknown error")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (canViewHistory) Arrangement.Top else Arrangement.Center,
    ) {
        var loading by remember { mutableStateOf(false) }
        var value: String by rememberSaveable { mutableStateOf("") }
        var labels: List<TslmDisplayEntity> by rememberSaveable { mutableStateOf(emptyList()) }
        LabelDisplayer(
            input = value,
            labels = labels,
        )
        TextField(
            value = value,
            onValueChange = {
                labels = emptyList()
                value = it
            },
            enabled = !loading,
            modifier = Modifier.padding(top = 50.dp)
        )
        Button(
            enabled = !loading,
            onClick = {
                if (value.isBlank()) {
                    return@Button
                }
                labels = emptyList()
                loading = true
                scope.launch {
                    try {
                        val result = TslmApiClient.post(PredictTorrent()) {
                            setBody(PredictTorrent.Req(value))
                        }
                        val resp = result.body<PredictTorrent.Resp>()
                        if (resp.code != 200 || resp.data == null) {
                            launch { snackbarHost.showSnackbar(resp.message) }
                            return@launch
                        }
                        labels = resp.data
                        if (canViewHistory) {
                            historyPageIndex = 0
                            historyReloadVersion++
                        }
                        log.info { "save labels: ${GlobalJson.encodeToString(resp.data)}" }
                    } catch (e: Throwable) {
                        launch { snackbarHost.showSnackbar(e.message ?: "Unknown error") }
                    } finally {
                        loading = false
                    }
                }
            }
        ) {
            Text(stringResource(Res.string.submit))
        }
        if (canViewHistory) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxSize(),
                ) {
                    Text(
                        text = stringResource(Res.string.recognition_records),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 8.dp),
                    )
                    if (history.isEmpty() && historyTotalPage == 0L) {
                        Text(stringResource(Res.string.no_recognition_records))
                    } else {
                        Pagination(
                            list = history,
                            totalPage = historyTotalPage,
                            currentPage = historyPageIndex,
                            pageSize = historyPageSize,
                            pagingChanged = { pageSize, pageIndex ->
                                historyPageSize = pageSize
                                historyPageIndex = pageIndex
                            },
                            itemContent = { record ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(record.title)
                                        Text(stringResource(Res.string.recognition_called_at, record.calledAt))
                                        TextButton(
                                            onClick = {
                                                value = record.title
                                                labels = record.result
                                            },
                                        ) {
                                            Text(stringResource(Res.string.view))
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                    }
                }
            }
        }
    }
}
