package com.edworld.attendance_control_app.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.edworld.attendance_control_app.dataStore
import com.edworld.attendance_control_app.USER_ID_KEY
import com.edworld.attendance_control_app.data.models.*
import com.edworld.attendance_control_app.utils.PermissionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Base64

class RegistrarAsistenciaActivity : ComponentActivity() {

    // Estados para la UI
    private var currentStep by mutableStateOf(RegistroStep.SCANNING)
    private var isLoading by mutableStateOf(false)
    private var qrData by mutableStateOf<QRClassData?>(null)
    private var originalJWT by mutableStateOf("") // AGREGAR: JWT original del QR
    private var ubicacionActual by mutableStateOf<LocationData?>(null)
    private var isObteniendoUbicacion by mutableStateOf(false)
    private var errorMessage by mutableStateOf("")
    private var asistenciaRegistrada by mutableStateOf<AsistenciaRegistradaResponse?>(null)

    // Cliente de ubicación
    private lateinit var fusedLocationClient: FusedLocationProviderClient

//    val url: String = "http://192.168.100.101:3000"
//        val url:String="http://172.20.10.3:300"

    // Launcher para QR Scanner usando Intent
    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val qrContent = result.data?.getStringExtra("SCAN_RESULT")
            if (!qrContent.isNullOrEmpty()) {
                processQR(qrContent)
            } else {
                Toast.makeText(this, "No se detectó ningún código QR", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para permisos de cámara
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            currentStep = RegistroStep.SCANNING
        } else {
            Toast.makeText(
                this,
                "Se necesita permiso de cámara para escanear QR",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Verificar permisos de cámara
        checkCameraPermission()

        setContent {
            RegistrarAsistenciaScreen(
                currentStep = currentStep,
                isLoading = isLoading,
                qrData = qrData,
                ubicacionActual = ubicacionActual,
                isObteniendoUbicacion = isObteniendoUbicacion,
                errorMessage = errorMessage,
                asistenciaRegistrada = asistenciaRegistrada,
                onNavigateBack = { finish() },
                onQRScanned = { qrContent -> processQR(qrContent) },
                onConfirmarAsistencia = { confirmarAsistencia() },
                onReiniciarProceso = { reiniciarProceso() },
                onStartScanner = { startQRScanner() }
            )
        }
    }

    private fun startQRScanner() {
        try {
            val intent = Intent(this, com.journeyapps.barcodescanner.CaptureActivity::class.java)
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE")
            intent.putExtra("PROMPT_MESSAGE", "Escanea el código QR de la clase")
            intent.putExtra("BEEP_ENABLED", true)
            intent.putExtra("VIBRATE_ENABLED", true)
            qrScannerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir escáner: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                currentStep = RegistroStep.SCANNING
            }

            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun processQR(qrContent: String) {
        try {
            // Guardar JWT original completo
            originalJWT = qrContent

            // Extraer datos del JWT (simplificado - en producción validar la firma)
            val parts = qrContent.split(".")
            if (parts.size != 3) {
                errorMessage = "Código QR inválido"
                currentStep = RegistroStep.ERROR
                return
            }

            // Decodificar payload del JWT
            val payload = String(Base64.getDecoder().decode(parts[1]))
            val json = JSONObject(payload)

            qrData = QRClassData(
                materiaId = json.getInt("materia_id"),
                horarioId = json.getInt("horario_id"),
                docenteId = json.getInt("docente_id"),
                materiaNombre = json.optString("materia_nombre", "Clase"),
                diaSemana = json.getString("dia_semana"),
                horaInicio = json.getString("hora_inicio"),
                horaFin = json.getString("hora_fin"),
                fecha = json.getString("fecha"),
                exp = json.getLong("exp")
            )

            // Verificar si no está expirado
            val ahora = System.currentTimeMillis() / 1000
            if (qrData!!.exp < ahora) {
                errorMessage = "El código QR ha expirado"
                currentStep = RegistroStep.ERROR
                return
            }

            // Obtener ubicación
            obtenerUbicacion()

        } catch (e: Exception) {
            errorMessage = "Error al procesar QR: ${e.message}"
            currentStep = RegistroStep.ERROR
        }
    }

    private fun obtenerUbicacion() {
        if (!PermissionManager.hasLocationPermission(this)) {
            PermissionManager.requestLocationPermission(this)
            return
        }

        isObteniendoUbicacion = true
        currentStep = RegistroStep.GETTING_LOCATION

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    isObteniendoUbicacion = false
                    if (location != null) {
                        ubicacionActual = LocationData(
                            latitud = location.latitude,
                            longitud = location.longitude
                        )
                        currentStep = RegistroStep.CONFIRMATION
                    } else {
                        errorMessage = "No se pudo obtener la ubicación actual"
                        currentStep = RegistroStep.ERROR
                    }
                }
                .addOnFailureListener { e ->
                    isObteniendoUbicacion = false
                    errorMessage = "Error al obtener ubicación: ${e.localizedMessage}"
                    currentStep = RegistroStep.ERROR
                }
        } catch (e: SecurityException) {
            isObteniendoUbicacion = false
            errorMessage = "Error de permisos de ubicación"
            currentStep = RegistroStep.ERROR
        }
    }

    private fun confirmarAsistencia() {
        if (qrData == null || ubicacionActual == null) {
            Toast.makeText(this, "Faltan datos para confirmar asistencia", Toast.LENGTH_SHORT)
                .show()
            return
        }

        isLoading = true
        errorMessage = ""

        lifecycleScope.launch {
            try {
                val userId = getUserId()
                if (userId == null) {
                    errorMessage = "Error: No se encontró ID del usuario"
                    currentStep = RegistroStep.ERROR
                    isLoading = false
                    return@launch
                }

                val request = RegistrarAsistenciaRequest(
                    qr_token = originalJWT, // Usar JWT original guardado
                    estudiante_id = userId.toInt(),
                    ubicacion_lat = ubicacionActual!!.latitud,
                    ubicacion_lng = ubicacionActual!!.longitud
                )

                val response = ApiClient.client.post("${Constants.BASE_URL}/attendance/asistencia") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                when (response.status) {
                    HttpStatusCode.Created -> {
                        val asistenciaResponse: AsistenciaRegistradaResponse = response.body()
                        asistenciaRegistrada = asistenciaResponse
                        currentStep = RegistroStep.SUCCESS
                        isLoading = false
                    }

                    HttpStatusCode.BadRequest -> {
                        val errorResponse = response.body<Map<String, String>>()
                        errorMessage = errorResponse["error"] ?: "Error en la solicitud"
                        currentStep = RegistroStep.ERROR
                        isLoading = false
                    }

                    HttpStatusCode.Forbidden -> {
                        val errorResponse = response.body<Map<String, String>>()
                        errorMessage = errorResponse["error"] ?: "No estás inscrito en esta materia"
                        currentStep = RegistroStep.ERROR
                        isLoading = false
                    }

                    HttpStatusCode.Conflict -> {
                        val errorResponse = response.body<Map<String, String>>()
                        errorMessage = errorResponse["error"] ?: "Ya registraste asistencia hoy"
                        currentStep = RegistroStep.ERROR
                        isLoading = false
                    }

                    HttpStatusCode.InternalServerError -> {
                        errorMessage = "Error interno del servidor"
                        currentStep = RegistroStep.ERROR
                        isLoading = false
                    }

                    else -> {
                        errorMessage = "Error desconocido: ${response.status}"
                        currentStep = RegistroStep.ERROR
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.localizedMessage}"
                currentStep = RegistroStep.ERROR
                isLoading = false
            }
        }
    }

    private fun reiniciarProceso() {
        currentStep = RegistroStep.SCANNING
        qrData = null
        originalJWT = "" // Limpiar JWT guardado
        ubicacionActual = null
        errorMessage = ""
        asistenciaRegistrada = null
        isLoading = false
        isObteniendoUbicacion = false
    }

    private suspend fun getUserId(): String? {
        return dataStore.data.first()[USER_ID_KEY]
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
                obtenerUbicacion()
            },
            onLocationDenied = {
                errorMessage = "Se necesitan permisos de ubicación para registrar asistencia"
                currentStep = RegistroStep.ERROR
            }
        )
    }
}

