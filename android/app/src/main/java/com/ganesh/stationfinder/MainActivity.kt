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
import com.ganesh.stationfinder.data.model.StationMarker
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

object MarkerIconCache {
    private val cache = mutableMapOf<String, com.google.android.gms.maps.model.BitmapDescriptor>()
    
    fun get(colorHex: String, isLarge: Boolean): com.google.android.gms.maps.model.BitmapDescriptor {
        val key = "$colorHex-$isLarge"
        return cache.getOrPut(key) {
            val size = if (isLarge) 90 else 55
            val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
            }
            val colorInt = android.graphics.Color.parseColor(colorHex)
            
            // 1. Draw a soft black drop shadow oval under the tip
            val shadowPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.BLACK
                alpha = 40
            }
            canvas.drawOval(size * 0.35f, size * 0.9f, size * 0.65f, size.toFloat(), shadowPaint)
            
            // 2. Draw teardrop body path
            val pinHeight = size * 0.9f
            val pinPath = android.graphics.Path().apply {
                moveTo(size / 2f, pinHeight)
                cubicTo(size * 0.12f, pinHeight * 0.6f, size * 0.12f, 0f, size / 2f, 0f)
                cubicTo(size * 0.88f, 0f, size * 0.88f, pinHeight * 0.6f, size / 2f, pinHeight)
                close()
            }
            
            paint.color = colorInt
            paint.style = android.graphics.Paint.Style.FILL
            canvas.drawPath(pinPath, paint)
            
            // 3. Draw white stroke outline around teardrop
            paint.color = android.graphics.Color.WHITE
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = if (isLarge) 4f else 2.5f
            canvas.drawPath(pinPath, paint)
            
            // 4. Draw white inner circular core
            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.WHITE
            canvas.drawCircle(size / 2f, pinHeight * 0.38f, pinHeight * 0.13f, paint)
            
