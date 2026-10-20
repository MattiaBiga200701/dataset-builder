package it.uniroma2.isw2.model.dao;

import it.uniroma2.isw2.model.entity.DatasetEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Contenitore iniziale delle entry del dataset.
 * Il recupero dei dati e la costruzione delle entity saranno aggiunti nei passi successivi.
 */
public class DatasetEntryDAO {
    private List<DatasetEntry> entries = new ArrayList<>();

    public List<DatasetEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<DatasetEntry> entries) {
        this.entries = new ArrayList<>(Objects.requireNonNull(entries, "entries"));
    }
}
