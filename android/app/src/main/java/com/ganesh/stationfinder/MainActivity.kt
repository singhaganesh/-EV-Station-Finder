package com.ganesh.stationfinder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ganesh.stationfinder.data.model.OCMStation
import com.ganesh.stationfinder.util.LocationHelper
import com.ganesh.stationfinder.util.FavoriteManager
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF0F766E),      // Premium Teal
                    secondary = Color(0xFF1E293B),    // Slate Gray
                    background = Color(0xFFF8FAFC)
                )
            ) {
                MainAppScreen()
            }
        }
    }
}

sealed class NavigationItem(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String) {
    object Map : NavigationItem("map", Icons.Default.Map, "Map")
    object List : NavigationItem("list", Icons.AutoMirrored.Filled.List, "List")
    object RoutePlan : NavigationItem("route_planner", Icons.Default.Navigation, "Route Plan")
    object Saved : NavigationItem("saved", Icons.Default.Bookmark, "Saved")
    object Profile : NavigationItem("profile", Icons.Default.Person, "Profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: StationViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    var selectedStation by remember { mutableStateOf<OCMStation?>(null) }
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Sync preferred connector on startup
    val activeConnector = remember { FavoriteManager.getPreferredConnector(context) }
    LaunchedEffect(Unit) {
        viewModel.selectConnectorFilter(activeConnector)
    }

    Scaffold(
        topBar = {
            // Show top bar only for main screens (Map, List, Saved)
            val showTopBar = currentRoute in listOf(NavigationItem.Map.route, NavigationItem.List.route, NavigationItem.Saved.route)
            if (showTopBar) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EvStation,
                                contentDescription = null,
                                tint = Color(0xFF0F766E),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Station Finder",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E293B),
                                fontSize = 20.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                )
            }
        },
        bottomBar = {
            // Show bottom bar only for main screens
            val showBottomBar = currentRoute in listOf(
                NavigationItem.Map.route,
                NavigationItem.List.route,
                NavigationItem.RoutePlan.route,
                NavigationItem.Saved.route,
                NavigationItem.Profile.route
            )
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    val items = listOf(
                        NavigationItem.Map,
                        NavigationItem.List,
                        NavigationItem.RoutePlan,
                        NavigationItem.Saved,
                        NavigationItem.Profile
                    )
                    items.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) Color(0xFF0F766E) else Color.Gray
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF0F766E) else Color.Gray
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color(0xFFE0F2F1)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = NavigationItem.Map.route
            ) {
                composable(NavigationItem.Map.route) {
                    MapTabScreen(viewModel) { station ->
                        selectedStation = station
                    }
                }
                composable(NavigationItem.List.route) {
                    ListScreen(viewModel) { station ->
                        selectedStation = station
                    }
                }
                composable(NavigationItem.Saved.route) {
                    SavedScreen(viewModel) { station ->
                        selectedStation = station
                    }
                }
                composable(NavigationItem.Profile.route) {
                    ProfileScreen(
                        onBackClick = { navController.navigate(NavigationItem.Map.route) { popUpTo(NavigationItem.Map.route) { inclusive = false } } }
                    )
                }
                composable(NavigationItem.RoutePlan.route) {
                    RoutePlannerScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.navigate(NavigationItem.Map.route) { popUpTo(NavigationItem.Map.route) { inclusive = false } } },
                        onStationClick = { station -> selectedStation = station }
                    )
                }
            }

            // Bottom sheet display (overlays active screen)
            selectedStation?.let { station ->
                StationDetailsSheet(
                    station = station,
                    viewModel = viewModel,
                    onDismiss = { selectedStation = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapTabScreen(
    viewModel: StationViewModel,
    onStationClick: (OCMStation) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val selectedConnector by viewModel.selectedConnectorFilter.collectAsState()
    
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            LocationHelper.getCurrentLocation(context) { location ->
                userLocation = location
                location?.let { viewModel.fetchNearbyStations(it) }
            }
        } else {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is StationUiState.Error -> {
                android.widget.Toast.makeText(
                    context, 
                    "Error: ${(uiState as StationUiState.Error).message}", 
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            is StationUiState.Success -> {
                val stations = (uiState as StationUiState.Success).stations
                if (stations.isEmpty() && viewModel.isManualSearch) {
                    android.widget.Toast.makeText(
                        context, 
                        "No stations found in this area", 
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (userLocation != null) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(userLocation!!.let { LatLng(it.latitude, it.longitude) }, 12f)
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(myLocationButtonEnabled = true)
            ) {
                if (uiState is StationUiState.Success) {
                    val stations = (uiState as StationUiState.Success).stations
                    stations.forEach { station ->
                        val isCompatible = selectedConnector == null || station.connectorTypes?.contains(selectedConnector) == true
                        Marker(
                            state = MarkerState(
                                position = LatLng(
                                    station.latitude,
                                    station.longitude
                                )
                            ),
                            title = station.name,
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (isCompatible) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED
                            ),
                            onClick = {
                                onStationClick(station)
                                true // consume click
                            }
                        )
                    }
                }
            }

            // Trigger fetch when camera stops
            LaunchedEffect(cameraPositionState.isMoving) {
                if (!cameraPositionState.isMoving) {
                    val center = cameraPositionState.position.target
                    val zoom = cameraPositionState.position.zoom
                    viewModel.fetchNearbyStationsDebounced(center, zoom)
                }
            }

            // Top Filter Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // Connector Filter Chips Row
                val connectors = listOf("CCS2", "Type 2", "CHAdeMO", "Type 1")
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedConnector == null,
                            onClick = { viewModel.selectConnectorFilter(null) },
                            label = { Text("All", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0F766E),
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Color.Gray
                            )
                        )
                    }
                    items(connectors.size) { index ->
                        val connector = connectors[index]
                        val isSelected = selectedConnector == connector
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectConnectorFilter(connector) },
                            label = { Text(connector, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0F766E),
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Color.Gray
                            )
                        )
                    }
                }
            }

            // Nearby Stations Carousel overlay at the bottom
            if (uiState is StationUiState.Success) {
                val stations = (uiState as StationUiState.Success).stations
                if (stations.isNotEmpty()) {
                    NearbyStationsCarousel(
                        stations = stations,
                        onStationClick = { station ->
                            // Center camera on clicked station
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                LatLng(station.latitude, station.longitude),
                                14f
                            )
                            onStationClick(station)
                        },
                        onNavigateClick = { station ->
                            val lat = station.latitude
                            val lng = station.longitude
                            val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF0F766E))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Fetching your location...", fontWeight = FontWeight.Medium, color = Color.Gray)
                }
            }
        }

        // Loading indicator overlay
        if (uiState is StationUiState.Loading && userLocation != null) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color = Color(0xFF0F766E)
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NearbyStationsCarousel(
    stations: List<OCMStation>,
    onStationClick: (OCMStation) -> Unit,
    onNavigateClick: (OCMStation) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    androidx.compose.foundation.lazy.LazyRow(
        state = lazyListState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        val nearbyFive = stations.take(5)
        items(nearbyFive.size) { index ->
            val station = nearbyFive[index]
            Card(
                modifier = Modifier
                    .width(300.dp)
                    .clickable { onStationClick(station) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Top row: Title and Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = station.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val isAvailable = (station.availableSlots ?: 0) > 0
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isAvailable) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    BorderStroke(1.dp, if (isAvailable) Color(0xFF15803D).copy(alpha = 0.2f) else Color(0xFF991B1B).copy(alpha = 0.2f)),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isAvailable) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(10.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = Color(0xFF991B1B),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAvailable) "Available" else "In Use",
                                    color = if (isAvailable) Color(0xFF15803D) else Color(0xFF991B1B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Second row: Rating, Distance, Connector Type & Never used
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = String.format("%.1f", station.rating ?: 0.0),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF1E293B)
                        )

                        Spacer(modifier = Modifier.width(2.dp))

                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${String.format("%.1f", station.distance ?: 0.0)} km",
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = Color(0xFF545F73)
                        )

                        Spacer(modifier = Modifier.width(2.dp))

                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFFF4081),
                            modifier = Modifier.size(14.dp)
                        )
                        val connector = station.connectorTypes?.firstOrNull() ?: "Unknown"
                        Text(
                            text = connector,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF545F73)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "Never used",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }

                    // Divider
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Third row: Navigate button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateClick(station) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Navigate to Station",
                            color = Color(0xFF0F766E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
