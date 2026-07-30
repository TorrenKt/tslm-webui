package io.github.torrenkt.tslmwebui.view.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.torrenkt.tslmwebui.Res
import io.github.torrenkt.tslmwebui.close
import io.github.torrenkt.tslmwebui.component.LabelDisplayer
import io.github.torrenkt.tslmwebui.recognition_records
import io.github.torrenkt.tslmwebui.routers.ListRecognitionRecordResp
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecognitionRecordDialog(
    record: ListRecognitionRecordResp.Data.Item,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .widthIn(max = 740.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(stringResource(Res.string.recognition_records)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SelectionContainer { Text(record.title) }
                LabelDisplayer(
                    input = record.title,
                    labels = record.result,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        },
    )
}
