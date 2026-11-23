package de.videomod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BrowserHandle {
    private static MCEFBrowser browser;
    private static int lastW = -1, lastH = -1;
    private static String pendingUrl;
    private static boolean initTried;
    private static GpuSampler sampler;

    private BrowserHandle() {}

    /** Ist Chromium fertig geladen? (1. Start: Download, dauert) */
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
            // Chromium laedt noch: URL merken, wird automatisch gestartet sobald fertig
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
            if (browser == null) {
                browser = MCEFApi.getInstance().createBrowser(url, false);
                browser.setFocus(true);
                lastW = lastH = -1;
            } else {
                browser.getCefBrowser().loadURL(url);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void close() {
        if (browser != null) { browser.close(); browser = null; }
    }

    /** Zeichnet das Video an x,y mit Groesse w,h. */
    public static void draw(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        if (pendingUrl != null && ready()) {
            String u = pendingUrl;
            pendingUrl = null;
            open(u);
        }
        if (browser == null) return;
        if (w != lastW || h != lastH) {
            browser.resize(w, h);
            lastW = w; lastH = h;
        }
        if (sampler == null) {
            sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }
        g.blit(browser.getTextureView(), sampler, x, y, x + w, y + h, 0f, 1f, 0f, 1f);
    }

    public static void mouse(double bx, double by, int button, boolean down) {
        if (browser == null) return;
        browser.onMouseMoved((int) bx, (int) by);
        MouseButtonEvent ev = new MouseButtonEvent(bx, by, new MouseButtonInfo(button, 0));
        if (down) browser.onMouseClicked(ev, false);
        else browser.onMouseReleased(ev);
    }

    public static void scroll(double bx, double by, double delta) {
        if (browser != null) browser.onMouseScrolled((int) bx, (int) by, delta);
    }

    /** YouTube-Links -> Embed-Player (mit Autoplay). Rest: unveraendert. */
    static String normalize(String in) {
        if (in == null) return null;
        String s = in.trim();
        if (s.isEmpty()) return null;
        if (!s.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) s = "https://" + s;
        try {
            URI u = URI.create(s);
            String scheme = u.getScheme();
            if (!"http".equals(scheme) && !"https".equals(scheme)) return null;
        } catch (Exception e) { return null; }
        Matcher m = Pattern.compile("(?:youtu\\.be/|youtube\\.com/(?:watch\\?(?:.*&)?v=|live/|shorts/))([\\w-]{11})").matcher(s);
        if (m.find()) return "https://www.youtube.com/embed/" + m.group(1) + "?autoplay=1";
        return s;
    }
}
