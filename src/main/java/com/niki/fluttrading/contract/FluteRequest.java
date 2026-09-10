package com.niki.fluttrading.contract;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor
@Builder
@Getter
@Setter
public class FluteRequest {

    @NotEmpty(message = "prices must not be null or empty")
    private List<@Positive(message = "price must be positive") Integer> prices;

}