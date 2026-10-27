package io.hotwop.dungeonbl.structure.container;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTType;
import de.tr7zw.changeme.nbtapi.handler.NBTHandlers;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import io.hotwop.dungeonbl.structure.StructureSerializeException;
import io.hotwop.dungeonbl.util.math.SideRotation;
import io.hotwop.dungeonbl.util.math.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Wall;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public record BlockDat(
    BlockData data,
    @Nullable ReadableNBT nbt
){
    public static final BlockDat airData=new BlockDat(Material.AIR.createBlockData(),null);

    public BlockDat{
        if(data.createBlockState() instanceof TileState){
            if(nbt==null)throw new IllegalArgumentException("Block tile not providing nbt");
        }else{
            if(nbt!=null)throw new IllegalArgumentException("Not tile block providing nbt");
        }
    }

    public void place(World world,Vec3 pos){
        BlockData out=data.clone();

        world.getBlockAt(pos.x(),pos.y(),pos.z()).setBlockData(out,false);
        if(nbt!=null){
            NBT.modify(world.getBlockState(pos.x(),pos.y(),pos.z()),block->{
                block.mergeCompound(nbt);
            });
        }
    }

    public BlockDat replaceData(BlockData data){
        return new BlockDat(data,nbt);
    }

    public BlockDat rotate(SideRotation rotation) {
        return switch (data) {
            case org.bukkit.block.data.Directional dir -> {
                dir = (org.bukkit.block.data.Directional) dir.clone();
                dir.setFacing(rotation.addTo(dir.getFacing()));
                yield replaceData(dir);
            }
            case MultipleFacing multiFacing -> {
                multiFacing = (MultipleFacing) multiFacing.clone();
                Set<BlockFace> faces = Set.copyOf(multiFacing.getFaces());

                for (BlockFace face : faces) {
                    multiFacing.setFace(face, false);
                }

                for (BlockFace face : faces) {
                    multiFacing.setFace(rotation.addTo(face), true);
                }

                yield replaceData(multiFacing);
            }
            case Rotatable rotatable -> {
                rotatable = (Rotatable) rotatable.clone();
                rotatable.setRotation(rotation.addTo(rotatable.getRotation()));
                yield replaceData(rotatable);
            }
            case Wall wall -> {
                wall = (Wall) wall.clone();
                Wall.Height[] heights = {
                    wall.getHeight(BlockFace.NORTH),
                    wall.getHeight(BlockFace.WEST),
                    wall.getHeight(BlockFace.SOUTH),
                    wall.getHeight(BlockFace.EAST),
                };

                int shift = rotation.ordinal();
                wall.setHeight(BlockFace.NORTH, heights[shift]);
                wall.setHeight(BlockFace.WEST, heights[(1 + shift) % 4]);
                wall.setHeight(BlockFace.SOUTH, heights[(2 + shift) % 4]);
                wall.setHeight(BlockFace.EAST, heights[(3 + shift) % 4]);

                yield replaceData(wall);
            }
            default->this;
        };
    }

    public static @Nullable BlockDat read(World world, Vec3 pos){
        BlockState state=world.getBlockState(pos.x(),pos.y(),pos.z());
        BlockData data=state.getBlockData();

        if(data.getMaterial().isEmpty())return null;

        ReadWriteNBT nbt;
        if(state instanceof TileState){
            nbt=NBT.createNBTObject();
            NBT.get(state,nbt::mergeCompound);
        }else nbt=null;

        return new BlockDat(data.clone(),nbt);
    }

    public void encode(ReadWriteNBT out){
        out.setString("state",data.getAsString(true));
        if(nbt!=null)out.set("nbt",nbt,NBTHandlers.STORE_READABLE_TAG);
    }

    public static BlockDat decode(ReadableNBT nbt) throws StructureSerializeException{
        String paletteStr=nbt.getString("state");
        if(paletteStr==null||paletteStr.isEmpty())throw
            new StructureSerializeException("\"state\" is missing");

        BlockData data;
        try {
            data = Bukkit.createBlockData(paletteStr);
        } catch (IllegalArgumentException e) {
            throw new StructureSerializeException("\"state\" is invalid", e);
        }

        ReadableNBT nbtOut;
        if(nbt.hasTag("nbt", NBTType.NBTTagCompound)){
            nbtOut=nbt.getCompound("nbt");
        }else nbtOut=null;

        try{
            return new BlockDat(data,nbtOut);
        }catch(IllegalArgumentException e){
            throw new StructureSerializeException("Block data is invalid",e);
        }
    }
}