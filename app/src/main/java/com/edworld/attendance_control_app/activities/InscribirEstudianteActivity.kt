package com.edworld.attendance_control_app.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.edworld.attendance_control_app.data.models.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch

class InscribirEstudianteActivity : ComponentActivity() {

    // Estados para la UI
    private var isLoading by mutableStateOf(false)
    private var isSearching by mutableStateOf(false)
    private var isInscribing by mutableStateOf(false)

    // Estados para la búsqueda de estudiante
    private var emailEstudiante by mutableStateOf("")
    private var estudianteEncontrado by mutableStateOf<EstudianteInfo?>(null)
    private var errorMessage by mutableStateOf("")

    // Estados para la lista de estudiantes inscritos
    private var estudiantesInscritos by mutableStateOf<List<EstudianteInfo>>(emptyList())
    private var isLoadingEstudiantes by mutableStateOf(false)
    private var totalEstudiantes by mutableStateOf(0)

    // Datos de la materia
    private var materiaId by mutableStateOf(0)
    private var materiaNombre by mutableStateOf("")
    private var materiaCodigo by mutableStateOf("")
    private var materiaGrupo by mutableStateOf("")

    val url: String = "http://192.168.100.101:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener datos de la materia del Intent
        materiaId = intent.getIntExtra("materia_id", 0)
        materiaNombre = intent.getStringExtra("materia_nombre") ?: ""
        materiaCodigo = intent.getStringExtra("materia_codigo") ?: ""
        materiaGrupo = intent.getStringExtra("materia_grupo") ?: ""

        setContent {
            InscribirEstudianteScreen(
                materiaNombre = materiaNombre,
                materiaCodigo = materiaCodigo,
                materiaGrupo = materiaGrupo,
                emailEstudiante = emailEstudiante,
                onEmailChange = {
                    emailEstudiante = it
                    errorMessage = ""
                    if (it.isEmpty()) {
                        estudianteEncontrado = null
                    }
                },
                estudianteEncontrado = estudianteEncontrado,
                isSearching = isSearching,
                isInscribing = isInscribing,
                errorMessage = errorMessage,
                onNavigateBack = { finish() },
                onBuscarEstudiante = { buscarEstudiante() },
                onInscribirEstudiante = { inscribirEstudiante() },
                // Parámetros para lista de estudiantes
                estudiantesInscritos = estudiantesInscritos,
                isLoadingEstudiantes = isLoadingEstudiantes,
                totalEstudiantes = totalEstudiantes,
                onLoadEstudiantes = { loadEstudiantesInscritos() }
            )
        }

