package com.edworld.attendance_control_app.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory

class MapSelectionActivity : ComponentActivity() {

    private var selectedLatitud by mutableStateOf(0.0)
    private var selectedLongitud by mutableStateOf(0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener ubicación inicial
        val initialLat = intent.getDoubleExtra("initial_lat", 0.0)
        val initialLng = intent.getDoubleExtra("initial_lng", 0.0)

        selectedLatitud = initialLat
        selectedLongitud = initialLng

        setContent {
            MapSelectionScreen(
                initialLatitud = initialLat,
                initialLongitud = initialLng,
                selectedLatitud = selectedLatitud,
                selectedLongitud = selectedLongitud,
                onLocationSelected = { lat, lng ->
                    selectedLatitud = lat
                    selectedLongitud = lng
                    Log.d("MAP_SELECTION", "Nueva ubicación: $lat, $lng")
                },
                onConfirm = {
                    val resultIntent = Intent().apply {
                        putExtra("selected_lat", selectedLatitud)
                        putExtra("selected_lng", selectedLongitud)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                },
                onCancel = {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapSelectionScreen(
    initialLatitud: Double,
    initialLongitud: Double,
    selectedLatitud: Double,
    selectedLongitud: Double,
    onLocationSelected: (Double, Double) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Seleccionar Ubicación",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Cancelar",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onConfirm) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirmar",
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
        ) {
            // Mapa interactivo
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        onCreate(null)
                        onResume()
                        getMapAsync { googleMap ->
                            val initialLocation = LatLng(initialLatitud, initialLongitud)

                            // Marcador inicial
                            var currentMarker = googleMap.addMarker(
                                MarkerOptions()
                                    .position(initialLocation)
                                    .title("Ubicación seleccionada")
                                    .icon(
                                        BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_RED
                                        )
                                    )
                            )

                            // Configurar cámara
                            googleMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(initialLocation, 15f)
                            )

                            // Habilitar gestos para interactuar
                            googleMap.uiSettings.isZoomControlsEnabled = true
                            googleMap.uiSettings.isScrollGesturesEnabled = true
                            googleMap.uiSettings.isZoomGesturesEnabled = true
                            googleMap.uiSettings.isRotateGesturesEnabled = true
                            googleMap.uiSettings.isTiltGesturesEnabled = true

                            // Listener para toques en el mapa
                            googleMap.setOnMapClickListener { latLng ->
                                // Remover marcador anterior
                                currentMarker?.remove()

                                // Agregar nuevo marcador
                                currentMarker = googleMap.addMarker(
                                    MarkerOptions()
                                        .position(latLng)
                                        .title("Ubicación seleccionada")
                                        .icon(
                                            BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_RED
                                            )
                                        )
                                )

                                // Actualizar ubicación seleccionada
                                onLocationSelected(latLng.latitude, latLng.longitude)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Información de la ubicación seleccionada
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Ubicación Seleccionada:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Latitud:", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                String.format("%.6f", selectedLatitud),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Column {
                            Text("Longitud:", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                String.format("%.6f", selectedLongitud),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Toca en el mapa para cambiar la ubicación",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}