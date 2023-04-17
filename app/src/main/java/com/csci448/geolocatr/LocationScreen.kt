package com.csci448.geolocatr

import android.location.Location
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun LocationScreen(location: Location?,
                   locationAvailable: Boolean,
                   onGetLocation: () -> Unit,
                   address: String ){

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 0f)
    }

    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    LaunchedEffect(location) {
        if(location != null) {
            val bounds = LatLngBounds.Builder()
                .include(LatLng(location.latitude, location.longitude))
                .build()
            val padding = context.resources
                .getDimensionPixelSize(R.dimen.map_inset_padding)
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            cameraPositionState.animate(cameraUpdate)
        }
    }

    val dataStoreManager = remember {
        DataStoreManager(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    val compassState = dataStoreManager
        .compassFlow
        .collectAsStateWithLifecycle(
            initialValue = true,
            lifecycle = lifecycleOwner.lifecycle
        )
    val trafficState = dataStoreManager
        .trafficFlow
        .collectAsStateWithLifecycle(
            initialValue = false,
            lifecycle = lifecycleOwner.lifecycle
        )

    val mapUiSettings = MapUiSettings(
        compassEnabled = compassState.value
    )
    val mapProperties = MapProperties(
        isTrafficEnabled = trafficState.value
    )

    Column{
        Text(text = "Latitude / Longitude")
        Text(text = "${location?.latitude}, ${location?.longitude}")
        Text(text = "Address")
        Text(text = address)

        Row(
            verticalAlignment = Alignment.CenterVertically
        ){
            Text("Traffic")
            Switch(
                checked = trafficState.value,
                onCheckedChange = {
                    coroutineScope.launch{dataStoreManager.setTrafficFlow(it)}
                }
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ){
            Text("Compass")
            Switch(
                checked = compassState.value,
                onCheckedChange = {
                    coroutineScope.launch{dataStoreManager.setCompassFlow(it)}
                }
            )
        }
        Button(
            enabled = locationAvailable,
            onClick = onGetLocation
        ){
            Text(text = "Get Current Location")
        }
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = mapUiSettings,
            properties = mapProperties
        ) {
            if(location != null) {
                val markerState = MarkerState().apply {
                    position = LatLng(location.latitude, location.longitude)
                }
                Marker(
                    state = markerState,
                    title = address,
                    snippet = "${location.latitude} / ${location.longitude}"
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLocationScreen() {
    val locationState = remember { mutableStateOf<Location?>(null) }
    val addressState = remember { mutableStateOf("") }
    LocationScreen(
        location = locationState.value,
        locationAvailable = true,
        onGetLocation = {
            locationState.value = Location("").apply {
                latitude = 1.35
                longitude = 103.87
            }
            addressState.value = "Singapore"
        },
        address = addressState.value
    )
}