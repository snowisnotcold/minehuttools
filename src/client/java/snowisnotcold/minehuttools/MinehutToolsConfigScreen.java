package snowisnotcold.minehuttools;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Animated config screen. All motion uses one symmetric easing so exits mirror entrances exactly. */
public class MinehutToolsConfigScreen extends Screen {
    private static final int PW = 340, BASE_H = 200, ROW_PITCH = 52, ROWS = 3;
    private static final String[] TITLES = { "Block [AD] messages", "Decline lobby resource pack", "Auto-use server packs" };
    private static final String[] SUBS = {
        "Hide Ad Messages in the Lobby",
        "Never download the Minehut lobby pack",
        "Load packs when you join other servers" };

    // ---- unified timing ----
    private static final float DUR = 0.35f;
    private static final double ROW_DELAY = 0.20;
    private static final double STAGGER = 0.08;
    private static final double TOTAL = ROW_DELAY + (ROWS - 1) * STAGGER + DUR;
    private static final double SPEED = 2.0; // shared idle-animation speed

    private final Screen parent;
    private boolean started = false;
    private long startNs, lastFrame;
    private final float[] toggleAnim = new float[ROWS];
    private final float[] rowHover = new float[ROWS];
    private float doneHover = 0f;
    private float subAnim;

    private boolean closing = false, closeDone = false;
    private long closeStart;
    private double closeFrom;

    public MinehutToolsConfigScreen(Screen parent) {
        super(Component.literal("MinehutTools"));
        this.parent = parent;
        for (int i = 0; i < ROWS; i++) toggleAnim[i] = get(i) ? 1f : 0f;
        subAnim = MinehutToolsConfig.get().declineLobbyPack ? 1f : 0f;
    }

    // ---- state ----
    private boolean get(int i) {
        MinehutToolsConfig c = MinehutToolsConfig.get();
        return i == 0 ? c.blockAds : i == 1 ? c.declineLobbyPack : c.autoUsePacks;
    }
    private void flip(int i) {
        MinehutToolsConfig c = MinehutToolsConfig.get();
        if (i == 0) c.blockAds = !c.blockAds;
        else if (i == 1) c.declineLobbyPack = !c.declineLobbyPack;
        else c.autoUsePacks = !c.autoUsePacks;
        MinehutToolsConfig.save();
    }

    // ---- timeline (forward on open, backward on close) ----
    private double sec() { return started ? (System.nanoTime() - startNs) / 1_000_000_000.0 : 0; }
    private double tl() {
        if (closing) return Math.max(0, closeFrom - (System.nanoTime() - closeStart) / 1_000_000_000.0);
        return Math.min(TOTAL, sec());
    }
    private float panelT() { return ease((float) (tl() / DUR)); }
    private float rowT(int i) { return ease((float) ((tl() - ROW_DELAY - i * STAGGER) / DUR)); }

    private int panelH() { return BASE_H + (int) (ROW_PITCH * ease(subAnim)); }
    private int px() { return (width - PW) / 2; }
    private int py(float t) { return (height - panelH()) / 2 + (int) ((1f - t) * 24); }
    private int rowY(int i, float t) { return py(t) + 52 + i * ROW_PITCH; }
    private boolean inRow(int i, double mx, double my, float t) {
        if (closing) return false;
        if (i == 2 && (subAnim < 0.8f || !MinehutToolsConfig.get().declineLobbyPack)) return false;
        int x = px() + 16, y = rowY(i, t);
        return mx >= x && mx <= x + PW - 32 && my >= y && my <= y + 44;
    }
    private boolean inDone(double mx, double my, float t) {
        if (closing) return false;
        int x = px() + PW / 2 - 50, y = py(t) + panelH() - 34;
        return mx >= x && mx <= x + 100 && my >= y && my <= y + 20;
    }

    // ---- helpers ----
    /** Symmetric ease-in-out: ease(1-x) == 1-ease(x), so reversed playback looks identical to forward. */
    private static float ease(float x) {
        x = Math.max(0f, Math.min(1f, x));
        return x < 0.5f ? 4f * x * x * x : 1f - (float) Math.pow(-2f * x + 2f, 3) / 2f;
    }
    private static final int PARTICLES = 90;

