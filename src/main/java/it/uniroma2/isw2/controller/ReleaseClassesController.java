package it.uniroma2.isw2.controller;

import it.uniroma2.isw2.model.dao.ReleaseClassesDAO;
import it.uniroma2.isw2.model.dao.ReleaseDAO;
import it.uniroma2.isw2.model.entity.Release;
import it.uniroma2.isw2.model.entity.ReleaseClasses;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Coordina recupero Jira, selezione delle release e recupero degli snapshot Git. */
public class ReleaseClassesController {
    private final ReleaseDAO releaseDAO;
    private final ReleaseClassesDAO releaseClassesDAO;

    public ReleaseClassesController() {
        this(new ReleaseDAO(), new ReleaseClassesDAO());
    }

    public ReleaseClassesController(ReleaseDAO releaseDAO,
                                    ReleaseClassesDAO releaseClassesDAO) {
        this.releaseDAO = Objects.requireNonNull(releaseDAO, "releaseDAO");
        this.releaseClassesDAO = Objects.requireNonNull(releaseClassesDAO, "releaseClassesDAO");
    }

    /** Recupera le release da Jira una sola volta prima del ciclo su Git. */
    public List<ReleaseClasses> getReleaseClasses(String projectName) throws IOException {
        List<Release> releases = getSelectedReleases(projectName);
        List<ReleaseClasses> result = new ArrayList<>();
        for (Release release : releases) {
            result.add(releaseClassesDAO.getReleaseClasses(release));
        }
        return result;
    }

    /** Mantiene il primo 34%, arrotondato per difetto, con gli indici originali. */
    public List<Release> getSelectedReleases(String projectName) throws IOException {
        List<Release> releases = getReleases(projectName);
        int count = (int) ((long) releases.size() * 34 / 100);
        if (count == 0) {
            throw new IllegalArgumentException("Release insufficienti: il 34% per difetto e' zero.");
        }
        return List.copyOf(releases.subList(0, count));
    }

    /** Recupera le release da Jira, le ordina per data e assegna indici da 1. */
    public List<Release> getReleases(String projectName) throws IOException {
        List<Release> releases = new ArrayList<>(releaseDAO.getReleases(projectName));
        releases.sort(Comparator.comparing(Release::getDate));
        for (int i = 0; i < releases.size(); i++) {
            releases.get(i).setIndex(i + 1);
        }
        return releases;
    }
}
