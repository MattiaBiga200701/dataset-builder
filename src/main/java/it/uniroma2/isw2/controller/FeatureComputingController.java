package it.uniroma2.isw2.controller;

import it.uniroma2.isw2.utils.GitHistoryReader.Revision;

import it.uniroma2.isw2.model.dao.FeatureComputingDAO;
import it.uniroma2.isw2.metrics.LocCalculator;
import it.uniroma2.isw2.metrics.LocTouchedCalculator;
import it.uniroma2.isw2.metrics.NrCalculator;
import it.uniroma2.isw2.model.entity.DatasetEntry;
import it.uniroma2.isw2.model.entity.ReleaseDataset;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import it.uniroma2.isw2.metrics.NfixCalculator;
import it.uniroma2.isw2.metrics.NauthCalculator;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import it.uniroma2.isw2.utils.GitSourceReader;

public class FeatureComputingController {
    private final FeatureComputingDAO featureComputingDAO;
    private final LocCalculator locCalculator;
    private final LocTouchedCalculator locTouchedCalculator;
    private final NrCalculator nrCalculator;
    private final NfixCalculator nfixCalculator;
    private final NauthCalculator nauthCalculator;

    public FeatureComputingController() {
        this(new FeatureComputingDAO());
    }

    public FeatureComputingController(FeatureComputingDAO featureComputingDAO) {
        this.featureComputingDAO = Objects.requireNonNull(featureComputingDAO, "featureComputingDAO");
        this.locCalculator = new LocCalculator();
        this.locTouchedCalculator = new LocTouchedCalculator();
        this.nrCalculator = new NrCalculator();
        this.nfixCalculator = new NfixCalculator();
        this.nauthCalculator = new NauthCalculator();
    }

    /** Arricchisce il CSV di base mantenendo ordine e dati preesistenti. */
    public Path createDatasetCsv(List<ReleaseDataset> datasets) throws IOException {
        Path baseCsv = Path.of("output", "dataset_base.csv");
        Path output = Path.of("output", "Dataset.csv");
        Objects.requireNonNull(datasets, "datasets");
        try (FeatureComputingDAO dao = featureComputingDAO) {
            dao.readBaseCsv(baseCsv, output);
            Set<String> fixedBugKeys = Set.copyOf(dao.getTicketIDs(DatasetEntry.getProjectName()));
            dao.prepareHistory(datasets.stream().map(ReleaseDataset::getCommitHash).toList());
            Map<String, Integer> locByBlob = new HashMap<>();
            String previousCommit = null;
            for (ReleaseDataset dataset : datasets) {
                Map<String, List<Revision>> changesByFile = dao.getChanges(previousCommit, dataset.getCommitHash());
                for (DatasetEntry entry : dataset.getEntries()) {
                    locComputing(dataset, entry, locByBlob);
                    locTouchedComputing(entry, changesByFile);
                    nrComputing(entry, changesByFile);
                    nfixComputing(entry, changesByFile, fixedBugKeys);
                    nauthComputing(entry, changesByFile);
                    dao.updateEntry(entry);
                }
                previousCommit = dataset.getCommitHash();
            }
            dao.writeDatasetCsv();
        }
        return output.toAbsolutePath();
    }

    private void locComputing(ReleaseDataset dataset, DatasetEntry entry,
                              Map<String, Integer> locByBlob) throws IOException {
        GitSourceReader.Source source = featureComputingDAO.getSource(dataset.getCommitHash(), entry.getClassName());
        entry.setLOC(locByBlob.computeIfAbsent(source.blob(), blob -> locCalculator.calculate(source.text())));
    }

    private void nfixComputing(DatasetEntry entry, Map<String, List<Revision>> changesByFile,
                               Set<String> fixedBugKeys) {
        Map<String, String> messagesByCommit = new HashMap<>();
        for (Revision revision : changesByFile.getOrDefault(entry.getClassName(), List.of())) {
            messagesByCommit.put(revision.commitHash(), revision.message());
        }
        entry.setNfix(nfixCalculator.calculate(messagesByCommit, fixedBugKeys));
    }

    private void nauthComputing(DatasetEntry entry, Map<String, List<Revision>> changesByFile) {
        List<String> authorEmails = changesByFile.getOrDefault(entry.getClassName(), List.of())
                .stream().map(Revision::authorEmail).toList();
        entry.setNauth(nauthCalculator.calculate(authorEmails));
    }

    private void nrComputing(DatasetEntry entry, Map<String, List<Revision>> changesByFile) {
        entry.setNr(nrCalculator.calculate(changesByFile.getOrDefault(entry.getClassName(), List.of())));
    }

    private void locTouchedComputing(DatasetEntry entry, Map<String, List<Revision>> changesByFile) {
        entry.setLocTouched(locTouchedCalculator.calculate(
                changesByFile.getOrDefault(entry.getClassName(), List.of())));
    }
}
