package com.bettersettings.settings;

import java.util.List;

/** Summary shown by reload, info, and validate commands. */
public record ValidationReport(
    String activePreset, int loadedCount, int unavailableCount, int skippedCount,
    int duplicateCount, int invalidCount, boolean applied, List<ValidationIssue> issues
) {
    public ValidationReport { issues = List.copyOf(issues); }

    public static ValidationReport success(String preset, int loaded, int skipped, List<ValidationIssue> issues) {
        int invalid = (int) issues.stream().filter(i -> i.severity() == ValidationIssue.Severity.ERROR).count();
        return new ValidationReport(preset, loaded, 0, skipped, 0, invalid, invalid == 0, issues);
    }

    public boolean valid() { return duplicateCount == 0 && invalidCount == 0; }

    public ValidationReport withApplied(boolean value) {
        return new ValidationReport(activePreset, loadedCount, unavailableCount, skippedCount,
            duplicateCount, invalidCount, value, issues);
    }

    public ValidationReport withUnavailableCount(int value) {
        return new ValidationReport(activePreset, loadedCount, value, skippedCount,
            duplicateCount, invalidCount, applied, issues);
    }
}
