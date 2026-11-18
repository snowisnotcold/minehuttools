package snowisnotcold.minehuttools;

import net.minecraft.network.Connection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Per-connection pack bookkeeping: push count and which pack IDs were actually allowed through. */
public final class PackTracker {
    private static final Map<Connection, Integer> COUNTS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Connection, Set<UUID>> ACCEPTED = Collections.synchronizedMap(new WeakHashMap<>());

    private PackTracker() {}

    public static int next(Connection c) { return COUNTS.merge(c, 1, Integer::sum); }

    public static void markAccepted(Connection c, UUID id) {
        ACCEPTED.computeIfAbsent(c, k -> Collections.synchronizedSet(new HashSet<>())).add(id);
    }

    public static boolean wasAccepted(Connection c, UUID id) {
        Set<UUID> s = ACCEPTED.get(c);
        return s != null && s.contains(id);
    }

    public static boolean hasAny(Connection c) {
        Set<UUID> s = ACCEPTED.get(c);
        return s != null && !s.isEmpty();
    }

    public static void forget(Connection c, UUID id) {
        Set<UUID> s = ACCEPTED.get(c);
        if (s != null) s.remove(id);
    }

    public static void forgetAll(Connection c) { ACCEPTED.remove(c); }
}
