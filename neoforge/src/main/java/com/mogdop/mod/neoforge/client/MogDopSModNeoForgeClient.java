package com.mogdop.mod.neoforge.client;

import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.client.gui.ChatNotificationHud;
import com.mogdop.mod.client.gui.ImageSelectorScreen;
import com.mogdop.mod.client.gui.SchematicScreen;
import com.mogdop.mod.client.gui.SelectionAxeHud;
import com.mogdop.mod.network.SpawnEntityPayload;
import com.mogdop.mod.network.ToolActionPayload;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Locale;

public class MogDopSModNeoForgeClient {

    private static final SelectionAxeHud SELECTION_AXE_HUD = new SelectionAxeHud();
    private static final ChatNotificationHud CHAT_NOTIFICATION_HUD = new ChatNotificationHud();

    public static void init(IEventBus modEventBus) {
        // 1. Инициализация общих клиентских систем
        MogDopSModClient.initClient();

        // 2. Регистрация слушателей NeoForge
        NeoForge.EVENT_BUS.addListener(MogDopSModNeoForgeClient::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(MogDopSModNeoForgeClient::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(MogDopSModNeoForgeClient::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(MogDopSModNeoForgeClient::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(MogDopSModNeoForgeClient::onRenderGui);
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        SELECTION_AXE_HUD.render(event.getGuiGraphics(), event.getPartialTick());
        CHAT_NOTIFICATION_HUD.render(event.getGuiGraphics(), event.getPartialTick());
    }

    private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClient()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ItemStack held = client.player.getMainHandStack();
        if (!MogDopSModClient.isSelectionAxe(held)) return;

        BlockPos pos = event.getPos();

        switch (MogDopSModClient.currentToolMode) {
            case 0 -> {
                if (MogDopSModClient.currentSelectionMode == 0) {
                    MogDopSModClient.pos1 = pos;
                    MogDopSModClient.syncSelectionPoints();
                    client.player.sendMessage(Text.translatable("mogdops-mod.selection.pos1", pos.toShortString()), true);
                } else {
                    MogDopSModClient.selectionPoints.add(pos);
                    client.player.sendMessage(Text.translatable("mogdops-mod.selection.point_added", MogDopSModClient.selectionPoints.size(), pos.toShortString()), true);
                }
                event.setCanceled(true);
            }
            case 1 -> {
                NetworkManager.sendToServer(new ToolActionPayload("REMOVER", pos, 0F, false, MogDopSModClient.CONFIG.toolRemoverRadius()));
                event.setCanceled(true);
            }
            case 2 -> {
                NetworkManager.sendToServer(new ToolActionPayload("EXPLOSION", pos, MogDopSModClient.CONFIG.toolExplosionPower(), MogDopSModClient.CONFIG.toolExplosionFire(), 1));
                event.setCanceled(true);
            }
            case 3 -> {
                NetworkManager.sendToServer(new ToolActionPayload("TELEPORT", pos, 0F, false, 1));
                event.setCanceled(true);
            }
            case 4 -> {
                NetworkManager.sendToServer(new SpawnEntityPayload(MogDopSModClient.activeSpawnId, MogDopSModClient.activeSpawnCustomName, MogDopSModClient.activeSpawnNameVisible, MogDopSModClient.activeSpawnNoGravity, MogDopSModClient.activeSpawnSilent, MogDopSModClient.activeSpawnGlowing, MogDopSModClient.activeSpawnIsBaby, MogDopSModClient.activeSpawnSlimeSize, MogDopSModClient.activeSpawnFireTicks));
                event.setCanceled(true);
            }
            case 6 -> {
                if (client.crosshairTarget instanceof BlockHitResult hitResult) {
                    MogDopSModClient.imagePos1 = MogDopSModClient.getSnappedPixelPoint(hitResult);
                    MogDopSModClient.imageSide = hitResult.getSide();
                    MogDopSModClient.imagePos2 = null;
                    client.player.sendMessage(Text.literal(String.format(Locale.ROOT, "§a[Изображение] Точка 1: (%.2f, %.2f, %.2f) на грани %s", MogDopSModClient.imagePos1.x, MogDopSModClient.imagePos1.y, MogDopSModClient.imagePos1.z, MogDopSModClient.imageSide.asString())), true);
                }
                event.setCanceled(true);
            }
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClient()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ItemStack held = client.player.getMainHandStack();
        if (!MogDopSModClient.isSelectionAxe(held)) return;

        BlockPos pos = event.getPos();

        switch (MogDopSModClient.currentToolMode) {
            case 0 -> {
                if (MogDopSModClient.currentSelectionMode == 0) {
                    MogDopSModClient.pos2 = pos;
                    MogDopSModClient.syncSelectionPoints();
                    client.player.sendMessage(Text.translatable("mogdops-mod.selection.pos2", pos.toShortString()), true);
                } else {
                    if (!MogDopSModClient.selectionPoints.isEmpty()) {
                        MogDopSModClient.selectionPoints.remove(MogDopSModClient.selectionPoints.size() - 1);
                        client.player.sendMessage(Text.translatable("mogdops-mod.selection.point_removed", MogDopSModClient.selectionPoints.size()), true);
                    }
                }
                event.setCanceled(true);
            }
            case 1 -> {
                NetworkManager.sendToServer(new ToolActionPayload("REMOVER", pos, 0F, false, MogDopSModClient.CONFIG.toolRemoverRadius()));
                event.setCanceled(true);
            }
            case 2 -> {
                NetworkManager.sendToServer(new ToolActionPayload("EXPLOSION", pos, MogDopSModClient.CONFIG.toolExplosionPower(), MogDopSModClient.CONFIG.toolExplosionFire(), 1));
                event.setCanceled(true);
            }
            case 3 -> {
                NetworkManager.sendToServer(new ToolActionPayload("TELEPORT", pos, 0F, false, 1));
                event.setCanceled(true);
            }
            case 4 -> {
                NetworkManager.sendToServer(new SpawnEntityPayload(MogDopSModClient.activeSpawnId, MogDopSModClient.activeSpawnCustomName, MogDopSModClient.activeSpawnNameVisible, MogDopSModClient.activeSpawnNoGravity, MogDopSModClient.activeSpawnSilent, MogDopSModClient.activeSpawnGlowing, MogDopSModClient.activeSpawnIsBaby, MogDopSModClient.activeSpawnSlimeSize, MogDopSModClient.activeSpawnFireTicks));
                event.setCanceled(true);
            }
            case 6 -> {
                if (MogDopSModClient.imagePos1 == null) {
                    client.player.sendMessage(Text.literal("§c[Изображение] Сначала установите первую точку (ЛКМ)!"), true);
                    event.setCanceled(true);
                    return;
                }
                BlockHitResult hitResult = event.getHitVec();
                MogDopSModClient.imagePos2 = MogDopSModClient.getSnappedPointOnPlane(hitResult, MogDopSModClient.imageSide, MogDopSModClient.imagePos1);
                client.player.sendMessage(Text.literal(String.format(Locale.ROOT, "§b[Изображение] Точка 2: (%.2f, %.2f, %.2f)", MogDopSModClient.imagePos2.x, MogDopSModClient.imagePos2.y, MogDopSModClient.imagePos2.z)), true);
                client.setScreen(new ImageSelectorScreen());
                event.setCanceled(true);
            }
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClient()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ItemStack held = client.player.getMainHandStack();
        if (!MogDopSModClient.isSelectionAxe(held)) return;

        HitResult hit = client.player.raycast(100.0D, 1.0F, false);

        switch (MogDopSModClient.currentToolMode) {
            case 1 -> {
                if (hit.getType() == HitResult.Type.BLOCK) {
                    NetworkManager.sendToServer(new ToolActionPayload("REMOVER", ((BlockHitResult) hit).getBlockPos(), 0F, false, MogDopSModClient.CONFIG.toolRemoverRadius()));
                }
            }
            case 2 -> {
                if (hit.getType() == HitResult.Type.BLOCK) {
                    NetworkManager.sendToServer(new ToolActionPayload("EXPLOSION", ((BlockHitResult) hit).getBlockPos(), MogDopSModClient.CONFIG.toolExplosionPower(), MogDopSModClient.CONFIG.toolExplosionFire(), 1));
                }
            }
            case 3 -> {
                if (hit.getType() == HitResult.Type.BLOCK) {
                    NetworkManager.sendToServer(new ToolActionPayload("TELEPORT", ((BlockHitResult) hit).getBlockPos(), 0F, false, 1));
                }
            }
            case 4 -> {
                NetworkManager.sendToServer(new SpawnEntityPayload(MogDopSModClient.activeSpawnId, MogDopSModClient.activeSpawnCustomName, MogDopSModClient.activeSpawnNameVisible, MogDopSModClient.activeSpawnNoGravity, MogDopSModClient.activeSpawnSilent, MogDopSModClient.activeSpawnGlowing, MogDopSModClient.activeSpawnIsBaby, MogDopSModClient.activeSpawnSlimeSize, MogDopSModClient.activeSpawnFireTicks));
            }
            case 5 -> {
                client.setScreen(new SchematicScreen());
            }
        }
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (!MogDopSModClient.isSelectionAxe(client.player.getMainHandStack())) return;

        Camera camera = event.getCamera();
        Vec3d camPos = camera.getPos();
        MatrixStack matrices = event.getPoseStack();
        VertexConsumerProvider consumers = client.getBufferBuilders().getEntityVertexConsumers();
        if (consumers == null) return;

        float[] col = MogDopSModClient.getSelectionColor();

        // 1. Режим изображений
        if (MogDopSModClient.currentToolMode == 6 && MogDopSModClient.imagePos1 != null) {
            Vec3d p2 = MogDopSModClient.imagePos2;
            if (p2 == null && client.crosshairTarget instanceof BlockHitResult hitResult) {
                p2 = MogDopSModClient.getSnappedPointOnPlane(hitResult, MogDopSModClient.imageSide, MogDopSModClient.imagePos1);
            }

            if (p2 != null) {
                matrices.push();
                matrices.translate(-camPos.x, -camPos.y, -camPos.z);

                double minX = Math.min(MogDopSModClient.imagePos1.x, p2.x); double maxX = Math.max(MogDopSModClient.imagePos1.x, p2.x);
                double minY = Math.min(MogDopSModClient.imagePos1.y, p2.y); double maxY = Math.max(MogDopSModClient.imagePos1.y, p2.y);
                double minZ = Math.min(MogDopSModClient.imagePos1.z, p2.z); double maxZ = Math.max(MogDopSModClient.imagePos1.z, p2.z);

                double off = 0.004;
                double ox = MogDopSModClient.imageSide.getOffsetX() * off;
                double oy = MogDopSModClient.imageSide.getOffsetY() * off;
                double oz = MogDopSModClient.imageSide.getOffsetZ() * off;

                Vec3d c0 = Vec3d.ZERO, c1 = Vec3d.ZERO, c2 = Vec3d.ZERO, c3 = Vec3d.ZERO;
                switch (MogDopSModClient.imageSide) {
                    case UP, DOWN -> {
                        double y = MogDopSModClient.imagePos1.y + oy;
                        c0 = new Vec3d(minX, y, minZ); c1 = new Vec3d(maxX, y, minZ); c2 = new Vec3d(maxX, y, maxZ); c3 = new Vec3d(minX, y, maxZ);
                    }
                    case NORTH, SOUTH -> {
                        double z = MogDopSModClient.imagePos1.z + oz;
                        c0 = new Vec3d(minX, minY, z); c1 = new Vec3d(maxX, minY, z); c2 = new Vec3d(maxX, maxY, z); c3 = new Vec3d(minX, maxY, z);
                    }
                    case WEST, EAST -> {
                        double x = MogDopSModClient.imagePos1.x + ox;
                        c0 = new Vec3d(x, minY, minZ); c1 = new Vec3d(x, minY, maxZ); c2 = new Vec3d(x, maxY, maxZ); c3 = new Vec3d(x, maxY, minZ);
                    }
                }

                VertexConsumer quads = consumers.getBuffer(MogDopSModClient.SELECTION_QUADS);
                MatrixStack.Entry entry = matrices.peek();
                quads.vertex(entry, (float)c0.x, (float)c0.y, (float)c0.z).color(0.0F, 0.8F, 1.0F, 0.35F);
                quads.vertex(entry, (float)c1.x, (float)c1.y, (float)c1.z).color(0.0F, 0.8F, 1.0F, 0.35F);
                quads.vertex(entry, (float)c2.x, (float)c2.y, (float)c2.z).color(0.0F, 0.8F, 1.0F, 0.35F);
                quads.vertex(entry, (float)c3.x, (float)c3.y, (float)c3.z).color(0.0F, 0.8F, 1.0F, 0.35F);

                VertexConsumer lines = consumers.getBuffer(MogDopSModClient.SELECTION_LINES);
                lines.vertex(entry, (float)c0.x, (float)c0.y, (float)c0.z).color(0.0F, 0.8F, 1.0F, 1.0F).normal(0, 1, 0);
                lines.vertex(entry, (float)c1.x, (float)c1.y, (float)c1.z).color(0.0F, 0.8F, 1.0F, 1.0F).normal(0, 1, 0);
                lines.vertex(entry, (float)c1.x, (float)c1.y, (float)c1.z).color(0.0F, 0.8F, 1.0F, 1.0F).normal(0, 1, 0);
                lines.vertex(entry, (float)c2.x, (float)c2.y, (float)c2.z).color(0.0F, 0.8F, 1.0F, 1.0F).normal(0, 1, 0);
                lines.vertex(entry, (float)c2.x, (float)c2.y, (float)c2.z).color(0.0F, 0.8F, 1.0F, 1.0F).normal(0, 1, 0);
                lines.vertex(entry, (float)c3.x, (float)c3.y, (float)c3.z).color(0.0F, 0.8F, 1.0F, 1.0F).normal(0, 1, 0);
                lines.vertex(entry, (float)c3.x, (float)c3.y, (float)c3.z).color(0.0F, 0.8F, 1.0F, 1.0F).normal(0, 1, 0);
                lines.vertex(entry, (float)c0.x, (float)c0.y, (float)c0.z).color(0.0F, 0.8F, 1.0F, 1.0F).normal(0, 1, 0);

                matrices.pop();
            }
        }

        // 2. Режим кубоида WorldEdit
        if (MogDopSModClient.currentToolMode == 0 && MogDopSModClient.currentSelectionMode == 0 && MogDopSModClient.pos1 != null && MogDopSModClient.pos2 != null) {
            double minX = Math.min(MogDopSModClient.pos1.getX(), MogDopSModClient.pos2.getX());
            double minY = Math.min(MogDopSModClient.pos1.getY(), MogDopSModClient.pos2.getY());
            double minZ = Math.min(MogDopSModClient.pos1.getZ(), MogDopSModClient.pos2.getZ());
            double maxX = Math.max(MogDopSModClient.pos1.getX(), MogDopSModClient.pos2.getX()) + 1.0;
            double maxY = Math.max(MogDopSModClient.pos1.getY(), MogDopSModClient.pos2.getY()) + 1.0;
            double maxZ = Math.max(MogDopSModClient.pos1.getZ(), MogDopSModClient.pos2.getZ()) + 1.0;

            matrices.push();
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);

            VertexConsumer linesConsumer = consumers.getBuffer(MogDopSModClient.SELECTION_LINES);
            WorldRenderer.drawBox(matrices, linesConsumer, minX, minY, minZ, maxX, maxY, maxZ, col[0], col[1], col[2], 1.0F);

            VertexConsumer quadsConsumer = consumers.getBuffer(MogDopSModClient.SELECTION_QUADS);
            Direction hitFace = MogDopSModClient.getTargetedSelectionFace();

            for (Direction dir : Direction.values()) {
                float faceAlpha = (dir == hitFace) ? 0.35F : 0.12F;
                MogDopSModClient.drawFaceQuad(matrices, quadsConsumer, dir, minX, minY, minZ, maxX, maxY, maxZ, col[0], col[1], col[2], faceAlpha);
            }

            matrices.pop();
        }

        // 3. Режим Полигона 2D
        if (MogDopSModClient.currentToolMode == 0 && MogDopSModClient.currentSelectionMode == 1 && !MogDopSModClient.selectionPoints.isEmpty()) {
            matrices.push();
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);

            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (BlockPos p : MogDopSModClient.selectionPoints) {
                if (p.getY() < minY) minY = p.getY();
                if (p.getY() > maxY) maxY = p.getY();
            }
            double y1 = minY;
            double y2 = maxY + 1.0;
            int n = MogDopSModClient.selectionPoints.size();

            VertexConsumer quadsConsumer = consumers.getBuffer(MogDopSModClient.SELECTION_QUADS);
            for (int i = 0; i < n; i++) {
                if (i > 0 || n >= 3) {
                    BlockPos pA = MogDopSModClient.selectionPoints.get(i);
                    BlockPos pB = MogDopSModClient.selectionPoints.get((i + 1) % n);
                    if (i == n - 1 && n < 3) break;

                    double xA = pA.getX() + 0.5, zA = pA.getZ() + 0.5;
                    double xB = pB.getX() + 0.5, zB = pB.getZ() + 0.5;

                    MatrixStack.Entry entry = matrices.peek();
                    quadsConsumer.vertex(entry, (float)xA, (float)y1, (float)zA).color(col[0], col[1], col[2], 0.20F);
                    quadsConsumer.vertex(entry, (float)xB, (float)y1, (float)zB).color(col[0], col[1], col[2], 0.20F);
                    quadsConsumer.vertex(entry, (float)xB, (float)y2, (float)zB).color(col[0], col[1], col[2], 0.20F);
                    quadsConsumer.vertex(entry, (float)xA, (float)y2, (float)zA).color(col[0], col[1], col[2], 0.20F);

                    quadsConsumer.vertex(entry, (float)xA, (float)y2, (float)zA).color(col[0], col[1], col[2], 0.20F);
                    quadsConsumer.vertex(entry, (float)xB, (float)y2, (float)zB).color(col[0], col[1], col[2], 0.20F);
                    quadsConsumer.vertex(entry, (float)xB, (float)y1, (float)zB).color(col[0], col[1], col[2], 0.20F);
                    quadsConsumer.vertex(entry, (float)xA, (float)y1, (float)zA).color(col[0], col[1], col[2], 0.20F);
                }
            }

            VertexConsumer linesConsumer = consumers.getBuffer(MogDopSModClient.SELECTION_LINES);
            for (int i = 0; i < n; i++) {
                BlockPos p = MogDopSModClient.selectionPoints.get(i);
                WorldRenderer.drawBox(matrices, linesConsumer, p.getX(), p.getY(), p.getZ(), p.getX() + 1.0, p.getY() + 1.0, p.getZ() + 1.0, 1.0F, 0.8F, 0.0F, 1.0F);

                double xA = p.getX() + 0.5, zA = p.getZ() + 0.5;
                linesConsumer.vertex(matrices.peek(), (float)xA, (float)y1, (float)zA).color(col[0], col[1], col[2], 1.0F).normal(0, 1, 0);
                linesConsumer.vertex(matrices.peek(), (float)xA, (float)y2, (float)zA).color(col[0], col[1], col[2], 1.0F).normal(0, 1, 0);

                if (i < n - 1 || n >= 3) {
                    BlockPos nextP = MogDopSModClient.selectionPoints.get((i + 1) % n);
                    double xB = nextP.getX() + 0.5, zB = nextP.getZ() + 0.5;
                    linesConsumer.vertex(matrices.peek(), (float)xA, (float)y1, (float)zA).color(col[0], col[1], col[2], 1.0F).normal(0, 1, 0);
                    linesConsumer.vertex(matrices.peek(), (float)xB, (float)y1, (float)zB).color(col[0], col[1], col[2], 1.0F).normal(0, 1, 0);
                    linesConsumer.vertex(matrices.peek(), (float)xA, (float)y2, (float)zA).color(col[0], col[1], col[2], 1.0F).normal(0, 1, 0);
                    linesConsumer.vertex(matrices.peek(), (float)xB, (float)y2, (float)zB).color(col[0], col[1], col[2], 1.0F).normal(0, 1, 0);
                }
            }

            matrices.pop();
        }

        // 4. Режим Выпуклого тела
        if (MogDopSModClient.currentToolMode == 0 && MogDopSModClient.currentSelectionMode == 2 && !MogDopSModClient.selectionPoints.isEmpty()) {
            matrices.push();
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);

            int n = MogDopSModClient.selectionPoints.size();
            VertexConsumer linesConsumer = consumers.getBuffer(MogDopSModClient.SELECTION_LINES);
            for (int i = 0; i < n; i++) {
                BlockPos p = MogDopSModClient.selectionPoints.get(i);
                WorldRenderer.drawBox(matrices, linesConsumer, p.getX(), p.getY(), p.getZ(), p.getX() + 1.0, p.getY() + 1.0, p.getZ() + 1.0, 1.0F, 0.3F, 0.8F, 1.0F);

                if (i > 0) {
                    BlockPos prev = MogDopSModClient.selectionPoints.get(i - 1);
                    linesConsumer.vertex(matrices.peek(), (float)(prev.getX() + 0.5), (float)(prev.getY() + 0.5), (float)(prev.getZ() + 0.5)).color(0F, 1F, 1F, 1F).normal(0, 1, 0);
                    linesConsumer.vertex(matrices.peek(), (float)(p.getX() + 0.5), (float)(p.getY() + 0.5), (float)(p.getZ() + 0.5)).color(0F, 1F, 1F, 1F).normal(0, 1, 0);
                }
                if (i == n - 1 && n >= 3) {
                    BlockPos first = MogDopSModClient.selectionPoints.get(0);
                    linesConsumer.vertex(matrices.peek(), (float)(p.getX() + 0.5), (float)(p.getY() + 0.5), (float)(p.getZ() + 0.5)).color(0F, 1F, 1F, 1F).normal(0, 1, 0);
                    linesConsumer.vertex(matrices.peek(), (float)(first.getX() + 0.5), (float)(first.getY() + 0.5), (float)(first.getZ() + 0.5)).color(0F, 1F, 1F, 1F).normal(0, 1, 0);
                }
            }

            matrices.pop();
        }

        // 5. Режим предпросмотра схематики
        if (MogDopSModClient.schematicPreviewActive && client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockPos target = ((BlockHitResult) client.crosshairTarget).getBlockPos().offset(((BlockHitResult) client.crosshairTarget).getSide());
            matrices.push();
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);

            VertexConsumer linesConsumer = consumers.getBuffer(MogDopSModClient.SELECTION_LINES);
            WorldRenderer.drawBox(matrices, linesConsumer, target.getX(), target.getY(), target.getZ(), target.getX() + MogDopSModClient.schematicSizeX, target.getY() + MogDopSModClient.schematicSizeY, target.getZ() + MogDopSModClient.schematicSizeZ, 0.0F, 0.8F, 1.0F, 1.0F);

            matrices.pop();
        }
    }
}