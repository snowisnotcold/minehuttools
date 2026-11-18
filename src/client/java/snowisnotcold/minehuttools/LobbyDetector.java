package snowisnotcold.minehuttools;

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
}
