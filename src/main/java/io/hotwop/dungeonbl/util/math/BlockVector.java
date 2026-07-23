package io.hotwop.dungeonbl.util.math;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public final class BlockVector extends Vector{
    public BlockVector(Vector parent){
        copy(parent);
    }

    public BlockVector(Vec3 vec){
        x=vec.x();
        y=vec.y();
        z=vec.z();
    }

    public BlockVector(Vec3d vec){
        x=(int)vec.x();
        y=(int)vec.y();
        z=(int)vec.z();
    }

    public BlockVector(){
        x=0;
        y=0;
        z=0;
    }

    public BlockVector(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @NotNull
    public BlockVector add(@NotNull BlockVector vec) {
        return (BlockVector) super.add(vec);
    }

    @NotNull
    public BlockVector subtract(@NotNull BlockVector vec) {
        return (BlockVector) super.subtract(vec);
    }

    @NotNull
    public BlockVector multiply(@NotNull BlockVector vec) {
        return (BlockVector) super.multiply(vec);
    }

    @NotNull
    public BlockVector divide(@NotNull BlockVector vec) {
        return (BlockVector) super.divide(vec);
    }

    @NotNull
    public BlockVector copy(@NotNull Vector vec) {
        x=(int)vec.getX();
        y=(int)vec.getY();
        z=(int)vec.getZ();
        return this;
    }

    @NotNull
    public BlockVector copy(@NotNull BlockVector vec){
        return (BlockVector)super.copy(vec);
    }

    @NotNull
    public BlockVector midpoint(@NotNull Vector other) {
        x = Math.round((x + other.getX()) / 2);
        y = Math.round((y + other.getY()) / 2);
        z = Math.round((z + other.getZ()) / 2);
        return this;
    }

    @NotNull
    public BlockVector multiply(int m) {
        x *= m;
        y *= m;
        z *= m;
        return this;
    }

    @NotNull
    public BlockVector crossProduct(@NotNull BlockVector o) {
        return (BlockVector)super.crossProduct(o);
    }

    @NotNull
    public BlockVector zero() {
        x = 0;
        y = 0;
        z = 0;
        return this;
    }

    @NotNull
    public BlockVector rotateAroundX(double angle) {
        double angleCos = Math.cos(angle);
        double angleSin = Math.sin(angle);

        int y = (int)Math.round(angleCos * getY() - angleSin * getZ());
        int z = (int)Math.round(angleSin * getY() + angleCos * getZ());
        return setY(y).setZ(z);
    }

    @NotNull
    public BlockVector rotateAroundY(double angle) {
        double angleCos = Math.cos(angle);
        double angleSin = Math.sin(angle);

        int x = (int)Math.round(angleCos * getX() + angleSin * getZ());
        int z = (int)Math.round(-angleSin * getX() + angleCos * getZ());
        return setX(x).setZ(z);
    }

    @NotNull
    public BlockVector rotateAroundZ(double angle) {
        double angleCos = Math.cos(angle);
        double angleSin = Math.sin(angle);

        int x = (int)Math.round(angleCos * getX() - angleSin * getY());
        int y = (int)Math.round(angleSin * getX() + angleCos * getY());
        return setX(x).setY(y);
    }

    @NotNull
    public BlockVector setX(int x) {
        this.x = x;
        return this;
    }
    @NotNull
    public BlockVector setY(int y) {
        this.y = y;
        return this;
    }
    @NotNull
    public BlockVector setZ(int z) {
        this.z = z;
        return this;
    }

    @NotNull
    @Override
    public BlockVector clone() {
        return (BlockVector) super.clone();
    }
}
