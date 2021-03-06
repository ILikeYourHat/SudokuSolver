package pl.laskowski.marcin.solving.deduction.algorithm;

import pl.laskowski.marcin.solving.deduction.combinations.CollectionCombinator;
import pl.laskowski.marcin.model.Field;
import pl.laskowski.marcin.model.Region;
import pl.laskowski.marcin.model.SudokuHintGrid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Marcin Laskowski.
 */

public class NakedValuesAlgorithm extends DeductionAlgorithm {

    public static class Factory implements DeductionAlgorithm.Factory {

        private final int size;

        public Factory(int size) {
            this.size = size;
        }

        public NakedValuesAlgorithm instance(Set<Region> regions, SudokuHintGrid sudokuHintGrid) {
            return new NakedValuesAlgorithm(regions, sudokuHintGrid, size);
        }

    }

    private final int size;

    private NakedValuesAlgorithm(Set<Region> regions, SudokuHintGrid possibilities, int size) {
        super(regions, possibilities);
        this.size = size;
    }

    @Override
    protected void solve(Region region) {
        List<Set<Integer>> list = new ArrayList<>();
        for (Field field : region) {
            Set<Integer> hints = possibilities.forField(field);
            if (!hints.isEmpty()) {
                list.add(hints);
            }
        }
        CollectionCombinator combinator = new CollectionCombinator(size);
        combinator.iterate(list, values -> {
            Set<Integer> sum = new HashSet<>();
            for (Set<Integer> set : values) {
                sum.addAll(set);
            }
            if (sum.size() == size) {
                clearFromRegion(region, sum);
            }
        });
    }

    private void clearFromRegion(Region region, Set<Integer> hintsToRemove) {
        for (Field field : region) {
            Set<Integer> hints = possibilities.forField(field);
            if (!hints.isEmpty() && shouldBeCleared(hints, hintsToRemove)) {
                possibilities.removeAll(field, hintsToRemove);
                notifyChange();
            }
        }
    }

    private boolean shouldBeCleared(Set<Integer> currentHints, Set<Integer> hintsToRemove) {
        Set<Integer> tempHints = new HashSet<>(currentHints);
        tempHints.removeAll(hintsToRemove);
        return !tempHints.isEmpty() && currentHints.size() > tempHints.size();
    }

}
