package com.edworld.attendance_control_app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.edworld.attendance_control_app.MainActivity
import com.edworld.attendance_control_app.USER_ID_KEY
import com.edworld.attendance_control_app.dataStore
import com.edworld.attendance_control_app.screens.HomeDocenteScreen
import com.edworld.attendance_control_app.utils.PermissionManager
import kotlinx.coroutines.flow.first

class HomeDocenteActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HomeDocenteScreen(
                onCrearMateriaClick = { checkLocationForCrearMateria() },
                onQRScanClick = { checkCameraForQR() },
                onAsistenciasClick = { checkLocationForAsistencias() },
                onLogoutClick = { logout() }
            )
        }
    }

    /**
     * Verifica ubicación antes de crear materia
     */
    private fun checkLocationForCrearMateria() {
        if (PermissionManager.hasLocationPermission(this)) {
            // Navegar a CrearMateriaActivity
            navigateToCrearMateria()
        } else {
            Toast.makeText(
                this,
                "Se necesita acceso a ubicación para crear materias",
                Toast.LENGTH_SHORT
            ).show()
            PermissionManager.requestLocationPermission(this)
        }
    }

    /**
     * Verifica cámara antes de usar QR
     */
    private fun checkCameraForQR() {
        if (PermissionManager.hasCameraPermission(this)) {
            // Abrir QR Scanner
            navigateToQRScanner()
        } else {
            Toast.makeText(this, "Se necesita acceso a cámara para escanear QR", Toast.LENGTH_SHORT)
                .show()
            PermissionManager.requestCameraPermission(this)
        }
    }

    /**
     * Verifica ubicación antes de ver asistencias
     */
    private fun checkLocationForAsistencias() {
        if (PermissionManager.hasLocationPermission(this)) {
            // Navegar a AsistenciasActivity
            navigateToAsistencias()
        } else {
            Toast.makeText(
                this,
                "Se necesita acceso a ubicación para gestionar asistencias",
                Toast.LENGTH_SHORT
            ).show()
            PermissionManager.requestLocationPermission(this)
        }
    }

    private fun logout() {
        // Crear intent para regresar al MainActivity (login)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private suspend fun getUserId(): String? {
        return dataStore.data.first()[USER_ID_KEY] as String?
    }

//    private fun loadMaterias() {
//        lifecycleScope.launch {
//            val userId = getUserId()
//            if (userId != null) {
//                // Llamar API para obtener materias del docente
//                getMateriasByDocente(userId.toInt(), onSuccess, onError)
//            }
//        }
//    }

    private fun navigateToCrearMateria() {
        // TODO: Implementar navegación a CrearMateriaActivity
        Toast.makeText(this, "Navegando a Crear Materia...", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToQRScanner() {
        // TODO: Implementar navegación a QRScannerActivity
        Toast.makeText(this, "Abriendo QR Scanner...", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToAsistencias() {
        // TODO: Implementar navegación a AsistenciasActivity
        Toast.makeText(this, "Navegando a Asistencias...", Toast.LENGTH_SHORT).show()
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
                // Aquí puedes reintentar la acción que requería ubicación
            },
            onLocationDenied = {
                Toast.makeText(
                    this,
                    "Permisos de ubicación denegados. Algunas funciones no estarán disponibles.",
                    Toast.LENGTH_LONG
                ).show()
            },
            onCameraGranted = {
                Toast.makeText(this, "Permiso de cámara concedido", Toast.LENGTH_SHORT).show()
                // Aquí puedes reintentar la acción que requería cámara
            },
            onCameraDenied = {
                Toast.makeText(
                    this,
                    "Permiso de cámara denegado. No se podrá escanear códigos QR.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}