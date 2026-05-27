package com.ganesh.stationfinder

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganesh.stationfinder.data.model.OCMStation
import com.ganesh.stationfinder.util.FavoriteManager
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlannerScreen(
    viewModel: StationViewModel,
    onBackClick: () -> Unit,
    onStationClick: (OCMStation) -> Unit
) {
    val context = LocalContext.current
    val routeStations by viewModel.routeStations.collectAsState()
    val isRouteLoading by viewModel.isRouteLoading.collectAsState()

    var fromText by remember { mutableStateOf("Bandra, Mumbai") }
    var toText by remember { mutableStateOf("Pune, Maharashtra") }

    val vehicleModel = remember { FavoriteManager.getVehicleModel(context) }
    val preferredConnector = remember { FavoriteManager.getPreferredConnector(context) }
    val range = remember { FavoriteManager.getRange(context) }

    // Map properties
    // Mumbai center coordinates
    val defaultCenter = LatLng(18.85, 73.25)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, 9f)
    }

    // Hardcoded route coordinates for Mumbai-Pune Expressway demo
    val routePoints = remember {
        listOf(
            LatLng(19.0596, 72.8295), // Bandra
            LatLng(19.0330, 73.0297), // Navi Mumbai
            LatLng(18.7557, 73.4091), // Lonavala
            LatLng(18.5204, 73.8567)  // Pune
        )
    }

    val waypointsQuery = remember(routePoints) {
        routePoints.joinToString("|") { "${it.latitude},${it.longitude}" }
    }

    // Auto-fetch on screen open
    LaunchedEffect(Unit) {
        viewModel.fetchStationsAlongRoute(waypointsQuery, preferredConnector)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Route Planner",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F766E))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Google Map View (renders underneath panels)
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false)
            ) {
                // Draw route line
                Polyline(
                    points = routePoints,
                    color = Color(0xFF0F766E),
                    width = 8f
                )

                // Route pins
                Marker(
                    state = MarkerState(position = routePoints.first()),
                    title = "Start: $fromText",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
                Marker(
                    state = MarkerState(position = routePoints.last()),
                    title = "End: $toText",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )

                // Recommended charging stops along the route
                routeStations.forEachIndexed { index, station ->
                    val isAvailable = (station.availableSlots ?: 0) > 0
                    Marker(
                        state = MarkerState(
                            position = LatLng(station.latitude, station.longitude)
                        ),
                        title = "Stop ${index + 1}: ${station.name}",
                        snippet = if (isAvailable) "Available" else "Busy",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (isAvailable) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_ORANGE
                        ),
                        onClick = {
                            onStationClick(station)
                            true
                        }
                    )
                }
            }

            // Route Input overlay card (positioned at the top)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Route visual indicator (circle -> line -> circle)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF0F766E), RoundedCornerShape(5.dp))
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color.Red, RoundedCornerShape(5.dp))
                                )
                            }

                            // Input fields
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = fromText,
                                    onValueChange = { fromText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("From") },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF0F766E),
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    )
                                )
                                OutlinedTextField(
                                    value = toText,
                                    onValueChange = { toText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("To") },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF0F766E),
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    )
                                )
                            }

                            // Swap button
                            IconButton(onClick = {
                                val temp = fromText
                                fromText = toText
                                toText = temp
                            }) {
                                Icon(Icons.Default.SwapVert, contentDescription = "Swap", tint = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Info chips (Model, connector, range)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistChip(
                                onClick = {},
                                label = { Text(vehicleModel, fontSize = 10.sp) },
                                leadingIcon = { Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(12.dp)) }
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text(preferredConnector, fontSize = 10.sp) },
                                leadingIcon = { Icon(Icons.Default.EvStation, null, modifier = Modifier.size(12.dp)) }
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text(range, fontSize = 10.sp) },
                                leadingIcon = { Icon(Icons.Default.BatteryChargingFull, null, modifier = Modifier.size(12.dp)) }
                            )
                        }
                    }
                }
            }

            // Bottom Panel: Suggested Stops list (slidable overlay)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(320.dp),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Panel Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Suggested Stops",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "145 km • 3h 15m total route estimation",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        
                        Button(
                            onClick = {
                                viewModel.fetchStationsAlongRoute(waypointsQuery, preferredConnector)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0F2F1), contentColor = Color(0xFF0F766E)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isRouteLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFF0F766E))
                            } else {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Refresh", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Suggested Stops timeline list
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (routeStations.isNotEmpty()) {
                            routeStations.forEachIndexed { index, station ->
                                val isAvailable = (station.availableSlots ?: 0) > 0
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onStationClick(station) },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Stop ${index + 1}: ${station.name}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF1E293B)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (isAvailable) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (isAvailable) "Available" else "Busy",
                                                    color = if (isAvailable) Color(0xFF15803D) else Color(0xFF991B1B),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = station.address ?: "",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                            station.slots?.firstOrNull()?.let { slot ->
                                                val formattedPower = slot.powerKw?.let { p ->
                                                    val rounded = Math.round(p * 10.0) / 10.0
                                                    if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
                                                } ?: ""
                                                Text(
                                                    text = "${slot.connectorType} • ${formattedPower}kW",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF0F766E)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No compatible stations along this route.",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Navigation Action
                    Button(
                        onClick = {
                            // Launch navigation intent to final destination (Pune)
                            val gmmIntentUri = Uri.parse("google.navigation:q=18.5204,73.8567")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Icon(Icons.Default.Navigation, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Navigation", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
