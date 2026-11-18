package snowisnotcold.minehuttools.mixin;

import snowisnotcold.minehuttools.MinehutToolsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleSystemChat", at = @At("HEAD"), cancellable = true)
    private void minehuttools$filterSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        String text = packet.content().getString().trim();

        if (MinehutToolsConfig.get().blockAds && text.startsWith("[AD]")) {
            ci.cancel();
            return;
        }

        if (text.startsWith("Welcome back,")) {
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
    }

    @Inject(method = "handlePlayerChat", at = @At("HEAD"), cancellable = true)
    private void minehuttools$filterPlayerChat(ClientboundPlayerChatPacket packet, CallbackInfo ci) {
        if (MinehutToolsConfig.get().blockAds) {
            String text = packet.unsignedContent() != null ? packet.unsignedContent().getString() : "";
            if (text.trim().startsWith("[AD]")) {
                ci.cancel();
            }
        }
    }
}
