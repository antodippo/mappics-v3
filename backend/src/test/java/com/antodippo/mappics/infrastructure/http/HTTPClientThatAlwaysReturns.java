package com.antodippo.mappics.infrastructure.http;

import java.util.Map;

public class HTTPClientThatAlwaysReturns implements HTTPClient {

    private final String body;
    private String lastUrl;

    public HTTPClientThatAlwaysReturns(String body) {
        this.body = body;
    }

    @Override
    public String get(String url, Map<String, String> headers) {
        this.lastUrl = url;
        return body;
    }

    public String getLastUrl() {
        return lastUrl;
    }
}
