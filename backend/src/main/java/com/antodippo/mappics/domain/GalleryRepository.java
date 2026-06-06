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
}
