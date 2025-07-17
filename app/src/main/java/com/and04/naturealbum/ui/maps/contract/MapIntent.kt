package com.and04.naturealbum.ui.maps.contract

import com.and04.naturealbum.data.dto.FirebaseFriend
import com.and04.naturealbum.ui.maps.utils.PhotoItem
import com.and04.naturealbum.ui.maps.utils.PreloadState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap

sealed interface MapIntent {
    data object BackButtonClicked : MapIntent

    data class PickChanged(val pick: PhotoItem?) : MapIntent

    data class ClusterClicked(
        val pick: PhotoItem?,
        val bottomSheetPhotos: ImmutableList<PhotoItem>
    ) : MapIntent

    data class ClusterChanged(
        val bottomSheetPhotos: ImmutableList<PhotoItem>
    ) : MapIntent

    sealed class InitMap : MapIntent {
        data object MapClicked : InitMap()
        data object BackButtonClicked : InitMap()
        data object FriendsReload : InitMap()
    }

    data object MarkerClicked : MapIntent

    data object FriendIconClicked : MapIntent

    data class BottomSheetStateChanged(val isCollapsed: Boolean) : MapIntent

    data object PhotoContentDisMiss : MapIntent

    data class PhotoClicked(val photo: PhotoItem, val isDoubleClicked: Boolean) : MapIntent

    data object FriendDialogDisMiss : MapIntent

    data class FriendDialogConfirm(val friends: ImmutableList<FirebaseFriend>) : MapIntent

    data class PreloadListener(val preload: Pair<String, PreloadState>): MapIntent
}
