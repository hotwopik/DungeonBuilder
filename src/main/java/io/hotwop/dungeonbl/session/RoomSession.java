package io.hotwop.dungeonbl.session;

import io.hotwop.dungeonbl.io.DBlRegistries;
import io.hotwop.dungeonbl.mark.dungeon.DungeonMarkData;
import io.hotwop.dungeonbl.mark.dungeon.LoadOrder;
import io.hotwop.dungeonbl.mob.Boss;
import io.hotwop.dungeonbl.mob.BossEntity;
import io.hotwop.dungeonbl.session.collector.*;
import io.hotwop.dungeonbl.session.event.EventConsumer;
import io.hotwop.dungeonbl.session.event.RoomEvent;
import io.hotwop.dungeonbl.structure.Decoration;
import io.hotwop.dungeonbl.util.math.*;
import io.hotwop.dungeonbl.util.task.CancelTask;
import io.hotwop.dungeonbl.util.task.DoneTaskResult;
import io.hotwop.dungeonbl.util.view.ListView;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import io.hotwop.dungeonbl.DungeonBl;
import io.hotwop.dungeonbl.config.Room;
import io.hotwop.dungeonbl.structure.RoomBuilding;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;

import static io.hotwop.dungeonbl.DungeonBl.logger;

public final class RoomSession{
    private static final BukkitScheduler scheduler=Bukkit.getScheduler();

    private final Map<RoomEvent,Map<Vec3,EventConsumer>> eventTasks;

    private final Set<Entity> mobs=new HashSet<>();
    private final Set<BossEntity> bosses=new HashSet<>();

    private final DoneTaskResult<RoomSession> task;
    private DungeonSession session;

    public final Room room;

    public final SideRotation rotation;
    public final Vec3 pos;
    public final Box3 box;
    public final Set<Vec2> chunks;

    public final RoomBuilding roomBuilding;
    public final List<Decoration> decorations;
    public final RoomType type;
    public final Iterator<Boss> bossIterator;

    public final SignalStorage signals=new SignalStorage();

    private boolean prerun=false;
    private boolean run=false;
    private boolean stopped=false;
    private int tick;

    private int wave;
    public int wave(){
        return wave;
    }

    public final int depth;

    private RoomSession(
        Builder builder,
        Map<RoomEvent,Map<Vec3,EventConsumer>> eventTasks
    ){
        rotation=builder.rotation;
        pos=builder.pos;
        box=builder.box;
        chunks=builder.chunks;
        roomBuilding=builder.roomBuilding;
        decorations=builder.decorations;
        type=builder.type;
        task=builder.task;
        room=builder.room;
        wave=builder.room.waves();
        depth=builder.depth;

        this.eventTasks=eventTasks;

        if(room.bosses()==null)bossIterator=Collections.emptyIterator();
        else bossIterator=room.bosses().iterator();
    }

    protected void fulfill(DungeonSession session){
        this.session=session;
        task.done(this);
    }

    public void run(){
        if(run||prerun||stopped)return;
        prerun=true;

        DungeonBl.executorService.execute(()->{
            tick=scheduler.scheduleSyncRepeatingTask(DungeonBl.plugin(),this::tick,1,1);
            if(tick!=-1){
                roomBuilding.place(pos,rotation,session.worldBuilder);
                run=true;
                scheduler.runTask(DungeonBl.plugin(),()->{
                    runEvent(RoomEvent.activation);
                    if(type==RoomType.START&&session.spawnLocation==null){
                        logger.warn(
                            "Player Spawn in Start Room wasn't set, error to begin dungeon, {} {}",
                            session.dungeonName,room.building()
                        );
                        session.syncDestroyTask.run();
                    }
                });
            }else{
                logger.warn("Error to start room ticking, {} {}",session.dungeonName,room.building());
                session.syncDestroyTask.run();
            }
        });
    }

