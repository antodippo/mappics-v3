package com.antodippo.mappics.infrastructure.api;

import com.antodippo.mappics.domain.GalleryRepository;
import com.antodippo.mappics.domain.GpsCoordinates;
import com.antodippo.mappics.domain.Picture;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pictures")
@CrossOrigin(origins = "*")
public class PictureController {

    private final GalleryRepository repository;

    public PictureController(GalleryRepository repository) {
        this.repository = repository;
    }

    // Slim, world-map payload: only GPS-bearing pictures, only the fields the map plots.
    // May return thousands of points, so it deliberately omits exif/location/weather.
    @GetMapping
    public List<PictureMapPoint> listPictures() {
        return repository.findAllPictures().stream()
                .filter(p -> p.getGpsCoordinates().isPresent())
                .map(this::toMapPoint)
                .toList();
    }

    private PictureMapPoint toMapPoint(Picture picture) {
        GpsCoordinates gps = picture.getGpsCoordinates().orElseThrow();
        return new PictureMapPoint(
                picture.getId(),
                picture.getGalleryId(),
                new GpsResponse(gps.latitude(), gps.longitude(), gps.altitude()),
                picture.getThumbnailUrl().orElse(null)
        );
    }

    record GpsResponse(double latitude, double longitude, Double altitude) {}

    record PictureMapPoint(
            String id,
            String galleryId,
            GpsResponse gps,
            String thumbnailUrl
    ) {}
}
