package com.and04.naturealbum.ui.maps.utils

import android.content.Context
import android.view.Gravity
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.and04.naturealbum.R
import com.and04.naturealbum.ui.maps.contract.MapIntent
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Stable
class MapInfo(val mapView: MapView) {
    lateinit var marker: Marker
    lateinit var imageMarker: ImageMarker
    lateinit var clusterManagers: ImmutableList<ClusterManager>

    fun setMarker(marker: Marker, imageMarker: ImageMarker) {
        this.marker = marker
        this.imageMarker = imageMarker
    }

    fun setClusterManager(clusterManagers: ImmutableList<ClusterManager>) {
        this.clusterManagers = clusterManagers
    }

    companion object {
        fun get(
            context: Context,
            clusterManagers: ImmutableList<ClusterManager>,
            onMapClicked: () -> Unit,
            onMarkerClicked: () -> Unit,
        ): MapInfo {
            return MapInfo(
                mapViewSettings(MapView(context), clusterManagers) {
                    onMapClicked()
                }
            ).apply {
                setMarker(
                    marker = Marker().apply {
                        onClickListener = Overlay.OnClickListener {
                            onMarkerClicked()
                            true
                        }
                    },

                    imageMarker = ImageMarker(context).apply {
                        visibility = View.INVISIBLE
                        mapView.addView(this)
                    }
                )

                setClusterManager(clusterManagers)
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
    }
}

@Composable
fun rememberMapInfo(
    context: Context = LocalContext.current,
    onIntent: (MapIntent) -> Unit,
): MapInfo {
    return remember {
        MapInfo.get(
            context = context,
            clusterManagers = ClusterManager.getList(
                onClusterClicked = { bottomSheetPhotos ->
                    onIntent(
                        MapIntent.ClusterClicked(
                            bottomSheetPhotos = bottomSheetPhotos.toImmutableList(),
                            pick = bottomSheetPhotos
                                .groupBy { photoItem -> photoItem.label }
                                .maxBy { (_, photoItems) -> photoItems.size }.value
                                .maxBy { photoItem -> photoItem.time }
                        )
                    )
                },
                onClusterChanged = { changedCluster ->
                    onIntent(
                        MapIntent.ClusterChanged(bottomSheetPhotos = changedCluster.toImmutableList())
                    )
                }
            ),
            onMapClicked = { onIntent(MapIntent.InitMap.MapClicked) },
            onMarkerClicked = { onIntent(MapIntent.MarkerClicked) }
        )
    }
}
