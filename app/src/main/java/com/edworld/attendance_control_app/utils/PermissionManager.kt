package com.edworld.attendance_control_app.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionManager {

    // Códigos de solicitud de permisos
    const val LOCATION_PERMISSION_CODE = 100
    const val CAMERA_PERMISSION_CODE = 101
    const val STORAGE_PERMISSION_CODE = 102

    // Permisos necesarios
    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val CAMERA_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )

    val STORAGE_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    /**
     * Verifica si todos los permisos de ubicación están concedidos
     */
    fun hasLocationPermission(context: Context): Boolean {
        return LOCATION_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Verifica si el permiso de cámara está concedido
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Verifica si los permisos de almacenamiento están concedidos
     */
    fun hasStoragePermission(context: Context): Boolean {
        return STORAGE_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Solicita permisos de ubicación
     */
    fun requestLocationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            LOCATION_PERMISSIONS,
            LOCATION_PERMISSION_CODE
        )
    }

    /**
     * Solicita permiso de cámara
     */
    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            CAMERA_PERMISSIONS,
            CAMERA_PERMISSION_CODE
        )
    }

    /**
     * Solicita permisos de almacenamiento
     */
    fun requestStoragePermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            STORAGE_PERMISSIONS,
            STORAGE_PERMISSION_CODE
        )
    }

    /**
     * Maneja los resultados de solicitud de permisos
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onLocationGranted: () -> Unit = {},
        onLocationDenied: () -> Unit = {},
        onCameraGranted: () -> Unit = {},
        onCameraDenied: () -> Unit = {},
        onStorageGranted: () -> Unit = {},
        onStorageDenied: () -> Unit = {}
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    onLocationGranted()
                } else {
                    onLocationDenied()
                }
            }

            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onCameraGranted()
                } else {
                    onCameraDenied()
                }
            }

            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    onStorageGranted()
                } else {
                    onStorageDenied()
                }
            }
        }
    }
}