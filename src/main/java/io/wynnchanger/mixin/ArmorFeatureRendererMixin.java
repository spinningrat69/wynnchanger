package io.wynnchanger.mixin;

import io.wynnchanger.client.SkinType;
import io.wynnchanger.client.WynnchangerClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorFeatureRendererMixin {
    private static final ThreadLocal<Boolean> HIDE_HEAD_SLOT = ThreadLocal.withInitial(() -> false);

    @Shadow
    protected abstract void renderArmor(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack,
                                        EquipmentSlot slot, int light, BipedEntityModel<?> armorModel);

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD")
    )
    private void wynnchanger$trackHeadVisibility(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                                 LivingEntityRenderState state, float limbAngle, float limbDistance,
                                                 CallbackInfo ci) {
        HIDE_HEAD_SLOT.set(shouldHideHead(state));
    }

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/LivingEntityRenderState;FF)V",
            at = @At("TAIL")
    )
    private void wynnchanger$clearHeadVisibility(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                                 LivingEntityRenderState state, float limbAngle, float limbDistance,
                                                 CallbackInfo ci) {
        HIDE_HEAD_SLOT.set(false);
    }

    @Redirect(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/LivingEntityRenderState;FF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/feature/ArmorFeatureRenderer;renderArmor(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EquipmentSlot;ILnet/minecraft/client/render/entity/model/BipedEntityModel;)V",
                    ordinal = 3
            )
    )
    private void wynnchanger$skipHeadArmor(ArmorFeatureRenderer<?, ?, ?> instance, MatrixStack matrices,
                                           VertexConsumerProvider vertexConsumers, ItemStack stack,
                                           EquipmentSlot slot, int light, BipedEntityModel<?> armorModel) {
        if (slot == EquipmentSlot.HEAD && Boolean.TRUE.equals(HIDE_HEAD_SLOT.get())) {
            return;
        }
        renderArmor(matrices, vertexConsumers, stack, slot, light, armorModel);
    }

    private static boolean shouldHideHead(LivingEntityRenderState state) {
        if (!(state instanceof PlayerEntityRenderState playerState)) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || playerState.id != client.player.getId()) {
            return false;
        }
        return WynnchangerClient.getSwapState().getSelection(SkinType.HAT).isPresent();
    }
}
