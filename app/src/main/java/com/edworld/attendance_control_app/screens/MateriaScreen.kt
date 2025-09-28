package com.edworld.attendance_control_app.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.edworld.attendance_control_app.data.models.HorarioItem
import com.edworld.attendance_control_app.data.models.Materia
import com.edworld.attendance_control_app.data.models.Horario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateriaScreen(
    isEditMode: Boolean = false,
    materiaId: Int? = null,
    isLoading: Boolean = false,
    currentLatitud: Double = 0.0,
    currentLongitud: Double = 0.0,
    isLocationLoading: Boolean = false,
    datosMateriaCargados: Materia? = null,
    horariosCargados: List<Horario> = emptyList(),
    onNavigateBack: () -> Unit = {},
    onLocationUpdate: (Double, Double) -> Unit = { _, _ -> },
    onGuardarMateria: (String, String, String, String, List<HorarioItem>, Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onAgregarHorario: (String, String, String, (Horario) -> Unit, (String) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onActualizarHorario: (Int, String, String, String, (Horario) -> Unit, (String) -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onEliminarHorario: (Int, () -> Unit, (String) -> Unit) -> Unit = { _, _, _ -> },
) {
    val context = LocalContext.current

    // Estados para los campos del formulario
    var nombre by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var grupo by remember { mutableStateOf("") }
    // Estado para el switch de activo
    var estadoActivo by remember { mutableStateOf(true) }

    // Estados para GPS
    var selectedLatitud by remember { mutableStateOf(0.0) }
    var selectedLongitud by remember { mutableStateOf(0.0) }

    // Estado para horarios (diferente según modo)
    var horarios by remember {
        mutableStateOf(if (isEditMode) emptyList<HorarioItem>() else listOf<HorarioItem>())
    }

    // Pre-llenar datos cuando se cargan en modo edición
    LaunchedEffect(datosMateriaCargados) {
        datosMateriaCargados?.let { materia ->
            nombre = materia.nombre
            codigo = materia.codigo
            descripcion = materia.descripcion ?: ""
            grupo = materia.grupo
            selectedLatitud = materia.latitud
            selectedLongitud = materia.longitud
            estadoActivo = materia.activo
        }
    }

    // Actualizar coordenadas seleccionadas cuando cambien las actuales (solo si no es modo edición)
    LaunchedEffect(currentLatitud, currentLongitud) {
        if (!isEditMode && currentLatitud != 0.0 && currentLongitud != 0.0) {
            selectedLatitud = currentLatitud
            selectedLongitud = currentLongitud
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isEditMode) "Editar Materia" else "Crear Materia",
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
                        // Solo el botón Guardar
                        Button(
                            onClick = {
                                // Validaciones
                                when {
                                    nombre.isBlank() -> {
                                        Toast.makeText(
                                            context,
                                            "El nombre es requerido",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    codigo.isBlank() -> {
                                        Toast.makeText(
                                            context,
                                            "El código es requerido",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    grupo.isBlank() -> {
                                        Toast.makeText(
                                            context,
                                            "El grupo es requerido",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    selectedLatitud == 0.0 && selectedLongitud == 0.0 -> {
                                        Toast.makeText(
                                            context,
                                            "Debe seleccionar una ubicación GPS",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    !isEditMode && horarios.isEmpty() -> {
                                        Toast.makeText(
                                            context,
                                            "Debe agregar al menos un horario",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    else -> {
                                        // Todo está correcto, guardar materia
                                        onGuardarMateria(
                                            nombre,
                                            codigo,
                                            descripcion,
                                            grupo,
                                            horarios,
                                            estadoActivo
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Guardar",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isEditMode) "Guardar Cambios" else "Guardar",
                                fontSize = 12.sp
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
                .verticalScroll(rememberScrollState())
        ) {
            // Sección azul - Formulario básico
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E3A8A))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FormularioBasico(
                    nombre = nombre,
                    onNombreChange = { nombre = it },
                    codigo = codigo,
                    onCodigoChange = { codigo = it },
                    descripcion = descripcion,
                    onDescripcionChange = { descripcion = it },
                    grupo = grupo,
                    onGrupoChange = { grupo = it },
                    isEditMode = isEditMode,
                    estadoActivo = estadoActivo,
                    onEstadoActivoChange = { estadoActivo = it }
                )
            }

            // Sección blanca - GPS y Horarios
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GPSSection(
                    latitud = selectedLatitud,
                    longitud = selectedLongitud,
                    isLoading = isLocationLoading,
                    onLocationUpdate = { lat, lng ->
                        selectedLatitud = lat
                        selectedLongitud = lng
                        onLocationUpdate(lat, lng)
                    }
                )

                if (isEditMode) {
                    SeccionHorariosEdicion(
                        materiaId = materiaId,
                        horariosCargados = horariosCargados,
                        onAgregarHorario = onAgregarHorario,
                        onActualizarHorario = onActualizarHorario,
                        onEliminarHorario = onEliminarHorario
                    )
                } else {
                    SeccionHorarios(
                        isEditMode = isEditMode,
                        horarios = horarios,
                        onHorariosChange = { newHorarios ->
                            horarios = newHorarios
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeccionHorariosEdicion(
    materiaId: Int?,
    horariosCargados: List<Horario>,
    onAgregarHorario: (String, String, String, (Horario) -> Unit, (String) -> Unit) -> Unit,
    onActualizarHorario: (Int, String, String, String, (Horario) -> Unit, (String) -> Unit) -> Unit,
    onEliminarHorario: (Int, () -> Unit, (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var selectedDia by remember { mutableStateOf("") }
    var selectedHoraInicio by remember { mutableStateOf("") }
    var selectedHoraFin by remember { mutableStateOf("") }
    var expandedDia by remember { mutableStateOf(false) }
    var showTimePickerInicio by remember { mutableStateOf(false) }
    var showTimePickerFin by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var horarioAEditar by remember { mutableStateOf<Horario?>(null) }

    val diasSemana =
        listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    Column {
        // Título con botón de agregar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Horarios",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Button(
                onClick = {
                    if (selectedDia.isNotEmpty() && selectedHoraInicio.isNotEmpty() && selectedHoraFin.isNotEmpty()) {
                        onAgregarHorario(
                            selectedDia,
                            selectedHoraInicio,
                            selectedHoraFin,
                            { horario ->
                                // Limpiar campos después de agregar
                                selectedDia = ""
                                selectedHoraInicio = ""
                                selectedHoraFin = ""
                                Toast.makeText(context, "Horario agregado", Toast.LENGTH_SHORT)
                                    .show()
                            },
                            { error ->
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        Toast.makeText(context, "Complete todos los campos", Toast.LENGTH_SHORT)
                            .show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar horario",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fila de campos para agregar horario
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Dropdown para Día
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Día:",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedDia,
                    onExpandedChange = { expandedDia = it }
                ) {
                    OutlinedTextField(
                        value = selectedDia,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDia)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDia,
                        onDismissRequest = { expandedDia = false }
                    ) {
                        diasSemana.forEach { dia ->
                            DropdownMenuItem(
                                text = { Text(dia, fontSize = 14.sp) },
                                onClick = {
                                    selectedDia = dia
                                    expandedDia = false
                                }
                            )
                        }
                    }
                }
            }

            // Campo Hora Inicio
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hora Inicio:",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = selectedHoraInicio,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePickerInicio = true },
                    shape = RoundedCornerShape(8.dp),
                    placeholder = { Text("00:00", fontSize = 12.sp) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Seleccionar hora",
                            modifier = Modifier.clickable { showTimePickerInicio = true }
                        )
                    },
                    singleLine = true
                )
            }

            // Campo Hora Fin
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hora Fin:",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = selectedHoraFin,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePickerFin = true },
                    shape = RoundedCornerShape(8.dp),
                    placeholder = { Text("00:00", fontSize = 12.sp) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Seleccionar hora",
                            modifier = Modifier.clickable { showTimePickerFin = true }
                        )
                    },
                    singleLine = true
                )
            }
        }

        // Time Picker Dialogs
        if (showTimePickerInicio) {
            TimePickerDialog(
                onTimeSelected = { hora, minuto ->
                    selectedHoraInicio = String.format("%02d:%02d", hora, minuto)
                    showTimePickerInicio = false
                },
                onDismiss = { showTimePickerInicio = false }
            )
        }

        if (showTimePickerFin) {
            TimePickerDialog(
                onTimeSelected = { hora, minuto ->
                    selectedHoraFin = String.format("%02d:%02d", hora, minuto)
                    showTimePickerFin = false
                },
                onDismiss = { showTimePickerFin = false }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de horarios cargados
        horariosCargados.forEach { horario ->
            HorarioCardEdicion(
                horario = horario,
                onEditClick = {
                    horarioAEditar = horario
                    showEditDialog = true
                },
                onDeleteClick = {
                    onEliminarHorario(
                        horario.id,
                        {
                            Toast.makeText(context, "Horario eliminado", Toast.LENGTH_SHORT)
                                .show()
                        },
                        { error ->
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Dialog para editar horario
        if (showEditDialog && horarioAEditar != null) {
            EditarHorarioDialog(
                horario = horarioAEditar!!,
                onDismiss = {
                    showEditDialog = false
                    horarioAEditar = null
                },
                onConfirm = { horarioId, dia, horaInicio, horaFin ->
                    onActualizarHorario(
                        horarioId,
                        dia,
                        horaInicio,
                        horaFin,
                        { horario ->
                            showEditDialog = false
                            horarioAEditar = null
                            Toast.makeText(context, "Horario actualizado", Toast.LENGTH_SHORT)
                                .show()
                        },
                        { error ->
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun HorarioCardEdicion(
    horario: Horario,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Día",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = horario.dia_semana.replaceFirstChar { it.uppercase() },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Hora Inicio",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = horario.hora_inicio,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Hora Fin",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = horario.hora_fin,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = Color(0xFF1E3A8A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditarHorarioDialog(
    horario: Horario,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String, String) -> Unit
) {
    var selectedDia by remember { mutableStateOf(horario.dia_semana.replaceFirstChar { it.uppercase() }) }
    var selectedHoraInicio by remember { mutableStateOf(horario.hora_inicio) }
    var selectedHoraFin by remember { mutableStateOf(horario.hora_fin) }
    var expandedDia by remember { mutableStateOf(false) }
    var showTimePickerInicio by remember { mutableStateOf(false) }
    var showTimePickerFin by remember { mutableStateOf(false) }

    val diasSemana =
        listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Editar Horario",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Dropdown para Día
                Column {
                    Text(
                        text = "Día:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedDia,
                        onExpandedChange = { expandedDia = it }
                    ) {
                        OutlinedTextField(
                            value = selectedDia,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDia)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDia,
                            onDismissRequest = { expandedDia = false }
                        ) {
                            diasSemana.forEach { dia ->
                                DropdownMenuItem(
                                    text = { Text(dia) },
                                    onClick = {
                                        selectedDia = dia
                                        expandedDia = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Campo Hora Inicio
                Column {
                    Text(
                        text = "Hora Inicio:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = selectedHoraInicio,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePickerInicio = true },
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Seleccionar hora",
                                modifier = Modifier.clickable { showTimePickerInicio = true }
                            )
                        }
                    )
                }

                // Campo Hora Fin
                Column {
                    Text(
                        text = "Hora Fin:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = selectedHoraFin,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePickerFin = true },
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Seleccionar hora",
                                modifier = Modifier.clickable { showTimePickerFin = true }
                            )
                        }
                    )
                }

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            onConfirm(
                                horario.id,
                                selectedDia,
                                selectedHoraInicio,
                                selectedHoraFin
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E3A8A)
                        )
                    ) {
                        Text("Actualizar")
                    }
                }
            }
        }
    }

    // Time Picker Dialogs
    if (showTimePickerInicio) {
        TimePickerDialog(
            onTimeSelected = { hora, minuto ->
                selectedHoraInicio = String.format("%02d:%02d", hora, minuto)
                showTimePickerInicio = false
            },
            onDismiss = { showTimePickerInicio = false }
        )
    }

    if (showTimePickerFin) {
        TimePickerDialog(
            onTimeSelected = { hora, minuto ->
                selectedHoraFin = String.format("%02d:%02d", hora, minuto)
                showTimePickerFin = false
            },
            onDismiss = { showTimePickerFin = false }
        )
    }
}

// Resto de funciones iguales (FormularioBasico, SeccionHorarios, TimePickerDialog, HorarioCard)
@Composable
private fun FormularioBasico(
    nombre: String,
    onNombreChange: (String) -> Unit,
    codigo: String,
    onCodigoChange: (String) -> Unit,
    descripcion: String,
    onDescripcionChange: (String) -> Unit,
    grupo: String,
    onGrupoChange: (String) -> Unit,
    isEditMode: Boolean = false,
    estadoActivo: Boolean = true,
    onEstadoActivoChange: (Boolean) -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Campo Nombre
        Text(
            text = "Nombre:",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = nombre,
            onValueChange = onNombreChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color.Gray,
                unfocusedBorderColor = Color.Gray
            )
        )

        // Fila con Código y Grupo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Código:",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = codigo,
                    onValueChange = onCodigoChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Grupo:",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = grupo,
                    onValueChange = onGrupoChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }
        }

        // Campo Descripción
        Text(
            text = "Descripción:",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = descripcion,
            onValueChange = onDescripcionChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(8.dp),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color.Gray,
                unfocusedBorderColor = Color.Gray
            )
        )

        // Switch para estado activo (solo en modo edición)
        if (isEditMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Materia activa:",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Switch(
                    checked = estadoActivo,
                    onCheckedChange = onEstadoActivoChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.Green,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Red
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeccionHorarios(
    isEditMode: Boolean,
    horarios: List<HorarioItem>,
    onHorariosChange: (List<HorarioItem>) -> Unit
) {
    var selectedDia by remember { mutableStateOf("") }
    var selectedHoraInicio by remember { mutableStateOf("") }
    var selectedHoraFin by remember { mutableStateOf("") }
    var expandedDia by remember { mutableStateOf(false) }
    var showTimePickerInicio by remember { mutableStateOf(false) }
    var showTimePickerFin by remember { mutableStateOf(false) }

    val diasSemana =
        listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    Column {
        // Título con botón de agregar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Horarios",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            // Botón + al lado del título
            Button(
                onClick = {
                    if (selectedDia.isNotEmpty() && selectedHoraInicio.isNotEmpty() && selectedHoraFin.isNotEmpty()) {
                        val nuevoHorario = HorarioItem(
                            id = (horarios.maxOfOrNull { it.id } ?: 0) + 1,
                            dia = selectedDia,
                            horaInicio = selectedHoraInicio,
                            horaFin = selectedHoraFin
                        )
                        onHorariosChange(horarios + nuevoHorario)

                        // Limpiar campos
                        selectedDia = ""
                        selectedHoraInicio = ""
                        selectedHoraFin = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black
                ),
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar horario",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fila de campos para agregar horario
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Dropdown para Día
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Día:",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedDia,
                    onExpandedChange = { expandedDia = it }
                ) {
                    OutlinedTextField(
                        value = selectedDia,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDia)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDia,
                        onDismissRequest = { expandedDia = false }
                    ) {
                        diasSemana.forEach { dia ->
                            DropdownMenuItem(
                                text = { Text(dia, fontSize = 14.sp) },
                                onClick = {
                                    selectedDia = dia
                                    expandedDia = false
                                }
                            )
                        }
                    }
                }
            }

            // Campo Hora Inicio con Time Picker
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hora Inicio:",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = selectedHoraInicio,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePickerInicio = true },
                    shape = RoundedCornerShape(8.dp),
                    placeholder = { Text("00:00", fontSize = 12.sp) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Seleccionar hora",
                            modifier = Modifier.clickable { showTimePickerInicio = true }
                        )
                    },
                    singleLine = true
                )
            }

            // Campo Hora Fin con Time Picker
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hora Fin:",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = selectedHoraFin,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePickerFin = true },
                    shape = RoundedCornerShape(8.dp),
                    placeholder = { Text("00:00", fontSize = 12.sp) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Seleccionar hora",
                            modifier = Modifier.clickable { showTimePickerFin = true }
                        )
                    },
                    singleLine = true
                )
            }
        }

        // Time Picker Dialogs
        if (showTimePickerInicio) {
            TimePickerDialog(
                onTimeSelected = { hora, minuto ->
                    selectedHoraInicio = String.format("%02d:%02d", hora, minuto)
                    showTimePickerInicio = false
                },
                onDismiss = { showTimePickerInicio = false }
            )
        }

        if (showTimePickerFin) {
            TimePickerDialog(
                onTimeSelected = { hora, minuto ->
                    selectedHoraFin = String.format("%02d:%02d", hora, minuto)
                    showTimePickerFin = false
                },
                onDismiss = { showTimePickerFin = false }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista dinámica de horarios usando el componente
        horarios.forEach { horario ->
            HorarioCard(
                dia = horario.dia,
                horaInicio = horario.horaInicio,
                horaFin = horario.horaFin,
                onEditClick = {
                    // TODO: Implementar edición
                },
                onDeleteClick = {
                    onHorariosChange(horarios.filter { it.id != horario.id })
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(timePickerState.hour, timePickerState.minute)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}

@Composable
private fun HorarioCard(
    dia: String,
    horaInicio: String,
    horaFin: String,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Día",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = dia,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Hora Inicio",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = horaInicio,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Hora Fin",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = horaFin,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = Color(0xFF1E3A8A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}