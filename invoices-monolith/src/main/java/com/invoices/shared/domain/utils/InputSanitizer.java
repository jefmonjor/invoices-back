package com.invoices.shared.domain.utils;

import java.util.regex.Pattern;

/**
 * Utility class for sanitizing user input to prevent XSS attacks.
 * Uses a whitelist approach to allow only safe characters or strips dangerous
 * tags.
 */
public class InputSanitizer {

    // Regex to match potentially dangerous HTML tags
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SRC_PATTERN = Pattern.compile("src[\r\n]*=[\r\n]*\\'(.*?)\\'",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern SRC_DOUBLE_QUOTE_PATTERN = Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern EVAL_PATTERN = Pattern.compile("eval\\((.*?)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("expression\\((.*?)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern VBSCRIPT_PATTERN = Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONLOAD_PATTERN = Pattern.compile("onload(.*?)=",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    /**
     * Sanitizes a string by removing potentially dangerous HTML/Script tags.
     * 
     * @param value The input string
     * @return The sanitized string
     */
    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }

        String clean = value;
        clean = SCRIPT_PATTERN.matcher(clean).replaceAll("");
        clean = SRC_PATTERN.matcher(clean).replaceAll("");
        clean = SRC_DOUBLE_QUOTE_PATTERN.matcher(clean).replaceAll("");
        clean = EVAL_PATTERN.matcher(clean).replaceAll("");
        clean = EXPRESSION_PATTERN.matcher(clean).replaceAll("");
        clean = JAVASCRIPT_PATTERN.matcher(clean).replaceAll("");
        clean = VBSCRIPT_PATTERN.matcher(clean).replaceAll("");
        clean = ONLOAD_PATTERN.matcher(clean).replaceAll("");

        // Basic HTML tag stripping for extra safety in non-HTML fields
        clean = clean.replaceAll("<", "&lt;").replaceAll(">", "&gt;");

        return clean;
    }
}
