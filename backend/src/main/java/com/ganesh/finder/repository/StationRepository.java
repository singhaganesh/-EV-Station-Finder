package com.ganesh.finder.repository;

import com.ganesh.finder.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {

    // Find stations within a bounding box (viewport)
    @Query("SELECT s FROM Station s WHERE s.latitude BETWEEN :swLat AND :neLat AND s.longitude BETWEEN :swLng AND :neLng")
    List<Station> findStationsInViewport(
        @Param("swLat") double swLat,
        @Param("neLat") double neLat,
        @Param("swLng") double swLng,
        @Param("neLng") double neLng
    );

    // Search by name or address (case-insensitive)
    @Query("SELECT s FROM Station s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.address) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Station> searchByNameOrAddress(@Param("query") String query);

    // Check if station already imported by OCM ID
    Optional<Station> findByOcmId(Long ocmId);
}
