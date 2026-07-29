package io.github.torrenkt.tslmwebui.view.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.torrenkt.tslmwebui.Res
import io.github.torrenkt.tslmwebui.cancel
import io.github.torrenkt.tslmwebui.close
import io.github.torrenkt.tslmwebui.create_token
import io.github.torrenkt.tslmwebui.create_token_success
import io.github.torrenkt.tslmwebui.created_token
import io.github.torrenkt.tslmwebui.email
import io.github.torrenkt.tslmwebui.core.isValidEmail
import org.jetbrains.compose.resources.stringResource

@Composable
fun CreateTokenDialog(
    creating: Boolean,
    createdToken: String?,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var emailInput by remember { mutableStateOf("") }
    val created = createdToken != null

    AlertDialog(
        onDismissRequest = {
            if (!creating) {
                onDismiss()
            }
        },
        title = { Text(stringResource(Res.string.create_token)) },
        text = {
            if (created) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.create_token_success))
                    Text(stringResource(Res.string.created_token))
                    SelectionContainer { Text(createdToken) }
                }
            } else {
                TextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    enabled = !creating,
                    label = { Text(stringResource(Res.string.email)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            if (created) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.close))
                }
            } else {
                TextButton(
                    enabled = !creating && emailInput.isValidEmail(),
                    onClick = { onCreate(emailInput) },
                ) {
                    Text(stringResource(Res.string.create_token))
                }
            }
        },
        dismissButton = {
            if (!created) {
                TextButton(
                    enabled = !creating,
                    onClick = onDismiss,
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        },
    )
}
