package com.mogdop.mod.client;

import com.mogdop.mod.MogDopSMod;
import com.mogdop.mod.client.gui.ChatNotificationHud;
import com.mogdop.mod.client.gui.ImageSelectorScreen;
import com.mogdop.mod.client.gui.MobSpawnerSlabScreen;
import com.mogdop.mod.client.gui.QuickFillReplaceScreen;
import com.mogdop.mod.client.gui.SchematicScreen;
import com.mogdop.mod.client.gui.SelectionAxeHud;
import com.mogdop.mod.client.gui.SelectionModeScreen;
import com.mogdop.mod.client.gui.SpawnerScreen;
import com.mogdop.mod.client.gui.ToolSelectorScreen;
import com.mogdop.mod.client.render.ImageDisplayEntityRenderer;
import com.mogdop.mod.network.OpenMobSpawnerSlabScreenPayload;
import com.mogdop.mod.network.SchematicPreviewPayload;
import com.mogdop.mod.network.SyncSchematicsListPayload;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public class MogDopSModClient {

    public static KeyBinding openSpawnerKey;
    public static KeyBinding quickFillKey;
    public static KeyBinding openToolSelectorKey;
    public static KeyBinding openBlockSelectorKey;
    public static KeyBinding openSchematicKey;

    public static final MogdopsModConfig CONFIG = initConfig();

    private static MogdopsModConfig initConfig() {
        try {
            return MogdopsModConfig.createAndLoad();
        } catch (Throwable t) {
            MogDopSMod.LOGGER.warn("OWO config not available (NeoForge without owo-lib), using fallback defaults", t);
            // Fallback via dynamic proxy — возвращает дефолты из MogdopsModConfigModel
            MogdopsModConfigModel defaults = new MogdopsModConfigModel();
            return (MogdopsModConfig) java.lang.reflect.Proxy.newProxyInstance(
                    MogdopsModConfig.class.getClassLoader(),
                    new Class[]{MogdopsModConfig.class},
                    (proxy, method, args) -> {
                        String n = method.getName();
                        // save()
                        if (n.equals("save")) return null;
                        // hasSeenWelcome
                        if (n.equals("hasSeenWelcome") && (args == null || args.length == 0)) return defaults.hasSeenWelcome;
                        if (n.equals("hasSeenWelcome") && args != null && args.length == 1) { defaults.hasSeenWelcome = (Boolean) args[0]; return null; }
                        if (n.equals("hideChatHUD") && (args == null || args.length == 0)) return defaults.hideChatHUD;
                        if (n.equals("enableCustomNotifications") && (args == null || args.length == 0)) return defaults.enableCustomNotifications;
                        if (n.equals("vanillaSkin") && (args == null || args.length == 0)) return defaults.vanillaSkin;
                        if (n.equals("toolExplosionFire") && (args == null || args.length == 0)) return defaults.toolExplosionFire;
                        if (n.equals("enableSelectionAnimation") && (args == null || args.length == 0)) return defaults.enableSelectionAnimation;
                        if (n.equals("enableSelectionParticles") && (args == null || args.length == 0)) return defaults.enableSelectionParticles;
                        if (n.equals("toolSelectionColor") && (args == null || args.length == 0)) return defaults.toolSelectionColor;
                        if (n.equals("toolSelectionColor") && args != null && args.length == 1) { defaults.toolSelectionColor = (String) args[0]; return null; }
                        if (n.equals("toolRemoverRadius") && (args == null || args.length == 0)) return defaults.toolRemoverRadius;
                        if (n.equals("toolRemoverRadius") && args != null && args.length == 1) { defaults.toolRemoverRadius = (Integer) args[0]; return null; }
                        if (n.equals("toolExplosionPower") && (args == null || args.length == 0)) return defaults.toolExplosionPower;
                        if (n.equals("toolExplosionPower") && args != null && args.length == 1) { defaults.toolExplosionPower = (Float) args[0]; return null; }
                        // геттеры для boolean с is-prefix (если сгенерированы как isX)
                        if (n.startsWith("is") && (args == null || args.length == 0)) {
                            String field = Character.toLowerCase(n.charAt(2)) + n.substring(3);
                            try { return MogdopsModConfigModel.class.getField(field).get(defaults); } catch (Exception ignored) {}
                        }
                        if (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class) return false;
                        if (method.getReturnType() == int.class || method.getReturnType() == Integer.class) return 0;
                        if (method.getReturnType() == float.class || method.getReturnType() == Float.class) return 0f;
                        if (method.getReturnType() == String.class) return "";
                        return null;
                    });
        }
    }

    public static Block activeBlock = Blocks.STONE;
    public static BlockPos pos1 = null;
    public static BlockPos pos2 = null;

    public static Vec3d imagePos1 = null;
    public static Vec3d imagePos2 = null;
    public static Direction imageSide = Direction.UP;
    /** Поворот черновика/картинки вокруг центра (градусы, против часовой в плоскости грани). */
    public static float imageRotation = 0f;

    public static final List<BlockPos> selectionPoints = new ArrayList<>();

    public static boolean schematicPreviewActive = false;
    public static int schematicSizeX = 1;
    public static int schematicSizeY = 1;
    public static int schematicSizeZ = 1;
    public static String schematicName = "";

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
    private static final SelectionAxeHud SELECTION_AXE_HUD = new SelectionAxeHud();
    private static final ChatNotificationHud CHAT_NOTIFICATION_HUD = new ChatNotificationHud();

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
        if (stack == null || stack.isEmpty()) return false;
        if (stack.isOf(MogDopSMod.STAFF.get())) {
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
        if (hex.startsWith("#")) hex = hex.substring(1);
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

    public static void initClient() {
        PlayerBlockHistoryManager.load();

        // 1. Регистрация клавиш через Architectury KeyMappingRegistry
        openSpawnerKey = new KeyBinding("key.mogdops-mod.spawner", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, "category.mogdops-mod");
        quickFillKey = new KeyBinding("key.mogdops-mod.quick_fill", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.mogdops-mod");
        openToolSelectorKey = new KeyBinding("key.mogdops-mod.tool_selector", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.mogdops-mod");
        openBlockSelectorKey = new KeyBinding("key.mogdops-mod.block_selector", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.mogdops-mod");
        openSchematicKey = new KeyBinding("key.mogdops-mod.schematic", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.mogdops-mod");

        KeyMappingRegistry.register(openSpawnerKey);
        KeyMappingRegistry.register(quickFillKey);
        KeyMappingRegistry.register(openToolSelectorKey);
        KeyMappingRegistry.register(openBlockSelectorKey);
        KeyMappingRegistry.register(openSchematicKey);

        // 2. Кроссплатформенный рендеринг HUD через Architectury Event
        ClientGuiEvent.RENDER_HUD.register((drawContext, tickCounter) -> {
            SELECTION_AXE_HUD.render(drawContext, tickCounter);
            CHAT_NOTIFICATION_HUD.render(drawContext, tickCounter);
        });

        // 3. Регистрация рендереров сущностей
        EntityRendererRegistry.register(MogDopSMod.IMAGE_DISPLAY_ENTITY, ImageDisplayEntityRenderer::new);

        // 3.1 S2C-пакеты через Architectury — работает и на Fabric, и на NeoForge.
        // Раньше они были только на Fabric через Fabric API, поэтому на NeoForge
        // экран спавнера и список схематик никогда не открывались.
        NetworkManager.registerReceiver(NetworkManager.s2c(), OpenMobSpawnerSlabScreenPayload.ID, OpenMobSpawnerSlabScreenPayload.CODEC, (payload, context) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> client.setScreen(new MobSpawnerSlabScreen(
                    payload.pos(), payload.mobId(), payload.spawnInterval(),
                    payload.maxMobs(), payload.active(), payload.spawnRange())));
        });
        NetworkManager.registerReceiver(NetworkManager.s2c(), SyncSchematicsListPayload.ID, SyncSchematicsListPayload.CODEC, (payload, context) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                SchematicScreen.cachedSchematicsList.clear();
                SchematicScreen.cachedSchematicsList.addAll(payload.files());
                if (client.currentScreen instanceof SchematicScreen screen) {
                    screen.rebuildFilesUI();
                }
            });
        });
        NetworkManager.registerReceiver(NetworkManager.s2c(), SchematicPreviewPayload.ID, SchematicPreviewPayload.CODEC, (payload, context) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                schematicSizeX = payload.sizeX();
                schematicSizeY = payload.sizeY();
                schematicSizeZ = payload.sizeZ();
                schematicName = payload.filename();
                schematicPreviewActive = true;
            });
        });

        // 4. Тики клиента
        ClientTickEvent.CLIENT_POST.register(client -> {
            notificationManager.update();

            while (openSpawnerKey.wasPressed()) {
                client.setScreen(new SpawnerScreen());
            }

            while (quickFillKey.wasPressed()) {
                if (!(client.currentScreen instanceof QuickFillReplaceScreen)) {
                    client.setScreen(new QuickFillReplaceScreen());
                }
            }

            while (openToolSelectorKey.wasPressed()) {
                client.setScreen(new ToolSelectorScreen());
            }

            while (openBlockSelectorKey.wasPressed()) {
                if (!(client.currentScreen instanceof SelectionModeScreen)) {
                    client.setScreen(new SelectionModeScreen());
                }
            }

            while (openSchematicKey.wasPressed()) {
                client.setScreen(new SchematicScreen());
            }
        });
    }
}