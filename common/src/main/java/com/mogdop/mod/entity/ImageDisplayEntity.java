package com.mogdop.mod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Locale;

public class ImageDisplayEntity extends Entity {

    private static final TrackedData<String> IMAGE_NAME = DataTracker.registerData(ImageDisplayEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> COORDS = DataTracker.registerData(ImageDisplayEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Integer> FACING = DataTracker.registerData(ImageDisplayEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> ROTATION = DataTracker.registerData(ImageDisplayEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public ImageDisplayEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(IMAGE_NAME, "");
        builder.add(COORDS, "0;0;0;0;0;0");
        builder.add(FACING, Direction.UP.getId());
        builder.add(ROTATION, 0f);
    }

    public void setImageData(String imageName, Vec3d pos1, Vec3d pos2, Direction facing) {
        setImageData(imageName, pos1, pos2, facing, 0f);
    }

    public void setImageData(String imageName, Vec3d pos1, Vec3d pos2, Direction facing, float rotation) {
        this.dataTracker.set(IMAGE_NAME, imageName);
        this.dataTracker.set(COORDS, String.format(Locale.ROOT, "%.4f;%.4f;%.4f;%.4f;%.4f;%.4f", pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z));
        this.dataTracker.set(FACING, facing.getId());
        this.dataTracker.set(ROTATION, rotation);

        // Смещаем центр сущности на 1 см наружу в воздух, чтобы координаты хитбокса не были замурованы в блоке
        double midX = (pos1.x + pos2.x) / 2.0 + facing.getOffsetX() * 0.01;
        double midY = (pos1.y + pos2.y) / 2.0 + facing.getOffsetY() * 0.01;
        double midZ = (pos1.z + pos2.z) / 2.0 + facing.getOffsetZ() * 0.01;
        this.setPos(midX, midY, midZ);

        updateBoundingBox();
    }

    private void updateBoundingBox() {
        Vec3d p1 = getPos1();
        Vec3d p2 = getPos2();
        Direction side = getFacingSide();
        float angle = getRotation();
        double[] uv1 = toUV(p1, side);
        double[] uv2 = toUV(p2, side);
        double cu = (uv1[0] + uv2[0]) / 2.0;
        double cv = (uv1[1] + uv2[1]) / 2.0;
        double hw = Math.abs(uv2[0] - uv1[0]) / 2.0;
        double hh = Math.abs(uv2[1] - uv1[1]) / 2.0;
        // Полуоси повёрнутого прямоугольника (относительно центра)
        double rad = Math.toRadians(angle);
        double cos = Math.abs(Math.cos(rad));
        double sin = Math.abs(Math.sin(rad));
        double rhw = hw * cos + hh * sin;
        double rhh = hw * sin + hh * cos;
        double plane = planeCoord(p1, side);
        Box box = aabbFromUV(side, cu - rhw, cu + rhw, cv - rhh, cv + rhh, plane);
        this.setBoundingBox(box.expand(0.05));
    }

    /**
     * Проекция точки на плоскость грани: {u, v}.
     * Y: u=x, v=z · Z: u=x, v=y · X: u=z, v=y.
     */
    public static double[] toUV(Vec3d p, Direction side) {
        return switch (side.getAxis()) {
            case Y -> new double[]{p.x, p.z};
            case Z -> new double[]{p.x, p.y};
            case X -> new double[]{p.z, p.y};
        };
    }

    /** Координата вдоль нормали грани. */
    public static double planeCoord(Vec3d p, Direction side) {
        return switch (side.getAxis()) {
            case Y -> p.y;
            case Z -> p.z;
            case X -> p.x;
        };
    }

    /** Точка из (u, v) + координаты плоскости обратно в мир. */
    public static Vec3d fromUV(double u, double v, double plane, Direction side) {
        return switch (side.getAxis()) {
            case Y -> new Vec3d(u, plane, v);
            case Z -> new Vec3d(u, v, plane);
            case X -> new Vec3d(plane, v, u);
        };
    }

    /** AABB из UV-границ. */
    public static Box aabbFromUV(Direction side, double umin, double umax, double vmin, double vmax, double plane) {
        Vec3d a = fromUV(umin, vmin, plane, side);
        Vec3d b = fromUV(umax, vmax, plane, side);
        return new Box(
                Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z),
                Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
    }

    /** Поворот точки вокруг центра в плоскости грани (против часовой, градусы). */
    public static Vec3d rotateAroundCenter(Vec3d p, Vec3d center, Direction side, float angleDeg) {
        if (angleDeg == 0f) return p;
        double[] uv = toUV(p, side);
        double[] cuv = toUV(center, side);
        double rad = Math.toRadians(angleDeg);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double du = uv[0] - cuv[0];
        double dv = uv[1] - cuv[1];
        double ru = cuv[0] + du * cos - dv * sin;
        double rv = cuv[1] + du * sin + dv * cos;
        return fromUV(ru, rv, planeCoord(p, side), side);
    }

    public String getImageName() {
        return this.dataTracker.get(IMAGE_NAME);
    }

    public Vec3d getPos1() {
        String c = this.dataTracker.get(COORDS);
        if (c == null || c.isEmpty()) return Vec3d.ZERO;
        String[] split = c.split(";");
        if (split.length >= 3) {
            try {
                return new Vec3d(Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
            } catch (Exception ignored) {}
        }
        return Vec3d.ZERO;
    }

    public Vec3d getPos2() {
        String c = this.dataTracker.get(COORDS);
        if (c == null || c.isEmpty()) return Vec3d.ZERO;
        String[] split = c.split(";");
        if (split.length >= 6) {
            try {
                return new Vec3d(Double.parseDouble(split[3]), Double.parseDouble(split[4]), Double.parseDouble(split[5]));
            } catch (Exception ignored) {}
        }
        return Vec3d.ZERO;
    }

    public Direction getFacingSide() {
        return Direction.byId(this.dataTracker.get(FACING));
    }

    public float getRotation() {
        Float r = this.dataTracker.get(ROTATION);
        return r == null ? 0f : r;
    }

    /**
     * setImageData выполняется только на сервере — на клиенте координаты
     * прилетают синком трекера, и без этого бокс навсегда остаётся дефолтным
     * 0.5³ в центре: как только центр уходит из кадра, движок отсекает
     * сущность целиком вместе с картинкой.
     */
    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (data == COORDS || data == FACING || data == ROTATION) {
            updateBoundingBox();
        }
    }

    @Override
    public boolean canHit() {
        return !this.isRemoved();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!this.getWorld().isClient && !this.isRemoved()) {
            this.discard();
            return true;
        }
        return super.damage(source, amount);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("ImageName")) {
            this.dataTracker.set(IMAGE_NAME, nbt.getString("ImageName"));
        }
        if (nbt.contains("P1X")) {
            Vec3d p1 = new Vec3d(nbt.getDouble("P1X"), nbt.getDouble("P1Y"), nbt.getDouble("P1Z"));
            Vec3d p2 = new Vec3d(nbt.getDouble("P2X"), nbt.getDouble("P2Y"), nbt.getDouble("P2Z"));
            Direction facing = Direction.byId(nbt.getInt("Facing"));
            float rotation = nbt.contains("Rotation") ? nbt.getFloat("Rotation") : 0f;
            setImageData(getImageName(), p1, p2, facing, rotation);
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putString("ImageName", getImageName());
        Vec3d p1 = getPos1();
        Vec3d p2 = getPos2();
        nbt.putDouble("P1X", p1.x);
        nbt.putDouble("P1Y", p1.y);
        nbt.putDouble("P1Z", p1.z);
        nbt.putDouble("P2X", p2.x);
        nbt.putDouble("P2Y", p2.y);
        nbt.putDouble("P2Z", p2.z);
        nbt.putInt("Facing", getFacingSide().getId());
        nbt.putFloat("Rotation", getRotation());
    }
}