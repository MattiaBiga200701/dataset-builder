package it.uniroma2.isw2.metrics;

/** Righe fisiche contenenti codice Java; escluse righe vuote e soli commenti. */
public final class LocCalculator {
    private enum State { CODE, BLOCK_COMMENT, STRING, CHARACTER, TEXT_BLOCK }

    public int calculate(String source) {
        State state = State.CODE;
        int count = 0;
        for (String line : source.split("\\r\\n|\\r|\\n", -1)) {
            boolean code = false;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                char next = i + 1 < line.length() ? line.charAt(i + 1) : '\0';
                if (state == State.BLOCK_COMMENT) {
                    if (c == '*' && next == '/') { state = State.CODE; i++; }
                } else if (state == State.STRING || state == State.CHARACTER) {
                    code = true;
                    if (c == '\\') i++;
                    else if ((state == State.STRING && c == '"') || (state == State.CHARACTER && c == '\'')) {
                        state = State.CODE;
                    }
                } else if (state == State.TEXT_BLOCK) {
                    if (!Character.isWhitespace(c)) code = true;
                    if (c == '\\') i++;
                    else if (line.startsWith("\"\"\"", i)) { state = State.CODE; i += 2; }
                } else if (c == '/' && next == '/') {
                    break;
                } else if (c == '/' && next == '*') {
                    state = State.BLOCK_COMMENT;
                    i++;
                } else if (!Character.isWhitespace(c) && c != '\uFEFF') {
                    code = true;
                    if (line.startsWith("\"\"\"", i)) { state = State.TEXT_BLOCK; i += 2; }
                    else if (c == '"') state = State.STRING;
                    else if (c == '\'') state = State.CHARACTER;
                }
            }
            if (code) count++;
        }
        return count;
    }
}
