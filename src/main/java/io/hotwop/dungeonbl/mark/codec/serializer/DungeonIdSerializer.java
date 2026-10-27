package io.hotwop.dungeonbl.mark.codec.serializer;

import dev.jorel.commandapi.arguments.Argument;
import io.hotwop.dungeonbl.DBlCommand;
import io.hotwop.dungeonbl.DungeonBl;

public final class DungeonIdSerializer extends StringSerializer{
    public static final DungeonIdSerializer instance=new DungeonIdSerializer();
    private DungeonIdSerializer(){}

    @Override
    public Argument<String> getArgument(String name) {
        return DBlCommand
            .stringListArgument(name,"Dungeon",()-> DungeonBl.registries().dungeons.idSet(),null);
    }
}
