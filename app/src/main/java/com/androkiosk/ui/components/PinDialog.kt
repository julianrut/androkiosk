package com.androkiosk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.androkiosk.R

private val PinLength = 4..8

@Composable
fun PinDialog(
    title: String,
    confirmLabel: String,
    errorLabel: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Boolean,
    requireRepeat: Boolean = false,
    extraAction: @Composable () -> Unit = {},
) {
    var pin by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var rejected by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    val valid = pin.length in PinLength && (!requireRepeat || repeat == pin)
    val submit = { if (!onSubmit(pin)) rejected = true }

    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PinField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter(Char::isDigit)
                        rejected = false
                    },
                    label = stringResource(R.string.pin_label),
                    isError = rejected,
                    imeAction = if (requireRepeat) ImeAction.Next else ImeAction.Done,
                    onDone = { if (valid) submit() },
                    modifier = Modifier.focusRequester(focus),
                )
                if (requireRepeat) {
                    PinField(
                        value = repeat,
                        onValueChange = { repeat = it.filter(Char::isDigit) },
                        label = stringResource(R.string.pin_repeat_label),
                        isError = repeat.isNotEmpty() && repeat != pin,
                        imeAction = ImeAction.Done,
                        onDone = { if (valid) submit() },
                    )
                }
                if (rejected) {
                    Text(
                        text = errorLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = valid) { Text(confirmLabel) }
        },
        dismissButton = {
            extraAction()
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    imeAction: ImeAction,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier,
    )
}