    private void tick(){
        runEvent(RoomEvent.tick);

        mobs.removeIf(Entity::isDead);
        if(mobs.isEmpty()&&wave>=1){
            --wave;
            runEvent(RoomEvent.spawn);
        }

        bosses.removeIf(BossEntity::update);

        if(room.endsWithPlayers()){
            for(Player player:session.players){
                if(box.collides(player.getBoundingBox())){
                    stop();
                    return;
                }
            }
        }

        if(room.endsWithClear()){
            if(mobs.isEmpty()&&bosses.isEmpty()){
                stop();
                return;
            }
        }

        if(room.endsOnSignal()!=null){
            if(signals.get(room.endsOnSignal()).reset()>=room.endSignalValue()){
                stop();
                return;
            }
        }

        signals.resetAll();
    }

    public void stop(){
        if(!run||stopped)return;
        scheduler.cancelTask(tick);

        int nextRoomDelay=room.nextRoomDelay()==null?session.dungeon.nextRoomDelay():room.nextRoomDelay();

        if(nextRoomDelay==0)stopTask();
        else DungeonBl.scheduler.scheduleSyncDelayedTask(DungeonBl.plugin(),this::stopTask,nextRoomDelay);
    }

    private void stopTask(){
        runEvent(RoomEvent.deactivation);
        stopped=true;
        if(type==RoomType.END)session.end();
    }

    public void destroy(){
        if(!run||stopped)return;

        scheduler.cancelTask(tick);
        stopped=true;
    }

    public void runEvent(RoomEvent event){
        if(!run||stopped)return;
        Map<Vec3,EventConsumer> eventMap=eventTasks.get(event);
        if(eventMap!=null)eventMap.forEach((loc,task)->task
            .consume(loc.toLocation(session.world),this)
        );
    }

    public void hookMob(Entity entity){
        if(!run||stopped||entity.isDead()||!entity.getWorld().equals(session.world))return;
        mobs.add(entity);
    }

    public void hookBoss(BossEntity boss){
        if(!run||stopped||boss.entity().isDead()||!boss.entity().getWorld().equals(session.world))return;
        bosses.add(boss);
    }

    public boolean isStarted(){
        return run;
    }
    public boolean isStopped(){
        return stopped;
    }

    public boolean collide(Box3 other){
        return box.collides(other);
    }

    public DungeonSession dungeonSession() {
        return session;
    }

    public static Builder builder(
        DungeonSession.Builder builder,
        @Nullable Builder parent,
        Room room,
        RoomType type,

        DoneTaskResult<RoomSession> task,

        Vec3 pos,
        SideRotation rotation
    ){
        DBlRegistries structStorage=DungeonBl.registries();
        RoomBuilding struct=structStorage.roomBuildings
            .get(room.building());

        if(struct==null){
            logger.warn("Error, can't generate room, building does not exist, {} {}",builder.dungeonName,room.building());
            return null;
        }

        List<Decoration> decorations=new ArrayList<>();
        if(room.decorations()!=null)room.decorations().forEach(name->{
            Decoration dec=structStorage.decorations.get(name);
            if(dec==null){
                logger.warn("Unknown decoration {}, {} {}", name, builder.dungeonName, room.building());
                return;
            }
            decorations.add(dec);
        });
        return new Builder(
            builder,
            parent,
            task,

            room,
            type,

            struct,
            List.copyOf(decorations),

            pos,
            rotation
        );
    }

    public static final class Builder extends CancelTask{
        public final DungeonSession.Builder builder;
        public final @Nullable Builder parent;
        private final DoneTaskResult<RoomSession> task;

        public final int depth;
        public final Vec3 placeFrom;
        public final SideRotation placeRotation;

        public final Room room;
        public final RoomType type;

        public final int sizeX;
        public final int sizeY;
        public final int sizeZ;

        public final RoomBuilding roomBuilding;
        public final List<Decoration> decorations;

        private SideRotation rotation;
        private Box3 box;
        private Set<Vec2> chunks;
        private Vec3 pos;

        private boolean placementStep=false;

        private RoomSession result=null;
        private final List<Builder> children=new ArrayList<>();
        private final ListView<Builder> childrenView=new ListView<>(children);

