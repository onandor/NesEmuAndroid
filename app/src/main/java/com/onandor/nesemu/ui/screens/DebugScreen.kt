package com.onandor.nesemu.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.nesemu.emulation.nes.DebugFeature
import com.onandor.nesemu.ui.components.CheckboxListItem
import com.onandor.nesemu.ui.components.NesRenderer
import com.onandor.nesemu.viewmodels.DebugViewModel
import com.onandor.nesemu.viewmodels.DebugViewModel.Event
import com.onandor.nesemu.ui.components.NesSurfaceView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: DebugViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.expand()
    }

    BottomSheetScaffold(
        topBar = { TopBar(onEvent = viewModel::onEvent) },
        scaffoldState = scaffoldState,
        sheetPeekHeight = 70.dp,
        sheetDragHandle = {
            val expanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
            val arrowAngle by animateFloatAsState(if (expanded) 0f else 180f, label = "")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .clickable {
                        coroutineScope.launch {
                            if (expanded) {
                                scaffoldState.bottomSheetState.partialExpand()
                            } else {
                                scaffoldState.bottomSheetState.expand()
                            }
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(start = 30.dp),
                    text = "View options"
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.rotate(arrowAngle)) {
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, null)
                }
                Spacer(modifier = Modifier.width(30.dp))
            }
        },
        sheetContent = {
            SheetContent(onEvent = viewModel::onEvent)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
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

    BackHandler {
        if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
            coroutineScope.launch { scaffoldState.bottomSheetState.partialExpand() }
        } else {
            viewModel.onEvent(Event.OnNavigateBack)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onEvent: (Event) -> Unit
) {
    TopAppBar(
        title = { Text("Debug view") },
        navigationIcon = {
            IconButton(onClick = { onEvent(Event.OnNavigateBack) }) {
                Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, null)
            }
        }
    )
}

@Composable
private fun SheetContent(
    modifier: Modifier = Modifier,
    onEvent: (Event) -> Unit
) {
    Column(modifier = modifier.padding(bottom = 10.dp)) {
        CheckboxListItem(
            onCheckedChange = {
                onEvent(Event.OnSetDebugFeatureBool(DebugFeature.PPU_RENDER_COLOR_PALETTES, it))
            }
        ) {
            Text(text = "Show color palettes")
        }
        CheckboxListItem(
            onCheckedChange = {
                onEvent(Event.OnSetDebugFeatureBool(DebugFeature.PPU_RENDER_PATTERN_TABLE, it))
            }
        ) {
            Text(text = "Show pattern tables")
        }
        CheckboxListItem(
            onCheckedChange = {
                onEvent(Event.OnSetDebugFeatureBool(DebugFeature.PPU_RENDER_NAMETABLE, it))
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
        var paletteModifier = Modifier.width(screenWidth / 4.5f).aspectRatio(60f / 15f)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 0 ..< 4) {
                var modifier = if (i == selectedIdx) {
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
                var modifier = if (i == selectedIdx) {
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