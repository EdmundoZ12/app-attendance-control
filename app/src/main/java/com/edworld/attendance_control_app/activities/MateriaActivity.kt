package com.edworld.attendance_control_app.activities

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.edworld.attendance_control_app.dataStore
import com.edworld.attendance_control_app.USER_ID_KEY
import com.edworld.attendance_control_app.data.models.*
import com.edworld.attendance_control_app.screens.MateriaScreen
import com.edworld.attendance_control_app.utils.PermissionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MateriaActivity : ComponentActivity() {

    // Estados para la UI
    private var isLoading by mutableStateOf(false)
    private var isEditMode by mutableStateOf(false)
    private var materiaId by mutableStateOf<Int?>(null)

    // Estados para GPS
    private var currentLatitud by mutableStateOf(0.0)
    private var currentLongitud by mutableStateOf(0.0)
    private var selectedLatitud by mutableStateOf(0.0)
    private var selectedLongitud by mutableStateOf(0.0)
    private var isLocationLoading by mutableStateOf(false)

    // Estados para datos cargados en modo edición
    private var datosMateriaCargados by mutableStateOf<Materia?>(null)
    private var horariosCargados by mutableStateOf<List<Horario>>(emptyList())

    // Cliente de ubicación
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // URL del servidor
//    val url: String = "http://192.168.100.101:3000"
//        val url:String="http://172.20.10.3:300"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtener parámetros del Intent
        isEditMode = intent.getBooleanExtra("isEditMode", false)
        materiaId = intent.getIntExtra("materiaId", -1).takeIf { it != -1 }

        // Verificar permisos y obtener ubicación
        checkLocationPermissionAndGetLocation()

        setContent {
            MateriaScreen(
                isEditMode = isEditMode,
                materiaId = materiaId,
                isLoading = isLoading,
                currentLatitud = selectedLatitud,
                currentLongitud = selectedLongitud,
                isLocationLoading = isLocationLoading,
                datosMateriaCargados = datosMateriaCargados,
                horariosCargados = horariosCargados,
                onNavigateBack = { finish() },
                onLocationUpdate = { lat, lng ->
                    selectedLatitud = lat
                    selectedLongitud = lng
                },
                onGuardarMateria = { nombre, codigo, descripcion, grupo, horarios, estadoActivo ->  // ← AGREGAR estadoActivo
                    isLoading = true
                    if (isEditMode && materiaId != null) {
                        actualizarMateria(
                            materiaId = materiaId!!,
                            nombre = nombre,
                            codigo = codigo,
                            descripcion = descripcion,
                            grupo = grupo,
                            latitud = selectedLatitud,
                            longitud = selectedLongitud,
                            activo = estadoActivo,  // ← USAR estadoActivo EN LUGAR DE datosMateriaCargados?.activo
                            onSuccess = {
                                isLoading = false
                                finish()
                            },
                            onError = { error ->
                                isLoading = false
                                Toast.makeText(this@MateriaActivity, error, Toast.LENGTH_LONG)
                                    .show()
                            }
                        )
                    } else {
                        // Para crear materia también agregar el parámetro
                        crearMateria(
                            nombre = nombre,
                            codigo = codigo,
                            descripcion = descripcion,
                            grupo = grupo,
                            latitud = selectedLatitud,
                            longitud = selectedLongitud,
                            horarios = horarios,
                            onSuccess = {
                                isLoading = false
                                finish()
                            },
                            onError = { error ->
                                isLoading = false
                                Toast.makeText(this@MateriaActivity, error, Toast.LENGTH_LONG)
                                    .show()
                            }
                        )
                    }
                },
                onAgregarHorario = { diaSemana, horaInicio, horaFin, onSuccess, onError ->
                    if (isEditMode && materiaId != null) {
                        agregarHorario(
                            materiaId = materiaId!!,
                            diaSemana = diaSemana,
                            horaInicio = horaInicio,
                            horaFin = horaFin,
                            onSuccess = { horario ->
                                horariosCargados = horariosCargados + horario
                                onSuccess(horario)
                            },
                            onError = onError
                        )
                    }
                },
                onActualizarHorario = { horarioId, diaSemana, horaInicio, horaFin, onSuccess, onError ->
                    actualizarHorario(
                        horarioId = horarioId,
                        diaSemana = diaSemana,
                        horaInicio = horaInicio,
                        horaFin = horaFin,
                        onSuccess = { horarioActualizado ->
                            horariosCargados = horariosCargados.map {
                                if (it.id == horarioId) horarioActualizado else it
                            }
                            onSuccess(horarioActualizado)
                        },
                        onError = onError
                    )
                },
                onEliminarHorario = { horarioId, onSuccess, onError ->
                    eliminarHorario(
                        horarioId = horarioId,
                        onSuccess = {
                            horariosCargados = horariosCargados.filter { it.id != horarioId }
                            onSuccess()
                        },
                        onError = onError
                    )
                },
            )
        }

        // Cargar datos si es modo edición
        if (isEditMode && materiaId != null) {
            isLoading = true
            cargarMateriaParaEdicion(
                materiaId = materiaId!!,
                onSuccess = { materia, horarios ->
                    datosMateriaCargados = materia
                    horariosCargados = horarios
                    selectedLatitud = materia.latitud
                    selectedLongitud = materia.longitud
                    isLoading = false
                },
                onError = { error ->
                    isLoading = false
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun checkLocationPermissionAndGetLocation() {
        if (PermissionManager.hasLocationPermission(this)) {
            getCurrentLocation()
        } else {
            PermissionManager.requestLocationPermission(this)
        }
    }

    private fun getCurrentLocation() {
        if (!PermissionManager.hasLocationPermission(this)) {
            Toast.makeText(this, "Permisos de ubicación requeridos", Toast.LENGTH_SHORT).show()
            return
        }

        isLocationLoading = true

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    isLocationLoading = false
                    if (location != null) {
                        currentLatitud = location.latitude
                        currentLongitud = location.longitude

                        // Solo actualizar si no hay coordenadas seleccionadas y no es modo edición
                        if (selectedLatitud == 0.0 && selectedLongitud == 0.0 && !isEditMode) {
                            selectedLatitud = location.latitude
                            selectedLongitud = location.longitude
                        }

                        Toast.makeText(this, "Ubicación obtenida", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            this,
                            "No se pudo obtener la ubicación actual",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener { e ->
                    isLocationLoading = false
                    Toast.makeText(
                        this,
                        "Error al obtener ubicación: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } catch (e: SecurityException) {
            isLocationLoading = false
            Toast.makeText(this, "Error de permisos de ubicación", Toast.LENGTH_SHORT).show()
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
                getCurrentLocation()
            },
            onLocationDenied = {
                Toast.makeText(
                    this,
                    "Los permisos de ubicación son necesarios para crear materias",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    // Función para cargar datos de materia en modo edición
    private fun cargarMateriaParaEdicion(
        materiaId: Int,
        onSuccess: (Materia, List<Horario>) -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                // Llamada 1: Obtener datos de la materia
                val materiaResponse = ApiClient.client.get("${Constants.BASE_URL}/academic/materia") {
                    contentType(ContentType.Application.Json)
                    setBody(ObtenerMateriaRequest(materiaId))
                }

                if (materiaResponse.status != HttpStatusCode.OK) {
                    onError("Error al cargar la materia: ${materiaResponse.status}")
                    return@launch
                }

                val materia = materiaResponse.body<MateriaResponseSingle>().materia

                // Llamada 2: Obtener horarios de la materia
                val horariosResponse = ApiClient.client.get("${Constants.BASE_URL}/academic/materias/horarios") {
                    contentType(ContentType.Application.Json)
                    setBody(ObtenerHorariosRequest(materiaId))
                }

                if (horariosResponse.status != HttpStatusCode.OK) {
                    onError("Error al cargar horarios: ${horariosResponse.status}")
                    return@launch
                }

                val horarios = horariosResponse.body<HorariosResponse>().horarios
                onSuccess(materia, horarios)

            } catch (e: Exception) {
                onError("Error de conexión: ${e.localizedMessage}")
            }
        }
    }

    private fun crearMateria(
        nombre: String,
        codigo: String,
        descripcion: String,
        grupo: String,
        latitud: Double,
        longitud: Double,
        horarios: List<HorarioItem>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val userId = getUserId()
                if (userId == null) {
                    onError("Error: No se encontró ID del usuario")
                    return@launch
                }

                val horariosRequest = horarios.map { horario ->
                    HorarioRequest(
                        dia_semana = horario.dia.lowercase(),
                        hora_inicio = horario.horaInicio,
                        hora_fin = horario.horaFin
                    )
                }

                val request = CrearMateriaRequest(
                    nombre = nombre,
                    codigo = codigo,
                    descripcion = descripcion,
                    grupo = grupo,
                    docente_id = userId.toInt(),
                    latitud = latitud,
                    longitud = longitud,
                    horarios = horariosRequest
                )

                val response = ApiClient.client.post("${Constants.BASE_URL}/academic/materia") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                when (response.status) {
                    HttpStatusCode.Created -> {
                        val materiaResponse: MateriaOperationResponse = response.body()
                        Toast.makeText(
                            this@MateriaActivity,
                            materiaResponse.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        setResult(Activity.RESULT_OK)
                        onSuccess()
                    }

                    HttpStatusCode.Unauthorized -> {
                        onError("Las coordenadas GPS no son válidas")
                    }

                    HttpStatusCode.PaymentRequired -> {
                        onError("Uno o más horarios tienen formato inválido")
                    }

                    HttpStatusCode.NotFound -> {
                        onError("El docente no existe")
                    }

                    HttpStatusCode.InternalServerError -> {
                        onError("Error interno del sistema")
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

    // Función para actualizar materia (solo campos básicos)
    private fun actualizarMateria(
        materiaId: Int,
        nombre: String,
        codigo: String,
        descripcion: String,
        grupo: String,
        latitud: Double,
        longitud: Double,
        activo: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val request = ActualizarMateriaRequest(
                    id = materiaId,
                    nombre = nombre,
                    codigo = codigo,
                    descripcion = descripcion,
                    grupo = grupo,
                    latitud = latitud,
                    longitud = longitud,
                    activo = activo
                )

                val response = ApiClient.client.put("${Constants.BASE_URL}/academic/materia") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val materiaResponse: MateriaOperationResponse = response.body()
                        Toast.makeText(
                            this@MateriaActivity,
                            materiaResponse.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        setResult(Activity.RESULT_OK)
                        onSuccess()
                    }

                    HttpStatusCode.PaymentRequired -> {
                        onError("Materia no encontrada")
                    }

                    HttpStatusCode.NotFound -> {
                        onError("Las coordenadas GPS no son válidas")
                    }

                    HttpStatusCode.InternalServerError -> {
                        onError("Error interno del sistema")
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

    // Función para agregar nuevo horario
    private fun agregarHorario(
        materiaId: Int,
        diaSemana: String,
        horaInicio: String,
        horaFin: String,
        onSuccess: (Horario) -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val request = AgregarHorarioRequest(
                    materia_id = materiaId,
                    dia_semana = diaSemana.lowercase(),
                    hora_inicio = horaInicio,
                    hora_fin = horaFin
                )

                val response = ApiClient.client.post("${Constants.BASE_URL}/academic/materia/horario") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                when (response.status) {
                    HttpStatusCode.Created -> {
                        val horarioResponse: HorarioOperationResponse = response.body()
                        Toast.makeText(
                            this@MateriaActivity,
                            horarioResponse.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        onSuccess(horarioResponse.horario)
                    }

                    HttpStatusCode.PaymentRequired -> {
                        onError("Formato de horario inválido")
                    }

                    HttpStatusCode.InternalServerError -> {
                        onError("Error interno del sistema")
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

    // Función para actualizar horario existente
    private fun actualizarHorario(
        horarioId: Int,
        diaSemana: String,
        horaInicio: String,
        horaFin: String,
        onSuccess: (Horario) -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val request = ActualizarHorarioRequest(
                    horario_id = horarioId,
                    dia_semana = diaSemana.lowercase(),
                    hora_inicio = horaInicio,
                    hora_fin = horaFin
                )

                val response = ApiClient.client.put("${Constants.BASE_URL}/academic/materia/horario") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val horarioResponse: HorarioOperationResponse = response.body()
                        Toast.makeText(
                            this@MateriaActivity,
                            horarioResponse.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        onSuccess(horarioResponse.horario)
                    }

                    HttpStatusCode.PaymentRequired -> {
                        onError("Formato de horario inválido")
                    }

                    HttpStatusCode.NotFound -> {
                        onError("Horario no encontrado")
                    }

                    HttpStatusCode.InternalServerError -> {
                        onError("Error interno del sistema")
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

    // Función para eliminar horario
    private fun eliminarHorario(
        horarioId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val request = EliminarHorarioRequest(horarioId)

                val response = ApiClient.client.delete("${Constants.BASE_URL}/academic/materia/horario") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val horarioResponse: HorarioOperationResponse = response.body()
                        Toast.makeText(
                            this@MateriaActivity,
                            horarioResponse.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        onSuccess()
                    }

                    HttpStatusCode.NotFound -> {
                        onError("Horario no encontrado")
                    }

                    HttpStatusCode.InternalServerError -> {
                        onError("Error interno del sistema")
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

    private fun cambiarEstadoMateria(
        materiaId: Int,
        onSuccess: (String, Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val request = CambiarEstadoMateriaRequest(materiaId)

                val response = ApiClient.client.patch("${Constants.BASE_URL}/academic/materia") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val materiaResponse: MateriaOperationResponse = response.body()
                        onSuccess(materiaResponse.message, materiaResponse.materia.activo)
                    }

                    HttpStatusCode.NotFound -> {
                        onError("Materia no encontrada")
                    }

                    HttpStatusCode.InternalServerError -> {
                        onError("Error interno del sistema")
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

    private suspend fun getUserId(): String? {
        return dataStore.data.first()[USER_ID_KEY]
    }
}