package it.uniroma2.isw2.metrics;

import it.uniroma2.isw2.utils.GitHistoryReader.Revision;

import java.util.List;

/** Somma le righe aggiunte e cancellate delle revisioni della classe. */
public final class LocTouchedCalculator {
    /** Conteggi condivisi da LOC Added, Max LOC Added e LOC Touched. */
    public record LineCounts(int added, int deleted, int maxAdded) {
        public int touched() {
            return Math.addExact(added, deleted);
        }
    }

    public int calculate(List<Revision> revisions) {
        return calculateLines(revisions).touched();
    }

    public LineCounts calculateLines(List<Revision> revisions) {
        int added = 0;
        int deleted = 0;
        int maxAdded = 0;
        for (Revision revision : revisions) {
            added = Math.addExact(added, revision.added());
            deleted = Math.addExact(deleted, revision.deleted());
            maxAdded = Math.max(maxAdded, revision.added());
        }
        return new LineCounts(added, deleted, maxAdded);
    }
}
