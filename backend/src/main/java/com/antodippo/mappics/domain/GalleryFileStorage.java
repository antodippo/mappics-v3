package com.antodippo.mappics.domain;

import java.util.List;

public interface GalleryFileStorage {

    List<String> listGalleryIds();
    List<String> listPictureFilenames(String galleryId);

    byte[] readOriginalPicture(String galleryId, String filename);

    void writeThumbnail(String galleryId, String filename, byte[] data);
    void writeFullSize(String galleryId, String filename, byte[] data);

    boolean thumbnailExists(String galleryId, String filename);
    boolean fullSizeExists(String galleryId, String filename);

    String getThumbnailUrl(String galleryId, String filename);
    String getFullSizeUrl(String galleryId, String filename);
}