// Enum para los pasos del registro
enum class RegistroStep {
    SCANNING,
    GETTING_LOCATION,
    CONFIRMATION,
    SUCCESS,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarAsistenciaScreen(
    currentStep: RegistroStep,
    isLoading: Boolean,
    qrData: QRClassData?,
    ubicacionActual: LocationData?,
    isObteniendoUbicacion: Boolean,
    errorMessage: String,
    asistenciaRegistrada: AsistenciaRegistradaResponse?,
    onNavigateBack: () -> Unit = {},
    onQRScanned: (String) -> Unit = {},
    onConfirmarAsistencia: () -> Unit = {},
    onReiniciarProceso: () -> Unit = {},
    onStartScanner: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentStep) {
                            RegistroStep.SCANNING -> "Escanear QR"
                            RegistroStep.GETTING_LOCATION -> "Obteniendo Ubicación"
                            RegistroStep.CONFIRMATION -> "Confirmar Asistencia"
                            RegistroStep.SUCCESS -> "Asistencia Registrada"
                            RegistroStep.ERROR -> "Error"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E3A8A)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            when (currentStep) {
                RegistroStep.SCANNING -> {
                    QRScannerStep(
                        onQRScanned = onQRScanned,
                        onStartScanner = onStartScanner
                    )
                }

                RegistroStep.GETTING_LOCATION -> {
                    ObteniendoUbicacionStep(isObteniendoUbicacion = isObteniendoUbicacion)
                }

                RegistroStep.CONFIRMATION -> {
                    ConfirmacionStep(
                        qrData = qrData,
                        ubicacionActual = ubicacionActual,
                        isLoading = isLoading,
                        onConfirmar = onConfirmarAsistencia
                    )
                }

                RegistroStep.SUCCESS -> {
                    ExitoStep(
                        asistenciaRegistrada = asistenciaRegistrada,
                        onReiniciar = onReiniciarProceso
                    )
                }

                RegistroStep.ERROR -> {
                    ErrorStep(
                        errorMessage = errorMessage,
                        onReiniciar = onReiniciarProceso
                    )
                }
            }
        }
    }
}

