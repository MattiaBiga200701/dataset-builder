package it.uniroma2.isw2;

import it.uniroma2.isw2.metrics.LocCalculator;

/** Controlli offline della convenzione LOC adottata. */
public final class LocCalculatorCheck {
    public static void main(String[] args) {
        check("", 0);
        check(" \n\t\r\n// comment\n/* comment\n comment */", 0);
        check("class A {\n\n // comment\n int n; // comment\n}\n", 3);
        check("/* header */ class A {} /* trailing */", 1);
        check("/* comment\n*/ int x; /* comment\n*/ int y;", 2);
        check("String url = \"https://example.org/*code*/\";\nchar q = '\"';", 2);
        check("String s = \"escaped \\\" // still string\"; // comment", 1);
        check("String s = \"\"\"\n// text content\n/* text content */\n\n\"\"\";", 4);
        check("int x;\rint y;\r\nint z;", 3);
        check("\uFEFF// header\n{}", 1);
        System.out.println("OK: LOC, commenti, stringhe, caratteri, text block e terminatori di riga.");
    }

    private static void check(String source, int expected) {
        int actual = new LocCalculator().calculate(source);
        if (actual != expected) throw new AssertionError("Atteso " + expected + ", ottenuto " + actual + ": " + source);
    }
}
