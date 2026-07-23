package io.hotwop.dungeonbl.structure.container;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import io.hotwop.dungeonbl.util.math.Vec3;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;

public record BlockDat(
    BlockData data,
    @Nullable ReadableNBT nbt
){
    public static final BlockDat airData=new BlockDat(Material.AIR.createBlockData(),null);

    public BlockDat{
        if(data.createBlockState() instanceof TileState){
            if(nbt==null)throw new RuntimeException("Block tile not providing nbt");
        }else{
            if(nbt!=null)throw new RuntimeException("Not tile block providing nbt");
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
}