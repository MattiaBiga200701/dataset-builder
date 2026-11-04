package it.uniroma2.isw2.model.entity;

public class DatasetEntry {
    private static String projectName;

    private String className;

    private Release release;

    private Integer loc;

    private Integer locTouched;

    private Integer nr;

    private Integer nfix;

    private Integer nauth;

    private Integer locAdded;

    private Integer maxLocAdded;

    private Integer wmc;

    private Integer dit;

    private Integer noc;

    private Integer cbo;

    private Integer rfc;

    private Integer lcom;

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

    public Integer getLOC() {
        return loc;
    }

    public void setLOC(Integer loc) {
        this.loc = loc;
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

    public Integer getWMC() {
        return wmc;
    }

    public void setWMC(Integer wmc) {
        this.wmc = wmc;
    }

    public Integer getDIT() {
        return dit;
    }

    public void setDIT(Integer dit) {
        this.dit = dit;
    }

    public Integer getNOC() {
        return noc;
    }

    public void setNOC(Integer noc) {
        this.noc = noc;
    }

    public Integer getCBO() {
        return cbo;
    }

    public void setCBO(Integer cbo) {
        this.cbo = cbo;
    }

    public Integer getRFC() {
        return rfc;
    }

    public void setRFC(Integer rfc) {
        this.rfc = rfc;
    }

    public Integer getLCOM() {
        return lcom;
    }

    public void setLCOM(Integer lcom) {
        this.lcom = lcom;
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
