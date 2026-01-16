package io.wynnchanger.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import io.wynnchanger.client.render.WynnHatFeatureRenderer;
import io.wynnchanger.client.ui.SkinChangerScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class WynnchangerClient implements ClientModInitializer {
    public static final String MOD_ID = "wynnchanger";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final SkinRegistry SKIN_REGISTRY = new SkinRegistry();
    private static final SkinSwapState SWAP_STATE = new SkinSwapState();

    private static KeyBinding openGuiKey;
    private static boolean openGuiRequested;

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
        initCommands();
        initRenderers();

        LOGGER.info("Wynnchanger client initialized.");
    }

    private void initRegistry() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(SKIN_REGISTRY);
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new GlintSupport());
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
                requestOpenGui();
            }
            openGuiIfRequested(client);
        });
    }

    private void initCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("wynnchanger")
                        .executes(context -> {
                            requestOpenGui();
                            return 1;
                        })
                        .then(ClientCommandManager.literal("setglint")
                                .then(ClientCommandManager.argument("type", StringArgumentType.word())
                                        .suggests((ctx, builder) -> CommandSource.suggestMatching(SkinType.getCommandNames(), builder))
                                        .then(ClientCommandManager.argument("glint", StringArgumentType.word())
                                                .suggests((ctx, builder) -> CommandSource.suggestMatching(GlintType.getCommandNames(), builder))
                                                .executes(context -> {
                                                    String typeInput = StringArgumentType.getString(context, "type");
                                                    String glintInput = StringArgumentType.getString(context, "glint");
                                                    return handleSetGlint(context.getSource(), typeInput, glintInput);
                                                }))))
                        .then(ClientCommandManager.literal("setskin")
                                .then(ClientCommandManager.argument("type", StringArgumentType.word())
                                        .suggests((ctx, builder) -> CommandSource.suggestMatching(SkinType.getCommandNames(), builder))
                                        .then(ClientCommandManager.argument("skin", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    String typeInput = StringArgumentType.getString(context, "type");
                                                    String skinInput = StringArgumentType.getString(context, "skin");
                                                    return handleSetSkin(context.getSource(), typeInput, skinInput);
                                                }))))
                        .then(ClientCommandManager.literal("setvisibility")
                                .then(ClientCommandManager.argument("type", StringArgumentType.word())
                                        .suggests((ctx, builder) -> CommandSource.suggestMatching(SkinType.getArmorCommandNames(), builder))
                                        .then(ClientCommandManager.argument("visible", BoolArgumentType.bool())
                                                .executes(context -> {
                                                    String typeInput = StringArgumentType.getString(context, "type");
                                                    boolean visible = BoolArgumentType.getBool(context, "visible");
                                                    return handleSetVisibility(context.getSource(), typeInput, visible);
                                                }))))
        ));
    }

    private static int handleSetGlint(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
                                      String typeInput, String glintInput) {
        SkinType type = SkinType.fromCommand(typeInput);
        if (type == null || type == SkinType.UNKNOWN) {
            source.sendError(Text.literal("Unknown type: " + typeInput));
            return 0;
        }
        GlintType glint = GlintType.fromName(glintInput);
        if (glint == null) {
            source.sendError(Text.literal("Unknown glint: " + glintInput));
            return 0;
        }
        SWAP_STATE.setGlint(type, glint);
        SWAP_STATE.saveIfDirty();
        source.sendFeedback(Text.literal("Set " + type.getDisplayName() + " glint to " + glint.getDisplayName()));
        return 1;
    }

    private static int handleSetSkin(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
                                     String typeInput, String skinInput) {
        SkinType type = SkinType.fromCommand(typeInput);
        if (type == null || type == SkinType.UNKNOWN) {
            source.sendError(Text.literal("Unknown type: " + typeInput));
            return 0;
        }
        if (skinInput == null || skinInput.isBlank()) {
            source.sendError(Text.literal("Skin name required."));
            return 0;
        }
        String trimmed = skinInput.trim();
        if (trimmed.equalsIgnoreCase("none") || trimmed.equalsIgnoreCase("clear") || trimmed.equalsIgnoreCase("off")) {
            SWAP_STATE.clearSelection(type);
            SWAP_STATE.saveIfDirty();
            source.sendFeedback(Text.literal("Cleared " + type.getDisplayName() + " skin."));
            return 1;
        }

        Optional<SkinEntry> selection = resolveSkin(type, trimmed);
        if (selection.isEmpty()) {
            source.sendError(Text.literal("Skin not found: " + trimmed));
            return 0;
        }
        SWAP_STATE.setSelection(type, selection.get().modelId());
        SWAP_STATE.saveIfDirty();
        source.sendFeedback(Text.literal("Set " + type.getDisplayName() + " skin to " + selection.get().displayName()));
        return 1;
    }

    private static int handleSetVisibility(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
                                           String typeInput, boolean visible) {
        SkinType type = SkinType.fromCommand(typeInput);
        if (type == null || !type.isArmorType()) {
            source.sendError(Text.literal("Unknown armor type: " + typeInput));
            return 0;
        }
        SWAP_STATE.setVisibility(type, visible);
        SWAP_STATE.saveIfDirty();
        if (visible) {
            source.sendFeedback(Text.literal("Showing " + type.getDisplayName() + " model."));
        } else {
            source.sendFeedback(Text.literal("Hiding " + type.getDisplayName() + " model."));
        }
        return 1;
    }

    private static Optional<SkinEntry> resolveSkin(SkinType type, String input) {
        List<SkinEntry> skins = SKIN_REGISTRY.getSkins(type);
        String normalized = normalizeName(input);

        for (SkinEntry entry : skins) {
            if (normalizeName(entry.displayName()).equals(normalized)) {
                return Optional.of(entry);
            }
            if (normalizeName(entry.modelId().toString()).equals(normalized)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    private static String normalizeName(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }

    private static void requestOpenGui() {
        openGuiRequested = true;
    }

    private static void openGuiIfRequested(MinecraftClient client) {
        if (!openGuiRequested) {
            return;
        }
        openGuiRequested = false;
        if (client.player == null) {
            return;
        }
        client.setScreen(new SkinChangerScreen());
    }

    private void initRenderers() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                registrationHelper.register(new WynnHatFeatureRenderer(playerRenderer));
            }
        });
    }
}
