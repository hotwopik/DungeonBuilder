package io.hotwop.dungeonbl;

import de.tr7zw.changeme.nbtapi.NBTType;

import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTList;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBTList;
import io.hotwop.dungeonbl.util.math.*;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;

public final class DBlUtil {
    public static boolean giveIfCan(Entity entity, ItemStack item){
        if(entity==null||item==null)return false;
        if(entity instanceof InventoryHolder){
            return ((InventoryHolder)entity).getInventory().addItem(item).isEmpty();
        }
        return false;
    }

    public static void drawParticleLine(
        Particle.DustOptions particle,
        World world,List<Player> players,
        Vector origin,Vector dir,double space
    ){
        double distance=dir.length();
        Vector line=dir.clone().normalize().multiply(space);
        Vector pos=origin.clone();

        for(double drawed=0;drawed<=distance;pos.add(line)){
            world.spawnParticle(Particle.DUST,players,null,pos.getX(),pos.getY(),pos.getZ(),1,0,0,0,0,particle,false);
            drawed+=space;
        }
    }

    public static void drawParticleLine(
        Particle.DustOptions particle,
        World world,List<Player> players,
        AsVec origin,AsVec dir,double space
    ){
        double length=dir.length();
        Vector line=dir.toVector().normalize().multiply(space);
        Vector pos=origin.toVector();

        for(double drawed=0;drawed<=length;pos.add(line)){
            world.spawnParticle(Particle.DUST,players,null,pos.getX(),pos.getY(),pos.getZ(),1,0,0,0,0,particle,false);
            drawed+=space;
        }
    }

    public static void drawMultipleLines(
        Particle.DustOptions particle,
        World world,List<Player> players,
        Vector origin,Vector dir,double space,List<Vector> poses
    ){
        poses.forEach(pos->drawParticleLine(particle,world,players,origin.clone().add(pos),dir.clone(),space));
    }

    public static void drawMultipleLines(
        Particle.DustOptions particle,
        World world, List<Player> players,
        AsVec origin, AsVec dir, double space, List<AsVec> poses
    ){
        poses.forEach(pos->drawParticleLine(particle,world,players,origin.add(pos),dir,space));
    }

    private static final double arrowLeftRadians=Math.toRadians(120);
    private static final double arrowRightRadians=Math.toRadians(240);

    public static void drawArrow(
        Particle.DustOptions particle,
        World world,List<Player> players,
        Vector origin,Vector dir,double space,double arrowWidth
    ){
        drawParticleLine(particle,world,players,origin.clone(),dir.clone(),space);
        Vector arrowEnd=origin.clone().add(dir);

        Vector arrowLeft=dir.clone().rotateAroundY(arrowLeftRadians).multiply(arrowWidth);
        Vector arrowRight=dir.clone().rotateAroundY(arrowRightRadians).multiply(arrowWidth);

        drawParticleLine(particle,world,players,arrowEnd.clone(),arrowLeft,space);
        drawParticleLine(particle,world,players,arrowEnd.clone(),arrowRight,space);
    }

    public static void drawArrow(
        Particle.DustOptions particle,
        World world,List<Player> players,
        Vec3d origin,Vec3d dir,double space,double arrowWidth
    ){
        drawParticleLine(particle,world,players,origin,dir,space);
        Vec3d arrowEnd=origin.add(dir);

        Vec3d arrowLeft=dir.rotate(arrowLeftRadians).mul(arrowWidth);
        Vec3d arrowRight=dir.rotate(arrowRightRadians).mul(arrowWidth);

        drawParticleLine(particle,world,players,arrowEnd,arrowLeft,space);
        drawParticleLine(particle,world,players,arrowEnd,arrowRight,space);
    }

    public static void drawCircle(
        Particle.DustOptions particle,
        World world,List<Player> players,
        Vector center,double radius
    ){
        for(double degrees=0;degrees<=360;degrees+=10/radius){
            Vector pos=center.clone().add(new Vector(0,0,radius).rotateAroundY(Math.toRadians(degrees)));
            world.spawnParticle(Particle.DUST,players,null,pos.getX(),pos.getY(),pos.getZ(),1,0,0,0,0,particle,false);
        }
    }

    public static void drawBox(
        Particle.DustOptions particle,
        World world,List<Player> players,
        Box3 box
    ){
        Vec3 origin=box.min();
        Vec3 size=box.size();

        Vec3 x=new Vec3(size.x(),0,0);
        Vec3 y=new Vec3(0,size.y(),0);
        Vec3 z=new Vec3(0,0,size.z());

        Vec3 xz=x.add(z);
        Vec3 yz=y.add(z);
        Vec3 xy=x.add(y);

        drawMultipleLines(
            particle,
            world,players,
            origin,
            x,
            0.1,
            List.of( Vec3.ZERO, y, z, yz )
        );
        drawMultipleLines(
            particle,
            world,players,
            origin,
            y,
            0.1,
            List.of( Vec3.ZERO, x, z, xz )
        );
        drawMultipleLines(
            particle,
            world,players,
            origin,
            z,
            0.1,
            List.of( Vec3.ZERO, x, y, xy )
        );
    }

