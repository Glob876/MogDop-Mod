package com.mogdop.mod.fabric.client;

import com.mogdop.mod.MogDopSMod;
import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.client.gui.*;
import com.mogdop.mod.network.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

public class MogDopSModFabricClient implements ClientModInitializer {

    private static final SelectionAxeHud SELECTION_AXE_HUD = new SelectionAxeHud();
    private static final ChatNotificationHud CHAT_NOTIFICATION_HUD = new ChatNotificationHud();

    @Override
    public void onInitializeClient() {
        // 1. Инициализация общих клиентских систем
        MogDopSModClient.initClient();

        // 2. Слой прозрачности для полублока-спавнера
        BlockRenderLayerMap.INSTANCE.putBlock(MogDopSMod.MOB_SPAWNER_SLAB.get(), RenderLayer.getCutout());

        // 3. HUD интерфейсы через единый вызов
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            SELECTION_AXE_HUD.render(drawContext, tickCounter);
            CHAT_NOTIFICATION_HUD.render(drawContext, tickCounter);
        });

        // 4. Сетевые S2C пакеты
        ClientPlayNetworking.registerGlobalReceiver(OpenMobSpawnerSlabScreenPayload.ID, (payload, ctx) -> ctx.client().execute(() -> {
            ctx.client().setScreen(new MobSpawnerSlabScreen(
                    payload.pos(),
                    payload.mobId(),
                    payload.spawnInterval(),
                    payload.maxMobs(),
                    payload.active(),
                    payload.spawnRange()
            ));
        }));

        ClientPlayNetworking.registerGlobalReceiver(SyncSchematicsListPayload.ID, (payload, ctx) -> ctx.client().execute(() -> {
            SchematicScreen.cachedSchematicsList.clear();
            SchematicScreen.cachedSchematicsList.addAll(payload.files());
            if (ctx.client().currentScreen instanceof SchematicScreen screen) {
                screen.rebuildFilesUI();
            }
        }));

        ClientPlayNetworking.registerGlobalReceiver(SchematicPreviewPayload.ID, (payload, ctx) -> ctx.client().execute(() -> {
            MogDopSModClient.schematicSizeX = payload.sizeX();
            MogDopSModClient.schematicSizeY = payload.sizeY();
            MogDopSModClient.schematicSizeZ = payload.sizeZ();
            MogDopSModClient.schematicName = payload.filename();
            MogDopSModClient.schematicPreviewActive = true;
        }));

        // 5. Обработка кликов ЛКМ
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient() || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!MogDopSModClient.isSelectionAxe(player.getMainHandStack())) return ActionResult.PASS;

            switch (MogDopSModClient.currentToolMode) {
                case 0 -> {
                    if (MogDopSModClient.currentSelectionMode == 0) {
                        MogDopSModClient.pos1 = pos;
                        MogDopSModClient.syncSelectionPoints();
                        player.sendMessage(Text.translatable("mogdops-mod.selection.pos1", pos.toShortString()), true);
                    } else {
                        MogDopSModClient.selectionPoints.add(pos);
                        player.sendMessage(Text.translatable("mogdops-mod.selection.point_added", MogDopSModClient.selectionPoints.size(), pos.toShortString()), true);
                    }
                    return ActionResult.FAIL;
                }
                case 1 -> {
                    ClientPlayNetworking.send(new ToolActionPayload("REMOVER", pos, 0F, false, MogDopSModClient.CONFIG.toolRemoverRadius()));
                    return ActionResult.FAIL;
                }
                case 2 -> {
                    ClientPlayNetworking.send(new ToolActionPayload("EXPLOSION", pos, MogDopSModClient.CONFIG.toolExplosionPower(), MogDopSModClient.CONFIG.toolExplosionFire(), 1));
                    return ActionResult.FAIL;
                }
                case 3 -> {
                    ClientPlayNetworking.send(new ToolActionPayload("TELEPORT", pos, 0F, false, 1));
                    return ActionResult.FAIL;
                }
                case 4 -> {
                    ClientPlayNetworking.send(new SpawnEntityPayload(MogDopSModClient.activeSpawnId, MogDopSModClient.activeSpawnCustomName, MogDopSModClient.activeSpawnNameVisible, MogDopSModClient.activeSpawnNoGravity, MogDopSModClient.activeSpawnSilent, MogDopSModClient.activeSpawnGlowing, MogDopSModClient.activeSpawnIsBaby, MogDopSModClient.activeSpawnSlimeSize, MogDopSModClient.activeSpawnFireTicks));
                    return ActionResult.FAIL;
                }
                case 6 -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.crosshairTarget instanceof BlockHitResult hitResult) {
                        MogDopSModClient.imagePos1 = MogDopSModClient.getSnappedPixelPoint(hitResult);
                        MogDopSModClient.imageSide = hitResult.getSide();
                        MogDopSModClient.imagePos2 = null;
                        player.sendMessage(Text.literal(String.format(Locale.ROOT, "§a[Изображение] Точка 1: (%.2f, %.2f, %.2f) на грани %s", MogDopSModClient.imagePos1.x, MogDopSModClient.imagePos1.y, MogDopSModClient.imagePos1.z, MogDopSModClient.imageSide.asString())), true);
                    }
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        // 6. Обработка кликов ПКМ
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient() || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!MogDopSModClient.isSelectionAxe(player.getMainHandStack())) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();

            switch (MogDopSModClient.currentToolMode) {
                case 0 -> {
                    if (MogDopSModClient.currentSelectionMode == 0) {
                        MogDopSModClient.pos2 = pos;
                        MogDopSModClient.syncSelectionPoints();
                        player.sendMessage(Text.translatable("mogdops-mod.selection.pos2", pos.toShortString()), true);
                    } else {
                        if (!MogDopSModClient.selectionPoints.isEmpty()) {
                            MogDopSModClient.selectionPoints.remove(MogDopSModClient.selectionPoints.size() - 1);
                            player.sendMessage(Text.translatable("mogdops-mod.selection.point_removed", MogDopSModClient.selectionPoints.size()), true);
                        }
                    }
                    return ActionResult.SUCCESS;
                }
                case 1 -> {
                    ClientPlayNetworking.send(new ToolActionPayload("REMOVER", pos, 0F, false, MogDopSModClient.CONFIG.toolRemoverRadius()));
                    return ActionResult.SUCCESS;
                }
                case 2 -> {
                    ClientPlayNetworking.send(new ToolActionPayload("EXPLOSION", pos, MogDopSModClient.CONFIG.toolExplosionPower(), MogDopSModClient.CONFIG.toolExplosionFire(), 1));
                    return ActionResult.SUCCESS;
                }
                case 3 -> {
                    ClientPlayNetworking.send(new ToolActionPayload("TELEPORT", pos, 0F, false, 1));
                    return ActionResult.SUCCESS;
                }
                case 4 -> {
                    ClientPlayNetworking.send(new SpawnEntityPayload(MogDopSModClient.activeSpawnId, MogDopSModClient.activeSpawnCustomName, MogDopSModClient.activeSpawnNameVisible, MogDopSModClient.activeSpawnNoGravity, MogDopSModClient.activeSpawnSilent, MogDopSModClient.activeSpawnGlowing, MogDopSModClient.activeSpawnIsBaby, MogDopSModClient.activeSpawnSlimeSize, MogDopSModClient.activeSpawnFireTicks));
                    return ActionResult.SUCCESS;
                }
                case 6 -> {
                    if (MogDopSModClient.imagePos1 == null) {
                        player.sendMessage(Text.literal("§c[Изображение] Сначала установите первую точку (ЛКМ)!"), true);
                        return ActionResult.SUCCESS;
                    }
                    MogDopSModClient.imagePos2 = MogDopSModClient.getSnappedPointOnPlane(hitResult, MogDopSModClient.imageSide, MogDopSModClient.imagePos1);
                    player.sendMessage(Text.literal(String.format(Locale.ROOT, "§b[Изображение] Точка 2: (%.2f, %.2f, %.2f)", MogDopSModClient.imagePos2.x, MogDopSModClient.imagePos2.y, MogDopSModClient.imagePos2.z)), true);
                    MinecraftClient.getInstance().setScreen(new ImageSelectorScreen());
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });

        // 7. Обработка клика в воздух на дистанции
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient() || hand != Hand.MAIN_HAND) return TypedActionResult.pass(player.getStackInHand(hand));
            if (!MogDopSModClient.isSelectionAxe(player.getMainHandStack())) return TypedActionResult.pass(player.getStackInHand(hand));

            HitResult hit = player.raycast(100.0D, 1.0F, false);

            switch (MogDopSModClient.currentToolMode) {
                case 1 -> {
                    if (hit.getType() == HitResult.Type.BLOCK) {
                        ClientPlayNetworking.send(new ToolActionPayload("REMOVER", ((BlockHitResult) hit).getBlockPos(), 0F, false, MogDopSModClient.CONFIG.toolRemoverRadius()));
                        return TypedActionResult.success(player.getStackInHand(hand));
                    }
                }
                case 2 -> {
                    if (hit.getType() == HitResult.Type.BLOCK) {
                        ClientPlayNetworking.send(new ToolActionPayload("EXPLOSION", ((BlockHitResult) hit).getBlockPos(), MogDopSModClient.CONFIG.toolExplosionPower(), MogDopSModClient.CONFIG.toolExplosionFire(), 1));
                        return TypedActionResult.success(player.getStackInHand(hand));
                    }
                }
                case 3 -> {
                    if (hit.getType() == HitResult.Type.BLOCK) {
                        ClientPlayNetworking.send(new ToolActionPayload("TELEPORT", ((BlockHitResult) hit).getBlockPos(), 0F, false, 1));
                        return TypedActionResult.success(player.getStackInHand(hand));
                    }
                }
                case 4 -> {
                    ClientPlayNetworking.send(new SpawnEntityPayload(MogDopSModClient.activeSpawnId, MogDopSModClient.activeSpawnCustomName, MogDopSModClient.activeSpawnNameVisible, MogDopSModClient.activeSpawnNoGravity, MogDopSModClient.activeSpawnSilent, MogDopSModClient.activeSpawnGlowing, MogDopSModClient.activeSpawnIsBaby, MogDopSModClient.activeSpawnSlimeSize, MogDopSModClient.activeSpawnFireTicks));
                    return TypedActionResult.success(player.getStackInHand(hand));
                }
                case 5 -> {
                    MinecraftClient.getInstance().setScreen(new SchematicScreen());
                    return TypedActionResult.success(player.getStackInHand(hand));
                }
            }

            return TypedActionResult.pass(player.getStackInHand(hand));
        });

        // 8. Приветственный экран
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!MogDopSModClient.CONFIG.hasSeenWelcome()) {
                client.execute(() -> client.setScreen(new WelcomeScreen()));
                MogDopSModClient.CONFIG.hasSeenWelcome(true);
                MogDopSModClient.CONFIG.save();
            }
        });

        // 9. 3D Рендеринг выделения в мире
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            if (!MogDopSModClient.isSelectionAxe(client.player.getMainHandStack())) return;

            Camera camera = context.camera();
            Vec3d camPos = camera.getPos();
            MatrixStack matrices = context.matrixStack();
            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) return;

            float[] col = MogDopSModClient.getSelectionColor();

            // 9.1 Режим изображений
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

            // 9.2 Режим кубоида WorldEdit
            if (MogDopSModClient.currentToolMode == 0 && MogDopSModClient.currentSelectionMode == 0 && MogDopSModClient.pos1 != null && MogDopSModClient.pos2 != null) {
                double minX = Math.min(MogDopSModClient.pos1.getX(), MogDopSModClient.pos2.getX());
                double minY = Math.min(MogDopSModClient.pos1.getY(), MogDopSModClient.pos2.getY());
                double minZ = Math.min(MogDopSModClient.pos1.getZ(), MogDopSModClient.pos2.getZ());
                double maxX = Math.max(MogDopSModClient.pos1.getX(), MogDopSModClient.pos2.getX()) + 1.0;
                double maxY = Math.max(MogDopSModClient.pos1.getY(), MogDopSModClient.pos2.getY()) + 1.0;
                double maxZ = Math.max(MogDopSModClient.pos1.getZ(), MogDopSModClient.pos2.getZ()) + 1.0;

                if (!MogDopSModClient.selectionAnimInitialized) {
                    MogDopSModClient.animMinX = minX; MogDopSModClient.animMinY = minY; MogDopSModClient.animMinZ = minZ;
                    MogDopSModClient.animMaxX = maxX; MogDopSModClient.animMaxY = maxY; MogDopSModClient.animMaxZ = maxZ;
                    MogDopSModClient.selectionAnimInitialized = true;
                }

                if (MogDopSModClient.CONFIG.enableSelectionAnimation()) {
                    double speed = 0.25;
                    MogDopSModClient.animMinX += (minX - MogDopSModClient.animMinX) * speed;
                    MogDopSModClient.animMinY += (minY - MogDopSModClient.animMinY) * speed;
                    MogDopSModClient.animMinZ += (minZ - MogDopSModClient.animMinZ) * speed;
                    MogDopSModClient.animMaxX += (maxX - MogDopSModClient.animMaxX) * speed;
                    MogDopSModClient.animMaxY += (maxY - MogDopSModClient.animMaxY) * speed;
                    MogDopSModClient.animMaxZ += (maxZ - MogDopSModClient.animMaxZ) * speed;
                } else {
                    MogDopSModClient.animMinX = minX; MogDopSModClient.animMinY = minY; MogDopSModClient.animMinZ = minZ;
                    MogDopSModClient.animMaxX = maxX; MogDopSModClient.animMaxY = maxY; MogDopSModClient.animMaxZ = maxZ;
                }

                matrices.push();
                matrices.translate(-camPos.x, -camPos.y, -camPos.z);

                VertexConsumer linesConsumer = consumers.getBuffer(MogDopSModClient.SELECTION_LINES);
                WorldRenderer.drawBox(matrices, linesConsumer, MogDopSModClient.animMinX, MogDopSModClient.animMinY, MogDopSModClient.animMinZ, MogDopSModClient.animMaxX, MogDopSModClient.animMaxY, MogDopSModClient.animMaxZ, col[0], col[1], col[2], 1.0F);

                VertexConsumer quadsConsumer = consumers.getBuffer(MogDopSModClient.SELECTION_QUADS);
                Direction hitFace = MogDopSModClient.getTargetedSelectionFace();

                for (Direction dir : Direction.values()) {
                    float faceAlpha = (dir == hitFace) ? 0.35F : 0.12F;
                    MogDopSModClient.drawFaceQuad(matrices, quadsConsumer, dir, MogDopSModClient.animMinX, MogDopSModClient.animMinY, MogDopSModClient.animMinZ, MogDopSModClient.animMaxX, MogDopSModClient.animMaxY, MogDopSModClient.animMaxZ, col[0], col[1], col[2], faceAlpha);
                }

                matrices.pop();
            }

            // 9.3 Режим Полигона 2D
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

            // 9.4 Режим Выпуклого тела
            if (MogDopSModClient.currentToolMode == 0 && MogDopSModClient.currentSelectionMode == 2 && !MogDopSModClient.selectionPoints.isEmpty()) {
                matrices.push();
                matrices.translate(-camPos.x, -camPos.y, -camPos.z);

                int n = MogDopSModClient.selectionPoints.size();
                VertexConsumer linesConsumer = consumers.getBuffer(MogDopSModClient.SELECTION_LINES);
                for (int i = 0; i < n; i++) {
                    BlockPos p = MogDopSModClient.selectionPoints.get(i);
                    WorldRenderer.drawBox(matrices, linesConsumer, p.getX(), p.getY(), p.getZ(), p.getX() + 1.0, p.getY() + 1.0, p.getZ() + 1.0, 1.0F, 0.3F, 0.8F, 1.0F);

                    if (i > 0) {
                        BlockPos prev = selectionPoints.get(i - 1);
                        linesConsumer.vertex(matrices.peek(), (float)(prev.getX() + 0.5), (float)(prev.getY() + 0.5), (float)(prev.getZ() + 0.5)).color(0F, 1F, 1F, 1F).normal(0, 1, 0);
                        linesConsumer.vertex(matrices.peek(), (float)(p.getX() + 0.5), (float)(p.getY() + 0.5), (float)(p.getZ() + 0.5)).color(0F, 1F, 1F, 1F).normal(0, 1, 0);
                    }
                    if (i == n - 1 && n >= 3) {
                        BlockPos first = selectionPoints.get(0);
                        linesConsumer.vertex(matrices.peek(), (float)(p.getX() + 0.5), (float)(p.getY() + 0.5), (float)(p.getZ() + 0.5)).color(0F, 1F, 1F, 1F).normal(0, 1, 0);
                        linesConsumer.vertex(matrices.peek(), (float)(first.getX() + 0.5), (float)(first.getY() + 0.5), (float)(first.getZ() + 0.5)).color(0F, 1F, 1F, 1F).normal(0, 1, 0);
                    }
                }

                matrices.pop();
            }

            // 9.5 3D Предпросмотр Схематики
            if (MogDopSModClient.schematicPreviewActive && client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                BlockPos target = ((BlockHitResult) client.crosshairTarget).getBlockPos().offset(((BlockHitResult) client.crosshairTarget).getSide());
                matrices.push();
                matrices.translate(-camPos.x, -camPos.y, -camPos.z);

                VertexConsumer linesConsumer = consumers.getBuffer(MogDopSModClient.SELECTION_LINES);
                WorldRenderer.drawBox(matrices, linesConsumer, target.getX(), target.getY(), target.getZ(), target.getX() + MogDopSModClient.schematicSizeX, target.getY() + MogDopSModClient.schematicSizeY, target.getZ() + MogDopSModClient.schematicSizeZ, 0.0F, 0.8F, 1.0F, 1.0F);

                matrices.pop();
            }
        });
    }
}