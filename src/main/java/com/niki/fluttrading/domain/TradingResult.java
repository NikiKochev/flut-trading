package com.niki.fluttrading.domain;

import java.util.List;

public record TradingResult(int maximumProfit, List<Integer> possibleFlutCounts) {
}
