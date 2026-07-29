package io.github.torrenkt.tslmwebui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.torrenkt.tslmwebui.Res
import io.github.torrenkt.tslmwebui.next_page
import io.github.torrenkt.tslmwebui.page_size
import io.github.torrenkt.tslmwebui.page_status
import io.github.torrenkt.tslmwebui.previous_page
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Pagination(
    list: List<T>,
    totalPage: Long,
    currentPage: Long = 0,
    pageSize: Int = 10,
    pageSizeOptions: List<Int> = listOf(10, 20, 50, 100),
    pagingChanged: (pageSize: Int, currentPage: Long) -> Unit,
    itemContent: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pageSizeExpanded by remember { mutableStateOf(false) }
    val validPageSizeOptions = remember(pageSizeOptions, pageSize) {
        (pageSizeOptions + pageSize).filter { it > 0 }.distinct()
    }
    val canGoPrevious = currentPage > 0
    val canGoNext = totalPage > 0 && currentPage + 1 < totalPage
    val displayedPage = if (totalPage > 0) currentPage + 1 else 0

    Column(modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            items(list) { item ->
                itemContent(item)
            }
        }
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .padding(top = 12.dp)
                .align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                enabled = canGoPrevious,
                onClick = { pagingChanged(pageSize, currentPage - 1) },
            ) {
                Text(stringResource(Res.string.previous_page))
            }
            Text(stringResource(Res.string.page_status, displayedPage, totalPage))
            TextButton(
                enabled = canGoNext,
                onClick = { pagingChanged(pageSize, currentPage + 1) },
            ) {
                Text(stringResource(Res.string.next_page))
            }
            ExposedDropdownMenuBox(
                expanded = pageSizeExpanded,
                onExpandedChange = { pageSizeExpanded = it },
            ) {
                OutlinedTextField(
                    value = pageSize.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.page_size)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = pageSizeExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .width(120.dp),
                    singleLine = true,
                )
                ExposedDropdownMenu(
                    expanded = pageSizeExpanded,
                    onDismissRequest = { pageSizeExpanded = false },
                ) {
                    validPageSizeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.toString()) },
                            onClick = {
                                pageSizeExpanded = false
                                if (option != pageSize) {
                                    pagingChanged(option, 0)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
