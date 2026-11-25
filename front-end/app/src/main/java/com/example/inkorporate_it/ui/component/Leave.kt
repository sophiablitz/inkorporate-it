package com.example.inkorporate_it.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

@Composable
fun LeaveButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp),
    ) {
        Text("Leave")
    }
}

/**
 * Asks "Leave the game?" before the player leaves.
 * - onConfirm: the player tapped Leave. Leave the game.
 * - onDismiss: the player tapped Stay, tapped outside the dialog, or used Back. Close the dialog.
 */
@Composable
fun LeaveGameDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text("Leave the game?") },
        text = { Text("You'll leave this room. To come back, join again with the game code.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                // Red text: leaving is the action that can't be undone.
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Leave")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Stay")
            }
        },
    )
}

@Preview(showBackground = true, name = "Leave dialog, light")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Leave dialog, dark")
@Composable
fun LeaveGameDialogPreview() {
    InkorporateitTheme(dynamicColor = false) {
        LeaveGameDialog(onConfirm = {}, onDismiss = {})
    }
}