        // Cargar estudiantes inscritos al iniciar
        loadEstudiantesInscritos()
    }

    private fun buscarEstudiante() {
        when {
            emailEstudiante.isEmpty() -> {
                errorMessage = "Por favor ingresa un email"
                return
            }

            !android.util.Patterns.EMAIL_ADDRESS.matcher(emailEstudiante).matches() -> {
                errorMessage = "Por favor ingresa un email válido"
                return
            }
        }

        isSearching = true
        errorMessage = ""
        estudianteEncontrado = null

        lifecycleScope.launch {
            try {
                val response = ApiClient.client.get("${url}/auth/student") {
                    parameter("email", emailEstudiante)
                }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val estudianteResponse: EstudianteResponse = response.body()
                        estudianteEncontrado = estudianteResponse.estudiante
                        isSearching = false
                        Toast.makeText(
                            this@InscribirEstudianteActivity,
                            "Estudiante encontrado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    HttpStatusCode.BadRequest -> {
                        val errorResponse = response.body<Map<String, String>>()
                        errorMessage = errorResponse["error"] ?: "Email inválido"
                        isSearching = false
                    }

                    HttpStatusCode.NotFound -> {
                        val errorResponse = response.body<Map<String, String>>()
                        errorMessage = errorResponse["error"] ?: "Estudiante no encontrado"
                        isSearching = false
                    }

                    HttpStatusCode.InternalServerError -> {
                        errorMessage = "Error interno del servidor"
                        isSearching = false
                    }

                    else -> {
                        errorMessage = "Error desconocido: ${response.status}"
                        isSearching = false
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.localizedMessage}"
                isSearching = false
            }
        }
    }

    private fun inscribirEstudiante() {
        if (estudianteEncontrado == null) {
            Toast.makeText(
                this,
                "Primero debe buscar y encontrar un estudiante",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        isInscribing = true
        errorMessage = ""

        lifecycleScope.launch {
            try {
                val request = InscribirEstudianteRequest(
                    materia_id = materiaId,
                    estudiante_id = estudianteEncontrado!!.id
                )

                val response = ApiClient.client.post("${url}/academic/asignacion") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                when (response.status) {
                    HttpStatusCode.Created -> {
                        val inscripcionResponse: InscripcionResponse = response.body()
                        Toast.makeText(
                            this@InscribirEstudianteActivity,
                            inscripcionResponse.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        isInscribing = false
                        // Recargar lista de estudiantes después de inscribir
                        loadEstudiantesInscritos()
                        // Limpiar formulario
                        emailEstudiante = ""
                        estudianteEncontrado = null
                    }

                    HttpStatusCode.BadRequest -> {
                        val errorResponse = response.body<Map<String, String>>()
                        errorMessage = errorResponse["error"]
                            ?: "No se puede asignar estudiantes a una materia inactiva"
                        isInscribing = false
                    }

                    HttpStatusCode.Conflict -> {
                        val errorResponse = response.body<Map<String, String>>()
                        errorMessage = errorResponse["error"]
                            ?: "El estudiante ya está inscrito en esta materia"
                        isInscribing = false
                    }

                    HttpStatusCode.InternalServerError -> {
                        errorMessage = "Error interno del servidor"
                        isInscribing = false
                    }

                    else -> {
                        errorMessage = "Error desconocido: ${response.status}"
                        isInscribing = false
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.localizedMessage}"
                isInscribing = false
            }
        }
    }

    private fun loadEstudiantesInscritos() {
        isLoadingEstudiantes = true

        lifecycleScope.launch {
            try {
                val request = EstudiantesInscritosRequest(materia_id = materiaId)

                val response = ApiClient.client.get("${url}/academic/asignacion/estudiantes") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val estudiantesResponse: EstudiantesInscritosResponse = response.body()
                        estudiantesInscritos = estudiantesResponse.estudiantes
                        totalEstudiantes = estudiantesResponse.total_estudiantes
                        isLoadingEstudiantes = false
                    }

                    HttpStatusCode.BadRequest -> {
                        Toast.makeText(
                            this@InscribirEstudianteActivity,
                            "ID de materia requerido",
                            Toast.LENGTH_SHORT
                        ).show()
                        isLoadingEstudiantes = false
                    }

                    HttpStatusCode.NotFound -> {
                        Toast.makeText(
                            this@InscribirEstudianteActivity,
                            "Materia no encontrada",
                            Toast.LENGTH_SHORT
                        ).show()
                        isLoadingEstudiantes = false
                    }

                    HttpStatusCode.InternalServerError -> {
                        Toast.makeText(
                            this@InscribirEstudianteActivity,
                            "Error interno del servidor",
                            Toast.LENGTH_SHORT
                        ).show()
                        isLoadingEstudiantes = false
                    }

                    else -> {
                        Toast.makeText(
                            this@InscribirEstudianteActivity,
                            "Error desconocido: ${response.status}",
                            Toast.LENGTH_SHORT
                        ).show()
                        isLoadingEstudiantes = false
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@InscribirEstudianteActivity,
                    "Error de conexión: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
                isLoadingEstudiantes = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InscribirEstudianteScreen(
    materiaNombre: String = "",
    materiaCodigo: String = "",
    materiaGrupo: String = "",
    emailEstudiante: String = "",
    onEmailChange: (String) -> Unit = {},
    estudianteEncontrado: EstudianteInfo? = null,
    isSearching: Boolean = false,
    isInscribing: Boolean = false,
    errorMessage: String = "",
    onNavigateBack: () -> Unit = {},
    onBuscarEstudiante: () -> Unit = {},
    onInscribirEstudiante: () -> Unit = {},
    // Parámetros para lista de estudiantes
    estudiantesInscritos: List<EstudianteInfo> = emptyList(),
    isLoadingEstudiantes: Boolean = false,
    totalEstudiantes: Int = 0,
    onLoadEstudiantes: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Inscribir Estudiante",
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
                InformacionMateria(
                    nombre = materiaNombre,
                    codigo = materiaCodigo,
                    grupo = materiaGrupo
                )
            }

            // Sección blanca - Búsqueda e inscripción de estudiante
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título
                Text(
                    text = "Buscar Estudiante",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )

                // Campo de email con botón buscar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Email del estudiante:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E3A8A),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = emailEstudiante,
                            onValueChange = onEmailChange,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            placeholder = { Text("ejemplo@universidad.edu", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Search
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1E3A8A),
                                unfocusedBorderColor = Color.Gray
                            ),
                            enabled = !isSearching && !isInscribing
                        )
                    }

                    // Botón buscar
                    Button(
                        onClick = onBuscarEstudiante,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E3A8A)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(56.dp),
                        enabled = !isSearching && !isInscribing && emailEstudiante.isNotEmpty()
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSearching) "Buscando..." else "Buscar",
                            fontSize = 12.sp
                        )
                    }
                }

                // Mensaje de error
                if (errorMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                color = Color(0xFFDC2626),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Información del estudiante encontrado
                estudianteEncontrado?.let { estudiante ->
                    EstudianteEncontradoCard(
                        estudiante = estudiante,
                        onInscribirClick = onInscribirEstudiante,
                        isInscribing = isInscribing
                    )
                }

                // Sección de estudiantes inscritos
                Spacer(modifier = Modifier.height(24.dp))

                EstudiantesInscritosSection(
                    estudiantesInscritos = estudiantesInscritos,
                    isLoadingEstudiantes = isLoadingEstudiantes,
                    totalEstudiantes = totalEstudiantes,
                    onLoadEstudiantes = onLoadEstudiantes
                )
            }
        }
    }
}

