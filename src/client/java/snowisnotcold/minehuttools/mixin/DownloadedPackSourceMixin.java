package snowisnotcold.minehuttools.mixin;

import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.resources.server.PackReloadConfig;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Diagnostic: logs what triggers a server-pack resource reload. */
@Mixin(DownloadedPackSource.class)
public abstract class DownloadedPackSourceMixin {
    @Inject(method = "startReload", at = @At("HEAD"))
    private void minehuttools$logReload(PackReloadConfig.Callbacks callbacks, CallbackInfo ci) {
        StackTraceElement[] st = new Throwable().getStackTrace();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < Math.min(st.length, 9); i++) sb.append("\n    at ").append(st[i]);
        LoggerFactory.getLogger("MinehutTools").info("Server pack resource reload started{}", sb);
    }
}
