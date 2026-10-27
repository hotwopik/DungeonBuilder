package io.hotwop.dungeonbl;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import io.hotwop.dungeonbl.io.DBlRegistries;
import io.hotwop.dungeonbl.io.DBlRegistry;
import io.hotwop.dungeonbl.mark.MarkType;
import io.hotwop.dungeonbl.menu.dungeon.DungeonMenu;
import io.hotwop.dungeonbl.mob.Mob;
import io.hotwop.dungeonbl.session.DungeonSession;
import io.hotwop.dungeonbl.structure.Decoration;
import io.hotwop.dungeonbl.structure.Doorway;
import io.hotwop.dungeonbl.structure.RoomBuilding;
import io.hotwop.dungeonbl.structure.StructureScanException;
import io.hotwop.dungeonbl.structure.builder.OneTimeBuilder;
import io.hotwop.dungeonbl.util.math.SideRotation;
import io.hotwop.dungeonbl.util.math.Vec3;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("deprecation")
public final class DBlCommand{
    private static final String[] anyOfPermissions=new String[]{
        "dungeonbl.*",
        "dungeonbl.command.*",
        "dungeonbl.command.session",
        "dungeonbl.command.dungeon",
        "dungeonbl.command.build",
        "dungeonbl.command.items",
        "dungeonbl.command.mob",
        "dungeonbl.command.reload"
    };

    public static Argument<String> validIdArgument(String name){
        return new CustomArgument<>(
            new StringArgument(name),
            info->{
                String value=info.currentInput();
                if(!DBlRegistry.validId.test(value))throw CustomArgument.CustomArgumentException
                    .fromString("Invalid id, contains unallowed symbols: "+value);
                return value;
            }
        );
    }

    public static Argument<NamespacedKey> namespacedKeyListArgument(
        String name,
        Supplier<Collection<NamespacedKey>> suggestions,
        Predicate<NamespacedKey> checker
    ){
        return new CustomArgument<>(
            new NamespacedKeyArgument(name),
            info->{
                NamespacedKey value=info.currentInput();

                if(checker!=null&&!checker.test(value))throw CustomArgument.CustomArgumentException
                    .fromString("Unknown "+name+": "+value.toString());

                return value;
            }
        ).replaceSuggestions(ArgumentSuggestions.stringCollection(
            info->{
                String inp=info.currentArg();
                Collection<NamespacedKey> suggests=suggestions.get();

                if(inp.isEmpty())return suggests.stream()
                    .map(NamespacedKey::toString)
                    .toList();
                else return suggests.stream()
                    .filter(suggest->suggest.toString().startsWith(inp)||suggest.asMinimalString().startsWith(inp))
                    .map(NamespacedKey::toString)
                    .toList();
            }
        ));
    }

    public static Argument<String> stringListArgument(
        String name,
        String typeName,
        Supplier<Collection<String>> suggestions,
        Predicate<String> checker
    ){
        return new CustomArgument<>(
            new StringArgument(name),
            info->{
                String value=info.currentInput();

                if(value.isEmpty())throw CustomArgument.CustomArgumentException
                    .fromString(name+" is missing");
                if(checker!=null&&!checker.test(value))throw CustomArgument.CustomArgumentException
                    .fromString("Unknown "+typeName+": "+value);

                return value;
            }
        ).replaceSuggestions(ArgumentSuggestions.stringCollection(
            info->{
                String inp=info.currentArg();
                Collection<String> suggests=suggestions.get();

                if(inp.isEmpty())return suggests;
                return suggests.stream()
                    .filter(s->s.startsWith(inp))
                    .toList();
            }
        ));
    }

    public static Argument<SideRotation> rotationArgument(String name){
        return new CustomArgument<>(
            new IntegerArgument(name),
            info->switch(info.currentInput()){
                case 0-> SideRotation.R0;
                case 90-> SideRotation.R90;
                case 180-> SideRotation.R180;
                case 270-> SideRotation.R270;
                default->throw CustomArgument.CustomArgumentException.fromString("Rotation is incorrect: "+info.currentInput());
            }
        ).replaceSuggestions(ArgumentSuggestions.strings("0","90","180","270"));
    }

