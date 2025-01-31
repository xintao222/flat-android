package io.agora.flat.ui.activity.setting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.compose.FlatTextBodyOne
import io.agora.flat.ui.compose.FlatTextBodyOneSecondary
import io.agora.flat.ui.viewmodel.UserViewModel

@AndroidEntryPoint
class UserInfoActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatColumnPage {
                val viewModel = viewModel(UserViewModel::class.java)
                val userInfo = viewModel.userInfo.collectAsState()

                BackTopAppBar(stringResource(R.string.title_user_info), { finish() })
                userInfo.value?.apply {
                    SettingList(name)
                }
            }
        }
    }
}

@Composable
private fun SettingList(name: String) {
    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            // Item(stringResource(R.string.nickname), "", onClickOrNull = {})
            // Divider(Modifier.padding(start = 16.dp, end = 16.dp), thickness = 1.dp)
            Item(tip = stringResource(R.string.username), desc = name)
        }
    }
}

@Composable
private fun Item(
    tip: String,
    desc: String = "",
    onClickOrNull: (() -> Unit)? = null,
) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)

    if (onClickOrNull != null) {
        modifier.clickable(onClick = onClickOrNull)
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(16.dp))
        FlatTextBodyOne(tip)
        Spacer(modifier = Modifier.weight(1f))
        FlatTextBodyOneSecondary(desc)
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
    FlatColumnPage {
        SettingList("Name")
    }
}
