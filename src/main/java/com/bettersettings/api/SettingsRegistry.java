package com.bettersettings.api;

import com.bettersettings.settings.ValidationReport;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Thread-safe settings registry with an atomic managed layer and a durable
 * programmatic layer. YAML reloads never erase third-party registrations.
 */
public final class SettingsRegistry {

    private static final SettingsRegistry INSTANCE = new SettingsRegistry();
    private static final Logger LOGGER = Logger.getLogger("BetterSettings");

    private final ConcurrentHashMap<String, Setting> programmaticSettings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SettingCategory> programmaticCategories = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SettingBehavior> behaviors = new ConcurrentHashMap<>();
    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>(Snapshot.empty());
    private final ThreadLocal<ManagedCapture> managedCapture = new ThreadLocal<>();
    private volatile Map<String, Setting> managedSettings = Map.of();
    private volatile Map<String, SettingCategory> managedCategories = Map.of();
    private volatile ValidationReport validationReport = ValidationReport.success("classic", 0, 0, List.of());

    private SettingsRegistry() {}

    static SettingsRegistry isolatedForTests() {
        return new SettingsRegistry();
    }

    public static SettingsRegistry getInstance() {
        return INSTANCE;
    }

    /** Registers a legacy boolean setting in the durable programmatic layer. */
    public synchronized void registerSetting(Setting setting) {
        validateSetting(setting);
        ManagedCapture capture = managedCapture.get();
        if (capture != null) {
            capture.settings().add(setting);
            return;
        }
        String id = setting.getId();
        if (snapshot.get().settings().containsKey(id) || programmaticSettings.putIfAbsent(id, setting) != null) {
            LOGGER.warning("Attempted to register duplicate setting with ID '" + id + "'. The original setting was kept.");
            return;
        }
        rebuildSnapshot();
    }

    public void registerValueSetting(ValueSetting setting) {
        registerSetting(setting);
    }

    /** Starts a main-thread definition build without changing the live snapshot. */
    public void beginManagedRegistration() {
        if (managedCapture.get() != null) {
            throw new IllegalStateException("A managed registration is already in progress on this thread");
        }
        managedCapture.set(new ManagedCapture(new ArrayList<>(), new ArrayList<>()));
    }

    /** Validates and publishes the current thread's complete managed definition candidate. */
    public boolean commitManagedRegistration(ValidationReport report) {
        ManagedCapture capture = managedCapture.get();
        if (capture == null) {
            throw new IllegalStateException("No managed registration is in progress");
        }
        try {
            return replaceManaged(capture.settings(), capture.categories(), report);
        } finally {
            managedCapture.remove();
        }
    }

    public void cancelManagedRegistration(ValidationReport report) {
        managedCapture.remove();
        if (report != null) validationReport = report.withApplied(false);
    }

    public int getCapturedSettingCount() {
        ManagedCapture capture = managedCapture.get();
        return capture == null ? 0 : capture.settings().size();
    }

