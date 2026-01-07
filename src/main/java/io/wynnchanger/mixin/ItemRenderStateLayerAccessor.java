package io.wynnchanger.mixin;

import net.minecraft.client.render.item.ItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderState.LayerRenderState.class)
public interface ItemRenderStateLayerAccessor {
    @Accessor("tints")
    int[] wynnchanger$getTints();
}
