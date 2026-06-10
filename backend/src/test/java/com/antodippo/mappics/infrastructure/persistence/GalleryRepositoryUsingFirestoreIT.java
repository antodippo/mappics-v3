package com.antodippo.mappics.infrastructure.persistence;

import com.antodippo.mappics.domain.GalleryRepository;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.FirestoreEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Tag("integration")
@Testcontainers
class GalleryRepositoryUsingFirestoreIT extends GalleryRepositoryAbstractTest {

    private static final String PROJECT_ID = "test-project";

    @Container
    static final FirestoreEmulatorContainer emulator = new FirestoreEmulatorContainer(
            DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:emulators")
    );

    private static ManagedChannel channel;
    private static Firestore firestore;

    @BeforeAll
    static void startFirestoreClient() {
        channel = ManagedChannelBuilder.forTarget(emulator.getEmulatorEndpoint())
                .usePlaintext()
                .build();
        firestore = FirestoreOptions.newBuilder()
                .setProjectId(PROJECT_ID)
                .setChannelProvider(
                        FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel)))
                .setCredentialsProvider(NoCredentialsProvider.create())
                .build()
                .getService();
    }

    @AfterAll
    static void stopFirestoreClient() throws Exception {
        firestore.close();
        channel.shutdown();
    }

    @BeforeEach
    @Override
    void setUp() {
        clearEmulatorData();
        super.setUp();
    }

    @Override
    protected GalleryRepository createRepository() {
        return new GalleryRepositoryUsingFirestore(firestore);
    }

    // The Firestore emulator exposes a REST endpoint to wipe all documents between tests.
    private static void clearEmulatorData() {
        try {
            HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder()
                            .uri(URI.create("http://" + emulator.getEmulatorEndpoint()
                                    + "/emulator/v1/projects/" + PROJECT_ID
                                    + "/databases/(default)/documents"))
                            .DELETE()
                            .build(),
                    HttpResponse.BodyHandlers.discarding()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear Firestore emulator data", e);
        }
    }
}
