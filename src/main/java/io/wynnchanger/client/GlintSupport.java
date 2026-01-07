package io.wynnchanger.client;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlintSupport implements SimpleSynchronousResourceReloadListener {
    private static final Identifier RELOAD_ID = Identifier.of(WynnchangerClient.MOD_ID, "glint_support");
    private static final Identifier GLINT_CONFIG_ID = Identifier.of("minecraft", "shaders/include/config/glint.glsl");
    private static final Pattern CASE_PATTERN = Pattern.compile("\\bcase\\s+(\\d+)\\s*:");

    private static volatile EnumSet<GlintType> availableGlints = EnumSet.of(GlintType.NONE);
    private static volatile boolean supported;

    @Override
    public Identifier getFabricId() {
        return RELOAD_ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        EnumSet<GlintType> next = EnumSet.of(GlintType.NONE);
        boolean found = false;

        Optional<Resource> resource = manager.getResource(GLINT_CONFIG_ID);
        if (resource.isPresent()) {
            found = true;
            Set<Integer> glintIds = new HashSet<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.get().getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = CASE_PATTERN.matcher(line);
                    while (matcher.find()) {
                        glintIds.add(Integer.parseInt(matcher.group(1)));
                    }
                }
            } catch (IOException ignored) {
                glintIds.clear();
            }

            if (glintIds.isEmpty()) {
                next = EnumSet.allOf(GlintType.class);
            } else {
                for (GlintType type : GlintType.values()) {
                    if (type.isNone() || glintIds.contains(type.getGlintId())) {
                        next.add(type);
                    }
                }
            }
        }

        availableGlints = next;
        supported = found;
    }

    public static boolean isSupported() {
        return supported;
    }

    public static boolean isGlintAvailable(GlintType type) {
        if (type == null) {
            return false;
        }
        return availableGlints.contains(type);
    }

    public static List<GlintType> getAvailableGlints() {
        return List.copyOf(availableGlints);
    }
}