    public java.util.Set<String> getCapturedSettingIds() {
        ManagedCapture capture = managedCapture.get();
        if (capture == null) return java.util.Set.of();
        return capture.settings().stream().map(Setting::getId)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public java.util.Set<String> getCapturedCategoryIds() {
        ManagedCapture capture = managedCapture.get();
        if (capture == null) return java.util.Set.of();
        return capture.categories().stream().map(SettingCategory::getId)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public void registerBehavior(SettingBehavior behavior) {
        if (behavior == null || behavior.getId() == null || behavior.getId().isBlank()) {
            throw new IllegalArgumentException("Behavior and behavior ID must not be null or empty");
        }
        behaviors.put(behavior.getId(), behavior);
        refreshUnavailableCount();
    }

    public SettingBehavior getBehavior(String id) {
        return id == null ? null : behaviors.get(id);
    }

    /** Atomically replaces BetterSettings-owned definitions when the candidate is valid. */
    public synchronized boolean replaceManaged(Collection<? extends Setting> settings,
                                               Collection<SettingCategory> categories,
                                               ValidationReport report) {
        if (report == null || !report.valid()) {
            if (report != null) validationReport = report.withApplied(false);
            return false;
        }

        Map<String, Setting> nextSettings = new LinkedHashMap<>();
        for (Setting setting : settings) {
            validateSetting(setting);
            if (nextSettings.putIfAbsent(setting.getId(), setting) != null
                || programmaticSettings.containsKey(setting.getId())) {
                validationReport = new ValidationReport(
                    report.activePreset(), report.loadedCount(), report.unavailableCount(), report.skippedCount(),
                    report.duplicateCount() + 1, report.invalidCount(), false, report.issues()
                );
                return false;
            }
        }

        Map<String, SettingCategory> nextCategories = new LinkedHashMap<>();
        for (SettingCategory category : categories) {
            if (category != null && category.getId() != null && !category.getId().isBlank()) {
                nextCategories.putIfAbsent(category.getId(), category);
            }
        }

        managedSettings = Map.copyOf(nextSettings);
        managedCategories = Map.copyOf(nextCategories);
        validationReport = report.withApplied(true);
        rebuildSnapshot();
        refreshUnavailableCount();
        return true;
    }

    public Setting getSetting(String id) {
        return id == null ? null : snapshot.get().settings().get(id);
    }

    public ValueSetting getValueSetting(String id) {
        Setting setting = getSetting(id);
        return setting == null ? null : asValueSetting(setting);
    }

    public Collection<Setting> getSettings() {
        return snapshot.get().settings().values();
    }

    public Collection<ValueSetting> getValueSettings() {
        List<ValueSetting> values = new ArrayList<>();
        snapshot.get().settings().values().forEach(setting -> values.add(asValueSetting(setting)));
        return List.copyOf(values);
    }

    public synchronized void registerCategory(SettingCategory category) {
        if (category == null || category.getId() == null || category.getId().isBlank()) return;
        ManagedCapture capture = managedCapture.get();
        if (capture != null) {
            capture.categories().add(category);
            return;
        }
        if (snapshot.get().categories().containsKey(category.getId())
            || programmaticCategories.putIfAbsent(category.getId(), category) != null) return;
        rebuildSnapshot();
    }

    public SettingCategory getCategory(String id) {
        return id == null ? null : snapshot.get().categories().get(id);
    }

    public Collection<SettingCategory> getCategories() {
        return snapshot.get().categories().values();
    }

    public boolean isAvailable(ValueSetting setting) {
        String behaviorId = setting.getBehaviorId();
        if (behaviorId == null || behaviorId.isBlank()) return true;
        SettingBehavior behavior = behaviors.get(behaviorId);
        if (behavior == null) return false;
        try {
            return behavior.isAvailable();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public ValidationReport getValidationReport() {
        return validationReport;
    }

    private static void validateSetting(Setting setting) {
        if (setting == null) throw new IllegalArgumentException("Cannot register null setting");
        if (setting.getId() == null || setting.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Setting ID cannot be null or empty");
        }
    }

    private synchronized void rebuildSnapshot() {
        Map<String, Setting> allSettings = new LinkedHashMap<>(managedSettings);
        programmaticSettings.forEach(allSettings::putIfAbsent);
        Map<String, SettingCategory> allCategories = new LinkedHashMap<>(managedCategories);
        programmaticCategories.forEach(allCategories::putIfAbsent);
        snapshot.set(new Snapshot(Map.copyOf(allSettings), Map.copyOf(allCategories)));
    }

    private void refreshUnavailableCount() {
        int unavailable = (int) getValueSettings().stream().filter(setting -> !isAvailable(setting)).count();
        validationReport = validationReport.withUnavailableCount(unavailable);
    }

    private static ValueSetting asValueSetting(Setting setting) {
        return setting instanceof ValueSetting value ? value : new LegacyValueSetting(setting);
    }

    private record Snapshot(Map<String, Setting> settings, Map<String, SettingCategory> categories) {
        private static Snapshot empty() {
            return new Snapshot(Map.of(), Map.of());
        }
    }

    private record ManagedCapture(List<Setting> settings, List<SettingCategory> categories) {}

    private record LegacyValueSetting(Setting delegate) implements ValueSetting {
        @Override public String getId() { return delegate.getId(); }
        @Override public String getDescription() { return delegate.getDescription(); }
        @Override public ItemStack getIcon(Player player, String value) {
            return delegate.getIcon(player, Boolean.parseBoolean(value));
        }
        @Override public SettingType getType() { return SettingType.TOGGLE; }
        @Override public String getDefaultValue() { return Boolean.toString(delegate.getDefaultState()); }
        @Override public List<SettingOption> getOptions() { return List.of(); }
        @Override public String getPermission() { return delegate.getPermission(); }
        @Override public SettingCategory getCategory() { return delegate.getCategory(); }
        @Override public int getPriority() { return delegate.getPriority(); }
        @Override public boolean onToggle(Player player, boolean state) { return delegate.onToggle(player, state); }
    }
}
