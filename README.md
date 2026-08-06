# MogDop's Mod

MogDop's Mod is a sandbox toolkit and in-game WorldEdit utility for Minecraft Fabric 1.21.1. Inspired by Garry's Mod, it integrates real-time building utilities, entity customizers, item generators, and world controls directly into interactive menus.
Key Features
Multi-Tool & WorldEdit Utilities

## Selection Axe
Define 3D region bounds via left and right click. Expand selected faces dynamically using Alt + Scroll Wheel.
Multi-Tool Modes: Cycle modes on the fly using Ctrl + Scroll Wheel:
Selection: Standard WorldEdit region marking.
Remover: Instantly clears blocks in line of sight with a customizable radius (1–16 blocks).
Explosion: Fires explosive blasts with configurable power (1.0–50.0) and fire toggles.
Teleport: Instantly teleports the player to the targeted block (up to 100 blocks distance).
Spawner: Fires configured entity presets directly into the world.
Quick Action Menu (H Key): Fast radial menu providing instant access to Set (Fill), Replace, Undo, and Redo operations.
Block Selector & Replace GUI: Dual-column interface featuring search filters, crosshair block pickup, active block previews, and bulk block replacement controls.

## Entity Editor & Spawning System

Entity Spawner GUI (~ Key): Interactive browser with 3D entity models. Adjust spawn parameters including custom names, nameplate visibility, gravity (NoGravity), silent mode, glowing outlines, baby variants, slime sizes, and fire durations.
Toolgun Integration: Export modified entity NBT configurations directly into the Multi-Tool for rapid spawning.
Mob Spawner Slab: A custom functional block that periodically spawns selected mobs. Configurable via a dedicated GUI with parameters for spawn intervals (seconds), max active mob limits, and spawn radius.

## Item Creator & Management

Item Customizer: Generate custom item stacks with active modifier stacks, including custom names, max stack overrides, damage values, unbreakable flags, enchantment glint toggles, and enchantment levels.
Entity Purger: Scan entities within a 50-block radius to remove specific mob types in bulk or eliminate individual entities by UUID.

## World & Player Utilities

World Controls: One-click controls for setting day/night time cycles and clearing or starting rain weather.
Player Cheats: One-click toggles for survival flight, Speed III, Strength III, and status effect clearing.

## Dependencies

Minecraft: 1.21.1
Fabric Loader: >= 0.16.0 
Fabric API
oωo-lib: >= 0.12.0