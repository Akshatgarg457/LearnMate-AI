package com.learnmate.backend.exam.judge;

/**
 * Output comparison for judged programs. Trailing whitespace, trailing blank
 * lines and Windows line endings are ignored; internal spacing is significant.
 */
public final class OutputNormalizer {

    private OutputNormalizer() {}

    public static String normalize(String value) {
        if (value == null) return "";
        String[] lines = value.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        StringBuilder out = new StringBuilder();
        for (String line : lines) out.append(stripTrailing(line)).append('\n');
        int end = out.length();
        while (end > 0 && out.charAt(end - 1) == '\n') end--;
        return out.substring(0, end);
    }

    public static boolean matches(String actual, String expected) {
        return normalize(actual).equals(normalize(expected));
    }

    private static String stripTrailing(String line) {
        int end = line.length();
        while (end > 0 && (line.charAt(end - 1) == ' ' || line.charAt(end - 1) == '\t')) end--;
        return line.substring(0, end);
    }
}
