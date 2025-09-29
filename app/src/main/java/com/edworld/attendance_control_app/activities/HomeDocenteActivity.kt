package com.edworld.attendance_control_app.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.edworld.attendance_control_app.MainActivity
import com.edworld.attendance_control_app.clearToken
import com.edworld.attendance_control_app.dataStore
import com.edworld.attendance_control_app.USER_ID_KEY
import com.edworld.attendance_control_app.data.models.*
import com.edworld.attendance_control_app.screens.HomeDocenteScreen
import com.edworld.attendance_control_app.utils.PermissionManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object ApiClient {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) { gson() }
    }
}

class HomeDocenteActivity : ComponentActivity() {

    private var materias by mutableStateOf<List<Materia>>(emptyList())
    private var isLoading by mutableStateOf(true)

    // Launcher para crear/editar materia
    private lateinit var crearMateriaLauncher: ActivityResultLauncher<Intent>

//    val url: String = "http://192.168.100.101:3000"
    //    val url:String="http://172.20.10.3:300"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar launcher antes del setContent
        crearMateriaLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Se creó/editó una materia exitosamente, refrescar lista
                Toast.makeText(this, "Operación exitosa", Toast.LENGTH_SHORT).show()
                loadMateriasInitial() // Recargar las materias
            }
        }

        setContent {
            HomeDocenteScreen(
                onCrearMateriaClick = { checkLocationForCrearMateria() },
                onEditarMateriaClick = { materiaId -> navigateToEditarMateria(materiaId) },
                onQRScanClick = { checkCameraForQR() },
                onAsistenciasClick = { navigateToAsistencias() },
                onLogoutClick = { logout() },
                onInscribirEstudiantesClick = { navigateToInscribirEstudiantes() },
                materias = materias,
                isLoading = isLoading,
                onLoadMaterias = {
                    isLoading = true
                    loadMaterias(
                        onSuccess = { materiasFromAPI ->
                            materias = materiasFromAPI
                            isLoading = false
                        },
                        onError = {
                            isLoading = false
                        }
                    )
                }
            )
        }

        // Cargar materias al iniciar
        loadMateriasInitial()
    }

    private suspend fun getUserId(): String? {
        return dataStore.data.first()[USER_ID_KEY]
    }

    private fun navigateToInscribirEstudiantes() {
        val intent = Intent(this, SeleccionarMateriaActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToAsistencias() {
        val intent = Intent(this, AsistenciasActivity::class.java)
        startActivity(intent)
    }

    private fun loadMateriasInitial() {
        lifecycleScope.launch {
            val userId = getUserId()
            if (userId != null) {
                getMateriasByDocente(
                    docenteId = userId.toInt(),
                    onSuccess = { materiasFromAPI ->
                        materias = materiasFromAPI
                        isLoading = false
                    },
                    onError = { error ->
                        Toast.makeText(this@HomeDocenteActivity, error, Toast.LENGTH_SHORT).show()
                        isLoading = false
                    }
                )
            } else {
                Toast.makeText(
                    this@HomeDocenteActivity,
                    "Error: No se encontró ID del usuario",
                    Toast.LENGTH_SHORT
                ).show()
                isLoading = false
            }
        }
    }

    private fun loadMaterias(
        onSuccess: (List<Materia>) -> Unit,
        onError: () -> Unit
    ) {
        lifecycleScope.launch {
            val userId = getUserId()
            if (userId != null) {
                getMateriasByDocente(
                    docenteId = userId.toInt(),
                    onSuccess = onSuccess,
                    onError = { error ->
                        Toast.makeText(this@HomeDocenteActivity, error, Toast.LENGTH_SHORT).show()
                        onError()
                    }
                )
            } else {
                Toast.makeText(
                    this@HomeDocenteActivity,
                    "Error: No se encontró ID del usuario",
                    Toast.LENGTH_SHORT
                ).show()
                onError()
            }
        }
    }

    private fun getMateriasByDocente(
        docenteId: Int,
        onSuccess: (List<Materia>) -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.client.get("${Constants.BASE_URL}/academic/materias") {
                    contentType(ContentType.Application.Json)
                    setBody(GetMateriasRequest(docenteId))
                }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val materiaResponse: MateriaResponse = response.body()
                        onSuccess(materiaResponse.materias)
                    }

                    HttpStatusCode.NotFound -> {
                        onError("No se encontraron materias")
                    }

                    HttpStatusCode.InternalServerError -> {
                        onError("Error interno del servidor")
                    }

                    else -> {
                        onError("Error desconocido: ${response.status}")
                    }
                }
            } catch (e: Exception) {
                onError("Error de conexión: ${e.localizedMessage}")
            }
        }
    }

    private fun checkLocationForCrearMateria() {
        if (PermissionManager.hasLocationPermission(this)) {
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

    private fun checkCameraForQR() {
        if (PermissionManager.hasCameraPermission(this)) {
            navigateToQRScanner()
        } else {
            Toast.makeText(this, "Se necesita acceso a cámara para escanear QR", Toast.LENGTH_SHORT)
                .show()
            PermissionManager.requestCameraPermission(this)
        }
    }


    private fun navigateToCrearMateria() {
        val intent = Intent(this, MateriaActivity::class.java)
        intent.putExtra("isEditMode", false)
        crearMateriaLauncher.launch(intent)
    }

    private fun navigateToEditarMateria(materiaId: Int) {
        val intent = Intent(this, MateriaActivity::class.java)
        intent.putExtra("isEditMode", true)
        intent.putExtra("materiaId", materiaId)
        crearMateriaLauncher.launch(intent)
    }

    private fun navigateToQRScanner() {
        Toast.makeText(this, "Abriendo QR Scanner...", Toast.LENGTH_SHORT).show()
    }


    private fun logout() {
        lifecycleScope.launch {
            clearToken(this@HomeDocenteActivity)
            val intent = Intent(this@HomeDocenteActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

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
                    "Sin ubicación algunas funciones no estarán disponibles.",
                    Toast.LENGTH_LONG
                ).show()
            },
            onCameraGranted = {
                Toast.makeText(this, "Permiso de cámara concedido", Toast.LENGTH_SHORT).show()
            },
            onCameraDenied = {
                Toast.makeText(
                    this,
                    "Sin cámara no se puede escanear códigos QR.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}