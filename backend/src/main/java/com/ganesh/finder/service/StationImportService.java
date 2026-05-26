package com.ganesh.finder.service;

import com.ganesh.finder.model.ChargerSlot;
import com.ganesh.finder.model.Station;
import com.ganesh.finder.repository.ChargerSlotRepository;
import com.ganesh.finder.repository.StationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StationImportService {

    private static final Logger log = LoggerFactory.getLogger(StationImportService.class);

    @Value("${ocm.api.key}")
    private String ocmApiKey;

    @Value("${ocm.sync.radius-km:500}")
    private int defaultRadiusKm;

    @Value("${ocm.sync.country-id:101}")
    private int countryId;

    private final StationRepository stationRepository;
    private final ChargerSlotRepository chargerSlotRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public StationImportService(StationRepository stationRepository,
                                ChargerSlotRepository chargerSlotRepository) {
        this.stationRepository = stationRepository;
        this.chargerSlotRepository = chargerSlotRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetch stations from OCM within a radius of a center point.
     *
     * @param centerLat Center latitude
     * @param centerLng Center longitude
     * @param radiusKm  Search radius in kilometers
     * @return Number of stations imported
     */
    @Transactional
    public int importFromOCM(double centerLat, double centerLng, int radiusKm) {
        int imported = 0;
        int skipped = 0;

        try {
            String url = buildOCMUrl(centerLat, centerLng, radiusKm);
            log.info("Fetching OCM stations from: {}", url);

            // Set standard User-Agent header to avoid 403 Forbidden responses
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "station-finder-backend/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("OCM API returned status: {}", response.getStatusCode());
                return 0;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.isArray()) {
                log.error("OCM response is not an array");
                return 0;
            }

            for (JsonNode node : root) {
                try {
                    if (transformAndSave(node)) {
                        imported++;
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to import OCM station: {}", e.getMessage());
                    skipped++;
                }
            }

            log.info("OCM import complete: {} imported, {} skipped (total {})",
                    imported, skipped, root.size());

        } catch (Exception e) {
            log.error("OCM import failed: {}", e.getMessage(), e);
        }

        return imported;
    }

    private String buildOCMUrl(double lat, double lng, int radiusKm) {
        String baseUrl = "https://api.openchargemap.io/v3/poi";
        return baseUrl + "?key=" + URLEncoder.encode(ocmApiKey, StandardCharsets.UTF_8)
                + "&latitude=" + lat
                + "&longitude=" + lng
                + "&distance=" + radiusKm
                + "&distanceunit=KM"
                + "&maxresults=100"
                + "&compact=true"
                + "&verbose=false";
    }

    /**
     * Fetch stations from OCM for a specific country code.
     *
     * @param countryCode Country code (e.g. "IN")
     * @param maxResults  Maximum number of results to fetch
     * @return Number of stations imported
     */
    @Transactional
    public int importForCountry(String countryCode, int maxResults) {
        int imported = 0;
        int skipped = 0;

        try {
            String url = buildOCMCountryUrl(countryCode, maxResults);
            log.info("Fetching OCM stations for country {} from: {}", countryCode, url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "station-finder-backend/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("OCM API returned status: {}", response.getStatusCode());
                return 0;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.isArray()) {
                log.error("OCM response is not an array");
                return 0;
            }

            for (JsonNode node : root) {
                try {
                    if (transformAndSave(node)) {
                        imported++;
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to import OCM station: {}", e.getMessage());
                    skipped++;
                }
            }

            log.info("OCM country import complete: {} imported, {} skipped (total {})",
                    imported, skipped, root.size());

        } catch (Exception e) {
            log.error("OCM country import failed: {}", e.getMessage(), e);
        }

        return imported;
    }

    private String buildOCMCountryUrl(String countryCode, int maxResults) {
        String baseUrl = "https://api.openchargemap.io/v3/poi";
        return baseUrl + "?key=" + URLEncoder.encode(ocmApiKey, StandardCharsets.UTF_8)
                + "&countrycode=" + countryCode
                + "&maxresults=" + maxResults
                + "&compact=true"
                + "&verbose=false";
    }

    @Transactional
    public boolean transformAndSave(JsonNode ocmNode) {
        // Extract OCM ID for deduplication
        long ocmId = ocmNode.has("ID") && !ocmNode.get("ID").isNull()
                ? ocmNode.get("ID").asLong()
                : -1;

        if (ocmId == -1) {
            return false; // Skip stations without ID
        }

        // Check if already imported
        Optional<Station> existing = stationRepository.findByOcmId(ocmId);
        if (existing.isPresent()) {
            // Update lastSynced timestamp
            Station station = existing.get();
            station.setLastSynced(LocalDateTime.now());
            stationRepository.save(station);
            return false; // Already exists
        }

        // Extract AddressInfo
        JsonNode addressInfo = ocmNode.get("AddressInfo");
        if (addressInfo == null) {
            return false;
        }

        String name = addressInfo.has("Title") && !addressInfo.get("Title").isNull()
                ? addressInfo.get("Title").asText()
                : "Unknown Station";

        double latitude = addressInfo.has("Latitude") && !addressInfo.get("Latitude").isNull()
                ? addressInfo.get("Latitude").asDouble()
                : 0;

        double longitude = addressInfo.has("Longitude") && !addressInfo.get("Longitude").isNull()
                ? addressInfo.get("Longitude").asDouble()
                : 0;

        if (latitude == 0 && longitude == 0) {
            return false; // Skip stations with no coordinates
        }

        // Build address string
        StringBuilder addressBuilder = new StringBuilder();
        if (addressInfo.has("AddressLine1") && !addressInfo.get("AddressLine1").isNull()) {
            addressBuilder.append(addressInfo.get("AddressLine1").asText());
        }
        if (addressInfo.has("Town") && !addressInfo.get("Town").isNull()) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(addressInfo.get("Town").asText());
        }
        if (addressInfo.has("StateOrProvince") && !addressInfo.get("StateOrProvince").isNull()) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(addressInfo.get("StateOrProvince").asText());
        }
        String address = addressBuilder.length() > 0
                ? addressBuilder.toString()
                : "Address not available";

        // Extract OCM UUID
        String ocmUuid = ocmNode.has("UUID") && !ocmNode.get("UUID").isNull()
                ? ocmNode.get("UUID").asText()
                : null;

        // Build meta JSON
        StringBuilder metaBuilder = new StringBuilder();
        metaBuilder.append("{");
        metaBuilder.append("\"ocm_id\":").append(ocmId);
        if (ocmUuid != null) {
            metaBuilder.append(",\"ocm_uuid\":\"").append(escapeJson(ocmUuid)).append("\"");
        }

        // Operator info
        if (ocmNode.has("OperatorInfo") && !ocmNode.get("OperatorInfo").isNull()) {
            JsonNode operator = ocmNode.get("OperatorInfo");
            if (operator.has("Title") && !operator.get("Title").isNull()) {
                metaBuilder.append(",\"ocm_operator\":\"")
                        .append(escapeJson(operator.get("Title").asText())).append("\"");
            }
            if (operator.has("WebsiteURL") && !operator.get("WebsiteURL").isNull()) {
                metaBuilder.append(",\"ocm_website\":\"")
                        .append(escapeJson(operator.get("WebsiteURL").asText())).append("\"");
            }
        }

        // Usage type
        if (ocmNode.has("UsageType") && !ocmNode.get("UsageType").isNull()) {
            JsonNode usage = ocmNode.get("UsageType");
            if (usage.has("Title") && !usage.get("Title").isNull()) {
                metaBuilder.append(",\"ocm_usage_type\":\"")
                        .append(escapeJson(usage.get("Title").asText())).append("\"");
            }
        }

        // Comments
        if (ocmNode.has("GeneralComments") && !ocmNode.get("GeneralComments").isNull()) {
            metaBuilder.append(",\"ocm_comments\":\"")
                    .append(escapeJson(ocmNode.get("GeneralComments").asText())).append("\"");
        }

        metaBuilder.append(",\"source\":\"OCM\"");
        metaBuilder.append(",\"last_synced\":\"").append(LocalDateTime.now()).append("\"");
        metaBuilder.append("}");

        // Determine operating hours from usage type
        String operatingHours = "24 Hours";
        if (ocmNode.has("UsageType") && !ocmNode.get("UsageType").isNull()) {
            JsonNode usage = ocmNode.get("UsageType");
            String usageTitle = usage.has("Title") ? usage.get("Title").asText().toLowerCase() : "";
            if (!usageTitle.contains("pay") && !usageTitle.contains("public")) {
                operatingHours = "Check with operator";
            }
        }

        // Create Station entity
        Station station = Station.builder()
                .name(name)
                .latitude(latitude)
                .longitude(longitude)
                .address(address)
                .operatingHours(operatingHours)
                .pricePerKwh(0.0) // Unknown from OCM
                .rating(0.0)
                .isOpen(true)
                .ocmId(ocmId)
                .ocmUuid(ocmUuid)
                .meta(metaBuilder.toString())
                .lastSynced(LocalDateTime.now())
                .build();

        station = stationRepository.save(station);

        // Create ChargerSlots from OCM connection data
        List<ChargerSlot> slotsToCreate = new ArrayList<>();

        if (ocmNode.has("Connections") && ocmNode.get("Connections").isArray()) {
            for (JsonNode conn : ocmNode.get("Connections")) {
                int quantity = conn.has("Quantity") && !conn.get("Quantity").isNull()
                        ? conn.get("Quantity").asInt()
                        : 1;

                double powerKw = conn.has("PowerKW") && !conn.get("PowerKW").isNull()
                        ? conn.get("PowerKW").asDouble()
                        : 22.0;

                String connectorType = mapConnectorType(conn);
                String slotPrefix = connectorType.replaceAll("[^a-zA-Z0-9]", "");

                for (int i = 0; i < quantity; i++) {
                    ChargerSlot slot = ChargerSlot.builder()
                            .station(station)
                            .slotLabel(slotPrefix + " #" + (i + 1))
                            .connectorType(connectorType)
                            .powerKw(powerKw)
                            .isAvailable(true)
                            .build();
                    slotsToCreate.add(slot);
                }
            }
        }

        // If no connections, create 2 default CCS2 slots
        if (slotsToCreate.isEmpty()) {
            for (int i = 0; i < 2; i++) {
                ChargerSlot slot = ChargerSlot.builder()
                        .station(station)
                        .slotLabel("CCS2 #" + (i + 1))
                        .connectorType("CCS2")
                        .powerKw(60.0)
                        .isAvailable(true)
                        .build();
                slotsToCreate.add(slot);
            }
        }

        chargerSlotRepository.saveAll(slotsToCreate);
        log.info("Imported station: {} ({} slots)", name, slotsToCreate.size());
        return true;
    }

    private String mapConnectorType(JsonNode connection) {
        if (connection.has("ConnectionType") && !connection.get("ConnectionType").isNull()) {
            JsonNode connType = connection.get("ConnectionType");
            String title = connType.has("Title") && !connType.get("Title").isNull()
                    ? connType.get("Title").asText().toLowerCase()
                    : "";

            if (title.contains("ccs") || title.contains("combo")) {
                return "CCS2";
            }
            if (title.contains("type 2") || title.contains("type2") || title.contains("mennekes")) {
                return "Type 2";
            }
            if (title.contains("chademo")) {
                return "CHAdeMO";
            }
            if (title.contains("type 1") || title.contains("type1") || title.contains("j1772")) {
                return "Type 1";
            }
        }

        // Default based on power level
        if (connection.has("PowerKW") && !connection.get("PowerKW").isNull()) {
            double power = connection.get("PowerKW").asDouble();
            return power > 22 ? "CCS2" : "Type 2";
        }

        return "CCS2";
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
