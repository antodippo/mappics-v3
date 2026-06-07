package com.antodippo.mappics.infrastructure.http;

import java.util.Map;

public interface HTTPClient {

    String get(String url, Map<String, String> headers);
}
