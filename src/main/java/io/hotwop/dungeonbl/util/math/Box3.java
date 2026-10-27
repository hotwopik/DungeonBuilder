package io.hotwop.dungeonbl.util.math;

import org.bukkit.util.BoundingBox;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public record Box3(
    int x1,int y1,int z1,
    int x2,int y2,int z2
) implements Iterable<Vec3>{
    public Box3(
        int x1,int y1,int z1,
        int x2,int y2,int z2
    ){
        this.x1=Math.min(x1,x2);
        this.y1=Math.min(y1,y2);
        this.z1=Math.min(z1,z2);
        this.x2=Math.max(x1,x2);
        this.y2=Math.max(y1,y2);
        this.z2=Math.max(z1,z2);
    }

    public Box3(Vec3 first,Vec3 second){
        this(first.x(),first.y(),first.z(),second.x(),second.y(),second.z());
    }

    public static Box3 withSizeAndRotation(Vec3 origin, Vec3 size, SideRotation rotation){
        int x2=size.x()-1;
        int y2=size.y()-1;
        int z2=size.z()-1;

        return new Box3(
            origin.x(),origin.y(),origin.z(),
            origin.x()+x2*rotation.cos+z2*rotation.sin,
            origin.y()+y2,
            origin.z()+z2*rotation.cos-x2*rotation.sin
        );
    }

    public static Box3 withSize(Vec3 origin,Vec3 size){
        return new Box3(
            origin.x(),origin.y(),origin.z(),
            origin.x()+size.x()-1,origin.y()+size.y()-1,origin.z()+size.z()-1
        );
    }

    public Box3 add(Vec3 vec){
        return new Box3(
            x1+vec.x(),y1+vec.y(),z1+vec.z(),
            x2+vec.x(),y2+vec.y(),z2+vec.z()
        );
    }

    public Box3 sub(Vec3 vec){
        return new Box3(
            x1-vec.x(),y1-vec.y(),z1-vec.z(),
            x2-vec.x(),y2-vec.y(),z2-vec.z()
        );
    }

    public Box3 rotate(SideRotation rotation){
        return new Box3(
            x1*rotation.cos+z1*rotation.sin,y1,z1*rotation.cos-x1*rotation.sin,
            x2*rotation.cos+z2*rotation.sin,y2,z2*rotation.cos-x2*rotation.sin
        );
    }

    public Vec3 min(){
        return new Vec3(x1,y1,z1);
    }

    public Vec3 max(){
        return new Vec3(x2,y2,z2);
    }

    public Vec3 size(){
        return new Vec3(x2-x1+1,y2-y1+1,z2-x1+1);
    }

    public int volume(){
        return (x2-x1+1)*(y2-y1+1)*(z2-x1+1);
    }

    public boolean collides(Box3 other){
        return x1<=other.x2&&y1<=other.y2&&z1<=other.z2&&
            x2>=other.x1&&y2>=other.y1&&z2>=other.z1;
    }

    public boolean collides(BoundingBox other){
        return x1<=(int)other.getMaxX() && y1<=(int)other.getMaxY() && z1<=(int)other.getMaxZ() &&
            x2>=(int)other.getMinX() && y2>=(int)other.getMinY() && z2>=(int)other.getMinZ();
    }

    public BoundingBox toBoundingBox(){
        return new BoundingBox(x1,y1,z1,x2+1,y2+1,z2+1);
    }

    public Set<Vec2> occupiedChunks(){
        Set<Vec2> occupied=new HashSet<>();

        int minX=x1>>4;
        int minZ=z1>>4;
        int maxX=x2>>4;
        int maxZ=z2>>4;

        for(int x=minX;x<=maxX;x++){
            for(int z=minZ;z<=maxZ;z++){
                occupied.add(new Vec2(x,z));
            }
        }

        return occupied;
    }

    @Override
    public @NonNull Iterator<Vec3> iterator() {
        return new Iter();
    }

    public final class Iter implements Iterator<Vec3>{
        private int x;
        private int y;
        private int z;

        private Iter(){
            this.x=x1-1;
            this.y=y1;
            this.z=z1;
        }

        private void addX(){
            if(x==x2){
                x=x1;
                addZ();
            }else{
                x++;
            }
        }

        private void addZ(){
            if(z==z2){
                z=z1;
                addY();
            }else{
                z++;
            }
        }

        private void addY(){
            if(y==y2){
                x=x2;
                z=z2;
                throw new NoSuchElementException();
            }
            y++;
        }

        @Override
        public boolean hasNext() {
            return y!=y2 || z!=z2 || x!=x2;
        }

        @Override
        public Vec3 next() {
            addX();
            return new Vec3(x,y,z);
        }
    }
}
