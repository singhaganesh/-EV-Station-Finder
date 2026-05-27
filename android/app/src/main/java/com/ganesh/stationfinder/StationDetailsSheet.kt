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
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Bookmark
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationDetailsSheet(
    station: OCMStation,
    viewModel: StationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val reviews by viewModel.reviews.collectAsState()
    val isSubmitting by viewModel.isSubmittingReview.collectAsState()
    
    // Load reviews on sheet display
    LaunchedEffect(station.id) {
        viewModel.fetchReviews(station.id)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Name and Operator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFE0F2F1), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EvStation, null, tint = Color(0xFF0F766E))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = station.operatorName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        if (station.rating != null && station.rating > 0.0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format("%.1f", station.rating),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                var isFavorited by remember { mutableStateOf(FavoriteManager.isFavorite(context, station.id)) }
                IconButton(
                    onClick = {
                        isFavorited = FavoriteManager.toggleFavorite(context, station.id)
                    }
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorited) Color(0xFF0F766E) else Color.LightGray,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pricing and Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                station.pricePerKwh?.let { price ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "₹${String.format("%.2f", price)} / kWh",
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
                
                station.isOpen?.let { isOpen ->
                    Box(
                        modifier = Modifier
                            .background(
                                if (isOpen) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isOpen) "Open" else "Closed",
                            color = if (isOpen) Color(0xFF15803D) else Color(0xFF991B1B),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Address Section
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Map, 
                    null, 
                    modifier = Modifier.size(20.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = station.address ?: "Address not available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF1E293B)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Connectors Section
            Text(
                text = "Connectors & Slots",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            if (!station.slots.isNullOrEmpty()) {
                station.slots.forEach { slot ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = slot.label ?: "Charger Slot",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFF1E293B)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val isAvail = slot.isAvailable == true
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isAvail) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isAvail) "Available" else "In Use",
                                            color = if (isAvail) Color(0xFF15803D) else Color(0xFF991B1B),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = slot.connectorType ?: "Unknown Connector",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.DarkGray
                                )
                            }
                            if (slot.powerKw != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    val formattedPower = slot.powerKw.let { p ->
                                        val rounded = Math.round(p * 10.0) / 10.0
                                        if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
                                    }
                                    Text(
                                        text = "$formattedPower kW",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF0F766E)
                                    )
                                    Text(
                                        text = "Power Output",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text("No charger slots available", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action: Navigate Button
            Button(
                onClick = {
                    val lat = station.latitude
                    val lng = station.longitude
                    val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    context.startActivity(mapIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
            ) {
                Icon(Icons.Default.Directions, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Navigation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Reviews & Ratings Header
            Text(
                text = "User Reviews & Ratings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Submit Review Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Write a Review",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF1E293B)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Star Rating selector
                    var ratingInput by remember { mutableStateOf(5) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = if (i <= ratingInput) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { ratingInput = i }
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$ratingInput / 5 Stars",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F766E)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reviewer Name Field
                    var reviewerNameInput by remember { mutableStateOf(FavoriteManager.getVehicleModel(context)) }
                    OutlinedTextField(
                        value = reviewerNameInput,
                        onValueChange = { reviewerNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Your Name / Vehicle Model") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0F766E),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Comment Field
                    var commentInput by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Share your experience...") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0F766E),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (reviewerNameInput.isNotBlank()) {
                                viewModel.submitReview(
                                    stationId = station.id,
                                    reviewerName = reviewerNameInput,
                                    rating = ratingInput.toDouble(),
                                    comment = commentInput
                                ) {
                                    commentInput = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text("Submit Review", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reviews List
            if (reviews.isNotEmpty()) {
                reviews.forEach { review ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = review.reviewerName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = review.rating.toInt().toString(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                            if (!review.comment.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = review.comment,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.DarkGray
                                )
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
                        text = "No reviews yet. Be the first to review!",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
