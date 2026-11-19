package snowisnotcold.minehuttools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class MinehutToolsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = FabricLoader.getInstance().getConfigDir();
    private static final Path FILE = DIR.resolve("minehuttools.json");
    private static final Path OLD_FILE = DIR.resolve("minehutissoskibidi.json");
    private static MinehutToolsConfig instance = new MinehutToolsConfig();

    public boolean blockAds = true;
    public boolean declineLobbyPack = true;
    public boolean autoUsePacks = true;
    public boolean muteLobbyAdSounds = true;
    public String lobbyPackKey = "";

    public static MinehutToolsConfig get() { return instance; }

    /**
     * Whether packs pushed by player servers should be loaded. "Auto-use server packs" is only shown
     * (and only applies) while "Decline lobby resource pack" is on; otherwise packs are never touched.
     */
    public boolean shouldAcceptPlayerServerPacks() {
        return !declineLobbyPack || autoUsePacks;
    }

    public static void load() {
        try {
            Path src = Files.exists(FILE) ? FILE : (Files.exists(OLD_FILE) ? OLD_FILE : null);
            if (src != null) {
                MinehutToolsConfig c = GSON.fromJson(Files.readString(src), MinehutToolsConfig.class);
                if (c != null) instance = c;
            }
            save(); // writes the new file (migrates old settings)
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void save() {
        try { Files.writeString(FILE, GSON.toJson(instance)); }
        catch (Exception e) { e.printStackTrace(); }
    }
}
