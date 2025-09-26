package com.edworld.attendance_control_app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterStudent: (String, String, String, String, String, () -> Unit, (String) -> Unit) -> Unit = { _, _, _, _, _, _, _ -> },
    onRegisterTeacher: (String, String, String, String, String, () -> Unit, (String) -> Unit) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    var selectedTab by remember { mutableStateOf("Estudiante") }
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var carreraTitulo by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A),
                        Color(0xFF1E3A8A),
                        Color(0xFFDC2626),
                        Color(0xFFDC2626)
                    )
                )
            )
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tabs Estudiante/Docente
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clickable { selectedTab = "Estudiante" }
                            .background(
                                color = if (selectedTab == "Estudiante") Color(0xFFDC2626) else Color.Transparent,
                                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFFDC2626),
                                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Estudiante",
                            color = if (selectedTab == "Estudiante") Color.White else Color(
                                0xFFDC2626
                            ),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clickable { selectedTab = "Docente" }
                            .background(
                                color = if (selectedTab == "Docente") Color(0xFFDC2626) else Color.Transparent,
                                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFFDC2626),
                                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Docente",
                            color = if (selectedTab == "Docente") Color.White else Color(0xFFDC2626),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Crear Cuenta",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Mensajes
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (successMessage.isNotEmpty()) {
                    Text(
                        text = successMessage,
                        color = Color(0xFF4CAF50),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Campos del formulario
                FormField(
                    label = "Nombre:",
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        errorMessage = ""
                        successMessage = ""
                    },
                    enabled = !isLoading
                )

                FormField(
                    label = "Apellido:",
                    value = apellido,
                    onValueChange = {
                        apellido = it
                        errorMessage = ""
                        successMessage = ""
                    },
                    enabled = !isLoading
                )

                FormField(
                    label = "Email:",
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = ""
                        successMessage = ""
                    },
                    keyboardType = KeyboardType.Email,
                    enabled = !isLoading
                )

                FormField(
                    label = "Contraseña:",
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = ""
                        successMessage = ""
                    },
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityChange = { passwordVisible = it },
                    enabled = !isLoading
                )

                FormField(
                    label = if (selectedTab == "Estudiante") "Carrera:" else "Titulo:",
                    value = carreraTitulo,
                    onValueChange = {
                        carreraTitulo = it
                        errorMessage = ""
                        successMessage = ""
                    },
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        when {
                            nombre.isEmpty() -> errorMessage = "Por favor ingresa tu nombre"
                            apellido.isEmpty() -> errorMessage = "Por favor ingresa tu apellido"
                            email.isEmpty() -> errorMessage = "Por favor ingresa tu email"
                            password.isEmpty() -> errorMessage = "Por favor ingresa tu contraseña"
                            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                                errorMessage = "Por favor ingresa un email válido"

                            carreraTitulo.isEmpty() -> errorMessage =
                                if (selectedTab == "Estudiante") "Por favor ingresa tu carrera"
                                else "Por favor ingresa tu título"

                            else -> {
                                isLoading = true
                                errorMessage = ""
                                successMessage = ""

                                if (selectedTab == "Estudiante") {
                                    onRegisterStudent(
                                        nombre, apellido, email, password, carreraTitulo,
                                        {
                                            isLoading = false
                                            successMessage =
                                                "¡Estudiante registrado exitosamente! Redirigiendo al login..."

                                            // Esperar 2 segundos y redirigir
                                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                                                .launch {
                                                    kotlinx.coroutines.delay(2000)
                                                    onNavigateToLogin()
                                                }
                                        },
                                        { error ->
                                            isLoading = false
                                            errorMessage = error
                                        }
                                    )
                                } else {
                                    onRegisterTeacher(
                                        nombre, apellido, email, password, carreraTitulo,
                                        {
                                            isLoading = false
                                            successMessage =
                                                "¡Docente registrado exitosamente! Redirigiendo al login..."

                                            // Esperar 2 segundos y redirigir
                                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
                                                .launch {
                                                    kotlinx.coroutines.delay(2000)
                                                    onNavigateToLogin()
                                                }
                                        },
                                        { error ->
                                            isLoading = false
                                            errorMessage = error
                                        }
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E3A8A)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "Registrar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "¿Tienes una cuenta? ",
                        fontSize = 12.sp,
                        color = Color(0xFF1E3A8A)
                    )
                    Text(
                        text = "Iniciar Sesion",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                        modifier = Modifier.clickable {
                            if (!isLoading) onNavigateToLogin()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityChange: (Boolean) -> Unit = {},
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E3A8A),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            enabled = enabled,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Next
            ),
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { onPasswordVisibilityChange(!passwordVisible) }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = Color.Gray
                        )
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1E3A8A),
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}