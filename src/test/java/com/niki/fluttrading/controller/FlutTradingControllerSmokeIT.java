package com.niki.fluttrading.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class FlutTradingControllerSmokeIT extends AbstractFlutTradingControllerIT {

    @DisplayName("GIVEN the full Spring Boot application running on a random port, " +
            "WHEN posting a valid JSON request to /trade over real HTTP, " +
            "THEN the response is 200 OK and contains the expected result line.")
    @Test
    void tradeEndpointRespondsOverRealHttp() {
        String body = """
                {"schuurs":[{"schuur":1,"flutes":[{"prices":[9,13,5,10]}]}]}
                """;

        ResponseEntity<String> response = postJson("/api/v1/fluts/trade", body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("schuurs 1");
    }
}

