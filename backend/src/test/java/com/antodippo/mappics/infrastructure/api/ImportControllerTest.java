package com.antodippo.mappics.infrastructure.api;

import com.antodippo.mappics.application.ImportJob;
import com.antodippo.mappics.application.ProcessUploadedGalleries;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ImportControllerTest {

    private final ImportJobStore importJobStore = mock(ImportJobStore.class);
    private final ProcessUploadedGalleries process = mock(ProcessUploadedGalleries.class);

    @Test
    void startImport_withCorrectSecret_returns202() {
        var job = new ImportJob("abc");
        when(importJobStore.hasRunningJob()).thenReturn(false);
        when(importJobStore.create()).thenReturn(job);

        var response = controller("secret123").startImport("secret123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void startImport_withWrongSecret_returns401() {
        var response = controller("secret123").startImport("wrong");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(importJobStore, process);
    }

    @Test
    void startImport_withMissingHeader_returns401() {
        var response = controller("secret123").startImport(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(importJobStore, process);
    }

    @Test
    void startImport_withNoSecretConfigured_allowsCallWithoutHeader() {
        var job = new ImportJob("abc");
        when(importJobStore.hasRunningJob()).thenReturn(false);
        when(importJobStore.create()).thenReturn(job);

        var response = controller("").startImport(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    private ImportController controller(String secret) {
        return new ImportController(process, importJobStore, secret);
    }
}
