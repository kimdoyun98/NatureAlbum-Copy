package com.and04.naturealbum.ui.add.savephoto.navigation

import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.and04.naturealbum.background.service.FirebaseInsertService
import com.and04.naturealbum.background.service.FirebaseInsertService.Companion.SERVICE_DATETIME
import com.and04.naturealbum.background.service.FirebaseInsertService.Companion.SERVICE_DESCRIPTION
import com.and04.naturealbum.background.service.FirebaseInsertService.Companion.SERVICE_FILENAME
import com.and04.naturealbum.background.service.FirebaseInsertService.Companion.SERVICE_LABEL
import com.and04.naturealbum.background.service.FirebaseInsertService.Companion.SERVICE_LOCATION_LATITUDE
import com.and04.naturealbum.background.service.FirebaseInsertService.Companion.SERVICE_LOCATION_LONGITUDE
import com.and04.naturealbum.background.service.FirebaseInsertService.Companion.SERVICE_URI
import com.and04.naturealbum.data.localdata.room.Label
import com.and04.naturealbum.ui.add.savephoto.SavePhotoScreen
import com.and04.naturealbum.ui.add.savephoto.SavePhotoViewModel
import com.and04.naturealbum.ui.add.savephoto.contract.SavePhotoEffect
import com.and04.naturealbum.ui.add.savephoto.contract.SavePhotoState.Companion.init
import com.and04.naturealbum.ui.navigation.NatureAlbumNavigator
import com.and04.naturealbum.ui.navigation.NatureAlbumState
import com.and04.naturealbum.ui.navigation.NavigateDestination
import com.and04.naturealbum.utils.image.ImageConvert
import com.and04.naturealbum.utils.network.NetworkState
import com.and04.naturealbum.utils.network.NetworkState.DISCONNECTED
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun NavGraphBuilder.saveAlbumNavGraph(
    state: NatureAlbumState,
    navigator: NatureAlbumNavigator,
    takePictureLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    composable(NavigateDestination.SavePhoto.route) { backStackEntry ->
        val savePhotoBackStackEntry = remember(backStackEntry) {
            navigator.getNavBackStackEntry(NavigateDestination.SavePhoto.route)
        }
        val viewModel: SavePhotoViewModel =
            hiltViewModel(viewModelStoreOwner = savePhotoBackStackEntry)
        val savePhotoState by viewModel.collectAsState()
        val context = LocalContext.current

        viewModel.collectSideEffect { effect ->
            when (effect) {
                is SavePhotoEffect.Navigation.Save -> {
                    val time = LocalDateTime.now(ZoneId.of("UTC"))
                    val fileName = "${System.currentTimeMillis()}.jpg"
                    val fileUri =
                        ImageConvert.makeFileToUri(savePhotoState.uri.toString(), fileName)
                    val label = savePhotoState.appState?.selectedLabel?.value

                    viewModel.savePhoto(
                        uri = fileUri,
                        fileName = fileName,
                        label = label!!,
                        location = savePhotoState.location!!,
                        description = savePhotoState.description,
                        isRepresented = savePhotoState.represented,
                        time = time
                    )

                    insertFirebaseService(
                        context = context,
                        uri = fileUri,
                        fileName = fileName,
                        label = label,
                        location = savePhotoState.location!!,
                        description = savePhotoState.description,
                        time = time
                    )
                }

                is SavePhotoEffect.Navigation.Back -> savePhotoState.onBack()

                is SavePhotoEffect.Navigation.Cancel -> savePhotoState.onCancel()

                is SavePhotoEffect.Navigation.MyPage -> savePhotoState.onNavigateToMyPage()

                is SavePhotoEffect.Navigation.LabelSelect -> savePhotoState.onLabelSelect()
            }
        }

        SavePhotoScreen(
            state = { savePhotoState },
            initState = { savePhotoState.init(state, navigator, takePictureLauncher) },
            onIntent = viewModel::onIntent,
            changeState = viewModel::changeState,
        )
    }
}

private fun insertFirebaseService(
    context: Context,
    uri: String,
    fileName: String,
    label: Label,
    location: Location,
    description: String,
    time: LocalDateTime,
) {
    if (Firebase.auth.currentUser == null || NetworkState.getNetWorkCode() == DISCONNECTED) return
    val newTime = time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    val intent = Intent(context, FirebaseInsertService::class.java).apply {
        putExtra(SERVICE_URI, uri)
        putExtra(SERVICE_FILENAME, fileName)
        putExtra(SERVICE_LABEL, label)
        putExtra(SERVICE_LOCATION_LATITUDE, location.latitude)
        putExtra(SERVICE_LOCATION_LONGITUDE, location.longitude)
        putExtra(SERVICE_DESCRIPTION, description)
        putExtra(SERVICE_DATETIME, newTime)
    }

    context.startService(intent)
}

