package com.edworld.attendance_control_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.edworld.attendance_control_app.activities.HistorialAsistenciasActivity
import com.edworld.attendance_control_app.activities.HomeDocenteActivity
import com.edworld.attendance_control_app.screens.RegisterScreen
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.launch
import com.edworld.attendance_control_app.data.models.*

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val TOKEN_KEY = stringPreferencesKey("auth_token")
val USER_ID_KEY = stringPreferencesKey("user_id")
val USER_ROL_KEY = stringPreferencesKey("user_rol")

class MainActivity : ComponentActivity() {

//    val url: String = "http://192.168.100.101:3000"
//        val url:String="http://172.20.10.3:300"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var currentScreen by remember { mutableStateOf("login") }

            when (currentScreen) {
                "login" -> LoginScreen(
                    onNavigateToRegister = { currentScreen = "register" },
                    onLoginSuccess = { rol -> navigateToHome(rol) },
                    onLogin = { email, password, onSuccess, onError ->
                        login(email, password, onSuccess, onError)
                    }
                )

                "register" -> RegisterScreen(
                    onNavigateToLogin = { currentScreen = "login" },
                    onRegisterStudent = { nombre, apellido, email, password, carrera, onSuccess, onError ->
                        registerStudent(
                            nombre,
                            apellido,
                            email,
                            password,
                            carrera,
                            onSuccess,
                            onError
                        )
                    },
                    onRegisterTeacher = { nombre, apellido, email, password, titulo, onSuccess, onError ->
                        registerTeacher(
                            nombre,
                            apellido,
                            email,
                            password,
                            titulo,
                            onSuccess,
                            onError
                        )
                    }
                )
            }
        }
    }

    // Singleton para Ktor
    object KtorClient {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) { gson() }
        }
    }

    private fun login(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val response = KtorClient.client.post("${Constants.BASE_URL}/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(email, password))
                }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val loginResponse: LoginResponse = response.body()
                        dataStore.edit { preferences ->
                            preferences[TOKEN_KEY] = loginResponse.token
                            preferences[USER_ID_KEY] = loginResponse.user.id.toString()
                            preferences[USER_ROL_KEY] = loginResponse.user.rol
                        }
                        onSuccess(loginResponse.user.rol)
                    }

                    HttpStatusCode.Unauthorized -> {
                        val errorResponse = response.body<Map<String, String>>()
                        onError(errorResponse["error"] ?: "Email o contraseña incorrectos")
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

    private fun registerStudent(
        nombre: String,
        apellido: String,
        email: String,
        password: String,
        carrera: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val response =
                    KtorClient.client.post("${Constants.BASE_URL}/auth/register/student") {
                        contentType(ContentType.Application.Json)
                        setBody(RegisterStudentRequest(nombre, apellido, email, password, carrera))
                    }

                when (response.status) {
                    HttpStatusCode.Created -> {
                        val registerResponse: RegisterResponse = response.body()
                        onSuccess()
                    }

                    HttpStatusCode.Conflict -> {
                        val errorResponse = response.body<Map<String, String>>()
                        onError(errorResponse["error"] ?: "El email ya está registrado")
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

    private fun registerTeacher(
        nombre: String,
        apellido: String,
        email: String,
        password: String,
        titulo: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val response =
                    KtorClient.client.post("${Constants.BASE_URL}/auth/register/teacher") {
                        contentType(ContentType.Application.Json)
                        setBody(RegisterTeacherRequest(nombre, apellido, email, password, titulo))
                    }

                when (response.status) {
                    HttpStatusCode.Created -> {
                        val registerResponse: Map<String, Any> = response.body()
                        onSuccess()
                    }

                    HttpStatusCode.Conflict -> {
                        val errorResponse = response.body<Map<String, String>>()
                        onError(errorResponse["error"] ?: "El email ya está registrado")
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

    private fun navigateToHome(rol: String) {
        val intent = when (rol.lowercase()) {
            "docente" -> Intent(this, HomeDocenteActivity::class.java)
            "estudiante" -> Intent(this, HistorialAsistenciasActivity::class.java)
            else -> Intent(this, HomeDocenteActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}

suspend fun clearToken(context: Context) {
    context.dataStore.edit { preferences ->
        preferences.remove(TOKEN_KEY)
    }
}

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    onLogin: (String, String, (String) -> Unit, (String) -> Unit) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "UAGRM",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
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
                    Text(
                        text = "Iniciar Sesion",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Email:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E3A8A),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            enabled = !isLoading,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1E3A8A),
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Password:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E3A8A),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            enabled = !isLoading,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                        tint = Color.Gray
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1E3A8A),
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            when {
                                email.isEmpty() -> errorMessage = "Por favor ingresa tu email"
                                password.isEmpty() -> errorMessage =
                                    "Por favor ingresa tu contraseña"

                                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                                    errorMessage = "Por favor ingresa un email válido"

                                else -> {
                                    isLoading = true
                                    errorMessage = ""
                                    onLogin(
                                        email,
                                        password,
                                        { rol ->
                                            isLoading = false
                                            onLoginSuccess(rol)
                                        },
                                        { error ->
                                            isLoading = false
                                            errorMessage = error
                                        }
                                    )
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
                                text = "Ingresar",
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
                            text = "¿No tienes una cuenta? ",
                            fontSize = 12.sp,
                            color = Color(0xFF1E3A8A)
                        )
                        Text(
                            text = "Crear Cuenta",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.clickable {
                                if (!isLoading) onNavigateToRegister()
                            }
                        )
                    }
                }
            }
        }
    }
}