package com.mogdop.mod.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BaseBlock;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorldEditIntegration {

    private static Player getActor(ServerPlayerEntity player) {
        return FabricAdapter.adaptPlayer(player);
    }

    private static LocalSession getSession(ServerPlayerEntity player) {
        return WorldEdit.getInstance().getSessionManager().get(getActor(player));
    }

    private static com.sk89q.worldedit.world.World getWorld(ServerPlayerEntity player) {
        return FabricAdapter.adapt(player.getServerWorld());
    }

    private static ParserContext createParserContext(ServerPlayerEntity player) {
        ParserContext context = new ParserContext();
        context.setActor(getActor(player));
        context.setWorld(getWorld(player));
        context.setSession(getSession(player));
        return context;
    }

    private static Region createRegion(ServerPlayerEntity player, List<BlockPos> points, int mode) {
        if (points == null || points.isEmpty()) return null;
        com.sk89q.worldedit.world.World weWorld = getWorld(player);

        if (mode == 0) { // Cuboid
            if (points.size() < 2) return null;
            BlockPos p1 = points.get(0);
            BlockPos p2 = points.get(1);
            BlockVector3 v1 = BlockVector3.at(p1.getX(), p1.getY(), p1.getZ());
            BlockVector3 v2 = BlockVector3.at(p2.getX(), p2.getY(), p2.getZ());
            return new CuboidRegion(weWorld, v1, v2);
        } else if (mode == 1) { // Poly 2D
            if (points.size() < 3) return null;
            List<com.sk89q.worldedit.math.BlockVector2> points2D = new ArrayList<>();
            int minY = points.get(0).getY();
            int maxY = points.get(0).getY();
            for (BlockPos p : points) {
                points2D.add(com.sk89q.worldedit.math.BlockVector2.at(p.getX(), p.getZ()));
                if (p.getY() < minY) minY = p.getY();
                if (p.getY() > maxY) maxY = p.getY();
            }
            return new Polygonal2DRegion(weWorld, points2D, minY, maxY);
        } else if (mode == 2) { // Convex 3D
            if (points.size() < 2) return null;
            ConvexPolyhedralRegion region = new ConvexPolyhedralRegion(weWorld);
            for (BlockPos p : points) {
                region.addVertex(BlockVector3.at(p.getX(), p.getY(), p.getZ()));
            }
            return region;
        }
        return null;
    }

    public static void copyClipboard(ServerPlayerEntity player, List<BlockPos> points, int mode) {
        Region region = createRegion(player, points, mode);
        if (region == null) return;

        try {
            com.sk89q.worldedit.world.World weWorld = getWorld(player);
            BlockArrayClipboard target = new BlockArrayClipboard(region);
            target.setOrigin(region.getMinimumPoint());

            ForwardExtentCopy copy = new ForwardExtentCopy(
                    weWorld, region, target, region.getMinimumPoint()
            );
            Operations.complete(copy);

            LocalSession session = getSession(player);
            session.setClipboard(new ClipboardHolder(target));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveSchematic(ServerPlayerEntity player, String filename) {
        if (!filename.endsWith(".schem") && !filename.endsWith(".schematic")) {
            filename += ".schem";
        }
        LocalSession session = getSession(player);
        try {
            ClipboardHolder holder = session.getClipboard();
            if (holder == null) return;
            Clipboard clipboard = holder.getClipboard();

            File folder = new File("worldedit/schematics");
            if (!folder.exists()) folder.mkdirs();
            File file = new File(folder, filename);

            ClipboardFormat format = ClipboardFormats.findByAlias("schem");
            if (format == null) format = BuiltInClipboardFormat.SPONGE_SCHEMATIC;

            try (FileOutputStream fos = new FileOutputStream(file);
                 ClipboardWriter writer = format.getWriter(fos)) {
                writer.write(clipboard);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Vec3i loadSchematic(ServerPlayerEntity player, String filename) {
        if (!filename.endsWith(".schem") && !filename.endsWith(".schematic")) {
            filename += ".schem";
        }
        File folder = new File("worldedit/schematics");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, filename);
        if (!file.exists()) return null;

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) format = BuiltInClipboardFormat.SPONGE_SCHEMATIC;

        try (FileInputStream fis = new FileInputStream(file);
             ClipboardReader reader = format.getReader(fis)) {
            Clipboard clipboard = reader.read();
            LocalSession session = getSession(player);
            session.setClipboard(new ClipboardHolder(clipboard));

            BlockVector3 dims = clipboard.getDimensions();
            return new Vec3i(dims.getBlockX(), dims.getBlockY(), dims.getBlockZ());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void pasteClipboard(ServerPlayerEntity player, boolean ignoreAir) {
        LocalSession session = getSession(player);
        try {
            ClipboardHolder holder = session.getClipboard();
            if (holder == null) return;

            com.sk89q.worldedit.world.World weWorld = getWorld(player);
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                BlockVector3 to = BlockVector3.at(player.getBlockPos().getX(), player.getBlockPos().getY(), player.getBlockPos().getZ());
                Operation operation = holder
                        .createPaste(editSession)
                        .to(to)
                        .ignoreAirBlocks(ignoreAir)
                        .build();
                Operations.complete(operation);
                session.remember(editSession);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void rotateClipboard(ServerPlayerEntity player, int degrees) {
        LocalSession session = getSession(player);
        try {
            ClipboardHolder holder = session.getClipboard();
            if (holder == null) return;
            AffineTransform transform = new AffineTransform();
            transform = transform.rotateY(degrees);
            holder.setTransform(holder.getTransform().combine(transform));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getSchematicsList() {
        List<String> list = new ArrayList<>();
        File folder = new File("worldedit/schematics");
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".schem") || name.endsWith(".schematic"));
            if (files != null) {
                for (File f : files) list.add(f.getName());
            }
        }
        return list;
    }

    public static void walls(ServerPlayerEntity player, List<BlockPos> points, int mode, String blockId) {
        Region region = createRegion(player, points, mode);
        if (region == null) return;
        com.sk89q.worldedit.world.World weWorld = getWorld(player);
        LocalSession session = getSession(player);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            Pattern pattern = WorldEdit.getInstance().getPatternFactory().parseFromInput(blockId, createParserContext(player));
            editSession.makeWalls(region, pattern);
            session.remember(editSession);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void outline(ServerPlayerEntity player, List<BlockPos> points, int mode, String blockId) {
        Region region = createRegion(player, points, mode);
        if (region == null) return;
        com.sk89q.worldedit.world.World weWorld = getWorld(player);
        LocalSession session = getSession(player);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            Pattern pattern = WorldEdit.getInstance().getPatternFactory().parseFromInput(blockId, createParserContext(player));
            editSession.makeCuboidFaces(region, pattern);
            session.remember(editSession);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void replaceArea(ServerPlayerEntity player, List<BlockPos> points, int mode, String targetId, String replaceId) {
        Region region = createRegion(player, points, mode);
        if (region == null) return;
        com.sk89q.worldedit.world.World weWorld = getWorld(player);
        LocalSession session = getSession(player);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            Pattern pattern = WorldEdit.getInstance().getPatternFactory().parseFromInput(replaceId, createParserContext(player));
            BaseBlock targetBlock = WorldEdit.getInstance().getBlockFactory().parseFromInput(targetId, createParserContext(player));
            editSession.replaceBlocks(region, Collections.singleton(targetBlock), pattern);
            session.remember(editSession);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void fillArea(ServerPlayerEntity player, List<BlockPos> points, int mode, String blockId) {
        Region region = createRegion(player, points, mode);
        if (region == null) return;
        com.sk89q.worldedit.world.World weWorld = getWorld(player);
        LocalSession session = getSession(player);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            Pattern pattern = WorldEdit.getInstance().getPatternFactory().parseFromInput(blockId, createParserContext(player));
            editSession.setBlocks(region, pattern);
            session.remember(editSession);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void undo(ServerPlayerEntity player) {
        LocalSession session = getSession(player);
        session.undo(session.getBlockBag(getActor(player)), getActor(player));
    }

    public static void redo(ServerPlayerEntity player) {
        LocalSession session = getSession(player);
        session.redo(session.getBlockBag(getActor(player)), getActor(player));
    }

    public static void drain(ServerPlayerEntity player, int radius) {
        com.sk89q.worldedit.world.World weWorld = getWorld(player);
        LocalSession session = getSession(player);
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            BlockVector3 center = BlockVector3.at(player.getBlockPos().getX(), player.getBlockPos().getY(), player.getBlockPos().getZ());
            editSession.drainArea(center, radius);
            session.remember(editSession);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}