    public static final CommandAPICommand reloadCommand = new CommandAPICommand("reload")
        .withPermission("dungeonbl.command.reload")
        .withOptionalArguments(
            new MultiLiteralArgument("mode","dungeons","mobs","buildings","config")
        )
        .executesNative((sender,args)->{
            String mode=args.getByClass("mode",String.class);
            DBlRegistries registries=DungeonBl.registries();

            DungeonBl.executorService.execute(()->{
                switch(mode){
                    case "dungeons"->registries.dungeons.load();
                    case "mobs"->registries.mobs.load();
                    case "buildings"->{
                        registries.roomBuildings.load();
                        registries.decorations.load();
                        registries.doorways.load();
                    }
                    case "config"->DungeonBl.loadConfig();
                    case null, default->{
                        DungeonBl.loadConfig();
                        registries.loadAll();
                    }
                }
            });
            sender.sendMessage("Reloading...");
        });

    public static final CommandAPICommand buildingCommand=new CommandAPICommand("building")
        .withPermission("dungeonbl.command.build")
        .withSubcommands(
            RoomCommand.root,
            DoorwayCommand.root,
            DecorationCommand.root
        );

    public static final CommandAPICommand pluginCommandRoot=new CommandAPICommand("dungeonbl")
        .withRequirement(DBlCommand::hasAnyPermission)
        .withAliases("dbl")
        .withShortDescription("Main DungeonBuilder command")
        .withSubcommands(
            SessionCommand.root,
            DungeonCommand.root,
            buildingCommand,
            MobCommand.root,
            reloadCommand,
            MarkType.collectCommand()
        );

    public static final class SessionCommand{
        public static final CommandAPICommand run=new CommandAPICommand("run")
            .withArguments(
                stringListArgument(
                    "dungeon","Dungeon",
                    ()->DungeonBl.registries().dungeons.idSet(),
                    name->DungeonBl.registries().dungeons.get(name)!=null
                ),
                new LocationArgument("from",LocationType.BLOCK_POSITION),
                new LocationArgument("to",LocationType.BLOCK_POSITION),
                new EntitySelectorArgument.ManyPlayers("players",false)
            )
            .executesNative((sender,args)->{
                String dungeon=args.getByClass("dungeon",String.class);
                Location from=args.getByClass("from",Location.class);
                Location to=args.getByClass("to",Location.class);

                if(!DBlRegistry.validId.test(dungeon))throw CommandAPI.failWithString("Invalid id: "+dungeon);

                @SuppressWarnings("unchecked")
                Collection<Player> players=args.getByClass("players",Collection.class);

                DungeonSession.create(dungeon,from,to,players);
                sender.sendMessage("Starting session...");
            });

        public static final CommandAPICommand list=new CommandAPICommand("list")
            .executesConsole((sender,args)->{
                sender.sendMessage(sessionListMessageExpanded());
            })
            .executesPlayer((sender,args)->{
                sender.sendMessage(sessionListMessage());
            });

        public static final CommandAPICommand stop=new CommandAPICommand("stop")
            .withOptionalArguments(new IntegerArgument("id",0))
            .executesNative((sender,args)->{
                Integer id=args.getByClass("id",Integer.class);
                if(id==null){
                    if(sender.getCallee() instanceof Player pl){
                        if(DungeonSession.stopByPlayer(pl)){
                            sender.sendMessage("Successfully stopped");
                        }else{
                            throw CommandAPI.failWithString("Player not in session");
                        }
                    }else{
                        throw CommandAPI.failWithString("Only player can stop current session");
                    }
                }else{
                    if(DungeonSession.stop(id)){
                        sender.sendMessage("Successfully stopped");
                    }else{
                        throw CommandAPI.failWithString("Unknown session: "+id);
                    }
                }
            });


        public static final CommandAPICommand root=new CommandAPICommand("session")
            .withPermission("dungeonbl.command.session")
            .withSubcommands(run,list,stop);

        private SessionCommand(){}
    }

    public static final class DungeonCommand{
        public static final CommandAPICommand edit=new CommandAPICommand("edit")
            .withArguments(stringListArgument(
                "dungeon","Dungeon",
                ()->DungeonBl.registries().dungeons.idSet(),
                null
            ))
            .executesPlayer((sender,args)->{
                String name=args.getByClass("dungeon",String.class);

                if(!DBlRegistry.validId.test(name))throw CommandAPI.failWithString("Invalid id: "+name);
                new DungeonMenu(DungeonBl.registries().dungeons.get(name),name,sender);
            });

