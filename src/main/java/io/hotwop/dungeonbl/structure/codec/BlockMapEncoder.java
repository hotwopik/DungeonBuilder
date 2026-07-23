package io.hotwop.dungeonbl.structure.codec;

import io.hotwop.dungeonbl.structure.container.BlockDat;
import io.hotwop.dungeonbl.util.math.Vec3;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.bukkit.block.data.BlockData;

import java.util.*;

import static io.hotwop.dungeonbl.DungeonBl.logger;

public final class BlockMapEncoder{
    /*
        Encoding blocks in order y z x
        >=0 - Block index from palette

        -1 - Skip y level
        -2 - Skip z line
        -3 - Skip x block

        yShiftStart>i>=zShiftStart - Repeat y level -i-yShiftStart times
        zShiftStart>i>=xShiftStart - Repeat z line -i-zShiftStart times
        xShiftStart>i>=shiftEnd - Repeat x block -i-xShiftStart times
    */

    public final int width;
    public final int height;
    public final int length;

    private final int yShiftStart;
    private final int zShiftStart;
    private final int xShiftStart;

    private final List<BlockDat> palette;
    private final Map<Vec3, BlockDat> blockMap;

    private final Integer[][][] blockList;

    public BlockMapEncoder(int width, int height, int length, List<BlockDat> palette, Map<Vec3, BlockDat> blockMap){
        this.width=width;
        this.height=height;
        this.length=length;

        this.palette = palette;
        this.blockMap=blockMap;

        yShiftStart=-3;
        zShiftStart=yShiftStart-height-1;
        xShiftStart=zShiftStart-length-1;

        blockList=new Integer[height][length][width];
    }

    public int[] run(){
        blockMap.forEach((vec,data)->{
            blockList[vec.y()][vec.z()][vec.x()]=palette.indexOf(data);
        });

        return runY().toIntArray();
    }

    private IntList runY(){
        IntList out=new IntArrayList();

        int y=0;
        while(y<height){
            if(y!=height-1){
                int shift=0;
                for(int jY=y+1;jY<height;jY++){
                    if(Arrays.deepEquals(blockList[y],blockList[jY]))shift++;
                    else break;
                }

                if(shift>0){
                    out.add(-shift+yShiftStart);
                    y+=shift-1;
                }
            }

            IntList layer=runZ(y);
            if(layer.intStream().allMatch(val->val<0))out.add(-1);
            else out.addAll(layer);

            y++;
        }

        return out;
    }

    private IntList runZ(int y){
        IntList out=new IntArrayList();
        Integer[][] data=blockList[y];

        int z=0;
        while(z<length){
            if(z!=length-1){
                int shift=0;
                for(int jZ=z+1;jZ<length;jZ++){
                    if(Arrays.deepEquals(data[z],data[jZ]))shift++;
                    else break;
                }

                if(shift>0){
                    out.add(-shift+zShiftStart);
                    z+=shift-1;
                }
            }

            IntList layer=runX(y,z);
            if(layer.intStream().allMatch(val->val<0))out.add(-2);
            else out.addAll(layer);

            z++;
        }

        return out;
    }

    private IntList runX(int y,int z){
        IntList out=new IntArrayList();
        Integer[] data=blockList[y][z];

        int x=0;
        while(x<width){
            if(x!=width-1){
                int shift=0;
                for(int jX=x+1;jX<width;jX++){
                    if(Objects.equals(data[x],data[jX]))shift++;
                    else break;
                }

                if(shift>0){
                    out.add(-shift+xShiftStart);
                    x+=shift-1;
                }
            }

            Integer value=data[x];
            if(value==null)out.add(-3);
            else out.add(value.intValue());

            x++;
        }

        return out;
    }
}
