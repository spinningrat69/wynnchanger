package io.wynnchanger.client;

public enum SkinPropertyType {
    CUSTOM_MODEL_DATA,
    DAMAGE;

    public static SkinPropertyType fromProperty(String property) {
        if (property == null) {
            return null;
        }
        return switch (property) {
            case "custom_model_data", "minecraft:custom_model_data" -> CUSTOM_MODEL_DATA;
            case "damage", "minecraft:damage" -> DAMAGE;
            default -> null;
        };
    }
}
