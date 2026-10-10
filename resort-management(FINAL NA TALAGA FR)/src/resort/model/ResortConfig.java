package resort.model;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResortConfig {

    private static volatile ResortConfig instance;
    private final Map<String, Map<String, Double>> roomRates;
    private final List<String> standardAmenities;
    private final Map<String, List<String>> roomAmenities;
    private final List<AddOnPreset> addOnPresets;

    private ResortConfig(ConfigFile file) throws IOException {
        validate(file);
        roomRates = immutableNestedMap(file.roomRates);
        standardAmenities = List.copyOf(file.standardAmenities);
        roomAmenities = immutableNestedListMap(file.roomAmenities);
        List<AddOnPreset> presets = new ArrayList<>();
        for (PresetFile preset : file.addOnPresets) {
            try {
                presets.add(new AddOnPreset(preset.id, preset.name,
                        AddOnCategory.valueOf(preset.category),
                        preset.price, preset.details,
                        AddOnChargeBasis.valueOf(preset.chargeBasis)));
            } catch (IllegalArgumentException ex) {
                throw new IOException("Invalid add-on preset configuration for "
                        + preset.id + ".", ex);
            }
        }
        addOnPresets = List.copyOf(presets);
    }

    public static synchronized void initialize() throws IOException {
        Path externalConfig = Path.of("config.json");
        if (Files.isRegularFile(externalConfig)) {
            try (Reader reader = Files.newBufferedReader(
                    externalConfig, StandardCharsets.UTF_8)) {
                instance = parse(reader);
            }
            return;
        }
        try (InputStream resource = ResortConfig.class.getResourceAsStream("/config.json")) {
            if (resource == null) {
                throw new IOException("config.json was not found in the working directory "
                        + "or application resources.");
            }
            try (Reader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
                instance = parse(reader);
            }
        }
    }

    public static ResortConfig getInstance() {
        ResortConfig current = instance;
        if (current == null) {
            try {
                initialize();
            } catch (IOException ex) {
                throw new IllegalStateException("Could not load resort config.json.", ex);
            }
            current = instance;
        }
        return current;
    }

    public double getNightlyRate(String roomType, String viewType) {
        Map<String, Double> rates = findIgnoreCase(roomRates, roomType);
        if (rates == null) {
            throw new IllegalArgumentException("Unknown room type: " + roomType);
        }
        Double rate = findIgnoreCase(rates, viewType);
        if (rate == null) {
            throw new IllegalArgumentException("Unknown room view: " + viewType);
        }
        return rate;
    }

    public List<String> getAmenitiesForRoomType(String roomType) {
        List<String> amenities = new ArrayList<>(standardAmenities);
        List<String> extras = findIgnoreCase(roomAmenities, roomType);
        if (extras != null) {
            amenities.addAll(extras);
        }
        return Collections.unmodifiableList(amenities);
    }

    public List<AddOnPreset> getAddOnPresets() {
        return addOnPresets;
    }

    private static ResortConfig parse(Reader reader) throws IOException {
        try {
            ConfigFile file = new Gson().fromJson(reader, ConfigFile.class);
            return new ResortConfig(file);
        } catch (JsonParseException | IllegalArgumentException ex) {
            throw new IOException("config.json contains invalid resort configuration: "
                    + ex.getMessage(), ex);
        }
    }

    private static void validate(ConfigFile file) throws IOException {
        if (file == null || file.roomRates == null || file.roomRates.isEmpty()
                || file.standardAmenities == null || file.roomAmenities == null
                || file.addOnPresets == null) {
            throw new IOException("config.json must define roomRates, standardAmenities, "
                    + "roomAmenities, and addOnPresets.");
        }
        for (Map.Entry<String, Map<String, Double>> room : file.roomRates.entrySet()) {
            if (isBlank(room.getKey()) || room.getValue() == null
                    || room.getValue().isEmpty()) {
                throw new IOException("Room rate entries require a room type and view rates.");
            }
            for (Map.Entry<String, Double> view : room.getValue().entrySet()) {
                if (isBlank(view.getKey()) || view.getValue() == null
                        || !Double.isFinite(view.getValue()) || view.getValue() < 0) {
                    throw new IOException("Room rates must have a view and a finite "
                            + "non-negative amount.");
                }
            }
        }
        for (RoomType roomType : RoomType.values()) {
            Map<String, Double> rates = findIgnoreCase(
                    file.roomRates, roomType.getDisplayName());
            if (rates == null || findIgnoreCase(rates, "Garden View") == null
                    || findIgnoreCase(rates, "Pool View") == null
                    || findIgnoreCase(rates, "Beachfront View") == null) {
                throw new IOException("config.json must provide Garden, Pool, and "
                        + "Beachfront rates for " + roomType.getDisplayName() + ".");
            }
        }
        if (file.standardAmenities.stream().anyMatch(ResortConfig::isBlank)) {
            throw new IOException("Standard amenities cannot be blank.");
        }
        for (Map.Entry<String, List<String>> entry : file.roomAmenities.entrySet()) {
            if (isBlank(entry.getKey()) || entry.getValue() == null
                    || entry.getValue().stream().anyMatch(ResortConfig::isBlank)) {
                throw new IOException("Room amenity entries require a room type and "
                        + "non-blank amenity names.");
            }
        }
        Set<String> presetIds = new HashSet<>();
        for (PresetFile preset : file.addOnPresets) {
            if (preset == null || isBlank(preset.id) || isBlank(preset.name)
                    || isBlank(preset.category) || isBlank(preset.details)
                    || isBlank(preset.chargeBasis) || preset.price == null
                    || !Double.isFinite(preset.price) || preset.price < 0) {
                throw new IOException("Add-on presets require an id, name, category, "
                        + "price, details, and chargeBasis.");
            }
            if (!presetIds.add(preset.id.toUpperCase(java.util.Locale.ROOT))) {
                throw new IOException("Add-on preset ids must be unique: " + preset.id);
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static <V> V findIgnoreCase(Map<String, V> values, String key) {
        if (key == null) {
            return null;
        }
        for (Map.Entry<String, V> entry : values.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Map<String, Map<String, Double>> immutableNestedMap(
            Map<String, Map<String, Double>> source) {
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();
        source.forEach((key, value) ->
                result.put(key, Collections.unmodifiableMap(new LinkedHashMap<>(value))));
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, List<String>> immutableNestedListMap(
            Map<String, List<String>> source) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, List.copyOf(value)));
        return Collections.unmodifiableMap(result);
    }

    private static final class ConfigFile {
        private Map<String, Map<String, Double>> roomRates;
        private List<String> standardAmenities;
        private Map<String, List<String>> roomAmenities;
        private List<PresetFile> addOnPresets;
    }

    private static final class PresetFile {
        private String id;
        private String name;
        private String category;
        private Double price;
        private String details;
        private String chargeBasis;
    }
}
