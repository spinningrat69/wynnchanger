package io.wynnchanger.client;

import io.wynnchanger.client.model.SkinMappingResolver;
import io.wynnchanger.client.model.SkinModelOverride;
import io.wynnchanger.client.model.WynnItemClassifier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.Optional;

public final class WynnGlint {
    private static final ThreadLocal<GlintType> PREVIEW_GLINT = new ThreadLocal<>();

    private WynnGlint() {
    }

    public static void withPreviewGlint(GlintType glint, Runnable action) {
        GlintType previous = PREVIEW_GLINT.get();
        PREVIEW_GLINT.set(glint);
        try {
            action.run();
        } finally {
            PREVIEW_GLINT.set(previous);
        }
    }

    public static GlintType resolveGlintForStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !GlintSupport.isSupported()) {
            return GlintType.NONE;
        }

        GlintType preview = PREVIEW_GLINT.get();
        if (preview != null) {
            return GlintSupport.isGlintAvailable(preview) ? preview : GlintType.NONE;
        }

        Optional<SkinModelMapping> mapping = SkinMappingResolver.resolveMappingForStack(stack);
        if (mapping.isPresent()) {
            SkinModelMapping resolved = mapping.get();
            SkinType type = resolved.type();
            SkinSwapState state = WynnchangerClient.getSwapState();
            Optional<Identifier> selected = state.getSelection(type);
            if (selected.isEmpty() || !selected.get().equals(resolved.modelId())) {
                return GlintType.NONE;
            }

            GlintType glint = state.getGlint(type).orElse(GlintType.NONE);
            return GlintSupport.isGlintAvailable(glint) ? glint : GlintType.NONE;
        }

        SkinType type = WynnItemClassifier.classify(stack);
        if (!type.isArmorType()) {
            return GlintType.NONE;
        }
        SkinSwapState state = WynnchangerClient.getSwapState();
        Optional<Identifier> selected = state.getSelection(type);
        if (selected.isEmpty()) {
            return GlintType.NONE;
        }
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable == null) {
            return GlintType.NONE;
        }
        Optional<RegistryKey<EquipmentAsset>> expectedKey =
                SkinModelOverride.resolveEquipmentAssetKey(type, selected.get());
        if (expectedKey.isEmpty() || equippable.assetId().isEmpty()) {
            return GlintType.NONE;
        }
        if (!expectedKey.get().equals(equippable.assetId().get())) {
            return GlintType.NONE;
        }

        GlintType glint = state.getGlint(type).orElse(GlintType.NONE);
        return GlintSupport.isGlintAvailable(glint) ? glint : GlintType.NONE;
    }
}
