package io.wynnchanger.client;

import com.mojang.logging.LogUtils;
import io.wynnchanger.client.render.WynnHatFeatureRenderer;
import io.wynnchanger.client.ui.SkinChangerScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourceType;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

public class WynnchangerClient implements ClientModInitializer {
    public static final String MOD_ID = "wynnchanger";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final SkinRegistry SKIN_REGISTRY = new SkinRegistry();
    private static final SkinSwapState SWAP_STATE = new SkinSwapState();

    private static KeyBinding openGuiKey;

    public static SkinRegistry getSkinRegistry() {
        return SKIN_REGISTRY;
    }

    public static SkinSwapState getSwapState() {
        return SWAP_STATE;
    }

    @Override
    public void onInitializeClient() {
        initRegistry();
        initState();
        initKeybinds();
        initRenderers();

        LOGGER.info("Wynnchanger client initialized.");
    }

    private void initRegistry() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(SKIN_REGISTRY);
    }

    private void initState() {
        SWAP_STATE.setConfigPath(FabricLoader.getInstance().getConfigDir().resolve("wynnchanger.json"));
        SWAP_STATE.load();
    }

    private void initKeybinds() {
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.wynnchanger.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "category.wynnchanger"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.player == null) {
                    return;
                }
                client.setScreen(new SkinChangerScreen());
            }
        });
    }

    private void initRenderers() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                registrationHelper.register(new WynnHatFeatureRenderer(playerRenderer));
            }
        });
    }
}
