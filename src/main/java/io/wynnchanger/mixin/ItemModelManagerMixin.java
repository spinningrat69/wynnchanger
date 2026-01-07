package io.wynnchanger.mixin;

import io.wynnchanger.client.GlintType;
import io.wynnchanger.client.WynnGlint;
import io.wynnchanger.client.model.SkinModelOverride;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ItemModelManager.class)
public class ItemModelManagerMixin {
    @ModifyArgs(
            method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/model/ItemModel;update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/item/ItemModelManager;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/entity/LivingEntity;I)V"
            )
    )
    private void wynnchanger$swapItemStack(Args args) {
        ItemStack original = args.get(1);
        ModelTransformationMode mode = args.get(3);
        LivingEntity entity = args.get(5);
        args.set(1, SkinModelOverride.overrideStack(original, entity, mode));
    }

    @Inject(
            method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
            at = @At("RETURN")
    )
    private void wynnchanger$applyGlint(ItemRenderState renderState, ItemStack stack, ModelTransformationMode mode,
                                        World world, LivingEntity entity, int seed, CallbackInfo ci) {
        GlintType glint = WynnGlint.resolveGlintForStack(stack);
        if (glint == null || glint.isNone()) {
            return;
        }

        int tintColor = glint.toTintColor();
        ItemRenderStateAccessor accessor = (ItemRenderStateAccessor) renderState;
        ItemRenderState.LayerRenderState[] layers = accessor.wynnchanger$getLayers();
        int layerCount = accessor.wynnchanger$getLayerCount();
        int cappedCount = Math.min(layerCount, layers.length);

        for (int i = 0; i < cappedCount; i++) {
            ItemRenderState.LayerRenderState layer = layers[i];
            if (layer == null) {
                continue;
            }
            int[] tints = ((ItemRenderStateLayerAccessor) layer).wynnchanger$getTints();
            if (tints == null || tints.length == 0) {
                tints = layer.initTints(1);
            }
            if (tints == null || tints.length == 0) {
                continue;
            }
            tints[0] = tintColor;
        }
    }
}
