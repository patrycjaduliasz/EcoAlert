package com.example.ecoalert.ui.theme.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import pub.devrel.easypermissions.EasyPermissions

object PermissionUtils {

    private const val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            LOCATION_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationPermissionFromActivity(
        activity: Activity,
        rationale: String,
        requestCode: Int
    ) {
        EasyPermissions.requestPermissions(
            activity,
            rationale,
            requestCode,
            LOCATION_PERMISSION
        )
    }

    fun requestLocationPermissionFromFragment(
        fragment: Fragment,
        rationale: String,
        requestCode: Int
    ) {
        EasyPermissions.requestPermissions(
            fragment,
            rationale,
            requestCode,
            LOCATION_PERMISSION
        )
    }
}
