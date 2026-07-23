package io.hotwop.dungeonbl.structure.codec;

import io.hotwop.dungeonbl.structure.StructureSerializeException;
import io.hotwop.dungeonbl.structure.container.BlockDat;
import io.hotwop.dungeonbl.util.math.Vec3;
import org.bukkit.block.data.BlockData;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static io.hotwop.dungeonbl.DungeonBl.logger;

public final class BlockMapDecoder{
    // Read encoding in BlockMapEncoder

    private int index=-1;

    public final int width;
    public final int height;
    public final int length;

    private final int yShiftStart;
    private final int zShiftStart;
    private final int xShiftStart;
    private final int shiftEnd;

    private final List<BlockDat> pallet;
    private final int[] blocks;

    public BlockMapDecoder(int width, int height, int length, List<BlockDat> pallet, int[] blocks){
        this.width=width;
        this.height=height;
        this.length=length;
        this.pallet=pallet;
        this.blocks=blocks;

        yShiftStart=-3;
        zShiftStart=yShiftStart-height-1;
        xShiftStart=zShiftStart-length-1;
        shiftEnd=xShiftStart-width-1;
    }

    public Map<Vec3,BlockDat> run() throws StructureSerializeException {
        BlockDat[][][] list=runY();

        Map<Vec3,BlockDat> out=new ConcurrentHashMap<>();

        for(int y=0;y<height;y++){
            BlockDat[][] yLevel=list[y];
            if(yLevel!=null)for(int z=0;z<length;z++){
                BlockDat[] zLevel=yLevel[z];
                if(zLevel!=null)for(int x=0;x<width;x++){
                    BlockDat block=zLevel[x];
                    if(block!=null)out.put(new Vec3(x,y,z),block);
                }
            }
        }

        return out;
    }

    private void next() throws StructureSerializeException {
        index++;
        if(index>=blocks.length)throw new StructureSerializeException("Data corrupted out of range, index: "+index);
    }

    private BlockDat[][][] runY() throws StructureSerializeException {
        BlockDat[][][] list=new BlockDat[height][length][width];

        int y=0;
        while(y<height){
            next();
            int val=blocks[index];

            BlockDat[][] out;
            if(val==-1){
                out=null;
            }else if(val<yShiftStart&&val>=zShiftStart){
                next();
                int val2=blocks[index];

                if(val2==-1){
                    out=null;
                }else{
                    index--;
                    out=runZ();
                }

                int toY=y-val+yShiftStart;
                if(toY>=height)throw new StructureSerializeException("Data corrupted unallowed code, index: "+index);
                for(int jY=y;jY<=toY;jY++)list[jY]=out;
                y=toY;

                continue;
            }else{
                index--;
                out=runZ();
            }

            list[y]=out;
            y++;
        }

        return list;
    }

    private BlockDat[][] runZ() throws StructureSerializeException {
        BlockDat[][] list=new BlockDat[length][width];

        int z=0;
        while(z<length){
            next();
            int val=blocks[index];

            BlockDat[] out;
            if(val==-2){
                out=null;
            }else if(val<zShiftStart&&val>=xShiftStart){
                next();
                int val2=blocks[index];

                if(val2==-2){
                    out=null;
                }else{
                    index--;
                    out=runX();
                }

                int toZ=z-val+zShiftStart;
                if(toZ>=length)throw new StructureSerializeException("Data corrupted unallowed code, index: "+index);
                for(int jZ=z;jZ<=toZ;jZ++)list[jZ]=out;
                z=toZ;

                continue;
            }else{
                index--;
                out=runX();
            }

            list[z]=out;
            z++;
        }

        return list;
    }

    private BlockDat[] runX() throws StructureSerializeException {
        BlockDat[] list=new BlockDat[width];

        int x=0;
        while(x<width){
            next();
            int val=blocks[index];

            BlockDat out;
            if(val==-3){
                out=null;
            }else if(val<xShiftStart&&val>=shiftEnd){
                next();
                int val2=blocks[index];

                if(val2==-3){
                    out=null;
                }else if(val2>=0&&val2<pallet.size()){
                    out=pallet.get(val2);
                }else throw new StructureSerializeException("Data corrupted unallowed code, index: "+index);

                int toX=x-val+xShiftStart;
                if(toX>=width)throw new StructureSerializeException("Data corrupted unallowed code, index: "+index);
                for(int jX=x;jX<=toX;jX++)list[jX]=out;
                x=toX;

                continue;
            }else if(val>=0&&val<pallet.size()){
                out=pallet.get(val);
            }else throw new StructureSerializeException("Data corrupted unallowed code, index: "+index);

            list[x]=out;
            x++;
        }

        return list;
    }
}
