package com.onandor.nesemu.ui.components

import android.content.Context
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val STATUS_BAR_HEIGHT = 40

@Composable
fun StatusBarScaffold(
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Box (
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val density = LocalDensity.current
        var topBarHeight by remember { mutableStateOf(0.dp) }

        Row(
            modifier = Modifier
                .height(STATUS_BAR_HEIGHT.dp)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Clock()
            BatteryIndicator()
        }
        if (topBar !== {}) {
            Box(
                modifier = Modifier
                    .padding(top = STATUS_BAR_HEIGHT.dp)
                    .onSizeChanged { size ->
                        topBarHeight = with(density) { size.height.toDp() }
                    }
            ) {
                topBar()
            }
            content(PaddingValues(top = STATUS_BAR_HEIGHT.dp + topBarHeight))
        } else {
            content(PaddingValues(top = STATUS_BAR_HEIGHT.dp))
        }
        Spacer(modifier = Modifier.fillMaxSize())
        bottomBar()
    }
}

@Composable
private fun Clock(modifier: Modifier = Modifier) {
    var time by remember { mutableStateOf("") }
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.ROOT) }

    LaunchedEffect(Unit) {
        while (isActive) {
            time = sdf.format(Date())
            delay(5.seconds)
        }
    }

    Text(
        modifier = modifier,
        text = time,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun BatteryIndicator(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    var batteryLevel by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (isActive) {
            batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            delay(2.minutes)
        }
    }

    Text(
        modifier = modifier,
        text = "$batteryLevel%",
        fontWeight = FontWeight.SemiBold
    )
}