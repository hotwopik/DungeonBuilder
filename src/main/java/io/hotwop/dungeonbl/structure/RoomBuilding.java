package io.hotwop.dungeonbl.structure;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.handler.NBTHandlers;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBTCompoundList;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBTList;
import io.hotwop.dungeonbl.DBlUtil;
import io.hotwop.dungeonbl.mark.Mark;
import io.hotwop.dungeonbl.mark.MarkStorage;
import io.hotwop.dungeonbl.mark.MarkType;
import io.hotwop.dungeonbl.mark.codec.MarkCodec;
import io.hotwop.dungeonbl.mark.dungeon.DungeonMark;
import io.hotwop.dungeonbl.mark.dungeon.DungeonMarkData;
import io.hotwop.dungeonbl.mark.place.MarkPlaceData;
import io.hotwop.dungeonbl.structure.builder.WorldBuilder;
import io.hotwop.dungeonbl.structure.container.StructureContainer;
import io.hotwop.dungeonbl.util.math.SideRotation;
import io.hotwop.dungeonbl.util.math.Vec3;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class RoomBuilding {
    public final StructureContainer container;
    public final Map<Vec3, MarkSavedData> marks;

    public final Map<DungeonMarkData<?,?>,Map<Vec3,MarkSavedData>> markTypesCache;

    public record MarkSavedData(
        MarkCodec<?> codec,
        ReadableNBT nbt,
        MarkPlaceData placeData
    ){}

    public RoomBuilding(StructureContainer container, Map<Vec3, MarkSavedData> marks) {
        this.container = container;
        this.marks = Map.copyOf(marks);

        Vec3 size = container.size();
        for (Map.Entry<Vec3, MarkSavedData> entry : marks.entrySet()) {
            Vec3 pos = entry.getKey();
            if (
                pos.x() < 0 || pos.y() < 0 || pos.z() < 0 ||
                    pos.x() >= size.x() || pos.y() >= size.y() || pos.z() >= size.z()
            ) throw new IllegalArgumentException("Mark is out of bounds");
            if (entry.getValue().codec.dungeon == null) throw new IllegalArgumentException("Mark type is invalid");
        }

        Map<DungeonMarkData<?,?>,Map<Vec3,MarkSavedData>> markTypes=new HashMap<>();
        marks.forEach((pos,mark)->{
            markTypes
                .computeIfAbsent(mark.codec.dungeon,k->new HashMap<>())
                .put(pos,mark);
        });

        Map<DungeonMarkData<?,?>,Map<Vec3,MarkSavedData>> markTypesCooked=new HashMap<>();
        markTypes.forEach((type,map)->markTypesCooked
            .put(type,Map.copyOf(map))
        );

        this.markTypesCache=Map.copyOf(markTypesCooked);
    }

    public Vec3 size() {
        return container.size();
    }

    public void place(Vec3 pos, SideRotation rotation, WorldBuilder builder) {
        container.place(pos, new Vec3(0,0,0), rotation, builder);
    }

    public static RoomBuilding scan(World world, Vec3 begin, Vec3 end) {
        Map<Vec3, MarkSavedData> marks = new HashMap<>();
        StructureContainer container = StructureContainer.scan(
            world, begin, end,
            (global,local) -> {
                Mark<?> mark = MarkStorage.getMark(global.toLocation(world));
                if (mark instanceof DungeonMark<?, ?>) {
                    MarkCodec<?> codec = mark.markType();
                    if (codec.dungeon != null) {
                        ReadableNBT nbt = getMarkData(mark);
                        MarkPlaceData data = codec.dungeon
                            .rebuildData(new MarkCodec.Info(nbt, codec));

                        marks.put(
                            local,
                            new MarkSavedData(codec, nbt, data)
                        );
                    }
                }
            },
            (entity,local)->true
        );

        return new RoomBuilding(container, marks);
    }

    public ReadWriteNBT encode() throws StructureSerializeException {
        ReadWriteNBT out = NBT.createNBTObject();
        container.encode(out, true);

        ReadWriteNBTCompoundList marksOut = out.getCompoundList("marks");
        marks.forEach((pos, mark) -> {
            ReadWriteNBT markOut = marksOut.addCompound();

            markOut.setIntArray("pos", DBlUtil.vec3ToArray(pos));
            markOut.setString("type", mark.codec.name);
            markOut.set("data", mark.nbt, NBTHandlers.STORE_READABLE_TAG);
        });

        return out;
    }

    public static RoomBuilding decode(ReadableNBT nbt) throws StructureSerializeException {
        StructureContainer container = StructureContainer.decode(nbt, true);
        Vec3 size = container.size();

        ReadableNBTList<ReadWriteNBT> markList = nbt.getCompoundList("marks");
        Map<Vec3, MarkSavedData> marks = new HashMap<>();
        if (markList != null && !markList.isEmpty()) {
            int index = 0;
            for (ReadWriteNBT markEntry : markList) {
                int[] posArr = markEntry.getIntArray("pos");
                if (posArr == null) throw new StructureSerializeException("Mark " + index + " \"pos\" is missing");

                Vec3 pos = DBlUtil.arrayToVec3(posArr);
                if (pos == null) throw new StructureSerializeException("Mark " + index + " \"pos\" is invalid");

                if (
                    pos.x() < 0 || pos.y() < 0 || pos.z() < 0 ||
                        pos.x() >= size.x() || pos.y() >= size.y() || pos.z() >= size.z()
                ) throw new StructureSerializeException("Mark " + index + " \"pos\" is out of bounds");

                String type = markEntry.getString("type");
                if (type == null || type.isEmpty())
                    throw new StructureSerializeException("Mark " + index + " \"type\" is missing");

                MarkType markType = MarkType.getType(type);
                if (markType == null)
                    throw new StructureSerializeException("Mark " + index + " type is unknown: " + type);
                if (!(markType instanceof MarkCodec<?> markCodec) || markCodec.dungeon == null) throw
                    new StructureSerializeException("Mark " + index + " type is unsupported for room building: " + type);

                ReadableNBT data = markEntry.getCompound("data");

                MarkPlaceData placeData;
                try {
                    placeData = markCodec.dungeon
                        .rebuildData(new MarkCodec.Info(data, markCodec));
                } catch (RuntimeException e) {
                    throw new StructureSerializeException("Mark " + index + " \"data\" is invalid", e);
                }

                marks.put(pos, new MarkSavedData(markCodec, data, placeData));

                index++;
            }
        }

        return new RoomBuilding(container, marks);
    }

    private static <T extends Mark<T>> ReadableNBT getMarkData(Mark<T> mark) {
        MarkCodec<T> codec = mark.markType();
        ReadWriteNBT data = NBT.createNBTObject();
        codec.writeData(mark, data);
        return data;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RoomBuilding) obj;
        return Objects.equals(this.container, that.container) &&
            Objects.equals(this.marks, that.marks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(container, marks);
    }

    @Override
    public String toString() {
        return "RoomBuilding[" +
            "container=" + container + ", " +
            "marks=" + marks + ']';
    }
}
