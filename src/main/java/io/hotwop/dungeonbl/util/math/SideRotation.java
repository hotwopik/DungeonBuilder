package io.hotwop.dungeonbl.util.math;

import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.Nullable;

public enum SideRotation {
    R0(0,0, 0, 1),
    R90(90,Math.toRadians(270), -1, 0),
    R180(180,Math.toRadians(180), 0, -1),
    R270(270,Math.toRadians(90), 1, 0);

    public final int degrees;
    public final double radians;
    public final int sin;
    public final int cos;

    SideRotation(int degrees, double radians, int sin, int cos){
        this.degrees=degrees;
        this.radians=radians;
        this.sin = sin;
        this.cos = cos;
    }

    public SideRotation addOne(){
        int ord=ordinal();
        SideRotation[] val=values();

        if(ord==val.length-1)return R0;
        return val[ord+1];
    }

    public SideRotation subOne(){
        int ord=ordinal();
        SideRotation[] val=values();

        if(ord==0)return R270;
        return val[ord-1];
    }

    public SideRotation add(SideRotation rotation){
        SideRotation[] val=values();

        return val[(this.ordinal()+rotation.ordinal())%val.length];
    }

    public SideRotation sub(SideRotation rotation){
        SideRotation[] val=values();

        return val[(4+this.ordinal()-rotation.ordinal())%val.length];
    }

    public SideRotation yMirror(){
        return switch(this){
            case R0->R0;
            case R90->R270;
            case R180->R180;
            case R270->R90;
        };
    }

    public SideRotation xMirror(){
        return switch(this){
            case R0->R180;
            case R90->R90;
            case R180->R0;
            case R270->R270;
        };
    }

    public BlockFace addTo(BlockFace blockFace){
        return switch(this){
            case R0 ->blockFace;
            case R180 ->switch(blockFace){
                case UP,DOWN,SELF->blockFace;
                default->blockFace.getOppositeFace();
            };
            case R90 ->switch(blockFace){
                case UP,DOWN,SELF -> blockFace;
                case NORTH -> BlockFace.EAST;
                case NORTH_NORTH_EAST -> BlockFace.EAST_SOUTH_EAST;
                case NORTH_EAST -> BlockFace.SOUTH_EAST;
                case EAST_NORTH_EAST -> BlockFace.SOUTH_SOUTH_EAST;
                case EAST -> BlockFace.SOUTH;
                case EAST_SOUTH_EAST -> BlockFace.SOUTH_SOUTH_WEST;
                case SOUTH_EAST -> BlockFace.SOUTH_WEST;
                case SOUTH_SOUTH_EAST -> BlockFace.WEST_SOUTH_WEST;
                case SOUTH -> BlockFace.WEST;
                case SOUTH_SOUTH_WEST -> BlockFace.WEST_NORTH_WEST;
                case SOUTH_WEST -> BlockFace.NORTH_WEST;
                case WEST_SOUTH_WEST -> BlockFace.NORTH_NORTH_WEST;
                case WEST -> BlockFace.NORTH;
                case WEST_NORTH_WEST -> BlockFace.NORTH_NORTH_EAST;
                case NORTH_WEST -> BlockFace.NORTH_EAST;
                case NORTH_NORTH_WEST -> BlockFace.EAST_NORTH_EAST;
            };
            case R270 ->switch(blockFace){
                case UP,DOWN,SELF->blockFace;
                case NORTH -> BlockFace.WEST;
                case NORTH_NORTH_EAST -> BlockFace.WEST_NORTH_WEST;
                case NORTH_EAST -> BlockFace.NORTH_WEST;
                case EAST_NORTH_EAST -> BlockFace.NORTH_NORTH_WEST;
                case EAST -> BlockFace.NORTH;
                case EAST_SOUTH_EAST -> BlockFace.NORTH_NORTH_EAST;
                case SOUTH_EAST -> BlockFace.NORTH_EAST;
                case SOUTH_SOUTH_EAST -> BlockFace.EAST_NORTH_EAST;
                case SOUTH -> BlockFace.EAST;
                case SOUTH_SOUTH_WEST -> BlockFace.EAST_SOUTH_EAST;
                case SOUTH_WEST -> BlockFace.SOUTH_EAST;
                case WEST_SOUTH_WEST -> BlockFace.SOUTH_SOUTH_EAST;
                case WEST -> BlockFace.SOUTH;
                case WEST_NORTH_WEST -> BlockFace.SOUTH_SOUTH_WEST;
                case NORTH_WEST -> BlockFace.SOUTH_WEST;
                case NORTH_NORTH_WEST -> BlockFace.WEST_SOUTH_WEST;
            };
        };
    }

    public static SideRotation get(int index){
        SideRotation[] val=values();
        return val[index%val.length];
    }

    public static @Nullable SideRotation fromBlockFace(BlockFace face){
        return switch(face){
            case SOUTH-> SideRotation.R180;
            case EAST-> SideRotation.R270;
            case NORTH-> SideRotation.R0;
            case WEST-> SideRotation.R90;
            default->null;
        };
    }

    public BlockFace toBlockFace(){
        return switch(this){
            case R0 ->BlockFace.NORTH;
            case R90 ->BlockFace.EAST;
            case R180 ->BlockFace.SOUTH;
            case R270 ->BlockFace.WEST;
        };
    }
}
