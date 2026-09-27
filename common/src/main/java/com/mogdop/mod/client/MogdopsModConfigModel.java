package com.mogdop.mod.client;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.RangeConstraint;

@Config(name = "mogdops-mod", wrapperName = "MogdopsModConfig")
public class MogdopsModConfigModel {
    public boolean hasSeenWelcome = false;
    public boolean hideChatHUD = true;
    public boolean enableCustomNotifications = true;
    public boolean vanillaSkin = false;
    public boolean toolExplosionFire = false;
    public boolean enableSelectionAnimation = true;
    public boolean enableSelectionParticles = true;
    public String toolSelectionColor = "#FFAA00";

    @RangeConstraint(min = 1, max = 16)
    public int toolRemoverRadius = 1;

    @RangeConstraint(min = 1.0f, max = 50.0f)
    public float toolExplosionPower = 4.0F;

    public static class Chat {
        public boolean bgEnabled = true;

        @RangeConstraint(min = 0, max = 255)
        public int bgOpacity = 0xAA;

        @RangeConstraint(min = 150, max = 600)
        public int widthPx = 310;

        @RangeConstraint(min = 60, max = 400)
        public int heightPx = 180;

        @RangeConstraint(min = 4, max = 7)
        public int padding = 6;

        public String accentColor = "#00C8FF";
    }

    public Chat chat = new Chat();
}