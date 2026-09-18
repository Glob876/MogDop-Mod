package com.mogdop.mod.neoforge.client;

import dev.architectury.networking.NetworkManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LevelRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

public class MogDopSModNeoForgeClient {

    public static void init(IEventBus modEventBus) {
        try {
            Class<?> clientCls = Class.forName("com.mogdop.mod.client.MogDopSModClient");
            clientCls.getMethod("initClient").invoke(null);
        } catch (Exception e) {
            // Fallback: log
            try { Class.forName("com.mogdop.mod.MogDopSMod").getField("LOGGER").get(null); } catch (Exception ignored) {}
        }

        NeoForge.EVENT_BUS.addListener(MogDopSModNeoForgeClient::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(MogDopSModNeoForgeClient::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(MogDopSModNeoForgeClient::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(MogDopSModNeoForgeClient::onRenderLevelStage);
    }

    private static boolean isHoldingStaff(Minecraft client) {
        if (client.player == null) return false;
        ItemStack stack = client.player.getMainHandItem();
        if (stack.isEmpty()) return false;
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return id.contains("staff") || id.contains("iron_axe");
    }

    private static void openScreenSafe(Object screen) {
        Minecraft.getInstance().setScreen((net.minecraft.client.gui.screens.Screen) screen);
    }

    // Кэшированные Field — через Class.forName чтобы не грузить MogDopSModClient в <clinit>
    private static Field FIELD_POS1, FIELD_POS2, FIELD_SELECTION_POINTS,
            FIELD_IMAGE_POS1, FIELD_IMAGE_POS2, FIELD_IMAGE_SIDE, FIELD_SELECTION_LINES, FIELD_SELECTION_QUADS;
    private static boolean fieldsCached = false;
    private static void ensureFieldsCached() {
        if (fieldsCached) return;
        try {
            Class<?> cls = Class.forName("com.mogdop.mod.client.MogDopSModClient");
            FIELD_POS1 = cls.getField("pos1");
            FIELD_POS2 = cls.getField("pos2");
            FIELD_SELECTION_POINTS = cls.getField("selectionPoints");
            FIELD_IMAGE_POS1 = cls.getField("imagePos1");
            FIELD_IMAGE_POS2 = cls.getField("imagePos2");
            FIELD_IMAGE_SIDE = cls.getField("imageSide");
            FIELD_SELECTION_LINES = cls.getField("SELECTION_LINES");
            FIELD_SELECTION_QUADS = cls.getField("SELECTION_QUADS");
            fieldsCached = true;
        } catch (Exception e) { throw new RuntimeException("Failed to cache MogDopSModClient fields", e); }
    }

    private static void setPos1(BlockPos pos) {
        try { ensureFieldsCached(); FIELD_POS1.set(null, pos); } catch (Exception e) { org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("setPos1 failed", e); }
    }
    private static void setPos2(BlockPos pos) {
        try { ensureFieldsCached(); FIELD_POS2.set(null, pos); } catch (Exception e) { org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("setPos2 failed", e); }
    }
    private static BlockPos getPos1() {
        try { ensureFieldsCached(); return (BlockPos) FIELD_POS1.get(null); } catch (Exception e) { return null; }
    }
    private static BlockPos getPos2() {
        try { ensureFieldsCached(); return (BlockPos) FIELD_POS2.get(null); } catch (Exception e) { return null; }
    }
    @SuppressWarnings("unchecked")
    private static List<BlockPos> getSelectionPointsRaw() {
        try { ensureFieldsCached(); return (List<BlockPos>) (List) FIELD_SELECTION_POINTS.get(null); }
        catch (Exception e) { return List.of(); }
    }
    private static Vec3 getImagePos1() {
        try { ensureFieldsCached(); return (Vec3) FIELD_IMAGE_POS1.get(null); } catch (Exception e) { return null; }
    }
    private static Vec3 getImagePos2() {
        try { ensureFieldsCached(); return (Vec3) FIELD_IMAGE_POS2.get(null); } catch (Exception e) { return null; }
    }
    private static void setImagePos1(Vec3 v) {
        try { ensureFieldsCached(); FIELD_IMAGE_POS1.set(null, v); } catch (Exception e) { org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("setImagePos1", e); }
    }
    private static void setImagePos2(Vec3 v) {
        try { ensureFieldsCached(); FIELD_IMAGE_POS2.set(null, v); } catch (Exception e) { org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("setImagePos2", e); }
    }
    private static void setImageSide(Direction dir) {
        try { ensureFieldsCached(); FIELD_IMAGE_SIDE.set(null, dir); } catch (Exception e) { org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("setImageSide", e); }
    }
    private static Direction getImageSide() {
        try { ensureFieldsCached(); return (Direction) FIELD_IMAGE_SIDE.get(null); } catch (Exception e) { return Direction.UP; }
    }
    private static Object getSelectionLines() {
        try { ensureFieldsCached(); return FIELD_SELECTION_LINES.get(null); } catch (Exception e) { return null; }
    }

    private static int getCurrentToolMode() {
        try { return (int) Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("currentToolMode").get(null); } catch (Exception e) { return 0; }
    }
    private static int getCurrentSelectionMode() {
        try { return (int) Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("currentSelectionMode").get(null); } catch (Exception e) { return 0; }
    }
    private static void syncSelectionPointsReflect() {
        try { Class.forName("com.mogdop.mod.client.MogDopSModClient").getMethod("syncSelectionPoints").invoke(null); } catch (Exception ignored) {}
    }
    private static double snap16Reflect(double v) {
        try { return (double) Class.forName("com.mogdop.mod.client.MogDopSModClient").getMethod("snap16", double.class).invoke(null, v); } catch (Exception e) { return v; }
    }
    private static float[] getSelectionColorReflect() {
        try { return (float[]) Class.forName("com.mogdop.mod.client.MogDopSModClient").getMethod("getSelectionColor").invoke(null); } catch (Exception e) { return new float[]{1,0.6f,0,1}; }
    }
    private static int getToolRemoverRadius() {
        try {
            Object cfg = Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("CONFIG").get(null);
            return (int) cfg.getClass().getMethod("toolRemoverRadius").invoke(cfg);
        } catch (Exception e) { return 1; }
    }
    private static float getToolExplosionPower() {
        try {
            Object cfg = Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("CONFIG").get(null);
            return (float) cfg.getClass().getMethod("toolExplosionPower").invoke(cfg);
        } catch (Exception e) { return 4.0f; }
    }
    private static boolean getToolExplosionFire() {
        try {
            Object cfg = Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("CONFIG").get(null);
            return (boolean) cfg.getClass().getMethod("toolExplosionFire").invoke(cfg);
        } catch (Exception e) { return false; }
    }
    private static String getActiveSpawnId() {
        try { return (String) Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("activeSpawnId").get(null); } catch (Exception e) { return "minecraft:cow"; }
    }
    private static String getActiveSpawnCustomName() {
        try { return (String) Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("activeSpawnCustomName").get(null); } catch (Exception e) { return ""; }
    }
    private static boolean getActiveSpawnNameVisible() {
        try { return (boolean) Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("activeSpawnNameVisible").get(null); } catch (Exception e) { return false; }
    }
    private static boolean getActiveSpawnNoGravity() {
        try { return (boolean) Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("activeSpawnNoGravity").get(null); } catch (Exception e) { return false; }
    }
    private static boolean getActiveSpawnSilent() {
        try { return (boolean) Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("activeSpawnSilent").get(null); } catch (Exception e) { return false; }
    }
    private static boolean getActiveSpawnGlowing() {
        try { return (boolean) Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("activeSpawnGlowing").get(null); } catch (Exception e) { return false; }
    }
    private static boolean getActiveSpawnIsBaby() {
        try { return (boolean) Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("activeSpawnIsBaby").get(null); } catch (Exception e) { return false; }
    }
    private static int getActiveSpawnSlimeSize() {
        try { return (int) Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("activeSpawnSlimeSize").get(null); } catch (Exception e) { return 0; }
    }
    private static int getActiveSpawnFireTicks() {
        try { return (int) Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("activeSpawnFireTicks").get(null); } catch (Exception e) { return 0; }
    }
    private static boolean getSchematicPreviewActive() {
        try { return (boolean) Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("schematicPreviewActive").get(null); } catch (Exception e) { return false; }
    }
    private static int getSchematicSizeX() {
        try { return (int) Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("schematicSizeX").get(null); } catch (Exception e) { return 1; }
    }
    private static int getSchematicSizeY() {
        try { return (int) Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("schematicSizeY").get(null); } catch (Exception e) { return 1; }
    }
    private static int getSchematicSizeZ() {
        try { return (int) Class.forName("com.mogdop.mod.client.MogDopSModClient").getField("schematicSizeZ").get(null); } catch (Exception e) { return 1; }
    }

    // Создаёт ToolActionPayload через рефлексию, обходя проверку типа BlockPos (Yarn vs Mojang)
    private static Object createToolPayload(String action, BlockPos pos, float power, boolean fire, int radius) {
        try {
            Class<?> clazz = Class.forName("com.mogdop.mod.network.ToolActionPayload");
            for (var ctor : clazz.getConstructors()) {
                var params = ctor.getParameterTypes();
                if (params.length == 5 && params[0] == String.class) {
                    return ctor.newInstance(action, pos, power, fire, radius);
                }
            }
            // Fallback: record canonical constructor
            return clazz.getConstructors()[0].newInstance(action, pos, power, fire, radius);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("createToolPayload failed", e);
            return null;
        }
    }
    @SuppressWarnings("unchecked")
    private static void sendToServer(Object payload) {
        try {
            var method = NetworkManager.class.getMethod("sendToServer", net.minecraft.network.protocol.common.custom.CustomPacketPayload.class);
            method.invoke(null, payload);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("sendToServer failed", e);
        }
    }

    private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide()) return;
        Minecraft client = Minecraft.getInstance();
        if (!isHoldingStaff(client) || client.player == null) return;

        BlockPos pos = event.getPos();

        switch (getCurrentToolMode()) {
            case 0 -> {
                if (getCurrentSelectionMode() == 0) {
                    setPos1(pos);
                    syncSelectionPointsReflect();
                    client.player.displayClientMessage(Component.translatable("mogdops-mod.selection.pos1", pos.toShortString()), true);
                } else {
                    getSelectionPointsRaw().add(pos);
                    client.player.displayClientMessage(Component.translatable("mogdops-mod.selection.point_added", getSelectionPointsRaw().size(), pos.toShortString()), true);
                }
                event.setCanceled(true);
            }
            case 1 -> {
                Object payload = createToolPayload("REMOVER", pos, 0F, false, getToolRemoverRadius());
                if (payload != null) sendToServer(payload);
                event.setCanceled(true);
            }
            case 2 -> {
                Object payload = createToolPayload("EXPLOSION", pos, getToolExplosionPower(), getToolExplosionFire(), 1);
                if (payload != null) sendToServer(payload);
                event.setCanceled(true);
            }
            case 3 -> {
                Object payload = createToolPayload("TELEPORT", pos, 0F, false, 1);
                if (payload != null) sendToServer(payload);
                event.setCanceled(true);
            }
            case 4 -> {
                try { Class<?> c = Class.forName("com.mogdop.mod.network.SpawnEntityPayload"); Object p = c.getConstructor(String.class, String.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, int.class, int.class).newInstance(getActiveSpawnId(), getActiveSpawnCustomName(), getActiveSpawnNameVisible(), getActiveSpawnNoGravity(), getActiveSpawnSilent(), getActiveSpawnGlowing(), getActiveSpawnIsBaby(), getActiveSpawnSlimeSize(), getActiveSpawnFireTicks()); sendToServer(p); } catch (Exception e) { org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("SpawnEntityPayload failed", e); }
                event.setCanceled(true);
            }
            case 6 -> {
                if (client.hitResult instanceof BlockHitResult hitResult) {
                    Direction side = hitResult.getDirection();
                    Vec3 hit = hitResult.getLocation();
                    double x = snap16Reflect(hit.x);
                    double y = snap16Reflect(hit.y);
                    double z = snap16Reflect(hit.z);

                    switch (side) {
                        case UP -> y = pos.getY() + 1.0;
                        case DOWN -> y = pos.getY();
                        case NORTH -> z = pos.getZ();
                        case SOUTH -> z = pos.getZ() + 1.0;
                        case WEST -> x = pos.getX();
                        case EAST -> x = pos.getX() + 1.0;
                    }

                    setImagePos1(new Vec3(x, y, z));
                    setImageSide(side);
                    setImagePos2(null);
                    client.player.displayClientMessage(Component.literal(String.format(Locale.ROOT, "§a[Изображение] Точка 1: (%.2f, %.2f, %.2f) на грани %s", x, y, z, side.getName())), true);
                }
                event.setCanceled(true);
            }
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) return;
        Minecraft client = Minecraft.getInstance();
        if (!isHoldingStaff(client) || client.player == null) return;

        BlockPos pos = event.getPos();

        switch (getCurrentToolMode()) {
            case 0 -> {
                if (getCurrentSelectionMode() == 0) {
                    setPos2(pos);
                    syncSelectionPointsReflect();
                    client.player.displayClientMessage(Component.translatable("mogdops-mod.selection.pos2", pos.toShortString()), true);
                } else {
                    List<BlockPos> points = getSelectionPointsRaw();
                    if (!points.isEmpty()) {
                        points.remove(points.size() - 1);
                        client.player.displayClientMessage(Component.translatable("mogdops-mod.selection.point_removed", points.size()), true);
                    }
                }
                event.setCanceled(true);
            }
            case 1 -> {
                Object payload = createToolPayload("REMOVER", pos, 0F, false, getToolRemoverRadius());
                if (payload != null) sendToServer(payload);
                event.setCanceled(true);
            }
            case 2 -> {
                Object payload = createToolPayload("EXPLOSION", pos, getToolExplosionPower(), getToolExplosionFire(), 1);
                if (payload != null) sendToServer(payload);
                event.setCanceled(true);
            }
            case 3 -> {
                Object payload = createToolPayload("TELEPORT", pos, 0F, false, 1);
                if (payload != null) sendToServer(payload);
                event.setCanceled(true);
            }
            case 4 -> {
                try { Class<?> c = Class.forName("com.mogdop.mod.network.SpawnEntityPayload"); Object p = c.getConstructor(String.class, String.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, int.class, int.class).newInstance(getActiveSpawnId(), getActiveSpawnCustomName(), getActiveSpawnNameVisible(), getActiveSpawnNoGravity(), getActiveSpawnSilent(), getActiveSpawnGlowing(), getActiveSpawnIsBaby(), getActiveSpawnSlimeSize(), getActiveSpawnFireTicks()); sendToServer(p); } catch (Exception e) { org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("SpawnEntityPayload failed", e); }
                event.setCanceled(true);
            }
            case 6 -> {
                if (getImagePos1() == null) {
                    client.player.displayClientMessage(Component.literal("§c[Изображение] Сначала установите первую точку (ЛКМ)!"), true);
                    event.setCanceled(true);
                    return;
                }
                BlockHitResult hitResult = event.getHitVec();
                Vec3 hit = hitResult.getLocation();
                double x = snap16Reflect(hit.x);
                double y = snap16Reflect(hit.y);
                double z = snap16Reflect(hit.z);

                Direction.Axis axis = hitResult.getDirection().getAxis();
                Vec3 p1 = getImagePos1();
                if (axis == Direction.Axis.X) x = p1.x;
                else if (axis == Direction.Axis.Y) y = p1.y;
                else if (axis == Direction.Axis.Z) z = p1.z;

                setImagePos2(new Vec3(x, y, z));
                client.player.displayClientMessage(Component.literal(String.format(Locale.ROOT, "§b[Изображение] Точка 2: (%.2f, %.2f, %.2f)", x, y, z)), true);
                try { Class<?> c = Class.forName("com.mogdop.mod.client.gui.ImageEditorPanelScreen"); Object s = c.getDeclaredConstructor().newInstance(); openScreenSafe(s); } catch (Exception e) { org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("ImageEditorPanelScreen failed", e); }
                event.setCanceled(true);
            }
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) return;
        Minecraft client = Minecraft.getInstance();
        if (!isHoldingStaff(client) || client.player == null) return;

        HitResult hit = client.hitResult;
        if (hit == null) return;

        switch (getCurrentToolMode()) {
            case 1 -> {
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos bPos = ((BlockHitResult) hit).getBlockPos();
                    Object payload = createToolPayload("REMOVER", bPos, 0F, false, getToolRemoverRadius());
                    if (payload != null) sendToServer(payload);
                }
            }
            case 2 -> {
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos bPos = ((BlockHitResult) hit).getBlockPos();
                    Object payload = createToolPayload("EXPLOSION", bPos, getToolExplosionPower(), getToolExplosionFire(), 1);
                    if (payload != null) sendToServer(payload);
                }
            }
            case 3 -> {
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos bPos = ((BlockHitResult) hit).getBlockPos();
                    Object payload = createToolPayload("TELEPORT", bPos, 0F, false, 1);
                    if (payload != null) sendToServer(payload);
                }
            }
            case 4 -> {
                try { Class<?> c = Class.forName("com.mogdop.mod.network.SpawnEntityPayload"); Object p = c.getConstructor(String.class, String.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, int.class, int.class).newInstance(getActiveSpawnId(), getActiveSpawnCustomName(), getActiveSpawnNameVisible(), getActiveSpawnNoGravity(), getActiveSpawnSilent(), getActiveSpawnGlowing(), getActiveSpawnIsBaby(), getActiveSpawnSlimeSize(), getActiveSpawnFireTicks()); sendToServer(p); } catch (Exception e) { org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("SpawnEntityPayload failed", e); }
            }
            case 5 -> {
                try { Class<?> c = Class.forName("com.mogdop.mod.client.gui.SchematicScreen"); Object s = c.getDeclaredConstructor().newInstance(); openScreenSafe(s); } catch (Exception e) { org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("SchematicScreen failed", e); }
            }
        }
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft client = Minecraft.getInstance();
        if (!isHoldingStaff(client) || client.level == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack matrices = event.getPoseStack();
        MultiBufferSource.BufferSource consumers = client.renderBuffers().bufferSource();
        if (consumers == null) return;

        float[] col = getSelectionColorReflect();
        // Early cull: если выделение слишком далеко — не рендерим против зависания
        BlockPos p1c = getPos1();
        BlockPos p2c = getPos2();
        if (p1c != null && p2c != null) {
            double cx = (p1c.getX() + p2c.getX()) * 0.5;
            double cy = (p1c.getY() + p2c.getY()) * 0.5;
            double cz = (p1c.getZ() + p2c.getZ()) * 0.5;
            if (camPos.distanceToSqr(cx, cy, cz) > 128*128*4) return;
        }

        // 1. Картинки
        if (getCurrentToolMode() == 6 && getImagePos1() != null) {
            Vec3 p1 = getImagePos1();
            Vec3 p2 = getImagePos2();

            if (p2 != null) {
                matrices.pushPose();
                matrices.translate(-camPos.x, -camPos.y, -camPos.z);

                double minX = Math.min(p1.x, p2.x); double maxX = Math.max(p1.x, p2.x);
                double minY = Math.min(p1.y, p2.y); double maxY = Math.max(p1.y, p2.y);
                double minZ = Math.min(p1.z, p2.z); double maxZ = Math.max(p1.z, p2.z);

                Object linesLayer = getSelectionLines();
                VertexConsumer lines = consumers.getBuffer((net.minecraft.client.renderer.RenderType) linesLayer);
                PoseStack.Pose entry = matrices.last();

                lines.addVertex(entry, (float)minX, (float)minY, (float)minZ).setColor(0.0F, 0.8F, 1.0F, 1.0F).setNormal(0, 1, 0);
                lines.addVertex(entry, (float)maxX, (float)maxY, (float)maxZ).setColor(0.0F, 0.8F, 1.0F, 1.0F).setNormal(0, 1, 0);

                matrices.popPose();
            }
        }

        BlockPos pos1 = getPos1();
        BlockPos pos2 = getPos2();

        // 2. Кубоид — ограничиваем рендер огромных выделений против GPU hang
        if (getCurrentToolMode() == 0 && getCurrentSelectionMode() == 0 && pos1 != null && pos2 != null) {
            long vol = (long)(Math.abs(pos1.getX()-pos2.getX())+1) * (long)(Math.abs(pos1.getY()-pos2.getY())+1) * (long)(Math.abs(pos1.getZ()-pos2.getZ())+1);
            if (vol > 500_000) {
                // Слишком большая — пропускаем тяжелый рендер (иначе зависание)
                // Можно показать только угловые маркеры, но сейчас skip
            } else {
                double minX = Math.min(pos1.getX(), pos2.getX());
                double minY = Math.min(pos1.getY(), pos2.getY());
                double minZ = Math.min(pos1.getZ(), pos2.getZ());
                double maxX = Math.max(pos1.getX(), pos2.getX()) + 1.0;
                double maxY = Math.max(pos1.getY(), pos2.getY()) + 1.0;
                double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1.0;

                matrices.pushPose();
                matrices.translate(-camPos.x, -camPos.y, -camPos.z);

                Object linesLayer = getSelectionLines();
                VertexConsumer linesConsumer = consumers.getBuffer((net.minecraft.client.renderer.RenderType) linesLayer);
                LevelRenderer.renderLineBox(matrices, linesConsumer, minX, minY, minZ, maxX, maxY, maxZ, col[0], col[1], col[2], 1.0F);

                matrices.popPose();
            }
        }

        List<BlockPos> points = getSelectionPointsRaw();
        // Лимит точек — иначе зависание рендера
        if (points.size() > 64) points = points.subList(0, 64);

        // 3. Полигон 2D
        if (getCurrentToolMode() == 0 && getCurrentSelectionMode() == 1 && !points.isEmpty()) {
            matrices.pushPose();
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);

            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (BlockPos p : points) {
                if (p.getY() < minY) minY = p.getY();
                if (p.getY() > maxY) maxY = p.getY();
            }
            double y1 = minY;
            double y2 = maxY + 1.0;
            int n = points.size();

            Object linesLayer = getSelectionLines();
            VertexConsumer linesConsumer = consumers.getBuffer((net.minecraft.client.renderer.RenderType) linesLayer);
            for (int i = 0; i < n; i++) {
                BlockPos p = points.get(i);
                LevelRenderer.renderLineBox(matrices, linesConsumer, p.getX(), p.getY(), p.getZ(), p.getX() + 1.0, p.getY() + 1.0, p.getZ() + 1.0, 1.0F, 0.8F, 0.0F, 1.0F);

                double xA = p.getX() + 0.5, zA = p.getZ() + 0.5;
                PoseStack.Pose entry = matrices.last();
                linesConsumer.addVertex(entry, (float)xA, (float)y1, (float)zA).setColor(col[0], col[1], col[2], 1.0F).setNormal(0, 1, 0);
                linesConsumer.addVertex(entry, (float)xA, (float)y2, (float)zA).setColor(col[0], col[1], col[2], 1.0F).setNormal(0, 1, 0);

                if (i < n - 1 || n >= 3) {
                    BlockPos nextP = points.get((i + 1) % n);
                    double xB = nextP.getX() + 0.5, zB = nextP.getZ() + 0.5;
                    linesConsumer.addVertex(entry, (float)xA, (float)y1, (float)zA).setColor(col[0], col[1], col[2], 1.0F).setNormal(0, 1, 0);
                    linesConsumer.addVertex(entry, (float)xB, (float)y1, (float)zB).setColor(col[0], col[1], col[2], 1.0F).setNormal(0, 1, 0);
                    linesConsumer.addVertex(entry, (float)xA, (float)y2, (float)zA).setColor(col[0], col[1], col[2], 1.0F).setNormal(0, 1, 0);
                    linesConsumer.addVertex(entry, (float)xB, (float)y2, (float)zB).setColor(col[0], col[1], col[2], 1.0F).setNormal(0, 1, 0);
                }
            }

            matrices.popPose();
        }

        // 4. Выпуклое тело 3D
        if (getCurrentToolMode() == 0 && getCurrentSelectionMode() == 2 && !points.isEmpty()) {
            matrices.pushPose();
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);

            int n = points.size();
            Object linesLayer = getSelectionLines();
            VertexConsumer linesConsumer = consumers.getBuffer((net.minecraft.client.renderer.RenderType) linesLayer);
            PoseStack.Pose entry = matrices.last();

            for (int i = 0; i < n; i++) {
                BlockPos p = points.get(i);
                LevelRenderer.renderLineBox(matrices, linesConsumer, p.getX(), p.getY(), p.getZ(), p.getX() + 1.0, p.getY() + 1.0, p.getZ() + 1.0, 1.0F, 0.3F, 0.8F, 1.0F);

                if (i > 0) {
                    BlockPos prev = points.get(i - 1);
                    linesConsumer.addVertex(entry, (float)(prev.getX() + 0.5), (float)(prev.getY() + 0.5), (float)(prev.getZ() + 0.5)).setColor(0F, 1F, 1F, 1F).setNormal(0, 1, 0);
                    linesConsumer.addVertex(entry, (float)(p.getX() + 0.5), (float)(p.getY() + 0.5), (float)(p.getZ() + 0.5)).setColor(0F, 1F, 1F, 1F).setNormal(0, 1, 0);
                }
                if (i == n - 1 && n >= 3) {
                    BlockPos first = points.get(0);
                    linesConsumer.addVertex(entry, (float)(p.getX() + 0.5), (float)(p.getY() + 0.5), (float)(p.getZ() + 0.5)).setColor(0F, 1F, 1F, 1F).setNormal(0, 1, 0);
                    linesConsumer.addVertex(entry, (float)(first.getX() + 0.5), (float)(first.getY() + 0.5), (float)(first.getZ() + 0.5)).setColor(0F, 1F, 1F, 1F).setNormal(0, 1, 0);
                }
            }

            matrices.popPose();
        }

        // 5. Предпросмотр Схематики
        if (getSchematicPreviewActive() && client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos target = ((BlockHitResult) client.hitResult).getBlockPos().relative(((BlockHitResult) client.hitResult).getDirection());
            matrices.pushPose();
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);

            Object linesLayer = getSelectionLines();
            VertexConsumer linesConsumer = consumers.getBuffer((net.minecraft.client.renderer.RenderType) linesLayer);
            LevelRenderer.renderLineBox(matrices, linesConsumer, target.getX(), target.getY(), target.getZ(), target.getX() + getSchematicSizeX(), target.getY() + getSchematicSizeY(), target.getZ() + getSchematicSizeZ(), 0.0F, 0.8F, 1.0F, 1.0F);

            matrices.popPose();
        }
    }
}
