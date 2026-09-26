package com.mogdop.mod.client.render;

import com.mogdop.mod.entity.NextbotEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;

public class NextbotEntityRenderer extends EntityRenderer<NextbotEntity> {

    public NextbotEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.4F;
    }

    @Override
    public Identifier getTexture(NextbotEntity entity) {
        ClientImageTextureManager.ImageTextureInfo info = ClientImageTextureManager.getTexture(entity.getTextureName());
        if (info != null) return info.id();
        return null;
    }

    @Override
    public void render(NextbotEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        String imageName = entity.getTextureName();
        ClientImageTextureManager.ImageTextureInfo info = null;
        if (imageName != null && !imageName.isEmpty()) {
            info = ClientImageTextureManager.getTexture(imageName);
        }
        if (info == null) {
            super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
            return;
        }
        Identifier textureId = info.id();

        BlockPos pos = BlockPos.ofFloored(entity.getPos());
        int renderLight = WorldRenderer.getLightmapCoordinates(entity.getWorld(), pos);
        if (renderLight == 0) renderLight = light;

        matrices.push();
        // Центр билборда на уровне глаз (~0.9 блока от ног)
        matrices.translate(0.0, 0.9, 0.0);
        matrices.multiply(this.dispatcher.getRotation());

        float w = 1.2F;
        float h = 1.2F;
        float hw = w / 2.0F;
        float hh = h / 2.0F;

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(textureId));
        Matrix4f mat = matrices.peek().getPositionMatrix();

        // Квад всегда лицом к камере; UV 0,0 сверху-слева
        buffer.vertex(mat, -hw, -hh, 0.0F).color(255, 255, 255, 255).texture(0f, 1f).overlay(OverlayTexture.DEFAULT_UV).light(renderLight).normal(0f, 0f, 1f);
        buffer.vertex(mat, hw, -hh, 0.0F).color(255, 255, 255, 255).texture(1f, 1f).overlay(OverlayTexture.DEFAULT_UV).light(renderLight).normal(0f, 0f, 1f);
        buffer.vertex(mat, hw, hh, 0.0F).color(255, 255, 255, 255).texture(1f, 0f).overlay(OverlayTexture.DEFAULT_UV).light(renderLight).normal(0f, 0f, 1f);
        buffer.vertex(mat, -hw, hh, 0.0F).color(255, 255, 255, 255).texture(0f, 0f).overlay(OverlayTexture.DEFAULT_UV).light(renderLight).normal(0f, 0f, 1f);

        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, renderLight);
    }
}
