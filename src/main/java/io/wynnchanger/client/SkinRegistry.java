package io.wynnchanger.client;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SkinRegistry implements SimpleSynchronousResourceReloadListener {
    private static final Identifier RELOAD_ID = Identifier.of(WynnchangerClient.MOD_ID, "skin_registry");
    private static final Identifier POTION_BASE_ID = Identifier.of("minecraft", "potion");
    private static final String MODEL_DIR = "models/item/wynn";
    private static final String ITEM_ASSET_DIR = "items";
    private static final String MODEL_PREFIX = "models/";
    private static final String MODEL_SUFFIX = ".json";
    private static final String ITEM_PREFIX = "items/";
    private static final String ITEM_SUFFIX = ".json";
    private static final String RANGE_DISPATCH_TYPE = "range_dispatch";
    private static final String KEY_TYPE = "type";
    private static final String KEY_PROPERTY = "property";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_MODEL = "model";
    private static final String KEY_THRESHOLD = "threshold";
    private static final String WYNN_MODEL_PREFIX = "item/wynn/";

    private final EnumMap<SkinType, List<SkinEntry>> skins = new EnumMap<>(SkinType.class);
    private final Map<Identifier, Map<Identifier, SkinModelMapping>> mappingByBaseItem = new HashMap<>();
    private final Map<Identifier, List<SkinModelMapping>> mappingByBaseItemValue = new HashMap<>();
    private final Map<Identifier, Identifier> baseItemByModelId = new HashMap<>();

    public SkinRegistry() {
        reset();
    }

    @Override
    public Identifier getFabricId() {
        return RELOAD_ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        reset();

        Map<Identifier, net.minecraft.resource.Resource> resources = manager.findResources(
                MODEL_DIR,
                path -> path.getPath().endsWith(MODEL_SUFFIX)
        );

        for (Identifier id : resources.keySet()) {
            String modelPath = stripModelPath(id.getPath());
            if (modelPath == null) {
                continue;
            }
            SkinType type = SkinType.fromModelPath(modelPath);
            if (type == SkinType.UNKNOWN) {
                continue;
            }
            Identifier modelId = Identifier.of(id.getNamespace(), modelPath);
            String displayName = formatDisplayName(modelId.getPath());
            skins.get(type).add(new SkinEntry(modelId, displayName, type));
        }

        Comparator<SkinEntry> byName = Comparator.comparing(SkinEntry::displayName, String.CASE_INSENSITIVE_ORDER);
        for (List<SkinEntry> entries : skins.values()) {
            entries.sort(byName);
        }

        loadItemMappings(manager);
    }

    public List<SkinEntry> getSkins(SkinType type) {
        List<SkinEntry> entries = skins.get(type);
        if (entries == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(entries);
    }

    public Collection<SkinEntry> getAllSkins() {
        List<SkinEntry> all = new ArrayList<>();
        for (SkinType type : SkinType.values()) {
            if (type == SkinType.UNKNOWN) {
                continue;
            }
            all.addAll(getSkins(type));
        }
        return Collections.unmodifiableList(all);
    }

    public Optional<SkinModelMapping> findMapping(Identifier baseItemId, Identifier modelId) {
        Map<Identifier, SkinModelMapping> mapping = mappingByBaseItem.get(baseItemId);
        if (mapping == null) {
            return findPotionFallback(baseItemId, modelId);
        }
        SkinModelMapping found = mapping.get(modelId);
        if (found != null) {
            return Optional.of(found);
        }
        return findPotionFallback(baseItemId, modelId);
    }

    public Optional<Identifier> findBaseItemForModel(Identifier modelId) {
        return Optional.ofNullable(baseItemByModelId.get(modelId));
    }

    public Optional<SkinModelMapping> findMapping(Identifier baseItemId, SkinPropertyValue value) {
        List<SkinModelMapping> mappings = mappingByBaseItemValue.get(baseItemId);
        if (mappings == null) {
            return findPotionFallback(baseItemId, value);
        }
        for (SkinModelMapping mapping : mappings) {
            if (mapping.propertyType() != value.type()) {
                continue;
            }
            if (Math.abs(mapping.value() - value.value()) <= value.epsilon()) {
                return Optional.of(mapping);
            }
        }
        return findPotionFallback(baseItemId, value);
    }

    private void reset() {
        skins.clear();
        mappingByBaseItem.clear();
        mappingByBaseItemValue.clear();
        baseItemByModelId.clear();
        for (SkinType type : SkinType.values()) {
            skins.put(type, new ArrayList<>());
        }
    }

    private static String stripModelPath(String resourcePath) {
        if (!resourcePath.startsWith(MODEL_PREFIX) || !resourcePath.endsWith(MODEL_SUFFIX)) {
            return null;
        }
        return resourcePath.substring(MODEL_PREFIX.length(), resourcePath.length() - MODEL_SUFFIX.length());
    }

    private static String formatDisplayName(String modelPath) {
        int lastSlash = modelPath.lastIndexOf('/');
        String raw = lastSlash >= 0 ? modelPath.substring(lastSlash + 1) : modelPath;
        String[] parts = raw.split("_");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                builder.append(parts[i].substring(1));
            }
        }
        return builder.toString();
    }

    private void loadItemMappings(ResourceManager manager) {
        Map<Identifier, net.minecraft.resource.Resource> itemAssets = manager.findResources(
                ITEM_ASSET_DIR,
                path -> path.getPath().endsWith(ITEM_SUFFIX)
        );

        for (Map.Entry<Identifier, net.minecraft.resource.Resource> entry : itemAssets.entrySet()) {
            Identifier assetId = entry.getKey();
            Identifier baseItemId = toBaseItemId(assetId);
            if (baseItemId == null) {
                continue;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(entry.getValue().getInputStream()))) {
                JsonElement element = JsonParser.parseReader(reader);
                collectMappings(baseItemId, element);
            } catch (IOException ignored) {
                // Skip malformed assets to keep reload resilient.
            }
        }
    }

    private void collectMappings(Identifier baseItemId, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            parseRangeDispatchMappings(baseItemId, obj);
            for (Map.Entry<String, JsonElement> child : obj.entrySet()) {
                collectMappings(baseItemId, child.getValue());
            }
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectMappings(baseItemId, child);
            }
        }
    }

    private static Identifier toBaseItemId(Identifier assetId) {
        String path = assetId.getPath();
        if (!path.startsWith(ITEM_PREFIX) || !path.endsWith(ITEM_SUFFIX)) {
            return null;
        }
        String itemPath = path.substring(ITEM_PREFIX.length(), path.length() - ITEM_SUFFIX.length());
        return Identifier.of(assetId.getNamespace(), itemPath);
    }

    private static Identifier parseModelId(JsonElement modelElement) {
        if (modelElement == null) {
            return null;
        }
        if (modelElement.isJsonObject()) {
            JsonObject modelObj = modelElement.getAsJsonObject();
            if (modelObj.has(KEY_MODEL)) {
                return parseModelId(modelObj.get(KEY_MODEL));
            }
            return null;
        }
        if (modelElement.isJsonPrimitive()) {
            String raw = modelElement.getAsString();
            if (raw.isBlank()) {
                return null;
            }
            if (raw.contains(":")) {
                return Identifier.of(raw);
            }
            return Identifier.of("minecraft", raw);
        }
        return null;
    }

    private static boolean isWynnModel(Identifier modelId) {
        return modelId.getPath().startsWith(WYNN_MODEL_PREFIX);
    }

    private void parseRangeDispatchMappings(Identifier baseItemId, JsonObject obj) {
        if (!obj.has(KEY_TYPE) || !obj.has(KEY_PROPERTY) || !obj.has(KEY_ENTRIES)) {
            return;
        }
        String type = obj.get(KEY_TYPE).getAsString();
        if (!RANGE_DISPATCH_TYPE.equals(type)) {
            return;
        }
        SkinPropertyType propertyType = SkinPropertyType.fromProperty(obj.get(KEY_PROPERTY).getAsString());
        if (propertyType == null) {
            return;
        }
        JsonArray entries = obj.getAsJsonArray(KEY_ENTRIES);
        for (JsonElement entry : entries) {
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject entryObj = entry.getAsJsonObject();
            Identifier modelId = parseModelId(entryObj.get(KEY_MODEL));
            if (modelId == null || !isWynnModel(modelId)) {
                continue;
            }
            float threshold = entryObj.has(KEY_THRESHOLD) ? entryObj.get(KEY_THRESHOLD).getAsFloat() : 0.0f;
            SkinType skinType = SkinType.fromModelPath(modelId.getPath());
            SkinModelMapping mapping = new SkinModelMapping(modelId, skinType, propertyType, threshold);
            mappingByBaseItem
                    .computeIfAbsent(baseItemId, ignored -> new HashMap<>())
                    .put(modelId, mapping);
            mappingByBaseItemValue
                    .computeIfAbsent(baseItemId, ignored -> new ArrayList<>())
                    .add(mapping);
            baseItemByModelId.putIfAbsent(modelId, baseItemId);
        }
    }

    private Optional<SkinModelMapping> findPotionFallback(Identifier baseItemId, Identifier modelId) {
        if (!isPotionVariant(baseItemId)) {
            return Optional.empty();
        }
        Map<Identifier, SkinModelMapping> mapping = mappingByBaseItem.get(POTION_BASE_ID);
        if (mapping == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapping.get(modelId));
    }

    private Optional<SkinModelMapping> findPotionFallback(Identifier baseItemId, SkinPropertyValue value) {
        if (!isPotionVariant(baseItemId)) {
            return Optional.empty();
        }
        List<SkinModelMapping> mappings = mappingByBaseItemValue.get(POTION_BASE_ID);
        if (mappings == null) {
            return Optional.empty();
        }
        for (SkinModelMapping mapping : mappings) {
            if (mapping.propertyType() != value.type()) {
                continue;
            }
            if (Math.abs(mapping.value() - value.value()) <= value.epsilon()) {
                return Optional.of(mapping);
            }
        }
        return Optional.empty();
    }

    private boolean isPotionVariant(Identifier baseItemId) {
        return baseItemId != null && baseItemId.getPath().endsWith("_potion");
    }
}