@Composable
private fun InformacionMateria(
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
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
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

@Composable
private fun EstudianteEncontradoCard(
    estudiante: EstudianteInfo,
    onInscribirClick: () -> Unit = {},
    isInscribing: Boolean = false
) {
    Column {
        Text(
            text = "Estudiante encontrado:",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E3A8A)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono del estudiante
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

                    Spacer(modifier = Modifier.width(12.dp))

                    // Información del estudiante
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${estudiante.nombre} ${estudiante.apellido}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = estudiante.email,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Carrera: ${estudiante.carrera}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón Inscribir
                Button(
                    onClick = onInscribirClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isInscribing
                ) {
                    if (isInscribing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Inscribiendo...",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Inscribir",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Inscribir Estudiante",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EstudiantesInscritosSection(
    estudiantesInscritos: List<EstudianteInfo> = emptyList(),
    isLoadingEstudiantes: Boolean = false,
    totalEstudiantes: Int = 0,
    onLoadEstudiantes: () -> Unit = {}
) {
    Column {
        // Título con contador y botón refrescar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Estudiantes Inscritos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )
                Text(
                    text = "Total: $totalEstudiantes estudiantes",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            IconButton(
                onClick = onLoadEstudiantes,
                enabled = !isLoadingEstudiantes
            ) {
                if (isLoadingEstudiantes) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF1E3A8A),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualizar lista",
                        tint = Color(0xFF1E3A8A)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Lista de estudiantes o estados especiales
        when {
            isLoadingEstudiantes -> {
                // Estado de carga
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
                            CircularProgressIndicator(
                                color = Color(0xFF1E3A8A)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Cargando estudiantes...",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            estudiantesInscritos.isEmpty() -> {
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
                                imageVector = Icons.Default.PersonOff,
                                contentDescription = "Sin estudiantes",
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No hay estudiantes inscritos",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Agrega estudiantes usando el formulario de arriba",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            else -> {
                // Lista de estudiantes
                estudiantesInscritos.forEach { estudiante ->
                    EstudianteInscritoCard(estudiante = estudiante)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun EstudianteInscritoCard(
    estudiante: EstudianteInfo
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícono del estudiante
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Estudiante",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información del estudiante
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${estudiante.nombre} ${estudiante.apellido}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = estudiante.email,
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = estudiante.carrera,
                    fontSize = 11.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium
                )
            }

            // Indicador de inscrito
            Box(
                modifier = Modifier
                    .background(
                        Color(0xFF4CAF50).copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Inscrito",
                    fontSize = 10.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}