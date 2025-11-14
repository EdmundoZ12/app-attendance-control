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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.edworld.attendance_control_app.dataStore
import com.edworld.attendance_control_app.USER_ID_KEY
import com.edworld.attendance_control_app.data.models.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Base64

class InscribirseQRActivity : ComponentActivity() {

    // Estados para la UI
    private var currentStep by mutableStateOf(InscripcionStep.SCANNING)
    private var isLoading by mutableStateOf(false)
    private var qrData by mutableStateOf<QRInscripcionData?>(null)
    private var originalJWT by mutableStateOf("")
    private var errorMessage by mutableStateOf("")
    private var inscripcionExitosa by mutableStateOf<InscripcionQRResponse?>(null)

    // Launcher para QR Scanner
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
            currentStep = InscripcionStep.SCANNING
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

        // Verificar permisos de cámara
        checkCameraPermission()

        setContent {
            InscribirseQRScreen(
                currentStep = currentStep,
                isLoading = isLoading,
                qrData = qrData,
                errorMessage = errorMessage,
                inscripcionExitosa = inscripcionExitosa,
                onNavigateBack = { finish() },
                onQRScanned = { qrContent -> processQR(qrContent) },
                onConfirmarInscripcion = { confirmarInscripcion() },
                onReiniciarProceso = { reiniciarProceso() },
                onStartScanner = { startQRScanner() }
            )
        }
    }

    private fun startQRScanner() {
        try {
            val intent = Intent(this, com.journeyapps.barcodescanner.CaptureActivity::class.java)
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE")
            intent.putExtra("PROMPT_MESSAGE", "Escanea el código QR de inscripción")
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
                currentStep = InscripcionStep.SCANNING
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

            // Extraer datos del JWT
            val parts = qrContent.split(".")
            if (parts.size != 3) {
                errorMessage = "Código QR inválido"
                currentStep = InscripcionStep.ERROR
                return
            }

            // Decodificar payload del JWT
            val payload = String(Base64.getDecoder().decode(parts[1]))
            val json = JSONObject(payload)

            // Verificar que sea un QR de inscripción
            val tipo = json.optString("tipo", "")
            if (tipo != "INSCRIPCION") {
                errorMessage = "Este código QR no es para inscripción"
                currentStep = InscripcionStep.ERROR
                return
            }

            qrData = QRInscripcionData(
                qr_token = qrContent,
                qr_image = "", // No necesitamos la imagen
                expires_at = "", // Se validará en el backend
                tipo = tipo,
                materia = MateriaQRInfo(
                    id = json.getInt("materia_id"),
                    nombre = json.optString("materia_nombre", "Materia"),
                    codigo = json.optString("materia_codigo", ""),
                    grupo = json.optString("materia_grupo", "")
                )
            )

            // Verificar si no está expirado
            val exp = json.getLong("exp")
            val ahora = System.currentTimeMillis() / 1000
            if (exp < ahora) {
                errorMessage = "El código QR ha expirado"
                currentStep = InscripcionStep.ERROR
                return
            }

            // Pasar directamente a confirmación (no necesitamos ubicación)
            currentStep = InscripcionStep.CONFIRMATION

        } catch (e: Exception) {
            errorMessage = "Error al procesar QR: ${e.message}"
            currentStep = InscripcionStep.ERROR
        }
    }

    private fun confirmarInscripcion() {
        if (qrData == null) {
            Toast.makeText(this, "Faltan datos para confirmar inscripción", Toast.LENGTH_SHORT)
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
                    currentStep = InscripcionStep.ERROR
                    isLoading = false
                    return@launch
                }

                val request = InscribirseQRRequest(
                    qr_token = originalJWT,
                    estudiante_id = userId.toInt()
                )

                val response = ApiClient.client.post("${Constants.BASE_URL}/academic/asignacion/qr") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                when (response.status) {
                    HttpStatusCode.Created -> {
                        val inscripcionResponse: InscripcionQRResponse = response.body()
                        inscripcionExitosa = inscripcionResponse
                        currentStep = InscripcionStep.SUCCESS
                        isLoading = false
                    }

                    HttpStatusCode.BadRequest -> {
                        val errorResponse = response.body<Map<String, String>>()
                        errorMessage = errorResponse["error"] ?: "El código QR ha expirado o no es válido"
                        currentStep = InscripcionStep.ERROR
                        isLoading = false
                    }

                    HttpStatusCode.NotFound -> {
                        val errorResponse = response.body<Map<String, String>>()
                        errorMessage = errorResponse["error"] ?: "La materia o estudiante no existe"
                        currentStep = InscripcionStep.ERROR
                        isLoading = false
                    }

                    HttpStatusCode.Conflict -> {
                        val errorResponse = response.body<Map<String, String>>()
                        errorMessage = errorResponse["error"] ?: "Ya estás inscrito en esta materia"
                        currentStep = InscripcionStep.ERROR
                        isLoading = false
                    }

                    HttpStatusCode.InternalServerError -> {
                        errorMessage = "Error interno del servidor"
                        currentStep = InscripcionStep.ERROR
                        isLoading = false
                    }

                    else -> {
                        errorMessage = "Error desconocido: ${response.status}"
                        currentStep = InscripcionStep.ERROR
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.localizedMessage}"
                currentStep = InscripcionStep.ERROR
                isLoading = false
            }
        }
    }

    private fun reiniciarProceso() {
        currentStep = InscripcionStep.SCANNING
        qrData = null
        originalJWT = ""
        errorMessage = ""
        inscripcionExitosa = null
        isLoading = false
    }

    private suspend fun getUserId(): String? {
        return dataStore.data.first()[USER_ID_KEY]
    }
}

