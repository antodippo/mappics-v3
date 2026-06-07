package com.antodippo.mappics.infrastructure.http;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Component
public class HTTPClientWithJavaHttpClient implements HTTPClient {

    private final HttpClient client = HttpClient.newHttpClient();

    @Override
    public String get(String url, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();
        headers.forEach(builder::header);

        try {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + response.statusCode() + " from " + url);
            }
            return response.body();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("HTTP GET failed for " + url + ": " + e.getMessage(), e);
        }
    }
}
