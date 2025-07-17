package com.and04.naturealbum.ui.maps

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.request.ImageRequest
import coil3.request.placeholder
import com.and04.naturealbum.R
import com.and04.naturealbum.ui.component.LabelChip
import com.and04.naturealbum.ui.component.LoadingAsyncImage
import com.and04.naturealbum.ui.component.LoadingIcons
import com.and04.naturealbum.ui.component.NetworkDisconnectContent
import com.and04.naturealbum.ui.component.PartialBottomSheet
import com.and04.naturealbum.ui.component.PhotoContent
import com.and04.naturealbum.ui.component.RotatingImageLoading
import com.and04.naturealbum.ui.maps.component.FriendDialog
import com.and04.naturealbum.ui.maps.contract.MapIntent
import com.and04.naturealbum.ui.maps.contract.MapState
import com.and04.naturealbum.ui.maps.utils.LabelItem
import com.and04.naturealbum.ui.maps.utils.MapInfo
import com.and04.naturealbum.ui.maps.utils.PhotoItem
import com.and04.naturealbum.ui.maps.utils.PreloadState
import com.and04.naturealbum.ui.utils.UserManager
import com.and04.naturealbum.utils.network.NetworkState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap

@Composable
fun MapScreen(
    state: () -> MapState,
    mapInfo: MapInfo,
    onIntent: (MapIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle

        lifecycle.addObserver(
            object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    when (event) {
                        Lifecycle.Event.ON_CREATE -> mapInfo.mapView.onCreate(null)
                        Lifecycle.Event.ON_START -> mapInfo.mapView.onStart()
                        Lifecycle.Event.ON_RESUME -> mapInfo.mapView.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapInfo.mapView.onPause()
                        Lifecycle.Event.ON_STOP -> mapInfo.mapView.onStop()
                        Lifecycle.Event.ON_DESTROY -> {
                            mapInfo.clusterManagers.forEach { cluster ->
                                cluster.clear()
                            }
                            mapInfo.mapView.onDestroy()
                            lifecycle.removeObserver(this)
                        }

                        else -> Unit
                    }
                }
            }
        )
    }

    BackHandler(
        enabled = (state().pick != null)
    ) {
        onIntent(MapIntent.InitMap.BackButtonClicked)
    }

    NatureAlbumMap(
        modifier = modifier,
        state = state,
        onIntent = onIntent,
        mapInfo = mapInfo,
    )
}

