package com.antodippo.mappics.infrastructure.persistence;

import com.antodippo.mappics.domain.Gallery;
import com.antodippo.mappics.domain.GalleryRepository;
import com.antodippo.mappics.domain.Picture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GalleryRepositoryInMemory implements GalleryRepository {

    private final Map<String, Gallery> galleries = new HashMap<>();
    private final Map<String, Picture> pictures = new HashMap<>();

    @Override
    public void save(Gallery gallery) {
        galleries.put(gallery.getId(), gallery);
    }

    @Override
    public Optional<Gallery> findById(String id) {
        return Optional.ofNullable(galleries.get(id));
    }

    @Override
    public List<Gallery> findAll() {
        return new ArrayList<>(galleries.values());
    }

    @Override
    public void savePicture(Picture picture) {
        pictures.put(picture.getId(), picture);
    }

    @Override
    public Optional<Picture> findPictureById(String pictureId) {
        return Optional.ofNullable(pictures.get(pictureId));
    }

    @Override
    public List<Picture> findPicturesByGalleryId(String galleryId) {
        return pictures.values().stream()
                .filter(p -> p.getGalleryId().equals(galleryId))
                .toList();
    }

    @Override
    public List<Picture> findAllPictures() {
        return new ArrayList<>(pictures.values());
    }
}
