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

// Información básica del estudiante
data class EstudianteInfo(
    val id: Int,
    val nombre: String,
    val apellido: String,
    val email: String,
    val carrera: String
)

// Response para búsqueda de estudiante por email
data class EstudianteResponse(
    val estudiante: EstudianteInfo
)

// Request para inscribir estudiante a materia
data class InscribirEstudianteRequest(
    val materia_id: Int,
    val estudiante_id: Int
)

// Response para inscripción exitosa
data class InscripcionResponse(
    val message: String,
    val asignacion: AsignacionInfo
)

data class AsignacionInfo(
    val estudiante_id: Int,
    val materia_id: Int
)

// Request para obtener estudiantes inscritos en una materia
data class EstudiantesInscritosRequest(
    val materia_id: Int
)

// Response para lista de estudiantes inscritos
data class EstudiantesInscritosResponse(
    val message: String,
    val materia: MateriaBasicInfo,
    val estudiantes: List<EstudianteInfo>,
    val total_estudiantes: Int
)

// Información básica de la materia para respuestas
data class MateriaBasicInfo(
    val id: Int,
    val nombre: String,
    val codigo: String,
    val grupo: String
)

// Request para generar QR
data class GenerarQRRequest(
    val materia_id: Int,
    val horario_id: Int,
    val docente_id: Int
)

// Response del QR generado
data class QRGeneratedResponse(
    val message: String,
    val qr: QRData
)

data class QRData(
    val qr_token: String,
    val qr_image: String,
    val expires_at: String,
    val horario: HorarioQR
)

data class HorarioQR(
    val id: Int,
    val dia_semana: String,
    val hora_inicio: String,
    val hora_fin: String
)

// Datos extraídos del QR escaneado
data class QRClassData(
    val materiaId: Int,
    val horarioId: Int,
    val docenteId: Int,
    val materiaNombre: String,
    val diaSemana: String,
    val horaInicio: String,
    val horaFin: String,
    val fecha: String,
    val exp: Long
)

// Datos de ubicación GPS
data class LocationData(
    val latitud: Double,
    val longitud: Double
)

// Request para registrar asistencia
data class RegistrarAsistenciaRequest(
    val qr_token: String,
    val estudiante_id: Int,
    val ubicacion_lat: Double,
    val ubicacion_lng: Double
)

// Response de asistencia registrada exitosamente
data class AsistenciaRegistradaResponse(
    val message: String,
    val asistencia: AsistenciaData,
    val distancia_metros: Int,
    val horario: HorarioData
)

data class AsistenciaData(
    val id: Int,
    val estudiante_id: Int,
    val materia_id: Int,
    val fecha: String,
    val hora_registro: String,
    val ubicacion_lat: Double,
    val ubicacion_lng: Double
)

data class HorarioData(
    val dia: String,
    val inicio: String,
    val fin: String
)

// MODELOS PARA HISTORIAL DE ASISTENCIAS

// Modelo para mostrar en el historial del estudiante
// Agregar estos modelos a tu archivo ApiModels.kt
// Asegúrate de tener el import correcto:
// import kotlinx.serialization.Serializable

// Request para obtener asistencias del estudiante
data class GetAsistenciasEstudianteRequest(
    val estudiante_id: Int
)

// Response del endpoint de asistencias del estudiante
data class AsistenciasEstudianteResponse(
    val asistencias: List<AsistenciaEstudianteData>
)

// Datos de cada asistencia desde la API
data class AsistenciaEstudianteData(
    val id: Int,
    val estudiante_id: Int,
    val materia_id: Int,
    val fecha: String,
    val hora_registro: String,
    val ubicacion_lat: Double,
    val ubicacion_lng: Double,
    val materia_nombre: String,
    val materia_codigo: String
)

// Modelo para la UI del historial
data class AsistenciaHistorial(
    val id: Int,
    val estudianteId: Int,
    val materiaId: Int,
    val materiaNombre: String,
    val materiaCodigo: String,
    val fecha: String,
    val horaRegistro: String,
    val ubicacionLat: Double,
    val ubicacionLng: Double
)