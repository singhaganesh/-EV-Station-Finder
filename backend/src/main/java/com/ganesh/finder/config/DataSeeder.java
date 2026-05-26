package com.ganesh.finder.config;

import com.ganesh.finder.model.ChargerSlot;
import com.ganesh.finder.model.Station;
import com.ganesh.finder.repository.ChargerSlotRepository;
import com.ganesh.finder.repository.StationRepository;
import com.ganesh.finder.service.StationImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final StationImportService stationImportService;
    private final StationRepository stationRepository;
    private final ChargerSlotRepository chargerSlotRepository;

    @Value("${app.seeding.enabled:true}")
    private boolean seedingEnabled;

    public DataSeeder(StationImportService stationImportService, 
                      StationRepository stationRepository,
                      ChargerSlotRepository chargerSlotRepository) {
        this.stationImportService = stationImportService;
        this.stationRepository = stationRepository;
        this.chargerSlotRepository = chargerSlotRepository;
    }

    @Override
    public void run(String... args) {
        if (!seedingEnabled) {
            log.info("====================================");
            log.info("🔍 DataSeeder: Seeding is disabled via configuration. No station data will be inserted.");
            log.info("====================================");
            return;
        }

        long currentCount = stationRepository.count();
        if (currentCount > 0) {
            log.info("====================================");
            log.info("🔍 DataSeeder: Database already contains {} stations. Skipping seeding.", currentCount);
            log.info("====================================");
            return;
        }

        log.info("====================================");
        log.info("🔍 STATION FINDER DATA SEEDER STARTED");
        log.info("====================================");

        int total = 0;
        try {
            // Import stations for major Indian cities on first run
            // Using radius 50km for each city center

            log.info("🗺️ Importing stations from Mumbai...");
            int mumbaiCount = stationImportService.importFromOCM(19.0760, 72.8777, 50);
            log.info("✅ Imported {} stations from Mumbai region", mumbaiCount);

            log.info("🗺️ Importing stations from Bangalore...");
            int bangaloreCount = stationImportService.importFromOCM(12.9716, 77.5946, 50);
            log.info("✅ Imported {} stations from Bangalore region", bangaloreCount);

            log.info("🗺️ Importing stations from Delhi...");
            int delhiCount = stationImportService.importFromOCM(28.7041, 77.1025, 50);
            log.info("✅ Imported {} stations from Delhi region", delhiCount);

            log.info("🗺️ Importing stations from Hyderabad...");
            int hyderabadCount = stationImportService.importFromOCM(17.3850, 78.4867, 50);
            log.info("✅ Imported {} stations from Hyderabad region", hyderabadCount);

            log.info("🗺️ Importing stations from Chennai...");
            int chennaiCount = stationImportService.importFromOCM(13.0827, 80.2707, 50);
            log.info("✅ Imported {} stations from Chennai region", chennaiCount);

            total = mumbaiCount + bangaloreCount + delhiCount + hyderabadCount + chennaiCount;
            log.info("====================================");
            log.info("🎉 OCM SEEDING COMPLETE: {} total stations imported", total);
            log.info("====================================");

        } catch (Exception e) {
            log.warn("⚠️ OCM Seeding failed or was partially completed: {}", e.getMessage());
        }

        // Fallback to mock data if OCM returned absolutely nothing
        if (stationRepository.count() == 0) {
            seedMockData();
        }
    }

    private void seedMockData() {
        log.info("⚠️ No stations imported from OCM (possibly offline or rate-limited). Seeding high-quality offline mock stations for development...");

        // Mumbai 1
        Station m1 = Station.builder()
                .name("Tata Power Charging Station - Bandra")
                .latitude(19.0596)
                .longitude(72.8295)
                .address("Bandra West, Mumbai, Maharashtra 400050")
                .pricePerKwh(18.5)
                .rating(4.5)
                .isOpen(true)
                .operatingHours("24 Hours")
                .meta("{\"source\":\"Offline Mock\",\"ocm_operator\":\"Tata Power\"}")
                .lastSynced(LocalDateTime.now())
                .build();
        m1 = stationRepository.save(m1);
        chargerSlotRepository.save(ChargerSlot.builder().station(m1).slotLabel("CCS2 #1").connectorType("CCS2").powerKw(60.0).isAvailable(true).build());
        chargerSlotRepository.save(ChargerSlot.builder().station(m1).slotLabel("CCS2 #2").connectorType("CCS2").powerKw(60.0).isAvailable(true).build());
        chargerSlotRepository.save(ChargerSlot.builder().station(m1).slotLabel("Type 2 #1").connectorType("Type 2").powerKw(22.0).isAvailable(false).build());

        // Mumbai 2
        Station m2 = Station.builder()
                .name("Ather Grid Charging Station - Lower Parel")
                .latitude(18.9953)
                .longitude(72.8257)
                .address("Senapati Bapat Marg, Lower Parel, Mumbai, Maharashtra 400013")
                .pricePerKwh(15.0)
                .rating(4.2)
                .isOpen(true)
                .operatingHours("08:00 AM - 10:00 PM")
                .meta("{\"source\":\"Offline Mock\",\"ocm_operator\":\"Ather Energy\"}")
                .lastSynced(LocalDateTime.now())
                .build();
        m2 = stationRepository.save(m2);
        chargerSlotRepository.save(ChargerSlot.builder().station(m2).slotLabel("Type 2 #1").connectorType("Type 2").powerKw(11.0).isAvailable(true).build());
        chargerSlotRepository.save(ChargerSlot.builder().station(m2).slotLabel("Type 2 #2").connectorType("Type 2").powerKw(11.0).isAvailable(true).build());

        // Bangalore 1
        Station b1 = Station.builder()
                .name("Tata Power Charging Station - Indiranagar")
                .latitude(12.9719)
                .longitude(77.6412)
                .address("100 Feet Rd, Indiranagar, Bengaluru, Karnataka 560038")
                .pricePerKwh(19.0)
                .rating(4.7)
                .isOpen(true)
                .operatingHours("24 Hours")
                .meta("{\"source\":\"Offline Mock\",\"ocm_operator\":\"Tata Power\"}")
                .lastSynced(LocalDateTime.now())
                .build();
        b1 = stationRepository.save(b1);
        chargerSlotRepository.save(ChargerSlot.builder().station(b1).slotLabel("CCS2 #1").connectorType("CCS2").powerKw(50.0).isAvailable(true).build());
        chargerSlotRepository.save(ChargerSlot.builder().station(b1).slotLabel("CCS2 #2").connectorType("CCS2").powerKw(50.0).isAvailable(false).build());
        chargerSlotRepository.save(ChargerSlot.builder().station(b1).slotLabel("Type 2 #1").connectorType("Type 2").powerKw(22.0).isAvailable(true).build());

        // Bangalore 2
        Station b2 = Station.builder()
                .name("Jio-bp Pulse Charging Hub - Whitefield")
                .latitude(12.9698)
                .longitude(77.7499)
                .address("Whitefield, Bengaluru, Karnataka 560066")
                .pricePerKwh(17.5)
                .rating(4.4)
                .isOpen(true)
                .operatingHours("24 Hours")
                .meta("{\"source\":\"Offline Mock\",\"ocm_operator\":\"Jio-bp Pulse\"}")
                .lastSynced(LocalDateTime.now())
                .build();
        b2 = stationRepository.save(b2);
        chargerSlotRepository.save(ChargerSlot.builder().station(b2).slotLabel("CCS2 #1").connectorType("CCS2").powerKw(120.0).isAvailable(true).build());
        chargerSlotRepository.save(ChargerSlot.builder().station(b2).slotLabel("CCS2 #2").connectorType("CCS2").powerKw(120.0).isAvailable(true).build());

        // Delhi 1
        Station d1 = Station.builder()
                .name("Kazam EV Charging Station - Connaught Place")
                .latitude(28.6304)
                .longitude(77.2177)
                .address("Connaught Place, New Delhi, Delhi 110001")
                .pricePerKwh(16.0)
                .rating(4.0)
                .isOpen(true)
                .operatingHours("24 Hours")
                .meta("{\"source\":\"Offline Mock\",\"ocm_operator\":\"Kazam EV\"}")
                .lastSynced(LocalDateTime.now())
                .build();
        d1 = stationRepository.save(d1);
        chargerSlotRepository.save(ChargerSlot.builder().station(d1).slotLabel("Type 2 #1").connectorType("Type 2").powerKw(7.4).isAvailable(true).build());
        chargerSlotRepository.save(ChargerSlot.builder().station(d1).slotLabel("Type 2 #2").connectorType("Type 2").powerKw(7.4).isAvailable(true).build());

        // Delhi 2
        Station d2 = Station.builder()
                .name("Tata Power Charging Station - Dwarka")
                .latitude(28.5823)
                .longitude(77.0500)
                .address("Sector 10, Dwarka, New Delhi, Delhi 110075")
                .pricePerKwh(21.0)
                .rating(4.3)
                .isOpen(true)
                .operatingHours("24 Hours")
                .meta("{\"source\":\"Offline Mock\",\"ocm_operator\":\"Tata Power\"}")
                .lastSynced(LocalDateTime.now())
                .build();
        d2 = stationRepository.save(d2);
        chargerSlotRepository.save(ChargerSlot.builder().station(d2).slotLabel("CCS2 #1").connectorType("CCS2").powerKw(60.0).isAvailable(true).build());

        // Chennai 1
        Station c1 = Station.builder()
                .name("Zeon Charging Station - T. Nagar")
                .latitude(13.0418)
                .longitude(80.2341)
                .address("T. Nagar, Chennai, Tamil Nadu 600017")
                .pricePerKwh(22.0)
                .rating(4.8)
                .isOpen(true)
                .operatingHours("24 Hours")
                .meta("{\"source\":\"Offline Mock\",\"ocm_operator\":\"Zeon Charging\"}")
                .lastSynced(LocalDateTime.now())
                .build();
        c1 = stationRepository.save(c1);
        chargerSlotRepository.save(ChargerSlot.builder().station(c1).slotLabel("CCS2 #1").connectorType("CCS2").powerKw(50.0).isAvailable(true).build());
        chargerSlotRepository.save(ChargerSlot.builder().station(c1).slotLabel("Type 2 #1").connectorType("Type 2").powerKw(22.0).isAvailable(true).build());

        // Hyderabad 1
        Station h1 = Station.builder()
                .name("Fortum Charge & Drive - Jubilee Hills")
                .latitude(17.4325)
                .longitude(78.4072)
                .address("Road No. 36, Jubilee Hills, Hyderabad, Telangana 500033")
                .pricePerKwh(18.0)
                .rating(4.6)
                .isOpen(true)
                .operatingHours("24 Hours")
                .meta("{\"source\":\"Offline Mock\",\"ocm_operator\":\"Fortum\"}")
                .lastSynced(LocalDateTime.now())
                .build();
        h1 = stationRepository.save(h1);
        chargerSlotRepository.save(ChargerSlot.builder().station(h1).slotLabel("CCS2 #1").connectorType("CCS2").powerKw(60.0).isAvailable(true).build());
        chargerSlotRepository.save(ChargerSlot.builder().station(h1).slotLabel("CCS2 #2").connectorType("CCS2").powerKw(60.0).isAvailable(false).build());

        log.info("✅ Offline mock seeding complete. 8 stations successfully seeded into Supabase!");
    }
}
