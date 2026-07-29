package io.github.torrenkt.tslmwebui.view.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import io.github.torrenkt.tslmwebui.Res
import io.github.torrenkt.tslmwebui.cancel
import io.github.torrenkt.tslmwebui.confirm
import io.github.torrenkt.tslmwebui.confirm_refresh_token
import io.github.torrenkt.tslmwebui.confirm_refresh_token_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConfirmRefreshTokenDialog(
    refreshing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!refreshing) {
                onDismiss()
            }
        },
        title = { Text(stringResource(Res.string.confirm_refresh_token_title)) },
        text = { Text(stringResource(Res.string.confirm_refresh_token)) },
        confirmButton = {
            TextButton(enabled = !refreshing, onClick = onConfirm) {
                Text(stringResource(Res.string.confirm))
            }
        },
        dismissButton = {
            TextButton(enabled = !refreshing, onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
