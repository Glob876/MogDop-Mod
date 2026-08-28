package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.client.PlayerBlockHistoryManager;
import com.mogdop.mod.network.FillAreaPayload;
import com.mogdop.mod.network.OutlinePayload;
import com.mogdop.mod.network.WallsPayload;
import dev.architectury.networking.NetworkManager;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class BlockSelectorScreen extends BaseOwoScreen<FlowLayout> {

    public enum TargetAction {
        FILL("mogdops-mod.block_selector.title_fill", "mogdops-mod.block_selector.btn_fill"),
        WALLS("mogdops-mod.block_selector.title_walls", "mogdops-mod.block_selector.btn_walls"),
        OUTLINE("mogdops-mod.block_selector.title_outline", "mogdops-mod.block_selector.btn_outline");

        final String titleKey;
        final String btnKey;

        TargetAction(String titleKey, String btnKey) {
            this.titleKey = titleKey;
            this.btnKey = btnKey;
        }
    }

    private final TargetAction targetAction;
    private String activeBlockId = Registries.BLOCK.getId(MogDopSModClient.activeBlock).toString();

    private TextBoxComponent activeBox;
    private FlowLayout activeIconWrapper;
    private FlowLayout historyRowWrapper;

    private String gridSearch = "";
    private FlowLayout gridContent;
    private final List<Block> allBlocks = new ArrayList<>();

    public BlockSelectorScreen() {
        this(TargetAction.FILL);
    }

    public BlockSelectorScreen(TargetAction targetAction) {
        this.targetAction = targetAction;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.surface(Surface.flat(0xCC141414));
        rootComponent.horizontalAlignment(HorizontalAlignment.CENTER);
        rootComponent.verticalAlignment(VerticalAlignment.CENTER);
        rootComponent.padding(Insets.of(15));

        allBlocks.clear();
        for (Block block : Registries.BLOCK) {
            if (block != Blocks.AIR && block.asItem() != net.minecraft.item.Items.AIR) {
                allBlocks.add(block);
            }
        }

        LabelComponent title = Components.label(Text.translatable(targetAction.titleKey));
        title.color(Color.ofArgb(0xFF00C8FF));
        title.margins(Insets.bottom(10));
        rootComponent.child(title);

        FlowLayout mainBox = Containers.horizontalFlow(Sizing.fixed(520), Sizing.fixed(200));
        mainBox.surface(Surface.flat(0xFA1A1A1A));
        mainBox.padding(Insets.of(10));
        mainBox.gap(15);

        FlowLayout leftCol = Containers.verticalFlow(Sizing.fixed(235), Sizing.fill(100));
        leftCol.gap(8);

        leftCol.child(Components.label(Text.translatable("mogdops-mod.block_selector.active_label")).color(Color.ofArgb(0xFFFFAA00)));

        FlowLayout activeRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        activeRow.verticalAlignment(VerticalAlignment.CENTER);
        activeRow.gap(8);

        activeIconWrapper = Containers.horizontalFlow(Sizing.fixed(28), Sizing.fixed(28));
        activeIconWrapper.verticalAlignment(VerticalAlignment.CENTER);
        activeIconWrapper.horizontalAlignment(HorizontalAlignment.CENTER);

        activeBox = Components.textBox(Sizing.fixed(120));
        activeBox.setText(activeBlockId);
        activeBox.onChanged().subscribe(val -> {
            this.activeBlockId = val;
            updatePreview();
        });

        FlowLayout pickAimBtn = createFlatButton(45, 18, Text.translatable("mogdops-mod.replace_screen.btn_aim"), () -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                Block block = client.world.getBlockState(((BlockHitResult) client.crosshairTarget).getBlockPos()).getBlock();
                this.activeBlockId = Registries.BLOCK.getId(block).toString();
                activeBox.setText(this.activeBlockId);
                updatePreview();
            }
        });

        activeRow.child(activeIconWrapper).child(activeBox).child(pickAimBtn);
        leftCol.child(activeRow);

        leftCol.child(Components.label(Text.translatable("mogdops-mod.block_selector.history")).color(Color.ofArgb(0xFF55FFFF)).margins(Insets.top(8)));

        historyRowWrapper = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        historyRowWrapper.gap(4);

        ScrollContainer<FlowLayout> historyScroll = Containers.horizontalScroll(Sizing.fill(100), Sizing.fixed(32), historyRowWrapper);
        historyScroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xAAFFFFFF)));
        leftCol.child(historyScroll);

        mainBox.child(leftCol);

        FlowLayout rightCol = Containers.verticalFlow(Sizing.fixed(250), Sizing.fill(100));
        rightCol.surface(Surface.flat(0xFF222222));
        rightCol.padding(Insets.of(8));
        rightCol.gap(6);

        LabelComponent gridTitle = Components.label(Text.translatable("mogdops-mod.block_selector.browser_title"));
        gridTitle.color(Color.ofArgb(0xFF00C8FF));
        rightCol.child(gridTitle);

        TextBoxComponent searchBox = Components.textBox(Sizing.fill(100));
        searchBox.setPlaceholder(Text.translatable("mogdops-mod.block_selector.search"));
        searchBox.setText(gridSearch);
        searchBox.onChanged().subscribe(text -> {
            this.gridSearch = text;
            rebuildGrid();
        });
        rightCol.child(searchBox);

        gridContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        gridContent.horizontalAlignment(HorizontalAlignment.CENTER);
        gridContent.gap(6);

        ScrollContainer<FlowLayout> gridScroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), gridContent);
        gridScroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xAAFFFFFF)));
        rightCol.child(gridScroll);

        mainBox.child(rightCol);
        rootComponent.child(mainBox);

        FlowLayout footerBar = Containers.horizontalFlow(Sizing.fixed(520), Sizing.content());
        footerBar.horizontalAlignment(HorizontalAlignment.CENTER);
        footerBar.verticalAlignment(VerticalAlignment.CENTER);
        footerBar.margins(Insets.top(12));
        footerBar.gap(20);

        FlowLayout runBtn = createFlatButton(180, 26, Text.translatable(targetAction.btnKey), this::executeAction);
        runBtn.surface(Surface.flat(0xFF00AA00));

        FlowLayout clearAirBtn = createFlatButton(180, 26, Text.translatable("mogdops-mod.block_selector.btn_clear_air"), this::executeClearAir);
        clearAirBtn.surface(Surface.flat(0xCCAA0000));

        footerBar.child(runBtn).child(clearAirBtn);
        rootComponent.child(footerBar);

        rebuildGrid();
        rebuildHistoryUI();
        updatePreview();
    }

    private void rebuildHistoryUI() {
        if (historyRowWrapper == null) return;
        historyRowWrapper.clearChildren();

        for (String bId : PlayerBlockHistoryManager.getHistory()) {
            Identifier id = Identifier.tryParse(bId);
            if (id != null && Registries.BLOCK.containsId(id)) {
                Block block = Registries.BLOCK.get(id);

                FlowLayout card = Containers.verticalFlow(Sizing.fixed(26), Sizing.fixed(26));
                card.surface(Surface.flat(0xFF333333));
                card.horizontalAlignment(HorizontalAlignment.CENTER);
                card.verticalAlignment(VerticalAlignment.CENTER);
                card.cursorStyle(CursorStyle.HAND);
                card.child(Components.item(new ItemStack(block)));

                card.mouseEnter().subscribe(() -> card.surface(Surface.flat(0xFF555555)));
                card.mouseLeave().subscribe(() -> card.surface(Surface.flat(0xFF333333)));
                card.mouseDown().subscribe((mX, mY, button) -> {
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        this.activeBlockId = bId;
                        if (activeBox != null) activeBox.setText(bId);
                        updatePreview();
                        return true;
                    }
                    return false;
                });
                historyRowWrapper.child(card);
            }
        }
    }

    private void updatePreview() {
        if (activeIconWrapper != null) {
            activeIconWrapper.clearChildren();
            Identifier id = Identifier.tryParse(activeBlockId);
            if (id != null && Registries.BLOCK.containsId(id)) {
                Block block = Registries.BLOCK.get(id);
                MogDopSModClient.activeBlock = block;
                activeIconWrapper.child(Components.item(new ItemStack(block)));
            }
        }
    }

    private void rebuildGrid() {
        if (gridContent == null) return;
        gridContent.clearChildren();

        String lowerFilter = gridSearch.toLowerCase();
        FlowLayout currentRow = null;
        int itemsInRow = 0;
        int maxItemsPerRow = 5;
        int renderCount = 0;

        for (Block block : allBlocks) {
            String name = block.getName().getString();
            String id = Registries.BLOCK.getId(block).getPath();
            if (!gridSearch.isEmpty() && !name.toLowerCase().contains(lowerFilter) && !id.toLowerCase().contains(lowerFilter)) {
                continue;
            }

            FlowLayout card = Containers.verticalFlow(Sizing.fixed(32), Sizing.fixed(32));
            card.surface(Surface.flat(0xFF333333));
            card.horizontalAlignment(HorizontalAlignment.CENTER);
            card.verticalAlignment(VerticalAlignment.CENTER);
            card.cursorStyle(CursorStyle.HAND);
            card.child(Components.item(new ItemStack(block)));

            card.mouseEnter().subscribe(() -> card.surface(Surface.flat(0xFF555555)));
            card.mouseLeave().subscribe(() -> card.surface(Surface.flat(0xFF333333)));
            card.mouseDown().subscribe((mX, mY, button) -> {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    String selectedId = Registries.BLOCK.getId(block).toString();
                    this.activeBlockId = selectedId;
                    if (activeBox != null) activeBox.setText(selectedId);
                    updatePreview();
                    return true;
                }
                return false;
            });

            if (currentRow == null || itemsInRow >= maxItemsPerRow) {
                if (currentRow != null) gridContent.child(currentRow);
                currentRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
                currentRow.gap(6);
                itemsInRow = 0;
            }
            currentRow.child(card);
            itemsInRow++;

            renderCount++;
            if (renderCount >= 80) break;
        }

        if (currentRow != null && itemsInRow > 0) {
            gridContent.child(currentRow);
        }
    }

    private void executeAction() {
        if (MogDopSModClient.getSelectionPoints().isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("mogdops-mod.error.positions_not_set"), false);
            }
            this.close();
            return;
        }

        PlayerBlockHistoryManager.pushToHistory(activeBlockId);

        switch (targetAction) {
            case FILL -> NetworkManager.sendToServer(new FillAreaPayload(MogDopSModClient.getSelectionPoints(), MogDopSModClient.currentSelectionMode, activeBlockId));
            case WALLS -> NetworkManager.sendToServer(new WallsPayload(MogDopSModClient.getSelectionPoints(), MogDopSModClient.currentSelectionMode, activeBlockId));
            case OUTLINE -> NetworkManager.sendToServer(new OutlinePayload(MogDopSModClient.getSelectionPoints(), MogDopSModClient.currentSelectionMode, activeBlockId));
        }

        this.close();
    }

    private void executeClearAir() {
        if (MogDopSModClient.getSelectionPoints().isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("mogdops-mod.error.positions_not_set"), false);
            }
            this.close();
            return;
        }
        NetworkManager.sendToServer(new FillAreaPayload(MogDopSModClient.getSelectionPoints(), MogDopSModClient.currentSelectionMode, "minecraft:air"));
        this.close();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            executeAction();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private FlowLayout createFlatButton(int width, int height, Text text, Runnable onClick) {
        FlowLayout btn = Containers.horizontalFlow(Sizing.fixed(width), Sizing.fixed(height));
        btn.surface(Surface.flat(0xFF444444));
        btn.cursorStyle(CursorStyle.HAND);
        btn.horizontalAlignment(HorizontalAlignment.CENTER);
        btn.verticalAlignment(VerticalAlignment.CENTER);

        LabelComponent lbl = Components.label(text);
        lbl.color(Color.ofArgb(0xFFFFFFFF));
        btn.child(lbl);

        btn.mouseEnter().subscribe(() -> btn.surface(Surface.flat(0xFF555555)));
        btn.mouseLeave().subscribe(() -> btn.surface(Surface.flat(0xFF444444)));
        btn.mouseDown().subscribe((mX, mY, button) -> {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                onClick.run();
                return true;
            }
            return false;
        });

        return btn;
    }
}