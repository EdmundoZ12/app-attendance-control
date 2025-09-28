package com.edworld.attendance_control_app.data.models

// Modelos para Login
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val message: String, val token: String, val user: User)

// Modelos para Registro
data class RegisterStudentRequest(
    val nombre: String,
    val apellido: String,
    val email: String,
    val password: String,
    val carrera: String
)

data class RegisterTeacherRequest(
    val nombre: String,
    val apellido: String,
    val email: String,
    val password: String,
    val titulo: String
)

data class RegisterResponse(val message: String, val estudiante: User)
data class RegisterTeacherResponse(val message: String, val docente: User)

// Modelo de Usuario base
data class User(
    val id: Int,
    val nombre: String,
    val apellido: String,
    val email: String,
    val rol: String
)

// MODELOS UNIFICADOS PARA MATERIAS
data class MateriaResponse(val materias: List<Materia>)
data class GetMateriasRequest(val docente_id: Int)

// Modelo unificado para materia (usar en todas las respuestas)
data class Materia(
    val id: Int,
    val nombre: String,
    val codigo: String,
    val descripcion: String?,
    val grupo: String,
    val docente_id: Int,
    val latitud: Double,
    val longitud: Double,
    val activo: Boolean
)

// REQUESTS para operaciones de materia
data class CrearMateriaRequest(
    val nombre: String,
    val codigo: String,
    val descripcion: String,
    val grupo: String,
    val docente_id: Int,
    val latitud: Double,
    val longitud: Double,
    val horarios: List<HorarioRequest>
)

data class ActualizarMateriaRequest(
    val id: Int,
    val nombre: String,
    val codigo: String,
    val descripcion: String,
    val grupo: String,
    val latitud: Double,
    val longitud: Double,
    val activo: Boolean
)

data class ObtenerMateriaRequest(val id: Int)

// RESPONSES unificadas para materia
data class MateriaResponseSingle(val materia: Materia)
data class MateriaOperationResponse(val message: String, val materia: Materia)

// MODELOS UNIFICADOS PARA HORARIOS
// Modelo unificado para horario (usar en todas las respuestas)
data class Horario(
    val id: Int,
    val materia_id: Int,
    val dia_semana: String,
    val hora_inicio: String,
    val hora_fin: String
)

// Modelo para UI (mantener separado porque tiene estructura diferente)
data class HorarioItem(
    val id: Int = 0,
    val dia: String,
    val horaInicio: String,
    val horaFin: String
)

// REQUESTS para operaciones de horario
data class HorarioRequest(
    val dia_semana: String,
    val hora_inicio: String,
    val hora_fin: String
)

data class AgregarHorarioRequest(
    val materia_id: Int,
    val dia_semana: String,
    val hora_inicio: String,
    val hora_fin: String
)

data class ActualizarHorarioRequest(
    val horario_id: Int,
    val dia_semana: String,
    val hora_inicio: String,
    val hora_fin: String
)

data class EliminarHorarioRequest(val horario_id: Int)
data class ObtenerHorariosRequest(val materia_id: Int)

// RESPONSES unificadas para horario
data class HorariosResponse(val horarios: List<Horario>)
data class HorarioOperationResponse(val message: String, val horario: Horario)

// Modelo para cambiar estado de materia
data class CambiarEstadoMateriaRequest(
    val id: Int
)