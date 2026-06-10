package com.antodippo.mappics.infrastructure.persistence;

import com.antodippo.mappics.domain.GalleryRepository;

class GalleryRepositoryInMemoryTest extends GalleryRepositoryAbstractTest {

    @Override
    protected GalleryRepository createRepository() {
        return new GalleryRepositoryInMemory();
    }
}
