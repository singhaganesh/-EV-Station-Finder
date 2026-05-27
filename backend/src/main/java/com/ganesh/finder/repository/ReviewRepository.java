package com.ganesh.finder.repository;

import com.ganesh.finder.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByStationIdOrderByCreatedAtDesc(Long stationId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.station.id = :stationId")
    Double getAverageRatingForStation(@Param("stationId") Long stationId);
}
