package io.hotwop.dungeonbl.mark.codec.serializer;

import dev.jorel.commandapi.arguments.Argument;
import io.hotwop.dungeonbl.DBlCommand;
import io.hotwop.dungeonbl.DungeonBl;

public final class DoorwayIdSerializer extends StringSerializer{
    public static final DoorwayIdSerializer instance=new DoorwayIdSerializer();
    private DoorwayIdSerializer(){}

    @Override
    public Argument<String> getArgument(String name) {
        return DBlCommand
            .stringListArgument(name,"Doorway",()-> DungeonBl.registries().doorways.idSet(),null);
    }
}
