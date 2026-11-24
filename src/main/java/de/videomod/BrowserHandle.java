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
        g.blit(browser.getTextureView
