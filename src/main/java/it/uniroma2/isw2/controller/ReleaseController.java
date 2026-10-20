package it.uniroma2.isw2.controller;

import it.uniroma2.isw2.model.dao.ReleaseDAO;
import it.uniroma2.isw2.model.entity.Release;
import org.json.JSONException;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Coordina il recupero, l'ordinamento e l'indicizzazione delle release. */
public class ReleaseController {
    private final ReleaseDAO releaseDAO;

    public ReleaseController() {
        this(new ReleaseDAO());
    }

    public ReleaseController(ReleaseDAO releaseDAO) {
        this.releaseDAO = Objects.requireNonNull(releaseDAO, "releaseDAO");
    }

    /**
     * Restituisce le release in ordine cronologico, con indici progressivi da 1.
     * Gli errori di recupero vengono propagati al chiamante.
     */
    public List<Release> getReleases(String projectName) throws IOException, JSONException {
        List<Release> releases = releaseDAO.getReleases(projectName);

        // Come nello script del professore, ordina per data crescente.
        Collections.sort(releases, new Comparator<Release>() {
            @Override
            public int compare(Release first, Release second) {
                return first.getDate().compareTo(second.getDate());
            }
        });

        for (int i = 0; i < releases.size(); i++) {
            releases.get(i).setIndex(i + 1);
        }
        return releases;
    }
}
