package com.mogdop.mod.client.gui;

import com.mogdop.mod.MogDopSMod;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class WelcomeScreen extends BaseOwoScreen<FlowLayout> {

    record WelcomePage(String titleKey, String categoryKey, Item iconItem, List<String> textKeys) {}

    private final List<WelcomePage> pages = new ArrayList<>();
    private int currentPage = 0;

    private FlowLayout pageContentWrapper;
    private SmallLabelComponent pageIndicatorLabel;
    private FlowLayout prevBtn;
    private FlowLayout nextBtn;
    private SmallLabelComponent nextBtnLabel;

    public static class SmallLabelComponent extends BaseComponent {
        private Text text;
        private final float scale;
        private final int color;
        private final boolean shadow;
        private HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;

        public SmallLabelComponent(Text text, float scale, int color, boolean shadow) {
            this.text = text;
            this.scale = scale;
            this.color = color;
            this.shadow = shadow;
        }

        public SmallLabelComponent text(Text text) {
            this.text = text;
            return this;
        }

        public Text text() {
            return this.text;
        }

        public SmallLabelComponent horizontalAlignment(HorizontalAlignment align) {
            this.horizontalAlignment = align;
            return this;
        }

        @Override
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;
            if (textRenderer == null) return;
            var matrices = context.getMatrices();
            matrices.push();

            int textWidth = textRenderer.getWidth(text);
            float renderX = this.x;
            if (horizontalAlignment == HorizontalAlignment.CENTER) {
                renderX = this.x + (this.width - textWidth * scale) / 2.0f;
            } else if (horizontalAlignment == HorizontalAlignment.RIGHT) {
                renderX = this.x + this.width - textWidth * scale;
            }

            matrices.translate(renderX, this.y, 0);
            matrices.scale(scale, scale, 1.0f);

            if (shadow) {
                context.drawTextWithShadow(textRenderer, text, 0, 0, color);
            } else {
                context.drawText(textRenderer, text, 0, 0, color, false);
            }
            matrices.pop();
        }

        @Override
        protected int determineHorizontalContentSize(Sizing sizing) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;
            return textRenderer != null ? (int) Math.ceil(textRenderer.getWidth(text) * scale) : 10;
        }

        @Override
        protected int determineVerticalContentSize(Sizing sizing) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;
            return textRenderer != null ? (int) Math.ceil(textRenderer.fontHeight * scale) : 8;
        }
    }

    public static class SmallWrappedTextComponent extends BaseComponent {
        private final float scale;
        private final int color;
        private final int maxWidth;
        private List<OrderedText> lines = new ArrayList<>();

        public SmallWrappedTextComponent(Text text, float scale, int color, int maxWidth) {
            this.scale = scale;
            this.color = color;
            this.maxWidth = maxWidth;
            var textRenderer = MinecraftClient.getInstance().textRenderer;
            if (textRenderer != null) {
                this.lines = textRenderer.wrapLines(text, (int)(maxWidth / scale));
            }
        }

        @Override
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;
            if (textRenderer == null) return;
            var matrices = context.getMatrices();
            matrices.push();
            matrices.translate(this.x, this.y, 0);
            matrices.scale(scale, scale, 1.0f);

            int yOffset = 0;
            for (OrderedText line : lines) {
                context.drawTextWithShadow(textRenderer, line, 0, yOffset, color);
                yOffset += 10;
            }
            matrices.pop();
        }

        @Override
        protected int determineHorizontalContentSize(Sizing sizing) {
            return maxWidth;
        }

        @Override
        protected int determineVerticalContentSize(Sizing sizing) {
            return (int) Math.ceil(Math.max(1, lines.size()) * 10 * scale);
        }
    }

    public static SmallLabelComponent smallLabel(Text text, float scale, int color) {
        return new SmallLabelComponent(text, scale, color, true);
    }

    public static SmallLabelComponent smallLabel(String text, float scale, int color) {
        return new SmallLabelComponent(Text.literal(text), scale, color, true);
    }

    public WelcomeScreen() {
        initPages();
    }

    private void initPages() {
        pages.clear();

        // Страница 1: Обзор
        pages.add(new WelcomePage(
                "mogdops-mod.welcome.page1.title",
                "mogdops-mod.welcome.page1.category",
                Items.NETHER_STAR,
                List.of(
                        "mogdops-mod.welcome.page1.text1",
                        "mogdops-mod.welcome.page1.text2",
                        "mogdops-mod.welcome.page1.text3"
                )
        ));

        // Страница 2: Творческий Посох
        pages.add(new WelcomePage(
                "mogdops-mod.welcome.page2.title",
                "mogdops-mod.welcome.page2.category",
                MogDopSMod.STAFF.get(),
                List.of(
                        "mogdops-mod.welcome.page2.text1",
                        "mogdops-mod.welcome.page2.text2",
                        "mogdops-mod.welcome.page2.text3",
                        "mogdops-mod.welcome.page2.text4"
                )
        ));

        // Страница 3: Быстрая панель
        pages.add(new WelcomePage(
                "mogdops-mod.welcome.page3.title",
                "mogdops-mod.welcome.page3.category",
                Items.BRICKS,
                List.of(
                        "mogdops-mod.welcome.page3.text1",
                        "mogdops-mod.welcome.page3.text2",
                        "mogdops-mod.welcome.page3.text3"
                )
        ));

        // Страница 4: Режимы выделения
        pages.add(new WelcomePage(
                "mogdops-mod.welcome.page4.title",
                "mogdops-mod.welcome.page4.category",
                Items.COMPASS,
                List.of(
                        "mogdops-mod.welcome.page4.text1",
                        "mogdops-mod.welcome.page4.text2",
                        "mogdops-mod.welcome.page4.text3",
                        "mogdops-mod.welcome.page4.text4"
                )
        ));

        // Страница 5: Спавнер мобов
        pages.add(new WelcomePage(
                "mogdops-mod.welcome.page5.title",
                "mogdops-mod.welcome.page5.category",
                Items.ZOMBIE_HEAD,
                List.of(
                        "mogdops-mod.welcome.page5.text1",
                        "mogdops-mod.welcome.page5.text2",
                        "mogdops-mod.welcome.page5.text3"
                )
        ));

        // Страница 6: Предметы и Мир
        pages.add(new WelcomePage(
                "mogdops-mod.welcome.page6.title",
                "mogdops-mod.welcome.page6.category",
                Items.DIAMOND_SWORD,
                List.of(
                        "mogdops-mod.welcome.page6.text1",
                        "mogdops-mod.welcome.page6.text2",
                        "mogdops-mod.welcome.page6.text3"
                )
        ));

        // Страница 7: Полублок-спавнер
        pages.add(new WelcomePage(
                "mogdops-mod.welcome.page7.title",
                "mogdops-mod.welcome.page7.category",
                MogDopSMod.MOB_SPAWNER_SLAB_ITEM.get(),
                List.of(
                        "mogdops-mod.welcome.page7.text1",
                        "mogdops-mod.welcome.page7.text2",
                        "mogdops-mod.welcome.page7.text3"
                )
        ));
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
        rootComponent.padding(Insets.of(6));

        SmallLabelComponent title = smallLabel(Text.translatable("mogdops-mod.welcome.main_title"), 0.85f, 0xFF00C8FF);
        title.margins(Insets.bottom(4));
        rootComponent.child(title);

        FlowLayout mainBox = Containers.horizontalFlow(Sizing.fixed(380), Sizing.fixed(140));
        mainBox.surface(Surface.flat(0xFA1A1A1A));
        mainBox.padding(Insets.of(6));

        pageContentWrapper = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
        mainBox.child(pageContentWrapper);
        rootComponent.child(mainBox);

        FlowLayout navBar = Containers.horizontalFlow(Sizing.fixed(380), Sizing.fixed(20));
        navBar.verticalAlignment(VerticalAlignment.CENTER);
        navBar.margins(Insets.top(6));

        prevBtn = createFlatButton(80, 18, smallLabel(Text.literal("← ").append(Text.translatable("mogdops-mod.welcome.prev")), 0.72f, 0xFFFFFFFF), () -> {
            if (currentPage > 0) {
                currentPage--;
                updatePage();
            }
        });

        pageIndicatorLabel = smallLabel(Text.literal(""), 0.72f, 0xFFAAAAAA);

        FlowLayout centerIndicatorBox = Containers.horizontalFlow(Sizing.fixed(180), Sizing.content());
        centerIndicatorBox.horizontalAlignment(HorizontalAlignment.CENTER);
        centerIndicatorBox.child(pageIndicatorLabel);

        nextBtnLabel = smallLabel(Text.literal(""), 0.72f, 0xFFFFFFFF);
        nextBtn = createFlatButton(100, 18, nextBtnLabel, () -> {
            if (currentPage < pages.size() - 1) {
                currentPage++;
                updatePage();
            } else {
                this.close();
            }
        });

        navBar.child(prevBtn).child(centerIndicatorBox).child(nextBtn);
        rootComponent.child(navBar);

        updatePage();
    }

    private void updatePage() {
        if (pageContentWrapper == null) return;
        pageContentWrapper.clearChildren();

        WelcomePage page = pages.get(currentPage);

        FlowLayout leftCol = Containers.verticalFlow(Sizing.fixed(90), Sizing.fill(100));
        leftCol.surface(Surface.flat(0xFF222222));
        leftCol.padding(Insets.of(6));
        leftCol.horizontalAlignment(HorizontalAlignment.CENTER);
        leftCol.verticalAlignment(VerticalAlignment.CENTER);
        leftCol.gap(4);

        FlowLayout iconBox = Containers.horizontalFlow(Sizing.fixed(32), Sizing.fixed(32));
        iconBox.horizontalAlignment(HorizontalAlignment.CENTER);
        iconBox.verticalAlignment(VerticalAlignment.CENTER);
        iconBox.child(Components.item(new ItemStack(page.iconItem)));
        leftCol.child(iconBox);

        SmallLabelComponent catLabel = smallLabel(Text.translatable(page.categoryKey), 0.72f, 0xFFFFAA00);
        catLabel.horizontalAlignment(HorizontalAlignment.CENTER);
        leftCol.child(catLabel);

        pageContentWrapper.child(leftCol);

        FlowLayout rightCol = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        rightCol.padding(Insets.of(2, 2, 8, 2));
        rightCol.gap(4);

        SmallLabelComponent pageTitle = smallLabel(Text.translatable(page.titleKey), 0.78f, 0xFF55FFFF);
        rightCol.child(pageTitle);

        FlowLayout textList = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        textList.gap(4);

        for (String key : page.textKeys) {
            FlowLayout bulletRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            bulletRow.gap(4);
            bulletRow.child(smallLabel("•", 0.72f, 0xFF00C8FF));
            bulletRow.child(new SmallWrappedTextComponent(Text.translatable(key), 0.72f, 0xFFDDDDDD, 245));
            textList.child(bulletRow);
        }

        ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), textList);
        scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xAAFFFFFF)));
        rightCol.child(scroll);

        pageContentWrapper.child(rightCol);

        if (pageIndicatorLabel != null) {
            pageIndicatorLabel.text(Text.translatable("mogdops-mod.welcome.page_count", (currentPage + 1), pages.size()));
        }

        if (nextBtnLabel != null) {
            if (currentPage == pages.size() - 1) {
                nextBtnLabel.text(Text.translatable("mogdops-mod.welcome.finish"));
            } else {
                nextBtnLabel.text(Text.literal("").append(Text.translatable("mogdops-mod.welcome.next")).append(" →"));
            }
        }
    }

    private FlowLayout createFlatButton(int width, int height, Component labelComp, Runnable onClick) {
        FlowLayout btn = Containers.horizontalFlow(Sizing.fixed(width), Sizing.fixed(height));
        btn.surface(Surface.flat(0xFF444444));
        btn.cursorStyle(CursorStyle.HAND);
        btn.horizontalAlignment(HorizontalAlignment.CENTER);
        btn.verticalAlignment(VerticalAlignment.CENTER);
        btn.child(labelComp);

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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT && currentPage > 0) {
            currentPage--;
            updatePage();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (currentPage < pages.size() - 1) {
                currentPage++;
                updatePage();
            } else {
                this.close();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}