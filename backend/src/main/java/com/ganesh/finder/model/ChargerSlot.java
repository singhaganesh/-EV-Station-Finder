package com.ganesh.finder.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "charger_slots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargerSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Station station;

    private String slotLabel;

    private String connectorType;

    private Double powerKw;

    private Boolean isAvailable;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
