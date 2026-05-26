package com.ganesh.finder.config;

import com.ganesh.finder.service.StationImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ocm.sync.enabled", havingValue = "true", matchIfMissing = false)
public class StationSyncJob {

    private static final Logger log = LoggerFactory.getLogger(StationSyncJob.class);

    private final StationImportService stationImportService;

    @Value("${ocm.sync.radius-km:500}")
    private int radiusKm;

    public StationSyncJob(StationImportService stationImportService) {
        this.stationImportService = stationImportService;
    }

    /**
     * Daily sync at 3:00 AM.
     * Syncs stations around 5 major Indian cities.
     */
    @Scheduled(cron = "${ocm.sync.interval-cron:0 0 3 * * ?}")
    public void syncStations() {
        log.info("Starting scheduled OCM station sync...");

        int total = 0;

        // Mumbai
        total += stationImportService.importFromOCM(19.0760, 72.8777, radiusKm);

        // Bangalore
        total += stationImportService.importFromOCM(12.9716, 77.5946, radiusKm);

        // Delhi
        total += stationImportService.importFromOCM(28.7041, 77.1025, radiusKm);

        // Hyderabad
        total += stationImportService.importFromOCM(17.3850, 78.4867, radiusKm);

        // Chennai
        total += stationImportService.importFromOCM(13.0827, 80.2707, radiusKm);

        log.info("Scheduled OCM sync complete. Total new stations: {}", total);
    }
}
