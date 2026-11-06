package it.uniroma2.isw2.metrics;

import com.sun.source.tree.ClassTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/** Profondita' interna al progetto, massima per file; le librerie terminano il percorso. */
public final class DitCalculator {
    public Map<String, Integer> calculate(Map<String, String> sources) throws IOException {
        Map<String, Integer> result = new HashMap<>();
        if (sources.isEmpty()) return result;
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IOException("DIT richiede un JDK con compilatore Java");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<JavaFileObject> inputs = new ArrayList<>();
        Map<URI, String> paths = new HashMap<>();
        for (var source : sources.entrySet()) {
            // URI univoco anche con percorsi contenenti spazi, virgole o virgolette.
            String filename = source.getKey().substring(source.getKey().lastIndexOf('/') + 1);
            URI uri = URI.create("string:///source" + inputs.size() + "/"
                    + java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"));
            paths.put(uri, source.getKey());
            inputs.add(new SimpleJavaFileObject(uri, JavaFileObject.Kind.SOURCE) {
                @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    String text = source.getValue();
                    return text.startsWith("\uFEFF") ? text.substring(1) : text;
                }
            });
        }
        try (var manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            var task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none", "--release", "17", "-classpath", "", "-sourcepath", ""), null, inputs);
            List<TreePath> declarations = new ArrayList<>();
            for (var unit : task.parse()) {
                result.put(paths.get(unit.getSourceFile().toUri()), 0);
                new TreePathScanner<Void, Void>() {
                    @Override public Void visitClass(ClassTree tree, Void unused) {
                        declarations.add(getCurrentPath());
                        return super.visitClass(tree, unused);
                    }
                }.scan(unit, null);
            }
            for (var diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    throw new IOException("DIT: sintassi non valida in "
                            + paths.get(diagnostic.getSource().toUri()) + ": " + diagnostic.getMessage(null));
                }
            }
            // Risolve nomi, package, import e tipi JDK; non genera bytecode.
            // Errori nei corpi dei metodi non impediscono di leggere le superclassi.
            task.analyze();
            Trees trees = Trees.instance(task);
            Set<TypeElement> projectTypes = new HashSet<>();
            Set<String> names = new HashSet<>();
            Set<String> duplicates = new HashSet<>();
            for (TreePath path : declarations) {
                if (trees.getElement(path) instanceof TypeElement type) {
                    projectTypes.add(type);
                    String name = type.getQualifiedName().toString();
                    if (!name.isEmpty() && !names.add(name)) duplicates.add(name);
                }
            }
            Map<TypeElement, Integer> cache = new HashMap<>();
            for (TreePath path : declarations) {
                String file = paths.get(path.getCompilationUnit().getSourceFile().toUri());
                Integer depth = trees.getElement(path) instanceof TypeElement type
                        ? depth(type, task.getTypes(), cache, new HashSet<>(), duplicates, projectTypes) : null;
                Integer previous = result.get(file);
                result.put(file, previous == null || depth == null ? null : Math.max(previous, depth));
            }
        }
        return result;
    }

    private Integer depth(TypeElement type, Types types, Map<TypeElement, Integer> cache,
                          Set<TypeElement> visiting, Set<String> duplicates, Set<TypeElement> projectTypes) {
        if (cache.containsKey(type)) return cache.get(type);
        if (duplicates.contains(type.getQualifiedName().toString()) || !visiting.add(type)) return null;
        // Le classi seguono la superclasse; le interfacce seguono solo extends.
        var parents = type.getKind().isInterface() ? type.getInterfaces()
                : type.getSuperclass().getKind() == TypeKind.NONE ? List.<javax.lang.model.type.TypeMirror>of()
                : List.of(type.getSuperclass());
        Integer value = 0;
        for (var parent : parents) {
            // Object, tipi JDK e dipendenze assenti non aggiungono archi interni.
            if (!(types.asElement(parent) instanceof TypeElement ancestor)
                    || !projectTypes.contains(ancestor)) continue;
            Integer parentDepth = depth(ancestor, types, cache, visiting, duplicates, projectTypes);
            if (parentDepth == null) { value = null; break; }
            value = Math.max(value, parentDepth + 1);
        }
        visiting.remove(type);
        cache.put(type, value);
        return value;
    }
}
