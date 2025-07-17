package com.and04.naturealbum.ui.add.labelsearch

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.and04.naturealbum.NatureAlbum
import com.and04.naturealbum.R
import com.and04.naturealbum.data.localdata.room.Label
import com.and04.naturealbum.ui.add.labelsearch.contract.LabelSearchEffect
import com.and04.naturealbum.ui.add.labelsearch.contract.LabelSearchIntent
import com.and04.naturealbum.ui.add.labelsearch.contract.LabelSearchState
import com.and04.naturealbum.ui.add.savephoto.SavePhotoViewModel
import com.and04.naturealbum.ui.component.LabelChip
import com.and04.naturealbum.ui.component.ProgressIndicator
import com.and04.naturealbum.ui.utils.UiState
import com.and04.naturealbum.utils.color.toColor
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun LabelSearchScreen(
    state: () -> LabelSearchState,
    onSelected: (Label) -> Unit,
    viewModel: LabelSearchViewModel,
    savePhotoViewModel: SavePhotoViewModel,
) {
    val vertexAIState = savePhotoViewModel.vertexAIState.collectAsStateWithLifecycle()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is LabelSearchEffect.LabelSelected -> {
                onSelected(state().label)
            }

            is LabelSearchEffect.ToastMassage -> {
                Toast.makeText(
                    NatureAlbum.getInstance(),
                    sideEffect.massage.text,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    LabelSearchScreen(
        vertexAIState = vertexAIState,
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelSearchScreen(
    vertexAIState: State<UiState<String>>,
    state: () -> LabelSearchState,
    onIntent: (LabelSearchIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.label_search_title_topbar),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        when (state().uiState) {
            is LabelSearchUiState.Loading -> {
                Box(modifier = Modifier.padding(innerPadding)) {
                    ProgressIndicator(true)
                }
            }

            is LabelSearchUiState.Success -> {
                SearchContent(
                    innerPadding = innerPadding,
                    vertexAIState = vertexAIState,
                    state = state,
                    onIntent = onIntent,
                )
            }
        }
    }
}


@Composable
private fun SearchContent(
    innerPadding: PaddingValues,
    vertexAIState: State<UiState<String>>,
    state: () -> LabelSearchState,
    onIntent: (LabelSearchIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
    ) {
        LabelTextField(query = state().query, onIntent = onIntent)

        Text(
            modifier = Modifier.padding(12.dp),
            text = stringResource(R.string.label_search_select_create),
            style = MaterialTheme.typography.bodyMedium
        )

        LabelChipList(
            state = state,
            onIntent = onIntent,
        )

        if (state().query.isNotEmpty()) {
            CreateLabelContent(
                state = state,
                onIntent = onIntent
            )
        }

        //TODO 추후 시연할 때 주석 해제
//        GeminiLabelContent(
//            vertexAIState = vertexAIState,
//            state = state,
//            onIntent = onIntent,
//        )
    }
}

@Composable
private fun LabelTextField(
    query: String,
    onIntent: (LabelSearchIntent) -> Unit,
) {
    TextField(
        modifier = Modifier.fillMaxWidth(),
        value = query,
        onValueChange = { changeQuery ->
            if (changeQuery.length > 100) return@TextField
            else onIntent(LabelSearchIntent.QueryInput(changeQuery))
        },
        placeholder = { Text(stringResource(R.string.label_search_label_search)) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent
        ),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
    )
}

@Composable
private fun LabelChipList(
    state: () -> LabelSearchState,
    onIntent: (LabelSearchIntent) -> Unit,
) {
    LazyColumn {
        val queryLabelList =
            state().labelList.filter { label -> label.name.contains(state().query) }

        items(
            items = queryLabelList,
            key = { label -> label.id }
        ) { label ->
            UnderLineSuggestionChip(label, onIntent)
        }
    }
}

@Composable
private fun UnderLineSuggestionChip(
    label: Label,
    onIntent: (LabelSearchIntent) -> Unit,
) {
    LabelChip(
        modifier = Modifier.padding(start = 12.dp),
        backgroundColor = label.backgroundColor,
        onClick = { onIntent(LabelSearchIntent.LabelClicked(label = label)) }
    ) {
        Text(label.name)
    }

    Spacer(
        modifier = Modifier
            .height(0.5.dp)
            .fillMaxWidth()
            .background(Color.Gray)
    )
}

@Composable
private fun CreateLabelContent(
    state: () -> LabelSearchState,
    onIntent: (LabelSearchIntent) -> Unit,
) {
    Row(
        modifier = Modifier.padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.label_search_create))

        Spacer(Modifier.size(4.dp))

        SuggestionChip(
            modifier = Modifier.semantics { testTag = "chip" },
            onClick = {
                onIntent(
                    LabelSearchIntent.LabelClicked(
                        Label(
                            backgroundColor = state().color,
                            name = state().query
                        )
                    )
                )
            },
            label = { Text(state().query) },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = state().color.toColor(),
                labelColor = if (Color(state().color.toLong(16)).luminance() > 0.5f) Color.Black else Color.White
            )
        )
    }
}

@Preview
@Composable
private fun PreviewFunc() {
    val vertexAIState = remember { mutableStateOf(UiState.Idle) }

    LabelSearchScreen(
        vertexAIState = vertexAIState,
        state = { LabelSearchState() },
        onIntent = {},
    )
}