    public static void locationToData(Location location,ReadWriteNBT out){
        out.setString("world",location.getWorld().getName());

        out.setDouble("x",location.getX());
        out.setDouble("y",location.getY());
        out.setDouble("z",location.getZ());

        out.setFloat("yaw",location.getYaw());
        out.setFloat("pitch",location.getPitch());
    }

    public static Location dataToLocation(ReadableNBT data){
        if(!(
            data.hasTag("world", NBTType.NBTTagString)
            &&data.hasTag("x",NBTType.NBTTagDouble)
            &&data.hasTag("y",NBTType.NBTTagDouble)
            &&data.hasTag("z",NBTType.NBTTagDouble)
            &&data.hasTag("yaw",NBTType.NBTTagDouble)
            &&data.hasTag("pitch",NBTType.NBTTagDouble)
        ))return null;

        String worldName=data.getString("world");
        if(worldName.isEmpty())return null;
        World world=Bukkit.getWorld(worldName);
        if(world==null)return null;

        double x=data.getDouble("x");
        double y=data.getDouble("y");
        double z=data.getDouble("z");

        float yaw=data.getFloat("yaw");
        float pitch=data.getFloat("pitch");

        return new Location(world,x,y,z,yaw,pitch);
    }

    public static int[] posToData(Location location){
        return new int[]{location.getBlockX(),location.getBlockY(),location.getBlockZ()};
    }

    public static Location dataToPos(int[] data,World world){
        if(data.length!=3)return null;
        return new Location(world,data[0],data[1],data[2]);
    }

    public static int[] vec3ToArray(Vec3 pos){
        return new int[]{pos.x(),pos.y(),pos.z()};
    }

    public static Vec3 arrayToVec3(int[] data){
        if(data.length!=3)return null;
        return new Vec3(data[0],data[1],data[2]);
    }

    public static double[] vec3dToArray(Vec3d pos){
        return new double[]{pos.x(),pos.y(),pos.z()};
    }

    public static Vec3d arrayToVec3d(double[] data){
        if(data.length!=3)return null;
        return new Vec3d(data[0],data[1],data[2]);
    }

    public static Vec3d readVec3d(ReadableNBTList<Double> list){
        if(list.size()!=3)return null;
        return new Vec3d(list.get(0),list.get(1),list.get(2));
    }

    public static void writeVec3d(Vec3d pos,ReadWriteNBTList<Double> list){
        list.add(0,pos.x());
        list.add(1,pos.y());
        list.add(2,pos.z());
    }

    public static void writeLocRotation(Location loc,ReadWriteNBTList<Float> list){
        list.add(0,loc.getYaw());
        list.add(1,loc.getPitch());
    }

    public static boolean readLocRotation(Location loc,ReadableNBTList<Float> list){
        if(list.size()!=2)return false;
        loc.setYaw(list.get(0));
        loc.setPitch(list.get(1));
        return true;
    }

    public static String toLowerString(String input){
        final StringBuilder build = new StringBuilder(input);
        for (int i = 0; i < build.length(); i++) {
            final int ch = build.codePointAt(i);
            if (ch == '-') {
                if (i != 0 && i != build.length() - 1) { // only convert actual separators
                    build.setCharAt(i,'_');
                }
            } else if (Character.isUpperCase(ch)) {
                build.insert(i++, '_');
                final int lower = Character.toLowerCase(ch);
                if (Character.isBmpCodePoint(lower)) {
                    build.setCharAt(i, (char) lower);
                } else {
                    build.setCharAt(i++, Character.highSurrogate(lower));
                    build.setCharAt(i, Character.lowSurrogate(lower));
                }
            }
        }
        return build.toString();
    }

    @SuppressWarnings("deprecation")
    public static String posLore(Location location, SideRotation rotation){
        return
            ChatColor.RED+"x"+ChatColor.DARK_GRAY+": "+ChatColor.YELLOW+location.getBlockX()+
            ChatColor.GREEN+" y"+ChatColor.DARK_GRAY+": "+ChatColor.YELLOW+location.getBlockY()+
            ChatColor.BLUE+" z"+ChatColor.DARK_GRAY+": "+ChatColor.YELLOW+location.getBlockZ()+
            ChatColor.DARK_PURPLE+" yaw"+ChatColor.DARK_GRAY+": "+ChatColor.YELLOW+rotation.degrees;
    }

    @SuppressWarnings("deprecation")
    public static String posLore(Location location){
        return
            ChatColor.RED+"x"+ChatColor.DARK_GRAY+": "+ChatColor.YELLOW+location.getBlockX()+
            ChatColor.GREEN+" y"+ChatColor.DARK_GRAY+": "+ChatColor.YELLOW+location.getBlockY()+
            ChatColor.BLUE+" z"+ChatColor.DARK_GRAY+": "+ChatColor.YELLOW+location.getBlockZ();
    }

    public static BoundingBox createBox(Vector block, Vector sizeVector, SideRotation rotation){
        Vector first=block.clone();
        switch(rotation){
            case R90 ->first.add(new Vector(1,0,0));
            case R180 ->first.add(new Vector(1,0,1));
            case R270 ->first.add(new Vector(0,0,1));
        }

        Vector second=first.clone().add(sizeVector);
        return BoundingBox.of(first,second);
    }

    private DBlUtil(){}
}
