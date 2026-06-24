package com.antodippo.mappics.infrastructure.api;

import com.antodippo.mappics.application.ImportJob;
import com.antodippo.mappics.application.ProcessUploadedGalleries;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ImportControllerTest {

    private final ImportJobStore importJobStore = mock(ImportJobStore.class);
    private final ProcessUploadedGalleries process = mock(ProcessUploadedGalleries.class);
    private final Environment localEnv = environmentWithProfile(true);
    private final Environment prodEnv = environmentWithProfile(false);

    @Test
    void startImport_withCorrectSecret_returns202() {
        var job = new ImportJob("abc");
        when(importJobStore.hasRunningJob()).thenReturn(false);
        when(importJobStore.create()).thenReturn(job);

        var response = controller("secret123", prodEnv).startImport("secret123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void startImport_withWrongSecret_returns401() {
        var response = controller("secret123", prodEnv).startImport("wrong");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(importJobStore, process);
    }

    @Test
    void startImport_withMissingHeader_returns401() {
        var response = controller("secret123", prodEnv).startImport(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(importJobStore, process);
    }

    @Test
    void startImport_withNoSecretConfigured_localProfile_allowsCallWithoutHeader() {
        var job = new ImportJob("abc");
        when(importJobStore.hasRunningJob()).thenReturn(false);
        when(importJobStore.create()).thenReturn(job);

        var response = controller("", localEnv).startImport(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void startImport_withNoSecretConfigured_nonLocalProfile_returns503() {
        var response = controller("", prodEnv).startImport(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verifyNoInteractions(importJobStore, process);
    }

    private ImportController controller(String secret, Environment environment) {
        return new ImportController(process, importJobStore, secret, environment);
    }

    private static Environment environmentWithProfile(boolean isLocal) {
        var env = mock(Environment.class);
        when(env.matchesProfiles("local")).thenReturn(isLocal);
        return env;
    }
}
