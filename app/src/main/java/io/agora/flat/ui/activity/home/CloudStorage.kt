package io.agora.flat.ui.activity.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.agora.flat.R
import io.agora.flat.data.model.FileConvertStep
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.FlatTopAppBar
import io.agora.flat.ui.theme.*
import io.agora.flat.util.FlatFormatter
import io.agora.flat.util.contentFileInfo

@Composable
fun CloudStorage() {
    val viewModel = viewModel(CloudStorageViewModel::class.java)
    val viewState by viewModel.state.collectAsState()

    CloudStorage(viewState) { action ->
        when (action) {
            is CloudStorageUIAction.CheckItem -> viewModel.checkItem(action)
            is CloudStorageUIAction.Delete -> viewModel.deleteChecked()
            is CloudStorageUIAction.Reload -> viewModel.reloadFileList()
            is CloudStorageUIAction.UploadFile -> viewModel.uploadFile(action)
            else -> ""
        }
    }
}

@Composable
internal fun CloudStorage(viewState: CloudStorageViewState, actioner: (CloudStorageUIAction) -> Unit) {
    FlatPage {
        Column {
            FlatCloudStorageTopBar()

            SwipeRefresh(
                state = rememberSwipeRefreshState(viewState.refreshing),
                onRefresh = { actioner(CloudStorageUIAction.Reload) },
                indicator = { state, trigger ->
                    SwipeRefreshIndicator(
                        state = state,
                        refreshTriggerDistance = trigger,
                        contentColor = MaterialTheme.colors.primary,
                    )
                }) {
                Box(Modifier.fillMaxSize()) {
                    CloudStorageContent(viewState.totalUsage, viewState.files, actioner)

                    CloudStorageAddFile(actioner)
                }
            }
        }
        if (viewState.uploadFiles.isNotEmpty()) {
            UploadList()
        }
    }
}

@Composable
private fun BoxScope.CloudStorageAddFile(actioner: (CloudStorageUIAction) -> Unit) {
    var showPick by remember { mutableStateOf(false) }

    FloatingActionButton(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
        onClick = { showPick = true },
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = FlatColorWhite)
    }

    val aniValue: Float by animateFloatAsState(if (showPick) 1f else 0f)
    if (aniValue > 0) {
        UpdatePickLayout(aniValue, actioner = {
            actioner(it)
            showPick = false
        }) {
            showPick = false
        }
    }
}

@Composable
private fun UpdatePickLayout(aniValue: Float, actioner: (CloudStorageUIAction) -> Unit, onCoverClick: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        it?.run {
            val info = context.contentFileInfo(this) ?: return@run
            actioner(CloudStorageUIAction.UploadFile(filename = info.filename, size = info.size, uri = this))
        }
    }

    Column {
        Box(Modifier
            .fillMaxWidth()
            .weight(1f)
            .graphicsLayer(alpha = aniValue)
            .background(Color(0x52000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCoverClick() }) {
        }
        Box(Modifier
            .height(160.dp * aniValue)
            .fillMaxWidth()
            .background(FlatColorWhite)) {
            Box(Modifier
                .align(Alignment.TopCenter)
                .clickable { onCoverClick() }) {
                Image(
                    painter = painterResource(R.drawable.ic_record_arrow_down),
                    contentDescription = "",
                    Modifier.padding(4.dp)
                )
            }
            Row(Modifier.align(Alignment.Center)) {
                UpdatePickItem(R.drawable.ic_cloud_storage_image, R.string.cloud_storage_upload_image) {
                    launcher.launch("image/*")
                }
                UpdatePickItem(R.drawable.ic_cloud_storage_video, R.string.cloud_storage_upload_video) {
                    launcher.launch("video/*")
                }
                UpdatePickItem(R.drawable.ic_cloud_storage_music, R.string.cloud_storage_upload_music) {
                    launcher.launch("audio/*")
                }
                UpdatePickItem(R.drawable.ic_cloud_storage_doc, R.string.cloud_storage_upload_doc) {
                    launcher.launch("*/*")
                }
            }
        }
    }
}

@Composable
private fun RowScope.UpdatePickItem(@DrawableRes id: Int, @StringRes text: Int, onClick: () -> Unit) {
    Column(Modifier
        .weight(1F)
        .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Image(painter = painterResource(id), contentDescription = "", Modifier.size(48.dp))
        Text(text = stringResource(text))
    }
}


