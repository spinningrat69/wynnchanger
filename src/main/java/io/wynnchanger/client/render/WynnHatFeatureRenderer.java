package io.wynnchanger.client.render;

import io.wynnchanger.client.SkinSwapState;
import io.wynnchanger.client.SkinType;
import io.wynnchanger.client.WynnchangerClient;
import io.wynnchanger.client.model.SkinModelOverride;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.Optional;

public class WynnHatFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    public WynnHatFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        Optional<ItemStack> hatStack = resolveHatStack(state);
        if (hatStack.isEmpty()) {
            return;
        }
        renderHat(matrices, vertexConsumers, light, state, hatStack.get());
    }

    private Optional<ItemStack> resolveHatStack(PlayerEntityRenderState state) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return Optional.empty();
        }
        if (state == null || client.player.getId() != state.id) {
            return Optional.empty();
        }

        SkinSwapState swapState = WynnchangerClient.getSwapState();
        Optional<Identifier> selection = swapState.getSelection(SkinType.HAT);
        if (selection.isEmpty()) {
            return Optional.empty();
        }
        return SkinModelOverride.buildStackForModel(selection.get());
    }

    private void renderHat(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                           PlayerEntityRenderState state, ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();

        matrices.push();
        getContextModel().head.rotate(matrices);
        matrices.translate(0.0F, -0.25F, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        matrices.scale(0.625F, -0.625F, -0.625F);
        client.getItemRenderer().renderItem(
                stack,
                ModelTransformationMode.HEAD,
                light,
                OverlayTexture.DEFAULT_UV,
                matrices,
                vertexConsumers,
                client.world,
                state.id
        );
        matrices.pop();
    }
}
