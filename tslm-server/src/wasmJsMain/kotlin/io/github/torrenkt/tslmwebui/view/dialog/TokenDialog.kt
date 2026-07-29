package io.github.torrenkt.tslmwebui.view.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.torrenkt.tslmwebui.Res
import io.github.torrenkt.tslmwebui.cancel
import io.github.torrenkt.tslmwebui.save
import io.github.torrenkt.tslmwebui.token_dialog_title
import io.github.torrenkt.tslmwebui.token_input_label
import org.jetbrains.compose.resources.stringResource

@Composable
fun TokenDialog(
    token: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var editedToken by remember(token) { mutableStateOf(token.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.token_dialog_title)) },
        text = {
            TextField(
                value = editedToken,
                onValueChange = { editedToken = it },
                label = { Text(stringResource(Res.string.token_input_label)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(editedToken) }) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
