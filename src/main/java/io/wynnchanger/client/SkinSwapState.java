package io.wynnchanger.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SkinSwapState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<SkinType, Identifier> selectedByType = new EnumMap<>(SkinType.class);
    private final Map<SkinType, GlintType> glintByType = new EnumMap<>(SkinType.class);
    private Path configPath;
    private boolean dirty;

    public void setSelection(SkinType type, Identifier modelId) {
        if (type == null || type == SkinType.UNKNOWN) {
            return;
        }
        if (modelId == null) {
            selectedByType.remove(type);
            dirty = true;
            return;
        }
        selectedByType.put(type, modelId);
        dirty = true;
    }

    public Optional<Identifier> getSelection(SkinType type) {
        return Optional.ofNullable(selectedByType.get(type));
    }

    public void setGlint(SkinType type, GlintType glint) {
        if (type == null || type == SkinType.UNKNOWN) {
            return;
        }
        if (glint == null || glint.isNone()) {
            if (glintByType.remove(type) != null) {
                dirty = true;
            }
            return;
        }
        glintByType.put(type, glint);
        dirty = true;
    }

    public Optional<GlintType> getGlint(SkinType type) {
        return Optional.ofNullable(glintByType.get(type));
    }

    public boolean hasSelection(SkinType type) {
        return selectedByType.containsKey(type);
    }

    public void clearSelection(SkinType type) {
        if (type == null || type == SkinType.UNKNOWN) {
            return;
        }
        if (selectedByType.remove(type) != null) {
            dirty = true;
        }
    }

    public void clearGlint(SkinType type) {
        if (type == null || type == SkinType.UNKNOWN) {
            return;
        }
        if (glintByType.remove(type) != null) {
            dirty = true;
        }
    }

    public void setConfigPath(Path path) {
        this.configPath = path;
    }

    public void load() {
        if (configPath == null) {
            return;
        }
        loadFromDisk(configPath);
    }

    public void saveIfDirty() {
        if (!dirty) {
            return;
        }
        save();
    }

    public void save() {
        if (configPath == null) {
            return;
        }
        saveToDisk(configPath);
    }

    public void clearSelections(Set<SkinType> types) {
        if (types == null || types.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (SkinType type : types) {
            if (type == null || type == SkinType.UNKNOWN) {
                continue;
            }
            changed |= selectedByType.remove(type) != null;
        }
        if (changed) {
            dirty = true;
        }
    }

    public void clearAllSelections() {
        if (selectedByType.isEmpty()) {
            return;
        }
        selectedByType.clear();
        dirty = true;
    }

    private void loadFromDisk(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        selectedByType.clear();
        glintByType.clear();
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return;
            }
            JsonObject selections = root.getAsJsonObject("selections");
            if (selections != null) {
                for (Map.Entry<String, JsonElement> entry : selections.entrySet()) {
                    try {
                        SkinType type = SkinType.valueOf(entry.getKey());
                        Identifier modelId = Identifier.of(entry.getValue().getAsString());
                        selectedByType.put(type, modelId);
                    } catch (IllegalArgumentException ex) {
                        LOGGER.warn("Skipping invalid skin entry: {}", entry.getKey());
                    }
                }
            }
            JsonObject glints = root.getAsJsonObject("glints");
            if (glints != null) {
                for (Map.Entry<String, JsonElement> entry : glints.entrySet()) {
                    try {
                        SkinType type = SkinType.valueOf(entry.getKey());
                        GlintType glint = GlintType.fromName(entry.getValue().getAsString());
                        if (glint != null && !glint.isNone()) {
                            glintByType.put(type, glint);
                        }
                    } catch (IllegalArgumentException ex) {
                        LOGGER.warn("Skipping invalid glint entry: {}", entry.getKey());
                    }
                }
            }
        } catch (IOException ignored) {
        } finally {
            dirty = false;
        }
    }

    private void saveToDisk(Path path) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            JsonObject root = new JsonObject();
            JsonObject selections = new JsonObject();
            for (Map.Entry<SkinType, Identifier> entry : selectedByType.entrySet()) {
                selections.addProperty(entry.getKey().name(), entry.getValue().toString());
            }
            root.add("selections", selections);
            JsonObject glints = new JsonObject();
            for (Map.Entry<SkinType, GlintType> entry : glintByType.entrySet()) {
                glints.addProperty(entry.getKey().name(), entry.getValue().name());
            }
            root.add("glints", glints);
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(root, writer);
            }
            dirty = false;
        } catch (IOException ignored) {
        }
    }
}
