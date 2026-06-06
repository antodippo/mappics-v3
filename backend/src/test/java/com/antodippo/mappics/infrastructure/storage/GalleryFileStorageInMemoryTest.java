package com.antodippo.mappics.infrastructure.storage;

import com.antodippo.mappics.domain.GalleryFileStorage;

import java.util.Map;

class GalleryFileStorageInMemoryTest extends GalleryFileStorageAbstractTest {

    @Override
    protected GalleryFileStorage createAndSeedStorage(Map<String, Map<String, byte[]>> sourcePictures) {
        GalleryFileStorageInMemory storage = new GalleryFileStorageInMemory("http://localhost/processed");
        sourcePictures.forEach((galleryId, pictures) ->
                pictures.forEach((filename, data) -> storage.addPicture(galleryId, filename, data)));
        return storage;
    }
}