        private boolean checked=false;

        private Builder(
            DungeonSession.Builder builder,
            @Nullable Builder parent,
            DoneTaskResult<RoomSession> task,

            Room room,
            RoomType type,

            RoomBuilding roomBuilding,
            List<Decoration> decorations,

            Vec3 pos,
            SideRotation rotation
        ){
            this.builder = builder;
            this.parent = parent;
            this.task = task;

            this.placeFrom = pos;
            this.placeRotation = rotation;

            this.room = room;
            this.type = type;

            this.roomBuilding = roomBuilding;
            this.decorations = decorations;

            if(parent!=null)depth=parent.depth+1;
            else depth=0;

            Vec3 size=roomBuilding.size();
            sizeX=size.x();
            sizeY=size.y();
            sizeZ=size.z();
        }

        protected boolean isChecked(){
            return checked;
        }

        protected void check(){
            checked=true;
        }

        public Vec3 pos(){
            if(!placementStep)throw new RuntimeException("Room Building Step wasn't complete");
            return pos;
        }

        public SideRotation rotation(){
            if(!placementStep)throw new RuntimeException("Room Building Step wasn't complete");
            return rotation;
        }

        public Box3 box(){
            if(!placementStep)throw new RuntimeException("Room Building Step wasn't complete");
            return box;
        }

