package io.wynnchanger.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum GlintType {
    NONE("None", 0),
    BLUE("Blue", 15),
    GREEN("Green", 16),
    RED("Red", 17),
    YELLOW("Yellow", 24),
    ORANGE("Orange", 19),
    BLACK("Black", 20),
    CYAN("Cyan", 18),
    WHITE("White", 21),
    PINK("Pink", 22),
    PURPLE("Purple", 23),
    INVERT("Invert", 8),
    PLASMA("Plasma", 12),
    RAINBOW("Rainbow", 3),
    AURORA("Aurora", 10),
    SHADOW("Shadow", 9),
    RIPPLE("Ripple", 5),
    DISTORT("Distort", 13),
    CHROME("Chrome", 14),
    REFLECTION("Reflection", 11),
    GLITCH("Glitch", 4);

    private final String displayName;
    private final int glintId;

    GlintType(String displayName, int glintId) {
        this.displayName = displayName;
        this.glintId = glintId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getGlintId() {
        return glintId;
    }

    public boolean isNone() {
        return glintId <= 0;
    }

    public int toTintColor() {
        if (glintId <= 0) {
            return -1;
        }
        return 0xFF000000 | (glintId << 16) | (0xFF << 8);
    }

    public static GlintType fromName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace(' ', '_').replace('-', '_').toUpperCase(Locale.ROOT);
        if (normalized.equals("OFF") || normalized.equals("CLEAR") || normalized.equals("NONE")) {
            return NONE;
        }
        for (GlintType type : values()) {
            if (type.name().equals(normalized) || type.displayName.toUpperCase(Locale.ROOT).equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    public static List<String> getCommandNames() {
        List<String> names = new ArrayList<>();
        for (GlintType type : values()) {
            names.add(type.displayName.toLowerCase(Locale.ROOT));
        }
        names.add("off");
        names.add("clear");
        return names;
    }
}
