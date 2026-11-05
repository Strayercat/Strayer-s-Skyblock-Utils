# Strayer's Skyblock Utils (SSU)

A client-side Fabric mod with quality-of-life tweaks for Hypixel SkyBlock.

SSU started as a personal project: things I wanted in the game that other mods didn't do, or didn't do the way I liked. It's my way of contributing to the SkyBlock modding community.

**[Download on Modrinth](https://modrinth.com/mod/strayers-skyblock-utils)** · **[Report an issue](https://github.com/Strayercat/Strayer-s-Skyblock-Utils/issues)**

---

## Requirements
- [Fabric API](https://modrinth.com/mod/fabric-api) : Required
- [Cloth Config API](https://modrinth.com/mod/cloth-config) : Required
- [Mod Menu](https://modrinth.com/mod/modmenu) : Recommended

Everything can be configured with `/ssu config` or through Mod Menu. The config is split into **General**, **HUD Settings**, **Chat**, **Per Skill**, **Events** and **Voice Chat** tabs.

---

## Features

<details>
<summary><b>HUD Editor</b></summary>

- Move, scale and resize every SSU HUD with `/ssu hud` or the **Edit HUD Layout** button in the config
- Drag a HUD to move it, it snaps to the screen edges and center (hold Shift to disable snapping)
- Scroll over a HUD to scale it (Shift for fine steps), text and images scale with it
- Drag a HUD's sides to change its width, text wraps to fit
- Right click resets a HUD, double click resets its width
- HUDs can't go off screen, corners touching a screen edge are square and every other corner is rounded
- Works with the SSU HUD, custom sidebar, voice chat HUD, carry tracker and system messages HUD

</details>

<details>
<summary><b>Voice Chat</b></summary>

- Talk with other SSU users in your party or co-op, no Discord needed
- Push-to-talk or voice activation
- RNNoise noise suppression, a speech gate and automatic gain so voices come through clean and at a steady volume
- Mute and deafen, with your status shown to others
- Voice HUD showing who's connected and who's talking
- Mic test to hear what others hear
- Off by default, enable it in the **Voice Chat** tab of the config

</details>

<details>
<summary><b>SSU Users</b></summary>

- A gem appears next to other SSU users in the tab list, chat and nametags
- SSU users get a custom nameplate framed in their gem color
- Gem colors:
    - **Prismatic**: mod developer
    - **Purple**: VIPs who supported the mod during development
    - **Red**: helpers who gave useful feedback, bug reports or ideas
- A few easter eggs for VIPs and special users, like paw print trails and custom capes
- Emoji picker in chat, emojis are sent as `:name:` and show up for other SSU users

</details>

<details>
<summary><b>Slayer</b></summary>

- Carry tracker with `/ssu carry <player> <amount> <price>`
- Counts a carry every time the player's slayer boss dies
- Track several carries at once, each with its own progress bar on the carry HUD
- Prices accept `26k`, `1.2m`, `1,2m`, `1,200,000` and more
- A title and notification tell you when it's time to collect the payment, with the total owed
- `/ssu carry stop [player]` cancels a carry and shows how much is owed so far

</details>

<details>
<summary><b>Dungeons</b></summary>

- Floor join commands from party chat (`!m7`, `!f3`…)
- Floor auto-rejoin with `/ssu autorejoin <floor>`
- Downtime tracker with `!dt [reason]`, reminds you when the run ends
- Custom void lava texture in the F7 and M7 boss rooms (also removes the fire overlay)

</details>

<details>
<summary><b>Mining</b></summary>

- Corleone timer, reminds you 1 minute after killing him
- Umber and tungsten vein waypoints in the Glacite Tunnels
- Powder chest and loot chest rewards shown as a notification instead of chat spam

</details>

<details>
<summary><b>Foraging</b></summary>

- Tree gift notifications
- Phantom titles when a Phanflare, Phanpyre or Dreadwing spawns

</details>

<details>
<summary><b>Party</b></summary>

- Party commands for the leader: `!pt`, `!warp`, `!allinv`
- Party invites shown as clickable on-screen notifications, `/ssu partyinvites` lists recent ones
- Boop notifications that let you party whoever booped you
- Party members glow automatically

</details>

<details>
<summary><b>Chat</b></summary>

- Toggleable chat filters (work in progress), including stash reminders and stash pickup messages
- Stat commands that answer in the same channel: `!tps`, `!ping`, `!fps`
- Silly percentage commands in party, guild and co-op chat (`!sus`, `!furry` and more)
- Fancy emotes (`o/` becomes `( ﾟ◡ﾟ)/` and more)
- Separate HUDs for server messages and player messages
- Duplicate counter for system messages, repeated messages show `(x2)`, `(x3)`… with customizable text and color
- Chat peek and server chat history, with search
- Copy a system message by middle clicking it in the history, or with a keybind
- Separator width fix for non-default chat widths
- Send your coordinates (and location) to chat with a keybind

</details>

<details>
<summary><b>Glowing Players</b></summary>

- Make any player glow in the color of your choice
- Manage them with `/ssu glowingplayers` or the config GUI

</details>

<details>
<summary><b>HUD & Misc</b></summary>

- Contextual HUD with time, ping, TPS, FPS, coordinates, dailies, party info and island fun facts
- Custom sidebar
- Customizable mod colors and notification style
- Notifications and screenshot previews always show on top of everything, including chat and menus
- Zoom
- NPC locator with `/ssu npcfinder <npc>`
- Puff farming helper that tells you when to kill
- Screenshot HUD with a preview, click to copy
- Daily task reminders
- Spooky Festival chest titles and loot notifications
- Auto Hoppity eggs
- Vanilla recipe book redirects to the SkyBlock recipe book

</details>

---

## Commands

| Command | Description |
|---|---|
| `/ssu config` | Open the config screen |
| `/ssu hud` | Open the HUD editor |
| `/ssu carry <player> <amount> <price>` | Start tracking a slayer carry |
| `/ssu carry stop [player]` | Stop a carry (the player is required when carrying several) |
| `/ssu autorejoin <floor\|off>` | Auto-rejoin a floor (`m1`–`m7`, `f1`–`f7`) |
| `/ssu glowingplayers add <player>` | Make a player glow |
| `/ssu glowingplayers remove <player>` | Remove a glowing player |
| `/ssu glowingplayers list` | List glowing players |
| `/ssu glowingplayers clear` | Remove all glowing players |
| `/ssu glowingplayers gui` | Open the glowing players GUI |
| `/ssu npcfinder <npc>` | Locate an NPC |
| `/ssu partyinvites` | Show your last 10 party invites |
| `/ssu disableReminder <type>` | Disable a daily reminder |

`/strayerskyblockutils` works as an alias for `/ssu`.

## Keybinds

All keybinds can be changed in **Options → Controls → Strayer's Skyblock Utils**.

| Keybind | Default |
|---|---|
| Show SSU HUD | `Z` |
| Zoom | `C` |
| Chat Peek | `Left Alt` |
| Server Chat History | `H` |
| Copy Hovered System Message | Unbound |
| Send coordinates in chat | `Home` |
| Corleone Timer | `T` |
| Puff Timer | `Y` |
| Voice Chat Push To Talk | `V` |
| Voice Chat Mute | `M` |
| Voice Chat Deafen | Unbound |

---

## Privacy

SSU connects to its own server to find other SSU users and to set up voice chat.

- **Login:** your account is verified through Mojang's session server, the same check a normal server join does. Your access token is never sent to the SSU server.
- **Finding SSU users:** only hashed player names from your tab list, party and guild are sent.
- **Voice chat:** Audio goes directly between players and is encrypted, it never passes through the SSU server.

---

## Reporting Issues

This is the first mod I've developed and released, so bugs and crashes can happen :p

If you run into one, please [open an issue](https://github.com/Strayercat/Strayer-s-Skyblock-Utils/issues/new) and include:
- What happened and how to reproduce it
- Your mod version
- Screenshots if they help
- Your `logs/latest.log` file

---

## Credits

- [Concentus](https://github.com/lostromb/concentus), a pure Java Opus codec (BSD license)
- [rnnoise4j](https://github.com/henkelmax/rnnoise4j) by Max Henkel, Java bindings for [RNNoise](https://github.com/xiph/rnnoise) (BSD license)

## License

[CC0 1.0](LICENSE)