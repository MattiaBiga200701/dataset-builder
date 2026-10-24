package it.uniroma2.isw2.model.dao;

import it.uniroma2.isw2.model.entity.ReleaseDataset;

public class FeatureComputingDAO {
    private ReleaseDataset releaseDataset;

    public ReleaseDataset getReleaseDataset() {
        return releaseDataset;
    }

    public void setReleaseDataset(ReleaseDataset releaseDataset) {
        this.releaseDataset = releaseDataset;
    }
}
