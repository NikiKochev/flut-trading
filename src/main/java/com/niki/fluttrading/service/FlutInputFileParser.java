package com.niki.fluttrading.service;

import com.niki.fluttrading.contract.FlutTradingRequest;
import org.springframework.web.multipart.MultipartFile;


public interface FlutInputFileParser {
    FlutTradingRequest parse(MultipartFile file);
}
