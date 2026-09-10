package com.niki.fluttrading.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class FlutTradingControllerFileIT extends AbstractFlutTradingControllerIT {

    @DisplayName("GIVEN a valid input file with one schuur and one pile, " +
            "WHEN uploading it to /trade/file, " +
            "THEN the response is 200 OK, text/plain, with the exact 3-line result.")
    @Test
    void f1_validFileSingleSchuurSinglePile() {
        String content = "1\n4 9 13 5 10\n0\n";

        ResponseEntity<String> response = postFile("/api/v1/fluts/trade/file", "input.txt", bytesOf(content));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_PLAIN)).isTrue();
        assertThat(response.getBody()).isEqualTo("schuurs 1\nMaximum profit is 3.\nNumber of fluts to buy: 3, 4");
    }

    @DisplayName("GIVEN a valid input file with two schuurs, each with its own piles, " +
            "WHEN uploading it to /trade/file, " +
            "THEN the response is 200 OK and contains a result block for both schuurs.")
    @Test
    void f2_validFileMultipleSchuurs() {
        String content = """
                1
                4 9 13 5 10
                2
                4 7 3 9 10
                3 2 5 6
                0
                """;

        ResponseEntity<String> response = postFile("/api/v1/fluts/trade/file", "input.txt", bytesOf(content));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("schuurs 1").contains("schuurs 2");
    }

    @DisplayName("GIVEN a file with a leading terminating-zero line before a real test case, " +
            "WHEN uploading it to /trade/file, " +
            "THEN the response is 200 OK and the real test case after the zero is parsed correctly.")
    @Test
    void f3_leadingTerminatingZeroThenRealTestCase() {
        String content = "0\n1\n4 9 13 5 10\n0\n";

        ResponseEntity<String> response = postFile("/api/v1/fluts/trade/file", "input.txt", bytesOf(content));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("schuurs 1");
    }

    @DisplayName("GIVEN a file with blank lines interspersed between valid lines, " +
            "WHEN uploading it to /trade/file, " +
            "THEN the response is 200 OK and blank lines don't affect parsing.")
    @Test
    void f4_blankLinesAreTolerated() {
        String content = "1\n\n4 9 13 5 10\n\n0\n";

        ResponseEntity<String> response = postFile("/api/v1/fluts/trade/file", "input.txt", bytesOf(content));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("schuurs 1");
    }

    @DisplayName("GIVEN the existing 50-case large fixture file used by the parser unit tests, " +
            "WHEN uploading it to /trade/file, " +
            "THEN the response is 200 OK with a non-blank body, confirming the full pipeline handles it end-to-end.")
    @Test
    void f5_largeFixtureFileSmokeTest() throws IOException {
        byte[] content = new ClassPathResource("parser/large-fluttrading-input.txt").getInputStream().readAllBytes();

        ResponseEntity<String> response = postFile("/api/v1/fluts/trade/file", "large-fluttrading-input.txt", content);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotBlank();
    }

    @DisplayName("GIVEN a file where one pile line has a negative price and another schuur is valid, " +
            "WHEN uploading it to /trade/file, " +
            "THEN the response is 200 OK, the valid schuur resolves normally and the bad line's failure message is included.")
    @Test
    void g1_negativePriceLineProducesFailureMessageButOthersStillResolve() {
        String content = """
                1
                4 9 13 5 10
                1
                4 9 -13 5 10
                0
                """;

        ResponseEntity<String> response = postFile("/api/v1/fluts/trade/file", "input.txt", bytesOf(content));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody())
                .contains("schuurs 1")
                .contains("Invalid line format: 4 9 -13 5 10");
    }

    @DisplayName("GIVEN a file where a pile line contains a non-numeric token, " +
            "WHEN uploading it to /trade/file, " +
            "THEN the response is 200 OK and contains that line's failure message.")
    @Test
    void g2_nonNumericTokenProducesFailureMessage() {
        String content = "1\n4 9 abc 5 10\n0\n";

        ResponseEntity<String> response = postFile("/api/v1/fluts/trade/file", "input.txt", bytesOf(content));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Invalid line format: 4 9 abc 5 10");
    }

    @DisplayName("GIVEN a file where a pile line's declared count doesn't match the number of prices given, " +
            "WHEN uploading it to /trade/file, " +
            "THEN the response is 200 OK and contains that line's failure message.")
    @Test
    void g3_countMismatchProducesFailureMessage() {
        String content = "1\n5 9 13 5 10\n0\n"; // declares 5 prices but only 4 given

        ResponseEntity<String> response = postFile("/api/v1/fluts/trade/file", "input.txt", bytesOf(content));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Invalid line format: 5 9 13 5 10");
    }

    @DisplayName("GIVEN a multipart request with no file part at all, " +
            "WHEN posting it to /trade/file, " +
            "THEN the response is 400 Bad Request.")
    @Test
    void g4_noFilePartIsRejected() {
        ResponseEntity<String> response = postMultipart("/api/v1/fluts/trade/file", new MultipartBodyBuilder());

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @DisplayName("GIVEN a multipart request where the file part is named \"document\" instead of \"file\", " +
            "WHEN posting it to /trade/file, " +
            "THEN the response is 400 Bad Request.")
    @Test
    void g5_wrongFieldNameIsRejected() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("document", new ByteArrayResource(bytesOf("1\n4 9 13 5 10\n0\n")) {
            @Override
            public String getFilename() {
                return "input.txt";
            }
        });

        ResponseEntity<String> response = postMultipart("/api/v1/fluts/trade/file", builder);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @DisplayName("GIVEN an empty (zero-byte) file uploaded, " +
            "WHEN posting it to /trade/file, " +
            "THEN the response is 400 Bad Request with the \"Uploaded file must not be empty\" message.")
    @Test
    void g6_emptyFileIsRejected() {
        ResponseEntity<String> response = postFile("/api/v1/fluts/trade/file", "empty.txt", new byte[0]);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("Uploaded file must not be empty");
    }

    @DisplayName("GIVEN a file containing non-numeric garbage content instead of pile data, " +
            "WHEN uploading it to /trade/file, " +
            "THEN the response is 200 OK, containing a failure message, without the server crashing.")
    @Test
    void g7_nonNumericGarbageContentFailsGracefullyWithoutCrashing() {
        String content = "1\nhello world this is not numeric\n0\n";

        ResponseEntity<String> response = postFile("/api/v1/fluts/trade/file", "garbage.txt", bytesOf(content));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Invalid line format:");
    }
}