        public static final CommandAPICommand list=new CommandAPICommand("list")
            .executesConsole((sender,args)->{
                sender.sendMessage(dungeonListMessageExpanded());
            })
            .executesPlayer((sender,args)->{
                sender.sendMessage(dungeonListMessage());
            });

        public static final CommandAPICommand delete=new CommandAPICommand("delete")
            .withArguments(stringListArgument(
                "dungeon","Dungeon",
                ()->DungeonBl.registries().dungeons.idSet(),
                name->DungeonBl.registries().dungeons.get(name)!=null
            ))
            .executesNative((sender,args)->{
                String name=args.getByClass("dungeon",String.class);

                DungeonBl.registries().dungeons.remove(name);
                sender.sendMessage("Dungeon deleted");
            });

        public static final CommandAPICommand root=new CommandAPICommand("dungeon")
            .withPermission("dungeonbl.command.dungeon")
            .withSubcommands(edit,list,delete);

        private DungeonCommand(){}
    }

    public static final class RoomCommand{
        public static final CommandAPICommand save=new CommandAPICommand("save")
            .withArguments(
                stringListArgument("name","Room Building",()->DungeonBl.registries().roomBuildings.idSet(),null),
                new LocationArgument("from",LocationType.BLOCK_POSITION),
                new LocationArgument("to",LocationType.BLOCK_POSITION)
            )
            .executesNative((sender,args)->{
                String name=args.getByClass("name",String.class);
                Location from=args.getByClass("from",Location.class);
                Location to=args.getByClass("to",Location.class);

                if(!DBlRegistry.validId.test(name))throw CommandAPI.failWithString("Invalid id: "+name);

                RoomBuilding roomBuilding=RoomBuilding.scan(from.getWorld(),new Vec3(from),new Vec3(to));

                DungeonBl.registries().roomBuildings
                    .add(name,roomBuilding);

                sender.sendMessage("Room saved successfully");
            });

        public static final CommandAPICommand remove=new CommandAPICommand("remove")
            .withArguments(
                stringListArgument(
                    "name",
                    "Room Building",
                    ()->DungeonBl.registries().roomBuildings.idSet(),
                    name->DungeonBl.registries().roomBuildings.get(name)!=null
                )
            )
            .executesNative((sender,args)->{
                String name=args.getByClass("name",String.class);

                DungeonBl.registries().roomBuildings
                    .remove(name);

                sender.sendMessage("Room removed");
            });

        public static final CommandAPICommand place=new CommandAPICommand("place")
            .withArguments(
                new LocationArgument("pos",LocationType.BLOCK_POSITION),
                stringListArgument(
                    "name",
                    "Room Building",
                    ()->DungeonBl.registries().roomBuildings.idSet(),
                    name->DungeonBl.registries().roomBuildings.get(name)!=null
                ),
                rotationArgument("rotation")
            )
            .executesNative(((sender, args) -> {
                String name=args.getByClass("name",String.class);
                SideRotation rotation=args.getByClass("rotation",SideRotation.class);
                Location location=args.getByClass("pos",Location.class);

                DungeonBl.registries().roomBuildings.get(name)
                    .placeWithMarks(location,rotation,new OneTimeBuilder(location.getWorld()));
            }));

        public static final CommandAPICommand root=new CommandAPICommand("room")
            .withSubcommands(save,remove,place);

        private RoomCommand(){}
    }

    public static final class DoorwayCommand{
        public static final CommandAPICommand save=new CommandAPICommand("save")
            .withArguments(
                stringListArgument("name","Doorway",()->DungeonBl.registries().doorways.idSet(),null),
                new LocationArgument("from",LocationType.BLOCK_POSITION),
                new LocationArgument("to",LocationType.BLOCK_POSITION)
            )
            .executesNative((sender,args)->{
                String name=args.getByClass("name",String.class);
                Location from=args.getByClass("from",Location.class);
                Location to=args.getByClass("to",Location.class);

                if(!DBlRegistry.validId.test(name))throw CommandAPI.failWithString("Invalid id: "+name);

                Doorway doorway;
                try{
                    doorway=Doorway.scan(from.getWorld(),new Vec3(from),new Vec3(to));
                } catch (StructureScanException e) {
                    throw CommandAPI.failWithString("Doorway is invalid: "+e.getMessage());
                }

                DungeonBl.registries().doorways
                    .add(name,doorway);

                sender.sendMessage("Doorway saved successfully");
            });

