package com.ganesh.stationfinder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.ganesh.stationfinder.util.FavoriteManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Preferences states
    var vehicleModel by remember { mutableStateOf(FavoriteManager.getVehicleModel(context)) }
    var batteryCapacity by remember { mutableStateOf(FavoriteManager.getBatteryCapacity(context)) }
    var range by remember { mutableStateOf(FavoriteManager.getRange(context)) }
    var preferredConnector by remember { mutableStateOf(FavoriteManager.getPreferredConnector(context)) }
    var minPower by remember { mutableStateOf(FavoriteManager.getMinPower(context)) }
    var onlyOpen by remember { mutableStateOf(FavoriteManager.getOnlyOpen(context)) }
    var onlyAvailable by remember { mutableStateOf(FavoriteManager.getOnlyAvailable(context)) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "My EV Profile",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Your Vehicle Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Your Vehicle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    // Model Name Input
                    OutlinedTextField(
                        value = vehicleModel,
                        onValueChange = { vehicleModel = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Model Name") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF0F766E)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0F766E),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    // Specs Row (Battery and Range)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = batteryCapacity,
                            onValueChange = { batteryCapacity = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Battery (kWh)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0F766E),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                        OutlinedTextField(
                            value = range,
                            onValueChange = { range = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Range (km)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0F766E),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }
            }

            // Preferred Connector Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Preferred Connector",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    val connectors = listOf("CCS2", "Type 2", "CHAdeMO", "Type 1")
                    
                    // Grid Layout using Columns/Rows
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (row in 0 until 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (col in 0 until 2) {
                                    val index = row * 2 + col
                                    val connector = connectors[index]
                                    val isSelected = preferredConnector == connector
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                border = BorderStroke(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF0F766E) else Color(0xFFE2E8F0)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .background(
                                                color = if (isSelected) Color(0xFFE0F2F1) else Color.White,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { preferredConnector = connector }
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = null,
                                                tint = if (isSelected) Color(0xFF0F766E) else Color.Gray
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = connector,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color(0xFF0F766E) else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Charging Preferences Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Charging Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    // Slider: Minimum Power Output
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Minimum Power Output", fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE0F2F1), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$minPower kW",
                                    color = Color(0xFF0F766E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = minPower.toFloat(),
                            onValueChange = { minPower = it.toInt() },
                            valueRange = 3f..150f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF0F766E),
                                activeTrackColor = Color(0xFF0F766E)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "3 kW", fontSize = 10.sp, color = Color.Gray)
                            Text(text = "150 kW", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Toggle: Only show open stations
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Only show open stations", fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                        Switch(
                            checked = onlyOpen,
                            onCheckedChange = { onlyOpen = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0F766E))
                        )
                    }

                    // Toggle: Only show available chargers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Only show available chargers", fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                        Switch(
                            checked = onlyAvailable,
                            onCheckedChange = { onlyAvailable = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0F766E))
                        )
                    }
                }
            }

            // Save Profile Button
            Button(
                onClick = {
                    FavoriteManager.setVehicleModel(context, vehicleModel)
                    FavoriteManager.setBatteryCapacity(context, batteryCapacity)
                    FavoriteManager.setRange(context, range)
                    FavoriteManager.setPreferredConnector(context, preferredConnector)
                    FavoriteManager.setMinPower(context, minPower)
                    FavoriteManager.setOnlyOpen(context, onlyOpen)
                    FavoriteManager.setOnlyAvailable(context, onlyAvailable)
                    onBackClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
            ) {
                Text(text = "SAVE PROFILE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
