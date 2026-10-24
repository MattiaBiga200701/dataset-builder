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
    AVERAGE_LOC_ADDED("Average LOC Added"),
    CHURN("Churn"),
    MAX_CHURN("Max Churn"),
    AVERAGE_CHURN("Average Churn"),
    CHANGE_SET_SIZE("Change Set Size"),
    MAX_CHANGE_SET("Max Change Set"),
    AVERAGE_CHANGE_SET("Average Change Set"),
    AGE("Age"),
    WEIGHTED_AGE("Weighted Age"),
    MEAN_NS("Mean NS"),
    MEAN_LA("Mean LA"),
    MEAN_LD("Mean LD"),
    MEAN_ND("Mean ND"),
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
