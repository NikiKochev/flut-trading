package com.niki.fluttrading.service;

import com.niki.fluttrading.contract.FlutTradingRequest;
import com.niki.fluttrading.contract.FlutTradingResponse;

public interface FlutTradingService {
    FlutTradingResponse trade(FlutTradingRequest request);
}
