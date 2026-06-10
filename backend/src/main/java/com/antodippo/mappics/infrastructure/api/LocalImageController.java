package com.antodippo.mappics.infrastructure.api;

import com.antodippo.mappics.infrastructure.storage.GalleryFileStorageInMemory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("local")
@RequestMapping("/local-images")
public class LocalImageController {

    private final GalleryFileStorageInMemory fileStorage;

    public LocalImageController(GalleryFileStorageInMemory fileStorage) {
        this.fileStorage = fileStorage;
    }

    @GetMapping("/{galleryId}/{filename}")
    public ResponseEntity<byte[]> getImage(
            @PathVariable String galleryId,
            @PathVariable String filename) {
        try {
            byte[] data = fileStorage.readOriginalPicture(galleryId, filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(data);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
