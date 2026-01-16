package io.wynnchanger.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

public final class GlintVertexConsumerProvider implements VertexConsumerProvider {
    private final VertexConsumerProvider delegate;
    private final int glintId;

    private GlintVertexConsumerProvider(VertexConsumerProvider delegate, int glintId) {
        this.delegate = delegate;
        this.glintId = glintId;
    }

    public static VertexConsumerProvider wrap(VertexConsumerProvider delegate, int glintId) {
        if (delegate == null || glintId <= 0) {
            return delegate;
        }
        return new GlintVertexConsumerProvider(delegate, glintId);
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        return new GlintVertexConsumer(delegate.getBuffer(layer), glintId);
    }

    private static final class GlintVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final int glintId;

        private GlintVertexConsumer(VertexConsumer delegate, int glintId) {
            this.delegate = delegate;
            this.glintId = glintId;
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            if (glintId > 0) {
                delegate.color(glintId, 255, 0, alpha);
                return this;
            }
            delegate.color(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer color(float red, float green, float blue, float alpha) {
            if (glintId > 0) {
                delegate.color(glintId / 255.0f, 1.0f, 0.0f, alpha);
                return this;
            }
            delegate.color(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            delegate.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            delegate.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            delegate.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }
    }
}
