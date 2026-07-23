package io.hotwop.dungeonbl.session;

import io.hotwop.dungeonbl.DBlVersioned;
import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.config.Dungeon;
import io.hotwop.dungeonbl.integrations.VaultIntegration;
import io.hotwop.dungeonbl.io.PlayerData;
import io.hotwop.dungeonbl.io.PlayerDataStorage;
import io.hotwop.dungeonbl.structure.builder.AsyncWorldBuilder;
import io.hotwop.dungeonbl.util.RandomSource;
import io.hotwop.dungeonbl.util.math.*;
import io.hotwop.dungeonbl.util.math.SideRotation;
import io.hotwop.dungeonbl.util.task.AfterCounterTask;
import io.hotwop.dungeonbl.util.task.DoneTaskResult;
import io.hotwop.dungeonbl.versioned.TemporalWorld;
import io.hotwop.dungeonbl.versioned.WorldException;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

import static io.hotwop.dungeonbl.DungeonBl.logger;

public final class DungeonSession{
    private static final List<DungeonSession> all=new ArrayList<>();

    public static List<DungeonSession> all(){
        return List.copyOf(all);
    }

    public static @Nullable DungeonSession getById(int id){
        return all.stream().filter(ses->ses.id==id).findFirst().orElse(null);
    }

    public static boolean stop(int id){
        DungeonSession session=getById(id);
        if(session==null)return false;

        session.syncDestroyTask
            .runTask(DungeonBl.plugin());
        return true;
    }

    public static boolean stopByPlayer(Player player){
        Optional<DungeonSession> session=all.stream()
            .filter(ses->ses.players.contains(player))
            .findAny();

        if(session.isEmpty())return false;

        session.get().syncDestroyTask
            .runTask(DungeonBl.plugin());
        return true;
    }

    private static boolean shutdown=false;

    public static void shutdown(){
        if(shutdown)return;
        shutdown=true;

        all.forEach(DungeonSession::destroy);
        all.clear();
    }

    public static int sessionsCount(){
        return all.size();
    }

    private static final Path dungeonsFolder= Bukkit.getWorldContainer().toPath().resolve("dungeons");

    public static final NamespacedKey dungeonWorldField=new NamespacedKey(DungeonBl.namespace,"dungeon_world");
    public static final NamespacedKey rewardStorageFiled=new NamespacedKey(DungeonBl.namespace,"reward");

    private static int worldId=0;

    public static void create(String dungeon, Location from, Location to, Collection<Player> players){
        if(shutdown)return;

        Dungeon dung=DungeonBl.registries().dungeons
            .get(dungeon);

        if(dung==null){
            logger.warn("Error to start session, dungeon does not exist: {}", dungeon);
            return;
        }

        String folderName=String.valueOf(worldId);
        String worldName="dungeon_"+worldId;
        int id=worldId;
        worldId++;

        List<Player> playerList=new ArrayList<>(players);
        playerList.removeIf(player->!player.isOnline()||!player.hasPermission("dungeonbl.enter_dungeon"));
        if(playerList.isEmpty()){
            logger.warn("Error to start dungeon session {}, session has no players (players may have no permission)",dungeon);
            return;
        }

        PlayerDataStorage playerDataStorage=DungeonBl.playerDataStorage();
        playerList.forEach(player->{
            playerDataStorage.beginDungeon(player,dung.disableStarvation());
            playerDataStorage.beginTransfer(player);
        });

        DungeonBl.executorService.execute(()->{
            Builder builder=generateDungeon(dungeon,dung);
            if(builder==null){
                playerList.forEach(player->{
                    playerDataStorage.endTransfer(player);
                    playerDataStorage.endDungeon(player,null,()->{});
                });
                return;
            }

            TemporalWorld world;
            try{
                world=DungeonBl.versioned()
                    .createTemporalWorld(dungeonsFolder,folderName,DungeonBl.namespace+"_"+worldName,new NamespacedKey(DungeonBl.namespace,worldName));
            } catch (WorldException e) {
                logger.error("Error to create dungeon {} world",dungeon,e);
                playerList.forEach(player->{
                    playerDataStorage.endTransfer(player);
                    playerDataStorage.endDungeon(player,null,()->{});
                });
                return;
            }

            new DungeonSession(builder,world,new ImmutableLocation(from),new ImmutableLocation(to),new HashSet<>(playerList),id);
        });
    }

