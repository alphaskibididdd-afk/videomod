package de.videomod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BrowserHandle {
    // Interne Aufloesung des Browsers. YouTube waehlt danach die Qualitaet (1920x1080 = 1080p).
    // Wenn es ruckelt: auf 1280 / 720 senken.
    private static final int BW = 1920, BH = 1080;

    private static MCEFBrowser browser;
    private static int lastW = -1, lastH = -1;
    private static int dispW = 320, dispH = 180;
    private static String pendingUrl;
    private static boolean initTried;
    private static GpuSampler sampler;

    private BrowserHandle() {}

    public static boolean ready() {
        try {
            CompletableFuture<MCEFApi> f = MCEFApi.getInstanceFuture();
            return f.isDone() && !f.isCompletedExceptionally();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean hasBrowser() { return browser != null; }

    public static void load(String input) {
        String url = normalize(input);
        if (url == null) return;
        if (!ready()) {
            pendingUrl = url;
            if (!initTried) {
                initTried = true;
                try { MCEFApi.initialize(); } catch (Throwable ignored) {}
            }
            return;
        }
        open(url);
    }

    private static void open(String url) {
        try {
            if (browser != null) {
                browser.close();
                browser = null;
            }
            browser = MCEFApi.getInstance().createBrowser(url, false);
            browser.setFocus(true);
            lastW = lastH = -1;
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void close() {
        if (browser != null) { browser.close(); browser = null; }
    }

    public static void draw(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        if (pendingUrl != null && ready()) {
            String u = pendingUrl;
            pendingUrl = null;
            open(u);
        }
        if (browser == null) return;
        dispW = w;
        dispH = h;
        if (lastW != BW || lastH != BH) {
            browser.resize(BW, BH);
            lastW = BW; lastH = BH;
        }
        if (sampler == null) {
            sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }
        g.blit(browser.getTextureView(), sampler, x, y, x + w, y + h, 0f, 1f, 0f, 1f);
    }

    private static int sx(double bx) { return (int) (bx * BW / Math.max(1, dispW)); }
    private static int sy(double by) { return (int) (by * BH / Math.max(1, dispH)); }

    public static void setFocus(boolean f) {
        if (browser != null) browser.setFocus(f);
    }

    public static void mouseMove(double bx, double by) {
        if (browser != null) browser.onMouseMoved(sx(bx), sy(by));
    }

    public static void mouse(double bx, double by, int button, boolean down) {
        if (browser == null) return;
        int px = sx(bx), py = sy(by);
        browser.onMouseMoved(px, py);
        MouseButtonEvent ev = new MouseButtonEvent(px, py, new MouseButtonInfo(button, 0));
        if (down) browser.onMouseClicked(ev, false);
        else browser.onMouseReleased(ev);
    }

    public static void scroll(double bx, double by, double delta) {
        if (browser != null) browser.onMouseScrolled(sx(bx), sy(by), delta);
    }

    public static void keyDown(KeyEvent e) {
        if (browser != null) browser.onKeyPressed(e);
    }

    public static void keyUp(KeyEvent e) {
        if (browser != null) browser.onKeyReleased(e);
    }

    public static void typed(CharacterEvent e) {
        if (browser != null) browser.onCharTyped(e);
    }

    /**
     * - Suchbegriff (kein Punkt oder mit Leerzeichen) -> YouTube-Suche
     * - YouTube-Link -> normale Videoseite (der Embed-Player gibt Fehler)
     * - sonst: Adresse unveraendert
     */
    static String normalize(String in) {
        if (in == null) return null;
        String s = in.trim();
        if (s.isEmpty()) return null;

        if (!s.contains("://") && (s.contains(" ") || !s.contains("."))) {
            return "https://www.youtube.com/results?search_query="
                    + URLEncoder.encode(s, StandardCharsets.UTF_8);
        }

        if (!s.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) s = "https://" + s;
        try {
            URI u = URI.create(s);
            String scheme = u.getScheme();
            if (!"http".equals(scheme) && !"https".equals(scheme)) return null;
        } catch (Exception e) { return null; }

        Matcher m = Pattern.compile("(?:youtu\\.be/|youtube\\.com/(?:watch\\?(?:.*&)?v=|live/|shorts/|embed/))([\\w-]{11})").matcher(s);
        if (m.find()) return "https://www.youtube.com/watch?v=" + m.group(1);
        return s;
    }
}
