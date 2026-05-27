package com.ganesh.finder.service;

import com.ganesh.finder.dto.StationMarker;
import com.ganesh.finder.dto.StationWithScore;
import com.ganesh.finder.model.ChargerSlot;
import com.ganesh.finder.model.Station;
import com.ganesh.finder.model.Review;
import com.ganesh.finder.repository.ChargerSlotRepository;
import com.ganesh.finder.repository.StationRepository;
import com.ganesh.finder.repository.ReviewRepository;
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
    private final ReviewRepository reviewRepository;

    public StationService(StationRepository stationRepository,
                          ChargerSlotRepository chargerSlotRepository,
                          ReviewRepository reviewRepository) {
        this.stationRepository = stationRepository;
        this.chargerSlotRepository = chargerSlotRepository;
        this.reviewRepository = reviewRepository;
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

    /**
     * Get reviews for a station.
     */
    public List<Review> getReviewsForStation(Long stationId) {
        return reviewRepository.findByStationIdOrderByCreatedAtDesc(stationId);
    }

    /**
     * Add a review for a station and recalculate its average rating.
     */
    @org.springframework.transaction.annotation.Transactional
    public Review addReviewForStation(Long stationId, String reviewerName, double rating, String comment) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Station not found with id: " + stationId));

        Review review = Review.builder()
                .station(station)
                .reviewerName(reviewerName)
                .rating(rating)
                .comment(comment)
                .build();

        review = reviewRepository.save(review);

        // Recalculate average rating
        Double avgRating = reviewRepository.getAverageRatingForStation(stationId);
        if (avgRating != null) {
            // Round to 1 decimal place
            double roundedRating = Math.round(avgRating * 10.0) / 10.0;
            station.setRating(roundedRating);
            stationRepository.save(station);
            log.info("Updated station {} average rating to {}", stationId, roundedRating);
        }

        return review;
    }

    /**
     * Get stations along a route.
     */
    public List<StationWithScore> getStationsAlongRoute(String waypointsStr, String connectorType, double bufferKm) {
        List<double[]> waypoints = new ArrayList<>();
        if (waypointsStr.contains("|")) {
            String[] parts = waypointsStr.split("\\|");
            for (String part : parts) {
                String[] coords = part.split(",");
                if (coords.length >= 2) {
                    try {
                        double lat = Double.parseDouble(coords[0]);
                        double lng = Double.parseDouble(coords[1]);
                        waypoints.add(new double[]{lat, lng});
                    } catch (NumberFormatException ignored) {}
                }
            }
        } else {
            waypoints = decodePolyline(waypointsStr);
        }

        if (waypoints.isEmpty()) {
            return new ArrayList<>();
        }

        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE, maxLng = -Double.MAX_VALUE;
        for (double[] pt : waypoints) {
            minLat = Math.min(minLat, pt[0]);
            maxLat = Math.max(maxLat, pt[0]);
            minLng = Math.min(minLng, pt[1]);
            maxLng = Math.max(maxLng, pt[1]);
        }

        // Expand bounding box by bufferKm
        double latDelta = bufferKm / 111.0;
        double lngDelta = bufferKm / (111.0 * Math.cos(Math.toRadians((minLat + maxLat) / 2.0)));

        List<Station> stations = stationRepository.findStationsInViewport(
                minLat - latDelta, maxLat + latDelta,
                minLng - lngDelta, maxLng + lngDelta);

        List<StationWithScore> results = new ArrayList<>();
        for (Station station : stations) {
            double minDistance = Double.MAX_VALUE;
            for (int i = 0; i < waypoints.size() - 1; i++) {
                double[] pA = waypoints.get(i);
                double[] pB = waypoints.get(i + 1);
                double dist = distanceToSegment(station.getLatitude(), station.getLongitude(), pA[0], pA[1], pB[0], pB[1]);
                minDistance = Math.min(minDistance, dist);
            }

            if (waypoints.size() == 1) {
                double[] pt = waypoints.get(0);
                minDistance = calculateDistance(station.getLatitude(), station.getLongitude(), pt[0], pt[1]);
            }

            if (minDistance <= bufferKm) {
                StationWithScore enriched = enrichWithScore(station, station.getLatitude(), station.getLongitude());
                enriched.setDistance(Math.round(minDistance * 100.0) / 100.0); // round to 2 decimal places

                if (connectorType == null || connectorType.trim().isEmpty() ||
                        (enriched.getConnectorTypes() != null && enriched.getConnectorTypes().stream().anyMatch(c -> c.equalsIgnoreCase(connectorType.trim())))) {
                    results.add(enriched);
                }
            }
        }

        results.sort(Comparator.comparingDouble(StationWithScore::getDistance));
        return results;
    }

    private double distanceToSegment(double latS, double lngS, double latA, double lngA, double latB, double lngB) {
        double xS = lngS, yS = latS;
        double xA = lngA, yA = latA;
        double xB = lngB, yB = latB;

        double dx = xB - xA;
        double dy = yB - yA;

        if (dx == 0 && dy == 0) {
            return calculateDistance(latS, lngS, latA, lngA);
        }

        double t = ((xS - xA) * dx + (yS - yA) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t)); // clamp to segment

        double projLat = latA + t * (latB - latA);
        double projLng = lngA + t * (lngB - lngA);

        return calculateDistance(latS, lngS, projLat, projLng);
    }

    private List<double[]> decodePolyline(String encoded) {
        List<double[]> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            double latitude = lat / 1E5;
            double longitude = lng / 1E5;
            poly.add(new double[]{latitude, longitude});
        }
        return poly;
    }
}