    private static Builder generateDungeon(String dungeonName,Dungeon dungeon){
        Builder builder=new Builder(dungeonName,dungeon);
        DoneTaskResult<RoomSession> task=new DoneTaskResult<>();

        RoomSession.Builder startRoomBuilder=RoomSession.builder(
            builder,
            null,
            dungeon.startRoom(),
            RoomType.START,

            task,
            new Vec3(0,0,0),
            SideRotation.R0
        );
        if(startRoomBuilder==null)return null;
        builder.current=startRoomBuilder;

        try{
            builder.build();
        }catch(DungeonBuildException e){
            logger.warn("Dungeon {} building is impossible, session canceled",dungeonName);
            return null;
        }catch(RuntimeException e){
            logger.warn("Error to build dungeon {}",dungeonName,e);
            return null;
        }

        return builder;
    }

    public static final class Builder{
        public final String dungeonName;
        public final Dungeon dungeon;
        public final RandomSource randomSource=new RandomSource();

        private RoomSession.Builder current;
        private final List<RoomSession> rooms=new ArrayList<>();
        private final Map<Vec2,List<Box3>> roomBounds=new HashMap<>();

        private Builder(String dungeonName, Dungeon dungeon){
            this.dungeonName=dungeonName;
            this.dungeon = dungeon;
        }

        private void build() throws DungeonBuildException{
            while(true){
                current.check();
                if(current.result()==null){
                    current.build();

                    RoomSession result=current.result();
                    if(result==null){
                        current=current.parent;
                        if(current==null)throw new DungeonBuildException("Dead end");
                        continue;
                    }

                    rooms.add(result);
                    result.chunks.forEach(chunk->roomBounds
                        .computeIfAbsent(chunk,c->new ArrayList<>())
                        .add(result.box)
                    );
                }

                if(current.type==RoomType.END)return;

                RoomSession.Builder next=null;
                for(RoomSession.Builder child:current.children()){
                    if(!child.isChecked()){
                        next=child;
                        break;
                    }
                }

                if(next==null){
                    current=current.parent;
                    if(current==null)throw new DungeonBuildException("Dead end");

                    RoomSession room=rooms.removeLast();
                    room.chunks.forEach(chunk->roomBounds
                        .get(chunk)
                        .remove(room.box)
                    );
                }else current=next;
            }
        }

        public boolean checkCollisions(Box3 box){
            Set<Vec2> chunks=box.occupiedChunks();
            for(Vec2 chunk:chunks){
                List<Box3> bounds=roomBounds.get(chunk);
                if(bounds!=null){
                    for(Box3 bound:bounds){
                        if(bound.collides(box))return true;
                    }
                }
            }
            return false;
        }
    }

    public final TemporalWorld versionedWorld;
    public final World world;
    public final AsyncWorldBuilder worldBuilder;
    public final RandomSource randomSource;
    public final Location from;
    public final Location to;

    public final int id;

    public final Dungeon dungeon;
    public final String dungeonName;

    public final BukkitRunnable syncDestroyTask=new BukkitRunnable(){
        @Override
        public void run() {
            all.remove(DungeonSession.this);
            destroy();
        }
    };

    private final DungeonEventListener eventListener;

    protected Location spawnLocation=null;
    protected final Set<Player> players=new HashSet<>();
    private final Set<Player> transfer;

    protected final Map<Player,Integer> deathCount=new HashMap<>();

