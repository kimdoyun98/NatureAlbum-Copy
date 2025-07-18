package com.and04.naturealbum.ui.add.savephoto

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import coil3.Bitmap
import com.and04.naturealbum.R
import com.and04.naturealbum.data.localdata.room.Label
import com.and04.naturealbum.ui.add.savephoto.contract.SavePhotoIntent
import com.and04.naturealbum.ui.add.savephoto.contract.SavePhotoState
import com.and04.naturealbum.ui.component.AppBarType
import com.and04.naturealbum.ui.component.BackgroundImage
import com.and04.naturealbum.ui.component.LabelChip
import com.and04.naturealbum.ui.component.ProgressIndicator
import com.and04.naturealbum.ui.component.RotatingImageLoading
import com.and04.naturealbum.ui.theme.NatureAlbumTheme
import com.and04.naturealbum.ui.utils.UiState
import com.and04.naturealbum.ui.utils.UiStatus
import com.and04.naturealbum.utils.GetTopBar
import com.and04.naturealbum.utils.isPortrait
import com.and04.naturealbum.utils.network.NetworkState
import com.and04.naturealbum.utils.network.NetworkState.DISCONNECTED
import java.io.IOException

@Composable
fun SavePhotoScreen(
    state: () -> SavePhotoState,
    initState: () -> SavePhotoState,
    changeState: (SavePhotoState) -> Unit,
    onIntent: (SavePhotoIntent) -> Unit,
) {
    //val vertexAIState = viewModel.vertexAIState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            context.GetTopBar(
                title = stringResource(R.string.topbar_title_add_album),
                type = AppBarType.All,
                navigateToMyPage = { onIntent(SavePhotoIntent.MyPageButtonClicked) },
                navigateToBackScreen = { onIntent(SavePhotoIntent.BackButtonClicked) },
            )
        },
    ) { innerPadding ->
        BackgroundImage()

        when (state().status) {
            is UiStatus.Idle -> {
                changeState(
                    if (initState().location == null) initState()
                    else initState().copy(status = UiStatus.Success)
                )
            }

            is UiStatus.Loading -> {
                state().getLocation { location ->
                    changeState(state().copy(status = UiStatus.Success, location = location))
                }

                Box(modifier = Modifier.padding(innerPadding)) {
                    ProgressIndicator(state().location == null)
                }
            }

            is UiStatus.Success -> {
                SavePhotoContent(
                    innerPadding = innerPadding,
                    state = state,
                    onIntent = onIntent,
                )
            }
        }
    }

    when (state().saveState) {
        is UiState.Loading -> {
            RotatingImageLoading(
                drawableRes = R.drawable.fish_loading_image,
                stringRes = R.string.save_photo_screen_loading,
            )
        }

        is UiState.Success -> {
            state().onSave()
        }

        else -> Unit
    }

    BackHandler(onBack = { onIntent(SavePhotoIntent.BackButtonClicked) })

    //    vertexAI(
    //        context = context,
    //        uri = initState.uri,
    //        vertexAIState = vertexAIState,
    //        getGeneratedContent = viewModel::getGeneratedContent
    //    )
}

@Composable
private fun SavePhotoContent(
    innerPadding: PaddingValues,
    state: () -> SavePhotoState,
    onIntent: (SavePhotoIntent) -> Unit
) {
    val context = LocalContext.current
    if (context.isPortrait()) {
        SavePhotoScreenPortrait(
            innerPadding = innerPadding,
            state = state,
            onIntent = onIntent,
        )
    } else {
        SavePhotoScreenLandscape(
            innerPadding = innerPadding,
            state = state,
            onIntent = onIntent,
        )
    }
}

@Composable
fun ToggleButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = { onClick() }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = { onClick() },
            modifier = modifier
                .size(24.dp)
                .focusable(false),
        )
        Text(stringResource(R.string.save_photo_screen_set_represent))
    }
}

@Composable
fun ColumnScope.SavePhotoBody(
    state: () -> SavePhotoState,
    onIntent: (SavePhotoIntent) -> Unit,
) {
    LabelSelection(
        label = { state().appState?.selectedLabel?.value },
        onClick = state().onLabelSelect,
    )

    Description(
        description = { state().description },
        modifier = Modifier.weight(1f),
        onValueChange = { newDescription ->
            onIntent(
                SavePhotoIntent.DescriptionInput(
                    newDescription
                )
            )
        }
    )
}

@Composable
fun SavePhotoFooter(
    state: () -> SavePhotoState,
    onIntent: (SavePhotoIntent) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(30.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTextButton(
            modifier = Modifier.weight(1f),
            imageVector = Icons.Default.Close,
            stringRes = R.string.save_photo_screen_cancel,
            onClick = { onIntent(SavePhotoIntent.CancelButtonClicked) })

        IconTextButton(
            enabled = (state().appState?.selectedLabel?.value != null) && (state().saveState != UiState.Loading),
            modifier = Modifier.weight(1f),
            imageVector = Icons.Outlined.Create,
            stringRes = R.string.save_photo_screen_save,
            onClick = { onIntent(SavePhotoIntent.SaveButtonClicked) }
        )
    }
}

@Composable
private fun IconTextButton(
    enabled: Boolean = true,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    @StringRes stringRes: Int,
    onClick: () -> Unit,
) {
    Button(
        enabled = enabled,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = imageVector,
                contentDescription = null
            )
            Text(
                text = stringResource(stringRes),
                textAlign = TextAlign.Center,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun LabelSelection(
    label: () -> Label?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 12.dp)) {
        Text(
            stringResource(R.string.save_photo_screen_label),
            style = MaterialTheme.typography.headlineLarge,
            fontSize = TextUnit(20f, TextUnitType.Sp),
        )

        Button(
            onClick = { onClick() },
            modifier = modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null
                )
                Box(
                    modifier = modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                ) {
                    label()?.let { label ->
                        LabelChip(
                            modifier = Modifier.heightIn(max = 24.dp),
                            backgroundColor = label.backgroundColor,
                            onClick = onClick
                        ) {
                            Text(text = label.name)
                        }
                    } ?: Text(text = stringResource(R.string.save_photo_screen_select_label))
                }
            }
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun Description(
    description: () -> String,
    modifier: Modifier,
    onValueChange: (String) -> Unit,
) {
    Column(
        modifier = modifier.padding(12.dp)
    ) {
        Text(
            stringResource(R.string.save_photo_screen_description),
            style = MaterialTheme.typography.headlineLarge,
            fontSize = TextUnit(20f, TextUnitType.Sp),
        )
        TextField(
            value = description(),
            onValueChange = { text -> onValueChange(text) },
            placeholder = { Text(stringResource(R.string.save_photo_screen_description_about_photo)) },
            modifier = modifier
                .weight(1f)
                .fillMaxWidth(),
        )
    }
}

private fun vertexAI(
    context: Context,
    uri: Uri,
    vertexAIState: State<UiState<String>>,
    getGeneratedContent: (Bitmap?) -> Unit
) {
    if (vertexAIState.value is UiState.Idle && NetworkState.getNetWorkCode() != DISCONNECTED) {
        val bitmap = loadImageFromUri(context, uri)
        getGeneratedContent(bitmap)
    }
}

private fun loadImageFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: IOException) {
        null
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun ScreenPreview() {
    NatureAlbumTheme {
        val state = SavePhotoState()

        SavePhotoScreen(
            state = { state },
            initState = { state },
            changeState = {},
            onIntent = {},
        )
    }
}
