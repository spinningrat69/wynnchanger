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
