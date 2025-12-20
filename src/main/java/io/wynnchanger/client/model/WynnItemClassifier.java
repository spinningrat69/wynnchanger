package io.wynnchanger.client.model;

import io.wynnchanger.client.SkinType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.item.ItemStack;

import java.util.Map;
import java.util.Optional;

public final class WynnItemClassifier {
    private static final Map<Identifier, SkinType> FALLBACK_TYPES = Map.of(
            Identifier.of("minecraft", "bow"), SkinType.BOW,
            Identifier.of("minecraft", "shears"), SkinType.DAGGER,
            Identifier.of("minecraft", "iron_shovel"), SkinType.SPEAR,
            Identifier.of("minecraft", "wooden_shovel"), SkinType.WAND,
            Identifier.of("minecraft", "stone_shovel"), SkinType.RELIK,
            Identifier.of("minecraft", "leather_helmet"), SkinType.HELMET,
            Identifier.of("minecraft", "leather_chestplate"), SkinType.CHESTPLATE,
            Identifier.of("minecraft", "leather_leggings"), SkinType.LEGGINGS,
            Identifier.of("minecraft", "leather_boots"), SkinType.BOOTS
    );

    private WynnItemClassifier() {
    }

    public static SkinType classify(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return SkinType.UNKNOWN;
        }
        Optional<SkinType> mapped = SkinMappingResolver.resolveType(stack);
        if (mapped.isPresent()) {
            return mapped.get();
        }
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        if (itemId == null) {
            return SkinType.UNKNOWN;
        }
        SkinType fallback = FALLBACK_TYPES.get(itemId);
        return fallback != null ? fallback : SkinType.UNKNOWN;
    }
}
