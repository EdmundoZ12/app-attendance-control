package com.edworld.attendance_control_app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.edworld.attendance_control_app.MainActivity
import com.edworld.attendance_control_app.screens.HomeEstudianteScreen
import com.edworld.attendance_control_app.utils.PermissionManager

class HomeEstudianteActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HomeEstudianteScreen(
                onUnirseClaseClick = { checkCameraForQR() },
                onMarcarAsistenciaClick = { checkLocationForAsistencia() },
                onLogoutClick = { logout() }
            )
        }
    }

    /**
     * Verifica cámara para unirse por QR
     */
    private fun checkCameraForQR() {
        if (PermissionManager.hasCameraPermission(this)) {
            navigateToQRScanner()
        } else {
            Toast.makeText(
                this,
                "Se necesita acceso a cámara para escanear códigos QR",
                Toast.LENGTH_SHORT
            ).show()
            PermissionManager.requestCameraPermission(this)
        }
    }

    /**
     * Verifica ubicación para marcar asistencia
     */
    private fun checkLocationForAsistencia() {
        if (PermissionManager.hasLocationPermission(this)) {
            navigateToAsistencia()
        } else {
            Toast.makeText(
                this,
                "Se necesita acceso a ubicación para marcar asistencia",
                Toast.LENGTH_SHORT
            ).show()
            PermissionManager.requestLocationPermission(this)
        }
    }

    private fun navigateToQRScanner() {
        Toast.makeText(this, "Abriendo escáner QR...", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToAsistencia() {
        Toast.makeText(this, "Navegando a marcar asistencia...", Toast.LENGTH_SHORT).show()
    }

    private fun logout() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Maneja las respuestas de solicitud de permisos
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        PermissionManager.handlePermissionResult(
            requestCode = requestCode,
            permissions = permissions,
            grantResults = grantResults,
            onLocationGranted = {
                Toast.makeText(this, "Permisos de ubicación concedidos", Toast.LENGTH_SHORT).show()
            },
            onLocationDenied = {
                Toast.makeText(
                    this,
                    "Sin ubicación no se puede marcar asistencia",
                    Toast.LENGTH_LONG
                ).show()
            },
            onCameraGranted = {
                Toast.makeText(this, "Permiso de cámara concedido", Toast.LENGTH_SHORT).show()
            },
            onCameraDenied = {
                Toast.makeText(
                    this,
                    "Sin cámara no se puede escanear códigos QR",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}