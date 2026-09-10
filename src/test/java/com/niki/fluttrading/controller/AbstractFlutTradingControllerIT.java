package com.niki.fluttrading.controller;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractFlutTradingControllerIT {

    @LocalServerPort
    private int port;

    protected RestClient restClient;

    @BeforeEach
    void setUpRestClient() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    protected ResponseEntity<String> postJson(String uri, String jsonBody) {
        try {
            return restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .toEntity(String.class);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    /**
     * POSTs a raw JSON body with an explicit (possibly wrong) content type,
     * e.g. to test unsupported-media-type scenarios.
     */
    protected ResponseEntity<String> postWithContentType(String uri, String body, MediaType contentType) {
        try {
            return restClient.post()
                    .uri(uri)
                    .contentType(contentType)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    /**
     * Uploads {@code content} as a multipart file part named {@code file}.
     */
    protected ResponseEntity<String> postFile(String uri, String filename, byte[] content) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        return postMultipart(uri, builder);
    }

    /** Uploads a multipart request built with a custom (e.g. wrong-named, or empty) {@link MultipartBodyBuilder}. */
    protected ResponseEntity<String> postMultipart(String uri, MultipartBodyBuilder builder) {
        MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();
        try {
            return restClient.post()
                    .uri(uri)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipartBody)
                    .retrieve()
                    .toEntity(String.class);
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    protected static byte[] bytesOf(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}

