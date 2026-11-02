package it.uniroma2.isw2.metrics;

import it.uniroma2.isw2.utils.GitHistoryReader.Revision;

import java.util.List;

/** Somma le righe aggiunte e cancellate delle revisioni della classe. */
public final class LocTouchedCalculator {
    public int calculate(List<Revision> revisions) {
        int touched = 0;
        for (Revision revision : revisions) {
            touched = Math.addExact(touched, Math.addExact(revision.added(), revision.deleted()));
        }
        return touched;
    }
}
