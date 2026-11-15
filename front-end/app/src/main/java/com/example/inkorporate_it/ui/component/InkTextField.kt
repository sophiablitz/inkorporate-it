package com.example.inkorporate_it.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.inkorporate_it.ui.theme.InkorporateitTheme

/*
 * A one-line text field in the ink style: white fill, thick dark outline.
 *
 * Why not OutlinedTextField? Its outline is always 1dp (2dp when focused),
 * and the design wants a thick one. So this builds the same Material field
 * by hand (BasicTextField + Material's own DecorationBox), which lets us
 * set the outline thickness. Error, placeholder and icons work the same.
 *
 * - errorText: shown under the field in red (null = no error)
 * - textStyle: the text's style; the text is always centered
 */
@Composable
fun InkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    errorText: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    shape: Shape = RoundedCornerShape(18.dp),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val isError = errorText != null
    // Lets the outline know when the field is focused.
    val interactionSource = remember { MutableInteractionSource() }

    // Material's field colors, changed to the ink look (dark outline, white fill).
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.onBackground,
        unfocusedBorderColor = colors.onBackground,
        focusedContainerColor = colors.surfaceContainerLowest,
        unfocusedContainerColor = colors.surfaceContainerLowest,
        errorContainerColor = colors.surfaceContainerLowest,
        unfocusedPlaceholderColor = colors.outline,
        focusedPlaceholderColor = colors.outline
    )
    // Centered text in the theme's onBackground color.
    val centeredStyle = textStyle.copy(color = colors.onBackground, textAlign = TextAlign.Center)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        textStyle = centeredStyle,
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            // Material draws the placeholder, icon, error text and outline around our text.
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                isError = isError,
                placeholder = {
                    // Same style as the text, but grey (outline) so it reads as a hint.
                    Text(
                        text = placeholder,
                        style = centeredStyle.copy(color = colors.outline),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                // With an icon on the right, the text is centered in the space left over,
                // so it looks pushed to the left. An empty slot of the same size on the
                // left (both icon slots are 48dp wide) puts the text back in the middle.
                leadingIcon = if (trailingIcon != null) {
                    { Spacer(modifier = Modifier.size(24.dp)) }
                } else {
                    null
                },
                trailingIcon = trailingIcon,
                supportingText = errorText?.let { message -> { Text(message) } },
                colors = fieldColors,
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = fieldColors,
                        shape = shape,
                        focusedBorderThickness = 3.dp,          // a bit thicker while typing
                        unfocusedBorderThickness = InkOutlineWidth
                    )
                }
            )
        }
    )
}

@Preview(showBackground = true, name = "InkTextField: text, placeholder, error")
@Composable
fun InkTextFieldPreview() {
    InkorporateitTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InkTextField(value = "Jerry", onValueChange = {})
            InkTextField(value = "", onValueChange = {}, placeholder = "Code")
            InkTextField(value = "123456", onValueChange = {}, errorText = "Room not found")
        }
    }
}
