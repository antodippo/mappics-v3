package com.antodippo.mappics.infrastructure.api;

import com.antodippo.mappics.domain.GpsCoordinates;
import com.antodippo.mappics.domain.Picture;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PictureController.class)
@Import(PictureControllerTest.TestConfig.class)
class PictureControllerTest {

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

    @Test
    void listPictures_returnsEmptyArray_whenNoPictures() throws Exception {
        mockMvc.perform(get("/api/pictures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listPictures_returnsSlimMapPointForEachPicture() throws Exception {
        Picture iceland = new PictureBuilder()
                .withId("iceland/p1.jpg")
                .withGalleryId("iceland")
                .build();
        Picture azores = new PictureBuilder()
                .withId("azores/p2.jpg")
                .withGalleryId("azores")
                .build();

        repository.savePicture(iceland);
        repository.savePicture(azores);

        mockMvc.perform(get("/api/pictures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("iceland/p1.jpg"))
                .andExpect(jsonPath("$[0].galleryId").value("iceland"))
                .andExpect(jsonPath("$[0].thumbnailUrl").isString())
                .andExpect(jsonPath("$[0].gps.latitude").isNumber())
                .andExpect(jsonPath("$[0].gps.longitude").isNumber())
                // Slim payload: heavy fields are never serialised.
                .andExpect(jsonPath("$[0].exif").doesNotExist())
                .andExpect(jsonPath("$[0].location").doesNotExist())
                .andExpect(jsonPath("$[0].weather").doesNotExist())
                .andExpect(jsonPath("$[0].fullSizeUrl").doesNotExist());
    }

    @Test
    void listPictures_skipsPicturesWithoutGps() throws Exception {
        Picture withGps = new PictureBuilder()
                .withId("iceland/p1.jpg")
                .withGalleryId("iceland")
                .build();
        Picture withoutGps = Picture.create("iceland/raw.jpg", "iceland", "raw.jpg");

        repository.savePicture(withGps);
        repository.savePicture(withoutGps);

        mockMvc.perform(get("/api/pictures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("iceland/p1.jpg"));
    }

    @Test
    void listPictures_includesNullThumbnail_whenNotYetProcessed() throws Exception {
        Picture noThumb = Picture.create("iceland/raw.jpg", "iceland", "raw.jpg")
                .withGpsCoordinates(new GpsCoordinates(64.26, -21.12, null));

        repository.savePicture(noThumb);

        mockMvc.perform(get("/api/pictures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].gps.latitude").value(64.26))
                .andExpect(jsonPath("$[0].thumbnailUrl").doesNotExist());
    }
}
