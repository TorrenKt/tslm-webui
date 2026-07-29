package io.github.torrenkt.tslmwebui.view.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import io.github.torrenkt.tslmwebui.Res
import io.github.torrenkt.tslmwebui.close
import io.github.torrenkt.tslmwebui.created_token
import io.github.torrenkt.tslmwebui.refresh_token
import io.github.torrenkt.tslmwebui.refresh_token_success
import org.jetbrains.compose.resources.stringResource

@Composable
fun RefreshTokenDialog(
    token: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.refresh_token)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.refresh_token_success))
                Text(stringResource(Res.string.created_token))
                SelectionContainer { Text(token) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        },
    )
}
