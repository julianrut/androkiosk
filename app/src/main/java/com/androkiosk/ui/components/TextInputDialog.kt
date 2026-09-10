package com.androkiosk.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.androkiosk.R

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    confirmLabel: String,
    errorLabel: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Boolean,
    initial: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    helperText: String? = null,
) {
    var value by remember { mutableStateOf(initial) }
    var rejected by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val submit = { if (!onSubmit(value)) rejected = true }

    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                    rejected = false
                },
                label = { Text(label) },
                singleLine = true,
                isError = rejected,
                supportingText = when {
                    rejected -> {
                        { Text(errorLabel) }
                    }
                    helperText != null -> {
                        { Text(helperText) }
                    }
                    else -> null
                },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = value.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
