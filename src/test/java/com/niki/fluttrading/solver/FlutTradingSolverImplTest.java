package com.niki.fluttrading.solver;

import com.niki.fluttrading.domain.FlutPile;
import com.niki.fluttrading.domain.TradingResult;
import com.niki.fluttrading.solver.impl.FlutTradingSolverImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlutTradingSolverImplTest {

    private final FlutTradingSolverImpl solver = new FlutTradingSolverImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(solver, "salePrice", 10);
    }

    @Test
    void returnsBestProfitOptionForSampleSchuur() {
        FlutPile pile = new FlutPile(List.of(7, 3, 11, 9, 10));

        TradingResult result = solver.solve(List.of(pile));

        assertThat(result.maximumProfit()).isEqualTo(10);
        assertThat(result.possibleFlutCounts()).containsExactly(2, 4, 5);
    }

    @Test
    void returnsZeroProfitOptionWhenNotBuyingAnythingIsBest() {
        FlutPile pile = new FlutPile(List.of(11, 12, 15));

        TradingResult result = solver.solve(List.of(pile));

        assertThat(result.maximumProfit()).isEqualTo(0);
        assertThat(result.possibleFlutCounts()).containsExactly(0);
    }

    @Test
    void returnsSingleTopItemWhenBuyingOnlyTheTopIsBest() {
        FlutPile pile = new FlutPile(List.of(3, 12, 12));

        TradingResult result = solver.solve(List.of(pile));

        assertThat(result.maximumProfit()).isEqualTo(7);
        assertThat(result.possibleFlutCounts()).containsExactly(1);
    }

    @Test
    void returnsWholePileWhenBuyingEverythingIsBest() {
        FlutPile pile = new FlutPile(List.of(2, 3, 4));

        TradingResult result = solver.solve(List.of(pile));

        assertThat(result.maximumProfit()).isEqualTo(21);
        assertThat(result.possibleFlutCounts()).containsExactly(3);
    }

    @Test
    void keepsNegativeIntermediateProfitWhenLaterItemsCompensate() {
        FlutPile pile = new FlutPile(List.of(12, 3));

        TradingResult result = solver.solve(List.of(pile));

        assertThat(result.maximumProfit()).isEqualTo(5);
        assertThat(result.possibleFlutCounts()).containsExactly(2);
    }

    @Test
    void combinesPileWithNegativeIntermediateProfitAndAnotherPile() {
        FlutPile pile1 = new FlutPile(List.of(12, 3));
        FlutPile pile2 = new FlutPile(List.of(8));

        TradingResult result = solver.solve(List.of(pile1, pile2));

        assertThat(result.maximumProfit()).isEqualTo(7);
        assertThat(result.possibleFlutCounts()).containsExactly(3);
    }

    @Test
    void handlesSinglePricePile() {
        FlutPile pile = new FlutPile(List.of(4));

        TradingResult result = solver.solve(List.of(pile));

        assertThat(result.maximumProfit()).isEqualTo(6);
        assertThat(result.possibleFlutCounts()).containsExactly(1);
    }

    @Test
    void combinesMultiplePilesByAddingTheirBestProfitsAndCounts() {
        FlutPile pile1 = new FlutPile(List.of(7, 3, 11, 9, 10));
        FlutPile pile2 = new FlutPile(List.of(1, 2, 3, 4, 10, 16, 10, 4, 16));

        TradingResult result = solver.solve(List.of(pile1, pile2));

        assertThat(result.maximumProfit()).isEqualTo(40);
        assertThat(result.possibleFlutCounts()).containsExactly(6, 7, 8, 9, 10, 12, 13);
    }

    @Test
    void combinesTwoPilesWithMultipleCountsRemovingDuplicatesAndSorting() {
        FlutPile pile1 = new FlutPile(List.of(7, 3, 11, 9, 10));
        FlutPile pile2 = new FlutPile(List.of(10, 10));

        TradingResult result = solver.solve(List.of(pile1, pile2));

        assertThat(result.maximumProfit()).isEqualTo(10);
        assertThat(result.possibleFlutCounts()).containsExactly(2, 3, 4, 5, 6, 7);
    }

    @Test
    void combinesThreePiles() {
        FlutPile pileA = new FlutPile(List.of(5));
        FlutPile pileB = new FlutPile(List.of(8));
        FlutPile pileC = new FlutPile(List.of(3));

        TradingResult result = solver.solve(List.of(pileA, pileB, pileC));

        assertThat(result.maximumProfit()).isEqualTo(14);
        assertThat(result.possibleFlutCounts()).containsExactly(3);
    }

    @Test
    void combinesFourPiles() {
        FlutPile pileA = new FlutPile(List.of(5));
        FlutPile pileB = new FlutPile(List.of(8));
        FlutPile pileC = new FlutPile(List.of(3));
        FlutPile pileD = new FlutPile(List.of(1));

        TradingResult result = solver.solve(List.of(pileA, pileB, pileC, pileD));

        assertThat(result.maximumProfit()).isEqualTo(23);
        assertThat(result.possibleFlutCounts()).containsExactly(4);
    }

    @Test
    void returnsZeroTotalProfitWhenEveryPileIsOnlyProfitableAtZeroPurchases() {
        FlutPile pile1 = new FlutPile(List.of(11, 12, 13));
        FlutPile pile2 = new FlutPile(List.of(15, 20));

        TradingResult result = solver.solve(List.of(pile1, pile2));

        assertThat(result.maximumProfit()).isEqualTo(0);
        assertThat(result.possibleFlutCounts()).containsExactly(0);
    }

    @Test
    void capsCombinedFlutCountsAtTenSmallestWhenMoreThanTenAreOptimal() {
        FlutPile pile1 = new FlutPile(List.of(10, 10, 10, 10, 10, 10, 10, 10, 10));
        FlutPile pile2 = new FlutPile(List.of(10, 10));

        TradingResult result = solver.solve(List.of(pile1, pile2));

        assertThat(result.maximumProfit()).isEqualTo(0);
        assertThat(result.possibleFlutCounts()).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
    }

    @Test
    void combinesTwoPilesWithMixedPositiveAndNegativeIntermediateDeltas() {
        FlutPile pile1 = new FlutPile(List.of(13, 8, 10, 14, 7, 15, 9));
        FlutPile pile2 = new FlutPile(List.of(10, 11, 9, 12, 9));

        TradingResult result = solver.solve(List.of(pile1, pile2));

        assertThat(result.maximumProfit()).isEqualTo(0);
        assertThat(result.possibleFlutCounts()).containsExactly(0, 1, 3);
    }
}
