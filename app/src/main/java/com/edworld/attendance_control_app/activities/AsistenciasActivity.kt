package com.edworld.attendance_control_app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
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

class AsistenciasActivity : ComponentActivity() {

    private var materias by mutableStateOf<List<Materia>>(emptyList())
    private var isLoading by mutableStateOf(true)

//    val url: String = "http://192.168.100.101:3000"
//    val url:String="http://172.20.10.3:300"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AsistenciasScreen(
                materias = materias,
                isLoading = isLoading,
                onNavigateBack = { finish() },
                onVerHorariosClick = { materia -> navigateToHorariosAsistencia(materia) },
                onVerAsistenciasClick = { materia -> navigateToVerAsistencias(materia) }, // NUEVA FUNCIÓN
                onLoadMaterias = { loadMaterias() }
            )
        }

        // Cargar materias al iniciar
        loadMaterias()
    }

    private fun loadMaterias() {
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
                        Toast.makeText(this@AsistenciasActivity, error, Toast.LENGTH_SHORT).show()
                        isLoading = false
                    }
                )
            } else {
                Toast.makeText(
                    this@AsistenciasActivity,
                    "Error: No se encontró ID del usuario",
                    Toast.LENGTH_SHORT
                ).show()
                isLoading = false
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

    private fun navigateToHorariosAsistencia(materia: Materia) {
        val intent = Intent(this, HorariosAsistenciaActivity::class.java)
        intent.putExtra("materia_id", materia.id)
        intent.putExtra("materia_nombre", materia.nombre)
        intent.putExtra("materia_codigo", materia.codigo)
        intent.putExtra("materia_grupo", materia.grupo)
        startActivity(intent)
    }

    // NUEVA FUNCIÓN para navegar a ver asistencias
    private fun navigateToVerAsistencias(materia: Materia) {
        val intent = Intent(this, VerAsistenciasMateriaActivity::class.java)
        intent.putExtra("materia_id", materia.id)
        intent.putExtra("materia_nombre", materia.nombre)
        intent.putExtra("materia_codigo", materia.codigo)
        intent.putExtra("materia_grupo", materia.grupo)
        startActivity(intent)
    }

    private suspend fun getUserId(): String? {
        return dataStore.data.first()[USER_ID_KEY]
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsistenciasScreen(
    materias: List<Materia> = emptyList(),
    isLoading: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onVerHorariosClick: (Materia) -> Unit = {},
    onVerAsistenciasClick: (Materia) -> Unit = {}, // NUEVA FUNCIÓN
    onLoadMaterias: () -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Control de Asistencias",
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
                    IconButton(onClick = onLoadMaterias) {
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
                        imageVector = Icons.Default.Assignment,
                        contentDescription = "Asistencias",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Generar Códigos QR",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Selecciona una materia para ver sus horarios o asistencias",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Barra de búsqueda
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
                    placeholder = { Text("Buscar materia...", color = Color.Gray) },
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

            // Contenido principal
            if (isLoading) {
                // Indicador de carga
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
                            text = "Cargando materias...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (materias.isEmpty()) {
                // Estado vacío
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = "Sin materias",
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay materias disponibles",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Crea materias primero para generar códigos QR",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Lista de materias
                val materiasFiltradas = materias.filter { materia ->
                    materia.nombre.contains(searchText, ignoreCase = true) ||
                            materia.codigo.contains(searchText, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(materiasFiltradas) { materia ->
                        MateriaAsistenciaCard(
                            materia = materia,
                            onVerHorariosClick = onVerHorariosClick,
                            onVerAsistenciasClick = onVerAsistenciasClick // PASAR LA NUEVA FUNCIÓN
                        )
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
private fun MateriaAsistenciaCard(
    materia: Materia,
    onVerHorariosClick: (Materia) -> Unit = {},
    onVerAsistenciasClick: (Materia) -> Unit = {} // NUEVA FUNCIÓN
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { println("Click en materia: ${materia.nombre}") },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Fila superior con icono e información
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ícono de materia
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color(0xFF1E3A8A).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = "Materia",
                        tint = Color(0xFF1E3A8A),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Información de la materia
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = materia.nombre,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Código: ${materia.codigo}    Grupo: ${materia.grupo}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Estado de la materia
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (materia.activo) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = if (materia.activo) "Activa" else "Inactiva",
                            tint = if (materia.activo) Color(0xFF4CAF50) else Color(0xFFDC2626),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (materia.activo) "Materia Activa" else "Materia Inactiva",
                            fontSize = 10.sp,
                            color = if (materia.activo) Color(0xFF4CAF50) else Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fila de botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón Ver Horarios
                Button(
                    onClick = { onVerHorariosClick(materia) },
                    enabled = materia.activo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        disabledContainerColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Ver horarios",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ver Horarios",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                // NUEVO BOTÓN Ver Asistencias
                Button(
                    onClick = { onVerAsistenciasClick(materia) },
                    enabled = materia.activo,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        disabledContainerColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Ver asistencias",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ver Asistencias",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}