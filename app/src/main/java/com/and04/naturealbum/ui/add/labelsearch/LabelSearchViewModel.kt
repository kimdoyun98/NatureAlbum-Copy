package com.and04.naturealbum.ui.add.labelsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.and04.naturealbum.data.repository.local.LabelRepository
import com.and04.naturealbum.ui.add.labelsearch.contract.LabelSearchEffect
import com.and04.naturealbum.ui.add.labelsearch.contract.LabelSearchIntent
import com.and04.naturealbum.ui.add.labelsearch.contract.LabelSearchState
import com.and04.naturealbum.ui.add.labelsearch.contract.LabelSelectEffectMassage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class LabelSearchViewModel @Inject constructor(
    private val labelRepository: LabelRepository,
) : ContainerHost<LabelSearchState, LabelSearchEffect>, ViewModel() {

    override val container: Container<LabelSearchState, LabelSearchEffect> =
        container(LabelSearchState())

    init {
        flow {
            emit(labelRepository.getLabels())
        }.onEach {
            intent {
                reduce {
                    state.copy(
                        uiState = LabelSearchUiState.Success,
                        labelList = it
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onIntent(intent: LabelSearchIntent) = intent {
        when (intent) {
            is LabelSearchIntent.QueryInput -> {
                val queryIsEmpty = state.query.isEmpty()
                reduce {
                    state.copy(
                        query = intent.query,
                        color = if (queryIsEmpty) getRandomColor() else state.color
                    )
                }
            }

            is LabelSearchIntent.LabelClicked -> {
                reduce {
                    state.copy(label = intent.label)
                }

                postSideEffect(
                    if (state.labelList.any { label -> label.name == state.query }) {
                        LabelSearchEffect.ToastMassage(LabelSelectEffectMassage.USED)
                    } else {
                        LabelSearchEffect.LabelSelected
                    }
                )
            }
        }
    }
}
