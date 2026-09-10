package com.niki.fluttrading.service.impl;

import com.niki.fluttrading.contract.FlutTradingRequest;
import com.niki.fluttrading.contract.FlutTradingResponse;
import com.niki.fluttrading.contract.SchuurRequest;
import com.niki.fluttrading.domain.FlutPile;
import com.niki.fluttrading.service.FlutTradingService;
import com.niki.fluttrading.solver.FlutTradingSolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlutTradingServiceImpl implements FlutTradingService {

    private static final String SCHUURS_PREFIX = "schuurs ";
    private static final String MAXIMUM_PROFIT_PREFIX = "Maximum profit is ";
    private static final String MAXIMUM_PROFIT_SUFFIX = ".";
    private static final String NUMBER_OF_FLUTS_TO_BUY_PREFIX = "Number of fluts to buy: ";

    private final FlutTradingSolver flutTradingSolver;

    public FlutTradingResponse trade(FlutTradingRequest request) {
        log.debug("Processing trade request with {} schuur(s)", request.getSchuurs().size());
        List<String> messagesForEachSchuur = new ArrayList<>();
        for (SchuurRequest schuur : request.getSchuurs()) {
            if (!schuur.isValid()) {
                log.warn("Skipping invalid schuur {}: {}", schuur.getSchuur(), schuur.getFailureMessage());
                messagesForEachSchuur.add(schuur.getFailureMessage());
                continue;
            }
            List<FlutPile> piles = schuur.getFlutes().stream()
                    .map(flute -> new FlutPile(flute.getPrices()))
                    .collect(toList());
            if (piles.isEmpty()) {
                log.warn("Skipping schuur {} because it has no piles", schuur.getSchuur());
                continue;
            }
            var result = flutTradingSolver.solve(piles);
            log.debug("Schuur {} solved: maximumProfit={}, possibleFlutCounts={}",
                    schuur.getSchuur(), result.maximumProfit(), result.possibleFlutCounts());

            messagesForEachSchuur.add(SCHUURS_PREFIX + schuur.getSchuur());
            messagesForEachSchuur.add(MAXIMUM_PROFIT_PREFIX + result.maximumProfit() + MAXIMUM_PROFIT_SUFFIX);
            String flutCounts = result.possibleFlutCounts().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            messagesForEachSchuur.add(NUMBER_OF_FLUTS_TO_BUY_PREFIX + flutCounts);

        }
        log.info("Trade request processed: {} schuur(s) in, {} result line(s) out",
                request.getSchuurs().size(), messagesForEachSchuur.size());
        return FlutTradingResponse.builder().result(messagesForEachSchuur).build();
    }
}

