package com.ganesh.finder;

import com.ganesh.finder.service.StationImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class StationImporterApp {

    private static final Logger log = LoggerFactory.getLogger(StationImporterApp.class);

    public static void main(String[] args) {
        // Disable automatic seeding and job scheduling to run only the country-wide importer
        System.setProperty("app.seeding.enabled", "false");
        System.setProperty("ocm.sync.enabled", "false");

        SpringApplication app = new SpringApplication(StationImporterApp.class);
        app.setBannerMode(Banner.Mode.OFF);

        ConfigurableApplicationContext context = app.run(args);

        log.info("====================================");
        log.info("🚀 STANDALONE STATION IMPORTER STARTING");
        log.info("====================================");

        try {
            StationImportService importer = context.getBean(StationImportService.class);
            log.info("Executing India station import...");
            int count = importer.importForCountry("IN", 5000);
            log.info("====================================");
            log.info("🎉 SUCCESS: Imported {} stations from India into Supabase!", count);
            log.info("====================================");
        } catch (Exception e) {
            log.error("❌ Importer encountered an error: {}", e.getMessage(), e);
        } finally {
            context.close();
            log.info("Context closed. Exiting.");
        }
    }
}
