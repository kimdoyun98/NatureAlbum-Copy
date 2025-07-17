package com.and04.naturealbum.ui.maps.navigation

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import com.and04.naturealbum.ui.maps.MapScreen
import com.and04.naturealbum.ui.maps.MapScreenViewModel
import com.and04.naturealbum.ui.maps.contract.MapEffect
import com.and04.naturealbum.ui.maps.contract.MapIntent
import com.and04.naturealbum.ui.maps.utils.ClusterManager
import com.and04.naturealbum.ui.maps.utils.PreloadState
import com.and04.naturealbum.ui.maps.utils.rememberMapInfo
import com.and04.naturealbum.ui.navigation.NatureAlbumNavigator
import com.and04.naturealbum.ui.navigation.NavigateDestination
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.overlay.OverlayImage
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

fun NavGraphBuilder.mapNavigation(
    navigator: NatureAlbumNavigator
) {
    composable(NavigateDestination.Map.route) {
        val context = LocalContext.current
        val viewModel: MapScreenViewModel = hiltViewModel()
        val state by viewModel.collectAsState()

        val mapInfo = rememberMapInfo(onIntent = viewModel::onIntent)

        viewModel.collectSideEffect { effect ->
            when (effect) {
                is MapEffect.NavigateHome -> {
                    navigator.popupBackStack()
                }

                is MapEffect.CameraPivotChanged -> {
                    mapInfo.mapView.getMapAsync { naverMap ->
                        state.pick?.let { pick ->
                            naverMap.moveCamera(
                                CameraUpdate.scrollTo(pick.position).pivot(state.cameraPivot)
                                    .animate(CameraAnimation.Easing, 500)
                            )
                        }
                    }
                }

                is MapEffect.PickChanged -> {
                    mapInfo.mapView.getMapAsync { naverMap ->
                        mapInfo.marker.map = state.pick?.let { pick ->
                            naverMap.moveCamera(
                                CameraUpdate.scrollTo(pick.position).pivot(state.cameraPivot)
                                    .animate(CameraAnimation.Easing, 500)
                            )
                            mapInfo.imageMarker.loadImage(pick.uri) {
                                mapInfo.marker.icon = OverlayImage.fromView(mapInfo.imageMarker)
                            }
                            mapInfo.marker.position = pick.position
                            naverMap
                        }
                    }

                    ClusterManager.updatePick(state.pick)
                }

                is MapEffect.PhotosByUidChanged -> {
                    val totalPhotos = state.photosByUid.values.flatten()

                    val imageLoader = ImageLoader.Builder(context)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .build()

                    state.scope?.launch {
                        totalPhotos.forEach { photo ->
                            launch {
                                preload(photo.uri, context, imageLoader, viewModel::onIntent)
                            }
                        }
                    }

                    mapInfo.clusterManagers.forEachIndexed { index, cluster ->
                        cluster.setPhotoItems(
                            state.photosByUid.keys.elementAtOrNull(index) ?: "",
                            state.photosByUid.values.elementAtOrNull(index) ?: emptyList()
                        )
                    }

                    viewModel.onIntent(MapIntent.InitMap.FriendsReload)

                    if (totalPhotos.isNotEmpty()) {
                        val bound = LatLngBounds.Builder().apply {
                            totalPhotos.forEach { photoItem ->
                                include(photoItem.position)
                            }
                        }.build()
                        mapInfo.mapView.getMapAsync { naverMap ->
                            naverMap.moveCamera(
                                CameraUpdate.fitBounds(bound, 300)
                                    .animate(CameraAnimation.Easing, 500)
                            )
                        }
                    }
                }
            }
        }

        MapScreen(
            state = { state },
            onIntent = viewModel::onIntent,
            mapInfo = mapInfo
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
