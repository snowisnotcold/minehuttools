package snowisnotcold.minehuttools;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

public class MinehutToolsMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MinehutToolsConfig.load();

        // System / server messages
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> !AdFilter.shouldBlock(message));
        // Player chat messages
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, time) -> !AdFilter.shouldBlock(message));

        int[] ticks = { 0 };
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (ticks[0]++ % 20 == 0) LobbyDetector.refresh(mc);
        });

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

}
