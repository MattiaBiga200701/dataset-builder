package it.uniroma2.isw2;

import it.uniroma2.isw2.metrics.NfixCalculator;
import it.uniroma2.isw2.model.dao.FeatureComputingDAO;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Verifica offline dei ticket e del matching; --live interroga Jira senza scrivere file. */
public class NfixCheck {
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--live")) {
            var ids = new FeatureComputingDAO().getTicketIDs("STORM");
            System.out.println("Jira: " + ids.size() + " ID, distinti: " + Set.copyOf(ids).size());
            return;
        }
        FeatureComputingDAO dao = new FeatureComputingDAO() {
            private int calls;
            @Override protected JSONObject readJsonFromUrl(String url) {
                if (!url.contains("project=%22STORM%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                        + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,created")) {
                    throw new AssertionError("Query originale modificata: " + url);
                }
                int start = calls++ * 1000;
                if (calls > 2 || !url.endsWith("&startAt=" + start + "&maxResults=" + (start + 1000))) {
                    throw new AssertionError("Paginazione originale modificata");
                }
                JSONArray issues = new JSONArray();
                for (int i = start; i < Math.min(start + 1000, 1001); i++) {
                    issues.put(new JSONObject().put("key", "STORM-" + (i + 1)));
                }
                return new JSONObject().put("total", 1001).put("issues", issues);
            }
        };
        var ids = dao.getTicketIDs("STORM");
        if (ids.size() != 1001 || !ids.getFirst().equals("STORM-1")
                || !ids.getLast().equals("STORM-1001")) throw new AssertionError("ID persi");
        FeatureComputingDAO unavailable = new FeatureComputingDAO() {
            @Override protected JSONObject readJsonFromUrl(String url) throws IOException {
                throw new IOException("Jira non disponibile");
            }
        };
        try {
            unavailable.getTicketIDs("STORM");
            throw new AssertionError("Errore Jira ignorato");
        } catch (IOException expected) { /* Un errore non deve diventare una lista vuota. */ }
        NfixCalculator calculator = new NfixCalculator();
        var messagesByCommit = Map.of(
                "a", "Fix\n\n[STORM-12] STORM-12 STORM-13",
                "b", "https://issues.apache.org/jira/browse/STORM-12",
                "c", "storm-12",
                "d", "STORM-123 XSTORM-12 STORM-12suffix OTHER-12",
                "e", "fix bug");
        if (calculator.calculate(messagesByCommit, Set.of("STORM-12", "STORM-13")) != 3) {
            throw new AssertionError("Matching o unicita' dei fix errati");
        }
        if (calculator.calculate(messagesByCommit, Set.of()) != 0
                || calculator.calculate(Map.of(), Set.of("STORM-12")) != 0) {
            throw new AssertionError("Caso vuoto");
        }
        System.out.println("OK: query e paginazione originali, ID, errori Jira, matching esatto e commit distinti.");
    }
}