        public static final CommandAPICommand remove=new CommandAPICommand("remove")
            .withArguments(
                stringListArgument(
                    "name",
                    "Doorway",
                    ()->DungeonBl.registries().doorways.idSet(),
                    name->DungeonBl.registries().doorways.get(name)!=null
                )
            )
            .executesNative((sender,args)->{
                String name=args.getByClass("name",String.class);

                DungeonBl.registries().doorways
                    .remove(name);

                sender.sendMessage("Doorway removed");
            });

        public static final CommandAPICommand place=new CommandAPICommand("place")
            .withArguments(
                new LocationArgument("pos",LocationType.BLOCK_POSITION),
                stringListArgument(
                    "name",
                    "Doorway",
                    ()->DungeonBl.registries().doorways.idSet(),
                    name->DungeonBl.registries().doorways.get(name)!=null
                ),
                rotationArgument("rotation")
            )
            .executesNative(((sender, args) -> {
                String name=args.getByClass("name",String.class);
                SideRotation rotation=args.getByClass("rotation",SideRotation.class);
                Location location=args.getByClass("pos",Location.class);

                DungeonBl.registries().doorways.get(name)
                    .place(new Vec3(location),rotation,new OneTimeBuilder(location.getWorld()));
            }));

        public static final CommandAPICommand root=new CommandAPICommand("doorway")
            .withSubcommands(save,remove,place);

        private DoorwayCommand(){}
    }

    public static final class DecorationCommand{
        public static final CommandAPICommand save=new CommandAPICommand("save")
            .withArguments(
                stringListArgument("name","Decoration",()->DungeonBl.registries().decorations.idSet(),null),
                new LocationArgument("from",LocationType.BLOCK_POSITION),
                new LocationArgument("to",LocationType.BLOCK_POSITION)
            )
            .executesNative((sender,args)->{
                String name=args.getByClass("name",String.class);
                Location from=args.getByClass("from",Location.class);
                Location to=args.getByClass("to",Location.class);

                if(!DBlRegistry.validId.test(name))throw CommandAPI.failWithString("Invalid id: "+name);

                Decoration decoration=Decoration.scan(from.getWorld(),new Vec3(from),new Vec3(to));

                DungeonBl.registries().decorations
                    .add(name,decoration);

                sender.sendMessage("Decoration saved successfully");
            });

        public static final CommandAPICommand remove=new CommandAPICommand("remove")
            .withArguments(
                stringListArgument(
                    "name",
                    "Decoration",
                    ()->DungeonBl.registries().decorations.idSet(),
                    name->DungeonBl.registries().decorations.get(name)!=null
                )
            )
            .executesNative((sender,args)->{
                String name=args.getByClass("name",String.class);

                DungeonBl.registries().decorations
                    .remove(name);

                sender.sendMessage("Decoration removed");
            });

        public static final CommandAPICommand place=new CommandAPICommand("place")
            .withArguments(
                new LocationArgument("pos",LocationType.BLOCK_POSITION),
                stringListArgument(
                    "name",
                    "Decoration",
                    ()->DungeonBl.registries().decorations.idSet(),
                    name->DungeonBl.registries().decorations.get(name)!=null
                ),
                rotationArgument("rotation")
            )
            .executesNative(((sender, args) -> {
                String name=args.getByClass("name",String.class);
                SideRotation rotation=args.getByClass("rotation",SideRotation.class);
                Location location=args.getByClass("pos",Location.class);

                DungeonBl.registries().decorations.get(name)
                    .place(new Vec3(location),rotation,new OneTimeBuilder(location.getWorld()));
            }));

        public static final CommandAPICommand root=new CommandAPICommand("decoration")
            .withSubcommands(save,remove,place);

        private DecorationCommand(){}
    }

