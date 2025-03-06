package ru.debajo.locationprovider

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import ru.debajo.locationprovider.ui.theme.LocationProviderTheme
import ru.debajo.locationprovider.utils.Di

internal class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val activityHolder: ActivityHolder by lazy { Di.activityHolder }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityHolder.register(this)
        enableEdgeToEdge()

        setContent {
            val bluetoothPermissionsState = rememberMultiplePermissionsState(PermissionUtils.BluetoothPermissionsList) { state ->
                viewModel.onBluetoothPermissionsChanged(state.all { it.value })
            }

            val notificationsPermissionState = rememberPermissionState(PermissionUtils.NotificationsPermission) {
                viewModel.onNotificationsPermissionChanged(it)
            }

            val locationPermissionsState = rememberMultiplePermissionsState(PermissionUtils.LocationPermissionsList) { state ->
                viewModel.onLocationPermissionChanged(state.all { it.value })
            }

            val backgroundLocationPermissionsState = rememberPermissionState(PermissionUtils.BackgroundLocationPermission) {
                viewModel.onBackgroundLocationPermissionChanged(it)
            }

            val hostState = remember { SnackbarHostState() }
            LaunchedEffect(
                viewModel,
                hostState,
                bluetoothPermissionsState,
                notificationsPermissionState,
                locationPermissionsState,
                backgroundLocationPermissionsState
            ) {
                viewModel.news.collect { news ->
                    when (news) {
                        is MainNews.ShowSnackBar -> hostState.showSnackbar(news.text)
                        is MainNews.RequestBluetoothPermission -> bluetoothPermissionsState.launchMultiplePermissionRequest()
                        is MainNews.RequestLocationPermission -> locationPermissionsState.launchMultiplePermissionRequest()
                        is MainNews.RequestBackgroundLocationPermission -> backgroundLocationPermissionsState.launchPermissionRequest()
                        is MainNews.RequestNotificationsPermission -> notificationsPermissionState.launchPermissionRequest()
                    }
                }
            }

            LocationProviderTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(hostState) },
                ) { innerPadding ->
                    PermittedContent(innerPadding)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshMockPermission()
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
                when (val localState = state) {
                    is MainState.Provider -> {
                        Spacer(Modifier.size(20.dp))
                        AvailableEndpoints(localState, modifier = Modifier.weight(1f))
                    }

                    is MainState.Receiver -> {
                        Spacer(modifier = Modifier.weight(1f))
                    }
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

        val stateReceiver = state as? MainState.Receiver
        if (stateReceiver?.showMockPermissionDialog == true) {
            MockLocationDialog()
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
                selected = state is MainState.Receiver,
                onClick = { viewModel.onReceiverSelected() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isRunning,
            )
            TextRadioButton(
                text = "Передатчик геолокации",
                selected = state is MainState.Provider,
                onClick = { viewModel.onProviderSelected() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isRunning,
            )
        }
    }

    @Composable
    private fun AvailableEndpoints(
        state: MainState.Provider,
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

    @Composable
    private fun MockLocationDialog() {
        AlertDialog(
            onDismissRequest = { viewModel.hideMockLocationDialog() },
            title = {
                Text("Нужен доступ к подмене геолокации")
            },
            text = {
                Text("Для работы в режиме приемника нужно разрешить приложению подменять геолокацию. Чтобы это сделать нужно включить режим разработчика и в нем выбрать данное приложение как провайдер фиктивного местоположения")
            },
            confirmButton = {
                TextButton({
                    viewModel.hideMockLocationDialog()
                    openMockLocationSettings()
                }) {
                    Text("Перейти в настройки")
                }
            },
            dismissButton = {
                TextButton({ viewModel.hideMockLocationDialog() }) {
                    Text("Отмена")
                }
            },
        )
    }

    private fun openMockLocationSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        }.onFailure {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}
