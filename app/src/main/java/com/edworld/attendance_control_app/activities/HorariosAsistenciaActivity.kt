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
import androidx.lifecycle.lifecycleScope
import com.edworld.attendance_control_app.dataStore
import com.edworld.attendance_control_app.USER_ID_KEY
import com.edworld.attendance_control_app.data.models.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HorariosAsistenciaActivity : ComponentActivity() {

    // Estados para la UI
    private var isLoading by mutableStateOf(true)
    private var horarios by mutableStateOf<List<Horario>>(emptyList())

    // Datos de la materia
    private var materiaId by mutableStateOf(0)
    private var materiaNombre by mutableStateOf("")
    private var materiaCodigo by mutableStateOf("")
    private var materiaGrupo by mutableStateOf("")

//    val url: String = "http://192.168.100.101:3000"
    //    val url:String="http://172.20.10.3:300"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener datos de la materia del Intent
        materiaId = intent.getIntExtra("materia_id", 0)
        materiaNombre = intent.getStringExtra("materia_nombre") ?: ""
        materiaCodigo = intent.getStringExtra("materia_codigo") ?: ""
        materiaGrupo = intent.getStringExtra("materia_grupo") ?: ""

        setContent {
            HorariosAsistenciaScreen(
                materiaNombre = materiaNombre,
                materiaCodigo = materiaCodigo,
                materiaGrupo = materiaGrupo,
                horarios = horarios,
                isLoading = isLoading,
                onNavigateBack = { finish() },
                onGenerarQRClick = { horario -> navigateToGenerarQR(horario) },
                onLoadHorarios = { loadHorarios() }
            )
        }

        // Cargar horarios al iniciar
        loadHorarios()
    }

    private fun loadHorarios() {
        isLoading = true

        lifecycleScope.launch {
            try {
                val request = ObtenerHorariosRequest(materia_id = materiaId)

                val response = ApiClient.client.get("${Constants.BASE_URL}/academic/materias/horarios") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val horariosResponse: HorariosResponse = response.body()
                        horarios = horariosResponse.horarios
                        isLoading = false
                    }
                    HttpStatusCode.InternalServerError -> {
                        Toast.makeText(
                            this@HorariosAsistenciaActivity,
                            "Error interno del servidor",
                            Toast.LENGTH_SHORT
                        ).show()
                        isLoading = false
                    }
                    else -> {
                        Toast.makeText(
                            this@HorariosAsistenciaActivity,
                            "Error desconocido: ${response.status}",
                            Toast.LENGTH_SHORT
                        ).show()
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@HorariosAsistenciaActivity,
                    "Error de conexión: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
                isLoading = false
            }
        }
    }

    private fun navigateToGenerarQR(horario: Horario) {
        lifecycleScope.launch {
            val userId = getUserId()
            if (userId != null) {
                val intent = Intent(this@HorariosAsistenciaActivity, GenerarQRActivity::class.java)
                intent.putExtra("materia_id", materiaId)
                intent.putExtra("materia_nombre", materiaNombre)
                intent.putExtra("materia_codigo", materiaCodigo)
                intent.putExtra("materia_grupo", materiaGrupo)
                intent.putExtra("horario_id", horario.id)
                intent.putExtra("horario_dia", horario.dia_semana)
                intent.putExtra("horario_inicio", horario.hora_inicio)
                intent.putExtra("horario_fin", horario.hora_fin)
                intent.putExtra("docente_id", userId.toInt())
                startActivity(intent)
            } else {
                Toast.makeText(
                    this@HorariosAsistenciaActivity,
                    "Error: No se encontró ID del usuario",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun getUserId(): String? {
        return dataStore.data.first()[USER_ID_KEY]
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorariosAsistenciaScreen(
    materiaNombre: String = "",
    materiaCodigo: String = "",
    materiaGrupo: String = "",
    horarios: List<Horario> = emptyList(),
    isLoading: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onGenerarQRClick: (Horario) -> Unit = {},
    onLoadHorarios: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Horarios de Clase",
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
                        IconButton(onClick = onLoadHorarios) {
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

                // Línea divisoria
                Divider(
                    thickness = 1.dp,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Sección azul - Información de la materia
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E3A8A))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InformacionMateriaHorarios(
                    nombre = materiaNombre,
                    codigo = materiaCodigo,
                    grupo = materiaGrupo
                )
            }

            // Sección blanca - Lista de horarios
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título con información
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Horarios Programados",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                        Text(
                            text = "Selecciona un horario para generar QR",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    // Día actual
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF))
                    ) {
                        Text(
                            text = getCurrentDayName(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Contenido principal
                if (isLoading) {
                    // Estado de carga
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF1E3A8A)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Cargando horarios...",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else if (horarios.isEmpty()) {
                    // Estado vacío
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Sin horarios",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No hay horarios configurados",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Configura horarios en la materia primero",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Lista de horarios
                    horarios.forEach { horario ->
                        HorarioAsistenciaCard(
                            horario = horario,
                            isToday = isToday(horario.dia_semana),
                            onGenerarQRClick = onGenerarQRClick
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun InformacionMateriaHorarios(
    nombre: String,
    codigo: String,
    grupo: String
) {
    Column {
        Text(
            text = "Materia seleccionada:",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "QR",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = nombre,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Código: $codigo    Grupo: $grupo",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HorarioAsistenciaCard(
    horario: Horario,
    isToday: Boolean,
    onGenerarQRClick: (Horario) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) Color(0xFFF0F8FF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Información del horario
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de día
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (isToday) Color(0xFF4CAF50) else Color(0xFF1E3A8A),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = horario.dia_semana.take(3).uppercase(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = horario.dia_semana.replaceFirstChar { it.uppercase() },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) Color(0xFF4CAF50) else Color(0xFF1E3A8A)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${horario.hora_inicio} - ${horario.hora_fin}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    if (isToday) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "• Clase de hoy",
                            fontSize = 10.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Botón Generar QR
            Button(
                onClick = { onGenerarQRClick(horario) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isToday) Color(0xFF4CAF50) else Color(0xFFDC2626)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "Generar QR",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isToday) "Generar QR" else "Generar QR",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Función para obtener el día actual en español
private fun getCurrentDayName(): String {
    val calendar = Calendar.getInstance()
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

    return when (dayOfWeek) {
        Calendar.MONDAY -> "Hoy: Lunes"
        Calendar.TUESDAY -> "Hoy: Martes"
        Calendar.WEDNESDAY -> "Hoy: Miércoles"
        Calendar.THURSDAY -> "Hoy: Jueves"
        Calendar.FRIDAY -> "Hoy: Viernes"
        Calendar.SATURDAY -> "Hoy: Sábado"
        Calendar.SUNDAY -> "Hoy: Domingo"
        else -> "Hoy"
    }
}

// Función para verificar si el horario es hoy
private fun isToday(diaSemana: String): Boolean {
    val calendar = Calendar.getInstance()
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

    val today = when (dayOfWeek) {
        Calendar.MONDAY -> "lunes"
        Calendar.TUESDAY -> "martes"
        Calendar.WEDNESDAY -> "miercoles"
        Calendar.THURSDAY -> "jueves"
        Calendar.FRIDAY -> "viernes"
        Calendar.SATURDAY -> "sabado"
        Calendar.SUNDAY -> "domingo"
        else -> ""
    }

    return diaSemana.lowercase() == today
}