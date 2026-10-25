package it.uniroma2.isw2.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** Un solo processo Git per leggere i blob storici senza checkout. */
public final class GitSourceReader implements AutoCloseable {

    public record Source(String blob, String text) { }
    private final Process process;
    private final OutputStream requests;
    private final InputStream responses;

    public GitSourceReader(Path repository) throws IOException {
        process = new ProcessBuilder("git", "-C", repository.toAbsolutePath().toString(),
                "cat-file", "--batch").redirectError(ProcessBuilder.Redirect.INHERIT).start();
        requests = new BufferedOutputStream(process.getOutputStream());
        responses = new BufferedInputStream(process.getInputStream());
    }

    public Source read(String commit, String path) throws IOException {
        if (!commit.matches("[0-9a-fA-F]{40}")) throw new IOException("Hash commit non valido: " + commit);
        if (path.contains("\n") || path.contains("\r")) throw new IOException("Percorso Git con newline non supportato: " + path);
        requests.write((commit + ":" + path + "\n").getBytes(StandardCharsets.UTF_8));
        requests.flush();
        StringBuilder header = new StringBuilder();
        int c;
        while ((c = responses.read()) != -1 && c != '\n') header.append((char) c);
        String[] parts = header.toString().split(" ");
        if (parts.length != 3 || !parts[1].equals("blob")) {
            throw new IOException("Sorgente Git non disponibile: " + commit + ":" + path + " (" + header + ")");
        }
        int size;
        try { size = Integer.parseInt(parts[2]); }
        catch (NumberFormatException e) { throw new IOException("Dimensione blob non valida", e); }
        byte[] bytes = responses.readNBytes(size);
        if (bytes.length != size || responses.read() != '\n') throw new IOException("Risposta Git incompleta");
        return new Source(parts[0], new String(bytes, StandardCharsets.UTF_8));
    }

    @Override public void close() throws IOException {
        try { requests.close(); }
        finally {
            try { responses.close(); }
            finally { process.destroy(); }
        }
    }
}
