package it.uniroma2.isw2.metrics;

import com.sun.source.tree.MethodTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/** WMC a peso unitario, aggregata sui metodi dichiarati nell'intero file Java. */
public final class WmcCalculator {
    public int calculate(String source) throws IOException {
        Objects.requireNonNull(source, "source");
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IOException("WMC richiede un JDK con compilatore Java");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        // Il nome fittizio non deve coincidere con la classe: si esegue solo il parsing.
        var input = new SimpleJavaFileObject(URI.create("string:///Source.java"), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source.startsWith("\uFEFF") ? source.substring(1) : source;
            }
        };
        try (var fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
            var task = (JavacTask) compiler.getTask(null, fileManager, diagnostics,
                    List.of("-proc:none", "--source", "17"), null, List.of(input));
            var trees = task.parse();
            for (var diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    throw new IOException("Sorgente Java non analizzabile per WMC, riga "
                            + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(Locale.ROOT));
                }
            }
            var counter = new TreeScanner<Integer, Void>() {
                @Override public Integer visitMethod(MethodTree method, Void unused) {
                    // I costruttori non hanno tipo di ritorno; si visitano comunque i tipi locali.
                    return reduce(method.getReturnType() == null ? 0 : 1, super.visitMethod(method, unused));
                }
                @Override public Integer reduce(Integer left, Integer right) {
                    return (left == null ? 0 : left) + (right == null ? 0 : right);
                }
            };
            Integer count = counter.scan(trees, null);
            return count == null ? 0 : count;
        }
    }
}
