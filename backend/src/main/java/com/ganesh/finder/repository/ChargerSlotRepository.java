package com.ganesh.finder.repository;

import com.ganesh.finder.model.ChargerSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChargerSlotRepository extends JpaRepository<ChargerSlot, Long> {

    List<ChargerSlot> findByStationId(Long stationId);

    List<ChargerSlot> findByStationIdAndIsAvailableTrue(Long stationId);

    long countByStationIdAndIsAvailableTrue(Long stationId);

    long countByStationId(Long stationId);

    void deleteByStationId(Long stationId);
}
