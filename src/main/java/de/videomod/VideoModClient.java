package de.videomod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class VideoModClient implements ClientModInitializer {
    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("videomod", "main"));
        openKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.videomod.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, cat));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new VideoScreen());
            }
        });

        // Video als HUD-Overlay, wenn kein Menü offen ist
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("videomod", "video"), (g, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            VideoConfig c = VideoConfig.I;
            if (!c.visible || mc.screen instanceof VideoScreen || mc.options.hideGui) return;
            BrowserHandle.draw(g, c.x, c.y, c.w, c.h);
        });
    }
}
