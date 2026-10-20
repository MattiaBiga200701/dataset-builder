package it.uniroma2.isw2.model.dao;

import it.uniroma2.isw2.model.entity.Release;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Accesso alle versioni Jira con la metodologia dello script GetVersionsFromJIRA. */
public class ReleaseDAO {

    /**
     * Recupera le release datate, senza ordinarle o assegnare l'indice.
     * Come nello script del professore, a parita di data prevale l'ultima versione letta.
     * Gli errori HTTP o JSON vengono propagati al chiamante.
     */
    public List<Release> getReleases(String projName) throws IOException, JSONException {
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
        JSONObject json = readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");
        ArrayList<LocalDateTime> releases = new ArrayList<>();
        HashMap<LocalDateTime, String> releaseNames = new HashMap<>();
        HashMap<LocalDateTime, String> releaseID = new HashMap<>();

        for (int i = 0; i < versions.length(); i++) {
            String name = "";
            String id = "";
            if (versions.getJSONObject(i).has("releaseDate")) {
                if (versions.getJSONObject(i).has("name")) {
                    name = versions.getJSONObject(i).get("name").toString();
                }
                if (versions.getJSONObject(i).has("id")) {
                    id = versions.getJSONObject(i).get("id").toString();
                }
                LocalDate date = LocalDate.parse(
                        versions.getJSONObject(i).get("releaseDate").toString());
                LocalDateTime dateTime = date.atStartOfDay();
                if (!releases.contains(dateTime)) {
                    releases.add(dateTime);
                }
                releaseNames.put(dateTime, name);
                releaseID.put(dateTime, id);
            }
        }

        List<Release> result = new ArrayList<>();
        for (LocalDateTime date : releases) {
            Release release = new Release();
            release.setVersionId(releaseID.get(date));
            release.setVersionName(releaseNames.get(date));
            release.setDate(date);
            result.add(release);
        }
        return result;
    }

    private static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream();
             BufferedReader rd = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
}
