package com.edworld.attendance_control_app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.lifecycleScope
import com.edworld.attendance_control_app.MainActivity
import com.edworld.attendance_control_app.clearToken
import com.edworld.attendance_control_app.dataStore
import com.edworld.attendance_control_app.USER_ID_KEY
import com.edworld.attendance_control_app.data.models.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HistorialAsistenciasActivity : ComponentActivity() {

    // Estados para la UI
    private var isLoading by mutableStateOf(true)
    private var asistencias by mutableStateOf<List<AsistenciaHistorial>>(emptyList())
    private var errorMessage by mutableStateOf("")
    private var nombreEstudiante by mutableStateOf("Estudiante")

    val url: String = "http://192.168.100.101:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HistorialAsistenciasScreen(
                nombreEstudiante = nombreEstudiante,
                asistencias = asistencias,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onRegistrarAsistencia = { navigateToRegistrarAsistencia() },
                onLogout = { logout() },
                onLoadAsistencias = { loadHistorialAsistencias() }
            )
        }

        // Cargar datos al iniciar
        loadUserData()
        loadHistorialAsistencias()
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            try {
                val userId = getUserId()
                if (userId != null) {
                    // Aquí podrías hacer una llamada al API para obtener el nombre del estudiante
                    // Por ahora usamos un valor por defecto
                    nombreEstudiante = "Estudiante"
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HistorialAsistenciasActivity,
                    "Error al cargar datos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadHistorialAsistencias() {
        lifecycleScope.launch {
            val userId = getUserId()
            if (userId != null) {
                getAsistenciasByEstudiante(
                    estudianteId = userId.toInt(),
                    onSuccess = { asistenciasFromAPI ->
                        asistencias = asistenciasFromAPI
                        isLoading = false
                        errorMessage = ""
                    },
                    onError = { error ->
                        Toast.makeText(this@HistorialAsistenciasActivity, error, Toast.LENGTH_SHORT)
                            .show()
                        errorMessage = error
                        isLoading = false
                    }
                )
            } else {
                Toast.makeText(
                    this@HistorialAsistenciasActivity,
                    "Error: No se encontró ID del usuario",
                    Toast.LENGTH_SHORT
                ).show()
                errorMessage = "Error: No se encontró ID del usuario"
                isLoading = false
            }
        }
    }

    private fun getAsistenciasByEstudiante(
        estudianteId: Int,
        onSuccess: (List<AsistenciaHistorial>) -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val request = GetAsistenciasEstudianteRequest(estudiante_id = estudianteId)

                val response = ApiClient.client.get("${url}/attendance/asistencia/estudiante") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val asistenciaResponse: AsistenciasEstudianteResponse = response.body()

                        // Convertir la respuesta al formato que necesitamos
                        val asistenciasFormateadas =
                            asistenciaResponse.asistencias.map { asistencia ->
                                AsistenciaHistorial(
                                    id = asistencia.id,
                                    estudianteId = asistencia.estudiante_id,
                                    materiaId = asistencia.materia_id,
                                    materiaNombre = asistencia.materia_nombre,
                                    materiaCodigo = asistencia.materia_codigo,
                                    fecha = asistencia.fecha,
                                    horaRegistro = formatearHoraRegistro(asistencia.hora_registro),
                                    ubicacionLat = asistencia.ubicacion_lat,
                                    ubicacionLng = asistencia.ubicacion_lng
                                )
                            }

                        onSuccess(asistenciasFormateadas)
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

    private fun formatearHoraRegistro(horaRegistro: String): String {
        return try {
            // Convertir de "2025-01-15T09:15:30.000Z" a "09:15"
            val inputFormat = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                java.util.Locale.getDefault()
            )
            val outputFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val date = inputFormat.parse(horaRegistro)
            outputFormat.format(date ?: java.util.Date())
        } catch (e: Exception) {
            // Si falla, intentar extraer solo la hora
            try {
                horaRegistro.substring(11, 16) // Extraer HH:mm
            } catch (e: Exception) {
                horaRegistro
            }
        }
    }

    private fun navigateToRegistrarAsistencia() {
        val intent = Intent(this, RegistrarAsistenciaActivity::class.java)
        startActivity(intent)
    }

    private fun logout() {
        lifecycleScope.launch {
            clearToken(this@HistorialAsistenciasActivity)
            val intent = Intent(this@HistorialAsistenciasActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private suspend fun getUserId(): String? {
        return dataStore.data.first()[USER_ID_KEY]
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialAsistenciasScreen(
    nombreEstudiante: String = "Estudiante",
    asistencias: List<AsistenciaHistorial> = emptyList(),
    isLoading: Boolean = false,
    errorMessage: String = "",
    onRegistrarAsistencia: () -> Unit = {},
    onLogout: () -> Unit = {},
    onLoadAsistencias: () -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "¡Hola, $nombreEstudiante!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Mis Asistencias",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    // Botón de actualizar
                    IconButton(onClick = onLoadAsistencias) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = Color.White
                        )
                    }
                    // Botón principal: Registrar Asistencia
                    Button(
                        onClick = onRegistrarAsistencia,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Registrar",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Registrar",
                            fontSize = 12.sp
                        )
                    }
                    // Botón logout
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar sesión",
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
                .background(Color(0xFFF5F5F5))
        ) {
            // Línea separadora
            Divider(
                thickness = 1.dp,
                color = Color.Gray.copy(alpha = 0.3f)
            )

            // Información de cabecera
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Asistencias",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Historial de Asistencias",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Total registradas: ${asistencias.size}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Barra de búsqueda
            if (asistencias.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.fillMaxWidth(0.9f),
                        placeholder = { Text("Buscar por materia...", color = Color.Gray) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = Color.Gray
                            )
                        },
                        shape = RoundedCornerShape(25.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E3A8A),
                            unfocusedBorderColor = Color.Gray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Contenido principal
            if (isLoading) {
                // Estado de carga
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1E3A8A)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Cargando asistencias...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (errorMessage.isNotEmpty()) {
                // Estado de error
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Error al cargar asistencias",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onLoadAsistencias,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E3A8A)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reintentar"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reintentar")
                        }
                    }
                }
            } else if (asistencias.isEmpty()) {
                // Estado vacío con llamada a la acción
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Sin asistencias",
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay asistencias registradas",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Usa el botón 'Registrar' para marcar tu primera asistencia",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        // Botón llamativo para primera asistencia
                        Button(
                            onClick = onRegistrarAsistencia,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Registrar Primera Asistencia",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Registrar Primera Asistencia",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                // Lista de asistencias con filtro
                val asistenciasFiltradas = asistencias.filter { asistencia ->
                    asistencia.materiaNombre.contains(searchText, ignoreCase = true) ||
                            asistencia.materiaCodigo.contains(searchText, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(asistenciasFiltradas) { asistencia ->
                        AsistenciaHistorialCard(asistencia = asistencia)
                    }

                    // Espacio adicional al final
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AsistenciaHistorialCard(
    asistencia: AsistenciaHistorial
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícono de estado
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Presente",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Información de la asistencia
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = asistencia.materiaNombre,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Código: ${asistencia.materiaCodigo}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Hora",
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Registrado a las ${asistencia.horaRegistro}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            // Fecha y estado
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatearFecha(asistencia.fecha),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E3A8A)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .background(
                            Color(0xFF4CAF50).copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Presente",
                        fontSize = 10.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Función para formatear fecha
private fun formatearFecha(fecha: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())
        val date = inputFormat.parse(fecha)
        outputFormat.format(date ?: java.util.Date())
    } catch (e: Exception) {
        fecha
    }
}