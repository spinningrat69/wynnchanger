package io.wynnchanger.client;

public enum SkinType {
    DAGGER("Dagger"),
    SPEAR("Spear"),
    WAND("Wand"),
    BOW("Bow"),
    RELIK("Relik"),
    HELMET("Helmet"),
    CHESTPLATE("Chestplate"),
    LEGGINGS("Leggings"),
    BOOTS("Boots"),
    HAT("Hat"),
    UNKNOWN("Unknown");

    private final String displayName;

    SkinType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SkinType fromCommand(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace(' ', '_').replace('-', '_').toUpperCase(java.util.Locale.ROOT);
        switch (normalized) {
            case "RELIC":
                normalized = "RELIK";
                break;
            case "HELM":
                normalized = "HELMET";
                break;
            case "CHEST":
                normalized = "CHESTPLATE";
                break;
            case "LEGS":
            case "PANTS":
                normalized = "LEGGINGS";
                break;
            case "BOOT":
                normalized = "BOOTS";
                break;
            default:
                break;
        }
        try {
            return SkinType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static java.util.List<String> getCommandNames() {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (SkinType type : values()) {
            if (type == UNKNOWN) {
                continue;
            }
            names.add(type.displayName.toLowerCase(java.util.Locale.ROOT));
        }
        names.add("relic");
        names.add("helm");
        names.add("chest");
        names.add("legs");
        names.add("pants");
        names.add("boot");
        return names;
    }

    public boolean isArmorType() {
        return this == HELMET || this == CHESTPLATE || this == LEGGINGS || this == BOOTS;
    }

    public static java.util.List<String> getArmorCommandNames() {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (SkinType type : values()) {
            if (!type.isArmorType()) {
                continue;
            }
            names.add(type.displayName.toLowerCase(java.util.Locale.ROOT));
        }
        names.add("helm");
        names.add("chest");
        names.add("legs");
        names.add("pants");
        names.add("boot");
        return names;
    }

    public static SkinType fromModelPath(String modelPath) {
        if (modelPath.contains("/weapon/assassin/") || modelPath.contains("/skin/dagger/")) {
            return DAGGER;
        }
        if (modelPath.contains("/weapon/warrior/") || modelPath.contains("/skin/spear/")) {
            return SPEAR;
        }
        if (modelPath.contains("/weapon/mage/") || modelPath.contains("/skin/wand/")) {
            return WAND;
        }
        if (modelPath.contains("/weapon/archer/") || modelPath.contains("/skin/bow/")) {
            return BOW;
        }
        if (modelPath.contains("/weapon/shaman/") || modelPath.contains("/skin/relik/")) {
            return RELIK;
        }
        if (modelPath.contains("/armor/helmet/")) {
            return HELMET;
        }
        if (modelPath.contains("/armor/chestplate/")) {
            return CHESTPLATE;
        }
        if (modelPath.contains("/armor/leggings/")) {
            return LEGGINGS;
        }
        if (modelPath.contains("/armor/boots/")) {
            return BOOTS;
        }
        if (modelPath.contains("/skin/hat/")) {
            return HAT;
        }
        return UNKNOWN;
    }
}
