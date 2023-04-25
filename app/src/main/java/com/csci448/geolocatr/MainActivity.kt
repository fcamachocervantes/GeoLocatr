package com.csci448.geolocatr

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.csci448.geolocatr.ui.theme.GeoLocatrTheme
import com.google.android.gms.location.LocationSettingsStates

class MainActivity : ComponentActivity() {

    private lateinit var locationUtility: LocationUtility
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var locationLauncher: ActivityResultLauncher<IntentSenderRequest>
    private val locationAlarmReceiver = LocationAlarmReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationUtility = LocationUtility(this)

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            locationUtility.checkPermissionAndGetLocation(this@MainActivity, permissionLauncher)
        }

        locationLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { data ->
                    val states = LocationSettingsStates.fromIntent(data)
                    locationUtility.verifyLocationSettingsStates(states)
                }
            }
        }

        setContent {
            GeoLocatrTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val locationState = locationUtility
                        .currentLocationStateFlow
                        .collectAsStateWithLifecycle(lifecycle = this@MainActivity.lifecycle)

                    val addressState = locationUtility
                        .currentAddressStateFlow
                        .collectAsStateWithLifecycle(lifecycle = this@MainActivity.lifecycle)

                    val locationAvailableState = locationUtility
                        .isLocationAvailableStateFlow
                        .collectAsStateWithLifecycle(lifecycle = this@MainActivity.lifecycle)

                    LaunchedEffect(locationState.value) {
                        locationUtility.getAddress(locationState.value)
                    }

                    LocationScreen(
                        location = locationState.value,
                        locationAvailable = locationAvailableState.value,
                        onGetLocation = {
                            locationUtility.checkPermissionAndGetLocation(this@MainActivity,
                                                                                    permissionLauncher)
                        },
                        address = addressState.value,
                        onNotify = { lastLocation ->
                            locationAlarmReceiver.lastLocation = lastLocation
                            locationAlarmReceiver.scheduleAlarm(this@MainActivity)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationUtility.removeLocationRequest()
    }

    override fun onStart() {
        super.onStart()
        locationUtility.checkIfLocationCanBeRetrieved(this, locationLauncher)
    }
}