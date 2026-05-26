package com.ganesh.stationfinder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
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

@Composable
fun SavedScreen(
    viewModel: StationViewModel,
    onStationClick: (OCMStation) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Trigger recomposition when a favorite is toggled
    var favoriteUpdateTrigger by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
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
                
                // Get the favorited IDs
                val favoriteIds = remember(favoriteUpdateTrigger) {
                    FavoriteManager.getFavorites(context)
                }

                // Filter stations list by favorite IDs
                val savedStations = remember(allStations, favoriteIds) {
                    allStations.filter { favoriteIds.contains(it.id) }
                }

                if (savedStations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No favorite stations yet",
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Bookmark your preferred charging points in the map or list tabs to access them instantly here.",
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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(savedStations, key = { it.id }) { station ->
                            StationRowItem(
                                station = station,
                                isFavorited = true,
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
