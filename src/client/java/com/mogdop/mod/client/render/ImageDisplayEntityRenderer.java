package com.mogdop.mod.client.render;

import com.mogdop.mod.entity.ImageDisplayEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class ImageDisplayEntityRenderer extends EntityRenderer<ImageDisplayEntity> {

    public ImageDisplayEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(ImageDisplayEntity entity) {
        return null;
    }

    @Override
    public void render(ImageDisplayEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        String imageName = entity.getImageName();
        if (imageName == null || imageName.isEmpty()) return;

        ClientImageTextureManager.ImageTextureInfo textureInfo = ClientImageTextureManager.getTexture(imageName);
        if (textureInfo == null) return;

        Vec3d p1 = entity.getPos1();
        Vec3d p2 = entity.getPos2();
        Direction side = entity.getFacingSide();

        Vec3d entityPos = entity.getPos();
        Vec3d rel1 = p1.subtract(entityPos);
        Vec3d rel2 = p2.subtract(entityPos);

        matrices.push();

        double minX = Math.min(rel1.x, rel2.x);
        double maxX = Math.max(rel1.x, rel2.x);
        double minY = Math.min(rel1.y, rel2.y);
        double maxY = Math.max(rel1.y, rel2.y);
        double minZ = Math.min(rel1.z, rel2.z);
        double maxZ = Math.max(rel1.z, rel2.z);

        double offset = 0.005;
        double ox = side.getOffsetX() * offset;
        double oy = side.getOffsetY() * offset;
        double oz = side.getOffsetZ() * offset;

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(textureInfo.id()));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f mat = entry.getPositionMatrix();

        float nx = side.getOffsetX();
        float ny = side.getOffsetY();
        float nz = side.getOffsetZ();

        Vec3d c0, c1, c2, c3;

        switch (side) {
            case UP -> {
                c0 = new Vec3d(minX, rel1.y + oy, minZ);
                c1 = new Vec3d(maxX, rel1.y + oy, minZ);
                c2 = new Vec3d(maxX, rel1.y + oy, maxZ);
                c3 = new Vec3d(minX, rel1.y + oy, maxZ);
            }
            case DOWN -> {
                c0 = new Vec3d(minX, rel1.y + oy, maxZ);
                c1 = new Vec3d(maxX, rel1.y + oy, maxZ);
                c2 = new Vec3d(maxX, rel1.y + oy, minZ);
                c3 = new Vec3d(minX, rel1.y + oy, minZ);
            }
            case NORTH -> {
                c0 = new Vec3d(maxX + ox, maxY, rel1.z + oz);
                c1 = new Vec3d(minX + ox, maxY, rel1.z + oz);
                c2 = new Vec3d(minX + ox, minY, rel1.z + oz);
                c3 = new Vec3d(maxX + ox, minY, rel1.z + oz);
            }
            case SOUTH -> {
                c0 = new Vec3d(minX + ox, maxY, rel1.z + oz);
                c1 = new Vec3d(maxX + ox, maxY, rel1.z + oz);
                c2 = new Vec3d(maxX + ox, minY, rel1.z + oz);
                c3 = new Vec3d(minX + ox, minY, rel1.z + oz);
            }
            case WEST -> {
                c0 = new Vec3d(rel1.x + ox, maxY, minZ + oz);
                c1 = new Vec3d(rel1.x + ox, maxY, maxZ + oz);
                c2 = new Vec3d(rel1.x + ox, minY, maxZ + oz);
                c3 = new Vec3d(rel1.x + ox, minY, minZ + oz);
            }
            case EAST -> {
                c0 = new Vec3d(rel1.x + ox, maxY, maxZ + oz);
                c1 = new Vec3d(rel1.x + ox, maxY, minZ + oz);
                c2 = new Vec3d(rel1.x + ox, minY, minZ + oz);
                c3 = new Vec3d(rel1.x + ox, minY, maxZ + oz);
            }
            default -> {
                c0 = new Vec3d(minX, maxY, minZ);
                c1 = new Vec3d(maxX, maxY, minZ);
                c2 = new Vec3d(maxX, minY, maxZ);
                c3 = new Vec3d(minX, minY, maxZ);
            }
        }

        // Передняя грань
        drawVertex(buffer, mat, c0, 0f, 0f, nx, ny, nz, light);
        drawVertex(buffer, mat, c1, 1f, 0f, nx, ny, nz, light);
        drawVertex(buffer, mat, c2, 1f, 1f, nx, ny, nz, light);
        drawVertex(buffer, mat, c3, 0f, 1f, nx, ny, nz, light);

        // Задняя грань (для видимости с обратной стороны)
        drawVertex(buffer, mat, c3, 0f, 1f, -nx, -ny, -nz, light);
        drawVertex(buffer, mat, c2, 1f, 1f, -nx, -ny, -nz, light);
        drawVertex(buffer, mat, c1, 1f, 0f, -nx, -ny, -nz, light);
        drawVertex(buffer, mat, c0, 0f, 0f, -nx, -ny, -nz, light);

        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private void drawVertex(VertexConsumer buffer, Matrix4f mat, Vec3d pos, float u, float v, float nx, float ny, float nz, int light) {
        buffer.vertex(mat, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(nx, ny, nz);
    }
}