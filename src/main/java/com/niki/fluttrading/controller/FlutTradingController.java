package com.niki.fluttrading.controller;

import com.niki.fluttrading.contract.FlutTradingRequest;
import com.niki.fluttrading.contract.FlutTradingResponse;
import com.niki.fluttrading.service.FlutInputFileParser;
import com.niki.fluttrading.service.FlutTradingResponseFormatter;
import com.niki.fluttrading.service.FlutTradingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@RestController
@RequestMapping("/api/v1/fluts")
@RequiredArgsConstructor
public class FlutTradingController {

    private final FlutTradingService flutTradingService;
    private final FlutInputFileParser parcer;
    private final FlutTradingResponseFormatter responseFormatter;

    @PostMapping("/trade")
    public FlutTradingResponse trade(@Valid @RequestBody FlutTradingRequest request) {
        log.info("Received JSON trade request.");
        return flutTradingService.trade(request);
    }

    @PostMapping("/trade/file")
    public String tradeFile(@RequestParam("file") MultipartFile file) {
        log.info("Received file trade request");
        var requestList = parcer.parse(file);
        var response = flutTradingService.trade(requestList);
        return responseFormatter.format(response);
    }
}
