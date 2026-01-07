package io.wynnchanger.client;

import io.wynnchanger.client.model.SkinMappingResolver;
import net.minecraft.item.ItemStack;
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
        if (mapping.isEmpty()) {
            return GlintType.NONE;
        }

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
}