@Composable
private fun QRScannerStep(
    onQRScanned: (String) -> Unit,
    onStartScanner: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icono grande
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = "Scanner",
            tint = Color(0xFF1E3A8A),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Escanear Código QR",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E3A8A),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Presiona el botón para abrir la cámara y escanear el código QR de la clase",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Botón para abrir scanner
        Button(
            onClick = onStartScanner,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E3A8A)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Escanear",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Abrir Escáner QR",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Información adicional
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "💡 Consejos para escanear:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Mantén el dispositivo estable\n• Asegúrate de tener buena iluminación\n• Coloca el QR dentro del marco de la cámara",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ObteniendoUbicacionStep(
    isObteniendoUbicacion: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color(0xFF1E3A8A),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Obteniendo ubicación...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E3A8A)
            )
            Text(
                text = "Por favor espera",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ConfirmacionStep(
    qrData: QRClassData?,
    ubicacionActual: LocationData?,
    isLoading: Boolean,
    onConfirmar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Información de la clase
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Información de la clase:",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = qrData?.materiaNombre ?: "Clase",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${qrData?.diaSemana?.replaceFirstChar { it.uppercase() }} - ${qrData?.horaInicio} a ${qrData?.horaFin}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }

        // Información de ubicación
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Tu ubicación:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lat: ${String.format("%.6f", ubicacionActual?.latitud ?: 0.0)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Lng: ${String.format("%.6f", ubicacionActual?.longitud ?: 0.0)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botón confirmar
        Button(
            onClick = onConfirmar,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registrando...")
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Confirmar"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirmar Asistencia")
            }
        }
    }
}

@Composable
private fun ExitoStep(
    asistenciaRegistrada: AsistenciaRegistradaResponse?,
    onReiniciar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Éxito",
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "¡Asistencia Registrada!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = asistenciaRegistrada?.message ?: "Tu asistencia ha sido registrada exitosamente",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onReiniciar,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E3A8A)
            )
        ) {
            Text("Registrar Otra Asistencia")
        }
    }
}

@Composable
private fun ErrorStep(
    errorMessage: String,
    onReiniciar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = Color(0xFFDC2626),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFDC2626)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onReiniciar,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E3A8A)
            )
        ) {
            Text("Intentar de Nuevo")
        }
    }
}