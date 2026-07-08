package com.antodippo.mappics.infrastructure.api;

import com.antodippo.mappics.domain.*;
import com.antodippo.mappics.domain.PictureBuilder;
import com.antodippo.mappics.infrastructure.persistence.GalleryRepositoryInMemory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GalleryController.class)
@Import(GalleryControllerTest.TestConfig.class)
class GalleryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired GalleryRepositoryInMemory repository;

    @TestConfiguration
    static class TestConfig {
        @Bean
        GalleryRepositoryInMemory galleryRepository() {
            return new GalleryRepositoryInMemory();
        }
    }

    @BeforeEach
    void clearRepository() {
        repository.clear();
    }

    // ── GET /api/galleries ────────────────────────────────────────────────────

    @Test
    void listGalleries_returnsEmptyArray_whenNoGalleries() throws Exception {
        mockMvc.perform(get("/api/galleries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listGalleries_returnsSummaryForEachGallery() throws Exception {
        Gallery iceland = Gallery.create("iceland")
                .withPictureIds(List.of("iceland/p1.jpg", "iceland/p2.jpg"))
                .withAverageGpsCoordinates(new GpsCoordinates(64.26, -21.12, null));
        Gallery azores = Gallery.create("azores")
                .withPictureIds(List.of("azores/p3.jpg"));

        repository.save(iceland);
        repository.save(azores);

        mockMvc.perform(get("/api/galleries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("iceland"))
                .andExpect(jsonPath("$[0].name").value("Iceland"))
                .andExpect(jsonPath("$[0].pictureCount").value(2))
                .andExpect(jsonPath("$[0].averageGps.latitude").value(64.26))
                .andExpect(jsonPath("$[0].averageGps.longitude").value(-21.12))
                .andExpect(jsonPath("$[1].id").value("azores"))
                .andExpect(jsonPath("$[1].pictureCount").value(1));
    }

    @Test
    void listGalleries_includesNullAverageGps_whenGalleryNotYetProcessed() throws Exception {
        repository.save(Gallery.create("iceland"));

        mockMvc.perform(get("/api/galleries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].averageGps").doesNotExist());
    }

    // ── GET /api/galleries/{id} ───────────────────────────────────────────────

    @Test
    void getGallery_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/galleries/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getGallery_returnsGalleryWithEmptyPictures_whenNoneProcessed() throws Exception {
        repository.save(Gallery.create("iceland"));

        mockMvc.perform(get("/api/galleries/iceland"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("iceland"))
                .andExpect(jsonPath("$.name").value("Iceland"))
                .andExpect(jsonPath("$.pictures.length()").value(0));
    }

    @Test
    void getGallery_returnsFullyProcessedPicture() throws Exception {
        Gallery gallery = Gallery.create("iceland")
                .withPictureIds(List.of("iceland/DSC_0114.JPG"));
        Picture picture = new PictureBuilder()
                .withId("iceland/DSC_0114.JPG")
                .withGalleryId("iceland")
                .withOriginalFilename("DSC_0114.JPG")
                .build();

        repository.save(gallery);
        repository.savePicture(picture);

        mockMvc.perform(get("/api/galleries/iceland"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pictures.length()").value(1))
                .andExpect(jsonPath("$.pictures[0].id").value("iceland/DSC_0114.JPG"))
                .andExpect(jsonPath("$.pictures[0].thumbnailUrl").isString())
                .andExpect(jsonPath("$.pictures[0].fullSizeUrl").isString())
                .andExpect(jsonPath("$.pictures[0].gps.latitude").isNumber())
                .andExpect(jsonPath("$.pictures[0].exif.cameraMake").value("Canon"))
                .andExpect(jsonPath("$.pictures[0].exif.takenAt").isString())
                .andExpect(jsonPath("$.pictures[0].location.name").value("London"))
                .andExpect(jsonPath("$.pictures[0].weather.temperatureCelsius").isNumber())
                .andExpect(jsonPath("$.pictures[0].weather.humidity").isNumber());
    }

    @Test
    void getGallery_returnsNullFieldsForUnprocessedPicture() throws Exception {
        Gallery gallery = Gallery.create("iceland").withPictureIds(List.of("iceland/raw.JPG"));
        Picture picture = Picture.create("iceland/raw.JPG", "iceland", "raw.JPG");

        repository.save(gallery);
        repository.savePicture(picture);

        mockMvc.perform(get("/api/galleries/iceland"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pictures[0].id").value("iceland/raw.JPG"))
                .andExpect(jsonPath("$.pictures[0].thumbnailUrl").doesNotExist())
                .andExpect(jsonPath("$.pictures[0].gps").doesNotExist())
                .andExpect(jsonPath("$.pictures[0].exif").doesNotExist())
                .andExpect(jsonPath("$.pictures[0].location").doesNotExist())
                .andExpect(jsonPath("$.pictures[0].weather").doesNotExist());
    }

    @Test
    void getGallery_formatsExifTakenAtAsIsoString() throws Exception {
        Gallery gallery = Gallery.create("iceland").withPictureIds(List.of("iceland/p.JPG"));
        ExifData exif = new ExifData("Sony", "F5121",
                LocalDateTime.of(2017, 6, 9, 18, 43, 32),
                "4.2mm", "f/2.0", 40);
        Picture picture = Picture.create("iceland/p.JPG", "iceland", "p.JPG")
                .withExifData(exif)
                .withGpsCoordinates(new GpsCoordinates(64.26, -21.12, null))
                .withProcessedImages("http://t.jpg", "http://f.jpg")
                .withLocationDescription(new LocationDescription("Reykjavik", "Iceland"))
                .withWeatherData(new WeatherData(10.5, 72, 8.5, 1, "Mainly clear"));

        repository.save(gallery);
        repository.savePicture(picture);

        mockMvc.perform(get("/api/galleries/iceland"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pictures[0].exif.takenAt").value("2017-06-09T18:43:32"))
                .andExpect(jsonPath("$.pictures[0].exif.iso").value(40))
                .andExpect(jsonPath("$.pictures[0].weather.humidity").value(72))
                .andExpect(jsonPath("$.pictures[0].weather.windSpeedKmh").value(8.5));
    }
}
