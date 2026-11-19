package snowisnotcold.minehuttools;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.Objective;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public final class LobbyDetector {
    private LobbyDetector() {}

    public static String hostOf(SocketAddress address) {
        if (address instanceof InetSocketAddress isa) {
            return isa.getHostString();
        }
        return "";
    }

    public static boolean isLobbyHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase();
        return h.equals("mc.minehut.com") || h.equals("minehut.com") || h.equals("localhost") || h.equals("127.0.0.1");
    }

    public static boolean isPlayerServer(String host) {
        if (host == null) return false;
        String h = host.toLowerCase();
        return h.endsWith(".minehut.gg");
    }

    /** True for the Minehut lobby (or a local test host), never for a player server. */
    public static boolean isMinehutLobby(String host) {
        if (host == null || isPlayerServer(host)) return false;
        return isLobbyHost(host) || host.toLowerCase().contains("minehut");
    }

    private static volatile boolean inLobby = false;

    /** True while you are in the Minehut lobby itself (not a player server, not the captcha server). */
    public static boolean inLobby() { return inLobby; }

    /** Called from the client tick (main thread); packet handlers on other threads just read the flag. */
    public static void refresh(Minecraft mc) {
        try {
            if (mc.getConnection() == null || mc.level == null) { inLobby = false; return; }
            String host = hostOf(mc.getConnection().getConnection().getRemoteAddress());
            if (!isMinehutLobby(host)) { inLobby = false; return; }
            boolean board = false;
            for (Objective o : mc.level.getScoreboard().getObjectives()) {
                if (o.getDisplayName().getString().toLowerCase().contains("minehut")) { board = true; break; }
            }
            inLobby = board;
        } catch (Exception e) {
            inLobby = false;
        }
    }
}
