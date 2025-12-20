package io.wynnchanger.mixin;

import io.wynnchanger.client.model.SkinModelOverride;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
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
}
