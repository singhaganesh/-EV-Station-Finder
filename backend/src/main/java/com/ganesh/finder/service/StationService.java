package com.ganesh.finder.service;

import com.ganesh.finder.dto.StationMarker;
import com.ganesh.finder.dto.StationWithScore;
import com.ganesh.finder.model.ChargerSlot;
import com.ganesh.finder.model.Station;
import com.ganesh.finder.repository.ChargerSlotRepository;
import com.ganesh.finder.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StationService {

    private static final Logger log = LoggerFactory.getLogger(StationService.class);

    private final StationRepository stationRepository;
    private final ChargerSlotRepository chargerSlotRepository;

    public StationService(StationRepository stationRepository,
                          ChargerSlotRepository chargerSlotRepository) {
        this.stationRepository = stationRepository;
        this.chargerSlotRepository = chargerSlotRepository;
    }

    /**
     * Get lightweight markers for stations in a viewport.
     */
    public List<StationMarker> getStationsInViewport(double neLat, double neLng,
                                                      double swLat, double swLng) {
        List<Station> stations = stationRepository.findStationsInViewport(
                Math.min(swLat, neLat), Math.max(swLat, neLat),
                Math.min(swLng, neLng), Math.max(swLng, neLng));

        return stations.stream().map(station -> {
            long availableSlots = chargerSlotRepository.countByStationIdAndIsAvailableTrue(station.getId());
            return StationMarker.builder()
                    .id(station.getId())
                    .name(station.getName())
                    .latitude(station.getLatitude())
                    .longitude(station.getLongitude())
                    .available(availableSlots > 0)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Get nearby stations ranked by distance.
     */
    public List<StationWithScore> getNearbyStations(double lat, double lng,
                                                     double radiusKm, int limit) {
        // Calculate approximate bounding box
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        List<Station> stations = stationRepository.findStationsInViewport(
                lat - latDelta, lat + latDelta,
                lng - lngDelta, lng + lngDelta);

        return stations.stream()
                .map(station -> enrichWithScore(station, lat, lng))
                .filter(s -> s.getDistance() <= radiusKm)
                .sorted(Comparator.comparingDouble(StationWithScore::getDistance))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get detailed info for a single station.
     */
    public Optional<StationWithScore> getStationDetail(Long id, double userLat, double userLng) {
        return stationRepository.findById(id)
                .map(station -> enrichWithScore(station, userLat, userLng));
    }

    /**
     * Search stations by name or address.
     */
    public List<StationWithScore> searchStations(String query, double lat, double lng, double radiusKm) {
        List<Station> stations = stationRepository.searchByNameOrAddress(query.toLowerCase().trim());

        return stations.stream()
                .map(station -> enrichWithScore(station, lat, lng))
                .filter(s -> s.getDistance() <= radiusKm)
                .sorted(Comparator.comparingDouble(StationWithScore::getDistance))
                .collect(Collectors.toList());
    }

    /**
     * Get total station count.
     */
    public long getStationCount() {
        return stationRepository.count();
    }

    /**
     * Enrich a Station entity with scoring and slot data.
     */
    private StationWithScore enrichWithScore(Station station, double userLat, double userLng) {
        // Calculate distance using Haversine formula
        double distance = calculateDistance(userLat, userLng, station.getLatitude(), station.getLongitude());

        // Get slot data
        List<ChargerSlot> slots = chargerSlotRepository.findByStationId(station.getId());
        long availableCount = slots.stream().filter(ChargerSlot::getIsAvailable).count();

        // Extract unique connector types
        List<String> connectorTypes = slots.stream()
                .map(ChargerSlot::getConnectorType)
                .distinct()
                .collect(Collectors.toList());

        // Slot details
        List<StationWithScore.SlotInfo> slotInfos = slots.stream()
                .map(slot -> StationWithScore.SlotInfo.builder()
                        .id(slot.getId())
                        .label(slot.getSlotLabel())
                        .connectorType(slot.getConnectorType())
                        .powerKw(slot.getPowerKw())
                        .isAvailable(slot.getIsAvailable())
                        .build())
                .collect(Collectors.toList());

        return StationWithScore.builder()
                .id(station.getId())
                .name(station.getName())
                .latitude(station.getLatitude())
                .longitude(station.getLongitude())
                .address(station.getAddress())
                .operatingHours(station.getOperatingHours())
                .pricePerKwh(station.getPricePerKwh())
                .rating(station.getRating())
                .isOpen(station.getIsOpen())
                .meta(station.getMeta())
                .distance(distance)
                .totalSlots(slots.size())
                .availableSlots((int) availableCount)
                .connectorTypes(connectorTypes)
                .slots(slotInfos)
                .build();
    }

    /**
     * Haversine distance between two lat/lng points in kilometers.
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371; // Earth's radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
