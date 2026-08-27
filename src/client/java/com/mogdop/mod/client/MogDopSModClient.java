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
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    // Пиксельные координаты для режима изображений (16x16 в блоке)
    public static Vec3d imagePos1 = null;
    public static Vec3d imagePos2 = null;
    public static Direction imageSide = Direction.UP;

    public static final List<BlockPos> selectionPoints = new ArrayList<>();

    public static boolean schematicPreviewActive = false;
    public static int schematicSizeX = 1;
    public static int schematicSizeY = 1;
    public static int schematicSizeZ = 1;
    public static String schematicName = "";

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
        if (stack.isOf(MogDopSMod.STAFF)) {
            return true;
        }
        if (!stack.isOf(Items.IRON_AXE)) {
            return false;
        }
        if (!stack.contains(DataComponentTypes.CUSTOM_NAME)) {
            return true;
        }
        String name = stack.getName().getString().toLowerCase();
        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        String customStr = customName != null ? customName.toString().toLowerCase() : "";
        return name.contains("посох") || name.contains("staff") || name.contains("топор") || name.contains("selection") || name.contains("axe") || customStr.contains("selection_axe");
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

    public static double snap16(double val) {
        return Math.round(val * 16.0) / 16.0;
    }

    public static Vec3d getSnappedPixelPoint(BlockHitResult hitResult) {
        BlockPos pos = hitResult.getBlockPos();
        Direction side = hitResult.getSide();
        Vec3d hit = hitResult.getPos();

        double x = snap16(hit.x);
        double y = snap16(hit.y);
        double z = snap16(hit.z);

        switch (side) {
            case UP -> y = pos.getY() + 1.0;
            case DOWN -> y = pos.getY();
            case NORTH -> z = pos.getZ();
            case SOUTH -> z = pos.getZ() + 1.0;
            case WEST -> x = pos.getX();
            case EAST -> x = pos.getX() + 1.0;
        }

        return new Vec3d(x, y, z);
    }

    public static Vec3d getSnappedPointOnPlane(BlockHitResult hitResult, Direction planeSide, Vec3d planePoint) {
        Vec3d hit = hitResult.getPos();
        double x = snap16(hit.x);
        double y = snap16(hit.y);
        double z = snap16(hit.z);

        switch (planeSide.getAxis()) {
            case X -> x = planePoint.x;
            case Y -> y = planePoint.y;
            case Z -> z = planePoint.z;
        }

        return new Vec3d(x, y, z);
    }

    private static Vec3d[] calculatePlanarCorners(Vec3d p1, Vec3d p2, Direction side) {
        double minX = Math.min(p1.x, p2.x); double maxX = Math.max(p1.x, p2.x);
        double minY = Math.min(p1.y, p2.y); double maxY = Math.max(p1.y, p2.y);
        double minZ = Math.min(p1.z, p2.z); double maxZ = Math.max(p1.z, p2.z);

        double off = 0.004;
        double ox = side.getOffsetX() * off;
        double oy = side.getOffsetY() * off;
        double oz = side.getOffsetZ() * off;

        switch (side) {
            case UP, DOWN -> {
                double y = p1.y + oy;
                return new Vec3d[]{
                        new Vec3d(minX, y, minZ),
                        new Vec3d(maxX, y, minZ),
                        new Vec3d(maxX, y, maxZ),
                        new Vec3d(minX, y, maxZ)
                };
            }
            case NORTH, SOUTH -> {
                double z = p1.z + oz;
                return new Vec3d[]{
                        new Vec3d(minX, minY, z),
                        new Vec3d(maxX, minY, z),
                        new Vec3d(maxX, maxY, z),
                        new Vec3d(minX, maxY, z)
                };
            }
            case WEST, EAST -> {
                double x = p1.x + ox;
                return new Vec3d[]{
                        new Vec3d(x, minY, minZ),
                        new Vec3d(x, minY, maxZ),
                        new Vec3d(x, maxY, maxZ),
                        new Vec3d(x, maxY, minZ)
                };
            }
            default -> { return null; }
        }
    }

    private static void drawPlanarQuadFaces(MatrixStack matrices, VertexConsumer quadsConsumer, Vec3d c0, Vec3d c1, Vec3d c2, Vec3d c3, float r, float g, float b, float a) {
        MatrixStack.Entry entry = matrices.peek();
        quadsConsumer.vertex(entry, (float)c0.x, (float)c0.y, (float)c0.z).color(r, g, b, a);
        quadsConsumer.vertex(entry, (float)c1.x, (float)c1.y, (float)c1.z).color(r, g, b, a);
        quadsConsumer.vertex(entry, (float)c2.x, (float)c2.y, (float)c2.z).color(r, g, b, a);
        quadsConsumer.vertex(entry, (float)c3.x, (float)c3.y, (float)c3.z).color(r, g, b, a);

        quadsConsumer.vertex(entry, (float)c3.x, (float)c3.y, (float)c3.z).color(r, g, b, a);
        quadsConsumer.vertex(entry, (float)c2.x, (float)c2.y, (float)c2.z).color(r, g, b, a);
        quadsConsumer.vertex(entry, (float)c1.x, (float)c1.y, (float)c1.z).color(r, g, b, a);
        quadsConsumer.vertex(entry, (float)c0.x, (float)c0.y, (float)c0.z).color(r, g, b, a);
    }

    private static void drawPlanarQuadOutline(MatrixStack matrices, VertexConsumer linesConsumer, Vec3d c0, Vec3d c1, Vec3d c2, Vec3d c3, float r, float g, float b, float a) {
        MatrixStack.Entry entry = matrices.peek();
        linesConsumer.vertex(entry, (float)c0.x, (float)c0.y, (float)c0.z).color(r, g, b, a).normal(0, 1, 0);
        linesConsumer.vertex(entry, (float)c1.x, (float)c1.y, (float)c1.z).color(r, g, b, a).normal(0, 1, 0);

        linesConsumer.vertex(entry, (float)c1.x, (float)c1.y, (float)c1.z).color(r, g, b, a).normal(0, 1, 0);
        linesConsumer.vertex(entry, (float)c2.x, (float)c2.y, (float)c2.z).color(r, g, b, a).normal(0, 1, 0);

        linesConsumer.vertex(entry, (float)c2.x, (float)c2.y, (float)c2.z).color(r, g, b, a).normal(0, 1, 0);
        linesConsumer.vertex(entry, (float)c3.x, (float)c3.y, (float)c3.z).color(r, g, b, a).normal(0, 1, 0);

        linesConsumer.vertex(entry, (float)c3.x, (float)c3.y, (float)c3.z).color(r, g, b, a).normal(0, 1, 0);
        linesConsumer.vertex(entry, (float)c0.x, (float)c0.y, (float)c0.z).color(r, g, b, a).normal(0, 1, 0);
    }

    @Override
    public void onInitializeClient() {
        PlayerBlockHistoryManager.load();

        // 1. Регистрация клиентских команд (/md, /md settings, /md staff и т.д.)
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("md")
                    .executes(ctx -> {
                        MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new SpawnerScreen()));
                        return 1;
                    })
                    .then(ClientCommandManager.literal("settings")
                            .executes(ctx -> {
                                MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new SpawnerScreen(null, 4)));
                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("config")
                            .executes(ctx -> {
                                MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new SpawnerScreen(null, 4)));
                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("guide")
                            .executes(ctx -> {
                                MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new WelcomeScreen()));
                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("staff")
                            .executes(ctx -> {
                                ClientPlayNetworking.send(new GiveItemPayload(new ItemStack(MogDopSMod.STAFF)));
                                if (MinecraftClient.getInstance().player != null) {
                                    MinecraftClient.getInstance().player.sendMessage(Text.literal("§a[MogDop] Выдан Творческий Посох!"), true);
                                }
                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("schematics")
                            .executes(ctx -> {
                                MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new SchematicScreen()));
                                return 1;
                            })
                    )
            );
        });

        // 2. Регистрация биндов клавиш
        openSpawnerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mogdops-mod.spawner",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                "category.mogdops-mod"
        ));

        quickFillKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mogdops-mod.quick_fill",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.mogdops-mod"
        ));

        openToolSelectorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mogdops-mod.tool_selector",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.mogdops-mod"
        ));

        openBlockSelectorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mogdops-mod.block_selector",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "category.mogdops-mod"
        ));

        openSchematicKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mogdops-mod.schematic",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.mogdops-mod"
        ));

        // 3. Регистрация рендереров и слоев
        EntityRendererRegistry.register(MogDopSMod.IMAGE_DISPLAY_ENTITY, ImageDisplayEntityRenderer::new);
        BlockRenderLayerMap.INSTANCE.putBlock(MogDopSMod.MOB_SPAWNER_SLAB, RenderLayer.getCutout());

        // 4. Регистрация HUD
        HudRenderCallback.EVENT.register(new SelectionAxeHud());
        HudRenderCallback.EVENT.register(new ChatNotificationHud());

        // 5. Регистрация S2C пакетов
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
            schematicSizeX = payload.sizeX();
            schematicSizeY = payload.sizeY();
            schematicSizeZ = payload.sizeZ();
            schematicName = payload.filename();
            schematicPreviewActive = true;
        }));

        // 6. Обработка ЛКМ по блоку
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient() || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!isSelectionAxe(player.getMainHandStack())) return ActionResult.PASS;

            switch (currentToolMode) {
                case 0 -> {
                    if (currentSelectionMode == 0) {
                        pos1 = pos;
                        syncSelectionPoints();
                        player.sendMessage(Text.translatable("mogdops-mod.selection.pos1", pos.toShortString()), true);
                    } else {
                        selectionPoints.add(pos);
                        player.sendMessage(Text.translatable("mogdops-mod.selection.point_added", selectionPoints.size(), pos.toShortString()), true);
                    }
                    return ActionResult.FAIL;
                }
                case 1 -> { // Уничтожитель
                    ClientPlayNetworking.send(new ToolActionPayload("REMOVER", pos, 0F, false, CONFIG.toolRemoverRadius()));
                    return ActionResult.FAIL;
                }
                case 2 -> { // Взрыватель
                    ClientPlayNetworking.send(new ToolActionPayload("EXPLOSION", pos, CONFIG.toolExplosionPower(), CONFIG.toolExplosionFire(), 1));
                    return ActionResult.FAIL;
                }
                case 3 -> { // Телепортер
                    ClientPlayNetworking.send(new ToolActionPayload("TELEPORT", pos, 0F, false, 1));
                    return ActionResult.FAIL;
                }
                case 4 -> { // Спавнер
                    ClientPlayNetworking.send(new SpawnEntityPayload(activeSpawnId, activeSpawnCustomName, activeSpawnNameVisible, activeSpawnNoGravity, activeSpawnSilent, activeSpawnGlowing, activeSpawnIsBaby, activeSpawnSlimeSize, activeSpawnFireTicks));
                    return ActionResult.FAIL;
                }
                case 6 -> { // Изображения: Точка 1
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.crosshairTarget instanceof BlockHitResult hitResult) {
                        imagePos1 = getSnappedPixelPoint(hitResult);
                        imageSide = hitResult.getSide();
                        imagePos2 = null;
                        player.sendMessage(Text.literal(String.format(Locale.ROOT, "§a[Изображение] Точка 1: (%.2f, %.2f, %.2f) на грани %s", imagePos1.x, imagePos1.y, imagePos1.z, imageSide.asString())), true);
                    }
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        // 7. Обработка ПКМ по блоку
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient() || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!isSelectionAxe(player.getMainHandStack())) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();

            switch (currentToolMode) {
                case 0 -> {
                    if (currentSelectionMode == 0) {
                        pos2 = pos;
                        syncSelectionPoints();
                        player.sendMessage(Text.translatable("mogdops-mod.selection.pos2", pos.toShortString()), true);
                    } else {
                        if (!selectionPoints.isEmpty()) {
                            selectionPoints.remove(selectionPoints.size() - 1);
                            player.sendMessage(Text.translatable("mogdops-mod.selection.point_removed", selectionPoints.size()), true);
                        }
                    }
                    return ActionResult.SUCCESS;
                }
                case 1 -> { // Уничтожитель
                    ClientPlayNetworking.send(new ToolActionPayload("REMOVER", pos, 0F, false, CONFIG.toolRemoverRadius()));
                    return ActionResult.SUCCESS;
                }
                case 2 -> { // Взрыватель
                    ClientPlayNetworking.send(new ToolActionPayload("EXPLOSION", pos, CONFIG.toolExplosionPower(), CONFIG.toolExplosionFire(), 1));
                    return ActionResult.SUCCESS;
                }
                case 3 -> { // Телепортер
                    ClientPlayNetworking.send(new ToolActionPayload("TELEPORT", pos, 0F, false, 1));
                    return ActionResult.SUCCESS;
                }
                case 4 -> { // Спавнер
                    ClientPlayNetworking.send(new SpawnEntityPayload(activeSpawnId, activeSpawnCustomName, activeSpawnNameVisible, activeSpawnNoGravity, activeSpawnSilent, activeSpawnGlowing, activeSpawnIsBaby, activeSpawnSlimeSize, activeSpawnFireTicks));
                    return ActionResult.SUCCESS;
                }
                case 6 -> { // Изображения: Точка 2
                    if (imagePos1 == null) {
                        player.sendMessage(Text.literal("§c[Изображение] Сначала установите первую точку (ЛКМ)!"), true);
                        return ActionResult.SUCCESS;
                    }

                    imagePos2 = getSnappedPointOnPlane(hitResult, imageSide, imagePos1);
                    player.sendMessage(Text.literal(String.format(Locale.ROOT, "§b[Изображение] Точка 2: (%.2f, %.2f, %.2f)", imagePos2.x, imagePos2.y, imagePos2.z)), true);
                    MinecraftClient.getInstance().setScreen(new ImageSelectorScreen());
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });

        // 8. Обработка кликов в воздухе и на дистанции
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient() || hand != Hand.MAIN_HAND) return TypedActionResult.pass(player.getStackInHand(hand));
            if (!isSelectionAxe(player.getMainHandStack())) return TypedActionResult.pass(player.getStackInHand(hand));

            HitResult hit = player.raycast(100.0D, 1.0F, false);

            switch (currentToolMode) {
                case 1 -> { // Дальнобойный Уничтожитель
                    if (hit.getType() == HitResult.Type.BLOCK) {
                        ClientPlayNetworking.send(new ToolActionPayload("REMOVER", ((BlockHitResult) hit).getBlockPos(), 0F, false, CONFIG.toolRemoverRadius()));
                        return TypedActionResult.success(player.getStackInHand(hand));
                    }
                }
                case 2 -> { // Дальнобойный Взрыватель
                    if (hit.getType() == HitResult.Type.BLOCK) {
                        ClientPlayNetworking.send(new ToolActionPayload("EXPLOSION", ((BlockHitResult) hit).getBlockPos(), CONFIG.toolExplosionPower(), CONFIG.toolExplosionFire(), 1));
                        return TypedActionResult.success(player.getStackInHand(hand));
                    }
                }
                case 3 -> { // Дальнобойный Телепортер
                    if (hit.getType() == HitResult.Type.BLOCK) {
                        ClientPlayNetworking.send(new ToolActionPayload("TELEPORT", ((BlockHitResult) hit).getBlockPos(), 0F, false, 1));
                        return TypedActionResult.success(player.getStackInHand(hand));
                    }
                }
                case 4 -> { // Дальнобойный Спавнер
                    ClientPlayNetworking.send(new SpawnEntityPayload(activeSpawnId, activeSpawnCustomName, activeSpawnNameVisible, activeSpawnNoGravity, activeSpawnSilent, activeSpawnGlowing, activeSpawnIsBaby, activeSpawnSlimeSize, activeSpawnFireTicks));
                    return TypedActionResult.success(player.getStackInHand(hand));
                }
                case 5 -> { // Схематики
                    MinecraftClient.getInstance().setScreen(new SchematicScreen());
                    return TypedActionResult.success(player.getStackInHand(hand));
                }
            }

            return TypedActionResult.pass(player.getStackInHand(hand));
        });

        // 9. Тики клиента (Горячие клавиши)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            notificationManager.update();

            while (openSpawnerKey.wasPressed()) {
                client.setScreen(new SpawnerScreen());
            }

            if (quickFillKey.isPressed() && !(client.currentScreen instanceof QuickFillReplaceScreen)) {
                client.setScreen(new QuickFillReplaceScreen());
            }

            while (openToolSelectorKey.wasPressed()) {
                client.setScreen(new ToolSelectorScreen());
            }

            if (openBlockSelectorKey.isPressed() && !(client.currentScreen instanceof SelectionModeScreen)) {
                client.setScreen(new SelectionModeScreen());
            }

            while (openSchematicKey.wasPressed()) {
                client.setScreen(new SchematicScreen());
            }
        });

        // 10. Приветственный экран при входе
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!CONFIG.hasSeenWelcome()) {
                client.execute(() -> client.setScreen(new WelcomeScreen()));
                CONFIG.hasSeenWelcome(true);
                CONFIG.save();
            }
        });

        // 11. 3D Рендеринг выделения в мире
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            if (!isSelectionAxe(client.player.getMainHandStack())) return;

            Camera camera = context.camera();
            Vec3d camPos = camera.getPos();
            MatrixStack matrices = context.matrixStack();
            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) return;

            float[] col = getSelectionColor();

            // 11.1 Режим изображений
            if (currentToolMode == 6 && imagePos1 != null) {
                Vec3d p2 = imagePos2;
                if (p2 == null && client.crosshairTarget instanceof BlockHitResult hitResult) {
                    p2 = getSnappedPointOnPlane(hitResult, imageSide, imagePos1);
                }

                if (p2 != null) {
                    matrices.push();
                    matrices.translate(-camPos.x, -camPos.y, -camPos.z);

                    Vec3d[] corners = calculatePlanarCorners(imagePos1, p2, imageSide);
                    if (corners != null) {
                        VertexConsumer quads = consumers.getBuffer(SELECTION_QUADS);
                        drawPlanarQuadFaces(matrices, quads, corners[0], corners[1], corners[2], corners[3], 0.0F, 0.8F, 1.0F, 0.35F);

                        VertexConsumer lines = consumers.getBuffer(SELECTION_LINES);
                        drawPlanarQuadOutline(matrices, lines, corners[0], corners[1], corners[2], corners[3], 0.0F, 0.8F, 1.0F, 1.0F);
                    }

                    matrices.pop();
                }
            }

            // 11.2 Режим кубоида WorldEdit
            if (currentToolMode == 0 && currentSelectionMode == 0 && pos1 != null && pos2 != null) {
                double minX = Math.min(pos1.getX(), pos2.getX());
                double minY = Math.min(pos1.getY(), pos2.getY());
                double minZ = Math.min(pos1.getZ(), pos2.getZ());
                double maxX = Math.max(pos1.getX(), pos2.getX()) + 1.0;
                double maxY = Math.max(pos1.getY(), pos2.getY()) + 1.0;
                double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1.0;

                if (!selectionAnimInitialized) {
                    animMinX = minX; animMinY = minY; animMinZ = minZ;
                    animMaxX = maxX; animMaxY = maxY; animMaxZ = maxZ;
                    selectionAnimInitialized = true;
                }

                if (CONFIG.enableSelectionAnimation()) {
                    double speed = 0.25;
                    animMinX += (minX - animMinX) * speed;
                    animMinY += (minY - animMinY) * speed;
                    animMinZ += (minZ - animMinZ) * speed;
                    animMaxX += (maxX - animMaxX) * speed;
                    animMaxY += (maxY - animMaxY) * speed;
                    animMaxZ += (maxZ - animMaxZ) * speed;
                } else {
                    animMinX = minX; animMinY = minY; animMinZ = minZ;
                    animMaxX = maxX; animMaxY = maxY; animMaxZ = maxZ;
                }

                matrices.push();
                matrices.translate(-camPos.x, -camPos.y, -camPos.z);

                VertexConsumer linesConsumer = consumers.getBuffer(SELECTION_LINES);
                WorldRenderer.drawBox(matrices, linesConsumer, animMinX, animMinY, animMinZ, animMaxX, animMaxY, animMaxZ, col[0], col[1], col[2], 1.0F);

                VertexConsumer quadsConsumer = consumers.getBuffer(SELECTION_QUADS);
                Direction hitFace = getTargetedSelectionFace();

                for (Direction dir : Direction.values()) {
                    float faceAlpha = (dir == hitFace) ? 0.35F : 0.12F;
                    drawFaceQuad(matrices, quadsConsumer, dir, animMinX, animMinY, animMinZ, animMaxX, animMaxY, animMaxZ, col[0], col[1], col[2], faceAlpha);
                }

                matrices.pop();
            }

            // 11.3 Режим Полигона 2D
            if (currentToolMode == 0 && currentSelectionMode == 1 && !selectionPoints.isEmpty()) {
                matrices.push();
                matrices.translate(-camPos.x, -camPos.y, -camPos.z);

                int minY = Integer.MAX_VALUE;
                int maxY = Integer.MIN_VALUE;
                for (BlockPos p : selectionPoints) {
                    if (p.getY() < minY) minY = p.getY();
                    if (p.getY() > maxY) maxY = p.getY();
                }
                double y1 = minY;
                double y2 = maxY + 1.0;
                int n = selectionPoints.size();

                VertexConsumer quadsConsumer = consumers.getBuffer(SELECTION_QUADS);
                for (int i = 0; i < n; i++) {
                    if (i > 0 || n >= 3) {
                        BlockPos pA = selectionPoints.get(i);
                        BlockPos pB = selectionPoints.get((i + 1) % n);
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

                VertexConsumer linesConsumer = consumers.getBuffer(SELECTION_LINES);
                for (int i = 0; i < n; i++) {
                    BlockPos p = selectionPoints.get(i);
                    WorldRenderer.drawBox(matrices, linesConsumer, p.getX(), p.getY(), p.getZ(), p.getX() + 1.0, p.getY() + 1.0, p.getZ() + 1.0, 1.0F, 0.8F, 0.0F, 1.0F);

                    double xA = p.getX() + 0.5, zA = p.getZ() + 0.5;

                    linesConsumer.vertex(matrices.peek(), (float)xA, (float)y1, (float)zA).color(col[0], col[1], col[2], 1.0F).normal(0, 1, 0);
                    linesConsumer.vertex(matrices.peek(), (float)xA, (float)y2, (float)zA).color(col[0], col[1], col[2], 1.0F).normal(0, 1, 0);

                    if (i < n - 1 || n >= 3) {
                        BlockPos nextP = selectionPoints.get((i + 1) % n);
                        double xB = nextP.getX() + 0.5, zB = nextP.getZ() + 0.5;

                        linesConsumer.vertex(matrices.peek(), (float)xA, (float)y1, (float)zA).color(col[0], col[1], col[2], 1.0F).normal(0, 1, 0);
                        linesConsumer.vertex(matrices.peek(), (float)xB, (float)y1, (float)zB).color(col[0], col[1], col[2], 1.0F).normal(0, 1, 0);

                        linesConsumer.vertex(matrices.peek(), (float)xA, (float)y2, (float)zA).color(col[0], col[1], col[2], 1.0F).normal(0, 1, 0);
                        linesConsumer.vertex(matrices.peek(), (float)xB, (float)y2, (float)zB).color(col[0], col[1], col[2], 1.0F).normal(0, 1, 0);
                    }
                }

                matrices.pop();
            }

            // 11.4 Режим Выпуклого тела
            if (currentToolMode == 0 && currentSelectionMode == 2 && !selectionPoints.isEmpty()) {
                matrices.push();
                matrices.translate(-camPos.x, -camPos.y, -camPos.z);

                int n = selectionPoints.size();
                VertexConsumer linesConsumer = consumers.getBuffer(SELECTION_LINES);
                for (int i = 0; i < n; i++) {
                    BlockPos p = selectionPoints.get(i);
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

            // 11.5 3D Предпросмотр Схематики
            if (schematicPreviewActive && client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                BlockPos target = ((BlockHitResult) client.crosshairTarget).getBlockPos().offset(((BlockHitResult) client.crosshairTarget).getSide());
                matrices.push();
                matrices.translate(-camPos.x, -camPos.y, -camPos.z);

                VertexConsumer linesConsumer = consumers.getBuffer(SELECTION_LINES);
                WorldRenderer.drawBox(matrices, linesConsumer, target.getX(), target.getY(), target.getZ(), target.getX() + schematicSizeX, target.getY() + schematicSizeY, target.getZ() + schematicSizeZ, 0.0F, 0.8F, 1.0F, 1.0F);

                matrices.pop();
            }
        });
    }
}