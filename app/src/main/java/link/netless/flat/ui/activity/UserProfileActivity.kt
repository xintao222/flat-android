package link.netless.flat.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import link.netless.flat.R
import link.netless.flat.ui.activity.ui.theme.FlatCommonTextStyle
import link.netless.flat.ui.activity.ui.theme.FlatCommonTipTextStyle
import link.netless.flat.ui.compose.BackTopAppBar
import link.netless.flat.ui.compose.FlatColumnPage

@AndroidEntryPoint
class UserProfileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatColumnPage {
                BackTopAppBar(
                    title = stringResource(id = R.string.title_user_profile),
                    { finish() })
                SettingList()
            }
        }
    }
}

@Composable
private fun SettingList() {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        item {
            Item("匿名", "Hello", onClickOrNull = {})
            Divider(Modifier.padding(start = 16.dp, end = 16.dp), thickness = 1.dp)
            Item("微信", "HelloWx")
        }
    }
}

@Composable
private fun Item(
    tip: String,
    desc: String = "",
    onClickOrNull: (() -> Unit)? = null
) {
    // TODO
    val modifier = if (onClickOrNull != null) {
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClickOrNull)
    } else {
        Modifier
            .fillMaxWidth()
            .height(48.dp)
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = tip, style = FlatCommonTextStyle)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = desc, style = FlatCommonTipTextStyle)
        if (onClickOrNull != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Outlined.NavigateNext, contentDescription = null)
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {

}
