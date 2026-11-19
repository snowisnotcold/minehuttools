package snowisnotcold.minehuttools.mixin;

import snowisnotcold.minehuttools.AdSoundFilter;
import snowisnotcold.minehuttools.LobbyDetector;
import snowisnotcold.minehuttools.MinehutToolsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ad blocking lives in AdFilter (via Fabric's message events); this mixin only handles the lobby pack pop. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleSystemChat", at = @At("HEAD"))
    private void minehuttools$onSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        // Only when the user wants the lobby pack declined, and only on the Minehut lobby itself.
        if (!MinehutToolsConfig.get().declineLobbyPack) return;
        if (!packet.content().getString().trim().startsWith("Welcome back,")) return;
        if (!LobbyDetector.isMinehutLobby(LobbyDetector.hostOf(((ClientPacketListener) (Object) this).getConnection().getRemoteAddress()))) return;

        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            try {
                if (mc.getDownloadedPackSource() != null) {
                    LoggerFactory.getLogger("MinehutTools").info("Lobby welcome message seen, popping server packs");
                    mc.getDownloadedPackSource().popAll();
                }
            } catch (Exception e) {
                LoggerFactory.getLogger("MinehutTools").error("Pack pop failed", e);
            }
        });
    }

    @Inject(method = "handleSoundEvent", at = @At("HEAD"), cancellable = true)
    private void minehuttools$onSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        if (AdSoundFilter.shouldMute(packet)) ci.cancel();
    }
}
