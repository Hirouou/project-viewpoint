package local.vpinteriors;

import java.lang.reflect.*;
import java.util.*;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;

/** Bounded, read-only streaming watches for roof components actually inspected. */
public final class RoofDependencies {
    static final int MAX_TARGETS=1024,MAX_LINKS=4096,MAX_DEPENDENCIES=64,LOOKUPS_PER_POLL=64;
    static final long POLL_NANOS=250_000_000L;
    public record Chunk(int x,int y) {}
    private record Target(int x,int y,int level) {}
    private static final LinkedHashMap<Target,LinkedHashSet<Chunk>> targets=new LinkedHashMap<>();
    private static final LinkedHashMap<Chunk,LinkedHashSet<Target>> watches=new LinkedHashMap<>();
    private static final LinkedHashSet<Target> ready=new LinkedHashSet<>();
    private static IsoCell world;
    private static int links;
    private static long nextPoll;
    private static boolean pollScheduled;
    private static Object inbox;
    private static Method record;
    private static boolean warned;

    private static void context(IsoCell cell) {
        if(world==cell)return;
        world=cell;targets.clear();watches.clear();ready.clear();links=0;nextPoll=0;pollScheduled=false;
    }
    private static void drop(Target target) {
        Set<Chunk> missing=targets.remove(target);ready.remove(target);
        if(missing==null)return;
        for(Chunk chunk:missing) {
            links--;
            Set<Target> waiting=watches.get(chunk);
            if(waiting!=null) {
                waiting.remove(target);
                if(waiting.isEmpty())watches.remove(chunk);
            }
        }
    }
    static synchronized void observe(IsoCell cell,IsoChunk chunk,int level,Set<Chunk> missing) {
        context(cell);
        Target target=new Target(chunk.wx,chunk.wy,level);
        drop(target);
        if(cell==null || missing.isEmpty())return;
        int count=Math.min(MAX_DEPENDENCIES,missing.size());
        while(!targets.isEmpty() && (targets.size()>=MAX_TARGETS || links+count>MAX_LINKS))
            drop(targets.keySet().iterator().next());
        LinkedHashSet<Chunk> held=new LinkedHashSet<>();
        for(Chunk dependency:missing) {
            if(held.size()==MAX_DEPENDENCIES)break;
            if(held.add(dependency)) {
                watches.computeIfAbsent(dependency,k->new LinkedHashSet<>()).add(target);
                links++;
            }
        }
        targets.put(target,held);
    }
    private static void invalidate(Target target)throws ReflectiveOperationException {
        if(record==null) {
            Class<?> cache=Class.forName("viewpoint.world.ChunkCache");
            Field field=cache.getDeclaredField("INBOX");field.setAccessible(true);inbox=field.get(null);
            record=inbox.getClass().getDeclaredMethod("record",int.class,int.class,int.class,int.class);
            record.setAccessible(true);
        }
        // Use Viewpoint's geometry mutation queue; no vanilla counters or
        // streaming request/load APIs are changed by this helper.
        record.invoke(inbox,target.x,target.y,target.level,1);
        RoofCompletion.resetRetry(target.x,target.y,target.level);
    }
    public static void frame(IsoCell cell) {
        try { poll(cell,System.nanoTime()); }
        catch(Throwable failure) {
            if(!warned) { warned=true;System.out.println("[ViewpointInteriors] Roof dependency retry unavailable: "+failure); }
        }
    }
    /** Testable clock; all production lookups happen only after native frame entry. */
    static synchronized int poll(IsoCell cell,long now)throws ReflectiveOperationException {
        context(cell);
        if(cell==null || targets.isEmpty() || (pollScheduled && now-nextPoll<0))return 0;
        nextPoll=now+POLL_NANOS;pollScheduled=true;
        int lookups=0;
        // Leave half the bounded poll budget for rotating still-absent chunks.
        while(!ready.isEmpty() && lookups<LOOKUPS_PER_POLL/2) {
            Target target=ready.iterator().next();
            IsoChunk loaded=cell.getChunk(target.x,target.y);lookups++;
            drop(target);
            if(loaded!=null && loaded.loaded)invalidate(target);
        }
        int visits=Math.min(LOOKUPS_PER_POLL-lookups,watches.size());
        for(int n=0;n<visits;n++) {
            var iterator=watches.entrySet().iterator();var entry=iterator.next();
            Chunk dependency=entry.getKey();LinkedHashSet<Target> waiting=entry.getValue();iterator.remove();
            IsoChunk loaded=cell.getChunk(dependency.x,dependency.y);lookups++;
            if(loaded==null || !loaded.loaded) {
                watches.put(dependency,waiting);
                continue;
            }
            for(Target target:waiting) {
                Set<Chunk> missing=targets.get(target);
                if(missing!=null && missing.remove(dependency)) {
                    links--;ready.add(target);
                }
            }
        }
        return lookups;
    }
    private RoofDependencies() {}
}
