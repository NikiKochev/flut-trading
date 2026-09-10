package com.niki.fluttrading.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.*;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor
@Builder
@Getter
@Setter
public class SchuurRequest {
    private int schuur;

    @Valid
    private List<FluteRequest> flutes;

    private String failureMessage;

    // Explicit @JsonProperty needed: standard JavaBean introspection derives
    // the property name "valid" (not "isValid") from the isValid()/setValid()
    // accessor pair for a boolean field already prefixed with "is", which
    // would silently ignore an incoming "isValid" JSON key otherwise.
    @JsonProperty("isValid")
    @Builder.Default
    private boolean isValid = true;
}
