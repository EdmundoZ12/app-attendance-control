package com.edworld.attendance_control_app.screens

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

data class MateriaEstudiante(
    val id: Int,
    val nombre: String,
    val codigo: String,
    val grupo: String,
    val ubicacionGPS: String,
    val proximaClase: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeEstudianteScreen(
    onUnirseClaseClick: () -> Unit = {},
    onMarcarAsistenciaClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    // Lista de materias del estudiante
    val materias = remember {
        listOf(
            MateriaEstudiante(
                1,
                "Introduccion a la Informatica",
                "INF123",
                "SA",
                "UBICACION GPS",
                "Hoy 14:00"
            ),
            MateriaEstudiante(
                2,
                "Introduccion a la Informatica",
                "INF123",
                "SA",
                "UBICACION GPS",
                "Mañana 10:00"
            ),
            MateriaEstudiante(3, "Introduccion a la Informatica", "INF123", "SA", "UBICACION GPS"),
            MateriaEstudiante(4, "Introduccion a la Informatica", "INF123", "SA", "UBICACION GPS"),
            MateriaEstudiante(5, "Introduccion a la Informatica", "INF123", "SA", "UBICACION GPS")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Materias",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = Color(0xFF1E3A8A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            BottomNavigationEstudiante(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onUnirseClaseClick = onUnirseClaseClick,
                onMarcarAsistenciaClick = onMarcarAsistenciaClick
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

            // Campo de búsqueda centrado
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(0.8f),
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

            // Lista de materias
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(materias.filter {
                    it.nombre.contains(searchText, ignoreCase = true) ||
                            it.codigo.contains(searchText, ignoreCase = true)
                }) { materia ->
                    MateriaEstudianteCard(materia = materia)
                }

                // Espacio adicional al final
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MateriaEstudianteCard(materia: MateriaEstudiante) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { println("Ver detalles: ${materia.nombre}") },
        shape = RoundedCornerShape(16.dp),
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
                    text = "Codigo: ${materia.codigo}    Grupo: ${materia.grupo}",
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
                        text = materia.ubicacionGPS,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                // Próxima clase (si existe)
                materia.proximaClase?.let { proximaClase ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Próxima clase: $proximaClase",
                        fontSize = 10.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Botón Ver
            Button(
                onClick = { println("Ver materia ${materia.id}") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E3A8A)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Ver",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationEstudiante(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onUnirseClaseClick: () -> Unit = {},
    onMarcarAsistenciaClick: () -> Unit = {}
) {
    val tabs = listOf(
        Triple(Icons.Default.Book, "Materias", Color(0xFF1E3A8A)),
        Triple(Icons.Default.PersonAdd, "Inscribirse", Color.Gray),
        Triple(Icons.Default.Assignment, "Reg. Asistencia", Color.Gray),
        Triple(Icons.Default.Person, "Perfil", Color.Gray)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, (icon, label, color) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        when (index) {
                            1 -> onUnirseClaseClick() // Inscribirse (QR)
                            2 -> onMarcarAsistenciaClick() // Registrar Asistencia
                            else -> onTabSelected(index)
                        }
                    }
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selectedTab == index) Color(0xFF1E3A8A) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = if (selectedTab == index) Color(0xFF1E3A8A) else Color.Gray,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}