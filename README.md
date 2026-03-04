# Burp Plugin💨 

<img width="1536" height="1024" alt="изображение" src="https://github.com/user-attachments/assets/1899e530-bd07-42f5-bd9d-fb5c5f6a39c9" />

## Features

* Plays the Minecraft burp sound 🤢
* Only players in a configurable radius hear it 🔊
* Chat message when someone burps 💬
* Cooldown to prevent spam ⏳
* Fully configurable ⚙️

## Command

`/burp`

Makes your player burp. That's it.

## Config

You can change everything in the config file.

Example:

```
cooldown: 10
radius: 20

sound:
  volume: 1.0
  pitch: 1.0

messages:
  burp: "&e%player% just burped!"
  heard: "&7Players who heard it: %count%"
  cooldown: "&cWait %time%s before burping again"
```

## Installation

1. Download the plugin `.jar`
2. Put it into your server `plugins` folder
3. Start or restart the server

## Requirements

* Spigot / Paper
* Java 17+

## Why I made this

Just a small fun plugin I made for practice and learning Bukkit plugin development.
