package io.agora.flat.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.agora.flat.R
import io.agora.flat.ui.theme.*

@Composable
fun FlatPrimaryTextButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val darkMode = isDarkTheme()
    val colors = ButtonDefaults.textButtonColors(
        backgroundColor = if (enabled) {
            if (darkMode) Blue_7 else Blue_6
        } else {
            if (darkMode) Gray_9 else Gray_2
        },
        contentColor = if (darkMode) Gray_0 else Gray_0,
        disabledContentColor = if (darkMode) Gray_7 else Gray_5
    )
    TextButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        enabled = enabled,
        colors = colors,
        shape = Shapes.small,
        onClick = onClick
    ) {
        FlatTextButton(text)
    }
}

@Composable
fun FlatSecondaryTextButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val darkMode = isDarkTheme()
    val colors = ButtonDefaults.outlinedButtonColors(
        contentColor = if (darkMode) Gray_4 else Gray_6,
        disabledContentColor = if (darkMode) Gray_7 else Gray_5
    )

    OutlinedButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        enabled = enabled,
        shape = Shapes.small,
        border = BorderStroke(1.dp, if (darkMode) Gray_6 else Gray_3),
        colors = colors,
        onClick = onClick
    ) {
        FlatTextButton(text)
    }
}

@Composable
fun FlatHighlightTextButton(
    text: String,
    icon: Int = 0,
    color: Color = FlatColorRed,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        enabled = enabled,
        shape = Shapes.small,
        border = BorderStroke(1.dp, color),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        onClick = onClick
    ) {
        if (icon != 0) {
            Icon(painterResource(icon), contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
        }
        FlatTextButton(text)
    }
}

@Composable
fun FlatSmallPrimaryTextButton(
    text: String,
    modifier: Modifier = Modifier.defaultMinSize(minWidth = 86.dp),
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val darkMode = isDarkTheme()
    val colors = ButtonDefaults.textButtonColors(
        backgroundColor = if (enabled) {
            if (darkMode) Blue_7 else Blue_6
        } else {
            if (darkMode) Gray_9 else Gray_2
        },
        contentColor = if (darkMode) Gray_0 else Gray_0,
        disabledContentColor = if (darkMode) Gray_7 else Gray_5
    )

    TextButton(
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        shape = Shapes.small,
        onClick = onClick
    ) {
        FlatTextButton(text)
    }
}

@Composable
fun FlatSmallSecondaryTextButton(
    text: String,
    modifier: Modifier = Modifier.defaultMinSize(minWidth = 86.dp),
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val darkMode = isDarkTheme()
    val colors = ButtonDefaults.outlinedButtonColors(
        contentColor = if (darkMode) Gray_4 else Gray_6,
        disabledContentColor = if (darkMode) Gray_7 else Gray_5
    )

    OutlinedButton(
        modifier = modifier,
        enabled = enabled,
        shape = Shapes.small,
        border = BorderStroke(1.dp, FlatColorGray),
        colors = colors,
        onClick = onClick
    ) {
        FlatTextButton(text)
    }
}

@Composable
@Preview
@Preview(uiMode = 0x20)
private fun FlatTextButtonPreview() {
    FlatColumnPage {
        FlatPrimaryTextButton("TextButton", onClick = {})
        FlatNormalVerticalSpacer()
        FlatPrimaryTextButton("TextButton", enabled = false, onClick = {})

        FlatLargeVerticalSpacer()

        FlatSecondaryTextButton("TextButton", onClick = {})
        FlatNormalVerticalSpacer()
        FlatSecondaryTextButton("TextButton", enabled = false, onClick = {})

        FlatNormalVerticalSpacer()
        FlatHighlightTextButton("TextButton", icon = R.drawable.ic_login_out, enabled = true, onClick = {})

        FlatNormalVerticalSpacer()
        FlatSmallPrimaryTextButton("TextButton", onClick = {})
        FlatNormalVerticalSpacer()
        FlatSmallSecondaryTextButton("TextButton") {}
    }
}