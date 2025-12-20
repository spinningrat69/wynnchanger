package io.wynnchanger.client;

import net.minecraft.util.Identifier;

public record SkinEntry(Identifier modelId, String displayName, SkinType type) {
}
