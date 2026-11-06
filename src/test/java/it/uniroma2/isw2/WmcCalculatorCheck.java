package it.uniroma2.isw2;

import it.uniroma2.isw2.metrics.WmcCalculator;
import java.io.IOException;

public final class WmcCalculatorCheck {
    public static void main(String[] args) throws Exception {
        check("class Empty {}", 0);
        check("import java.util.List;; class A { void m() {} }", 1);
        check("class A { A() {} static {} {} void a() {} int a(int n) { return n; } }", 2);
        check("interface I { void a(); default void b() {} static void c() {} }", 3);
        check("abstract class A { abstract void a(); native void b(); private static void c() {} }", 3);
        check("class A extends Missing { @Override public void a() { unknown(); } }", 1);
        check("class A { String s = \"void fake() {}\"; /* void fake() {} */ void real() {} }", 1);
        check("class A { A() { class Local { void local() {} } } "
                + "class Nested { void nested() {} } Object o = new Object() { void anon() {} }; "
                + "void outer() { Runnable r = () -> missing(); } } class B { void b() {} }", 5);
        check("enum E { A { void special() {} }; void common() {} }", 2);
        check("record R(int n) { R {} public int n() { return n; } }", 1);
        check("@interface A { String value(); }", 1);
        check("\uFEFFclass A {}", 0);
        check("class A { void " + "\\u0061" + "() {} }", 1);
        try {
            new WmcCalculator().calculate("class Broken { void x( }");
            throw new AssertionError("Errore sintattico ignorato");
        } catch (IOException expected) { }
        System.out.println("OK: WMC, dichiarazioni, overload, tipi annidati, costruttori, lambda ed errori.");
    }

    private static void check(String source, int expected) throws IOException {
        int actual = new WmcCalculator().calculate(source);
        if (actual != expected) throw new AssertionError(actual + " != " + expected + ": " + source);
    }
}
