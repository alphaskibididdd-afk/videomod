package de.videomod;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Menü: URL einfügen (Strg+V), "Laden" drücken.
 * Titelleiste (oben, orange) ziehen = verschieben, Ecke unten rechts (grün) ziehen = Größe ändern.
 * Klick ins Video wird an den Browser weitergegeben (Play/Pause usw.).
 */
public class VideoScreen extends Screen {
    private static final int BAR = 10, HANDLE = 12, MIN_W = 160, MIN_H = 90;
    private EditBox urlBox;
    private enum Mode { NONE, MOVE, RESIZE, BROWSER }
    private Mode mode = Mode.NONE;

    public VideoScreen() { super(Component.literal("Video")); }

    @Override
    protected void init() {
        urlBox = new EditBox(font, width / 2 - 150, height - 30, 240, 20, Component.literal("URL"));
        urlBox.setMaxLength(2048);
        urlBox.setValue(VideoConfig.I.url);
        addRenderableWidget(urlBox);
        addRenderableWidget(Button.builder(Component.literal("Laden"), b -> loadUrl())
                .bounds(width / 2 + 95, height - 30, 55, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Ein/Aus"), b -> VideoConfig.I.visible = !VideoConfig.I.visible)
                .bounds(width / 2 + 155, height - 30, 55, 20).build());
        setInitialFocus(urlBox);
    }

    private void loadUrl() {
        VideoConfig.I.url = urlBox.getValue();
        VideoConfig.I.visible = true;
        BrowserHandle.load(urlBox.getValue());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        VideoConfig c = VideoConfig.I;
        g.fill(c.x - 1, c.y - BAR, c.x + c.w + 1, c.y + c.h + 1, 0xFF202020);       // Rahmen
        g.fill(c.x, c.y - BAR + 1, c.x + c.w, c.y, 0xFFE08A1E);                      // Titelleiste
        g.fill(c.x, c.y, c.x + c.w, c.y + c.h, 0xFF000000);                           // Hintergrund
        BrowserHandle.draw(g, c.x, c.y, c.w, c.h);
        g.fill(c.x + c.w - HANDLE, c.y + c.h - HANDLE, c.x + c.w, c.y + c.h, 0xCC30C030); // Resize-Griff
        super.extractRenderState(g, mx, my, pt);   // Widgets (URL-Feld, Buttons) oben drauf
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        if (super.mouseClicked(e, dbl)) return true;
        VideoConfig c = VideoConfig.I;
        double mx = e.x(), my = e.y();
        if (mx >= c.x + c.w - HANDLE && mx <= c.x + c.w && my >= c.y + c.h - HANDLE && my <= c.y + c.h) {
            mode = Mode.RESIZE; return true;
        }
        if (mx >= c.x && mx <= c.x + c.w && my >= c.y - BAR && my < c.y) { mode = Mode.MOVE; return true; }
        if (mx >= c.x && mx <= c.x + c.w && my >= c.y && my <= c.y + c.h) {
            mode = Mode.BROWSER;
            BrowserHandle.mouse(mx - c.x, my - c.y, e.button(), true);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        VideoConfig c = VideoConfig.I;
        switch (mode) {
            case MOVE -> {
                c.x = Math.max(0, Math.min(width - c.w, c.x + (int) Math.round(dx)));
                c.y = Math.max(BAR, Math.min(height - c.h, c.y + (int) Math.round(dy)));
                return true;
            }
            case RESIZE -> {
                // 16:9 beibehalten, Breite bestimmt die Höhe
                int nw = Math.max(MIN_W, Math.min(width - c.x, c.w + (int) Math.round(dx)));
                c.w = nw;
                c.h = Math.max(MIN_H, nw * 9 / 16);
                return true;
            }
            default -> { return super.mouseDragged(e, dx, dy); }
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        VideoConfig c = VideoConfig.I;
        if (mode == Mode.BROWSER) BrowserHandle.mouse(e.x() - c.x, e.y() - c.y, e.button(), false);
        mode = Mode.NONE;
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        VideoConfig c = VideoConfig.I;
        if (mx >= c.x && mx <= c.x + c.w && my >= c.y && my <= c.y + c.h) {
            BrowserHandle.scroll(mx - c.x, my - c.y, sy);
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public void onClose() {
        VideoConfig.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
