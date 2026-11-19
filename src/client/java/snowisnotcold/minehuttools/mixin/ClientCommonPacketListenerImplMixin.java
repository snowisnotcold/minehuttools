package snowisnotcold.minehuttools.mixin;

import snowisnotcold.minehuttools.LobbyDetector;
import snowisnotcold.minehuttools.MinehutToolsConfig;
import snowisnotcold.minehuttools.PackTracker;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Optional;
import java.util.UUID;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin {
    @Shadow @Final protected Connection connection;

    @Inject(method = "handleResourcePackPush", at = @At("HEAD"), cancellable = true)
    private void minehuttools$handlePack(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        String host = LobbyDetector.hostOf(connection.getRemoteAddress());
        boolean isPlayerServer = LobbyDetector.isPlayerServer(host);
        boolean isLobbyHost = LobbyDetector.isLobbyHost(host);

        if (isPlayerServer) {
            // "Auto-use server packs" off -> decline packs from player servers
            if (!MinehutToolsConfig.get().shouldAcceptPlayerServerPacks()) {
                LoggerFactory.getLogger("MinehutTools").info("Player server pack from '{}' url={} required={} -> DECLINED (auto-use off)",
                    host, packet.url(), packet.required());
                connection.send(new ServerboundResourcePackPacket(
                    packet.id(), ServerboundResourcePackPacket.Action.DECLINED));
                ci.cancel();
            }
            return;
        }

        if (!isLobbyHost && !host.contains("minehut")) {
            return;
        }

        Logger log = LoggerFactory.getLogger("MinehutTools");
        MinehutToolsConfig cfg = MinehutToolsConfig.get();
        int count = PackTracker.next(connection);
        String url = packet.url();
        String key = url + "|" + packet.hash();

        if (cfg.lobbyPackKey == null) cfg.lobbyPackKey = "";
        
        if (count == 1) {
            if (!key.equals(cfg.lobbyPackKey)) {
                log.info("Learned lobby pack: {}", key);
                cfg.lobbyPackKey = key;
                MinehutToolsConfig.save();
            }
        }

        boolean isLobbyPack = (count == 1 || (!cfg.lobbyPackKey.isEmpty() && key.equals(cfg.lobbyPackKey)));
        boolean isAdPack = url.contains("MHAds_RP") || url.contains("gsads-storage");
        
        boolean decline = (isLobbyPack && cfg.declineLobbyPack) || (isAdPack && (cfg.blockAds || cfg.declineLobbyPack));

        log.info("Resource pack push #{} from '{}' url={} required={} (isLobby={}, declineLobby={}, blockAds={}) -> {}",
            count, host, url, packet.required(), isLobbyPack, cfg.declineLobbyPack, cfg.blockAds, decline ? "DECLINED" : "allowed");

        if (decline) {
            connection.send(new ServerboundResourcePackPacket(
                packet.id(), ServerboundResourcePackPacket.Action.DECLINED));
            ci.cancel();
        } else {
            PackTracker.markAccepted(connection, packet.id());
        }
    }

    @Inject(method = "handleResourcePackPop", at = @At("HEAD"), cancellable = true)
    private void minehuttools$handlePop(ClientboundResourcePackPopPacket packet, CallbackInfo ci) {
        String host = LobbyDetector.hostOf(connection.getRemoteAddress());
        if (LobbyDetector.isPlayerServer(host)) return;

        if (!MinehutToolsConfig.get().declineLobbyPack) return;
        Logger log = LoggerFactory.getLogger("MinehutTools");
        Optional<UUID> id = packet.id();
        boolean skip;
        if (id.isPresent()) {
            skip = !PackTracker.wasAccepted(connection, id.get());
            if (!skip) PackTracker.forget(connection, id.get());
        } else {
            skip = !PackTracker.hasAny(connection);
            if (!skip) PackTracker.forgetAll(connection);
        }
        if (skip) ci.cancel();
    }
}
