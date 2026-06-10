package com.antodippo.mappics.infrastructure.persistence;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class PersistenceConfiguration {

    // Local-profile repository bean lives in LocalConfiguration.

    @Bean
    @Profile("prod")
    public Firestore firestore(@Value("${spring.cloud.gcp.project-id}") String projectId) throws Exception {
        return FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
    }
}
