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

    public ImageDisplayEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(IMAGE_NAME, "");
        builder.add(COORDS, "0;0;0;0;0;0");
        builder.add(FACING, Direction.UP.getId());
    }

    public void setImageData(String imageName, Vec3d pos1, Vec3d pos2, Direction facing) {
        this.dataTracker.set(IMAGE_NAME, imageName);
        this.dataTracker.set(COORDS, String.format(Locale.ROOT, "%.4f;%.4f;%.4f;%.4f;%.4f;%.4f", pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z));
        this.dataTracker.set(FACING, facing.getId());

        double midX = (pos1.x + pos2.x) / 2.0;
        double midY = (pos1.y + pos2.y) / 2.0;
        double midZ = (pos1.z + pos2.z) / 2.0;
        this.setPos(midX, midY, midZ);

        updateBoundingBox();
    }

    private void updateBoundingBox() {
        Vec3d p1 = getPos1();
        Vec3d p2 = getPos2();
        double minX = Math.min(p1.x, p2.x) - 0.1;
        double maxX = Math.max(p1.x, p2.x) + 0.1;
        double minY = Math.min(p1.y, p2.y) - 0.1;
        double maxY = Math.max(p1.y, p2.y) + 0.1;
        double minZ = Math.min(p1.z, p2.z) - 0.1;
        double maxZ = Math.max(p1.z, p2.z) + 0.1;
        this.setBoundingBox(new Box(minX, minY, minZ, maxX, maxY, maxZ));
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
            setImageData(getImageName(), p1, p2, facing);
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
    }
}