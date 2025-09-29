package com.edworld.attendance_control_app.activities

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
import com.edworld.attendance_control_app.data.models.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class VerAsistenciasMateriaActivity : ComponentActivity() {

    // Estados para la UI
    private var isLoading by mutableStateOf(true)
    private var asistencias by mutableStateOf<List<AsistenciaMateria>>(emptyList())
    private var errorMessage by mutableStateOf("")

    // Datos de la materia recibidos por Intent
    private var materiaId by mutableStateOf(0)
    private var materiaNombre by mutableStateOf("")
    private var materiaCodigo by mutableStateOf("")
    private var materiaGrupo by mutableStateOf("")

//    val url: String = "http://192.168.100.101:3000"
//        val url:String="http://172.20.10.3:300"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener datos de la materia del Intent
        materiaId = intent.getIntExtra("materia_id", 0)
        materiaNombre = intent.getStringExtra("materia_nombre") ?: ""
        materiaCodigo = intent.getStringExtra("materia_codigo") ?: ""
        materiaGrupo = intent.getStringExtra("materia_grupo") ?: ""

        setContent {
            VerAsistenciasMateriaScreen(
                materiaNombre = materiaNombre,
                materiaCodigo = materiaCodigo,
                materiaGrupo = materiaGrupo,
                asistencias = asistencias,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onNavigateBack = { finish() },
                onLoadAsistencias = { loadAsistenciasMateria() }
            )
        }

        // Cargar asistencias al iniciar
        loadAsistenciasMateria()
    }

    private fun loadAsistenciasMateria() {
        lifecycleScope.launch {
            try {
                isLoading = true
                errorMessage = ""

                getAsistenciasByMateria(
                    materiaId = materiaId,
                    onSuccess = { asistenciasFromAPI ->
                        asistencias = asistenciasFromAPI
                        isLoading = false
                    },
                    onError = { error ->
                        Toast.makeText(
                            this@VerAsistenciasMateriaActivity,
                            error,
                            Toast.LENGTH_SHORT
                        ).show()
                        errorMessage = error
                        isLoading = false
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this@VerAsistenciasMateriaActivity,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
                errorMessage = e.localizedMessage ?: "Error desconocido"
                isLoading = false
            }
        }
    }

    private fun getAsistenciasByMateria(
        materiaId: Int,
        onSuccess: (List<AsistenciaMateria>) -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val request = GetAsistenciasMateriaRequest(materia_id = materiaId)

                val response = ApiClient.client.get("${Constants.BASE_URL}/attendance/asistencia/materia") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val asistenciaResponse: AsistenciasMateriaResponse = response.body()

                        // Convertir la respuesta al formato que necesitamos
                        val asistenciasFormateadas =
                            asistenciaResponse.asistencias.map { asistencia ->
                                AsistenciaMateria(
                                    id = asistencia.id,
                                    estudianteId = asistencia.estudiante_id,
                                    materiaId = asistencia.materia_id,
                                    fecha = asistencia.fecha,
                                    horaRegistro = formatearHoraRegistro(asistencia.hora_registro),
                                    ubicacionLat = asistencia.ubicacion_lat.toDoubleOrNull() ?: 0.0,
                                    ubicacionLng = asistencia.ubicacion_lng.toDoubleOrNull() ?: 0.0,
                                    nombreCompleto = "${asistencia.nombre} ${asistencia.apellido}",
                                    email = asistencia.email,
                                    carrera = asistencia.carrera
                                )
                            }

                        onSuccess(asistenciasFormateadas)
                    }

                    HttpStatusCode.BadRequest -> {
                        val errorResponse = response.body<Map<String, String>>()
                        onError(errorResponse["error"] ?: "Error en la solicitud")
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
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = inputFormat.parse(horaRegistro)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            // Si falla, intentar extraer solo la hora
            try {
                horaRegistro.substring(11, 16) // Extraer HH:mm
            } catch (e: Exception) {
                horaRegistro
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerAsistenciasMateriaScreen(
    materiaNombre: String = "",
    materiaCodigo: String = "",
    materiaGrupo: String = "",
    asistencias: List<AsistenciaMateria> = emptyList(),
    isLoading: Boolean = false,
    errorMessage: String = "",
    onNavigateBack: () -> Unit = {},
    onLoadAsistencias: () -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Asistencias - $materiaCodigo",
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
                actions = {
                    // Botón de actualizar
                    IconButton(onClick = onLoadAsistencias) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
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

            // Información de cabecera de la materia
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
                        imageVector = Icons.Default.Assignment,
                        contentDescription = "Asistencias",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = materiaNombre,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Código: $materiaCodigo - Grupo: $materiaGrupo",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Total asistencias: ${asistencias.size}",
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
                        placeholder = { Text("Buscar por estudiante...", color = Color.Gray) },
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
                // Estado vacío
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
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
                            text = "Los estudiantes aún no han registrado asistencias en esta materia",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Lista de asistencias con filtro
                val asistenciasFiltradas = asistencias.filter { asistencia ->
                    asistencia.nombreCompleto.contains(searchText, ignoreCase = true) ||
                            asistencia.email.contains(searchText, ignoreCase = true) ||
                            asistencia.carrera.contains(searchText, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(asistenciasFiltradas) { asistencia ->
                        AsistenciaMateriaCard(asistencia = asistencia)
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
private fun AsistenciaMateriaCard(
    asistencia: AsistenciaMateria
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
            // Ícono del estudiante
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
                    imageVector = Icons.Default.Person,
                    contentDescription = "Estudiante",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Información del estudiante y asistencia
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = asistencia.nombreCompleto,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = asistencia.email,
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = asistencia.carrera,
                    fontSize = 11.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium
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

            // Fecha y ubicación
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

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Ubicación",
                        tint = Color.Gray,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${
                            String.format(
                                "%.4f",
                                asistencia.ubicacionLat
                            )
                        }, ${String.format("%.4f", asistencia.ubicacionLng)}",
                        fontSize = 8.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// Función para formatear fecha
private fun formatearFecha(fecha: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        val date = inputFormat.parse(fecha)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        fecha
    }
}