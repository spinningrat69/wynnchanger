package io.wynnchanger.client.ui;

import io.wynnchanger.client.GlintSupport;
import io.wynnchanger.client.GlintType;
import io.wynnchanger.client.SkinEntry;
import io.wynnchanger.client.SkinSwapState;
import io.wynnchanger.client.WynnGlint;
import io.wynnchanger.client.WynnchangerClient;
import io.wynnchanger.client.model.SkinModelOverride;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GlintPickerWidget {
    private static final int PANEL_PADDING = 12;
    private static final int PANEL_HEADER_HEIGHT = 22;
    private static final int ROW_HEIGHT = 76;
    private static final int COLUMN_GAP = 10;
    private static final int ROW_GAP = 6;
    private static final int ICON_SIZE = 40;
    private static final int LABEL_GAP = 8;
    private static final int PANEL_BORDER = 1;
    private static final int PANEL_HEADER_COLOR = 0xFF181818;
    private static final int PANEL_BODY_COLOR = 0xFF181818;
    private static final int PANEL_BORDER_COLOR = 0xFF000000;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_TRACK = 0xFF101010;
    private static final int SCROLLBAR_THUMB = 0xFF3B3B3B;
    private static final int SCROLL_STEP = 14;

    private final SkinEntry entry;
    private final Optional<ItemStack> previewStack;
    private final List<GlintCell> cells;
    private final int screenWidth;
    private final int screenHeight;
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
    private boolean draggingScrollbar;
    private int dragOffsetY;

    private GlintPickerWidget(SkinEntry entry, Optional<ItemStack> previewStack, List<GlintCell> cells,
                              int screenWidth, int screenHeight, int x, int y, int width, int height,
                              int contentLeft, int contentRight, int contentTop, int contentBottom,
                              int contentHeight, int viewHeight, int maxScroll, int scrollbarX) {
        this.entry = entry;
        this.previewStack = previewStack;
        this.cells = cells;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
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

    public static GlintPickerWidget create(SkinEntry entry, Optional<ItemStack> previewStack, int screenWidth, int screenHeight) {
        List<GlintType> options = new ArrayList<>(GlintSupport.getAvailableGlints());
        if (options.isEmpty()) {
            options.add(GlintType.NONE);
        }

        int columns = screenWidth < 520 ? 2 : 4;
        columns = Math.min(columns, options.size());
        int rawColumnWidth = (screenWidth - 40 - PANEL_PADDING * 2 - COLUMN_GAP * (columns - 1)) / columns;
        int columnWidth = Math.max(110, rawColumnWidth);
        int rows = (options.size() + columns - 1) / columns;
        int visibleRows = Math.min(rows, 4);

        int panelWidth = PANEL_PADDING * 2 + columns * columnWidth + (columns - 1) * COLUMN_GAP;
        int panelHeight = PANEL_PADDING * 2 + PANEL_HEADER_HEIGHT
                + visibleRows * ROW_HEIGHT + Math.max(0, visibleRows - 1) * ROW_GAP;

        int maxWidth = Math.max(0, screenWidth - 20);
        if (panelWidth > maxWidth) {
            columnWidth = Math.max(96, (maxWidth - PANEL_PADDING * 2 - COLUMN_GAP * (columns - 1)) / columns);
            panelWidth = PANEL_PADDING * 2 + columns * columnWidth + (columns - 1) * COLUMN_GAP;
        }

        int maxHeight = Math.max(0, screenHeight - 20);
        if (panelHeight > maxHeight) {
            int available = maxHeight - PANEL_PADDING * 2 - PANEL_HEADER_HEIGHT + ROW_GAP;
            visibleRows = Math.max(2, available / (ROW_HEIGHT + ROW_GAP));
            visibleRows = Math.min(rows, visibleRows);
            panelHeight = PANEL_PADDING * 2 + PANEL_HEADER_HEIGHT
                    + visibleRows * ROW_HEIGHT + Math.max(0, visibleRows - 1) * ROW_GAP;
        }

        int x = clamp((screenWidth - panelWidth) / 2, 10, Math.max(10, screenWidth - panelWidth - 10));
        int y = clamp((screenHeight - panelHeight) / 2, 10, Math.max(10, screenHeight - panelHeight - 10));

        int gridX = x + PANEL_PADDING;
        int gridY = y + PANEL_PADDING + PANEL_HEADER_HEIGHT;
        int gridWidth = panelWidth - PANEL_PADDING * 2;
        columnWidth = Math.max(96, (gridWidth - (columns - 1) * COLUMN_GAP) / columns);
        int viewHeight = visibleRows * ROW_HEIGHT + Math.max(0, visibleRows - 1) * ROW_GAP;
        int contentHeight = rows * ROW_HEIGHT + Math.max(0, rows - 1) * ROW_GAP;
        int maxScroll = Math.max(0, contentHeight - viewHeight);

        int contentLeft = gridX;
        int contentRight = x + panelWidth - PANEL_PADDING;
        int contentTop = gridY;
        int contentBottom = gridY + viewHeight;
        int scrollbarX = x + panelWidth - PANEL_PADDING + (PANEL_PADDING - SCROLLBAR_WIDTH) / 2;

        List<GlintCell> cells = new ArrayList<>(options.size());
        for (int i = 0; i < options.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int cellX = gridX + col * (columnWidth + COLUMN_GAP);
            int cellY = gridY + row * (ROW_HEIGHT + ROW_GAP);
            cells.add(new GlintCell(options.get(i), cellX, cellY, columnWidth, ROW_HEIGHT));
        }

        return new GlintPickerWidget(entry, previewStack, List.copyOf(cells),
                screenWidth, screenHeight, x, y, panelWidth, panelHeight,
                contentLeft, contentRight, contentTop, contentBottom,
                contentHeight, viewHeight, maxScroll, scrollbarX);
    }

    public SkinEntry getEntry() {
        return entry;
    }

    public void render(DrawContext context, int mouseX, int mouseY, TextRenderer textRenderer) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(0.0f, 0.0f, 400.0f);

        context.fill(0, 0, screenWidth, screenHeight, 0xCC000000);
        context.fill(x, y, x + width, y + height, PANEL_BODY_COLOR);
        context.fill(x, y, x + width, y + PANEL_HEADER_HEIGHT, PANEL_HEADER_COLOR);
        context.fill(x, y, x + width, y + PANEL_BORDER, PANEL_BORDER_COLOR);
        context.fill(x, y + height - PANEL_BORDER, x + width, y + height, PANEL_BORDER_COLOR);
        context.fill(x, y, x + PANEL_BORDER, y + height, PANEL_BORDER_COLOR);
        context.fill(x + width - PANEL_BORDER, y, x + width, y + height, PANEL_BORDER_COLOR);

        Text title = Text.literal("Choose a glint - " + entry.type().getDisplayName());
        context.drawTextWithShadow(textRenderer, title, x + PANEL_PADDING, y + 6, 0xFFFFFF);

        Text closeHint = Text.literal("Esc to close").formatted(Formatting.GRAY);
        int closeWidth = textRenderer.getWidth(closeHint);
        context.drawTextWithShadow(textRenderer, closeHint,
                x + width - PANEL_PADDING - closeWidth,
                y + 6,
                0xAAAAAA);

        context.enableScissor(contentLeft, contentTop, contentRight, contentBottom);

        SkinSwapState state = WynnchangerClient.getSwapState();
        GlintType selectedGlint = state.getGlint(entry.type()).orElse(GlintType.NONE);
        for (GlintCell cell : cells) {
            int cellY = cell.y - scrollOffset;
            if (cellY + cell.height < contentTop || cellY > contentBottom) {
                continue;
            }
            boolean hovered = cell.contains(mouseX, mouseY, scrollOffset);
            boolean selected = cell.type == selectedGlint;

            int background = selected ? 0xFF2A2A2A : (hovered ? 0xFF242424 : 0xFF1E1E1E);
            int border = 0xFF0C0C0C;
            context.fill(cell.x, cellY, cell.x + cell.width, cellY + cell.height, background);
            context.fill(cell.x, cellY, cell.x + cell.width, cellY + 1, border);
            context.fill(cell.x, cellY + cell.height - 1, cell.x + cell.width, cellY + cell.height, border);
            context.fill(cell.x, cellY, cell.x + 1, cellY + cell.height, border);
            context.fill(cell.x + cell.width - 1, cellY, cell.x + cell.width, cellY + cell.height, border);

            int contentHeight = ICON_SIZE + LABEL_GAP + textRenderer.fontHeight;
            int iconX = cell.x + (cell.width - ICON_SIZE) / 2;
            int iconY = cellY + Math.max(6, (cell.height - contentHeight) / 2);
            previewStack.ifPresent(stack -> WynnGlint.withPreviewGlint(cell.type, () ->
                    SkinModelOverride.withOverridesSuppressed(() -> {
                        MatrixStack itemMatrices = context.getMatrices();
                        float scale = ICON_SIZE / 16.0f;
                        itemMatrices.push();
                        itemMatrices.translate(iconX, iconY, 0.0f);
                        itemMatrices.scale(scale, scale, 1.0f);
                        context.drawItem(stack, 0, 0);
                        itemMatrices.pop();
                    })));

            int maxLabelWidth = cell.width - 8;
            String label = textRenderer.trimToWidth(cell.type.getDisplayName(), maxLabelWidth);
            int textWidth = textRenderer.getWidth(label);
            int textX = cell.x + (cell.width - textWidth) / 2;
            int textY = iconY + ICON_SIZE + LABEL_GAP;
            int textColor = hovered ? 0xFFFFFF : 0xDADADA;
            context.drawTextWithShadow(textRenderer, label, textX, textY, textColor);

            if (selected) {
                Text check = Text.literal("v");
                int checkX = cell.x + cell.width - 10;
                int checkY = cellY + 4;
                context.drawTextWithShadow(textRenderer, check, checkX, checkY, 0xFFFFFF);
            }
        }
        context.disableScissor();

        if (maxScroll > 0) {
            int trackX = scrollbarX;
            int trackY = contentTop;
            int trackH = viewHeight;
            context.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackH, SCROLLBAR_TRACK);

            int thumbH = getThumbHeight();
            int thumbY = getThumbY(thumbH);
            context.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbH, SCROLLBAR_THUMB);
        }

        if (!GlintSupport.isSupported()) {
            Text warning = Text.literal("Glints not detected in active pack").formatted(Formatting.RED);
            context.drawTextWithShadow(textRenderer,
                    warning,
                    x + PANEL_PADDING,
                    y + height - PANEL_PADDING - textRenderer.fontHeight,
                    0xFF7777);
        }
        matrices.pop();
    }

    public Optional<GlintType> mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return Optional.empty();
        }
        if (startScrollbarDrag(mouseX, mouseY)) {
            return Optional.empty();
        }
        return getOptionAt(mouseX, mouseY);
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button != 0 || !draggingScrollbar || maxScroll <= 0) {
            return false;
        }
        int trackY = contentTop;
        int trackHeight = viewHeight;
        int thumbHeight = getThumbHeight();
        int maxThumbY = trackY + trackHeight - thumbHeight;
        int nextThumbY = clamp((int) Math.round(mouseY) - dragOffsetY, trackY, maxThumbY);
        if (maxThumbY <= trackY) {
            scrollOffset = 0;
            return true;
        }
        int nextScroll = Math.round(((nextThumbY - trackY) / (float) (maxThumbY - trackY)) * maxScroll);
        scrollOffset = clamp(nextScroll, 0, maxScroll);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (maxScroll <= 0) {
            return false;
        }
        int next = clamp(scrollOffset - (int) Math.round(verticalAmount * SCROLL_STEP), 0, maxScroll);
        if (next == scrollOffset) {
            return false;
        }
        scrollOffset = next;
        return true;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private Optional<GlintType> getOptionAt(double mouseX, double mouseY) {
        for (GlintCell cell : cells) {
            if (cell.contains(mouseX, mouseY, scrollOffset)) {
                return Optional.of(cell.type);
            }
        }
        return Optional.empty();
    }

    private boolean startScrollbarDrag(double mouseX, double mouseY) {
        if (maxScroll <= 0) {
            return false;
        }
        int thumbHeight = getThumbHeight();
        int thumbY = getThumbY(thumbHeight);
        if (mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH
                && mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
            draggingScrollbar = true;
            dragOffsetY = (int) Math.round(mouseY) - thumbY;
            return true;
        }
        return false;
    }

    private int getThumbHeight() {
        if (maxScroll <= 0) {
            return viewHeight;
        }
        return Math.max(18, Math.round(viewHeight * viewHeight / (float) contentHeight));
    }

    private int getThumbY(int thumbHeight) {
        if (maxScroll <= 0) {
            return contentTop;
        }
        int trackHeight = viewHeight - thumbHeight;
        if (trackHeight <= 0) {
            return contentTop;
        }
        return contentTop + Math.round(trackHeight * (scrollOffset / (float) maxScroll));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record GlintCell(GlintType type, int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY, int scrollOffset) {
            int adjustedY = y - scrollOffset;
            return mouseX >= x && mouseX <= x + width && mouseY >= adjustedY && mouseY <= adjustedY + height;
        }
    }
}
