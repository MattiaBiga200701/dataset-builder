package it.uniroma2.isw2;

import it.uniroma2.isw2.metrics.NauthCalculator;
import java.util.List;

public final class NauthCheck {
    public static void main(String[] args) {
        NauthCalculator calculator = new NauthCalculator();
        if (calculator.calculate(List.of("alice@example.org", "bob@example.org",
                "alice@example.org", " ALICE@EXAMPLE.ORG ")) != 2) {
            throw new AssertionError("Email duplicate contate piu' volte");
        }
        if (calculator.calculate(List.of()) != 0) throw new AssertionError("Lista vuota");
        if (calculator.calculate(List.of("alice@example.org", "alice@other.org")) != 2) {
            throw new AssertionError("Email diverse accorpate");
        }
        try {
            calculator.calculate(List.of(" "));
            throw new AssertionError("Email mancante ignorata");
        } catch (IllegalArgumentException expected) { }
        System.out.println("OK: Nauth, email distinte, normalizzazione, lista vuota ed email mancante.");
    }
}
