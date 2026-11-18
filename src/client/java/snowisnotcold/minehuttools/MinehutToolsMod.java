package snowisnotcold.minehuttools;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class MinehutToolsMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MinehutToolsConfig.load();

        // System / server messages
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> !shouldBlock(message));
        // Player chat messages
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, time) -> !shouldBlock(message));

        // /minehuttools and /mt both open the config screen
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            for (String name : new String[] { "minehuttools", "mt" }) {
                dispatcher.register(ClientCommands.literal(name).executes(ctx -> {
                    Minecraft mc = Minecraft.getInstance();
                    // Delay a tick so the chat screen doesn't immediately close ours
                    mc.execute(() -> mc.setScreenAndShow(new MinehutToolsConfigScreen(null)));
                    return 1;
                }));
            }
        });
    }

    private static boolean shouldBlock(Component message) {
        if (!MinehutToolsConfig.get().blockAds) return false;
        return message.getString().stripLeading().startsWith("[AD]");
    }
}
