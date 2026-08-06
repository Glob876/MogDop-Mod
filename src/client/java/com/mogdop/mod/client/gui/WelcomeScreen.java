package com.mogdop.mod.client.gui;

import com.mogdop.mod.MogDopSMod;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
    private LabelComponent pageIndicatorLabel;
    private FlowLayout prevBtn;
    private FlowLayout nextBtn;
    private LabelComponent nextBtnLabel;

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

        // Страница 2: Топор-Мультитул
        pages.add(new WelcomePage(
                "mogdops-mod.welcome.page2.title",
                "mogdops-mod.welcome.page2.category",
                Items.IRON_AXE,
                List.of(
                        "mogdops-mod.welcome.page2.text1",
                        "mogdops-mod.welcome.page2.text2",
                        "mogdops-mod.welcome.page2.text3",
                        "mogdops-mod.welcome.page2.text4"
                )
        ));

        // Страница 3: Быстрая панель (Зажатие H)
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

        // Страница 4: Режимы выделения (Клавиша J)
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

        // Страница 5: Спавнер и редактор мобов (Клавиша ~)
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

        // Страница 6: Предметы, Мир и Читы
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

        // Страница 7: Полублок-спавнер и Настройки
        pages.add(new WelcomePage(
                "mogdops-mod.welcome.page7.title",
                "mogdops-mod.welcome.page7.category",
                MogDopSMod.MOB_SPAWNER_SLAB.asItem(),
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
        rootComponent.padding(Insets.of(15));

        // 1. Главный заголовок
        LabelComponent title = Components.label(Text.translatable("mogdops-mod.welcome.main_title"));
        title.color(Color.ofArgb(0xFF00C8FF));
        title.margins(Insets.bottom(12));
        rootComponent.child(title);

        // 2. Основная карточка страницы (520x220px)
        FlowLayout mainBox = Containers.horizontalFlow(Sizing.fixed(520), Sizing.fixed(220));
        mainBox.surface(Surface.flat(0xFA1A1A1A));
        mainBox.padding(Insets.of(12));

        pageContentWrapper = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
        mainBox.child(pageContentWrapper);
        rootComponent.child(mainBox);

        // 3. Нижняя панель навигации (Сбалансированная ширина 520px)
        FlowLayout navBar = Containers.horizontalFlow(Sizing.fixed(520), Sizing.fixed(26));
        navBar.verticalAlignment(VerticalAlignment.CENTER);
        navBar.margins(Insets.top(10));

        prevBtn = createFlatButton(90, 22, Text.literal("← ").append(Text.translatable("mogdops-mod.welcome.prev")), () -> {
            if (currentPage > 0) {
                currentPage--;
                updatePage();
            }
        });

        pageIndicatorLabel = Components.label(Text.literal(""));
        pageIndicatorLabel.color(Color.ofArgb(0xFFAAAAAA));

        FlowLayout centerIndicatorBox = Containers.horizontalFlow(Sizing.fixed(280), Sizing.content());
        centerIndicatorBox.horizontalAlignment(HorizontalAlignment.CENTER);
        centerIndicatorBox.child(pageIndicatorLabel);

        nextBtnLabel = Components.label(Text.literal(""));
        nextBtn = createFlatButton(130, 22, nextBtnLabel, () -> {
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

        // Левая колонка (Иконка + Категория)
        FlowLayout leftCol = Containers.verticalFlow(Sizing.fixed(120), Sizing.fill(100));
        leftCol.surface(Surface.flat(0xFF222222));
        leftCol.padding(Insets.of(10));
        leftCol.horizontalAlignment(HorizontalAlignment.CENTER);
        leftCol.verticalAlignment(VerticalAlignment.CENTER);
        leftCol.gap(8);

        FlowLayout iconBox = Containers.horizontalFlow(Sizing.fixed(48), Sizing.fixed(48));
        iconBox.horizontalAlignment(HorizontalAlignment.CENTER);
        iconBox.verticalAlignment(VerticalAlignment.CENTER);
        iconBox.child(Components.item(new ItemStack(page.iconItem)));
        leftCol.child(iconBox);

        LabelComponent catLabel = Components.label(Text.translatable(page.categoryKey));
        catLabel.color(Color.ofArgb(0xFFFFAA00));
        leftCol.child(catLabel);

        pageContentWrapper.child(leftCol);

        // Правая колонка с комфортным отступом слева (Insets.left(12))
        FlowLayout rightCol = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        rightCol.padding(Insets.of(2, 2, 12, 2));
        rightCol.gap(8);

        LabelComponent pageTitle = Components.label(Text.translatable(page.titleKey));
        pageTitle.color(Color.ofArgb(0xFF55FFFF));
        rightCol.child(pageTitle);

        FlowLayout textList = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        textList.gap(6);

        for (String key : page.textKeys) {
            FlowLayout bulletRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            bulletRow.gap(8);
            bulletRow.child(Components.label(Text.literal("•")).color(Color.ofArgb(0xFF00C8FF)));
            LabelComponent txt = Components.label(Text.translatable(key));
            txt.color(Color.ofArgb(0xFFDDDDDD));
            txt.sizing(Sizing.fixed(320), Sizing.content());
            bulletRow.child(txt);
            textList.child(bulletRow);
        }

        ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), textList);
        scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xAAFFFFFF)));
        rightCol.child(scroll);

        pageContentWrapper.child(rightCol);

        // Обновляем состояние индикатора страниц и кнопки «Далее →»
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

    private FlowLayout createFlatButton(int width, int height, LabelComponent label, Runnable onClick) {
        FlowLayout btn = Containers.horizontalFlow(Sizing.fixed(width), Sizing.fixed(height));
        btn.surface(Surface.flat(0xFF444444));
        btn.cursorStyle(CursorStyle.HAND);
        btn.horizontalAlignment(HorizontalAlignment.CENTER);
        btn.verticalAlignment(VerticalAlignment.CENTER);
        btn.child(label);

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

    private FlowLayout createFlatButton(int width, int height, Text text, Runnable onClick) {
        return createFlatButton(width, height, Components.label(text), onClick);
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