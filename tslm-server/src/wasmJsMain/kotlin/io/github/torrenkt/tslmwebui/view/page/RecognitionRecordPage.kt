package io.github.torrenkt.tslmwebui.view.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.torrenkt.tslmwebui.Res
import io.github.torrenkt.tslmwebui.component.Pagination
import io.github.torrenkt.tslmwebui.core.TslmApiClient
import io.github.torrenkt.tslmwebui.no_recognition_records
import io.github.torrenkt.tslmwebui.recognition_called_at
import io.github.torrenkt.tslmwebui.reset
import io.github.torrenkt.tslmwebui.search
import io.github.torrenkt.tslmwebui.search_email
import io.github.torrenkt.tslmwebui.search_title
import io.github.torrenkt.tslmwebui.view
import io.github.torrenkt.tslmwebui.view.LocalSnackbarHostState
import io.github.torrenkt.tslmwebui.view.dialog.RecognitionRecordDialog
import io.github.torrenkt.tslmwebui.routers.ListAllRecognitionRecord
import io.github.torrenkt.tslmwebui.routers.ListAllRecognitionRecordResp
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

@Serializable
@SerialName("recognition-records")
data object RecognitionRecordManagement

@Composable
fun RecognitionRecordManagementPage() {
    val snackbarHost = LocalSnackbarHostState.current
    var pageSize by rememberSaveable { mutableStateOf(10) }
    var currentPage by rememberSaveable { mutableStateOf(0L) }
    var emailInput by rememberSaveable { mutableStateOf("") }
    var titleInput by rememberSaveable { mutableStateOf("") }
    var emailQuery by rememberSaveable { mutableStateOf("") }
    var titleQuery by rememberSaveable { mutableStateOf("") }
    var totalPage by remember { mutableStateOf(0L) }
    var list by remember { mutableStateOf<List<ListAllRecognitionRecordResp.Data.Item>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var reloadVersion by remember { mutableStateOf(0) }
    var selectedRecord by remember { mutableStateOf<ListAllRecognitionRecordResp.Data.Item?>(null) }

    LaunchedEffect(pageSize, currentPage, emailQuery, titleQuery, reloadVersion) {
        loading = true
        try {
            val response = TslmApiClient.get(
                ListAllRecognitionRecord(pageSize, currentPage, emailQuery, titleQuery),
            ).body<ListAllRecognitionRecordResp>()
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { Text(stringResource(Res.string.search_email)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text(stringResource(Res.string.search_title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    onClick = {
                        emailQuery = emailInput.trim()
                        titleQuery = titleInput.trim()
                        currentPage = 0
                        reloadVersion++
                    },
                ) {
                    Text(stringResource(Res.string.search))
                }
                TextButton(
                    onClick = {
                        emailInput = ""
                        titleInput = ""
                        emailQuery = ""
                        titleQuery = ""
                        currentPage = 0
                        reloadVersion++
                    },
                ) {
                    Text(stringResource(Res.string.reset))
                }
            }
            if (list.isEmpty() && totalPage == 0L) {
                Text(stringResource(Res.string.no_recognition_records))
            } else {
                Pagination(
                    list = list,
                    totalPage = totalPage,
                    currentPage = currentPage,
                    pageSize = pageSize,
                    pagingChanged = { newPageSize, newPage ->
                        pageSize = newPageSize
                        currentPage = newPage
                    },
                    itemContent = { record ->
                        RecognitionRecordListItem(
                            record = record,
                            onView = { selectedRecord = record },
                        )
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }

    selectedRecord?.let { record ->
        RecognitionRecordDialog(
            record = record.data,
            onDismiss = { selectedRecord = null },
        )
    }
}

@Composable
private fun RecognitionRecordListItem(
    record: ListAllRecognitionRecordResp.Data.Item,
    onView: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.email)
                SelectionContainer { Text(record.data.title) }
                Text(stringResource(Res.string.recognition_called_at, record.data.calledAt))
            }
            TextButton(onClick = onView) {
                Text(stringResource(Res.string.view))
            }
        }
    }
}
