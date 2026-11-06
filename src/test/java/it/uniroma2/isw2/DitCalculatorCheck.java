package it.uniroma2.isw2;

import it.uniroma2.isw2.metrics.DitCalculator;
import java.util.Map;
import java.util.Objects;

public final class DitCalculatorCheck {
    public static void main(String[] args) throws Exception {
        DitCalculator calculator = new DitCalculator();
        var result = calculator.calculate(Map.of(
                "A.java", "package p; public class A {}",
                "B.java", "package q; import p.A; public class B extends A {}",
                "C.java", "package q; public class C extends B {}",
                "I.java", "interface I {} interface J extends I {} interface K extends I, J {}",
                "Nested.java", "class Nested { class Inner extends q.C {} }",
                "External.java", "class External extends missing.Base {}",
                "Child.java", "class Child extends External {}",
                "Jdk.java", "class Jdk extends java.io.IOException {}",
                "Plain.java", "class Plain implements Runnable { public void run() { missing(); } }"));
        expect(result, "A.java", 0);
        expect(result, "B.java", 1);
        expect(result, "C.java", 2);
        expect(result, "I.java", 2);
        expect(result, "Nested.java", 3);
        expect(result, "External.java", 0);
        expect(result, "Child.java", 1);
        expect(result, "Jdk.java", 0);
        expect(result, "Plain.java", 0);
        expect(calculator.calculate(Map.of("A.java", "class A extends B {}", "B.java", "class B {}")), "A.java", 1);
        expect(calculator.calculate(Map.of("A.java", "class A extends B {}", "B.java", "class B extends java.lang.Exception {}")), "A.java", 1);
        expect(calculator.calculate(Map.of("A.java", "class A { Object o = new Object() {}; void m() { class Local extends A {} } }")), "A.java", 1);
        expect(calculator.calculate(Map.of("A.java", "class A extends Missing {} class B {}")), "A.java", 0);
        expect(calculator.calculate(Map.of("A.java", "class A extends B {} class B extends A {}")), "A.java", null);
        System.out.println("OK: DIT, catene, import, tipi annidati, interfacce, JDK, gerarchie mancanti e cambi di release.");
    }

    private static void expect(Map<String, Integer> result, String file, Integer expected) {
        if (!result.containsKey(file) || !Objects.equals(result.get(file), expected)) {
            throw new AssertionError(file + ": " + result.get(file) + " != " + expected);
        }
    }
}
