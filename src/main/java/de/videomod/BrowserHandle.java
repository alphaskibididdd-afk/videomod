package de.videomod;

import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BrowserHandle {
    private static MCEFBrowser browser;
    private static int lastW = -1, lastH = -1;

    private BrowserHandle() {}

    /** MCEF-API: ist Chromium fertig initialisiert? (läuft asynchron, 1. Start lädt Chromium herunter) */
    public static boolean ready() {
        return MCEFApi.isInitialized();
    }

    public static boolean hasBrowser() { return browser != null; }

    public static void load(String input) {
        String url = normalize(input);
        if (url == null || !ready()) return;
        if (browser == null) {
            // MCEF-API: Browser erzeugen (url, transparent)
            browser = MCEFApi.createBrowser(url, false);
            lastW = lastH = -1;
        } else {
            browser.loadURL(url);               // MCEF-API
        }
    }

    public static void close() {
        if (browser != null) { browser.close(); browser = null; }   // MCEF-API
    }

    /** Zeichnet das Video an x,y mit Größe w,h. */
    public static void draw(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        if (browser == null) return;
        if (w != lastW || h != lastH) {
            browser.resize(w, h);               // MCEF-API: Browsergröße = Fenstergröße
            lastW = w; lastH = h;
        }
        // MCEF-API: Textur des Browsers zeichnen (im Testmod nachsehen, wie sie geblittet wird)
        g.blit(browser.getTextureView(), x, y, x + w, y + h, 0f, 1f, 0f, 1f);
    }

    public static void mouse(double bx, double by, int button, boolean down) {
        if (browser == null) return;
        browser.sendMouseMove((int) bx, (int) by);                 // MCEF-API
        if (down) browser.sendMousePress((int) bx, (int) by, button);
        else browser.sendMouseRelease((int) bx, (int) by, button);
    }

    public static void scroll(double bx, double by, double delta) {
        if (browser != null) browser.sendMouseWheel((int) bx, (int) by, delta);  // MCEF-API
    }

    /** YouTube-Links -> Embed-Player (ohne Drumherum, mit Autoplay). Rest: unverändert. */
    static String normalize(String in) {
        if (in == null) return null;
        String s = in.trim();
        if (s.isEmpty()) return null;
        if (!s.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) s = "https://" + s;
        try {
            URI u = URI.create(s);
            String scheme = u.getScheme();
            if (!"http".equals(scheme) && !"https".equals(scheme)) return null;  // nur http(s)
        } catch (Exception e) { return null; }
        Matcher m = Pattern.compile("(?:youtu\\.be/|youtube\\.com/(?:watch\\?(?:.*&)?v=|live/|shorts/))([\\w-]{11})").matcher(s);
        if (m.find()) return "https://www.youtube.com/embed/" + m.group(1) + "?autoplay=1";
        return s;
    }
}