@Composable
internal fun CloudStorageContent(
    totalUsage: Long,
    files: List<CloudStorageUIFile>,
    actioner: (CloudStorageUIAction) -> Unit,
) {
    Column {
        Row(Modifier
            .fillMaxWidth()
            .background(FlatColorBackground), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.cloud_storage_usage_format, FlatFormatter.size(totalUsage)),
                color = FlatColorTextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { actioner(CloudStorageUIAction.Delete) }) {
                Text(text = stringResource(id = R.string.delete), color = Color.Red)
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(count = files.size, key = { index: Int ->
                files[index].fileUUID
            }) { index ->
                CloudStorageItem(files[index],
                    onCheckedChange = { checked -> actioner(CloudStorageUIAction.CheckItem(index, checked)) })
            }
        }
    }
}

@Composable
private fun CloudStorageItem(file: CloudStorageUIFile, onCheckedChange: ((Boolean) -> Unit)) {
    // "jpg", "jpeg", "png", "webp","doc", "docx", "ppt", "pptx", "pdf"
    val imageId = when (file.fileURL.substringAfterLast('.').toLowerCase()) {
        "jpg", "jpeg", "png", "webp" -> R.drawable.ic_cloud_storage_image
        "ppt", "pptx" -> R.drawable.ic_cloud_storage_ppt
        "pdf" -> R.drawable.ic_cloud_storage_pdf
        "mp4" -> R.drawable.ic_cloud_storage_video
        "mp3", "aac" -> R.drawable.ic_cloud_storage_music
        else -> R.drawable.ic_cloud_storage_doc
    }
    Column(Modifier.height(68.dp)) {
        Row(MaxWidthSpread, verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(12.dp))
            Box {
                Image(
                    painterResource(imageId),
                    contentDescription = ""
                )
                if (file.convertStep == FileConvertStep.Converting) {
                    ConvertingImage(Modifier.align(Alignment.BottomEnd))
                } else if (file.convertStep == FileConvertStep.Failed) {
                    Icon(painter = painterResource(R.drawable.ic_cloud_storage_convert_failure),
                        contentDescription = "",
                        Modifier.align(Alignment.BottomEnd),
                        tint = Color.Unspecified)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(text = file.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(text = FlatFormatter.dateDash(file.createAt))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = FlatFormatter.size(file.fileSize.toLong()))
                }
            }
            Checkbox(checked = file.checked, onCheckedChange = onCheckedChange, Modifier.padding(3.dp))
            Spacer(modifier = Modifier.width(12.dp))
        }
        Divider(modifier = Modifier.padding(start = 52.dp, end = 12.dp), thickness = 1.dp, color = FlatColorDivider)
    }
}

@Composable
fun ConvertingImage(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()

    val angle: Float by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = 360F,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
        )
    )

    Icon(painter = painterResource(R.drawable.ic_cloud_storage_converting),
        contentDescription = "",
        modifier.rotate(angle),
        tint = Color.Unspecified)
}

@Composable
private fun FlatCloudStorageTopBar() {
    FlatTopAppBar(title = { Text(stringResource(id = R.string.title_cloud_storage), style = FlatTitleTextStyle) })
}

@Composable
@Preview
private fun CloudStoragePreview() {
    val files = listOf(
        CloudStorageUIFile("1",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.jpg",
            1111024,
            createAt = 1627898586449,
            fileURL = "",
            convertStep = FileConvertStep.Done,
            taskUUID = "",
            taskToken = ""),
        CloudStorageUIFile("2",
            "2.doc",
            111024,
            createAt = 1627818586449,
            fileURL = "",
            convertStep = FileConvertStep.Done,
            taskUUID = "",
            taskToken = ""),
        CloudStorageUIFile("3",
            "3.mp4",
            111111024,
            createAt = 1617898586449,
            fileURL = "",
            convertStep = FileConvertStep.Done,
            taskUUID = "",
            taskToken = ""),
    )
    val viewState = CloudStorageViewState(
        files = files
    )
    CloudStorage(viewState) {

    }
}

@Composable
@Preview
private fun UpdatePickDialogPreview() {
    UpdatePickLayout(1f, {}) {

    }
}
