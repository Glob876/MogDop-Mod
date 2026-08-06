package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SelectionAxeHud implements HudRenderCallback {

    private static Entity hudMobEntity = null;
    private static String lastHudMobId = "";

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (client.currentScreen != null) return;

        ItemStack mainHand = client.player.getMainHandStack();
        if (MogDopSModClient.isSelectionAxe(mainHand)) {

            // Индикатор режима 3D Предпросмотра схематики
            if (MogDopSModClient.schematicPreviewActive) {
                int screenWidth = client.getWindow().getScaledWidth();
                int trX = screenWidth - 210;
                int trY = 60;

                drawContext.fill(trX, trY, trX + 200, trY + 36, 0xCC141414);

                String label = Text.translatable("mogdops-mod.hud.schematic_preview", MogDopSModClient.schematicName, MogDopSModClient.schematicSizeX, MogDopSModClient.schematicSizeY, MogDopSModClient.schematicSizeZ).getString();
                drawContext.drawTextWithShadow(client.textRenderer, Text.literal(label), trX + 8, trY + 6, 0xFF00C8FF);
                drawContext.drawTextWithShadow(client.textRenderer, Text.translatable("mogdops-mod.hud.schematic_instruction"), trX + 8, trY + 20, 0xFF55FF55);
            }

            // 1. СЛЕВА СВЕРХУ: ИНФОРМАЦИОННАЯ ПАНЕЛЬ КОНТЕНТА
            int tlX = 10;
            int tlY = 10;

            if (MogDopSModClient.currentToolMode == 0) {
                drawContext.fill(tlX, tlY, tlX + 180, tlY + 44, 0xCC141414);

                ItemStack activeStack = new ItemStack(MogDopSModClient.activeBlock);
                drawContext.drawItem(activeStack, tlX + 8, tlY + 14);

                String displayName = MogDopSModClient.activeBlock.getName().getString();
                String blockId = Registries.BLOCK.getId(MogDopSModClient.activeBlock).toString();

                drawContext.drawTextWithShadow(client.textRenderer, Text.literal(displayName), tlX + 32, tlY + 8, 0xFFFFFFFF);
                drawContext.drawTextWithShadow(client.textRenderer, Text.literal(blockId), tlX + 32, tlY + 22, 0x88FFFFFF);

            } else if (MogDopSModClient.currentToolMode == 4) {
                drawContext.fill(tlX, tlY, tlX + 180, tlY + 44, 0xCC141414);

                Identifier id = Identifier.tryParse(MogDopSModClient.activeSpawnId);
                if (id != null && Registries.ENTITY_TYPE.containsId(id)) {
                    if (hudMobEntity == null || !lastHudMobId.equals(MogDopSModClient.activeSpawnId)) {
                        try {
                            hudMobEntity = Registries.ENTITY_TYPE.get(id).create(client.world);
                        } catch (Exception ignored) {}
                        lastHudMobId = MogDopSModClient.activeSpawnId;
                    }

                    if (hudMobEntity instanceof LivingEntity) {
                        InventoryScreen.drawEntity(
                                drawContext,
                                tlX + 6, tlY + 6, tlX + 30, tlY + 38,
                                15,
                                0.0625F,
                                0.0F, 0.0F,
                                (LivingEntity) hudMobEntity
                        );
                    }

                    String mobName = Registries.ENTITY_TYPE.get(id).getName().getString();
                    drawContext.drawTextWithShadow(client.textRenderer, Text.literal(mobName), tlX + 32, tlY + 8, 0xFFFFFFFF);
                    drawContext.drawTextWithShadow(client.textRenderer, Text.literal(MogDopSModClient.activeSpawnId), tlX + 32, tlY + 22, 0x88FFFFFF);
                }
            }

            // 2. СПРАВА СВЕРХУ: ИНСТРУКЦИЯ И ОПИСАНИЕ РЕЖИМА
            int screenWidth = client.getWindow().getScaledWidth();
            int trX = screenWidth - 210;
            int trY = 10;

            drawContext.fill(trX, trY, trX + 200, trY + 44, 0xCC141414);

            String modeName = Text.translatable(MogDopSModClient.TOOL_MODE_KEYS[MogDopSModClient.currentToolMode]).getString();
            String descKey = "";
            if (MogDopSModClient.currentToolMode == 0) descKey = "mogdops-mod.tool_selector.mode.selection.desc";
            else if (MogDopSModClient.currentToolMode == 1) descKey = "mogdops-mod.tool_selector.mode.remover.desc";
            else if (MogDopSModClient.currentToolMode == 2) descKey = "mogdops-mod.tool_selector.mode.explosion.desc";
            else if (MogDopSModClient.currentToolMode == 3) descKey = "mogdops-mod.tool_selector.mode.teleport.desc";
            else if (MogDopSModClient.currentToolMode == 4) descKey = "mogdops-mod.tool_selector.mode.spawner.desc";
            else if (MogDopSModClient.currentToolMode == 5) descKey = "mogdops-mod.tool_selector.mode.schematics.desc";
            String briefDesc = Text.translatable(descKey).getString();

            drawContext.drawTextWithShadow(client.textRenderer, Text.translatable("mogdops-mod.hud.multitool"), trX + 8, trY + 6, 0xFFFFAA00);
            drawContext.drawTextWithShadow(client.textRenderer, Text.literal(modeName), trX + 8, trY + 18, 0xFF55FFFF);

            drawContext.getMatrices().push();
            drawContext.getMatrices().translate(trX + 8, trY + 31, 0);
            drawContext.getMatrices().scale(0.85F, 0.85F, 1.0F);
            drawContext.drawTextWithShadow(client.textRenderer, Text.literal(briefDesc), 0, 0, 0x88FFFFFF);
            drawContext.getMatrices().pop();
        }
    }
}