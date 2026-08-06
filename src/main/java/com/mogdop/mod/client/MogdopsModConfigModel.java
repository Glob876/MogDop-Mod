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
}