package com.niki.fluttrading.service.impl;

import com.niki.fluttrading.contract.FlutTradingResponse;
import com.niki.fluttrading.service.FlutTradingResponseFormatter;
import org.springframework.stereotype.Service;

@Service
public class FlutTradingResponseFormatterImpl implements FlutTradingResponseFormatter {

    @Override
    public String format(FlutTradingResponse response) {
        return String.join("\n", response.getResult());
    }
}


