package com.antodippo.mappics.infrastructure.storage;

final class StoragePaths {

    private StoragePaths() {}

    static String thumbnailPath(String galleryId, String filename) {
        return galleryId + "/" + stripExtension(filename) + "_thumb.jpg";
    }

    static String fullSizePath(String galleryId, String filename) {
        return galleryId + "/" + stripExtension(filename) + "_full.jpg";
    }

    static boolean isJpeg(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
