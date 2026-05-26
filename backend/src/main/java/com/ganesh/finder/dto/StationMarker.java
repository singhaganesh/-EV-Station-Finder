package com.ganesh.finder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationMarker {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private Boolean available;
}
