package com.niki.fluttrading.solver.impl;

import com.niki.fluttrading.domain.FlutPile;
import com.niki.fluttrading.domain.TradingResult;
import com.niki.fluttrading.solver.FlutTradingSolver;
import com.niki.fluttrading.solver.contract.ProfitOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlutTradingSolverImpl implements FlutTradingSolver {

    @Value("${flut-trading.sale-price}")
    private int salePrice;

    @Override
    public TradingResult solve(List<FlutPile> piles) {
        log.debug("Solving trading options for {} pile(s) at sale price {}", piles.size(), salePrice);
        Stack<ProfitOption> profitOptions = piles.stream()
                .map(this::calculateProfitOption)
                .collect(Collectors.toCollection(Stack::new));
        TradingResult result = combineAllSchuurOptions(profitOptions);
        log.debug("Solved: maximumProfit={}, possibleFlutCounts={}", result.maximumProfit(), result.possibleFlutCounts());
        return result;
    }

    private TradingResult combineAllSchuurOptions(Stack<ProfitOption> profitOptions) {
        while (profitOptions.size() != 1) {
            var currentProfitOption = profitOptions.pop();
            if (currentProfitOption.profit() < 0) {
                break;
            }
            var nextProfitOption = profitOptions.pop();
            var options = calculateOptions(currentProfitOption.numberOfFluts(), nextProfitOption.numberOfFluts());
            profitOptions.push(new ProfitOption(currentProfitOption.profit() + nextProfitOption.profit(), options));
        }
        var finalProfitOption = profitOptions.pop();
        List<Integer> possibleFlutCounts = finalProfitOption.numberOfFluts().stream()
                .toList();
        return new TradingResult(finalProfitOption.profit(), possibleFlutCounts);
    }

    private List<Integer> calculateOptions(List<Integer> firstCounts, List<Integer> secondCounts) {
        return firstCounts.stream()
                .flatMap(first -> secondCounts.stream().map(second -> first + second))
                .distinct()
                .sorted()
                .toList();
    }


    private ProfitOption calculateProfitOption(FlutPile pile) {
        Map<Integer, List<Integer>> numberOfFlutsByProfit = new TreeMap<>(Comparator.reverseOrder());
        int currentProfit = 0;
        numberOfFlutsByProfit.computeIfAbsent(currentProfit, key -> new ArrayList<>()).add(0);

        List<Integer> prices = pile.prices();
        for (int i = 0; i < prices.size(); i++) {
            currentProfit += salePrice - prices.get(i);
            int numberOfFluts = i + 1;
            numberOfFlutsByProfit.computeIfAbsent(currentProfit, key -> new ArrayList<>()).add(numberOfFluts);
        }

        return numberOfFlutsByProfit.entrySet().stream()
                .findFirst()
                .map(entry -> new ProfitOption(entry.getKey(), List.copyOf(entry.getValue())))
                .orElseThrow();
    }
}

