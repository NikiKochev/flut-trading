package com.niki.fluttrading.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class FlutTradingControllerJsonIT extends AbstractFlutTradingControllerIT {

    @DisplayName("GIVEN a schuur with a single flute pile of positive prices, " +
            "WHEN posting it to /trade, " +
            "THEN the response is 200 OK and contains the schuur, profit and flut-count lines.")
    @Test
    void v1_singleSchuurSinglePile() {
        String body = """
                {"schuurs":[{"schuur":1,"flutes":[{"prices":[9,13,5,10]}]}]}
                """;

        ResponseEntity<String> response = postJson("/api/v1/fluts/trade", body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody())
                .contains("\"schuurs 1\"")
                .contains("\"Maximum profit is")
                .contains("\"Number of fluts to buy:");
    }

    @DisplayName("GIVEN a request with two separate schuurs, " +
            "WHEN posting it to /trade, " +
            "THEN the response is 200 OK and contains a result block for each schuur, in order.")
    @Test
    void v2_multipleSchuurs() {
        String body = """
                {"schuurs":[
                  {"schuur":1,"flutes":[{"prices":[9,13,5,10]}]},
                  {"schuur":2,"flutes":[{"prices":[5,7,3,9,10]}]}
                ]}
                """;

        ResponseEntity<String> response = postJson("/api/v1/fluts/trade", body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"schuurs 1\"").contains("\"schuurs 2\"");
    }

    @DisplayName("GIVEN a single schuur with multiple flute piles, " +
            "WHEN posting it to /trade, " +
            "THEN the response is 200 OK and combines the optimal result across all piles.")
    @Test
    void v3_singleSchuurMultiplePiles() {
        String body = """
                {"schuurs":[{"schuur":1,"flutes":[
                  {"prices":[9,13,5,10]},
                  {"prices":[5,7,3,9,10]}
                ]}]}
                """;

        ResponseEntity<String> response = postJson("/api/v1/fluts/trade", body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"schuurs 1\"");
    }

    @DisplayName("GIVEN a schuur the client has already marked as invalid with a failure message, " +
            "WHEN posting it to /trade, " +
            "THEN the response is 200 OK and contains only that failure message, with the solver never invoked.")
    @Test
    void v4_clientPreInvalidatedSchuurReturnsFailureMessageOnly() {
        String body = """
                {"schuurs":[{"schuur":1,"isValid":false,"failureMessage":"Invalid line format: bad line","flutes":[]}]}
                """;

        ResponseEntity<String> response = postJson("/api/v1/fluts/trade", body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("{\"result\":[\"Invalid line format: bad line\"]}");
    }

    @DisplayName("GIVEN a valid schuur with an empty flutes list, " +
            "WHEN posting it to /trade, " +
            "THEN the response is 200 OK and produces no result lines for that schuur.")
    @Test
    void v5_schuurWithEmptyFlutesProducesNoResultLines() {
        String body = """
                {"schuurs":[{"schuur":1,"flutes":[]}]}
                """;

        ResponseEntity<String> response = postJson("/api/v1/fluts/trade", body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("{\"result\":[]}");
    }

    @DisplayName("GIVEN two piles whose prices all equal the sale price, yielding 12 equally optimal flut counts, " +
            "WHEN posting it to /trade, " +
            "THEN the response is 200 OK and lists all 12 counts, confirming there is no 10-item cap.")
    @Test
    void v6_moreThanTenPossibleFlutCountsAreAllReturned() {
        String body = """
                {"schuurs":[{"schuur":1,"flutes":[
                  {"prices":[10,10,10,10,10,10,10,10,10]},
                  {"prices":[10,10]}
                ]}]}
                """;

        ResponseEntity<String> response = postJson("/api/v1/fluts/trade", body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Number of fluts to buy: 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11");
    }

    @DisplayName("GIVEN a pile where every price is identical, " +
            "WHEN posting it to /trade, " +
            "THEN the response is 200 OK and reports a maximum profit of 0.")
    @Test
    void v7_allIdenticalPricesYieldsZeroProfit() {
        String body = """
                {"schuurs":[{"schuur":1,"flutes":[{"prices":[10,10,10]}]}]}
                """;

        ResponseEntity<String> response = postJson("/api/v1/fluts/trade", body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Maximum profit is 0.");
    }

    @DisplayName("GIVEN a request mixing one valid schuur and one client-pre-invalidated schuur, " +
            "WHEN posting it to /trade, " +
            "THEN the response is 200 OK and contains both the solved block and the failure message.")
    @Test
    void v8_mixedValidAndClientPreInvalidatedSchuurs() {
        String body = """
                {"schuurs":[
                  {"schuur":1,"flutes":[{"prices":[9,13,5,10]}]},
                  {"schuur":2,"isValid":false,"failureMessage":"Invalid line format: bad line","flutes":[]}
                ]}
                """;

        ResponseEntity<String> response = postJson("/api/v1/fluts/trade", body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody())
                .contains("\"schuurs 1\"")
                .contains("\"Invalid line format: bad line\"");
    }

    @DisplayName("GIVEN a JSON body with the schuurs field omitted entirely, " +
            "WHEN posting it to /trade, " +
            "THEN the response is 400 Bad Request.")
    @Test
    void i1_missingSchuursFieldIsRejected() {
        ResponseEntity<String> response = postJson("/api/v1/fluts/trade", "{}");

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @DisplayName("GIVEN a request body with malformed JSON syntax, " +
            "WHEN posting it to /trade, " +
            "THEN the response is 400 Bad Request.")
    @Test
    void i2_malformedJsonSyntaxIsRejected() {
        ResponseEntity<String> response = postJson("/api/v1/fluts/trade", "{\"schuurs\": [ this is not json");

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @DisplayName("GIVEN a valid JSON body sent with a text/plain content type, " +
            "WHEN posting it to /trade, " +
            "THEN the response is 415 Unsupported Media Type.")
    @Test
    void i3_wrongContentTypeIsRejected() {
        String body = """
                {"schuurs":[{"schuur":1,"flutes":[{"prices":[9,13,5,10]}]}]}
                """;

        ResponseEntity<String> response = postWithContentType("/api/v1/fluts/trade", body, MediaType.TEXT_PLAIN);

        assertThat(response.getStatusCode().value()).isEqualTo(415);
    }

    @DisplayName("GIVEN a prices array containing a non-integer value, " +
            "WHEN posting it to /trade, " +
            "THEN the response is 400 Bad Request.")
    @Test
    void i4_nonIntegerPriceValueIsRejected() {
        String body = """
                {"schuurs":[{"schuur":1,"flutes":[{"prices":["abc",13,5,10]}]}]}
                """;

        ResponseEntity<String> response = postJson("/api/v1/fluts/trade", body);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @DisplayName("GIVEN a prices array containing a negative price, " +
            "WHEN posting it to /trade, " +
            "THEN the response is 400 Bad Request.")
    @Test
    void i5_negativePriceIsRejected() {
        String body = """
                {"schuurs":[{"schuur":1,"flutes":[{"prices":[9,-13,5,10]}]}]}
                """;

        ResponseEntity<String> response = postJson("/api/v1/fluts/trade", body);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @DisplayName("GIVEN a JSON body containing an extra, unrecognized field, " +
            "WHEN posting it to /trade, " +
            "THEN the response is 200 OK and the unknown field is silently ignored.")
    @Test
    void i6_unknownFieldsAreIgnored() {
        String body = """
                {"schuurs":[{"schuur":1,"somethingUnknown":"ignored","flutes":[{"prices":[9,13,5,10]}]}]}
                """;

        ResponseEntity<String> response = postJson("/api/v1/fluts/trade", body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @DisplayName("GIVEN a JSON body with an empty schuurs list, " +
            "WHEN posting it to /trade, " +
            "THEN the response is 400 Bad Request, since @NotEmpty rejects empty collections as well as null.")
    @Test
    void i7_emptySchuursListIsRejected() {
        ResponseEntity<String> response = postJson("/api/v1/fluts/trade", "{\"schuurs\": []}");

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }
}