    public static final class MobCommand{
        public static final CommandAPICommand add=new CommandAPICommand("add")
            .withArguments(
                stringListArgument("name","Mob",()->DungeonBl.registries().mobs.idSet(),null),
                new EntityTypeArgument("type")
            )
            .withOptionalArguments(
                new NBTCompoundArgument<>("nbt")
            )
            .executesNative((sender,args)->{
                String name=args.getByClass("name",String.class);
                EntityType entityType=args.getByClass("type",EntityType.class);
                NBTContainer nbt=args.getByClass("nbt",NBTContainer.class);

                if(!DBlRegistry.validId.test(name))throw CommandAPI.failWithString("Invalid id: "+name);

                if(nbt==null)nbt=(NBTContainer)NBT.createNBTObject();

                nbt.removeKey("Pos");
                nbt.setString("id",entityType.getKey().toString());

                EntitySnapshot entity;
                try{
                    entity=Bukkit.getEntityFactory()
                        .createEntitySnapshot(nbt.toString());
                } catch (IllegalArgumentException e) {
                    throw CommandAPI.failWithString("Error to deserialize mob entity: "+e);
                }

                DungeonBl.registries().mobs
                    .add(name,new Mob(entity));

                sender.sendMessage("Mob added successfully");
            });

        public static final CommandAPICommand remove=new CommandAPICommand("remove")
            .withArguments(stringListArgument(
                "name","Mob",()->DungeonBl.registries().mobs.idSet(),name->DungeonBl.registries().mobs.get(name)!=null
            ))
            .executesNative((sender,args)->{
                String name=args.getByClass("name",String.class);

                DungeonBl.registries().mobs
                    .remove(name);

                sender.sendMessage("Mob removed");
            });

        public static final CommandAPICommand root=new CommandAPICommand("mob")
            .withPermission("dungeonbl.command.mob")
            .withSubcommands(add,remove);

        private MobCommand(){}
    }

    public static boolean hasAnyPermission(CommandSender sender){
        for(String permission:anyOfPermissions){
            if(sender.hasPermission(permission))return true;
        }
        return false;
    }

    public static String dungeonListMessageExpanded(){
        Set<String> dungeons=DungeonBl.registries().dungeons.idSet();
        if(dungeons.isEmpty())return "No Dungeons";

        StringBuilder message=new StringBuilder("Dungeons:\n");
        for(String dungeon:dungeons){
            message.append(" - ").append(dungeon).append("\n");
        }

        return message.toString();
    }

    public static BaseComponent dungeonListMessage(){
        Set<String> dungeons=DungeonBl.registries().dungeons.idSet();
        if(dungeons.isEmpty())return new TextComponent("No Dungeons");

        ComponentBuilder builder=new ComponentBuilder("Dungeons:");
        for(String dungeon:dungeons){
            builder.append("\n - ");

            var actionText=new TextComponent("*Press LMB to edit");

            var dungeonName=new TextComponent(dungeon);
            dungeonName.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new BaseComponent[]{actionText}));
            dungeonName.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/dungeonbl dungeon edit "+dungeonName));

