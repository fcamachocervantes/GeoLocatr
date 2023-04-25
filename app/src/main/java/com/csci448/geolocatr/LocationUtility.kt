package com.csci448.geolocatr

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException

class LocationUtility(context: Context) {
    private val LOG_TAG = "448.geolocatr"

    private val mCurrentLocationStateFlow: MutableStateFlow<Location?>
            = MutableStateFlow(null)
    val currentLocationStateFlow: StateFlow<Location?>
        get() = mCurrentLocationStateFlow.asStateFlow()

    private val mCurrentAddressStateFlow: MutableStateFlow<String>
            = MutableStateFlow("")
    val currentAddressStateFlow: StateFlow<String>
        get() = mCurrentAddressStateFlow.asStateFlow()

    private val mIsLocationAvailableStateFlow: MutableStateFlow<Boolean>
            = MutableStateFlow(false)
    val isLocationAvailableStateFlow: StateFlow<Boolean>
        get() = mIsLocationAvailableStateFlow.asStateFlow()

    private val locationRequest = LocationRequest
        .Builder(Priority.PRIORITY_HIGH_ACCURACY, 0L)
        .setMaxUpdates(1)
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            mCurrentLocationStateFlow.value = locationResult.lastLocation
        }
    }

    private val geocoder = Geocoder(context)

    private val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    fun checkPermissionAndGetLocation(
                                        activity: Activity,
                                        permissionLauncher: ActivityResultLauncher<Array<String>>
    ){
        if( activity.checkSelfPermission( ACCESS_FINE_LOCATION ) == PERMISSION_GRANTED
            || activity.checkSelfPermission( ACCESS_COARSE_LOCATION ) == PERMISSION_GRANTED
        ) {
            Log.d(LOG_TAG, "Granted Permission")
            fusedLocationProviderClient
                .requestLocationUpdates(locationRequest,
                                        locationCallback,
                                        Looper.getMainLooper())
            Log.d(LOG_TAG, "Finished Requesting Location Update")
        }
        else {
            if( ActivityCompat.shouldShowRequestPermissionRationale(activity, ACCESS_FINE_LOCATION)
                || ActivityCompat.shouldShowRequestPermissionRationale(activity, ACCESS_COARSE_LOCATION)
            ) {
                Log.d(LOG_TAG, "Permission Denied")
            }
            else {
                Log.d(LOG_TAG, "Requesting Permission")
                val permissions = arrayOf("android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION")
                permissionLauncher.launch( permissions )
            }
        }
    }

    suspend fun getAddress(location: Location?) {
        val addressTextBuilder = StringBuilder()
        if (location != null) {
            try {
                val addresses = geocoder.getFromLocation(location.latitude,
                    location.longitude,
                    1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    for (i in 0..address.maxAddressLineIndex) {
                        if (i > 0) {
                            addressTextBuilder.append("\n")
                        }
                        addressTextBuilder.append( address.getAddressLine(i) )
                    }
                }
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Error getting address", e)
            }
        }
        mCurrentAddressStateFlow.update { addressTextBuilder.toString() }
    }

    fun removeLocationRequest() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    fun verifyLocationSettingsStates(states: LocationSettingsStates?) {
        mIsLocationAvailableStateFlow.update { states?.isLocationUsable ?: false }
    }

    fun setStartingLocation(location: Location?) {
        mCurrentLocationStateFlow.update { location }
    }

    fun checkIfLocationCanBeRetrieved(
        activity: Activity,
        locationLauncher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(activity)
        client.checkLocationSettings(builder.build()).apply {
            addOnSuccessListener { response ->
                verifyLocationSettingsStates(response.locationSettingsStates)
            }
            addOnFailureListener { exc ->
                mIsLocationAvailableStateFlow.update { false }
                if (exc is ResolvableApiException) {
                    locationLauncher
                        .launch(IntentSenderRequest.Builder(exc.resolution).build())
                }
            }
        }
    }
}