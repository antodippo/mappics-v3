package com.antodippo.mappics.infrastructure;

import com.antodippo.mappics.domain.*;
import com.antodippo.mappics.infrastructure.storage.GalleryFileStorageInMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Component
@Profile("local")
public class LocalDevSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalDevSeeder.class);

    private final GalleryFileStorageInMemory fileStorage;
    private final GalleryRepository repository;
    private final ExifExtractor exifExtractor;

    @Value("${mappics.local.galleries-path:src/test/resources/galleries}")
    private String galleriesPath;

    public LocalDevSeeder(GalleryFileStorageInMemory fileStorage,
                          GalleryRepository repository,
                          ExifExtractor exifExtractor) {
        this.fileStorage   = fileStorage;
        this.repository    = repository;
        this.exifExtractor = exifExtractor;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        File galleriesDir = new File(galleriesPath);
        if (!galleriesDir.exists()) {
            log.warn("Local galleries directory not found at '{}' — skipping seed. " +
                     "Set mappics.local.galleries-path to a directory with JPEG sub-folders.",
                     galleriesDir.getAbsolutePath());
            return;
        }

        log.info("Seeding local dev data from {}…", galleriesDir.getAbsolutePath());
        Map<String, List<GpsCoordinates>> galleryGps = new LinkedHashMap<>();

        Files.walk(galleriesDir.toPath())
             .filter(p -> isJpeg(p.getFileName().toString()))
             .sorted()
             .forEach(p -> seedPicture(p, galleriesDir.toPath(), galleryGps));

        for (Map.Entry<String, List<GpsCoordinates>> entry : galleryGps.entrySet()) {
            String galleryId = entry.getKey();
            List<String> pictureIds = repository.findPicturesByGalleryId(galleryId)
                    .stream().map(Picture::getId).toList();
            Gallery gallery = Gallery.create(galleryId)
                    .withPictureIds(pictureIds)
                    .withAverageGpsCoordinates(
                            Gallery.calculateAverageGps(entry.getValue()).orElseThrow());
            repository.save(gallery);
        }

        log.info("Local dev seed complete — {} galleries, {} pictures",
                galleryGps.size(),
                galleryGps.values().stream().mapToInt(List::size).sum());
    }

    private void seedPicture(Path jpeg, Path galleriesRoot,
                             Map<String, List<GpsCoordinates>> galleryGps) {
        Path relative = galleriesRoot.relativize(jpeg);
        if (relative.getNameCount() != 2) return;

        String galleryId = relative.getName(0).toString();
        String filename  = relative.getName(1).toString();

        try {
            byte[] bytes = Files.readAllBytes(jpeg);
            ExifExtractionResult exif = exifExtractor.extract(bytes);

            if (exif.gpsCoordinates() == null) {
                log.warn("No GPS in {}/{} — skipping", galleryId, filename);
                return;
            }

            // Store source bytes so LocalImageController can serve them.
            fileStorage.addPicture(galleryId, filename, bytes);

            // In local dev both thumbnail and full-size point to the original JPEG —
            // no resizing at startup keeps seed time under 100ms.
            String localUrl = "http://localhost:8081/local-images/" + galleryId + "/" + filename;

            String pictureId = galleryId + "/" + filename;
            Picture picture = Picture.create(pictureId, galleryId, filename)
                    .withGpsCoordinates(exif.gpsCoordinates())
                    .withExifData(exif.exifData())
                    .withProcessedImages(localUrl, localUrl)
                    .withLocationDescription(new LocationDescription(galleryId, galleryId + " area"))
                    .withWeatherData(new WeatherData(18.0, 60, 12.0, 1, "Mainly clear"));

            repository.savePicture(picture);
            galleryGps.computeIfAbsent(galleryId, k -> new ArrayList<>())
                      .add(exif.gpsCoordinates());

            log.debug("Seeded {}/{}", galleryId, filename);

        } catch (Exception e) {
            log.warn("Failed to seed {}/{}: {}", galleryId, filename, e.getMessage());
        }
    }

    private static boolean isJpeg(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }
}