            builder.append(dungeonName);
        }

        return builder.build();
    }

    public static String sessionListMessageExpanded(){
        List<DungeonSession> sessions=DungeonSession.all();
        if(sessions.isEmpty())return "No active Sessions";

        StringBuilder message=new StringBuilder("Active Sessions:\n");
        for(DungeonSession session:sessions){
            message
                .append(" - ")
                .append(ChatColor.GOLD).append(session.dungeonName)
                .append(" ")
                .append(ChatColor.BLUE).append(session.id)
                .append("\n");

            message
                .append("    ")
                .append(ChatColor.GRAY).append("Player count: ")
                .append(ChatColor.YELLOW).append(session.playerCount())
                .append("\n");

            message
                .append("    ")
                .append(ChatColor.GRAY).append("Players: ")
                .append("\n");

            int playersSuggested=0;
            for(Player player: session.players()){
                playersSuggested++;
                message
                    .append(ChatColor.WHITE).append("     - ")
                    .append(ChatColor.AQUA).append(player.getName())
                    .append("\n");
                if(playersSuggested==3){
                    message.append(ChatColor.WHITE).append("     ...\n");
                    break;
                }
            }
            if(playersSuggested==0){
                message.append(ChatColor.WHITE).append("     No Players\n");
            }

            message
                .append("    ")
                .append(ChatColor.GRAY).append("World: ")
                .append(ChatColor.YELLOW).append(session.world.getName())
                .append("\n");

            message
                .append("    ")
                .append(ChatColor.GRAY).append("Enter location: ")
                .append(DBlUtil.posLore(session.from))
                .append("\n");

            message
                .append("    ")
                .append(ChatColor.GRAY).append("Exit location: ")
                .append(DBlUtil.posLore(session.to))
                .append(ChatColor.RESET).append("\n");
        }
        return message.toString();
    }

    public static BaseComponent sessionListMessage(){
        List<DungeonSession> sessions=DungeonSession.all();
        if(sessions.isEmpty())return new TextComponent("No active Sessions");

        ComponentBuilder builder=new ComponentBuilder("Active Sessions:\n");
        for(DungeonSession session:sessions){
            var begin=new TextComponent(" - ");
            begin.setColor(net.md_5.bungee.api.ChatColor.WHITE);
            builder.append(begin);

            var playerCountName=new TextComponent("Player Count: ");
            playerCountName.setColor(net.md_5.bungee.api.ChatColor.GRAY);

            var playerCount=new TextComponent(session.playerCount()+"\n");
            playerCount.setColor(net.md_5.bungee.api.ChatColor.YELLOW);

            var playerListTitle=new TextComponent("Players: ");
            playerListTitle.setColor(net.md_5.bungee.api.ChatColor.GRAY);

            var playerList=new ComponentBuilder(playerListTitle);
            int playersSuggested=0;
            for(Player player:session.players()){
                playersSuggested++;

                var playerBegin=new TextComponent("\n - ");
                playerBegin.setColor(net.md_5.bungee.api.ChatColor.WHITE);

                playerList.append(playerBegin);

                var playerName=new TextComponent(player.getName());
                playerName.setColor(net.md_5.bungee.api.ChatColor.AQUA);

                playerList.append(playerName);

                if(playersSuggested==3){
                    var playerEnd=new TextComponent("\n ...\n");
                    playerEnd.setColor(net.md_5.bungee.api.ChatColor.WHITE);

                    playerList.append(playerEnd);

                    break;
                }
            }
            if(playersSuggested==0){
                var noPlayers=new TextComponent("\n No Players\n");
                noPlayers.setColor(net.md_5.bungee.api.ChatColor.WHITE);

                playerList.append(noPlayers);
            }else if(playersSuggested!=3){
                playerList.append("\n");
            }

            var worldName=new TextComponent("World: ");
            worldName.setColor(net.md_5.bungee.api.ChatColor.GRAY);

            var world=new TextComponent(session.world.getName()+"\n");
            world.setColor(net.md_5.bungee.api.ChatColor.YELLOW);

            var enterLocationName=new TextComponent("Enter Location: ");
            enterLocationName.setColor(net.md_5.bungee.api.ChatColor.GRAY);

            var enterLocation=TextComponent.fromLegacy(DBlUtil.posLore(session.from));

            var exitLocationName=new TextComponent("\nExit Location: ");
            exitLocationName.setColor(net.md_5.bungee.api.ChatColor.GRAY);

            var exitLocation=TextComponent.fromLegacy(DBlUtil.posLore(session.to));

            var nameAction=new TextComponent("*Press LMB to teleport in session world");
            nameAction.setColor(net.md_5.bungee.api.ChatColor.WHITE);

            var name=new TextComponent(session.dungeonName+" ");
            name.setColor(net.md_5.bungee.api.ChatColor.GOLD);
            name.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new BaseComponent[]{
                playerCountName,playerCount,
                playerList.build(),
                worldName,world,
                enterLocationName,enterLocation,
                exitLocationName,exitLocation,
                nameAction
            }));
            name.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/execute in "+session.world.getKey()+" run tp @s 0 0 0"));

            builder.append(name);

            var idAction=new TextComponent("*Press LMB to stop session");
            nameAction.setColor(net.md_5.bungee.api.ChatColor.WHITE);

            var id=new TextComponent(session.id+"\n");
            id.setColor(net.md_5.bungee.api.ChatColor.BLUE);
            id.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new BaseComponent[]{idAction}));
            id.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/dungeonbl session stop "+session.id));

            builder.append(id);
        }

        return builder.build();
    }

    private DBlCommand(){}
}
