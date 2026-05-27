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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.*

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
    object Saved : NavigationItem("saved", Icons.Default.Bookmark, "Saved")
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
                    actions = {
                        IconButton(onClick = { navController.navigate("route_planner") }) {
                            Icon(Icons.Default.Navigation, contentDescription = "Route Planner", tint = Color(0xFF0F766E))
                        }
                        IconButton(onClick = { navController.navigate("profile") }) {
                            Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color(0xFF0F766E))
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
            val showBottomBar = currentRoute in listOf(NavigationItem.Map.route, NavigationItem.List.route, NavigationItem.Saved.route)
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    val items = listOf(
                        NavigationItem.Map,
                        NavigationItem.List,
                        NavigationItem.Saved
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
                composable("profile") {
                    ProfileScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable("route_planner") {
                    RoutePlannerScreen(
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() },
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
