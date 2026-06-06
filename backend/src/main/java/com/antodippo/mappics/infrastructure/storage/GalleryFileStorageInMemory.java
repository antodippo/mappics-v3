package com.antodippo.mappics.infrastructure.storage;

import com.antodippo.mappics.domain.GalleryFileStorage;

import java.util.*;

public class GalleryFileStorageInMemory implements GalleryFileStorage {

    // galleryId → (filename → bytes)
    private final Map<String, Map<String, byte[]>> sourcePictures = new LinkedHashMap<>();
    private final Map<String, byte[]> processedPictures = new HashMap<>();
    private final String processedBaseUrl;

    public GalleryFileStorageInMemory(String processedBaseUrl) {
        this.processedBaseUrl = processedBaseUrl;
    }

    public void addPicture(String galleryId, String filename, byte[] data) {
        sourcePictures.computeIfAbsent(galleryId, k -> new LinkedHashMap<>()).put(filename, data);
    }

    @Override
    public List<String> listGalleryIds() {
        return new ArrayList<>(sourcePictures.keySet());
    }

    @Override
    public List<String> listPictureFilenames(String galleryId) {
        return sourcePictures.getOrDefault(galleryId, Map.of()).keySet().stream()
                .filter(StoragePaths::isJpeg)
                .toList();
    }

    @Override
    public byte[] readOriginalPicture(String galleryId, String filename) {
        Map<String, byte[]> gallery = sourcePictures.get(galleryId);
        if (gallery == null || !gallery.containsKey(filename)) {
            throw new IllegalArgumentException("Picture not found: " + galleryId + "/" + filename);
        }
        return gallery.get(filename);
    }

    @Override
    public void writeThumbnail(String galleryId, String filename, byte[] data) {
        processedPictures.put(StoragePaths.thumbnailPath(galleryId, filename), data);
    }

    @Override
    public void writeFullSize(String galleryId, String filename, byte[] data) {
        processedPictures.put(StoragePaths.fullSizePath(galleryId, filename), data);
    }

    @Override
    public boolean thumbnailExists(String galleryId, String filename) {
        return processedPictures.containsKey(StoragePaths.thumbnailPath(galleryId, filename));
    }

    @Override
    public boolean fullSizeExists(String galleryId, String filename) {
        return processedPictures.containsKey(StoragePaths.fullSizePath(galleryId, filename));
    }

    @Override
    public String getThumbnailUrl(String galleryId, String filename) {
        return processedBaseUrl + "/" + StoragePaths.thumbnailPath(galleryId, filename);
    }

    @Override
    public String getFullSizeUrl(String galleryId, String filename) {
        return processedBaseUrl + "/" + StoragePaths.fullSizePath(galleryId, filename);
    }
}
