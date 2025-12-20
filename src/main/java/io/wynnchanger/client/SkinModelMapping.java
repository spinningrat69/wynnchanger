package io.wynnchanger.client;

import net.minecraft.util.Identifier;

public record SkinModelMapping(
        Identifier modelId,
        SkinType type,
        SkinPropertyType propertyType,
        float value
) {
}
