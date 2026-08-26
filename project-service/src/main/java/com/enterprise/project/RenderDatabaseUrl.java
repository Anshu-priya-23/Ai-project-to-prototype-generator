package com.enterprise.project;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

final class RenderDatabaseUrl {
    private RenderDatabaseUrl() {}

    static void configure() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) return;

        URI uri = URI.create(databaseUrl);
        String[] credentials = uri.getRawUserInfo().split(":", 2);
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();

        System.setProperty("spring.datasource.url",
                "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getRawPath() + query);
        System.setProperty("spring.datasource.username", decode(credentials[0]));
        System.setProperty("spring.datasource.password", credentials.length > 1 ? decode(credentials[1]) : "");
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