// Enum para los pasos de la inscripción
enum class InscripcionStep {
    SCANNING,
    CONFIRMATION,
    SUCCESS,
    ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InscribirseQRScreen(
    currentStep: InscripcionStep,
    isLoading: Boolean,
    qrData: QRInscripcionData?,
    errorMessage: String,
    inscripcionExitosa: InscripcionQRResponse?,
    onNavigateBack: () -> Unit = {},
    onQRScanned: (String) -> Unit = {},
    onConfirmarInscripcion: () -> Unit = {},
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
                            InscripcionStep.SCANNING -> "Escanear QR Inscripción"
                            InscripcionStep.CONFIRMATION -> "Confirmar Inscripción"
                            InscripcionStep.SUCCESS -> "Inscripción Exitosa"
                            InscripcionStep.ERROR -> "Error"
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
                InscripcionStep.SCANNING -> {
                    QRInscripcionScannerStep(
                        onQRScanned = onQRScanned,
                        onStartScanner = onStartScanner
                    )
                }

                InscripcionStep.CONFIRMATION -> {
                    ConfirmacionInscripcionStep(
                        qrData = qrData,
                        isLoading = isLoading,
                        onConfirmar = onConfirmarInscripcion
                    )
                }

                InscripcionStep.SUCCESS -> {
                    ExitoInscripcionStep(
                        inscripcionExitosa = inscripcionExitosa,
                        materiaNombre = qrData?.materia?.nombre ?: "",
                        onReiniciar = onReiniciarProceso,
                        onVolver = onNavigateBack
                    )
                }

                InscripcionStep.ERROR -> {
                    ErrorInscripcionStep(
                        errorMessage = errorMessage,
                        onReiniciar = onReiniciarProceso
                    )
                }
            }
        }
    }
}

@Composable
private fun QRInscripcionScannerStep(
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
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Escanear QR de Inscripción",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E3A8A),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Escanea el código QR proporcionado por tu docente para inscribirte en la materia",
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
                containerColor = Color(0xFF4CAF50)
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
                    text = "ℹ️ Instrucciones:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Solicita al docente el QR de inscripción\n• Escanea el código QR\n• Confirma tu inscripción\n• ¡Listo! Ya estarás inscrito en la materia",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ConfirmacionInscripcionStep(
    qrData: QRInscripcionData?,
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
        // Información de la materia
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Materia a inscribirse:",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = qrData?.materia?.nombre ?: "Materia",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Código: ${qrData?.materia?.codigo} - Grupo: ${qrData?.materia?.grupo}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }

        // Información importante
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Información",
                    tint = Color(0xFFF57C00),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Al confirmar, quedarás inscrito en esta materia y podrás registrar asistencias.",
                    fontSize = 12.sp,
                    color = Color(0xFF6D4C00)
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
                Text("Inscribiendo...")
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Confirmar"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirmar Inscripción")
            }
        }
    }
}

@Composable
private fun ExitoInscripcionStep(
    inscripcionExitosa: InscripcionQRResponse?,
    materiaNombre: String,
    onReiniciar: () -> Unit,
    onVolver: () -> Unit
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
            text = "¡Inscripción Exitosa!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = inscripcionExitosa?.message ?: "Te has inscrito exitosamente mediante código QR",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Materia:",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = materiaNombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E3A8A)
            )
        ) {
            Text("Volver al Inicio")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onReiniciar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Inscribirse en Otra Materia")
        }
    }
}

@Composable
private fun ErrorInscripcionStep(
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