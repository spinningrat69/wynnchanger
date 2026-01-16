package io.wynnchanger.mixin;

import io.wynnchanger.client.SkinSwapState;
import io.wynnchanger.client.SkinType;
import io.wynnchanger.client.model.SkinModelOverride;
import io.wynnchanger.client.WynnchangerClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;

@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorFeatureRendererMixin {
    private static final ThreadLocal<EnumSet<EquipmentSlot>> HIDDEN_SLOTS =
            ThreadLocal.withInitial(() -> EnumSet.noneOf(EquipmentSlot.class));

    @Shadow
    protected abstract void renderArmor(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ItemStack stack,
                                        EquipmentSlot slot, int light, BipedEntityModel<?> armorModel);

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
            at = @At("HEAD")
    )
    private void wynnchanger$trackHeadVisibility(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                                 BipedEntityRenderState state, float limbAngle, float limbDistance,
                                                 CallbackInfo ci) {
        EnumSet<EquipmentSlot> hidden = HIDDEN_SLOTS.get();
        hidden.clear();
        hidden.addAll(resolveHiddenSlots(state));
    }

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
            at = @At("TAIL")
    )
    private void wynnchanger$clearHeadVisibility(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                                 BipedEntityRenderState state, float limbAngle, float limbDistance,
                                                 CallbackInfo ci) {
        HIDDEN_SLOTS.get().clear();
    }

    @Redirect(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/feature/ArmorFeatureRenderer;renderArmor(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EquipmentSlot;ILnet/minecraft/client/render/entity/model/BipedEntityModel;)V"
            )
    )
    private void wynnchanger$skipHeadArmor(ArmorFeatureRenderer<?, ?, ?> instance, MatrixStack matrices,
                                           VertexConsumerProvider vertexConsumers, ItemStack stack,
                                           EquipmentSlot slot, int light, BipedEntityModel<?> armorModel) {
        if (HIDDEN_SLOTS.get().contains(slot)) {
            return;
        }
        ItemStack effectiveStack = SkinModelOverride.overrideStack(stack, MinecraftClient.getInstance().player,
                ModelTransformationMode.NONE);
        renderArmor(matrices, vertexConsumers, effectiveStack, slot, light, armorModel);
    }

    private static EnumSet<EquipmentSlot> resolveHiddenSlots(BipedEntityRenderState state) {
        EnumSet<EquipmentSlot> hidden = EnumSet.noneOf(EquipmentSlot.class);
        if (!(state instanceof PlayerEntityRenderState playerState)) {
            return hidden;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || playerState.id != client.player.getId()) {
            return hidden;
        }
        SkinSwapState swapState = WynnchangerClient.getSwapState();
        if (swapState.isHidden(SkinType.HELMET)) {
            hidden.add(EquipmentSlot.HEAD);
        }
        if (swapState.isHidden(SkinType.CHESTPLATE)) {
            hidden.add(EquipmentSlot.CHEST);
        }
        if (swapState.isHidden(SkinType.LEGGINGS)) {
            hidden.add(EquipmentSlot.LEGS);
        }
        if (swapState.isHidden(SkinType.BOOTS)) {
            hidden.add(EquipmentSlot.FEET);
        }
        if (swapState.getSelection(SkinType.HAT).isPresent()) {
            hidden.add(EquipmentSlot.HEAD);
        }
        return hidden;
    }
}
