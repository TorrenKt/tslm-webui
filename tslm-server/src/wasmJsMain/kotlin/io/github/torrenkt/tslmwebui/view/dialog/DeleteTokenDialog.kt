package io.github.torrenkt.tslmwebui.view.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import io.github.torrenkt.tslmwebui.Res
import io.github.torrenkt.tslmwebui.cancel
import io.github.torrenkt.tslmwebui.confirm
import io.github.torrenkt.tslmwebui.confirm_delete_token
import io.github.torrenkt.tslmwebui.confirm_delete_token_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeleteTokenDialog(
    count: Int,
    deleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!deleting) {
                onDismiss()
            }
        },
        title = { Text(stringResource(Res.string.confirm_delete_token_title)) },
        text = { Text(stringResource(Res.string.confirm_delete_token, count)) },
        confirmButton = {
            TextButton(
                enabled = !deleting,
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(Res.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                enabled = !deleting,
                onClick = onDismiss,
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
