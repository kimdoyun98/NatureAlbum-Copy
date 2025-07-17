package com.and04.naturealbum.ui.maps

import android.content.Context
import android.view.Gravity
import android.view.View
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.request.placeholder
import com.and04.naturealbum.NatureAlbum
import com.and04.naturealbum.R
import com.and04.naturealbum.ui.component.LabelChip
import com.and04.naturealbum.ui.component.LoadingAsyncImage
import com.and04.naturealbum.ui.component.LoadingIcons
import com.and04.naturealbum.ui.component.NetworkDisconnectContent
import com.and04.naturealbum.ui.component.PartialBottomSheet
import com.and04.naturealbum.ui.component.PhotoContent
import com.and04.naturealbum.ui.component.RotatingImageLoading
import com.and04.naturealbum.ui.maps.component.FriendDialog
import com.and04.naturealbum.ui.maps.contract.MapEffect
import com.and04.naturealbum.ui.maps.contract.MapIntent
import com.and04.naturealbum.ui.maps.contract.MapState
import com.and04.naturealbum.ui.maps.utils.ClusterManager
import com.and04.naturealbum.ui.maps.utils.ImageMarker
import com.and04.naturealbum.ui.maps.utils.LabelItem
import com.and04.naturealbum.ui.maps.utils.MapInfo
import com.and04.naturealbum.ui.maps.utils.PhotoItem
import com.and04.naturealbum.ui.maps.utils.PreloadState
import com.and04.naturealbum.ui.utils.UserManager
import com.and04.naturealbum.utils.network.NetworkState
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    state: () -> MapState,
    sideEffect: @Composable ((suspend (sideEffect: MapEffect) -> Unit)) -> Unit,
    onIntent: (MapIntent) -> Unit,
    navigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val clusterManagers =
        remember {
            ClusterManager.getList(
                onIntent = onIntent
            )
        }

    val mapInfo = remember {
        MapInfo(
            mapViewSettings(MapView(context), clusterManagers) {
                onIntent(MapIntent.InitMap.MapClicked)
            }
        ).apply {
            setMarker(
                marker = Marker().apply {
                    onClickListener = Overlay.OnClickListener {
                        onIntent(MapIntent.MarkerClicked)
                        true
                    }
                },

                imageMarker = ImageMarker(context).apply {
                    visibility = View.INVISIBLE
                    mapView.addView(this)
                }
            )
        }
    }

    EffectCollection(
        sideEffect = sideEffect,
        onIntent = onIntent,
        navigateToHome = navigateToHome,
        state = state,
        mapInfo = mapInfo,
        clusterManagers = clusterManagers,
    )

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
            // AndroidView를 MapView로 바로 설정
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

@Composable
private fun EffectCollection(
    sideEffect: @Composable ((suspend (sideEffect: MapEffect) -> Unit)) -> Unit,
    state: () -> MapState,
    onIntent: (MapIntent) -> Unit,
    navigateToHome: () -> Unit,
    mapInfo: MapInfo,
    clusterManagers: ImmutableList<ClusterManager>,
) {
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    sideEffect { effect ->
        when (effect) {
            is MapEffect.NavigateHome -> {
                navigateToHome()
            }

            is MapEffect.CameraPivotChanged -> {
                mapInfo.mapView.getMapAsync { naverMap ->
                    state().pick?.let { pick ->
                        naverMap.moveCamera(
                            CameraUpdate.scrollTo(pick.position).pivot(state().cameraPivot)
                                .animate(CameraAnimation.Easing, 500)
                        )
                    }
                }
            }

            is MapEffect.PickChanged -> {
                mapInfo.mapView.getMapAsync { naverMap ->
                    mapInfo.marker.map = state().pick?.let { pick ->
                        naverMap.moveCamera(
                            CameraUpdate.scrollTo(pick.position).pivot(state().cameraPivot)
                                .animate(CameraAnimation.Easing, 500)
                        )
                        mapInfo.imageMarker.loadImage(pick.uri) {
                            mapInfo.marker.icon = OverlayImage.fromView(mapInfo.imageMarker)
                        }
                        mapInfo.marker.position = pick.position
                        naverMap
                    }
                }

                ClusterManager.updatePick(state().pick)
            }

            is MapEffect.PhotosByUidChanged -> {
                val totalPhotos = state().photosByUid.values.flatten()
                val context = NatureAlbum.getInstance()

                val imageLoader = ImageLoader.Builder(context)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build()

                state().scope?.launch {
                    totalPhotos.forEach { photo ->
                        launch {
                            preload(photo.uri, context, imageLoader, onIntent)
                        }
                    }
                }

                clusterManagers.forEachIndexed { index, cluster ->
                    cluster.setPhotoItems(
                        state().photosByUid.keys.elementAtOrNull(index) ?: "",
                        state().photosByUid.values.elementAtOrNull(index) ?: emptyList()
                    )
                }

                onIntent(MapIntent.InitMap.FriendsReload)

                if (totalPhotos.isNotEmpty()) {
                    val bound = LatLngBounds.Builder().apply {
                        totalPhotos.forEach { photoItem ->
                            include(photoItem.position)
                        }
                    }.build()
                    mapInfo.mapView.getMapAsync { naverMap ->
                        naverMap.moveCamera(
                            CameraUpdate.fitBounds(bound, 300).animate(CameraAnimation.Easing, 500)
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle

        val observer = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> mapInfo.mapView.onCreate(null)
                    Lifecycle.Event.ON_START -> mapInfo.mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapInfo.mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapInfo.mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapInfo.mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> {
                        clusterManagers.forEach { cluster ->
                            cluster.clear()
                        }
                        mapInfo.mapView.onDestroy()
                        lifecycle.removeObserver(this)
                    }

                    else -> {}
                }
            }
        }

        lifecycle.addObserver(observer)

        onDispose {
        }
    }

    BackHandler(
        enabled = (state().pick != null)
    ) {
        onIntent(MapIntent.InitMap.BackButtonClicked)
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

private fun mapViewSettings(
    mapview: MapView,
    clusterManagers: ImmutableList<ClusterManager>,
    onMapClick: () -> Unit
): MapView {
    return mapview.apply {
        id = R.id.map_view_id
        getMapAsync { naverMap ->
            clusterManagers.forEach { cluster ->
                cluster.setMap(naverMap)
            }
            naverMap.maxZoom = 18.0
            naverMap.onMapClickListener = NaverMap.OnMapClickListener { _, _ ->
                onMapClick()
            }
            val uiSettings = naverMap.uiSettings
            uiSettings.logoGravity = Gravity.TOP or Gravity.END
            uiSettings.setLogoMargin(0, 16, 160, 0)
            uiSettings.isCompassEnabled = false
            uiSettings.isScaleBarEnabled = false
            uiSettings.isZoomControlEnabled = false
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

private fun preload(
    url: String,
    context: Context,
    imageLoader: ImageLoader,
    onIntent: (MapIntent) -> Unit,
) {
    val request = ImageRequest.Builder(context)
        .data(url)
        .listener(object : ImageRequest.Listener {
            override fun onStart(request: ImageRequest) {
                onIntent(MapIntent.PreloadListener(url to PreloadState.Loading))
            }

            override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                onIntent(MapIntent.PreloadListener(url to PreloadState.Success))
            }

            override fun onError(request: ImageRequest, result: ErrorResult) {
                onIntent(MapIntent.PreloadListener(url to PreloadState.Fail))
            }
        })
        .crossfade(true)
        .build()

    imageLoader.enqueue(request)
}
