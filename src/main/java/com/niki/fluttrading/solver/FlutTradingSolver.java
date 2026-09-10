package com.niki.fluttrading.solver;

import com.niki.fluttrading.domain.FlutPile;
import com.niki.fluttrading.domain.TradingResult;

import java.util.List;

public interface FlutTradingSolver {

    TradingResult solve(List<FlutPile> piles);
}
