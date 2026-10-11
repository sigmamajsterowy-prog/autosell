package pl.autosell;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class AutoSellMod implements ClientModInitializer {
    private static KeyBinding openKey;
    private static KeyBinding sellKey;

    @Override
    public void onInitializeClient() {
        SellConfig.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autosell.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.autosell"));
        sellKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autosell.sell", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.autosell"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) client.setScreen(new SellScreen());
            while (sellKey.wasPressed()) AutoSeller.sellHeld(client);
            AutoSeller.tick(client);
        });
    }
}
