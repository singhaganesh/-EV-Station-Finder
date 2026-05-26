package com.ganesh.finder.controller;

import com.ganesh.finder.dto.ApiResponse;
import com.ganesh.finder.dto.StationMarker;
import com.ganesh.finder.dto.StationWithScore;
import com.ganesh.finder.service.StationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private static final Logger log = LoggerFactory.getLogger(StationController.class);
    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    /**
     * GET /api/stations/nearby?lat=19.0760&lng=72.8777&radius=50&limit=20
     */
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<?>> getNearbyStations(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "50") double radius,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            log.info("Received nearby search request: lat={}, lng={}, radius={}km, limit={}", lat, lng, radius, limit);
            List<StationWithScore> stations = stationService.getNearbyStations(lat, lng, radius, limit);
            log.info("Found {} stations near lat={}, lng={}", stations.size(), lat, lng);
            return ResponseEntity.ok(ApiResponse.success(
                    "Found " + stations.size() + " nearby stations", stations));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/stations/viewport?neLat=19.2&neLng=73.0&swLat=18.9&swLng=72.7
     */
    @GetMapping("/viewport")
    public ResponseEntity<ApiResponse<?>> getStationsInViewport(
            @RequestParam double neLat,
            @RequestParam double neLng,
            @RequestParam double swLat,
            @RequestParam double swLng) {
        try {
            List<StationMarker> markers = stationService.getStationsInViewport(neLat, neLng, swLat, swLng);
            return ResponseEntity.ok(ApiResponse.success(
                    "Found " + markers.size() + " stations in viewport", markers));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/stations/{id}/detail?lat=19.0760&lng=72.8777
     */
    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<?>> getStationDetail(
            @PathVariable Long id,
            @RequestParam double lat,
            @RequestParam double lng) {
        try {
            var detail = stationService.getStationDetail(id, lat, lng);
            if (detail.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("Station found", detail.get()));
            }
            return ResponseEntity.status(404).body(ApiResponse.error("Station not found"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/stations/search?q=nexon&lat=19.0760&lng=72.8777&radius=50
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<?>> searchStations(
            @RequestParam String q,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "50") double radius) {
        try {
            List<StationWithScore> results = stationService.searchStations(q, lat, lng, radius);
            return ResponseEntity.ok(ApiResponse.success(
                    "Found " + results.size() + " stations matching \"" + q + "\"", results));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/stations/count
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<?>> getStationCount() {
        try {
            long count = stationService.getStationCount();
            return ResponseEntity.ok(ApiResponse.success("OK", Map.of("count", count)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }
}
