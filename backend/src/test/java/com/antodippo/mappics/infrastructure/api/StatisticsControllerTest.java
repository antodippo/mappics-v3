package com.antodippo.mappics.infrastructure.api;

import com.antodippo.mappics.application.GalleryStatistics;
import com.antodippo.mappics.domain.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatisticsController.class)
class StatisticsControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean GalleryStatistics statistics;

    @Test
    void returnsAggregatesAndExtremums() throws Exception {
        when(statistics.compute()).thenReturn(new Statistics(
                42, 5, 1234.5,
                new Statistics.PictureStat("iceland/p1.jpg", "iceland", "Iceland", "http://thumb/p1.jpg", 64.13),
                new Statistics.PictureStat("chile/p3.jpg", "chile", "Chile", "http://thumb/p3.jpg", -53.1),
                new Statistics.PictureStat("kenya/p4.jpg", "kenya", "Kenya", "http://thumb/p4.jpg", 36.8),
                new Statistics.PictureStat("chile/p3.jpg", "chile", "Chile", "http://thumb/p3.jpg", -70.9),
                new Statistics.PictureStat("kenya/p4.jpg", "kenya", "Kenya", "http://thumb/p4.jpg", 1700.0),
                new Statistics.PictureStat("chile/p3.jpg", "chile", "Chile", "http://thumb/p3.jpg", -5.0),
                new Statistics.PictureStat("kenya/p4.jpg", "kenya", "Kenya", "http://thumb/p4.jpg", 30.0),
                new Statistics.DatedPictureStat("chile/p3.jpg", "chile", "Chile", "http://thumb/p3.jpg", LocalDateTime.of(2019, 3, 1, 12, 0)),
                new Statistics.DatedPictureStat("kenya/p4.jpg", "kenya", "Kenya", "http://thumb/p4.jpg", LocalDateTime.of(2022, 12, 1, 12, 0)),
                "Nikon D850", 1371,
                new Statistics.BiggestGallery("iceland", "Iceland", 12), 13.0
        ));

        mockMvc.perform(get("/api/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPictures").value(42))
                .andExpect(jsonPath("$.galleryCount").value(5))
                .andExpect(jsonPath("$.totalTraveledKm").value(1234.5))
                .andExpect(jsonPath("$.northernmost.pictureId").value("iceland/p1.jpg"))
                .andExpect(jsonPath("$.northernmost.galleryId").value("iceland"))
                .andExpect(jsonPath("$.northernmost.galleryName").value("Iceland"))
                .andExpect(jsonPath("$.northernmost.thumbnailUrl").value("http://thumb/p1.jpg"))
                .andExpect(jsonPath("$.northernmost.value").value(64.13))
                .andExpect(jsonPath("$.oldest.takenAt").value("2019-03-01T12:00"))
                .andExpect(jsonPath("$.newest.takenAt").value("2022-12-01T12:00"))
                .andExpect(jsonPath("$.mostUsedCamera").value("Nikon D850"))
                .andExpect(jsonPath("$.dateSpanDays").value(1371))
                .andExpect(jsonPath("$.biggestGallery.name").value("Iceland"))
                .andExpect(jsonPath("$.biggestGallery.pictureCount").value(12))
                .andExpect(jsonPath("$.averageTemperatureCelsius").value(13.0));
    }

    @Test
    void omitsExtremumsWhenNoPicturesQualify() throws Exception {
        when(statistics.compute()).thenReturn(new Statistics(
                0, 0, 0.0,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null
        ));

        mockMvc.perform(get("/api/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPictures").value(0))
                .andExpect(jsonPath("$.northernmost").doesNotExist())
                .andExpect(jsonPath("$.oldest").doesNotExist())
                .andExpect(jsonPath("$.mostUsedCamera").doesNotExist())
                .andExpect(jsonPath("$.biggestGallery").doesNotExist());
    }
}
