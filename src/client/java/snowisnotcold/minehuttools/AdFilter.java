package snowisnotcold.minehuttools;

import net.minecraft.network.chat.Component;

/** The one place that decides whether a chat line is an ad. Does nothing unless "Block [AD] messages" is on. */
public final class AdFilter {
    private AdFilter() {}

    public static boolean shouldBlock(Component message) {
        if (!MinehutToolsConfig.get().blockAds) return false;
        return message.getString().stripLeading().startsWith("[AD]");
    }
}
