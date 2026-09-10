package com.niki.fluttrading.contract;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor
@Builder
@Getter
@Setter
public class FlutTradingRequest {

    @NotEmpty(message = "schuurs must not be null or empty")
    @Valid
    private List<SchuurRequest> schuurs;
}
