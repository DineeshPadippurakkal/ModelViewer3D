package com.modelviewer3d.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.modelviewer3d.model.ModelItem

@Composable
fun ModelPickerDialog(onSelect: (ModelItem) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Model") },
        text = {
            Column {
                ModelItem.entries.forEach { model ->
                    TextButton(
                        onClick = { onSelect(model) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(model.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
