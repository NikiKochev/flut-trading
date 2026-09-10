package com.niki.fluttrading.service.impl;

import com.niki.fluttrading.contract.FlutTradingRequest;
import com.niki.fluttrading.contract.SchuurRequest;
import com.niki.fluttrading.contract.FluteRequest;
import com.niki.fluttrading.service.FlutInputFileParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
public class FlutInputFileParserImpl implements FlutInputFileParser {

    public FlutTradingRequest parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.error("Rejected file upload: file is null or empty");
            throw new IllegalArgumentException("Uploaded file must not be empty");
        }

        log.info("Parsing uploaded file: name={}, size={} bytes", file.getOriginalFilename(), file.getSize());
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            return parseToTradingRequest(reader);
        } catch (IOException e) {
            log.error("Failed to read uploaded file: name={}", file.getOriginalFilename(), e);
            throw new IllegalArgumentException("Unable to read uploaded file", e);
        }
    }

    private FlutTradingRequest parseToTradingRequest(BufferedReader reader) throws IOException {
        String line;
        boolean isSchuursNumber = true;
        int numberOfSchuurs = 0;
        int counter = 0;
        List<SchuurRequest> tradingRequests = new ArrayList<>();
        SchuurRequest currentRequest = null;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            if (isSchuursNumber) {
                numberOfSchuurs = Integer.parseInt(line);
                if (numberOfSchuurs == 0) {
                    continue;
                }
                counter = 0;
                isSchuursNumber = false;
                currentRequest = SchuurRequest.builder()
                        .schuur(numberOfSchuurs)
                        .flutes(new ArrayList<>())
                        .build();
                continue;
            }
            try {
                var flutPile = separateLineToFlutPile(line);
                currentRequest.getFlutes().add(flutPile);

            } catch (Exception e) {
                currentRequest.setValid(false);
                currentRequest.setFailureMessage("Invalid line format: " + line);
                log.warn("Malformed pile line skipped: \"{}\" ({})", line, e.getMessage());
            }

            counter++;
            if (numberOfSchuurs == counter) {
                tradingRequests.add(currentRequest);
                isSchuursNumber = true;
            }

        }
        return FlutTradingRequest.builder()
                .schuurs(tradingRequests)
                .build();
    }

    private FluteRequest separateLineToFlutPile(String line) {
        List<Integer> prices = new ArrayList<>();
        var numbers = line.split(" ");
        int declaredCount = Integer.parseInt(numbers[0]);
        if (numbers.length - 1 != declaredCount) {
            throw new NoSuchElementException("No numbers found in line: " + line);
        }
        for (int i = 1; i < numbers.length; i++) {
            var number = Integer.parseInt(numbers[i]);
            if (number < 0) {
                throw new IllegalArgumentException("Price cannot be negative: " + number);
            }
            prices.add(number);
        }
        return FluteRequest.builder()
                .prices(prices)
                .build();
    }


}