@Composable
private fun NatureAlbumMap(
    modifier: Modifier,
    state: () -> MapState,
    onIntent: (MapIntent) -> Unit,
    mapInfo: MapInfo,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (state().networkState == NetworkState.DISCONNECTED) {
            NetworkDisconnectContent()
        } else {
            AndroidView(factory = { mapInfo.mapView }, modifier = modifier.fillMaxSize())

            if (UserManager.isSignIn()) {
                IconButton(
                    onClick = {
                        onIntent(MapIntent.FriendIconClicked)
                    },
                    modifier = modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Diversity3,
                        contentDescription = stringResource(R.string.map_show_friend_map)
                    )
                }
            }

            PartialBottomSheet(
                isVisible = state().bottomSheetPhotos.isNotEmpty(),
                onCollapsed = { isCollapsed ->
                    onIntent(MapIntent.BottomSheetStateChanged(isCollapsed = isCollapsed))
                },
                modifier = modifier.padding(horizontal = 16.dp),
                fullExpansionSize = 0.95f
            ) {
                PhotoGrid(
                    photos = state().bottomSheetPhotos,
                    modifier = modifier,
                    onPhotoClick = { photo ->
                        onIntent(
                            MapIntent.PhotoClicked(
                                photo = photo,
                                isDoubleClicked = false
                            )
                        )
                    },
                    onPhotoDoubleClick = { photo ->
                        onIntent(
                            MapIntent.PhotoClicked(
                                photo = photo,
                                isDoubleClicked = true
                            )
                        )
                    },
                    preloadState = state().preloadState,
                )
            }

            FriendDialog(
                isOpen = state().openDialog,
                friends = state().friends,
                selectedFriends = state().selectedFriends,
                onDismiss = { onIntent(MapIntent.FriendDialogDisMiss) },
                onConfirm = { friends ->
                    onIntent(MapIntent.FriendDialogConfirm(friends))
                }
            )
            if (state().showPhotoContent) {
                PhotoContent(
                    imageUri = state().pick!!.uri,
                    contentDescription = state().pick!!.label.name,
                    onDismiss = { onIntent(MapIntent.PhotoContentDisMiss) }
                )
            }
        }
        IconButton(
            onClick = { onIntent(MapIntent.BackButtonClicked) },
            modifier = modifier
                .size(48.dp)
                .align(Alignment.TopStart),
        ) {
            Icon(
                modifier = modifier.size(24.dp),
                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = stringResource(R.string.map_arrow_back_button),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGrid(
    photos: ImmutableList<PhotoItem>,
    columnCount: Int = 3,
    modifier: Modifier = Modifier,
    onPhotoClick: (PhotoItem) -> Unit,
    onPhotoDoubleClick: (PhotoItem) -> Unit,
    preloadState: ImmutableMap<String, PreloadState>,
) {
    LabelGroupLazyColumn(
        photos = photos
    ) { label, photoList ->
        labelWithPhotos(
            label = {
                LabelChip(backgroundColor = label.color) {
                    Text(
                        text = label.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            columnCount = columnCount,
            photos = photoList,
        ) { item ->
            Row(
                modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item.forEach { photo ->
                    when (preloadState[photo.uri] ?: PreloadState.Fail) {
                        is PreloadState.Loading -> {
                            LoadingImage(modifier)
                        }

                        is PreloadState.Success, PreloadState.Fail -> {
                            LoadingAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(photo.uri)
                                    .placeholder(R.drawable.ic_image)
                                    .build(),
                                contentDescription = photo.label.name,
                                modifier = modifier
                                    .wrapContentSize(Alignment.Center)
                                    .aspectRatio(1f)
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.medium)
                                    .combinedClickable(
                                        onClick = { onPhotoClick(photo) },
                                        onDoubleClick = { onPhotoDoubleClick(photo) },
                                    ),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }

                }

                EmptySpace(modifier = modifier, count = columnCount - item.size)
            }
        }
    }
}

@Composable
private fun LabelGroupLazyColumn(
    modifier: Modifier = Modifier,
    photos: List<PhotoItem>,
    column: LazyListScope.(LabelItem, List<PhotoItem>) -> Unit,
) {
    val groupByLabel =
        photos
            .groupBy { photoItem -> photoItem.label }
            .toList()
            .sortedByDescending { (_, photoItem) -> photoItem.size }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        groupByLabel.forEach { (label, photos) ->
            column(label, photos)
        }
    }
}

private fun LazyListScope.labelWithPhotos(
    label: @Composable () -> Unit,
    columnCount: Int,
    photos: List<PhotoItem>,
    photosRow: @Composable (List<PhotoItem>) -> Unit
) {
    item { label() }

    items(photos.windowed(columnCount, columnCount, true)) { item ->
        photosRow(item)
    }
}

@Composable
private fun RowScope.EmptySpace(
    modifier: Modifier = Modifier,
    count: Int,
) {
    repeat(count) {
        Box(modifier = modifier.weight(1f))
    }
}

@Composable
private fun RowScope.LoadingImage(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .wrapContentSize(Alignment.Center)
            .aspectRatio(1f)
            .weight(1f)
            .clip(MaterialTheme.shapes.medium),
    ) {
        RotatingImageLoading(
            drawableRes = LoadingIcons.entries.random().id,
            stringRes = null,
        )
    }
}
