package com.antodippo.mappics.domain;

import java.util.List;
import java.util.Optional;

public interface GalleryRepository {

    void save(Gallery gallery);
    Optional<Gallery> findById(String id);
    List<Gallery> findAll();

    void savePicture(Picture picture);
    Optional<Picture> findPictureById(String pictureId);
    List<Picture> findPicturesByGalleryId(String galleryId);

    /**
     * Bulk read-only projection for consumers that scan every picture (world map,
     * statistics). Only id, galleryId, thumbnailUrl, GPS, camera make/model,
     * takenAt and weather temperature are guaranteed to be populated; adapters may
     * omit everything else. Never re-save pictures loaded through this method —
     * use {@link #findPicturesByGalleryId(String)} for full-fidelity reads.
     */
    List<Picture> findAllPictures();
}
