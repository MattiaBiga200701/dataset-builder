package it.uniroma2.isw2.metrics;

import java.util.List;

/** Ogni coppia contiene [righe aggiunte, righe cancellate] di una revisione. */
public final class LocTouchedCalculator {
    public int calculate(List<int[]> revisions) {
        int touched = 0;
        for (int[] revision : revisions) {
            if (revision.length != 2 || revision[0] < 0 || revision[1] < 0) {
                throw new IllegalArgumentException("Attesa una coppia di conteggi non negativi");
            }
            touched = Math.addExact(touched, Math.addExact(revision[0], revision[1]));
        }
        return touched;
    }
}
