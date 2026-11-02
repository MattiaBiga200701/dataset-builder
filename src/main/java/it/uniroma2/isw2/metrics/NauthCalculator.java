package it.uniroma2.isw2.metrics;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Conta le email Author distinte ricevute dal controller. */
public final class NauthCalculator {
    public int calculate(List<String> authorEmails) {
        Set<String> authors = new HashSet<>();
        for (String email : authorEmails) {
            String identity = Objects.requireNonNull(email, "authorEmail").strip().toLowerCase(Locale.ROOT);
            if (identity.isEmpty()) throw new IllegalArgumentException("Email Author mancante");
            authors.add(identity);
        }
        return authors.size();
    }
}
