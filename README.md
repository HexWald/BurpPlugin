# BurpPlugin

`/burp` plays a burp with a little breath cloud. Nearby players get the sound and a chat message.

`/burp quiet` uses a lower volume and smaller radii. `/burp deep` plays a lower note. `/burp mute` toggles the sound, chat and cloud from other players for you. The mute setting is saved between sessions.

## Install

Use Java 21 and a Minecraft 1.21 server with the Bukkit API. Build with `mvn clean package`, then put `target/burp-plugin-1.1.jar` in `plugins` and restart the server.

## Binds

| Command | Gesture |
| --- | --- |
| `/burp bind double-sneak` | Tap sneak twice |
| `/burp bind sneak-swap` | Use swap hands while sneaking |
| `/burp bind off` | Turn the bind off |

Binds are off by default and saved per player. They follow your Minecraft controls. Add `quiet` after a gesture to use quiet mode, for example `/burp bind double-sneak quiet`. Set the gesture again by itself or add `normal` to switch back. `/burp bind` shows your current choice.

## Settings

Edit `plugins/BurpPlugin/config.yml` and run `/burp reload` in game or `burp reload` in the console. The reload permission is `burp.reload` and defaults to operators. `burp.use` controls player commands and binds and is allowed by default.

`sound.radius`, `messages.radius` and `breath.radius` control who gets the sound, the chat message and the cloud. Uncomment the radius you want to change. Each one uses the top-level `radius` when omitted, which defaults to 10 blocks. For sound over longer distances, you may also need to increase `sound.volume` because Minecraft fades sound with distance.

Set `quiet.volume-multiplier` and `quiet.radius-multiplier` to a value from 0 to 1. By default, quiet mode uses 35% of the volume setting and half of each radius. `sound.deep-pitch` sets the deep note. Set `sound.pitch-variation` to 0 to keep the regular note fixed. All modes share the same cooldown, which defaults to 5 seconds. Set `breath.green` to `true` for a green tint or `breath.enabled` to `false` to turn the cloud off.
