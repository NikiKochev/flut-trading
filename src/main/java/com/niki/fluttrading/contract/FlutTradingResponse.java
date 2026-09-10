package com.niki.fluttrading.contract;

import lombok.*;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor
@Builder
@Getter
@Setter
public class FlutTradingResponse{
    private List<String> result;
}