    /** Deterministic pseudo-random value in [0,1) for particle n and a given salt. */
    private static float rnd(int n, int salt) {
        int h = n * 374761393 + salt * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    private static float approach(float v, float target, float dt) {
        float step = dt / DUR;
        return v < target ? Math.min(target, v + step) : Math.max(target, v - step);
    }
    private static int lerpC(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
    private static int argb(float alpha, int rgb) {
        return (Math.max(0, Math.min(255, (int) (alpha * 255))) << 24) | (rgb & 0xFFFFFF);
    }
    private static void rrect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        for (int i = 0; i < h; i++) {
            int d = i < r ? r - i : (i >= h - r ? i - (h - r - 1) : 0);
            int inset = d == 0 ? 0 : (int) Math.round(r - Math.sqrt(Math.max(0, (double) r * r - (double) (d - 1) * (d - 1))));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        long now = System.nanoTime();
        if (!started) { started = true; startNs = now; lastFrame = now; }
        float dt = Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        double sec = sec();
        double ph = sec * SPEED;

        subAnim = approach(subAnim, MinehutToolsConfig.get().declineLobbyPack ? 1f : 0f, dt);

        float t = panelT();
        int H = panelH();
        float subE = ease(subAnim);
        for (int i = 0; i < ROWS; i++) toggleAnim[i] = approach(toggleAnim[i], get(i) ? 1f : 0f, dt);
        if (!closing) { // freeze hover states during exit so it mirrors the entrance
            for (int i = 0; i < ROWS; i++) rowHover[i] = approach(rowHover[i], inRow(i, mx, my, t) ? 1f : 0f, dt);
            doneHover = approach(doneHover, inDone(mx, my, t) ? 1f : 0f, dt);
        }

        if (closing && !closeDone && tl() <= 0) {
            closeDone = true;
            MinehutToolsConfig.save();
            minecraft.execute(() -> minecraft.setScreenAndShow(parent));
        }

        // backdrop
        float pulse = (float) (Math.sin(ph) * 0.5 + 0.5);
        g.fillGradient(0, 0, width, height,
            argb(0.78f * t, lerpC(0x0B0B1A, 0x160B2E, pulse)),
            argb(0.85f * t, lerpC(0x051018, 0x0B1E30, pulse)));

        // particles: each has its own random speed, size, drift, direction, lifetime and spawn spot
        for (int i = 0; i < PARTICLES; i++) {
            float speed = 8f + rnd(i, 1) * 30f;
            boolean down = rnd(i, 2) < 0.25f;
            double period = (height + 40) / (double) speed;
            double life = sec + rnd(i, 3) * period;
            long cyc = (long) Math.floor(life / period);
            float local = (float) ((life / period) - cyc);
            float baseX = rnd(i + (int) (cyc * 131), 4) * width;
            float drift = (rnd(i, 5) - 0.5f) * 60f * local;
            float sway = (float) Math.sin(ph * (0.15 + rnd(i, 6) * 0.35) + i * 1.7) * (4f + rnd(i, 7) * 18f);
            float pxx = baseX + drift + sway;
            float pyy = down ? -10 + local * (height + 40) : height + 10 - local * (height + 40);
            int sz = 1 + (int) (rnd(i, 8) * 3.99f);
            float twinkle = (float) Math.sin(ph * (0.6 + rnd(i, 9) * 0.8) + rnd(i, 10) * 6.283) * 0.5f + 0.5f;
            float fade = (float) Math.sin(Math.PI * local);
            float a = t * fade * (0.10f + 0.40f * twinkle);
            g.fill((int) pxx, (int) pyy, (int) pxx + sz, (int) pyy + sz,
                argb(a, lerpC(0x7C5CFF, 0x35D0FF, rnd(i, 11))));
        }

        int x = px(), y = py(t);
        int accentCol = lerpC(0x7C5CFF, 0x35D0FF, (float) (Math.sin(ph + 1.0) * 0.5 + 0.5));

        // glow, shadow, panel, accent bar
        float glow = (float) (Math.sin(ph) * 0.5 + 0.5);
        rrect(g, x - 5, y - 5, PW + 10, H + 10, 15, argb(t * (0.05f + 0.07f * glow), accentCol));
        rrect(g, x - 2, y - 2, PW + 4, H + 4, 12, argb(0.25f * t, 0x000000));
        rrect(g, x, y, PW, H, 10, argb(0.96f * t, 0x15151F));
        rrect(g, x, y, PW, 4, 2, argb(t, accentCol));

        // shimmer sweep
        double cycle = (ph / (Math.PI * 2)) % 1.0;
        int sx = x + (int) ((cycle * 1.6 - 0.3) * PW);
        for (int i = 0; i < 40; i++) {
            int cx = sx + i;
            if (cx < x + 3 || cx > x + PW - 3) continue;
            float a = 1f - Math.abs(i - 20) / 20f;
            g.fill(cx, y, cx + 1, y + 4, argb(t * a * 0.7f, 0xFFFFFF));
        }

        if (t < 0.05f) return;

        // title (level text, soft color wave)
        String title = "MinehutTools";
        int cx = width / 2 - font.width(title) / 2;
        for (int i = 0; i < title.length(); i++) {
            String s = String.valueOf(title.charAt(i));
            float wave = (float) Math.sin(ph - i * 0.4);
            int col = lerpC(0xFFFFFF, 0xB7A8FF, (wave + 1f) / 2f);
            g.text(font, s, cx, y + 16, argb(t, col));
            cx += font.width(s);
        }
        float sub = 0.6f + 0.25f * (float) Math.sin(ph);
        g.centeredText(font, "Helpful features", width / 2, y + 32, argb(t * sub, 0x8888A0));

        // rows
        for (int i = 0; i < ROWS; i++) {
            boolean isSub = i == 2;
            float vis = isSub ? subE : 1f;
            float rowIn = isSub ? rowT(1) : rowT(i);
            float rt = rowIn * t * vis;
            if (rt < 0.02f) continue;
            int indent = isSub ? 12 : 0;
            float slide = isSub ? (1f - vis) * 18f + (1f - rowIn) * 14f : (1f - rowIn) * 14f;
            int rx = x + 16 + indent - (int) slide, ry = rowY(i, t), rw = PW - 32 - indent;

            if (isSub) {
                g.fill(x + 24, rowY(1, t) + 44, x + 25, ry + 22, argb(rt * 0.5f, accentCol));
                g.fill(x + 24, ry + 22, rx, ry + 23, argb(rt * 0.5f, accentCol));
            }

            float hv = ease(rowHover[i]);
            rrect(g, rx, ry, rw, 44, 8, argb(rt, lerpC(0x1E1E2C, 0x28283C, hv)));
            int bar = (int) (3 * hv);
            if (bar > 0) rrect(g, rx, ry + 6, bar, 32, 1, argb(rt, accentCol));
            g.text(font, TITLES[i], rx + 14, ry + 12, argb(rt, 0xFFFFFF));
            g.text(font, SUBS[i], rx + 14, ry + 25, argb(rt, 0x8888A0));

            int tw = 36, th = 18, tx = rx + rw - tw - 14, ty = ry + 13;
            float on = ease(toggleAnim[i]);
            if (on > 0.5f) {
                float ring = (float) (Math.sin(ph + i) * 0.5 + 0.5);
                rrect(g, tx - 2, ty - 2, tw + 4, th + 4, 11, argb(rt * 0.18f * ring * on, 0x2ECC71));
            }
            rrect(g, tx, ty, tw, th, 9, argb(rt, lerpC(0x45455A, 0x2ECC71, on)));
            int knob = th - 4;
            int kx = tx + 2 + (int) ((tw - knob - 4) * on);
            rrect(g, kx, ty + 2, knob, knob, knob / 2, argb(rt, 0xFFFFFF));
        }

        // done button
        int bx = x + PW / 2 - 50, by = y + H - 34;
        float breathe = (float) (Math.sin(ph) * 0.5 + 0.5) * 0.25f;
        rrect(g, bx, by, 100, 20, 6, argb(t, lerpC(0x3A3A55, 0x7C5CFF, Math.min(1f, ease(doneHover) + breathe))));
        g.centeredText(font, "Done", bx + 50, by + 6, argb(t, 0xFFFFFF));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (closing) return true;
        float t = panelT();
        if (event.button() == 0) {
            for (int i = 0; i < ROWS; i++) {
                if (inRow(i, event.x(), event.y(), t)) { flip(i); return true; }
            }
            if (inDone(event.x(), event.y(), t)) { onClose(); return true; }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Starts the reversed-open exit animation; the real close happens when it finishes. */
    @Override public void onClose() {
        if (closing) return;
        MinehutToolsConfig.save();
        closeFrom = tl();
        closeStart = System.nanoTime();
        closing = true;
    }
    @Override public boolean isPauseScreen() { return false; }
}
