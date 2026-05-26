package com.ganesh.finder.controller;

import com.ganesh.finder.dto.ApiResponse;
import com.ganesh.finder.service.StationImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final StationImportService stationImportService;

    public ImportController(StationImportService stationImportService) {
        this.stationImportService = stationImportService;
    }

    /**
     * POST /api/import/trigger?lat=19.0760&lng=72.8777&radius=50
     * Manually trigger an OCM import.
     */
    @PostMapping("/trigger")
    public ResponseEntity<ApiResponse<?>> triggerImport(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "50") int radius) {
        try {
            int count = stationImportService.importFromOCM(lat, lng, radius);
            return ResponseEntity.ok(ApiResponse.success(
                    "Imported " + count + " stations", Map.of("imported", count)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Import failed: " + e.getMessage()));
        }
    }
}
