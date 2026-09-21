package com.bettersettings.settings;

/** One actionable issue discovered while building a settings registry candidate. */
public record ValidationIssue(Severity severity, String code, String source, String path, String message) {
    public enum Severity {
        WARNING,
        ERROR
    }
}
