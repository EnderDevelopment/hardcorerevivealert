# HardcoreReviveAlert Minecraft Plugin

HardcoreReviveAlert is a Minecraft plugin that adds a hardcore death penalty system to your server. When a player dies in a hardcore world, they are automatically set to spectator mode for 24 hours, receive a red chat box message saying "hardcore (is uitgeschakeld)", and are automatically revived after the penalty period expires. The plugin tracks disabled players persistently, displays remaining time in chat messages, and restores players to survival mode with full health at their world spawn when the 24-hour penalty ends.

## Install

1. Download the HardcoreReviveAlert plugin JAR file.
2. Place the JAR file into your server's `plugins` folder.
3. Restart your server or run `/reload confirm` to load the plugin.
4. Verify the plugin loaded with `/plugins` and check for `EndHardcoreRevive` in the list.

## Use

1. Ensure you have a hardcore world configured on your server (worlds with `hardcore: true` or hardcore game mode settings).
2. When a player dies in a hardcore world:
   - Their game mode changes to spectator.
   - A red box appears in chat for all players: "hardcore (is uitgeschakeld)" and the player's name with "is voor 24 uur uitgeschakeld".
   - The dead player receives a red box with "Je bent uitgeschakeld voor 24 uur (remaining time)".
   - The dead player also receives a gray message: "Na 24 uur word je automatisch gerevived."
3. After 24 hours (configurable in `config.yml`), the plugin automatically checks every second and revives the player:
   - They are restored to survival mode.
   - Health is set to 20 (full hearts).
   - Hunger is set to 20 (full food).
   - They are teleported to their world's spawn point.
   - They receive a red box: "Je bent weer gerevived! Je kunt weer meedoen met hardcore."
4. To change the penalty duration, edit `plugins/EndHardcoreRevive/config.yml` and set `disabled-hours` to your desired number of hours. Then restart the server or reload the config with `/endreload` (if available).
5. Player penalties are saved automatically and survive server restarts. Online disabled players are applied their spectator mode on plugin load.

The plugin works with Minecraft 1.21 and Paper 1.21 API. It uses a config file for duration settings and handles player state persistence through the config. The red box chat messages use Unicode block characters (█) to create a visible frame with white text inside.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/967d26e8-30a1-435e-a9d2-63f65940dd2d?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=967d26e8-30a1-435e-a9d2-63f65940dd2d
