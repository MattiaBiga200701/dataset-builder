package it.uniroma2.isw2.model.entity;

/**
 * Una riga del dataset: una classe in una specifica release.
 * I valori null rappresentano informazioni non ancora disponibili o calcolate.
 */
public class DatasetEntry {
    /** Colonna CSV: Project Name. */
    private String projectName;

    /** Colonna CSV: Class Name. */
    private String className;

    /** Colonna CSV: Release ID. */
    private Integer releaseId;

    /** Colonna CSV: Size. */
    private Integer size;

    /** Colonna CSV: LOC Touched. */
    private Integer locTouched;

    /** Colonna CSV: NR. */
    private Integer nr;

    /** Colonna CSV: Nfix. */
    private Integer nfix;

    /** Colonna CSV: Nauth. */
    private Integer nauth;

    /** Colonna CSV: LOC Added. */
    private Integer locAdded;

    /** Colonna CSV: Max LOC Added. */
    private Integer maxLocAdded;

    /** Colonna CSV: Average LOC Added. */
    private Double averageLocAdded;

    /** Colonna CSV: Churn. */
    private Integer churn;

    /** Colonna CSV: Max Churn. */
    private Integer maxChurn;

    /** Colonna CSV: Average Churn. */
    private Double averageChurn;

    /** Colonna CSV: Change Set Size. */
    private Integer changeSetSize;

    /** Colonna CSV: Max Change Set. */
    private Integer maxChangeSet;

    /** Colonna CSV: Average Change Set. */
    private Double averageChangeSet;

    /** Colonna CSV: Age. */
    private Double age;

    /** Colonna CSV: Weighted Age. */
    private Double weightedAge;

    /** Colonna CSV: Mean NS. */
    private Double meanNs;

    /** Colonna CSV: Mean LA. */
    private Double meanLa;

    /** Colonna CSV: Mean LD. */
    private Double meanLd;

    /** Colonna CSV: Mean ND. */
    private Double meanNd;

    /** Colonna CSV: NSmells. */
    private Integer nSmells;

    /** Colonna CSV: Bugginess. */
    private String bugginess;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Integer getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(Integer releaseId) {
        this.releaseId = releaseId;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getLocTouched() {
        return locTouched;
    }

    public void setLocTouched(Integer locTouched) {
        this.locTouched = locTouched;
    }

    public Integer getNr() {
        return nr;
    }

    public void setNr(Integer nr) {
        this.nr = nr;
    }

    public Integer getNfix() {
        return nfix;
    }

    public void setNfix(Integer nfix) {
        this.nfix = nfix;
    }

    public Integer getNauth() {
        return nauth;
    }

    public void setNauth(Integer nauth) {
        this.nauth = nauth;
    }

    public Integer getLocAdded() {
        return locAdded;
    }

    public void setLocAdded(Integer locAdded) {
        this.locAdded = locAdded;
    }

    public Integer getMaxLocAdded() {
        return maxLocAdded;
    }

    public void setMaxLocAdded(Integer maxLocAdded) {
        this.maxLocAdded = maxLocAdded;
    }

    public Double getAverageLocAdded() {
        return averageLocAdded;
    }

    public void setAverageLocAdded(Double averageLocAdded) {
        this.averageLocAdded = averageLocAdded;
    }

    public Integer getChurn() {
        return churn;
    }

    public void setChurn(Integer churn) {
        this.churn = churn;
    }

    public Integer getMaxChurn() {
        return maxChurn;
    }

    public void setMaxChurn(Integer maxChurn) {
        this.maxChurn = maxChurn;
    }

    public Double getAverageChurn() {
        return averageChurn;
    }

    public void setAverageChurn(Double averageChurn) {
        this.averageChurn = averageChurn;
    }

    public Integer getChangeSetSize() {
        return changeSetSize;
    }

    public void setChangeSetSize(Integer changeSetSize) {
        this.changeSetSize = changeSetSize;
    }

    public Integer getMaxChangeSet() {
        return maxChangeSet;
    }

    public void setMaxChangeSet(Integer maxChangeSet) {
        this.maxChangeSet = maxChangeSet;
    }

    public Double getAverageChangeSet() {
        return averageChangeSet;
    }

    public void setAverageChangeSet(Double averageChangeSet) {
        this.averageChangeSet = averageChangeSet;
    }

    public Double getAge() {
        return age;
    }

    public void setAge(Double age) {
        this.age = age;
    }

    public Double getWeightedAge() {
        return weightedAge;
    }

    public void setWeightedAge(Double weightedAge) {
        this.weightedAge = weightedAge;
    }

    public Double getMeanNs() {
        return meanNs;
    }

    public void setMeanNs(Double meanNs) {
        this.meanNs = meanNs;
    }

    public Double getMeanLa() {
        return meanLa;
    }

    public void setMeanLa(Double meanLa) {
        this.meanLa = meanLa;
    }

    public Double getMeanLd() {
        return meanLd;
    }

    public void setMeanLd(Double meanLd) {
        this.meanLd = meanLd;
    }

    public Double getMeanNd() {
        return meanNd;
    }

    public void setMeanNd(Double meanNd) {
        this.meanNd = meanNd;
    }

    public Integer getNSmells() {
        return nSmells;
    }

    public void setNSmells(Integer nSmells) {
        this.nSmells = nSmells;
    }

    public String getBugginess() {
        return bugginess;
    }

    public void setBugginess(String bugginess) {
        this.bugginess = bugginess;
    }

}
