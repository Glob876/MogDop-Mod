# PLAN.md — MogDop-Mod: выделение, NextBots, чат

> Проверка: только `./gradlew :fabric:build`. NeoForge не трогать (временно сломан).

## 1. Анимации выделения ✅ ВЫПОЛНЕНО

### 1.1 Одна точка (pos1 без pos2)
- Проблема: `fabric/.../MogDopSModFabricClient.java:278` требует `pos1 && pos2`.
- Решение: ветка `pos1 != null && pos2 == null` → бокс `pos1..pos1+1` через те же `animMin/Max`, `enableSelectionAnimation()` (0.25), `drawFaceQuad` + `getSelectionColor()`. Сброс `selectionAnimInitialized=false` в Clear/Reset.
- Файлы: `MogDopSModFabricClient.java`, `MogDopSModClient.java`.

### 1.2 Попиксельное выделение (toolMode 6) — нет анимаций
- Проблема: `MogDopSModFabricClient.java:213-276` — статичный квад `0,0.8,1,0.35` + контур.
- Решение: `animImageC0..C3 + imageAnimInit` в `MogDopSModClient`, lerp углов к цели, пульс `alpha = 0.28 + 0.12 * sin(t)`. Сохранить `off=0.004`, `rotateAroundCenter()`. Сброс при новом ЛКМ.
- Файлы: те же.

## 2. NextBots ✅ ВЫПОЛНЕНО
- Суть: `PathAwareEntity` + билборд-рендер картинкой из `pics/`. Бегут за игроком, урон **настраиваемый (дефолт = ваншот)**.
- Новое:
  - `entity/NextbotEntity.java extends PathAwareEntity`: `FollowTargetGoal`, контактный урон из пресета, `movementSpeed` настраиваемая.
  - Регистрация типа + атрибуты в `MogDopSMod.java`.
  - `NextbotEntityRenderer` (билборд, `ClientImageTextureManager.getTexture(file)`).
  - Сеть: `SpawnNextbotPayload(fileName, pos, speed, damage)` + `UpdateNextbotPayload`.
  - UI: `NextbotSettingsScreen` — превью, Browse через `ImageSelectorScreen.pickCallback`, проверка **1:1 обязательна** (`NativeImage w == h`), чекбокс «обрезать принудительно» (центр-квадрат, оригинал не портить), слайдеры скорости/урона, Спавн/Применить.
  - Вкладка `NextBots` в `SpawnerScreen` + кейбинд `key.mogdops-mod.nextbot_settings`, `GLFW_KEY_UNKNOWN` → в настройках «Не назначена».
- Переиспользовать: `pics/` резолв, Explorer, `ClientImageTextureManager`.

## 3. Чат
### 3.1 Дефолт — тёмная подложка без рамок
- Убрать в `ChatNotificationHud.java:50-56` border + cyan-полосу. Одна общая подложка снизу-слева, сообщения поверх с паддингом 4–7 (дефолт 6).

### 3.2 Конфиг (пиксели) + слайдеры с предпросмотром
- `MogdopsModConfigModel.Chat`: `bgEnabled=true`, `bgOpacity (0..255, дефолт 0xAA)`, `widthPx (150..600, дефолт 310)`, `heightPx (px)`, `padding (4..7, дефолт 6)`, `accentColor="#00C8FF"`. + fallback в `MogDopSModClient.initConfig()` proxy.
- `ChatNotificationHud`: `x=12`, `y=H-bgH-25`, `wrapWidth=bgW-padding*2`; `fill(bgEnabled ? opacity|0x101015 : transparent)`.
- `SettingsTab`: слайдеры ширины/высоты/прозрачности/паддинга + колорпикер + тоггл; пока тянется слайдер — меню `~0.4` прозрачности + live-превью подложки от левого нижнего края.
- Файлы: `MogdopsModConfigModel.java`, `MogDopSModClient.java`, `ChatNotificationHud.java`, `NotificationManager.java`, `SpawnerScreen.java`, `MogDopConfigScreen.java`, `ru/en lang`.

## 4. Порядок
1. 1.1 → `:fabric:build`. 2. 1.2 → build. 3. Чат → build. 4. NextBot → build. 5. Локализация + финал build.

## Решения (зафиксированы)
1. Подложка чата — пиксели.
2. Урон NextBot — настраивается.
3. Кнопка меню NextBot — неназначена (`GLFW_KEY_UNKNOWN`).
