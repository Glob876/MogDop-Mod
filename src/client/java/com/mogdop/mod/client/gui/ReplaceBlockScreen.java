package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.network.ReplaceAreaPayload;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

public class ReplaceBlockScreen extends BaseOwoScreen<FlowLayout> {

    private String targetBlockId = "minecraft:stone";
    private String replacementBlockId = Registries.BLOCK.getId(MogDopSModClient.activeBlock).toString();

    private TextBoxComponent targetBox;
    private TextBoxComponent replaceBox;
    private FlowLayout targetIconWrapper;
    private FlowLayout replaceIconWrapper;

    private boolean selectingForTarget = false;
    private LabelComponent gridModeLabel;

    private String gridSearch = "";
    private FlowLayout gridContent;
    private final List<Block> allBlocks = new ArrayList<>();

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

        LabelComponent title = Components.label(Text.translatable("mogdops-mod.replace_screen.title"));
        title.color(Color.ofArgb(0xFF00C8FF));
        title.margins(Insets.bottom(12));
        rootComponent.child(title);

        FlowLayout mainBox = Containers.horizontalFlow(Sizing.fixed(520), Sizing.fixed(250));
        mainBox.surface(Surface.flat(0xFA1A1A1A));
        mainBox.padding(Insets.of(10));
        mainBox.gap(15);

        FlowLayout leftCol = Containers.verticalFlow(Sizing.fixed(235), Sizing.fill(100));
        leftCol.gap(10);

        leftCol.child(Components.label(Text.translatable("mogdops-mod.replace_screen.target_block")).color(Color.ofArgb(0xFFFFAA00)));

        FlowLayout targetRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        targetRow.verticalAlignment(VerticalAlignment.CENTER);
        targetRow.gap(6);

        targetIconWrapper = Containers.horizontalFlow(Sizing.fixed(24), Sizing.fixed(24));
        targetIconWrapper.verticalAlignment(VerticalAlignment.CENTER);
        targetIconWrapper.horizontalAlignment(HorizontalAlignment.CENTER);

        targetBox = Components.textBox(Sizing.fixed(100));
        targetBox.setText(targetBlockId);
        targetBox.onChanged().subscribe(val -> {
            this.targetBlockId = val;
            updatePreviews();
        });

        FlowLayout pickTargetAimBtn = createFlatButton(40, 18, Text.translatable("mogdops-mod.replace_screen.btn_aim"), () -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                Block block = client.world.getBlockState(((BlockHitResult) client.crosshairTarget).getBlockPos()).getBlock();
                this.targetBlockId = Registries.BLOCK.getId(block).toString();
                targetBox.setText(this.targetBlockId);
                updatePreviews();
            }
        });

        FlowLayout selectTargetGridBtn = createFlatButton(45, 18, Text.translatable("mogdops-mod.replace_screen.btn_browse"), () -> {
            this.selectingForTarget = true;
            updateGridModeLabel();
        });

        targetRow.child(targetIconWrapper).child(targetBox).child(pickTargetAimBtn).child(selectTargetGridBtn);
        leftCol.child(targetRow);

        leftCol.child(Components.label(Text.translatable("mogdops-mod.replace_screen.replacement_block")).color(Color.ofArgb(0xFF55FF55)).margins(Insets.top(5)));

        FlowLayout replaceRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        replaceRow.verticalAlignment(VerticalAlignment.CENTER);
        replaceRow.gap(6);

        replaceIconWrapper = Containers.horizontalFlow(Sizing.fixed(24), Sizing.fixed(24));
        replaceIconWrapper.verticalAlignment(VerticalAlignment.CENTER);
        replaceIconWrapper.horizontalAlignment(HorizontalAlignment.CENTER);

        replaceBox = Components.textBox(Sizing.fixed(100));
        replaceBox.setText(replacementBlockId);
        replaceBox.onChanged().subscribe(val -> {
            this.replacementBlockId = val;
            updatePreviews();
        });

        FlowLayout pickActiveBtn = createFlatButton(40, 18, Text.translatable("mogdops-mod.replace_screen.btn_active"), () -> {
            this.replacementBlockId = Registries.BLOCK.getId(MogDopSModClient.activeBlock).toString();
            replaceBox.setText(this.replacementBlockId);
            updatePreviews();
        });

        FlowLayout selectReplaceGridBtn = createFlatButton(45, 18, Text.translatable("mogdops-mod.replace_screen.btn_browse"), () -> {
            this.selectingForTarget = false;
            updateGridModeLabel();
        });

        replaceRow.child(replaceIconWrapper).child(replaceBox).child(pickActiveBtn).child(selectReplaceGridBtn);
        leftCol.child(replaceRow);

        FlowLayout runBtn = createFlatButton(180, 26, Text.translatable("mogdops-mod.replace_screen.btn_replace"), this::executeReplace);
        leftCol.child(runBtn.margins(Insets.top(12)));

        mainBox.child(leftCol);

        FlowLayout rightCol = Containers.verticalFlow(Sizing.fixed(250), Sizing.fill(100));
        rightCol.surface(Surface.flat(0xFF222222));
        rightCol.padding(Insets.of(8));
        rightCol.gap(6);

        gridModeLabel = Components.label(Text.literal(""));
        gridModeLabel.color(Color.ofArgb(0xFF00C8FF));
        rightCol.child(gridModeLabel);

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

        updateGridModeLabel();
        rebuildGrid();
        updatePreviews();
    }

    private void updateGridModeLabel() {
        if (gridModeLabel != null) {
            if (selectingForTarget) {
                gridModeLabel.text(Text.translatable("mogdops-mod.replace_screen.select_mode_target"));
            } else {
                gridModeLabel.text(Text.translatable("mogdops-mod.replace_screen.select_mode_replacement"));
            }
        }
    }

    private void updatePreviews() {
        if (targetIconWrapper != null) {
            targetIconWrapper.clearChildren();
            Identifier id = Identifier.tryParse(targetBlockId);
            if (id != null && Registries.BLOCK.containsId(id)) {
                Block block = Registries.BLOCK.get(id);
                targetIconWrapper.child(Components.item(new ItemStack(block)));
            }
        }

        if (replaceIconWrapper != null) {
            replaceIconWrapper.clearChildren();
            Identifier id = Identifier.tryParse(replacementBlockId);
            if (id != null && Registries.BLOCK.containsId(id)) {
                Block block = Registries.BLOCK.get(id);
                replaceIconWrapper.child(Components.item(new ItemStack(block)));
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
                    if (selectingForTarget) {
                        this.targetBlockId = selectedId;
                        if (targetBox != null) targetBox.setText(selectedId);
                    } else {
                        this.replacementBlockId = selectedId;
                        if (replaceBox != null) replaceBox.setText(selectedId);
                    }
                    updatePreviews();
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

    private void executeReplace() {
        if (MogDopSModClient.getSelectionPoints().isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("mogdops-mod.error.positions_not_set"), false);
            }
            this.close();
            return;
        }
        ClientPlayNetworking.send(new ReplaceAreaPayload(MogDopSModClient.getSelectionPoints(), MogDopSModClient.currentSelectionMode, targetBlockId, replacementBlockId));
        this.close();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            executeReplace();
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