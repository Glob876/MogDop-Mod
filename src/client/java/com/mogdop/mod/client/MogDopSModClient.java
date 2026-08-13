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
import com.mogdop.mod.client.gui.ImageSelectorScreen;
import com.mogdop.mod.client.gui.WelcomeScreen;
import com.mogdop.mod.client.gui.SelectionAxeHud;
import com.mogdop.mod.client.gui.ChatNotificationHud;
import com.mogdop.mod.client.render.ImageDisplayEntityRenderer;
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
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
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

    // Режим работы с точечными координатами изображений (по пикселям / поверхности)
    public static Vec3d imagePos1 = null;
    public static Vec3d imagePos2 = null;
    public static Direction imageSide = Direction.UP;

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
            "mogdops-mod.tool_selector.modes.schematics",
            "mogdops-mod.tool_selector.modes.image"
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
                consumer.vertex(entry, (float)x2, (float)