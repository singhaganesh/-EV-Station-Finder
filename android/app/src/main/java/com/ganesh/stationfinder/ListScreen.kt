package com.ganesh.stationfinder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    viewModel: StationViewModel,
    onStationClick: (OCMStation) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedConnectorFilter by remember { mutableStateOf<String?>(null) }
    var onlyAvailableFilter by remember { mutableStateOf(false) }
    
    // Track favorite toggles locally
    var favoriteUpdateTrigger by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Modern off-white background
    ) {
        // Search & Filter Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Search TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by station name or address") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0F766E),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Active filters row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Filters",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    
                    // Toggle for Only Available
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onlyAvailableFilter = !onlyAvailableFilter }
                    ) {
                        Checkbox(
                            checked = onlyAvailableFilter,
                            onCheckedChange = { onlyAvailableFilter = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0F766E))
                        )
                        Text("Only Available", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        when (uiState) {
            is StationUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF0F766E))
                }
            }
            is StationUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: ${(uiState as StationUiState.Error).message}",
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is StationUiState.Success -> {
                val allStations = (uiState as StationUiState.Success).stations
                
                // Get all unique connector types for filter chips
                val connectorTypes = remember(allStations) {
                    allStations.flatMap { it.connectorTypes ?: emptyList() }.distinct().sorted()
                }

                // Horizontal list of connector type filter chips
                if (connectorTypes.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedConnectorFilter == null,
                                onClick = { selectedConnectorFilter = null },
                                label = { Text("All Connectors") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0F766E),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        items(connectorTypes) { type ->
                            FilterChip(
                                selected = selectedConnectorFilter == type,
                                onClick = {
                                    selectedConnectorFilter = if (selectedConnectorFilter == type) null else type
                                },
                                label = { Text(type) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0F766E),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Filter stations list
                val filteredStations = remember(allStations, searchQuery, selectedConnectorFilter, onlyAvailableFilter, favoriteUpdateTrigger) {
                    allStations.filter { station ->
                        val matchesQuery = searchQuery.isEmpty() || 
                                station.name.contains(searchQuery, ignoreCase = true) ||
                                (station.address ?: "").contains(searchQuery, ignoreCase = true)
                        
                        val matchesConnector = selectedConnectorFilter == null ||
                                (station.connectorTypes ?: emptyList()).contains(selectedConnectorFilter)
                        
                        val matchesAvailability = !onlyAvailableFilter ||
                                (station.availableSlots ?: 0) > 0
                        
                        matchesQuery && matchesConnector && matchesAvailability
                    }
                }

                if (filteredStations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.EvStation,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No stations found matching filters",
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Try adjusting your filters, search queries, or zooming out on the map tab.",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredStations, key = { it.id }) { station ->
                            val isFavorited = FavoriteManager.isFavorite(context, station.id)
                            
                            StationRowItem(
                                station = station,
                                isFavorited = isFavorited,
                                onFavoriteToggle = {
                                    FavoriteManager.toggleFavorite(context, station.id)
                                    favoriteUpdateTrigger++
                                },
                                onClick = { onStationClick(station) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StationRowItem(
    station: OCMStation,
    isFavorited: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name, Favorite, & Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = station.operatorName,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorited) Color(0xFF0F766E) else Color.LightGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rating & Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (station.rating != null && station.rating > 0.0) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", station.rating),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Open/Closed badge
                station.isOpen?.let { isOpen ->
                    Box(
                        modifier = Modifier
                            .background(
                                if (isOpen) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isOpen) "Open" else "Closed",
                            color = if (isOpen) Color(0xFF15803D) else Color(0xFF991B1B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Distance Badge
                station.distance?.let { dist ->
                    Text(
                        text = "${String.format("%.2f", dist)} km away",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F766E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Divider line
            HorizontalDivider(color = Color(0xFFF1F5F9))

            Spacer(modifier = Modifier.height(12.dp))

            // Slots & Connectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Slots Count
                val total = station.totalSlots ?: 0
                val avail = station.availableSlots ?: 0
                Column {
                    Text(
                        text = if (total > 0) "$avail / $total Available" else "No Slots",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (avail > 0) Color(0xFF15803D) else Color(0xFF991B1B)
                    )
                    Text(
                        text = "Charging Slots",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                // Connector chips list (horizontal flow)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                    reverseLayout = true
                ) {
                    val connectors = station.connectorTypes ?: emptyList()
                    items(connectors) { type ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = type,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }
    }
}
