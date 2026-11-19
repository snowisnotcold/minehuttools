package snowisnotcold.minehuttools;

import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundSource;

/** Mutes the lobby's ad sound (an ambient sound sent as an unregistered/custom sound). Only in the lobby. */
public final class AdSoundFilter {
    private AdSoundFilter() {}

    public static boolean shouldMute(ClientboundSoundPacket packet) {
        if (!MinehutToolsConfig.get().muteLobbyAdSounds) return false;
        if (!LobbyDetector.inLobby()) return false;
        return packet.getSource() == SoundSource.AMBIENT && packet.getSound().unwrapKey().isEmpty();
    }
}
