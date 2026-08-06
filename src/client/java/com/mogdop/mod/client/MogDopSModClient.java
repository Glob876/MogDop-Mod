package com.mogdop.mod.client;

import com.mogdop.mod.MogDopSMod;
import com.mogdop.mod.client.gui.SpawnerScreen;
import com.mogdop.mod.client.gui.ToolSelectorScreen;
import com.mogdop.mod.client.gui.BlockSelectorScreen;
import com.mogdop.mod.client.gui.MogDopConfigScreen;
import com.mogdop.mod.client.gui.MobSpawnerSlabScreen;
import com.mogdop.mod.client.gui.QuickFillReplaceScreen;
import com.mogdop.mod.client.gui.SelectionModeScreen;
import com.mogdop.mod.client.gui.SchematicScreen;
import com.mogdop.mod.client.gui.WelcomeScreen;
import com.mogdop.mod.client.gui.SelectionAxeHud;
import com.mogdop.mod.client.gui.ChatNotificationHud;
import com.mogdop.mod.network.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.config.ui.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public class MogDopSModClient implements ClientModInitializer {

    public static KeyBinding openSpawnerKey;
    public static KeyBinding quickFillKey;
    public static KeyBinding openToolSelectorKey;
    public static KeyBinding openBlockSelectorKey;
    public static KeyBinding openSchematicKey;

    public static final MogdopsModConfig CONFIG = MogdopsModConfig.createAndLoad();

    public static Block activeBlock = Blocks.STONE;
    public static BlockPos pos1 = null;
    public static BlockPos pos2 = null;

    public static final List<BlockPos> selectionPoints = new ArrayList<>();

    // Переменные режима 3D Предпросмотра схематики
    public static boolean schematicPreviewActive = false;
    public static int schematicSizeX = 1;
    public static int schematicSizeY = 1;
    public static int schematicSizeZ = 1;
    public static String schematicName = "";

    public static double schemAnimX = 0, schemAnimY = 0, schemAnimZ = 0;
    public static boolean schemAnimInit = false;
    public static BlockPos currentSchemTargetPos = null;

    public static void syncSelectionPoints() {
        if (currentSelectionMode == 0) {
            selectionPoints.clear();
            if (pos1 != null) selectionPoints.add(pos1);
            if (pos2 != null) selectionPoints.add(pos2);
        }
    }

    public static List<BlockPos> getSelectionPoints() {
        if (currentSelectionMode == 0) {
            List<BlockPos> list = new ArrayList<>();
            if (pos1 != null) list.add(pos1);
            if (pos2 != null) list.add(pos2);
            return list;
        }
        return new ArrayList<>(selectionPoints);
    }

    public static int currentSelectionMode = 0;
    public static final String[] SELECTION_MODE_KEYS = {
            "mogdops-mod.selection_mode.cuboid",
            "mogdops-mod.selection_mode.poly",
            "mogdops-mod.selection_mode.convex"
    };

    public static double animMinX = 0, animMinY = 0, animMinZ = 0;
    public static double animMaxX = 0, animMaxY = 0, animMaxZ = 0;
    public static boolean selectionAnimInitialized = false;

    public static final String[] TOOL_MODE_KEYS = {
            "mogdops-mod.tool_selector.modes.selection",
            "mogdops-mod.tool_selector.modes.remover",
            "mogdops-mod.tool_selector.modes.explosion",
            "mogdops-mod.tool_selector.modes.teleport",
            "mogdops-mod.tool_selector.modes.spawner",
            "mogdops-mod.tool_selector.modes.schematics"
    };
    public static int currentToolMode = 0;

    public static final RenderLayer SELECTION_LINES = RenderLayer.of(
            "selection_lines",
            VertexFormats.LINES,
            VertexFormat.DrawMode.LINES,
            1536,
            false,
            false,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.LINES_PROGRAM)
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(2.5D)))
                    .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .writeMaskState(RenderPhase.ALL_MASK)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .build(false)
    );

    public static final RenderLayer SELECTION_QUADS = RenderLayer.of(
            "selection_quads",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            1536,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.COLOR_PROGRAM)
                    .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .writeMaskState(RenderPhase.ALL_MASK)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .build(false)
    );

    private static final NotificationManager notificationManager = new NotificationManager();

    public static NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public static String activeSpawnId = "minecraft:cow";
    public static String activeSpawnCustomName = "";
    public static boolean activeSpawnNameVisible = false;
    public static boolean activeSpawnNoGravity = false;
    public static boolean activeSpawnSilent = false;
    public static boolean activeSpawnGlowing = false;
    public static boolean activeSpawnIsBaby = false;
    public static int activeSpawnSlimeSize = 0;
    public static int activeSpawnFireTicks = 0;

    public static boolean isSelectionAxe(ItemStack stack) {
        if (!stack.isOf(Items.IRON_AXE)) {
            return false;
        }
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) {
            return true;
        }
        String name = stack.getName().getString().toLowerCase();
        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        String customStr = customName != null ? customName.toString().toLowerCase() : "";
        return name.contains("топор") || name.contains("selection") || name.contains("axe") || customStr.contains("selection_axe");
    }

    public static void cycleToolMode(int dir) {
        currentToolMode = (currentToolMode + dir + TOOL_MODE_KEYS.length) % TOOL_MODE_KEYS.length;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.translatable("mogdops-mod.tool.mode_changed", Text.translatable(TOOL_MODE_KEYS[currentToolMode])), true);
        }
    }

    public static float[] getSelectionColor() {
        String hex = CONFIG.toolSelectionColor();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        try {
            int rgb = Integer.parseInt(hex, 16);
            float r = ((rgb >> 16) & 0xFF) / 255.0F;
            float g = ((rgb >> 8) & 0xFF) / 255.0F;
            float b = (rgb & 0xFF) / 255.0F;
            return new float[]{r, g, b, 1.0F};
        } catch (Exception e) {
            return new float[]{1.0F, 0.6F, 0.0F, 1.0F};
        }
    }

    public static Direction getTargetedSelectionFace() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || pos1 == null || pos2 == null) return null;

        double minX = Math.min(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());

        double maxX = Math.max(pos1.getX(), pos2.getX()) + 1.0;
        double maxY = Math.max(pos1.getY(), pos2.getY()) + 1.0;
        double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1.0;

        Box box = new Box(minX, minY, minZ, maxX, maxY, maxZ);

        Vec3d eyePos = client.player.getEyePos();
        Vec3d rotVec = client.player.getRotationVec(1.0F);
        Vec3d reachVec = eyePos.add(rotVec.multiply(50.0D));

        Optional<Vec3d> hitOpt = box.raycast(eyePos, reachVec);
        if (hitOpt.isEmpty()) return null;

        Vec3d hit = hitOpt.get();
        double eps = 1e-3;

        if (Math.abs(hit.x - minX) < eps) return Direction.WEST;
        if (Math.abs(hit.x - maxX) < eps) return Direction.EAST;
        if (Math.abs(hit.y - minY) < eps) return Direction.DOWN;
        if (Math.abs(hit.y - maxY) < eps) return Direction.UP;
        if (Math.abs(hit.z - minZ) < eps) return Direction.NORTH;
        if (Math.abs(hit.z - maxZ) < eps) return Direction.SOUTH;

        return null;
    }

    public static void expandSelectionFace(Direction face, int delta) {
        if (pos1 == null || pos2 == null || delta == 0) return;

        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        switch (face) {
            case EAST -> {
                if (pos1.getX() == maxX) pos1 = pos1.add(delta, 0, 0);
                else pos2 = pos2.add(delta, 0, 0);
            }
            case WEST -> {
                if (pos1.getX() == minX) pos1 = pos1.add(-delta, 0, 0);
                else pos2 = pos2.add(-delta, 0, 0);
            }
            case UP -> {
                if (pos1.getY() == maxY) pos1 = pos1.add(0, delta, 0);
                else pos2 = pos2.add(0, delta, 0);
            }
            case DOWN -> {
                if (pos1.getY() == minY) pos1 = pos1.add(0, -delta, 0);
                else pos2 = pos2.add(0, -delta, 0);
            }
            case SOUTH -> {
                if (pos1.getZ() == maxZ) pos1 = pos1.add(0, 0, delta);
                else pos2 = pos2.add(0, 0, delta);
            }
            case NORTH -> {
                if (pos1.getZ() == minZ) pos1 = pos1.add(0, 0, -delta);
                else pos2 = pos2.add(0, 0, -delta);
            }
        }

        syncSelectionPoints();
    }

    public static void drawFaceQuad(MatrixStack matrices, VertexConsumer consumer, Direction dir, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        MatrixStack.Entry entry = matrices.peek();
        switch (dir) {
            case DOWN -> {
                consumer.vertex(entry, (float)x1, (float)y1, (float)z1).color(r, g, b, a);
                consumer.vertex(entry, (float)x2, (float)y1, (float)z1).color(r, g, b, a);
                consumer.vertex(entry, (float)x2, (float)y1, (float)z2).color(r, g, b, a);
                consumer.vertex(entry, (float)x1, (float)y1, (float)z2).color(r, g, b, a);
            }
            case UP -> {
                consumer.vertex(entry, (float)x1, (float)y2, (float)z1).color(r, g, b, a);
                consumer.vertex(entry, (float)x1, (float)y2, (float)z2).color(r, g, b, a);
                consumer.vertex(entry, (float)x2, (float)y2, (float)z2).color(r, g, b, a);
                consumer.vertex(entry, (float)x2, (float)y2, (float)z1).color(r, g, b, a);
            }
            case NORTH -> {
                consumer.vertex(entry, (float)x1, (float)y1, (float)z1).color(r, g, b, a);
                consumer.vertex(entry, (float)x1, (float)y2, (float)z1).color(r, g, b, a);
                consumer.vertex(entry, (float)x2, (float)y2, (float)z1).color(r, g, b, a);
                consumer.vertex(entry, (float)x2, (float)y1, (float)z1).color(r, g, b, a);
            }
            case SOUTH -> {
                consumer.vertex(entry, (float)x1, (float)y1, (float)z2).color(r, g, b, a);
                consumer.vertex(entry, (float)x2, (float)y1, (float)z2).color(r, g, b, a);
                consumer.vertex(entry, (float)x2, (float)y2, (float)z2).color(r, g, b, a);
                consumer.vertex(entry, (float)x1, (float)y2, (float)z2).color(r, g, b, a);
            }
            case WEST -> {
                consumer.vertex(entry, (float)x1, (float)y1, (float)z1).color(r, g, b, a);
                consumer.vertex(entry, (float)x1, (float)y1, (float)z2).color(r, g, b, a);
                consumer.vertex(entry, (float)x1, (float)y2, (float)z2).color(r, g, b, a);
                consumer.vertex(entry, (float)x1, (float)y2, (float)z1).color(r, g, b, a);
            }
            case EAST -> {
                consumer.vertex(entry, (float)x2, (float)y1, (float)z1).color(r, g, b, a);
                consumer.vertex(entry, (float)x2, (float)y2, (float)z1).color(r, g, b, a);
                consumer.vertex(entry, (float)x2, (float)y2, (float)z2).color(r, g, b, a);
                consumer.vertex(entry, (float)x2, (float)y1, (float)z2).color(r, g, b, a);
            }
        }
    }

    public static void draw3DLine(MatrixStack.Entry entry, VertexConsumer consumer, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        float nx = (float)(x2 - x1);
        float ny = (float)(y2 - y1);
        float nz = (float)(z2 - z1);
        float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0) { nx /= len; ny /= len; nz /= len; } else { nx = 0; ny = 1; nz = 0; }
        consumer.vertex(entry, (float)x1, (float)y1, (float)z1).color(r, g, b, a).normal(entry, nx, ny, nz);
        consumer.vertex(entry, (float)x2, (float)y2, (float)z2).color(r, g, b, a).normal(entry, nx, ny, nz);
    }

    public static void spawnSelectionBurst(World world, BlockPos pos, boolean isPos1) {
        if (!CONFIG.enableSelectionParticles()) return;

        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;

        ParticleEffect particle = isPos1 ? ParticleTypes.END_ROD : ParticleTypes.SOUL_FIRE_FLAME;

        for (int i = 0; i < 20; i++) {
            double vx = (world.random.nextDouble() - 0.5D) * 0.15D;
            double vy = world.random.nextDouble() * 0.2D;
            double vz = (world.random.nextDouble() - 0.5D) * 0.15D;
            world.addParticle(particle, centerX, centerY, centerZ, vx, vy, vz);
        }
    }

    @Override
    public void onInitializeClient() {
        openSpawnerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mogdops-mod.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                "category.mogdops-mod.general"
        ));

        quickFillKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mogdops-mod.quick_fill",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.mogdops-mod.general"
        ));

        openToolSelectorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mogdops-mod.open_tool_selector",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "category.mogdops-mod.general"
        ));

        openBlockSelectorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mogdops-mod.open_block_selector",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "category.mogdops-mod.general"
        ));

        openSchematicKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mogdops-mod.open_schematics",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                "category.mogdops-mod.general"
        ));

        PlayerBlockHistoryManager.load();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!CONFIG.hasSeenWelcome()) {
                client.execute(() -> {
                    client.setScreen(new WelcomeScreen());
                    CONFIG.hasSeenWelcome(true);
                    CONFIG.save();
                });
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            getNotificationManager().update();

            while (openSpawnerKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new SpawnerScreen());
                }
            }

            while (openToolSelectorKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new ToolSelectorScreen());
                }
            }

            while (openBlockSelectorKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new SelectionModeScreen());
                }
            }

            while (openSchematicKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new SchematicScreen());
                }
            }

            while (quickFillKey.wasPressed()) {
                if (client.player != null && client.world != null && client.currentScreen == null) {
                    if (currentToolMode == 5) {
                        if (!getSelectionPoints().isEmpty()) {
                            ClientPlayNetworking.send(new CopyClipboardPayload(getSelectionPoints(), currentSelectionMode));
                        } else {
                            client.player.sendMessage(Text.translatable("mogdops-mod.error.positions_not_set"), false);
                        }
                    } else {
                        client.setScreen(new QuickFillReplaceScreen());
                    }
                }
            }
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient() && isSelectionAxe(player.getStackInHand(hand))) {
                if (schematicPreviewActive && currentSchemTargetPos != null) {
                    pos1 = currentSchemTargetPos;
                    pos2 = currentSchemTargetPos.add(schematicSizeX - 1, schematicSizeY - 1, schematicSizeZ - 1);
                    syncSelectionPoints();
                    ClientPlayNetworking.send(new PasteClipboardPayload(false));
                    schematicPreviewActive = false;
                    schemAnimInit = false;
                    player.sendMessage(Text.translatable("mogdops-mod.server.paste_success"), true);
                    return ActionResult.FAIL;
                }

                if (currentToolMode == 0 || currentToolMode == 5) {
                    BlockPos p = pos.toImmutable();

                    if (currentSelectionMode == 0) {
                        pos1 = p;
                        syncSelectionPoints();
                    } else {
                        selectionPoints.clear();
                        pos1 = p;
                        pos2 = null;
                        selectionPoints.add(pos1);
                    }

                    spawnSelectionBurst(world, pos1, true);
                    player.sendMessage(Text.translatable("mogdops-mod.selection.pos1_set", pos1.toShortString()), true);
                    return ActionResult.FAIL;
                } else {
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() && isSelectionAxe(player.getStackInHand(hand))) {
                if (schematicPreviewActive && currentSchemTargetPos != null) {
                    pos1 = currentSchemTargetPos;
                    pos2 = currentSchemTargetPos.add(schematicSizeX - 1, schematicSizeY - 1, schematicSizeZ - 1);
                    syncSelectionPoints();
                    ClientPlayNetworking.send(new PasteClipboardPayload(false));
                    schematicPreviewActive = false;
                    schemAnimInit = false;
                    player.sendMessage(Text.translatable("mogdops-mod.server.paste_success"), true);
                    return ActionResult.FAIL;
                }

                if (currentToolMode == 0 || currentToolMode == 5) {
                    BlockPos p = hitResult.getBlockPos().toImmutable();

                    if (currentSelectionMode == 0) {
                        pos2 = p;
                        syncSelectionPoints();
                        player.sendMessage(Text.translatable("mogdops-mod.selection.pos2_set", pos2.toShortString()), true);
                    } else {
                        if (!selectionPoints.contains(p)) {
                            selectionPoints.add(p);
                        }
                        pos2 = p;
                        player.sendMessage(Text.translatable("mogdops-mod.selection.point_added", selectionPoints.size(), p.toShortString()), true);
                    }

                    spawnSelectionBurst(world, p, false);
                    return ActionResult.FAIL;
                } else {
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient() && hand == Hand.MAIN_HAND && isSelectionAxe(player.getStackInHand(hand))) {
                if (schematicPreviewActive && currentSchemTargetPos != null) {
                    pos1 = currentSchemTargetPos;
                    pos2 = currentSchemTargetPos.add(schematicSizeX - 1, schematicSizeY - 1, schematicSizeZ - 1);
                    syncSelectionPoints();
                    ClientPlayNetworking.send(new PasteClipboardPayload(false));
                    schematicPreviewActive = false;
                    schemAnimInit = false;
                    player.sendMessage(Text.translatable("mogdops-mod.server.paste_success"), true);
                    return TypedActionResult.success(player.getStackInHand(hand));
                }

                if (currentToolMode != 0 && currentToolMode != 5) {
                    HitResult hit = player.raycast(100.0D, 1.0F, false);

                    if (currentToolMode == 4) {
                        ClientPlayNetworking.send(new SpawnEntityPayload(
                                activeSpawnId, activeSpawnCustomName, activeSpawnNameVisible,
                                activeSpawnNoGravity, activeSpawnSilent, activeSpawnGlowing,
                                activeSpawnIsBaby, activeSpawnSlimeSize, activeSpawnFireTicks
                        ));
                        return TypedActionResult.success(player.getStackInHand(hand));
                    } else if (hit.getType() == HitResult.Type.BLOCK) {
                        BlockPos hitPos = ((BlockHitResult) hit).getBlockPos();
                        String action = "";
                        if (currentToolMode == 1) action = "REMOVER";
                        else if (currentToolMode == 2) action = "EXPLOSION";
                        else if (currentToolMode == 3) action = "TELEPORT";

                        ClientPlayNetworking.send(new ToolActionPayload(
                                action, hitPos, CONFIG.toolExplosionPower(), CONFIG.toolExplosionFire(), CONFIG.toolRemoverRadius()
                        ));
                        return TypedActionResult.success(player.getStackInHand(hand));
                    }
                }
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("md")
                    .then(ClientCommandManager.literal("settings")
                            .executes(context -> {
                                MinecraftClient.getInstance().execute(() -> {
                                    MinecraftClient.getInstance().setScreen(new MogDopConfigScreen(null));
                                });
                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("guide")
                            .executes(context -> {
                                MinecraftClient.getInstance().execute(() -> {
                                    MinecraftClient.getInstance().setScreen(new WelcomeScreen());
                                });
                                return 1;
                            })
                    )
            );
        });

        // ОБРАБОТЧИК S2C: СИНХРОНИЗАЦИЯ СПИСКА СХЕМАТИК
        ClientPlayNetworking.registerGlobalReceiver(SyncSchematicsListPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                SchematicScreen.cachedSchematicsList.clear();
                SchematicScreen.cachedSchematicsList.addAll(payload.files());
                if (context.client().currentScreen instanceof SchematicScreen screen) {
                    screen.rebuildFilesUI();
                }
            });
        });

        // ОБРАБОТЧИК S2C: АКТИВАЦИЯ 3D ПРЕДПРОСМОТРА СХЕМАТИКИ
        ClientPlayNetworking.registerGlobalReceiver(SchematicPreviewPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                schematicPreviewActive = true;
                schematicSizeX = payload.sizeX();
                schematicSizeY = payload.sizeY();
                schematicSizeZ = payload.sizeZ();
                schematicName = payload.filename();
                currentToolMode = 5;
                schemAnimInit = false;
                if (context.client().player != null) {
                    context.client().player.sendMessage(Text.translatable("mogdops-mod.schematic.loaded_preview", payload.filename(), payload.sizeX(), payload.sizeY(), payload.sizeZ()), true);
                }
            });
        });

        HudRenderCallback.EVENT.register(new ChatNotificationHud());
        HudRenderCallback.EVENT.register(new SelectionAxeHud());

        WorldRenderEvents.LAST.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;

            // 1. РЕНДЕР 3D ПРЕДПРОСМОТРА СХЕМАТИКИ
            if (schematicPreviewActive && isSelectionAxe(client.player.getMainHandStack())) {
                HitResult hit = client.crosshairTarget;
                if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) hit;
                    BlockPos hitPos = blockHit.getBlockPos();
                    Direction side = blockHit.getSide();
                    BlockPos targetPos = hitPos.offset(side);
                    currentSchemTargetPos = targetPos;

                    double targetMinX = targetPos.getX();
                    double targetMinY = targetPos.getY();
                    double targetMinZ = targetPos.getZ();

                    if (!CONFIG.enableSelectionAnimation() || !schemAnimInit) {
                        schemAnimX = targetMinX;
                        schemAnimY = targetMinY;
                        schemAnimZ = targetMinZ;
                        schemAnimInit = true;
                    } else {
                        double lerp = 0.3;
                        schemAnimX += (targetMinX - schemAnimX) * lerp;
                        schemAnimY += (targetMinY - schemAnimY) * lerp;
                        schemAnimZ += (targetMinZ - schemAnimZ) * lerp;
                    }

                    double renderMinX = schemAnimX;
                    double renderMinY = schemAnimY;
                    double renderMinZ = schemAnimZ;
                    double renderMaxX = renderMinX + schematicSizeX;
                    double renderMaxY = renderMinY + schematicSizeY;
                    double renderMaxZ = renderMinZ + schematicSizeZ;

                    Vec3d cameraPos = context.camera().getPos();
                    MatrixStack matrices = context.matrixStack();

                    matrices.push();
                    matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

                    VertexConsumerProvider providers = context.consumers();
                    if (providers != null) {
                        VertexConsumer linesConsumer = providers.getBuffer(SELECTION_LINES);
                        WorldRenderer.drawBox(matrices, linesConsumer, renderMinX, renderMinY, renderMinZ, renderMaxX, renderMaxY, renderMaxZ, 0.0f, 0.9f, 1.0f, 1.0f);

                        VertexConsumer quadsConsumer = providers.getBuffer(SELECTION_QUADS);
                        drawFaceQuad(matrices, quadsConsumer, Direction.UP, renderMinX, renderMinY, renderMinZ, renderMaxX, renderMaxY, renderMaxZ, 0.0f, 0.9f, 1.0f, 0.25f);
                        drawFaceQuad(matrices, quadsConsumer, Direction.DOWN, renderMinX, renderMinY, renderMinZ, renderMaxX, renderMaxY, renderMaxZ, 0.0f, 0.9f, 1.0f, 0.25f);
                        drawFaceQuad(matrices, quadsConsumer, Direction.NORTH, renderMinX, renderMinY, renderMinZ, renderMaxX, renderMaxY, renderMaxZ, 0.0f, 0.9f, 1.0f, 0.25f);
                        drawFaceQuad(matrices, quadsConsumer, Direction.SOUTH, renderMinX, renderMinY, renderMinZ, renderMaxX, renderMaxY, renderMaxZ, 0.0f, 0.9f, 1.0f, 0.25f);
                        drawFaceQuad(matrices, quadsConsumer, Direction.WEST, renderMinX, renderMinY, renderMinZ, renderMaxX, renderMaxY, renderMaxZ, 0.0f, 0.9f, 1.0f, 0.25f);
                        drawFaceQuad(matrices, quadsConsumer, Direction.EAST, renderMinX, renderMinY, renderMinZ, renderMaxX, renderMaxY, renderMaxZ, 0.0f, 0.9f, 1.0f, 0.25f);
                    }
                    matrices.pop();
                }
            }

            // 2. РЕНДЕР СТАНДАРТНОЙ РАМКИ ВЫДЕЛЕНИЯ
            if (isSelectionAxe(client.player.getMainHandStack()) && !getSelectionPoints().isEmpty()) {
                Vec3d cameraPos = context.camera().getPos();
                MatrixStack matrices = context.matrixStack();

                matrices.push();
                matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

                VertexConsumerProvider providers = context.consumers();
                if (providers != null) {
                    float[] colors = getSelectionColor();
                    float r = colors[0];
                    float g = colors[1];
                    float b = colors[2];
                    float a = colors[3];

                    VertexConsumer linesConsumer = providers.getBuffer(SELECTION_LINES);
                    MatrixStack.Entry entry = matrices.peek();

                    if (currentSelectionMode == 0 && pos1 != null && pos2 != null) {
                        double targetMinX = Math.min(pos1.getX(), pos2.getX());
                        double targetMinY = Math.min(pos1.getY(), pos2.getY());
                        double targetMinZ = Math.min(pos1.getZ(), pos2.getZ());

                        double targetMaxX = Math.max(pos1.getX(), pos2.getX()) + 1.0;
                        double targetMaxY = Math.max(pos1.getY(), pos2.getY()) + 1.0;
                        double targetMaxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1.0;

                        if (!CONFIG.enableSelectionAnimation()) {
                            animMinX = targetMinX; animMinY = targetMinY; animMinZ = targetMinZ;
                            animMaxX = targetMaxX; animMaxY = targetMaxY; animMaxZ = targetMaxZ;
                            selectionAnimInitialized = true;
                        } else if (!selectionAnimInitialized) {
                            animMinX = targetMinX; animMinY = targetMinY; animMinZ = targetMinZ;
                            animMaxX = targetMaxX; animMaxY = targetMaxY; animMaxZ = targetMaxZ;
                            selectionAnimInitialized = true;
                        } else {
                            float lerpSpeed = 0.25f;
                            animMinX += (targetMinX - animMinX) * lerpSpeed;
                            animMinY += (targetMinY - animMinY) * lerpSpeed;
                            animMinZ += (targetMinZ - animMinZ) * lerpSpeed;
                            animMaxX += (targetMaxX - animMaxX) * lerpSpeed;
                            animMaxY += (targetMaxY - animMaxY) * lerpSpeed;
                            animMaxZ += (targetMaxZ - animMaxZ) * lerpSpeed;
                        }

                        WorldRenderer.drawBox(matrices, linesConsumer, animMinX, animMinY, animMinZ, animMaxX, animMaxY, animMaxZ, r, g, b, a);

                        if (currentToolMode == 0 || currentToolMode == 5) {
                            Direction targetedFace = getTargetedSelectionFace();
                            if (targetedFace != null) {
                                VertexConsumer quadsConsumer = providers.getBuffer(SELECTION_QUADS);
                                drawFaceQuad(matrices, quadsConsumer, targetedFace, animMinX, animMinY, animMinZ, animMaxX, animMaxY, animMaxZ, r, g, b, 0.35F);
                            }
                        }

                    } else if (currentSelectionMode == 1 && selectionPoints.size() >= 2) {
                        double minY = selectionPoints.stream().mapToDouble(BlockPos::getY).min().orElse(0);
                        double maxY = selectionPoints.stream().mapToDouble(BlockPos::getY).max().orElse(0) + 1.0;
                        int size = selectionPoints.size();

                        for (int i = 0; i < size; i++) {
                            BlockPos p1 = selectionPoints.get(i);
                            BlockPos p2 = selectionPoints.get((i + 1) % size);

                            double x1 = p1.getX() + 0.5;
                            double z1 = p1.getZ() + 0.5;
                            double x2 = p2.getX() + 0.5;
                            double z2 = p2.getZ() + 0.5;

                            draw3DLine(entry, linesConsumer, x1, minY, z1, x2, minY, z2, r, g, b, a);
                            draw3DLine(entry, linesConsumer, x1, maxY, z1, x2, maxY, z2, r, g, b, a);
                            draw3DLine(entry, linesConsumer, x1, minY, z1, x1, maxY, z1, r, g, b, a);
                        }

                    } else if (currentSelectionMode == 2 && selectionPoints.size() >= 2) {
                        int size = selectionPoints.size();
                        for (int i = 0; i < size; i++) {
                            BlockPos p1 = selectionPoints.get(i);
                            for (int j = i + 1; j < size; j++) {
                                BlockPos p2 = selectionPoints.get(j);
                                draw3DLine(entry, linesConsumer, p1.getX() + 0.5, p1.getY() + 0.5, p1.getZ() + 0.5, p2.getX() + 0.5, p2.getY() + 0.5, p2.getZ() + 0.5, r, g, b, a);
                            }
                        }
                    }
                }
                matrices.pop();
            } else {
                selectionAnimInitialized = false;
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(OpenMobSpawnerSlabScreenPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                context.client().setScreen(new MobSpawnerSlabScreen(
                        payload.pos(), payload.mobId(), payload.spawnInterval(), payload.maxMobs(), payload.active(), payload.spawnRange()
                ));
            });
        });

        BlockRenderLayerMap.INSTANCE.putBlock(MogDopSMod.MOB_SPAWNER_SLAB, RenderLayer.getCutout());
    }
}