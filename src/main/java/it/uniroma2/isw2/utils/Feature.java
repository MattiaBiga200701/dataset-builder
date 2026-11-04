package it.uniroma2.isw2.utils;

/** Nomi delle colonne delle feature e del target del dataset. */
public enum Feature {
    LOC("LOC"),
    LOC_TOUCHED("LOC Touched"),
    NR("NR"),
    NFIX("Nfix"),
    NAUTH("Nauth"),
    LOC_ADDED("LOC Added"),
    MAX_LOC_ADDED("Max LOC Added"),
    WMC("WMC"),
    DIT("DIT"),
    NOC("NOC"),
    CBO("CBO"),
    RFC("RFC"),
    LCOM("LCOM"),
    NSMELLS("NSmells"),
    BUGGINESS("Bugginess");

    private final String columnName;

    Feature(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnName() {
        return columnName;
    }
}
