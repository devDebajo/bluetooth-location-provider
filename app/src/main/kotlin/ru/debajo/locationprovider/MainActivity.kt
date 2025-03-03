package ru.debajo.locationprovider

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import ru.debajo.locationprovider.ui.theme.LocationProviderTheme

internal class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LocationProviderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val state = rememberMultiplePermissionsState(PermissionsList) { map ->
                        if (map.all { it.value }) {
                            viewModel.onPermissionsGranted()
                        }
                    }
                    LaunchedEffect(state) {
                        if (state.permissions.any { !it.status.isGranted }) {
                            state.launchMultiplePermissionRequest()
                        } else {
                            viewModel.onPermissionsGranted()
                        }
                    }

                    val hasAllPermissions = state.permissions.all { it.status.isGranted }
                    if (hasAllPermissions) {
                        PermittedContent(innerPadding)
                    }
                }
            }
        }
    }

    @Composable
    private fun PermittedContent(paddings: PaddingValues) {
        val state by viewModel.state.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.size(10.dp))
                RadioButtons(state)
                if (state.isProvider) {
                    Spacer(Modifier.size(20.dp))
                    AvailableEndpoints(state, modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Row {
                    Button(
                        onClick = { viewModel.start() },
                        enabled = state.canStart
                    ) {
                        Text("Старт")
                    }
                    Spacer(Modifier.size(20.dp))
                    Button(
                        onClick = { viewModel.stop() },
                        enabled = state.canStop
                    ) {
                        Text("Стоп")
                    }
                }
                Spacer(Modifier.size(20.dp))
            }
        }
    }

    @Composable
    private fun RadioButtons(
        state: MainState,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "Режим",
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
            )
            Spacer(Modifier.size(8.dp))
            TextRadioButton(
                text = "Приемник геолокации",
                selected = !state.isProvider,
                onClick = { viewModel.onReceiverSelected() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isRunning,
            )
            TextRadioButton(
                text = "Передатчик геолокации",
                selected = state.isProvider,
                onClick = { viewModel.onProviderSelected() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isRunning,
            )
        }
    }

    @Composable
    private fun AvailableEndpoints(
        state: MainState,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "Выберите устройство-приемник",
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
            )
            Spacer(Modifier.size(8.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                items(count = state.availableEndpoints.size) { index ->
                    val endpoint = state.availableEndpoints[index]
                    TextRadioButton(
                        text = endpoint.name,
                        selected = endpoint.address == state.selectedEndpointAddress,
                        onClick = { viewModel.onEndpointSelected(endpoint) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isRunning,
                    )
                }
            }
        }
    }

    @Composable
    private fun TextRadioButton(
        text: String,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
    ) {
        Row(
            modifier = modifier
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = text
            )
            RadioButton(
                enabled = enabled,
                selected = selected,
                onClick = onClick,
            )
        }
    }

    private companion object {
        val PermissionsList: List<String> = listOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    }
}
