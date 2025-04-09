package com.onandor.nesemu.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import com.onandor.nesemu.ui.components.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.core.DragIndication
import com.composables.core.ModalBottomSheet
import com.composables.core.Scrim
import com.composables.core.Sheet
import com.composables.core.SheetDetent
import com.composables.core.rememberModalBottomSheetState
import com.onandor.nesemu.domain.emulation.nes.DebugFeature
import com.onandor.nesemu.ui.components.CheckboxListItem
import com.onandor.nesemu.ui.components.ColoredNavigationBar
import com.onandor.nesemu.ui.components.RectangularIconButton
import com.onandor.nesemu.ui.components.TopBar
import com.onandor.nesemu.ui.components.game.NesRenderer
import com.onandor.nesemu.viewmodels.DebugViewModel
import com.onandor.nesemu.viewmodels.DebugViewModel.Event
import com.onandor.nesemu.ui.components.game.NesSurfaceView
import com.onandor.nesemu.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: DebugViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val bottomSheetState = rememberModalBottomSheetState(initialDetent = SheetDetent.Hidden)

    Scaffold(
        topBar = {
            TopBar(
                emulationPaused = uiState.emulationPaused,
                onEvent = viewModel::onEvent
            )
        },
        bottomBar = { ColoredNavigationBar() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (uiState.renderColorPalettes) {
                item {
                    Text(
                        modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
                        text = "Color palettes"
                    )
                    ColorPalettes(
                        renderers = viewModel.colorPaletteRenderers,
                        onEvent = viewModel::onEvent
                    )
                    if (uiState.renderPatternTable || uiState.renderNametable) {
                        HorizontalDivider(modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
            if (uiState.renderPatternTable) {
                item {
                    Text(
                        modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
                        text = "Pattern table"
                    )
                    NesSurfaceView(
                        modifier = Modifier.fillMaxWidth().aspectRatio(256f / 128f),
                        renderer = viewModel.patternTableRenderer,
                        onRenderCallbackCreated = {
                            viewModel.onEvent(Event.OnPatternTableRenderCallbackCreated(it))
                        }
                    )
                    if (uiState.renderNametable) {
                        HorizontalDivider(modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
            if (uiState.renderNametable) {
                item {
                    Text(
                        modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
                        text = "Nametable"
                    )
                    NesSurfaceView(
                        modifier = Modifier.fillMaxWidth().aspectRatio(512f / 480f),
                        renderer = viewModel.nametableRenderer,
                        onRenderCallbackCreated = {
                            viewModel.onEvent(Event.OnNametableRenderCallbackCreated(it))
                        }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    LaunchedEffect(uiState.showBottomSheet) {
        if (uiState.showBottomSheet) {
            bottomSheetState.currentDetent = SheetDetent.FullyExpanded
        } else {
            bottomSheetState.currentDetent = SheetDetent.Hidden
        }
    }

    ModalBottomSheet(
        state = bottomSheetState,
        onDismiss = { viewModel.onEvent(Event.OnHideBottomSheet) }
    ) {
        Scrim()
        Sheet(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DragIndication(
                    modifier = Modifier
                        .padding(top = 15.dp, bottom = 15.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(100)
                        )
                        .width(32.dp)
                        .height(4.dp)
                )
                SheetContent(
                    onEvent = viewModel::onEvent,
                    renderColorPalettes = uiState.renderColorPalettes,
                    renderPatternTable = uiState.renderPatternTable,
                    renderNametable = uiState.renderNametable
                )
            }
        }
    }

    BackHandler {
        viewModel.onEvent(Event.OnNavigateBack)
    }
}

@Composable
private fun TopBar(
    emulationPaused: Boolean,
    onEvent: (Event) -> Unit
) {
    TopBar(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .statusBarsPadding()
            .padding(top = 10.dp, bottom = 10.dp),
        title = "Debug view",
        navigationIcon = {
            RectangularIconButton(onClick = { onEvent(Event.OnNavigateBack) }) {
                Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, null)
            }
        },
        actions = {
            RectangularIconButton(onClick = { onEvent(Event.OnToggleEmulationPaused) }) {
                if (emulationPaused) {
                    Icon(imageVector = Icons.Default.PlayArrow, null)
                } else {
                    Icon(painter = painterResource(R.drawable.ic_pause), null)
                }
            }
            RectangularIconButton(onClick = { onEvent(Event.OnShowBottomSheet) }) {
                Icon(imageVector = Icons.Default.Build, null)
            }
        }
    )
}

@Composable
private fun SheetContent(
    modifier: Modifier = Modifier,
    renderColorPalettes: Boolean,
    renderPatternTable: Boolean,
    renderNametable: Boolean,
    onEvent: (Event) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        CheckboxListItem(
            initialValue = renderColorPalettes,
            onCheckedChange = {
                onEvent(Event.OnSetDebugFeatureBool(DebugFeature.PpuRenderColorPalettes, it))
            }
        ) {
            Text(text = "Show color palettes")
        }
        CheckboxListItem(
            initialValue = renderPatternTable,
            onCheckedChange = {
                onEvent(Event.OnSetDebugFeatureBool(DebugFeature.PpuRenderPatternTable, it))
            }
        ) {
            Text(text = "Show pattern tables")
        }
        CheckboxListItem(
            initialValue = renderNametable,
            onCheckedChange = {
                onEvent(Event.OnSetDebugFeatureBool(DebugFeature.PpuRenderNametable, it))
            }
        ) {
            Text(text = "Show nametables")
        }
    }
}

@Composable
private fun ColorPalettes(
    renderers: Array<NesRenderer>,
    onEvent: (Event) -> Unit
 ) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    var selectedIdx by remember { mutableIntStateOf(0) }

    Column {
        val paletteModifier = Modifier.width(screenWidth / 4.5f).aspectRatio(60f / 15f)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 0 ..< 4) {
                val modifier = if (i == selectedIdx) {
                    paletteModifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    paletteModifier
                }
                NesSurfaceView(
                    modifier = modifier,
                    renderer = renderers[i],
                    onRenderCallbackCreated = {
                        onEvent(Event.OnColorPaletteRenderCallbackCreated(i, it))
                    },
                    onTouchEvent = {
                        selectedIdx = i
                        onEvent(Event.OnColorPaletteTouch(i, it))
                    }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 4 ..< 8) {
                val modifier = if (i == selectedIdx) {
                    paletteModifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    paletteModifier
                }
                NesSurfaceView(
                    modifier = modifier,
                    renderer = renderers[i],
                    onRenderCallbackCreated = {
                        onEvent(Event.OnColorPaletteRenderCallbackCreated(i, it))
                    },
                    onTouchEvent = {
                        selectedIdx = i
                        onEvent(Event.OnColorPaletteTouch(i, it))
                    }
                )
            }
        }
    }
}