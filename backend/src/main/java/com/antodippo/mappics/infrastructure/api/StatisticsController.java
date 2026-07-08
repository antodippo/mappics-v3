package com.antodippo.mappics.infrastructure.api;

import com.antodippo.mappics.application.GalleryStatistics;
import com.antodippo.mappics.domain.Statistics;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = "*")
public class StatisticsController {

    private final GalleryStatistics statistics;

    public StatisticsController(GalleryStatistics statistics) {
        this.statistics = statistics;
    }

    @GetMapping
    public StatisticsResponse getStatistics() {
        Statistics s = statistics.compute();
        return new StatisticsResponse(
                s.totalPictures(),
                s.galleryCount(),
                s.totalTraveledKm(),
                PictureStatResponse.from(s.northernmost()),
                PictureStatResponse.from(s.southernmost()),
                PictureStatResponse.from(s.easternmost()),
                PictureStatResponse.from(s.westernmost()),
                PictureStatResponse.from(s.highestAltitude()),
                PictureStatResponse.from(s.coldest()),
                PictureStatResponse.from(s.hottest()),
                DatedPictureStatResponse.from(s.oldest()),
                DatedPictureStatResponse.from(s.newest()),
                s.mostUsedCamera(),
                s.dateSpanDays(),
                BiggestGalleryResponse.from(s.biggestGallery()),
                s.averageTemperatureCelsius()
        );
    }

    // ── Response records ──────────────────────────────────────────────────────

    record PictureStatResponse(String pictureId, String galleryId, String galleryName, String thumbnailUrl, double value) {
        static PictureStatResponse from(Statistics.PictureStat s) {
            return s == null ? null
                    : new PictureStatResponse(s.pictureId(), s.galleryId(), s.galleryName(), s.thumbnailUrl(), s.value());
        }
    }

    record DatedPictureStatResponse(String pictureId, String galleryId, String galleryName, String thumbnailUrl, String takenAt) {
        static DatedPictureStatResponse from(Statistics.DatedPictureStat s) {
            return s == null ? null
                    : new DatedPictureStatResponse(s.pictureId(), s.galleryId(), s.galleryName(), s.thumbnailUrl(), s.takenAt().toString());
        }
    }

    record BiggestGalleryResponse(String galleryId, String name, int pictureCount) {
        static BiggestGalleryResponse from(Statistics.BiggestGallery b) {
            return b == null ? null
                    : new BiggestGalleryResponse(b.galleryId(), b.name(), b.pictureCount());
        }
    }

    record StatisticsResponse(
            int totalPictures,
            int galleryCount,
            double totalTraveledKm,
            PictureStatResponse northernmost,
            PictureStatResponse southernmost,
            PictureStatResponse easternmost,
            PictureStatResponse westernmost,
            PictureStatResponse highestAltitude,
            PictureStatResponse coldest,
            PictureStatResponse hottest,
            DatedPictureStatResponse oldest,
            DatedPictureStatResponse newest,
            String mostUsedCamera,
            Integer dateSpanDays,
            BiggestGalleryResponse biggestGallery,
            Double averageTemperatureCelsius
    ) {}
}