            com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }
}

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
    val markerState by viewModel.markerState.collectAsState()
    val selectedConnector by viewModel.selectedConnectorFilter.collectAsState()
    val selectedStationDetail by viewModel.selectedStationDetail.collectAsState()
    
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

    LaunchedEffect(markerState) {
        when (val state = markerState) {
            is MarkerUiState.Error -> {
                android.widget.Toast.makeText(
                    context, 
                    "Error: ${state.message}", 
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            is MarkerUiState.Success -> {
                if (state.tooMany) {
                    android.widget.Toast.makeText(
                        context, 
                        "Showing nearest 200. Zoom in for more.", 
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            else -> {}
        }
    }

    LaunchedEffect(selectedStationDetail) {
        selectedStationDetail?.let { detail ->
            onStationClick(detail)
            viewModel.clearSelectedStationDetail()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (userLocation != null) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(userLocation!!.let { LatLng(it.latitude, it.longitude) }, 10f)
            }

            // Carousel Stations Flow and Active Element tracking
            val carouselStations by viewModel.carouselStations.collectAsState()
            var selectedMarkerId by remember { mutableStateOf<Long?>(null) }
            val visibleCarousel = carouselStations.take(5)

            val pagerState = rememberPagerState(pageCount = { visibleCarousel.size })

            // Reset pager to page 0 when carousel data changes to avoid stale index
            LaunchedEffect(visibleCarousel) {
                if (visibleCarousel.isNotEmpty() && pagerState.currentPage >= visibleCarousel.size) {
                    pagerState.scrollToPage(0)
                }
            }

            val activeStationId = remember(visibleCarousel, pagerState.currentPage, selectedMarkerId) {
                selectedMarkerId ?: if (visibleCarousel.isNotEmpty()) {
                    val index = pagerState.currentPage
                    if (index in visibleCarousel.indices) {
                        visibleCarousel[index].id
                    } else null
                } else null
            }

            // Sync selectedMarkerId when swiping the pager
            LaunchedEffect(pagerState.currentPage, visibleCarousel) {
                if (visibleCarousel.isNotEmpty()) {
                    val index = pagerState.currentPage
                    if (index in visibleCarousel.indices) {
                        val station = visibleCarousel[index]
                        if (selectedMarkerId != station.id) {
                            selectedMarkerId = station.id
                        }
                    }
                }
            }

            // Scroll pager to selected marker when it gets updated or loaded
            LaunchedEffect(visibleCarousel, selectedMarkerId) {
                if (selectedMarkerId != null && visibleCarousel.isNotEmpty()) {
                    val index = visibleCarousel.indexOfFirst { it.id == selectedMarkerId }
                    if (index >= 0 && pagerState.currentPage != index) {
                        pagerState.scrollToPage(index)
                    }
                }
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(myLocationButtonEnabled = true)
            ) {
                if (markerState is MarkerUiState.Success) {
                    val markers = (markerState as MarkerUiState.Success).markers
                    markers.forEach { marker ->
                        val isActive = marker.id == activeStationId
                        val isCompatible = selectedConnector == null || marker.connectorTypes?.contains(selectedConnector) == true
                        val colorHex = if (isCompatible) "#0F766E" else "#EF4444"
                        
                        Marker(
                            state = MarkerState(
                                position = LatLng(
                                    marker.latitude,
                                    marker.longitude
                                )
                            ),
                            title = marker.name,
                            icon = MarkerIconCache.get(colorHex, isLarge = isActive),
                            onClick = {
                                selectedMarkerId = marker.id
                                // Compute nearest 5 stations client-side from already-loaded markers
                                // Distance displayed is from user's actual GPS location
                                if (userLocation != null && markerState is MarkerUiState.Success) {
                                    val allMarkers = (markerState as MarkerUiState.Success).markers
                                    viewModel.computeCarouselFromMarkers(
                                        markers = allMarkers,
                                        pinLat = marker.latitude,
                                        pinLng = marker.longitude,
                                        userLat = userLocation!!.latitude,
                                        userLng = userLocation!!.longitude
                                    )
                                }
                                true // consume click
                            }
                        )
                    }
                }
            }

            // Trigger fetch when camera stops or projection becomes available on initial load
            LaunchedEffect(cameraPositionState.isMoving, cameraPositionState.projection) {
                if (cameraPositionState.isMoving) {
                    if (cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
                        selectedMarkerId = null
                    }
                } else {
                    val bounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
                    val projection = cameraPositionState.projection
                    val visibleRegionInternal = projection?.visibleRegion
                    if (bounds != null) {
                        viewModel.fetchViewportMarkers(
                            bounds.northeast.latitude, bounds.northeast.longitude,
                            bounds.southwest.latitude, bounds.southwest.longitude
                        )
                        if (selectedMarkerId == null) {
                            val center = cameraPositionState.position.target
                            val radius = if (visibleRegionInternal != null) {
                                val results = FloatArray(1)
                                android.location.Location.distanceBetween(
                                    center.latitude, center.longitude,
                                    visibleRegionInternal.farRight.latitude, visibleRegionInternal.farRight.longitude,
                                    results
                                )
                                // Convert meters to kilometers and add 20% buffer
                                (results[0] / 1000.0) * 1.2
                            } else {
                                10.0
                            }
                            viewModel.fetchCarouselStations(center.latitude, center.longitude, radius)
                        }
                    }
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

            if (visibleCarousel.isNotEmpty()) {
                NearbyStationsCarousel(
                    stations = visibleCarousel,
                    pagerState = pagerState,
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
        if (markerState is MarkerUiState.Loading && userLocation != null) {
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
    pagerState: androidx.compose.foundation.pager.PagerState,
    onStationClick: (OCMStation) -> Unit,
    onNavigateClick: (OCMStation) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.pager.HorizontalPager(
        state = pagerState,
        pageSize = androidx.compose.foundation.pager.PageSize.Fixed(300.dp),
        pageSpacing = 12.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier.fillMaxWidth()
    ) { page ->
        val station = stations[page]
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
