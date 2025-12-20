package io.wynnchanger.client.ui;

import io.wynnchanger.client.SkinEntry;
import io.wynnchanger.client.SkinSwapState;
import io.wynnchanger.client.SkinType;
import io.wynnchanger.client.WynnchangerClient;
import io.wynnchanger.client.model.SkinModelOverride;
import io.wynnchanger.client.model.WynnItemClassifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SkinChangerScreen extends Screen {
    private static final int COLUMNS = 3;
    private static final int ROWS = 5;
    private static final int ITEMS_PER_PAGE = COLUMNS * ROWS;
    private static final int MIN_BUTTON_WIDTH = 170;
    private static final int MAX_BUTTON_WIDTH = 190;
    private static final int BUTTON_HEIGHT = 60;
    private static final int GRID_PADDING = 8;
    private static final int TYPE_BUTTON_HEIGHT = 20;
    private static final int TYPE_PADDING = 6;
    private static final int PANEL_PADDING = 12;
    private static final int SIDEBAR_WIDTH = 220;
    private static final int GRID_GAP = 16;
    private static final int GRID_TOP_OFFSET = 64;
    private static final int TYPE_TOP_OFFSET = 48;
    private static final int PREVIEW_TOP_GAP = 40;
    private static final int PREVIEW_BOTTOM_PADDING = 2;
    private static final int SIDEBAR_RIGHT_PADDING = 24;
    private static final int NAV_Y_GAP = 6;
    private static final int TITLE_Y = 20;
    private static final int EMPTY_MESSAGE_OFFSET = 26;

    private SkinType detectedType = SkinType.UNKNOWN;
    private SkinType selectedType;
    private List<SkinEntry> entries = List.of();
    private int page = 0;
    private int gridStartY = 200;
    private int gridStartX = 0;
    private int gridTotalWidth = 0;
    private int buttonWidth = MIN_BUTTON_WIDTH;
    private int navY = 0;
    private int typeStartX = 0;
    private int typeStartY = 0;
    private int panelLeft = 0;
    private int panelRight = 0;
    private int panelTop = 0;
    private int panelBottom = 0;
    private int sidebarLeft = 0;
    private int sidebarRight = 0;
    private int previewLeft = 0;
    private int previewRight = 0;
    private int previewTop = 0;
    private int previewBottom = 0;
    private final Map<Identifier, Optional<ItemStack>> previewCache = new HashMap<>();

    public SkinChangerScreen() {
        super(Text.literal("Wynnchanger"));
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        clearChildren();
        refreshDetectedType();
        SkinType activeType = getActiveType();
        entries = WynnchangerClient.getSkinRegistry().getSkins(activeType);
        clampPage();

        panelLeft = PANEL_PADDING;
        panelRight = width - PANEL_PADDING;
        panelTop = PANEL_PADDING;
        panelBottom = height - PANEL_PADDING;

        sidebarLeft = panelLeft;
        sidebarRight = Math.min(panelLeft + SIDEBAR_WIDTH, panelRight - SIDEBAR_RIGHT_PADDING);

        int gridAreaLeft = sidebarRight + GRID_GAP;
        int gridAreaRight = panelRight;
        int gridAreaWidth = Math.max(0, gridAreaRight - gridAreaLeft);
        int maxWidth = (gridAreaWidth - (COLUMNS - 1) * GRID_PADDING) / COLUMNS;
        buttonWidth = Math.min(MAX_BUTTON_WIDTH, Math.max(MIN_BUTTON_WIDTH, maxWidth));
        if (maxWidth < MIN_BUTTON_WIDTH) {
            buttonWidth = Math.max(120, maxWidth);
        }
        gridTotalWidth = COLUMNS * buttonWidth + (COLUMNS - 1) * GRID_PADDING;
        gridStartX = gridAreaLeft + Math.max(0, (gridAreaWidth - gridTotalWidth) / 2);
        gridStartY = panelTop + GRID_TOP_OFFSET;

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(entries.size(), startIndex + ITEMS_PER_PAGE);
        int buttonIndex = 0;

        for (int i = startIndex; i < endIndex; i++) {
            SkinEntry entry = entries.get(i);
            int row = buttonIndex / COLUMNS;
            int col = buttonIndex % COLUMNS;
            int x = gridStartX + col * (buttonWidth + GRID_PADDING);
            int y = gridStartY + row * (BUTTON_HEIGHT + GRID_PADDING);
            addDrawableChild(buildSkinButton(entry, x, y, buttonWidth, BUTTON_HEIGHT));
            buttonIndex++;
        }

        navY = gridStartY + ROWS * (BUTTON_HEIGHT + GRID_PADDING) + NAV_Y_GAP;

        typeStartY = panelTop + TYPE_TOP_OFFSET;
        typeStartX = sidebarLeft;
        SkinType[] types = {
                SkinType.DAGGER,
                SkinType.SPEAR,
                SkinType.WAND,
                SkinType.BOW,
                SkinType.RELIK,
                SkinType.HAT
        };

        for (int i = 0; i < types.length; i++) {
            int x = typeStartX;
            int y = typeStartY + i * (TYPE_BUTTON_HEIGHT + TYPE_PADDING);
            SkinType type = types[i];
            Text label = Text.literal(type.getDisplayName());
            addDrawableChild(ButtonWidget.builder(label, button -> {
                selectedType = type;
                page = 0;
                rebuildWidgets();
            }).dimensions(x, y, sidebarRight - sidebarLeft, TYPE_BUTTON_HEIGHT).build());
        }
        if (entries.size() > ITEMS_PER_PAGE) {
            addDrawableChild(ButtonWidget.builder(Text.literal("< Prev"), button -> {
                page = Math.max(0, page - 1);
                rebuildWidgets();
            }).dimensions(gridStartX, navY, 90, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Next >"), button -> {
                page = Math.min(getMaxPage(), page + 1);
                rebuildWidgets();
            }).dimensions(gridStartX + gridTotalWidth - 90, navY, 90, 20).build());
        }

        int bottomY = height - 30;
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> close())
                .dimensions(width / 2 - 50, bottomY, 100, 20)
                .build());

        int typeListHeight = types.length * (TYPE_BUTTON_HEIGHT + TYPE_PADDING) - TYPE_PADDING;
        previewTop = typeStartY + typeListHeight + PREVIEW_TOP_GAP;
        previewBottom = panelBottom - PREVIEW_BOTTOM_PADDING;
        previewLeft = sidebarLeft;
        previewRight = sidebarRight;
    }

    private PressableWidget buildSkinButton(SkinEntry entry, int x, int y, int width, int height) {
        return new SkinEntryWidget(entry, x, y, width, height, getPreviewStack(entry.modelId()));
    }

    private Optional<ItemStack> getPreviewStack(Identifier modelId) {
        return previewCache.computeIfAbsent(modelId, SkinModelOverride::buildStackForModel);
    }

    private void refreshDetectedType() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            detectedType = WynnItemClassifier.classify(client.player.getMainHandStack());
        } else {
            detectedType = SkinType.UNKNOWN;
        }
    }

    private SkinType getActiveType() {
        return selectedType != null ? selectedType : detectedType;
    }

    private void clampPage() {
        if (page < 0) {
            page = 0;
        }
        int maxPage = getMaxPage();
        if (page > maxPage) {
            page = maxPage;
        }
    }

    private int getMaxPage() {
        return Math.max(0, (entries.size() - 1) / ITEMS_PER_PAGE);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, TITLE_Y, 0xFFFFFF);

        if (entries.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("No skins found for this item."),
                    width / 2,
                    gridStartY + EMPTY_MESSAGE_OFFSET,
                    0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);

        renderPreview(context, mouseX, mouseY);
    }

    @Override
    public void close() {
        WynnchangerClient.getSwapState().saveIfDirty();
        super.close();
    }

    private void renderPreview(DrawContext context, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        int previewHeight = previewBottom - previewTop;
        if (previewHeight < 32) {
            return;
        }

        int previewWidth = previewRight - previewLeft;
        int size = Math.max(28, Math.min(previewWidth, previewHeight) / 2);
        InventoryScreen.drawEntity(context, previewLeft, previewTop, previewRight, previewBottom, size, 0.0F, mouseX, mouseY, client.player);
    }

    private final class SkinEntryWidget extends PressableWidget {
        private final SkinEntry entry;
        private final Optional<ItemStack> previewStack;

        private SkinEntryWidget(SkinEntry entry, int x, int y, int width, int height, Optional<ItemStack> previewStack) {
            super(x, y, width, height, Text.literal(entry.displayName()));
            this.entry = entry;
            this.previewStack = previewStack;
        }

        @Override
        public void onPress() {
            WynnchangerClient.getSwapState().setSelection(entry.type(), entry.modelId());
            rebuildWidgets();
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            SkinSwapState state = WynnchangerClient.getSwapState();
            boolean selected = state.getSelection(entry.type()).filter(entry.modelId()::equals).isPresent();
            int background = selected ? 0xFF2E6D3A : (isHovered() ? 0xFF2D2D2D : 0xFF1F1F1F);
            int border = selected ? 0xFF6BD88A : 0xFF000000;

            int x = getX();
            int y = getY();
            context.fill(x, y, x + width, y + height, background);
            context.fill(x, y, x + width, y + 1, border);
            context.fill(x, y + height - 1, x + width, y + height, border);
            context.fill(x, y, x + 1, y + height, border);
            context.fill(x + width - 1, y, x + width, y + height, border);

            float iconScale = Math.min(2.2f, (height - 12) / 16.0f);
            int iconSize = Math.round(16 * iconScale);
            int iconX = x + 8;
            int iconY = y + (height - iconSize) / 2;
            previewStack.ifPresent(stack -> {
                MatrixStack matrices = context.getMatrices();
                matrices.push();
                matrices.translate(iconX, iconY, 0.0f);
                matrices.scale(iconScale, iconScale, 1.0f);
                SkinModelOverride.withOverridesSuppressed(() -> context.drawItem(stack, 0, 0));
                matrices.pop();
            });

            Text label = selected
                    ? Text.literal("✓ " + entry.displayName()).formatted(Formatting.GREEN)
                    : Text.literal(entry.displayName());
            int textX = iconX + iconSize + 10;
            int textY = y + (height - textRenderer.fontHeight) / 2;
            context.drawTextWithShadow(textRenderer, label, textX, textY, 0xFFFFFF);
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }

}
