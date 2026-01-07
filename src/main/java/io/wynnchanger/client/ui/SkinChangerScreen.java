package io.wynnchanger.client.ui;

import io.wynnchanger.client.SkinEntry;
import io.wynnchanger.client.SkinSwapState;
import io.wynnchanger.client.SkinType;
import io.wynnchanger.client.WynnchangerClient;
import io.wynnchanger.client.GlintSupport;
import io.wynnchanger.client.GlintType;
import io.wynnchanger.client.WynnGlint;
import io.wynnchanger.client.model.SkinModelOverride;
import io.wynnchanger.client.model.WynnItemClassifier;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class SkinChangerScreen extends Screen {
    private static final int COLUMNS = 3;
    private static final int ROWS = 5;
    private static final int ITEMS_PER_PAGE = COLUMNS * ROWS;
    private static final int MIN_BUTTON_WIDTH = 170;
    private static final int MAX_BUTTON_WIDTH = 190;
    private static final int MIN_NARROW_BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 60;
    private static final int TYPE_BUTTON_HEIGHT = 20;
    private static final int SEARCH_HEIGHT = 20;
    private static final int CLEAR_BUTTON_HEIGHT = 20;
    private static final int GLINT_PANEL_PADDING = 12;
    private static final int GLINT_PANEL_HEADER_HEIGHT = 22;
    private static final int GLINT_ROW_HEIGHT = 46;
    private static final int GLINT_COLUMN_GAP = 10;
    private static final int GLINT_ROW_GAP = 6;
    private static final int GLINT_ICON_SIZE = 24;
    private static final int GLINT_LABEL_GAP = 4;
    private static final int GLINT_PANEL_BORDER = 1;
    private static final int GLINT_PANEL_HEADER_COLOR = 0xFF181818;
    private static final int GLINT_PANEL_BODY_COLOR = 0xFF181818;
    private static final int GLINT_PANEL_BORDER_COLOR = 0xFF000000;
    private static final int GLINT_SCROLLBAR_WIDTH = 4;
    private static final int GLINT_SCROLLBAR_TRACK = 0xFF101010;
    private static final int GLINT_SCROLLBAR_THUMB = 0xFF3B3B3B;

    private SkinType detectedType = SkinType.UNKNOWN;
    private SkinType selectedType;
    private List<SkinEntry> entries = List.of();
    private int page = 0;
    private Layout layout;
    private final Map<Identifier, Optional<ItemStack>> previewCache = new HashMap<>();
    private TextFieldWidget searchField;
    private String searchText = "";
    private boolean previewDragging;
    private float previewExtraYaw;
    private static final float PREVIEW_DRAG_SENSITIVITY = 1.8f;
    private GlintPicker glintPicker;

    public SkinChangerScreen() {
        super(Text.literal("Wynnchanger"));
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        boolean keepSearchFocus = searchField != null && searchField.isFocused();
        clearChildren();
        glintPicker = null;
        refreshDetectedType();
        SkinType activeType = getActiveType();
        entries = filterEntries(WynnchangerClient.getSkinRegistry().getSkins(activeType));
        clampPage();

        SkinType[] types = {
                SkinType.DAGGER,
                SkinType.SPEAR,
                SkinType.WAND,
                SkinType.BOW,
                SkinType.RELIK,
                SkinType.HAT
        };

        layout = Layout.of(width, height, types.length);

        int searchX = layout.searchX;
        int searchY = layout.searchY;
        searchField = new TextFieldWidget(textRenderer, searchX, searchY, layout.gridTotalWidth, SEARCH_HEIGHT, Text.literal("Search"));
        searchField.setText(searchText);
        searchField.setSuggestion(searchText.isEmpty() ? "Search skins..." : "");
        searchField.setChangedListener(text -> {
            searchText = text;
            page = 0;
            rebuildWidgets();
        });
        searchField.setFocused(keepSearchFocus);
        addDrawableChild(searchField);

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(entries.size(), startIndex + ITEMS_PER_PAGE);
        int buttonIndex = 0;

        for (int i = startIndex; i < endIndex; i++) {
            SkinEntry entry = entries.get(i);
            int row = buttonIndex / COLUMNS;
            int col = buttonIndex % COLUMNS;
            int x = layout.gridStartX + col * (layout.buttonWidth + layout.gridPadding);
            int y = layout.gridStartY + row * (BUTTON_HEIGHT + layout.gridPadding);
            addDrawableChild(buildSkinButton(entry, x, y, layout.buttonWidth, BUTTON_HEIGHT));
            buttonIndex++;
        }

        int typeStartY = layout.typeStartY;
        int typeStartX = layout.typeStartX;

        for (int i = 0; i < types.length; i++) {
            int x = typeStartX;
            int y = typeStartY + i * (TYPE_BUTTON_HEIGHT + layout.typePadding);
            SkinType type = types[i];
            Text label = Text.literal(type.getDisplayName());
            addDrawableChild(ButtonWidget.builder(label, button -> {
                selectedType = type;
                page = 0;
                rebuildWidgets();
            }).dimensions(x, y, layout.sidebarRight - layout.sidebarLeft, TYPE_BUTTON_HEIGHT).build());
        }
        if (entries.size() > ITEMS_PER_PAGE) {
            addDrawableChild(ButtonWidget.builder(Text.literal("< Prev"), button -> {
                page = Math.max(0, page - 1);
                rebuildWidgets();
            }).dimensions(layout.gridStartX, layout.navY, 90, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Next >"), button -> {
                page = Math.min(getMaxPage(), page + 1);
                rebuildWidgets();
            }).dimensions(layout.gridStartX + layout.gridTotalWidth - 90, layout.navY, 90, 20).build());
        }

        int bottomY = height - 30;
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> close())
                .dimensions(width / 2 - 50, bottomY, 100, 20)
                .build());

        int clearY = layout.clearY;
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear All"), button -> {
            WynnchangerClient.getSwapState().clearAllSelections();
            rebuildWidgets();
        }).dimensions(layout.sidebarLeft, clearY, layout.sidebarRight - layout.sidebarLeft, CLEAR_BUTTON_HEIGHT).build());
    }

    private PressableWidget buildSkinButton(SkinEntry entry, int x, int y, int width, int height) {
        return new SkinEntryWidget(entry, x, y, width, height, getPreviewStack(entry.modelId()));
    }

    private Optional<ItemStack> getPreviewStack(Identifier modelId) {
        return previewCache.computeIfAbsent(modelId, SkinModelOverride::buildStackForModel);
    }

    private List<SkinEntry> filterEntries(List<SkinEntry> source) {
        if (searchText == null || searchText.isBlank()) {
            return source;
        }
        String query = searchText.toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(entry -> entry.displayName().toLowerCase(Locale.ROOT).contains(query))
                .toList();
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
        if (layout == null) {
            rebuildWidgets();
        }
        renderBackground(context, mouseX, mouseY, delta);

        if (entries.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("No skins found for this item."),
                    width / 2,
                    layout.gridStartY + layout.emptyMessageOffset,
                    0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);

        renderPreview(context, mouseX, mouseY);
        renderGlintPicker(context, mouseX, mouseY);
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

        int previewHeight = layout.previewBottom - layout.previewTop;
        int previewWidth = layout.previewRight - layout.previewLeft;
        if (previewHeight < 32 || previewWidth < 32) {
            return;
        }

        int size = Math.min(90, Math.max(28, Math.round(Math.min(previewWidth, previewHeight) * 0.45f)));
        float centerX = (layout.previewLeft + layout.previewRight) / 2.0f;
        float centerY = (layout.previewTop + layout.previewBottom) / 2.0f;

        if (!previewDragging) {
            previewExtraYaw = MathHelper.lerp(0.15f, previewExtraYaw, 0.0f);
        }

        float effectiveMouseX = (float) mouseX;
        float effectiveMouseY = (float) mouseY;
        effectiveMouseX = MathHelper.clamp(effectiveMouseX, 0.0f, (float) width);
        effectiveMouseY = MathHelper.clamp(effectiveMouseY, 0.0f, (float) height);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT);
        drawPreviewEntity(context, centerX, centerY, size, effectiveMouseX, effectiveMouseY, previewExtraYaw, client.player);
    }

    private static void drawPreviewEntity(DrawContext context, float centerX, float centerY, int size, float mouseX, float mouseY, float extraYaw, LivingEntity entity) {
        float yawOffset = (float) Math.atan((centerX - mouseX) / 40.0f);
        float pitchOffset = (float) Math.atan((centerY - mouseY) / 40.0f);

        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf pitchRotation = new Quaternionf().rotateX(pitchOffset * 20.0f * ((float) Math.PI / 180.0f));
        rotation.mul(pitchRotation);

        float prevBodyYaw = entity.bodyYaw;
        float prevYaw = entity.getYaw();
        float prevPitch = entity.getPitch();
        float prevHeadYaw = entity.headYaw;
        float prevPrevHeadYaw = entity.prevHeadYaw;

        entity.bodyYaw = 180.0f + yawOffset * 20.0f + extraYaw;
        entity.setYaw(180.0f + yawOffset * 40.0f + extraYaw);
        entity.setPitch(-pitchOffset * 20.0f);
        entity.headYaw = entity.getYaw();
        entity.prevHeadYaw = entity.getYaw();

        float scale = entity.getScale();
        if (scale <= 0.0f) {
            scale = 1.0f;
        }
        float renderScale = size / scale;
        float yOffset = 0.0625f * scale;
        Vector3f translation = new Vector3f(0.0f, entity.getHeight() / 2.0f + yOffset, 0.0f);

        try {
            InventoryScreen.drawEntity(context, centerX, centerY, renderScale, translation, rotation, pitchRotation, entity);
        } finally {
            entity.bodyYaw = prevBodyYaw;
            entity.setYaw(prevYaw);
            entity.setPitch(prevPitch);
            entity.headYaw = prevHeadYaw;
            entity.prevHeadYaw = prevPrevHeadYaw;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (glintPicker != null) {
            if (button == 0) {
                GlintType glint = glintPicker.getOptionAt(mouseX, mouseY);
                if (glint != null) {
                    WynnchangerClient.getSwapState().setGlint(glintPicker.entry.type(), glint);
                    glintPicker = null;
                    return true;
                }
            }
            if (!glintPicker.contains(mouseX, mouseY)) {
                glintPicker = null;
            }
            return true;
        }
        if (button == 0 && isInsidePreview(mouseX, mouseY)) {
            previewDragging = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (glintPicker != null) {
            return true;
        }
        if (previewDragging && button == 0) {
            previewDragging = false;
            previewExtraYaw = MathHelper.wrapDegrees(previewExtraYaw);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (glintPicker != null) {
            return true;
        }
        if (previewDragging && button == 0) {
            previewExtraYaw -= (float) deltaX * PREVIEW_DRAG_SENSITIVITY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (glintPicker != null && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            glintPicker = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (glintPicker != null) {
            if (glintPicker.scrollBy(-(int) Math.round(verticalAmount * 14.0))) {
                return true;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean isInsidePreview(double mouseX, double mouseY) {
        if (layout == null) {
            return false;
        }
        return mouseX >= layout.previewLeft && mouseX <= layout.previewRight
                && mouseY >= layout.previewTop && mouseY <= layout.previewBottom;
    }

    private void openGlintPicker(SkinEntry entry, Optional<ItemStack> previewStack, int mouseX, int mouseY) {
        glintPicker = GlintPicker.create(entry, previewStack, width, height, mouseX, mouseY);
    }

    private void renderGlintPicker(DrawContext context, int mouseX, int mouseY) {
        if (glintPicker == null) {
            return;
        }
        GlintPicker picker = glintPicker;
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(0.0f, 0.0f, 400.0f);

        context.fill(0, 0, width, height, 0xCC000000);
        context.fill(picker.x, picker.y, picker.x + picker.width, picker.y + picker.height, GLINT_PANEL_BODY_COLOR);
        context.fill(picker.x, picker.y, picker.x + picker.width, picker.y + GLINT_PANEL_HEADER_HEIGHT,
                GLINT_PANEL_HEADER_COLOR);
        context.fill(picker.x, picker.y, picker.x + picker.width, picker.y + GLINT_PANEL_BORDER, GLINT_PANEL_BORDER_COLOR);
        context.fill(picker.x, picker.y + picker.height - GLINT_PANEL_BORDER,
                picker.x + picker.width, picker.y + picker.height, GLINT_PANEL_BORDER_COLOR);
        context.fill(picker.x, picker.y, picker.x + GLINT_PANEL_BORDER, picker.y + picker.height, GLINT_PANEL_BORDER_COLOR);
        context.fill(picker.x + picker.width - GLINT_PANEL_BORDER, picker.y,
                picker.x + picker.width, picker.y + picker.height, GLINT_PANEL_BORDER_COLOR);

        Text title = Text.literal("Choose a glint • " + picker.entry.type().getDisplayName());
        context.drawTextWithShadow(textRenderer, title,
                picker.x + GLINT_PANEL_PADDING,
                picker.y + 6,
                0xFFFFFF);

        Text closeHint = Text.literal("Esc to close").formatted(Formatting.GRAY);
        int closeWidth = textRenderer.getWidth(closeHint);
        context.drawTextWithShadow(textRenderer, closeHint,
                picker.x + picker.width - GLINT_PANEL_PADDING - closeWidth,
                picker.y + 6,
                0xAAAAAA);

        int gridLeft = picker.contentLeft;
        int gridRight = picker.contentRight;
        int gridTop = picker.contentTop;
        int gridBottom = picker.contentBottom;
        context.enableScissor(gridLeft, gridTop, gridRight, gridBottom);

        SkinSwapState state = WynnchangerClient.getSwapState();
        GlintType selectedGlint = state.getGlint(picker.entry.type()).orElse(GlintType.NONE);
        for (GlintCell cell : picker.cells) {
            int cellY = cell.y - picker.scrollOffset;
            if (cellY + cell.height < gridTop || cellY > gridBottom) {
                continue;
            }
            boolean hovered = cell.contains(mouseX, mouseY, picker.scrollOffset);
            boolean selected = cell.type == selectedGlint;

            int background = selected ? 0xFF2A2A2A : (hovered ? 0xFF242424 : 0xFF1E1E1E);
            int border = 0xFF0C0C0C;
            context.fill(cell.x, cellY, cell.x + cell.width, cellY + cell.height, background);
            context.fill(cell.x, cellY, cell.x + cell.width, cellY + 1, border);
            context.fill(cell.x, cellY + cell.height - 1, cell.x + cell.width, cellY + cell.height, border);
            context.fill(cell.x, cellY, cell.x + 1, cellY + cell.height, border);
            context.fill(cell.x + cell.width - 1, cellY, cell.x + cell.width, cellY + cell.height, border);

            int iconX = cell.x + (cell.width - GLINT_ICON_SIZE) / 2;
            int iconY = cellY + 6;
            picker.previewStack.ifPresent(stack -> WynnGlint.withPreviewGlint(cell.type, () ->
                    SkinModelOverride.withOverridesSuppressed(() -> context.drawItem(stack, iconX, iconY))));

            int maxLabelWidth = cell.width - 6;
            String label = textRenderer.trimToWidth(cell.type.getDisplayName(), maxLabelWidth);
            int textWidth = textRenderer.getWidth(label);
            int textX = cell.x + (cell.width - textWidth) / 2;
            int textY = iconY + GLINT_ICON_SIZE + GLINT_LABEL_GAP;
            int textColor = hovered ? 0xFFFFFF : 0xDADADA;
            context.drawTextWithShadow(textRenderer, label, textX, textY, textColor);

            if (selected) {
                Text check = Text.literal("✓");
                int checkX = cell.x + cell.width - 10;
                int checkY = cellY + 4;
                context.drawTextWithShadow(textRenderer, check, checkX, checkY, 0xFFFFFF);
            }
        }
        context.disableScissor();

        if (picker.maxScroll > 0) {
            int trackX = picker.scrollbarX;
            int trackY = picker.contentTop;
            int trackH = picker.viewHeight;
            context.fill(trackX, trackY, trackX + GLINT_SCROLLBAR_WIDTH, trackY + trackH, GLINT_SCROLLBAR_TRACK);

            int thumbH = Math.max(18, Math.round(trackH * picker.viewHeight / (float) picker.contentHeight));
            int thumbY = trackY + Math.round((trackH - thumbH) * (picker.scrollOffset / (float) picker.maxScroll));
            context.fill(trackX, thumbY, trackX + GLINT_SCROLLBAR_WIDTH, thumbY + thumbH, GLINT_SCROLLBAR_THUMB);
        }

        if (!GlintSupport.isSupported()) {
            Text warning = Text.literal("Glints not detected in active pack").formatted(Formatting.RED);
            context.drawTextWithShadow(textRenderer,
                    warning,
                    picker.x + GLINT_PANEL_PADDING,
                    picker.y + picker.height - GLINT_PANEL_PADDING - textRenderer.fontHeight,
                    0xFF7777);
        }
        matrices.pop();
    }

    private static int scaleWidth(int width, float ratio, int min, int max) {
        return clamp(Math.round(width * ratio), min, max);
    }

    private static int scaleHeight(int height, float ratio, int min, int max) {
        return clamp(Math.round(height * ratio), min, max);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class GlintPicker {
        private final SkinEntry entry;
        private final Optional<ItemStack> previewStack;
        private final List<GlintCell> cells;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final int contentLeft;
        private final int contentRight;
        private final int contentTop;
        private final int contentBottom;
        private final int contentHeight;
        private final int viewHeight;
        private final int maxScroll;
        private final int scrollbarX;
        private int scrollOffset;

        private GlintPicker(SkinEntry entry, Optional<ItemStack> previewStack, List<GlintCell> cells,
                            int x, int y, int width, int height, int contentLeft, int contentRight,
                            int contentTop, int contentBottom, int contentHeight, int viewHeight,
                            int maxScroll, int scrollbarX) {
            this.entry = entry;
            this.previewStack = previewStack;
            this.cells = cells;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.contentLeft = contentLeft;
            this.contentRight = contentRight;
            this.contentTop = contentTop;
            this.contentBottom = contentBottom;
            this.contentHeight = contentHeight;
            this.viewHeight = viewHeight;
            this.maxScroll = maxScroll;
            this.scrollbarX = scrollbarX;
        }

        private static GlintPicker create(SkinEntry entry, Optional<ItemStack> previewStack,
                                          int screenWidth, int screenHeight, int mouseX, int mouseY) {
            List<GlintType> options = new ArrayList<>(GlintSupport.getAvailableGlints());
            if (options.isEmpty()) {
                options.add(GlintType.NONE);
            }

            int columns = screenWidth < 520 ? 2 : 4;
            columns = Math.min(columns, options.size());
            int rawColumnWidth = (screenWidth - 40 - GLINT_PANEL_PADDING * 2 - GLINT_COLUMN_GAP * (columns - 1)) / columns;
            int columnWidth = Math.max(84, rawColumnWidth);
            int rows = (options.size() + columns - 1) / columns;
            int visibleRows = Math.min(rows, 4);

            int panelWidth = GLINT_PANEL_PADDING * 2 + columns * columnWidth + (columns - 1) * GLINT_COLUMN_GAP;
            int panelHeight = GLINT_PANEL_PADDING * 2 + GLINT_PANEL_HEADER_HEIGHT
                    + visibleRows * GLINT_ROW_HEIGHT + Math.max(0, visibleRows - 1) * GLINT_ROW_GAP;

            int maxWidth = Math.max(0, screenWidth - 20);
            if (panelWidth > maxWidth) {
                columnWidth = Math.max(90, (maxWidth - GLINT_PANEL_PADDING * 2 - GLINT_COLUMN_GAP * (columns - 1)) / columns);
                panelWidth = GLINT_PANEL_PADDING * 2 + columns * columnWidth + (columns - 1) * GLINT_COLUMN_GAP;
            }

            int maxHeight = Math.max(0, screenHeight - 20);
            if (panelHeight > maxHeight) {
                int available = maxHeight - GLINT_PANEL_PADDING * 2 - GLINT_PANEL_HEADER_HEIGHT + GLINT_ROW_GAP;
                visibleRows = Math.max(2, available / (GLINT_ROW_HEIGHT + GLINT_ROW_GAP));
                visibleRows = Math.min(rows, visibleRows);
                panelHeight = GLINT_PANEL_PADDING * 2 + GLINT_PANEL_HEADER_HEIGHT
                        + visibleRows * GLINT_ROW_HEIGHT + Math.max(0, visibleRows - 1) * GLINT_ROW_GAP;
            }

            int x = clamp((screenWidth - panelWidth) / 2, 10, Math.max(10, screenWidth - panelWidth - 10));
            int y = clamp((screenHeight - panelHeight) / 2, 10, Math.max(10, screenHeight - panelHeight - 10));

            int gridX = x + GLINT_PANEL_PADDING;
            int gridY = y + GLINT_PANEL_PADDING + GLINT_PANEL_HEADER_HEIGHT;
            int gridWidth = panelWidth - GLINT_PANEL_PADDING * 2;
            columnWidth = Math.max(84, (gridWidth - (columns - 1) * GLINT_COLUMN_GAP) / columns);
            int viewHeight = visibleRows * GLINT_ROW_HEIGHT + Math.max(0, visibleRows - 1) * GLINT_ROW_GAP;
            int contentHeight = rows * GLINT_ROW_HEIGHT + Math.max(0, rows - 1) * GLINT_ROW_GAP;
            int maxScroll = Math.max(0, contentHeight - viewHeight);

            int contentLeft = gridX;
            int contentRight = x + panelWidth - GLINT_PANEL_PADDING;
            int contentTop = gridY;
            int contentBottom = gridY + viewHeight;
            int scrollbarX = x + panelWidth - GLINT_PANEL_PADDING + (GLINT_PANEL_PADDING - GLINT_SCROLLBAR_WIDTH) / 2;

            List<GlintCell> cells = new ArrayList<>(options.size());
            for (int i = 0; i < options.size(); i++) {
                int col = i % columns;
                int row = i / columns;
                int cellX = gridX + col * (columnWidth + GLINT_COLUMN_GAP);
                int cellY = gridY + row * (GLINT_ROW_HEIGHT + GLINT_ROW_GAP);
                cells.add(new GlintCell(options.get(i), cellX, cellY, columnWidth, GLINT_ROW_HEIGHT));
            }

            return new GlintPicker(entry, previewStack, List.copyOf(cells), x, y, panelWidth, panelHeight,
                    contentLeft, contentRight, contentTop, contentBottom, contentHeight, viewHeight, maxScroll, scrollbarX);
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        private GlintType getOptionAt(double mouseX, double mouseY) {
            for (GlintCell cell : cells) {
                if (cell.contains(mouseX, mouseY, scrollOffset)) {
                    return cell.type;
                }
            }
            return null;
        }

        private boolean scrollBy(int delta) {
            if (maxScroll <= 0) {
                return false;
            }
            int next = clamp(scrollOffset + delta, 0, maxScroll);
            if (next == scrollOffset) {
                return false;
            }
            scrollOffset = next;
            return true;
        }
    }

    private record GlintCell(GlintType type, int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY, int scrollOffset) {
            int adjustedY = y - scrollOffset;
            return mouseX >= x && mouseX <= x + width && mouseY >= adjustedY && mouseY <= adjustedY + height;
        }
    }

    private record Layout(
            int panelLeft,
            int panelRight,
            int panelTop,
            int panelBottom,
            int sidebarLeft,
            int sidebarRight,
            int gridPadding,
            int typePadding,
            int buttonWidth,
            int gridTotalWidth,
            int gridStartX,
            int gridStartY,
            int searchX,
            int searchY,
            int navY,
            int typeStartX,
            int typeStartY,
            int clearY,
            int previewLeft,
            int previewRight,
            int previewTop,
            int previewBottom,
            int emptyMessageOffset
    ) {
        private static Layout of(int width, int height, int typeCount) {
            int panelPadding = scaleWidth(width, 0.02f, 10, 20);
            int panelLeft = panelPadding;
            int panelRight = width - panelPadding;
            int panelTop = panelPadding;
            int panelBottom = height - panelPadding;

            int sidebarWidth = scaleWidth(width, 0.22f, 200, 260);
            int sidebarRightPadding = scaleWidth(width, 0.02f, 16, 28);
            int sidebarLeft = panelLeft;
            int sidebarRight = Math.min(panelLeft + sidebarWidth, panelRight - sidebarRightPadding);

            int gridGap = scaleWidth(width, 0.02f, 12, 20);
            int gridAreaLeft = sidebarRight + gridGap;
            int gridAreaRight = panelRight;
            int gridAreaWidth = Math.max(0, gridAreaRight - gridAreaLeft);
            int gridPadding = scaleWidth(width, 0.008f, 6, 10);
            int maxWidth = (gridAreaWidth - (COLUMNS - 1) * gridPadding) / COLUMNS;
            int buttonWidth = Math.min(MAX_BUTTON_WIDTH, Math.max(MIN_BUTTON_WIDTH, maxWidth));
            if (maxWidth < MIN_BUTTON_WIDTH) {
                buttonWidth = Math.max(MIN_NARROW_BUTTON_WIDTH, maxWidth);
            }
            int gridTotalWidth = COLUMNS * buttonWidth + (COLUMNS - 1) * gridPadding;
            int gridStartX = gridAreaLeft + Math.max(0, (gridAreaWidth - gridTotalWidth) / 2);

            int searchTopOffset = scaleHeight(height, 0.035f, 18, 28);
            int searchBottomGap = scaleHeight(height, 0.02f, 10, 16);
            int searchX = gridStartX;
            int searchY = panelTop + searchTopOffset;
            int gridStartY = searchY + SEARCH_HEIGHT + searchBottomGap;

            int navGap = scaleHeight(height, 0.01f, 4, 8);
            int navY = gridStartY + ROWS * (BUTTON_HEIGHT + gridPadding) + navGap;

            int typeStartY = searchY;
            int typeStartX = sidebarLeft;
            int typePadding = scaleHeight(height, 0.01f, 6, 10);
            int typeListHeight = typeCount * (TYPE_BUTTON_HEIGHT + typePadding) - typePadding;

            int clearGap = scaleHeight(height, 0.012f, 6, 12);
            int clearY = typeStartY + typeListHeight + clearGap;

            int previewTopGap = scaleHeight(height, 0.03f, 18, 30);
            int previewDrop = scaleHeight(height, 0.05f, 24, 40);
            int previewBottomPadding = scaleHeight(height, 0.008f, 2, 6);
            int previewTop = clearY + CLEAR_BUTTON_HEIGHT + previewTopGap + previewDrop;
            int previewBottom = panelBottom - previewBottomPadding;
            int previewLeft = sidebarLeft;
            int previewRight = sidebarRight;

            int emptyMessageOffset = Math.round(BUTTON_HEIGHT * 0.45f);

            return new Layout(
                    panelLeft,
                    panelRight,
                    panelTop,
                    panelBottom,
                    sidebarLeft,
                    sidebarRight,
                    gridPadding,
                    typePadding,
                    buttonWidth,
                    gridTotalWidth,
                    gridStartX,
                    gridStartY,
                    searchX,
                    searchY,
                    navY,
                    typeStartX,
                    typeStartY,
                    clearY,
                    previewLeft,
                    previewRight,
                    previewTop,
                    previewBottom,
                    emptyMessageOffset
            );
        }
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
            SkinSwapState swapState = WynnchangerClient.getSwapState();
            if (isSelected(swapState)) {
                swapState.clearSelection(entry.type());
            } else {
                swapState.setSelection(entry.type(), entry.modelId());
            }
            rebuildWidgets();
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            SkinSwapState state = WynnchangerClient.getSwapState();
            boolean selected = isSelected(state);
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
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 1 && isMouseOver(mouseX, mouseY)) {
                openGlintPicker(entry, previewStack, (int) mouseX, (int) mouseY);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        private boolean isSelected(SkinSwapState state) {
            return state.getSelection(entry.type()).filter(entry.modelId()::equals).isPresent();
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }

}
