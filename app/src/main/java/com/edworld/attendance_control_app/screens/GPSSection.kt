package com.edworld.attendance_control_app.screens

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.edworld.attendance_control_app.activities.MapSelectionActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory

@Composable
fun GPSSection(
    latitud: Double = 0.0,
    longitud: Double = 0.0,
    isLoading: Boolean = false,
    onLocationUpdate: (Double, Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    // Launcher para abrir el mapa de selección
    val mapSelectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedLat = result.data?.getDoubleExtra("selected_lat", 0.0) ?: 0.0
            val selectedLng = result.data?.getDoubleExtra("selected_lng", 0.0) ?: 0.0

            Log.d("GPS_SECTION", "Nueva ubicación seleccionada: $selectedLat, $selectedLng")
            onLocationUpdate(selectedLat, selectedLng)
        } else {
            Log.d("GPS_SECTION", "Selección de mapa cancelada")
        }
    }

    // Log para debug
    LaunchedEffect(latitud, longitud) {
        Log.d("GPS_DEBUG", "Latitud: $latitud, Longitud: $longitud")
    }

    Column {
        // Título con botón de actualizar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ubicación GPS:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E3A8A)
            )

            // Botón para actualizar ubicación
            IconButton(
                onClick = {
                    Log.d("GPS_REFRESH", "Botón de actualizar presionado")
                    // TODO: Actualizar ubicación actual
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizar ubicación",
                    tint = Color(0xFF1E3A8A)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mapa real de Google Maps
        RealMapView(
            latitud = latitud,
            longitud = longitud,
            isLoading = isLoading,
            onMapClick = {
                Log.d("GPS_CLICK", "onMapClick ejecutado - abriendo MapSelectionActivity")
                try {
                    val intent = Intent(context, MapSelectionActivity::class.java).apply {
                        putExtra("initial_lat", latitud)
                        putExtra("initial_lng", longitud)
                    }
                    Log.d("GPS_CLICK", "Intent creado con lat: $latitud, lng: $longitud")
                    mapSelectionLauncher.launch(intent)
                } catch (e: Exception) {
                    Log.e("GPS_ERROR", "Error al abrir MapSelectionActivity: ${e.message}")
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Información de coordenadas
        LocationInfo(
            latitud = latitud,
            longitud = longitud,
            isLoading = isLoading
        )
    }
}

@Composable
private fun RealMapView(
    latitud: Double,
    longitud: Double,
    isLoading: Boolean,
    onMapClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading) {
                // Indicador de carga
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF0F0F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1E3A8A),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Obteniendo ubicación...",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else if (latitud != 0.0 && longitud != 0.0) {
                // Mapa con overlay clickeable
                Box(modifier = Modifier.fillMaxSize()) {
                    GoogleMapView(
                        latitud = latitud,
                        longitud = longitud
                    )

                    // Overlay transparente para capturar clicks
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                Log.d("GPS_CLICK", "Overlay del mapa tocado")
                                onMapClick()
                            }
                    )
                }
            } else {
                // Estado sin ubicación
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF0F0F0))
                        .clickable {
                            Log.d("GPS_CLICK", "Estado sin ubicación tocado")
                            onMapClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Sin ubicación",
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ubicación no disponible",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Toca para seleccionar",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Indicador de que es clickeable
            if (!isLoading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .clickable {
                            Log.d("GPS_CLICK", "Botón 'Toca para cambiar' presionado")
                            onMapClick()
                        }
                ) {
                    Text(
                        text = "Toca para cambiar",
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun GoogleMapView(
    latitud: Double,
    longitud: Double
) {
    var mapView: MapView? by remember { mutableStateOf(null) }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                mapView = this
                onCreate(null)
                onResume()
                getMapAsync { googleMap ->
                    updateMapLocation(googleMap, latitud, longitud)
                }
            }
        },
        update = { view ->
            // Esta función se ejecuta cuando cambian las coordenadas
            view.getMapAsync { googleMap ->
                updateMapLocation(googleMap, latitud, longitud)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// Función helper para actualizar la ubicación del mapa
private fun updateMapLocation(
    googleMap: com.google.android.gms.maps.GoogleMap,
    latitud: Double,
    longitud: Double
) {
    val userLocation = LatLng(latitud, longitud)

    // Limpiar marcadores anteriores
    googleMap.clear()

    // Agregar nuevo marcador en la ubicación actualizada
    googleMap.addMarker(
        MarkerOptions()
            .position(userLocation)
            .title("Ubicación seleccionada")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
    )

    // Mover cámara a la nueva ubicación
    googleMap.moveCamera(
        CameraUpdateFactory.newLatLngZoom(userLocation, 15f)
    )

    // Configurar el mapa (solo visualización)
    googleMap.uiSettings.isZoomControlsEnabled = false
    googleMap.uiSettings.isScrollGesturesEnabled = false
    googleMap.uiSettings.isZoomGesturesEnabled = false
    googleMap.uiSettings.isRotateGesturesEnabled = false
    googleMap.uiSettings.isTiltGesturesEnabled = false
}

@Composable
private fun LocationInfo(
    latitud: Double,
    longitud: Double,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFF1E3A8A),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Obteniendo ubicación...",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            } else if (latitud != 0.0 && longitud != 0.0) {
                // Mostrar coordenadas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Latitud:",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = String.format("%.6f", latitud),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Column {
                        Text(
                            text = "Longitud:",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = String.format("%.6f", longitud),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mostrar coordenadas como dirección temporal
                Text(
                    text = "Coordenadas: ${String.format("%.4f", latitud)}, ${
                        String.format(
                            "%.4f",
                            longitud
                        )
                    }",
                    fontSize = 12.sp,
                    color = Color(0xFF1E3A8A)
                )
            } else {
                Text(
                    text = "Ubicación no establecida",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}