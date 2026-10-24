package it.uniroma2.isw2.model.entity;

public class DatasetEntry {
    private static String projectName;

    private String className;

    private Release release;

    private Integer size;

    private Integer locTouched;

    private Integer nr;

    private Integer nfix;

    private Integer nauth;

    private Integer locAdded;

    private Integer maxLocAdded;

    private Double averageLocAdded;

    private Integer churn;

    private Integer maxChurn;

    private Double averageChurn;

    private Integer changeSetSize;

    private Integer maxChangeSet;

    private Double averageChangeSet;

    private Double age;

    private Double weightedAge;

    private Double meanNs;

    private Double meanLa;

    private Double meanLd;

    private Double meanNd;

    private Integer nSmells;

    private String bugginess;

    public static String getProjectName() {
        return projectName;
    }

    public static void setProjectName(String projectName) {
        DatasetEntry.projectName = projectName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Release getRelease() {
        return release;
    }

    public void setRelease(Release release) {
        this.release = release;
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
