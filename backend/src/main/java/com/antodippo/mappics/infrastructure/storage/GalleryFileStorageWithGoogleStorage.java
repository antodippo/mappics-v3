package com.antodippo.mappics.infrastructure.storage;

import com.antodippo.mappics.domain.GalleryFileStorage;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@Profile("prod")
public class GalleryFileStorageWithGoogleStorage implements GalleryFileStorage {

    private static final String GCS_BASE_URL = "https://storage.googleapis.com";

    private final Storage storage;
    private final String sourceBucket;
    private final String processedBucket;

    public GalleryFileStorageWithGoogleStorage(
            Storage storage,
            @Value("${mappics.bucket.source}") String sourceBucket,
            @Value("${mappics.bucket.processed}") String processedBucket) {
        this.storage = storage;
        this.sourceBucket = sourceBucket;
        this.processedBucket = processedBucket;
    }

    @Override
    public List<String> listGalleryIds() {
        Set<String> galleryIds = new LinkedHashSet<>();
        for (Blob blob : storage.list(sourceBucket).iterateAll()) {
            String name = blob.getName();
            int slash = name.indexOf('/');
            if (slash > 0) {
                galleryIds.add(name.substring(0, slash));
            }
        }
        return new ArrayList<>(galleryIds);
    }

    @Override
    public List<String> listPictureFilenames(String galleryId) {
        String prefix = galleryId + "/";
        List<String> filenames = new ArrayList<>();
        for (Blob blob : storage.list(sourceBucket, Storage.BlobListOption.prefix(prefix)).iterateAll()) {
            String name = blob.getName().substring(prefix.length());
            if (!name.isEmpty() && StoragePaths.isJpeg(name)) {
                filenames.add(name);
            }
        }
        return filenames;
    }

    @Override
    public byte[] readOriginalPicture(String galleryId, String filename) {
        Blob blob = storage.get(sourceBucket, galleryId + "/" + filename);
        if (blob == null) {
            throw new IllegalArgumentException("Picture not found: " + galleryId + "/" + filename);
        }
        return blob.getContent();
    }

    @Override
    public void writeThumbnail(String galleryId, String filename, byte[] data) {
        storage.create(
                BlobInfo.newBuilder(processedBucket, StoragePaths.thumbnailPath(galleryId, filename))
                        .setContentType("image/jpeg")
                        .build(),
                data
        );
    }

    @Override
    public void writeFullSize(String galleryId, String filename, byte[] data) {
        storage.create(
                BlobInfo.newBuilder(processedBucket, StoragePaths.fullSizePath(galleryId, filename))
                        .setContentType("image/jpeg")
                        .build(),
                data
        );
    }

    @Override
    public boolean thumbnailExists(String galleryId, String filename) {
        return storage.get(processedBucket, StoragePaths.thumbnailPath(galleryId, filename)) != null;
    }

    @Override
    public boolean fullSizeExists(String galleryId, String filename) {
        return storage.get(processedBucket, StoragePaths.fullSizePath(galleryId, filename)) != null;
    }

    @Override
    public String getThumbnailUrl(String galleryId, String filename) {
        return GCS_BASE_URL + "/" + processedBucket + "/" + StoragePaths.thumbnailPath(galleryId, filename);
    }

    @Override
    public String getFullSizeUrl(String galleryId, String filename) {
        return GCS_BASE_URL + "/" + processedBucket + "/" + StoragePaths.fullSizePath(galleryId, filename);
    }
}
