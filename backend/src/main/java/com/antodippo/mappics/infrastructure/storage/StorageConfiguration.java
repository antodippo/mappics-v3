package com.antodippo.mappics.infrastructure.storage;

import com.antodippo.mappics.domain.GalleryFileStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class StorageConfiguration {

    @Bean
    @Profile("local")
    public GalleryFileStorage galleryFileStorageLocal() {
        return new GalleryFileStorageInMemory("http://localhost:8080/processed");
    }
}
