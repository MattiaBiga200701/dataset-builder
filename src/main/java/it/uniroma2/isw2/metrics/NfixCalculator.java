package it.uniroma2.isw2.metrics;

import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Calcola Nfix dai messaggi per commit e dagli ID dei bug ricevuti dal controller. */
public final class NfixCalculator {
    private static final Pattern ISSUE_KEY = Pattern.compile(
            "(?<![A-Z0-9_-])[A-Z][A-Z0-9_]*-[0-9]+(?![A-Z0-9_-])",
            Pattern.CASE_INSENSITIVE);

    public int calculate(Map<String, String> messagesByCommit, Set<String> fixedBugKeys) {
        int fixes = 0;
        for (String message : messagesByCommit.values()) {
            var matcher = ISSUE_KEY.matcher(message);
            while (matcher.find()) {
                if (fixedBugKeys.contains(matcher.group().toUpperCase(Locale.ROOT))) {
                    fixes++;
                    break;
                }
            }
        }
        return fixes;
    }
}
