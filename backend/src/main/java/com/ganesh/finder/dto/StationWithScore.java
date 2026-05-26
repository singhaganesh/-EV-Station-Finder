package com.ganesh.finder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationWithScore {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private String address;
    private String operatingHours;
    private Double pricePerKwh;
    private Double rating;
    private Boolean isOpen;
    private String meta;

    // Computed fields
    private Double distance;
    private Integer availableSlots;
    private Integer totalSlots;
    private List<String> connectorTypes;
    private List<SlotInfo> slots;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotInfo {
        private Long id;
        private String label;
        private String connectorType;
        private Double powerKw;
        private Boolean isAvailable;
    }
}
