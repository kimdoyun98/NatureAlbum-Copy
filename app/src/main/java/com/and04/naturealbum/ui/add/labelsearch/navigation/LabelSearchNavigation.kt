package com.and04.naturealbum.ui.add.labelsearch.navigation

import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.and04.naturealbum.ui.add.labelsearch.LabelSearchScreen
import com.and04.naturealbum.ui.add.labelsearch.LabelSearchViewModel
import com.and04.naturealbum.ui.add.labelsearch.contract.LabelSearchEffect
import com.and04.naturealbum.ui.navigation.NatureAlbumNavigator
import com.and04.naturealbum.ui.navigation.NatureAlbumState
import com.and04.naturealbum.ui.navigation.NavigateDestination
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

fun NavGraphBuilder.labelSearchNavigation(
    state: NatureAlbumState,
    navigator: NatureAlbumNavigator
) {
    composable(NavigateDestination.SearchLabel.route) { backStackEntry ->
        val savePhotoBackStackEntryForSearchLabel = remember(backStackEntry) {
            navigator.getNavBackStackEntry(NavigateDestination.SavePhoto.route)
        }
        val viewModel: LabelSearchViewModel = hiltViewModel()
        val labelSearchState by viewModel.collectAsState()
        val context = LocalContext.current

        viewModel.collectSideEffect { sideEffect ->
            when (sideEffect) {
                is LabelSearchEffect.LabelSelected -> {
                    state.selectedLabel.value = labelSearchState.label
                    navigator.popupBackStack()
                }

                is LabelSearchEffect.ToastMassage -> {
                    Toast.makeText(
                        context,
                        sideEffect.massage.text,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        LabelSearchScreen(
            state = { labelSearchState },
            onIntent = viewModel::onIntent,
            savePhotoViewModel = hiltViewModel(savePhotoBackStackEntryForSearchLabel),
        )
    }
}
