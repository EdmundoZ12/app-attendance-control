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

class SeleccionarMateriaActivity : ComponentActivity() {

    private var materias by mutableStateOf<List<Materia>>(emptyList())
    private var isLoading by mutableStateOf(true)

    val url: String = "http://192.168.100.101:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SeleccionarMateriaScreen(
                materias = materias,
                isLoading = isLoading,
                onNavigateBack = { finish() },
                onInscribirClick = { materia -> navigateToInscribirEstudiante(materia) },
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
                        Toast.makeText(this@SeleccionarMateriaActivity, error, Toast.LENGTH_SHORT)
                            .show()
                        isLoading = false
                    }
                )
            } else {
                Toast.makeText(
                    this@SeleccionarMateriaActivity,
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
                val response = ApiClient.client.get("${url}/academic/materias") {
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

    private fun navigateToInscribirEstudiante(materia: Materia) {
        val intent = Intent(this, InscribirEstudianteActivity::class.java)
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
fun SeleccionarMateriaScreen(
    materias: List<Materia> = emptyList(),
    isLoading: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onInscribirClick: (Materia) -> Unit = {},
    onLoadMaterias: () -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Seleccionar Materia",
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
                .background(Color(0xFFF5F5F5))
        ) {
            // Línea separadora
            Divider(
                thickness = 1.dp,
                color = Color.Gray.copy(alpha = 0.3f)
            )

            // Barra de búsqueda
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                            imageVector = Icons.Default.MenuBook,
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
                            text = "Crea materias primero para poder inscribir estudiantes",
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
                        MateriaInscripcionCard(
                            materia = materia,
                            onInscribirClick = onInscribirClick
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
private fun MateriaInscripcionCard(
    materia: Materia,
    onInscribirClick: (Materia) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { println("Click en materia: ${materia.nombre}") },
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
                    imageVector = Icons.Default.MenuBook,
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

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Ubicación",
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Lat: ${
                            String.format(
                                "%.4f",
                                materia.latitud
                            )
                        }, Lng: ${String.format("%.4f", materia.longitud)}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                // Estado de la materia
                Spacer(modifier = Modifier.height(4.dp))
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
                        text = if (materia.activo) "Activa" else "Inactiva",
                        fontSize = 10.sp,
                        color = if (materia.activo) Color(0xFF4CAF50) else Color(0xFFDC2626),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Botón Inscribir
            Button(
                onClick = { onInscribirClick(materia) },
                enabled = materia.activo, // Solo habilitar si la materia está activa
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626),
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Inscribir",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}