    public final List<RoomSession> rooms;
    private final List<Runnable> tickTasks=new ArrayList<>();

    private DungeonSuccess success=DungeonSuccess.CONTINUOUS;
    private boolean transferred=false;
    private final int tick;

    private DungeonSession(
        Builder builder,
        TemporalWorld versionedWorld,
        Location from, Location to,
        Set<Player> players,
        int id
    ){
        this.dungeon=builder.dungeon;
        this.dungeonName=builder.dungeonName;
        this.randomSource=builder.randomSource;
        this.versionedWorld=versionedWorld;
        this.transfer=players;
        this.from=from;
        this.to=to;
        this.id=id;
        world=versionedWorld.world;
        worldBuilder=DungeonBl.config().builder()
            .makeBuilder(world);

        world.getPersistentDataContainer().set(dungeonWorldField, PersistentDataType.BYTE,(byte)1);

        //GameRules
        // Players
        versionedWorld.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN,true);
        versionedWorld.setGameRule(GameRule.KEEP_INVENTORY,true);
        versionedWorld.setGameRule(GameRule.SPAWN_RADIUS,0);
        versionedWorld.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS,false);
        versionedWorld.setGameRule(GameRule.SHOW_DEATH_MESSAGES,false);
        // Entities
        versionedWorld.setGameRule(GameRule.DISABLE_RAIDS,true);
        versionedWorld.setGameRule(GameRule.MOB_GRIEFING,false);
        versionedWorld.setGameRule(GameRule.DO_MOB_SPAWNING,false);
        versionedWorld.setGameRule(GameRule.DO_INSOMNIA,false);
        versionedWorld.setGameRule(GameRule.DO_PATROL_SPAWNING,false);
        versionedWorld.setGameRule(GameRule.DO_TRADER_SPAWNING,false);
        versionedWorld.setGameRule(GameRule.DO_WARDEN_SPAWNING,false);
        versionedWorld.setGameRule(GameRule.PROJECTILES_CAN_BREAK_BLOCKS,false);
        // World
        versionedWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);
        versionedWorld.setGameRule(GameRule.DO_WEATHER_CYCLE,false);
        versionedWorld.setGameRule(GameRule.DO_FIRE_TICK,false);
        versionedWorld.setGameRule(GameRule.DO_VINES_SPREAD,false);
        versionedWorld.setGameRule(GameRule.RANDOM_TICK_SPEED,0);
        versionedWorld.setGameRule(GameRule.WATER_SOURCE_CONVERSION,false);
        if(DBlVersioned.dataVersion()<4554)versionedWorld.setGameRule(GameRule.SPAWN_CHUNK_RADIUS,0);

        all.add(this);
        eventListener=new DungeonEventListener(this);

        rooms=List.copyOf(builder.rooms);
        rooms.forEach(room->room.fulfill(this));

        tick= DungeonBl.scheduler.scheduleSyncRepeatingTask(DungeonBl.plugin(),this::tick,1,1);
        if(tick==-1){
            logger.warn("Dungeon {} session error to start ticking",dungeonName);
            syncDestroyTask.runTask(DungeonBl.plugin());
            return;
        }

        PluginManager manager=Bukkit.getPluginManager();
        manager.registerEvents(eventListener, DungeonBl.plugin());

        DungeonBl.scheduler.runTaskLater(DungeonBl.plugin(),()->{
            manager.callEvent(new WorldInitEvent(world));
            manager.callEvent(new WorldLoadEvent(world));

            world.getChunkAt(0,0).load();

            rooms.getFirst().run();
        },1);
    }

    public void updateSpawn(Location location){
        Objects.requireNonNull(location,"location can't be null");
        if(!location.getWorld().equals(world))throw new RuntimeException("Given location in other world");

        if(spawnLocation==null){
            PlayerDataStorage playerDataStorage=DungeonBl.playerDataStorage();

            transfer.removeIf(pl->{
                if(pl.isOnline()){
                    playerDataStorage.get(pl).setDungeonSession(this);
                    DungeonBl.platformed().teleport(pl,location,()->{
                        pl.setFallDistance(0);

                        transfer.remove(pl);
                        players.add(pl);

                        playerDataStorage.endTransfer(pl);

                        if(transfer.isEmpty())transferred=true;
                    },()->{});

                    return false;
                }
                else return true;
            });
            if(transfer.isEmpty())transferred=true;
        }

        spawnLocation=new ImmutableLocation(location);
    }

    public void addTickTask(Runnable task){
        tickTasks.add(task);
    }

    private final PlayerDataStorage playerDataStorage=DungeonBl.playerDataStorage();

    private void tick(){
        worldBuilder.tick();

        tickTasks.forEach(Runnable::run);

        players.removeIf(player->{
            if(!player.isOnline())return false;
            World playerWorld=player.getWorld();
            PlayerData playerData=playerDataStorage.get(player);

            if(!playerWorld.equals(world)){
                DungeonSession session=playerData.getDungeonSession();
                if(session==null||session.id==id){
                    playerData.setDungeonSession(null);
                    playerData.getAndDeleteDungeonData();
                    playerData.getAndDeleteFoodData();
                }
                return true;
            }
            return false;
        });

        if(dungeon.disableStarvation()){
            players.forEach(pl->{
                pl.setFoodLevel(20);
                pl.setSaturation(1);
            });
        }

        if(transferred){
            if(players.isEmpty()||players.stream().noneMatch(Player::isOnline)){
                all.remove(this);
                destroy();
            }
        }
    }

    public int playerCount(){
        return players.size();
    }
    public List<Player> players(){
        return List.copyOf(players);
    }

    public DungeonSuccess status(){
        return success;
    }

    public Location getSpawnLocation(){
        return spawnLocation;
    }

    public void afterTransfer(Player player){
        if(player.getWorld().equals(world))return;
        playerDataStorage.beginTransfer(player);

        transfer.add(player);

        DungeonBl.platformed()
            .teleport(player,spawnLocation,()->{
                transfer.remove(player);
                players.add(player);

                playerDataStorage.endTransfer(player);
            },()->{
                transfer.remove(player);
                playerDataStorage.endTransfer(player);
            });
    }

    private void destroy(){
        if(success==DungeonSuccess.CONTINUOUS)success=DungeonSuccess.FAILURE;
        if(tick!=-1) DungeonBl.scheduler.cancelTask(tick);
        rooms.forEach(RoomSession::destroy);
        HandlerList.unregisterAll(eventListener);

        transfer.forEach(pl->{
            playerDataStorage.endTransfer(pl);
            playerDataStorage.endDungeon(pl,null,()->{});
        });

        List<Player> playerList=world.getPlayers();
        if(!playerList.isEmpty()){
            AfterCounterTask playerCounter=new AfterCounterTask(playerList.size(),versionedWorld::destroy);

            playerList.forEach(player-> {
                if(players.contains(player))playerDataStorage
                    .endDungeon(player,null,playerCounter::count);
                else DungeonBl.platformed()
                    .teleport(player,from,playerCounter::count,playerCounter::count);
            });
        }else versionedWorld.destroy();

        players.clear();
    }

    public void end(){
        success=DungeonSuccess.SUCCESS;
        all.remove(this);
        if(!players.isEmpty()){
            AfterCounterTask playerCounter=new AfterCounterTask(players.size(),this::destroy);

            players.forEach(player->{
                VaultIntegration vault=DungeonBl.vaultIntegration();
                if(vault!=null)vault.deposit(player,dungeon.reward().money());

                if(dungeon.reward().items()!=null)playerDataStorage.get(player)
                    .addRewards(dungeon.reward().items());

                if(player.isOnline())playerDataStorage.endDungeon(player,to,playerCounter::count);
                else playerCounter.count();
            });

            players.clear();
        }else destroy();
    }
}
