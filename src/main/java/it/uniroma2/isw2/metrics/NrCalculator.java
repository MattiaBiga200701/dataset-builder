package it.uniroma2.isw2.metrics;

import java.util.List;

/** Conta le revisioni distinte, comprese quelle senza righe aggiunte o cancellate. */
public final class NrCalculator {
    public int calculate(List<int[]> revisions) {
        return revisions.size();
    }
}
