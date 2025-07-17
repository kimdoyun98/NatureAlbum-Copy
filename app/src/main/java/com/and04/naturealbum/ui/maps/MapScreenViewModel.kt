package com.and04.naturealbum.ui.maps

import android.graphics.PointF
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.and04.naturealbum.data.repository.firebase.AlbumRepository
import com.and04.naturealbum.data.repository.firebase.FriendRepository
import com.and04.naturealbum.data.repository.local.LabelRepository
import com.and04.naturealbum.data.repository.local.PhotoDetailRepository
import com.and04.naturealbum.ui.maps.contract.MapEffect
import com.and04.naturealbum.ui.maps.contract.MapIntent
import com.and04.naturealbum.ui.maps.contract.MapState
import com.and04.naturealbum.ui.maps.utils.toFriendPhotoItems
import com.and04.naturealbum.ui.maps.utils.toPhotoItems
import com.and04.naturealbum.ui.utils.UserManager
import com.and04.naturealbum.utils.network.NetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class MapScreenViewModel @Inject constructor(
    private val photoDetailRepository: PhotoDetailRepository,
    private val albumRepository: AlbumRepository,
    private val friendRepository: FriendRepository,
    private val labelRepository: LabelRepository,
    private val networkManager: NetworkManager,
) : ContainerHost<MapState, MapEffect>, ViewModel() {

    override val container: Container<MapState, MapEffect> = container(MapState())

    init {
        checkNetwork()
        initPhotos()

        intent {
            reduce { state.copy(scope = viewModelScope) }
        }
    }

    fun onIntent(intent: MapIntent) = intent {
        when (intent) {
            is MapIntent.BackButtonClicked -> {
                postSideEffect(MapEffect.NavigateHome)
            }

            is MapIntent.InitMap -> {
                reduce { state.copy(pick = null, bottomSheetPhotos = persistentListOf()) }

                postSideEffect(MapEffect.PickChanged)
            }

            is MapIntent.PickChanged -> {
                reduce { state.copy(pick = intent.pick) }

                postSideEffect(MapEffect.PickChanged)
            }

            is MapIntent.ClusterClicked -> {
                reduce {
                    state.copy(
                        pick = intent.pick,
                        bottomSheetPhotos = intent.bottomSheetPhotos
                    )
                }

                postSideEffect(MapEffect.PickChanged)
            }

            is MapIntent.ClusterChanged -> {
                reduce { state.copy(bottomSheetPhotos = intent.bottomSheetPhotos) }
            }

            is MapIntent.MarkerClicked -> {
                reduce { state.copy(showPhotoContent = true) }
            }

            is MapIntent.FriendIconClicked -> {
                val uid = UserManager.getUser()!!.uid
                fetchFriends(uid)

                reduce { state.copy(openDialog = true) }
            }

            is MapIntent.BottomSheetStateChanged -> {
                reduce {
                    state.copy(
                        cameraPivot =
                        if (intent.isCollapsed) PointF(0.5f, 0.5f)
                        else PointF(
                            0.5f,
                            0.3f
                        )
                    )
                }

                postSideEffect(MapEffect.CameraPivotChanged)
            }

            is MapIntent.PhotoContentDisMiss -> {
                reduce { state.copy(showPhotoContent = false) }
            }

            is MapIntent.PhotoClicked -> {
                reduce { state.copy(pick = intent.photo) }

                if (intent.isDoubleClicked) {
                    reduce {
                        state.copy(showPhotoContent = true)
                    }
                }

                postSideEffect(MapEffect.PickChanged)
            }

            is MapIntent.FriendDialogDisMiss -> {
                reduce { state.copy(openDialog = false) }
            }

            is MapIntent.FriendDialogConfirm -> {
                reduce {
                    state.copy(
                        selectedFriends = intent.friends,
                        openDialog = false
                    )
                }

                fetchFriendsPhotos(intent.friends.map { friend -> friend.user.uid })
            }

            is MapIntent.PreloadListener -> {
                val (key, value) = intent.preload
                val map = state.preloadState.toMutableMap()
                map[key] = value

                reduce { state.copy(preloadState = map.toImmutableMap()) }
            }
        }
    }

    private fun checkNetwork() = intent {
        viewModelScope.launch {
            networkManager.networkState.collect { networkState ->
                reduce {
                    state.copy(networkState = networkState)
                }
            }
        }
    }

    private fun initPhotos() = intent {
        viewModelScope.launch {
            val fetchPhotos = async { photoDetailRepository.getAllPhotoDetail() }
            val fetchLabels = labelRepository.getLabels()
            val myPhotos = fetchPhotos.await().toPhotoItems(fetchLabels).toImmutableList()

            reduce {
                state.copy(photosByUid = persistentMapOf("" to myPhotos))
            }

            postSideEffect(MapEffect.PhotosByUidChanged)
        }
    }

    private fun fetchFriendsPhotos(friends: List<String>) = intent {
        viewModelScope.launch {
            try {
                val photos = async { albumRepository.getPhotos(friends) }
                val labels = albumRepository.getLabelsToMap(friends)
                val photosMap = state.photosByUid +
                        photos.await()
                            .getOrThrow()
                            .mapValues { (uid, photos) ->
                                photos.toFriendPhotoItems(labels.getOrThrow().getValue(uid))
                            }

                reduce {
                    state.copy(photosByUid = photosMap.toImmutableMap())
                }

                postSideEffect(MapEffect.PhotosByUidChanged)

            } catch (e: Exception) {
                Log.e("MapScreenViewModel", e.toString())
            }
        }
    }

    private fun fetchFriends(uid: String) = intent {
        friendRepository.getFriendsAsFlow(uid)
            .onEach { friends ->
                reduce {
                    state.copy(friends = friends.toImmutableList())
                }
            }
            .launchIn(viewModelScope)
    }
}
