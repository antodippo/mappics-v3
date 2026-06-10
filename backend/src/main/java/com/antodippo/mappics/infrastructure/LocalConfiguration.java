package com.antodippo.mappics.infrastructure;

import com.antodippo.mappics.domain.GalleryRepository;
import com.antodippo.mappics.infrastructure.http.HTTPClient;
import com.antodippo.mappics.infrastructure.http.HTTPClientForLocalDev;
import com.antodippo.mappics.infrastructure.persistence.GalleryRepositoryInMemory;
import com.antodippo.mappics.infrastructure.storage.GalleryFileStorageInMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class LocalConfiguration {

    // Return the concrete type so LocalDevSeeder can inject it directly.
    @Bean
    public GalleryFileStorageInMemory galleryFileStorage() {
        return new GalleryFileStorageInMemory("http://localhost:8081/local-images");
    }

    @Bean
    public GalleryRepository galleryRepository() {
        return new GalleryRepositoryInMemory();
    }

    @Bean
    public HTTPClient httpClient() {
        return new HTTPClientForLocalDev();
    }
}