        protected void build() throws DungeonBuildException{
            Map<RoomEvent,Map<Vec3,EventConsumer>> events=new HashMap<>();
            List<NextSuggestion> exits=new ArrayList<>();

            if(type==RoomType.START){
                pos=placeFrom;
                rotation=placeRotation;
            }else{
                List<PlacementSuggestion> placements=new ArrayList<>();

                Map<DungeonMarkData<?,?,?>,Map<Vec3, RoomBuilding.MarkSavedData>> buildingMarks=roomBuilding.markTypesCache
                    .get(LoadOrder.BUILDING);

                shuffleMarks(buildingMarks,(pos,data)->{
                    RoomBuildingCollector collector=new RoomBuildingCollector();
                    boolean value=data.codec().dungeon
                        .place(pos,data.placeData(),this,collector);
                    RoomBuildingCollector.Result result=collector.done();

                    placements.addAll(result.suggestions());
                    result.events().forEach((ev,ec)->events
                        .computeIfAbsent(ev,k->new HashMap<>())
                        .put(pos,ec)
                    );

                    return value;
                });

                if(isCanceled())return;

                if(placements.isEmpty()){
                    logger.warn("Room has no entrance, {} {}",builder.dungeonName,room.building());
                    throw new DungeonBuildException("Room has no entrance");
                }

                PlacementSuggestion outPlacement=null;
                for(PlacementSuggestion placement:placements){
                    SideRotation outRot=placeRotation.sub(placement.rotation());
                    Vec3 outPos=placeFrom.add(placement.pos().rotate(outRot));

                    Box3 box=Box3.withSizeAndRotation(outPos,roomBuilding.size(),outRot);

                    if( // Out of world check
                        box.y1()<-64||box.y2()>319||
                        box.x1()<-29999984||box.x2()>29999984||
                        box.z1()<-29999984||box.z2()>29999984
                    )continue;

                    if(!builder.checkCollisions(box)){
                        outPlacement=placement;
                        break;
                    }
                }

                if(outPlacement==null)return;

                rotation=placeRotation.sub(outPlacement.rotation());
                pos=placeFrom.add(outPlacement.pos().rotate(rotation));
            }

            box=Box3.withSizeAndRotation(pos,roomBuilding.size(),rotation);
            chunks=Set.copyOf(box.occupiedChunks());

            placementStep=true;

            Map<DungeonMarkData<?,?,?>,Map<Vec3, RoomBuilding.MarkSavedData>> inRoomMarks=roomBuilding.markTypesCache
                .get(LoadOrder.IN_ROOM);

            shuffleMarks(inRoomMarks,(pos,data)->{
                InRoomCollector collector=new InRoomCollector();
                boolean value=data.codec().dungeon
                    .place(pos,data.placeData(),this,collector);

                collector.done().forEach((ev,ec)->events
                    .computeIfAbsent(ev,k->new HashMap<>())
                    .put(pos,ec)
                );

                return value;
            });

            if(isCanceled())return;

            if(type!=RoomType.END){
                Map<DungeonMarkData<?,?,?>,Map<Vec3, RoomBuilding.MarkSavedData>> outRoomMarks=roomBuilding.markTypesCache
                    .get(LoadOrder.OUT_ROOM);

                shuffleMarks(outRoomMarks,(pos,data)->{
                    OutRoomCollector collector=new OutRoomCollector();
                    boolean value=data.codec().dungeon
                        .place(pos,data.placeData(),this,collector);

                    OutRoomCollector.Result result=collector.done();

                    exits.addAll(result.suggestions());

                    result.events().forEach((ev,ec)->events
                        .computeIfAbsent(ev,k->new HashMap<>())
                        .put(pos,ec)
                    );

                    return value;
                });

                if(isCanceled())return;

                if(exits.isEmpty()){
                    logger.warn("Room has no exits, {} {}",builder.dungeonName,room.building());
                    throw new DungeonBuildException("Room has no exits");
                }

                List<Room> rooms=new ArrayList<>(builder.dungeon.rooms());

                if(depth==builder.dungeon.roomCount()){
                    Room endRoom=builder.dungeon.endRoom();

                    for(NextSuggestion next:exits){
                        RoomSession.Builder nextBuilder=RoomSession.builder(
                            builder,
                            this,
                            endRoom,
                            RoomType.END,
                            next.task(),
                            next.pos()
                                .rotate(rotation)
                                .add(pos),
                            next.rotation()
                        );

                        if(nextBuilder!=null)children.add(nextBuilder);
                    }
                }else{
                    Random roomRandom=builder.randomSource.get("rooms");

                    for(NextSuggestion next:exits){
                        Collections.shuffle(rooms,roomRandom);
                        Vec3 position=next.pos()
                            .rotate(rotation)
                            .add(pos);
                        SideRotation rot=next.rotation()
                            .add(rotation);

                        for(Room room:rooms){
                            RoomSession.Builder nextBuilder=RoomSession.builder(
                                builder,
                                this,
                                room,
                                RoomType.BASE,
                                next.task(),
                                position,
                                rot
                            );

                            if(nextBuilder!=null)children.add(nextBuilder);
                        }
                    }
                }
            }

            Map<RoomEvent,Map<Vec3,EventConsumer>> outTasks=new HashMap<>();

            events.forEach((event,tasks)->{
                Map<Vec3,EventConsumer> eventMap=new HashMap<>();

                tasks.forEach((pos,task)->{
                    eventMap.put(pos.rotate(rotation).add(this.pos),task);
                });

                outTasks.put(event,Map.copyOf(eventMap));
            });

            result=new RoomSession(this,Map.copyOf(outTasks));
        }

        private void shuffleMarks(
            Map<DungeonMarkData<?,?,?>,Map<Vec3,RoomBuilding.MarkSavedData>> marks,
            BiPredicate<Vec3,RoomBuilding.MarkSavedData> iterator
        ){
            if(marks==null)return;
            Random marksRandom=builder.randomSource.get("marks");

            List<DungeonMarkData<?,?,?>> types=new ArrayList<>(marks.keySet());
            Collections.shuffle(types,marksRandom);

            for(DungeonMarkData<?,?,?> type:types){
                Map<Vec3, RoomBuilding.MarkSavedData> typeMap=marks.get(type);

                List<Vec3> poses=new ArrayList<>(typeMap.keySet());
                Collections.shuffle(poses,marksRandom);
                for(Vec3 pos:poses){
                    if(iterator.test(pos,typeMap.get(pos)))break;
                }
            }
        }

        public @Nullable RoomSession result(){
            return result;
        }

        public List<Builder> children(){
            return childrenView;
        }
    }
}
